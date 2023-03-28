package org.orph2020.pst.apiimpl.rest;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.prop.Telescope;
import org.ivoa.dm.proposal.prop.WikiDataId;
import org.ivoa.dm.stc.coords.CoordSys;
import org.ivoa.dm.stc.coords.GeocentricPoint;
import org.ivoa.vodml.stdtypes2.RealQuantity;
import org.orph2020.pst.common.json.ObjectIdentifier;

import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Produces(MediaType.APPLICATION_JSON)
@Path("telescopes")
@Tag(name = "proposal-tool-telescopes")
public class TelescopeResource extends ObjectResourceBase{

    @GET
    @Operation(summary = "get all the Telescopes stored in the database")
    public List<ObjectIdentifier> getTelescopes() {
        return super.getObjects("SELECT o._id,o.name FROM Telescope o ORDER BY o.name");
    }

    @GET
    @Path("{id}")
    @Operation(summary = "get the Telescope specified by the 'id'")
    public Telescope getTelescope(@PathParam("id") Long id)
            throws WebApplicationException
    {
        return super.findObject(Telescope.class, id);
    }

    @POST
    @Operation(summary = "create a new Telescope in the database")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response createTelescope(Telescope telescope)
        throws WebApplicationException
    {
        return super.persistObject(telescope);
    }

    @DELETE
    @Path("{id}")
    @Operation(summary = "remove the Telescope specified by the 'id'")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response deleteTelescope(@PathParam("id") Long id)
        throws WebApplicationException
    {
        return super.removeObject(Telescope.class, id);
    }

    @PUT
    @Path("{id}/name")
    @Operation(summary = "update the name of the Telescope specified by 'id'")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateName(@PathParam("id") Long id, String replacementName)
        throws WebApplicationException
    {
        Telescope telescope = super.findObject(Telescope.class, id);

        telescope.setName(replacementName);

        return super.responseWrapper(telescope, 201);
    }

    @PUT
    @Path("{id}/wikiId")
    @Operation(summary = "update the wikiId of the Telescope specified by 'id'")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateWikiId(@PathParam("id") Long id, String replacementWikiId)
            throws WebApplicationException
    {
        Telescope telescope = super.findObject(Telescope.class, id);

        telescope.setWikiId(new WikiDataId(replacementWikiId));

        return super.responseWrapper(telescope, 201);
    }

    @PUT
    @Path("{id}/location")
    @Operation(summary = "update the location of the Telescope specified by 'id'")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateLocation(@PathParam("id") Long id, GeocentricPoint replacementLocation)
            throws WebApplicationException
    {
        Telescope telescope = super.findObject(Telescope.class, id);

        telescope.setLocation(replacementLocation);

        return super.responseWrapper(telescope, 201);
    }

    @PUT
    @Path("{id}/location/xyz")
    @Operation(summary = "update the x,y and z coordinates of the location of the Telescope specified by 'id'")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateLocationXYZ(@PathParam("id") Long id, List<RealQuantity> xyz)
            throws WebApplicationException
    {
        Telescope telescope = super.findObject(Telescope.class, id);

        telescope.getLocation().setX(xyz.get(0));
        telescope.getLocation().setY(xyz.get(1));
        telescope.getLocation().setZ(xyz.get(2));

        return super.responseWrapper(telescope, 201);
    }

    @PUT
    @Path("{id}/location/x")
    @Operation(summary = "update the x coordinate of the location of the Telescope specified by 'id'")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateLocationX(@PathParam("id") Long id, RealQuantity replacementX)
            throws WebApplicationException
    {
        Telescope telescope = super.findObject(Telescope.class, id);

        telescope.getLocation().setX(replacementX);

        return super.responseWrapper(telescope, 201);
    }

    @PUT
    @Path("{id}/location/y")
    @Operation(summary = "update the y coordinate of the location of the Telescope specified by 'id'")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateLocationY(@PathParam("id") Long id, RealQuantity replacementY)
            throws WebApplicationException
    {
        Telescope telescope = super.findObject(Telescope.class, id);

        telescope.getLocation().setY(replacementY);

        return super.responseWrapper(telescope, 201);
    }

    @PUT
    @Path("{id}/location/z")
    @Operation(summary = "update the z coordinate of the location of the Telescope specified by 'id'")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateLocationZ(@PathParam("id") Long id, RealQuantity replacementZ)
            throws WebApplicationException
    {
        Telescope telescope = super.findObject(Telescope.class, id);

        telescope.getLocation().setZ(replacementZ);

        return super.responseWrapper(telescope, 201);
    }

    @PUT
    @Path("{id}/location/coordinateSystem")
    @Operation(summary = "update the coordinate system of the location of the Telescope specified by 'id'")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateLocationCoordinateSystem(@PathParam("id") Long id, Long coordinateSystemId)
            throws WebApplicationException
    {
        Telescope telescope = super.findObject(Telescope.class, id);

        CoordSys coordSys = super.findObject(CoordSys.class, coordinateSystemId);

        telescope.getLocation().setCoordSys(coordSys);

        return super.responseWrapper(telescope, 201);
    }
}
