package org.orph2020.pst.apiimpl.rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.management.Reviewer;
import org.orph2020.pst.common.json.ObjectIdentifier;

import java.util.List;

@Path("reviewers")
@Tag(name = "reviewers")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({"tac_admin", "tac_member"})
public class ReviewerResource extends ObjectResourceBase{

    @GET
    @Operation(summary = "Get a list of Reviewer identities")
    public List<ObjectIdentifier> getReviewers()
        throws WebApplicationException
    {
        // 3 argument ObjectIdentifier is Long, String, String
        String qlString = "select r._id,cast(r.person._id as string),r.person.fullName from Reviewer r";

        Query query = em.createQuery(qlString);
        return getObjectIdentifiersAlt(query);
    }

    @GET
    @Path("/{reviewerId}")
    @Operation(summary = "Get the Reviewer specified by 'reviewerId'")
    public Reviewer getReviewer(@PathParam("reviewerId") Long reviewerId)
        throws WebApplicationException
    {
        return findObject(Reviewer.class, reviewerId);
    }

    @POST
    @Operation(summary = "add a new Reviewer")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Reviewer addReviewer(Reviewer reviewer)
        throws WebApplicationException
    {
        return persistObject(reviewer);
    }

    @DELETE
    @Path("/{reviewerId}")
    @Operation(summary = "Remove the Reviewer specified by 'reviewerId'")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response removeReviewer(@PathParam("reviewerId") Long reviewerId)
        throws WebApplicationException
    {
        return removeObject(Reviewer.class, reviewerId);
    }

}
