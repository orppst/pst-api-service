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
                                                  @RestQuery String srcName, @RestQuery ObsType type)
            throws WebApplicationException
    {
        String tquery="";
        if(type != null)
        {
            tquery = " and Type(o) = "+type.name();
        }
        if (srcName == null) {
            return getObjectIdentifiers("SELECT o._id,t.sourceName FROM ObservingProposal p Inner Join p.observations o Inner Join  o.target t WHERE p._id = "+proposalCode+" "+tquery+" ORDER BY t.sourceName");
        } else {
            return getObjectIdentifiers("SELECT o._id,t.sourceName FROM ObservingProposal p Inner Join p.observations o Inner Join  o.target t WHERE p._id = "+proposalCode+" "+tquery+" and t.sourceName like '"+srcName+"' ORDER BY t.sourceName");
        }
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
        return addNewChildObject(observingProposal,observation, observingProposal::addToObservations);
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
        ObservingProposal observingProposal = findObject(ObservingProposal.class, proposalCode);
        Observation observation =
                findObservation(observingProposal.getObservations(), id, proposalCode);
        return deleteChildObject(observingProposal,observation, observingProposal::removeFromObservations);
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
        ObservingProposal observingProposal = findObject(ObservingProposal.class, proposalCode);
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
        ObservingProposal observingProposal = findObject(ObservingProposal.class, proposalCode);
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
        ObservingProposal observingProposal = findObject(ObservingProposal.class, proposalCode);
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
        ObservingProposal observingProposal = findObject(ObservingProposal.class, proposalCode);
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
        ObservingProposal observingProposal = findObject(ObservingProposal.class, proposalCode);
        Observation observation =
                findObservation(observingProposal.getObservations(), id, proposalCode);
        observation.addToConstraints(constraint);

        return mergeObject(observingProposal);
    }

    @DELETE
    @Path("/{observationId}/constraints/{constraintId}")
    @Operation(summary = "remove the specified Constraint from the Observation of the given ObservationProposal")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response removeConstraint(@PathParam("proposalCode") Long proposalCode, @PathParam("observationId") Long id,
                                     @PathParam("constraintId") Long constraintId)
            throws WebApplicationException
    {
        ObservingProposal observingProposal = findObject(ObservingProposal.class, proposalCode);
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
