package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 16/03/2022 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.prop.*;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestQuery;
import org.orph2020.pst.common.json.ObjectIdentifier;

import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/*
   For use cases see:
         https://gitlab.com/opticon-radionet-pilot/proposal-submission-tool/requirements/-/blob/main/UseCases.adoc
 */
//TODO - should really ensure that submitted proposals are not editable even via the direct {proposalCode} route
@Path("proposals")
@Tag(name = "proposals")
@Produces(MediaType.APPLICATION_JSON)
public class ProposalResource extends ObjectResourceBase {
    private final Logger logger;

    public ProposalResource(Logger logger) {
        this.logger = logger;
    }

    private static final String proposalRoot = "{proposalCode}";


    @GET
    @Operation(summary = "Get all ObservingProposals identifiers, optionally get the ObservingProposal identifier for the named proposal")
    public List<ObjectIdentifier> getProposals(@RestQuery String title) {
        if (title == null) {
            return super.getObjects("SELECT o._id,o.title FROM ObservingProposal o  WHERE o.submitted = false or o.submitted = null ORDER BY o.title");
        } else {
            return super.getObjects("SELECT o._id,o.title FROM ObservingProposal o WHERE  (o.submitted = false or o.submitted = null) and o.title like '"+title+"' ORDER BY o.title");
        }
    }


    @GET
    @Operation(summary = "get the Proposal specified by the 'proposalCode'")
    @APIResponse(
            responseCode = "200",
            description = "get a single Proposal specified by the code"
    )
    @Path(proposalRoot)
    public ObservingProposal getObservingProposal(@PathParam("proposalCode") Long proposalCode)
            throws WebApplicationException
    {
        return super.findObject(ObservingProposal.class, proposalCode);
    }

    @POST
    @Operation(summary = "create a new Proposal in the database")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response createObservingProposal(ObservingProposal op)
            throws WebApplicationException
    {
        return super.persistObject(op);
    }

    @DELETE
    @Path(proposalRoot)
    @Operation(summary = "remove the ObservingProposal specified by the 'proposalCode'")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response deleteObservingProposal(@PathParam("proposalCode") long code)
            throws WebApplicationException
    {
        return super.removeObject(ObservingProposal.class, code);
    }


    //********************** TITLE ***************************
    @PUT
    @Operation(summary = "change the title of an ObservingProposal")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    @Path(proposalRoot +"/title")
    public Response replaceTitle(
            @PathParam("proposalCode") long proposalCode,
            String replacementTitle)
            throws WebApplicationException
    {
        ObservingProposal proposal = findObject(ObservingProposal.class, proposalCode);

        proposal.setTitle(replacementTitle);

        return responseWrapper(proposal, 201);
    }

    //********************** SUMMARY ***************************
    @PUT
    @Operation(summary = "replace the summary of an ObservingProposal")
    @Path(proposalRoot +"/summary")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response replaceSummary(@PathParam("proposalCode") long proposalCode, String replacementSummary)
            throws WebApplicationException
    {
        ObservingProposal proposal = findObject(ObservingProposal.class, proposalCode);

        proposal.setSummary(replacementSummary);

        return responseWrapper(proposal, 201);
    }

    //********************** KIND ***************************
    @PUT
    @Operation(summary = "change the 'kind' of the ObservingProposal specified, one-of: STANDARD, TOO, SURVEY")
    @Path(proposalRoot +"/kind")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response changeKind(@PathParam("proposalCode") long proposalCode, String kind)
            throws WebApplicationException
    {
        ObservingProposal proposal = findObject(ObservingProposal.class, proposalCode);

        try{
            proposal.setKind(ProposalKind.fromValue(kind));
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(e.getMessage(), 422);
        }

        return responseWrapper(proposal, 201);
    }

    //********************** JUSTIFICATIONS ***************************

    @GET
    @Path(proposalRoot +"/justifications/{which}")
    @Operation(summary = "get the technical or scientific justification associated with the ObservingProposal specified by 'proposalCode'")
    public Justification getJustification(@PathParam("proposalCode") Long proposalCode,
                                          @PathParam("which") String which)
        throws WebApplicationException
    {
        ObservingProposal observingProposal = super.findObject(ObservingProposal.class, proposalCode);

        switch (which) {
            case "technical":
            {
                return observingProposal.getTechnicalJustification();
            }

            case "scientific":
            {
                return observingProposal.getScientificJustification();
            }

            default:
            {
                throw new WebApplicationException(
                        String.format("Justifications are either 'technical' or 'scientific', I got %s", which),
                        400
                );
            }
        }
    }


    @PUT
    @Operation( summary = "update a technical or scientific Justification in the ObservingProposal specified by the 'proposalCode'")
    @Path(proposalRoot +"/justifications/{which}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn={WebApplicationException.class})
    public Response updateJustification(
            @PathParam("proposalCode") long proposalCode,
            @PathParam("which") String which,
            Justification incoming )
            throws WebApplicationException
    {
        ObservingProposal proposal = findObject(ObservingProposal.class, proposalCode);

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

        return super.mergeObject(proposal);
    }

    //********************** RELATED PROPOSALS ***************************
    @PUT
    @Operation(summary = "add a RelatedProposal to the ObservingProposal specified by the 'proposalCode'")
    @Path("{proposalCode}/relatedProposals")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response addRelatedProposal(@PathParam("proposalCode") Long proposalCode,
                                       Long relatedProposalCode)
            throws WebApplicationException
    {
        if (proposalCode.equals(relatedProposalCode)) {
            throw new WebApplicationException(
                    "ObservingProposal cannot refer to itself as a RelatedProposal", 418);
        }

        ObservingProposal proposal = findObject(ObservingProposal.class, proposalCode);

        ObservingProposal relatedProposal = findObject(ObservingProposal.class, relatedProposalCode);

        proposal.addToRelatedProposals(new RelatedProposal(relatedProposal));

        return responseWrapper(proposal, 201);
    }

    //Other fields of an ObservingProposal have been split out into their own source file
}
