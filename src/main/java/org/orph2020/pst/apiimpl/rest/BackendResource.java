package org.orph2020.pst.apiimpl.rest;


import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.management.Backend;
import org.ivoa.dm.proposal.management.Observatory;

import java.util.List;

@Produces(MediaType.APPLICATION_JSON)
@Path("observatories/{observatoryId}/backends")
@Tag(name = "observatory-backends" )
public class BackendResource extends ObjectResourceBase{

    //BACKEND **************************************************************************************

    @GET
    @Operation(summary = "get all the Backends associated with the Observatory specified by the 'observatoryId'")
    public List<Backend> getObservatoryBackends(@PathParam("observatoryId") Long observatoryId)
            throws WebApplicationException
    {
        return findObject(Observatory.class, observatoryId).getBackends();
    }

    @GET
    @Path("{backendId}")
    @Operation(summary = "get the specific Backend associated with the Observatory")
    public Backend getObservatoryBackend(
            @PathParam("observatoryId") Long observatoryId,
            @PathParam("backendId") Long backendId
    )
            throws WebApplicationException
    {
        Backend backend =
                (Backend) findObjectInList(backendId, findObject(Observatory.class, observatoryId)
                        .getBackends());
        if (backend == null) {
            throw new WebApplicationException(
                    String.format(NON_ASSOCIATE_ID, "Backend", backendId, "Observatory", observatoryId),
                    422);
        }

        return backend;
    }

    @PUT
    @Operation(summary = "add an Observatory backend")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response addBackend(@PathParam("observatoryId") Long observatoryId, Long backendId)
            throws WebApplicationException
    {
        Observatory observatory = findObject(Observatory.class, observatoryId);

        Backend backend = findObject(Backend.class, backendId);

        observatory.addToBackends(backend);

        return responseWrapper(observatory, 201);
    }

    @POST
    @Operation(summary = "create a Backend in the database and add it to the Observatory specified by the 'observatoryId'")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Backend createAndAddBackend(@PathParam("observatoryId") Long observatoryId, Backend backend)
            throws WebApplicationException
    {
        Observatory observatory = findObject(Observatory.class, observatoryId);

        for (Backend b : observatory.getBackends()) {
            if (b.equals(backend)) {
                throw new WebApplicationException("Backend already added to Observatory", 400);
            }
        }

        return addNewChildObject(observatory, backend, observatory::addToBackends);
    }

    @PUT
    @Path("{backendId}/name")
    @Operation(summary = "replace the name of the Backend specified by the 'backendId'")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceBackendName(@PathParam("observatoryId") Long id, @PathParam("backendId") Long subId,
                                       String replacementName) throws WebApplicationException
    {
        Observatory observatory = findObject(Observatory.class, id);

        Backend backend = (Backend) findObjectInList(subId, observatory.getBackends());

        if (backend == null) {
            throw new WebApplicationException(
                    String.format(NON_ASSOCIATE_ID, "Backend", subId, "Observatory", id),
                    422);
        }

        backend.setName(replacementName);

        return mergeObject(observatory);
    }

    @PUT
    @Path("{backendId}/parallel")
    @Operation(summary = "update the 'parallel' status (true/false) of the Backend specified by the 'backendId'")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateBackendParallel(
            @PathParam("observatoryId") Long observatoryId,
            @PathParam("backendId") Long backendId,
            Boolean updateParallel)
            throws WebApplicationException
    {
        Observatory observatory = findObject(Observatory.class, observatoryId);

        Backend backend = (Backend) findObjectInList(backendId, observatory.getBackends());

        if (backend == null) {
            throw new WebApplicationException(
                    String.format(NON_ASSOCIATE_ID, "Backend", backendId, "Observatory", observatoryId),
                    422);
        }

        backend.setParallel(updateParallel);

        return mergeObject(observatory);
    }



}
