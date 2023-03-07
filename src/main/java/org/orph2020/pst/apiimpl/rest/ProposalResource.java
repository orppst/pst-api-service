package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 16/03/2022 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

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


@Path("/api/proposal")
@ApplicationScoped
@Tag(name = "proposal")
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
   // Persistence utilities
   //--------------------------------------------------------------------------------


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

      return Response.ok().entity(String.format("Justification replaced successfully")).build();
   }

   // Investigator objects
   @POST
   @Operation(summary = "add an Investigator to an ObservationProposal")
   @APIResponse(
           responseCode = "200",
           description = "add an Investigator to an ObservationProposal"
   )
   @Consumes(MediaType.APPLICATION_JSON)
   @Transactional(rollbackOn = {ProposalToolException.class})
   @Path("/users/{user}/proposals/{proposalCode}/investigators")
   public Response addInvestigator(@PathParam("proposalCode") String code, Long investigatorID)
           throws ProposalToolException
   {
      try
      {
         Person person = em.find(Person.class, investigatorID);

         if (person == null)
         {
            throw new Exception(String.format("Investigator %d not found", investigatorID));
         }

         // find the ObservingProposal by code
         ObservingProposal proposal = em.createNamedQuery("ObservingProposal.findById", ObservingProposal.class)
                 .setParameter("id", code)
                 .getSingleResult();

         Investigator investigator = new Investigator(InvestigatorKind.COI, false, person);

         // call ObservingProposal.add(Investigator) to attach said investigator to the proposal
         proposal.addInvestigators(investigator);

      }
      catch (Exception e) //response code 400 BAD INPUT
      {
         logger.info(e.getMessage());
         throw new ProposalToolException(e.getMessage());
      }

      return Response.ok()
              .entity(String.format("Investigator %s attached to proposal %s successfully", investigatorID, code))
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
   @Operation(summary = "create a new Investigator in the database")
   @APIResponse(
           responseCode = "201",
           description = "create a new Investigator in the database"
   )
   @Consumes(MediaType.APPLICATION_JSON)
   @Transactional(rollbackOn = {ProposalToolException.class})
   @Path("/admin/{adminId}/create/investigator")
   public Response createInvestigator(@PathParam("adminId") String adminId, String jsonInvestigator)
           throws ProposalToolException
   {
      try
      {
         //em.getTransaction().begin();

         //throws if JSON string not valid or cannot build object from the string
         Investigator investigator = mapper.readValue(jsonInvestigator, Investigator.class);

         //throws if 'investigator' already persisted
         em.persist(investigator);

         //em.getTransaction().commit();
      }
      catch (Exception e) //re-throw as a ProposalToolException (response code: 400); could be made more specific
      {
         throw new ProposalToolException(e.getMessage());
      }

      return Response.ok().entity("Investigator created successfully").build();
   }


}
