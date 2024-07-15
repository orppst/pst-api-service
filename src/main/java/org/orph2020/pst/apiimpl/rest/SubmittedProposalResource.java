package org.orph2020.pst.apiimpl.rest;


import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.management.*;
import org.ivoa.dm.proposal.prop.ObservingProposal;
import org.orph2020.pst.common.json.ObjectIdentifier;
import org.orph2020.pst.common.json.ProposalSynopsis;

import java.util.Date;
import java.util.List;

@Path("proposalCycles/{cycleCode}/submittedProposals")
@Tag(name="proposalCycles-submitted-proposals")
@Produces(MediaType.APPLICATION_JSON)
public class SubmittedProposalResource extends ObjectResourceBase{

    @GET
    @Operation(summary = "get the identifiers for the SubmittedProposals in the ProposalCycle")
    public List<ObjectIdentifier> getSubmittedProposals(@PathParam("cycleCode") Long cycleCode)
    {
        String select = "select s._id,p.title ";
        String from = "from ProposalCycle c ";
        String innerJoins = "inner join c.submittedProposals s inner join s.proposal p ";
        String where = "where c._id=" + cycleCode + " ";
        String orderBy = "order by p.title";

        return getObjectIdentifiers(select + from + innerJoins + where + orderBy);
    }

    @GET
    @Path("/{submittedProposalId}")
    @Operation(summary = "get the SubmittedProposal specified by 'submittedProposalId'")
    public SubmittedProposal getSubmittedProposal(@PathParam("cycleCode") Long cycleCode,
                                                @PathParam("submittedProposalId") Long submittedProposalId)
    {
        return findChildByQuery(ProposalCycle.class, SubmittedProposal.class,
              "submittedProposals", cycleCode, submittedProposalId);
    }

    @GET
    @Path("/notYetAllocated")
    @Operation(summary = "get the Submitted Proposal Ids that have yet to be Allocated in the given cycle")
    public List<ObjectIdentifier> getSubmittedNotYetAllocated(@PathParam("cycleCode") Long cycleCode)
        throws WebApplicationException
    {
        ProposalCycle proposalCycle = findObject(ProposalCycle.class, cycleCode);

        List<SubmittedProposal> submittedProposals = proposalCycle.getSubmittedProposals();
        List<AllocatedProposal> allocatedProposals = proposalCycle.getAllocatedProposals();

        return submittedProposals
                .stream()
                .filter(sp -> allocatedProposals.stream().noneMatch(
                        ap -> sp.getId().equals(ap.getSubmitted().getId())))
                .map(sp -> new ObjectIdentifier(sp.getId(), sp.getProposal().getTitle()))
                .toList();
    }


    @PUT
    @Operation(summary = "submit a proposal")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public ProposalSynopsis submitProposal(@PathParam("cycleCode") long cycleId, long proposalId)
    {
        /*
            SubmittedProposals need to remember the original proposalId from which they create
            a clone; it is the clone to which the SubmittedProposal refers, NOT the original
            proposal. SubmittedProposals currently do not have a means to do this.

            In this way, we can check for proposals that have been re-submitted i.e., compare the
            'proposalId' input to the same in the list of SubmittedProposals, and remove any now stale
            SubmittedProposals and their clones, before adding a new SubmittedProposal with a new,
            updated clone.
         */

        ProposalCycle cycle =  findObject(ProposalCycle.class,cycleId);

        ObservingProposal proposal = findObject(ObservingProposal.class, proposalId);

        new ProposalManagementModel().createContext(); // TODO API subject to change
        ObservingProposal pclone = new ObservingProposal(proposal); // create clone TODO perhaps we should not create the clone
        pclone.updateClonedReferences();// TODO API subject to change
        pclone.setSubmitted(true);
        em.persist(pclone);
        //constructor args.:(submission date, successful, reviews-complete-date, reviews, the-proposal)
        SubmittedProposal submittedProposal = new SubmittedProposal(pclone, new Date(), false, new Date(0L), null );
        cycle.addToSubmittedProposals(submittedProposal);
        em.merge(cycle);

        //get the proposal we have just submitted
        List<SubmittedProposal> submittedProposals = cycle.getSubmittedProposals();
        ObservingProposal responseProposal =
                submittedProposals.get(submittedProposals.size() - 1).getProposal();

        return new ProposalSynopsis(responseProposal);
    }

    @PUT
    @Path("/{submittedProposalId}/success")
    @Operation(summary = "update the 'successful' status of the given SubmittedProposal")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateSubmittedProposalSuccess(@PathParam("cycleCode") Long cycleCode,
                                                  @PathParam("submittedProposalId") Long submittedProposalId,
                                                  Boolean successStatus)
          throws WebApplicationException
    {
        SubmittedProposal submittedProposal = findChildByQuery(ProposalCycle.class, SubmittedProposal.class,
              "submittedProposals", cycleCode, submittedProposalId);

        //the success state of a proposal may only be changed once ALL currently assigned reviews are complete
        boolean allReviewsComplete = true;
        for (ProposalReview review : submittedProposal.getReviews()) {
            if (review.getReviewDate().compareTo(new Date(0L)) == 0) {
                allReviewsComplete = false;
                break;
            }
        }

        if (!allReviewsComplete) {
            throw new WebApplicationException(
                    "All reviews must be complete before the 'successful' status can be updated", 400
            );
        }

        submittedProposal.setSuccessful(successStatus);

        return responseWrapper(submittedProposal, 200);
    }

    @PUT
    @Path("/{submittedProposalId}/completeDate")
    @Operation(summary = "update the 'reviewsCompleteDate' of the given SubmittedProposal to today's date")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateReviewsCompleteDate(
          @PathParam("cycleCode") Long cycleCode,
          @PathParam("submittedProposalId") Long submittedProposalId)
          throws WebApplicationException
    {
        SubmittedProposal submittedProposal = findChildByQuery(ProposalCycle.class, SubmittedProposal.class,
              "submittedProposals", cycleCode, submittedProposalId);

        //check that the reviews have actually been submitted before setting the complete date
        if (submittedProposal.getReviews().stream()
                .anyMatch(review -> review.getReviewDate().compareTo(new Date(0L)) == 0)) {
            throw new WebApplicationException(
                    "Not all reviews have been submitted", 400
            );
        }

        submittedProposal.setReviewsCompleteDate(new Date());

        return responseWrapper(submittedProposal, 200);
    }

}



