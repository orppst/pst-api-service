package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 20/04/2023 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.management.*;
import org.ivoa.dm.proposal.prop.ObservingProposal;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestQuery;
import org.orph2020.pst.common.json.ObjectIdentifier;
import org.orph2020.pst.common.json.ProposalCycleDates;
import org.orph2020.pst.common.json.ProposalSynopsis;

import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.*;

@Path("proposalCycles")
@Tag(name="proposalCycles")
@Produces(MediaType.APPLICATION_JSON)
public class ProposalCyclesResource extends ObjectResourceBase {
    private final Logger logger;

    private ReviewedProposal findReviewedProposal(long cycleId, long revId) {
        TypedQuery<ReviewedProposal> q = em.createQuery(
                "Select o From ProposalCycle c join c.reviewedProposals o where c._id = :cid and  o._id = :rid",
                ReviewedProposal.class
        );
        q.setParameter("cid", cycleId);
        q.setParameter("rid", revId);
        return q.getSingleResult();
    }

    private SubmittedProposal findSubmittedProposal(long cycleId, long submittedId) {
        TypedQuery<SubmittedProposal> q = em.createQuery(
                "Select o From ProposalCycle c join c.submittedProposals o where c._id = :cid and o._id = :sid",
                SubmittedProposal.class
        );
        q.setParameter("cid", cycleId);
        q.setParameter("sid", submittedId);
        return q.getSingleResult();
    }

    private AllocatedProposal findAllocatedProposal(long cycleId, long allocatedId) {
        TypedQuery<AllocatedProposal> q = em.createQuery(
                "Select o From ProposalCycle c join c.allocatedProposals o where c._id = :cid and o._id = :aid",
                AllocatedProposal.class
        );
        q.setParameter("cid", cycleId);
        q.setParameter("aid", allocatedId);
        return q.getSingleResult();
    }

    private AllocationGrade findAllocatedGrade(long cycleId, long gradeId) {
        TypedQuery<AllocationGrade> q = em.createQuery(
                "Select o From ProposalCycle c join c.possibleGrades o where c._id = :cid and o._id = :gid",
                AllocationGrade.class
        );
        q.setParameter("cid", cycleId);
        q.setParameter("gid", gradeId);
        return q.getSingleResult();
    }


    private ProposalSynopsis createSynopsisFromProposal(ObservingProposal proposal) {
        return new ProposalSynopsis(
                proposal.getId(), proposal.getTitle(), proposal.getSummary(), proposal.getKind(),
                proposal.getSubmitted()
        );
    }


    public ProposalCyclesResource(Logger logger) {
        this.logger = logger;
    }


    @GET
    @Operation(summary = "List the ProposalCycles")
    public List<ObjectIdentifier> getProposalCycles(@RestQuery boolean includeClosed) {
        if(includeClosed)
            return super.getObjectIdentifiers("SELECT o._id,o.title FROM ProposalCycle o ORDER BY o.title");
        else
            return super.getObjectIdentifiers("SELECT o._id,o.title FROM ProposalCycle o ORDER BY o.title");//FIXME actually do only return open
    }


    @GET
    @Path("{cycleCode}")
    @Operation(summary = "Get proposal cycle")
    public ProposalCycle getProposalCycle(@PathParam("cycleCode") long cycleId) {
        return findObject(ProposalCycle.class,cycleId);
    }

    @GET
    @Path("{cycleCode}/dates")
    @Operation(summary = "Get the dates associated with a given ProposalCycle")
    public ProposalCycleDates getProposalCycleDates(@PathParam("cycleCode") long cycleId) {
        ProposalCycle fullCycle =  findObject(ProposalCycle.class,cycleId);
        return new ProposalCycleDates(fullCycle.getTitle(), fullCycle.getSubmissionDeadline(),
                fullCycle.getObservationSessionStart(), fullCycle.getObservationSessionEnd());
    }

