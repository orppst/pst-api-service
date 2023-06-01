package org.orph2020.pst.apiimpl.rest;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.management.ProposalCycle;
import org.ivoa.dm.proposal.management.Resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("proposalCycles/{cycleId}/availableResources")
@Tag(name = "proposalCycles-availableResources")
@Produces(MediaType.APPLICATION_JSON)
public class AvailableResourcesResource extends ObjectResourceBase {

    @GET
    @Operation(summary = "get all the AvailableResources associated with the given ProposalCycle")
    public List<Resource> getCycleAvailableResources(@PathParam("cycleId") Long cycleId)
    {
        //boiled down a "Resource" consists of a double and two (short) Strings
        return findObject(ProposalCycle.class, cycleId).getAvailableResources().getResources();
    }
}
