package org.orph2020.pst.apiimpl.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.orph2020.pst.apiimpl.entities.opticalTelescopeService.XmlReaderService;
import org.orph2020.pst.common.json.OpticalTelescopeDataLoad;
import org.orph2020.pst.common.json.OpticalTelescopeDataSave;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains the calls to the optical telescope data.
 */
@Path("opticalTelescopes")
@Tag(name = "proposalCycles-opticalTelescopes")
@Produces(MediaType.APPLICATION_JSON)
public class OpticalTelescopeResource extends ObjectResourceBase {

    @Inject
    XmlReaderService xmlReader;

    /**
     * return the list of names for the available telescopes.
     * @return the list of the available telescopes.
     */
    @GET
    @Path("names")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "get a list of the optical telescopes available.")
    public Response getOpticalTelescopeNames() {
        return responseWrapper(
            xmlReader.getTelescopes().keySet().stream().toList(), 201);
    }

    /**
     * get the telescopes. Contains all the instruments and options.
     */
    @GET
    @Path("telescopes")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "returns the set of instruments and their options.")
    public Response getOpticalTelescopes() {
        return responseWrapper(xmlReader.getTelescopes(), 201);
    }

    /**
     * save new states into the database for a telescope data.
     *
     * @param choices the set of choices made by the user. contains the
     *                proposal, observation and telescope ids.
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @ResponseStatus(value = 201)
    @Path("save")
    @Operation(summary = "saves the telescope specific data")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response saveOpticalTelescopeData(OpticalTelescopeDataSave choices) {
        return responseWrapper(true, 201);
    }

    /**
     * save new states into the database for a telescope data.
     *
     * @param data: the proposal, and observation id to extract the loaded
     *           data for.
     */
    @POST
    @ResponseStatus(value = 201)
    @Path("load")
    @Operation(summary = "load the telescope specific data for a given " +
        "observation")
    public Response loadOpticalTelescopeData(
        OpticalTelescopeDataLoad data
    ) {
        // empty return until we figure out how to save this data and extract it.
        HashMap<String, Map<String, Map<String, String>>> outputData =
            new HashMap<>();
        outputData.put("SALT", new HashMap<>());
        outputData.get("SALT").put("Salticam", new HashMap<>());
        try {
            return Response.ok(
                new ObjectMapper().writeValueAsString(outputData)).build();
        } catch (JsonProcessingException e) {
            return responseWrapper(e.getMessage(), 500);
        }
    }
}
