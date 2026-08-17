package edu.unisabana.otel.company.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public final class CompanyOtelConfig {
    public static final String DEFAULT_RESOURCE = "company-otel.properties";
    private static final int DEFAULT_MAX_ATTRIBUTE_LENGTH = 256;
    private static final String DEFAULT_DENYLIST = "password,secret,token,authorization,cvv,pan,creditCard,accessToken,refreshToken";

    private final Properties properties;
    private final int maxAttributeLength;
    private final Set<String> redactedFields;

    private CompanyOtelConfig(Properties properties) {
        this.properties = properties;
        this.maxAttributeLength = parsePositiveInt(
                properties.getProperty("company.otel.max-attribute-length"),
                DEFAULT_MAX_ATTRIBUTE_LENGTH);
        this.redactedFields = parseDenylist(properties.getProperty("company.otel.redacted-fields", DEFAULT_DENYLIST));
    }

    public static CompanyOtelConfig loadDefault() {
        return load(Thread.currentThread().getContextClassLoader(), DEFAULT_RESOURCE);
    }

    public static CompanyOtelConfig load(ClassLoader classLoader, String resourceName) {
        Properties loaded = new Properties();
        ClassLoader loader = classLoader == null ? CompanyOtelConfig.class.getClassLoader() : classLoader;
        try (InputStream input = loader.getResourceAsStream(resourceName)) {
            if (input != null) {
                loaded.load(input);
            }
        } catch (IOException ignored) {
            return new CompanyOtelConfig(new Properties());
        }
        return new CompanyOtelConfig(loaded);
    }

    public static CompanyOtelConfig from(Properties properties) {
        Properties copy = new Properties();
        copy.putAll(properties);
        return new CompanyOtelConfig(copy);
    }

    public OperationConfig operation(String operation) {
        String prefix = operation + ".";
        String attrPrefix = prefix + "attr.";
        Map<String, String> attributes = new LinkedHashMap<>();
        properties.stringPropertyNames().stream()
                .filter(name -> name.startsWith(attrPrefix))
                .sorted()
                .forEach(name -> attributes.put(name.substring(attrPrefix.length()), properties.getProperty(name)));

        return new OperationConfig(
                operation,
                Boolean.parseBoolean(properties.getProperty(prefix + "enabled", "true")),
                properties.getProperty(prefix + "span.name", operation),
                Boolean.parseBoolean(properties.getProperty(prefix + "record.exceptions", "true")),
                Boolean.parseBoolean(properties.getProperty(prefix + "capture.result", "true")),
                attributes);
    }

    public int maxAttributeLength() {
        return maxAttributeLength;
    }

    public boolean isRedacted(String attributeName, String expression) {
        Set<String> tokens = Arrays.stream((attributeName + " " + expression).split("[^A-Za-z0-9]+"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        return redactedFields.stream().anyMatch(tokens::contains);
    }

    private static int parsePositiveInt(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            int value = Integer.parseInt(raw);
            return value > 0 ? value : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static Set<String> parseDenylist(String raw) {
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(TreeSet::new));
    }
}
