package org.orph2020.pst.apiimpl.rest;

import jakarta.persistence.Query;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.management.Filter;
import org.ivoa.dm.proposal.management.ObservingMode;
import org.ivoa.dm.proposal.management.ProposalCycle;
import org.orph2020.pst.common.json.ObjectIdentifier;

import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.*;


/*
The observing modes are offered in a particular ProposalCycle. An observing mode describes the ObservingConfigurations
of an observatory, this includes the telescope(s), instrument(s) and backend(s) that are to be used for an observation.
Note that an ObservingConfiguration does not necessarily contain unique elements. For example, an ObservingMode may
consist of multiple ObservingConfigurations each with the same telescope list/array but with different instruments
and/or different backends.
 */

@Path("proposalCycles/{cycleId}/observingModes")
@Tag(name = "proposalCycles-observingModes")
@Produces(MediaType.APPLICATION_JSON)
public class ObservingModeResource extends ObjectResourceBase {

    private ObservingMode findObservingModeByQuery(long cycleId, long modeId) {
        TypedQuery<ObservingMode>  q = em.createQuery(
                "select om from ProposalCycle c join c.observingModes om where c._id = :cid and om._id = :mid",
                ObservingMode.class
        );
        q.setParameter("cid", cycleId);
        q.setParameter("mid", modeId);
        return q.getSingleResult(); //throws NoResultException if entity not found
    }

    @GET
    @Operation(summary = "get a list of object identifiers for all the ObservingModes associated with the given ProposalCycle")
    public List<ObjectIdentifier> getCycleObservingModes(@PathParam("cycleId") Long cycleId)
    {
        String qlString = "select om._id,om.name,om.description from ProposalCycle c "
                + "inner join c.observingModes om "
                + "where c._id=" + cycleId + " order by om._id";

        Query query = em.createQuery(qlString);

        return getObjectIdentifiersAlt(query);
    }


    // including this call to simplify building a filtered-search in the GUI for observing modes
    // based on Instrument, Backend, and (Spectroscopic) Filter
    @GET
    @Path("objectList")
    @Operation(summary = "get a list of ObservingModes (whole objects) for the given ProposalCycle")
    public List<ObservingMode> getObservingModeObjects(@PathParam("cycleId") Long cycleId)
    {
        return findObject(ProposalCycle.class, cycleId).getObservingModes();
    }

    @GET
    @Path("{modeId}")
    @Operation(summary = "get the ObservingMode specified by 'modeId'")
    public ObservingMode getCycleObservingMode(@PathParam("cycleId") Long cycleId,
                                               @PathParam("modeId") Long modeId)
            throws WebApplicationException
    {
        return findObservingModeByQuery(cycleId, modeId);
    }

    @GET
    @Path("filters")
    @Operation(summary = "get all the distinct filters associated with observing modes of a given cycle")
    public List<Filter> getObservingModesFilters(@PathParam("cycleId") Long cycleId)
        throws WebApplicationException
    {
        String qlString = "select distinct om.filter from ProposalCycle c inner join c.observingModes om where c._id=" + cycleId;

        TypedQuery<Filter> query = em.createQuery(qlString, Filter.class);

        List<Filter> allFilters = query.getResultList();

        //the "same" Filter may be found in different ObservingModes as different DB entities, we
        //want a list of distinct Filters by name. Notice that this assumes an observatory has used
        //consistent filter names when creating the ObservingModes i.e., multiple Filter entities that
        //share the same name ARE the same Filter (barring the DB id the value of all their other
        // members are equivalent)

        List<String> distinctFilterNames = allFilters.stream().map(Filter::getName).distinct().toList();

        List<Filter> result = new ArrayList<>();


        for (String filterName : distinctFilterNames) {
            for(Filter filter : allFilters) {
                if (filter.getName().equals(filterName)) {
                    result.add(filter);
                    break;
                }
            }
        }

        return result;
    }
}
