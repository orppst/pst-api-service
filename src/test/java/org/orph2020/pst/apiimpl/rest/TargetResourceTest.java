package org.orph2020.pst.apiimpl.rest;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;

@QuarkusTest
public class TargetResourceTest {

    @Test
    void testGetTargets() {
        given()
                .when()
                .get("targets")
                .then()
                .statusCode(200)
                .body(
                        "$.size()", greaterThan(0)
                );
    }

    @Test
    void testGetTarget() {
        given()
                .when()
                .get("targets/42")
                .then()
                .statusCode(200)
                .body (
                        containsString("fictonal") //matches typo in auto-generated code
                );
    }
}
