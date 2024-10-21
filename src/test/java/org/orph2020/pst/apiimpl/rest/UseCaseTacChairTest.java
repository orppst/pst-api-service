package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 13/04/2023 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.internal.mapping.Jackson2Mapper;
import org.ivoa.dm.proposal.management.*;
import org.ivoa.dm.proposal.management.ObservingMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

import java.util.Calendar;
import java.util.Date;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

/**
 * Use case that the TAC Chair will perform. Reviewing a proposal and allocating time.
 *
 */
@QuarkusTest
public class UseCaseTacChairTest {

   private long cycleId;
   @Inject
   protected ObjectMapper mapper;
   private io.restassured.mapper.ObjectMapper raObjectMapper;
   private CommitteeMember reviewer;

   @BeforeEach
   void setup(){
      //pick up a proposalCycle
      cycleId = given()
            .when()
            .get("proposalCycles")
            .then()
            .statusCode(200)
            .body(
                  "$.size()", equalTo(1)
            )
            .extract().jsonPath().getLong("[0].dbid");
      raObjectMapper = new Jackson2Mapper(((type, charset) -> {
         return mapper;
      }));

      TAC tac =
            given()
                  .when()
                  .get("proposalCycles/"+cycleId+"/TAC")
                  .then()
                  .statusCode(200)
                  .extract().as(TAC.class, raObjectMapper);
      reviewer = tac.getMembers().get(0);


   }
   @Test
   void testListSubmittedProposals() {
       given()
            .when()
            .get("proposalCycles/"+cycleId+"/submittedProposals")
            .then()
            .statusCode(200)
            .body(
                  "$.size()", greaterThan(0)
            );

   }

   @Test
   void reviewProposal() throws JsonProcessingException {
      long revId = given()
            .when()
            .get("proposalCycles/" + cycleId + "/submittedProposals")
            .then()
            .statusCode(200)
            .body(
                  "$.size()", greaterThan(0)
            )
            .extract().jsonPath().getLong("[0].dbid");

      // the TAC member gets a proposal for review
      SubmittedProposal revprop = given()
            .when()
            .get("proposalCycles/" + cycleId + "/submittedProposals/" + revId)
            .then()
            .statusCode(200)
            .extract().as(SubmittedProposal.class, raObjectMapper);


      // TAC member adds a review
      ProposalReview rev = new ProposalReview().withComment("this is good").withScore(5.0).withTechnicalFeasibility(true);
      rev.setReviewer(reviewer.getMember());
      given()
            .contentType("application/json; charset=UTF-16")
            .body(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rev))
            .when()
            .post("proposalCycles/" + cycleId + "/submittedProposals/"+revId+"/reviews")
            .then()
            .contentType(JSON)
            .statusCode(200)
            .log().body(); // TODO not sure that we want to return this all...

   }

   @Test
   void allocateProposal() throws JsonProcessingException {
      long revId = given()
            .when()
            .get("proposalCycles/" + cycleId + "/submittedProposals")
            .then()
            .statusCode(200)
            .body(
                  "$.size()", greaterThan(0)
            )
            .extract().jsonPath().getLong("[0].dbid");

      SubmittedProposal revprop = given()
            .when()
            .get("proposalCycles/" + cycleId + "/submittedProposals/" + revId)
            .then()
            .statusCode(200)
            .extract().as(SubmittedProposal.class, raObjectMapper);

      long subId = revprop.getId();

      //push reviewed proposal to 'allocatedProposals' list
      given()
              .when()
              .body(subId)
              .put("proposalCycles/" + cycleId + "/allocatedProposals")
              .then()
              .statusCode(200);

      //Create a new AllocatedBlock
      //IMPL have chosen the first of everything here - in GUI each will be a list.
      Integer gradeId = given()
              .when()
              .get("proposalCycles/" + cycleId + "/grades")
              .then()
              .body(
                      "$.size()", greaterThan(0)
              )
              .extract().jsonPath().getInt("[0].dbid");

      AllocationGrade grade = given()
              .when()
              .get("proposalCycles/" + cycleId + "/grades/" + gradeId)
              .then()
              .statusCode(200)
              .extract().as(AllocationGrade.class, raObjectMapper);


      Integer modeId = given()
              .when()
              .get("proposalCycles/" + cycleId + "/observingModes")
              .then()
              .body(
                      "$.size()", greaterThan(0)
              )
              .extract().jsonPath().getInt("[0].dbid");

      ObservingMode mode = given()
              .when()
              .get("proposalCycles/" + cycleId + "/observingModes/" + modeId)
              .then()
              .statusCode(200)
              .extract().as(ObservingMode.class, raObjectMapper);

      Integer resourceTypeId = given()
              .when()
              .get("proposalCycles/" + cycleId + "/availableResources/types" )
              .then()
              .body("$.size()", greaterThan(0))
              .extract().jsonPath().getInt("[0].dbid");

      ResourceType resourceType = given()
              .when()
              .get("proposalCycles/" + cycleId + "/availableResources/types/" + resourceTypeId)
              .then()
              .statusCode(200)
              .extract().as(ResourceType.class, raObjectMapper);

      AllocatedBlock allocatedBlock = AllocatedBlock.createAllocatedBlock(
            a -> {
               a.grade = grade;
               a.mode = mode;
               a.resource = new Resource(resourceType, 48.0);
            }
      );

      Integer allocatedId = given()
              .when()
              .get("proposalCycles/" + cycleId + "/allocatedProposals")
              .then()
              .body("$.size()", greaterThan(0))
              .extract().jsonPath().getInt("[0].dbid");

      given()
              .when()
              .body(mapper.writeValueAsString(allocatedBlock))
              .contentType(JSON)
              .post("proposalCycles/" + cycleId + "/allocatedProposals/" + allocatedId + "/allocatedBlock")
              .then()
              .statusCode(200);

   }

}
