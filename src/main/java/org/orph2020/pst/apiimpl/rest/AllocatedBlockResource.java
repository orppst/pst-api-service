package org.orph2020.pst.apiimpl.rest;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.management.AllocatedBlock;
import org.ivoa.dm.proposal.management.AllocatedProposal;
import org.ivoa.dm.proposal.management.ProposalCycle;
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
        String select = "select b._id,b.resource.type.name,b.grade.name ";
        String from = "from ProposalCycle c ";
        String innerJoins = "inner join c.allocatedProposals a inner join a.allocation b ";
        String where = "where c._id=" + cycleCode + " and a._id=" + allocatedId + " ";
        String orderBy = "order by b.grade.name";

        return getObjectIdentifiers(select + from + innerJoins + where + orderBy);
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
}
