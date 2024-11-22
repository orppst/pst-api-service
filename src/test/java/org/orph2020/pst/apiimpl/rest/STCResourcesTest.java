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

/*  it is ambitious to test this because of https://github.com/ivoa-std/CoordinateDM/issues/10 sort of breaking the logic that VO-DML tools have for containment at the moement.
    @Test
    void testSpaceFrames() {
        given()
              .when()
              .get("spaceFrames/ICRS")
              .then()
              .statusCode(200)
              .log().body();

    }
*/
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
