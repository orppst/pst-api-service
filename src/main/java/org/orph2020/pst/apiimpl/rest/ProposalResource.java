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


   private ObservingProposal findProposal(long code)
      throws WebApplicationException
   {
      ObservingProposal op = em.find(ObservingProposal.class, code);

      if (op == null) {
         String ERR_PROPOSAL_NOT_FOUND = "ObservingProposal with code: %s not found";
         throw new WebApplicationException(
                 String.format(ERR_PROPOSAL_NOT_FOUND, code), 404);
      }

      return op;
   }

   private Response persistObservingProposal(ObservingProposal op)
      throws WebApplicationException
   {
      try {
         em.persist(op);
      } catch (EntityExistsException e) {
         throw new WebApplicationException(e.getMessage(), 422);
      }
      return super.responseWrapper(op, 201);
   }

   private Response removeObservingProposal(long code)
      throws WebApplicationException
   {
      ObservingProposal op = findProposal(code);
      em.remove(op);
      return Response.ok().status(204).build();
   }

   @GET
   @Operation(summary = "Get all the ObservingProposals from the database")
   public List<ObjectIdentifier> getProposals() {
      List<ObjectIdentifier> result = new ArrayList<>();
      String queryStr = "SELECT o._id,o.title FROM ObservingProposal o ORDER BY o.title";
      Query query = em.createQuery(queryStr);
      List<Object[]> results = query.getResultList();
      for (Object[] r : results)
      {
         result.add(new ObjectIdentifier((String) r[0], (String) r[1]));
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
   public ObservingProposal getObservingProposal(@PathParam("proposalCode") Long proposalCode)
           throws WebApplicationException
   {
      return findProposal(proposalCode);
   }

   @POST
   @Operation(summary = "create a new ObservingProposal in the database")
   @Consumes(MediaType.APPLICATION_JSON)
   @Transactional(rollbackOn = {WebApplicationException.class})
   public Response createObservingProposal(ObservingProposal op)
      throws WebApplicationException
   {
      return persistObservingProposal(op);
   }

   @DELETE
   @Path("{proposalCode}")
   @Operation(summary = "remove the ObservingProposal specified by the 'proposalCode'")
   @Transactional(rollbackOn = {WebApplicationException.class})
   public Response deleteObservingProposal(@PathParam("proposalCode") long code)
           throws WebApplicationException
   {
      return removeObservingProposal(code);
   }


   //title
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
      ObservingProposal proposal = findProposal(proposalCode);

      proposal.setTitle(replacementTitle);

      return responseWrapper(proposal, 201);
   }

   //summary
   @PUT
   @Operation(summary = "replace the summary of an ObservingProposal")
   @Path("{proposalCode}/summary")
   @Consumes(MediaType.TEXT_PLAIN)
   @Transactional(rollbackOn = {WebApplicationException.class})
   public Response replaceSummary(@PathParam("proposalCode") long proposalCode, String replacementSummary)
   throws WebApplicationException
   {
      ObservingProposal proposal = findProposal(proposalCode);

      proposal.setSummary(replacementSummary);

      return responseWrapper(proposal, 201);
   }

   //kind
   @PUT
   @Operation(summary = "change the 'kind' of the ObservingProposal specified, one-of: STANDARD, TOO, SURVEY")
   @Path("{proposalCode}/kind")
   @Consumes(MediaType.TEXT_PLAIN)
   @Transactional(rollbackOn = {WebApplicationException.class})
   public Response changeKind(@PathParam("proposalCode") long proposalCode, String kind)
      throws WebApplicationException
   {
      ObservingProposal proposal = findProposal(proposalCode);

      try{
         proposal.setKind(ProposalKind.fromValue(kind));
      } catch (IllegalArgumentException e) {
         throw new WebApplicationException(e.getMessage(), 422);
      }

      return responseWrapper(proposal, 201);
   }

   //Justifications
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
      ObservingProposal proposal = findProposal(proposalCode);

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
   @Operation(summary = "add an Investigator to the ObservationProposal specified")
   @Consumes(MediaType.APPLICATION_JSON)
   @Transactional(rollbackOn = {WebApplicationException.class})
   @Path("{proposalCode}/investigators")
   public Response addPersonAsInvestigator(@PathParam("proposalCode") long proposalCode,
                                           PersonInvestigator personInvestigator)
           throws WebApplicationException
   {
      Person person = findObject(Person.class, personInvestigator.personId);

      ObservingProposal proposal = findProposal(proposalCode);

      try {
         Investigator investigator = new Investigator(
                 InvestigatorKind.fromValue(personInvestigator.investigatorKind),
                 personInvestigator.forPhD, person) ;

         proposal.addInvestigators(investigator);
      } catch (IllegalArgumentException e) {
         throw new WebApplicationException(e, 422);
      }

      return responseWrapper(proposal, 201);
   }

   //relatedProposals
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

      ObservingProposal proposal = findProposal(proposalCode);

      ObservingProposal relatedProposal = findProposal(relatedProposalCode);

      proposal.addRelatedProposals(new RelatedProposal(relatedProposal));

      return responseWrapper(proposal, 201);
   }

   //supporting documents
   @PUT
   @Operation(summary = "add a SupportingDocument to the ObservingProposal specified")
   @Path("{proposalCode}/supportingDocuments")
   @Consumes(MediaType.TEXT_PLAIN)
   @Transactional(rollbackOn = {WebApplicationException.class})
   public Response addSupportingDocument(@PathParam("proposalCode") Long proposalCode,
                                      Long supportingDocumentId)
           throws WebApplicationException
   {
      ObservingProposal proposal = findProposal(proposalCode);

      SupportingDocument supportingDocument = findObject(SupportingDocument.class, supportingDocumentId);

      proposal.addSupportingDocuments(supportingDocument);

      return responseWrapper(proposal, 201);
   }

   //observations
   @PUT
   @Operation(summary="add an observation to the ObservingProposal specified")
   @Path("{proposalCode}/observations")
   @Consumes(MediaType.TEXT_PLAIN)
   @Transactional(rollbackOn = {WebApplicationException.class})
   public Response addObservation(@PathParam("proposalCode") Long proposalCode,
                                  Long observationId)
      throws WebApplicationException
   {
      ObservingProposal proposal = findProposal(proposalCode);

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
