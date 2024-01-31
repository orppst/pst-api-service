package org.orph2020.pst.apiimpl.rest;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;

@Path("admin")
public class KeycloakResource {

    Keycloak keycloak;

    @ConfigProperty(name = "keycloak.admin-username")
    String admin_username;

    @ConfigProperty(name = "keycloak.admin-password")
    String admin_password;

    @PostConstruct
    public void initKeyCloak() {
        keycloak = KeycloakBuilder.builder()
                .serverUrl("http://localhost:53536")
                .realm("master")
                .clientId("admin-cli")
                .grantType("password")
                .username(admin_username)
                .password(admin_password)
                .build();
    }

    @PreDestroy
    public void closeKeycloak() {
        keycloak.close();
    }


    @GET
    @Path("/roles")
    public List<RoleRepresentation> getRoles() {
        return keycloak.realm("orppst").roles().list();
    }

    @GET
    @Path("/userRepresentations")
    public List<UserRepresentation> getUserRepresentation() {
        return keycloak.realm("orppst").users().list();
    }

}
