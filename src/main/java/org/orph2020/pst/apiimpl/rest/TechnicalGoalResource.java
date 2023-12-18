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

        //use copy constructor in case the front-end is attempting to clone the technical goal,
        //for a completely new technical goal this is inefficient but livable.
        return addNewChildObject(observingProposal, new TechnicalGoal(technicalGoal),
                observingProposal::addToTechnicalGoals);
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
    @Path("{technicalGoalId}/spectrum/{spectralWindowId}/")
    @Operation(summary = "remove the ScienceSpectralWindow with 'spectralWindowId' from the given TechnicalGoal")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response removeSpectrum(@PathParam("proposalCode") Long proposalCode,
                                   @PathParam("technicalGoalId") Long technicalGoalId,
                                   @PathParam("spectralWindowId") Long spectralWindowId)
            throws WebApplicationException
    {
        TechnicalGoal goal = findChildByQuery(ObservingProposal.class, TechnicalGoal.class,
                "technicalGoals", proposalCode, technicalGoalId);

        ScienceSpectralWindow spectralWindow =
                findChildByQuery(TechnicalGoal.class, ScienceSpectralWindow.class,
                        "spectrum", technicalGoalId, spectralWindowId);

        return deleteChildObject(goal, spectralWindow, goal::removeFromSpectrum);
    }

    @PUT
    @Path("{technicalGoalId}/spectrum/{spectralWindowId}/")
    @Operation(summary = "replace the ScienceSpectralWindow with 'spectralWindowId' in the given TechnicalGoal")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public ScienceSpectralWindow replaceSpectrum(
            @PathParam("proposalCode") Long proposalCode,
            @PathParam("technicalGoalId") Long technicalGoalId,
            @PathParam("spectralWindowId") Long spectralWindowId,
            ScienceSpectralWindow replacementWindow
    )
            throws WebApplicationException
    {
        TechnicalGoal goal = findChildByQuery(ObservingProposal.class, TechnicalGoal.class,
                "technicalGoals", proposalCode, technicalGoalId);

        ScienceSpectralWindow spectralWindow =
                findChildByQuery(TechnicalGoal.class, ScienceSpectralWindow.class,
                        "spectrum", technicalGoalId, spectralWindowId);

        spectralWindow.updateUsing(replacementWindow);

        return spectralWindow;
    }

    //ExpectedSpectralLines

    @POST
    @Path("{technicalGoalId}/spectrum/{spectralWindowId}/expectedSpectralLine")
    @Operation(summary = "add an expected spectral line to the spectral window with 'spectralWindowId' for the given TechnicalGoal")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public ExpectedSpectralLine addExpectedSpectralLine(
            @PathParam("proposalCode") Long proposalCode,
            @PathParam("technicalGoalId") Long technicalGoalId,
            @PathParam("spectralWindowId") Long spectralWindowId,
            ExpectedSpectralLine spectralLine
    )
        throws WebApplicationException
    {
        TechnicalGoal goal = findChildByQuery(ObservingProposal.class, TechnicalGoal.class,
                "technicalGoals", proposalCode, technicalGoalId);

        ScienceSpectralWindow spectralWindow =
                findChildByQuery(TechnicalGoal.class, ScienceSpectralWindow.class,
                        "spectrum", technicalGoalId, spectralWindowId);

        return addNewChildObject(spectralWindow, spectralLine, spectralWindow::addToExpectedSpectralLine);
    }

    @DELETE
    @Path("{technicalGoalId}/spectrum/{spectralWindowId}/expectedSpectralLine/{lineIndex}")
    @Operation(summary = "remove the expected spectral line at 'lineIndex' from the spectral window with 'spectralWindowId' for the given TechnicalGoal")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response removeExpectedSpectralLine(
            @PathParam("proposalCode") Long proposalCode,
            @PathParam("technicalGoalId") Long technicalGoalId,
            @PathParam("spectralWindowId") Long spectralWindowId,
            @PathParam("lineIndex") int lineIndex
    )
        throws WebApplicationException
    {
        TechnicalGoal goal = findChildByQuery(ObservingProposal.class, TechnicalGoal.class,
                "technicalGoals", proposalCode, technicalGoalId);

        ScienceSpectralWindow spectralWindow =
                findChildByQuery(TechnicalGoal.class, ScienceSpectralWindow.class,
                        "spectrum", technicalGoalId, spectralWindowId);

        ExpectedSpectralLine line = spectralWindow.getExpectedSpectralLine().get(lineIndex);

        return deleteChildObject(spectralWindow, line, spectralWindow::removeFromExpectedSpectralLine);
    }

    //no PUT method - we don't replace spectral lines, we only ever add or remove them
}
