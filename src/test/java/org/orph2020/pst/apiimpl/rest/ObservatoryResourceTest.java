package org.orph2020.pst.apiimpl.rest;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;

@QuarkusTest
public class ObservatoryResourceTest {

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
        //somewhat fragile to database changes

        given()
                .when()
                .get("observatories/5")
                .then()
                .statusCode(200)
                .body(
                        containsString("\"_id\":5")
                );


        given()
                .when()
                .get("observatories/999")
                .then()
                .statusCode(404)
                .body(
                        containsString("Observatory with id: 999 not found")
                );
    }
}
