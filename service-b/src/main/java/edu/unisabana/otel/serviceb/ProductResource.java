package edu.unisabana.otel.serviceb;

import io.opentelemetry.api.trace.Span;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

@Path("/products")
@Produces(MediaType.APPLICATION_JSON)
public class ProductResource {
    private static final Logger LOG = Logger.getLogger(ProductResource.class);

    @Inject
    ProductService productService;

    @GET
    @Path("/{id}")
    public ProductResponse getProduct(@PathParam("id") String productId) {
        TraceMdc.putCurrentSpan();
        Span.current().setAttribute("product.id", productId);
        try {
            LOG.infov("received product lookup product_id={0}", productId);
            return productService.lookupProduct(productId)
                    .orElseThrow(() -> new NotFoundException("product not found: " + productId));
        } finally {
            MDC.remove("trace_id");
            MDC.remove("span_id");
        }
    }
}
