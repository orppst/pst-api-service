package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 16/03/2022 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.prop.EmerlinExample;
import org.ivoa.dm.proposal.prop.ProposalModel;
import org.jboss.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/api/proposal")
@Tag(name = "proposal")
@Produces(APPLICATION_JSON)
public class ProposalResource {
   private final Logger logger;


   public ProposalResource(Logger logger) {
      this.logger = logger;
   }

   @GET
   @Operation(summary = "Returns all the proposals in the database")
   @APIResponse(
         responseCode = "200",
         description = "Gets all proposals"
   )
   public ProposalModel getAllProposals() {
      ProposalModel proposalModel = new ProposalModel();
      proposalModel.addContent(new EmerlinExample().getProposal()); //FIXME this needs to really contact the propdm-sync-service via kafka
      return proposalModel ;

   }

}
