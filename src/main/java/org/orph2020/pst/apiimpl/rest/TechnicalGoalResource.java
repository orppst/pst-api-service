package org.orph2020.pst.apiimpl.rest;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.prop.*;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.orph2020.pst.common.json.ObjectIdentifier;

import java.util.ArrayList;
import java.util.List;

@Path("proposals/{proposalCode}/technicalGoals")
@Tag(name = "proposals-technicalGoals")
@Produces(MediaType.APPLICATION_JSON)
public class TechnicalGoalResource extends ObjectResourceBase{

    // technicalGoals
    //if we were following the design pattern we should return a list of TechnicalGoal identifiers
    // - problem is there is no natural name so cast id to string
    @GET
    @Operation(summary = "get the list of TechnicalGoals associated with the given ObservingProposal")
    public List<ObjectIdentifier> getTechnicalGoals(@PathParam("proposalCode") Long proposalCode)
    {
        return getObjectIdentifiers("SELECT t._id,cast(t._id as string) FROM ObservingProposal o Inner Join o.technicalGoals t WHERE o._id = "+proposalCode);
    }

    @GET
    @Path("{technicalGoalId}")
    @Operation(summary = "get a specific TechnicalGoal for the given ObservingProposal")
    public TechnicalGoal getTechnicalGoal(@PathParam("proposalCode") Long proposalCode,
                                          @PathParam("technicalGoalId") Long techGoalId)
    {
        return findChildByQuery(ObservingProposal.class, TechnicalGoal.class, "technicalGoals",
                proposalCode, techGoalId);
    }

    @POST
    @Operation(summary = "add a new technical goal to the given ObservingProposal")
    @Consumes(MediaType.APPLICATION_JSON)
    @ResponseStatus(201)
    @Transactional
    public TechnicalGoal addTechnicalGoal(@PathParam("proposalCode") Long proposalCode,
                                             TechnicalGoal technicalGoal)
            throws WebApplicationException
    {
        ObservingProposal observingProposal = findObject(ObservingProposal.class, proposalCode);
        return addNewChildObject(observingProposal,technicalGoal, observingProposal::addToTechnicalGoals);
    }


    @DELETE
    @Path("{technicalGoalId}")
    @Operation(summary = "remove the Technical Goal specified by 'technicalGoalId' from the given ObservingProposal")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response removeTechnicalGoal(@PathParam("proposalCode") Long proposalCode,
                                        @PathParam("technicalGoalId") Long technicalGoalId)
            throws WebApplicationException
    {
        ObservingProposal observingProposal = findObject(ObservingProposal.class, proposalCode);

        //we've just found the ObservingProposal so may as well use it to find the TechnicalGoal
        //rather than doing a 'findChildByQuery()'
        TechnicalGoal technicalGoal = observingProposal
                .getTechnicalGoals()
                .stream()
                .filter(o -> technicalGoalId.equals(o.getId())).findAny()
                .orElseThrow(() -> new WebApplicationException(
                        String.format(NON_ASSOCIATE_ID, "Technical Goal", technicalGoalId,
                                "ObservingProposal", proposalCode)
                ));

        return deleteChildObject(observingProposal, technicalGoal, observingProposal::removeFromTechnicalGoals);
    }

    //TechnicalGoal::PerformanceParameters

    @PUT
    @Path("{technicalGoalId}/performanceParameters")
    @Operation(summary = "replace the PerformanceParameters of the TechnicalGoal referred to by the 'technicalGoalId")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public PerformanceParameters replacePerformanceParameters(@PathParam("proposalCode") Long proposalCode,
                                                      @PathParam("technicalGoalId") Long technicalGoalId,
                                                      PerformanceParameters replacementParameters)
        throws WebApplicationException
    {
        TechnicalGoal currentGoal = findChildByQuery(ObservingProposal.class, TechnicalGoal.class,
                "technicalGoals", proposalCode, technicalGoalId);

        currentGoal.setPerformance(replacementParameters);

        return currentGoal.getPerformance();
    }

    //TechnicalGoal::Spectrum (List<ScienceSpectralWindow>)

    @POST
    @Path("{technicalGoalId}/spectrum")
    @Operation(summary = "add a new spectral window to the TechnicalGoal referred to by the 'technicalGoalId'")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public ScienceSpectralWindow addSpectrum(@PathParam("proposalCode") Long proposalCode,
                                             @PathParam("technicalGoalId") Long technicalGoalId,
                                             ScienceSpectralWindow spectralWindow)
        throws WebApplicationException
    {
        TechnicalGoal goal = findChildByQuery(ObservingProposal.class, TechnicalGoal.class,
                "technicalGoals", proposalCode, technicalGoalId);

        return addNewChildObject(goal, spectralWindow, goal::addToSpectrum);
    }

    @DELETE
    @Path("{technicalGoalId}/spectrum/{windowIndex}/")
    @Operation(summary = "remove the Spectrum at 'windowIndex' of the TechnicalGoal referred to by the 'technicalGoalId")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response removeSpectrum(@PathParam("proposalCode") Long proposalCode,
                                   @PathParam("technicalGoalId") Long technicalGoalId,
                                   @PathParam("windowIndex") int windowIndex)
            throws WebApplicationException
    {
        TechnicalGoal goal = findChildByQuery(ObservingProposal.class, TechnicalGoal.class,
                "technicalGoals", proposalCode, technicalGoalId);

        if (windowIndex < 0 || windowIndex >= goal.getSpectrum().size()) {
            throw new WebApplicationException("index out-of-bounds", 400);
        }

        return deleteChildObject(goal, goal.getSpectrum().get(windowIndex),
                goal::removeFromSpectrum);
    }

