# Benchmark y overhead

Fecha de ejecucion: 2026-08-17.

Ambiente medido: stack local Docker Compose sobre Windows, usando los mismos microservicios Quarkus que se despliegan en AWS. El flujo probado fue `k6 -> service-a -> service-b -> DynamoDB Local`. La comparacion se hizo alternando `OTEL_ENABLED=false` y `OTEL_ENABLED=true`, ejecutando siempre primero el caso sin OTel y despues el caso con OTel.

Configuracion de carga:

- Herramienta: k6.
- VUs maximos: 75 (dentro del rango 50-100 solicitado).
- Warmup: 30s.
- Duracion estable: 5m.
- Ramp down: 30s.
- Endpoint: `GET /orders/{id}`.
- Criterios k6: `http_req_failed < 1%`, `p95 < 750ms`, `p99 < 1500ms`.

## Resultados k6

Evidencias crudas:

- `docs/evidence/benchmark-without-otel-20260817-002533.log`
- `docs/evidence/benchmark-with-otel-20260817-002533.log`

| Metric | Without OTel | With OTel | Observed delta |
| --- | ---: | ---: | ---: |
| Average latency | 7.35 ms | 7.00 ms | -4.76% |
| p50 latency | 4.77 ms | 5.44 ms | +14.05% |
| p95 latency | 20.03 ms | 15.53 ms | -22.47% |
| p99 latency | 40.20 ms | 31.24 ms | -22.29% |
| Throughput | 68.260 req/s | 68.264 req/s | +0.01% |
| Error rate | 0.02% (5/24603) | 0.00% (0/24601) | -0.02 pp |
| Requests totales | 24,603 | 24,601 | — |
| VUs maximos | 75 | 75 | — |

Corridas anteriores (10 VUs, 2 min, fuera del rango exigido por el enunciado) quedan en `docs/evidence/benchmark-*-20260815-224421.log` solo como referencia historica; no se usan para la tabla anterior.

## CPU y memoria local

**No capturado en esta corrida.** El script `benchmark/run-overhead.sh` no automatiza `docker stats` durante la carga — es un hallazgo pendiente, no un dato inventado. Los valores de CPU/memoria que aparecian aqui en la version anterior de este documento correspondian a la corrida de 10 VUs/2min del 2026-08-15 y se retiraron porque no son comparables con esta corrida de 75 VUs/5min.

## Interpretacion

**El resultado es contraintuitivo y hay que decirlo asi en el reporte, no maquillarlo:** la fase con OTel activo no mostro el overhead esperado — de hecho p95 y p99 salieron mas bajos que el baseline. La muestra es robusta (24.6k requests por fase, miles de observaciones detras del p99, no las ~14 de la corrida anterior de 10 VUs), asi que no es ruido por tamano de muestra.

La explicacion mas plausible no es que la instrumentacion acelere el sistema, sino un **efecto de orden**: el script siempre corre "without-otel" primero, contra imagenes recien construidas, JVM fria, y cache de SO/Docker sin calentar; para cuando arranca "with-otel" segundos despues, todo eso ya esta caliente (JIT del JVM, conexiones a DynamoDB Local ya establecidas, capas de Docker en cache). El p50 subio +14% mientras que p95/p99 bajaron ~22% — un patron mixto, no una mejora sistematica, consistente con ruido de entorno mas que con un efecto real de la instrumentacion.

Para un resultado que aisle mejor el overhead real de OTel del efecto de warm-up, el siguiente paso metodologico seria alternar el orden entre corridas (o promediar varias corridas con orden invertido). Se documenta la limitacion explicitamente en vez de reportar una conclusion que los datos no sostienen.

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

Ejecucion completa con evidencias separadas (75 VUs / 5m son el default; se puede sobreescribir):

```bash
make benchmark-overhead
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
