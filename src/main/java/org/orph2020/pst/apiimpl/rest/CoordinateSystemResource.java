package org.orph2020.pst.apiimpl.rest;

import io.quarkus.runtime.annotations.ConfigRoot;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.stc.coords.CoordSys;
import org.orph2020.pst.common.json.ObjectIdentifier;

import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Produces(MediaType.APPLICATION_JSON)
@Path("coordinateSystems")
@Tag(name = "proposal-tool-coordinateSystem")
public class CoordinateSystemResource extends ObjectResourceBase{

    @GET
    @Operation(summary = "get all the CoordinateSystems stored in the database")
    public List<ObjectIdentifier> getCoordinateSystems() {
        return super.getObjects("SELECT o._id,o.class FROM CoordSys o ORDER BY o._id");
    }

    @GET
    @Path("{id}")
    @Operation(summary = "get the CoordinateSystem specified by the 'id'")
    public CoordSys getCoordinateSystem(@PathParam("id") Long id)
        throws WebApplicationException
    {
        return super.findObject(CoordSys.class, id);
    }


    //json string of CoordSys type will require subtype hints
    @POST
    @Operation(summary = "create a new CoordinateSystem in the database")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response createCoordinateSystem(CoordSys coordinateSystem)
        throws WebApplicationException
    {
        return super.persistObject(coordinateSystem);
    }

    @DELETE
    @Path("{id}")
    @Operation(summary = "delete the CoordinateSystem specified by 'id' from the database")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response deleteCoordinateSystem(@PathParam("id") Long id)
        throws WebApplicationException
    {
        return super.removeObject(CoordSys.class, id);
    }
}
