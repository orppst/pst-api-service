package org.orph2020.pst.apiimpl.rest;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.prop.*;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.jboss.resteasy.reactive.RestQuery;
import org.orph2020.pst.common.json.ObjectIdentifier;

import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("proposals/{proposalCode}/observations")
@Tag(name = "proposals-observations")
@Produces(MediaType.APPLICATION_JSON)
public class ObservationResource extends ObjectResourceBase {

    private static final String targetsRoot = "/targets";
    private static final String fieldsRoot = "/fields";
    private static final String techGoalsRoot = "/technicalGoals";


    private Observation findObservation(List<Observation> observations, Long id, Long proposalCode)
            throws WebApplicationException
    {
        return observations.stream().filter(o -> id.equals(o.getId())).findAny()
                .orElseThrow(() -> new WebApplicationException(
                        String.format(NON_ASSOCIATE_ID, "Observation", id, "ObservingProposal", proposalCode)
                ));
    }


    @GET
    @Operation(summary = "get the list of ObjectIdentifiers for the Observations associated with the given ObservingProposal, optionally provide a fieldName as a query to get that particular Observation's identifier")
    public List<ObjectIdentifier> getObservations(@PathParam("proposalCode") Long proposalCode,
                                                  @RestQuery String fieldName)
            throws WebApplicationException
    {
        if (fieldName == null) {
            return super.getObjects("SELECT t._id,f.name FROM ObservingProposal p Inner Join p.observations t Inner Join t.field f WHERE p._id = '"+proposalCode+"' ORDER BY f.name");
        } else {
            return super.getObjects("SELECT t._id,f.name FROM ObservingProposal p Inner Join p.observations t Inner Join t.field f WHERE p._id = '"+proposalCode+"' and f.name like '"+fieldName+"' ORDER BY f.name");
        }
    }

    @GET
    @Path("targetObservations")
    @Operation(summary = "get the ObjectIdentifiers for the TargetObservations associated with the given ObservingProposal")
    public List<ObjectIdentifier> getTargetObservations(@PathParam("proposalCode") Long proposalCode)
    {

        return super.getObjects("SELECT o._id,t.sourceName FROM ObservingProposal p Inner Join p.observations o Inner Join o.target t WHERE p._id = '"+proposalCode+"' AND type(o) = 'proposal:TargetObservation' ORDER BY t.sourceName");

    }

    @GET
    @Path("calibrationObservations")
    @Operation(summary = "get the ObjectIdentifiers for the CalibrationObservations associated with the given ObservingProposal")
    public List<ObjectIdentifier> getCalibrationObservations(@PathParam("proposalCode") Long proposalCode)
    {
        return super.getObjects("SELECT o._id,t.target.sourceName FROM ObservingProposal p Inner Join p.observations o Inner Join o.target t WHERE p._id = '"+proposalCode+"' AND type(o) = 'proposal:CalibrationObservation' ORDER BY t.sourceName");
    }

    @GET
    @Path(targetsRoot)
    @Operation(summary = "get the list of ObjectIdentifiers for the targets associated with the given ObservingProposal, optionally provide a sourceName as a query to get that particular Observation's identifier")
    public List<ObjectIdentifier> getTargets(@PathParam("proposalCode") Long proposalCode,
                                             @RestQuery String sourceName)
            throws WebApplicationException
    {
        if (sourceName == null) {
            return super.getObjects("SELECT t._id,t.sourceName FROM ObservingProposal o Inner Join o.targets t WHERE o._id = '"+proposalCode+"' ORDER BY t.sourceName");
        } else {
            return super.getObjects("SELECT t._id,t.sourceName FROM ObservingProposal o Inner Join o.targets t WHERE o._id = '"+proposalCode+"' and t.sourceName like '"+sourceName+"' ORDER BY t.sourceName");
        }

    }

    @POST
    @Path(targetsRoot)
    @Operation(summary = "add a new Target to the given ObservingProposal")
    @Consumes(MediaType.APPLICATION_JSON)
    @ResponseStatus(201)
    @Transactional
    public Target addNewTarget(@PathParam("proposalCode") Long proposalCode, Target target)
    {
        ObservingProposal observingProposal = super.findObject(ObservingProposal.class, proposalCode);
        return super.addNewChildObject(observingProposal,target, observingProposal::addToTargets);
    }


    @DELETE
    @Path(targetsRoot+"/{targetId}")
    @Operation(summary = "remove the Target specified by 'id' from the given ObservingProposal")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response removeTarget(@PathParam("proposalCode") Long proposalCode, @PathParam("targetId") Long targetId)
            throws WebApplicationException
    {
        ObservingProposal observingProposal = super.findObject(ObservingProposal.class, proposalCode);

        Target target = observingProposal.getTargets().stream().filter(o -> targetId.equals(o.getId())).findAny()
                .orElseThrow(() -> new WebApplicationException(
                        String.format(NON_ASSOCIATE_ID, "Target", targetId, "ObservingProposal", proposalCode)
                ));
        observingProposal.removeFromTargets(target);
        return responseWrapper(observingProposal, 201);
    }


