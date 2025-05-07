package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 16/03/2022 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnit;
import jakarta.persistence.TypedQuery;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.management.ProposalManagementModel;
import org.ivoa.dm.proposal.prop.*;
import org.ivoa.dm.stc.coords.SpaceSys;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.orph2020.pst.AppLifecycleBean.MODES;
import org.orph2020.pst.common.json.*;

import jakarta.annotation.security.RolesAllowed;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.util.*;

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
//@RolesAllowed("default-roles-orppst")
public class ProposalResource extends ObjectResourceBase {
    private final Logger logger;
    @Inject
    ProposalDocumentStore proposalDocumentStore;

    @PersistenceUnit(unitName = "optical")
    EntityManager opticalEntityManager;

    // get the mode from the application level.
    @Inject
    MODES mode;

    public ProposalResource(Logger logger) {
        this.logger = logger;
    }

    @Inject
    ObservationResource observationResource;
    @Inject
    TechnicalGoalResource technicalGoalResource;
    @Inject
    ProposalCyclesResource proposalCyclesResource;
    @Inject
    SubjectMapResource subjectMapResource;
    @Inject
    JsonWebToken userInfo;
//    UserInfo userInfo; // IMPL it would be nice to use UserInfo

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

    @Inject
    OpticalTelescopeResource opticalTelescopeResource;

    private List<ProposalSynopsis> getSynopses(String queryStr) {
        List<ProposalSynopsis> result = new ArrayList<>();
        Query query = em.createQuery(queryStr);
        List<Object[]> results = query.getResultList();
        for (Object[] r : results) {
            result.add(
                    new ProposalSynopsis((long) r[0], (String) r[1], (String) r[2], (ProposalKind) r[3])
            );
        }
        return result;
    }

    private ProposalSynopsis createSynopsisFromProposal(ObservingProposal proposal) {
        return new ProposalSynopsis(proposal.getId(), proposal.getTitle(), proposal.getSummary(),
                proposal.getKind());
    }

    private String modifyProposalTitle(String currentTitle, String modifier) {
        int titleLength = currentTitle.length();
        int maxTitleLength = 255; //have we got the max length codified somewhere?

        if (titleLength > maxTitleLength - modifier.length()) {
            return currentTitle.substring(0, titleLength - modifier.length()) + modifier;
        } else {
            return currentTitle + modifier;
        }
    }

    @GET
    @Operation(summary = "get the synopsis for each Proposal in the database, optionally provide an investigator name and/or a proposal title to see specific proposals.  Filters out submitted copies.")
    //@RolesAllowed("default-roles-orppst")
    public List<ProposalSynopsis> getProposals(@RestQuery String investigatorName, @RestQuery String title) {

        boolean noQuery = investigatorName == null && title == null;
        boolean investigatorOnly = investigatorName != null && title == null;
        boolean titleOnly = investigatorName == null && title != null;
        Long personId = subjectMapResource.subjectMap(userInfo.getSubject()).getPerson().getId();

        //if 'ProposalSynopsis' is modified we should check these Strings for suitability
        //Investigator table is joined twice, once for user view scope and again for searching other investigators.
        String baseStr = "select distinct o._id,o.title,o.summary,o.kind from ObservingProposal o, Investigator inv, Investigator i "
                        + "where inv member of o.investigators and inv.person._id = " + personId + " and i member of o.investigators ";
        String orderByStr = "order by o.title";
        String investigatorLikeStr = "and i.person.fullName like '" +investigatorName+ "' ";
        String titleLikeStr = "o.title like '" +title+ "' ";

        if (noQuery) {
            return getSynopses(baseStr  + orderByStr);
        } else if (investigatorOnly) {
            return getSynopses(baseStr + investigatorLikeStr +  orderByStr);
        } else if (titleOnly) {
            return getSynopses(baseStr + "and " + titleLikeStr  + orderByStr);
        } else { //name and title given as queries
            return getSynopses(baseStr + investigatorLikeStr + "and " + titleLikeStr  + orderByStr);
        }
    }

    private ObservingProposal singleObservingProposal(Long proposalCode)
    {
        TypedQuery<ObservingProposal> q = em.createQuery(
                "Select o From ObservingProposal o, Investigator i where i member of o.investigators "
                        + "and o._id = :pid and i.person._id = :uid",
                ObservingProposal.class
        );
        q.setParameter("pid", proposalCode);
        q.setParameter("uid", subjectMapResource.subjectMap(userInfo.getSubject()).getPerson().getId());
        return q.getSingleResult();
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
        return singleObservingProposal(proposalCode);
    }

