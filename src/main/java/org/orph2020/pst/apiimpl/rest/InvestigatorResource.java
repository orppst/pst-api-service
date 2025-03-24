package org.orph2020.pst.apiimpl.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.prop.Investigator;
import org.ivoa.dm.proposal.prop.InvestigatorKind;
import org.ivoa.dm.proposal.prop.ObservingProposal;
import org.jboss.resteasy.reactive.RestQuery;
import org.orph2020.pst.common.json.ObjectIdentifier;

import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("proposals/{proposalCode}/investigators")
@Tag(name = "proposals-investigators")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@RolesAllowed("default-roles-orppst")
public class InvestigatorResource extends ObjectResourceBase {

    private Investigator findInvestigatorFromList(List<Investigator> investigators, long id) {
        return investigators
                .stream().filter(o -> Long.valueOf(id).equals(o.getId())).findAny().orElse(null);
    }

    private Investigator findInvestigatorByQuery(long proposalCode, long id) {
        TypedQuery<Investigator> q = em.createQuery(
                "Select i From ObservingProposal p join p.investigators i where p._id = :pid and  i._id = :iid",
                Investigator.class
        );
        q.setParameter("pid", proposalCode);
        q.setParameter("iid", id);
        return q.getSingleResult();
    }

    @GET
    @Operation(summary = "get the list of ObjectIdentifiers for the Investigators associated with the given ObservingProposal, optionally provide a name as a query to get that particular Investigator's identifier")
    public List<ObjectIdentifier> getInvestigators(@PathParam("proposalCode") Long proposalCode,
                                                   @RestQuery String fullName)
            throws WebApplicationException
    {
        if (fullName == null) {
            return getObjectIdentifiers(
                    "Select i._id,p.fullName From ObservingProposal o Inner join o.investigators i Inner join i.person p where o._id = "+proposalCode+" ORDER BY p.fullName"
            );
        } else {
            return getObjectIdentifiers(
                    "Select i._id,p.fullName From ObservingProposal o Inner join o.investigators i Inner join i.person p where o._id = "+proposalCode+" and p.fullName like '"+fullName+"' ORDER BY p.fullName"
            );
        }
    }

    @GET
    @Path("/asObjects")
    @Operation(summary = "get a list of Investigators for a given ObservingProposal, returns a list of investigator objects")
    public List<Investigator> getInvestigatorsAsObjects(@PathParam("proposalCode") Long proposalCode) {
        //List<Investigator> result = new ArrayList<>();
        TypedQuery<Investigator> q = em.createQuery(
                "Select i from ObservingProposal p join p.investigators i where p._id = :pid order by i._id",
                Investigator.class);
        q.setParameter("pid", proposalCode);
        return q.getResultList();
    }


    @GET
    @Path("/{investigatorId}")
    @Operation(summary = "get the Investigator specified by the 'id' associated with the given ObservingProposal")
    public Investigator getInvestigator(@PathParam("proposalCode") Long proposalCode,
                                        @PathParam("investigatorId") Long id)
            throws WebApplicationException
    {
        return findInvestigatorByQuery(proposalCode, id);
    }


    @POST
    @Operation(summary = "add a new Investigator, using an existing Person, to the ObservationProposal specified")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Investigator addPersonAsInvestigator(@PathParam("proposalCode") long proposalCode,
                                            Investigator investigator)
            throws WebApplicationException
    {
        ObservingProposal proposal = findObject(ObservingProposal.class, proposalCode);
        return addNewChildObject(proposal, investigator, proposal::addToInvestigators);
    }

    @DELETE
    @Path("/{investigatorId}")
    @Operation(summary = "remove the Investigator specified by 'id' from the ObservingProposal identified by 'proposalCode'")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response removeInvestigator(@PathParam("proposalCode") Long proposalCode,
                                       @PathParam("investigatorId") Long id)
            throws WebApplicationException
    {
        ObservingProposal observingProposal = findObject(ObservingProposal.class, proposalCode);

        Investigator investigator = findInvestigatorFromList(observingProposal.getInvestigators(), id);

        if (investigator == null) {
            throw new WebApplicationException(
                    String.format(NON_ASSOCIATE_ID, "Investigator", id, "ObservingProposal", proposalCode),
                    422
            );
        }

        observingProposal.removeFromInvestigators(investigator);

        return emptyResponse204();
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
        Investigator investigator = findInvestigatorByQuery(proposalCode, id);
        investigator.setType(replacementKind);
        return responseWrapper(investigator, 201);
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
        Investigator investigator = findInvestigatorByQuery(proposalCode, id);
        investigator.setForPhD(replacementForPhD);

        return responseWrapper(investigator, 201);
    }
}
