package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 16/03/2022 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.prop.EmerlinExample;
import org.ivoa.dm.proposal.prop.ObservingProposal;
import org.ivoa.dm.proposal.prop.ProposalModel;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

/*
 Semantics of (public) interface methods (these are all REST Endpoints):

      getUserProposals(user) - gets all proposals associated with the 'user'

      @Path(/{obs_code}/add/X)
      addX(obs_code, X_id) - adds the existing entity X with key X_id to the specified observing proposal. X_id
                              passed via the body of POST as a JSON String

      @Path(/{obs_code}/edit/X)
      editX(obs_code, String X_edit) - edit the X property of the specified observing proposal. X_edit passed via
                                       the body of a POST


      createEntity(String object_X) - creates a persistent entity X in the DB. object_X will be passed in
                                 the body of a 'POST' operation as a JSON String.


      createProposal(new_proposal) - creates a new proposal for the current user. The 'new_proposal' string needs
                                     to contain any required data e.g., investigator(s), the rest can be blank to
                                     be filled in at a later date.

      deleteProposal(obs_code) - delete the specified proposal (this will only delete the proposal not the entities
                                 to which it refers).

  Notes:
  1. An observation code should uniquely define an observing proposal and should be known to the user.
     That is, a user logs on to the system that then provides them with a list of observing proposals with which
     they are associated. Each has a unique "observing proposal code".

  2. The user can add or remove existing entities to an observing proposal with which they are associated
     (role dependent). Entities can consist of for example, CelestialTargets, People (Principal Investigator,
     Co-Investigator, ...), TechnicalGoals, and so forth. Basically, any item found in the ProposalModel.

  3. If there is no appropriate entity in the database then it can be created using a 'createEntity' method,
     subject to verification depending on the type of entity being created (role specific). It is envisaged that
     this will be used mostly for the creation of scientific and technical justification documents, and any other
     supporting documents.

  4. Deletion of database entities can only be performed by specific user/admin roles.

  5. Database entities can be updated using the 'editEntity'. For example, an Investigator may have changed
     organisations, or a CelestialTarget has modified attributes, or a justification document requires modification,
     or whatever.

  methods to be added: getAllX() - where 'X' refers to any relevant entity of the ProposalModel e.g. Targets,
  Spectra, TechnicalGoals and so on, though some of these may have to be restricted depending on the role of the
  user. These are needed so that the user may browse and use items already stored in the database should they suit
  requirements, or can be used as a template for a new entity with modified fields.
 */


@Path("/api/proposal")
@ApplicationScoped
@Tag(name = "proposal")
@Produces(MediaType.APPLICATION_JSON)
public class ProposalResource {
   private final Logger logger;

   @Inject
   EntityManager em;

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

   @Transactional
   public List<ObservingProposal> findObservingProposalsByQuery(String query) {
      return em.createQuery(query, ObservingProposal.class).getResultList();
   }

   //TODO: check if Transactional annotation is required
   @Transactional
   private ObservingProposal findObservingProposalById(String id) {
      TypedQuery<ObservingProposal> query = em.createNamedQuery("ObservingProposal.findById",
              ObservingProposal.class);
      query.setParameter("id", id);
      return query.getSingleResult();
   }

   //--------------------------------------------------------------------------------
   // read content from an ObservationProposal (GET Methods)
   //--------------------------------------------------------------------------------
   @GET
   @Operation(summary = "get all ObservationProposals associated with the user")
   @APIResponse(
           responseCode = "200",
           description = "get all ObservationProposals associated with the user"
   )
   @Path("/{user}/proposals")
   public List<ObservingProposal> getUserProposals(@PathParam("user") String user, String userId)
   {
      //FIXME: query needs to loop over the list of investigators in each ObservingProposal in the DB
      String queryStr = "SELECT o FROM ObservingProposal o WHERE o.investigators[0] = :userId";
      TypedQuery<ObservingProposal> query = em.createQuery(queryStr, ObservingProposal.class);
      query.setParameter("userId", userId);

      return query.getResultList();
   }

