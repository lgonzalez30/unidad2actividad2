package edu.unisabana.otel.company.core;

@FunctionalInterface
public interface BusinessCallable {
    Object call() throws Throwable;
}
