package org.orph2020.pst.apiimpl.rest;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.prop.ObservingProposal;
import org.ivoa.dm.proposal.prop.SupportingDocument;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.orph2020.pst.common.json.ObjectIdentifier;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/*
    Policy: make supporting document titles unique within a proposal - meaning we need to check for
    uniqueness when a supporting document is uploaded or when a user decides to change the title of
    an existing document.

    Uniqueness should be case-sensitive, and with leading and trailing white space ignored
 */

@Path("proposals/{proposalCode}/supportingDocuments")
@Tag(name = "proposals-supportingDocuments")
@Produces(MediaType.APPLICATION_JSON)
public class SupportingDocumentResource extends ObjectResourceBase {

    private static final Logger logger =
        Logger.getLogger(SupportingDocumentResource.class.getName());

    //FIXME: need to confirm a path location on our server for this
    private static final String documentStoreRoot = "/tmp/documentStore/";

    private
    String sanitiseTitle(String input, ObservingProposal proposal)
    {
        //we could add more restrictions if needed
        String result = input.trim();
        for (SupportingDocument s : proposal.getSupportingDocuments()) {
            if (s.getTitle().equals(input)) {
                throw new WebApplicationException(
                        "'" + input + "'" +
                                " already exists, please provide a unique title for your supporting document",
                        400);
            }
        }
        return result;
    }

    @GET
    @Operation(summary = "get the list of ObjectIdentifiers for the SupportingDocuments associated with the given ObservingProposal, optionally provide a title as a query to get that particular SupportingDocument's identifier")
    public List<ObjectIdentifier> getSupportingDocuments(@PathParam("proposalCode") Long proposalCode,
                                                         @RestQuery String title)
            throws WebApplicationException
    {
        if (title == null) {
            return getObjectIdentifiers("SELECT s._id,s.title FROM ObservingProposal o Inner Join o.supportingDocuments s WHERE o._id = "+proposalCode+" ORDER BY s.title");
        } else {
            return getObjectIdentifiers("SELECT s._id,s.title FROM ObservingProposal o Inner Join o.supportingDocuments s WHERE o._id = "+proposalCode+" and s.title like '"+title+"' ORDER BY s.title");
        }
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "get the SupportingDocument specified by the 'id' for the given ObservingProposal")
    public SupportingDocument getSupportingDocument(@PathParam("proposalCode") Long proposalCode,
                                                    @PathParam("id") Long id)
            throws WebApplicationException
    {
        return findChildByQuery(ObservingProposal.class, SupportingDocument.class, "supportingDocuments",
                proposalCode, id);
    }

    //required to make form-upload input work
    @Schema(type = SchemaType.STRING, format = "binary")
    public static class UploadItemSchema {}

    @POST
    @Operation(summary = "upload a new SupportingDocument to the ObservingProposal specified")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public SupportingDocument uploadSupportingDocument(
            @PathParam("proposalCode") Long proposalCode,
            @RestForm("document") @Schema(implementation = UploadItemSchema.class)
            FileUpload fileUpload,
            @RestForm @PartType(MediaType.APPLICATION_JSON) String title)
            throws WebApplicationException
    {
        if(fileUpload == null) {
          throw new WebApplicationException("No file uploaded", 400);
        }

        ObservingProposal proposal = findObject(ObservingProposal.class, proposalCode);
        String _title = sanitiseTitle(title, proposal);

        //'result' is the managed SupportingDocument object instance after return from 'addNewChildObject'
        SupportingDocument result =
            addNewChildObject(proposal, new SupportingDocument(_title, ""),
                proposal::addToSupportingDocuments);

        File destination = createDestination(
            title, proposal, proposalCode, fileUpload.fileName(),
            result.getId());

        //move the uploaded file to the new destination
        if(!fileUpload.uploadedFile().toFile().renameTo(destination))
        {
            throw new WebApplicationException(
                "Unable to save file " + fileUpload.fileName(), 400);
        }
        //else all good, set the location for the result
        result.setLocation(destination.toString());

        return result;
    }

    /**
     * creates the destination location file as needed.
     *
     * @param title: the title of the document.
     * @param proposal: the proposal object.
     * @param proposalCode: the proposal code.
     * @param fileName: the filename for the destination.
     * @return The destination file.
     */
    private File createDestination(
            String title, ObservingProposal proposal, Long proposalCode,
            String fileName, long resultId) {
        //relocate to /tmp for testing only - on Mac upload random temporary location is in /var/folders which
        // is deleted on return from this request (quarkus configuration)

        String destinationStr = documentStoreRoot + proposalCode + "/supportingDocuments/" + resultId;

        File destinationPath = new File(destinationStr);

        if (destinationPath.exists())
        {
            logger.info("destination str = " + destinationStr);
            throw new WebApplicationException(destinationStr + " already exists", 400);
        }

        if (!destinationPath.mkdirs())
        {
            throw new WebApplicationException("Unable to create path " + destinationPath);
        }

        //create the file-location
        return new File(destinationStr, fileName);
    }

