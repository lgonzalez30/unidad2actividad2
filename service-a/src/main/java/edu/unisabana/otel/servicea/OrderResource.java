package edu.unisabana.otel.servicea;

import io.opentelemetry.api.trace.Span;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
public class OrderResource {
    private static final Logger LOG = Logger.getLogger(OrderResource.class);

    @Inject
    OrderService orderService;

    @GET
    @Path("/{id}")
    public OrderResponse getOrder(@PathParam("id") String orderId) {
        Span.current().setAttribute("order.id", orderId);
        LOG.infov("received order request order_id={0}", orderId);
        return orderService.processOrder(orderId);
    }
}
