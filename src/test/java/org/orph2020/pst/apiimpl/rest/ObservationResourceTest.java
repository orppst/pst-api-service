package org.orph2020.pst.apiimpl.rest;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;

@QuarkusTest
public class ObservationResourceTest {

    @Test
    void testGetObservations() {
        given()
                .when()
                .get("observations")
                .then()
                .statusCode(200)
                .body(
                        "$.size()", greaterThan(0)
                );
    }

    @Test
    void testGetObservation() {

        //fragile to database changes

        given()
                .when()
                .get("observations/40")
                .then()
                .statusCode(200)
                .body(
                        containsString("fictonal") //there's a typo in the autogenerated code
                );
    }

}