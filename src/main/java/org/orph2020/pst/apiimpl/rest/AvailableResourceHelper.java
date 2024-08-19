package org.orph2020.pst.apiimpl.rest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.ivoa.dm.proposal.management.Resource;

/**
 *  Helper class to get the named Resource for a given cycle, and get the amount of the named Resource
 *  currently allocated in the given cycle i.e., sum of the named Resource used per allocated proposal.
 *  Used by AvailableResourcesResource and AllocatedBlockResource.
 */
public class AvailableResourceHelper {

    public static
    Resource findAvailableResource(EntityManager em, Long cycleCode, String resourceName)
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

    public static
    Double getAllocatedResourceAmount(EntityManager em, Long cycleCode, String resourceName) {
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




}
