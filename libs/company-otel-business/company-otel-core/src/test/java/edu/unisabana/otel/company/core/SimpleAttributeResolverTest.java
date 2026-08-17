package edu.unisabana.otel.company.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SimpleAttributeResolverTest {
    private final SimpleAttributeResolver resolver = new SimpleAttributeResolver();

    @Test
    void resolvesArgumentNestedProperty() throws Exception {
        Method method = Sample.class.getMethod("create", PaymentRequest.class);
        InvocationContext context = new InvocationContext(
                method,
                new Object[] { new PaymentRequest("C-1", 42.5) },
                new String[] { "request" },
                null,
                null);

        assertEquals(Optional.of("C-1"), resolver.resolve("#request.customerId", context));
        assertEquals(Optional.of(42.5), resolver.resolve("#request.amount", context));
    }

    @Test
    void resolvesResultRecordProperty() throws Exception {
        Method method = Sample.class.getMethod("create", PaymentRequest.class);
        InvocationContext context = new InvocationContext(
                method,
                new Object[] { new PaymentRequest("C-1", 42.5) },
                new String[] { "request" },
                new Payment("P-1", "APPROVED"),
                null);

        assertEquals(Optional.of("P-1"), resolver.resolve("#result.id", context));
        assertEquals(Optional.of("APPROVED"), resolver.resolve("#result.status", context));
    }

    @Test
    void missingPropertyIsEmpty() throws Exception {
        Method method = Sample.class.getMethod("create", PaymentRequest.class);
        InvocationContext context = new InvocationContext(method, new Object[] { new PaymentRequest("C-1", 42.5) },
                new String[] { "request" }, null, null);

        assertTrue(resolver.resolve("#request.password.value", context).isEmpty());
        assertTrue(resolver.resolve("not-dynamic", context).isPresent());
    }

    public record PaymentRequest(String customerId, double amount) {
    }

    public record Payment(String id, String status) {
    }

    public static final class Sample {
        public Payment create(PaymentRequest request) {
            return new Payment("P-1", "APPROVED");
        }
    }
}
