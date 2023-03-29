package org.orph2020.pst.apiimpl.rest;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThan;

@QuarkusTest
public class OrganizationResourceTest {

    @Test
    void testGetOrganizations() {
        given()
                .when()
                .get("organizations")
                .then()
                .statusCode(200)
                .body(
                        "$.size()", greaterThan(0)
                );
    }
}
