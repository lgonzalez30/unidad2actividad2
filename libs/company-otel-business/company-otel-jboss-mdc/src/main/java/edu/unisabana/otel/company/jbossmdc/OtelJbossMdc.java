package edu.unisabana.otel.company.jbossmdc;

import edu.unisabana.otel.company.core.CurrentSpanContextBinder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import org.jboss.logging.MDC;

public final class OtelJbossMdc {
    public static final CurrentSpanContextBinder BINDER = OtelJbossMdc::putSpan;

    private static final String TRACE_ID = "trace_id";
    private static final String SPAN_ID = "span_id";

    private OtelJbossMdc() {
    }

    public static CurrentSpanContextBinder.BoundContext putCurrentSpan() {
        return putSpan(Span.current());
    }

    public static CurrentSpanContextBinder.BoundContext putSpan(Span span) {
        SpanContext context = span.getSpanContext();
        if (!context.isValid()) {
            return () -> {
            };
        }
        MDC.put(TRACE_ID, context.getTraceId());
        MDC.put(SPAN_ID, context.getSpanId());
        return OtelJbossMdc::clear;
    }

    public static void clear() {
        MDC.remove(TRACE_ID);
        MDC.remove(SPAN_ID);
    }
}
