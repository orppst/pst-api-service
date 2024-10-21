package org.orph2020.pst.apiimpl.rest;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.management.Observatory;
import org.ivoa.dm.proposal.management.Telescope;
import org.ivoa.dm.proposal.prop.WikiDataId;
import org.ivoa.dm.stc.coords.CoordSys;
import org.ivoa.dm.stc.coords.RealCartesianPoint;
import org.ivoa.dm.ivoa.RealQuantity;
import org.jboss.resteasy.reactive.RestQuery;
import org.orph2020.pst.common.json.ObjectIdentifier;

import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Produces(MediaType.APPLICATION_JSON)
@Path("observatories/{observatoryId}/telescopes")
@Tag(name = "observatory-telescopes")
public class TelescopeResource extends ObjectResourceBase{

    private Telescope findTelescopeInList(List<Telescope> telescopes, long telescopeId) {
        return (Telescope) findObjectInList(telescopeId, telescopes);
    }

    private Telescope findTelescopeByQuery(long observatoryId, long telescopeId) {
        TypedQuery<Telescope> q = em.createQuery(
                "Select t From Observatory o join o.telescopes t where o._id = :oid and  t._id = :tid",
                Telescope.class
        );
        q.setParameter("oid", observatoryId);
        q.setParameter("tid", telescopeId);
        return q.getSingleResult();
    }

    @GET
    @Operation(summary = "get all Telescope identifiers associated with the given Observatory, optionally provide a name to find a specific Telescope")
    public List<ObjectIdentifier> getObservatoryTelescopes( @PathParam("observatoryId") Long observatoryId,
                                                            @RestQuery String name)
    {
        if (name == null) {
            return getObjectIdentifiers("SELECT t._id,t.name FROM Observatory o Inner Join o.telescopes t WHERE o._id = "+observatoryId+" ORDER BY t.name");
        } else {
            return getObjectIdentifiers("SELECT t._id,t.name FROM Observatory o Inner Join o.telescopes t WHERE o._id = "+observatoryId+" and t.name like '"+name+"' ORDER BY t.name");
        }

    }

    @GET
    @Path("{telescopeId}")
    @Operation(summary = "get the Telescope specified by the 'telescopeId' associated with the given Observatory")
    public Telescope getTelescope(@PathParam("observatoryId") Long observatoryId,
                                  @PathParam("telescopeId") Long telescopeId)
            throws WebApplicationException
    {
        try {
            Telescope telescope = findTelescopeByQuery(observatoryId, telescopeId);
            return telescope;
        } catch (NoResultException e) {
            String ERR_NOT_FOUND = "%s with id: %d not found";
            throw new WebApplicationException(
                    String.format(ERR_NOT_FOUND, "Telescope", telescopeId), 404
            );
        }
    }

    @POST
    @Operation(summary = "create a new Telescope and add it to the given Observatory")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Telescope createAndAddTelescopeToObservatory(@PathParam("observatoryId") Long observatoryId,
                                                       Telescope telescope)
        throws WebApplicationException
    {
        Observatory observatory = findObject(Observatory.class, observatoryId);

        for (Telescope t : observatory.getTelescopes()) {
            if (t.equals(telescope)) {
                throw new WebApplicationException("Telescope already added to Observatory", 400);
            }
        }

        return addNewChildObject(observatory, telescope, observatory::addToTelescopes);
    }


    @DELETE
    @Path("{telescopeId}")
    @Operation(summary = "remove the Telescope specified by the 'telescopeId' from the given Observatory, also removes Telescope entity from the database")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response removeTelescopeFromObservatory(@PathParam("observatoryId") Long observatoryId,
                                    @PathParam("telescopeId") Long telescopeId)
        throws WebApplicationException
    {
        Observatory observatory = findObject(Observatory.class, observatoryId);

        Telescope telescope = findTelescopeInList(observatory.getTelescopes(), telescopeId);

        if (telescope == null) {
            throw new WebApplicationException(
                    String.format(NON_ASSOCIATE_ID, "Telescope", telescopeId, "Observatory", observatoryId),
                    400
            );
        }

        observatory.removeFromTelescopes(telescope);

        return removeObject(Telescope.class, telescopeId); //erase Telescope from the database
    }

