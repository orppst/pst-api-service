package org.orph2020.pst.apiimpl.rest;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.management.ProposalCycle;
import org.ivoa.dm.proposal.management.ProposalReview;
import org.ivoa.dm.proposal.management.ReviewedProposal;
import org.orph2020.pst.common.json.ObjectIdentifier;

import java.util.Date;
import java.util.List;

@Path("proposalCycles/{cycleCode}/proposalsInReview/{reviewedProposalId}/reviews")
@Tag(name = "proposalCycles-proposals-in-review-the-reviews")
@Produces(MediaType.APPLICATION_JSON)
public class ProposalReviewResource extends ObjectResourceBase{

    @GET
    @Operation(summary = "get the ObjectIdentifiers of the reviews of the given ReviewedProposal")
    public List<ObjectIdentifier> getReviews(@PathParam("cycleCode") Long cycleCode,
                                             @PathParam("reviewedProposalId") Long reviewedProposalId)
    {
        String select = "select r._id,r.reviewer.person.fullName ";
        String from = "from ProposalCycle c ";
        String innerJoins = "inner join c.reviewedProposals p inner join p.reviews r ";
        String where = "where c._id=" + cycleCode + " and p._id=" + reviewedProposalId + " ";
        String orderBy = "order by r.reviewer.person.fullName";

        return getObjectIdentifiers(select + from + innerJoins + where + orderBy);
    }

    @GET
    @Path("/{reviewId}")
    @Operation(summary = "get the specific review of the given ReviewedProposal")
    public ProposalReview getReview(@PathParam("cycleCode") Long cycleCode,
                                    @PathParam("reviewedProposalId") Long reviewedProposalId,
                                    @PathParam("reviewId") Long reviewId)
    {
        return findChildByQuery(ReviewedProposal.class, ProposalReview.class,
                "reviews", reviewedProposalId, reviewId);
    }

    @POST
    @Operation(summary = "add a new review to the given ReviewedProposal")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public ProposalReview addReview(@PathParam("cycleCode") Long cycleCode,
                                    @PathParam("reviewedProposalId") Long reviewedProposalId,
                                    ProposalReview proposalReview)
        throws WebApplicationException
    {
        ReviewedProposal reviewedProposal = findChildByQuery(ProposalCycle.class, ReviewedProposal.class,
                "reviewedProposals", cycleCode, reviewedProposalId);

        //ensure the date is set to 'now'
        proposalReview.setReviewDate(new Date());

        return addNewChildObject(reviewedProposal, proposalReview, reviewedProposal::addToReviews);
    }

    @DELETE
    @Path("/{reviewId}")
    @Operation(summary = "remove the specified review from the given ReviewedProposal")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response removeReview(@PathParam("cycleCode") Long cycleCode,
                                 @PathParam("reviewedProposalId") Long reviewedProposalId,
                                 @PathParam("reviewId") Long reviewId)
        throws WebApplicationException
    {
        ReviewedProposal reviewedProposal = findChildByQuery(ProposalCycle.class, ReviewedProposal.class,
                "reviewedProposals", cycleCode, reviewedProposalId);

        ProposalReview proposalReview = findChildByQuery(ReviewedProposal.class, ProposalReview.class,
            "reviews", reviewedProposalId, reviewId);

        return deleteChildObject(reviewedProposal,proposalReview, reviewedProposal::removeFromReviews);
    }

    /*
        Whenever we update a field in a review we update the reviewDate with the current date
     */

    @PUT
    @Path("/{reviewId}")
    @Operation(summary = "update the review with new data")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public ProposalReview updateReviewComment(@PathParam("cycleCode") Long cycleCode,
                                              @PathParam("reviewedProposalId") Long reviewedProposalId,
                                              @PathParam("reviewId") Long reviewId,
                                              ProposalReview replacementReview)
        throws WebApplicationException
    {
        ProposalReview proposalReview = findChildByQuery(ReviewedProposal.class, ProposalReview.class,
                "reviews", reviewedProposalId, reviewId);

        proposalReview.updateUsing(replacementReview);

        proposalReview.setReviewDate(new Date());

        em.merge(proposalReview);

        return proposalReview;
    }


}
