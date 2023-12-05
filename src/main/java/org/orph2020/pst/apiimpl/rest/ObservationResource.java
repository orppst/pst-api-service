package org.orph2020.pst.apiimpl.rest;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.prop.*;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.jboss.resteasy.reactive.RestQuery;
import org.orph2020.pst.common.json.ObjectIdentifier;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("proposals/{proposalCode}/observations")
@Tag(name = "proposals-observations")
@Produces(MediaType.APPLICATION_JSON)
public class ObservationResource extends ObjectResourceBase {

    private Observation findObservation(List<Observation> observations, Long id, Long proposalCode)
            throws WebApplicationException
    {
        return observations.stream().filter(o -> id.equals(o.getId())).findAny()
                .orElseThrow(() -> new WebApplicationException(
                        String.format(NON_ASSOCIATE_ID, "Observation", id, "ObservingProposal", proposalCode)
                ));
    }


    enum ObsType {
        TargetObservation,
        CalibrationObservation
    }
    @GET
    @Operation(summary = "get the list of ObjectIdentifiers for the Observations associated with the given ObservingProposal, optionally provide a srcName as a query to get that particular Observation's identifier")
    public List<ObjectIdentifier> getObservations(@PathParam("proposalCode") Long proposalCode,
                                                  @RestQuery String srcName,
                                                  @RestQuery ObsType type)
            throws WebApplicationException
    {
        String select = "select o._id,t.sourceName ";
        String from = "from ObservingProposal p ";
        String innerJoin = "inner join p.observations o inner join o.target t ";
        String where = "where p._id=" + proposalCode + " ";
        String orderBy = "order by t.sourceName";

        String typeQuery = type != null ?
                "and Type(o)=" + type.name() + " " : "";
        String srcLike = srcName != null ?
                "and t.sourceName like '" + srcName + "' " : "";

        return getObjectIdentifiers(select + from + innerJoin + where +
                typeQuery + srcLike + orderBy);
    }

    @GET
    @Path("/{observationId}")
    @Operation(summary = "get the Observation specified by the DB id belonging to the given proposal")
    public Observation getObservation(@PathParam("proposalCode") Long proposalCode,
                                      @PathParam("observationId") Long observationId)
        throws WebApplicationException
    {
        return findChildByQuery(ObservingProposal.class, Observation.class, "observations",
                proposalCode, observationId);
    }



    @POST
    @Operation(summary = "add a new Observation to the given ObservingProposal")
    @Consumes(MediaType.APPLICATION_JSON)
    @ResponseStatus(201)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Observation addNewObservation(@PathParam("proposalCode") Long proposalCode,
                                         Observation observation)
    {
        ObservingProposal observingProposal = findObject(ObservingProposal.class, proposalCode);
        return addNewChildObject(observingProposal, observation,
                observingProposal::addToObservations);
    }


    @DELETE
    @Path("/{observationId}")
    @Operation(summary = "remove the Observation specified by 'observationId' from the given ObservingProposal")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response removeObservation(@PathParam("proposalCode") Long proposalCode,
                                      @PathParam("observationId") Long id)
            throws WebApplicationException
    {
        ObservingProposal observingProposal = findObject(ObservingProposal.class, proposalCode);
        Observation observation =
                findObservation(observingProposal.getObservations(), id, proposalCode);
        return deleteChildObject(observingProposal,observation,
                observingProposal::removeFromObservations);
    }

    @PUT
    @Path("/{observationId}/target")
    @Operation(summary = "replace the Target of the Observation for the given ObservingProposal")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceTarget(@PathParam("proposalCode") Long proposalCode,
                                  @PathParam("observationId") Long observationId,
                                  Target target)
            throws WebApplicationException
    {
        Observation observation = findChildByQuery(ObservingProposal.class, Observation.class,
                "observations", proposalCode, observationId);

        observation.setTarget(target);

        return responseWrapper(observation, 201);
    }

    @PUT
    @Path("/{observationId}/field")
    @Operation(summary = "replace the Field of the given Observation for the given ObservingProposal")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceField(@PathParam("proposalCode") Long proposalCode,
                                 @PathParam("observationId") Long observationId,
                                 Field field)
            throws WebApplicationException
    {
        Observation observation = findChildByQuery(ObservingProposal.class, Observation.class,
                "observations", proposalCode, observationId);

        observation.setField(field);

        return responseWrapper(observation, 201);
    }

