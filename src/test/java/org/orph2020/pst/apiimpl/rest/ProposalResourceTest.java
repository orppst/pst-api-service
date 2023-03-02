package org.orph2020.pst.apiimpl.rest;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class ProposalResourceTest {

    @Test
    void testAddInvestigator() {
        given()
                .body("37")
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .when()
                .post("/api/proposal/pr1/investigators")
                .then()
                .statusCode(200)
                .body(is("addInvestigator(pr1, 37)"))
                ;
    }



}
