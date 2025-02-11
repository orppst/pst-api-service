package org.orph2020.pst.apiimpl.rest;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.management.Instrument;
import org.ivoa.dm.proposal.management.InstrumentKind;
import org.ivoa.dm.proposal.management.Observatory;
import org.ivoa.dm.proposal.prop.WikiDataId;
import org.jboss.resteasy.reactive.RestQuery;
import org.orph2020.pst.common.json.ObjectIdentifier;

import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

/*
 semantics: Instruments are "owned" or contained by Observatories.
 */

@Path("observatories/{observatoryId}/instruments")
@Tag(name = "observatory-instruments")
@Produces(MediaType.APPLICATION_JSON)
public class InstrumentResource extends ObjectResourceBase {

    private Instrument findInstrumentInList(List<Instrument> instruments, long instrumentId ) {
        return (Instrument) findObjectInList(instrumentId, instruments);
    }

    private Instrument findInstrumentByQuery(long observatoryId, long instrumentId) {
        TypedQuery<Instrument> q = em.createQuery(
                "Select t From Observatory o join o.instruments t where o._id = :oid and  t._id = :tid",
                Instrument.class
        );
        q.setParameter("oid", observatoryId);
        q.setParameter("tid", instrumentId);
        return q.getSingleResult(); //throws NoResultException if entity not found
    }

    @GET
    @Operation(summary = "get all Instrument identifiers associated with the given Observatory, optionally provide a name to find a specific Instrument")
    public List<ObjectIdentifier> getObservatoryInstruments(@PathParam("observatoryId") Long observatoryId,
                                                            @RestQuery String name)
    {
        if (name == null) {
            return getObjectIdentifiers("SELECT i._id,i.name FROM Observatory o Inner Join o.instruments i WHERE o._id = "+observatoryId+" ORDER BY i.name");
        } else {
            return getObjectIdentifiers("SELECT i._id,i.name FROM Observatory o Inner Join o.instruments i WHERE o._id = "+observatoryId+" and i.name like '"+name+"' ORDER BY i.name");
        }
    }

    @DELETE
    @Path("/{instrumentId}")
    @Operation(summary = "remove the Instrument specified by 'instrumentId' from the given Observatory, also removes the instrument from the database")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response removeInstrumentFromObservatory(@PathParam("observatoryId") Long observatoryId,
                                                    @PathParam("instrumentId") Long instrumentId)
        throws WebApplicationException
    {
        Observatory observatory = findObject(Observatory.class, observatoryId);

        Instrument instrument = findInstrumentByQuery(observatoryId, instrumentId );

        observatory.removeFromInstruments(instrument);

        return removeObject(Instrument.class, instrumentId);
    }

    @POST
    @Operation(summary = "create an Instrument in the database and add it to the Observatory specified by the 'id'")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Instrument createAndAddInstrumentToObservatory(@PathParam("observatoryId") Long observatoryId,
                                                        Instrument instrument)
            throws WebApplicationException
    {
        Observatory observatory = findObject(Observatory.class, observatoryId);

        for (Instrument i : observatory.getInstruments()) {
            //semantically what we want but unsure how 'equals' is implemented
            if (i.equals(instrument)) {
                throw new WebApplicationException("Instrument already added to Observatory", 400);
            }
        }

        return addNewChildObject(observatory, instrument, observatory::addToInstruments);
    }

    @PUT
    @Path("{instrumentId}/name")
    @Operation(summary = "replace the name of the Instrument specified by the 'subId'")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceInstrumentName(@PathParam("observatoryId") Long observatoryId,
                                          @PathParam("instrumentId") Long instrumentId,
                                          String replacementName) throws WebApplicationException
    {
        Instrument instrument = findInstrumentByQuery(observatoryId, instrumentId);

        instrument.setName(replacementName);

        return responseWrapper(instrument, 201);
    }

    @PUT
    @Path("{instrumentId}/description")
    @Operation(summary = "replace the description of the Instrument specified by the 'instrumentId'")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceInstrumentDescription(@PathParam("observatoryId") Long observatoryId,
                                                 @PathParam("instrumentId") Long instrumentId,
                                                 String replacementDescription) throws WebApplicationException
    {

        Instrument instrument = findInstrumentByQuery(observatoryId, instrumentId);

        instrument.setDescription(replacementDescription);

        return responseWrapper(instrument, 201);
    }

    @PUT
    @Path("{instrumentId}/wikiId")
    @Operation(summary = "replace the wikiId of the Instrument specified by the 'instrumentId'")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceInstrumentWikiId(@PathParam("observatoryId") Long observatoryId,
                                            @PathParam("instrumentId") Long instrumentId,
                                            String replacementWikiId) throws WebApplicationException
    {
        Instrument instrument = findInstrumentByQuery(observatoryId, instrumentId);

        instrument.setWikiId(new WikiDataId(replacementWikiId));

        return responseWrapper(instrument, 201);
    }

    @PUT
    @Path("{instrumentId}/reference")
    @Operation(summary = "replace the reference (external URL) of the Instrument specified by the 'instrumentId'")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceInstrumentReference(@PathParam("observatoryId") Long observatoryId,
                                               @PathParam("instrumentId") Long instrumentId,
                                               String replacementReference) throws WebApplicationException
    {
        Instrument instrument = findInstrumentByQuery(observatoryId, instrumentId);

        instrument.setReference(replacementReference);

        return responseWrapper(instrument, 201);
    }

    @PUT
    @Path("{instrumentId}/kind")
    @Operation(summary = "replace the 'kind' of the Instrument specified by the 'instrumentId'; one-of CONTINUUM, SPECTROSCOPIC")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceInstrumentKind(@PathParam("observatoryId") Long observatoryId,
                                          @PathParam("instrumentId") Long instrumentId,
                                          String replacementKind) throws WebApplicationException
    {
        Instrument instrument = findInstrumentByQuery(observatoryId, instrumentId);

        try {
            instrument.setKind(InstrumentKind.fromValue(replacementKind));
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(e.getMessage(), 400);
        }

        return responseWrapper(instrument, 201);
    }

}
