package edu.unisabana.otel.serviceb;

import java.math.BigDecimal;

public record ProductResponse(String productId, String name, BigDecimal price) {
}
