# Costo

## Budget

Terraform crea:

```text
otel-lab-budget
USD 5/month
```

Notificaciones:

- 50% actual: USD 2.50
- 80% actual: USD 4.00
- 100% actual: USD 5.00
- 100% forecasted: USD 5.00 forecasted

AWS Budget no es un hard spending cap. AWS no detiene automaticamente ECS al llegar a USD 5.

## Cost drivers

- ECS Fargate runtime.
- Public IPv4 en las tasks.
- CloudWatch Logs.
- CloudWatch Logs para logs OTLP exportados por el Collector.
- AWS X-Ray.
- DynamoDB on-demand.
- ECR storage.
- Cloud Map privado para descubrimiento interno de `service-b`.

## Cost avoidance

No se usa:

- NAT Gateway.
- Application Load Balancer.
- Network Load Balancer.
- RDS/Aurora.
- EKS.
- Amazon Managed Prometheus.
- Amazon Managed Grafana.
- OpenSearch.
- multiples AZ.
- multiples replicas.
- Auto Scaling.

## Decision sobre Cloud Map

La consigna prohibe Route53 salvo necesidad tecnica demostrable. Sin ALB, sin exponer `service-b` publicamente y manteniendo dos ECS Services separados, `service-a` necesita resolver `service-b`. Se usa Cloud Map privado como excepcion tecnica minima. La alternativa seria exponer `service-b` o introducir un Load Balancer, ambas peores para seguridad/costo.

## Operational rule

```text
NO dejar Fargate funcionando despues del laboratorio.
```

Flujo esperado:

```text
terraform apply
pruebas
benchmark
capturas
terraform destroy
```

## Cleanup checks

```bash
make verify-cleanup
```

Revisar especialmente:

- ECS services.
- ECS tasks.
- public IPv4.
- DynamoDB.
- CloudWatch Logs.
- ECR.
- VPC resources.

El Budget puede recrearse en futuras ejecuciones. No se usa `prevent_destroy` porque bloquearia `terraform destroy`.