    @GET
    @Path("{cycleCode}/TAC")
    @Operation(summary = "Get the time allocation committee")
    public TAC getTAC(@PathParam("cycleCode") long cycleId) {
        return findObject(ProposalCycle.class,cycleId).getTac();
    }

    @GET
    @Path("{cycleCode}/grades")
    @Operation(summary = "List the possible grades of the given ProposalCycle")
    public List<ObjectIdentifier> getCycleAllocationGrades(@PathParam("cycleCode") long cycleCode) {
        return getObjectIdentifiers("Select o._id,o.name from ProposalCycle p inner join p.possibleGrades o where p._id = "+cycleCode+" Order by o.name");
    }

    @GET
    @Path("{cycleCode}/grades/{gradeId}")
    @Operation(summary = "get the specific grade associated with the given ProposalCycle")
    public AllocationGrade getCycleAllocatedGrade(@PathParam("cycleCode") long cycleCode,
                                                  @PathParam("gradeId") long gradeId) {
        return findAllocatedGrade(cycleCode, gradeId);
    }


    @GET
    @Path("{cycleCode}/submittedProposals")
    @Operation(summary = "list the SubmittedProposals")
    public List<ObjectIdentifier> getSubmittedProposals(@PathParam("cycleCode") long cycleId,
                                                        @RestQuery String title) {
        if(title == null)
            return getObjectIdentifiers("SELECT o._id,o.proposal.title FROM ProposalCycle p inner join p.submittedProposals o where p._id = "+cycleId+" ORDER BY o.proposal.title");
        else
            return getObjectIdentifiers("SELECT o._id,o.proposal.title FROM ProposalCycle p inner join p.submittedProposals o where p._id = "+cycleId+" and o.proposal.title like '"+title+"' ORDER BY o.proposal.title");
    }

    @PUT
    @Operation(summary = "submit a proposal")
    @Path("{cycleCode}/submittedProposals")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response submitProposal(@PathParam("cycleCode") long cycleId, long proposalId)
    {
        //tried using addNewChildObject() here but persistence layer throws an error

        ProposalCycle cycle =  findObject(ProposalCycle.class,cycleId);
        ObservingProposal proposal = findObject(ObservingProposal.class, proposalId);
        ObservingProposal pclone = new ObservingProposal(proposal); // create clone TODO perhaps we should not create the clone
        pclone.setSubmitted(true);
        em.persist(pclone);
        SubmittedProposal submittedProposal = new SubmittedProposal(new Date(),pclone);
        cycle.addToSubmittedProposals(submittedProposal);
        em.merge(cycle);

        //get the proposal we have just submitted
        List<SubmittedProposal> submittedProposals = cycle.getSubmittedProposals();
        ObservingProposal responseProposal =
                submittedProposals.get(submittedProposals.size() - 1).getProposal();

        return responseWrapper(createSynopsisFromProposal(responseProposal), 201);
    }

    @GET
    @Path("{cycleCode}/proposalsInReview")
    @Operation(summary = "List the identifiers for the Proposals under review")
    public List<ObjectIdentifier> getReviewedProposals(@PathParam("cycleCode") long cycleId,
                                                       @RestQuery String title) {
        if(title == null)
            return getObjectIdentifiers("SELECT o._id,o.submitted.proposal.title FROM ProposalCycle p inner join p.reviewedProposals o WHERE p._id = "+cycleId+" ORDER BY o.submitted.proposal.title");
        else
            return getObjectIdentifiers("SELECT o._id,o.submitted.proposal.title FROM ProposalCycle p inner join p.reviewedProposals o Where p._id = "+cycleId+" and o.submitted.proposal.title like '"+title+"' ORDER BY o.submitted.proposal.title");
    }


