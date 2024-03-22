package org.orph2020.pst.apiimpl.rest;

import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.management.CommitteeMember;
import org.ivoa.dm.proposal.management.ProposalCycle;
import org.ivoa.dm.proposal.management.TAC;
import org.ivoa.dm.proposal.management.TacRole;
import org.jboss.resteasy.reactive.RestQuery;
import org.orph2020.pst.common.json.ObjectIdentifier;

import java.util.ArrayList;
import java.util.List;

@Path("proposalCycles/{cycleCode}/TAC")
@Tag(name="time-allocation-committee")
@Produces(MediaType.APPLICATION_JSON)
public class TACResource extends ObjectResourceBase {

    @GET
    @Operation(summary = "Get the CommitteeMembers (of the TAC) identifiers")
    public List<ObjectIdentifier> getCommitteeMembers(@PathParam("cycleCode") Long cycleCode,
                                                      @RestQuery String personName,
                                                      @RestQuery TacRole memberRole)
    {

        String nameLike = (personName == null) ? "" :
                "and m.member.person.name = :pName ";

        String roleLike = (memberRole == null) ? "" :
                "and m.role like '" + memberRole + "' ";

        String qlString = "select m._id,m.role,m.member.person.name from ProposalCycle p "
                + "inner join p.tac t inner join t.members m "
                + "where p._id=" + cycleCode + " "
                + nameLike + roleLike + "order by m.role";

        Query query = em.createQuery(qlString);

        if (personName != null)  query.setParameter("pName", personName);

        //using the 3 argument ObjectIdentifier constructor

        List<ObjectIdentifier> result = new ArrayList<>();
        List<Object[]> results = query.getResultList();
        for (Object[] r : results) {
            result.add(new ObjectIdentifier((Long)r[0], (String)r[1], (String)r[2]));
        }
        return result;
    }

    @GET
    @Path("/{memberId}")
    public CommitteeMember getCommitteeMember(@PathParam("cycleCode") Long cycleCode,
                                              @PathParam("memberId") Long memberId)
            throws WebApplicationException
    {
        ProposalCycle proposalCycle = findObject(ProposalCycle.class, cycleCode);

        return proposalCycle.getTac().getMembers()
                .stream().filter(m -> memberId.equals(m.getId())).findAny()
                .orElseThrow(() -> new WebApplicationException(
                        String.format(NON_ASSOCIATE_ID, "Committee Member", memberId, "Proposal Cycle", cycleCode)
                ));
    }

    @POST
    @Operation(summary = "Add a new CommitteeMember to the TAC")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public CommitteeMember addCommitteeMember(@PathParam("cycleCode") Long cycleCode,
                                              CommitteeMember committeeMember)
        throws WebApplicationException
    {
        ProposalCycle proposalCycle = findObject(ProposalCycle.class, cycleCode);

        TAC tac = proposalCycle.getTac();
        //CommitteeMember contains a reference to a Reviewer, which in turn contains a reference to a Person
        return addNewChildObject(tac, new CommitteeMember(committeeMember), tac::addToMembers);
    }

    @DELETE
    @Path("/{memberId}")
    @Operation(summary = "Remove the CommitteeMember specified by 'memberId' from the TAC")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response removeCommitteeMember(@PathParam("cycleCode") Long cycleCode,
                                          @PathParam("memberId") Long memberId)
        throws WebApplicationException
    {
        ProposalCycle proposalCycle = findObject(ProposalCycle.class, cycleCode);

        TAC tac = proposalCycle.getTac();

        CommitteeMember committeeMember = tac.getMembers()
                .stream().filter(m -> memberId.equals(m.getId())).findAny()
                .orElseThrow(() -> new WebApplicationException(
                        String.format(NON_ASSOCIATE_ID, "Committee Member", memberId, "Proposal Cycle", cycleCode)
                ));


        return deleteChildObject(tac, committeeMember, tac::removeFromMembers);
    }


    @PUT
    @Path("/{memberId}/role")
    @Operation(summary = "Edit the role of the CommitteeMember specified by 'memberId'")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public CommitteeMember replaceRole(@PathParam("cycleCode") Long cycleCode,
                                       @PathParam("memberId") Long memberId,
                                       TacRole replacementRole)
        throws WebApplicationException
    {
        ProposalCycle proposalCycle = findObject(ProposalCycle.class, cycleCode);

        TAC tac = proposalCycle.getTac();

        CommitteeMember committeeMember = tac.getMembers()
                .stream().filter(m -> memberId.equals(m.getId())).findAny()
                .orElseThrow(() -> new WebApplicationException(
                        String.format(NON_ASSOCIATE_ID, "Committee Member", memberId, "Proposal Cycle", cycleCode)
                ));

        committeeMember.setRole(replacementRole);

        return committeeMember;
    }


}
