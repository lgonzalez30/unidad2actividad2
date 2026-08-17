package edu.unisabana.otel.serviceb;

import edu.unisabana.otel.company.core.OtelCompanyTrace;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;

@ApplicationScoped
public class ProductService {
    private static final Logger LOG = Logger.getLogger(ProductService.class);

    @Inject
    DynamoDbClient dynamoDbClient;

    @Inject
    MeterRegistry meterRegistry;

    @ConfigProperty(name = "products.table.name", defaultValue = "otel-lab-products")
    String tableName;

    @OtelCompanyTrace(operation = "otel.company.product.lookup")
    public Optional<ProductResponse> lookupProduct(String productId) {
        TraceMdc.putCurrentSpan();
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            Map<String, AttributeValue> item = dynamoDbClient.getItem(GetItemRequest.builder()
                    .tableName(tableName)
                    .key(Map.of("product_id", AttributeValue.fromS(productId)))
                    .consistentRead(true)
                    .build()).item();

            if (item == null || item.isEmpty()) {
                meterRegistry.counter("product_lookup_total", "status", "not_found").increment();
                LOG.warnv("product not found product_id={0}", productId);
                return Optional.empty();
            }

            meterRegistry.counter("product_lookup_total", "status", "success").increment();
            LOG.infov("product found product_id={0}", productId);
            return Optional.of(new ProductResponse(
                    item.get("product_id").s(),
                    item.get("name").s(),
                    new BigDecimal(item.get("price").n())));
        } catch (RuntimeException ex) {
            meterRegistry.counter("product_lookup_total", "status", "error").increment();
            LOG.errorv(ex, "product lookup failed product_id={0}", productId);
            throw ex;
        } finally {
            sample.stop(meterRegistry.timer("product_lookup_duration_seconds"));
        }
    }
}
