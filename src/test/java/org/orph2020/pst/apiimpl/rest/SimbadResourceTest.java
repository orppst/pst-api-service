package org.orph2020.pst.apiimpl.rest;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;


@QuarkusTest
public class SimbadResourceTest {

    @Test
    void testGetSimbadFindTarget()
    {
        given()
                .when()
                .param("targetName", "m31")
                .get("simbad")
                .then()
                .statusCode(200)
                .body(
                        containsString("m31")
                );
    }
}
