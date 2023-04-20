package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 13/04/2023 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.internal.mapping.Jackson2Mapper;
import io.restassured.response.Response;
import org.ivoa.dm.proposal.prop.*;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import java.util.Arrays;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.*;

/**
 * tests the full Use case of a PI adding and submitting a proposal.
 * This test is intended to follow a realistic workflow that might be followed in creating a new proposal.
 * It can even test alternative workflows.
 */
@QuarkusTest
public class UseCasePiTest {

    @Inject
    protected ObjectMapper mapper;
    @Test
    void testCreateProposal() throws JsonProcessingException {


        io.restassured.mapper.ObjectMapper raObjectMapper = new Jackson2Mapper(((type, charset) -> {
            return mapper;
        }));

        //IMPL the principalInvestigator would usually be the logged-in person....
        //find the PI
        Integer personid = given()
                .when()
                .param("name","PI")
                .get("people")
                .then()
                .statusCode(200)
                .body(
                        "$.size()", equalTo(1)
                )
                .extract().jsonPath().getInt("[0].dbid"); //note does not actually use JSONPath syntax! https://github.com/rest-assured/rest-assured/wiki/Usage#json-using-jsonpath

        System.out.println("personId="+personid);
        //get the PI person
        Person principalInvestigator = given()
                .when()
                .get("people/"+personid)
                .then()
                .statusCode(200)
                .body(
                        "fullName", equalTo("PI")
                ).extract().as(Person.class, raObjectMapper);
        ObservingProposal prop = new ObservingProposal().withTitle("My New Proposal")
                .withKind(ProposalKind.STANDARD)
                .withSummary("search for something new")
                .withScientificJustification(new Justification("scientific justification", TextFormats.ASCIIDOC))
                .withTechnicalJustification(new Justification("technical justification", TextFormats.ASCIIDOC))
                ;

        prop.setInvestigators(Arrays.asList(new Investigator(InvestigatorKind.PI,false,principalInvestigator)));

        //create minimal proposal
        String propjson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(prop);
        Integer proposalid =
                given()
                        .contentType("application/json; charset=UTF-16")
                        .body(propjson)
                        .when()
                        .post("/proposals")
                        .then()
                        .contentType(JSON)
                        .body("title", equalTo("My New Proposal"))
                        .extract()
                        .path("_id");

        System.out.println("proposalCode="+proposalid);

        Response response = given().when()
                .get("/proposals/"+proposalid)
                .then()
                .statusCode(200)
                .body("title", equalTo("My New Proposal"))
                .extract().response()
                ;
        System.out.println(response.asString()); // readable output - can be removed later

        //Add a person as COI Investigator

        //find CO-I id

        Integer coiPersonId =
                given()
                    .when()
                    .param("name","CO-I")
                    .get("people")
                    .then()
                    .statusCode(200)
                    .body(
                            "$.size()", equalTo(1)
                    )
                    .extract().jsonPath().getInt("[0].dbid");

        //get the Person object
        Person coiPerson =
                given()
                    .when()
                    .get("people/"+coiPersonId)
                    .then()
                    .statusCode(200)
                    .body(
                            "fullName", equalTo("CO-I")
                    ).extract().as(Person.class, raObjectMapper);

        //create a new Investigator
        Investigator coiInvestigator = new Investigator(InvestigatorKind.COI, true, coiPerson);

        //convert to a JSON string
        String jsonCoiInvestigator = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(coiInvestigator);

        //add new COI Investigator to the proposal
        Response response1 =
                given()
                        .body(jsonCoiInvestigator)
                        .contentType("application/json; charset=UTF-16")
                        .when()
                        .put("proposals/"+proposalid+"/investigators")
                        .then()
                        .contentType(JSON)
                        .body(containsString("\"type\":\"COI\",\"forPhD\":true"))
                        .extract().response();

        System.out.println(response1.asString()); // readable output - can be removed later

        //change the 'forPhD' field of the newly added COI Investigator to false

        //first get the DB id of the newly added COI Investigator
        Integer coiInvestigatorId =
                given()
                        .when()
                        .param("fullName", "CO-I")
                        .get("proposals/"+proposalid+"/investigators")
                        .then()
                        .body(
                                "$.size()", equalTo(1)
                        )
                        .extract().jsonPath().getInt("[0].dbid");

        Response response2 =
                given()
                        .body(false)
                        .contentType("application/json; charset=UTF-16")
                        .when()
                        .put("proposals/"+proposalid+"/investigators/"+coiInvestigatorId+"/forPhD")
                        .then()
                        .contentType(JSON)
                        .body(
                                containsString("\"type\":\"COI\",\"forPhD\":false")
                        )
                        .extract().response();

        System.out.println(response2.asString()); // readable output - can be removed later


    }


    //TODO: continue manipulating the proposal - add Observations,... submit
}
