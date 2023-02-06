package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 16/03/2022 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Multi;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.ivoa.dm.proposal.prop.EmerlinExample;
import org.ivoa.dm.proposal.prop.ProposalModel;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestStreamElementType;


import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/api/proposal")
@Tag(name = "proposal")
@Produces(APPLICATION_JSON)
public class ProposalResource {
   private final Logger logger;

   @Inject
   ObjectMapper mapper;

   public ProposalResource(Logger logger) {
      this.logger = logger;
   }


   //--------------------------------------------------------------------------------
   // Channel (Topic) Emitters
   //--------------------------------------------------------------------------------
   @Channel("query-observing-proposal")
   Emitter<String> OP_queryEmitter;

   @Channel("read-observing-proposal-by-code")
   Emitter<String> OP_readByCodeEmitter;

   @Channel("delete-observing-proposal-by-code")
   Emitter<String> OP_deleteByCodeEmitter;

   @Channel("update-observing-proposal")
   Emitter<String> OP_updateEmitter;

   @Channel("create-observing-proposal")
   Emitter<String> OP_createEmitter;

   //--------------------------------------------------------------------------------
   // API REQUESTS TO STORAGE SERVICE
   //--------------------------------------------------------------------------------
   @POST
   @Operation(summary = "query the persistence store")
   @APIResponse (
           responseCode = "200",
           description = "query the persistence store using a Hibernate query language string"
   )
   @Path("/query/{qlString}")
   @Produces(MediaType.TEXT_PLAIN)
   public String queryStore(@PathParam("qlString") String qlString)
   {
      //String q = "SELECT o FROM ObservingProposal o WHERE o.code = 'pr1'";
      logger.info(String.format("createQuery - %s", qlString));
      OP_queryEmitter.send(qlString); //writes the query string to the "query" Channel
      return qlString;
   }

   // Creates a new proposal in the database.
   // "new_proposal" is a String that must contain the sufficient data to create a proposal
   @POST
   @Operation(summary = "create a new proposal")
   @APIResponse (
           responseCode = "200",
           description = "create a proposal from the provided jsonString"
   )
   @Path("/create/{new_proposal}")
   @Produces(MediaType.TEXT_PLAIN)
   public void createProposal(@PathParam("new_proposal") String new_proposal)
   {
      //assume the "new_proposal" String is formatted as JSON
      logger.info(String.format("createProposal - %s", new_proposal));
      OP_createEmitter.send(new_proposal);
   }


   @POST
   @Operation(summary = "fetch a specific proposal")
   @APIResponse (
           responseCode = "200",
           description = "fetch a proposal from the persistence store using a code(id)"
   )
   @Path("/read/{code}")
   @Produces(MediaType.TEXT_PLAIN)
   public void readProposalByCode(@PathParam("code") String code)
   {
      logger.info(String.format("readProposalByCode(%s)", code));
      OP_readByCodeEmitter.send(code);
   }

   @POST
   @Operation(summary = "delete a specific proposal")
   @APIResponse (
           responseCode = "200",
           description = "delete a proposal from the persistence store using the code(id)"
   )
   @Path("/delete/{code}")
   @Produces(MediaType.TEXT_PLAIN)
   public void deleteObservingProposalByCode(@PathParam("code") String code)
   {
      logger.info(String.format("deleteObservingProposalByCode(%s)", code));
      OP_deleteByCodeEmitter.send(code);
   }

   @POST
   @Operation(summary = "update a proposal")
   @APIResponse(
           responseCode = "200",
           description = "update a proposal using the provided jsonString"
   )
   @Path("/update/{updated_observing_proposal}")
   @Produces(MediaType.TEXT_PLAIN)
   public void updateObservingProposal(@PathParam("updated_observing_proposal") String update)
   {
      logger.info(String.format("updateObservingProposal - %s", update));
      OP_updateEmitter.send(update);
   }

   //--------------------------------------------------------------------------------
   // STORAGE SERVICE RESPONSE HANDLERS
   //--------------------------------------------------------------------------------

   //Consumes records from the "proposals" channel - has multiple producers
   @Channel("proposals")
   Multi<String> proposals;

   @GET
   @Operation(summary = "Returns one or more proposals")
   @APIResponse(
           responseCode = "200",
           description = "Returns a jsonString of either a ProposalModel, a delete message or an error"
   )
   @Produces(MediaType.SERVER_SENT_EVENTS) // to trigger dynamic changes in html
   public Multi<String> getAllProposals() {
      return proposals.log();
   }


}
