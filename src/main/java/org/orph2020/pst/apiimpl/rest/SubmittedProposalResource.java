package org.orph2020.pst.apiimpl.rest;


import io.quarkus.mailer.MailTemplate;
import io.quarkus.qute.CheckedTemplate;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.management.*;
import org.ivoa.dm.proposal.prop.*;
import org.jboss.resteasy.reactive.RestQuery;
import org.orph2020.pst.apiimpl.ProposalCodeGenerator;
import org.orph2020.pst.apiimpl.entities.SubmissionConfiguration;
import org.orph2020.pst.common.json.ObjectIdentifier;
import org.orph2020.pst.common.json.SubmittedProposalMailData;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import io.quarkus.logging.Log;

@Path("proposalCycles/{cycleCode}/submittedProposals")
@Tag(name="proposalCycles-submitted-proposals")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({"default-roles-orppst"})
public class SubmittedProposalResource extends ObjectResourceBase{

    @Inject
    ProposalDocumentStore proposalDocumentStore;

    @Inject
    ProposalCodeGenerator   proposalCodeGenerator;

    @Inject
    JsonWebToken userInfo;

    @Inject
    SubjectMapResource subjectMapResource;

    //Justification header stuff
    String justificationsHeaderTemplate = "justificationsHeaderTemplate.tex";
    String justificationsHeader = "justificationsHeader.tex";

    @CheckedTemplate
    static class Templates {
        public static native
        MailTemplate.MailTemplateInstance
        tacReviewResults(SubmittedProposalMailData proposal);

        public static native
        MailTemplate.MailTemplateInstance
        confirmSubmittedProposal(SubmittedProposalMailData proposal);
    }

    @GET
    @Operation(summary = "get the identifiers for the SubmittedProposals in the ProposalCycle, note optional use of sourceProposalId overrides title and investigatorName")
    public List<ObjectIdentifier> getSubmittedProposals(
            @PathParam("cycleCode") Long cycleCode,
            @RestQuery String title,
            @RestQuery String investigatorName,
            @RestQuery Long sourceProposalId
    )
    {

        if (sourceProposalId != null) {
            String qlString = getQlString(cycleCode);
            Query query = em.createQuery(qlString);
            query.setParameter("sourceProposalId", sourceProposalId);
            return getObjectIdentifiersAlt(query);

        } else {

            String qlString = getQlString(cycleCode, title, investigatorName);
            Query query = em.createQuery(qlString);
            if (investigatorName != null) query.setParameter("investigatorName", investigatorName);
            if (title != null) query.setParameter("title", title);
            return getObjectIdentifiersAlt(query);

        }

    }

    private String getQlString(Long cycleCode) {
        String baseStr = "select distinct s._id,cast(s.submissionDate as string),s.title "
                + "from ProposalCycle c "
                + "inner join c.submittedProposals s "
                + "inner join s.relatedProposals r "
                + "where c._id=" + cycleCode + " "
                + "and r.proposal._id = :sourceProposalId ";

        String orderByStr = "order by s._id";

        return baseStr + orderByStr;
    }

    private static String getQlString(Long cycleCode, String title, String investigatorName) {
        String baseStr = "select distinct s._id,cast(s.submissionDate as string),s.title "
                + "from ProposalCycle c, Investigator i "
                + "inner join c.submittedProposals s "
                + "where i member of s.investigators "
                + "and c._id=" + cycleCode + " ";

        String orderByStr = "order by s.title";

        String investigatorLikeStr = investigatorName != null ?
                "and i.person.fullName like :investigatorName " : "";
        String titleLikeStr = title != null ?
                "and s.title like :title " : "";

        return baseStr + investigatorLikeStr + titleLikeStr + orderByStr;
    }

    @GET
    @Path("/{submittedProposalId}")
    @Operation(summary = "get the SubmittedProposal specified by 'submittedProposalId'")
    @RolesAllowed({"tac_admin", "tac_member"})
    public SubmittedProposal getSubmittedProposal(@PathParam("cycleCode") Long cycleCode,
                                                @PathParam("submittedProposalId") Long submittedProposalId)
    {
        return findChildByQuery(ProposalCycle.class, SubmittedProposal.class,
              "submittedProposals", cycleCode, submittedProposalId);
    }

