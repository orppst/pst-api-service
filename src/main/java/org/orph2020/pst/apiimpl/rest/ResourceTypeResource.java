package org.orph2020.pst.apiimpl.rest;

import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.management.ResourceType;
import org.orph2020.pst.common.json.ObjectIdentifier;

import java.util.List;

@Path("resourceTypes")
@Tag(name = "resource-types")
@Produces(MediaType.APPLICATION_JSON)
public class ResourceTypeResource extends ObjectResourceBase{

    @GET
    @Operation(summary = "get all the ResourceTypes that have been defined in the App")
    public List<ObjectIdentifier> getAllResourceTypes() {
        return getObjectIdentifiers("select r._id,r.name from ResourceType r");
    }

    @GET
    @Path("{resourceTypeId}")
    @Operation(summary = "get the specified resource type")
    public ResourceType getResourceType(@PathParam("resourceTypeId") Long resourceTypeId)
    {
        return findObject(ResourceType.class, resourceTypeId);
    }

    @POST
    @Operation(summary = "add a new ResourceType to the App, the name of the type must be unique")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public ResourceType addNewResourceType(ResourceType resourceType)
            throws WebApplicationException {
        //ensure ResourceType names are unique
        Query query = em.createQuery("select r from ResourceType r where r.name = :name");
        query.setParameter("name", resourceType.getName());
        if (query.getResultList().isEmpty()) {
            return persistObject(resourceType);
        } else {
            throw new WebApplicationException(Response.Status.CONFLICT);
        }
    }

    @DELETE
    @Path("{resourceTypeId}")
    @Operation(summary = "remove the ResourceType given by the 'resourceTypeId'")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response removeResourceType(@PathParam("resourceTypeId") Long resourceTypeId) {
        return removeObject(ResourceTypeResource.class, resourceTypeId);
    }

}