    @PUT
    @Path("/{observationId}/technicalGoal")
    @Operation(summary = "replace the TechnicalGoal of the given Observation for the given ObservingProposal")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceTechnicalGoal(@PathParam("proposalCode") Long proposalCode,
                                         @PathParam("observationId") Long observationId,
                                         TechnicalGoal technicalGoal)
            throws WebApplicationException
    {
        ObservingProposal observingProposal = findObject(ObservingProposal.class, proposalCode);
        Observation observation =
                findObservation(observingProposal.getObservations(), observationId, proposalCode);
        observation.setTechnicalGoal(technicalGoal);

        return responseWrapper(observation, 201);
    }

    @GET
    @Path("/{observationId}/constraints")
    @Operation(summary = "get the list of Constraints for the given Observation in the given ObservingProposal")
    public List<ObservingConstraint> getConstraints(@PathParam("proposalCode") Long proposalCode,
                                           @PathParam("observationId") Long observationId)
            throws WebApplicationException
    {
        Observation observation = findChildByQuery(ObservingProposal.class, Observation.class,
                "observations", proposalCode, observationId);
        return observation.getConstraints();
    }

    @GET
    @Path("/{observationId}/constraints/{constraintId}")
    @Operation(summary = "get the constraint referenced by the 'constraintId' for the given observation")
    public ObservingConstraint getConstraint(@PathParam("observationId") Long observationId,
                                    @PathParam("constraintId") Long constraintId)
        throws WebApplicationException
    {
        return findChildByQuery(Observation.class, ObservingConstraint.class,
                "constraints", observationId, constraintId);
    }

    @POST
    @Path("/{observationId}/constraints")
    @Operation(summary = "add a new Constraint to the Observation specified by 'id' in the given ObservingProposal")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public ObservingConstraint addNewConstraint(@PathParam("proposalCode") Long proposalCode,
                                       @PathParam("observationId") Long id,
                                                ObservingConstraint constraint)
            throws WebApplicationException
    {
        ObservingProposal observingProposal = findObject(ObservingProposal.class, proposalCode);
        Observation observation =
                findObservation(observingProposal.getObservations(), id, proposalCode);

        return addNewChildObject(observation, constraint, observation::addToConstraints);
    }

    @DELETE
    @Path("/{observationId}/constraints/{constraintId}")
    @Operation(summary = "remove the specified Constraint from the Observation of the given ObservationProposal")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response removeConstraint(@PathParam("proposalCode") Long proposalCode,
                                     @PathParam("observationId") Long observationId,
                                     @PathParam("constraintId") Long constraintId)
            throws WebApplicationException
    {
        ObservingProposal observingProposal = findObject(ObservingProposal.class, proposalCode);
        Observation observation =
                findObservation(observingProposal.getObservations(), observationId, proposalCode);
        List<ObservingConstraint> constraints = observation.getConstraints();

        ObservingConstraint constraint = constraints
                .stream().filter(o -> constraintId.equals(o.getId())).findAny()
                .orElseThrow(() -> new WebApplicationException(
                        String.format(NON_ASSOCIATE_ID, "Constraint", constraintId,
                                "Observation", observationId)
                ));

        return deleteChildObject(observation, constraint, observation::removeFromConstraints);
    }

    @PUT
    @Path("/{observationId}/timingWindows/{timingWindowId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "replaces the timing window referenced by 'timingWindowId' for the given observation")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public TimingWindow replaceTimingWindow(
            @PathParam("proposalCode") Long proposalCode,
            @PathParam("observationId") Long observationId,
            @PathParam("timingWindowId") Long timingWindowId,
            TimingWindow replacementWindow
    )
        throws WebApplicationException
    {
        Observation observation = findChildByQuery(ObservingProposal.class, Observation.class,
                "observations", proposalCode, observationId);

        TimingWindow window = findChildByQuery(Observation.class, TimingWindow.class,
                "constraints", observationId, timingWindowId);

        // only sets those members that are immediate children of the TimingWindow class
        window.updateUsing(replacementWindow);

        em.merge(window);

        return window;
    }


}
