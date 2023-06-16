package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 16/03/2022 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.prop.*;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.jboss.resteasy.reactive.RestQuery;
import org.orph2020.pst.common.json.ObjectIdentifier;
import org.orph2020.pst.common.json.ProposalSynopsis;

import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

/*
   For use cases see:
         https://gitlab.com/opticon-radionet-pilot/proposal-submission-tool/requirements/-/blob/main/UseCases.adoc
 */
//TODO - should really ensure that submitted proposals are not editable even via the direct {proposalCode} route

//TODO - split more parameters of ObservingProposals out of this source file into their own files and provide more specific tags
@Path("proposals")
@Tag(name = "proposals")
@Produces(MediaType.APPLICATION_JSON)
public class ProposalResource extends ObjectResourceBase {
    private final Logger logger;

    public ProposalResource(Logger logger) {
        this.logger = logger;
    }

    private static final String proposalRoot = "{proposalCode}";

    private static final String targetsRoot = proposalRoot + "/targets";
    private static final String fieldsRoot = proposalRoot + "/fields";
    private static final String techGoalsRoot = proposalRoot + "/technicalGoals";

    private List<ProposalSynopsis> getSynopses(String queryStr) {
        List<ProposalSynopsis> result = new ArrayList<>();
        Query query = em.createQuery(queryStr);
        List<Object[]> results = query.getResultList();
        for (Object[] r : results) {
            result.add(
                    new ProposalSynopsis((long) r[0], (String) r[1], (String) r[2], (ProposalKind) r[3],
                            (Boolean) r[4])
            );
        }
        return result;
    }

    //TODO: these private functions to find a Child object contained in the Parent object are being
    //repeated often - could probably generalise this function in the 'ObjectResourceBase' class
    private Target findTargetByQuery(long proposalCode, long targetId) {
        TypedQuery<Target> q = em.createQuery(
                "select t from ObservingProposal p join p.targets t where p._id = :pid and t._id = :tid",
                Target.class
        );
        q.setParameter("pid", proposalCode);
        q.setParameter("tid", targetId);
        return q.getSingleResult();
    }


    @GET
    @Operation(summary = "get the synopsis for each Proposal in the database, optionally provide an investigator name and/or a proposal title to see specific proposals")
    public List<ProposalSynopsis> getProposals(@RestQuery String investigatorName, @RestQuery String title) {

        boolean noQuery = investigatorName == null && title == null;
        boolean investigatorOnly = investigatorName != null && title == null;
        boolean titleOnly = investigatorName == null && title != null;

        //if 'ProposalSynopsis' is modified we should check these Strings for suitability
        String baseStr = "select o._id,o.title,o.summary,o.kind,o.submitted from ObservingProposal o ";
        String orderByStr = "order by o.title";
        String investigatorLikeStr = "inner join o.investigators i where i.person.fullName like '" +investigatorName+ "' ";
        String titleLikeStr = "o.title like '" +title+ "' ";


        if (noQuery) {
            return getSynopses(baseStr + orderByStr);
        } else if (investigatorOnly) {
            return getSynopses(baseStr + investigatorLikeStr + orderByStr);
        } else if (titleOnly) {
            return getSynopses(baseStr + "where " + titleLikeStr + orderByStr);
        } else { //name and title given as queries
            return getSynopses(baseStr + investigatorLikeStr + "and " + titleLikeStr + orderByStr);
        }
    }


    @GET
    @Operation(summary = "get the Proposal specified by the 'proposalCode'")
    @APIResponse(
            responseCode = "200",
            description = "get a single Proposal specified by the code"
    )
    @Path(proposalRoot)
    public ObservingProposal getObservingProposal(@PathParam("proposalCode") Long proposalCode)
            throws WebApplicationException
    {
        return findObject(ObservingProposal.class, proposalCode);
    }

    @POST
    @Operation(summary = "create a new Proposal in the database")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response createObservingProposal(ObservingProposal op)
            throws WebApplicationException
    {
        return persistObject(op);
    }

    @DELETE
    @Path(proposalRoot)
    @Operation(summary = "remove the ObservingProposal specified by the 'proposalCode'")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response deleteObservingProposal(@PathParam("proposalCode") long code)
            throws WebApplicationException
    {
        return removeObject(ObservingProposal.class, code);
    }


    //********************** TITLE ***************************

