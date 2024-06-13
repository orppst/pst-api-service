package org.orph2020.pst.apiimpl.rest;

import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.management.*;
import org.orph2020.pst.common.json.ObjectIdentifier;

import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

@Path("proposalCycles/{cycleCode}/availableResources")
@Tag(name = "proposalCycles-availableResources")
@Produces(MediaType.APPLICATION_JSON)
public class AvailableResourcesResource extends ObjectResourceBase {

    private Resource findAvailableResource(Long cycleCode, String resourceName)
            throws WebApplicationException
    {
        Query query = em.createQuery("select r from ProposalCycle c inner join c.availableResources.resources r where r.type.name = :resourceName and c.id = :cycleCode");
        query.setParameter("cycleCode", cycleCode);
        query.setParameter("resourceName", resourceName);
        if (query.getResultList().isEmpty()) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        } else {
            //we've ensured that resource type-names are unique i.e., only one element will exist
            return (Resource) query.getResultList().get(0);
        }
    }

    private Double GetAllocatedResourceAmount(Long cycleCode, String resourceName) {
        Query responseQuery = em.createQuery("select r from ProposalCycle c inner join c.allocatedProposals p inner join p.allocation a inner join a.resource r where r.type.name = :resourceName and c.id = :cycleCode");
        responseQuery.setParameter("cycleCode", cycleCode);
        responseQuery.setParameter("resourceName", resourceName);

        //sum the amounts
        double result = 0.0;
        for (Object o : responseQuery.getResultList()) {
            result += ((Resource) o).getAmount();
        }
        return result;
    }

    @GET
    @Operation(summary = "get all the AvailableResources associated with the given ProposalCycle")
    public AvailableResources getCycleAvailableResources(@PathParam("cycleCode") Long cycleCode)
    {
        //available resources is a wrapper class for a List<Resources>
        return findObject(ProposalCycle.class, cycleCode).getAvailableResources();
    }

    @GET
    @Path("{resourceName}")
    @Operation(summary = "get the AvailableResource identified by 'resourceName' in the given ProposalCycle")
    public Resource getCycleAvailableResource(@PathParam("cycleCode") Long cycleCode,
                                              @PathParam("resourceName") String resourceName)
        throws WebApplicationException
    {
        return findAvailableResource(cycleCode, resourceName);
    }

    @GET
    @Path("{resourceName}/total")
    @Operation(summary = "get the total amount of 'resourceName' available in the cycle")
    public Double getCycleResourceTotal(@PathParam("cycleCode") Long cycleCode,
                                        @PathParam("resourceName") String resourceName)
            throws WebApplicationException
    {
        return findAvailableResource(cycleCode, resourceName).getAmount();
    }

    @GET
    @Path("{resourceName}/allocated")
    @Operation(summary = "get the amount of 'resourceName' currently allocated in the cycle")
    public Double getCycleResourceUsed(@PathParam("cycleCode") Long cycleCode,
                                       @PathParam("resourceName") String resourceName)
            throws WebApplicationException
    {
        //check that the resource name exists at as an "AvailableResource" in the given cycle
        findAvailableResource(cycleCode,resourceName); //this is OK, we're not using the return value

        //sum of all resource amounts with 'resourceName' that have been allocated in the given cycle
        return GetAllocatedResourceAmount(cycleCode,resourceName);
    }

    @GET
    @Path("{resourceName}/remaining")
    @Operation(summary = "get the amount of 'resourceName' yet to be allocated in the given cycle")
    public Double getCycleResourceRemaining(@PathParam("cycleCode") Long cycleCode,
                                            @PathParam("resourceName") String resourceName)
        throws WebApplicationException
    {
        // findAvailableResource performs a check on the 'resourceName'
        Double availableResourceAmount = findAvailableResource(cycleCode, resourceName).getAmount();
        Double allocatedResourceAmount = GetAllocatedResourceAmount(cycleCode,resourceName);
        return availableResourceAmount - allocatedResourceAmount;
    }

    @GET
    @Path("types")
    @Operation(summary = "get all the ResourceTypes associated with the given ProposalCycle")
    public List<ObjectIdentifier> getCycleResourceTypes(@PathParam("cycleCode") Long cycleCode)
    {
        List<Resource> resources = findObject(ProposalCycle.class, cycleCode)
                .getAvailableResources().getResources();

        List<ObjectIdentifier> result = new ArrayList<>();
        for (Resource r : resources) {
            result.add(new ObjectIdentifier(r.getType().getId(), r.getType().getName()));
        }

        return result;
    }

    @GET
    @Path("types/{typeId}")
    @Operation(summary = "get the ResourceType specified by 'typeId'")
    public ResourceType getCycleResourceType(@PathParam("cycleCode") long cycleId,
                                             @PathParam("typeId") long typeId)
    {
        return findObject(ResourceType.class, typeId);
    }

    @POST
    @Operation(summary = "add a resource to the available resources of the given cycle")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Resource addCycleResource(@PathParam("cycleCode") Long cycleCode,
                                     Resource resource)
            throws WebApplicationException
    {
        ProposalCycle proposalCycle = findObject(ProposalCycle.class, cycleCode);

        AvailableResources availableResources = proposalCycle.getAvailableResources();

        List<Resource> resources = availableResources.getResources();

        //ensure available resource types are unique to the cycle
        if (resources.stream()
                .anyMatch(r -> r.getType().getId().equals(resource.getType().getId())))
        {
            throw new WebApplicationException("Resource Types must be unique per Proposal Cycle",
                    Response.Status.CONFLICT);
        }

        //Resource contains a reference to a resource type hence the copy
        return addNewChildObject(availableResources, new Resource(resource),
                availableResources::addToResources);

    }

    @DELETE
    @Path("{resourceId}")
    @Operation(summary = "remove the AvailableResource given by 'resourceId'")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response removeCycleResource(@PathParam("cycleCode") Long cycleCode,
                                        @PathParam("resourceId") Long resourceId)
    {
        ProposalCycle proposalCycle = findObject(ProposalCycle.class, cycleCode);

        AvailableResources availableResources = proposalCycle.getAvailableResources();

        Resource resource = availableResources.getResources()
                .stream().filter(r -> resourceId.equals(r.getId())).findAny()
                .orElseThrow(() -> new WebApplicationException(
                        String.format(NON_ASSOCIATE_ID, "Resource", resourceId, "Proposal Cycle", cycleCode)
                ));

        return deleteChildObject(availableResources, resource,
                availableResources::removeFromResources);
    }

    @PUT
    @Path("{resourceId}/amount")
    @Operation(summary = "edit the 'amount' of the specified AvailableResource in the given proposal cycle")
    @Consumes(MediaType.TEXT_PLAIN)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Resource updateCycleResourceAmount(@PathParam("cycleCode") Long cycleCode,
                                              @PathParam("resourceId") Long resourceId,
                                              Double newAmount)
        throws WebApplicationException
    {
        ProposalCycle proposalCycle = findObject(ProposalCycle.class, cycleCode);

        Resource resource = proposalCycle.getAvailableResources().getResources()
                .stream().filter(r -> resourceId.equals(r.getId())).findAny()
                .orElseThrow(() -> new WebApplicationException(
                        String.format(NON_ASSOCIATE_ID, "Resource", resourceId, "Proposal Cycle", cycleCode)
                ));

        resource.setAmount(newAmount);

        return resource;
    }
}