    // field operations
    @GET
    @Path(fieldsRoot)
    @Operation(summary = "get the list of ObjectIdentifiers for the Fields associated with the given ObservingProposal, optionally provide a name as a query to get that particular Fields's identifier")
    public List<ObjectIdentifier> getFields(@PathParam("proposalCode") Long proposalCode,
                                            @RestQuery String fieldName)
            throws WebApplicationException
    {
        if (fieldName == null) {
            return super.getObjects("SELECT t._id,t.name FROM ObservingProposal o Inner Join o.fields t WHERE o._id = '"+proposalCode+"' ORDER BY t.name");
        } else {
            return super.getObjects("SELECT t._id,t.name FROM ObservingProposal o Inner Join o.fields t WHERE o._id = '"+proposalCode+"' and t.name like '"+fieldName+"' ORDER BY t.name");
        }

    }

    @POST
    @Path(fieldsRoot)
    @Operation(summary = "add a new Field to the given ObservingProposal")
    @Consumes(MediaType.APPLICATION_JSON)
    @ResponseStatus(201)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Field addNewField(@PathParam("proposalCode") Long proposalCode,
                             Field field)
    {
        ObservingProposal observingProposal = super.findObject(ObservingProposal.class, proposalCode);
        return addNewChildObject(observingProposal, field, observingProposal::addToFields);
    }

    @DELETE
    @Path(fieldsRoot+"/{fieldId}")
    @Operation(summary = "remove the Field specified by 'id' from the given ObservingProposal")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response removeField(@PathParam("proposalCode") Long proposalCode, @PathParam("fieldId") Long fieldId)
            throws WebApplicationException
    {
        ObservingProposal observingProposal = super.findObject(ObservingProposal.class, proposalCode);

        Field field = observingProposal.getFields().stream().filter(o -> fieldId.equals(o.getId())).findAny()
                .orElseThrow(() -> new WebApplicationException(
                        String.format(NON_ASSOCIATE_ID, "Field", fieldId, "ObservingProposal", proposalCode)
                ));
        observingProposal.removeFromFields(field);
        return responseWrapper(observingProposal, 201);
    }


    // technicalGoals
    @GET
    @Path(techGoalsRoot)
    @Operation(summary = "get the list of TechnicalGoals associated with the given ObservingProposal")
    public List<TechnicalGoal> getTechGoals(@PathParam("proposalCode") Long proposalCode)
    {
        TypedQuery<TechnicalGoal> q = em.createQuery("SELECT t FROM ObservingProposal o Inner Join o.technicalGoals t WHERE o._id = '" + proposalCode + "'", TechnicalGoal.class);
        return q.getResultList();
    }

    @POST
    @Path(techGoalsRoot)
    @Operation(summary = "add a new technical goal to the given ObservingProposal")
    @Consumes(MediaType.APPLICATION_JSON)
    @ResponseStatus(201)
    @Transactional
    public TechnicalGoal addNewTechGoal(@PathParam("proposalCode") Long proposalCode, TechnicalGoal technicalGoal)
            throws WebApplicationException
    {
        ObservingProposal observingProposal = super.findObject(ObservingProposal.class, proposalCode);
        return super.addNewChildObject(observingProposal,technicalGoal, observingProposal::addToTechnicalGoals);
    }


    @DELETE
    @Path(techGoalsRoot+"/{techGoalId}")
    @Operation(summary = "remove the Technical Goal specified by 'id' from the given ObservingProposal")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response removeTechGoal(@PathParam("proposalCode") Long proposalCode, @PathParam("techGoalId") Long techGoalId)
            throws WebApplicationException
    {
        ObservingProposal observingProposal = super.findObject(ObservingProposal.class, proposalCode);

        TechnicalGoal target = observingProposal.getTechnicalGoals().stream().filter(o -> techGoalId.equals(o.getId())).findAny()
                .orElseThrow(() -> new WebApplicationException(
                        String.format(NON_ASSOCIATE_ID, "Technical Goal", techGoalId, "ObservingProposal", proposalCode)
                ));
        observingProposal.removeFromTechnicalGoals(target);
        return responseWrapper(observingProposal, 201);
    }

    @POST
    @Operation(summary = "add a new Observation to the given ObservingProposal")
    @Consumes(MediaType.APPLICATION_JSON)
    @ResponseStatus(201)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Observation addNewObservation(@PathParam("proposalCode") Long proposalCode,
                                               Observation observation)
    {
        ObservingProposal observingProposal = super.findObject(ObservingProposal.class, proposalCode);
        return super.addNewChildObject(observingProposal,observation, observingProposal::addToObservations);
    }


