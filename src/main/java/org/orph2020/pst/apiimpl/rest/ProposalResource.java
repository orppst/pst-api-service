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

   @Inject
   ObjectMapper mapper;

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

      if (op == null)
      {
         throw new WebApplicationException(
                 String.format("ObservingProposal: %s does not exist", proposalCode), 404);
      }
      return op;
   }

   //--------------------------------------------------------------------------------
   // Internal helpers
   //--------------------------------------------------------------------------------

   private static class PersonInvestigator {
     @JsonProperty("investigatorKind")
      public String investigatorKind; //should be "COI" or "PI" only

     @JsonProperty("forPhD")
      boolean forPhD;

     @JsonProperty("personId")
      Long personId; //must match an existing Person in the database
   }

   //--------------------------------------------------------------------------------
   // Principle Investigator API
   //--------------------------------------------------------------------------------

   //Import a proposal


   //Export a proposal


   //Submit a proposal


   //Create a proposal (from "scratch")


   //Edit a proposal
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

      if (proposal == null)
      {
         throw new WebApplicationException(
                 String.format("No proposal with code: %s exists in the database", proposalCode), 404
         );
      }

      Justification incoming;
      try {
         incoming = mapper.readValue(jsonJustification, Justification.class);
      } catch (JsonProcessingException e) {
         throw new WebApplicationException("Invalid JSON input", 422);
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
              String.format("%s Justification for ObservingProposal %s replaced successfully", which, proposalCode))
              .build();
   }

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
                 String.format("No proposal with code: %s exists in the database", proposalCode), 404
         );
      }

      proposal.setTitle(replacementTitle);

      return Response.ok().entity(
              String.format("Title for ObservingProposal %s replaced successfully", proposalCode))
              .build();
   }

   // Investigator objects
   @POST
   @Operation(summary = "add an Investigator to an ObservationProposal")
   @APIResponse(
           responseCode = "200",
           description = "add a Person as an Investigator to an ObservationProposal"
   )
   @Consumes(MediaType.APPLICATION_JSON)
   @Transactional(rollbackOn = {WebApplicationException.class})
   @Path("{proposalCode}/investigators")
   public Response addPersonAsInvestigator(@PathParam("proposalCode") String code, String jsonPersonInvestigator)
           throws WebApplicationException
   {
      PersonInvestigator personInvestigator;
      try {
         personInvestigator = mapper.readValue(jsonPersonInvestigator, PersonInvestigator.class);
      } catch (JsonProcessingException e) {
         throw new WebApplicationException("Invalid JSON input", 422);
      }

      Person person = em.find(Person.class, personInvestigator.personId);

      if (person == null)
      {
         throw new WebApplicationException(String.format("Person %d not found", personInvestigator.personId), 404);
      }

      ObservingProposal proposal = em.find(ObservingProposal.class, code);

      if (proposal == null)
      {
         throw new WebApplicationException(String.format("ObservingProposal %s not found", code), 404);
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
              .entity(String.format("Person %s attached as Investigator to proposal %s successfully",
                      personInvestigator.personId, code))
              .build();


   }



}
