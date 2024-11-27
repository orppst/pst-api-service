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
import org.ivoa.dm.proposal.management.AllocatedProposal;
import org.ivoa.dm.proposal.management.SubmittedProposal;
import org.ivoa.dm.proposal.prop.ProposalKind;
import org.ivoa.dm.proposal.prop.RelatedProposal;
import org.orph2020.pst.common.json.ObjectIdentifier;
import org.orph2020.pst.common.json.SubmittedProposalSynopsis;

import java.util.ArrayList;
import java.util.List;

@Path("proposalsSubmitted")
@Tag(name="user-proposals-submitted")

public class UserProposalsSubmitted extends ObjectResourceBase {
    @Inject
    SubjectMapResource subjectMapResource;
    @Inject
    JsonWebToken userInfo;

    @GET
    @Operation(summary = "Get a list of synopsis for proposals submitted by the authenticated user")
    @RolesAllowed("default-roles-orppst")
    public List<SubmittedProposalSynopsis> getProposalsSubmitted()
    {
        long personId = subjectMapResource.subjectMap(userInfo.getSubject()).getPerson().getId();
        List<SubmittedProposalSynopsis> listOfSubmitted = new ArrayList<>();

        String queryStr1 = "select distinct o._id,o.title,o.summary,o.kind,c._id "
                + "from SubmittedProposal o, ProposalCycle c "
                + "where o member of c.submittedProposals";

        String queryStr = "select distinct o._id,c._id "
                + "from SubmittedProposal o, Investigator inv, Investigator i, ProposalCycle c "
                + "where inv member of o.investigators and inv.person._id = " + personId + " "
                + "and i member of o.investigators "
                + "and o member of c.submittedProposals";

        Query query = em.createQuery(queryStr);
        List<ObjectIdentifier[]> results = query.getResultList();
        for (Object[] r : results) {
            SubmittedProposal prop = findObject(SubmittedProposal.class, (long)r[0]);
            List<RelatedProposal> sourcePropList = prop.getRelatedProposals();
            String status = "Waiting";
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

    @GET
    @Operation(summary = "Withdraw a submitted proposal from an observing cycle")
    @Path("{submittedProposalId}/withdraw")
    @RolesAllowed("default-roles-orppst")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response withdrawProposal(@PathParam("submittedProposalId") long submittedProposalId,
                                     @QueryParam("cycleId") long cycleCode)
        throws WebApplicationException
    {
        long personId = subjectMapResource.subjectMap(userInfo.getSubject()).getPerson().getId();
        SubmittedProposal submittedProposal = findObject(SubmittedProposal.class, submittedProposalId);

        //Check this person has rights to withdraw this submitted proposal

        //Check if the cycle deadline has passed

        //Withdraw from observing cycle & delete it
        //AllocatedProposal allocatedProposal = findChildByQuery(AllocatedProposal.class, SubmittedProposal.class, "submitted_ID", cycleCode, submittedProposalId);

        submittedProposal.getObservations().forEach(observation -> em.remove(observation));
        return removeObject(SubmittedProposal.class, submittedProposalId);

    }

}
