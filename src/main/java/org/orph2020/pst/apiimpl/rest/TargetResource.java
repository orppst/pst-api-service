package org.orph2020.pst.apiimpl.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
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
    @APIResponse(
            responseCode = "200"
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response createTarget(String jsonTarget)
        throws WebApplicationException
    {
        return super.persistObject(jsonTarget, Target.class);
    }

    @PUT
    @Path("{id}/sourceName")
    @Operation(summary = "replace the sourceName of the specified Target")
    @APIResponse(
            responseCode = "201"
    )
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceSourceName(@PathParam("id") Long id, String replacementSourceName)
        throws WebApplicationException
    {
        Target target = super.findObject(Target.class, id);

        target.setSourceName(replacementSourceName);

        return Response.ok().entity(String.format(OK_UPDATE, "Target.sourceName")).build();
    }

    @PUT
    @Path("{id}/sourceCoordinates")
    @Operation(summary = "replace the sourceCoordinates of the specified CelestialTarget")
    @APIResponse(
            responseCode = "200"
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceSourceCoordinates(@PathParam("id") Long id, String jsonSourceCoordinates )
            throws WebApplicationException
    {
        Target target = super.findObject(Target.class, id);

        if (!target.getClass().equals(CelestialTarget.class)) {
            throw new WebApplicationException(String.format(ERR_NOT_CELESTIAL, id), 422);
        }

        EquatorialPoint equatorialPoint;
        try {
            equatorialPoint= mapper.readValue(jsonSourceCoordinates, EquatorialPoint.class);
        } catch (JsonProcessingException e) {
            throw new WebApplicationException(String.format(ERR_JSON_INPUT, "sourceCoordinate"), 422);
        }

        ((CelestialTarget) target).setSourceCoordinates(equatorialPoint);

        return Response.ok().entity(String.format(OK_UPDATE, "CelestialTarget.sourceCoordinates")).build();
    }

    @PUT
    @Path("{id}/positionEpoch")
    @Operation(summary = "replace the positionEpoch of the specified CelestialTarget")
    @APIResponse(
            responseCode = "200"
    )
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replacePositionEpoch(@PathParam("id") Long id, String replacementPositionEpoch )
        throws WebApplicationException
    {
        Target target = super.findObject(Target.class, id);

        if (!target.getClass().equals(CelestialTarget.class)) {
            throw new WebApplicationException(String.format(ERR_NOT_CELESTIAL, id), 422);
        }

        ((CelestialTarget) target).setPositionEpoch(new Epoch(replacementPositionEpoch));

        return Response.ok().entity(String.format(OK_UPDATE, "CelestialTarget.positionEpoch")).build();
    }

    @PUT
    @Path("{id}/pmRA")
    @Operation(summary = "replace the pmRA (Right Ascension) of the specified CelestialTarget")
    @APIResponse(
            responseCode = "200"
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replacePmRA(@PathParam("id") Long id, String jsonPmRA )
            throws WebApplicationException
    {
        Target target = super.findObject(Target.class, id);

        if (!target.getClass().equals(CelestialTarget.class)) {
            throw new WebApplicationException(String.format(ERR_NOT_CELESTIAL, id), 422);
        }

        RealQuantity replacementPmRA;
        try {
            replacementPmRA = mapper.readValue(jsonPmRA, RealQuantity.class);
        } catch (JsonProcessingException e) {
            throw new WebApplicationException(String.format(ERR_JSON_INPUT, "pmRA"), 422);
        }

        ((CelestialTarget) target).setPmRA(replacementPmRA);

        return Response.ok().entity(String.format(OK_UPDATE, "CelestialTarget.pmRA")).build();
    }

    @PUT
    @Path("{id}/pmDec")
    @Operation(summary = "replace the pmDec (Declination) of the specified CelestialTarget")
    @APIResponse(
            responseCode = "200"
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replacePmDec(@PathParam("id") Long id, String jsonPmDec )
            throws WebApplicationException
    {
        Target target = super.findObject(Target.class, id);

        if (!target.getClass().equals(CelestialTarget.class)) {
            throw new WebApplicationException(String.format(ERR_NOT_CELESTIAL, id), 422);
        }

        RealQuantity replacementPmDec;
        try {
            replacementPmDec = mapper.readValue(jsonPmDec, RealQuantity.class);
        } catch (JsonProcessingException e) {
            throw new WebApplicationException(String.format(ERR_JSON_INPUT, "pmDec"), 422);
        }

        ((CelestialTarget) target).setPmDec(replacementPmDec);

        return Response.ok().entity(String.format(OK_UPDATE, "CelestialTarget.pmDec")).build();
    }

    @PUT
    @Path("{id}/parallax")
    @Operation(summary = "replace the parallax of the specified CelestialTarget")
    @APIResponse(
            responseCode = "200"
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceParallax(@PathParam("id") Long id, String jsonParallax )
            throws WebApplicationException
    {
        Target target = super.findObject(Target.class, id);

        if (!target.getClass().equals(CelestialTarget.class)) {
            throw new WebApplicationException(String.format(ERR_NOT_CELESTIAL, id), 422);
        }

        RealQuantity replacementParallax;
        try {
            replacementParallax = mapper.readValue(jsonParallax, RealQuantity.class);
        } catch (JsonProcessingException e) {
            throw new WebApplicationException(String.format(ERR_JSON_INPUT, "parallax"), 422);
        }

        ((CelestialTarget) target).setParallax(replacementParallax);

        return Response.ok().entity(String.format(OK_UPDATE, "CelestialTarget.parallax")).build();
    }

    @PUT
    @Path("{id}/sourceVelocity")
    @Operation(summary = "replace the sourceVelocity of the specified CelestialTarget")
    @APIResponse(
            responseCode = "200"
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceSourceVelocity(@PathParam("id") Long id, String jsonSourceVelocity )
            throws WebApplicationException
    {
        Target target = super.findObject(Target.class, id);

        if (!target.getClass().equals(CelestialTarget.class)) {
            throw new WebApplicationException(String.format(ERR_NOT_CELESTIAL, id), 422);
        }

        RealQuantity replacementSourceVelocity;
        try {
            replacementSourceVelocity = mapper.readValue(jsonSourceVelocity, RealQuantity.class);
        } catch (JsonProcessingException e) {
            throw new WebApplicationException(String.format(ERR_JSON_INPUT, "sourceVelocity"), 422);
        }

        ((CelestialTarget) target).setSourceVelocity(replacementSourceVelocity);

        return Response.ok().entity(String.format(OK_UPDATE, "CelestialTarget.sourceVelocity")).build();
    }

}
