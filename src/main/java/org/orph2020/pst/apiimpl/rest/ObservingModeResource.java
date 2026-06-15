package org.orph2020.pst.apiimpl.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
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
@RolesAllowed("default-roles-orppst")
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

    @POST
    @Operation(summary = "add a new ObservingMode (with its Filter) to the given ProposalCycle")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("obs_administration")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public ObservingMode addNewObservingMode(
            @PathParam("cycleId") Long cycleId,
            ObservingMode observingMode)
            throws WebApplicationException
    {
        ProposalCycle cycle = findObject(ProposalCycle.class, cycleId);
        return addNewChildObject(cycle, observingMode, cycle::addToObservingModes);
    }

    @PUT
    @Path("{modeId}")
    @Operation(summary = "update the name and description of the ObservingMode specified by 'modeId'")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("obs_administration")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public ObservingMode updateObservingMode(
            @PathParam("cycleId") Long cycleId,
            @PathParam("modeId") Long modeId,
            ObservingMode replacement)
            throws WebApplicationException
    {
        ObservingMode mode = findObservingModeByQuery(cycleId, modeId);
        mode.setName(replacement.getName());
        mode.setDescription(replacement.getDescription());
        return mode;
    }

    @PUT
    @Path("{modeId}/filter")
    @Operation(summary = "update the Filter of the ObservingMode specified by 'modeId'")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("obs_administration")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Filter updateObservingModeFilter(
            @PathParam("cycleId") Long cycleId,
            @PathParam("modeId") Long modeId,
            Filter replacement)
            throws WebApplicationException
    {
        ObservingMode mode = findObservingModeByQuery(cycleId, modeId);
        Filter filter = mode.getFilter();
        filter.setName(replacement.getName());
        filter.setDescription(replacement.getDescription());
        if (replacement.getFrequencyCoverage() != null) {
            filter.setFrequencyCoverage(replacement.getFrequencyCoverage());
        }
        return filter;
    }

    @DELETE
    @Path("{modeId}")
    @Operation(summary = "remove the ObservingMode specified by 'modeId' from the ProposalCycle, also deletes the linked Filter")
    @RolesAllowed("obs_administration")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response deleteObservingMode(
            @PathParam("cycleId") Long cycleId,
            @PathParam("modeId") Long modeId)
            throws WebApplicationException
    {
        ProposalCycle cycle = findObject(ProposalCycle.class, cycleId);
        ObservingMode mode = findObservingModeByQuery(cycleId, modeId);
        return deleteChildObject(cycle, mode, cycle::removeFromObservingModes);
    }

    @POST
    @Path("copyFrom/{sourceCycleId}")
    @Operation(summary = "copy all the observing modes from the source proposal cycle to this proposal cycle; "
            + "both cycles must belong to the same observatory")
    @Transactional(rollbackOn = {WebApplicationException.class})
    @RolesAllowed("obs_administration")
    public List<ObjectIdentifier> copyObservingModes(
            @PathParam("cycleId") Long cycleId,
            @PathParam("sourceCycleId") Long sourceCycleId)
            throws WebApplicationException
    {
        if (cycleId.equals(sourceCycleId)) {
            throw new WebApplicationException(
                    "Source and target proposal cycles must be different",
                    Response.Status.BAD_REQUEST);
        }

        ProposalCycle targetCycle = findObject(ProposalCycle.class, cycleId);
        ProposalCycle sourceCycle = findObject(ProposalCycle.class, sourceCycleId);

        if (!targetCycle.getObservatory().getId().equals(sourceCycle.getObservatory().getId())) {
            throw new WebApplicationException(
                    "Both proposal cycles must belong to the same observatory",
                    Response.Status.BAD_REQUEST);
        }

        for (ObservingMode sourceMode : sourceCycle.getObservingModes()) {
            Filter sourceFilter = sourceMode.getFilter();
            Filter newFilter = new Filter(
                    sourceFilter.getName(),
                    sourceFilter.getDescription(),
                    sourceFilter.getFrequencyCoverage()
            );
            ObservingMode newMode = new ObservingMode(
                    sourceMode.getName(),
                    sourceMode.getDescription(),
                    sourceMode.getTelescope(),
                    sourceMode.getInstrument(),
                    newFilter,
                    sourceMode.getBackend()
            );
            addNewChildObject(targetCycle, newMode, targetCycle::addToObservingModes);
        }

        Query query = em.createQuery(
                "select om._id,om.name,om.description from ProposalCycle c "
                        + "inner join c.observingModes om "
                        + "where c._id = :cycleId order by om._id");
        query.setParameter("cycleId", cycleId);
        return getObjectIdentifiersAlt(query);
    }
}
