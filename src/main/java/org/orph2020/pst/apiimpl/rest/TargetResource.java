package org.orph2020.pst.apiimpl.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.prop.CelestialTarget;
import org.ivoa.dm.proposal.prop.Target;
import org.ivoa.dm.stc.coords.Epoch;
import org.ivoa.dm.stc.coords.EquatorialPoint;
import org.ivoa.dm.ivoa.RealQuantity;
import org.orph2020.pst.common.json.ObjectIdentifier;

import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Produces(MediaType.APPLICATION_JSON)
@Path("targets")
@Tag(name = "proposal-tool-targets")
public class TargetResource extends ObjectResourceBase {

    @GET
    @Operation(summary = "get all the targets in the database")
    public List<ObjectIdentifier> getTargets() {
        return super.getObjects("SELECT o._id,o.sourceName FROM Target o ORDER BY o.sourceName");
    }

    @GET
    @Path("{id}")
    @Operation(summary = "get the specified Target")
    public Target getTarget(@PathParam("id") Long id)
        throws WebApplicationException
    {
        return super.findObject(Target.class, id);
    }

    //requires subtype in the json input
    @POST
    @Operation(summary = "create a new Target in the database")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response createTarget(Target target)
        throws WebApplicationException
    {
        return super.persistObject(target);
    }

    @DELETE
    @Path("{id}")
    @Operation(summary = "delete the Target specified by the 'id' from the database")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response deleteTarget(@PathParam("id") Long id)
        throws WebApplicationException
    {
        return super.removeObject(Target.class, id);
    }

    @PUT
    @Path("{id}/sourceName")
    @Operation(summary = "replace the sourceName of the specified Target")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceSourceName(@PathParam("id") Long id, String replacementSourceName)
        throws WebApplicationException
    {
        Target target = super.findObject(Target.class, id);

        target.setSourceName(replacementSourceName);

        return responseWrapper(target, 201);
    }
}
