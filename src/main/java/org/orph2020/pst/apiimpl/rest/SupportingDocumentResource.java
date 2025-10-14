package org.orph2020.pst.apiimpl.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.prop.ObservingProposal;
import org.ivoa.dm.proposal.prop.SupportingDocument;
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
import java.util.Collections;
import java.util.List;

/*
    Policy: make supporting document titles unique within a proposal - meaning we need to check for
    uniqueness when a supporting document is uploaded or when a user decides to change the title of
    an existing document.

    Uniqueness should be case-sensitive, and with leading and trailing white space trimmed
 */

@Path("proposals/{proposalCode}/supportingDocuments")
@Tag(name = "proposals-supportingDocuments")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("default-roles-orppst")
public class SupportingDocumentResource extends ObjectResourceBase {

    @Inject
    ProposalDocumentStore proposalDocumentStore;

    private String storePath(Long proposalCode) {
        return proposalCode.toString() + "/supportingDocuments";
    }

    private
    String sanitiseTitle(String input, List<SupportingDocument> supportingDocuments)
    {
        //we could add more restrictions if needed
        String result = input.trim();
        for (SupportingDocument s : supportingDocuments) {
            if (s.getTitle().equals(result)) {
                throw new WebApplicationException(
                        "'" + result + "'" +
                                " already exists, please provide a unique title for your supporting document. "
                                + "Notice that leading and trailing whitespaces trimmed.",
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
        if (fileUpload == null) {
            throw new WebApplicationException("No file uploaded", 400);
        }

        ObservingProposal proposal = findObject(ObservingProposal.class, proposalCode);

        List<SupportingDocument> supportingDocuments = proposal.getSupportingDocuments();

        String saveFileAs = storePath(proposalCode) + "/" + fileUpload.fileName();

        String extension = FilenameUtils.getExtension(fileUpload.fileName());

        String storeLocation = proposalDocumentStore.fetchFile(saveFileAs).getAbsolutePath();

        //check for existence of file with the same filename
        SupportingDocument supportingDocument = supportingDocuments.stream()
                .filter(s -> s.getLocation().equals(storeLocation))
                .findFirst()
                .orElse(null);

        if (supportingDocument == null) {
            //adding a new supporting document

            //Allow one '.bib' file at a time in the proposal store location
            if (extension.equals("bib")) {
                try {
                    if (!proposalDocumentStore.listFilesIn(
                            proposalDocumentStore.getSupportingDocumentsPath(proposalCode),
                            Collections.singletonList("bib")).isEmpty()) {
                        throw new WebApplicationException(
                                "Only one '.bib' allowed per proposal: either remove or replace the existing file"
                        );
                    }
                } catch (IOException e) {
                    throw new WebApplicationException(e);

                }
            }

            String _title = title == null ? fileUpload.fileName() : sanitiseTitle(title, supportingDocuments);

            SupportingDocument newSupportingDocument =
                    addNewChildObject(
                            proposal,
                            new SupportingDocument(_title, ""),
                            proposal::addToSupportingDocuments
                    );

            //save the uploaded file to the new destination
            if (!proposalDocumentStore.moveFile(fileUpload.uploadedFile().toFile(), saveFileAs)) {
                throw new WebApplicationException("Unable to save file " + fileUpload.fileName(), 400);
            }
            //else all good, set the location for the newSupportingDocument
            newSupportingDocument.setLocation(storeLocation);

            return newSupportingDocument;

        } else {
            //replacing an existing file i.e. overwrite the file

            if(!proposalDocumentStore
                    .moveFile(fileUpload.uploadedFile().toFile(), saveFileAs)) {
                throw new WebApplicationException("Unable to overwrite file " + fileUpload.fileName(), 400);
            }

            if (title != null && !title.equals(supportingDocument.getTitle())) {
                //user has supplied a new, alternate name for the document
                supportingDocument.setTitle(sanitiseTitle(title, supportingDocuments));
            }

            //location is the same

            return supportingDocument;
        }
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

        String documentFile = supportingDocument.getLocation();

        // need to get the File from the location BEFORE we remove the SupportingDocument object
        File fileToRemove = new File(documentFile);

        // remove the SupportingDocument
        Response response = deleteChildObject(observingProposal, supportingDocument,
                observingProposal::removeFromSupportingDocuments);

        // delete the file AFTER removing the entity in case of deletion failure and rollback
        if (!fileToRemove.delete())
        {
            throw new WebApplicationException("unable to delete file: " + fileToRemove.getName(), 400);
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

        String _title = sanitiseTitle(replacementTitle, proposal.getSupportingDocuments());

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

        if (!fileDownload.exists())
        {
            throw new WebApplicationException("Cannot find " + fileDownload.getName(), 400);
        }

        Response.ResponseBuilder response = Response.ok(fileDownload);
        response.header("Content-Disposition", "attachment;filename=" + fileDownload.getName());

        return response.build();
    }

}
