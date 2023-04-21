package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 21/04/2023 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.stc.coords.CoordSys;
import org.ivoa.dm.stc.coords.SpaceFrame;
import org.ivoa.dm.stc.coords.SpaceSys;

import javax.persistence.TypedQuery;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("spaceSystems")
@Tag(name = "standard Space Coordinate Systems")
@Produces(MediaType.APPLICATION_JSON)
public class SpaceSystemResource extends ObjectResourceBase {

   @GET
   @Operation(summary = "get a space system")
   @Path("{frameCode}")
   public SpaceSys getSpaceSystem(@PathParam("frameCode") String frameCode)
   {

      // this does not seem to work in hibernate - though it ought....
//      TypedQuery<SpaceSys> q = em.createQuery("select o From SpaceSys o where  o.frame.spaceRefFrame = :fn", SpaceSys.class);
//      q.setParameter("fn",frameCode);
//      return (SpaceSys) queryObject(q);

      TypedQuery<SpaceSys> q = em.createQuery("select o from SpaceSys o ", SpaceSys.class);
      SpaceSys retval = null;
      for (SpaceSys s : q.getResultList()) {
         if(s.getFrame().getSpaceRefFrame().equals(frameCode))
         {
            retval = s;
            break;
         }
      }
      return retval;
   }
}
