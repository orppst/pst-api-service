package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 16/03/2022 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.hibernate.Criteria;
import org.ivoa.dm.proposal.prop.*;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.List;

/*
   For use cases see:
         https://gitlab.com/opticon-radionet-pilot/proposal-submission-tool/requirements/-/blob/main/UseCases.adoc
 */


@Path("/api/proposal-tool")
@ApplicationScoped
@Tag(name = "proposal-tool")
@Produces(MediaType.APPLICATION_JSON)
public class ProposalResource {
   private final Logger logger;

   /*
   @PersistenceUnit
   EntityManagerFactory emf // em = emf.createEntityManager(); <persistence stuff>; em.close(); - per call

   --OR--

      @Inject
   EntityManager em;
    */

   @PersistenceContext
   EntityManager em;  // exists for the application lifetime no need to close


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

   //--------------------------------------------------------------------------------
   // Error Mapper
   //--------------------------------------------------------------------------------
   @Provider
   public static class ErrorMapper implements ExceptionMapper<Exception> {

      private static final Logger LOGGER = Logger.getLogger(ProposalResource.class.getName());
      @Inject
      ObjectMapper objectMapper;

      @Override
      public Response toResponse(Exception e) {
         LOGGER.error("Failed to handle request", e);

         int code = 500;
         if (e instanceof WebApplicationException) {
            code = ((WebApplicationException) e).getResponse().getStatus();
         }

         ObjectNode exceptionJson = objectMapper.createObjectNode();
         exceptionJson.put("exceptionType", e.getClass().getName());
         exceptionJson.put("statusCode", code);

         if (e.getMessage() != null) {
            exceptionJson.put("error", e.getMessage());
         }

         return Response.status(code).entity(exceptionJson).build();
      }
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
   @Path("/users/{userName}/proposals/{proposalCode}/justifications/{which}")
   @Consumes(MediaType.APPLICATION_JSON)
   @Transactional(rollbackOn={WebApplicationException.class})
   public Response replaceJustification(@PathParam("userName") String userName,
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

      Justification incoming = null;
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
   @Path("/users/{userName}/proposals/{proposalCode}/title")
   public Response replaceTitle(
           @PathParam("userName") String userName,
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
   @Path("/users/{user}/proposals/{proposalCode}/investigators")
   public Response addPersonAsInvestigator(@PathParam("proposalCode") String code, String jsonPersonInvestigator)
           throws WebApplicationException
   {
      PersonInvestigator personInvestigator = null;
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


   //post required as we send 'userId' in the body of the request
   @POST
   @Operation(summary = "get all ObservationProposals associated with the user")
   @APIResponse(
           responseCode = "200",
           description = "get all ObservationProposals associated with the user"
   )
   @Consumes(MediaType.APPLICATION_JSON)
   @Path("/users/{user}/proposals")
   public  List<ObservingProposal> getUserProposals(@PathParam("user") String user, String userId)
   {
      //FIXME: query to find all observing proposals associated with the given 'userId'
      //currently just finds all observing proposals in the database
      String queryStr = "SELECT o FROM ObservingProposal o ORDER BY o.code";
      TypedQuery<ObservingProposal> query = em.createQuery(queryStr, ObservingProposal.class);
      //query.setParameter("userId", Integer.valueOf(userId));

      // This can be legitimately empty if the user has no proposals
      return query.getResultList();
   }

   @GET
   @Operation(summary = "get the specified ObservationProposal")
   @APIResponse(
           responseCode = "200",
           description = "get a single ObservationProposal specified by the code"
   )
   @Path("/users/{user}/proposals/{proposalCode}")
   public ObservingProposal getObservingProposal(@PathParam("proposalCode") String proposalCode)
           throws WebApplicationException
   {
      ObservingProposal op = em.find(ObservingProposal.class, proposalCode);

      if (op == null)
      {
         throw new WebApplicationException("ObservingProposal: %s does not exist", 404);
      }

      return op;
   }

   //--------------------------------------------------------------------------------
   // Admin API - root path '/api/proposal/admin/{adminID}/
   //--------------------------------------------------------------------------------

   @POST
   @Operation(summary = "create a new Person in the database")
   @APIResponse(
           responseCode = "201",
           description = "create a new Person in the database"
   )
   @Consumes(MediaType.APPLICATION_JSON)
   @Transactional(rollbackOn = {WebApplicationException.class})
   @Path("/admin/{adminId}/create/person")
   public Response createPerson(@PathParam("adminId") String adminId, String jsonPerson)
           throws WebApplicationException
   {
      //throws if JSON string not valid or cannot build object from the string
      Person person = null;
      try {
         person = mapper.readValue(jsonPerson, Person.class);
      } catch (JsonProcessingException e) {
         throw new WebApplicationException("Invalid JSON input", 422);
      }

      try {
         em.persist(person);
      } catch (EntityExistsException e) {
         throw new WebApplicationException(e, 400);
      }

      return Response.ok().entity("Person created successfully").build();
   }


}
