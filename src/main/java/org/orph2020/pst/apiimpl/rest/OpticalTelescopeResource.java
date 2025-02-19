package org.orph2020.pst.apiimpl.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.orph2020.pst.apiimpl.entities.opticalTelescopeService.Instrument;
import org.orph2020.pst.apiimpl.entities.opticalTelescopeService.XmlReaderService;

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
    @Operation(summary = "get a list of the optical telescopes available.")
    public List<String> getOpticalTelescopeNames() {
        return xmlReader.getTelescopes().keySet().stream().toList();
    }

    /**
     * get the params needed to be filled out by the specific telescope.
     */
    @GET
    @Operation(summary = "returns the set of instruments and their options.")
    public List<Instrument> getOpticalTelescopeInstruments(
            String telescopeName) {
        return xmlReader.getTelescopes().get(telescopeName).
            getInstruments().values().stream().toList();
    }

    /**
     * save new states into the database for a telescope data.
     *
     * @param proposalID: the proposal id
     * @param observationID the observation id
     * @param telescopeName the telescope name
     * @param choices: the set of choices made by the user.
     */
    @PUT
    @Operation(summary = "saves the telescope specific data")
    public boolean saveOpticalTelescopeData(
            String proposalID, String observationID, String telescopeName,
            HashMap<String, String> choices) {
        return true;
    }

}
