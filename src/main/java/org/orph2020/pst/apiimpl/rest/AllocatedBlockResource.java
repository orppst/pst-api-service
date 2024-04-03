package org.orph2020.pst.apiimpl.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.orph2020.pst.common.json.ObjectIdentifier;

import java.util.List;

@Path("proposalCycles/{cycleCode}/allocatedProposals/{allocatedId}/allocatedBlock")
@Tag(name = "proposalCycles-allocated-proposal-allocated-block")
@Produces(MediaType.APPLICATION_JSON)
public class AllocatedBlockResource extends ObjectResourceBase{

    @GET
    @Operation(summary = "get the allocated resource blocks associated with the given allocated proposal")
    public List<ObjectIdentifier> getAllocatedResourceBlocks(
            @PathParam("cycleCode") Long cycleCode,
            @PathParam("allocatedId") Long allocatedId)
    {
        String select = "select b._id,b.resource.type.name,b.grade.name ";
        String from = "from ProposalCycle c ";
        String innerJoins = "inner join c.allocatedProposals a inner join a.allocation b ";
        String where = "where c._id=" + cycleCode + " and a._id=" + allocatedId + " ";
        String orderBy = "order by b.grade.name";

        return getObjectIdentifiers(select + from + innerJoins + where + orderBy);
    }
}