    @PUT
    @Operation(summary = "add an existing Observation to the given ObservingProposal")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response addObservation(@PathParam("proposalCode") Long proposalCode, Long observationId)
            throws WebApplicationException
    {
        ObservingProposal proposal = findObject(ObservingProposal.class, proposalCode);

        Observation observation = findObject(Observation.class, observationId);

        for (Observation o : proposal.getObservations()) {
            if (o.getId().equals(observationId)) {
                throw new WebApplicationException(
                        String.format("Observation with id: %d already added to ObservingProposal %s",
                                observationId, proposalCode), 422);
            }
        }

        proposal.addToObservations(observation);

        return responseWrapper(proposal, 201);
    }

    @DELETE
    @Path("/{observationId}")
    @Operation(summary = "remove the Observation specified by 'observationId' from the given ObservingProposal")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response removeObservation(@PathParam("proposalCode") Long proposalCode, @PathParam("observationId") Long id)
            throws WebApplicationException
    {
        ObservingProposal observingProposal = super.findObject(ObservingProposal.class, proposalCode);
        Observation observation =
                findObservation(observingProposal.getObservations(), id, proposalCode);

        observingProposal.removeFromObservations(observation);

        return responseWrapper(observingProposal, 201);
    }

    @PUT
    @Path("/{observationId}/target")
    @Operation(summary = "replace the Target of the Observation for the given ObservingProposal")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceTarget(@PathParam("proposalCode") Long proposalCode, @PathParam("observationId") Long id,
                                  Target target)
            throws WebApplicationException
    {
        ObservingProposal observingProposal = super.findObject(ObservingProposal.class, proposalCode);
        Observation observation =
                findObservation(observingProposal.getObservations(), id, proposalCode);
        observation.setTarget(target);

        return responseWrapper(observingProposal, 201);
    }

    @PUT
    @Path("/{observationId}/field")
    @Operation(summary = "replace the Field of the given Observation for the given ObservingProposal")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceField(@PathParam("proposalCode") Long proposalCode, @PathParam("observationId") Long id,
                                 Field field)
            throws WebApplicationException
    {
        ObservingProposal observingProposal = super.findObject(ObservingProposal.class, proposalCode);
        Observation observation =
                findObservation(observingProposal.getObservations(), id, proposalCode);
        observation.setField(field);

        return responseWrapper(observingProposal, 201);
    }

    @PUT
    @Path("/{observationId}/technicalGoal")
    @Operation(summary = "replace the TechnicalGoal of the given Observation for the given ObservingProposal")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceTechnicalGoal(@PathParam("proposalCode") Long proposalCode, @PathParam("observationId") Long id,
                                         TechnicalGoal technicalGoal)
            throws WebApplicationException
    {
        ObservingProposal observingProposal = super.findObject(ObservingProposal.class, proposalCode);
        Observation observation =
                findObservation(observingProposal.getObservations(), id, proposalCode);
        observation.setTechnicalGoal(technicalGoal);

        return responseWrapper(observingProposal, 201);
    }

    @GET
    @Path("/{observationId}/constraints")
    @Operation(summary = "get the list of Constraints for the given Observation in the given ObservingProposal")
    public List<Constraint> getConstraints(@PathParam("proposalCode") Long proposalCode,
                                           @PathParam("observationId") Long id)
            throws WebApplicationException
    {
        ObservingProposal observingProposal = super.findObject(ObservingProposal.class, proposalCode);
        Observation observation = findObservation(observingProposal.getObservations(), id, proposalCode);
        return observation.getConstraints();
    }

    @POST
    @Path("/{observationId}/constraints")
    @Operation(summary = "add a new Constraint to the Observation specified by 'id' in the given ObservingProposal")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response addNewConstraint(@PathParam("proposalCode") Long proposalCode, @PathParam("observationId") Long id,
                                     Constraint constraint)
            throws WebApplicationException
    {
        ObservingProposal observingProposal = super.findObject(ObservingProposal.class, proposalCode);
        Observation observation =
                findObservation(observingProposal.getObservations(), id, proposalCode);
        observation.addToConstraints(constraint);

        return super.mergeObject(observingProposal);
    }

    @DELETE
    @Path("/{observationId}/constraints/{constraintId}")
    @Operation(summary = "remove the specified Constraint from the Observation of the given ObservationProposal")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response removeConstraint(@PathParam("proposalCode") Long proposalCode, @PathParam("observationId") Long id,
                                     @PathParam("constraintId") Long constraintId)
            throws WebApplicationException
    {
        ObservingProposal observingProposal = super.findObject(ObservingProposal.class, proposalCode);
        Observation observation = findObservation(observingProposal.getObservations(), id, proposalCode);
        List<Constraint> constraints = observation.getConstraints();

        Constraint constraint = constraints
                .stream().filter(o -> constraintId.equals(o.getId())).findAny()
                .orElseThrow(() -> new WebApplicationException(
                        String.format(NON_ASSOCIATE_ID, "Constraint", constraintId, "Observation", id)
                ));

        observation.removeFromConstraints(constraint);

        return responseWrapper(observingProposal, 201);
    }


}