   @GET
   @Operation(summary = "get the specified ObservationProposal")
   @APIResponse(
           responseCode = "200",
           description = "get a single ObservationProposal specified by the code"
   )
   @Path("/{user}/proposals/{proposalCode}")
   public ObservingProposal getObservingProposal(@PathParam("proposalCode") String proposalCode) {
      //assume proposalCode has originated from the database i.e., the entity exists
      return em.find(ObservingProposal.class, proposalCode);
   }

   //--------------------------------------------------------------------------------
   // create content for an ObservationProposal (PUT Methods)
   //--------------------------------------------------------------------------------


   //--------------------------------------------------------------------------------
   // add content to an ObservationProposal (POST Methods)
   //--------------------------------------------------------------------------------
   @POST
   @Operation(summary = "add an Investigator to an ObservationProposal")
   @APIResponse(
           responseCode = "200",
           description = "add an Investigator to an ObservationProposal"
   )
   @Path("/{proposalCode}/investigators")
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.TEXT_PLAIN)
   public String addInvestigator(@PathParam("proposalCode") String code, String investigatorID)
   {
      String message = String.format("addInvestigator(%s, %s)", code, investigatorID);
      logger.info(message);

      return message;
   }

   @POST
   @Operation(summary = "add a RelatedProposal to an ObservationProposal")
   @APIResponse(
           responseCode = "200",
           description = "add an RelatedProposal to an ObservationProposal"
   )
   @Path("/{proposalCode}/relatedProposal")
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.TEXT_PLAIN)
   public String addRelatedProposal(@PathParam("proposalCode") String code, String relatedProposalId)
   {
      String message = String.format("addRelatedProposal(%s, %s)", code, relatedProposalId);
      logger.info(message);

      return message;
   }

   @POST
   @Operation(summary = "add an Investigator to an ObservationProposal")
   @APIResponse(
           responseCode = "200",
           description = "add an Investigator to an ObservationProposal"
   )
   @Path("/{proposalCode}/supportingDocument")
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.TEXT_PLAIN)
   public String addSupportingDocument(@PathParam("proposalCode") String code, String supportingDocumentId)
   {
      String message = String.format("addSupportingDocument(%s, %s)", code, supportingDocumentId);
      logger.info(message);

      return message;
   }

   @POST
   @Operation(summary = "add an Investigator to an ObservationProposal")
   @APIResponse(
           responseCode = "200",
           description = "add an Investigator to an ObservationProposal"
   )
   @Path("/{proposalCode}/observation")
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.TEXT_PLAIN)
   public String addObservation(@PathParam("proposalCode") String code, String observationId)
   {
      String message = String.format("addSupportingDocument(%s, %s)", code, observationId);
      logger.info(message);

      return message;
   }


   //--------------------------------------------------------------------------------
   // edit the content of an ObservationProposal
   //--------------------------------------------------------------------------------
   @POST
   @Operation(summary = "edit the title of an ObservationProposal")
   @APIResponse(
           responseCode = "200",
           description = "edit the title of an ObservationProposal"
   )
   @Path("/{proposalCode}/title")
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.TEXT_PLAIN)
   public String editTitle(@PathParam("proposalCode") String code, String newTitle)
   {
      String message = String.format("editTitle(%s, %s)", code, newTitle);
      logger.info(message);

      return message;
   }

   @POST
   @Operation(summary = "edit the summary of an ObservationProposal")
   @APIResponse(
           responseCode = "200",
           description = "edit the summary of an ObservationProposal"
   )
   @Path("/{proposalCode}/summary")
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.TEXT_PLAIN)
   public String editSummary(@PathParam("proposalCode") String code, String newSummary)
   {
      String message = String.format("editSummary(%s, %s)", code, newSummary);
      logger.info(message);

      return message;
   }

   @POST
   @Operation(summary = "edit the ProposalKind of an ObservationProposal")
   @APIResponse(
           responseCode = "200",
           description = "edit the ProposalKind of an ObservationProposal"
   )
   @Path("/{proposalCode}/proposalKind")
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.TEXT_PLAIN)
   public String editProposalKind(@PathParam("proposalCode") String code, String newProposalKind)
   {
      String message = String.format("editProposalKind(%s, %s)", code, newProposalKind);
      logger.info(message);

      return message;
   }

}
