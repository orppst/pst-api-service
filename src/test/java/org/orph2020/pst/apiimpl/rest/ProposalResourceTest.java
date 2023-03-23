package org.orph2020.pst.apiimpl.rest;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.greaterThan;

@QuarkusTest
public class ProposalResourceTest {

    @Test
    void testGetObservingProposal() {
        //valid request
        given()
                .when()
                .get("proposals/pr1")
                .then()
                .statusCode(200)
                .body(
                        containsString("\"code\":\"pr1\",")
                )
        ;

        //invalid request
        given()
                .when()
                .get("proposals/not-a-code")
                .then()
                .statusCode(404)
                .body(
                        containsString("ObservingProposal with code: not-a-code not found")
                )
        ;

    }




    @Test
    void testReplaceJustification() {
        //replace the technical justification text
        given()
                .body("{\"text\":\"replacement justification\",\"format\":\"LATEX\"}")
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .when()
                .put("proposals/pr1/justifications/technical")
                .then()
                .statusCode(200)
                .body(
                        containsString("ObservingProposal.technicalJustification updated")
                )
        ;

        //check the technical justification text has actually been replaced
        given()
                .when()
                .get("proposals/pr1")
                .then()
                .statusCode(200)
                .body(
                        containsString("{\"text\":\"replacement justification\",\"format\":\"LATEX\"}")
                )
        ;

    }

    @Test
    void testReplaceTitle() {
        //replace title with another
        given()
                .body("replacement title")
                .header("Content-Type", MediaType.TEXT_PLAIN)
                .when()
                .put("proposals/pr1/title")
                .then()
                .statusCode(200)
                .body(
                        containsString("Title updated")
                )
        ;

        //check the title text has actually been replaced
        given()
                .when()
                .get("proposals/pr1")
                .then()
                .statusCode(200)
                .body(
                        containsString("\"title\":\"replacement title\"")
                )
        ;
    }


    @Test
    void testAddPersonAsInvestigator() {
        //add a person as an investigator to a proposal
        given()
                .body("{\"investigatorKind\": \"COI\", \"forPhD\": false, \"personId\": 37}")
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .when()
                .put("proposals/pr1/investigators")
                .then()
                .statusCode(200)
                .body(
                        containsString("ObservingProposal.investigators updated")
                )
        ;

        given()
                .when()
                .get("proposals/pr1")
                .then()
                .statusCode(200)
                .body(
                        containsString("\"type\":\"COI\",\"forPhD\":false,\"investigator\":37")
                )
        ;
    }



}
