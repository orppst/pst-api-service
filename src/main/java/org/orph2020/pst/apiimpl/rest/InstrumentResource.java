package org.orph2020.pst.apiimpl.rest;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.prop.*;
import org.jboss.resteasy.reactive.RestQuery;
import org.orph2020.pst.common.json.ObjectIdentifier;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/*
 semantics: you can create and add an Instrument to a specific Observatory. You can delete a specific
 Instrument from the given Observatory, which also removes the Instrument entity from the database.
 You can edit any field of a specific Instrument for a given Observatory. In other words, Instruments
 can only exist within the context of their associated Observatory.
 */

@Path("observatories/{observatoryId}/instruments")
@Tag(name = "observatory-instruments")
@Produces(MediaType.APPLICATION_JSON)
public class InstrumentResource extends ObjectResourceBase {

    private Instrument findInstrumentInList(List<Instrument> instruments, long instrumentId ) {
        return (Instrument) super.findObjectInList(instrumentId, instruments);
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
            return super.getObjects("SELECT i._id,i.name FROM Observatory o Inner Join o.instruments i WHERE o._id = '"+observatoryId+"' ORDER BY i.name");
        } else {
            return super.getObjects("SELECT i._id,i.name FROM Observatory o Inner Join o.instruments i WHERE o._id = '"+observatoryId+"' and i.name like '"+name+"' ORDER BY i.name");
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
        Observatory observatory = super.findObject(Observatory.class, observatoryId);

        Instrument instrument = findInstrumentByQuery(observatoryId, instrumentId );

        observatory.removeFromInstruments(instrument);

        return super.removeObject(Instrument.class, instrumentId);
    }

    @POST
    @Operation(summary = "create an Instrument in the database and add it to the Observatory specified by the 'id'")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Instrument createAndAddInstrumentToObservatory(@PathParam("observatoryId") Long observatoryId,
                                                        Instrument instrument)
            throws WebApplicationException
    {
        Observatory observatory = super.findObject(Observatory.class, observatoryId);

        for (Instrument i : observatory.getInstruments()) {
            //semantically what we want but unsure how 'equals' is implemented
            if (i.equals(instrument)) {
                throw new WebApplicationException("Instrument already added to Observatory", 400);
            }
        }

        return super.addNewChildObject(observatory, instrument, observatory::addToInstruments);
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

        return super.responseWrapper(instrument, 201);
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

        return super.responseWrapper(instrument, 201);
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

        return super.responseWrapper(instrument, 201);
    }

    @PUT
    @Path("{instrumentId}/frequencyCoverage")
    @Operation(summary = "replace the frequencyCoverage of the Instrument specified by the 'instrumentId'")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceInstrumentFrequencyCoverage(@PathParam("observatoryId") Long observatoryId,
                                                       @PathParam("instrumentId") Long instrumentId,
                                                       SpectralWindowSetup replacementFrequencyCoverage)
            throws WebApplicationException
    {
        Instrument instrument = findInstrumentByQuery(observatoryId, instrumentId);

        instrument.setFrequencyCoverage(replacementFrequencyCoverage);

        return super.responseWrapper(instrument, 201);
    }
}
