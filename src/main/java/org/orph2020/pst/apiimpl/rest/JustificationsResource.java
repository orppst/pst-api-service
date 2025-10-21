package org.orph2020.pst.apiimpl.rest;


import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.io.FileUtils;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.management.SubmittedProposal;
import org.ivoa.dm.proposal.prop.*;
import org.jboss.resteasy.reactive.RestQuery;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
    Dev note: there are two "types" of Justification: 'scientific' and 'technical', and these
    are NOT separate Java Classes. They are different Justification members of the
    ObservingProposal class. The Rest end-points specify "which" one we are using, and we deal
    with this is in the implementation. It is HIGHLY UNLIKELY that more "types" of Justification
    will be added as members to the ObservingProposal class, so use of 'if' and 'switch'
    constructs in the implementations is fine.
 */

@Path("proposals/{proposalCode}/justifications")
@Tag(name = "proposals-justifications")
@RolesAllowed("default-roles-orppst")
public class JustificationsResource extends ObjectResourceBase {

    //singular file names for LaTeX and RST justifications
    String texFileName = "main.tex";
    String jobName = "compiledJustification";

    @Inject
    ProposalDocumentStore proposalDocumentStore;

    @GET
    @Path("{which}")
    @Operation(summary = "get the technical or scientific justification associated with the ObservingProposal specified by 'proposalCode'")
    public Justification getJustification(@PathParam("proposalCode") Long proposalCode,
                                          @PathParam("which") String which)
            throws WebApplicationException
    {
        ObservingProposal observingProposal = findObject(ObservingProposal.class, proposalCode);

        //avoid returning nulls to frontend clients
        return switch (which) {
            case "technical" -> {
                Justification technical = observingProposal.getTechnicalJustification();
                yield Objects.requireNonNullElseGet(technical,
                        () -> new Justification("", TextFormats.LATEX));
            }
            case "scientific" -> {
                Justification scientific = observingProposal.getScientificJustification();
                yield Objects.requireNonNullElseGet(scientific,
                        () -> new Justification("", TextFormats.LATEX));
            }
            default -> throw new WebApplicationException(
                    String.format("Justifications are either 'technical' or 'scientific', I got '%s'", which),
                    400
            );
        };
    }

