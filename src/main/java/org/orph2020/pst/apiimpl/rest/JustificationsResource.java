package org.orph2020.pst.apiimpl.rest;


import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
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
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
public class JustificationsResource extends ObjectResourceBase {

    @ConfigProperty(name= "supporting-documents.store-root")
    String documentStoreRoot;

    //common file name for the Tex file for Latex type Justifications
    String texFileName = "main.tex";


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
                        () -> new Justification("", TextFormats.ASCIIDOC));
            }
            case "scientific" -> {
                Justification scientific = observingProposal.getScientificJustification();
                yield Objects.requireNonNullElseGet(scientific,
                        () -> new Justification("", TextFormats.ASCIIDOC));
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
            @PathParam("proposalCode") long proposalCode,
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

        //if justification format is Latex then update the *.tex file
        if (justification.getFormat() == TextFormats.LATEX) {
            try {
                writeLatexToFile(justification.getText(), proposalCode, which);
            } catch (IOException e) {
                //if we can't write the update to the *.tex file then we should roll back
                //the database transaction - otherwise may have mismatch between the database string
                //and the *.tex file.
                throw new WebApplicationException(e.getMessage());
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
            @PathParam("proposalCode") long proposalCode,
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

        Justification persisted =
                addNewChildObject(
                        proposal,
                        incoming,
                        which.equals("technical") ?
                                proposal::setTechnicalJustification :
                                proposal::setScientificJustification
        );

        if (persisted.getFormat() == TextFormats.LATEX) {
            try {
                //create and write the string to file
                writeLatexToFile(persisted.getText(), proposalCode, which);
            } catch (IOException e) {
                //if we can't write the justification text to the *.tex file then we should roll back
                //the database transaction - otherwise may have mismatch between the database string
                //and the *.tex file.
                throw new WebApplicationException(e.getMessage());
            }
        }
        return persisted;
    }


    //*********** LATEX Justifications ************

    @GET
    @Path("{which}/latexResource")
    @Operation(summary = "list the latex resource files uploaded by the user")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<String> getLatexResourceFiles(
            @PathParam("proposalCode") Long proposalCode,
            @PathParam("which") String which)
        throws WebApplicationException
    {
        try {
            return listResourceFilesInDir(
                    documentStoreRoot
                    + "/proposals/"
                    + proposalCode
                    + "/justifications/"
                    + which
            );
        } catch (IOException e) {
            throw new WebApplicationException(e.getMessage());
        }
    }

    //required to make form-upload input work
    @Schema(type = SchemaType.STRING, format = "binary")
    public static class UploadItemSchema {}

    //non-transactional, no modification to the database occurs
    @POST
    @Path("{which}/latexResource")
    @Operation(summary = "add a resource file for latex Justifications, only *.bib, *.jpg, and *.png supported")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response addLatexResourceFile(
            @PathParam("proposalCode") Long proposalCode,
            @PathParam("which") String which,
            @RestForm("document") @Schema(implementation = UploadItemSchema.class)
            FileUpload fileUpload)
        throws WebApplicationException
    {
        justificationIsLatex(proposalCode, which);

        checkFileUpload(fileUpload);

        String filename = fileUpload.fileName();

        File destination = getFile(proposalCode, which, filename);

        //check filename is unique per Justification
        if (destination.exists()) {
            throw new WebApplicationException(
                    String.format("File %s already exists, please 'replace' this file instead",
                            filename));
        }

        //save the uploaded file to the assigned location
        if (!fileUpload.uploadedFile().toFile().renameTo(destination)) {
            throw new WebApplicationException("Unable to save uploaded file");
        }

        return Response.ok(String.format("File %s saved", filename)).build();
    }


    @PUT
    @Path("{which}/latexResource")
    @Operation(summary = "replace a resource file for the given latex Justification")
    public Response replaceLatexResourceFile(
            @PathParam("proposalCode") Long proposalCode,
            @PathParam("which") String which,
            @RestForm("document") @Schema(implementation = UploadItemSchema.class)
            FileUpload fileUpload)
        throws WebApplicationException
    {
        justificationIsLatex(proposalCode, which);

        checkFileUpload(fileUpload);

        String filename = fileUpload.fileName();

        File destination = getFile(proposalCode, which, filename);

        if (!destination.exists()) {
            throw new WebApplicationException(
                    String.format("%s, does not exist, please 'add' this file instead", filename)
            );
        }

        //replace the file to the assigned location
        if (!fileUpload.uploadedFile().toFile().renameTo(destination)) {
            throw new WebApplicationException("Unable to save uploaded file");
        }

        return Response.ok(String.format("File %s replaced", filename)).build();
    }

    //non-transactional, no modification to the database occurs
    @DELETE
    @Path("{which}/latexResource")
    @Operation(summary = "remove the given resource file from the latex Justification")
    public Response removeLatexResourceFile(
            @PathParam("proposalCode") Long proposalCode,
            @PathParam("which") String which,
            String filename)
    throws WebApplicationException
    {
        justificationIsLatex(proposalCode, which);

        File toDelete = getFile(proposalCode, which, filename);

        if (!toDelete.exists()) {
            throw new WebApplicationException(String.format("Nonexistent file: %s", filename));
        }

        if (!toDelete.delete()) {
            throw new WebApplicationException(String.format("Unable to delete file: %s", filename));
        }

        return Response.ok(String.format("File %s deleted", filename)).build();
    }

    @GET
    @Path("{which}/latexPdf")
    @Operation(summary = "create PDF of the LaTex Justification from supplied files, we recommend using 'warningsAsErrors=true'")
    public Response createPDFLaTex(@PathParam("proposalCode") Long proposalCode,
                                              @PathParam("which") String which,
                                              @RestQuery Boolean warningsAsErrors
    )
        throws WebApplicationException
    {
        // NOTICE: we return "Response.ok" regardless of the exit status of the Latex command because
        // this API call has functioned correctly; it is the user defined files that need attention.
        // Errors are flagged back to the user as simple string message containing the list of issues.
        // If there is a problem server side we throw an exception.

        // NOTICE: 'latexmk' leaves intermediate files in the output directory on failed runs in an
        // attempt to speed up the next compilation. This has been observed to lead to potential
        // problems when producing the PDF output.
        // Removing the output directory on a failed run avoids this issue; subsequent runs do so
        // on a clean directory, but obviously they do not benefit from a speed-up.

        justificationIsLatex(proposalCode, which);

        File mainTex = getFile(proposalCode, which, texFileName);

        if (!mainTex.exists()) {
            throw new WebApplicationException(String.format("%s file not found", texFileName));
        }

        ProcessBuilder processBuilder = getLatexmkProcessBuilder(
                mainTex.getAbsolutePath(), which);

        try {
            Process process = processBuilder.start();

            int exitCode = process.waitFor();

            File outputDir = new File(documentStoreRoot
                    + "/proposals/"
                    + proposalCode
                    + "/justifications/"
                    + which
                    + "/out");

            //the outputDir should exist here
            if (!outputDir.exists()) {
                throw new WebApplicationException("Output directory does not exist");
            }

            File logFile = new File(outputDir, which + "-justification.log");

            //the log file should exist here
            if (!logFile.exists()) {
                FileUtils.deleteDirectory(outputDir); //clean up latex generated files for next run
                throw new WebApplicationException("Log file does not exist: " + logFile.getAbsolutePath());
            }

            List<String> warnings = findWarnings(Files.readString(logFile.toPath()));

            StringBuilder errorsStringBuilder = new StringBuilder();

            //if user selects 'warningsAsErrors' then we need to feed back the warnings along
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

                FileUtils.deleteDirectory(outputDir); //clean up latex generated files for next run
                return Response.ok(errorsStringBuilder.toString()).build();
            }

            // exit code is zero here, but there may be warnings
            if (warningsAsErrors && !warnings.isEmpty()) {
                FileUtils.deleteDirectory(outputDir); //clean up latex generated files for next run
                return Response.ok(errorsStringBuilder.toString()).build();
            }

        } catch (IOException | InterruptedException e) {
            throw new WebApplicationException(e.getMessage());
        }

        //fetch the output PDF of the Justification
        File output = getFile(proposalCode, which, "out/" + which + "-justification.pdf");

        return Response.ok(
                String.format("Latex compilation successful, PDF produced, file saved as: %s",
                        output.getName()))
                .build();
    }

    @GET
    @Path("{which}/latexPdf/download")
    @Operation(summary = "download the pdf file produced after running successfully running 'latexmk'")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadLatexPdf(
            @PathParam("proposalCode") Long proposalCode,
            @PathParam("which") String which)
        throws WebApplicationException {

        justificationIsLatex(proposalCode, which);

        //fetch the output PDF of the Justification
        File output = getFile(proposalCode, which, "out/" + which + "-justification.pdf");

        if (!output.exists()) {
            throw new WebApplicationException(String.format("Nonexistent file: %s", output.getName()));
        }

        return Response.ok(output)
                .header("Content-Disposition", "attachment; filename=" + output.getName())
                .build();

    }



// ****** Convenience functions private to this class ********


    /**
     * List *.bib, *.png, *.jp(e)g files in the given directory
     * @param dir the directory to search
     * @return Set of Strings containing the resource files
     * @throws IOException in case of 'dir' not being a directory
     */
    private Set<String> listResourceFilesInDir(String dir) throws IOException {
        try (Stream<java.nio.file.Path> stream = Files.list(Paths.get(dir))) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(java.nio.file.Path::getFileName)
                    .map(java.nio.file.Path::toString)
                    .filter(string -> string.endsWith(".bib")
                            || string.endsWith(".png")
                            || string.endsWith(".jpg")
                            || string.endsWith(".jpeg")
                    )
                    .collect(Collectors.toSet());
        }
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
     * convenience function to write a latex string to file - assumes calling function has checked
     * that the format of the Justification is LATEX, thus is sending a "Latex" string
     * @param latexText String of the contents of 'main.tex'
     * @param proposalCode Long proposal id
     * @param which String "type" of Justification
     * @throws IOException from FileWriter
     */
    private void writeLatexToFile(String latexText, Long proposalCode, String which)
            throws IOException {

        //use rest end-point path for the storage location
        String filePath = documentStoreRoot
                + "/proposals/"
                + proposalCode
                + "/justifications/"
                + which
                + "/" + texFileName;

        //To consider: BufferedWriter for performance?

        //either create the file and write to it, or overwrite the existing file
        try (FileWriter fw = new FileWriter(filePath)) {
            fw.write(latexText);
        }
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
                "-cd", //change directory into source file
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
     * Function to get the file identified by the parameters
     * @param proposalCode the proposal id (determined by rest end-point path)
     * @param which 'scientific' or 'technical' (determined by rest end-point path)
     * @param filename the name of the file, could include part of the path
     * @return the File object given the parameters above
     */
    private File getFile(Long proposalCode, String which, String filename) {
        return new File(
                documentStoreRoot
                        + "/proposals/"
                        + proposalCode
                        + "/justifications/"
                        + which,
                filename
        );
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
    private void checkFileUpload(FileUpload fileUpload)
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
            //.bib, .png, or .jpg (.jpeg) only; notice .bib may have 'application/octet-stream' type
            case "application/octet-stream":
            case "application/x-bibtex":
            case "image/jpeg":
            case "image/png":
                if (!extension.equals("bib") && !extension.equals("jpeg") &&
                        !extension.equals("jpg") && !extension.equals("png"))
                    throw new WebApplicationException("Invalid file extension");
                break;
            default:
                throw new WebApplicationException(
                        String.format("content-type: %s is not supported", contentType));
        }

        //NOTICE: content-type and file extension can be manipulated so that they misrepresent the
        // actual contents of the file. However, on Linux and Linux-like systems uploaded files saved
        // in this way have read-write permissions for the 'user' only (the "application" is the user here),
        // i.e. no executable permissions, and no permissions for 'groups' or 'other users'.
    }

}

