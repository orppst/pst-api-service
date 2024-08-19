package org.orph2020.pst.apiimpl.rest;

import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.management.*;
import org.ivoa.dm.proposal.prop.ObservingMode;
import org.orph2020.pst.common.json.ObjectIdentifier;

import java.util.List;

@Path("proposalCycles/{cycleCode}/allocatedProposals/{allocatedId}/allocatedBlock")
@Tag(name = "proposalCycles-allocated-proposal-allocated-block")
@Produces(MediaType.APPLICATION_JSON)
public class AllocatedBlockResource extends ObjectResourceBase{

    @GET
    @Operation(summary = "get the allocated resource blocks associated with the given allocated proposal")
    public List<ObjectIdentifier> getAllocatedBlocks(
            @PathParam("cycleCode") Long cycleCode,
            @PathParam("allocatedId") Long allocatedId)
    {
        String qlString = "select b._id,b.resource.type.name,b.grade.name from ProposalCycle c "
                + "inner join c.allocatedProposals a inner join a.allocation b "
                + "where c._id=" + cycleCode + " and a._id=" + allocatedId + " "
                + "order by b.grade.name";

        Query query = em.createQuery(qlString);

        return getObjectIdentifiersAlt(query);
    }

    @GET
    @Path("{blockId}")
    @Operation(summary = "get the AllocatedBlock specified by the 'blockId'")
    public AllocatedBlock getAllocatedBlock(@PathParam("cycleCode") Long cycleCode,
                                            @PathParam("allocatedId") Long allocatedId,
                                            @PathParam("blockId") Long blockId)
    {
        return findChildByQuery(AllocatedProposal.class, AllocatedBlock.class,
                "allocation", allocatedId, blockId);
    }

    @POST
    @Operation(summary = "add an AllocationBlock to the specific AllocatedProposal")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public AllocatedBlock addAllocatedBlock(@PathParam("cycleCode") Long cycleCode,
                                            @PathParam("allocatedId") Long allocatedId,
                                            AllocatedBlock allocatedBlock)
            throws WebApplicationException
    {
        AllocatedProposal allocatedProposal = findChildByQuery(ProposalCycle.class, AllocatedProposal.class,
                "allocatedProposals", cycleCode, allocatedId);

        // reminder that the incoming allocatedBlock has a resource.type.id only, NOT a
        // ResourceType object, thus we have to find the ResourceType with the id.

        ResourceType resourceType = findObject(ResourceType.class,
                allocatedBlock.getResource().getType().getId());

        String resourceName = resourceType.getName();

        Double resourceAmount = allocatedBlock.getResource().getAmount();

        Double available = AvailableResourceHelper
                .findAvailableResource(em, cycleCode, resourceName)
                .getAmount();
        Double allocated = AvailableResourceHelper
                .getAllocatedResourceAmount(em, cycleCode, resourceName);

        Double remaining = available - allocated;

        if (remaining < resourceAmount) {
            String resourceUnit = resourceType.getUnit();
            throw new WebApplicationException(
                    String.format("cannot allocate %.2f %s as only %.2f %s remains of %s",
                    resourceAmount, resourceUnit, remaining, resourceUnit, resourceName),
                    Response.Status.BAD_REQUEST);
        }

        return addNewChildObject(allocatedProposal, allocatedBlock, allocatedProposal::addToAllocation);
    }

    @DELETE
    @Path("{blockId}")
    @Operation(summary = "remove the specified AllocationBlock from the given AllocatedProposal")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response removeAllocatedBlock(@PathParam("cycleCode") Long cycleCode,
                                         @PathParam("allocatedId") Long allocatedId,
                                         @PathParam("blockId") Long blockId)
        throws WebApplicationException
    {
        AllocatedProposal allocatedProposal = findChildByQuery(ProposalCycle.class, AllocatedProposal.class,
                "allocatedProposals", cycleCode, allocatedId);

        AllocatedBlock allocatedBlock = findChildByQuery(AllocatedProposal.class, AllocatedBlock.class,
                "allocation", allocatedId, blockId);

        return deleteChildObject(allocatedProposal, allocatedBlock, allocatedProposal::removeFromAllocation);
    }

    @PUT
    @Path("{blockId}/grade")
    @Operation(summary = "change the grade of the given AllocatedBlock")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateGrade(@PathParam("cycleCode") Long cycleCode,
                                @PathParam("allocatedId") Long allocatedId,
                                @PathParam("blockId") Long blockId,
                                Long gradeId)
        throws WebApplicationException
    {
        AllocatedBlock allocatedBlock = findChildByQuery(AllocatedProposal.class, AllocatedBlock.class,
                "allocation", allocatedId, blockId);

        AllocationGrade allocationGrade = findChildByQuery(ProposalCycle.class, AllocationGrade.class,
                "possibleGrades", cycleCode, gradeId);

        allocatedBlock.setGrade(allocationGrade);

        return responseWrapper(allocatedBlock, 200);
    }

    @PUT
    @Path("{blockId}/resourceAmount")
    @Operation(summary = "change the amount of the resource of the given AllocatedBlock")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateResource(@PathParam("cycleCode") Long cycleCode,
                                   @PathParam("allocatedId") Long allocatedId,
                                   @PathParam("blockId") Long blockId,
                                   Double updateAmount)
        throws WebApplicationException
    {
        AllocatedBlock allocatedBlock = findChildByQuery(AllocatedProposal.class, AllocatedBlock.class,
                "allocation", allocatedId, blockId);

        Double currentAmount = allocatedBlock.getResource().getAmount();

        if (currentAmount < updateAmount) {
            //check remaining
            String resourceName = allocatedBlock.getResource().getType().getName();
            Double available = AvailableResourceHelper
                    .findAvailableResource(em, cycleCode, resourceName)
                    .getAmount();
            Double allocated = AvailableResourceHelper
                    .getAllocatedResourceAmount(em, cycleCode, resourceName);

            Double remaining = available - allocated;

            if (updateAmount > remaining + currentAmount) {
                String resourceUnit = allocatedBlock.getResource().getType().getUnit();

                throw new WebApplicationException(
                        String.format("cannot update %s from %.2f -> %.2f %s, exceeds total, currently remaining: %.2f %s",
                                resourceName, currentAmount, updateAmount, resourceUnit, remaining, resourceUnit),
                        Response.Status.BAD_REQUEST);
            }
        }

        allocatedBlock.getResource().setAmount(updateAmount);

        return responseWrapper(allocatedBlock, 200);
    }

    @PUT
    @Path("{blockId}/observingMode")
    @Operation(summary = "change the ObservingMode of the given AllocationBlock")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response changeObservingMode(@PathParam("cycleCode") Long cycleCode,
                                        @PathParam("allocatedId") Long allocatedId,
                                        @PathParam("blockId") Long blockId,
                                        Long observingModeId)
        throws WebApplicationException
    {
        AllocatedBlock allocatedBlock = findChildByQuery(AllocatedProposal.class, AllocatedBlock.class,
                "allocation", allocatedId, blockId);

        ObservingMode observingMode = findChildByQuery(ProposalCycle.class, ObservingMode.class,
                "observingModes", cycleCode, observingModeId);

        allocatedBlock.setMode(observingMode);

        return responseWrapper(allocatedBlock, 200);
    }


}
