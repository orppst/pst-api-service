package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 16/03/2022 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.prop.*;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestQuery;
import org.orph2020.pst.common.json.ObjectIdentifier;

import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

/*
   For use cases see:
         https://gitlab.com/opticon-radionet-pilot/proposal-submission-tool/requirements/-/blob/main/UseCases.adoc
 */
//TODO - should really ensure that submitted proposals are not editable even via the direct {proposalCode} route
@Path("proposals")
@Tag(name = "proposal-tool-proposals")
@Produces(MediaType.APPLICATION_JSON)
public class ProposalResource extends ObjectResourceBase {
    private final Logger logger;


    public ProposalResource(Logger logger) {
        this.logger = logger;
    }

    private static final String proposalsRoot = "{proposalCode}";

    private static final String investigatorsRoot = proposalsRoot+"/investigators";
    private static final String supportingDocumentsRoot = proposalsRoot+"/supportingDocuments";


    @GET
    @Operation(summary = "Get all ObservingProposals identifiers, optionally get the ObservingProposal identifier for the named proposal")
    public List<ObjectIdentifier> getProposals(@RestQuery String title) {
        if (title == null) {
            return super.getObjects("SELECT o._id,o.title FROM ObservingProposal o  WHERE o.submitted = false or o.submitted = null ORDER BY o.title");
        } else {
            return super.getObjects("SELECT o._id,o.title FROM ObservingProposal o WHERE  (o.submitted = false or o.submitted = null) and o.title like '"+title+"' ORDER BY o.title");
        }
    }


    @GET
    @Operation(summary = "get the Proposal specified by the 'proposalCode'")
    @APIResponse(
            responseCode = "200",
            description = "get a single Proposal specified by the code"
    )
    @Path(proposalsRoot)
    public ObservingProposal getObservingProposal(@PathParam("proposalCode") Long proposalCode)
            throws WebApplicationException
    {
        return super.findObject(ObservingProposal.class, proposalCode);
    }

    @POST
    @Operation(summary = "create a new Proposal in the database")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response createObservingProposal(ObservingProposal op)
            throws WebApplicationException
    {
        return super.persistObject(op);
    }

    @DELETE
    @Path(proposalsRoot)
    @Operation(summary = "remove the ObservingProposal specified by the 'proposalCode'")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response deleteObservingProposal(@PathParam("proposalCode") long code)
            throws WebApplicationException
    {
        return super.removeObject(ObservingProposal.class, code);
    }


    //********************** TITLE ***************************
    @PUT
    @Operation(summary = "change the title of an ObservingProposal")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    @Path(proposalsRoot+"/title")
    public Response replaceTitle(
            @PathParam("proposalCode") long proposalCode,
            String replacementTitle)
            throws WebApplicationException
    {
        ObservingProposal proposal = findObject(ObservingProposal.class, proposalCode);

        proposal.setTitle(replacementTitle);

        return responseWrapper(proposal, 201);
    }

    //********************** SUMMARY ***************************
    @PUT
    @Operation(summary = "replace the summary of an ObservingProposal")
    @Path(proposalsRoot+"/summary")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceSummary(@PathParam("proposalCode") long proposalCode, String replacementSummary)
            throws WebApplicationException
    {
        ObservingProposal proposal = findObject(ObservingProposal.class, proposalCode);

        proposal.setSummary(replacementSummary);

        return responseWrapper(proposal, 201);
    }

    //********************** KIND ***************************
    @PUT
    @Operation(summary = "change the 'kind' of the ObservingProposal specified, one-of: STANDARD, TOO, SURVEY")
    @Path(proposalsRoot+"/kind")
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

