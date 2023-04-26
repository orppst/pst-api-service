package org.orph2020.pst.apiimpl.rest;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.prop.Investigator;
import org.ivoa.dm.proposal.prop.InvestigatorKind;
import org.ivoa.dm.proposal.prop.ObservingProposal;
import org.jboss.resteasy.reactive.RestQuery;
import org.orph2020.pst.common.json.ObjectIdentifier;

import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Path("proposals/{proposalCode}/investigators")
@Tag(name = "proposals-investigators")
@Produces(MediaType.APPLICATION_JSON)
public class InvestigatorResource extends ObjectResourceBase {

    private Investigator findInvestigator(List<Investigator> investigators, Long id, Long proposalCode) {
        return investigators
                .stream().filter(o -> id.equals(o.getId())).findAny().orElseThrow(()->
                        new WebApplicationException(
                                String.format(NON_ASSOCIATE_ID, "Investigator", id, "ObservingProposal", proposalCode),
                                422
                        )
                );
    }

    @GET
    @Operation(summary = "get the list of ObjectIdentifiers for the Investigators associated with the given ObservingProposal, optionally provide a name as a query to get that particular Investigator's identifier")
    public List<ObjectIdentifier> getInvestigators(@PathParam("proposalCode") Long proposalCode,
                                                   @RestQuery String fullName)
            throws WebApplicationException
    {
        //Would this be better expressed as a query string?
        List<Investigator> investigators = super.findObject(ObservingProposal.class, proposalCode)
                .getInvestigators();

        List<ObjectIdentifier> response = new ArrayList<>();
        if (fullName == null) {

            for (Investigator i : investigators) {
                response.add(new ObjectIdentifier(i.getId(), i.getPerson().getFullName()));
            }

        } else {

            //search the list of Investigators for the queried personName
            Investigator investigator = investigators
                    .stream().filter(o -> fullName.equals(o.getPerson()
                            .getFullName())).findAny()
                    .orElseThrow(() -> new WebApplicationException(
                            String.format(NON_ASSOCIATE_NAME, "Investigator",
                                    fullName, "ObservingProposal", proposalCode), 404
                    ));

            //return value is a list of ObjectIdentifiers with one element
            response.add(new ObjectIdentifier(investigator.getId(), investigator.getPerson().getFullName()));
        }
        return response;
    }

    @GET
    @Path("/{investigatorId}")
    @Operation(summary = "get the Investigator specified by the 'id' associated with the given ObservingProposal")
    public Investigator getInvestigator(@PathParam("proposalCode") Long proposalCode, @PathParam("investigatorId") Long id)
            throws WebApplicationException
    {
        return findInvestigator(
                super.findObject(ObservingProposal.class, proposalCode).getInvestigators(), id, proposalCode
        );
    }


    @PUT
    @Operation(summary = "add an Investigator, using an existing Person, to the ObservationProposal specified")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response addPersonAsInvestigator(@PathParam("proposalCode") long proposalCode,
                                            Investigator investigator)
            throws WebApplicationException
    {
        if (investigator.getPerson().getId() == 0) {
            throw new WebApplicationException(
                    "Please create a new person at 'proposals/people' before trying to add them as an Investigator",
                    400
            );
        }
        ObservingProposal proposal = findObject(ObservingProposal.class, proposalCode);
        proposal.addToInvestigators(investigator);

        return super.mergeObject(proposal); //merge as we have a "new" Investigator to persist
    }

    @DELETE
    @Path("/{investigatorId}")
    @Operation(summary = "remove the Investigator specified by 'id' from the ObservingProposal identified by 'proposalCode'")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response removeInvestigator(@PathParam("proposalCode") Long proposalCode, @PathParam("investigatorId") Long id)
            throws WebApplicationException
    {
        ObservingProposal observingProposal = super.findObject(ObservingProposal.class, proposalCode);

        Investigator investigator = findInvestigator(observingProposal.getInvestigators(), id, proposalCode);

        observingProposal.removeFromInvestigators(investigator);

        return responseWrapper(observingProposal, 201);
    }


    @PUT
    @Path("/{investigatorId}/kind")
    @Operation(summary = "change the 'kind' ('PI' or 'COI') of the Investigator specified by the 'id'")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response changeInvestigatorKind(@PathParam("proposalCode") Long proposalCode,
                                           @PathParam("investigatorId") Long id,
                                           InvestigatorKind replacementKind)
            throws WebApplicationException
    {
        ObservingProposal observingProposal = super.findObject(ObservingProposal.class, proposalCode);

        findInvestigator(observingProposal.getInvestigators(), id, proposalCode).setType(replacementKind);

        return super.responseWrapper(observingProposal, 201);
    }

    @PUT
    @Path("/{investigatorId}/forPhD")
    @Operation(summary = "change the 'forPhD' status of the Investigator specified by the 'id'")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response changeInvestigatorForPhD(@PathParam("proposalCode") Long proposalCode,
                                             @PathParam("investigatorId") Long id,
                                             Boolean replacementForPhD)
            throws WebApplicationException
    {
        ObservingProposal observingProposal = super.findObject(ObservingProposal.class, proposalCode);

        findInvestigator(observingProposal.getInvestigators(), id, proposalCode).setForPhD(replacementForPhD);

        return super.responseWrapper(observingProposal, 201);
    }
}
