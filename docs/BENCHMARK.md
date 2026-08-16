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

| Metric | Without OTel | With OTel | Observed overhead |
| --- | ---: | ---: | ---: |
| Average latency | 26.71 ms | 16.72 ms | -37.40% |
| p50 latency | 18.85 ms | 14.82 ms | -21.38% |
| p95 latency | 69.61 ms | 28.19 ms | -59.50% |
| p99 latency | 143.70 ms | 52.44 ms | -63.51% |
| Throughput | 8.216 req/s | 8.264 req/s | +0.57% |
| Error rate | 0.00% | 0.00% | 0 pp |

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

En esta ejecucion local el escenario con OTel mostro menor latencia que el baseline. Esto no significa que OTel reduzca latencia; es un efecto esperable de benchmark local corto con JVM/Quarkus, caches, JIT warmup, DynamoDB Local y contenedores ya calientes. La conclusion tecnica correcta es que, bajo esta carga pequena, el overhead de latencia no fue observable de forma negativa y se mantuvo dentro de los SLOs configurados.

El mayor costo medible fue en CPU local de aplicacion, especialmente en `service-b`, y en memoria de los collectors. La memoria de aplicacion total no aumento en la muestra puntual, pero el collector agrego aproximadamente 96.39 MiB activos para ambos sidecars.

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
