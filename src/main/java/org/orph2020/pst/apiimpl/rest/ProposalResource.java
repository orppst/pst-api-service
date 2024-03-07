package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 16/03/2022 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.management.ProposalManagementModel;
import org.ivoa.dm.proposal.prop.*;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.jboss.resteasy.reactive.RestQuery;
import org.orph2020.pst.common.json.ObjectIdentifier;
import org.orph2020.pst.common.json.ProposalCycleDates;
import org.orph2020.pst.common.json.ProposalSynopsis;

import jakarta.annotation.security.RolesAllowed;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.orph2020.pst.common.json.ProposalValidation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/*
   For use cases see:
         https://gitlab.com/opticon-radionet-pilot/proposal-submission-tool/requirements/-/blob/main/UseCases.adoc
 */
//TODO - should really ensure that submitted proposals are not editable even via the direct {proposalCode} route

//TODO - split more parameters of ObservingProposals out of this source file into their own files and provide more specific tags
@Path("proposals")
@Tag(name = "proposals")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class ProposalResource extends ObjectResourceBase {
    private final Logger logger;

    public ProposalResource(Logger logger) {
        this.logger = logger;
    }
    @Inject
    private ObservationResource observationResource;
    @Inject
    private TechnicalGoalResource technicalGoalResource;
    @Inject
    private ProposalCyclesResource proposalCyclesResource;

    private static final String proposalRoot = "{proposalCode}";

    private static final String targetsRoot = proposalRoot + "/targets";
    private static final String fieldsRoot = proposalRoot + "/fields";

    // needed for import
    @Inject
    PersonResource personResource;

    @Inject
    OrganizationResource organizationResource;

    //needed for import.
    @Inject
    SupportingDocumentResource supportingDocumentResource;

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

    private ProposalSynopsis createSynopsisFromProposal(ObservingProposal proposal) {
        return new ProposalSynopsis(proposal.getId(), proposal.getTitle(), proposal.getSummary(),
                proposal.getKind(), proposal.getSubmitted());
    }

    @GET
    @Operation(summary = "get the synopsis for each Proposal in the database, optionally provide an investigator name and/or a proposal title to see specific proposals.  Filters out submitted copies.")
    public List<ProposalSynopsis> getProposals(@RestQuery String investigatorName, @RestQuery String title) {

        boolean noQuery = investigatorName == null && title == null;
        boolean investigatorOnly = investigatorName != null && title == null;
        boolean titleOnly = investigatorName == null && title != null;

        //if 'ProposalSynopsis' is modified we should check these Strings for suitability
        String baseStr = "select distinct o._id,o.title,o.summary,o.kind,o.submitted from ObservingProposal o ";
        String submittedStr = "(o.submitted is null OR not o.submitted) ";
        String orderByStr = "order by o.title";
        String investigatorLikeStr = ", Investigator i where i member of o.investigators and i.person.fullName like '" +investigatorName+ "' ";
        String titleLikeStr = "o.title like '" +title+ "' ";

        if (noQuery) {
            return getSynopses(baseStr + "where " + submittedStr + orderByStr);
        } else if (investigatorOnly) {
            return getSynopses(baseStr + investigatorLikeStr + "and " + submittedStr + orderByStr);
        } else if (titleOnly) {
            return getSynopses(baseStr + "where " + titleLikeStr + "and " + submittedStr + orderByStr);
        } else { //name and title given as queries
            return getSynopses(baseStr + investigatorLikeStr + "and " + titleLikeStr + "and " + submittedStr + orderByStr);
        }
    }

    @GET
    @Operation(summary = "get the Proposal specified by the 'proposalCode'")
    @APIResponse(
            responseCode = "200",
            description = "get a single Proposal specified by the code"
    )
    @Path(proposalRoot)
    @RolesAllowed("default-roles-orppst")
    public ObservingProposal getObservingProposal(@PathParam("proposalCode") Long proposalCode)
            throws WebApplicationException
    {
        return findObject(ObservingProposal.class, proposalCode);
    }

    @POST
    @Operation(summary = "create a new Proposal in the database")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    @ResponseStatus(value = 201)
    public ObservingProposal createObservingProposal(ObservingProposal op)
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

    //TODO - add more checks, consider where to put observatory / instrument specific validation.
    @GET
    @Path(proposalRoot + "/validate")
    @Operation(summary = "validate the proposal, get summary strings of it's state.  Optionally pass a cycle to compare dates with.")
    public ProposalValidation validateObservingProposal(@PathParam("proposalCode") Long proposalCode, @RestQuery long cycleId) {
        ObservingProposal proposal = findObject(ObservingProposal.class, proposalCode);
        boolean valid = true;
        String info = "Your proposal is ready for submission";
        StringBuilder warn = new StringBuilder();
        StringBuilder error = new StringBuilder();
        //Count the targets
        List<ObjectIdentifier> targets = getTargets(proposalCode, null);
        if(targets.isEmpty()) {
            valid = false;
            error.append("No targets defined.  ");
        }

        List<ObjectIdentifier> technicalGoals = technicalGoalResource.getTechnicalGoals(proposalCode);
        if(technicalGoals.isEmpty()) {
            valid = false;
            error.append("No technical goals defined.  ");
        }

        List<ObjectIdentifier> observations = observationResource.getObservations(proposalCode, null, null);
        if(observations.isEmpty()) {
            valid = false;
            error.append("No observations defined.  ");
        } else if(cycleId != 0) {
            //Compare timing windows with cycle dates and times.
            ProposalCycleDates theCycleDates = proposalCyclesResource.getProposalCycleDates(cycleId);

            for (ObjectIdentifier observation : observations) {
                List<ObservingConstraint> timingWindows = observationResource.getConstraints(proposalCode, observation.dbid);
                for (ObservingConstraint timingWindow : timingWindows) {
                    TimingWindow theWindow = (TimingWindow) timingWindow;
                    if (theWindow.getIsAvoidConstraint()) {
                        if (theCycleDates.observationSessionStart.after(theWindow.getStartTime())
                                && theCycleDates.observationSessionEnd.before(theWindow.getEndTime())) {
                            warn.append("A timing window excludes this entire observation session.  ");
                        }
                    } else {
                        if (theWindow.getEndTime().before(theCycleDates.observationSessionStart)) {
                            warn.append("A timing window ends before this observation session begins.  ");
                        }
                        if (theWindow.getStartTime().after(theCycleDates.observationSessionEnd)) {
                            warn.append("A timing window begins after this observation session has ended. ");
                        }
                    }
                }
            }
        }

        if(!valid) {
            info = "Your proposal is not ready for submission";
        }
        return (new ProposalValidation(proposalCode, proposal.getTitle(), valid, info, warn.toString(), error.toString()));
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

        return responseWrapper(proposal.getTitle(), 201);
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

        return responseWrapper(proposal.getSummary(), 201);
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

        return responseWrapper(proposal.getKind(), 201);
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

        return responseWrapper(createSynopsisFromProposal(relatedProposal), 201);
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
            return getObjectIdentifiers("SELECT t._id,t.sourceName FROM ObservingProposal o Inner Join o.targets t WHERE o._id = "+proposalCode+" ORDER BY t.sourceName");
        } else {
            return getObjectIdentifiers("SELECT t._id,t.sourceName FROM ObservingProposal o Inner Join o.targets t WHERE o._id = "+proposalCode+" and t.sourceName like '"+sourceName+"' ORDER BY t.sourceName");
        }

    }

    @GET
    @Path (targetsRoot + "/{targetId}")
    @Operation(summary = "get a specific Target for the given ObservingProposal")
    public Target getTarget(@PathParam("proposalCode") Long proposalCode,
                            @PathParam("targetId") Long targetId)
        throws WebApplicationException
    {
        return findChildByQuery(ObservingProposal.class, Target.class, "targets",
                proposalCode, targetId);
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

        List<Observation> observations = observingProposal.getObservations();

        for (Observation o : observations) {
            if(Objects.equals(targetId, o.getTarget().getId())) {
                throw new BadRequestException(
                        "Target cannot be deleted as it is currently referred to by at least one Observation");
            }
        }

        Target target = observingProposal.getTargets().stream().filter(o -> targetId.equals(o.getId())).findAny()
                .orElseThrow(() -> new WebApplicationException(
                        String.format(NON_ASSOCIATE_ID, "Target", targetId, "ObservingProposal", proposalCode)
                ));

        return deleteChildObject(observingProposal, target, observingProposal::removeFromTargets);
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
            return getObjectIdentifiers("SELECT t._id,t.name FROM ObservingProposal o Inner Join o.fields t WHERE o._id = "+proposalCode+" ORDER BY t.name");
        } else {
            return getObjectIdentifiers("SELECT t._id,t.name FROM ObservingProposal o Inner Join o.fields t WHERE o._id = "+proposalCode+" and t.name like '"+fieldName+"' ORDER BY t.name");
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

    //********************** EXPORT ***************************
    @GET
    @Operation(summary="export a proposal as a file")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Path(proposalRoot+"/export")
    public Response exportProposal(@PathParam("proposalCode")Long proposalCode)
            throws WebApplicationException {
        ObservingProposal proposalForExport = findObject(ObservingProposal.class, proposalCode);

        return Response
                .status(Response.Status.OK)
                .header("Content-Disposition", "attachment;filename=" + "proposal.json")
                .entity(writeAsJsonString(proposalForExport))
                .build();
    }


    //********************** IMPORT ***************************
    @POST
    @Operation(summary="import a proposal")
    @Path("/import")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public ObservingProposal importProposal(ObservingProposal importProposal) {
        if(importProposal==null){
            throw new WebApplicationException("No file uploaded",400);
        }

        new ProposalManagementModel().createContext();
        ObservingProposal newProposal = new ObservingProposal(importProposal);

        //List of existing organisations
        List<ObjectIdentifier> orgIds = organizationResource.getOrganizations();
        HashMap<String, Organization> existingOrganizationsMap = new HashMap<>();
        for (ObjectIdentifier oi: orgIds) {
            Organization organizationToAdd = organizationResource.getOrganization(oi.dbid);
            existingOrganizationsMap.put(organizationToAdd.getName(), organizationToAdd);
        }

        //List of existing people
        List<ObjectIdentifier> peopleIds = personResource.getPeople(null);
        HashMap<String, Person> existingPeopleMap = new HashMap<>();
        for (ObjectIdentifier pid: peopleIds) {
            Person personToAdd = personResource.getPerson(pid.dbid);
            existingPeopleMap.put(personToAdd.getOrcidId().toString(), personToAdd);
        }

        //Compare people and organisations to what's in the database
        List<Investigator> investigators = newProposal.getInvestigators();
        for (Investigator i : investigators) {
            Person person = i.getPerson();
            Organization organization = person.getHomeInstitute();

            //If organisation doesn't exist, add it
            if(!existingOrganizationsMap.containsKey(organization.getName())) {
                logger.info("Adding organisation " + organization.getName());
                organization.setXmlId("0");
                Organization newOrganization = organizationResource.createOrganization(organization);
                person.setHomeInstitute(newOrganization);
                existingOrganizationsMap.put(organization.getName(), person.getHomeInstitute());
            }

            //If person does not exist, add them
            if(!existingPeopleMap.containsKey(person.getOrcidId().toString())) {
                logger.info("Adding person " + person.getFullName());
                person.setXmlId("0");
                i.setPerson(personResource.createPerson(person));
                existingPeopleMap.put(person.getOrcidId().toString(), i.getPerson());
            }
        }

        //update references
        newProposal.updateClonedReferences();

        //Persist the proposal
        em.persist(newProposal);

        //Remove supporting document entries without deleting any files.
        List<ObjectIdentifier> oldDocuments = supportingDocumentResource.getSupportingDocuments(newProposal.getId(), null);
        for(ObjectIdentifier oldDocumentIdentifier : oldDocuments) {
            SupportingDocument supportingDocument = supportingDocumentResource.getSupportingDocument(newProposal.getId(), oldDocumentIdentifier.dbid);
            deleteChildObject(newProposal, supportingDocument,
                newProposal::removeFromSupportingDocuments);
        }

        //Import supporting documents separately.
        return newProposal;
    }

    //Other fields of an ObservingProposal have been split out into their own source file
}
