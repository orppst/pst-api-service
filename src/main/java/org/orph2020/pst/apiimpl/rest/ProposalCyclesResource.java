package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 20/04/2023 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.management.*;
import org.ivoa.dm.proposal.management.Observatory;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestQuery;
import org.orph2020.pst.common.json.CycleObservingTimeTotal;
import org.orph2020.pst.common.json.ObjectIdentifier;
import org.orph2020.pst.common.json.ProposalCycleDates;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Path("proposalCycles")
@Tag(name="proposalCycles")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({"default-roles-orppst"})
public class ProposalCyclesResource extends ObjectResourceBase {
    private final Logger logger;

    @Inject
    SubjectMapResource subjectMapResource;
    @Inject
    JsonWebToken userInfo;

    public ProposalCyclesResource(Logger logger) {
        this.logger = logger;
    }

    public void checkUserOnTAC(ProposalCycle cycle)
            throws WebApplicationException
    {
        // Get the logged in user details.
        Long personId = subjectMapResource.subjectMap(userInfo.getSubject()).getPerson().getId();

        // An observatory administrator can do _anything_
        if(userInfo.getClaim("realm_access") != null) {
            String roleList = userInfo.getClaim("realm_access").toString();

            if(roleList != null && roleList.contains("\"obs_administration\"")) {
                return;
            }
        }

        AtomicReference<Boolean> amIOnTheTAC = new AtomicReference<>(false);

        // See if user is member of the TAC
        cycle.getTac().getMembers().forEach(member -> {
            if(member.getMember().getId().equals(personId)) {
                amIOnTheTAC.set(true);
            }
        });

        if(amIOnTheTAC.get())
            return;

        throw new WebApplicationException("You are not on the TAC of this proposal cycle", 403);
    }

