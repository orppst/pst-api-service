package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 20/04/2023 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.management.*;
import org.ivoa.dm.proposal.management.Observatory;
import org.ivoa.dm.proposal.prop.ObservingProposal;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestQuery;
import org.orph2020.pst.common.json.CycleObservingTimeTotal;
import org.orph2020.pst.common.json.ObjectIdentifier;
import org.orph2020.pst.common.json.ObservatoryQuestion;
import org.orph2020.pst.common.json.ProposalCycleSynopsis;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

@Path("proposalCycles")
@Tag(name="proposalCycles")
@Produces(MediaType.APPLICATION_JSON)
//@RolesAllowed({"default-roles-orppst"})
public class ProposalCyclesResource extends ObjectResourceBase {
    private final Logger logger;

    @Inject
    SubjectMapResource subjectMapResource;
    @Inject
    JsonWebToken userInfo;
    @Inject
    ProposalDocumentStore proposalDocumentStore;

    @ConfigProperty(name = "answered.observatoryQuestions.filename")
    String answeredObservatoryQuestionsFileName;

    @ConfigProperty(name = "template.observatoryQuestions.filename")
    String templateObservatoryQuestionsFileName;

    private static final String notOnTACmsg = "This endpoint is restricted to TAC members only";

    public ProposalCyclesResource(Logger logger) {
        this.logger = logger;
    }


    /**
     * Checks that the current user, i.e. the user making the call, is on the TAC. This is
     * an additional check over that provided by the "@RolesAllowed" annotation.
     */
    public boolean isCurrentUserOnTAC(ProposalCycle cycle)
            throws WebApplicationException
    {
        // Get the current user details.
        Long personId = subjectMapResource.subjectMap(userInfo.getSubject()).getPerson().getId();

        // An observatory administrator can do _anything_
        if(userInfo.getClaim("realm_access") != null) {
            String roleList = userInfo.getClaim("realm_access").toString();

            if(roleList != null && roleList.contains("\"obs_administration\"")) {
                return true;
            }
        }

        AtomicReference<Boolean> amIOnTheTAC = new AtomicReference<>(false);

        // See if user is member of the TAC
        cycle.getTac().getMembers().forEach(member -> {
            if(member.getMember().getPerson().getId().equals(personId)) {
                amIOnTheTAC.set(true);
            }
        });
        return amIOnTheTAC.get();
    }

