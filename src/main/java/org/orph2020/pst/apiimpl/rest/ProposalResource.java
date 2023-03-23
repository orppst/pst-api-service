package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 16/03/2022 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.prop.*;
import org.jboss.logging.Logger;
import org.orph2020.pst.common.json.ProposalIdentifier;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.*;
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

@Path("proposals")
@ApplicationScoped
@Tag(name = "proposal-tool")
@Produces(MediaType.APPLICATION_JSON)
public class ProposalResource extends ObjectResourceBase {
   private final Logger logger;

   private final String ERR_PROPOSAL_NOT_FOUND = "ObservingProposal with code: %s not found";


   public ProposalResource(Logger logger) {
      this.logger = logger;
   }

   @Transactional
   public void initDB() {
      logger.info("initializing Database");
      EmerlinExample ex = new EmerlinExample();
      em.persist(ex.getCycle());
      em.persist(ex.getProposal());
   }

   @GET
   @Operation(summary = "Get all the ObservingProposals from the database")
   public List<ProposalIdentifier> getProposals() {
      List<ProposalIdentifier> result = new ArrayList<>();
      String queryStr = "SELECT o.code,o.title FROM ObservingProposal o ORDER BY o.title";
      Query query = em.createQuery(queryStr);
      List<Object[]> results = query.getResultList();
      for (Object[] r : results)
      {
         result.add(new ProposalIdentifier((String) r[0], (String) r[1]));
      }

      return result;
   }


   @GET
   @Operation(summary = "get the specified ObservationProposal")
   @APIResponse(
           responseCode = "200",
           description = "get a single ObservationProposal specified by the code"
   )
   @Path("{proposalCode}")
   public ObservingProposal getObservingProposal(@PathParam("proposalCode") String proposalCode)
           throws WebApplicationException
   {
      ObservingProposal op = em.find(ObservingProposal.class, proposalCode);

      if (op == null) {
         throw new WebApplicationException(
                 String.format(ERR_PROPOSAL_NOT_FOUND, proposalCode), 404);
      }
      return op;
   }


   //title
   @PUT
   @Operation(summary = "change the title of an ObservingProposal")
   @APIResponse(
           responseCode = "200",
           description = "change the title of an ObservingProposal"
   )
   @Consumes(MediaType.TEXT_PLAIN)
   @Transactional(rollbackOn = {WebApplicationException.class})
   @Path("{proposalCode}/title")
   public Response replaceTitle(
           @PathParam("proposalCode") String proposalCode,
           String replacementTitle)
           throws WebApplicationException
   {
      ObservingProposal proposal = em.find(ObservingProposal.class, proposalCode);

      if (proposal == null)
      {
         throw new WebApplicationException(
                 String.format(ERR_PROPOSAL_NOT_FOUND, proposalCode), 404
         );
      }

      proposal.setTitle(replacementTitle);

      return Response.ok().entity(
                      String.format(OK_UPDATE, "Title"))
              .build();
   }

   //summary
   @PUT
   @Operation(summary = "replace the summary with the data in this request")
   @APIResponse(
           responseCode = "200"
   )
   @Path("{proposalCode}/summary")
   @Consumes(MediaType.TEXT_PLAIN)
   @Transactional(rollbackOn = {WebApplicationException.class})
   public Response replaceSummary(@PathParam("proposalCode") String proposalCode, String replacementSummary)
   throws WebApplicationException
   {
      ObservingProposal proposal = em.find(ObservingProposal.class, proposalCode);

      if (proposal == null) {
         throw new WebApplicationException(
                 String.format(ERR_PROPOSAL_NOT_FOUND, proposalCode), 404);
      }

      proposal.setSummary(replacementSummary);

      return Response.ok().entity(String.format(OK_UPDATE, "ObservingProposal.summary")).build();
   }