        return responseWrapper(proposal, 201);
    }

    //********************** JUSTIFICATIONS ***************************

    @GET
    @Path(proposalsRoot+"/justifications/{which}")
    @Operation(summary = "get the technical or scientific justification associated with the ObservingProposal specified by 'proposalCode'")
    public Justification getJustification(@PathParam("proposalCode") Long proposalCode,
                                          @PathParam("which") String which)
        throws WebApplicationException
    {
        ObservingProposal observingProposal = super.findObject(ObservingProposal.class, proposalCode);

        switch (which) {
            case "technical":
            {
                return observingProposal.getTechnicalJustification();
            }

            case "scientific":
            {
                return observingProposal.getScientificJustification();
            }

            default:
            {
                throw new WebApplicationException(
                        String.format("Justifications are either 'technical' or 'scientific', I got %s", which),
                        400
                );
            }
        }
    }


    @PUT
    @Operation( summary = "update a technical or scientific Justification in the ObservingProposal specified by the 'proposalCode'")
    @Path(proposalsRoot+"/justifications/{which}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn={WebApplicationException.class})
    public Response updateJustification(
            @PathParam("proposalCode") long proposalCode,
            @PathParam("which") String which,
            Justification incoming )
            throws WebApplicationException
    {
        ObservingProposal proposal = findObject(ObservingProposal.class, proposalCode);

        switch (which)
        {
            case "technical":
            {
                proposal.setTechnicalJustification(incoming);
                break;
            }

            case "scientific":
            {
                proposal.setScientificJustification(incoming);
                break;
            }

            default:
            {
                throw new WebApplicationException(
                        String.format("Justifications are either 'technical' or 'scientific', I got %s", which),
                        418);
            }
        }

        return super.mergeObject(proposal);
    }


    //********************** INVESTIGATORS ***************************

    private Investigator findInvestigator(List<Investigator> investigators, Long id, Long proposalCode) {
        return investigators
                .stream().filter(o -> id.equals(o.getId())).findAny().orElseThrow(()->
                    new WebApplicationException(
                            String.format(NON_ASSOCIATE_ID, "Investigator", id, "ObservingProposal", proposalCode),
                            422
                    )
                );
    }

    @GET
    @Path(investigatorsRoot)
    @Operation(summary = "get the list of ObjectIdentifiers for the Investigators associated with the given ObservingProposal, optionally provide a name as a query to get that particular Investigator's identifier")
    public List<ObjectIdentifier> getInvestigators(@PathParam("proposalCode") Long proposalCode,
                                                   @RestQuery String fullName)
        throws WebApplicationException
    {
        List<Investigator> investigators = super.findObject(ObservingProposal.class, proposalCode)
                .getInvestigators();

        List<ObjectIdentifier> response = new ArrayList<>();
        if (fullName == null) {

            for (Investigator i : investigators) {
                response.add(new ObjectIdentifier(i.getId(), i.getInvestigator().getFullName()));
            }

        } else {

            //search the list of Investigators for the queried personName
            Investigator investigator = investigators
                    .stream().filter(o -> fullName.equals(o.getInvestigator()
                            .getFullName())).findAny()
                    .orElseThrow(() -> new WebApplicationException(
                            String.format(NON_ASSOCIATE_NAME, "Investigator",
                                    fullName, "ObservingProposal", proposalCode), 404
                    ));

            //return value is a list of ObjectIdentifiers with one element
            response.add(new ObjectIdentifier(investigator.getId(), investigator.getInvestigator().getFullName()));
        }
        return response;
    }

    @GET
    @Path(investigatorsRoot+"/{id}")
    @Operation(summary = "get the Investigator specified by the 'id' associated with the given ObservingProposal")
    public Investigator getInvestigator(@PathParam("proposalCode") Long proposalCode, @PathParam("id") Long id)
            throws WebApplicationException
    {
        return findInvestigator(
                super.findObject(ObservingProposal.class, proposalCode).getInvestigators(), id, proposalCode
        );
    }


    @PUT
    @Operation(summary = "add an Investigator, using an existing Person, to the ObservationProposal specified")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    @Path(investigatorsRoot)
    public Response addPersonAsInvestigator(@PathParam("proposalCode") long proposalCode,
                                            Investigator investigator)
            throws WebApplicationException
    {
        if (investigator.getInvestigator().getId() == 0) {
            throw new WebApplicationException(
                    "Please create a new person at 'proposals/people' before trying to add them as an Investigator",
                    400
            );
        }
        ObservingProposal proposal = findObject(ObservingProposal.class, proposalCode);
        proposal.addInvestigators(investigator);

        return super.mergeObject(proposal); //merge as we have a "new" Investigator to persist
    }

    @DELETE
    @Path(investigatorsRoot+"/{id}")
    @Operation(summary = "remove the Investigator specified by 'id' from the ObservingProposal identified by 'proposalCode'")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response removeInvestigator(@PathParam("proposalCode") Long proposalCode, @PathParam("id") Long id)
        throws WebApplicationException
    {
        ObservingProposal observingProposal = super.findObject(ObservingProposal.class, proposalCode);

        Investigator investigator = findInvestigator(observingProposal.getInvestigators(), id, proposalCode);

        observingProposal.removeInvestigators(investigator);

        return responseWrapper(observingProposal, 201);
    }


    @PUT
    @Path(investigatorsRoot+"/{id}/kind")
    @Operation(summary = "change the 'kind' ('PI' or 'COI') of the Investigator specified by the 'id'")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response changeInvestigatorKind(@PathParam("proposalCode") Long proposalCode, @PathParam("id") Long id,
                                            InvestigatorKind replacementKind)
        throws WebApplicationException
    {
        ObservingProposal observingProposal = super.findObject(ObservingProposal.class, proposalCode);

        findInvestigator(observingProposal.getInvestigators(), id, proposalCode).setType(replacementKind);

        return super.responseWrapper(observingProposal, 201);
    }

    @PUT
    @Path(investigatorsRoot+"/{id}/forPhD")
    @Operation(summary = "change the 'forPhD' status of the Investigator specified by the 'id'")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response changeInvestigatorForPhD(@PathParam("proposalCode") Long proposalCode, @PathParam("id") Long id,
                                            Boolean replacementForPhD)
            throws WebApplicationException
    {
        ObservingProposal observingProposal = super.findObject(ObservingProposal.class, proposalCode);

        findInvestigator(observingProposal.getInvestigators(), id, proposalCode).setForPhD(replacementForPhD);

        return super.responseWrapper(observingProposal, 201);
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

        proposal.addRelatedProposals(new RelatedProposal(relatedProposal));

        return responseWrapper(proposal, 201);
    }

    //********************** SUPPORTING DOCUMENTS ***************************

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
    @Path(supportingDocumentsRoot)
    @Operation(summary = "get the list of ObjectIdentifiers for the SupportingDocuments associated with the given ObservingProposal, optionally provide a title as a query to get that particular SupportingDocument's identifier")
    public List<ObjectIdentifier> getSupportingDocuments(@PathParam("proposalCode") Long proposalCode,
                                                   @RestQuery String title)
            throws WebApplicationException
    {
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
    @Path(supportingDocumentsRoot+"/{id}")
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
    @Path(supportingDocumentsRoot)
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response addNewSupportingDocument(@PathParam("proposalCode") Long proposalCode,
                                          SupportingDocument supportingDocument)
            throws WebApplicationException
    {
        ObservingProposal proposal = findObject(ObservingProposal.class, proposalCode);

        proposal.addSupportingDocuments(supportingDocument);

        return super.mergeObject(proposal);
    }

    @DELETE
    @Path(supportingDocumentsRoot+"/{id}")
    @Operation(summary = "remove the SupportingDocument specified by 'id' from the given ObservingProposal")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response removeSupportingDocument(@PathParam("proposalCode") Long proposalCode,
                                             @PathParam("id") Long id)
        throws WebApplicationException
    {
        ObservingProposal observingProposal = super.findObject(ObservingProposal.class, proposalCode);
        SupportingDocument supportingDocument =
                findSupportingDocument(observingProposal.getSupportingDocuments(), id, proposalCode);
        observingProposal.removeSupportingDocuments(supportingDocument);
        return responseWrapper(observingProposal, 201);
    }

    @PUT
    @Path(supportingDocumentsRoot+"/{id}/title")
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
    @Path(supportingDocumentsRoot+"/{id}/location")
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

    //********************** OBSERVATIONS ***************************
    @PUT
    @Operation(summary="add an observation to the ObservingProposal specified")
    @Path("{proposalCode}/observations")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response addObservation(@PathParam("proposalCode") Long proposalCode,
                                   Long observationId)
            throws WebApplicationException
    {
        ObservingProposal proposal = findObject(ObservingProposal.class, proposalCode);

        Observation observation = findObject(Observation.class, observationId);

        for (Observation o : proposal.getObservations()) {
            if (o.getId().equals(observationId)) {
                throw new WebApplicationException(
                        String.format("Observation with id: %d already added to ObservingProposal %s",
                                observationId, proposalCode), 418);
            }
        }

        proposal.addObservations(observation);

        return responseWrapper(proposal, 201);

    }

}
