package org.orph2020.pst.apiimpl.rest;

import jakarta.persistence.Query;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.management.ObservingMode;
import org.orph2020.pst.common.json.ObjectIdentifier;

import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;


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

    @GET
    @Path("{modeId}")
    @Operation(summary = "get the ObservingMode specified by 'modeId'")
    public ObservingMode getCycleObservingMode(@PathParam("cycleId") Long cycleId,
                                               @PathParam("modeId") Long modeId)
            throws WebApplicationException
    {
        return findObservingModeByQuery(cycleId, modeId);
    }
}
