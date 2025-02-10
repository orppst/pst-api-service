package org.orph2020.pst.apiimpl.rest;

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
import org.ivoa.dm.proposal.prop.InvestigatorKind;
import org.ivoa.dm.proposal.prop.Person;
import org.ivoa.dm.proposal.prop.RelatedProposal;
import org.orph2020.pst.common.json.ObjectIdentifier;
import org.orph2020.pst.common.json.SubmittedProposalSynopsis;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Path("proposalsSubmitted")
@Tag(name="user-proposals-submitted")

public class UserProposalsSubmitted extends ObjectResourceBase {
    @Inject
    SubjectMapResource subjectMapResource;
    @Inject
    JsonWebToken userInfo;

    @GET
    @Operation(summary = "Get a list of synopsis for proposals submitted by the authenticated user optionally pass a cycle id, or include all cycles that have not passed")
    @RolesAllowed("default-roles-orppst")
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
    @RolesAllowed("default-roles-orppst")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response withdrawProposal(@PathParam("submittedProposalId") long submittedProposalId,
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

        //Has this been assigned reviewer(s)
        if(!submittedProposal.getReviews().isEmpty()) {
            throw new WebApplicationException("Reviews have already been submitted please contact the TAC",
                    Response.Status.CONFLICT);
        }

        //Withdraw from observing cycle
        ProposalCycle cycle = findObject(ProposalCycle.class, cycleCode);
        cycle.removeFromSubmittedProposals(submittedProposal);

        //TODO: Check for and clean up and orphaned objects and documents

        return responseWrapper("Withdrawn", 200);

    }

}
