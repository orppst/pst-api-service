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

@Path("spaceFrames")
@Tag(name = "standard Space frames")
@Produces(MediaType.APPLICATION_JSON)
public class SpaceFrameResource extends ObjectResourceBase {

   @GET
   @Operation(summary = "get a space frame")
   @Path("{frameCode}")
   public String getSpaceFrame(@PathParam("frameCode") String frameCode)
   {

      return "ICRS";// FIXME this whole class should no longer be needed
   }


}