   //kind
   @PUT
   @Operation(summary = "change the 'kind' of ObservingProposal: STANDARD, TOO, SURVEY")
   @APIResponse(
           responseCode = "200"
   )
   @Path("{proposalCode}/kind")
   @Consumes(MediaType.TEXT_PLAIN)
   @Transactional(rollbackOn = {WebApplicationException.class})
   public Response changeKind(@PathParam("proposalCode") String proposalCode, String kind)
      throws WebApplicationException
   {
      ObservingProposal proposal = em.find(ObservingProposal.class, proposalCode);

      if (proposal == null) {
         throw new WebApplicationException(
                 String.format(ERR_PROPOSAL_NOT_FOUND, proposalCode), 404);
      }

      try{
         proposal.setKind(ProposalKind.fromValue(kind));
      } catch (IllegalArgumentException e) {
         throw new WebApplicationException(e, 422);
      }

      return Response.ok().entity(String.format(OK_UPDATE, "ObservingProposal.kind")).build();
   }

   //Justifications
   @PUT
   @Operation(summary = "replace a technical or scientific Justification with the data in this request")
   @APIResponse(
           responseCode = "200",
           description = "replace a technical or scientific Justification with the data in this request"
   )
   @Path("{proposalCode}/justifications/{which}")
   @Consumes(MediaType.APPLICATION_JSON)
   @Transactional(rollbackOn={WebApplicationException.class})
   public Response replaceJustification(
                                        @PathParam("proposalCode") String proposalCode,
                                        @PathParam("which") String which,
                                        String jsonJustification)
   throws WebApplicationException
   {
      ObservingProposal proposal = em.find(ObservingProposal.class, proposalCode);

      if (proposal == null) {
         throw new WebApplicationException(
                 String.format(ERR_PROPOSAL_NOT_FOUND, proposalCode), 404
         );
      }

      Justification incoming;
      try {
         incoming = mapper.readValue(jsonJustification, Justification.class);
      } catch (JsonProcessingException e) {
         throw new WebApplicationException(String.format(ERR_JSON_INPUT, which + "Justification"), 422);
      }

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

      return Response.ok().entity(
              String.format(OK_UPDATE,"ObservingProposal." + which + "Justification"))
              .build();
   }


   // Investigator objects
   private static class PersonInvestigator {
      @JsonProperty("investigatorKind")
      public String investigatorKind; //should be "COI" or "PI" only

      @JsonProperty("forPhD")
      boolean forPhD;

      @JsonProperty("personId")
      Long personId; //must match an existing Person in the database
   }

   @PUT
   @Operation(summary = "add an Investigator to an ObservationProposal")
   @APIResponse(
           responseCode = "200",
           description = "add a Person as an Investigator to an ObservationProposal"
   )
   @Consumes(MediaType.APPLICATION_JSON)
   @Transactional(rollbackOn = {WebApplicationException.class})
   @Path("{proposalCode}/investigators")
   public Response addPersonAsInvestigator(@PathParam("proposalCode") String code,
                                           String jsonPersonInvestigator)
           throws WebApplicationException
   {
      PersonInvestigator personInvestigator;
      try {
         personInvestigator = mapper.readValue(jsonPersonInvestigator, PersonInvestigator.class);
      } catch (JsonProcessingException e) {
         throw new WebApplicationException("Invalid JSON input", 422);
      }

      Person person = em.find(Person.class, personInvestigator.personId);

      if (person == null) {
         throw new WebApplicationException(
                 String.format(ERR_NOT_FOUND, "Person", personInvestigator.personId), 404);
      }

      ObservingProposal proposal = em.find(ObservingProposal.class, code);

      if (proposal == null) {
         throw new WebApplicationException(
                 String.format(ERR_PROPOSAL_NOT_FOUND, code), 404);
      }

      try {
         Investigator investigator = new Investigator(
                 InvestigatorKind.fromValue(personInvestigator.investigatorKind),
                 personInvestigator.forPhD, person) ;

         proposal.addInvestigators(investigator);
      } catch (IllegalArgumentException e) {
         throw new WebApplicationException(e, 422);
      }

      return Response.ok()
              .entity(String.format(OK_UPDATE, "ObservingProposal.investigators"))
              .build();
   }

