package org.orph2020.pst.apiimpl.rest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.orph2020.pst.apiimpl.entities.MODES;
import org.orph2020.pst.apiimpl.entities.polarisModeService.PolarisModeService;

@Produces(MediaType.APPLICATION_JSON)
@Path("polarisMode")
@Tag(name = "polarisMode")
@ApplicationScoped
public class PolarisModeResource {

    private final PolarisModeService polarisModeService;

    @Inject
    public PolarisModeResource(PolarisModeService polarisModeService) {
        this.polarisModeService = polarisModeService;
    }

    /**
     * getter rest api for the mode.
     * @return the current mode in int form.
     */
    @GET
    @Operation(summary = "get Polaris mode")
    @ResponseStatus(value = 201)
    public Response getMode() {
        return Response.ok(polarisModeService.getPolarisMode().getValue()).build();
    }

    /**
     * sets the polaris mode (
     * mainly for debug purposes. maybe we need a password thing here?)
     *
     * @param mode: the new mode.
     * @return the response echoing what occurred.
     */
    @POST
    @Operation(summary = "set polaris mode")
    @ResponseStatus(value = 201)
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response setMode(int mode) {
        MODES selectedMode;
        try {
            selectedMode = MODES.fromValue(mode);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid mode value: " + e.getMessage()).build();
        }

        // Use the centralized service to set the mode
        polarisModeService.setPolarisMode(selectedMode);
        return Response.ok("Mode set to " + selectedMode.getText()).build();
    }
}
