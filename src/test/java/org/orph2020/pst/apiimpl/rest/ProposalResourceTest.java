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
                .get("proposals/88")
                .then()
                .statusCode(200)
                .body(
                        containsString("\"title\":\"the proposal title\",")
                )
        ;

        //invalid request
        given()
                .when()
                .get("proposals/not-a-code")
                .then()
                .statusCode(404)
                .body(
                        containsString("404")
                )
        ;

    }


    @Test
    void testReplaceJustification() {
        //replace the technical justification text

        String textToCheck = "\"text\":\"replacement justification\",\"format\":\"LATEX\"";
        String replacementText = "{" + textToCheck + "}";

        given()
                .body(replacementText)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .when()
                .put("proposals/88/justifications/technical")
                .then()
                .statusCode(201)
                .body(
                        containsString(textToCheck)
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
                .put("proposals/88/title")
                .then()
                .statusCode(201)
                .body(
                        containsString("\"title\":\"replacement title\"")
                )
        ;

    }


    @Test
    void testAddPersonAsInvestigator() {
        //add a person as an investigator to a proposal
        String personToAdd = "{\"investigatorKind\":\"COI\",\"forPhD\":false,\"personId\":45}";
        String textToCheck = "\"type\":\"COI\",\"forPhD\":false,\"investigator\":45";

        given()
                .body(personToAdd)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .when()
                .put("proposals/88/investigators")
                .then()
                .statusCode(201)
                .body(
                        containsString(textToCheck)
                )
        ;

    }



}
