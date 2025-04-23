package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 13/04/2023 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.quarkus.test.security.oidc.UserInfo;
import io.restassured.internal.mapping.Jackson2Mapper;
import org.ivoa.dm.proposal.management.*;
import org.ivoa.dm.proposal.management.ObservingMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.*;

/**
 * Use case that the TAC Chair will perform. Reviewing a proposal and allocating time.
 *
 */
@QuarkusTest
@TestSecurity(user="tacchair", roles = {"default-roles-orppst", "tac_admin", "obs_administration"})
@OidcSecurity(claims = {
        @Claim(key = "email", value = "tacchair@unreal.not.email")
        ,@Claim(key = "sub", value = "b0f7b98e-ec1e-4cf9-844c-e9f192c97745")
}, userinfo = {
        @UserInfo(key = "sub", value = "b0f7b98e-ec1e-4cf9-844c-e9f192c97745")
})
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
                  "$.size()", greaterThanOrEqualTo(1)
            )
            .extract().jsonPath().getLong("[0].dbid");
      raObjectMapper = new Jackson2Mapper(((type, charset) -> mapper));

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
      given()
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
      int gradeId = given()
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




      int resourceTypeId = given()
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

      //NB - this is not really representative of what should happen in reality - there could be several modes on different observations
      ObservingMode mode = revprop.getConfig().get(0).getMode();

      AllocatedBlock allocatedBlock = AllocatedBlock.createAllocatedBlock(
            a -> {
               a.grade = grade;
               a.mode = mode;
               a.resource = new Resource(resourceType, 48.0);
            }
      );

      int allocatedId = given()
              .when()
              .get("proposalCycles/" + cycleId + "/allocatedProposals")
              .then()
              .body("$.size()", greaterThan(0))
              .extract().jsonPath().getInt("[0].dbid");

      int allocationsSize = given()
              .when()
              .get("proposalCycles/" + cycleId + "/allocatedProposals")
              .then()
              .body("$.size()", greaterThan(0))
              .extract().as(List.class).size();

      given()
              .when()
              .body(mapper.writeValueAsString(allocatedBlock))
              .contentType(JSON)
              .post("proposalCycles/" + cycleId + "/allocatedProposals/" + allocatedId + "/allocatedBlock")
              .then()
              .statusCode(200);

      //test remove the allocated proposal
      given()
              .when()
              .delete("proposalCycles/" + cycleId + "/allocatedProposals/" + allocatedId)
              .then()
              .statusCode(204);

      //check the list of allocated proposals; should now one fewer than before
      given()
              .when()
              .get("proposalCycles/" + cycleId + "/allocatedProposals")
              .then()
              .body("$.size()", equalTo(allocationsSize - 1));

   }

}
