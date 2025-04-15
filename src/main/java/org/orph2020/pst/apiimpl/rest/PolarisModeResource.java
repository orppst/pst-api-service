package org.orph2020.pst.apiimpl.rest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;

import static org.orph2020.pst.AppLifecycleBean.MODES;

@Produces(MediaType.APPLICATION_JSON)
@Path("polarisMode")
@Tag(name = "polarisMode")
@ApplicationScoped
public class PolarisModeResource extends ObjectResourceBase {

    // get the mode from the application level.
    @Inject
    int mode;

    /**
     * getter rest api for the mode.
     * @return the current mode in int form.
     */
    @GET
    @Operation(summary = "get Polaris mode")
    @ResponseStatus(value = 201)
    public Response getMode() {
        return responseWrapper(mode, 201);
    }

    /**
     * sets the polaris mode (
     * mainly for debug purposes. maybe we need a password thing here?)
     *
     * @param mode: the new mode.
     * @return the response echoing what occured.
     */
    @POST
    @Operation(summary = "set polaris mode")
    @ResponseStatus(value = 201)
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response setMode(int mode)
    {
        // locate correct enum
        MODES selectedMode = null;
        for (MODES m : MODES.values()) {
            if (m.getValue() == mode) {
                selectedMode = m;
                break;
            }
        }

        // handle enum detection failure.
        if(selectedMode == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid mode value.").build();
        }

        // execute switch.
        switch (selectedMode) {
            case OPTICAL -> {
                this.mode = MODES.OPTICAL.getValue();
                return Response.ok("Mode set to OPTICAL").build();
            }
            case RADIO -> {
                this.mode = MODES.RADIO.getValue();
                return Response.ok("Mode set to RADIO").build();
            }
            case BOTH -> {
                this.mode = MODES.BOTH.getValue();
                return Response.ok("Mode set to BOTH").build();
            }
            default -> {
                return Response.status(Response.Status.BAD_REQUEST).entity(
                        "Invalid mode value.").build();
            }
        }
    }
}
