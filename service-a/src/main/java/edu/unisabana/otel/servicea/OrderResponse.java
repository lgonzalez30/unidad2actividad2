package edu.unisabana.otel.servicea;

public record OrderResponse(String orderId, String status, ProductResponse product) {
}
