package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 13/04/2023 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.ivoa.StringIdentifier;
import org.ivoa.dm.proposal.prop.Organization;
import org.ivoa.dm.proposal.prop.Person;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.jboss.resteasy.reactive.RestQuery;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.orph2020.pst.apiimpl.entities.SubjectMap;

import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;



@Path("subjectMap")
@Tag(name="mapping between AAI user ids and People")
@Produces(MediaType.APPLICATION_JSON)
public class SubjectMapResource extends ObjectResourceBase {

    public enum RoleAction {
        ASSIGN,
        REVOKE
    }

    Keycloak keycloak;

    RealmResource realmOrppst;

    @ConfigProperty(name = "polaris-realm-name")
    String polarisRealmName;

    @ConfigProperty(name = "keycloak.admin-username")
    String admin_username;

    @ConfigProperty(name = "keycloak.admin-password")
    String admin_password;

    @ConfigProperty(name = "auth-server-master")
    String authServerMaster;

    @PostConstruct
    public void initKeyCloak() {
        keycloak = KeycloakBuilder.builder()
                .serverUrl(authServerMaster)
                .realm("master")
                .clientId("admin-cli")
                .grantType("password")
                .username(admin_username)
                .password(admin_password)
                .build();

        realmOrppst = keycloak.realm(polarisRealmName);
    }

    @PreDestroy
    public void closeKeycloak() {
        keycloak.close();
    }


    @GET
    @Operation(summary = "get a list of the SubjectMaps stored in the database, optionally provide a 'uid' to get that specific SubjectMap")
    public List<SubjectMap> subjectMapList(@RestQuery String uid) {

        String selectStr = "select o from SubjectMap o";
        String uidSearchStr = uid != null ? " where o.uid = :uid" : "";

        TypedQuery<SubjectMap> q = em.createQuery(
                selectStr + uidSearchStr, SubjectMap.class
        );

        if (uid != null) {
            q.setParameter("uid", uid);
        }


       return q.getResultList();
    }

   @POST
   @Operation(summary = "create new subjectMap")
   @Transactional(rollbackOn = {WebApplicationException.class})
   @Consumes(MediaType.APPLICATION_JSON)
   @ResponseStatus(value = 201)
    public SubjectMap createFromUser(@QueryParam("uuid") String uuid, Person user){
      SubjectMap ob = new SubjectMap( user, uuid);
      return persistObject(ob);
    }

    @GET
    @Path("{id}")
    @Operation(summary = "get the SubjectMap specified by the 'id'")
    public SubjectMap subjectMap(@PathParam("id") String id)
    {
        TypedQuery<SubjectMap> q = em.createQuery("select o from SubjectMap o where o.uid = :uid", SubjectMap.class);
        q.setParameter("uid", id);
        List<SubjectMap> res = q.getResultList();
        if (res.isEmpty()){
            return new SubjectMap( id );
        }
        else {
            return res.get(0);
        }
    }

    @GET
    @Path("{personId}/uid")
    @Operation(summary = "get the keycloak 'uid' related to the 'personId'")
    public Response getSubjectMapUid(@PathParam("personId") Long personId)
    {
        SubjectMap subjectMap = findSubjectMap(personId);

        return responseWrapper(subjectMap.uid,200);
    }

    @GET
    @Path("keycloakUserUIDs")
    @Operation(summary = "get the unique IDs of existing keycloak realm users")
    public List<String> existingUserUIDs()
    {
        List<UserRepresentation> userRepresentations = realmOrppst.users().list();

        return userRepresentations
                .stream()
                .map(UserRepresentation::getId)
                .collect(Collectors.toList());
    }

    @GET
    @Path("newUsers")
    @Operation(summary = "checks for new users, adds them as a Person if found, returns the number of new users found")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Integer checkForNewUsers()
            throws WebApplicationException
    {
        AtomicReference<Integer> result = new AtomicReference<>(0);

        List<UserRepresentation> userRepresentations = realmOrppst.users().list();

        userRepresentations.forEach((ur) -> {
            TypedQuery<SubjectMap> q = em.createQuery(
                    "select o from SubjectMap o where o.uid = :uid", SubjectMap.class
            );
            q.setParameter("uid", ur.getId());
            List<SubjectMap> res = q.getResultList();

            //notice "superuser" should not be used
            if (res.isEmpty() && !ur.getUsername().equals("superuser")) {
                //new user

                //fixme: organisation details
                Organization organization = findObject(Organization.class, 1L);

                Person newPerson = persistObject(
                        new Person( ur.getFirstName() + " " + ur.getLastName(),
                                    ur.getEmail(),
                                     organization,
                                    new StringIdentifier("") //fixme: orcid id
                                   
                        )
                );

                persistObject(new SubjectMap(newPerson, ur.getId()));

                result.getAndSet(result.get() + 1);

            } // else user exists as a Person, do nothing
        });

        return result.get();
    }

