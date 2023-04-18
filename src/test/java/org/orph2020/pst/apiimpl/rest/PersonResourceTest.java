package org.orph2020.pst.apiimpl.rest;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

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
   void testFilterPeople() {
      given()
            .when()
            .param("name","PI")
            .get("people")
            .then()
            .statusCode(200)
            .body(
                  "$.size()", equalTo(1)
            );
   }

    @Test
    void testGetPerson()  {

        Integer personId =
            given()
                    .when()
                    .param("name","PI")
                    .get("people")
                    .then()
                    .statusCode(200)
                    .body(
                            "$.size()", equalTo(1)
                    ).extract().jsonPath().getInt("[0].dbid");


        given()
                .when()
                .get("people/"+personId)
                .then()
                .statusCode(200)
                .body(
                        containsString("\"fullName\":\"PI\"")
                );
    }
}
