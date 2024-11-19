package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 16/03/2023 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.ivoa.Ivoid;
import org.ivoa.dm.proposal.management.Backend;
import org.ivoa.dm.proposal.management.Observatory;
import org.ivoa.dm.proposal.management.TelescopeArray;
import org.ivoa.dm.proposal.prop.WikiDataId;
import org.jboss.resteasy.reactive.RestQuery;
import org.orph2020.pst.common.json.ObjectIdentifier;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Produces(MediaType.APPLICATION_JSON)
@Path("observatories")
@Tag(name = "observatories")
public class ObservatoryResource extends ObjectResourceBase {

    @GET
    @Operation(summary = "get all of the Observatories, optionally provide a name to find the specific Observatory")
    @APIResponse(
            responseCode = "200"
    )
    public List<ObjectIdentifier> getObservatories(@RestQuery String name){
        if (name == null) {
            return getObjectIdentifiers("SELECT o._id,o.name FROM Observatory o ORDER BY o.name");
        } else {
            return getObjectIdentifiers("SELECT o._id,o.name FROM Observatory o WHERE o.name like '" +name+ "'ORDER BY o.name");
        }
    }

    @GET
    @Path("{id}")
    @Operation(summary = "get the specified Observatory")
    public Observatory getObservatory(@PathParam("id") Long id)
            throws WebApplicationException
    {
        return findObject(Observatory.class, id);
    }

    @POST
    @Operation(summary = "create a new Observatory in the database")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Observatory createObservatory(Observatory observatory)
            throws WebApplicationException
    {
        return persistObject(observatory);
    }


    @DELETE
    @Path("{id}")
    @Operation(summary = "delete the Observatory specified by the 'id' from the database")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response deleteObservatory(@PathParam("id") Long id)
            throws WebApplicationException
    {
        return removeObject(Observatory.class, id);
    }

    @PUT
    @Operation(summary = "update an Observatory name")
    @Path("{id}/name")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateObservatoryName(@PathParam("id") Long id, String replacementName )
            throws WebApplicationException
    {
        Observatory observatory = findObject(Observatory.class, id);

        observatory.setName(replacementName);

        return responseWrapper(observatory, 201);
    }

    @PUT
    @Operation(summary = "update an Observatory's address")
    @Path("{id}/address")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateAddress(@PathParam("id") Long id, String replacementAddress )
            throws WebApplicationException
    {
        Observatory observatory = findObject(Observatory.class, id);

        observatory.setAddress(replacementAddress);

        return responseWrapper(observatory, 201);
    }

    @PUT
    @Operation(summary = "update an Observatory's ivoId")
    @Path("{id}/ivoId")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateObservatoryIvoId(@PathParam("id") Long id, String replacementIvoId )
            throws WebApplicationException
    {
        Observatory observatory = findObject(Observatory.class, id);

        observatory.setIvoid(new Ivoid(replacementIvoId));

        return responseWrapper(observatory, 201);
    }

    @PUT
    @Operation(summary = "update an Observatory's wikiId")
    @Path("{id}/wikiId")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateObservatoryWikiId(@PathParam("id") Long id, String replacementWikiId )
            throws WebApplicationException
    {
        Observatory observatory = findObject(Observatory.class, id);

        observatory.setWikiId(new WikiDataId(replacementWikiId));

        return responseWrapper(observatory, 201);
    }


    //BACKEND **************************************************************************************

    @GET
    @Path("{id}/backend")
    @Operation(summary = "get all the Backends associated with the Observatory specified by the 'id'")
    public List<Backend> getObservatoryBackends(@PathParam("id") Long id)
        throws WebApplicationException
    {
        return findObject(Observatory.class, id).getBackends();
    }

    @GET
    @Path("{id}/backend/{subId}")
    @Operation(summary = "get the specific Backend associated with the Observatory")
    public Backend getObservatoryBackend(@PathParam("id") Long id, @PathParam("subId") Long subId)
        throws WebApplicationException
    {
        Backend backend =
                (Backend) findObjectInList(subId, findObject(Observatory.class, id).getBackends());
        if (backend == null) {
            throw new WebApplicationException(String.format(NON_ASSOCIATE_ID, "Backend", subId, "Observatory", id),
                    422);
        }

        return backend;
    }

    @PUT
    @Operation(summary = "add an Observatory backend")
    @Path("{id}/backend")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response addBackend(@PathParam("id") Long id, Long backendId)
            throws WebApplicationException
    {
        Observatory observatory = findObject(Observatory.class, id);

        Backend backend = findObject(Backend.class, backendId);

        observatory.addToBackends(backend);

        return responseWrapper(observatory, 201);
    }

    @POST
    @Operation(summary = "create a Backend in the database and add it to the Observatory specified by the 'id'")
    @Path("{id}/backend")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Backend createAndAddBackend(@PathParam("id") Long observatoryId, Backend backend)
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
    @Path("{observatoryId}/backend/{backendId}/name")
    @Operation(summary = "replace the name of the Backend specified by the 'subId'")
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
    @Path("{id}/backend/{subId}/parallel")
    @Operation(summary = "update the 'parallel' status (true/false) of the Backend specified by the 'subId'")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateBackendParallel(@PathParam("id") Long id, @PathParam("subId") Long subId,
                                       Boolean updateParallel) throws WebApplicationException
    {
        Observatory observatory = findObject(Observatory.class, id);

        Backend backend = (Backend) findObjectInList(subId, observatory.getBackends());

        if (backend == null) {
            throw new WebApplicationException(String.format(NON_ASSOCIATE_ID, "Backend", subId, "Observatory", id),
                    422);
        }

        backend.setParallel(updateParallel);

        return mergeObject(observatory);
    }

    //TELESCOPE ARRAY *****************************************************************************

    @PUT
    @Operation(summary = "add an existing TelescopeArray to the Observatory")
    @Path("{id}/array")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response addArray(@PathParam("id") Long id, Long telescopeArrayId )
            throws WebApplicationException
    {
        Observatory observatory = findObject(Observatory.class, id);

        TelescopeArray array = findObject(TelescopeArray.class, telescopeArrayId);

        observatory.addToArrays(array);

        return responseWrapper(observatory, 201);
    }

    @POST
    @Operation(summary = "create a TelescopeArray in the database and add it to the Observatory specified by the 'id'")
    @Path("{id}/array")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public TelescopeArray createAndAddArray(@PathParam("id") Long observatoryId, TelescopeArray telescopeArray)
            throws WebApplicationException
    {
        Observatory observatory = findObject(Observatory.class, observatoryId);

        for (TelescopeArray t : observatory.getArrays()) {
            if (t.equals(telescopeArray)) {
                throw new WebApplicationException("TelescopeArray already added to Observatory", 400);
            }
        }

        return addNewChildObject(observatory, telescopeArray, observatory::addToArrays);
    }

}