    @GET
    @Path("MyTACCycles")
    @Operation(summary = "get all the proposal cycles where I am a on the TAC")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"tac_member", "tac_admin"})
    public List<ObjectIdentifier> getMyTACMemberProposalCycles() {
        // Get the logged in user details.
        Long personId = subjectMapResource.subjectMap(userInfo.getSubject()).getPerson().getId();
        List<ObjectIdentifier> matchedCycles = new ArrayList<>();

        // Find list of Proposal Cycles and their TACs
        String query = "select p._id,p.title,tac from ProposalCycle p";

        List<Object[]> cycles = em.createQuery(query).getResultList();

        // Filter these TACs for those that include this user
        for (Object[] cycle : cycles) {
            TAC cycleTac = (TAC) cycle[2];
            cycleTac.getMembers().forEach(member -> {
                if(member.getMember().getId().equals(personId)) {
                    matchedCycles.add(new ObjectIdentifier((long)cycle[0], cycle[1].toString()));
                }
            });
        }

        return matchedCycles;
    }

    @GET
    @Operation(summary = "list the proposal cycles, optionally filter by observatory id and closed (passed submission deadline)")
    public List<ObjectIdentifier> getProposalCycles(@RestQuery boolean includeClosed, @RestQuery long observatoryId) {
        String select = "SELECT o._id,o.title FROM ProposalCycle o ";
        String where = "";
        String order = "ORDER BY o.submissionDeadline";

        if(!includeClosed) {
            where = "WHERE o.submissionDeadline > CURRENT_TIMESTAMP() ";
            if (observatoryId > 0)
                where += "AND o.observatory._id = "+observatoryId+" ";
        } else {
            if (observatoryId > 0)
                where = "WHERE o.observatory._id = "+observatoryId+" ";
        }

        return super.getObjectIdentifiers(select + where + order);
    }


    @GET
    @Path("{cycleCode}")
    @Operation(summary = "get the given proposal cycle")
    @RolesAllowed({"tac_member", "tac_admin", "obs_administration"})
    public ProposalCycle getProposalCycle(@PathParam("cycleCode") long cycleId) {
        return findObject(ProposalCycle.class,cycleId);
    }


    @POST
    @Operation(summary = "create a proposal cycle")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    @RolesAllowed("obs_administration")
    public ProposalCycle createProposalCycle(ProposalCycle cycle) {
        return persistObject(cycle);
    }

    ///********* TITLE **********

    @GET
    @Path("{cycleCode}/title")
    @Operation(summary = "Get the title for a given proposal cycle")
    public Response getProposalCycleTitle(@PathParam("cycleCode") Long cycleCode)
    {
        ProposalCycle fullCycle =  findObject(ProposalCycle.class, cycleCode);

        return responseWrapper(fullCycle.getTitle(), 200);
    }

    @PUT
    @Path("{cycleCode}/title")
    @Operation(summary = "change the title of the given proposal cycle")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    @RolesAllowed({"tac_admin"})
    public Response replaceCycleTitle(
            @PathParam("cycleCode") Long cycleCode,
            String replacementTitle
    )
            throws WebApplicationException
    {
        // Get the TAC for this cycle
        ProposalCycle cycle = findObject(ProposalCycle.class, cycleCode);

        // See if user is on the TAC
        checkUserOnTAC(cycle);

        cycle.setTitle(replacementTitle);

        return responseWrapper(cycle.getTitle(), 200);
    }


    //********* DATES **********

    @GET
    @Path("{cycleCode}/dates")
    @Operation(summary = "Get the dates associated with a given proposal cycle")
    public ProposalCycleDates getProposalCycleDates(@PathParam("cycleCode") Long cycleCode)
    {
        ProposalCycle fullCycle =  findObject(ProposalCycle.class, cycleCode);

        return new ProposalCycleDates(fullCycle.getTitle(), fullCycle.getSubmissionDeadline(),
                fullCycle.getObservationSessionStart(), fullCycle.getObservationSessionEnd(),
                fullCycle.getObservatory());
    }

    @PUT
    @Path("{cycleCode}/dates/deadline")
    @Operation(summary = "change the submission deadline of the given proposal cycle")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("tac_admin")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceCycleDeadline(
            @PathParam("cycleCode") Long cycleCode,
            Date replacementDeadline
    )
        throws WebApplicationException
    {
        ProposalCycle cycle = findObject(ProposalCycle.class, cycleCode);

        checkUserOnTAC(cycle);

        cycle.setSubmissionDeadline(replacementDeadline);

        return responseWrapper(cycle.getSubmissionDeadline(), 200);
    }


    @PUT
    @Path("{cycleCode}/dates/sessionStart")
    @Operation(summary = "change the observation session start of the given proposal cycle")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("tac_admin")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceCycleSessionStart(
            @PathParam("cycleCode") Long cycleCode,
            Date replacementStart
    )
            throws WebApplicationException
    {
        ProposalCycle cycle = findObject(ProposalCycle.class, cycleCode);

        checkUserOnTAC(cycle);

        cycle.setObservationSessionStart(replacementStart);

        return responseWrapper(cycle.getObservationSessionStart(), 200);
    }

    @PUT
    @Path("{cycleCode}/dates/sessionEnd")
    @Operation(summary = "change the observation session end of the given proposal cycle")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("tac_admin")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceCycleSessionEnd(
            @PathParam("cycleCode") Long cycleCode,
            Date replacementEnd
    )
            throws WebApplicationException
    {
        ProposalCycle cycle = findObject(ProposalCycle.class, cycleCode);

        checkUserOnTAC(cycle);

        cycle.setObservationSessionEnd(replacementEnd);

        return responseWrapper(cycle.getObservationSessionEnd(), 200);
    }


    //********* GRADES **********

    @GET
    @Path("{cycleCode}/grades")
    @Operation(summary = "List the possible grades of the given proposal cycle")
    @RolesAllowed({"tac_member", "tac_admin"})
    public List<ObjectIdentifier> getCycleAllocationGrades(@PathParam("cycleCode") Long cycleCode)
    {
        Query query = em.createQuery(
                "Select o._id,o.description,o.name from ProposalCycle p inner join p.possibleGrades o where p._id = "+cycleCode+" Order by o.name"
        );

        return getObjectIdentifiersAlt(query);
    }

    @GET
    @Path("{cycleCode}/grades/{gradeId}")
    @Operation(summary = "get the specific grade associated with the given proposal cycle")
    @RolesAllowed({"tac_member", "tac_admin"})
    public AllocationGrade getCycleAllocatedGrade(@PathParam("cycleCode") Long cycleCode,
                                                  @PathParam("gradeId") Long gradeId)
    {
        return findChildByQuery(ProposalCycle.class, AllocationGrade.class,
                "possibleGrades", cycleCode, gradeId);
    }

    @POST
    @Path("{cycleCode}/grades")
    @Operation(summary = "add a new possible allocation grade to the given proposal cycle")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("obs_administration")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public AllocationGrade addCycleAllocationGrade(@PathParam("cycleCode") Long cycleCode,
                                                   AllocationGrade grade)
        throws WebApplicationException
    {
        ProposalCycle cycle = findObject(ProposalCycle.class, cycleCode);

        return addNewChildObject(cycle, grade, cycle::addToPossibleGrades);
    }

    @DELETE
    @Path("{cycleCode}/grades/{gradeId}")
    @Operation(summary = "remove the specified possible grade from the given proposal cycle")
    @RolesAllowed("obs_administration")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response removeCycleAllocationGrade(@PathParam("cycleCode") Long cycleCode,
                                               @PathParam("gradeId") Long gradeId)
        throws WebApplicationException
    {
        ProposalCycle cycle = findObject(ProposalCycle.class, cycleCode);

        AllocationGrade grade = findChildByQuery(ProposalCycle.class, AllocationGrade.class,
                "possibleGrades", cycleCode, gradeId);

        return deleteChildObject(cycle, grade, cycle::removeFromPossibleGrades);
    }

    @PUT
    @Path("{cycleCode}/grades/{gradeId}/name")
    @Operation(summary = "change the name of the given allocation grade")
    @Consumes(MediaType.TEXT_PLAIN)
    @RolesAllowed("obs_administration")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public AllocationGrade replaceCycleAllocationGradeName(
            @PathParam("cycleCode") Long cycleCode,
            @PathParam("gradeId") Long gradeId,
            String replacementName
    )
        throws WebApplicationException
    {
        AllocationGrade grade = findChildByQuery(ProposalCycle.class, AllocationGrade.class,
                "possibleGrades", cycleCode, gradeId);

        grade.setName(replacementName);

        return grade;
    }

    @PUT
    @Path("{cycleCode}/grades/{gradeId}/description")
    @Operation(summary = "change the description of the given allocation grade")
    @Consumes(MediaType.TEXT_PLAIN)
    @RolesAllowed("obs_administration")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public AllocationGrade replaceCycleAllocationGradeDescription(
            @PathParam("cycleCode") Long cycleCode,
            @PathParam("gradeId") Long gradeId,
            String replacementDescription
    )
            throws WebApplicationException
    {
        AllocationGrade grade = findChildByQuery(ProposalCycle.class, AllocationGrade.class,
                "possibleGrades", cycleCode, gradeId);

        grade.setDescription(replacementDescription);

        return grade;
    }

    ///********* OBSERVATORY **********

    @GET
    @Path("{cycleCode}/observatory")
    @Operation(summary = "Get the observatory object identifier for a given proposal cycle")
    public Observatory getProposalCycleObservatory(@PathParam("cycleCode") Long cycleCode)
    {
        ProposalCycle fullCycle =  findObject(ProposalCycle.class, cycleCode);

        return fullCycle.getObservatory();
    }

    @PUT
    @Path("{cycleCode}/observatory")
    @Operation(summary = "change the observatory for the given proposal cycle")
    @Consumes(MediaType.TEXT_PLAIN)
    @RolesAllowed("obs_administration")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceCycleObservatory(
            @PathParam("cycleCode") Long cycleCode,
            Long replacementObservatoryCode
    )
            throws WebApplicationException
    {
        ProposalCycle cycle = findObject(ProposalCycle.class, cycleCode);

        Observatory replacementObservatory = findObject(Observatory.class, replacementObservatoryCode);

        cycle.setObservatory(replacementObservatory);

        return responseWrapper(cycle.getObservatory(), 200);
    }

    /// ***** Observing Time total for the entire cycle ***** ///

    @GET
    @Path("{cycleCode}/observingTimeTotals")
    @Operation(summary = "get total time allocated in the cycle broken down by observing mode and allocation grade")
    @RolesAllowed({"tac_member", "tac_admin"})
    public List<CycleObservingTimeTotal>
    getCycleObservingTimeTotals(@PathParam("cycleCode") Long cycleCode)
    {
        ProposalCycle cycle = findObject(ProposalCycle.class, cycleCode);

        checkUserOnTAC(cycle);

        List<AllocatedProposal> allocatedProposals = cycle.getAllocatedProposals();

        HashMap<String, Double> modeGradeTotals = new HashMap<>();

        for (AllocatedProposal allocatedProposal : allocatedProposals) {
            List<AllocatedBlock> allocatedBlocks = allocatedProposal.getAllocation();
            for (AllocatedBlock allocatedBlock : allocatedBlocks) {
                String modeName = allocatedBlock.getMode().getName();
                String gradeName = allocatedBlock.getGrade().getName();

                String key = modeName + "$" + gradeName;

                Double currentValue = modeGradeTotals.get(key) == null ?  0. : modeGradeTotals.get(key);

                modeGradeTotals.put(
                        key,
                        currentValue + allocatedBlock.getResource().getAmount()
                );
            }
        }

        List<CycleObservingTimeTotal> result = new ArrayList<>();

        for (String key : modeGradeTotals.keySet()) {
            String modeName = key.substring(0, key.indexOf("$"));
            String gradeName = key.substring(key.indexOf("$") + 1);

            result.add(new CycleObservingTimeTotal(modeName, gradeName, modeGradeTotals.get(key)));
        }

        return result;
    }

}
