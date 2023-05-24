package org.orph2020.pst.apiimpl.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.internal.mapping.Jackson2Mapper;
import io.restassured.response.Response;
import org.hamcrest.Matchers;
import org.ivoa.dm.proposal.prop.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

@QuarkusTest
public class ProposalResourceTest {
    String JSON_UTF16 = "application/json; charset=UTF-16";
    @Inject
    protected ObjectMapper mapper;

     private Integer proposalId;
     static Person person;

   @BeforeEach
     void setup() {
        io.restassured.mapper.ObjectMapper raObjectMapper = new Jackson2Mapper(((type, charset) -> {
            return mapper;
        }));
        proposalId = given()
              .when()
              .get("proposals")
              .then()
              .statusCode(200)
              .body(
                    "$.size()", equalTo(1)
              )
              .extract().jsonPath().getInt("[0].dbid");

        Integer coiInvestigatorId =
              given()
                    .when()
                    .param("fullName", "CO-I")
                    .get("people")
                    .then()
                    .body(
                          "$.size()", equalTo(5)
                    )
                    .extract().jsonPath().getInt("[0].dbid");

        person = given()
              .when()
              .get("people/"+coiInvestigatorId)
              .then()
              .statusCode(200)
              .extract().as(Person.class, raObjectMapper);
    }

   @Test
    void testGetObservingProposal() {


        //valid request
        given()
                .when()
                .get("proposals/"+ proposalId)
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
    void testListSpecificProposal()
    {
       Integer relatedProposalId =
             given()
                   .when()
                   .param("title","the proposal title")
                   .get("proposals")
                   .then()
                   .statusCode(200)
                   .body("$.size()", equalTo(1))
                   .extract().jsonPath().getInt("[0].dbid");

    }

    @Test
    void testReplaceJustification() throws JsonProcessingException {
        //replace the technical justification text


        Justification justification = new Justification("replacement justification",
                TextFormats.LATEX);

        given()
                .body(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(justification))
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .when()
                .put("proposals/"+proposalId+"/justifications/technical")
                .then()
                .statusCode(200)
                .body(
                        containsString("replacement justification")
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
                .put("proposals/"+proposalId+"/title")
                .then()
                .statusCode(201)
                .body(
                        containsString("\"title\":\"replacement title\"")
                )
        ;

    }


    @Test
    void testAddPersonAsInvestigator() {
        Investigator coiInvestigator = new Investigator(InvestigatorKind.COI, true, person);
        //first get the DB id of the newly added COI Investigator
        Integer coiInvestigatorId =
              given()
                    .when()
                    .param("fullName", "CO-I")
                    .get("proposals/"+proposalId+"/investigators")
                    .then()
                    .body(
                          "$.size()", equalTo(1)
                    )
                    .extract().jsonPath().getInt("[0].dbid");

        Response response2 =
              given()
                    .body(false)
                    .contentType(JSON_UTF16)
                    .when()
                    .put("proposals/"+proposalId+"/investigators/"+coiInvestigatorId+"/forPhD")
                    .then()
                    .contentType(JSON)
                    .body(
                          Matchers.containsString("\"type\":\"COI\",\"forPhD\":false")
                    )
                    .extract().response();

        System.out.println(response2.asString()); // readable output - can be removed later

    }


   @Test
   void testListTargets() {

      //valid request
      long targetId = given()
            .when()
            .get("proposals/" + proposalId + "/targets")
            .then()
            .statusCode(200)
            .body(
                  "$.size()", greaterThan(0)
            )
            .extract().jsonPath().getLong("[0].dbid");
      ;

   }
   @Test
   void testListFields() {

      //valid request
      long targetId = given()
            .when()
            .get("proposals/" + proposalId + "/fields")
            .then()
            .statusCode(200)
            .body(
                  "$.size()", greaterThan(0)
            )
            .extract().jsonPath().getLong("[0].dbid");
      ;

   }
   @Test
   void testAddField() throws JsonProcessingException {

      //valid request
      String body = ProposalModel.jsonMapper().writeValueAsString(new  TargetField("targetField"));
      System.out.println(body);
      given()
            .when()
            .body(body)
            .contentType(JSON_UTF16)
            .post("proposals/" + proposalId + "/fields")
            .then()
            .statusCode(201)
            .log().all();


   }
   @Test
   void testListGoals() {

      //valid request
      given()
            .when()
            .get("proposals/" + proposalId + "/technicalGoals")
            .then()
            .statusCode(200)
            .body(
                  "$.size()", greaterThan(0)
            );

      ;

   }
   @Test
   void testListObservations() {

      //valid request
      given()
            .when()
            .get("proposals/" + proposalId + "/observations")
            .then()
            .statusCode(200)
            .log().body()
            .body(
                  "$.size()", greaterThan(0)
            );

      ;

   }
}
