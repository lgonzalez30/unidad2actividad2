package edu.unisabana.otel.servicea;

import java.math.BigDecimal;

public record ProductResponse(String productId, String name, BigDecimal price) {
}
