package org.orph2020.pst.apiimpl.rest;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.prop.*;
import org.orph2020.pst.common.json.ObjectIdentifier;

import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("calibrationObservations")
@Tag(name = "proposal-tool-observations-calibration")
@Produces(MediaType.APPLICATION_JSON)
public class CalibrationObservationResource extends ObjectResourceBase{

    @GET
    @Operation(summary = "get all CalibrationObservations from the database")
    public List<ObjectIdentifier> getCalibrationObservations() {
        return super.getObjects("SELECT o._id,o.target.sourceName FROM CalibrationObservation o ORDER BY o.target.sourceName");
    }


    @GET
    @Path("{id}")
    @Operation(summary = "get the CalibrationObservation specified by the 'id'")
    public CalibrationObservation getTargetObservation(@PathParam("id") Long id) {
        return super.findObject(CalibrationObservation.class, id);
    }

    @POST
    @Operation(summary = "create a new CalibrationObservation in the database")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response createObservation(CalibrationObservation observation)
            throws WebApplicationException
    {
        return super.persistObject(observation);
    }

    @DELETE
    @Path("{id}")
    @Operation(summary = "delete the CalibrationObservation specified by the 'id' from the database")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response deleteObservation(@PathParam("id") Long id)
            throws WebApplicationException
    {
        return super.removeObject(CalibrationObservation.class, id);
    }

    @PUT
    @Path("{id}/constraints")
    @Operation(summary = "add a constraint to the specified CalibrationObservation")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response addConstraint(@PathParam("id") Long observationId, Long constraintId)
            throws WebApplicationException
    {
        CalibrationObservation observation = super.findObject(CalibrationObservation.class, observationId);

        Constraint constraint = super.findObject(Constraint.class, constraintId);

        observation.addConstraints(constraint);

        return responseWrapper(observation, 201);
    }

    @PUT
    @Path("{id}/target")
    @Operation(summary = "replace the target for the specified CalibrationObservation")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceTarget(@PathParam("id") Long observationId, Long targetId)
            throws WebApplicationException
    {
        CalibrationObservation observation = super.findObject(CalibrationObservation.class, observationId);

        Target target = super.findObject(Target.class, targetId);

        observation.setTarget(target);

        return responseWrapper(observation, 201);
    }

    @PUT
    @Path("{id}/field")
    @Operation(summary = "replace the field for the specified CalibrationObservation")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceField(@PathParam("id") Long observationId, Long fieldId)
            throws WebApplicationException
    {
        CalibrationObservation observation = super.findObject(CalibrationObservation.class, observationId);

        Field field = super.findObject(Field.class, fieldId);

        observation.setField(field);

        return responseWrapper(observation, 201);
    }

    @PUT
    @Path("{id}/technicalGoal")
    @Operation(summary = "replace the technical goal for the specified CalibrationObservation")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceTechnicalGoal(@PathParam("id") Long observationId, Long technicalGoalId)
            throws WebApplicationException
    {
        CalibrationObservation observation = super.findObject(CalibrationObservation.class, observationId);

        TechnicalGoal tech = super.findObject(TechnicalGoal.class, technicalGoalId);

        observation.setTech(tech);

        return responseWrapper(observation, 201);
    }

    @PUT
    @Path("{id}/intent")
    @Operation(summary = "update the intent for the specified CalibrationObservation; one of AMPLITUDE, ATMOSPHERIC, BANDPASS, PHASE, POINTING, FOCUS, POLARIZATION, DELAY")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateIntent(@PathParam("id") Long observationId, String intentStr)
            throws WebApplicationException
    {
        CalibrationObservation observation = super.findObject(CalibrationObservation.class, observationId);

        try {
            observation.setIntent(CalibrationTarget_intendedUse.fromValue(intentStr));
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(e, 400);
        }

        return responseWrapper(observation, 201);
    }
}
