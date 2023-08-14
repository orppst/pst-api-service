package org.orph2020.pst.apiimpl.rest;


import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.MediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class TelescopeResourceTest {

    private Integer observatoryId;

    @BeforeEach
    void setup() {

        observatoryId = given()
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
    void testGetTelescopes() {
        given()
                .when()
                .get("observatories/" + observatoryId + "/telescopes")
                .then()
                .statusCode(200)
                .body(
                        "$.size()", greaterThan(0)
                );
    }

    @Test
    void testGetTelescope() {

        //here the test relies on observatoryId being that associated with Jodrell Bank

        Integer telescopeId =
                given()
                        .when()
                        .param("name", "Lovell")
                        .get("observatories/" + observatoryId + "/telescopes")
                        .then()
                        .statusCode(200)
                        .body(
                                "$.size()", equalTo(1)
                        )
                        .extract().jsonPath().getInt("[0].dbid");

        given()
                .when()
                .get("observatories/" + observatoryId + "/telescopes/" + telescopeId)
                .then()
                .statusCode(200)
                .body(
                        "name", equalTo("Lovell")
                );

        //test an invalid telescopeId
        given()
                .when()
                .get("observatories/" + observatoryId + "/telescopes/0")
                .then()
                .statusCode(404)
                .body(
                        containsString("Telescope with id: 0 not found")
                );
    }

    @Test
    void testUpdateTelescopeName() {
        String replacementName = "Beowulf";

        Integer telescopeId =
            given()
                    .when()
                    .get("observatories/" + observatoryId + "/telescopes")
                    .then()
                    .statusCode(200)
                    .body(
                            "$.size()", greaterThan(0)
                    )
                    .extract().jsonPath().getInt("[0].dbid");

        given()
                .body(replacementName)
                .header("Content-Type", MediaType.TEXT_PLAIN)
                .when()
                .put("observatories/" + observatoryId + "/telescopes/" + telescopeId + "/name")
                .then()
                .statusCode(201)
                .body(
                        containsString(replacementName)
                );
    }

    @Test
    void testUpdateTelescopeLocationXYZ() {
        String replacementLocationXYZ =
                "[{\"value\":42.0,\"unit\":\"ft\"},{\"value\":87.0,\"unit\":\"m\"},{\"value\":99.0,\"unit\":\"m\"}]";

        String textToCheck = "\"x\":{\"unit\":{\"value\":\"ft\"},\"value\":42.0}";

        Integer telescopeId =
                given()
                        .when()
                        .get("observatories/" + observatoryId + "/telescopes")
                        .then()
                        .statusCode(200)
                        .body(
                                "$.size()", greaterThan(0)
                        )
                        .extract().jsonPath().getInt("[0].dbid");

        given()
                .body(replacementLocationXYZ)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .when()
                .put("observatories/" + observatoryId + "/telescopes/" + telescopeId + "/location/xyz")
                .then()
                .statusCode(201)
                .body(
                        containsString(textToCheck)
                );
    }
}
