package org.orph2020.pst.apiimpl.rest;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;

@QuarkusTest
public class PersonResourceTest {

    @Test
    void testGetPeople() {
        given()
                .when()
                .get("people")
                .then()
                .statusCode(200)
                .body(
                        "$.size()", greaterThan(0)
                );
    }

    @Test
    void testGetPerson() {
        given()
                .when()
                .get("people/37")
                .then()
                .statusCode(200)
                .body(
                        containsString("\"_id\":37")
                );
    }
}