    @PUT
    @Path("{telescopeId}/name")
    @Operation(summary = "update the name of the Telescope specified by 'telescopeId'")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateTelescopeName(@PathParam("observatoryId") Long observatoryId,
                                        @PathParam("telescopeId") Long telescopeId,
                                        String replacementName)
        throws WebApplicationException
    {
        Telescope telescope = findTelescopeByQuery(observatoryId, telescopeId);

        telescope.setName(replacementName);

        return responseWrapper(telescope, 201);
    }

    @PUT
    @Path("{telescopeId}/wikiId")
    @Operation(summary = "update the wikiId of the Telescope specified by 'telescopeId'")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateTelescopeWikiId(@PathParam("observatoryId") Long observatoryId,
                                          @PathParam("telescopeId") Long telescopeId,
                                          String replacementWikiId)
            throws WebApplicationException
    {
        Telescope telescope = findTelescopeByQuery(observatoryId, telescopeId);

        telescope.setWikiId(new WikiDataId(replacementWikiId));

        return responseWrapper(telescope, 201);
    }

    @PUT
    @Path("{telescopeId}/location")
    @Operation(summary = "update the location of the Telescope specified by 'telescopeId'")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateTelescopeLocation(@PathParam("observatoryId") Long observatoryId,
                                   @PathParam("telescopeId") Long telescopeId,
                                            RealCartesianPoint replacementLocation)
            throws WebApplicationException
    {
        Telescope telescope = findTelescopeByQuery(observatoryId, telescopeId);

        telescope.setLocation(replacementLocation);

        return responseWrapper(telescope, 201);
    }

    @PUT
    @Path("{telescopeId}/location/xyz")
    @Operation(summary = "update the x,y and z coordinates of the location of the Telescope specified by 'telescopeId'")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateTelescopeLocationXYZ(@PathParam("observatoryId") Long observatoryId,
                                      @PathParam("telescopeId") Long telescopeId,
                                      List<RealQuantity> xyz)
            throws WebApplicationException
    {
        Telescope telescope = findTelescopeByQuery(observatoryId, telescopeId);

        telescope.getLocation().setX(xyz.get(0));
        telescope.getLocation().setY(xyz.get(1));
        telescope.getLocation().setZ(xyz.get(2));

        return responseWrapper(telescope, 201);
    }

    @PUT
    @Path("{telescopeId}/location/x")
    @Operation(summary = "update the x coordinate of the location of the Telescope specified by 'telescopeId'")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateTelescopeLocationX(@PathParam("observatoryId") Long observatoryId,
                                    @PathParam("telescopeId") Long telescopeId,
                                    RealQuantity replacementX)
            throws WebApplicationException
    {
        Telescope telescope = findTelescopeByQuery(observatoryId, telescopeId);

        telescope.getLocation().setX(replacementX);

        return responseWrapper(telescope, 201);
    }

    @PUT
    @Path("{telescopeId}/location/y")
    @Operation(summary = "update the y coordinate of the location of the Telescope specified by 'telescopeId'")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateTelescopeLocationY(@PathParam("observatoryId") Long observatoryId,
                                    @PathParam("telescopeId") Long telescopeId,
                                    RealQuantity replacementY)
            throws WebApplicationException
    {
        Telescope telescope = findTelescopeByQuery(observatoryId, telescopeId);

        telescope.getLocation().setY(replacementY);

        return responseWrapper(telescope, 201);
    }

    @PUT
    @Path("{telescopeId}/location/z")
    @Operation(summary = "update the z coordinate of the location of the Telescope specified by 'telescopeId'")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateTelescopeLocationZ(@PathParam("observatoryId") Long observatoryId,
                                    @PathParam("telescopeId") Long telescopeId,
                                    RealQuantity replacementZ)
            throws WebApplicationException
    {
        Telescope telescope = findTelescopeByQuery(observatoryId, telescopeId);

        telescope.getLocation().setZ(replacementZ);

        return responseWrapper(telescope, 201);
    }

    @PUT
    @Path("{telescopeId}/location/coordinateSystem")
    @Operation(summary = "update the coordinate system of the location of the Telescope specified by 'telescopeId'")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateTelescopeLocationCoordinateSystem(@PathParam("observatoryId")Long observatoryId,
                                                            @PathParam("telescopeId") Long telescopeId,
                                                            Long coordinateSystemId)
            throws WebApplicationException
    {
        Telescope telescope = findTelescopeByQuery(observatoryId, telescopeId);

        CoordSys coordSys = findObject(CoordSys.class, coordinateSystemId);

        telescope.getLocation().setCoordSys(coordSys);

        return responseWrapper(telescope, 201);
    }
}
