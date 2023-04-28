package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 16/03/2023 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.ivoa.RealQuantity;
import org.ivoa.dm.proposal.prop.*;
import org.ivoa.dm.ivoa.Ivorn;
import org.jboss.resteasy.reactive.RestQuery;
import org.orph2020.pst.common.json.ObjectIdentifier;

import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
            return super.getObjects("SELECT o._id,o.name FROM Observatory o ORDER BY o.name");
        } else {
            return super.getObjects("SELECT o._id,o.name FROM Observatory o WHERE o.name like '" +name+ "'ORDER BY o.name");
        }
    }

    @GET
    @Path("{id}")
    @Operation(summary = "get the specified Observatory")
    public Observatory getObservatory(@PathParam("id") Long id)
            throws WebApplicationException
    {
        return super.findObject(Observatory.class, id);
    }

    @POST
    @Operation(summary = "create a new Observatory in the database")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response createObservatory(Observatory observatory)
            throws WebApplicationException
    {
        return super.persistObject(observatory);
    }


    @DELETE
    @Path("{id}")
    @Operation(summary = "delete the Observatory specified by the 'id' from the database")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response deleteObservatory(@PathParam("id") Long id)
            throws WebApplicationException
    {
        return super.removeObject(Observatory.class, id);
    }

    @PUT
    @Operation(summary = "update an Observatory name")
    @Path("{id}/name")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateObservatoryName(@PathParam("id") Long id, String replacementName )
            throws WebApplicationException
    {
        Observatory observatory = super.findObject(Observatory.class, id);

        observatory.setName(replacementName);

        return super.responseWrapper(observatory, 201);
    }

    @PUT
    @Operation(summary = "update an Observatory's address")
    @Path("{id}/address")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateAddress(@PathParam("id") Long id, String replacementAddress )
            throws WebApplicationException
    {
        Observatory observatory = super.findObject(Observatory.class, id);

        observatory.setAddress(replacementAddress);

        return super.responseWrapper(observatory, 201);
    }

    @PUT
    @Operation(summary = "update an Observatory's ivoId")
    @Path("{id}/ivoId")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateObservatoryIvoId(@PathParam("id") Long id, String replacementIvoId )
            throws WebApplicationException
    {
        Observatory observatory = super.findObject(Observatory.class, id);

        observatory.setIvoid(new Ivorn(replacementIvoId));

        return super.responseWrapper(observatory, 201);
    }

    @PUT
    @Operation(summary = "update an Observatory's wikiId")
    @Path("{id}/wikiId")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateObservatoryWikiId(@PathParam("id") Long id, String replacementWikiId )
            throws WebApplicationException
    {
        Observatory observatory = super.findObject(Observatory.class, id);

        observatory.setWikiId(new WikiDataId(replacementWikiId));

        return super.responseWrapper(observatory, 201);
    }

    //TELESCOPE **********************************************************************************

    @PUT
    @Operation(summary = "add an existing Telescope to the Observatory specified by the 'id'")
    @Path("{id}/telescope")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response addTelescope(@PathParam("id") Long id, Long telescopeId )
            throws WebApplicationException
    {
        Observatory observatory = super.findObject(Observatory.class, id);

        Telescope telescope = super.findObject(Telescope.class, telescopeId);

        observatory.addToTelescopes(telescope);

        return super.responseWrapper(observatory, 201);
    }

    @POST
    @Operation(summary = "create a Telescope in the database and add it to the Observatory specified by the 'id'")
    @Path("{id}/telescope")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response createAndAddTelescope(@PathParam("id") Long observatoryId, Telescope telescope)
        throws WebApplicationException
    {
        Observatory observatory = super.findObject(Observatory.class, observatoryId);

        for (Telescope t : observatory.getTelescopes()) {
            if (t.equals(telescope)) {
                throw new WebApplicationException("Telescope already added to Observatory", 400);
            }
        }

        observatory.addToTelescopes(telescope);

        return super.mergeObject(observatory);
    }

    @PUT
    @Path("{id}/telescope/{subId}/name")
    @Operation(summary = "replace the name of the Telescope specified by the 'subId'")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceTelescopeName(@PathParam("id") Long id, @PathParam("subId") Long subId,
                                         String replacementName) throws WebApplicationException
    {
        Observatory observatory = super.findObject(Observatory.class, id);

        Telescope telescope = (Telescope) super.findObjectInList(subId, observatory.getTelescopes());

        if (telescope == null) {
            throw new WebApplicationException(String.format(NON_ASSOCIATE_ID, "Telescope", subId, "Observatory", id),
                    422);
        }

        telescope.setName(replacementName);

        return super.mergeObject(observatory);
    }

    @PUT
    @Path("{id}/telescope/{subId}/location/xyz")
    @Operation(summary = "replace the xyz coordinates of the Telescope specified by the 'subId'")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceTelescopeXYZ(@PathParam("id") Long id, @PathParam("subId") Long subId,
                                         List<RealQuantity> xyz) throws WebApplicationException
    {
        Observatory observatory = super.findObject(Observatory.class, id);

        Telescope telescope = (Telescope) super.findObjectInList(subId, observatory.getTelescopes());

        if (telescope == null) {
            throw new WebApplicationException(String.format(NON_ASSOCIATE_ID, "Telescope", subId, "Observatory", id),
                    422);
        }

        telescope.getLocation().setX(xyz.get(0));
        telescope.getLocation().setY(xyz.get(1));
        telescope.getLocation().setZ(xyz.get(2));

        return super.mergeObject(observatory);
    }

    @PUT
    @Path("{id}/telescope/{subId}/location/x")
    @Operation(summary = "replace the x coordinate of the Telescope specified by the 'subId'")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceTelescopeX(@PathParam("id") Long id, @PathParam("subId") Long subId,
                                        RealQuantity updateX) throws WebApplicationException
    {
        Observatory observatory = super.findObject(Observatory.class, id);

        Telescope telescope = (Telescope) super.findObjectInList(subId, observatory.getTelescopes());

        if (telescope == null) {
            throw new WebApplicationException(String.format(NON_ASSOCIATE_ID, "Telescope", subId, "Observatory", id),
                    422);
        }

        telescope.getLocation().setX(updateX);

        return super.mergeObject(observatory);
    }

    @PUT
    @Path("{id}/telescope/{subId}/location/y")
    @Operation(summary = "replace the y coordinate of the Telescope specified by the 'subId'")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceTelescopeY(@PathParam("id") Long id, @PathParam("subId") Long subId,
                                        RealQuantity updateY) throws WebApplicationException
    {
        Observatory observatory = super.findObject(Observatory.class, id);

        Telescope telescope = (Telescope) super.findObjectInList(subId, observatory.getTelescopes());

        if (telescope == null) {
            throw new WebApplicationException(String.format(NON_ASSOCIATE_ID, "Telescope", subId, "Observatory", id),
                    422);
        }

        telescope.getLocation().setY(updateY);

        return super.mergeObject(observatory);
    }

    @PUT
    @Path("{id}/telescope/{subId}/location/z")
    @Operation(summary = "replace the z coordinate of the Telescope specified by the 'subId'")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceTelescopeZ(@PathParam("id") Long id, @PathParam("subId") Long subId,
                                        RealQuantity updateZ) throws WebApplicationException
    {
        Observatory observatory = super.findObject(Observatory.class, id);

        Telescope telescope = (Telescope) super.findObjectInList(subId, observatory.getTelescopes());

        if (telescope == null) {
            throw new WebApplicationException(String.format(NON_ASSOCIATE_ID, "Telescope", subId, "Observatory", id),
                    422);
        }

        telescope.getLocation().setZ(updateZ);

        return super.mergeObject(observatory);
    }

    //INSTRUMENT *************************************************************************************

    @PUT
    @Operation(summary = "add an existing Instrument to the Observatory specified by the 'id'")
    @Path("{id}/instrument")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response addInstrument(@PathParam("id") Long id, Long instrumentId )
            throws WebApplicationException
    {
        Observatory observatory = super.findObject(Observatory.class, id);

        Instrument instrument = super.findObject(Instrument.class, instrumentId);

        observatory.addToInstruments(instrument);

        return super.responseWrapper(observatory, 201);
    }

    @POST
    @Operation(summary = "create an Instrument in the database and add it to the Observatory specified by the 'id'")
    @Path("{id}/instrument")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response createAndAddInstrument(@PathParam("id") Long observatoryId, Instrument instrument)
            throws WebApplicationException
    {
        Observatory observatory = super.findObject(Observatory.class, observatoryId);

        for (Instrument i : observatory.getInstruments()) {
            if (i.equals(instrument)) {
                throw new WebApplicationException("Instrument already added to Observatory", 400);
            }
        }

        observatory.addToInstruments(instrument);

        return super.mergeObject(observatory);
    }

    @PUT
    @Path("{id}/instrument/{subId}/name")
    @Operation(summary = "replace the name of the Instrument specified by the 'subId'")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceInstrumentName(@PathParam("id") Long id, @PathParam("subId") Long subId,
                                         String replacementName) throws WebApplicationException
    {
        Observatory observatory = super.findObject(Observatory.class, id);

        Instrument instrument = (Instrument) super.findObjectInList(subId, observatory.getInstruments());

        if (instrument == null) {
            throw new WebApplicationException(String.format(NON_ASSOCIATE_ID, "Instrument", subId, "Observatory", id),
                    422);
        }

        instrument.setName(replacementName);

        return super.mergeObject(observatory);
    }

    @PUT
    @Path("{id}/instrument/{subId}/description")
    @Operation(summary = "replace the description of the Instrument specified by the 'subId'")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceInstrumentDescription(@PathParam("id") Long id, @PathParam("subId") Long subId,
                                          String replacementDescription) throws WebApplicationException
    {
        Observatory observatory = super.findObject(Observatory.class, id);

        Instrument instrument = (Instrument) super.findObjectInList(subId, observatory.getInstruments());

        if (instrument == null) {
            throw new WebApplicationException(String.format(NON_ASSOCIATE_ID, "Instrument", subId, "Observatory", id),
                    422);
        }

        instrument.setDescription(replacementDescription);

        return super.mergeObject(observatory);
    }

    @PUT
    @Path("{id}/instrument/{subId}/wikiId")
    @Operation(summary = "replace the wikiId of the Instrument specified by the 'subId'")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceInstrumentWikiId(@PathParam("id") Long id, @PathParam("subId") Long subId,
                                                 String replacementWikiId) throws WebApplicationException
    {
        Observatory observatory = super.findObject(Observatory.class, id);

        Instrument instrument = (Instrument) super.findObjectInList(subId, observatory.getInstruments());

        if (instrument == null) {
            throw new WebApplicationException(String.format(NON_ASSOCIATE_ID, "Instrument", subId, "Observatory", id),
                    422);
        }

        instrument.setWikiId(new WikiDataId(replacementWikiId));

        return super.mergeObject(observatory);
    }

    @PUT
    @Path("{id}/instrument/{subId}/reference")
    @Operation(summary = "replace the reference (external URL) of the Instrument specified by the 'subId'")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceInstrumentReference(@PathParam("id") Long id, @PathParam("subId") Long subId,
                                            String replacementReference) throws WebApplicationException
    {
        Observatory observatory = super.findObject(Observatory.class, id);

        Instrument instrument = (Instrument) super.findObjectInList(subId, observatory.getInstruments());

        if (instrument == null) {
            throw new WebApplicationException(String.format(NON_ASSOCIATE_ID, "Instrument", subId, "Observatory", id),
                    422);
        }

        instrument.setReference(replacementReference);

        return super.mergeObject(observatory);
    }

    @PUT
    @Path("{id}/instrument/{subId}/kind")
    @Operation(summary = "replace the 'kind' of the Instrument specified by the 'subId'; one-of CONTINUUM, SPECTROSCOPIC")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceInstrumentKind(@PathParam("id") Long id, @PathParam("subId") Long subId,
                                               String replacementKind) throws WebApplicationException
    {
        Observatory observatory = super.findObject(Observatory.class, id);

        Instrument instrument = (Instrument) super.findObjectInList(subId, observatory.getInstruments());

        if (instrument == null) {
            throw new WebApplicationException(String.format(NON_ASSOCIATE_ID, "Instrument", subId, "Observatory", id),
                    422);
        }

        try {
            instrument.setKind(InstrumentKind.fromValue(replacementKind));
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(e.getMessage(), 400);
        }

        return super.mergeObject(observatory);
    }

    @PUT
    @Path("{id}/instrument/{subId}/frequencyCoverage")
    @Operation(summary = "replace the frequencyCoverage of the Instrument specified by the 'subId'")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceInstrumentFrequencyCoverage(@PathParam("id") Long id, @PathParam("subId") Long subId,
                                          SpectralWindowSetup replacementFrequencyCoverage)
            throws WebApplicationException
    {
        Observatory observatory = super.findObject(Observatory.class, id);

        Instrument instrument = (Instrument) super.findObjectInList(subId, observatory.getInstruments());

        if (instrument == null) {
            throw new WebApplicationException(String.format(NON_ASSOCIATE_ID, "Instrument", subId, "Observatory", id),
                    422);
        }

        instrument.setFrequencyCoverage(replacementFrequencyCoverage);

        return super.mergeObject(observatory);
    }



    //BACKEND **************************************************************************************

    @GET
    @Path("{id}/backend")
    @Operation(summary = "get all the Backends associated with the Observatory specified by the 'id'")
    public List<Backend> getObservatoryBackends(@PathParam("id") Long id)
        throws WebApplicationException
    {
        return super.findObject(Observatory.class, id).getBackends();
    }

    @GET
    @Path("{id}/backend/{subId}")
    @Operation(summary = "get the specific Backend associated with the Observatory")
    public Backend getObservatoryBackend(@PathParam("id") Long id, @PathParam("subId") Long subId)
        throws WebApplicationException
    {
        Backend backend =
                (Backend) findObjectInList(subId, super.findObject(Observatory.class, id).getBackends());
        if (backend == null) {
            throw new WebApplicationException(String.format(NON_ASSOCIATE_ID, "Backend", subId, "Observatory", id),
                    422);
        }

        return backend;
    }

    @PUT
    @Operation(summary = "add an Observatory backend")
    @Path("{id}/backend")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response addBackend(@PathParam("id") Long id, Long backendId)
            throws WebApplicationException
    {
        Observatory observatory = super.findObject(Observatory.class, id);

        Backend backend = super.findObject(Backend.class, backendId);

        observatory.addToBackends(backend);

        return super.responseWrapper(observatory, 201);
    }

    @POST
    @Operation(summary = "create a Backend in the database and add it to the Observatory specified by the 'id'")
    @Path("{id}/backend")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response createAndAddBackend(@PathParam("id") Long observatoryId, Backend backend)
            throws WebApplicationException
    {
        Observatory observatory = super.findObject(Observatory.class, observatoryId);

        for (Backend b : observatory.getBackends()) {
            if (b.equals(backend)) {
                throw new WebApplicationException("Backend already added to Observatory", 400);
            }
        }

        observatory.addToBackends(backend);

        return super.mergeObject(observatory);
    }

    @PUT
    @Path("{id}/backend/{subId}/name")
    @Operation(summary = "replace the name of the Backend specified by the 'subId'")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceBackendName(@PathParam("id") Long id, @PathParam("subId") Long subId,
                                          String replacementName) throws WebApplicationException
    {
        Observatory observatory = super.findObject(Observatory.class, id);

        Backend backend = (Backend) super.findObjectInList(subId, observatory.getBackends());

        if (backend == null) {
            throw new WebApplicationException(String.format(NON_ASSOCIATE_ID, "Backend", subId, "Observatory", id),
                    422);
        }

        backend.setName(replacementName);

        return super.mergeObject(observatory);
    }

    @PUT
    @Path("{id}/backend/{subId}/parallel")
    @Operation(summary = "update the 'parallel' status (true/false) of the Backend specified by the 'subId'")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateBackendParallel(@PathParam("id") Long id, @PathParam("subId") Long subId,
                                       Boolean updateParallel) throws WebApplicationException
    {
        Observatory observatory = super.findObject(Observatory.class, id);

        Backend backend = (Backend) super.findObjectInList(subId, observatory.getBackends());

        if (backend == null) {
            throw new WebApplicationException(String.format(NON_ASSOCIATE_ID, "Backend", subId, "Observatory", id),
                    422);
        }

        backend.setParallel(updateParallel);

        return super.mergeObject(observatory);
    }

    //TELESCOPE ARRAY *****************************************************************************

    @PUT
    @Operation(summary = "add an existing TelescopeArray to the Observatory")
    @Path("{id}/array")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response addArray(@PathParam("id") Long id, Long telescopeArrayId )
            throws WebApplicationException
    {
        Observatory observatory = super.findObject(Observatory.class, id);

        TelescopeArray array = super.findObject(TelescopeArray.class, telescopeArrayId);

        observatory.addToArrays(array);

        return super.responseWrapper(observatory, 201);
    }

    @POST
    @Operation(summary = "create a TelescopeArray in the database and add it to the Observatory specified by the 'id'")
    @Path("{id}/array")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response createAndAddArray(@PathParam("id") Long observatoryId, TelescopeArray telescopeArray)
            throws WebApplicationException
    {
        Observatory observatory = super.findObject(Observatory.class, observatoryId);

        for (TelescopeArray t : observatory.getArrays()) {
            if (t.equals(telescopeArray)) {
                throw new WebApplicationException("TelescopeArray already added to Observatory", 400);
            }
        }

        observatory.addToArrays(telescopeArray);

        return super.mergeObject(observatory);
    }

}
