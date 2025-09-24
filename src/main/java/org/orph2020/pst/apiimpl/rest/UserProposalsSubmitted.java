package org.orph2020.pst.apiimpl.rest;

import io.quarkus.mailer.MailTemplate;
import io.quarkus.qute.CheckedTemplate;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.management.ProposalCycle;
import org.ivoa.dm.proposal.management.SubmittedProposal;
import org.ivoa.dm.proposal.prop.Investigator;
import org.ivoa.dm.proposal.prop.InvestigatorKind;
import org.ivoa.dm.proposal.prop.Person;
import org.ivoa.dm.proposal.prop.RelatedProposal;
import org.orph2020.pst.common.json.ObjectIdentifier;
import org.orph2020.pst.common.json.SubmittedProposalMailData;
import org.orph2020.pst.common.json.SubmittedProposalSynopsis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Path("proposalsSubmitted")
@Tag(name="user-proposals-submitted")
@RolesAllowed("default-roles-orppst")
public class UserProposalsSubmitted extends ObjectResourceBase {
    @Inject
    ProposalCyclesResource proposalCyclesResource;
    @Inject
    SubjectMapResource subjectMapResource;
    @Inject
    JsonWebToken userInfo;
    @Inject
    ProposalDocumentStore proposalDocumentStore;

    @CheckedTemplate
    static class Templates {
        public static native
        MailTemplate.MailTemplateInstance
        confirmWithdrawal(SubmittedProposalMailData proposal);
    }

    @GET
    @Operation(summary = "Get a list of synopsis for proposals submitted by the authenticated user optionally pass a cycle id, or include all cycles that have not passed")
    public List<SubmittedProposalSynopsis> getProposalsSubmitted(@QueryParam("cycleId") long cycleId)
    {
        long personId = subjectMapResource.subjectMap(userInfo.getSubject()).getPerson().getId();
        List<SubmittedProposalSynopsis> listOfSubmitted = new ArrayList<>();

        String queryStr = "select distinct o._id,c._id "
                + "from SubmittedProposal o, Investigator inv, Investigator i, ProposalCycle c "
                + "where inv member of o.investigators and inv.person._id = " + personId + " "
                + "and i member of o.investigators "
                + "and o member of c.submittedProposals";

        //Filter either by observing cycle, or only new and current observing cycles.
        if (cycleId > 0)
            queryStr += " and c._id = " + cycleId;
        else
            queryStr += " and c.observationSessionEnd >= current_date()";

        Query query = em.createQuery(queryStr);
        List<ObjectIdentifier[]> results = query.getResultList();
        for (Object[] r : results) {
            SubmittedProposal prop = findObject(SubmittedProposal.class, (long)r[0]);
            List<RelatedProposal> sourcePropList = prop.getRelatedProposals();
            String status = "Awaiting";
            long sourcePropId = 0;
            if(!sourcePropList.isEmpty())
                sourcePropId = sourcePropList.get(0).getId();
            if(!prop.getReviews().isEmpty())
                status = "In review";
            if(prop.getSuccessful())
                status = "Success";
            listOfSubmitted.add(new SubmittedProposalSynopsis(
                    prop.getId(),               // db id
                    prop.getTitle(),            // title
                    prop.getSummary(),          // summary
                    prop.getKind(),             // kind
                    sourcePropId,               // source proposal db id
                    (long) r[1],                // cycle id
                    prop.getSubmissionDate(),   // submission date
                    status                      // current status
            ));
        }

        return listOfSubmitted;

    }

    @DELETE
    @Operation(summary = "Withdraw a submitted proposal from an observing cycle")
    @Path("{submittedProposalId}/withdraw")
    @Blocking
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Uni<Void> withdrawProposal(@PathParam("submittedProposalId") long submittedProposalId,
                                      @QueryParam("cycleId") long cycleCode)
        throws WebApplicationException
    {
        Person currentUser = subjectMapResource.subjectMap(userInfo.getSubject()).getPerson();
        SubmittedProposal submittedProposal = findObject(SubmittedProposal.class, submittedProposalId);

        //Check this person has rights to withdraw this submitted proposal
        AtomicBoolean foundPI = new AtomicBoolean(false);
        submittedProposal.getInvestigators().forEach(investigator -> {
            if(investigator.getType() == InvestigatorKind.PI
                    && investigator.getPerson() == currentUser)
                foundPI.set(true);
        });

        //Authenticated user is not associated with this submittedProposal.
        if(!foundPI.get()) {
            throw new WebApplicationException("You are not a PI on this submitted proposal", Response.Status.FORBIDDEN);
        }

        if (proposalCyclesResource.getProposalCycleDates(cycleCode).submissionDeadline.before(new Date())) {
            throw new WebApplicationException("You may not withdraw your proposal as the submission date has been surpassed. Please contact the TAC if you want to withdraw",
                    Response.Status.CONFLICT);
        }

        //Withdraw from observing cycle
        ProposalCycle cycle = findObject(ProposalCycle.class, cycleCode);
        cycle.removeFromSubmittedProposals(submittedProposal);

        //remove the document store for the submitted proposal (copied from the original proposal on submission)
        try {
            proposalDocumentStore.removeStorePath(String.valueOf(submittedProposalId));
        } catch (IOException e) {
            throw new WebApplicationException(e);
        }


        //gather data to send in an email confirming the withdrawal
        SubmittedProposalMailData mailData = new SubmittedProposalMailData(submittedProposal, cycle);

        List<Investigator> investigators = submittedProposal.getInvestigators();

        List<String> recipientEmails = new ArrayList<>();

        for (Investigator investigator : investigators) {
            recipientEmails.add(investigator.getPerson().getEMail());
        }

        return Templates.confirmWithdrawal(mailData)
                .to(recipientEmails.toArray(new String[0]))
                .subject("Confirmation of withdrawal of proposal '"
                        + submittedProposal.getTitle() + "' from observation cycle '"
                        + cycle.getTitle() + "'")
                .send();
    }

}
