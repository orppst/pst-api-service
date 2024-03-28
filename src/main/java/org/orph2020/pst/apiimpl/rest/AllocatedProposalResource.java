package org.orph2020.pst.apiimpl.rest;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.management.AllocatedBlock;
import org.ivoa.dm.proposal.management.AllocatedProposal;
import org.ivoa.dm.proposal.management.ProposalCycle;
import org.ivoa.dm.proposal.management.SubmittedProposal;
import org.jboss.resteasy.reactive.RestQuery;
import org.orph2020.pst.common.json.ObjectIdentifier;
import org.orph2020.pst.common.json.ProposalSynopsis;

import java.util.ArrayList;
import java.util.List;

@Path("proposalCycles/{cycleCode}/allocatedProposals")
@Tag(name = "proposalCycles-allocated-proposals")
@Produces(MediaType.APPLICATION_JSON)
public class AllocatedProposalResource extends ObjectResourceBase{

    @GET
    @Operation(summary = "get identifiers for all the AllocatedProposals in the given ProposalCycle, optionally provide a proposal title to get a specific identifier ")
    public List<ObjectIdentifier> getAllocatedProposalsFromCycle(@PathParam("cycleCode") Long cycleCode,
                                                                 @RestQuery String title) {

        String select = "select o._id,o.submitted.proposal.title ";
        String from = "from ProposalCycle p ";
        String innerJoins = "inner join p.allocatedProposals o ";
        String where = "where p._id=" + cycleCode + " ";
        String titleLike = title == null ? "" : "and o.submitted.proposal.title like '" + title + "' ";
        String orderBy = "order by o.submitted.proposal.title";

        return getObjectIdentifiers(select + from + innerJoins + where + titleLike + orderBy);
    }


    @PUT
    @Operation(summary = "upgrade a proposal under review to an allocated proposal")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public ProposalSynopsis allocateProposalToCycle(@PathParam("cycleCode") Long cycleCode,
                                                    Long submittedId)
            throws WebApplicationException
    {
        AllocatedProposal allocatedProposal = new AllocatedProposal(new ArrayList<>(),
                findChildByQuery(ProposalCycle.class, SubmittedProposal.class,
                        "submittedProposals", cycleCode, submittedId));

        ProposalCycle cycle = findObject(ProposalCycle.class, cycleCode);

        cycle.addToAllocatedProposals(allocatedProposal);

        em.merge(cycle);

        return new ProposalSynopsis(allocatedProposal.getSubmitted().getProposal());
    }

    @POST
    @Path("/{allocatedId}")
    @Operation(summary = "add an AllocationBlock to the specific AllocatedProposal")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public AllocatedBlock addAllocatedBlockToAllocatedProposal(@PathParam("cycleCode") long cycleCode,
                                                               @PathParam("allocatedId") long allocatedId,
                                                               AllocatedBlock allocatedBlock)
            throws WebApplicationException
    {
        AllocatedProposal allocatedProposal = findChildByQuery(ProposalCycle.class, AllocatedProposal.class,
                "allocatedProposals", cycleCode, allocatedId);

        return addNewChildObject(allocatedProposal, allocatedBlock, allocatedProposal::addToAllocation);
    }


}
