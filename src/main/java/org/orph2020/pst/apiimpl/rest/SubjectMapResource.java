package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 13/04/2023 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.ivoa.StringIdentifier;
import org.ivoa.dm.proposal.prop.Organization;
import org.ivoa.dm.proposal.prop.Person;
import org.jboss.resteasy.reactive.RestQuery;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
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

    Keycloak keycloak;

    RealmResource realm;

    @PostConstruct
    public void initKeyCloak() {
        keycloak = KeycloakBuilder.builder()
                .serverUrl("http://localhost:53536")
                .realm("master")
                .clientId("admin-cli")
                .grantType("password")
                //fixme: we probably don't want hardcoded admin username and password :)
                .username("admin")
                .password("admin")
                .build();

        realm = keycloak.realm("orppst");
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

        List<SubjectMap> usersInDB = q.getResultList();

        //list all users in the keycloak realm
        List<UserRepresentation> usersInRealm = realm.users().list();

        //deal with superuser
        usersInRealm.removeIf(ur->ur.getUsername().equals("superuser"));

        if (usersInDB.size() > usersInRealm.size())
        {
            int diff = usersInDB.size() - usersInRealm.size();
            int count = 0;

            // we have people in DB that are not in the keycloak realm,
            // find which ones and take appropriate action
            for(SubjectMap sm : usersInDB)
            {
                if (usersInRealm.stream().noneMatch(ur -> sm.uid.equals(ur.getId())))
                {
                    sm.setInKeycloakRealm(false);
                    count++;
                }
                if (count == diff) break;
            }
        }

        return usersInDB;
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
            return new SubjectMap(null, id );
        }
        else {
            return res.get(0);
        }
    }

    @GET
    @Path("keycloakUserUIDs")
    @Operation(summary = "get the unique IDs of existing keycloak realm users")
    public List<String> existingUserUIDs()
    {
        List<UserRepresentation> userRepresentations = realm.users().list();

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

        List<UserRepresentation> userRepresentations = realm.users().list();

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
                                    new StringIdentifier(""), //fixme: orchid id
                                    organization
                        )
                );

                persistObject(new SubjectMap(newPerson, ur.getId()));

                result.getAndSet(result.get() + 1);

            } // else user exists as a Person, do nothing
        });

        return result.get();
    }

    @DELETE
    @Path("cleanUsers")
    @Operation(summary = "admin only: cleans up users that have been removed from the keycloak realm")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response cleanUsers()
            throws WebApplicationException
    {
        //query to get all SubjectMap objects in DB
        TypedQuery<SubjectMap> q = em.createQuery(
                "select o from SubjectMap o", SubjectMap.class
        );
        List<SubjectMap> usersInDB = q.getResultList();

        for (SubjectMap subjectMap : usersInDB) {
            if (!subjectMap.inKeycloak()) {
                //remove the current SubjectMap AND related Person
                Person person = findObject(Person.class, subjectMap.getPerson().getId());
                em.remove(person);
                em.remove(subjectMap);
            }
        }

        return emptyResponse204();
    }


}