    @PUT
    @Path("{personId}/firstName")
    @Operation(summary = "change the given person's first name")
    @Consumes(MediaType.TEXT_PLAIN)
    @RolesAllowed("default-roles-orppst")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response changeFirstName(@PathParam("personId") Long personId, String firstName)
            throws WebApplicationException
    {
        SubjectMap subjectMap = findSubjectMap(personId);

        UserRepresentation userRepresentation = realmOrppst.users().get(subjectMap.uid).toRepresentation();
        userRepresentation.setFirstName(firstName);

        realmOrppst.users().get(subjectMap.uid).update(userRepresentation);

        Person person = findObject(Person.class, personId);

        String currentFullName = person.getFullName();

        String currentFirstName = currentFullName.substring(0, currentFullName.indexOf(" "));

        String newFullName = currentFullName.replaceFirst(currentFirstName, firstName);

        person.setFullName(newFullName);

        return responseWrapper(person, 200);
    }

    @PUT
    @Path("{personId}/lastName")
    @Operation(summary = "change the given subject's last name")
    @Consumes(MediaType.TEXT_PLAIN)
    @RolesAllowed("default-roles-orppst")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response changeLastName(@PathParam("personId") Long personId, String lastName)
            throws WebApplicationException
    {
        SubjectMap subjectMap = findSubjectMap(personId);

        UserRepresentation userRepresentation = realmOrppst.users().get(subjectMap.uid).toRepresentation();
        userRepresentation.setLastName(lastName);

        realmOrppst.users().get(subjectMap.uid).update(userRepresentation);

        Person person = findObject(Person.class, personId);

        String currentFullName = person.getFullName();

        String currentFirstName = currentFullName.substring(0, currentFullName.indexOf(" "));

        String newFullName = currentFirstName + " " + lastName;

        person.setFullName(newFullName);

        return responseWrapper(person, 200);
    }

    @PUT
    @Path("{personId}/email")
    @Operation(summary = "change the given subject's email address")
    @Consumes(MediaType.TEXT_PLAIN)
    @RolesAllowed("default-roles-orppst")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response changeEmailAddress(@PathParam("personId") Long personId, String emailAddress)
            throws WebApplicationException
    {
        //check that the incoming email address is unique

        String queryStr = "select p.eMail from Person p";

        List<String> emails = em.createQuery(queryStr, String.class).getResultList();

        if (emails.contains(emailAddress)) {
            throw new WebApplicationException(String.format("email: '%s' already in use", emailAddress ), 400);
        }

        SubjectMap subjectMap = findSubjectMap(personId);

        UserRepresentation userRepresentation = realmOrppst.users().get(subjectMap.uid).toRepresentation();
        userRepresentation.setEmail(emailAddress);

        realmOrppst.users().get(subjectMap.uid).update(userRepresentation);

        Person person = findObject(Person.class, personId);

        person.setEMail(emailAddress);

        return responseWrapper(person, 200);
    }

    @PUT
    @Path("{personId}/password")
    @Operation(summary = "reset the given subject's password")
    @RolesAllowed("default-roles-orppst")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response resetPassword(@PathParam("personId") Long personId, String newPassword)
            throws WebApplicationException
    {
        // Dev Note: We assume a frontend client has provided a means to check the new password,
        // i.e., that the user hasn't typo-ed the new password via a confirm method.

        SubjectMap subjectMap = findSubjectMap(personId);

        CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
        credentialRepresentation.setType("password");
        credentialRepresentation.setValue(newPassword);
        credentialRepresentation.setTemporary(false); //just to be explicit

        // change the password in the keycloak realm
        realmOrppst.users().get(subjectMap.uid).resetPassword(credentialRepresentation);

        return emptyResponse204();
    }

    //convenience functions
    private SubjectMap findSubjectMap(Long personId) {
        String queryStr = "select o from SubjectMap o where o.person._id = :id";
        TypedQuery<SubjectMap> q = em.createQuery(queryStr, SubjectMap.class);
        return q.setParameter("id", personId).getSingleResult();
    }

    //use PUT semantics here as we are not adding/removing an object only editing an existing Person

    //this implementation was found on StackOverflow
    // https://stackoverflow.com/questions/49110262/add-a-client-role-to-a-keycloak-user-using-java

    /**
     * Convenience function to assign or revoke the given "role" to the Person (User) specified.
     * Does not check weather the Person already has the "role" assigned.
     * @param personId Long ID of the person you are assigning/revoking the role
     * @param role String name of the (realm) role to assign/revoke (check the list of available roles for the realm)
     * @param roleAction RoleAction enum; either ASSIGN or REVOKE
     *
     */
    public void roleManagement(Long personId, String role, RoleAction roleAction) {
        SubjectMap subjectMap = findSubjectMap(personId);

        RoleScopeResource roleScopeResource = realmOrppst.users().get(subjectMap.uid).roles().realmLevel();

        List<RoleRepresentation> rolesRepresentationList = roleScopeResource.listAvailable();

        // if "role" does not exist in the realm this does nothing
        for (RoleRepresentation roleRepresentation : rolesRepresentationList) {
            if (roleRepresentation.getName().equals(role)) {
                switch (roleAction) {
                    case ASSIGN -> roleScopeResource.add(List.of(roleRepresentation));
                    case REVOKE -> roleScopeResource.remove(List.of(roleRepresentation));
                }
                break; //from 'for'
            }
        }
    }
}
