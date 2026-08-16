# CloudWatch Logs correlation evidence

Evidence captured from AWS CloudWatch Logs for the distributed request:

```text
trace_id = ecdcaea8c31b1d951c53d0f59a6fa461
```

This evidence shows that `service-a` and `service-b` emit structured JSON logs with the same `trace_id`, and each log line includes `span_id`. This validates log correlation across the HTTP call `service-a -> service-b`.

## Commands

```bash
START_TIME=$(($(date +%s) * 1000 - 5 * 60 * 1000))

aws logs filter-log-events \
  --region us-east-1 \
  --log-group-name /otel-lab/service-a \
  --start-time "$START_TIME" \
  --limit 20 \
  --query 'events[].message' \
  --output text
```

```bash
aws logs filter-log-events \
  --region us-east-1 \
  --log-group-name /otel-lab/service-b \
  --filter-pattern "ecdcaea8c31b1d951c53d0f59a6fa461" \
  --query 'events[].message' \
  --output text
```

## service-a logs

```json
{"timestamp":"2026-08-16T01:18:23.216501119Z","sequence":154,"loggerClassName":"org.jboss.logging.Logger","loggerName":"edu.unisabana.otel.servicea.OrderResource","level":"INFO","message":"received order request order_id=order-1","threadName":"executor-thread-1","threadId":16,"spanId":"4d62f4dc88c7e7d4","trace_id":"ecdcaea8c31b1d951c53d0f59a6fa461","traceId":"ecdcaea8c31b1d951c53d0f59a6fa461","sampled":"true","span_id":"4d62f4dc88c7e7d4","ndc":"","hostName":"ip-10-42-1-146.ec2.internal","processName":"/opt/java/openjdk/bin/java","processId":1,"service":"service-a"}
{"timestamp":"2026-08-16T01:18:23.275773948Z","sequence":155,"loggerClassName":"org.jboss.logging.Logger","loggerName":"edu.unisabana.otel.servicea.OrderService","level":"INFO","message":"processed order order_id=order-1 product_id=product-3","threadName":"executor-thread-1","threadId":16,"spanId":"213190aa4aa01404","parentId":"4d62f4dc88c7e7d4","traceId":"ecdcaea8c31b1d951c53d0f59a6fa461","trace_id":"ecdcaea8c31b1d951c53d0f59a6fa461","sampled":"true","span_id":"213190aa4aa01404","ndc":"","hostName":"ip-10-42-1-146.ec2.internal","processName":"/opt/java/openjdk/bin/java","processId":1,"service":"service-a"}
```

## service-b logs

```json
{"timestamp":"2026-08-16T01:18:23.228271309Z","sequence":152,"loggerClassName":"org.jboss.logging.Logger","loggerName":"edu.unisabana.otel.serviceb.ProductResource","level":"INFO","message":"received product lookup product_id=product-3","threadName":"executor-thread-1","threadId":16,"spanId":"65e827351e2d7d29","parentId":"16342194f0248a65","traceId":"ecdcaea8c31b1d951c53d0f59a6fa461","trace_id":"ecdcaea8c31b1d951c53d0f59a6fa461","sampled":"true","span_id":"65e827351e2d7d29","ndc":"","hostName":"ip-10-42-1-51.ec2.internal","processName":"/opt/java/openjdk/bin/java","processId":1,"service":"service-b"}
{"timestamp":"2026-08-16T01:18:23.272431213Z","sequence":153,"loggerClassName":"org.jboss.logging.Logger","loggerName":"edu.unisabana.otel.serviceb.ProductService","level":"INFO","message":"product found product_id=product-3","threadName":"executor-thread-1","threadId":16,"spanId":"1312b7af5f99e9c0","parentId":"65e827351e2d7d29","traceId":"ecdcaea8c31b1d951c53d0f59a6fa461","trace_id":"ecdcaea8c31b1d951c53d0f59a6fa461","sampled":"true","span_id":"1312b7af5f99e9c0","ndc":"","hostName":"ip-10-42-1-51.ec2.internal","processName":"/opt/java/openjdk/bin/java","processId":1,"service":"service-b"}
```

## Validation

- Both services emitted logs using structured JSON.
- `service-a` and `service-b` share the same `trace_id`.
- Every relevant log line includes `span_id`.
- The messages show the business flow from order processing to product lookup.
- The evidence supports log-to-trace pivoting using `trace_id`.
