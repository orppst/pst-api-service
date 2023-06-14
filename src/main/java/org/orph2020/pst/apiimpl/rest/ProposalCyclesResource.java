package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 20/04/2023 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import io.quarkus.arc.All;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.management.*;
import org.ivoa.dm.proposal.prop.ObservingProposal;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestQuery;
import org.orph2020.pst.common.json.ObjectIdentifier;

import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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


    public ProposalCyclesResource(Logger logger) {
        this.logger = logger;
    }


    @GET
    @Operation(summary = "List the ProposalCycles")
    public List<ObjectIdentifier> getProposalCycles(@RestQuery boolean includeClosed) {
        if(includeClosed)
            return super.getObjects("SELECT o._id,o.title FROM ProposalCycle o ORDER BY o.title");
        else
            return super.getObjects("SELECT o._id,o.title FROM ProposalCycle o ORDER BY o.title");//FIXME actually do only return open
    }


    @GET
    @Path("{cycleCode}")
    @Operation(summary = "Get proposal cycle")
    public ProposalCycle getProposalCycle(@PathParam("cycleCode") long cycleId) {
        return findObject(ProposalCycle.class,cycleId);
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
        return getObjects("Select o._id,o.name from ProposalCycle p inner join p.possibleGrades o where p._id = '"+cycleCode+"' Order by o.name");
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
    @Operation(summary = "List the Submitted Proposals")
    public List<ObjectIdentifier> getSubmittedProposals(@PathParam("cycleCode") long cycleId, @RestQuery String title) {
        if(title == null)
            return super.getObjects("SELECT o._id,o.proposal.title FROM ProposalCycle p inner join p.submittedProposals o where p._id = '"+cycleId+"' ORDER BY o.proposal.title");
        else
            return super.getObjects("SELECT o._id,o.proposal.title FROM ProposalCycle p inner join p.submittedProposals o where p._id = '"+cycleId+"' and o.proposal.title like '"+title+"' ORDER BY o.proposal.title");
    }

    @PUT
    @Operation(summary = "submit a proposal")
    @Path("{cycleCode}/submittedProposals")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response submitProposal(@PathParam("cycleCode") long cycleId, long proposalId)
    {
        ProposalCycle cycle =  findObject(ProposalCycle.class,cycleId);
        ObservingProposal proposal = findObject(ObservingProposal.class, proposalId);
        ObservingProposal pclone = new ObservingProposal(proposal); // create clone TODO perhaps we should not create the clone
        pclone.setSubmitted(true);
        em.persist(pclone);
        SubmittedProposal submittedProposal = new SubmittedProposal(new Date(),pclone);
        cycle.addToSubmittedProposals(submittedProposal);
        return mergeObject(cycle);
    }

    @GET
    @Path("{cycleCode}/proposalsInReview")
    @Operation(summary = "List the identifiers for the Proposals being reviewed")
    public List<ObjectIdentifier> getReviewedProposals(@PathParam("cycleCode") long cycleId, @RestQuery String title) {
        if(title == null)
            return getObjects("SELECT o._id,o.submitted.proposal.title FROM ProposalCycle p inner join p.reviewedProposals o WHERE p._id = '"+cycleId+"' ORDER BY o.submitted.proposal.title");
        else
            return getObjects("SELECT o._id,o.submitted.proposal.title FROM ProposalCycle p inner join p.reviewedProposals o Where p._id = '"+cycleId+"' and o.submitted.proposal.title like '"+title+"' ORDER BY o.submitted.proposal.title");
    }


    @POST
    @Operation(summary = "add a submitted proposal to review")
    @Path("{cycleCode}/proposalsInReview")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response submitProposalForReview(@PathParam("cycleCode") long cycleId, ReviewedProposal revIn)
    {
        ProposalCycle cycle =  findObject(ProposalCycle.class, cycleId);
        revIn.setSuccessful(false); // newly added so cannot be successful yet.
        cycle.addToReviewedProposals(revIn);
        return mergeObject(cycle);
    }

    @GET
    @Operation(summary = "get a proposal in review")
    @Path("{cycleCode}/proposalsInReview/{reviewCode}")
    public ReviewedProposal getReviewedProposal(@PathParam("cycleCode") long cycleId, @PathParam("reviewCode") long revId)
    {
        return findReviewedProposal(cycleId, revId);
    }

    @POST
    @Operation(summary = "add new review of proposal")
    @Path("{cycleCode}/proposalsInReview/{reviewCode}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public ProposalReview submitReviewOfProposal(@PathParam("cycleCode") long cycleId, @PathParam("reviewCode") long revId, ProposalReview revIn)
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
            return getObjects("SELECT o._id,o.submitted.proposal.title FROM ProposalCycle p inner join p.allocatedProposals o where p._id = '"+cycleId+"' ORDER BY o.submitted.proposal.title");
        } else {
            return getObjects("SELECT o._id,o.submitted.proposal.title FROM ProposalCycle p inner join p.allocatedProposals o where p._id = '"+cycleId+"' and o.submitted.proposal.title like '"+title+"' ORDER BY o.submitted.proposal.title");
        }
    }


    @PUT
    @Path("{cycleCode}/allocatedProposals")
    @Operation(summary = "add a submitted proposal to the list of AllocatedProposals in the cycle")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response allocatedProposalToCycle(@PathParam("cycleCode") long cycleCode,
                                                      long submittedId)
            throws WebApplicationException
    {
        AllocatedProposal allocatedProposal = new AllocatedProposal(new ArrayList<>(),
                findSubmittedProposal(cycleCode, submittedId));

        ProposalCycle cycle = findObject(ProposalCycle.class, cycleCode);

        cycle.addToAllocatedProposals(allocatedProposal);

        return mergeObject(cycle);

        //return addNewChildObject(cycle, allocatedProposal, cycle::addToAllocatedProposals);
    }

    @POST
    @Path("{cycleCode}/allocatedProposals/{allocatedId}")
    @Operation(summary = "add an AllocationBlock to the AllocatedProposal in the ProposalCycle")
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
