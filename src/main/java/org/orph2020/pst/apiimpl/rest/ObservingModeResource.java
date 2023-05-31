package org.orph2020.pst.apiimpl.rest;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.management.ProposalCycle;
import org.ivoa.dm.proposal.prop.ObservingConfiguration;
import org.ivoa.dm.proposal.prop.ObservingMode;
import org.jboss.resteasy.reactive.RestQuery;
import org.orph2020.pst.common.json.ObjectIdentifier;
import org.orph2020.pst.common.json.ObservingConfigurationSynopsis;

import javax.persistence.TypedQuery;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
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
    @Operation(summary = "get all the ObservingMode identifiers associated with the given ProposalCycle")
    public List<ObjectIdentifier> getCycleObservingModes(@PathParam("cycleId") Long cycleId,
                                                               @RestQuery String modeName)
    {
        if(modeName == null) {
            return super.getObjects("SELECT om._id,om.name FROM ProposalCycle c Inner Join c.observingModes om WHERE c._id = '"+cycleId+"' ORDER BY om.name");
        } else {
            return super.getObjects("SELECT om._id,om.name FROM ProposalCycle c Inner Join c.observingModes om WHERE c._id = '"+cycleId+"' and om.name like '"+modeName+"' ORDER BY om.name");
        }
    }

    @GET
    @Path("{modeId}")
    @Operation(summary = "get a synopsis of the ObservingMode specified by 'modeId'")
    public List<ObservingConfigurationSynopsis> getCycleObservingMode(@PathParam("cycleId") Long cycleId,
                                                                      @PathParam("modeId") Long modeId)
            throws WebApplicationException
    {
        ObservingMode observingMode = findObservingModeByQuery(cycleId, modeId);

        List<ObservingConfiguration> observingConfigurations = observingMode.getConfigurations();

        List<ObservingConfigurationSynopsis> result = new ArrayList<>();

        for (ObservingConfiguration c : observingConfigurations) {
            result.add(
                    new ObservingConfigurationSynopsis(c.getId(),
                            c.getTelescope().getName(), c.getInstrument().getName(),
                            c.getBackend().getName())
            );
        }

        return result;
    }
}
