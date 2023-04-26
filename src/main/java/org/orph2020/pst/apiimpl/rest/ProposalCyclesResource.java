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

import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.List;

@Path("proposalCycles")
@Tag(name="proposalCycles")
@Produces(MediaType.APPLICATION_JSON)
public class ProposalCyclesResource extends ObjectResourceBase {
    private final Logger logger;


    public ProposalCyclesResource(Logger logger) {
        this.logger = logger;
    }


    @GET
    @Operation(summary = "List the ProposalCycles")
    public List<ObjectIdentifier> getProposals(@RestQuery boolean includeClosed) {
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
    @Path("{cycleCode}/submittedProposals")
    @Operation(summary = "List the Submitted Proposals")
    public List<ObjectIdentifier> getSubmittedProposals(@PathParam("cycleCode") long cycleId, @RestQuery String title) {
        if(title == null)
            return super.getObjects("SELECT o._id,o.proposal.title FROM SubmittedProposal o ORDER BY o.proposal.title"); //FIXME this returns all of the submitted proposals - should only be for the particular cycle
        else
            return super.getObjects("SELECT o._id,o.proposal.title FROM SubmittedProposal o Where o.proposal.title like '"+title+"' ORDER BY o.title");
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
    @Operation(summary = "List the Proposals being reviewed")
    public List<ObjectIdentifier> getReviewedProposals(@PathParam("cycleCode") long cycleId, @RestQuery String title) {
        if(title == null)
            return super.getObjects("SELECT o._id,o.submitted.proposal.title FROM ReviewedProposal o ORDER BY o.submitted.proposal.title");
        else
            return super.getObjects("SELECT o._id,o.submitted.proposal.title  FROM ReviewedProposal o Where o.submitted.proposal.title like '"+title+"' ORDER BY o.submitted.proposal.title");
    }


    @PUT
    @Operation(summary = "add a submitted proposal to review")
    @Path("{cycleCode}/proposalsInReview")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response submitProposalForReview(@PathParam("cycleCode") long cycleId, ReviewedProposal revIn)
    {
        ProposalCycle cycle =  findObject(ProposalCycle.class,cycleId);
        revIn.setSuccessful(false); // newly added so cannot be sucesssful yet.
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

    private ReviewedProposal findReviewedProposal(long cycleId, long revId) {
        TypedQuery<ReviewedProposal> q = em.createQuery("Select o From ProposalCycle c join c.reviewedProposals o where c._id = :cid and  o._id = :rid", ReviewedProposal.class);
        q.setParameter("cid", cycleId);
        q.setParameter("rid", revId);
        return q.getSingleResult();
    }

    @POST
    @Operation(summary = "add new review of proposal")
    @Path("{cycleCode}/proposalsInReview/{reviewCode}/reviews")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response submitProposalForReview(@PathParam("cycleCode") long cycleId, @PathParam("reviewCode") long revId, ProposalReview revIn)
    {
        ReviewedProposal revprop = findReviewedProposal(cycleId,revId);
        revIn.setReviewDate(new Date());
        revprop.addToReviews(revIn);
        return mergeObject(revprop);
    }
}
