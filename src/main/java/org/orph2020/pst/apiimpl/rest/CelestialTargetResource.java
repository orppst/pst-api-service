package org.orph2020.pst.apiimpl.rest;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.ivoa.RealQuantity;
import org.ivoa.dm.proposal.prop.CelestialTarget;
import org.ivoa.dm.proposal.prop.Target;
import org.ivoa.dm.stc.coords.Epoch;
import org.ivoa.dm.stc.coords.EquatorialPoint;
import org.orph2020.pst.common.json.ObjectIdentifier;

import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("celestialTargets")
@Tag(name="proposal-tool-targets-celestial")
@Produces(MediaType.APPLICATION_JSON)
public class CelestialTargetResource extends ObjectResourceBase{

    @GET
    @Operation(summary = "get all the CelestialTargets from the database")
    public List<ObjectIdentifier> getCelestialTargets() {
        return super.getObjects("SELECT o._id,o.sourceName FROM CelestialTarget o ORDER BY o.sourceName");
    }

    @GET
    @Path("{id}")
    @Operation(summary = "get the CelestialTarget specified by the 'id'")
    public CelestialTarget getCelestialTarget(@PathParam("id") Long id)
        throws WebApplicationException
    {
        return super.findObject(CelestialTarget.class, id);
    }

    @POST
    @Operation(summary = "create a new CelestialTarget in the database")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createCelestialTarget(CelestialTarget celestialTarget)
            throws WebApplicationException
    {
        return super.persistObject(celestialTarget);
    }

    @DELETE
    @Path("{id}")
    @Operation(summary = "remove the CelestialTarget specified by the 'id'")
    public Response deleteCelestialTarget(@PathParam("id") Long id)
        throws WebApplicationException
    {
        return super.removeObject(CelestialTarget.class, id);
    }


    @PUT
    @Path("{id}/sourceCoordinates")
    @Operation(summary = "replace the sourceCoordinates of the specified CelestialTarget")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceSourceCoordinates(@PathParam("id") Long id, EquatorialPoint equatorialPoint )
            throws WebApplicationException
    {
        CelestialTarget target = super.findObject(CelestialTarget.class, id);

        target.setSourceCoordinates(equatorialPoint);

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
        CelestialTarget target = super.findObject(CelestialTarget.class, id);

        target.setPositionEpoch(replacementPositionEpoch);

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
        CelestialTarget target = super.findObject(CelestialTarget.class, id);

        target.setPmRA(replacementPmRA);

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
        CelestialTarget target = super.findObject(CelestialTarget.class, id);

        target.setPmDec(replacementPmDec);

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
        CelestialTarget target = super.findObject(CelestialTarget.class, id);

        target.setParallax(replacementParallax);

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
        CelestialTarget target = super.findObject(CelestialTarget.class, id);

        target.setSourceVelocity(replacementSourceVelocity);

        return responseWrapper(target, 201);
    }

    @PUT
    @Path("{id}/sourceName")
    @Operation(summary = "replace the sourceName of the specified CelestialTarget")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceSourceName(@PathParam("id") Long id, String replacementSourceName)
            throws WebApplicationException
    {
        CelestialTarget target = super.findObject(CelestialTarget.class, id);

        target.setSourceName(replacementSourceName);

        return responseWrapper(target, 201);
    }
}
