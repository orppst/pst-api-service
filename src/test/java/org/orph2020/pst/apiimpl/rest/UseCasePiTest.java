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
import static io.restassured.http.ContentType.TEXT;
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

        String JSON_UTF16 = "application/json; charset=UTF-16";

        String randomText1 = "Spiderman likes cats";
        String randomText2 = "Batman prefers dogs";
        String randomText3 = "Wonderwoman drinks pints";
        String randomText4 = "Superman licks windows";


        io.restassured.mapper.ObjectMapper raObjectMapper = new Jackson2Mapper(((type, charset) -> {
            return mapper;
        }));


        // che4ck initial conditions
        given()
              .when()
              .get("proposals")
              .then()
              .statusCode(200)
              .body(
                    "$.size()", equalTo(1)
              );



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
                .get("/proposals/"+String.valueOf(proposalid))
                .then()
                .statusCode(200)
                .body("title", equalTo("My New Proposal"))
                .extract().response()
                ;
        System.out.println(response.asString());

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
                        .contentType(JSON_UTF16)
                        .when()
                        .put("proposals/"+proposalid+"/investigators")
                        .then()
                        .contentType(JSON)
                        .body(containsString("\"type\":\"COI\",\"forPhD\":true"))
                        .extract().response();






        //add a new SupportingDocument to the proposal
       //FIXME real use case here will actually upload the document to the document store with http POST form multipart

        SupportingDocument supportingDocument = new SupportingDocument(randomText1,
                "void://fake/path/to/nonexistent/file");

        String jsonSupportingDocument =
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(supportingDocument);

        Response response3 =
                given()
                        .body(jsonSupportingDocument)
                        .contentType(JSON_UTF16)
                        .when()
                        .post("proposals/"+proposalid+"/supportingDocuments")
                        .then()
                        .contentType(JSON)
                        .body(
                                containsString(randomText1)
                        )
                        .extract().response();


        Integer supportingDocumentId =
                given()
                        .when()
                        .param("title", randomText1)
                        .get("proposals/"+proposalid+"/supportingDocuments")
                        .then()
                        .body(
                                "$.size()", equalTo(1)
                        )
                        .extract().jsonPath().getInt("[0].dbid");


        // take a look at what is there now

       given().when().get("proposals").then().log().body();


        // add a related proposal.
        Integer relatedProposalId =
              given()
                    .when()
                    .param("title","%title%") // note only searching for title in part as other tests change this
                    .get("proposals")
                    .then()
                    .statusCode(200)
                    .body("$.size()", equalTo(1))
                    .extract().jsonPath().getInt("[0].dbid");



        ObservingProposal otherProposal =
              given()
                    .when()
                    .get("proposals/"+relatedProposalId)
                    .then()
                    .statusCode(200)
                    .extract().as(ObservingProposal.class, raObjectMapper);
        RelatedProposal relatedProposal = new RelatedProposal(otherProposal);


        given()
              .contentType("text/plain")
              .body(relatedProposalId) //IMPL - just sending the ID - however, when the related proposal contains more fields then this will need some JSON...
              .when()
              .put("/proposals/"+String.valueOf(proposalid)+"/relatedProposals")
              .then()
              .statusCode(201)
              ;


       //FIXME continue manipulating the proposal  add Observations

       //finally submit the proposal.

       Integer cycleId = given()
             .when()
             .get("proposalCycles")
             .then()
             .statusCode(200)
             .body(
                   "$.size()", equalTo(1)
             )
             .extract().jsonPath().getInt("[0].dbid"); //note does not actually use JSONPath syntax! https://github.com/rest-assured/rest-assured/wiki/Usage#json-using-jsonpath

       given()
             .contentType("text/plain")
             .body(proposalid)
             .when()
             .put("/proposalCycles/"+String.valueOf(cycleId)+"/submittedProposals")
             .then()
             .statusCode(201)
       ;


    }



}
