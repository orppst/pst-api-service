package org.orph2020.pst.apiimpl.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.persistence.Query;
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

import java.util.Date;
import java.util.List;

@Path("proposals/{proposalCode}/observations")
@Tag(name = "proposals-observations")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("default-roles-orppst")
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
        String select = type == ObsType.CalibrationObservation ?
                "select o._id,concat(cast(Type(o) as string), ':',o.intent),t.sourceName " :
                "select o._id,cast(Type(o) as string),t.sourceName ";
        String from = "from ObservingProposal p ";
        String innerJoin = "inner join p.observations o inner join o.target t ";
        String where = "where p._id=" + proposalCode + " ";
        String orderBy = "order by t.sourceName";

        String typeQuery = type != null ?
                "and cast(Type(o) as string) like :typeName " : "";
        String srcLike = srcName != null ?
                "and t.sourceName like :sourceName " : "";

        String qlString = select + from + innerJoin + where +
                typeQuery + srcLike + orderBy;

        Query query = em.createQuery(qlString);
        if (type != null) query.setParameter("typeName", "%" + type.name() + "%");
        if (srcName != null) query.setParameter("sourceName", srcName);

        List<ObjectIdentifier> result = getObjectIdentifiersAlt(query);

        // edit the 'type' name i.e., the ObjectIdentifier.code member, to use as a display string
        // general format: 'proposal:<Type>Observation'
        // query ObsType='CalibrationObservation' format: 'proposal:CalibrationObservation:<INTENT>'
        // we want '<Type>' in general, and 'Calibration (<intent>)' for the specified query

        if (type == ObsType.CalibrationObservation) {
            //specific query case
            for (ObjectIdentifier oi : result) {
                oi.code = "Calibration (" +
                        oi.code
                                .substring(oi.code.lastIndexOf(":") + 1)
                                .toLowerCase()
                        + ")";
            }
        } else {
            //general case
            for (ObjectIdentifier oi : result) {
                if (oi.code.contains("TargetObservation")) {
                    oi.code = "Target";
                } else if (oi.code.contains("CalibrationObservation")) {
                    oi.code = "Calibration";
                }
                //else nothing - 'code' remains as is from the query
            }
        }

        return result;
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

    private void CheckTimingWindow(TimingWindow timingWindow) throws WebApplicationException
    {
        if(timingWindow.getStartTime().getTime() > timingWindow.getEndTime().getTime()) {
            throw new WebApplicationException("Start time cannot be after end time");
        }
    }

    @POST
    @Operation(summary = "add a new Observation to the given ObservingProposal")
    @Consumes(MediaType.APPLICATION_JSON)
    @ResponseStatus(201)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Observation addNewObservation(@PathParam("proposalCode") Long proposalCode,
                                         Observation observation)
    {
        for(ObservingConstraint constraint : observation.getConstraints()) {
            if(constraint.getClass() == TimingWindow.class) {
                CheckTimingWindow((TimingWindow) constraint);
            }
        }

        ObservingProposal observingProposal = findObject(ObservingProposal.class, proposalCode);
        // Notice the use of 'copyMe' to clone any input observation in case it has been cloned
        // in the GUI and has any database Ids in it. Also note that if Observation were not
        // abstract then copy constructor would be the correct thing to do.
        return addNewChildObject(observingProposal, observation.copyMe(),
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
    @Operation(summary = "replace the list of Target(s) of the Observation for the given ObservingProposal")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceTargets(@PathParam("proposalCode") Long proposalCode,
                                  @PathParam("observationId") Long observationId,
                                  List<Target> targets)
            throws WebApplicationException
    {
        Observation observation = findChildByQuery(ObservingProposal.class, Observation.class,
                "observations", proposalCode, observationId);


        //TODO: Check each target is real and belongs to this proposal?
        observation.setTarget(targets);

        //observation.getTarget().replaceAll(t -> {if(t.getId() == target.getId()) return target; else return t;});//IMPL it would be nice if generated code had replace in list.

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

    @PUT
    @Path("{observationId}/calibrationIntendedUse")
    @Operation(summary = "replace the IntendedUse of the given CalibrationObservation")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceIntendedUse(@PathParam("proposalCode") Long proposalCode,
                                       @PathParam("observationId") Long observationId,
                                       CalibrationTarget_intendedUse replacementUse)
        throws WebApplicationException
    {
        Observation observingProposal = findChildByQuery(ObservingProposal.class, Observation.class,
                "observations", proposalCode, observationId);

        if (observingProposal instanceof CalibrationObservation) {
            ((CalibrationObservation) observingProposal).setIntent(replacementUse);
        } else {
            throw new WebApplicationException(
                    String.format("Observation with id %d is NOT a CalibrationObservation", observationId)
            );
        }

        return responseWrapper(observingProposal, 201);
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

        if(constraint.getClass() == TimingWindow.class) {
            CheckTimingWindow((TimingWindow) constraint);
         }

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
        CheckTimingWindow(replacementWindow);

        Observation observation = findChildByQuery(ObservingProposal.class, Observation.class,
                "observations", proposalCode, observationId);

        TimingWindow window = findChildByQuery(Observation.class, TimingWindow.class,
                "constraints", observationId, timingWindowId);

        //WORK-AROUND: changes to 'isAvoidConstraint' and/or 'note' fields only won't update the window,
        //so we nudge the 'endTime' (or could use startTime) by a millisecond to get the window to update
        if (window.getStartTime().getTime() == replacementWindow.getStartTime().getTime() &&
                window.getEndTime().getTime() == replacementWindow.getEndTime().getTime())
        {
            Date d = new Date();

            //randomise the sign of the nudge such that the long term mean is zero
            long nudge = d.getTime() % 2 == 0 ? 1L : -1L;

            replacementWindow.setEndTime(new Date(replacementWindow.getEndTime().getTime() + nudge));
        }

        window.updateUsing(replacementWindow);

        em.merge(window);

        return window;
    }


}
