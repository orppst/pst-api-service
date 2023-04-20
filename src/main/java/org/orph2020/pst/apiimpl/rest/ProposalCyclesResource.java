package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 20/04/2023 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.management.ProposalCycle;
import org.ivoa.dm.proposal.management.SubmittedProposal;
import org.ivoa.dm.proposal.prop.ObservingProposal;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestQuery;
import org.orph2020.pst.common.json.ObjectIdentifier;

import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.List;

@Path("proposalCycles")
@Tag(name="The proposalCycles")
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
    @Path("{cycleCode}/submittedProposals")
    @Operation(summary = "List the Submitted Proposals")
    public List<ObjectIdentifier> getProposals(@PathParam("cycleCode") long cycleId, @RestQuery String title) {
        if(title == null)
            return super.getObjects("SELECT o._id,o.title FROM SubmittedProposal o ORDER BY o.title");
        else
            return super.getObjects("SELECT o._id,o.title FROM SubmittedProposal o Where o.title like '"+title+"' ORDER BY o.title");
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
        em.detach(proposal);
        SubmittedProposal submittedProposal = new SubmittedProposal(new Date(), proposal);
        cycle.addSubmittedProposals(submittedProposal);
        return mergeObject(cycle);
    }

}
