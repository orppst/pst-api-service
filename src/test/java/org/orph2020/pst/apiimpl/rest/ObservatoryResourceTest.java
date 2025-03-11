package org.orph2020.pst.apiimpl.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.quarkus.test.security.oidc.UserInfo;
import org.ivoa.dm.proposal.management.Backend;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestSecurity(user="tacchair", roles = {"default-roles-orppst", "tac_admin"})
@OidcSecurity(claims = {
        @Claim(key = "email", value = "tacchair@unreal.not.email")
        ,@Claim(key = "sub", value = "b0f7b98e-ec1e-4cf9-844c-e9f192c97745")
}, userinfo = {
        @UserInfo(key = "sub", value = "b0f7b98e-ec1e-4cf9-844c-e9f192c97745")
})
public class ObservatoryResourceTest {

    @Inject
    protected ObjectMapper mapper;

    private Integer observatoryId;

    @BeforeEach
    void setup() {
        observatoryId =
                given()
                        .when()
                        .param("name", "Jodrell Bank")
                        .get("observatories")
                        .then()
                        .statusCode(200)
                        .body(
                                "$.size()", equalTo(1)
                        )
                        .extract().jsonPath().getInt("[0].dbid");
    }

    @Test
    void testGetObservatories() {
        given()
                .when()
                .get("observatories")
                .then()
                .statusCode(200)
                .body(
                       "$.size()", greaterThan(0)
                );
    }

    @Test
    void testGetObservatory() {

        given()
                .when()
                .get("observatories/"+observatoryId)
                .then()
                .statusCode(200)
                .body(
                        "name", equalTo("Jodrell Bank")
                );


        given()
                .when()
                .get("observatories/0")
                .then()
                .statusCode(404)
                .body(
                        containsString("Observatory with id: 0 not found")
                );
    }

    @Test
    void testPostBackend() throws JsonProcessingException {

        Backend backendToAdd = new Backend("myAwesomeBackend", true);

        given()
                .body(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(backendToAdd))
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .when()
                .post("observatories/"+observatoryId+"/backends")
                .then()
                .statusCode(200)
                .body(
                        containsString("myAwesomeBackend") //sounds wrong
                );
    }
}
