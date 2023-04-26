package org.orph2020.pst.apiimpl.rest;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.prop.ObservingProposal;
import org.ivoa.dm.proposal.prop.SupportingDocument;
import org.jboss.resteasy.reactive.RestQuery;
import org.orph2020.pst.common.json.ObjectIdentifier;

import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Path("proposals/{proposalCode}/supportingDocuments")
@Tag(name = "proposals-supportingDocuments")
@Produces(MediaType.APPLICATION_JSON)
public class SupportingDocumentResource extends ObjectResourceBase {

    private SupportingDocument findSupportingDocument(List<SupportingDocument> supportingDocuments, Long id,
                                                      Long proposalCode)
            throws WebApplicationException
    {
        return supportingDocuments.stream().filter(o -> id.equals(o.getId())).findAny()
                .orElseThrow(() -> new WebApplicationException(
                        String.format(NON_ASSOCIATE_ID, "SupportingDocument", id, "ObservingProposal", proposalCode)
                ));
    }

    @GET
    @Operation(summary = "get the list of ObjectIdentifiers for the SupportingDocuments associated with the given ObservingProposal, optionally provide a title as a query to get that particular SupportingDocument's identifier")
    public List<ObjectIdentifier> getSupportingDocuments(@PathParam("proposalCode") Long proposalCode,
                                                         @RestQuery String title)
            throws WebApplicationException
    {
        //Consider writing an SQL/Hibernate query string for this search

        List<SupportingDocument> supportingDocuments = super.findObject(ObservingProposal.class, proposalCode)
                .getSupportingDocuments();

        List<ObjectIdentifier> response = new ArrayList<>();
        if (title == null) {

            for (SupportingDocument s : supportingDocuments) {
                response.add(new ObjectIdentifier(s.getId(), s.getTitle()));
            }

        } else {

            //search the list of SupportingDocuments for the queried title
            SupportingDocument supportingDocument = supportingDocuments
                    .stream().filter(o -> title.equals(o.getTitle())).findAny()
                    .orElseThrow(() -> new WebApplicationException(
                            String.format(NON_ASSOCIATE_NAME, "SupportingDocument", title, "ObservingProposal",
                                    proposalCode), 404
                    ));

            //return value is a list of ObjectIdentifiers with one element
            response.add(new ObjectIdentifier(supportingDocument.getId(), supportingDocument.getTitle()));
        }
        return response;
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "get the SupportingDocument specified by the 'id' for the given ObservingProposal")
    public SupportingDocument getSupportingDocument(@PathParam("proposalCode") Long proposalCode,
                                                    @PathParam("id") Long id)
            throws WebApplicationException
    {
        return findSupportingDocument(
                super.findObject(ObservingProposal.class, proposalCode).getSupportingDocuments(), id, proposalCode
        );
    }

    @POST
    @Operation(summary = "add a new SupportingDocument to the ObservingProposal specified")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response addNewSupportingDocument(@PathParam("proposalCode") Long proposalCode,
                                             SupportingDocument supportingDocument)
            throws WebApplicationException
    {
        ObservingProposal proposal = findObject(ObservingProposal.class, proposalCode);

        proposal.addToSupportingDocuments(supportingDocument);

        return super.mergeObject(proposal);
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "remove the SupportingDocument specified by 'id' from the given ObservingProposal")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response removeSupportingDocument(@PathParam("proposalCode") Long proposalCode,
                                             @PathParam("id") Long id)
            throws WebApplicationException
    {
        ObservingProposal observingProposal = super.findObject(ObservingProposal.class, proposalCode);
        SupportingDocument supportingDocument =
                findSupportingDocument(observingProposal.getSupportingDocuments(), id, proposalCode);
        observingProposal.removeFromSupportingDocuments(supportingDocument);
        return responseWrapper(observingProposal, 201);
    }

    @PUT
    @Path("/{id}/title")
    @Operation(summary = "replace the title of the SupportingDocument specified by the 'id'")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceSupportingDocumentTitle(@PathParam("proposalCode") Long proposalCode,
                                                   @PathParam("id") Long id,
                                                   String replacementTitle)
            throws WebApplicationException
    {
        ObservingProposal observingProposal = super.findObject(ObservingProposal.class, proposalCode);
        SupportingDocument supportingDocument =
                findSupportingDocument(observingProposal.getSupportingDocuments(), id, proposalCode);
        supportingDocument.setTitle(replacementTitle);
        return responseWrapper(observingProposal, 201);
    }

    @PUT
    @Path("/{id}/location")
    @Operation(summary = "replace the location of the SupportingDocument specified by the 'id'")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceSupportingDocumentLocation(@PathParam("proposalCode") Long proposalCode,
                                                      @PathParam("id") Long id,
                                                      String replacementLocation)
            throws WebApplicationException
    {
        ObservingProposal observingProposal = super.findObject(ObservingProposal.class, proposalCode);
        SupportingDocument supportingDocument =
                findSupportingDocument(observingProposal.getSupportingDocuments(), id, proposalCode);
        supportingDocument.setLocation(replacementLocation);
        return responseWrapper(observingProposal, 201);
    }
}
