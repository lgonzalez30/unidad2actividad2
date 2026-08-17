package edu.unisabana.otel.company.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class OperationConfig {
    private final String operation;
    private final boolean enabled;
    private final String spanName;
    private final boolean recordExceptions;
    private final boolean captureResult;
    private final Map<String, String> attributes;

    OperationConfig(String operation, boolean enabled, String spanName, boolean recordExceptions,
            boolean captureResult, Map<String, String> attributes) {
        this.operation = operation;
        this.enabled = enabled;
        this.spanName = spanName == null || spanName.isBlank() ? operation : spanName;
        this.recordExceptions = recordExceptions;
        this.captureResult = captureResult;
        this.attributes = Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    }

    public String operation() {
        return operation;
    }

    public boolean enabled() {
        return enabled;
    }

    public String spanName() {
        return spanName;
    }

    public boolean recordExceptions() {
        return recordExceptions;
    }

    public boolean captureResult() {
        return captureResult;
    }

    public Map<String, String> attributes() {
        return attributes;
    }
}
