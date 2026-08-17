package edu.unisabana.otel.company.core;

import io.opentelemetry.api.baggage.Baggage;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Optional;

public final class SimpleAttributeResolver implements AttributeResolver {
    @Override
    public Optional<Object> resolve(String expression, InvocationContext context) {
        if (expression == null || expression.isBlank()) {
            return Optional.empty();
        }
        String trimmed = expression.trim();
        if (!trimmed.startsWith("#")) {
            return Optional.of(trimmed);
        }

        String[] path = trimmed.substring(1).split("\\.");
        if (path.length == 0 || path[0].isBlank()) {
            return Optional.empty();
        }

        Optional<Object> root = resolveRoot(path[0], context);
        if (root.isEmpty()) {
            return Optional.empty();
        }

        Object current = root.get();
        for (int i = 1; i < path.length; i++) {
            if (current == null || path[i].isBlank()) {
                return Optional.empty();
            }
            current = readProperty(current, path[i]).orElse(null);
        }
        return Optional.ofNullable(current);
    }

    private Optional<Object> resolveRoot(String name, InvocationContext context) {
        return switch (name) {
            case "result" -> Optional.ofNullable(context.result());
            case "exception" -> Optional.ofNullable(context.exception());
            case "baggage" -> Optional.of(Baggage.current());
            default -> context.argument(name);
        };
    }

    private Optional<Object> readProperty(Object target, String property) {
        if (target instanceof Baggage baggage) {
            return Optional.ofNullable(baggage.getEntryValue(property));
        }
        if (target instanceof Map<?, ?> map) {
            return Optional.ofNullable(map.get(property));
        }
        if (target instanceof Optional<?> optional) {
            if ("present".equals(property) || "isPresent".equals(property)) {
                return Optional.of(optional.isPresent());
            }
            if ("empty".equals(property) || "isEmpty".equals(property)) {
                return Optional.of(optional.isEmpty());
            }
            return optional.flatMap(value -> readProperty(value, property));
        }
        if (target instanceof Throwable throwable) {
            if ("message".equals(property)) {
                return Optional.ofNullable(throwable.getMessage());
            }
            if ("type".equals(property) || "class".equals(property)) {
                return Optional.of(throwable.getClass().getName());
            }
        }

        Method accessor = findAccessor(target.getClass(), property);
        if (accessor == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(accessor.invoke(target));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private Method findAccessor(Class<?> type, String property) {
        for (String methodName : accessorNames(property)) {
            try {
                Method method = type.getMethod(methodName);
                if (isAllowedAccessor(method)) {
                    return method;
                }
            } catch (NoSuchMethodException ignored) {
                // Try next accessor form.
            }
        }
        return null;
    }

    private String[] accessorNames(String property) {
        if (property.isBlank()) {
            return new String[0];
        }
        String capitalized = Character.toUpperCase(property.charAt(0)) + property.substring(1);
        return new String[] { property, "get" + capitalized, "is" + capitalized };
    }

    private boolean isAllowedAccessor(Method method) {
        return method.getParameterCount() == 0
                && !Modifier.isStatic(method.getModifiers())
                && !method.getName().equals("getClass")
                && !method.getDeclaringClass().equals(Object.class);
    }
}
