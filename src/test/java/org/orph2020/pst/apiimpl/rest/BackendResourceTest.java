package org.orph2020.pst.apiimpl.rest;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;

@QuarkusTest
public class BackendResourceTest {

    @Test
    void testGetBackends() {
        given()
                .when()
                .get("backends")
                .then()
                .statusCode(200)
                .body(
                        "$.size()", greaterThan(0)
                );
    }

    @Test
    void testGetBackend() {
        given()
                .when()
                .get("backends/24")
                .then()
                .statusCode(200)
                .body(
                        containsString("Widar Correlator")
                );
    }

}
