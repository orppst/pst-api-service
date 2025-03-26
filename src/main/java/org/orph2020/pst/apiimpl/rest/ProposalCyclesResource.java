package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 20/04/2023 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.management.*;
import org.ivoa.dm.proposal.management.Observatory;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestQuery;
import org.orph2020.pst.common.json.ObjectIdentifier;
import org.orph2020.pst.common.json.ProposalCycleDates;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.*;

@Path("proposalCycles")
@Tag(name="proposalCycles")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("default-roles-orppst")
public class ProposalCyclesResource extends ObjectResourceBase {
    private final Logger logger;

    public ProposalCyclesResource(Logger logger) {
        this.logger = logger;
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
    public ProposalCycle getProposalCycle(@PathParam("cycleCode") long cycleId) {
        return findObject(ProposalCycle.class,cycleId);
    }


    @POST
    @Operation(summary = "create a proposal cycle")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    @RolesAllowed("tac_admin")
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
    @RolesAllowed("tac_admin")
    public Response replaceCycleTitle(
            @PathParam("cycleCode") Long cycleCode,
            String replacementTitle
    )
            throws WebApplicationException
    {
        ProposalCycle cycle = findObject(ProposalCycle.class, cycleCode);

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

        cycle.setObservationSessionEnd(replacementEnd);

        return responseWrapper(cycle.getObservationSessionEnd(), 200);
    }


    //********* GRADES **********

    @GET
    @Path("{cycleCode}/grades")
    @Operation(summary = "List the possible grades of the given proposal cycle")
    public List<ObjectIdentifier> getCycleAllocationGrades(@PathParam("cycleCode") Long cycleCode)
    {
        return getObjectIdentifiers("Select o._id,o.name from ProposalCycle p inner join p.possibleGrades o where p._id = "+cycleCode+" Order by o.name");
    }

    @GET
    @Path("{cycleCode}/grades/{gradeId}")
    @Operation(summary = "get the specific grade associated with the given proposal cycle")
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
    @RolesAllowed("tac_admin")
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
    @RolesAllowed("tac_admin")
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
    @RolesAllowed("tac_admin")
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
    @RolesAllowed("tac_admin")
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
    @RolesAllowed("tac_admin")
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

}
