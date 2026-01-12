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
import org.ivoa.dm.proposal.management.*;
import org.ivoa.dm.proposal.prop.Person;
import org.jboss.resteasy.reactive.RestQuery;
import org.orph2020.pst.common.json.ObjectIdentifier;

import java.util.List;

@Path("proposalCycles/{cycleCode}/TAC")
@Tag(name="proposalCycles-time-allocation-committee")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({"tac_admin", "tac_member", "obs_administration"})
public class TACResource extends ObjectResourceBase {

    @Inject
    SubjectMapResource subjectMapResource;

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
    @Path("/members/{tacRole}")
    @Operation(summary = "Add a new CommitteeMember with the specified 'tacRole' to the TAC")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    @RolesAllowed({"tac_admin"})
    public CommitteeMember addCommitteeMember(@PathParam("cycleCode") Long cycleCode,
                                              @PathParam("tacRole") String tacRole,
                                              Person newMember)
        throws WebApplicationException
    {
        //All TAC members are Reviewers, but not all Reviewers are TAC members

        //we enforce a one-to-one relationship between Person and Reviewer

        //check to see if the incoming Person is an existing Reviewer
        String qlString = "select r._id,cast(r.person._id as string),r.person.fullName from Reviewer r where r.person._id =: pid";

        Query query = em.createQuery(qlString);
        query.setParameter("pid", newMember.getId());

        //This list should be either empty or containing exactly one element.
        List<ObjectIdentifier> existingReviewer = getObjectIdentifiersAlt(query);

        Reviewer reviewer = existingReviewer.isEmpty() ?
                em.merge(new Reviewer(newMember)) : //persist the Person as a Reviewer
                findObject(Reviewer.class, existingReviewer.get(0).dbid); //find the existing Reviewer

        ProposalCycle proposalCycle = findObject(ProposalCycle.class, cycleCode);

        TAC tac = proposalCycle.getTac();

        CommitteeMember committeeMember = new CommitteeMember(reviewer, TacRole.fromValue(tacRole));

        addNewChildObject(tac, committeeMember, tac::addToMembers);

        //assign the appropriate role in keycloak - do this AFTER successfully persisting the object in the DB

        //if the Person already existed as a Reviewer we must first revoke that role before assigning the
        //'tac_*' role - ensures we have the correct "inheritance" flag set for the 'reviewer' role
        if (!existingReviewer.isEmpty()) {
            subjectMapResource.roleManagement(
                    reviewer.getPerson().getId(),
                    "reviewer",
                    SubjectMapResource.RoleAction.REVOKE
            );
        }

        subjectMapResource.roleManagement(
                newMember.getId(),
                tacRole.equals("Chair") ? "tac_admin" : "tac_member",
                SubjectMapResource.RoleAction.ASSIGN
        );

        return committeeMember;
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
        //remove the person as a CommitteeMember only, they remain as a Reviewer.
        ProposalCycle proposalCycle = findObject(ProposalCycle.class, cycleCode);

        TAC tac = proposalCycle.getTac();

        List<CommitteeMember> committeeMembers = tac.getMembers();

        long numChairs = tac.getMembers()
                .stream()
                .filter(m -> m.getRole().equals(TacRole.CHAIR))
                .count();

        CommitteeMember committeeMember = committeeMembers
                .stream().filter(m -> memberId.equals(m.getId())).findAny()
                .orElseThrow(() -> new WebApplicationException(
                        String.format(NON_ASSOCIATE_ID, "Committee Member", memberId, "Proposal Cycle", cycleCode)
                ));

        if (committeeMember.getRole().equals(TacRole.CHAIR) && numChairs < 2) {
            throw new WebApplicationException(
                    "You are the only TAC administrator, please first promote another to TAC Chair",
                    Response.Status.FORBIDDEN
            );
        }

        Response response = deleteChildObject(tac, committeeMember, tac::removeFromMembers);

        //revoke the appropriate role AFTER successfully removing the member from the DB
        subjectMapResource.roleManagement(
                committeeMember.getMember().getPerson().getId(),
                committeeMember.getRole().equals(TacRole.CHAIR) ? "tac_admin" : "tac_member",
                SubjectMapResource.RoleAction.REVOKE
        );

        //re-assign the 'reviewer' role (inherited roles are removed recursively in keycloak)
        subjectMapResource.roleManagement(
                committeeMember.getMember().getPerson().getId(),
                "reviewer",
                SubjectMapResource.RoleAction.ASSIGN
        );

        return response;
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

        TAC tac = proposalCycle.getTac();

        CommitteeMember committeeMember = tac.getMembers()
                .stream().filter(m -> memberId.equals(m.getId())).findAny()
                .orElseThrow(() -> new WebApplicationException(
                        String.format(NON_ASSOCIATE_ID, "Committee Member", memberId, "Proposal Cycle", cycleCode)
                ));

        TacRole currentRole = committeeMember.getRole();

        if (currentRole.equals(replacementRole)) {
            throw new WebApplicationException("Selected member already has that role", Response.Status.CONFLICT);
        }

        committeeMember.setRole(replacementRole);

        // This is slightly cumbersome due to the use of inherited roles. Removing a role that has inherited
        // roles removes those roles recursively. We could apply roles directly such that they are not
        // inherited but then what's the point of having composite roles in the first place?

        if (currentRole.equals(TacRole.CHAIR)) {
            //demoting 'tac_admin' -> 'tac_member'
            subjectMapResource.roleManagement(
                    committeeMember.getMember().getPerson().getId(),
                    "tac_admin" ,
                    SubjectMapResource.RoleAction.REVOKE
            );

            subjectMapResource.roleManagement(
                    committeeMember.getMember().getPerson().getId(),
                    "tac_member" ,
                    SubjectMapResource.RoleAction.ASSIGN
            );
        } else if (replacementRole.equals(TacRole.CHAIR)) {
            //promoting 'tac_member' -> 'tac_admin'
            subjectMapResource.roleManagement(
                    committeeMember.getMember().getPerson().getId(),
                    "tac_member" ,
                    SubjectMapResource.RoleAction.REVOKE
            );

            subjectMapResource.roleManagement(
                    committeeMember.getMember().getPerson().getId(),
                    "tac_admin" ,
                    SubjectMapResource.RoleAction.ASSIGN
            );
        }//else switching between tac-roles at 'tac_member' level


        return committeeMember;
    }


}
