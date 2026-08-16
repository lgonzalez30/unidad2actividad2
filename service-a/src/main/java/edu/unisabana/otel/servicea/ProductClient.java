package edu.unisabana.otel.servicea;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "service-b")
@Path("/products")
@Produces(MediaType.APPLICATION_JSON)
public interface ProductClient {
    @GET
    @Path("/{id}")
    ProductResponse getProduct(@PathParam("id") String productId);
}
