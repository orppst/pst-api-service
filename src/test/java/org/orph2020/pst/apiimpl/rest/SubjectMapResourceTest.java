package org.orph2020.pst.apiimpl.rest;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

/*
 * Created on 13/04/2023 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

@QuarkusTest
class SubjectMapResourceTest {

    @Test
    void testGetMapping(){
        given()
              .when()
              .get("subjectMap/bb0b065f-6dc3-4062-9b3e-525c1a1a9bec")
              .then()
              .statusCode(200)
              .body(
                    "person.eMail",equalTo("pi@unreal.not.email")
              );
    }
}