    @PUT
    @Operation( summary = "update the technical or scientific justification associated with the ObservingProposal specified by 'proposalCode'")
    @Path("{which}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn={WebApplicationException.class})
    public Justification updateJustification(
            @PathParam("proposalCode") Long proposalCode,
            @PathParam("which") String which,
            Justification incoming
    )
        throws WebApplicationException
    {
        Justification justification = getWhichJustification(proposalCode, which);

        if (justification == null) {
            throw new WebApplicationException(
                    which + " justification does not exist, please add one instead", 404
            );
        }

        justification.updateUsing(incoming);
        em.merge(justification);

        return justification;
    }

    @POST
    @Operation( summary = "add a technical or scientific Justification to the ObservingProposal specified by the 'proposalCode'")
    @Path("{which}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn={WebApplicationException.class})
    public Justification addJustification(
            @PathParam("proposalCode") Long proposalCode,
            @PathParam("which") String which,
            Justification incoming )
            throws WebApplicationException
    {
        ObservingProposal proposal = findObject(ObservingProposal.class, proposalCode);
        Justification justification = getWhichJustification(proposalCode, which);

        if (justification != null) {
            throw new WebApplicationException(
                    String.format(
                            "Proposal has an existing %s Justification, please use the 'update' method instead",
                            which
                    )
            );
        }


        return addNewChildObject(
                proposal,
                incoming,
                which.equals("technical") ?
                        proposal::setTechnicalJustification :
                        proposal::setScientificJustification
        );
    }


    //*********** LATEX and RST Justifications ************

    @GET
    @Path ("checkForPdf")
    @Operation(summary = "checks for the existence of a latex PDF output file")
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkForPdf(@PathParam("proposalCode") Long proposalCode)
        throws WebApplicationException
    {
        return responseWrapper(
                proposalDocumentStore
                        .fetchFile(supportingDocumentsPath(proposalCode) + jobName + ".pdf")
                        .exists(), 200
        );
    }

    static final String tableLine = " \\hline\n";
    static final String endLine = " \\\\\n";

    private String targetTable(List<Target> targets) {
        StringBuilder proposalTargets = new StringBuilder("\\begin{tabular}{|c|}\n");
        proposalTargets.append(tableLine + " Name " + endLine + tableLine);
        for(Target target : targets) {
            proposalTargets.append(" ")
                    .append(target.getSourceName())
                    .append(endLine).append(tableLine);
        }
        proposalTargets.append("\\end{tabular}\n");
        return proposalTargets.toString();
    }

    private String investigatorsTable(List<Investigator> investigators) {
        StringBuilder proposalInvestigators = new StringBuilder("\\begin{tabular}{|c|c|c|c|}\n");
        proposalInvestigators.append(tableLine + " Name & email & Institute & for PHD?" + endLine + tableLine);
        for(Investigator investigator : investigators) {
            proposalInvestigators.append(" ")
                    .append(investigator.getPerson().getFullName())
                    .append(" & ").append(investigator.getPerson().getEMail())
                    .append(" & ").append(investigator.getPerson().getHomeInstitute().getName())
                    .append(" & ").append(investigator.getForPhD())
                    .append(endLine).append(tableLine);
        }
        proposalInvestigators.append("\\end{tabular}\n");
        return proposalInvestigators.toString();
    }

    private String technicalGoalsTable(List<TechnicalGoal> technicalGoals) {
        StringBuilder proposalTechnicalGoals = new StringBuilder("\\begin{tabular}{|c|}\n");
        proposalTechnicalGoals.append(tableLine + " Angular Resolution " + endLine + tableLine);

        for(TechnicalGoal technicalGoal : technicalGoals) {
            proposalTechnicalGoals.append(" ")
                    .append(technicalGoal.getPerformance().getDesiredAngularResolution())
                    .append(endLine).append(tableLine);
        }
        proposalTechnicalGoals.append("\\end{tabular}\n");
        return proposalTechnicalGoals.toString();
    }

    private String observationsTable(List<Observation> observations) {
        StringBuilder proposalObservations = new StringBuilder("\\begin{tabular}{|c|}\n");
        proposalObservations.append(tableLine + " Name " + endLine + tableLine);
        for(Observation observation : observations) {
            proposalObservations.append(" ").append(observation.getTarget().get(0).getSourceName())
                    .append(endLine).append(tableLine);
        }
        proposalObservations.append("\\end{tabular}\n");
        return proposalObservations.toString();
    }

    @POST
    @Path("latexPdf")
    @Operation(summary = "create PDF of the LaTex Justification from supplied files, we recommend using 'warningsAsErrors=true'")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn={WebApplicationException.class})
    public Response createPDFLaTex(@PathParam("proposalCode") Long proposalCode,
                                   @RestQuery Boolean warningsAsErrors,
                                   @RestQuery Boolean submittedProposal
    )
            throws WebApplicationException, IOException, URISyntaxException {
        // NOTICE: we return "Response.ok" regardless of the exit status of the Latex command because
        // this API call has functioned correctly; it is the user-defined files that need attention.
        // Errors are flagged back to the user as a simple string message containing the list of issues.
        // If there is a problem server side, we throw an exception.

        // NOTICE: 'latexmk' leaves intermediate files in the output directory on failed runs in an
        // attempt to speed up the next compilation. This has been observed to lead to potential
        // problems when producing the PDF output.
        // Removing the output directory on a failed run avoids this issue, but later runs will not
        // benefit from a speed-up. As Justifications are intended to be only a handful of A4
        // pages in length, this is not a problem.

        AbstractProposal proposal = findObject(AbstractProposal.class, proposalCode);
        
        String proposalTitle = proposal.getTitle();
        
        String observingCycleName = submittedProposal ?
                findObject(SubmittedProposal.class, proposalCode).getProposalCode() : 
                null;
        
        String scientificText = proposal.getScientificJustification().getText();

        String technicalText = proposal.getTechnicalJustification().getText();

        String proposalSummary = proposal.getSummary();

        String proposalTargets = targetTable(proposal.getTargets());

        String proposalInvestigators = investigatorsTable(proposal.getInvestigators());

        String proposalTechnicalGoals = technicalGoalsTable(proposal.getTechnicalGoals());

        String proposalObservations = observationsTable(proposal.getObservations());

        Set<String> bibFileList = proposalDocumentStore.listFilesIn(
                proposalDocumentStore.getSupportingDocumentsPath(proposalCode), Collections.singletonList("bib")
        );

        //this shouldn't happen but check anyway
        if (bibFileList.size() > 1) {
            throw new WebApplicationException("Multiple bib files found");
        }

        //Gather together everything needed to successfully compile PDF output
        String workingDirectory = proposalDocumentStore.createLatexWorkingDirectory(
                proposalCode,
                proposalTitle,
                proposalSummary,
                proposalInvestigators,
                proposalTargets,
                proposalTechnicalGoals,
                proposalObservations,
                observingCycleName,
                scientificText,
                technicalText,
                bibFileList.isEmpty() ? null : bibFileList.iterator().next()
        );

        justificationIsLatex(proposalCode);

        File mainTex = proposalDocumentStore.fetchFile(justificationsPath(proposalCode) + "/" + texFileName);

        if (!mainTex.exists()) {
            throw new WebApplicationException(String.format("%s file not found", texFileName));
        }

        ProcessBuilder processBuilder = getLatexmkProcessBuilder(mainTex.getAbsolutePath());

        try {
            Process process = processBuilder.start();

            int exitCode = process.waitFor();

            File logFile = proposalDocumentStore
                    .fetchFile(justificationsPath(proposalCode) + "/out/" + jobName + ".log");

            List<String> warnings = findWarnings(Files.readString(logFile.toPath()));

            StringBuilder errorsStringBuilder = new StringBuilder();

            //if the user selects 'warningsAsErrors', then we need to feed back the warnings along
            //with potential errors for them to attempt to fix the issues, potentially in one go.
            if (warningsAsErrors && !warnings.isEmpty()) {
                errorsStringBuilder
                        .append("You have LaTeX compilation warnings:\n")
                        .append(String.join("\n", warnings))
                        .append("\n\n");
            }

            //check 'latexmk' exit code
            if (exitCode != 0) {
                List<String> errors = scanLogForErrors(logFile);

                errorsStringBuilder
                        .append("You have LaTeX compilation errors:\n")
                        .append(String.join("\n", errors));

                FileUtils.deleteDirectory(new File(workingDirectory)); //clean up latex working directory
                return responseWrapper(errorsStringBuilder.toString(), 200);
            }

            // the exit code is zero here, but there may be warnings
            if (warningsAsErrors && !warnings.isEmpty()) {
                FileUtils.deleteDirectory(new File(workingDirectory)); //clean up latex working directory
                return responseWrapper(errorsStringBuilder.toString(), 200);
            }

        } catch (IOException | InterruptedException e) {
            throw new WebApplicationException(e.getMessage());
        }

        //Here if successful latex compilation

        File logFile = proposalDocumentStore
                .fetchFile(justificationsPath(proposalCode) + "/out/" + jobName + ".log");

        String pageCount = scanLogForPageNumber(logFile);

        //fetch the output PDF of the Justification
        File output = proposalDocumentStore
                .fetchFile(justificationsPath(proposalCode) + "/out/" + jobName + ".pdf");

        String destinationFile = proposalDocumentStore.getSupportingDocumentsPath(proposalCode) +
                jobName+ ".pdf";

        List<SupportingDocument> supportingDocuments = proposal.getSupportingDocuments();

        SupportingDocument supportingDocument = supportingDocuments.stream()
                .filter(s -> s.getLocation()
                        .equals(proposalDocumentStore.getStoreRoot() + destinationFile))
                .findFirst().orElse(null);

        if (supportingDocument == null) {
            //add "justification.pdf" as a new supporting document
            addNewChildObject(
                    proposal,
                    new SupportingDocument(output.getName(),
                            proposalDocumentStore.getStoreRoot() + destinationFile),
                    proposal::addToSupportingDocuments);
        }  //else the file is just being replaced with the latest version

        //move output to the 'supportingDocuments' level
        proposalDocumentStore.moveFile(output, destinationFile);

        //clean up the workingDirectory
        FileUtils.deleteDirectory(new File(workingDirectory));

        return responseWrapper(
                String.format("Latex compilation successful!\nPDF output file saved as: %s\nPage count: %s",
                output.getName(), pageCount), 200);
    }

    @GET
    @Path("latexPdf/download")
    @Operation(summary = "download the pdf file produced after successfully running 'latexmk'")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadLatexPdf(@PathParam("proposalCode") Long proposalCode)
        throws WebApplicationException {

        justificationIsLatex(proposalCode);

        //fetch the output PDF of the Justification
        File output = proposalDocumentStore
                .fetchFile(supportingDocumentsPath(proposalCode) + jobName + ".pdf");

        if (!output.exists()) {
            throw new WebApplicationException(String.format("Nonexistent file: %s", output.getName()));
        }

        return Response.ok(output)
                .header("Content-Disposition", "attachment; filename=" + output.getName())
                .build();
    }

// ****** Convenience functions private to this class ********

    /**
     * Convenience function returning the path to the justifications subdirectory of the given proposal
     * @param proposalCode proposal id
     * @return string of the path to the justifications section
     */
    private String justificationsPath(Long proposalCode) {
        return proposalDocumentStore.getJustificationsPath(proposalCode);
    }

    private String supportingDocumentsPath(Long proposalCode) {
        return proposalDocumentStore.getSupportingDocumentsPath(proposalCode);
    }

    /**
     * convenience function to get the specific Justification
     * @param proposalCode Long id from rest endpoint path
     * @param which String "type" of Justification from rest endpoint path
     * @return the Justification given by the specified parameters, can be null
     */
    private Justification getWhichJustification(Long proposalCode, String which) {
        ObservingProposal observingProposal = findObject(ObservingProposal.class, proposalCode);
        return switch (which) {
            case "technical" -> observingProposal.getTechnicalJustification();
            case "scientific" -> observingProposal.getScientificJustification();
            default -> throw new WebApplicationException(
                    String.format("Justifications are either 'technical' or 'scientific', I got '%s'", which),
                    400
            );
        };
    }

    /**
     * Convenience function to create the ProcessBuilder for 'latexmk'
     *
     * @param filePath  String containing the path-filename of the source *.tex file to compile
     * @return ProcessBuilder of the 'latexmk' command with desired options
     */
    private ProcessBuilder getLatexmkProcessBuilder(String filePath) {
        return new ProcessBuilder(
                "latexmk",
                "-cd", //change directory into the source subdirectory
                "-pdf", //we want PDF output
                "-interaction=nonstopmode", //i.e. non-interactive
                "-output-directory=out", //relative to source directory due to '-cd' option
                "-jobname=" + jobName, //output base name
                filePath
        );
    }

    /**
     * Function to find the "LaTeX Warning"s in the provided string. The input string should
     * be specifically obtained from the output log file of 'latexmk'
     *
     * @param searchStr String to search
     * @return List of Strings containing distinct warnings
     */
    private List<String> findWarnings(String searchStr) {
        Matcher matcher = Pattern
                .compile("^LaTeX Warning.*$", Pattern.MULTILINE)
                .matcher(searchStr);
        List<String> list = new ArrayList<>();
        //"LaTex Warning"s have the details on the same line
        while (matcher.find()) {
            list.add(matcher.group());
        }
        return list.stream().distinct().toList();
    }

    /**
     * Function to scan the latexmk log file for errors generated during compilation of the output
     *
     * @param file File the log file
     * @return List of Strings containing distinct errors
     * @throws FileNotFoundException thrown by Scanner constructor if the input file does not exist
     */
    private List<String> scanLogForErrors(File file)
            throws FileNotFoundException {
        List<String> list = new ArrayList<>();
        Scanner scanner = new Scanner(file);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            //all errors start with "! "
            if (line.contains("! ")) {
                if (line.contains("LaTeX Error")) {
                    // "LaTeX Error"s contain the details on the current line
                    list.add(line);
                } else if (scanner.hasNextLine()) {
                    // other errors have the details on the next line
                    list.add(line + ": " + scanner.nextLine());
                }
            }
        }
        scanner.close();
        return list.stream().distinct().toList();
    }

