package org.orph2020.pst.apiimpl.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.prop.ProposalKind;
import org.orph2020.pst.common.json.SubmittedProposalSynopsis;

import java.util.ArrayList;
import java.util.List;

@Path("proposalsSubmitted")
@Tag(name="proposals-submitted")

public class ProposalsSubmitted extends ObjectResourceBase {

    @GET
    @Operation(summary = "Get a list of synopsis for proposals submitted by the authenticated user")
    public List<SubmittedProposalSynopsis> getProposalsSubmitted()
    {
        SubmittedProposalSynopsis synopsis = new SubmittedProposalSynopsis(
              1, "Sample", "Example summary", ProposalKind.STANDARD, 0, 1, "Nothing yet"
        );

        List<SubmittedProposalSynopsis> listOfSubmitted = new ArrayList<SubmittedProposalSynopsis>();
        listOfSubmitted.add(synopsis);

        return listOfSubmitted;

    }

}
