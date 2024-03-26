package org.orph2020.pst.apiimpl.rest;


import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.management.ProposalCycle;
import org.ivoa.dm.proposal.management.ReviewedProposal;
import org.orph2020.pst.common.json.ObjectIdentifier;

import java.util.Date;
import java.util.List;

@Path("proposalCycles/{cycleCode}/reviewedProposals")
@Tag(name="reviewed-proposals")
@Produces(MediaType.APPLICATION_JSON)
public class ReviewedProposalResource extends ObjectResourceBase{

    @GET
    @Operation(summary = "get the ObjectIdentifiers for the reveiwed proposals")
    public List<ObjectIdentifier> getReviewedProposals(@PathParam("cycleCode") Long cycleCode)
    {
        String select = "select r._id,r.submitted.proposal.title ";
        String from = "from ProposalCycle c ";
        String innerJoins = "inner join c.reviewedProposals r ";
        String where = "where c._id=" + cycleCode + " ";
        String orderBy = "order by r.submitted.proposal.title";

        return getObjectIdentifiers(select + from + innerJoins + where + orderBy);
    }

    @GET
    @Path("/{reviewedProposalId}")
    @Operation(summary = "get the ReviewedProposal specified by 'reviewedProposalId'")
    public ReviewedProposal getReviewedProposal(@PathParam("cycleCode") Long cycleCode,
                                                @PathParam("reviewedProposalId") Long reviewedProposalId)
    {
        return findChildByQuery(ProposalCycle.class, ReviewedProposal.class,
                "reviewedProposals", cycleCode, reviewedProposalId);
    }

    @POST
    @Operation(summary = "add a new ReviewedProposal")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public ReviewedProposal addReviewedProposal(@PathParam("cycleCode") Long cycleCode,
                                                ReviewedProposal reviewedProposal)
        throws WebApplicationException
    {
        ProposalCycle proposalCycle = findObject(ProposalCycle.class, cycleCode);

        return addNewChildObject(proposalCycle, reviewedProposal,
                proposalCycle::addToReviewedProposals);
    }

    @DELETE
    @Path("/{reviewedProposalId}")
    @Operation(summary = "remove the ReviewedProposal specified by 'reviewedProposalId'")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response removeReviewedProposal(@PathParam("cycleCode") Long cycleCode,
                                           @PathParam("reviewedProposalId") Long reviewedProposalId)
        throws WebApplicationException
    {
        ProposalCycle proposalCycle = findObject(ProposalCycle.class, cycleCode);
        ReviewedProposal reviewedProposal = findChildByQuery(ProposalCycle.class, ReviewedProposal.class,
                "reviewedProposals", cycleCode, reviewedProposalId);

        return deleteChildObject(proposalCycle, reviewedProposal,
                proposalCycle::removeFromReviewedProposals);
    }

    @PUT
    @Path("/{reviewedProposalId}/success")
    @Operation(summary = "update the 'successful' status of the given ReviewedProposal")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public ReviewedProposal updateReviewedProposalSuccess(@PathParam("cycleCode") Long cycleCode,
                                                          @PathParam("reviewedProposalId") Long reviewedProposalId,
                                                          Boolean successStatus)
        throws WebApplicationException
    {
        ReviewedProposal reviewedProposal = findChildByQuery(ProposalCycle.class, ReviewedProposal.class,
                "reviewedProposals", cycleCode, reviewedProposalId);

        reviewedProposal.setSuccessful(successStatus);

        return reviewedProposal;
    }

    @PUT
    @Path("/{reviewedProposalId}/completeDate")
    @Operation(summary = "update the 'reviewsCompleteDate' of the given ReviewedProposal")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public ReviewedProposal updateReviewedProposalCompleteDate(
            @PathParam("cycleCode") Long cycleCode,
            @PathParam("reviewedProposalId") Long reviewedProposalId,
            Date completeDate)
        throws WebApplicationException
    {
        ReviewedProposal reviewedProposal = findChildByQuery(ProposalCycle.class, ReviewedProposal.class,
                "reviewedProposals", cycleCode, reviewedProposalId);

        reviewedProposal.setReviewsCompleteDate(completeDate);

        return reviewedProposal;
    }

}
