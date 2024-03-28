package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 20/04/2023 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.management.*;
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
public class ProposalCyclesResource extends ObjectResourceBase {
    private final Logger logger;

    public ProposalCyclesResource(Logger logger) {
        this.logger = logger;
    }


    @GET
    @Operation(summary = "List the ProposalCycles")
    public List<ObjectIdentifier> getProposalCycles(@RestQuery boolean includeClosed) {
        if(includeClosed)
            return super.getObjectIdentifiers("SELECT o._id,o.title FROM ProposalCycle o ORDER BY o.title");
        else
            return super.getObjectIdentifiers("SELECT o._id,o.title FROM ProposalCycle o ORDER BY o.title");//FIXME actually do only return open
    }


    @GET
    @Path("{cycleCode}")
    @Operation(summary = "Get proposal cycle")
    public ProposalCycle getProposalCycle(@PathParam("cycleCode") long cycleId) {
        return findObject(ProposalCycle.class,cycleId);
    }

    @GET
    @Path("{cycleCode}/dates")
    @Operation(summary = "Get the dates associated with a given ProposalCycle")
    public ProposalCycleDates getProposalCycleDates(@PathParam("cycleCode") long cycleId)
    {
        ProposalCycle fullCycle =  findObject(ProposalCycle.class,cycleId);

        return new ProposalCycleDates(fullCycle.getTitle(), fullCycle.getSubmissionDeadline(),
                fullCycle.getObservationSessionStart(), fullCycle.getObservationSessionEnd());
    }


    @GET
    @Path("{cycleCode}/grades")
    @Operation(summary = "List the possible grades of the given ProposalCycle")
    public List<ObjectIdentifier> getCycleAllocationGrades(@PathParam("cycleCode") long cycleCode)
    {
        return getObjectIdentifiers("Select o._id,o.name from ProposalCycle p inner join p.possibleGrades o where p._id = "+cycleCode+" Order by o.name");
    }

    @GET
    @Path("{cycleCode}/grades/{gradeId}")
    @Operation(summary = "get the specific grade associated with the given ProposalCycle")
    public AllocationGrade getCycleAllocatedGrade(@PathParam("cycleCode") long cycleCode,
                                                  @PathParam("gradeId") long gradeId)
    {
        return findChildByQuery(ProposalCycle.class, AllocationGrade.class,
                "possibleGrades", cycleCode, gradeId);
    }

}
