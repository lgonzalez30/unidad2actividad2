# Unidad 2 Actividad 2 - OpenTelemetry Microservices Lab

Laboratorio academico de observabilidad distribuida con dos microservicios Quarkus instrumentados con OpenTelemetry y desplegados en AWS ECS Fargate.

La arquitectura implementa el flujo `service-a -> service-b -> DynamoDB`, con trazas distribuidas, logs JSON correlacionados por `trace_id`, metricas, dashboard Grafana, Jaeger, AWS X-Ray y benchmark de overhead con k6.

## Arquitectura

```mermaid
flowchart TD
  K6[k6 local] --> A[service-a Quarkus]
  A -->|HTTP W3C tracecontext| B[service-b Quarkus]
  B --> D[(DynamoDB otel-lab-products)]
```

Diagrama editable:

- [docs/architecture.drawio](docs/architecture.drawio)

## Arquitectura AWS

```mermaid
flowchart TB
  subgraph AWS
    subgraph PublicSubnet[Public Subnet unica]
      subgraph TaskA[ECS Fargate Task A]
        SA[service-a]
        CA[otel-collector-a sidecar]
      end
      subgraph TaskB[ECS Fargate Task B]
        SB[service-b]
        CB[otel-collector-b sidecar]
      end
      J[Jaeger ECS Service]
      G[Grafana ECS Service]
    end
    SA -->|HTTP interno| SB
    SB --> DDB[(DynamoDB)]
    SA -->|OTLP gRPC localhost| CA
    SB -->|OTLP gRPC localhost| CB
    CA --> XR[AWS X-Ray]
    CB --> XR
    CA -->|OTLP gRPC| J
    CB -->|OTLP gRPC| J
    CA --> CW
    CB --> CW
    G -->|CloudWatch datasource| CW
    G -->|Jaeger datasource| J
  end
```

Se evita NAT Gateway, ALB, RDS, EKS, Managed Prometheus y Managed Grafana para reducir costo. `service-a`, Grafana y Jaeger se expusieron directamente con public IP de Fargate. `service-b` no se expone publicamente y se resuelve mediante Cloud Map privado.

## Observabilidad

- HTTP inbound: instrumentacion automatica de Quarkus REST.
- HTTP outbound: instrumentacion automatica de Quarkus REST Client.
- DynamoDB: instrumentacion AWS SDK via Quarkus Amazon Services con `quarkus.dynamodb.telemetry.enabled=true`.
- Custom spans: `process-order` y `lookup-product`.
- Context propagation: W3C Trace Context entre `service-a` y `service-b`.
- Logs: JSON en stdout con `service`, `trace_id` y `span_id`; en AWS tambien quedan en CloudWatch Logs.
- Traces AWS: aplicacion -> sidecar Collector -> AWS X-Ray y Jaeger.
- Logs OTLP AWS: Collector -> CloudWatch Logs mediante `awscloudwatchlogs`.
- Metrics local: Collector Prometheus exporter + Prometheus/Grafana OSS locales.
- Metrics AWS: CloudWatch `AWS/ECS` + Grafana CloudWatch datasource.

## Endpoints

Local Docker Compose:

- service-a: http://localhost:8080/orders/order-1
- service-b: http://localhost:8081/products/product-1
- Jaeger: http://localhost:16686
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000

AWS ECS Fargate:

- service-a: http://54.211.65.191:8080
- Grafana: http://3.90.145.54:3000
- Jaeger: http://3.89.48.195:16686

Nota: las IP publicas de Fargate son efimeras y pueden cambiar si se recrean las tasks. Resolverlas con:

```bash
make service-a-url
make grafana-url
make jaeger-url
```

## Local

```bash
docker compose up --build
curl http://localhost:8080/orders/order-1
docker compose --profile benchmark run --rm k6 run /scripts/load-test.js
```

## Terraform

Configurar variables:

```bash
cp infra/terraform.tfvars.example infra/terraform.tfvars
```

Editar `infra/terraform.tfvars` con el email real. No versionar ese archivo.

Validar y planear:

```bash
make plan
```

Desplegar solo cuando sea explicito:

```bash
make deploy
```

Resolver URL publica efimera de `service-a`:

```bash
make service-a-url
```

Destruir:

```bash
make destroy
make verify-cleanup
```

## Benchmark y Overhead

Baseline local sin OTel:

```bash
make benchmark-baseline
```

Con OTel:

```bash
make benchmark-otel
```

Ejecucion comparativa completa:

```bash
make benchmark-overhead
```

Resultados finales documentados (75 VUs, 5 min, rango 50-100 usuarios exigido por el enunciado):

| Metric | Without OTel | With OTel | Observed delta |
| --- | ---: | ---: | ---: |
| Average latency | 7.35 ms | 7.00 ms | -4.76% |
| p95 latency | 20.03 ms | 15.53 ms | -22.47% |
| p99 latency | 40.20 ms | 31.24 ms | -22.29% |
| Throughput | 68.260 req/s | 68.264 req/s | +0.01% |
| Error rate | 0.02% | 0.00% | -0.02 pp |

El resultado no muestra el overhead esperado (con-OTel salio mas rapido); la interpretacion mas plausible es un efecto de orden de ejecucion, no que la instrumentacion acelere el sistema. Ver [docs/BENCHMARK.md](docs/BENCHMARK.md) para el analisis completo.

Evidencia:

- [docs/BENCHMARK.md](docs/BENCHMARK.md)
- [docs/evidence/benchmark-without-otel-20260816-002533.log](docs/evidence/benchmark-without-otel-20260816-002533.log)
- [docs/evidence/benchmark-with-otel-20260816-002533.log](docs/evidence/benchmark-with-otel-20260816-002533.log)

## Budget

Terraform crea `otel-lab-budget` por USD 5/mes con alertas:

- 50% actual
- 80% actual
- 100% actual
- 100% forecasted

AWS Budget no es un hard cap. AWS no detiene automaticamente ECS al llegar a USD 5. La proteccion real del laboratorio es la combinacion de budget, alertas, recursos minimos, ausencia de NAT/ALB/RDS/EKS y `terraform destroy`.

## Evidencias

Entregables principales:

- Codigo instrumentado: [service-a](service-a), [service-b](service-b)
- Configuracion OTel Collector: [otel/collector-aws.yaml](otel/collector-aws.yaml), [otel/collector-local.yaml](otel/collector-local.yaml)
- Infraestructura Terraform: [infra](infra)
- Dashboard Grafana AWS: [grafana-aws/dashboards/otel-aws-lab.json](grafana-aws/dashboards/otel-aws-lab.json)
- Diagrama Draw.io: [docs/architecture.drawio](docs/architecture.drawio)
- Reporte tecnico Markdown, recomendado para ver en GitHub: [docs/REPORT.md](docs/REPORT.md)
- Reporte tecnico PDF, descarga directa si el visor de GitHub no carga: [docs/REPORT.pdf](docs/REPORT.pdf?raw=1)
- Evidencias: [docs/EVIDENCE.md](docs/EVIDENCE.md), [docs/evidence](docs/evidence), [docs/screenshots](docs/screenshots)
