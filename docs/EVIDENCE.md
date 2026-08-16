# Evidencias

Guardar capturas en `docs/screenshots/`.

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
