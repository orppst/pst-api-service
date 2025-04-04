package org.orph2020.pst.apiimpl.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnit;
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
import org.orph2020.pst.common.json.OpticalTelescopeDataId;
import org.orph2020.pst.common.json.OpticalTelescopeDataLoad;
import org.orph2020.pst.common.json.OpticalTelescopeDataSave;

import java.util.HashMap;

/**
 * Contains the calls to the optical telescope data.
 */
@Path("opticalTelescopes")
@Tag(name = "proposalCycles-opticalTelescopes")
@Produces(MediaType.APPLICATION_JSON)
public class OpticalTelescopeResource extends ObjectResourceBase {

    @Inject
    XmlReaderService xmlReader;

    @PersistenceUnit(unitName = "optical")
    EntityManager opticalEntityManager;

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
        OpticalTelescopeDataSave oldSave = opticalEntityManager.find(
            OpticalTelescopeDataSave.class, choices.getPrimaryKey());

        // handle old and new saves
        if(oldSave == null) {
            opticalEntityManager.persist(choices);
        } else {
            opticalEntityManager.merge(choices);
        }
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
        OpticalTelescopeDataId id = new OpticalTelescopeDataId(
            data.getProposalID(), data.getObservationID());
        OpticalTelescopeDataSave savedData =
            opticalEntityManager.find(OpticalTelescopeDataSave.class, id);

        // if no stored data. return a empty response.
        if (savedData == null) {
            return responseWrapper(new HashMap<>(), 201);
        }

        // build the response.
        try {
            // given the mess of ui code. better to fix it here into the format
            // the ui expects now.
            HashMap<String, HashMap<String, HashMap<String, String>>> dataFormat =
                new HashMap<>();
            dataFormat.put(savedData.getTelescopeName(), new HashMap<>());
            dataFormat.get(savedData.getTelescopeName()).put(
                savedData.getInstrumentName(), new HashMap<>());
            dataFormat.get(savedData.getTelescopeName()).get(
                savedData.getInstrumentName()).putAll(savedData.getChoices());

            return Response.ok(
                new ObjectMapper().writeValueAsString(dataFormat)).build();
        } catch (JsonProcessingException e) {
            return responseWrapper(e.getMessage(), 500);
        }
    }

    @POST
    @Path("deleteObs")
    @Operation(summary = "delete the optical telescope data for a given " +
            "observation")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response deleteObservationOpticalTelescopeData(
            OpticalTelescopeDataLoad data) {
        OpticalTelescopeDataId id = new OpticalTelescopeDataId(
                data.getProposalID(), data.getObservationID());
        try {
            OpticalTelescopeDataSave entity = opticalEntityManager.find(
                    OpticalTelescopeDataSave.class, id);

            // try to successfully delete
            if (entity != null) {
                opticalEntityManager.remove(entity);
                return responseWrapper(true, 201);
            } else {
                // Record not found
                return responseWrapper("Record not found", 404);
            }
        } catch (Exception e) {
            // Internal server error
            return responseWrapper(e.getMessage(), 500);
        }
    }

    @POST
    @Path("deleteProposal")
    @Operation(summary = "delete all telescope data associated with a given" +
            " proposal")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response deleteProposal(OpticalTelescopeDataLoad data) {
        String proposalID = data.getProposalID();
        try {
            // delete the entries. utilise cascading to delete the rest.
            opticalEntityManager.createQuery(
                "DELETE FROM OpticalTelescopeDataSave o WHERE " +
                    "o.primaryKey.proposalID = :proposalId")
                .setParameter("proposalId", proposalID)
                .executeUpdate();
            return responseWrapper(true, 201);
        } catch (Exception e) {
            // Internal server error
            return responseWrapper(e.getMessage(), 500);
        }
    }

    @POST
    @Path("hasEntry")
    @Operation(summary = "check if an entry exists for the optical database" +
            " for a given observation and proposal.")
    public Response hasEntry(OpticalTelescopeDataLoad data) {
        OpticalTelescopeDataId id = new OpticalTelescopeDataId(
                data.getProposalID(), data.getObservationID());
        try {
            // locate entry if exists
            OpticalTelescopeDataSave entity = opticalEntityManager.find(
                    OpticalTelescopeDataSave.class, id);
            return responseWrapper(entity != null, 201);
        } catch (Exception e) {
            // Internal server error
            return responseWrapper(e.getMessage(), 500);
        }
    }
}
