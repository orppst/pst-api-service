package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 16/03/2022 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.prop.*;
import org.jboss.logging.Logger;
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
    @Operation(summary = "Get all the ObservingProposals from the database")
    public List<ObjectIdentifier> getProposals() {
        return super.getObjects("SELECT o._id,o.title FROM ObservingProposal o ORDER BY o.title");
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
        ObservingProposal minimumProposal = new ObservingProposal()
                .withTitle(op.getTitle())
                .withKind(op.getKind())
                .withSummary(op.getSummary());

        Long scientificId = op.getScientificJustification().getId();
        if (scientificId == 0) {
            minimumProposal.setScientificJustification(op.getScientificJustification());
        }

        Long technicalId = op.getTechnicalJustification().getId();
        if (technicalId == 0) {
            minimumProposal.setTechnicalJustification(op.getTechnicalJustification());
        }

        //*************** -- WORK AROUND START-- ************************
        //ObservingProposal get<LIST> methods return an unmodifiableList which ultimately
        //throws a NullPointerException if the corresponding LIST has yet to be initialised
        //
        List<Investigator> investigators = new ArrayList<>();
        try {
            investigators = op.getInvestigators();
        } catch (NullPointerException e) {
            //okay list is empty
        }

        //work-around
        List<Observation> observations = new ArrayList<>();
        try {
            observations = op.getObservations();
        } catch (NullPointerException e) {
            //okay list is empty
        }

        //work-around
        List<RelatedProposal> relatedProposals = new ArrayList<>();
        try {
            relatedProposals = op.getRelatedProposals();
        } catch (NullPointerException e) {
            //okay list is empty
        }

        //work-around
        List<SupportingDocument> supportingDocuments = new ArrayList<>();
        try {
            supportingDocuments = op.getSupportingDocuments();
        } catch (NullPointerException e) {
            //okay list is empty
        }
        //*************** -- WORK AROUND END -- ************************

        for (Investigator i : investigators) {
            if (i.getId() == 0) {
                Long personId = i.getInvestigator().getId();
                if (personId == 0) {
                    minimumProposal.addInvestigators(i);
                }
            }
        }

        for (RelatedProposal r : relatedProposals) {
            if (r.getId() == 0) {
                throw new WebApplicationException(
                        "RelatedProposal must exist before being attached to a new ObservingProposal", 400);
            }
        }

        for (SupportingDocument s: supportingDocuments) {
            if (s.getId() == 0) {
                minimumProposal.addSupportingDocuments(s);
            }
        }

        for (Observation o : observations) {
            if (o.getId() == 0) {
                //TODO: add code to deal with sub-objects of an Observation that may exist
                minimumProposal.addObservations(o);
            }
        }


        try {
            em.persist(minimumProposal);
        } catch (EntityExistsException e) {
            throw new WebApplicationException(e.getMessage(), 400);
        }

        //add existing stuff to the proposal now it is persisted

        for (Investigator i : investigators) {
            Long investigatorId = i.getId();
            if (investigatorId == 0) {
                Long personId = i.getInvestigator().getId();
                if (personId > 0) {
                    Person person = super.findObject(Person.class, personId);
                    Investigator investigator = new Investigator()
                            .withForPhD(i.getForPhD())
                            .withType(i.getType());
                    investigator.setInvestigator(person);
                    minimumProposal.addInvestigators(investigator);
                }
            } else {
                Investigator investigator =
                        super.findObject(Investigator.class, investigatorId);
                minimumProposal.addInvestigators(investigator);
            }
        }


        for (RelatedProposal r : relatedProposals) {
            RelatedProposal relatedProposal =
                    findObject(RelatedProposal.class, r.getId());
            minimumProposal.addRelatedProposals(relatedProposal);
        }


        for (SupportingDocument s: supportingDocuments) {
            Long supportingId = s.getId();
            if (supportingId > 0) {
                SupportingDocument supportingDocument =
                        super.findObject(SupportingDocument.class, supportingId);
                minimumProposal.addSupportingDocuments(supportingDocument);
            }
        }

        for (Observation o : observations) {
            Long observationId = o.getId();
            if (observationId > 0) {
                Observation observation =
                        super.findObject(Observation.class, observationId);
                minimumProposal.addObservations(observation);
            } //TODO: else clause where the Observation is new but uses existing sub-objects
        }

        return responseWrapper(minimumProposal, 201);
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
    @PUT
    @Operation( summary = "replace a technical or scientific Justification in the ObservingProposal specified")
    @Path("{proposalCode}/justifications/{which}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn={WebApplicationException.class})
    public Response replaceJustification(
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

        return responseWrapper(proposal, 201);
    }


    //********************** INVESTIGATORS ***************************


    @PUT
    @Operation(summary = "add an Investigator to the ObservationProposal specified")
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

        return super.mergeObject(proposal);
    }

    //********************** RELATED PROPOSALS ***************************
    @PUT
    @Operation(summary = "add another ObservingProposal to the list of RelatedProposals of the ObservingProposal specified")
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
