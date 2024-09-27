package org.orph2020.pst.apiimpl.rest;


import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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
import java.util.Objects;

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

    //convenience function to get the specific Justification
    // - note that here the return value can be null
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

    //convenience function to write a latex string to file - assume calling function has checked
    //that the format of the Justification is LATEX, thus is sending a "Latex" string
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

    //required to make form-upload input work
    @Schema(type = SchemaType.STRING, format = "binary")
    public static class UploadItemSchema {}

    //non-transactional, no modification to the database occurs
    @POST
    @Path("{which}/fileUpload")
    @Operation(summary = "upload files for latex Justifications, only *.bib, *.jpg, and *.png supported")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadLatexFiles(@PathParam("proposalCode") Long proposalCode,
                                     @PathParam("which") String which,
                                     @RestForm("document") @Schema(implementation = UploadItemSchema.class)
                                         FileUpload fileUpload)
        throws WebApplicationException
    {
        if (!which.equals("technical") && !which.equals("scientific")) {
            throw new WebApplicationException(
                    String.format("Justifications are either 'scientific' or 'technical'; I got %s", which)
            );
        }

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

        String filename = fileUpload.fileName();

        File destination = getFile(proposalCode, which, filename);

        //check filename is unique per Justification
        if (destination.exists()) {
            throw new WebApplicationException(
                    String.format("File %s already exists: Please use unique names for uploads",
                            filename));
        }

        //save the uploaded file to the assigned location
        if (!fileUpload.uploadedFile().toFile().renameTo(destination)) {
            throw new WebApplicationException("Unable to save uploaded file");
        }

        return Response.ok(String.format("File %s saved", filename)).build();
    }

    //non-transactional, no modification to the database occurs
    @DELETE
    @Path("{which}/fileUpload")
    @Operation(summary = "delete the given file from the Justification document store")
    public Response removeLaTexFile(@PathParam("proposalCode") Long proposalCode,
                                    @PathParam("which") String which,
                                    String filename)
    throws WebApplicationException
    {
        if (!which.equals("technical") && !which.equals("scientific")) {
            throw new WebApplicationException(
                    String.format("Justifications are either 'scientific' or 'technical'; I got %s", which)
            );
        }

        File toDelete = getFile(proposalCode, which, filename);

        if (!toDelete.exists()) {
            throw new WebApplicationException(String.format("Nonexistent file: %s", filename));
        }

        if (!toDelete.delete()) {
            throw new WebApplicationException(String.format("Unable to delete file: %s", filename));
        }

        return Response.ok(String.format("File %s deleted", filename)).build();
    }

    private ProcessBuilder getLatexmkProcessBuilder(String filePath, String which) {
        ProcessBuilder processBuilder = new ProcessBuilder(
                "latexmk",
                "-cd", //change directory into source file
                "-pdf", //we want PDF output
                "-interaction=nonstopmode", //i.e. non-interactive
                "-output-directory=out", //relative to source directory due to '-cd' option
                "-jobname=" + which + "-justification", //output base name
                filePath
        );

        //tie stderr to stdout for convenience extracting 'latexmk' errors
        return processBuilder.redirectErrorStream(true);
    }

    @GET
    @Path("{which}/pdf")
    @Operation(summary = "create and return a PDF of the LaTex Justification from supplied files")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response createAndDownloadPDFLaTex(@PathParam("proposalCode") Long proposalCode,
                                              @PathParam("which") String which,
                                              @RestQuery Boolean warningsAsErrors
    )
        throws WebApplicationException
    {
        if (!which.equals("technical") && !which.equals("scientific")) {
            throw new WebApplicationException(
                    String.format("Justifications are either 'scientific' or 'technical'; I got %s",
                            which)
            );
        }

        File mainTex = getFile(proposalCode, which, texFileName);

        if (!mainTex.exists()) {
            throw new WebApplicationException(String.format("%s file not found", texFileName));
        }

        ProcessBuilder processBuilder = getLatexmkProcessBuilder(
                mainTex.getAbsolutePath(), which);

        try {
            Process process = processBuilder.start();

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                //Todo: try to build list of errors from <output>.log to send back to client
                throw new WebApplicationException("Latex error(s): blah");
            }

        } catch (IOException | InterruptedException e) {
            throw new WebApplicationException(e.getMessage());
        }

        if (warningsAsErrors) {
            //Todo: compile list of warnings, if there are any return back to client in the message of an exception
            //if no warnings here continue
            throw new WebApplicationException("Latex warnings treated as errors: blah");
        }

        //fetch the output PDF of the Justification
        File output = getFile(proposalCode, which, "out/" + which + "-justification.pdf");

        //this shouldn't happen at this point but check anyway
        if (!output.exists()) {
            throw new WebApplicationException("output PDF not found");
        }

        //this makes the file downloadable
        return Response.ok(output)
                .header("Content-Disposition", "attachment;filename=" + output.getName())
                .build();

    }
}