   //relatedProposals
   @PUT
   @Operation(summary = "add an ObservingProposal to the list of RelatedProposals")
   @APIResponse(
           responseCode = "200"
   )
   @Path("{proposalCode}/relatedProposals")
   @Consumes(MediaType.TEXT_PLAIN)
   @Transactional(rollbackOn = {WebApplicationException.class})
   public Response addRelatedProposal(@PathParam("proposalCode") String proposalCode,
                                       String relatedProposalCode)
      throws WebApplicationException
   {
      if (proposalCode.equals(relatedProposalCode)) {
         throw new WebApplicationException(
                 "ObservingProposal cannot refer to itself as a RelatedProposal", 418);
      }

      ObservingProposal proposal = em.find(ObservingProposal.class, proposalCode);

      if (proposal == null) {
         throw new WebApplicationException(
                 String.format(ERR_PROPOSAL_NOT_FOUND, proposalCode), 404);
      }

      ObservingProposal relatedProposal = em.find(ObservingProposal.class, relatedProposalCode);

      if (relatedProposal  == null) {
         throw new WebApplicationException(
                 String.format(ERR_PROPOSAL_NOT_FOUND, relatedProposalCode), 404);
      }

      proposal.addRelatedProposals(new RelatedProposal(relatedProposal));

      return Response.ok().entity(String.format(OK_UPDATE, "ObservingProposal.relatedProposals")).build();
   }

   //supporting documents
   @PUT
   @Operation(summary = "add a SupportingDocument to an ObservingProposal")
   @APIResponse(
           responseCode = "200"
   )
   @Path("{proposalCode}/supportingDocuments")
   @Consumes(MediaType.TEXT_PLAIN)
   @Transactional(rollbackOn = {WebApplicationException.class})
   public Response addSupportingDocument(@PathParam("proposalCode") String proposalCode,
                                      Long supportingDocumentId)
           throws WebApplicationException
   {
      ObservingProposal proposal = em.find(ObservingProposal.class, proposalCode);

      if (proposal == null) {
         throw new WebApplicationException(
                 String.format(ERR_PROPOSAL_NOT_FOUND, proposalCode), 404
         );
      }

      SupportingDocument supportingDocument = em.find(SupportingDocument.class, supportingDocumentId);

      if (supportingDocument == null) {
         throw new WebApplicationException(
                 String.format(ERR_NOT_FOUND, "SupportingDocument", supportingDocumentId), 404
         );
      }

      proposal.addSupportingDocuments(supportingDocument);

      return Response.ok().entity(String.format(OK_UPDATE, "ObservingProposal.supportingDocuments")).build();
   }

   //observations
   @PUT
   @Operation(summary="add an observation to an ObservingProposal")
   @APIResponse(
           responseCode = "200"
   )
   @Path("{proposalCode}/observations")
   @Consumes(MediaType.TEXT_PLAIN)
   @Transactional(rollbackOn = {WebApplicationException.class})
   public Response addObservation(@PathParam("proposalCode") String proposalCode,
                                  Long observationId)
      throws WebApplicationException
   {
      ObservingProposal proposal = em.find(ObservingProposal.class, proposalCode);

      if (proposal == null) {
         throw new WebApplicationException(
                 String.format(ERR_PROPOSAL_NOT_FOUND, proposalCode), 404);
      }


      Observation observation = em.find(Observation.class, observationId);
      if (observation == null) {
         throw new WebApplicationException(String.format(ERR_NOT_FOUND, "Observation", observationId), 404);
      }

      for (Observation o : proposal.getObservations()) {
         if (o.getId().equals(observationId)) {
            throw new WebApplicationException(
                    String.format("Observation with id: %d already added to ObservingProposal %s",
                            observationId, proposalCode), 418);
         }
      }

      proposal.addObservations(observation);

      return Response.ok().entity(String.format(OK_UPDATE, "ObservingProposal.observations")).build();

   }

}
