package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 16/03/2023 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.prop.Person;
import org.ivoa.dm.ivoa.StringIdentifier ;
import org.jboss.resteasy.reactive.RestQuery;
import org.orph2020.pst.common.json.ObjectIdentifier;

import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;


@Produces(MediaType.APPLICATION_JSON)
@Path("people")
@Tag(name = "people")
public class PersonResource extends ObjectResourceBase {

   @GET
   @Operation(summary = "get People from the database, optionally provide a name to find all the people with that name")
   public List<ObjectIdentifier> getPeople(@RestQuery String name) {
      if(name == null)
         return getObjectIdentifiers("SELECT o._id,o.fullName FROM Person o ORDER BY o.fullName");
      else
         return getObjectIdentifiers("SELECT o._id,o.fullName FROM Person o Where o.fullName like '"+name+"' ORDER BY o.fullName");
   }

   @GET
   @Path("{id}")
   @Operation(summary = "get the specified Person")
   public Person getPerson(@PathParam("id") Long id) {
      return findObject(Person.class, id);
   }

   @POST
   @Operation(summary = "create a new Person in the database")
   @Consumes(MediaType.APPLICATION_JSON)
   @Transactional(rollbackOn = {WebApplicationException.class})
   public Response createPerson(Person person)
         throws WebApplicationException
   {
      return persistObject(person);
   }

   @DELETE
   @Path("{id}")
   @Operation(summary = "delete the Person specified by the 'id' from the database")
   @Transactional(rollbackOn = {WebApplicationException.class})
   public Response deletePerson(@PathParam("id") Long id)
           throws WebApplicationException
   {
      return removeObject(Person.class, id);
   }

   @PUT
   @Operation(summary = "update a Person's full name")
   @Consumes(MediaType.TEXT_PLAIN)
   @Path("{id}/fullName")
   @Transactional(rollbackOn = {WebApplicationException.class})
   public Response updateFullName(@PathParam("id") long personId, String replacementFullName)
      throws WebApplicationException
   {
      Person person = findObject(Person.class, personId);

      person.setFullName(replacementFullName);

      return responseWrapper(person, 201);
   }

   @PUT
   @Operation(summary = "update a Person's email address")
   @Consumes(MediaType.TEXT_PLAIN)
   @Path("{id}/eMail")
   @Transactional(rollbackOn = {WebApplicationException.class})
   public Response updateEMail(@PathParam("id") long personId, String replacementEMail)
           throws WebApplicationException
   {
      Person person = findObject(Person.class, personId);

      person.setEMail(replacementEMail);

      return responseWrapper(person, 201);
   }

   @PUT
   @Operation(summary = "update a Person's orcid ID")
   @Consumes(MediaType.TEXT_PLAIN)
   @Path("{id}/orcidId")
   @Transactional(rollbackOn = {WebApplicationException.class})
   public Response updateOrcidId(@PathParam("id") long personId, String replacementOrcidId)
           throws WebApplicationException
   {
      Person person = findObject(Person.class, personId);

      person.setOrcidId(new StringIdentifier(replacementOrcidId));

      return responseWrapper(person, 201);
   }

}
