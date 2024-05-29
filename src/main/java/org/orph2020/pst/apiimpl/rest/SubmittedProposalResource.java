package org.orph2020.pst.apiimpl.rest;


import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.management.ProposalCycle;
import org.ivoa.dm.proposal.management.ProposalManagementModel;
import org.ivoa.dm.proposal.management.SubmittedProposal;
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
    @Path("/{reviewedProposalId}")
    @Operation(summary = "get the ReviewedProposal specified by 'reviewedProposalId'")
    public SubmittedProposal getReviewedProposal(@PathParam("cycleCode") Long cycleCode,
                                                @PathParam("reviewedProposalId") Long reviewedProposalId)
    {
        return findChildByQuery(ProposalCycle.class, SubmittedProposal.class,
              "submittedProposals", cycleCode, reviewedProposalId);
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
        SubmittedProposal submittedProposal = new SubmittedProposal(new Date(), false, new Date(), null, pclone);
        cycle.addToSubmittedProposals(submittedProposal);
        em.merge(cycle);

        //get the proposal we have just submitted
        List<SubmittedProposal> submittedProposals = cycle.getSubmittedProposals();
        ObservingProposal responseProposal =
                submittedProposals.get(submittedProposals.size() - 1).getProposal();

        return new ProposalSynopsis(responseProposal);
    }

    @PUT
    @Path("/{reviewedProposalId}/success")
    @Operation(summary = "update the 'successful' status of the given ReviewedProposal")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateReviewedProposalSuccess(@PathParam("cycleCode") Long cycleCode,
                                                  @PathParam("reviewedProposalId") Long reviewedProposalId,
                                                  Boolean successStatus)
          throws WebApplicationException
    {
        SubmittedProposal reviewedProposal = findChildByQuery(ProposalCycle.class, SubmittedProposal.class,
              "submittedProposals", cycleCode, reviewedProposalId);

        reviewedProposal.setSuccessful(successStatus);



        return mergeObject(reviewedProposalId);
    }

    @PUT
    @Path("/{reviewedProposalId}/completeDate")
    @Operation(summary = "update the 'reviewsCompleteDate' of the given ReviewedProposal to today's date")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateReviewedProposalCompleteDate(
          @PathParam("cycleCode") Long cycleCode,
          @PathParam("reviewedProposalId") Long reviewedProposalId)
          throws WebApplicationException
    {
        SubmittedProposal reviewedProposal = findChildByQuery(ProposalCycle.class, SubmittedProposal.class,
              "submittedProposals", cycleCode, reviewedProposalId);

        reviewedProposal.setReviewsCompleteDate(new Date());

        return mergeObject(reviewedProposalId);
    }

}



