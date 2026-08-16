package edu.unisabana.otel.servicea;

import io.opentelemetry.api.trace.Span;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
public class OrderResource {
    private static final Logger LOG = Logger.getLogger(OrderResource.class);

    @Inject
    OrderService orderService;

    @GET
    @Path("/{id}")
    public OrderResponse getOrder(@PathParam("id") String orderId) {
        TraceMdc.putCurrentSpan();
        Span.current().setAttribute("order.id", orderId);
        try {
            LOG.infov("received order request order_id={0}", orderId);
            return orderService.processOrder(orderId);
        } finally {
            MDC.remove("trace_id");
            MDC.remove("span_id");
        }
    }
}
