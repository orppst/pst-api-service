package org.orph2020.pst.apiimpl.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.orph2020.pst.apiimpl.entities.opticalTelescopeService.Telescope;
import org.orph2020.pst.apiimpl.entities.opticalTelescopeService.XmlReaderService;
import org.orph2020.pst.common.json.OpticalTelescopeDataLoad;
import org.orph2020.pst.common.json.OpticalTelescopeDataSave;
import org.orph2020.pst.common.json.OpticalTelescopeNames;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
    @Operation(summary = "get a list of the optical telescopes available.")
    public OpticalTelescopeNames getOpticalTelescopeNames() {
        return new OpticalTelescopeNames(
            xmlReader.getTelescopes().keySet().stream().toList(),
            xmlReader.getTelescopes().keySet().size());
    }

    /**
     * get the telescopes. Contains all the instruments and options.
     */
    @GET
    @Path("telescopes")
    @Operation(summary = "returns the set of instruments and their options.")
    public List<Telescope> getOpticalTelescopes(
            String telescopeName) {
        if (!xmlReader.getTelescopes().containsKey(telescopeName)) {
            return new ArrayList<>();
        }
        return xmlReader.getTelescopes().values().stream().toList();
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
    public boolean saveOpticalTelescopeData( OpticalTelescopeDataSave choices) {
        return true;
    }

    /**
     * save new states into the database for a telescope data.
     *
     * @param choices the set of choices made by the user. contains the
     *                proposal, observation and telescope ids.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ResponseStatus(value = 201)
    @Path("load")
    @Operation(summary = "load the telescope specific data for a given " +
        "observation")
    public OpticalTelescopeDataSave loadOpticalTelescopeData(
        OpticalTelescopeDataLoad data
    ) {
        // empty return until we figure out how to save this data and extract it.
        return new OpticalTelescopeDataSave(
            data.proposalID, data.observationID, "", new HashMap<>());
    }
}
