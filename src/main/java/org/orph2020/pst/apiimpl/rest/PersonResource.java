package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 16/03/2023 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.prop.Person;

import javax.inject.Inject;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


@Produces(MediaType.APPLICATION_JSON)
@Path("people")
@Tag(name = "proposal-tool")

public class PersonResource {
   @PersistenceContext
   EntityManager em;  // exists for the application lifetime no need to close

   @Inject
   ObjectMapper mapper;
   @POST
   @Operation(summary = "create a new Person in the database")
   @APIResponse(
         responseCode = "201",
         description = "create a new Person in the database"
   )
   @Consumes(MediaType.APPLICATION_JSON)
   @Transactional(rollbackOn = {WebApplicationException.class})
   public Response createPerson(String jsonPerson)
         throws WebApplicationException
   {
      //throws if JSON string not valid or cannot build object from the string
      Person person;
      try {
         person = mapper.readValue(jsonPerson, Person.class);
      } catch (JsonProcessingException e) {
         throw new WebApplicationException("Invalid JSON input", 422);
      }

      try {
         em.persist(person);
      } catch (EntityExistsException e) {
         throw new WebApplicationException(e, 400);
      }

      return Response.ok().entity("Person created successfully").build();
   }

}
