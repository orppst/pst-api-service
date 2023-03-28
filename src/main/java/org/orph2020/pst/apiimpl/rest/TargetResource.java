package org.orph2020.pst.apiimpl.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.prop.CelestialTarget;
import org.ivoa.dm.proposal.prop.Target;
import org.ivoa.dm.stc.coords.Epoch;
import org.ivoa.dm.stc.coords.EquatorialPoint;
import org.ivoa.vodml.stdtypes2.RealQuantity;
import org.orph2020.pst.common.json.ObjectIdentifier;

import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Produces(MediaType.APPLICATION_JSON)
@Path("targets")
@Tag(name = "proposal-tool")
public class TargetResource extends ObjectResourceBase {

    private static final String ERR_NOT_CELESTIAL = "Target %d is not a CelestialTarget";

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

    @PUT
    @Path("{id}/sourceCoordinates")
    @Operation(summary = "replace the sourceCoordinates of the specified CelestialTarget")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceSourceCoordinates(@PathParam("id") Long id, EquatorialPoint equatorialPoint )
            throws WebApplicationException
    {
        Target target = super.findObject(Target.class, id);

        if (!target.getClass().equals(CelestialTarget.class)) {
            throw new WebApplicationException(String.format(ERR_NOT_CELESTIAL, id), 422);
        }

        ((CelestialTarget) target).setSourceCoordinates(equatorialPoint);

        return responseWrapper(target, 201);
    }

    @PUT
    @Path("{id}/positionEpoch")
    @Operation(summary = "replace the positionEpoch of the specified CelestialTarget")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replacePositionEpoch(@PathParam("id") Long id, Epoch replacementPositionEpoch)
        throws WebApplicationException
    {
        Target target = super.findObject(Target.class, id);

        if (!target.getClass().equals(CelestialTarget.class)) {
            throw new WebApplicationException(String.format(ERR_NOT_CELESTIAL, id), 422);
        }

        ((CelestialTarget) target).setPositionEpoch(replacementPositionEpoch);

        return responseWrapper(target, 201);
    }

    @PUT
    @Path("{id}/pmRA")
    @Operation(summary = "replace the pmRA (Right Ascension) of the specified CelestialTarget")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replacePmRA(@PathParam("id") Long id, RealQuantity replacementPmRA )
            throws WebApplicationException
    {
        Target target = super.findObject(Target.class, id);

        if (!target.getClass().equals(CelestialTarget.class)) {
            throw new WebApplicationException(String.format(ERR_NOT_CELESTIAL, id), 422);
        }

        ((CelestialTarget) target).setPmRA(replacementPmRA);

        return responseWrapper(target, 201);
    }

    @PUT
    @Path("{id}/pmDec")
    @Operation(summary = "replace the pmDec (Declination) of the specified CelestialTarget")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replacePmDec(@PathParam("id") Long id, RealQuantity replacementPmDec )
            throws WebApplicationException
    {
        Target target = super.findObject(Target.class, id);

        if (!target.getClass().equals(CelestialTarget.class)) {
            throw new WebApplicationException(String.format(ERR_NOT_CELESTIAL, id), 422);
        }

        ((CelestialTarget) target).setPmDec(replacementPmDec);

        return responseWrapper(target, 201);
    }

    @PUT
    @Path("{id}/parallax")
    @Operation(summary = "replace the parallax of the specified CelestialTarget")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceParallax(@PathParam("id") Long id, RealQuantity replacementParallax )
            throws WebApplicationException
    {
        Target target = super.findObject(Target.class, id);

        if (!target.getClass().equals(CelestialTarget.class)) {
            throw new WebApplicationException(String.format(ERR_NOT_CELESTIAL, id), 422);
        }

        ((CelestialTarget) target).setParallax(replacementParallax);

        return responseWrapper(target, 201);
    }

    @PUT
    @Path("{id}/sourceVelocity")
    @Operation(summary = "replace the sourceVelocity of the specified CelestialTarget")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceSourceVelocity(@PathParam("id") Long id, RealQuantity replacementSourceVelocity )
            throws WebApplicationException
    {
        Target target = super.findObject(Target.class, id);

        if (!target.getClass().equals(CelestialTarget.class)) {
            throw new WebApplicationException(String.format(ERR_NOT_CELESTIAL, id), 422);
        }

        ((CelestialTarget) target).setSourceVelocity(replacementSourceVelocity);

        return responseWrapper(target, 201);
    }

}
