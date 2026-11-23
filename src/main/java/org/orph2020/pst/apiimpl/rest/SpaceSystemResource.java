package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 21/04/2023 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;




import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("spaceSystems")
@Tag(name = "standard Space Coordinate Systems")
@Produces(MediaType.APPLICATION_JSON)
public class SpaceSystemResource extends ObjectResourceBase {
// FIXME this class should be eliminated as unnecessary after ProposalDM simplification
   @GET
   @Operation(summary = "get a space system")
   @Path("{frameCode}")
   public String getSpaceSystem(@PathParam("frameCode") String frameCode)
   {

      // this does not seem to work in hibernate - though it ought....
//      TypedQuery<SpaceSys> q = em.createQuery("select o From SpaceSys o where  o.frame.spaceRefFrame = :fn", SpaceSys.class);
//      q.setParameter("fn",frameCode);
//      return (SpaceSys) queryObject(q);


      return "ICRS";
   }
}
