package org.orph2020.pst.apiimpl.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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
@RolesAllowed({"tac_admin", "tac_member"})
public class AllocatedProposalResource extends ObjectResourceBase{

    @GET
    @Operation(summary = "get identifiers for all the AllocatedProposals in the given ProposalCycle, optionally provide a proposal title to get a specific identifier ")
    public List<ObjectIdentifier> getAllocatedProposals(@PathParam("cycleCode") Long cycleCode,
                                                        @RestQuery String title) {

        String select = "select o._id,o.submitted.title ";
        String from = "from ProposalCycle p ";
        String innerJoins = "inner join p.allocatedProposals o ";
        String where = "where p._id=" + cycleCode + " ";
        String titleLike = title == null ? "" : "and o.submitted.title like '" + title + "' ";
        String orderBy = "order by o.submitted.title";

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
    @Operation(summary = "upgrade a submitted proposal to an allocated proposal")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public ProposalSynopsis allocateProposalToCycle(@PathParam("cycleCode") Long cycleCode,
                                                    Long submittedId)
            throws WebApplicationException
    {
        AllocatedProposal allocatedProposal = new AllocatedProposal(
                findChildByQuery(
                        ProposalCycle.class,
                        SubmittedProposal.class,
                        "submittedProposals",
                        cycleCode,
                        submittedId
                ),
                new ArrayList<>() //empty allocations list to be added to later
        );

        ProposalCycle cycle = findObject(ProposalCycle.class, cycleCode);

        cycle.addToAllocatedProposals(allocatedProposal);

        em.merge(cycle);

        //set the 'success' flag in the related submitted proposal
        allocatedProposal.getSubmitted().setSuccessful(true);

        return new ProposalSynopsis(allocatedProposal.getSubmitted());
    }

    //TODO: make this callable by a 'TAC Chair' user only
    @RolesAllowed("tac_admin")
    @DELETE
    @Path("{allocatedId}")
    @Operation(summary = "withdraw a previously allocated proposal from the cycle")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response withdrawAllocatedProposal(@PathParam("cycleCode") Long cycleCode,
                                              @PathParam("allocatedId") Long allocatedId)
    throws WebApplicationException {

        ProposalCycle cycle = findObject(ProposalCycle.class, cycleCode);

        AllocatedProposal allocatedProposal = findChildByQuery(
                ProposalCycle.class,
                AllocatedProposal.class,
                "allocatedProposals",
                cycleCode,
                allocatedId
        );

        cycle.removeFromAllocatedProposals(allocatedProposal);

        em.merge(cycle);

        //reset the success flag in the related submitted proposal
        allocatedProposal.getSubmitted().setSuccessful(false);

        return Response.noContent().build();
    }




}
