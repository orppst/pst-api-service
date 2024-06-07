package org.orph2020.pst.apiimpl.rest;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.management.ProposalCycle;
import org.ivoa.dm.proposal.management.ProposalReview;
import org.ivoa.dm.proposal.management.SubmittedProposal;
import org.orph2020.pst.common.json.ObjectIdentifier;

import java.util.Date;
import java.util.List;

@Path("proposalCycles/{cycleCode}/submittedProposals/{submittedProposalId}/reviews")
@Tag(name = "proposalCycles-submitted-proposals-reviews")
@Produces(MediaType.APPLICATION_JSON)
public class ProposalReviewResource extends ObjectResourceBase{

    private static final String confirmed =
            "Unable to edit as this Review has been confirmed as completed on %s";

    @GET
    @Operation(summary = "get the ObjectIdentifiers of the reviews of the given SubmittedProposal")
    public List<ObjectIdentifier> getReviews(@PathParam("cycleCode") Long cycleCode,
                                             @PathParam("submittedProposalId") Long submittedProposalId)
    {
        String select = "select r._id,r.reviewer.person.fullName ";
        String from = "from ProposalCycle c ";
        String innerJoins = "inner join c.submittedProposals p inner join p.reviews r ";
        String where = "where c._id=" + cycleCode + " and p._id=" + submittedProposalId + " ";
        String orderBy = "order by r.reviewer.person.fullName";

        return getObjectIdentifiers(select + from + innerJoins + where + orderBy);
    }

    @GET
    @Path("/{reviewId}")
    @Operation(summary = "get the specific review of the given SubmittedProposal")
    public ProposalReview getReview(@PathParam("cycleCode") Long cycleCode,
                                    @PathParam("submittedProposalId") Long submittedProposalId,
                                    @PathParam("reviewId") Long reviewId)
    {
        return findChildByQuery(SubmittedProposal.class, ProposalReview.class,
                "reviews", submittedProposalId, reviewId);
    }

    @POST
    @Operation(summary = "add a new review to the given SubmittedProposal")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public ProposalReview addReview(@PathParam("cycleCode") Long cycleCode,
                                    @PathParam("submittedProposalId") Long submittedProposalId,
                                    ProposalReview proposalReview)
        throws WebApplicationException
    {
        //IMPL do not strictly need to do thi
        SubmittedProposal submittedProposal = findChildByQuery(ProposalCycle.class, SubmittedProposal.class,
                "submittedProposals", cycleCode, submittedProposalId);

        //set the date to the posix epoch, user must confirm that the review is complete at which
        //point this date is updated to that at the point of confirmation, and the review becomes
        //un-editable
        proposalReview.setReviewDate(new Date(0L));

        return addNewChildObject(submittedProposal, proposalReview, submittedProposal::addToReviews);
    }

    @DELETE
    @Path("/{reviewId}")
    @Operation(summary = "remove the specified review from the given SubmittedProposal")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response removeReview(@PathParam("cycleCode") Long cycleCode,
                                 @PathParam("submittedProposalId") Long reviewedProposalId,
                                 @PathParam("reviewId") Long reviewId)
        throws WebApplicationException
    {
        SubmittedProposal reviewedProposal = findChildByQuery(ProposalCycle.class,
              SubmittedProposal.class, "submittedProposals", cycleCode, reviewedProposalId);

        ProposalReview proposalReview = findChildByQuery(SubmittedProposal.class,
                ProposalReview.class, "reviews", reviewedProposalId, reviewId);

        return deleteChildObject(reviewedProposal,proposalReview,
                reviewedProposal::removeFromReviews);
    }

    /*
        Here we provide an API to edit certain individual fields of the ProposalReview class rather
        that updating via an object in its entirety to avoid users setting whatever completion
        date they like. The completion date can only be updated by the user confirming that the
        review is completed. Additionally, this avoids users changing the "reviewer" at will; to
        change a "reviewer" one must delete "this" review and add an entirely new one with the
        "reviewer" of choice.
     */

    @PUT
    @Path("/{reviewId}/comment")
    @Operation(summary = "update the review with a new comment")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public ProposalReview updateReviewComment(
            @PathParam("cycleCode") Long cycleCode,
            @PathParam("submittedProposalId") Long reviewedProposalId,
            @PathParam("reviewId") Long reviewId,
            String replacementComment
    )
        throws WebApplicationException
    {
        ProposalReview proposalReview = findChildByQuery(SubmittedProposal.class,
                ProposalReview.class, "reviews", reviewedProposalId, reviewId);

        if (proposalReview.getReviewDate().compareTo(new Date(0L)) > 0) {
            throw new WebApplicationException(
                    String.format(confirmed, proposalReview.getReviewDate().toString()),
                    400
            );
        }

        proposalReview.setComment(replacementComment);

        em.merge(proposalReview);

        return proposalReview;
    }

    @PUT
    @Path("/{reviewId}/score")
    @Operation(summary = "update the review with a new score")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public ProposalReview updateReviewScore(@PathParam("cycleCode") Long cycleCode,
                                            @PathParam("submittedProposalId") Long reviewedProposalId,
                                            @PathParam("reviewId") Long reviewId,
                                            Double replacementScore
    )
            throws WebApplicationException
    {
        ProposalReview proposalReview = findChildByQuery(SubmittedProposal.class,
                ProposalReview.class, "reviews", reviewedProposalId, reviewId);

        if (proposalReview.getReviewDate().compareTo(new Date(0L)) > 0) {
            throw new WebApplicationException(
                    String.format(confirmed, proposalReview.getReviewDate().toString()),
                    400
            );
        }

        proposalReview.setScore(replacementScore);

        em.merge(proposalReview);

        return proposalReview;
    }

    @PUT
    @Path("/{reviewId}/technicalFeasibility")
    @Operation(summary = "update the technical feasibility of the review")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public ProposalReview updateReviewFeasibility(
            @PathParam("cycleCode") Long cycleCode,
            @PathParam("submittedProposalId") Long reviewedProposalId,
            @PathParam("reviewId") Long reviewId,
            Boolean replacementFeasibility
    )
            throws WebApplicationException
    {
        ProposalReview proposalReview = findChildByQuery(SubmittedProposal.class,
                ProposalReview.class, "reviews", reviewedProposalId, reviewId);

        if (proposalReview.getReviewDate().compareTo(new Date(0L)) > 0) {
            throw new WebApplicationException(
                    String.format(confirmed, proposalReview.getReviewDate().toString()),
                    400
            );
        }

        proposalReview.setTechnicalFeasibility(replacementFeasibility);

        em.merge(proposalReview);

        return proposalReview;
    }

    //confirmation that the review is complete
    @PUT
    @Path("/{reviewId}/confirmReview")
    @Operation(summary = "confirm the review is complete; sets the review's completeDate to now, making the review un-editable")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public ProposalReview confirmReviewComplete(
            @PathParam("cycleCode") Long cycleCode,
            @PathParam("submittedProposalId") Long reviewedProposalId,
            @PathParam("reviewId") Long reviewId
    )
        throws WebApplicationException
    {
        ProposalReview proposalReview = findChildByQuery(SubmittedProposal.class,
                ProposalReview.class, "reviews", reviewedProposalId, reviewId);

        proposalReview.setReviewDate(new Date());

        em.merge(proposalReview);

        return proposalReview;
    }


}
