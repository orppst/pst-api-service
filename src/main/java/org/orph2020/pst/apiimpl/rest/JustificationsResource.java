package org.orph2020.pst.apiimpl.rest;


import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.prop.Justification;
import org.ivoa.dm.proposal.prop.ObservingProposal;
import org.ivoa.dm.proposal.prop.TextFormats;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
    Dev note: there are two "types" of Justification: 'scientific' and 'technical', and these
    are NOT separate Java Classes. They are different Justification members of the
    ObservingProposal class. The Rest end-points specify "which" one we are using, and we deal
    with this is in the implementation. It is HIGHLY UNLIKELY that more "types" of Justification
    will be added as members to the ObservingProposal class, so simple 'if' and 'switch'
    constructs are okay to use.
 */

@Path("proposals/{proposalCode}/justifications")
@Tag(name = "proposals-justifications")
@RolesAllowed("default-roles-orppst")
public class JustificationsResource extends ObjectResourceBase {

    //singular file names for LaTeX and RST justifications
    String texFileName = "main.tex";
    String rstFileName = "main.rst";

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
    @Operation( summary = "update a technical or scientific Justification in the ObservingProposal specified by the 'proposalCode'")
    @Path("{which}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn={WebApplicationException.class})
    public Justification updateJustification(
            @PathParam("proposalCode") Long proposalCode,
            @PathParam("which") String which,
            Justification incoming )
            throws WebApplicationException
    {
        Justification justification = getWhichJustification(proposalCode, which);

        if (justification == null)
            throw new WebApplicationException(
                    String.format(
                            "The Proposal has no existing %s justification, please 'add' one",
                            which
                    )
            );

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
    @Path("{which}/latexResource")
    @Operation(summary = "list the resource files (images, bibliography) uploaded by the user")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<String> getLatexResourceFiles(
            @PathParam("proposalCode") Long proposalCode,
            @PathParam("which") String which)
        throws WebApplicationException
    {
        try {
            Justification justification = getWhichJustification(proposalCode, which);
            List<String> extensions = new ArrayList<>();
            extensions.add(".jpg");
            extensions.add(".png");
            extensions.add(".eps");
            extensions.add(".jpeg");
            if (justification.getFormat() == TextFormats.LATEX) {
                extensions.add(".bib");
            }
            return proposalDocumentStore.listFilesIn(justificationsStorePath(proposalCode, which), extensions);
        } catch (IOException e) {
            throw new WebApplicationException(e.getMessage());
        }
    }

    //required to make form-upload input work
    @Schema(type = SchemaType.STRING, format = "binary")
    public static class UploadLatexResourceSchema {}

    //non-transactional, no modification to the database occurs
    @POST
    @Path("{which}/latexResource")
    @Operation(summary = "add a resource file for LaTeX or RST Justifications; .bib, .jpg, .png, .eps supported only")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response addLatexResourceFile(
            @PathParam("proposalCode") Long proposalCode,
            @PathParam("which") String which,
            @RestForm("document") @Schema(implementation = UploadLatexResourceSchema.class)
            FileUpload fileUpload)
        throws WebApplicationException
    {
        checkResourceUpload(fileUpload);

        String filename = fileUpload.fileName();

        // save the uploaded file to the document store
        // this will overwrite existing files with the same filename
        if (!proposalDocumentStore.moveFile(fileUpload.uploadedFile().toFile(),
                justificationsStorePath(proposalCode, which) + "/" + filename)) {
            throw new WebApplicationException("Unable to save uploaded file");
        }

        return Response.ok(String.format("File %s saved", filename)).build();
    }

    @GET
    @Path("{which}/mainFile")
    @Operation(summary = "get the main file (.tex or .rst) of your justification")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getMainTextFile(
            @PathParam("proposalCode") Long proposalCode,
            @PathParam("which") String which
    ) throws WebApplicationException
    {
        Justification justification = getWhichJustification(proposalCode, which);

        String filename = justification.getFormat() == TextFormats.LATEX ? texFileName : rstFileName;

        File fileDownload = proposalDocumentStore.fetchFile(
                justificationsStorePath(proposalCode, which) + "/" + filename);

        if (!fileDownload.exists()) {
            throw new WebApplicationException(String.format("%s file not found", filename));
        }

        return Response.ok(fileDownload)
                .header("Content-Disposition", "attachment; filename=" + fileDownload.getName())
                .build();
    }

    @POST
    @Path("{which}/mainFile")
    @Operation(summary = "upload the main file (.tex or .rst) for your justification (overwrites any existing main file)")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response addMainTextFile(
            @PathParam("proposalCode") Long proposalCode,
            @PathParam("which") String which,
            @RestForm("document") @Schema(implementation = UploadLatexResourceSchema.class)
            FileUpload fileUpload
    )
            throws WebApplicationException
    {
        checkMainFileUpload(fileUpload);

        String extension = FilenameUtils.getExtension(fileUpload.fileName());

        String filename = extension.equals("tex") ? texFileName : rstFileName;

        // save the uploaded file to the document store,
        // this will overwrite the existing file with the same filename
        if (!proposalDocumentStore.moveFile(fileUpload.uploadedFile().toFile(),
                justificationsStorePath(proposalCode, which) + "/" + filename)) {
            throw new WebApplicationException("Unable to save uploaded file");
        }

        return Response.ok(String.format("File %s saved as %s", fileUpload.fileName(), filename)).build();
    }


    //non-transactional, no modification to the database occurs
    @DELETE
    @Path("{which}/latexResource")
    @Operation(summary = "remove the given resource file from the Justification")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response removeLatexResourceFile(
            @PathParam("proposalCode") Long proposalCode,
            @PathParam("which") String which,
            String filename)
    throws WebApplicationException
    {
        if (!proposalDocumentStore.deleteFile(justificationsStorePath(proposalCode, which) + "/" + filename)) {
            throw new WebApplicationException(String.format("Unable to delete file: %s", filename));
        }

        return Response.ok(String.format("File %s deleted", filename)).build();
    }

    @GET
    @Path ("{which}/checkForMain")
    @Operation(summary = "checks for the existence of a 'main' file, either .tex or .rst")
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkForMainFile(@PathParam("proposalCode") Long proposalCode,
                                     @PathParam("which") String which)
        throws WebApplicationException
    {
        Justification justification = getWhichJustification(proposalCode, which);
        String filename = justification.getFormat() == TextFormats.LATEX ? texFileName : rstFileName;


        return responseWrapper(
                proposalDocumentStore
                        .fetchFile(justificationsStorePath(proposalCode, which) + "/" + filename)
                        .exists(), 200
        );
    }

    @GET
    @Path ("{which}/checkForPdf")
    @Operation(summary = "checks for the existence of a latex PDF output file")
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkForPdf(@PathParam("proposalCode") Long proposalCode,
                                @PathParam("which") String which
    )
        throws WebApplicationException
    {
        return responseWrapper(
                proposalDocumentStore
                        .fetchFile(justificationsStorePath(proposalCode, which) + "/out/" + which + "-justification.pdf")
                        .exists(), 200
        );
    }


    @POST
    @Path("{which}/latexPdf")
    @Operation(summary = "create PDF of the LaTex Justification from supplied files, we recommend using 'warningsAsErrors=true'")
    @Produces(MediaType.APPLICATION_JSON)
    public Response createPDFLaTex(@PathParam("proposalCode") Long proposalCode,
                                   @PathParam("which") String which,
                                   @RestQuery Boolean warningsAsErrors
    )
        throws WebApplicationException
    {
        // NOTICE: we return "Response.ok" regardless of the exit status of the Latex command because
        // this API call has functioned correctly; it is the user-defined files that need attention.
        // Errors are flagged back to the user as a simple string message containing the list of issues.
        // If there is a problem server side, we throw an exception.

        // NOTICE: 'latexmk' leaves intermediate files in the output directory on failed runs in an
        // attempt to speed up the next compilation. This has been observed to lead to potential
        // problems when producing the PDF output.
        // Removing the output directory on a failed run avoids this issue, but later runs will not
        // benefit from a speed-up. As Justifications are, at most, intended to be a couple of A4
        // pages in length, this is not a problem.

        justificationIsLatex(proposalCode, which);

        File mainTex = proposalDocumentStore.fetchFile(justificationsStorePath(proposalCode, which) + "/" + texFileName);

        if (!mainTex.exists()) {
            throw new WebApplicationException(String.format("%s file not found", texFileName));
        }

        ProcessBuilder processBuilder = getLatexmkProcessBuilder(
                mainTex.getAbsolutePath(), which);

        try {
            Process process = processBuilder.start();

            int exitCode = process.waitFor();

            // output directory "out" created by latexmk process
            File outputDir = proposalDocumentStore.fetchFile(justificationsStorePath(proposalCode, which) + "/out");

            File logFile = proposalDocumentStore
                    .fetchFile(justificationsStorePath(proposalCode, which) + "/out/" + which + "-justification.log");

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

                FileUtils.deleteDirectory(outputDir); //clean up latex generated files for the next run
                return responseWrapper(errorsStringBuilder.toString(), 200);
            }

            // the exit code is zero here, but there may be warnings
            if (warningsAsErrors && !warnings.isEmpty()) {
                FileUtils.deleteDirectory(outputDir); //clean up latex generated files for the next run
                return responseWrapper(errorsStringBuilder.toString(), 200);
            }

        } catch (IOException | InterruptedException e) {
            throw new WebApplicationException(e.getMessage());
        }

        //fetch the output PDF of the Justification
        File output = proposalDocumentStore
                .fetchFile(justificationsStorePath(proposalCode, which) + "/out/" + which + "-justification.pdf");

        return responseWrapper(
                String.format("Latex compilation successful!\nPDF output file saved as: %s",
                output.getName()), 200);
    }

    @GET
    @Path("{which}/latexPdf/download")
    @Operation(summary = "download the pdf file produced after successfully running 'latexmk'")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadLatexPdf(
            @PathParam("proposalCode") Long proposalCode,
            @PathParam("which") String which)
        throws WebApplicationException {

        justificationIsLatex(proposalCode, which);

        //fetch the output PDF of the Justification
        File output = proposalDocumentStore
                .fetchFile(justificationsStorePath(proposalCode, which) + "/out/" + which + "-justification.pdf");

        if (!output.exists()) {
            throw new WebApplicationException(String.format("Nonexistent file: %s", output.getName()));
        }

        return Response.ok(output)
                .header("Content-Disposition", "attachment; filename=" + output.getName())
                .build();
    }



// ****** Convenience functions private to this class ********

    /**
     * Create the path to the justifications section of the proposal document store
     * @param proposalCode proposal id
     * @param which "scientific" or "technical"
     * @return string of the path to the justifications section
     */
    private String justificationsStorePath(Long proposalCode, String which) {
        return proposalCode + "/justifications/" + which;
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
     * @param which     String "type" of Justification
     * @return ProcessBuilder of the 'latexmk' command with desired options
     */
    private ProcessBuilder getLatexmkProcessBuilder(String filePath, String which) {
        return new ProcessBuilder(
                "latexmk",
                "-cd", //change directory into the source subdirectory
                "-pdf", //we want PDF output
                "-interaction=nonstopmode", //i.e. non-interactive
                "-output-directory=out", //relative to source directory due to '-cd' option
                "-jobname=" + which + "-justification", //output base name
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
        return list.stream().distinct().toList();
    }

    /**
     * Checks that the Justification exists and has Latex format
     *
     * @param proposalCode Long proposal id
     * @param which        String "type" of Justification
     * @throws WebApplicationException if either the Justification does not exist or the format is not LATEX
     */
    private void justificationIsLatex(Long proposalCode, String which)
            throws WebApplicationException
    {
        Justification justification = getWhichJustification(proposalCode, which);

        if (justification == null) {
            throw new WebApplicationException(String.format(
                    "Proposal code %d, %s justification does not exist",
                    proposalCode, which
            ));
        }

        if (justification.getFormat() != TextFormats.LATEX) {
            throw new WebApplicationException(String.format(
                    "%s Justification not LaTeX format, current format: %s",
                    which, justification.getFormat().toString()
            ));
        }
    }

    /**
     * Function to check uploaded files content-type and file extension
     * @param fileUpload the uploaded file
     * @throws WebApplicationException if fileUpload is null, or content-type is null,
     * or file extension is empty, or file extension does not match the content-type
     */
    private void checkResourceUpload(FileUpload fileUpload)
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

        // mime types aren't very well-defined, so we include octect-stream as a coverall for, well, everything.

        switch (contentType) {
            case "application/octet-stream":
            case "application/x-bibtex":
            case "application/postscript":
            case "image/jpeg":
            case "image/png":
            case "image/x-eps":
                if (!extension.equals("bib") &&
                        !extension.equals("jpeg") &&
                        !extension.equals("jpg") &&
                        !extension.equals("png") &&
                        !extension.equals("eps")
                )
                    throw new WebApplicationException(
                            String.format("Invalid file extension: %s", extension));
                break;
            default:
                throw new WebApplicationException(
                        String.format("content-type: %s is not supported", contentType));
        }

        //NOTICE: content-type and file extension can be manipulated so that they misrepresent the
        // actual contents of the file. However, on Linux and Linux-like systems uploaded files saved
        // in this way have read-write permissions for the 'user' only (the "application" is the user here),
        // i.e., no executable permissions, and no permissions for 'groups' or 'other users'.
    }

    private void checkMainFileUpload(FileUpload fileUpload) throws WebApplicationException {
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

        // mime type for .rst unknown, so we use 'application/octet-stream' as a coverall.
        switch (contentType) {
            case "application/octet-stream":
            case "application/x-tex":
            case "text/x-tex":
                if (!extension.equals("tex") && !extension.equals("rst"))
                    throw new WebApplicationException(
                            String.format("Invalid file extension: %s", extension));
                break;
            default:
                throw new WebApplicationException(
                        String.format("content-type: %s is not supported", contentType));
        }

    }

}

