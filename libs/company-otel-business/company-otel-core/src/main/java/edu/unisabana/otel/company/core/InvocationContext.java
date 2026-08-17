package edu.unisabana.otel.company.core;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class InvocationContext {
    private final Method method;
    private final Object[] arguments;
    private final String[] argumentNames;
    private final Object result;
    private final Throwable exception;

    public InvocationContext(Method method, Object[] arguments, String[] argumentNames, Object result, Throwable exception) {
        this.method = method;
        this.arguments = arguments == null ? new Object[0] : Arrays.copyOf(arguments, arguments.length);
        this.argumentNames = normalizeArgumentNames(method, argumentNames, this.arguments.length);
        this.result = result;
        this.exception = exception;
    }

    public Method method() {
        return method;
    }

    public Object result() {
        return result;
    }

    public Throwable exception() {
        return exception;
    }

    public Optional<Object> argument(String name) {
        Map<String, Object> indexed = argumentsByName();
        return Optional.ofNullable(indexed.get(name));
    }

    public Map<String, Object> argumentsByName() {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int i = 0; i < arguments.length; i++) {
            values.put(argumentNames[i], arguments[i]);
            values.put("arg" + i, arguments[i]);
            values.put("p" + i, arguments[i]);
        }
        return values;
    }

    public InvocationContext withResult(Object value) {
        return new InvocationContext(method, arguments, argumentNames, value, exception);
    }

    public InvocationContext withException(Throwable value) {
        return new InvocationContext(method, arguments, argumentNames, result, value);
    }

    private static String[] normalizeArgumentNames(Method method, String[] names, int count) {
        String[] normalized = new String[count];
        Parameter[] parameters = method == null ? new Parameter[0] : method.getParameters();
        for (int i = 0; i < count; i++) {
            if (names != null && i < names.length && names[i] != null && !names[i].isBlank()) {
                normalized[i] = names[i];
            } else if (i < parameters.length && parameters[i].isNamePresent()) {
                normalized[i] = parameters[i].getName();
            } else {
                normalized[i] = "arg" + i;
            }
        }
        return normalized;
    }
}
