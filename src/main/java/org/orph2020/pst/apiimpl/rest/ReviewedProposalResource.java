package org.orph2020.pst.apiimpl.rest;


import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.management.ProposalCycle;
import org.ivoa.dm.proposal.management.ReviewedProposal;
import org.ivoa.dm.proposal.management.SubmittedProposal;
import org.jboss.resteasy.reactive.RestQuery;
import org.orph2020.pst.common.json.ObjectIdentifier;
import org.orph2020.pst.common.json.ReviewedProposalSynopsis;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Path("proposalCycles/{cycleCode}/proposalsInReview")
@Tag(name="proposalCycles-proposals-in-review")
@Produces(MediaType.APPLICATION_JSON)
public class ReviewedProposalResource extends ObjectResourceBase{

    @GET
    @Operation(summary = "get the ObjectIdentifiers for the reviewed proposals")
    public List<ObjectIdentifier> getReviewedProposals(@PathParam("cycleCode") Long cycleCode,
                                                       @RestQuery String title)
    {

        String titleLike = title == null ? "" :
                "and r.submitted.proposal.title = :pTitle ";

        String qlString = "select r._id,cast(r.submitted.proposal._id as string),r.submitted.proposal.title "
                + "from ProposalCycle c "
                + "inner join c.reviewedProposals r "
                + "where c._id=" + cycleCode + " "
                + titleLike
                + " order by r.submitted.proposal.id desc";

        Query query = em.createQuery(qlString);

        if (title != null) query.setParameter("pTitle", title);

        return getObjectIdentifiersAlt(query);
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

    @PUT
    @Operation(summary = "upgrade a submitted proposal to a proposal under review")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public ReviewedProposalSynopsis upgradeSubmittedProposalToReview(
            @PathParam("cycleCode") Long cycleCode,
            Long submittedProposalId)
        throws WebApplicationException
    {
        ProposalCycle proposalCycle = findObject(ProposalCycle.class, cycleCode);

        SubmittedProposal submittedProposal = findChildByQuery(ProposalCycle.class, SubmittedProposal.class,
                "submittedProposals", cycleCode, submittedProposalId);

        //Date cannot be null (non-null constraint in DB) so we use the posix epoch for a "fresh" date
        ReviewedProposal reviewedProposal =
                new ReviewedProposal(false, new Date(0L), new ArrayList<>(), submittedProposal);

        proposalCycle.addToReviewedProposals(reviewedProposal);

        em.merge(proposalCycle);

        return new ReviewedProposalSynopsis(reviewedProposal);
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
    public ReviewedProposalSynopsis updateReviewedProposalSuccess(@PathParam("cycleCode") Long cycleCode,
                                                          @PathParam("reviewedProposalId") Long reviewedProposalId,
                                                          Boolean successStatus)
        throws WebApplicationException
    {
        ReviewedProposal reviewedProposal = findChildByQuery(ProposalCycle.class, ReviewedProposal.class,
                "reviewedProposals", cycleCode, reviewedProposalId);

        reviewedProposal.setSuccessful(successStatus);

        //update complete data to "now"
        reviewedProposal.setReviewsCompleteDate(new Date());

        return new ReviewedProposalSynopsis(reviewedProposal);
    }

    @PUT
    @Path("/{reviewedProposalId}/completeDate")
    @Operation(summary = "update the 'reviewsCompleteDate' of the given ReviewedProposal to today's date")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public ReviewedProposalSynopsis updateReviewedProposalCompleteDate(
            @PathParam("cycleCode") Long cycleCode,
            @PathParam("reviewedProposalId") Long reviewedProposalId)
        throws WebApplicationException
    {
        ReviewedProposal reviewedProposal = findChildByQuery(ProposalCycle.class, ReviewedProposal.class,
                "reviewedProposals", cycleCode, reviewedProposalId);

        reviewedProposal.setReviewsCompleteDate(new Date());

        return new ReviewedProposalSynopsis(reviewedProposal);
    }

}
