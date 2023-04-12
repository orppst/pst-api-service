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

@Path("targetObservations")
@Tag(name="proposal-tool-observations-target")
@Produces(MediaType.APPLICATION_JSON)
public class TargetObservationResource extends ObjectResourceBase {

    @GET
    @Operation(summary = "get all TargetObservations in the database")
    public List<ObjectIdentifier> getTargetObservations() {
        return super.getObjects("SELECT o._id,o.target.sourceName FROM TargetObservation o ORDER BY o.target.sourceName");
    }

    @GET
    @Path("{id}")
    @Operation(summary = "get the TargetObservation specified by the 'id'")
    public TargetObservation getTargetObservation(@PathParam("id") Long id) {
        return super.findObject(TargetObservation.class, id);
    }

    @POST
    @Operation(summary = "create a new TargetObservation in the database")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response createObservation(TargetObservation observation)
            throws WebApplicationException
    {
        return super.persistObject(observation);
    }

    @DELETE
    @Path("{id}")
    @Operation(summary = "delete the TargetObservation specified by the 'id' from the database")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response deleteObservation(@PathParam("id") Long id)
            throws WebApplicationException
    {
        return super.removeObject(TargetObservation.class, id);
    }

    @PUT
    @Path("{id}/constraints")
    @Operation(summary = "add a constraint to the specified TargetObservation")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response addConstraint(@PathParam("id") Long observationId, Long constraintId)
            throws WebApplicationException
    {
        TargetObservation observation = super.findObject(TargetObservation.class, observationId);

        Constraint constraint = super.findObject(Constraint.class, constraintId);

        observation.addConstraints(constraint);

        return responseWrapper(observation, 201);
    }

    @PUT
    @Path("{id}/target")
    @Operation(summary = "replace the target for the specified TargetObservation")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceTarget(@PathParam("id") Long observationId, Long targetId)
            throws WebApplicationException
    {
        TargetObservation observation = super.findObject(TargetObservation.class, observationId);

        Target target = super.findObject(Target.class, targetId);

        observation.setTarget(target);

        return responseWrapper(observation, 201);
    }

    @PUT
    @Path("{id}/field")
    @Operation(summary = "replace the field for the specified TargetObservation")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceField(@PathParam("id") Long observationId, Long fieldId)
            throws WebApplicationException
    {
        TargetObservation observation = super.findObject(TargetObservation.class, observationId);

        Field field = super.findObject(Field.class, fieldId);

        observation.setField(field);

        return responseWrapper(observation, 201);
    }

    @PUT
    @Path("{id}/technicalGoal")
    @Operation(summary = "replace the technical goal for the specified TargetObservation")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceTechnicalGoal(@PathParam("id") Long observationId, Long technicalGoalId)
            throws WebApplicationException
    {
        TargetObservation observation = super.findObject(TargetObservation.class, observationId);

        TechnicalGoal tech = super.findObject(TechnicalGoal.class, technicalGoalId);

        observation.setTech(tech);

        return responseWrapper(observation, 201);
    }

}
