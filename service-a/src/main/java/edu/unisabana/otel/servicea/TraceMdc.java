package edu.unisabana.otel.servicea;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import org.jboss.logging.MDC;

final class TraceMdc {
    private TraceMdc() {
    }

    static void putCurrentSpan() {
        SpanContext context = Span.current().getSpanContext();
        if (context.isValid()) {
            MDC.put("trace_id", context.getTraceId());
            MDC.put("span_id", context.getSpanId());
        }
    }
}
