package org.orph2020.pst.apiimpl.rest;


import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
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
import org.ivoa.dm.proposal.prop.SupportingDocument;
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
    will be added as members to the ObservingProposal class, so use of 'if' and 'switch'
    constructs in the implementations is fine.
 */

@Path("proposals/{proposalCode}/justifications")
@Tag(name = "proposals-justifications")
@RolesAllowed("default-roles-orppst")
public class JustificationsResource extends ObjectResourceBase {

    //singular file names for LaTeX and RST justifications
    String texFileName = "main.tex";
    String scientificTexFileName = "scientificJustification.tex";
    String technicalTexFileName = "technicalJustification.tex";
    //String templateTex = "mainTemplate.tex";

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

        if (justification.getFormat() == TextFormats.LATEX) {
            try {
                updateJustificationTex(proposalCode, which, justification.getText());
            } catch (IOException e) {
                throw new WebApplicationException(e);
            }
        }

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

        Justification result = addNewChildObject(
                proposal,
                incoming,
                which.equals("technical") ?
                        proposal::setTechnicalJustification :
                        proposal::setScientificJustification
        );

        if (result.getFormat() == TextFormats.LATEX) {
            try {
                updateJustificationTex(proposalCode, which, result.getText());
            } catch (IOException e) {
                throw new WebApplicationException(e);
            }
        }

