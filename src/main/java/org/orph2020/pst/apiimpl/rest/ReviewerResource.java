package org.orph2020.pst.apiimpl.rest;

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
public class ReviewerResource extends ObjectResourceBase{

    @GET
    @Operation(summary = "Get a list of Reviewer identities")
    public List<ObjectIdentifier> getReviewers()
        throws WebApplicationException
    {
        return getObjectIdentifiers("select r._id,p.name from Reviewer r inner join r.person p");
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
