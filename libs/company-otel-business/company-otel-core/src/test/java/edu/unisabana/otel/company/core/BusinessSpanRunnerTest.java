package edu.unisabana.otel.company.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.lang.reflect.Method;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BusinessSpanRunnerTest {
    private InMemorySpanExporter exporter;
    private SdkTracerProvider tracerProvider;

    @BeforeEach
    void setUp() {
        exporter = InMemorySpanExporter.create();
        tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .build();
    }

    @AfterEach
    void tearDown() {
        tracerProvider.close();
    }

    @Test
    void createsBusinessSpanWithConfiguredAttributes() throws Throwable {
        Properties properties = new Properties();
        properties.setProperty("otel.company.payment.create.enabled", "true");
        properties.setProperty("otel.company.payment.create.span.name", "create payment");
        properties.setProperty("otel.company.payment.create.attr.company.payment.id", "#result.id");
        properties.setProperty("otel.company.payment.create.attr.company.payment.status", "#result.status");
        properties.setProperty("otel.company.payment.create.attr.company.customer.id", "#request.customerId");

        BusinessSpanRunner runner = runner(properties);
        Method method = PaymentService.class.getMethod("createPayment", PaymentRequest.class);
        OtelCompanyTrace annotation = method.getAnnotation(OtelCompanyTrace.class);

        Object result = runner.run(annotation, method, new Object[] { new PaymentRequest("C-1") },
                new String[] { "request" }, () -> new Payment("P-1", "APPROVED"));

        assertEquals(new Payment("P-1", "APPROVED"), result);
        var span = exporter.getFinishedSpanItems().get(0);
        assertEquals("create payment", span.getName());
        assertEquals("otel.company.business", span.getInstrumentationScopeInfo().getName());
        assertEquals("P-1", span.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("company.payment.id")));
        assertEquals("APPROVED", span.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("company.payment.status")));
        assertEquals("C-1", span.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("company.customer.id")));
    }

    @Test
    void disabledOperationDoesNotCreateSpan() throws Throwable {
        Properties properties = new Properties();
        properties.setProperty("otel.company.payment.create.enabled", "false");

        Method method = PaymentService.class.getMethod("createPayment", PaymentRequest.class);
        Object result = runner(properties).run(method.getAnnotation(OtelCompanyTrace.class), method,
                new Object[] { new PaymentRequest("C-1") }, new String[] { "request" },
                () -> new Payment("P-1", "APPROVED"));

        assertEquals(new Payment("P-1", "APPROVED"), result);
        assertEquals(0, exporter.getFinishedSpanItems().size());
    }

    @Test
    void recordsExceptionAndRedactsSensitiveAttribute() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("otel.company.payment.create.span.name", "create payment");
        properties.setProperty("otel.company.payment.create.attr.company.payment.token", "#request.token");
        properties.setProperty("otel.company.payment.create.attr.company.error.message", "#exception.message");

        Method method = PaymentService.class.getMethod("createPayment", PaymentRequest.class);
        RuntimeException error = assertThrows(RuntimeException.class, () -> runner(properties).run(
                method.getAnnotation(OtelCompanyTrace.class),
                method,
                new Object[] { new PaymentRequest("SECRET") },
                new String[] { "request" },
                () -> {
                    throw new IllegalStateException("declined");
                }));

        assertEquals("declined", error.getMessage());
        var span = exporter.getFinishedSpanItems().get(0);
        assertEquals("ERROR", span.getStatus().getStatusCode().name());
        assertEquals("[REDACTED]", span.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("company.payment.token")));
        assertEquals("declined", span.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("company.error.message")));
        assertEquals(1, span.getEvents().size());
    }

    private BusinessSpanRunner runner(Properties properties) {
        OpenTelemetrySdk sdk = OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build();
        return new BusinessSpanRunner(
                sdk.getTracer(BusinessSpanRunner.INSTRUMENTATION_SCOPE, BusinessSpanRunner.LIBRARY_VERSION),
                CompanyOtelConfig.from(properties),
                new SimpleAttributeResolver());
    }

    public record PaymentRequest(String customerId) {
        public String token() {
            return customerId;
        }
    }

    public record Payment(String id, String status) {
    }

    public static final class PaymentService {
        @OtelCompanyTrace(operation = "otel.company.payment.create")
        public Payment createPayment(PaymentRequest request) {
            return new Payment("P-1", "APPROVED");
        }
    }
}