    @POST
    @Operation(summary = "create a new Proposal in the database")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    @ResponseStatus(value = 201)
    public ObservingProposal createObservingProposal(ObservingProposal op)
            throws WebApplicationException {
        ObservingProposal persisted = persistObject(op);

        //use the newly persisted proposal id (code) to create storage locations
        try {
            proposalDocumentStore.createStorePaths(persisted.getId());
        } catch (IOException e) {
            //if these directories cannot be created then we should roll back
            throw new WebApplicationException(e);
        }
        return persisted;
    }

    @DELETE
    @Path(proposalRoot)
    @Operation(summary = "remove the ObservingProposal specified by the 'proposalCode'")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response deleteObservingProposal(@PathParam("proposalCode") long code)
            throws WebApplicationException
    {
        //clean up the document store for this proposal
        try {
            proposalDocumentStore.removeStorePath(String.valueOf(code));
        } catch (IOException e) {
            throw new WebApplicationException(e);
        }
        // IMPL need to delete observations first
        ObservingProposal prop = findObject(ObservingProposal.class, code);
        prop.getObservations().forEach(observation -> em.remove(observation));
        return removeObject(ObservingProposal.class, code);
    }

    @POST
    @Path(proposalRoot)
    @Operation(summary = "clone ObservingProposal specified by the 'proposalCode'")
    @Transactional(rollbackOn = {WebApplicationException.class})
    @ResponseStatus(value = 201)
    public ObservingProposal cloneObservingProposal(@PathParam("proposalCode") long code)
          throws WebApplicationException
    {
        ObservingProposal prop = findObject(ObservingProposal.class, code);
        prop.forceLoad();
        new ProposalModel().createContext(); //IMPL nasty clone API...
        ObservingProposal newProp = new ObservingProposal(prop);
        newProp.updateClonedReferences();

        ObservingProposal clonedProp = persistObject(newProp);

        //copy the document store for the new, cloned proposal
        try {
            proposalDocumentStore.copyStore(
                    prop.getId().toString(),
                    clonedProp.getId().toString(),
                    clonedProp.getSupportingDocuments()
            );
        }
        catch (IOException e) {
            throw new WebApplicationException(e);
        }

        //**** clone the telescope store of the original proposal ****
        //in essence creates a snapshot of the telescope data at the point of
        // cloning
        //try {
        //    opticalTelescopeResource.copyProposal(prop, clonedProp);
        //} catch (Exception e) {
        //    throw new WebApplicationException(e);
        // }

        //add '(clone)' to the end of the title string
        clonedProp.setTitle(modifyProposalTitle(prop.getTitle(), " (clone)"));

        return clonedProp;
    }


    //********************** TITLE ***************************

    @GET
    @Path(proposalRoot + "/title")
    @Operation(summary = "get the title of the ObservingProposal specified by 'proposalCode'")
    public Response getObservingProposalTitle(@PathParam("proposalCode") Long proposalCode) {
        ObservingProposal proposal = singleObservingProposal(proposalCode);
        return responseWrapper(proposal.getTitle(), 200);
    }

    /**
     * checks the optical observation for telescope data.
     *
     * @param observationID: the observation id.
     * @param proposalID: the proposal id.
     * @param error: the error string.
     */
    private boolean checkOpticalTelescopes(
            long observationID, long proposalID, StringBuilder error) {
        OpticalTelescopeDataId id = new OpticalTelescopeDataId(
                String.valueOf(proposalID), String.valueOf(observationID));
        OpticalTelescopeDataSave savedData =
                opticalEntityManager.find(OpticalTelescopeDataSave.class, id);

        // if no data, then declare a failure.
        if (savedData == null) {
            error.append("No Telescope data defined.   ");
            return false;
        }
        return true;
    }

    /**
     * checks the radio timing windows.
     *
     * @param timingWindows: the timing windows for a given observation.
     * @param warn the warning message.
     * @param error the error message.
     * @param name the observation name.
     * @param theCycleDates: the cycle dates.
     */
    private boolean checkRadioWindows(
            List<ObservingConstraint> timingWindows, StringBuilder warn,
            StringBuilder error, ProposalCycleDates theCycleDates,
            String name) {
        if (timingWindows.isEmpty()) {
            error.append("No timing windows defined.<br/>");
            return false;
        } else {
            for (ObservingConstraint timingWindow : timingWindows) {
                TimingWindow theWindow = (TimingWindow) timingWindow;
                if (theWindow.getIsAvoidConstraint()) {
                    if (theCycleDates.observationSessionStart.after(
                                theWindow.getStartTime())
                            && theCycleDates.observationSessionEnd.before(
                                theWindow.getEndTime())) {
                        warn.append(
                            "A timing window for the observation of '" +
                            name + "' excludes this entire session.<br/>");
                    }
                } else {
                    if (theWindow.getEndTime().before(
                            theCycleDates.observationSessionStart)) {
                        warn.append(
                            "A timing window for the observation of '" +
                            name + "' ends before this session begins.<br/>");
                    }
                    if (theWindow.getStartTime().after(
                            theCycleDates.observationSessionEnd)) {
                        warn.append(
                            "A timing window for the observation of '" +
                            name +
                            "' begins after this session has ended.<br/>");
                    }
                }
            }
            return true;
        }
    }

