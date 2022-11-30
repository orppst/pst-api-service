package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 16/03/2022 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

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


import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/api/proposal")
@Tag(name = "proposal")
@Produces(APPLICATION_JSON)
public class ProposalResource {
   private final Logger logger;


   public ProposalResource(Logger logger) {
      this.logger = logger;
   }

   @Channel("query-proposal")
   Emitter<String> queryProposalEmitter;

   @POST
   @Path("/query")
   @Produces(MediaType.TEXT_PLAIN)
   public String createQuery() {
      String q = "SELECT o FROM ObservingProposal o ORDER BY o.code";
      queryProposalEmitter.send(q);
      return q;
   }

   /*
      the return type could be something other than Multi
    */

   @Channel("proposals")
   Multi<String> proposals;

   @GET
   @Operation(summary = "Returns all the proposals in the database")
   @APIResponse(
           responseCode = "200",
           description = "Gets all proposals"
   )
   @Produces(MediaType.SERVER_SENT_EVENTS) // to trigger dynamic changes in html
   public Multi<String> getAllProposals() {
      return proposals.log();
   }

}
