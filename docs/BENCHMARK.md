# Benchmark y overhead

Fecha de ejecucion: 2026-08-15.

Ambiente medido: stack local Docker Compose sobre macOS, usando los mismos microservicios Quarkus que se despliegan en AWS. El flujo probado fue `k6 -> service-a -> service-b -> DynamoDB Local`. La comparacion se hizo alternando `OTEL_ENABLED=false` y `OTEL_ENABLED=true`.

Configuracion de carga:

- Herramienta: k6.
- VUs maximos: 10.
- Warmup: 30s.
- Duracion estable: 2m.
- Ramp down: 30s.
- Endpoint: `GET /orders/{id}`.
- Criterios k6: `http_req_failed < 1%`, `p95 < 750ms`, `p99 < 1500ms`.

## Resultados k6

Evidencias crudas:

- `docs/evidence/benchmark-without-otel-20260815-224421.log`
- `docs/evidence/benchmark-with-otel-20260815-224421.log`

| Metric | Without OTel | With OTel | Observed overhead |
| --- | ---: | ---: | ---: |
| Average latency | 32.73 ms | 70.30 ms | +114.78% |
| p50 latency | 29.33 ms | 41.24 ms | +40.61% |
| p95 latency | 65.30 ms | 166.61 ms | +155.15% |
| p99 latency | 96.92 ms | 508.31 ms | +424.47% |
| Throughput | 8.133 req/s | 7.862 req/s | -3.33% |
| Error rate | 0.06% | 0.00% | -0.06 pp |

## CPU y memoria local

Valores tomados con `docker stats --no-stream` inmediatamente despues de cada ejecucion. Son muestras puntuales, no maximos historicos.

| Component | Without OTel | With OTel | Difference |
| --- | ---: | ---: | ---: |
| service-a CPU | 0.40% | 0.41% | +0.01 pp |
| service-b CPU | 0.55% | 2.51% | +1.96 pp |
| App CPU total | 0.95% | 2.92% | +207.37% |
| service-a memory | 179.4 MiB | 188.0 MiB | +8.6 MiB |
| service-b memory | 241.5 MiB | 220.8 MiB | -20.7 MiB |
| App memory total | 420.9 MiB | 408.8 MiB | -12.1 MiB |
| Collector memory total | 82.47 MiB idle | 96.39 MiB active | +13.92 MiB |

## Interpretacion

En esta ejecucion local el escenario con OTel mostro mayor latencia que el baseline, especialmente en p99. La latencia p99 paso de 96.92 ms a 508.31 ms, equivalente a un overhead observado de +424.47%. Aunque el p99 aumento, se mantuvo por debajo del umbral definido de 1500 ms. El throughput bajo 3.33%, de 8.133 req/s a 7.862 req/s.

El mayor costo medible fue la latencia de cola y el uso adicional de recursos asociado al procesamiento y exportacion de telemetria. La muestra puntual de CPU/memoria muestra mayor uso de CPU local de aplicacion, especialmente en `service-b`, y memoria adicional de los collectors.

Formula:

```text
overhead % = ((OTel - baseline) / baseline) * 100
```

Escenario recomendado:

```text
warmup 30s
baseline 5m
otel 5m
```

Local:

```bash
make benchmark-baseline
make benchmark-otel
```

Ejecucion completa con evidencias separadas:

```bash
make benchmark-overhead VUS=10 WARMUP=30s DURATION=2m
```

El script guarda salidas completas en `docs/evidence/benchmark-without-otel-*.log` y `docs/evidence/benchmark-with-otel-*.log`.

AWS:

1. Desplegar con `otel_enabled=false`.
2. Ejecutar k6 local apuntando a `service-a`.
3. Capturar p50/p95/p99/throughput/error rate.
4. Desplegar con `otel_enabled=true`.
5. Ejecutar el mismo k6.
6. Capturar CPU y memoria desde ECS/CloudWatch.

## Evidencia AWS complementaria

El dashboard Grafana en AWS muestra metricas CloudWatch ECS por `ServiceName`:

| ECS service | CPUUtilization Maximum 24h | MemoryUtilization Maximum 24h |
| --- | ---: | ---: |
| otel-lab-service-a | 34.65% | 16.02% |
| otel-lab-service-b | 45.97% | 18.65% |

Estas metricas provienen de `AWS/ECS` con dimensiones `ClusterName=otel-lab-cluster` y `ServiceName`.
