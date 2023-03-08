package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 16/03/2022 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.hibernate.Criteria;
import org.ivoa.dm.proposal.prop.*;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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

   @PersistenceContext
   EntityManager em;  // exists for the application lifetime no need to close

   --OR--
    */

   @Inject
   EntityManager em; // as above

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
   @Transactional(rollbackOn={ProposalToolException.class})
   public Response replaceJustification(@PathParam("userName") String userName,
                                        @PathParam("proposalCode") String proposalCode,
                                        @PathParam("which") String which,
                                        String jsonJustification)
   throws ProposalToolException
   {
      try
      {
         //em.getTransaction().begin();

         ObservingProposal proposal = em.find(ObservingProposal.class, proposalCode);

         if (proposal == null)
         {
            throw new Exception(
                    String.format("No proposal with code: %s exists in the database", proposalCode)
            );
         }

         Justification incoming = mapper.readValue(jsonJustification, Justification.class);

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
               throw new Exception(String.format("Justifications are either 'technical' or 'scientific', I got %s",
                       which));
            }
         }

         //em.getTransaction().commit();
      }
      catch (Exception e)
      {
         throw new ProposalToolException(e.getMessage());
      }

      return Response.ok().entity(
              String.format("Justification for ObservingProposal %s replaced successfully", proposalCode))
              .build();
   }

   @PUT
   @Operation(summary = "change the title of an ObservingProposal")
   @APIResponse(
           responseCode = "200",
           description = "change the title of an ObservingProposal"
   )
   @Consumes(MediaType.TEXT_PLAIN)
   @Transactional(rollbackOn = {ProposalToolException.class})
   @Path("/users/{userName}/proposals/{proposalCode}/title")
   public Response replaceTitle(
           @PathParam("userName") String userName,
           @PathParam("proposalCode") String proposalCode,
           String replacementTitle)
           throws ProposalToolException
   {
      try
      {
         //transaction begin

         ObservingProposal proposal = em.find(ObservingProposal.class, proposalCode);

         if (proposal == null)
         {
            throw new Exception(
                    String.format("No proposal with code: %s exists in the database", proposalCode)
            );
         }

         proposal.setTitle(replacementTitle);

         //transaction end
      }
      catch (Exception e)
      {
         throw new ProposalToolException(e.getMessage());
      }

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
   @Transactional(rollbackOn = {ProposalToolException.class})
   @Path("/users/{user}/proposals/{proposalCode}/investigators")
   public Response addPersonAsInvestigator(@PathParam("proposalCode") String code, String jsonPersonInvestigator)
           throws ProposalToolException
   {
      try
      {
         //transaction begin

         PersonInvestigator personInvestigator = mapper.readValue(jsonPersonInvestigator, PersonInvestigator.class);

         Person person = em.find(Person.class, personInvestigator.personId);

         if (person == null)
         {
            throw new Exception(String.format("Person %d not found", personInvestigator.personId));
         }

         ObservingProposal proposal = em.find(ObservingProposal.class, code);

         if (proposal == null)
         {
            throw new Exception(String.format("ObservingProposal %s not found", code));
         }

         //throws if 'investigatorKind' not one of "COI" or "PI"
         Investigator investigator = new Investigator(
                 InvestigatorKind.fromValue(personInvestigator.investigatorKind),
                 personInvestigator.forPhD, person) ;

         proposal.addInvestigators(investigator);

         //transaction end

         return Response.ok()
                 .entity(String.format("Person %s attached as Investigator to proposal %s successfully",
                         personInvestigator.personId, code))
                 .build();

      }
      catch (Exception e)
      {
         logger.info(e.getMessage());
         throw new ProposalToolException(e.getMessage()); //response code 400 BAD INPUT
      }
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
   {
      //assume proposalCode has originated from the database i.e., the entity exists
      return em.find(ObservingProposal.class, proposalCode);
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
   @Transactional(rollbackOn = {ProposalToolException.class})
   @Path("/admin/{adminId}/create/person")
   public Response createPerson(@PathParam("adminId") String adminId, String jsonPerson)
           throws ProposalToolException
   {
      try
      {
         //em.getTransaction().begin();

         //throws if JSON string not valid or cannot build object from the string
         Person person = mapper.readValue(jsonPerson, Person.class);


         //FIXME: we should check that we are not trying to persist an existing person

         em.persist(person);

         //em.getTransaction().commit();
      }
      catch (Exception e)
      {
         throw new ProposalToolException(e.getMessage()); //response code 400
      }

      return Response.ok().entity("Person created successfully").build();
   }


}
