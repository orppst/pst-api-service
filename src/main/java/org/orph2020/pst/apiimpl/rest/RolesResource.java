package org.orph2020.pst.apiimpl.rest;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.RoleRepresentation;

import java.util.List;

@Path("admin")
public class RolesResource {

    Keycloak keycloak;

    @PostConstruct
    public void initKeyCloak() {
        keycloak = KeycloakBuilder.builder()
                .serverUrl("http://localhost:53536")
                .realm("master")
                .clientId("admin-cli")
                .grantType("password")
                .username("admin")
                .password("admin")
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
}