    @GET
    @Path(proposalRoot + "/title")
    @Operation(summary = "get the title of the ObservingProposal specified by 'proposalCode'")
    public Response getObservingProposalTitle(@PathParam("proposalCode") Long proposalCode) {
        ObservingProposal proposal = findObject(ObservingProposal.class, proposalCode);
        return responseWrapper(proposal.getTitle(), 200);
    }


    @PUT
    @Operation(summary = "change the title of an ObservingProposal")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    @Path(proposalRoot +"/title")
    public Response replaceTitle(
            @PathParam("proposalCode") long proposalCode,
            String replacementTitle)
            throws WebApplicationException
    {
        ObservingProposal proposal = findObject(ObservingProposal.class, proposalCode);

        proposal.setTitle(replacementTitle);

        return responseWrapper(proposal, 201);
    }

    //********************** SUMMARY ***************************
    @PUT
    @Operation(summary = "replace the summary of an ObservingProposal")
    @Path(proposalRoot +"/summary")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceSummary(@PathParam("proposalCode") long proposalCode, String replacementSummary)
            throws WebApplicationException
    {
        ObservingProposal proposal = findObject(ObservingProposal.class, proposalCode);

        proposal.setSummary(replacementSummary);

        return responseWrapper(proposal, 201);
    }

    //********************** KIND ***************************

    @GET
    @Path(proposalRoot + "/kind")
    @Operation(summary = "get the 'kind' of ObservingProposal specified by the 'proposalCode")
    public ProposalKind getObservingProposalKind(@PathParam("proposalCode") Long proposalCode) {
        ObservingProposal proposal = findObject(ObservingProposal.class, proposalCode);
        return proposal.getKind();
    }

    @PUT
    @Operation(summary = "change the 'kind' of the ObservingProposal specified, one-of: STANDARD, TOO, SURVEY")
    @Path(proposalRoot +"/kind")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response changeKind(@PathParam("proposalCode") long proposalCode, String kind)
            throws WebApplicationException
    {
        ObservingProposal proposal = findObject(ObservingProposal.class, proposalCode);

        try{
            proposal.setKind(ProposalKind.fromValue(kind));
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(e.getMessage(), 422);
        }

        return responseWrapper(proposal, 201);
    }

    //********************** JUSTIFICATIONS ***************************

    @GET
    @Path(proposalRoot +"/justifications/{which}")
    @Operation(summary = "get the technical or scientific justification associated with the ObservingProposal specified by 'proposalCode'")
    public Justification getJustification(@PathParam("proposalCode") Long proposalCode,
                                          @PathParam("which") String which)
        throws WebApplicationException
    {
        ObservingProposal observingProposal = findObject(ObservingProposal.class, proposalCode);

        switch (which) {
            case "technical":
            {
                return observingProposal.getTechnicalJustification();
            }

            case "scientific":
            {
                return observingProposal.getScientificJustification();
            }

            default:
            {
                throw new WebApplicationException(
                        String.format("Justifications are either 'technical' or 'scientific', I got %s", which),
                        400
                );
            }
        }
    }


