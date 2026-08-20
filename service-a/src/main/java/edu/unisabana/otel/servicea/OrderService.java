package edu.unisabana.otel.servicea;

import edu.unisabana.otel.company.core.OtelCompanyTrace;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class OrderService {
    private static final Logger LOG = Logger.getLogger(OrderService.class);

    @Inject
    @RestClient
    ProductClient productClient;

    @Inject
    MeterRegistry meterRegistry;

    @OtelCompanyTrace(operation = "otel.company.order.process")
    public OrderResponse processOrder(String orderId) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String productId = productIdFor(orderId);
        try {
            ProductResponse product = productClient.getProduct(productId);
            meterRegistry.counter("orders_processed_total", "status", "success").increment();
            LOG.infov("processed order order_id={0} product_id={1}", orderId, productId);
            return new OrderResponse(orderId, "accepted", product);
        } catch (RuntimeException ex) {
            meterRegistry.counter("orders_processed_total", "status", "error").increment();
            LOG.errorv(ex, "failed order order_id={0} product_id={1}", orderId, productId);
            throw ex;
        } finally {
            sample.stop(meterRegistry.timer("order_processing_duration_seconds"));
        }
    }

    private String productIdFor(String orderId) {
        int suffix = Math.abs(orderId.hashCode() % 3) + 1;
        return "product-" + suffix;
    }
}
