package org.orph2020.pst.apiimpl.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.persistence.Query;
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

import java.util.List;

@Path("proposalCycles/{cycleCode}/TAC")
@Tag(name="proposalCycles-time-allocation-committee")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({"tac_admin", "tac_member", "obs_administration"})
public class TACResource extends ObjectResourceBase {

    @Inject
    ProposalCyclesResource proposalCyclesResource;

    @GET
    @Operation(summary = "get the TAC object for the given proposal cycle")
    public TAC getTAC(@PathParam("cycleCode") Long cycleCode)
    {
        return findObject(ProposalCycle.class, cycleCode).getTac();
    }


    @GET
    @Path("/members")
    @Operation(summary = "Get the CommitteeMembers (of the TAC) identifiers")
    public List<ObjectIdentifier> getCommitteeMembers(@PathParam("cycleCode") Long cycleCode,
                                                      @RestQuery String personName,
                                                      @RestQuery TacRole memberRole)
    {

        String nameLike = (personName == null) ? "" :
                "and m.member.person.fullName = :pName ";

        String roleLike = (memberRole == null) ? "" :
                "and m.role = :mRole ";

        String qlString = "select m._id,cast(m.role as string),m.member.person.fullName from ProposalCycle p "
                + "inner join p.tac t inner join t.members m "
                + "where p._id=" + cycleCode + " "
                + nameLike + roleLike + "order by m.role";

        Query query = em.createQuery(qlString);

        if (personName != null) query.setParameter("pName", personName);
        if (memberRole != null) query.setParameter("mRole", memberRole);

        //using the 3 argument ObjectIdentifier constructor

        return getObjectIdentifiersAlt(query);
    }

    @GET
    @Path("/members/{memberId}")
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
    @Path("/members")
    @Operation(summary = "Add a new CommitteeMember to the TAC")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    @RolesAllowed({"tac_admin"})
    public CommitteeMember addCommitteeMember(@PathParam("cycleCode") Long cycleCode,
                                              CommitteeMember committeeMember)
        throws WebApplicationException
    {
        ProposalCycle proposalCycle = findObject(ProposalCycle.class, cycleCode);

        proposalCyclesResource.checkUserOnTAC(proposalCycle);

        TAC tac = proposalCycle.getTac();
        //CommitteeMember contains a reference to a Reviewer, which in turn contains a reference to a Person
        return addNewChildObject(tac, new CommitteeMember(committeeMember), tac::addToMembers);
    }

    @DELETE
    @Path("/members/{memberId}")
    @Operation(summary = "Remove the CommitteeMember specified by 'memberId' from the TAC")
    @Transactional(rollbackOn = {WebApplicationException.class})
    @RolesAllowed({"tac_admin"})
    public Response removeCommitteeMember(@PathParam("cycleCode") Long cycleCode,
                                          @PathParam("memberId") Long memberId)
        throws WebApplicationException
    {
        ProposalCycle proposalCycle = findObject(ProposalCycle.class, cycleCode);

        proposalCyclesResource.checkUserOnTAC(proposalCycle);

        TAC tac = proposalCycle.getTac();

        CommitteeMember committeeMember = tac.getMembers()
                .stream().filter(m -> memberId.equals(m.getId())).findAny()
                .orElseThrow(() -> new WebApplicationException(
                        String.format(NON_ASSOCIATE_ID, "Committee Member", memberId, "Proposal Cycle", cycleCode)
                ));


        return deleteChildObject(tac, committeeMember, tac::removeFromMembers);
    }


    @PUT
    @Path("members/{memberId}/role")
    @Operation(summary = "Edit the role of the CommitteeMember specified by 'memberId'")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    @RolesAllowed({"tac_admin"})
    public CommitteeMember replaceRole(@PathParam("cycleCode") Long cycleCode,
                                       @PathParam("memberId") Long memberId,
                                       TacRole replacementRole)
        throws WebApplicationException
    {
        ProposalCycle proposalCycle = findObject(ProposalCycle.class, cycleCode);

        proposalCyclesResource.checkUserOnTAC(proposalCycle);

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
