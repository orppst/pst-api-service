package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 16/03/2023 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.prop.*;
import org.ivoa.vodml.stdtypes2.Ivorn;
import org.orph2020.pst.common.json.ObjectIdentifier;

import javax.inject.Inject;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Produces(MediaType.APPLICATION_JSON)
@Path("observatories")
@Tag(name = "proposal-tool")
public class ObservatoryResource extends ObjectResourceBase {

    @GET
    @Operation(summary = "Get all of the Observatories")
    @APIResponse(
            responseCode = "200"
    )
    public List<ObjectIdentifier> getObservatories(){
        return super.getObjects("SELECT o._id,o.name FROM Observatory o ORDER BY o.name");
    }

    @GET
    @Path("{id}")
    @Operation(summary = "Get the specified Observatory")
    public Observatory getObservatory(@PathParam("id") Long id)
            throws WebApplicationException
    {
        return super.findObject(Observatory.class, id);
    }

    @POST
    @Operation(summary = "create an Observatory")
    @APIResponse(
            responseCode = "201",
            description = "create a new Observatory in the database"
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response createObservatory(String jsonObservatory)
            throws WebApplicationException
    {
        return super.persistObject(jsonObservatory, Observatory.class);
    }

    @PUT
    @Operation(summary = "update an Observatory name")
    @APIResponse(
            responseCode = "200"
    )
    @Path("{id}/name")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateName(@PathParam("id") Long id, String replacementName )
            throws WebApplicationException
    {
        Observatory observatory = super.findObject(Observatory.class, id);

        observatory.setName(replacementName);

        return Response.ok().entity(String.format(OK_UPDATE, "Observatory.name")).build();
    }

    @PUT
    @Operation(summary = "update an Observatory's address")
    @APIResponse(
            responseCode = "200"
    )
    @Path("{id}/address")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateAddress(@PathParam("id") Long id, String replacementAddress )
            throws WebApplicationException
    {
        Observatory observatory = super.findObject(Observatory.class, id);

        observatory.setAddress(replacementAddress);

        return Response.ok().entity(String.format(OK_UPDATE, "Observatory.address")).build();
    }

    @PUT
    @Operation(summary = "update an Observatory's ivoId")
    @APIResponse(
            responseCode = "200"
    )
    @Path("{id}/ivoId")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateIvoId(@PathParam("id") Long id, String replacementIvoId )
            throws WebApplicationException
    {
        Observatory observatory = super.findObject(Observatory.class, id);

        observatory.setIvoid(new Ivorn(replacementIvoId));

        return Response.ok().entity(String.format(OK_UPDATE, "Observatory.ivoId")).build();
    }

    @PUT
    @Operation(summary = "update an Observatory's wikiId")
    @APIResponse(
            responseCode = "200"
    )
    @Path("{id}/wikiId")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateWikiId(@PathParam("id") Long id, String replacementWikiId )
            throws WebApplicationException
    {
        Observatory observatory = super.findObject(Observatory.class, id);

        observatory.setWikiId(new WikiDataId(replacementWikiId));

        return Response.ok().entity(String.format(OK_UPDATE, "Observatory.wikiId")).build();
    }

    @PUT
    @Operation(summary = "add an Observatory telescope")
    @APIResponse(
            responseCode = "200"
    )
    @Path("{id}/telescope")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response addTelescope(@PathParam("id") Long id, Long telescopeId )
            throws WebApplicationException
    {
        Observatory observatory = super.findObject(Observatory.class, id);

        Telescope telescope = super.findObject(Telescope.class, telescopeId);

        observatory.addTelescopes(telescope);

        return Response.ok().entity(String.format(OK_UPDATE, "Observatory.telescopes")).build();
    }

    @PUT
    @Operation(summary = "add an Observatory instrument")
    @APIResponse(
            responseCode = "200"
    )
    @Path("{id}/instrument")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response addInstrument(@PathParam("id") Long id, Long instrumentId )
            throws WebApplicationException
    {
        Observatory observatory = super.findObject(Observatory.class, id);

        Instrument instrument = super.findObject(Instrument.class, instrumentId);

        observatory.addInstruments(instrument);

        return Response.ok().entity(String.format(OK_UPDATE, "Observatory.instruments")).build();
    }

    @PUT
    @Operation(summary = "add an Observatory backend")
    @APIResponse(
            responseCode = "200"
    )
    @Path("{id}/backend")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response addBackend(@PathParam("id") Long id, Long backendId)
            throws WebApplicationException
    {
        Observatory observatory = super.findObject(Observatory.class, id);

        Backend backend = super.findObject(Backend.class, backendId);

        observatory.addBackends(backend);

        return Response.ok().entity(String.format(OK_UPDATE, "Observatory.backends")).build();
    }


    @PUT
    @Operation(summary = "add an Observatory array")
    @APIResponse(
            responseCode = "200"
    )
    @Path("{id}/array")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response addArray(@PathParam("id") Long id, Long telescopeArrayId )
            throws WebApplicationException
    {
        Observatory observatory = super.findObject(Observatory.class, id);

        TelescopeArray array = super.findObject(TelescopeArray.class, telescopeArrayId);

        observatory.addArrays(array);

        return Response.ok().entity(String.format(OK_UPDATE, "Observatory.arrays")).build();
    }

}
