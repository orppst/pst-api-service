package org.orph2020.pst.apiimpl.rest;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.management.ProposalCycle;
import org.ivoa.dm.proposal.management.Resource;
import org.ivoa.dm.proposal.management.ResourceType;
import org.orph2020.pst.common.json.ObjectIdentifier;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

@Path("proposalCycles/{cycleId}/availableResources")
@Tag(name = "proposalCycles-availableResources")
@Produces(MediaType.APPLICATION_JSON)
public class AvailableResourcesResource extends ObjectResourceBase {

    @GET
    @Operation(summary = "get all the Resources associated with the given ProposalCycle")
    public List<Resource> getCycleAvailableResources(@PathParam("cycleId") long cycleId)
    {
        //boiled down a "Resource" consists of a double and two (short) Strings
        return findObject(ProposalCycle.class, cycleId).getAvailableResources().getResources();
    }

    @GET
    @Path("types")
    @Operation(summary = "get all the ResourceTypes associated with the given ProposalCycle")
    public List<ObjectIdentifier> getCycleResourceTypes(@PathParam("cycleId") long cycleId)
    {
        List<Resource> resources = findObject(ProposalCycle.class, cycleId)
                .getAvailableResources().getResources();

        List<ObjectIdentifier> result = new ArrayList<>();
        for (Resource r : resources) {
            result.add(new ObjectIdentifier(r.getType().getId(), r.getType().getName()));
        }

        return result;
    }

    @GET
    @Path("types/{typeId}")
    @Operation(summary = "get the ResourceType specified by 'typeId'")
    public ResourceType getCycleResourceType(@PathParam("cycleId") long cycleId,
                                             @PathParam("typeId") long typeId)
    {
        return findObject(ResourceType.class, typeId);
    }

}
