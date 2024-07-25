package org.orph2020.pst.apiimpl.rest;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
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
    public List<ObjectIdentifier> getAllocatedProposals(@PathParam("cycleCode") Long cycleCode,
                                                        @RestQuery String title) {

        String select = "select o._id,o.submitted.proposal.title ";
        String from = "from ProposalCycle p ";
        String innerJoins = "inner join p.allocatedProposals o ";
        String where = "where p._id=" + cycleCode + " ";
        String titleLike = title == null ? "" : "and o.submitted.proposal.title like '" + title + "' ";
        String orderBy = "order by o.submitted.proposal.title";

        return getObjectIdentifiers(select + from + innerJoins + where + titleLike + orderBy);
    }

    @GET
    @Path("{allocatedId}")
    @Operation(summary = "get the Allocated Proposal specified by 'allocationId' in the given cycle")
    public AllocatedProposal getAllocatedProposal(@PathParam("cycleCode") Long cycleCode,
                                                  @PathParam("allocatedId") Long allocatedId) {
        return findChildByQuery(ProposalCycle.class, AllocatedProposal.class,
                "allocatedProposals", cycleCode, allocatedId);
    }


    @PUT
    @Operation(summary = "upgrade a proposal under review to an allocated proposal")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public ProposalSynopsis allocateProposalToCycle(@PathParam("cycleCode") Long cycleCode,
                                                    Long submittedId)
            throws WebApplicationException
    {
        AllocatedProposal allocatedProposal = new AllocatedProposal(
                findChildByQuery(ProposalCycle.class, SubmittedProposal.class,
                        "submittedProposals", cycleCode, submittedId),new ArrayList<>());

        ProposalCycle cycle = findObject(ProposalCycle.class, cycleCode);

        cycle.addToAllocatedProposals(allocatedProposal);

        em.merge(cycle);

        return new ProposalSynopsis(allocatedProposal.getSubmitted().getProposal());
    }




}