    @PUT
    @Operation( summary = "update a technical or scientific Justification in the ObservingProposal specified by the 'proposalCode'")
    @Path(proposalRoot +"/justifications/{which}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn={WebApplicationException.class})
    public Justification updateJustification(
            @PathParam("proposalCode") long proposalCode,
            @PathParam("which") String which,
            Justification incoming )
            throws WebApplicationException
    {
        ObservingProposal proposal = findObject(ObservingProposal.class, proposalCode);

        switch (which)
        {
            case "technical":
            {
                return addNewChildObject(proposal, incoming, proposal::setTechnicalJustification);
            }

            case "scientific":
            {
                return addNewChildObject(proposal, incoming, proposal::setScientificJustification);
            }

            default:
            {
                throw new WebApplicationException(
                        String.format("Justifications are either 'technical' or 'scientific', I got %s", which),
                        418);
            }
        }
    }

    //********************** RELATED PROPOSALS ***************************
    @PUT
    @Operation(summary = "add a RelatedProposal to the ObservingProposal specified by the 'proposalCode'")
    @Path("{proposalCode}/relatedProposals")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response addRelatedProposal(@PathParam("proposalCode") Long proposalCode,
                                       Long relatedProposalCode)
            throws WebApplicationException
    {
        if (proposalCode.equals(relatedProposalCode)) {
            throw new WebApplicationException(
                    "ObservingProposal cannot refer to itself as a RelatedProposal", 418);
        }

        ObservingProposal proposal = findObject(ObservingProposal.class, proposalCode);

        ObservingProposal relatedProposal = findObject(ObservingProposal.class, relatedProposalCode);

        proposal.addToRelatedProposals(new RelatedProposal(relatedProposal));

        return responseWrapper(proposal, 201);
    }

    //********** Observation References ************************************

    @GET
    @Path(targetsRoot)
    @Operation(summary = "get the list of ObjectIdentifiers for the targets associated with the given ObservingProposal, optionally provide a sourceName as a query to get that particular Observation's identifier")
    public List<ObjectIdentifier> getTargets(@PathParam("proposalCode") Long proposalCode,
                                             @RestQuery String sourceName)
            throws WebApplicationException
    {
        if (sourceName == null) {
            return getObjects("SELECT t._id,t.sourceName FROM ObservingProposal o Inner Join o.targets t WHERE o._id = '"+proposalCode+"' ORDER BY t.sourceName");
        } else {
            return getObjects("SELECT t._id,t.sourceName FROM ObservingProposal o Inner Join o.targets t WHERE o._id = '"+proposalCode+"' and t.sourceName like '"+sourceName+"' ORDER BY t.sourceName");
        }

    }

    @GET
    @Path (targetsRoot + "/{targetId}")
    @Operation(summary = "get a specific Target for the given ObservingProposal")
    public Target getTarget(@PathParam("proposalCode") Long proposalCode,
                            @PathParam("targetId") Long targetId)
        throws WebApplicationException
    {
        return findTargetByQuery(proposalCode, targetId);
    }

    @POST
    @Path(targetsRoot)
    @Operation(summary = "add a new Target to the given ObservingProposal")
    @Consumes(MediaType.APPLICATION_JSON)
    @ResponseStatus(201)
    @Transactional
    public Target addNewTarget(@PathParam("proposalCode") Long proposalCode, Target target)
    {
        ObservingProposal observingProposal = findObject(ObservingProposal.class, proposalCode);
        return addNewChildObject(observingProposal,target, observingProposal::addToTargets);
    }


    @DELETE
    @Path(targetsRoot+"/{targetId}")
    @Operation(summary = "remove the Target specified by 'id' from the given ObservingProposal")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response removeTarget(@PathParam("proposalCode") Long proposalCode, @PathParam("targetId") Long targetId)
            throws WebApplicationException
    {
        ObservingProposal observingProposal = findObject(ObservingProposal.class, proposalCode);

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
            return getObjects("SELECT t._id,t.name FROM ObservingProposal o Inner Join o.fields t WHERE o._id = '"+proposalCode+"' ORDER BY t.name");
        } else {
            return getObjects("SELECT t._id,t.name FROM ObservingProposal o Inner Join o.fields t WHERE o._id = '"+proposalCode+"' and t.name like '"+fieldName+"' ORDER BY t.name");
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
        ObservingProposal observingProposal = findObject(ObservingProposal.class, proposalCode);
        return addNewChildObject(observingProposal, field, observingProposal::addToFields);
    }

    @DELETE
    @Path(fieldsRoot+"/{fieldId}")
    @Operation(summary = "remove the Field specified by 'id' from the given ObservingProposal")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response removeField(@PathParam("proposalCode") Long proposalCode, @PathParam("fieldId") Long fieldId)
            throws WebApplicationException
    {
        ObservingProposal observingProposal = findObject(ObservingProposal.class, proposalCode);

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
        ObservingProposal observingProposal = findObject(ObservingProposal.class, proposalCode);
        return addNewChildObject(observingProposal,technicalGoal, observingProposal::addToTechnicalGoals);
    }


    @DELETE
    @Path(techGoalsRoot+"/{techGoalId}")
    @Operation(summary = "remove the Technical Goal specified by 'techGoalId' from the given ObservingProposal")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response removeTechGoal(@PathParam("proposalCode") Long proposalCode, @PathParam("techGoalId") Long techGoalId)
            throws WebApplicationException
    {
        ObservingProposal observingProposal = findObject(ObservingProposal.class, proposalCode);

        TechnicalGoal target = observingProposal.getTechnicalGoals().stream().filter(o -> techGoalId.equals(o.getId())).findAny()
                .orElseThrow(() -> new WebApplicationException(
                        String.format(NON_ASSOCIATE_ID, "Technical Goal", techGoalId, "ObservingProposal", proposalCode)
                ));
        observingProposal.removeFromTechnicalGoals(target);
        return responseWrapper(observingProposal, 201);
    }

    //Other fields of an ObservingProposal have been split out into their own source file
}
