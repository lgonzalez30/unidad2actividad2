package edu.unisabana.otel.company.core;

import io.opentelemetry.api.trace.Span;

@FunctionalInterface
public interface CurrentSpanContextBinder {
    CurrentSpanContextBinder NOOP = span -> () -> {
    };

    BoundContext bind(Span span);

    @FunctionalInterface
    interface BoundContext extends AutoCloseable {
        @Override
        void close();
    }
}
