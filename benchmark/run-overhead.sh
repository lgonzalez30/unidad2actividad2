#!/usr/bin/env bash
set -euo pipefail

# Git Bash on Windows rewrites leading-"/" args (like /scripts/load-test.js,
# a path inside the k6 container) into host paths before docker sees them.
# No-op on real bash (macOS/Linux).
export MSYS_NO_PATHCONV=1

VUS="${VUS:-75}"
WARMUP="${WARMUP:-30s}"
DURATION="${DURATION:-5m}"
EVIDENCE_DIR="${EVIDENCE_DIR:-docs/evidence}"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"

mkdir -p "$EVIDENCE_DIR"

run_case() {
  local label="$1"
  local otel_enabled="$2"
  local logfile="$EVIDENCE_DIR/benchmark-${label}-${TIMESTAMP}.log"

  echo "============================================================"
  echo "BENCHMARK: ${label}"
  echo "OTEL_ENABLED=${otel_enabled}"
  echo "VUS=${VUS} WARMUP=${WARMUP} DURATION=${DURATION}"
  echo "LOG_FILE=${logfile}"
  echo "============================================================"

  OTEL_ENABLED="$otel_enabled" docker compose up --build -d

  echo "Waiting for service-a readiness..."
  for attempt in $(seq 1 30); do
    if curl -fsS http://localhost:8080/orders/order-1 >/dev/null; then
      echo "service-a ready"
      break
    fi
    if [ "$attempt" -eq 30 ]; then
      echo "service-a did not become ready" >&2
      exit 1
    fi
    sleep 2
  done

  {
    echo "============================================================"
    echo "BENCHMARK: ${label}"
    echo "OTEL_ENABLED=${otel_enabled}"
    echo "VUS=${VUS} WARMUP=${WARMUP} DURATION=${DURATION}"
    echo "Started at: $(date -Iseconds)"
    echo "============================================================"
    docker compose --profile benchmark run --rm \
      -e VUS="$VUS" \
      -e WARMUP="$WARMUP" \
      -e DURATION="$DURATION" \
      k6 run --summary-trend-stats "avg,min,med,p(90),p(95),p(99),max" /scripts/load-test.js
    echo "Finished at: $(date -Iseconds)"
  } | tee "$logfile"

  echo "Saved evidence: $logfile"
}

run_case "without-otel" "false"
run_case "with-otel" "true"

echo "============================================================"
echo "Benchmark evidence generated in ${EVIDENCE_DIR}:"
ls -1 "$EVIDENCE_DIR"/benchmark-*-"$TIMESTAMP".log
echo "============================================================"
