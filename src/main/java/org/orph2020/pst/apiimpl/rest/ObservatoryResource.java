package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 16/03/2023 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.orph2020.pst.common.json.ObjectIdentifier;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
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
    public List<ObjectIdentifier> getObservatories(){
       List<ObjectIdentifier> retval = new ArrayList<>();
       String queryStr = "SELECT  o._id,o.name FROM Observatory o ORDER BY o.name";
       Query query = em.createQuery(queryStr);
       List<Object[]> results = query.getResultList();
       for (Object[] r : results)
       {
          retval.add(new ObjectIdentifier((Long) r[0], (String) r[1]));
       }

      return retval;
    }


}
