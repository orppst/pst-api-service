package org.orph2020.pst.apiimpl.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.orph2020.pst.apiimpl.entities.opticalTelescopeService.Instrument;
import org.orph2020.pst.apiimpl.entities.opticalTelescopeService.XmlReaderService;
import org.orph2020.pst.common.json.OpticalTelescopeData;

import java.util.ArrayList;
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
    public List<String> getOpticalTelescopeNames() {
        return xmlReader.getTelescopes().keySet().stream().toList();
    }

    /**
     * get the params needed to be filled out by the specific telescope.
     */
    @GET
    @Path("telescope")
    @Operation(summary = "returns the set of instruments and their options.")
    public List<Instrument> getOpticalTelescopeInstruments(
            String telescopeName) {
        if (!xmlReader.getTelescopes().containsKey(telescopeName)) {
            return new ArrayList<>();
        }
        return xmlReader.getTelescopes().get(telescopeName).
            getInstruments().values().stream().toList();
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
    @Path("save")
    @Operation(summary = "saves the telescope specific data")
    public boolean saveOpticalTelescopeData( OpticalTelescopeData choices) {
        return true;
    }

}
