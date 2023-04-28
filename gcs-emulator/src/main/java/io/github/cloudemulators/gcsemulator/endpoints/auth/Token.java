package io.github.cloudemulators.gcsemulator.endpoints.auth;

import com.google.api.services.storage.model.Bucket;
import com.google.cloud.storage.StorageException;
import io.github.cloudemulators.gcsemulator.endpoints.Base;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.spi.HttpRequest;

@Path("/token")
public class Token extends Base {
    @Context
    HttpRequest request;

    @POST
    @Produces("application/json")
    public Response authenticate() {
        return Response.ok("{}").header("Content-type", "application/json").build();
    }
}
