# ADR: Libreria Reutilizable Para Spans De Negocio

## Estado

Aceptada.

## Contexto

El laboratorio ya tenia auto-instrumentacion OpenTelemetry para HTTP, DynamoDB, metricas y logs. Faltaba evitar que los spans custom de negocio quedaran acoplados a cada microservicio mediante llamadas directas a `Span.current().setAttribute(...)` o anotaciones especificas de una libreria de instrumentacion.

El objetivo adicional es que el enfoque pueda reutilizarse en otros servicios Java, incluso si despues se usa Spring Boot, Kotlin o algun framework distinto a Quarkus.

## Decision

Se implemento una libreria con dos capas:

- `company-otel-core`: contiene `@OtelCompanyTrace`, configuracion, resolucion de atributos y ejecucion del span usando solo `opentelemetry-api`.
- `company-otel-jboss-mdc`: adaptador opcional para publicar `trace_id` y `span_id` en JBoss MDC sin repetir codigo en cada servicio.
- `company-otel-aspectj`: contiene un adapter AOP con AspectJ para interceptar metodos anotados.

La configuracion de atributos vive en `company-otel.properties` dentro de cada servicio. La aplicacion host sigue siendo responsable de configurar SDK/exporters mediante Quarkus OpenTelemetry, Java agent u otro runtime.

## Razones

- Mantiene el dominio limpio: el codigo de negocio solo conoce una anotacion declarativa.
- Evita acoplamiento al SDK: la libreria usa OpenTelemetry API, no configura exporters.
- Funciona de forma transversal: AspectJ puede aplicarse fuera de Spring AOP o CDI.
- Permite gobierno centralizado: redaccion, truncamiento, nombres de spans y atributos se controlan en un solo lugar.
- Reduce duplicacion entre microservicios.

## Consecuencias

Positivas:

- Los spans custom quedan estandarizados.
- La correlacion de logs tambien queda estandarizada para runtimes basados en JBoss Logging.
- Se puede desactivar o cambiar atributos sin tocar codigo Java.
- La libreria es portable a otros frameworks Java.

Costos:

- El build necesita weaving AspectJ.
- Hay que validar el weave en CI para asegurar que los metodos anotados fueron interceptados.
- Para operaciones asincronicas avanzadas puede requerirse extension de contexto.

## Aplicacion En Este Laboratorio

`service-a` usa:

```java
@OtelCompanyTrace(operation = "otel.company.order.process")
```

`service-b` usa:

```java
@OtelCompanyTrace(operation = "otel.company.product.lookup")
```

Los spans generados aparecen dentro de la misma traza distribuida que los spans HTTP y DynamoDB, permitiendo ver el flujo `service-a -> service-b -> DynamoDB` con contexto de negocio.
