package org.orph2020.pst.apiimpl.rest;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;

@QuarkusTest
public class TelescopeResourceTest {

    @Test
    void testGetTelescopes() {
        given()
                .when()
                .get("telescopes")
                .then()
                .statusCode(200)
                .body(
                        "$.size()", greaterThan(0)
                );
    }

    @Test
    void testGetTelescope() {
        given()
                .when()
                .get("telescopes/8")
                .then()
                .statusCode(200)
                .body(
                        containsString("\"_id\":8")
                );
    }

    @Test
    void testUpdateTelescopeName() {
        String replacementName = "Beowulf";

        given()
                .body(replacementName)
                .header("Content-Type", MediaType.TEXT_PLAIN)
                .when()
                .put("telescopes/8/name")
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

        given()
                .body(replacementLocationXYZ)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .when()
                .put("telescopes/8/location/xyz")
                .then()
                .statusCode(201)
                .body(
                        containsString(textToCheck)
                );
    }
}
