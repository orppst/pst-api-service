package org.orph2020.pst.apiimpl.rest;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.prop.Backend;
import org.orph2020.pst.common.json.ObjectIdentifier;

import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("backends")
@Tag(name="proposal-tool-backends")
@Produces(MediaType.APPLICATION_JSON)
public class BackendResource extends ObjectResourceBase {

    @GET
    @Operation(summary = "get all the Backends in the database")
    public List<ObjectIdentifier> getBackends() {
        return super.getObjects("SELECT o._id,o.name FROM Backend o ORDER BY o.name");
    }

    @GET
    @Path("{id}")
    @Operation(summary = "get the Backend specified by the 'id'")
    public Backend getBackend(@PathParam("id") Long id)
            throws WebApplicationException
    {
        return super.findObject(Backend.class, id);
    }

    @PUT
    @Path("{id}/name")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    @Operation(summary = "replace the name of the Backend specified by the 'id'")
    public Response replaceName(@PathParam("id") Long id, String replacementName)
        throws WebApplicationException
    {
        Backend backend = super.findObject(Backend.class, id);

        backend.setName(replacementName);

        return responseWrapper(backend, 201);
    }

    @PUT
    @Path("{id}/parallel")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    @Operation(summary = "update the 'parallel' status (true/false) of the Backend specified by the 'id'")
    public Response updateParallel(@PathParam("id") Long id, Boolean updateParallel)
            throws WebApplicationException
    {
        Backend backend = super.findObject(Backend.class, id);

        backend.setParallel(updateParallel);

        return responseWrapper(backend, 201);
    }

    //Creation/deletion of Backends is done via the ObservatoryResource interface
}