    private String scanLogForPageNumber(File file) throws WebApplicationException {
        try {
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                //all errors start with "! "
                if (line.contains("Output written on out/" + jobName + ".pdf")) {
                    scanner.close();
                    Pattern pattern = Pattern.compile("\\d+ page|pages");
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        return matcher.group();
                    } else {
                        throw new WebApplicationException(
                                String.format("No page count found in log file: %s", file.getName()));
                    }
                }
            }
            scanner.close();
            throw new WebApplicationException(
                    String.format("No page count found in log file: %s", file.getName())
            );
        } catch (FileNotFoundException e) {
            throw new WebApplicationException(e);
        }
    }


    /**
     * Checks that the Justification exists and has Latex format
     *
     * @param proposalCode Long proposal id
     * @throws WebApplicationException if either the Justification does not exist or the format is not LATEX
     */
    private void justificationIsLatex(Long proposalCode)
            throws WebApplicationException
    {
        Justification scientific = getWhichJustification(proposalCode, "scientific");
        Justification technical = getWhichJustification(proposalCode, "technical");

        //the following check should be impossible, but those are some famous last words.
        if (scientific == null || technical == null) {
            throw new WebApplicationException(String.format(
                    "Proposal code %d, one or both of the justifications does not exist",
                    proposalCode
            ));
        }

        if (scientific.getFormat() != TextFormats.LATEX && technical.getFormat() != TextFormats.LATEX) {
            throw new WebApplicationException(String.format(
                    "Both justifications must be latex format, current formats scientific: %s, technical: %s",
                    scientific.getFormat().toString(), technical.getFormat().toString()
            ));
        }
    }
}

