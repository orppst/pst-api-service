package org.orph2020.pst.apiimpl.rest;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.management.AvailableResources;
import org.ivoa.dm.proposal.management.ProposalCycle;
import org.ivoa.dm.proposal.management.Resource;
import org.ivoa.dm.proposal.management.ResourceType;
import org.orph2020.pst.common.json.ObjectIdentifier;

import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

@Path("proposalCycles/{cycleCode}/availableResources")
@Tag(name = "proposalCycles-availableResources")
@Produces(MediaType.APPLICATION_JSON)
public class AvailableResourcesResource extends ObjectResourceBase {

    @GET
    @Operation(summary = "get all the AvailableResources associated with the given ProposalCycle")
    public AvailableResources getCycleAvailableResources(@PathParam("cycleCode") Long cycleCode)
    {
        //available resources is a wrapper class for a List<Resources>
        return findObject(ProposalCycle.class, cycleCode).getAvailableResources();
    }

    @GET
    @Path("resources")
    @Operation(summary = "list the resources associated with the given ProposalCycle")
    public List<Resource> getCycleResources(@PathParam("cycleCode") Long cycleCode)
    {
        //Resource is not a large object; 1 double, 2 limited strings.
        return findObject(ProposalCycle.class, cycleCode).getAvailableResources().getResources();
    }

    @GET
    @Path("types")
    @Operation(summary = "get all the ResourceTypes associated with the given ProposalCycle")
    public List<ObjectIdentifier> getCycleResourceTypes(@PathParam("cycleCode") Long cycleCode)
    {
        List<Resource> resources = findObject(ProposalCycle.class, cycleCode)
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
    public ResourceType getCycleResourceType(@PathParam("cycleCode") long cycleId,
                                             @PathParam("typeId") long typeId)
    {
        return findObject(ResourceType.class, typeId);
    }

    @POST
    @Path("resources")
    @Operation(summary = "add a resource to the available resources of the given cycle")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Resource addCycleResource(@PathParam("cycleCode") Long cycleCode,
                                     Resource resource)
    {
        ProposalCycle proposalCycle = findObject(ProposalCycle.class, cycleCode);

        AvailableResources availableResources = proposalCycle.getAvailableResources();

        //Resource contains a reference to a resource type hence the copy
        return addNewChildObject(availableResources, new Resource(resource),
                availableResources::addToResources);

    }

    @DELETE
    @Path("resources/{resourceId}")
    @Operation(summary = "remove the resource given by 'resourceId'")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response removeCycleResource(@PathParam("cycleCode") Long cycleCode,
                                        @PathParam("resourceId") Long resourceId)
    {
        ProposalCycle proposalCycle = findObject(ProposalCycle.class, cycleCode);

        AvailableResources availableResources = proposalCycle.getAvailableResources();

        Resource resource = availableResources.getResources()
                .stream().filter(r -> resourceId.equals(r.getId())).findAny()
                .orElseThrow(() -> new WebApplicationException(
                        String.format(NON_ASSOCIATE_ID, "Resource", resourceId, "Proposal Cycle", cycleCode)
                ));

        return deleteChildObject(availableResources, resource,
                availableResources::removeFromResources);
    }

    @PUT
    @Path("resources/{resourceId}/amount")
    @Operation(summary = "edit the 'amount' of the specified resource in the given proposal cycle")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Resource updateCycleResourceAmount(@PathParam("cycleCode") Long cycleCode,
                                              @PathParam("resourceId") Long resourceId,
                                              Double newAmount)
        throws WebApplicationException
    {
        ProposalCycle proposalCycle = findObject(ProposalCycle.class, cycleCode);

        Resource resource = proposalCycle.getAvailableResources().getResources()
                .stream().filter(r -> resourceId.equals(r.getId())).findAny()
                .orElseThrow(() -> new WebApplicationException(
                        String.format(NON_ASSOCIATE_ID, "Resource", resourceId, "Proposal Cycle", cycleCode)
                ));

        resource.setAmount(newAmount);

        return resource;
    }
}
