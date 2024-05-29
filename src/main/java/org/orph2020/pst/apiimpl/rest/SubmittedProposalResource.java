package org.orph2020.pst.apiimpl.rest;


import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
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
        SubmittedProposal submittedProposal = new SubmittedProposal(new Date(), false, new Date(), pclone);
        cycle.addToSubmittedProposals(submittedProposal);
        em.merge(cycle);

        //get the proposal we have just submitted
        List<SubmittedProposal> submittedProposals = cycle.getSubmittedProposals();
        ObservingProposal responseProposal =
                submittedProposals.get(submittedProposals.size() - 1).getProposal();

        return new ProposalSynopsis(responseProposal);
    }


}
