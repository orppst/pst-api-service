package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 21/04/2023 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThan;

@QuarkusTest
public class STCResourcesTest {

    @Test
    void testSpaceFames() {
        given()
              .when()
              .get("spaceFrames/ICRS")
              .then()
              .statusCode(200)
              .log().body();

    }

    @Test
    void testSpaceSystems() {
        given()
              .when()
              .get("spaceSystems/ICRS")
              .then()
              .statusCode(200)
              .log().body();

    }

}
