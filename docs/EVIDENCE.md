# Evidencias

Capturas organizadas por fecha de despliegue en `docs/screenshots/<YYYY-MM-DD>/`:

- `docs/screenshots/2026-08-15/` — despliegue original. Historico: corresponde a una revision de codigo anterior a los fixes de logs OTLP y metricas AWS (ver `docs/BENCHMARK.md`), no se renombraron individualmente.
- `docs/screenshots/2026-08-16/` — despliegue actual, post-fix de logs/metricas y benchmark a 75 VUs/5min. Numeradas en el orden en que se tomaron. Ver tabla de mapeo abajo.

## Mapeo de capturas — 2026-08-16

| # | Archivo | Que muestra |
| --- | --- | --- |
| 1 | `01-jaeger-trace-list.png` | Jaeger UI, busqueda por `service-a`, lista de trazas recientes (6 spans cada una) |
| 2 | `02-jaeger-waterfall-full-trace.png` | Traza completa expandida: `service-a` -> `process-order` -> `service-b` -> `lookup-product` -> `DynamoDb.GetItem` |
| 3 | `03-jaeger-process-order-attributes.png` | Span `process-order` expandido con atributos custom (`order.id`, `product.id`, `business.operation`) |
| 4 | `04-grafana-aws-dashboard.png` | Dashboard Grafana AWS, 6 paneles (2 paneles de CPU/Memoria siguen siendo texto estatico, pendiente) |

Checklist:

- [x] `service-a` disponible.
- [x] llamada `service-a -> service-b`.
- [x] acceso real a DynamoDB.
- [x] trace end-to-end.
- [x] mismo `trace_id` en ambos servicios.
- [x] custom span `process-order`.
- [x] custom span `lookup-product`.
- [x] logs JSON.
- [x] `trace_id` y `span_id` en logs.
- [x] CloudWatch Logs recibe logs de contenedor.
- [x] Collector AWS configurado con exporter `awscloudwatchlogs`.
- [x] AWS X-Ray muestra `service-a` y `service-b`.
- [x] Jaeger UI en AWS ECS muestra traza completa.
- [x] metricas disponibles.
- [x] Grafana dashboard con 6 paneles.
- [x] Grafana AWS tiene datasources CloudWatch y Jaeger.
- [x] metricas internas del Collector.
- [x] benchmark baseline.
- [x] benchmark OTel.
- [x] tabla overhead completa.
- [x] AWS Budget USD 5 creado.
- [ ] `terraform destroy` ejecutado.
- [ ] verificacion cleanup ejecutada.