    /**
     * provides a supporting method to upload a supporting document from a zip
     * file.
     *
     * @param proposal: the proposal object.
     * @param docData: the raw byte[] data.
     * @param docTitle: the doc title.
     * @return the supporting document object.
     * @throws IOException when the writing fails.
     */
    public SupportingDocument uploadSupportingDocumentFromZip(
        ObservingProposal proposal, byte[] docData, String docTitle
    ) throws IOException {
        String _title = sanitiseTitle(docTitle, proposal);

        //'result' is the managed SupportingDocument object instance after return from 'addNewChildObject'
        SupportingDocument result =
            addNewChildObject(proposal, new SupportingDocument(_title, ""),
                proposal::addToSupportingDocuments);

        File destinationFile = this.createDestination(
            docTitle, proposal, proposal.getId(), docTitle, result.getId());

        Files.write(destinationFile.toPath(), docData);

        //else all good, set the location for the result
        result.setLocation(destinationFile.toString());
        return result;
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "replace the supporting document with a new file upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceSupportingDocument(@PathParam("proposalCode") Long proposalCode,
                                              @PathParam("id") Long id,
                                              @RestForm("document") @Schema(implementation = UploadItemSchema.class)
                                                  FileUpload fileUpload
                                              )
    {
        SupportingDocument supportingDocument =
                findChildByQuery(ObservingProposal.class, SupportingDocument.class,
                        "supportingDocuments", proposalCode, id);

        // first save the "new" upload file in the related directory of the store
        String destinationStr = documentStoreRoot + proposalCode + "/supportingDocuments/" + id ;

        File destination = new File(destinationStr, fileUpload.fileName());

        if(!fileUpload.uploadedFile().toFile().renameTo(destination))
        {
            throw new
                    WebApplicationException("Unable to save (new) file "
                    + fileUpload.fileName(), 400);
        }

        // second if the files names are different remove the "old" file from the store and
        // update the SupportingDocument location else, do nothing - file replaced and
        // locations the same
        File oldFile = new File(supportingDocument.getLocation());

        if (!oldFile.getName().equals(fileUpload.fileName()))
        {
            if (!oldFile.delete())
            {
                throw new WebApplicationException("Unable to delete (old) file " + oldFile.getName());
            }

            supportingDocument.setLocation(destination.getAbsolutePath());
        }

        return responseWrapper(supportingDocument, 201);
    }


    @DELETE
    @Path("/{id}")
    @Operation(summary = "remove the SupportingDocument specified by 'id' from the given ObservingProposal")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response removeSupportingDocument(@PathParam("proposalCode") Long proposalCode,
                                             @PathParam("id") Long id)
            throws WebApplicationException
    {
        //remove the associated document from the document store
        ObservingProposal observingProposal = findObject(ObservingProposal.class, proposalCode);
        SupportingDocument supportingDocument =
                findChildByQuery(ObservingProposal.class, SupportingDocument.class, "supportingDocuments",
                        proposalCode, id);

        File fileToRemove = new File(supportingDocument.getLocation());

        Response response = deleteChildObject(observingProposal, supportingDocument,
                observingProposal::removeFromSupportingDocuments);

        if (!fileToRemove.delete())
        {
            throw new WebApplicationException("unable to delete file: " + fileToRemove.getName(), 400);
        }

        String pathStr = fileToRemove.getAbsolutePath();
        String parentDirStr = pathStr.substring(0, pathStr.lastIndexOf('/'));

        File dirToRemove = new File(parentDirStr);

        if (!dirToRemove.delete())
        {
            throw new WebApplicationException("unable to delete directory: " + parentDirStr, 400);
        }

        return response;
    }

    @PUT
    @Path("/{id}/title")
    @Operation(summary = "replace the title of the SupportingDocument specified by the 'id'")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public SupportingDocument replaceSupportingDocumentTitle(@PathParam("proposalCode") Long proposalCode,
                                                   @PathParam("id") Long id,
                                                   String replacementTitle)
            throws WebApplicationException
    {
        ObservingProposal proposal = findObject(ObservingProposal.class, proposalCode);

        String _title = sanitiseTitle(replacementTitle, proposal);

        SupportingDocument supportingDocument = findChildByQuery(ObservingProposal.class,
                SupportingDocument.class, "supportingDocuments", proposalCode, id);

        supportingDocument.setTitle(_title);
        return supportingDocument;
    }

    //------Download file-----//
    @GET
    @Operation(summary = "download the document file associated with the SupportingDocument 'id'")
    @Path("{id}/get-file")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadSupportingDocument(@PathParam("proposalCode") Long proposalCode,
                                               @PathParam("id") Long id)
    {
        SupportingDocument supportingDocument = findChildByQuery(ObservingProposal.class,
                SupportingDocument.class, "supportingDocuments", proposalCode, id);

        File fileDownload = new File(supportingDocument.getLocation());

        if(!fileDownload.exists())
        {
            throw new WebApplicationException("Cannot find " + fileDownload.getName(), 400);
        }

        Response.ResponseBuilder response = Response.ok((Object) fileDownload);
        response.header("Content-Disposition", "attachment;filename=" + fileDownload.getName());

        return response.build();
    }

}
