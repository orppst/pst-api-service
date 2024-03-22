package org.orph2020.pst.apiimpl.rest;


import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestQuery;
import org.orph2020.pst.common.json.ObjectIdentifier;

import java.util.List;

@Path("proposalCycles/{cycleCode}/submittedProposals")
@Tag(name="submitted-proposals")
@Produces(MediaType.APPLICATION_JSON)
public class SubmittedProposalResource extends ObjectResourceBase{

    @GET
    @Operation(summary = "get the identifiers for the SubmittedProposals in the ProposalCycle")
    public List<ObjectIdentifier> getSubmittedProposals(@PathParam("cycleCode") Long cycleCode)
    {
        String select = "select s._id,p.title ";
        String from = "from ProposalCycle c ";
        String innerJoins = "inner join c.submittedProposals s inner join s.proposal p ";
        String where = "where c._id=" + cycleCode + " ";
        String orderBy = "order by p.title";

        return getObjectIdentifiers(select + from + innerJoins + where + orderBy);
    }



}
