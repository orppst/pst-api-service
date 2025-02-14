package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 16/03/2023 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.ivoa.Ivoid;
import org.ivoa.dm.proposal.management.*;
import org.ivoa.dm.proposal.prop.WikiDataId;
import org.jboss.resteasy.reactive.RestQuery;
import org.orph2020.pst.common.json.ObjectIdentifier;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
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

    //TELESCOPE ARRAY *****************************************************************************

    @GET
    @Path("{observatoryId}/array")
    @Operation(summary = "get a list of all TelescopeArrays belonging to the given Observatory")
    public List<ObjectIdentifier> getTelescopeArrays(@PathParam("observatoryId") Long observatoryId)
        throws WebApplicationException
    {
        return getObjectIdentifiers("select a._id,a.name from Observatory o inner join o.arrays a where o._id = "+observatoryId+" order by a.name");
    }

    @GET
    @Path("{observatoryId}/array/{arrayId}")
    @Operation(summary = "get the TelescopeArray specified from the given Observatory")
    public TelescopeArray getTelescopeArray(@PathParam("observatoryId") Long observatoryId,
                                            @PathParam("arrayId") Long arrayId)
        throws WebApplicationException
    {
        /*
            Please note that a single Telescope as an ObservingPlatform is returned as a
            TelescopeArray with a single TelescopeArrayMember. As these are NOT SAVED in
            the DB both the TelescopeArray and TelescopeArrayMember _ids will be zero.
            The Telescope itself will retain its _id.
         */


        ObservingPlatform observingPlatform = findObject(ObservingPlatform.class, arrayId);

        if (observingPlatform.getClass() == TelescopeArray.class) {
            return (TelescopeArray) observingPlatform;
        } else if (observingPlatform.getClass() == Telescope.class) {
            return TelescopeArray.createTelescopeArray((ta) -> {
                ta.name = ((Telescope) observingPlatform).getName();
                ta.arrayMembers = new ArrayList<>(
                        List.of(new TelescopeArrayMember((Telescope) observingPlatform))
                );
            });
        } else {
            throw new WebApplicationException(
                    "Class: " + observingPlatform.getClass() + " not recognized", 500);
        }
    }



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
