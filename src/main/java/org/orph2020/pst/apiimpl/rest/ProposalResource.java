package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 16/03/2022 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.hibernate.validator.constraints.pl.REGON;
import org.ivoa.dm.proposal.prop.*;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestQuery;
import org.orph2020.pst.common.json.ObjectIdentifier;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.*;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/*
   For use cases see:
         https://gitlab.com/opticon-radionet-pilot/proposal-submission-tool/requirements/-/blob/main/UseCases.adoc
 */

@Path("proposals")
@Tag(name = "proposal-tool-proposals")
@Produces(MediaType.APPLICATION_JSON)
public class ProposalResource extends ObjectResourceBase {
    private final Logger logger;


    public ProposalResource(Logger logger) {
        this.logger = logger;
    }


    @GET
    @Operation(summary = "Get all ObservingProposals identifiers, optionally get the ObservingProposal identifier for the named proposal")
    public List<ObjectIdentifier> getProposals(@RestQuery String title) {
        if (title == null) {
            return super.getObjects("SELECT o._id,o.title FROM ObservingProposal o ORDER BY o.title");
        } else {
            return super.getObjects("SELECT o._id,o.title FROM ObservingProposal o WHERE o.title like '"+title+"' ORDER BY o.title");
        }
    }


    @GET
    @Operation(summary = "get the ObservationProposal specified by the 'proposalCode'")
    @APIResponse(
            responseCode = "200",
            description = "get a single ObservationProposal specified by the code"
    )
    @Path("{proposalCode}")
    public ObservingProposal getObservingProposal(@PathParam("proposalCode") Long proposalCode)
            throws WebApplicationException
    {
        return super.findObject(ObservingProposal.class, proposalCode);
    }

    @POST
    @Operation(summary = "create a new ObservingProposal in the database")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response createObservingProposal(ObservingProposal op)
            throws WebApplicationException
    {
        return super.persistObject(op);
    }

    @DELETE
    @Path("{proposalCode}")
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
    @Path("{proposalCode}/title")
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
    @Path("{proposalCode}/summary")
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
    @Path("{proposalCode}/kind")
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
    @Path("{proposalCode}/justifications/{which}")
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
    @Path("{proposalCode}/justifications/{which}")
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
        Investigator investigator = investigators
                .stream().filter(o -> id.equals(o.getId())).findAny().orElseThrow(()->
                    new WebApplicationException(
                            String.format(NON_ASSOCIATE, "Investigator", id, "ObservingProposal", proposalCode),
                            422
                    )
                );
        return investigator;
    }

    @GET
    @Path("{proposalCode}/investigators")
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
                            String.format("Investigator with name: %s not associated with ObservingProposal: %d",
                                    fullName, proposalCode), 422
                    ));

            //return value is a list of ObjectIdentifiers with one element
            response.add(new ObjectIdentifier(investigator.getId(), investigator.getInvestigator().getFullName()));
        }
        return response;
    }

    @GET
    @Path("{proposalCode}/investigators/{id}")
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
    @Path("{proposalCode}/investigators")
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
    @Path("{proposalCode}/investigators/{id}")
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
    @Path("{proposalCode}/investigators/{id}/kind")
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
    @Path("{proposalCode}/investigators/{id}/forPhD")
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
    @PUT
    @Operation(summary = "add a SupportingDocument to the ObservingProposal specified")
    @Path("{proposalCode}/supportingDocuments")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response addSupportingDocument(@PathParam("proposalCode") Long proposalCode,
                                          Long supportingDocumentId)
            throws WebApplicationException
    {
        ObservingProposal proposal = findObject(ObservingProposal.class, proposalCode);

        SupportingDocument supportingDocument = findObject(SupportingDocument.class, supportingDocumentId);

        proposal.addSupportingDocuments(supportingDocument);

        return responseWrapper(proposal, 201);
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