    @PUT
    @Path("{technicalGoalId}/spectrum/{windowIndex}/")
    @Operation(summary = "replace the Spectrum at 'windowIndex' of the TechnicalGoal referred to by the 'technicalGoalId")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public ScienceSpectralWindow replaceSpectrum(@PathParam("proposalCode") Long proposalCode,
                                                 @PathParam("technicalGoalId") Long technicalGoalId,
                                                 @PathParam("windowIndex") int windowIndex,
                                                 ScienceSpectralWindow replacementWindow)
            throws WebApplicationException
    {
        TechnicalGoal goal = findChildByQuery(ObservingProposal.class, TechnicalGoal.class,
                "technicalGoals", proposalCode, technicalGoalId);

        if (windowIndex < 0 || windowIndex >= goal.getSpectrum().size()) {
            throw new WebApplicationException("index out-of-bounds", 400);
        }

        // goal.getSpectrum() returns an immutable List.
        // The following is what we want to do but can't:
        // goal.getSpectrum().set(windowIndex, replacementWindow);

        // instead we copy the entire list except for replacing the element we wish to update...
        List<ScienceSpectralWindow> replace = new ArrayList<>(goal.getSpectrum());
        replace.set(windowIndex, replacementWindow);

        //...then replace the entire list
        goal.setSpectrum(replace);

        return goal.getSpectrum().get(windowIndex);
    }

    //TechnicalGoal::Spectrum.at(index)::ExpectedSpectralLines (List<ExpectedSpectralLine>)

    //work-around the immutable ExpectedSpectralLines list by creating a new, mutable copy of the list
    private static List<ExpectedSpectralLine> getExpectedSpectralLines(int windowIndex, TechnicalGoal goal) {
        List<ExpectedSpectralLine> currentSpectralLines =
                goal.getSpectrum().get(windowIndex).getExpectedSpectralLine();

        return new ArrayList<>(currentSpectralLines);
    }

    @POST
    @Path("{technicalGoalId}/spectrum/{windowIndex}/expectedSpectralLine")
    @Operation(summary = "add an expected spectral line to the spectral window at 'windowIndex' for the given TechnicalGoal")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public ExpectedSpectralLine addExpectedSpectralLine(@PathParam("proposalCode") Long proposalCode,
                                                        @PathParam("technicalGoalId") Long technicalGoalId,
                                                        @PathParam("windowIndex") int windowIndex,
                                                        ExpectedSpectralLine spectralLine)
        throws WebApplicationException
    {
        TechnicalGoal goal = findChildByQuery(ObservingProposal.class, TechnicalGoal.class,
                "technicalGoals", proposalCode, technicalGoalId);

        if (windowIndex < 0 || windowIndex >= goal.getSpectrum().size()) {
            throw new WebApplicationException("index out-of-bounds", 400);
        }

        // Can't do this as returned list immutable
        // goal.getSpectrum().get(windowIndex).getExpectedSpectralLine().add(spectralLine);

        // Also there is no ScienceSpectralWindow::addToExpectedSpectralLines() so ...

        //... instead copy the current ExpectedSpectralLines list...
        List<ExpectedSpectralLine> newSpectralLines = getExpectedSpectralLines(windowIndex, goal);
        // ... add the new spectralLine ...
        newSpectralLines.add(spectralLine);

        // ... replace the entire ExpectedSpectralLines list
        goal.getSpectrum().get(windowIndex).setExpectedSpectralLine(newSpectralLines);

        return spectralLine;
    }

    @DELETE
    @Path("{technicalGoalId}/spectrum/{windowIndex}/expectedSpectralLine/{lineIndex}")
    @Operation(summary = "remove the expected spectral line at 'lineIndex' from the spectral window at 'windowIndex' of the given TechnicalGoal")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response removeExpectedSpectralLine(@PathParam("proposalCode") Long proposalCode,
                                               @PathParam("technicalGoalId") Long technicalGoalId,
                                               @PathParam("windowIndex") int windowIndex,
                                               @PathParam("lineIndex") int lineIndex)
        throws WebApplicationException
    {
        TechnicalGoal goal = findChildByQuery(ObservingProposal.class, TechnicalGoal.class,
                "technicalGoals", proposalCode, technicalGoalId);

        if (windowIndex < 0 || windowIndex >= goal.getSpectrum().size()) {
            throw new WebApplicationException("window index out-of-bounds", 400);
        }

        if (lineIndex < 0 || lineIndex >= goal.getSpectrum().get(windowIndex).getExpectedSpectralLine().size()) {
            throw new WebApplicationException("line index out-of-bounds", 400);
        }

        List<ExpectedSpectralLine> mutableList = getExpectedSpectralLines(windowIndex, goal);
        mutableList.remove(lineIndex);

        goal.getSpectrum().get(windowIndex).setExpectedSpectralLine(mutableList);

        return emptyResponse204();
    }


    //no PUT method - we don't replace spectral lines, we only ever add or remove them


}
