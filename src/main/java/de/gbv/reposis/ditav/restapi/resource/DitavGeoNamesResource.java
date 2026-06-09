package de.gbv.reposis.ditav.restapi.resource;

import de.gbv.reposis.ditav.geonames.DitavGeoNamesService;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST resource exposing the locally cached GeoNames entries.
 * <p>
 * Deployed by {@link de.gbv.reposis.ditav.restapi.DitavJerseyAPIDeployer} and therefore available
 * at {@code /api/geonames/{geonameId}}.
 */
@Path("/")
public class DitavGeoNamesResource {

    /**
     * Returns the GeoNames JSON for the given geonameId. The entry is served from the local
     * cache; on a cache miss it is fetched from the GeoNames API and cached.
     *
     * @param geonameId the numeric geonameId, e.g. {@code 554234}
     * @return the GeoNames JSON, {@code 400} for a malformed id or {@code 404} if it cannot be resolved
     */
    @GET
    @Path("{geonameId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGeoName(@PathParam("geonameId") String geonameId) {
        if (geonameId == null || !geonameId.matches("\\d+")) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"geonameId must be numeric\"}")
                .type(MediaType.APPLICATION_JSON)
                .build();
        }
        return DitavGeoNamesService.getInstance().resolve(geonameId)
            .map(json -> Response.ok(json).build())
            .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }
}
