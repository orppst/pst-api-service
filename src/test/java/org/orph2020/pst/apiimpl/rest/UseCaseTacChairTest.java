package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 13/04/2023 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.internal.mapping.Jackson2Mapper;
import org.ivoa.dm.proposal.management.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import java.util.Calendar;
import java.util.Date;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.fail;

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
   void setProposalForReview() throws JsonProcessingException {
      long subId = given()
            .when()
            .get("proposalCycles/" + cycleId + "/submittedProposals")
            .then()
            .statusCode(200)
            .body(
                  "$.size()", greaterThan(0)
            )
            .extract().jsonPath().getLong("[0].dbid");


      Calendar calendar = Calendar.getInstance();
      calendar.add(Calendar.DAY_OF_YEAR, 1);
      Date tomorrow = calendar.getTime();
      ReviewedProposal revp = new ReviewedProposal().withReviewsCompleteDate(tomorrow).withSuccessful(false);
      SubmittedProposal submitted = new SubmittedProposal();
      submitted.setXmlId(String.valueOf(subId));
      revp.setSubmitted(submitted);

      given()
            .contentType("application/json; charset=UTF-16")
            .body(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(revp))
            .when()
            .put("proposalCycles/" + cycleId + "/proposalsInReview")
            .then()
            .contentType(JSON)
            .statusCode(201)
            .log().body(); // TODO not sure that we want to return this all...

      // when all proposals set for revieww
      //send emails to the tac members, telling them to review.



   }

   @Test
   void reviewProposal() throws JsonProcessingException {
      long revId = given()
            .when()
            .get("proposalCycles/" + cycleId + "/proposalsInReview")
            .then()
            .statusCode(200)
            .body(
                  "$.size()", greaterThan(0)
            )
            .extract().jsonPath().getLong("[0].dbid");

      // the TAC member gets a proposal for review
      ReviewedProposal revprop = given()
            .when()
            .get("proposalCycles/" + cycleId + "/proposalsInReview/" + revId)
            .then()
            .statusCode(200)
            .extract().as(ReviewedProposal.class, raObjectMapper);


      // TAC member adds a review
      ProposalReview rev = new ProposalReview().withComment("this is good").withScore(5.0).withTechnicalFeasibility(true);
      rev.setReviewer(reviewer.getMember());
      given()
            .contentType("application/json; charset=UTF-16")
            .body(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rev))
            .when()
            .post("proposalCycles/" + cycleId + "/proposalsInReview/"+revId+"/reviews")
            .then()
            .contentType(JSON)
            .statusCode(201)
            .log().body(); // TODO not sure that we want to return this all...



   }


}
