package edu.unisabana.otel.company.core;

import io.opentelemetry.api.trace.SpanKind;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OtelCompanyTrace {
    String operation();

    SpanKind kind() default SpanKind.INTERNAL;

    boolean recordResult() default true;
}
