package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 16/03/2023 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.prop.Observatory;
import org.orph2020.pst.common.json.ObjectIdentifier;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Produces(MediaType.APPLICATION_JSON)
@Path("observatories")
@Tag(name = "proposal-tool")
public class ObservatoryResource extends ObjectResourceBase {

    @Inject
    ObjectMapper mapper;

    @GET
    @Operation(summary = "Get all of the Observatories")
    @APIResponse(
            responseCode = "200"
    )
    public List<ObjectIdentifier> getObservatories(){
        return super.getObjects("SELECT o._id,o.name FROM Observatory o ORDER BY o.name");
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get the specified Observatory")
    public Observatory getObservatory(@PathParam("id") Long id) {
        Observatory result = em.find(Observatory.class, id);
        if (result == null)
        {
            throw new WebApplicationException(String.format("Observatory with id %d not found", id), 404);
        }
        return result;
    }


}