    @GET
    @Path("MyTACCycles")
    @Operation(summary = "get all the proposal cycles where I am a on the TAC, include closed boolean flag and optional observatory id filter")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"tac_member", "tac_admin"})
    public List<ObjectIdentifier> getMyTACMemberProposalCycles(@RestQuery boolean includeClosed, @RestQuery long observatoryId) {
        // Get the current user details.
        Long personId = subjectMapResource.subjectMap(userInfo.getSubject()).getPerson().getId();
        List<ObjectIdentifier> matchedCycles = new ArrayList<>();

        // Find list of Proposal Cycles and their TACs
        String query = "select p._id,p.title,tac from ProposalCycle p";

        if(!includeClosed)
            if(observatoryId == 0)
                query += " WHERE p.submissionDeadline > CURRENT_TIMESTAMP() ";
            else
                query += " WHERE p.submissionDeadline > CURRENT_TIMESTAMP() AND p.observatory._id = "+observatoryId;
        else //include closed
            if(observatoryId > 0)
                query += " WHERE p.observatory._id = "+observatoryId;

        List<Object[]> cycles = em.createQuery(query).getResultList();

        // Filter these TACs for those that include this user
        for (Object[] cycle : cycles) {
            TAC cycleTac = (TAC) cycle[2];

            for(int i = 0; i < cycleTac.getMembers().size(); i++ ) {
                Long memberPersonId = cycleTac.getMembers().get(i).getMember().getPerson().getId();
                if(Objects.equals(memberPersonId, personId)) {
                    matchedCycles.add(new ObjectIdentifier((long)cycle[0], cycle[1].toString()));
                    break;
                }
            }

        }

        return matchedCycles;
    }

    @GET
    @Operation(summary = "list the proposal cycles, optionally filter by observatory id and closed (passed submission deadline)")
    public List<ObjectIdentifier> getProposalCycles(@RestQuery boolean includeClosed, @RestQuery long observatoryId) {
        String select = "SELECT o._id,o.title FROM ProposalCycle o ";
        String where = "";
        String order = "ORDER BY o.submissionDeadline";

        if(!includeClosed) {
            where = "WHERE o.submissionDeadline > CURRENT_TIMESTAMP() ";
            if (observatoryId > 0)
                where += "AND o.observatory._id = "+observatoryId+" ";
        } else {
            if (observatoryId > 0)
                where = "WHERE o.observatory._id = "+observatoryId+" ";
        }

        return super.getObjectIdentifiers(select + where + order);
    }


    @GET
    @Path("{cycleCode}")
    @Operation(summary = "get the given proposal cycle")
    @RolesAllowed({"tac_member", "tac_admin", "obs_administration", "reviewer"})
    public ProposalCycle getProposalCycle(@PathParam("cycleCode") long cycleId) {
        return findObject(ProposalCycle.class,cycleId);
    }


    @POST
    @Operation(summary = "create a proposal cycle")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    @RolesAllowed("obs_administration")
    public ProposalCycle createProposalCycle(ProposalCycle cycle) {

        ProposalCycle persistedCycle = persistObject(cycle);

        // Create a directory for the proposal cycle in the document store.
        // This is intended for the "working" location of the observatory-specific
        // questions for the cycle, and any other cycle-specific documents.

        try {
            proposalDocumentStore.createCyclePaths(persistedCycle.getId());
        } catch (IOException e) {
            throw new WebApplicationException(e);
        }

        return persistedCycle;
    }

    ///********* TITLE **********

    @GET
    @Path("{cycleCode}/title")
    @Operation(summary = "Get the title for a given proposal cycle")
    public Response getProposalCycleTitle(@PathParam("cycleCode") Long cycleCode)
    {
        ProposalCycle fullCycle =  findObject(ProposalCycle.class, cycleCode);

        return responseWrapper(fullCycle.getTitle(), 200);
    }

    @PUT
    @Path("{cycleCode}/title")
    @Operation(summary = "change the title of the given proposal cycle")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    @RolesAllowed({"tac_admin"})
    public Response replaceCycleTitle(
            @PathParam("cycleCode") Long cycleCode,
            String replacementTitle
    )
            throws WebApplicationException
    {
        // Get the TAC for this cycle
        ProposalCycle cycle = findObject(ProposalCycle.class, cycleCode);

        // See if user is on the TAC
        if (!isCurrentUserOnTAC(cycle)){
            throw new WebApplicationException(notOnTACmsg, Response.Status.FORBIDDEN);
        }

        cycle.setTitle(replacementTitle);

        return responseWrapper(cycle.getTitle(), 200);
    }

    ///********* CODE **********

    @GET
    @Path("{cycleCode}/code")
    @Operation(summary = "Get the code for a given proposal cycle")
    public Response getProposalCycleCode(@PathParam("cycleCode") Long cycleCode)
    {
        ProposalCycle fullCycle =  findObject(ProposalCycle.class, cycleCode);

        return responseWrapper(fullCycle.getCode(), 200);
    }

    @PUT
    @Path("{cycleCode}/code")
    @Operation(summary = "change the code of the given proposal cycle")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    @RolesAllowed({"tac_admin"})
    public Response replaceCycleCode(
            @PathParam("cycleCode") Long cycleCode,
            String replacementCode
    )
            throws WebApplicationException
    {
        // Get the TAC for this cycle
        ProposalCycle cycle = findObject(ProposalCycle.class, cycleCode);

        // See if user is on the TAC
        if (!isCurrentUserOnTAC(cycle)){
            throw new WebApplicationException(notOnTACmsg, Response.Status.FORBIDDEN);
        }

        cycle.setCode(replacementCode);

        return responseWrapper(cycle.getTitle(), 200);
    }

    //********* DETAILS **********

    @GET
    @Path("{cycleCode}/details")
    @Operation(summary = "Get the dates associated with a given proposal cycle")
    public ProposalCycleSynopsis getProposalCycleDetails(@PathParam("cycleCode") Long cycleCode)
    {
        ProposalCycle fullCycle =  findObject(ProposalCycle.class, cycleCode);

        return new ProposalCycleSynopsis(fullCycle.getTitle(), fullCycle.getCode(),
                fullCycle.getSubmissionDeadline(), fullCycle.getObservationSessionStart(),
                fullCycle.getObservationSessionEnd(), fullCycle.getObservatory());

    }

    @PUT
    @Path("{cycleCode}/details")
    @Operation(summary = "change all the details of a given proposal cycle")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("tac_admin")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public ProposalCycleSynopsis updateProposalCycleDetails(
            @PathParam("cycleCode") Long cycleCode,
            ProposalCycleSynopsis newDetails
    )
        throws WebApplicationException
    {
        ProposalCycle cycle = findObject(ProposalCycle.class, cycleCode);
        if (!isCurrentUserOnTAC(cycle)){
            throw new WebApplicationException(notOnTACmsg, Response.Status.FORBIDDEN);
        }

        cycle.setTitle(newDetails.title);
        cycle.setCode(newDetails.code);
        cycle.setSubmissionDeadline(newDetails.submissionDeadline);
        cycle.setObservationSessionStart(newDetails.observationSessionStart);
        cycle.setObservationSessionEnd(newDetails.observationSessionEnd);
        cycle.setObservatory(newDetails.observatory);

        return new ProposalCycleSynopsis(cycle.getTitle(), cycle.getCode(),
                cycle.getSubmissionDeadline(), cycle.getObservationSessionStart(),
                cycle.getObservationSessionEnd(), cycle.getObservatory());
    }

    @PUT
    @Path("{cycleCode}/dates/deadline")
    @Operation(summary = "change the submission deadline of the given proposal cycle")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("tac_admin")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceCycleDeadline(
            @PathParam("cycleCode") Long cycleCode,
            ZonedDateTime replacementDeadline
    )
        throws WebApplicationException
    {
        ProposalCycle cycle = findObject(ProposalCycle.class, cycleCode);

        if (!isCurrentUserOnTAC(cycle)){
            throw new WebApplicationException(notOnTACmsg, Response.Status.FORBIDDEN);
        }

        cycle.setSubmissionDeadline(replacementDeadline);

        return responseWrapper(cycle.getSubmissionDeadline(), 200);
    }


    @PUT
    @Path("{cycleCode}/dates/sessionStart")
    @Operation(summary = "change the observation session start of the given proposal cycle")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("tac_admin")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceCycleSessionStart(
            @PathParam("cycleCode") Long cycleCode,
            ZonedDateTime replacementStart
    )
            throws WebApplicationException
    {
        ProposalCycle cycle = findObject(ProposalCycle.class, cycleCode);

        if (!isCurrentUserOnTAC(cycle)){
            throw new WebApplicationException(notOnTACmsg, Response.Status.FORBIDDEN);
        }

        cycle.setObservationSessionStart(replacementStart);

        return responseWrapper(cycle.getObservationSessionStart(), 200);
    }

    @PUT
    @Path("{cycleCode}/dates/sessionEnd")
    @Operation(summary = "change the observation session end of the given proposal cycle")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("tac_admin")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceCycleSessionEnd(
            @PathParam("cycleCode") Long cycleCode,
            ZonedDateTime replacementEnd
    )
            throws WebApplicationException
    {
        ProposalCycle cycle = findObject(ProposalCycle.class, cycleCode);

        if (!isCurrentUserOnTAC(cycle)){
            throw new WebApplicationException(notOnTACmsg, Response.Status.FORBIDDEN);
        }

        cycle.setObservationSessionEnd(replacementEnd);

        return responseWrapper(cycle.getObservationSessionEnd(), 200);
    }


    //********* GRADES **********

    @GET
    @Path("{cycleCode}/grades")
    @Operation(summary = "List the possible grades of the given proposal cycle")
    @RolesAllowed({"tac_member", "tac_admin"})
    public List<ObjectIdentifier> getCycleAllocationGrades(@PathParam("cycleCode") Long cycleCode)
    {
        Query query = em.createQuery(
                "Select o._id,o.description,o.name from ProposalCycle p inner join p.possibleGrades o where p._id = "+cycleCode+" Order by o.name"
        );

        return getObjectIdentifiersAlt(query);
    }

    @GET
    @Path("{cycleCode}/grades/{gradeId}")
    @Operation(summary = "get the specific grade associated with the given proposal cycle")
    @RolesAllowed({"tac_member", "tac_admin"})
    public AllocationGrade getCycleAllocatedGrade(@PathParam("cycleCode") Long cycleCode,
                                                  @PathParam("gradeId") Long gradeId)
    {
        return findChildByQuery(ProposalCycle.class, AllocationGrade.class,
                "possibleGrades", cycleCode, gradeId);
    }

    @POST
    @Path("{cycleCode}/grades")
    @Operation(summary = "add a new possible allocation grade to the given proposal cycle")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("obs_administration")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public AllocationGrade addCycleAllocationGrade(@PathParam("cycleCode") Long cycleCode,
                                                   AllocationGrade grade)
        throws WebApplicationException
    {
        ProposalCycle cycle = findObject(ProposalCycle.class, cycleCode);

        return addNewChildObject(cycle, grade, cycle::addToPossibleGrades);
    }

    @DELETE
    @Path("{cycleCode}/grades/{gradeId}")
    @Operation(summary = "remove the specified possible grade from the given proposal cycle")
    @RolesAllowed("obs_administration")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response removeCycleAllocationGrade(@PathParam("cycleCode") Long cycleCode,
                                               @PathParam("gradeId") Long gradeId)
        throws WebApplicationException
    {
        ProposalCycle cycle = findObject(ProposalCycle.class, cycleCode);

        AllocationGrade grade = findChildByQuery(ProposalCycle.class, AllocationGrade.class,
                "possibleGrades", cycleCode, gradeId);

        return deleteChildObject(cycle, grade, cycle::removeFromPossibleGrades);
    }

    @PUT
    @Path("{cycleCode}/grades/{gradeId}/name")
    @Operation(summary = "change the name of the given allocation grade")
    @Consumes(MediaType.TEXT_PLAIN)
    @RolesAllowed("obs_administration")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public AllocationGrade replaceCycleAllocationGradeName(
            @PathParam("cycleCode") Long cycleCode,
            @PathParam("gradeId") Long gradeId,
            String replacementName
    )
        throws WebApplicationException
    {
        AllocationGrade grade = findChildByQuery(ProposalCycle.class, AllocationGrade.class,
                "possibleGrades", cycleCode, gradeId);

        grade.setName(replacementName);

        return grade;
    }

    @PUT
    @Path("{cycleCode}/grades/{gradeId}/description")
    @Operation(summary = "change the description of the given allocation grade")
    @Consumes(MediaType.TEXT_PLAIN)
    @RolesAllowed("obs_administration")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public AllocationGrade replaceCycleAllocationGradeDescription(
            @PathParam("cycleCode") Long cycleCode,
            @PathParam("gradeId") Long gradeId,
            String replacementDescription
    )
            throws WebApplicationException
    {
        AllocationGrade grade = findChildByQuery(ProposalCycle.class, AllocationGrade.class,
                "possibleGrades", cycleCode, gradeId);

        grade.setDescription(replacementDescription);

        return grade;
    }

    ///********* OBSERVATORY **********

    @GET
    @Path("{cycleCode}/observatory")
    @Operation(summary = "Get the observatory object identifier for a given proposal cycle")
    public Observatory getProposalCycleObservatory(@PathParam("cycleCode") Long cycleCode)
    {
        ProposalCycle fullCycle =  findObject(ProposalCycle.class, cycleCode);

        return fullCycle.getObservatory();
    }

    @PUT
    @Path("{cycleCode}/observatory")
    @Operation(summary = "change the observatory for the given proposal cycle")
    @Consumes(MediaType.TEXT_PLAIN)
    @RolesAllowed("obs_administration")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceCycleObservatory(
            @PathParam("cycleCode") Long cycleCode,
            Long replacementObservatoryCode
    )
            throws WebApplicationException
    {
        ProposalCycle cycle = findObject(ProposalCycle.class, cycleCode);

        Observatory replacementObservatory = findObject(Observatory.class, replacementObservatoryCode);

        cycle.setObservatory(replacementObservatory);

        return responseWrapper(cycle.getObservatory(), 200);
    }

    /// ***** Observing Time total for the entire cycle ***** ///

    @GET
    @Path("{cycleCode}/observingTimeTotals")
    @Operation(summary = "get total time allocated in the cycle broken down by observing mode and allocation grade")
    @RolesAllowed({"tac_member", "tac_admin"})
    public List<CycleObservingTimeTotal>
    getCycleObservingTimeTotals(@PathParam("cycleCode") Long cycleCode)
    {
        ProposalCycle cycle = findObject(ProposalCycle.class, cycleCode);

        if (!isCurrentUserOnTAC(cycle)){
            throw new WebApplicationException(notOnTACmsg, Response.Status.FORBIDDEN);
        }

        List<AllocatedProposal> allocatedProposals = cycle.getAllocatedProposals();

        HashMap<String, Double> modeGradeTotals = new HashMap<>();

        for (AllocatedProposal allocatedProposal : allocatedProposals) {
            List<AllocatedBlock> allocatedBlocks = allocatedProposal.getAllocation();
            for (AllocatedBlock allocatedBlock : allocatedBlocks) {
                String modeName = allocatedBlock.getMode().getName();
                String gradeName = allocatedBlock.getGrade().getName();

                String key = modeName + "$" + gradeName;

                Double currentValue = modeGradeTotals.get(key) == null ?  0. : modeGradeTotals.get(key);

                modeGradeTotals.put(
                        key,
                        currentValue + allocatedBlock.getResource().getAmount()
                );
            }
        }

        List<CycleObservingTimeTotal> result = new ArrayList<>();

        for (String key : modeGradeTotals.keySet()) {
            String modeName = key.substring(0, key.indexOf("$"));
            String gradeName = key.substring(key.indexOf("$") + 1);

            result.add(new CycleObservingTimeTotal(modeName, gradeName, modeGradeTotals.get(key)));
        }

        return result;
    }

    @GET
    @Path("{cycleCode}/excelReviews")
    @Operation(summary="Create and download an excel sheet of all submitted proposals and their review scores")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response ExcelReviews(@PathParam("cycleCode") Long cycleCode) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Get the whole proposal cycle
            ProposalCycle proposalCycle = findObject(ProposalCycle.class, cycleCode);

            // Create a sheet
            XSSFSheet sheet = workbook.createSheet(proposalCycle.getCode());

            // count the reviewers for the number of columns
            HashMap<Reviewer, Integer > allReviewers = new HashMap<>();
            Integer reviewerColumnPos = 0;
            for(SubmittedProposal submittedProposal : proposalCycle.getSubmittedProposals())
                for(ProposalReview review : submittedProposal.getReviews())
                    allReviewers.putIfAbsent(review.getReviewer(), reviewerColumnPos++);

            // write headers
            int rowNum = 0;

            Row row = sheet.createRow(rowNum++);
            Cell cell = row.createCell(0);
            cell.setCellValue("Code");
            Cell cellTitle = row.createCell(1);
            cellTitle.setCellValue("Title");

            for(Reviewer reviewer : allReviewers.keySet()) {
                Cell rCell = row.createCell(2 + allReviewers.get(reviewer));
                rCell.setCellValue(reviewer.getPerson().getFullName());
            }

            // write data
            for(SubmittedProposal submittedProposal: proposalCycle.getSubmittedProposals()) {
                int cellNum = 0;
                Row submittedRow = sheet.createRow(rowNum++);
                Cell code = submittedRow.createCell(cellNum++);
                code.setCellValue(submittedProposal.getProposalCode());
                Cell title = submittedRow.createCell(cellNum++);
                title.setCellValue(submittedProposal.getTitle());
                //Populate review scores
                for(ProposalReview review : submittedProposal.getReviews()) {
                    Cell reviewScore = submittedRow.createCell(cellNum + allReviewers.get(review.getReviewer()));
                    reviewScore.setCellValue(review.getScore());
                }

            }

            String filename = "/Reviews for " + proposalCycle.getCode() + ".xlsx";
            try (FileOutputStream out = new FileOutputStream(proposalDocumentStore.getStoreRoot() + filename)) {
                workbook.write(out);
            } catch (IOException e) {
                // error writing excel workbook to file
                return Response.status(500).build();
            }

            return Response.ok(proposalDocumentStore.fetchFile(filename))
                    .header("Content-Disposition", "attachment; filename=" + filename)
                    .build();
        }
        catch (Exception e) {
            return Response.status(500).build();
        }
    }

    /// **** XML UI Observatory Specific Additional Information **** ///

    private List<ObservatoryQuestion> parseQuestionsXml(String resourcePath)
            throws IOException, XMLStreamException {

        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);

        List<ObservatoryQuestion> questions = new ArrayList<>();
        XMLStreamReader reader = null;

        try (FileInputStream file = new FileInputStream(resourcePath)) {
            reader = factory.createXMLStreamReader(file);
            ObservatoryQuestion current = null;

            while (reader.hasNext()) {
                int event = reader.next();

                if (event == XMLStreamReader.START_ELEMENT) {
                    String tag = reader.getLocalName();
                    switch (tag) {
                        case "question" -> current = new ObservatoryQuestion();
                        case "id"          -> { if (current != null) current.id = Integer.parseInt(reader.getElementText()); }
                        case "label"       -> { if (current != null) current.label = reader.getElementText(); }
                        case "description" -> { if (current != null) current.description = reader.getElementText(); }
                        case "query"       -> { if (current != null) current.query = reader.getElementText(); }
                        case "response"    -> { if (current != null) current.response = reader.getElementText(); }
                        case "minChar"     -> { if (current != null) current.minChar = Integer.parseInt(reader.getElementText()); }
                        case "maxChar"     -> { if (current != null) current.maxChar = Integer.parseInt(reader.getElementText()); }
                    }
                } else if (event == XMLStreamReader.END_ELEMENT
                        && "question".equals(reader.getLocalName())
                        && current != null) {
                    questions.add(current);
                    current = null;
                }
            }
        } finally {
            if (reader != null) reader.close();
        }
        return questions;
    }

    /*
        It would be nice to be able to edit the "response" tag value of a specific question in the XML file;
        one way to do this is to parse the XML as a DOM, identify the specific "question" node, loop over
        the child nodes to find the "repsonse" node, then replace its text. Rewriting the entire XML file is
        okay for small files, but could become inefficient for larger files. For now, the XML files are
        expected to be small, so this is acceptable. This is also something to bear in mind if you want to
        implement a "save-as-you-type" feature in the front-end UI (currently save-on-demand is implemented,
        i.e. when the user clicks the "save" button).
     */

    /**
     * A strightforward method to save the answered questions to an XML file by (re)writing the entire file.
     * The "reponse" field of each question should be at most "maxChars" long. The file is saved to the
     * given resourcePath.
     */
    private void saveObservatoryQuestionsToFile(
            String resourcePath,
            List<ObservatoryQuestion> observatoryQuestions,
            String cycleTitle, String proposalTitle
    )
            throws IOException {

        Files.createDirectories(Paths.get(resourcePath).getParent());

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(resourcePath))) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<!-- Generated for proposal cycle: " + cycleTitle + " -->\n");
            if(!proposalTitle.equals("Template")) writer.write("<!-- Proposal: " + proposalTitle + " -->\n");
            writer.write("<questions>");
            for (ObservatoryQuestion question : observatoryQuestions) {
                writer.write("<question id=\"" + question.id + "\">");
                writer.write("<id>" + question.id + "</id>");
                writer.write("<label>" + question.label + "</label>");
                writer.write("<description>" + question.description + "</description>");
                writer.write("<query>" + question.query + "</query>");
                writer.write("<response>" + question.response + "</response>");
                writer.write("<minChar>" + question.minChar + "</minChar>");
                writer.write("<maxChar>" + question.maxChar + "</maxChar>");
                writer.write("</question>");
            }
            writer.write("</questions>");
        }
    }

    /**
     * Save the observatory-specific questions for the given proposal cycle. This is intended to be used
     * by the observatory administrator to set up the template for the cycle. The questions are saved
     * to an XML file in the proposal document store, under "proposalCycles/<cycleCode>/observatoryQuestions.xml".
     */
    @POST
    @Path("{cycleCode}/observatoryQuestions")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Save observatory-specific questions for the given proposal cycle")
    public Response saveObservatoryQuestionsTemplate(
            @PathParam("cycleCode") Long cycleCode,
            List<ObservatoryQuestion> questions)
            throws WebApplicationException
    {
        ProposalCycle cycle = findObject(ProposalCycle.class, cycleCode);

        // location of template for the cycle:
        // storeroot/proposalCycles/cycleCode/observatoryQuestions.xml
        String xmlPath = proposalDocumentStore.fetchFile(
                "proposalCycles/" + cycleCode + "/" + templateObservatoryQuestionsFileName)
                .getAbsolutePath();
        try {
            saveObservatoryQuestionsToFile(xmlPath, questions, cycle.getTitle(), "Template");
        } catch (IOException e) {
            logger.error("Error saving observatory questions template to file: " + e.getMessage());
            throw new WebApplicationException("Error saving observatory questions template to file",
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
        return Response.ok().build();
    }

    @GET
    @Path("{cycleCode}/observatoryQuestions")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get the observatory-specific questions for the given proposal cycle")
    public List<ObservatoryQuestion> getObservatoryQuestions(
            @PathParam("cycleCode") Long cycleCode,
            @RestQuery Long proposalId
    ) throws IOException, XMLStreamException {

        // location of template for the cycle:
        // storeroot/proposalCycles/cycleCode/observatoryQuestions.xml
        // location of answered questions for the proposal:
        // storeroot/proposalCycles/cycleCode/proposalId/answeredObservatoryQuestions.xml

        // If "answeredObservatoryQuestions.xml" exists for the query proposal, return that.
        // Otherwise, if the template exists, return the template. Finally, if neither exists,
        // return an empty list (observatory questions are optional).

        File templateFile = proposalDocumentStore.fetchFile(
                "proposalCycles/" + cycleCode + "/" + templateObservatoryQuestionsFileName);

        File questionsFile = proposalId == null ?
                templateFile
                :
                proposalDocumentStore.fetchFile(
                "proposalCycles/" + cycleCode + "/" + proposalId +
                        "/" + answeredObservatoryQuestionsFileName);

        if (questionsFile.isFile()) {
            return parseQuestionsXml(questionsFile.getAbsolutePath());
        } else if (templateFile.isFile()) {
            return parseQuestionsXml(templateFile.getAbsolutePath());
        } else {
            return new ArrayList<>(); // Return an empty list if neither file exists
        }
    }

    @POST
    @Path("{cycleCode}/observatoryQuestions/{proposalId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Save answered observatory-specific questions for the given proposal cycle and proposal")
    public Response saveObservatoryQuestions(
            @PathParam("cycleCode") Long cycleCode,
            @PathParam("proposalId") Long proposalId,
            List<ObservatoryQuestion> answeredQuestions)
            throws WebApplicationException
    {
        ObservingProposal proposal = findObject(ObservingProposal.class, proposalId);
        ProposalCycle cycle = findObject(ProposalCycle.class, cycleCode);

        String xmlFilename = answeredObservatoryQuestionsFileName;

        // This is the "working" location of the answered questions for the proposal being submitted to
        // the specific proposal cycle. It will be copied to the (submitted) proposal's supporting documents
        // directory when the proposal is submitted.
        String location = proposalDocumentStore
                .fetchFile("proposalCycles/" + cycleCode + "/" + proposalId).getAbsolutePath();

        // Write the answers to an XML file in the proposal's working directory
        try {
            saveObservatoryQuestionsToFile(
                    location + "/" + xmlFilename,
                    answeredQuestions,
                    cycle.getTitle(),
                    proposal.getTitle());
        } catch (IOException e) {
            logger.error("Error saving answered questions to file: " + e.getMessage());
            throw new WebApplicationException("Error saving answered questions to file",
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
        logger.infof(
                "Answered observatory-specific questions for proposal %d in cycle %d saved to %s",
                proposalId, cycleCode, location + "/" + xmlFilename
        );

        return Response.ok().build();
    }
}
