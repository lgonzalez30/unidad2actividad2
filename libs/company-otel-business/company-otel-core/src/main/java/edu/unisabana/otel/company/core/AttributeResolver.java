package edu.unisabana.otel.company.core;

import java.util.Optional;

public interface AttributeResolver {
    Optional<Object> resolve(String expression, InvocationContext context);
}
