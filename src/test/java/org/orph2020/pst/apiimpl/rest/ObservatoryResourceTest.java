package org.orph2020.pst.apiimpl.rest;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.*;

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

        Integer observatoryId =
                given()
                        .when()
                        .param("name", "Jodrell Bank")
                        .get("observatories")
                        .then()
                        .statusCode(200)
                        .body(
                                "$.size()", equalTo(1)
                        )
                        .extract().jsonPath().getInt("[0].dbid");



        given()
                .when()
                .get("observatories/"+observatoryId)
                .then()
                .statusCode(200)
                .body(
                        "name", equalTo("Jodrell Bank")
                );


        given()
                .when()
                .get("observatories/0")
                .then()
                .statusCode(404)
                .body(
                        containsString("Observatory with id: 0 not found")
                );
    }

    @Test
    void testPostBackend() {

        String backendToAdd = "{\"name\":\"myAwesomeBackend\",\"parallel\":true}";

        Integer observatoryId =
        given()
                .when()
                .get("observatories")
                .then()
                .statusCode(200)
                .body(
                        "$.size()", greaterThan(0)
                )
                .extract().jsonPath().getInt("[0].dbid");



        given()
                .body(backendToAdd)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .when()
                .post("observatories/"+observatoryId+"/backend")
                .then()
                .statusCode(201)
                .body(
                        containsString("myAwesomeBackend") //sounds wrong
                );


    }
}
