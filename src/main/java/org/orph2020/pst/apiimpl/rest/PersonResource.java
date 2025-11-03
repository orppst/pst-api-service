package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 16/03/2023 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.prop.Organization;
import org.ivoa.dm.proposal.prop.Person;
import org.ivoa.dm.ivoa.StringIdentifier ;
import org.jboss.resteasy.reactive.RestQuery;
import org.orph2020.pst.apiimpl.entities.SubjectMap;
import org.orph2020.pst.common.json.ObjectIdentifier;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;


@Produces(MediaType.APPLICATION_JSON)
@Path("people")
@Tag(name = "people")
@ApplicationScoped
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
   @Path("email/{email}")
   @RolesAllowed("default-roles-orppst")
   @Operation(summary = "get a Person with the provided email address, no match returns id:0 name:Not found")
   public ObjectIdentifier getPersonByEmail(@RestQuery String email)
   {
       List<ObjectIdentifier> people = getObjectIdentifiers("SELECT o._id,o.fullName FROM Person o Where o.eMail = '" + email + "'");
      if(people.isEmpty())
          return new ObjectIdentifier(0, "Not found");
       return people.get(0);
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
   public Person createPerson(Person person)
   {
      return persistObject(person);
   }

   @POST
   @Path("{keycloakUid}")
   @Operation(summary = "create a new Person in the database from a keycloak 'user'")
   @Consumes(MediaType.APPLICATION_JSON)
   @Transactional(rollbackOn = {WebApplicationException.class})
   public Person createPersonFromKeycloak(@PathParam("keycloakUid") String kcUid, Person person)
   {
      Person result = persistObject(person);

      //store the user's keycloak UID
      em.persist(new SubjectMap(person, kcUid));

      return result;
   }

   @DELETE
   @Path("{id}")
   @RolesAllowed("default-roles-orppst")
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
   @RolesAllowed("default-roles-orppst")
   @Transactional(rollbackOn = {WebApplicationException.class})
   public Response updateFullName(@PathParam("id") Long personId, String replacementFullName)
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
   @RolesAllowed("default-roles-orppst")
   @Transactional(rollbackOn = {WebApplicationException.class})
   public Response updateEMail(@PathParam("id") Long personId, String replacementEMail)
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
   @RolesAllowed("default-roles-orppst")
   @Transactional(rollbackOn = {WebApplicationException.class})
   public Response updateOrcidId(@PathParam("id") Long personId, String replacementOrcidId)
           throws WebApplicationException
   {
      Person person = findObject(Person.class, personId);

      person.setOrcidId(new StringIdentifier(replacementOrcidId));

      return responseWrapper(person, 201);
   }

   @PUT
   @Operation(summary = "update a Person's home institute")
   @Path("{id}/homeInstitute")
   @Consumes(MediaType.APPLICATION_JSON)
   @RolesAllowed("default-roles-orppst")
   @Transactional(rollbackOn = {WebApplicationException.class})
   public Response updateHomeInstitute(@PathParam("id") Long personId, Organization replacementHomeInstitute)
      throws WebApplicationException
   {
      Person person = findObject(Person.class, personId);

      person.setHomeInstitute(replacementHomeInstitute);

      return responseWrapper(person, 201);
   }

}