    //TODO - add more checks, consider where to put observatory / instrument specific validation.
    @GET
    @Path(proposalRoot + "/validate")
    @Operation(summary = "validate the proposal, get summary strings of" +
            " it's state.  Optionally pass a cycle to compare dates with.")
    public ProposalValidation validateObservingProposal(
            @PathParam("proposalCode") Long proposalCode,
            @RestQuery long cycleId) {
        ObservingProposal proposal = singleObservingProposal(proposalCode);
        boolean valid = true;
        String info = "Your proposal has passed preliminary checks," +
                " please now select modes for your observations.";
        StringBuilder warn = new StringBuilder();
        StringBuilder error = new StringBuilder();
        //Count the targets
        List<ObjectIdentifier> targets = getTargets(proposalCode, null);
        if(targets.isEmpty()) {
            valid = false;
            error.append("No targets defined.<br/>");
        }

        if (mode == MODES.RADIO) {
            List<ObjectIdentifier> technicalGoals =
                technicalGoalResource.getTechnicalGoals(proposalCode);
            if(technicalGoals.isEmpty()) {
                valid = false;
                error.append("No technical goals defined.<br/>");
            }
        }

        List<Long> opticalObservationIds =
            opticalTelescopeResource.listOfObservationIdsByProposal(
                proposalCode.toString());
        List<ObjectIdentifier> observations =
            observationResource.getObservations(proposalCode, null, null);

        // handle combination proposals vs mode.
        if (mode == MODES.OPTICAL &&
                opticalObservationIds.size() != observations.size()) {
            valid = false;
            error.append(
                "There are observations in this proposal which are not " +
                "optical.");
        }
        if (mode == MODES.RADIO && opticalObservationIds.size() != 0) {
            valid = false;
            error.append(
                "There are observations in this proposal which are optical.");
        }


        if(observations.isEmpty()) {
            valid = false;
            error.append("No observations defined.<br/>");
        } else if(cycleId != 0) {
            //Compare timing windows with cycle dates and times.
            ProposalCycleDates theCycleDates =
                    proposalCyclesResource.getProposalCycleDates(cycleId);

            //Has proposal cycle submission deadline passed?
            Date now = new Date();
            if(now.after(theCycleDates.submissionDeadline)) {
                valid = false;
                error.append("The submission deadline has passed.<br/>");
            } else {
                for (ObjectIdentifier observation : observations) {
                    List<ObservingConstraint> timingWindows =
                        observationResource.getConstraints(
                            proposalCode, observation.dbid);
                    switch (mode) {
                        case RADIO:
                            valid &= this.checkRadioWindows(
                                timingWindows, warn, error, theCycleDates,
                                observation.name);
                            break;
                        case OPTICAL:
                            valid &= this.checkOpticalTelescopes(
                                observation.dbid, proposalCode, error);
                            break;
                        case BOTH:
                            if (opticalObservationIds.contains(
                                    observation.dbid)) {
                                valid &= this.checkOpticalTelescopes(
                                    observation.dbid, proposalCode, error);
                            } else {
                                valid &= this.checkRadioWindows(
                                    timingWindows, warn, error, theCycleDates,
                                    observation.name);
                            }
                            break;
                        default:
                            error.append(
                                "Dont recognise mode. therefore not valid.");
                            break;
                    }
                }
            }
        }

        if(!valid) {
            info = "Your proposal is not ready for submission";
        }
        return (new ProposalValidation(
                proposalCode, proposal.getTitle(), valid, info, warn.toString(),
                error.toString()));
    }

    @PUT
    @Operation(summary = "change the title of an ObservingProposal")
    @Consumes(MediaType.TEXT_PLAIN)
    //@RolesAllowed("default-roles-orppst")
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
        ObservingProposal proposal = getObservingProposal(proposalCode);
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

