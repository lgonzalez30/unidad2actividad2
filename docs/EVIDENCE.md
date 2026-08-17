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
| 5 | `05-grafana-explore-logs-by-traceid.png` | Grafana Explore, datasource CloudWatch, query `filter trace_id = "..."` sobre `/otel-lab/service-a` y `/otel-lab/service-b`: 4 lineas de log con el mismo `trace_id` |
| 6 | `06-grafana-explore-trace-by-id.png` | Grafana Explore, datasource Jaeger, busqueda por el mismo Trace ID: waterfall completo de 6 spans. Demuestra el pivot `trace_id` entre logs y trazas que exige el enunciado (Fase 3) |

Correlacion `trace_id` verificada end-to-end en Grafana Explore el 2026-08-17 con `trace_id = 17f247afd480cf136de2c5bbb727e063`, presente en logs de ambos servicios y en la traza de Jaeger. Se encontro y corrigio en el proceso un bug de infraestructura: el security group de Jaeger no permitia trafico interno desde el security group de Grafana (solo permitia el puerto 16686 desde `allowed_ingress_cidr`), lo que rompia el datasource de Jaeger en Grafana. Ver `infra/networking.tf`.

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