    @GET
    @Path("/notYetAllocated")
    @RolesAllowed({"tac_admin", "tac_member"})
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
                .map(sp -> new ObjectIdentifier(sp.getId(), sp.getTitle()))
                .toList();
    }

    /*
        Work around: Java Dates seem to use local timezone i.e., new Date(0L) gives
        "1970-01-01 01:00:00" rather than "1970-01-01 00:00:00" - when creating Dates
        on a machine in the UK during the summer (BST).
     */
    private Boolean compareDatesOnly(Date a, Date b) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(a).equals(sdf.format(b));
    }

    @GET
    @Path("allReviewsLocked")
    @RolesAllowed({"tac_admin", "tac_member"})
    @Operation(summary = "check that the reviews for all submitted proposals have been locked")
    public boolean checkAllReviewsLocked(@PathParam("cycleCode") Long cycleCode)
        throws WebApplicationException
    {
        ProposalCycle proposalCycle = findObject(ProposalCycle.class, cycleCode);

        List<SubmittedProposal> submittedProposals = proposalCycle.getSubmittedProposals();

        return submittedProposals
                .stream()
                .noneMatch(sp -> compareDatesOnly(sp.getReviewsCompleteDate(), new Date(0L)));
    }


    @POST
    @Operation(summary = "submit a proposal")
    @Consumes(MediaType.APPLICATION_JSON)
    @Blocking
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response submitProposal(@PathParam("cycleCode") long cycleId, SubmissionConfiguration submissionConfiguration)
    {
        final long proposalId = submissionConfiguration.proposalId;
        ProposalCycle cycle =  findObject(ProposalCycle.class,cycleId);

        ObservingProposal proposal = findObject(ObservingProposal.class, proposalId);

        //Only a PI can submit this proposal
        Person currentUser = subjectMapResource.subjectMap(userInfo.getSubject()).getPerson();

        //Check this person has rights to withdraw this submitted proposal
        AtomicBoolean foundPI = new AtomicBoolean(false);
        proposal.getInvestigators().forEach(investigator -> {
            if(investigator.getType() == InvestigatorKind.PI
                    && investigator.getPerson() == currentUser)
                foundPI.set(true);
        });

        //Authenticated user is not associated with this submittedProposal.
        if(!foundPI.get()) {
            throw new WebApplicationException("You are not a PI on this proposal", Response.Status.FORBIDDEN);
        }

        List<ObservationConfiguration> configMappings = new ArrayList<>();
        for (SubmissionConfiguration.ObservationConfigMapping cm: submissionConfiguration.config)
        {
            ObservingMode mode = findObject(ObservingMode.class, cm.modeId);
            TypedQuery<Observation> obsquery = em.createQuery("select o from Observation o where o._id in :ids ", Observation.class);
            obsquery.setParameter("ids", cm.observationIds);
            List<Observation> observations = obsquery.getResultList();
            configMappings.add(new ObservationConfiguration(observations,mode));

        }

        new ProposalManagementModel().createContext(); // TODO API subject to change
        //constructor args.:(the-proposal, config, submission date, successful, reviews-complete-date, reviews)
        //FIXME need to gather the config

        // TODO Double check all references are updated correctly
        SubmittedProposal submittedProposal = new SubmittedProposal(proposal, proposalCodeGenerator.generateProposalCode(cycle), configMappings, new Date(), false, new Date(0L), null );
        submittedProposal.updateClonedReferences();
        em.persist(submittedProposal);
        submittedProposal.addToRelatedProposals(new RelatedProposal(proposal));

        //**** clone the document store of the original proposal ****
        //in essence creates a snapshot of the documents at the point of submission
        try {
            proposalDocumentStore.copyStore(
                    proposal.getId().toString(),
                    submittedProposal.getId().toString(),
                    submittedProposal.getSupportingDocuments()
            );
        } catch (IOException e) {
            // if we can't copy the store then we need to rollback
            throw new WebApplicationException(e);
        }
        //************************************************************

        //It is more convenient to replace both the proposal title and
        // cycle id placeholders in the 'justificationsHeaderTemplate.tex
        // file in one go, saving the result to 'justificationsHeader.tex',
        // than try to edit the header directly.
        try {
            insertTitleAndCycleCodeIntoHeader(submittedProposal.getId());
        } catch (IOException e) {
            throw new WebApplicationException(e);
        }

        cycle.addToSubmittedProposals(submittedProposal);
        em.merge(cycle);

        SubmittedProposalMailData mailData = new SubmittedProposalMailData(submittedProposal, cycle);

        List<Investigator> investigators = submittedProposal.getInvestigators();

        List<String> recipientEmails = new ArrayList<>();

        for (Investigator investigator : investigators) {
            recipientEmails.add(investigator.getPerson().getEMail());
        }

       Uni<Void> mail = Templates.confirmSubmittedProposal(mailData)
             .to(recipientEmails.toArray(new String[0]))
             .subject("Submission Confirmation of " + submittedProposal.getTitle() + " to " + cycle.getTitle())
             .send();

        mail.subscribe().with(
              item -> Log.info("submission mail sent"),
              error -> Log.error("submission mail failed", error)
        );


       return emptyResponse204();
    }


    @PUT
    @Path("/{submittedProposalId}/success")
    @Operation(summary = "update the 'successful' status of the given SubmittedProposal")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"tac_admin", "tac_member"})
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
    @RolesAllowed({"tac_admin", "tac_member"})
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

    @PUT
    @Path("/{submittedProposalId}/resetCompleteDate")
    @RolesAllowed({"tac_admin", "tac_member"})
    @Operation(summary = "reset the 'reviewsCompleteDate' of the given SubmittedProposal to the posix epoch")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response resetReviewsCompleteDate(
            @PathParam("cycleCode") Long cycleCode,
            @PathParam("submittedProposalId") Long submittedProposalId)
        throws WebApplicationException
    {
        SubmittedProposal submittedProposal = findChildByQuery(ProposalCycle.class, SubmittedProposal.class,
                "submittedProposals", cycleCode, submittedProposalId);

        //reset date to the posix epoch (use as an "undo" after setting the complete date)
        submittedProposal.setReviewsCompleteDate(new Date(0L));

        return responseWrapper(submittedProposal, 200);
    }

    @GET
    @Path("{submittedProposalId}/mailResults")
    @Operation(summary = "for the given submitted proposal in the cycle email all investigators with review details and success status")
    @Blocking
    @RolesAllowed({"tac_admin"})
    public Uni<Void> sendTACReviewResults(
            @PathParam("cycleCode") Long cycleCode,
            @PathParam("submittedProposalId") Long  submittedProposalId
    )
            throws WebApplicationException {

        SubmittedProposal submittedProposal = findChildByQuery(ProposalCycle.class, SubmittedProposal.class,
                "submittedProposals", cycleCode, submittedProposalId);

        //all dates are initialised to the posix epoch, meaning that if they equal to that date they've
        //yet to be updated.
        if (submittedProposal.getReviewsCompleteDate().compareTo(new Date(0L)) == 0) {
            throw new WebApplicationException(
                    "You may only send TAC result emails after the reviews have been finalised for this proposal"
            );
        }

        ProposalCycle cycle = findObject(ProposalCycle.class, cycleCode);

        SubmittedProposalMailData mailData = new SubmittedProposalMailData(submittedProposal, cycle);

        List<Investigator> investigators = submittedProposal.getInvestigators();

        List<String> recipientEmails = new ArrayList<>();

        for (Investigator investigator : investigators) {
            recipientEmails.add(investigator.getPerson().getEMail());
        }

        return Templates.tacReviewResults(mailData)
                .to(recipientEmails.toArray(new String[0]))
                .subject(cycle.getTitle() + " TAC review result for " + submittedProposal.getTitle())
                .send();
    }

    @PUT
    @Path("/{submittedProposalId}/replaceCode")
    @RolesAllowed({"tac_admin", "tac_member"})
    @Operation(summary = "update the 'proposalCode' of the given SubmittedProposal")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceCode(
            @PathParam("cycleCode") Long cycleCode,
            @PathParam("submittedProposalId") Long submittedProposalId,
            @QueryParam("proposalCode") String newProposalCode)
            throws WebApplicationException
    {
        SubmittedProposal submittedProposal = findChildByQuery(ProposalCycle.class, SubmittedProposal.class,
                "submittedProposals", cycleCode, submittedProposalId);

        submittedProposal.setProposalCode(newProposalCode);

        return responseWrapper(submittedProposal, 200);
    }

    //Convenience functions --------------

    private void insertTitleAndCycleCodeIntoHeader(
            Long proposalCode
    )
            throws IOException {
        String proposalTitleTarget = "PROPOSAL-TITLE-HERE";
        String cycleCodeTarget = "CYCLE-ID-HERE";

        //we control the input so we know that 'proposalCode' here belongs to a SubmittedProposal
        SubmittedProposal proposal = findObject(SubmittedProposal.class, proposalCode);
        String proposalTitle = proposal.getTitle();

        //read from this file
        File templateHeader = proposalDocumentStore.fetchFile(
                proposalDocumentStore.getJustificationsPath(proposalCode) + justificationsHeaderTemplate
        );

        //write to this file
        File header = proposalDocumentStore.fetchFile(
                proposalDocumentStore.getJustificationsPath(proposalCode) + justificationsHeader
        );

        String templateText = new String(Files.readAllBytes(templateHeader.toPath()));

        String headerText = templateText
                .replace(proposalTitleTarget, proposalTitle)
                .replace(cycleCodeTarget, proposal.getProposalCode());

        Files.write(header.toPath(), headerText.getBytes());
    }
}