    private String checkTargetListUpload(FileUpload fileUpload)
            throws WebApplicationException {
        if (fileUpload == null) {
            throw new WebApplicationException("No file uploaded");
        }

        String contentType = fileUpload.contentType();
        if (contentType == null) {
            throw new WebApplicationException("No content type information available");
        }

        String extension = FilenameUtils.getExtension(fileUpload.fileName());
        if (extension == null || extension.isEmpty()) {
            throw new WebApplicationException("Uploads require the correct file extension");
        }

        switch (contentType) {
            case "application/octet-stream": //cover-all
            case "text/plain":
            case "text/csv":
            case "text/xml":
                if (
                        !extension.equals("xml") &&
                        !extension.equals("txt") &&
                        !extension.equals("csv") &&
                        !extension.equals("ecsv")
                ) {
                    throw new WebApplicationException("Invalid file extension");
                }
                break;
            default:
                throw new WebApplicationException(
                    String.format("content-type: %s is not supported", contentType));
        }

        return extension;
    }

    enum FileType {
        PLAIN_TEXT,
        STAR_TABLE_FMT
    }

    private List<Target> getTargetListFromFile(
            java.nio.file.Path filePath,
            FileType fileType,
            SpaceSys spaceSys,
            List<String> currentNames
    ) throws WebApplicationException {
        return switch (fileType) {
            case PLAIN_TEXT -> TargetListFileReader.readTargetListFile(
                    filePath.toFile(),
                    spaceSys,
                    currentNames
            );
            case STAR_TABLE_FMT -> StarTableReader.convertToListOfTargets(
                    filePath.toString(),
                    spaceSys,
                    currentNames
            );
        };
    }


    @Schema(type = SchemaType.STRING, format = "binary")
    public static class UploadTargetList {}

    @POST
    @Path(targetsRoot+"/uploadList")
    @Operation(summary = "upload a list of targets contained in a file to this Proposal")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response uploadTargetList(@PathParam("proposalCode") Long proposalCode,
                                     @RestForm("document") @Schema(implementation = UploadTargetList.class)
                                     FileUpload fileUpload)
        throws WebApplicationException
    {
        String extension = checkTargetListUpload(fileUpload);

        ObservingProposal observingProposal = findObject(ObservingProposal.class, proposalCode);

        List<Target> currentTargets = observingProposal.getTargets();

        List<String> currentNames = new ArrayList<>();
        for (Target target : currentTargets) {
            currentNames.add(target.getSourceName());
        }

        //find the 'ICRS' SpaceSys
        String queryStr = "select s from SpaceSys s where s.frame.spaceRefFrame='ICRS'";
        TypedQuery<SpaceSys> query = em.createQuery(queryStr, SpaceSys.class);
        SpaceSys spaceSys = query.getResultList().get(0);

        // assume anything not '.txt' is STILTS compatible (STILTS will throw useful error message if not)
        FileType fileType = extension.equals("txt") ? FileType.PLAIN_TEXT : FileType.STAR_TABLE_FMT;

        List<Target> targetList = getTargetListFromFile(fileUpload.uploadedFile(), fileType,
                spaceSys, currentNames);

        for (Target target : targetList) {
            addNewChildObject(observingProposal, target, observingProposal::addToTargets);
        }

        return responseWrapper(observingProposal.getTargets(), 200);
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
           if( o.getTarget().stream().anyMatch(t -> targetId.equals(t.getId())))
                throw new BadRequestException(
                        "Target cannot be deleted as it is currently referred to by at least one Observation");
            
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

    @GET
    @Path(fieldsRoot+"/{fieldId}")
    @Operation(summary = "get the Field specified by the 'fieldId' in the given proposal")
    public Field getField(@PathParam("proposalCode") Long proposalCode,
                          @PathParam("fieldId") Long fieldId)
        throws WebApplicationException
    {
        return findChildByQuery(ObservingProposal.class, Field.class, "fields",
                proposalCode, fieldId);
    }

    @PUT
    @Path(fieldsRoot+"/{fieldId}/name")
    @Operation(summary = "change the name of the specified field")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response changeFieldName(@PathParam("proposalCode") Long proposalCode,
                                    @PathParam("fieldId") Long fieldId,
                                    String replacementName)
        throws WebApplicationException
    {
        Field field = findChildByQuery(ObservingProposal.class, Field.class, "fields",
                proposalCode, fieldId);

        field.setName(replacementName);

        return responseWrapper(field, 200);
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

        removeObject(Field.class, fieldId);

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

        //create the document store area for the new proposal
        try {
            proposalDocumentStore.createStorePaths(newProposal.getId());
        } catch (IOException e) {
            throw new WebApplicationException(e);
        }

        //add '(import)' to the end of the imported proposal
        newProposal.setTitle(modifyProposalTitle(importProposal.getTitle(), " (import)"));

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
