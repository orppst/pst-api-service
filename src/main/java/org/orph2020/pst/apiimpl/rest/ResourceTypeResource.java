package org.orph2020.pst.apiimpl.rest;

import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.management.ResourceType;

import java.util.List;

@Path("resourceTypes")
@Tag(name = "resource-types")
@Produces(MediaType.APPLICATION_JSON)
public class ResourceTypeResource extends ObjectResourceBase{

    @GET
    @Operation(summary = "get all the ResourceTypes that have been defined in the App")
    public List<ResourceType> getAllResourceTypes() {
        TypedQuery<ResourceType> query = em.createQuery("select r from ResourceType r", ResourceType.class);
        return query.getResultList();
    }

    @POST
    @Operation(summary = "add a new ResourceType to the App")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public ResourceType addNewResourceType(ResourceType resourceType) {
        return persistObject(resourceType);
    }

    @DELETE
    @Path("{resourceTypeId}")
    @Operation(summary = "remove the ResourceType given by the 'resourceTypeId'")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response removeResourceType(@PathParam("resourceTypeId") Long resourceTypeId) {
        return removeObject(ResourceTypeResource.class, resourceTypeId);
    }

}
