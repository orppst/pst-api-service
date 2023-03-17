package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 16/03/2023 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.prop.Observatory;
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
@Path("observatories")
@Tag(name = "proposal-tool")
public class ObservatoryResource {
   @PersistenceContext
   EntityManager em;  // exists for the application lifetime no need to close

   @Inject
   ObjectMapper mapper;
   @GET
    @Operation(summary = "Get all of the Observatories")
    public List<Observatory> getObservatories(){
       String queryStr = "SELECT o FROM Observatory o ORDER BY o.name";
       TypedQuery<Observatory> query = em.createQuery(queryStr, Observatory.class);
       return query.getResultList();
    }


}
