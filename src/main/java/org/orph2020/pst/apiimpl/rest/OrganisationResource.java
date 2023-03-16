package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 16/03/2023 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.prop.ObservingProposal;
import org.ivoa.dm.proposal.prop.Organization;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Produces(MediaType.APPLICATION_JSON)
@Path("organisations")
@Tag(name = "proposal-tool")
public class OrganisationResource {
   @PersistenceContext
   EntityManager em;  // exists for the application lifetime no need to close

   @Inject
   ObjectMapper mapper;
   @GET
    @Operation(summary = "Get all of the organisations")
    public List<Organization> getOrganisations(){
       String queryStr = "SELECT o FROM Organization o ORDER BY o.name";
       TypedQuery<Organization> query = em.createQuery(queryStr, Organization.class);
       return query.getResultList();
    }

}
