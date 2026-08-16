# k6 benchmark clean run evidence

This evidence captures a successful k6 benchmark execution against the local Docker Compose stack.

## Scenario

- Execution mode: local.
- Script: `/scripts/load-test.js`.
- Max VUs: 10.
- Duration: 3m total scenario window.
- Endpoint flow: `k6 -> service-a -> service-b -> DynamoDB Local`.
- Result: successful run with zero failed HTTP requests.

## Key results

| Metric | Value |
| --- | ---: |
| Checks | 100.00% |
| HTTP failures | 0.00% |
| HTTP requests | 1487 |
| Throughput | 8.250876 req/s |
| Average latency | 20.85 ms |
| Median latency | 17.92 ms |
| p90 latency | 31.98 ms |
| p95 latency | 42.05 ms |
| p99 latency | 96.81 ms |
| Max latency | 184.4 ms |

## Raw summary excerpt

```text
✓ status is 200
✓ has product

checks.........................: 100.00% 2974 out of 2974
http_req_duration..............: avg=20.85ms  min=3.61ms med=17.92ms p(90)=31.98ms  p(95)=42.05ms  p(99)=96.81ms  max=184.4ms
http_req_failed................: 0.00%   0 out of 1487
http_reqs......................: 1487    8.250876/s
iteration_duration.............: avg=1.02s    min=1s     med=1.01s   p(90)=1.03s    p(95)=1.04s    p(99)=1.1s     max=1.18s
iterations.....................: 1487    8.250876/s
vus............................: 1       min=1            max=10
vus_max........................: 10      min=10           max=10
```

## Interpretation

The run satisfies the benchmark thresholds:

- `http_req_failed < 1%`: observed `0.00%`.
- `p95 < 750ms`: observed `42.05ms`.
- `p99 < 1500ms`: observed `96.81ms`.

This output is suitable as supporting evidence for the overhead analysis section, together with the comparative table in `docs/BENCHMARK.md`.