    @POST
    @Operation(summary = "upgrade a submitted proposal to a proposal under review")
    @Path("{cycleCode}/proposalsInReview")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response submitProposalForReview(@PathParam("cycleCode") long cycleId, ReviewedProposal revIn)
    {
        ProposalCycle cycle =  findObject(ProposalCycle.class, cycleId);
        revIn.setSuccessful(false); // newly added so cannot be successful yet.
        cycle.addToReviewedProposals(revIn);
        em.merge(cycle);

        //get the proposal we have just added to the 'reviewed' list
        List<ReviewedProposal> reviewedProposals = cycle.getReviewedProposals();
        ObservingProposal responseProposal = reviewedProposals.get(reviewedProposals.size() - 1)
                .getSubmitted().getProposal();

        return responseWrapper(createSynopsisFromProposal(responseProposal), 201);
    }

    @GET
    @Operation(summary = "get a specific proposal under review")
    @Path("{cycleCode}/proposalsInReview/{reviewCode}")
    public ReviewedProposal getReviewedProposal(@PathParam("cycleCode") long cycleId,
                                                @PathParam("reviewCode") long revId)
    {
        return findReviewedProposal(cycleId, revId);
    }

    @POST
    @Operation(summary = "add new review of proposal")
    @Path("{cycleCode}/proposalsInReview/{reviewCode}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public ProposalReview submitReviewOfProposal(@PathParam("cycleCode") long cycleId,
                                                 @PathParam("reviewCode") long revId,
                                                 ProposalReview revIn)
    {
        ReviewedProposal reviewedProposal = findReviewedProposal(cycleId,revId);
        revIn.setReviewDate(new Date());

        return addNewChildObject(reviewedProposal, revIn, reviewedProposal::addToReviews);
    }

    @GET
    @Path("{cycleCode}/allocatedProposals")
    @Operation(summary = "get identifiers for all the AllocatedProposals in the given ProposalCycle, optionally provide a proposal title to get a specific identifier ")
    public List<ObjectIdentifier> getAllocatedProposalsFromCycle(@PathParam("cycleCode") long cycleId,
                                                                 @RestQuery String title) {
        if (title == null) {
            return getObjectIdentifiers("SELECT o._id,o.submitted.proposal.title FROM ProposalCycle p inner join p.allocatedProposals o where p._id = "+cycleId+" ORDER BY o.submitted.proposal.title");
        } else {
            return getObjectIdentifiers("SELECT o._id,o.submitted.proposal.title FROM ProposalCycle p inner join p.allocatedProposals o where p._id = "+cycleId+" and o.submitted.proposal.title like '"+title+"' ORDER BY o.submitted.proposal.title");
        }
    }


    @PUT
    @Path("{cycleCode}/allocatedProposals")
    @Operation(summary = "upgrade a proposal under review to an allocated proposal")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response allocateProposalToCycle(@PathParam("cycleCode") long cycleCode,
                                                      long submittedId)
            throws WebApplicationException
    {
        AllocatedProposal allocatedProposal = new AllocatedProposal(new ArrayList<>(),
                findSubmittedProposal(cycleCode, submittedId));

        ProposalCycle cycle = findObject(ProposalCycle.class, cycleCode);

        cycle.addToAllocatedProposals(allocatedProposal);

        em.merge(cycle);

        //get the allocated proposal just added to the 'allocated' list
        List<AllocatedProposal> allocatedProposals = cycle.getAllocatedProposals();
        ObservingProposal responseValue = allocatedProposals.get(allocatedProposals.size() - 1)
                .getSubmitted().getProposal();

        return responseWrapper(createSynopsisFromProposal(responseValue), 201);
    }

    @POST
    @Path("{cycleCode}/allocatedProposals/{allocatedId}")
    @Operation(summary = "add an AllocationBlock to the specific AllocatedProposal")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public AllocatedBlock addAllocatedBlockToAllocatedProposal(@PathParam("cycleCode") long cycleCode,
                                                               @PathParam("allocatedId") long allocatedId,
                                                               AllocatedBlock allocatedBlock)
        throws WebApplicationException
    {
        AllocatedProposal allocatedProposal = findAllocatedProposal(cycleCode, allocatedId);

        return addNewChildObject(allocatedProposal, allocatedBlock, allocatedProposal::addToAllocation);
    }
}
