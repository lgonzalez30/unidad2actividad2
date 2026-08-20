package edu.unisabana.otel.company.core;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class BusinessSpanRunner {
    public static final String INSTRUMENTATION_SCOPE = "otel.company.business";
    public static final String LIBRARY_VERSION = "1.0.0";
    private static final Logger LOG = Logger.getLogger(BusinessSpanRunner.class.getName());
    private static final long WARNING_INTERVAL_NANOS = 30_000_000_000L;

    private final Tracer tracer;
    private final CompanyOtelConfig config;
    private final AttributeResolver attributeResolver;
    private final CurrentSpanContextBinder contextBinder;
    private final AtomicLong nextWarningNanos = new AtomicLong();

    public BusinessSpanRunner(Tracer tracer, CompanyOtelConfig config, AttributeResolver attributeResolver) {
        this(tracer, config, attributeResolver, CurrentSpanContextBinder.NOOP);
    }

    public BusinessSpanRunner(Tracer tracer, CompanyOtelConfig config, AttributeResolver attributeResolver,
            CurrentSpanContextBinder contextBinder) {
        this.tracer = tracer;
        this.config = config;
        this.attributeResolver = attributeResolver;
        this.contextBinder = contextBinder;
    }

    public static BusinessSpanRunner createDefault() {
        return new BusinessSpanRunner(
                GlobalOpenTelemetry.getTracer(INSTRUMENTATION_SCOPE, LIBRARY_VERSION),
                CompanyOtelConfig.loadDefault(),
                new SimpleAttributeResolver());
    }

    public Object run(OtelCompanyTrace annotation, Method method, Object[] arguments, String[] argumentNames,
            BusinessCallable callable) throws Throwable {
        OperationConfig operation = config.operation(annotation.operation());
        if (!operation.enabled()) {
            return callable.call();
        }

        Span span = tracer.spanBuilder(operation.spanName())
                .setSpanKind(annotation.kind())
                .startSpan();
        span.setAttribute("company.operation", operation.operation());

        InvocationContext baseContext = new InvocationContext(method, arguments, argumentNames, null, null);
        try (Scope ignored = span.makeCurrent();
                CurrentSpanContextBinder.BoundContext ignoredContext = contextBinder.bind(span)) {
            Object result = callable.call();
            InvocationContext resultContext = baseContext.withResult(result);
            if (annotation.recordResult() && operation.captureResult()) {
                resolveAttributes(span, operation, resultContext);
            } else {
                resolveAttributes(span, operation, baseContext);
            }
            span.setStatus(StatusCode.OK);
            return result;
        } catch (Throwable error) {
            InvocationContext errorContext = baseContext.withException(error);
            if (operation.recordExceptions()) {
                span.recordException(error);
            }
            span.setStatus(StatusCode.ERROR, error.getMessage() == null ? error.getClass().getName() : error.getMessage());
            resolveAttributes(span, operation, errorContext);
            throw error;
        } finally {
            span.end();
        }
    }

    private void resolveAttributes(Span span, OperationConfig operation, InvocationContext context) {
        for (Map.Entry<String, String> entry : operation.attributes().entrySet()) {
            try {
                String attributeName = entry.getKey();
                String expression = entry.getValue();
                if (config.isRedacted(attributeName, expression)) {
                    span.setAttribute(attributeName, "[REDACTED]");
                    continue;
                }
                Optional<Object> value = attributeResolver.resolve(expression, context);
                value.ifPresent(resolved -> setAttribute(span, attributeName, sanitize(resolved)));
            } catch (RuntimeException error) {
                warnLimited("Failed to resolve company OTel attribute for operation " + operation.operation(), error);
            }
        }
    }

    private Object sanitize(Object value) {
        if (value instanceof CharSequence chars) {
            String text = chars.toString();
            int max = config.maxAttributeLength();
            return text.length() <= max ? text : text.substring(0, max);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private void setAttribute(Span span, String name, Object value) {
        if (value instanceof Boolean bool) {
            span.setAttribute(AttributeKey.booleanKey(name), bool);
        } else if (value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte) {
            span.setAttribute(AttributeKey.longKey(name), ((Number) value).longValue());
        } else if (value instanceof Float || value instanceof Double) {
            span.setAttribute(AttributeKey.doubleKey(name), ((Number) value).doubleValue());
        } else if (value instanceof Iterable<?> iterable) {
            span.setAttribute(AttributeKey.stringKey(name), iterable.toString());
        } else {
            span.setAttribute(AttributeKey.stringKey(name), String.valueOf(value));
        }
    }

    private void warnLimited(String message, RuntimeException error) {
        long now = System.nanoTime();
        long next = nextWarningNanos.get();
        if (now >= next && nextWarningNanos.compareAndSet(next, now + WARNING_INTERVAL_NANOS)) {
            LOG.log(Level.WARNING, message, error);
        }
    }
}