        return result;
    }


    //*********** LATEX and RST Justifications ************

    @GET
    @Path("resourceFile")
    @Operation(summary = "list the resource files (images, bibliography) uploaded by the user")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<String> getLatexResourceFiles(
            @PathParam("proposalCode") Long proposalCode)
        throws WebApplicationException
    {
        //either justification will do to check the format as we ensure they are the same format
        //elsewhere

        try {
            Justification justification = getWhichJustification(proposalCode, "scientific");
            List<String> extensions = new ArrayList<>();
            extensions.add(".jpg");
            extensions.add(".png");
            extensions.add(".eps");
            extensions.add(".jpeg");
            extensions.add(".pdf");
            if (justification.getFormat() == TextFormats.LATEX) {
                extensions.add(".bib");
            }
            return proposalDocumentStore.listFilesIn(justificationsPath(proposalCode), extensions);
        } catch (IOException e) {
            throw new WebApplicationException(e.getMessage());
        }
    }

    //required to make form-upload input work
    @Schema(type = SchemaType.STRING, format = "binary")
    public static class UploadLatexResourceSchema {}

    @POST
    @Path("resourceFile")
    @Operation(summary = "add a resource file for LaTeX or RST Justifications; .bib, .jpg, .png, .eps, .pdf supported only")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional(rollbackOn={WebApplicationException.class})
    public Response addLatexResourceFile(
            @PathParam("proposalCode") Long proposalCode,
            @RestForm("document") @Schema(implementation = UploadLatexResourceSchema.class)
            FileUpload fileUpload)
        throws WebApplicationException
    {
        String extension = checkResourceUpload(fileUpload);

        String filename = extension.equals("bib") ? "refs.bib" : fileUpload.fileName();

        String saveFileAs = proposalDocumentStore.getSupportingDocumentsPath(proposalCode) + filename;

        // save the uploaded file to the document store
        // this will overwrite existing files with the same filename
        if (!proposalDocumentStore.moveFile(fileUpload.uploadedFile().toFile(), saveFileAs)) {
            throw new WebApplicationException("Unable to save uploaded file");
        }

        //copy file into the justifications working directory
        try {
            FileUtils.copyFile(proposalDocumentStore.fetchFile(saveFileAs),
                    proposalDocumentStore.fetchFile(
                            proposalDocumentStore.getJustificationsPath(proposalCode) + filename));
        } catch (IOException e) {
            throw new WebApplicationException(e);
        }


        //store the resource file as a Supporting Document, notice we set the document title as the filename
        ObservingProposal proposal = findObject(ObservingProposal.class, proposalCode);
        addNewChildObject(
                proposal,
                new SupportingDocument(
                        filename,
                        proposalDocumentStore.fetchFile(saveFileAs).getAbsolutePath()
                ),
                proposal::addToSupportingDocuments
        );

        if (extension.equals("bib")) {
            // replace any latex macro calls in the bibliography file with literal acronyms
            try {
                //this call creates a new, sanitised "refs.bib" in the justifications working directory
                replaceMacrosWithLiteralAcronyms(proposalCode);
            } catch (IOException e) {
                throw new WebApplicationException(e);
            }

        }

        return Response.ok(String.format("File %s saved", filename)).build();
    }

    @DELETE
    @Path("resourceFile/{fileName}")
    @Operation(summary = "remove the given resource file from the Justification")
    @Transactional(rollbackOn={WebApplicationException.class})
    public Response removeLatexResourceFile(
            @PathParam("proposalCode") Long proposalCode,
            @PathParam("fileName") String fileName)
    throws WebApplicationException
    {
        ObservingProposal proposal = findObject(ObservingProposal.class, proposalCode);

        String queryStr = "select s from ObservingProposal o inner join o.supportingDocuments s where o._id = :pid and s.title = :fname";

        TypedQuery<SupportingDocument> q = em.createQuery(queryStr, SupportingDocument.class);
        q.setParameter("pid", proposalCode);
        q.setParameter("fname", fileName);

        try {
            SupportingDocument supportingDocument = q.getSingleResult();

            //file in the supporting documents directory
            File fileToRemove = new File(supportingDocument.getLocation());

            //copy of file in the justifications working directory
            File copyToRemove = proposalDocumentStore.fetchFile(
                    proposalDocumentStore.getJustificationsPath(proposalCode) + fileName
            );

            deleteChildObject(proposal, supportingDocument, proposal::removeFromSupportingDocuments);

            if (!fileToRemove.delete()) {
                throw new WebApplicationException("unable to delete file: " + fileToRemove.getName());
            }

            if (!copyToRemove.delete()) {
                throw new WebApplicationException("unable to delete copy of file: " + copyToRemove.getName());
            }

            return Response.ok(String.format("File %s deleted", fileToRemove.getName())).build();
        } catch (NoResultException e) {
            throw new WebApplicationException(e);
        }
    }

    @GET
    @Path ("checkForPdf")
    @Operation(summary = "checks for the existence of a latex PDF output file")
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkForPdf(@PathParam("proposalCode") Long proposalCode)
        throws WebApplicationException
    {
        return responseWrapper(
                proposalDocumentStore
                        .fetchFile(justificationsPath(proposalCode) + "/out/" + "justification.pdf")
                        .exists(), 200
        );
    }


    @POST
    @Path("latexPdf")
    @Operation(summary = "create PDF of the LaTex Justification from supplied files, we recommend using 'warningsAsErrors=true'")
    @Produces(MediaType.APPLICATION_JSON)
    public Response createPDFLaTex(@PathParam("proposalCode") Long proposalCode,
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
        // benefit from a speed-up. As Justifications are intended to be only a handful of A4
        // pages in length, this is not a problem.

        justificationIsLatex(proposalCode);

        File mainTex = proposalDocumentStore.fetchFile(justificationsPath(proposalCode) + "/" + texFileName);

        if (!mainTex.exists()) {
            throw new WebApplicationException(String.format("%s file not found", texFileName));
        }

        ProcessBuilder processBuilder = getLatexmkProcessBuilder(mainTex.getAbsolutePath());

        try {
            Process process = processBuilder.start();

            int exitCode = process.waitFor();

            // output directory "out" created by latexmk process
            File outputDir = proposalDocumentStore.fetchFile(justificationsPath(proposalCode) + "/out");

            File logFile = proposalDocumentStore
                    .fetchFile(justificationsPath(proposalCode) + "/out/" + "justification.log");

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

        //Here if successful latex compilation

        File logFile = proposalDocumentStore
                .fetchFile(justificationsPath(proposalCode) + "/out/" + "justification.log");

        String pageCount = scanLogForPageNumber(logFile);

        //fetch the output PDF of the Justification
        File output = proposalDocumentStore
                .fetchFile(justificationsPath(proposalCode) + "/out/" + "justification.pdf");

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
                .fetchFile(justificationsPath(proposalCode) + "/out/" + "justification.pdf");

        if (!output.exists()) {
            throw new WebApplicationException(String.format("Nonexistent file: %s", output.getName()));
        }

        return Response.ok(output)
                .header("Content-Disposition", "attachment; filename=" + output.getName())
                .build();
    }

    @GET
    @Path("latexPdf/pages")
    @Operation(summary = "get the number of pages in the pdf file produced after successfully running 'latexmk'")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLatexPdfPages(@PathParam("proposalCode") Long proposalCode)
        throws WebApplicationException {

        File logFile = proposalDocumentStore
                .fetchFile(justificationsPath(proposalCode) + "/out/" + "justification.log");

        String pageCount = scanLogForPageNumber(logFile);

        return responseWrapper(pageCount, 200);
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
                "-jobname=" + "justification", //output base name
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
                if (line.contains("Output written on out/justification.pdf")) {
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

    /**
     * Function to check uploaded files content-type and file extension
     * @param fileUpload the uploaded file
     * @return String containing the file extension.
     * @throws WebApplicationException if fileUpload is null, or content-type is null,
     * or file extension is empty, or file extension does not match the content-type
     */
    private String checkResourceUpload(FileUpload fileUpload)
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
            case "application/pdf":
                if (!extension.equals("bib") &&
                        !extension.equals("jpeg") &&
                        !extension.equals("jpg") &&
                        !extension.equals("png") &&
                        !extension.equals("eps") &&
                        !extension.equals("pdf")
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

        return extension;
    }


    private void updateJustificationTex(
            Long proposalCode,
            String which,
            String theText
    )
            throws IOException {

        String theFile = which.equals("scientific") ? scientificTexFileName : technicalTexFileName;

        proposalDocumentStore.writeStringToFile(theText,
                justificationsPath(proposalCode) + theFile
        );
    }

    private void replaceMacrosWithLiteralAcronyms(Long proposalCode)
    throws IOException {
        // I don't know who said it first but acronyms really are the bane of modern life!

        // Automatically exported references tend to give journal acronyms as a latex macro
        // e.g. 'journal = {\mnras}' meaning you must have that macro, \mnras, defined in your
        // .tex file/s somewhere to print the journal title however you want it printed.
        // This is fine for articles were you're in control of the bibliography; you simply
        // provide the macro definition. As we are providing a generalised main.tex template
        // this is not so straightforward.
        // We either have to define any and all possible journal acronyms as macros in the
        // 'main.tex' template, this is impractical,
        // --OR--
        // ask users to provide the macro definitions for the references they have cited,
        // taking up precious character space in their justifications (and is tedious, and even possible?)
        // --OR--
        // we programmatically edit the references here, replacing any "journal = {\xxx}" with
        // "journal = XXX", but there are exceptions - see below
        // None of these "solutions" are ideal.

        // Rather disappointingly the macro for "Astrophysics and Space Science" is not "\ass" but "\apss"
        // because generally Astrophysics is shortened to "Ap". So in general the string "ap" needs to be
        // replaced with "Ap" not "AP".
        // Even so, there are exceptions to this exception. For example, the acronym for
        // "Astronomy & Astrophysics" is "A&A" rather than "AAp", the macro for which is "\aap".
        // This is further complicated by "A&A" also having "review" and "supplementary" versions;
        // "\aapr" and "\aaps" respectively.
        // Another exception is the macro "\nat", which should be replaced with "Nature" not "NAT".
        // There are potentially other exceptions. So what could possibly go wrong? :P


        //get the original references.bib upload
        File references = proposalDocumentStore.fetchFile(
                proposalDocumentStore.getSupportingDocumentsPath(proposalCode) + "refs.bib");

        Scanner theScanner = new Scanner(references);
        StringBuilder newContent = new StringBuilder();

        while (theScanner.hasNextLine()) {
            String line = theScanner.nextLine();
            // I LOVE Java regex strings, they're SO intuitive!
            // We're matching for ' journal = {\<macro>},' where there could be zero or more spaces
            if (line.matches(" *journal *= *\\{\\\\[a-z]+},$")) {
                String acronym = line.substring(line.indexOf("{\\") + 1, line.lastIndexOf("}"));
                switch (acronym) {
                    case "\\aap":
                        newContent.append(line.replace("\\aap", "A&A"));
                        break;
                    case "\\aapr":
                        newContent.append(line.replace("\\aapr", "A&A Rev."));
                        break;
                    case "\\aaps":
                        newContent.append(line.replace("\\aaps", "A&A Sup."));
                        break;
                    case "\\jcap":
                        //Journal of Cosmology and Astroparticle Physics (avoids "AP" -> "Ap" issue)
                        newContent.append(line.replace("\\jcap", "JCAP"));
                        break;
                    case "\\nat":
                        newContent.append(line.replace("\\nat", "Nature"));
                        break;
                    default: {
                        String newLine = line.replace(
                                acronym, acronym.substring(acronym.indexOf("\\") + 1).toUpperCase());
                        if (acronym.contains("ap")) {
                            newContent.append(newLine.replace("AP", "Ap"));
                        } else {
                            newContent.append(newLine);
                        }
                        break;
                    }
                }
            } else {
                newContent.append(line);
            }
            newContent.append("\n");
        }
        theScanner.close();

        //write result to the justifications working directory
        proposalDocumentStore.writeStringToFile(newContent.toString(),
                justificationsPath(proposalCode) + "/refs.bib");
    }

}

