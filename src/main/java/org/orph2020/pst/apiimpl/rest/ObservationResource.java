package org.orph2020.pst.apiimpl.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.prop.*;
import org.orph2020.pst.common.json.ObjectIdentifier;

import javax.persistence.EntityExistsException;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Produces(MediaType.APPLICATION_JSON)
@Path("observations")
@Tag(name = "proposal-tool")
public class ObservationResource extends ObjectResourceBase {

    @GET
    @Operation(summary = "get all the Observations stored in the database")
    public List<ObjectIdentifier> getObservations() {
        return super.getObjects("SELECT o._id,o.target.sourceName FROM Observation o ORDER BY o.target.sourceName");
    }

    @GET
    @Path("{id}")
    @Operation(summary = "get the specified Observation")
    public Observation getObservation(@PathParam("id") Long id)
            throws WebApplicationException
    {
        return super.findObject(Observation.class, id);
    }

    // Does this require an Observation subtype hint in the jsonString?
    @POST
    @Operation(summary = "create a new Observation in the database")
    @APIResponse(
            responseCode = "201"
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response createObservation(Observation observation)
            throws WebApplicationException
    {
        return super.persistObject(observation);
    }

    @PUT
    @Path("{id}/constraints")
    @Operation(summary = "add a constraint to the specified Observation")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response addConstraint(@PathParam("id") Long observationId, Long constraintId)
        throws WebApplicationException
    {
        Observation observation = super.findObject(Observation.class, observationId);

        Constraint constraint = super.findObject(Constraint.class, constraintId);

        observation.addConstraints(constraint);

        return responseWrapper(observation, 201);
    }

    @PUT
    @Path("{id}/target")
    @Operation(summary = "replace the target for the specified Observation")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceTarget(@PathParam("id") Long observationId, Long targetId)
            throws WebApplicationException
    {
        Observation observation = super.findObject(Observation.class, observationId);

        Target target = super.findObject(Target.class, targetId);

        observation.setTarget(target);

        return responseWrapper(observation, 201);
    }

    @PUT
    @Path("{id}/field")
    @Operation(summary = "replace the field for the specified Observation")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceField(@PathParam("id") Long observationId, Long fieldId)
            throws WebApplicationException
    {
        Observation observation = super.findObject(Observation.class, observationId);

        Field field = super.findObject(Field.class, fieldId);

        observation.setField(field);

        return responseWrapper(observation, 201);
    }

    @PUT
    @Path("{id}/technicalGoal")
    @Operation(summary = "replace the technical goal for the specified Observation")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceTech(@PathParam("id") Long observationId, Long techId)
            throws WebApplicationException
    {
        Observation observation = super.findObject(Observation.class, observationId);

        TechnicalGoal tech = super.findObject(TechnicalGoal.class, techId);

        observation.setTech(tech);

        return responseWrapper(observation, 201);
    }

    //for use with CalibrationObservation subtype only
    @PUT
    @Path("{id}/intent")
    @Operation(summary = "replace the intent for the specified CalibrationObservation; one of AMPLITUDE, ATMOSPHERIC, BANDPASS, PHASE, POINTING, FOCUS, POLARIZATION, DELAY")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceIntent(@PathParam("id") Long observationId, String intentStr)
            throws WebApplicationException
    {
        Observation observation = super.findObject(Observation.class, observationId);

        if (!observation.getClass().equals(CalibrationObservation.class)) {
            throw new WebApplicationException(
                    String.format("Observation %d is not a CalibrationObservation", observationId), 422
            );
        }

        try {
            ((CalibrationObservation) observation).setIntent(CalibrationTarget_intendedUse.fromValue(intentStr));
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(e, 400);
        }

        return responseWrapper(observation, 201);
    }

}
