package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 21/04/2023 by Paul Harrison (paul.harrison@manchester.ac.uk).
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

import java.util.Date;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static io.restassured.http.ContentType.JSON;

@QuarkusTest
@TestSecurity(user="tacchair", roles = {"default-roles-orppst", "tac_admin", "obs_administration"})
@OidcSecurity(claims = {
        @Claim(key = "email", value = "tacchair@unreal.not.email")
        ,@Claim(key = "sub", value = "b0f7b98e-ec1e-4cf9-844c-e9f192c97745")
}, userinfo = {
        @UserInfo(key = "sub", value = "b0f7b98e-ec1e-4cf9-844c-e9f192c97745")
})
public class ProposalCycleResourceTest {

   @Inject
   protected ObjectMapper mapper;
   private io.restassured.mapper.ObjectMapper raObjectMapper;

   @BeforeEach
   void setup(){
      raObjectMapper = new Jackson2Mapper(((type, charset) -> {
         return mapper;
      }));
   }

   @Test
   void addProposalCycle() throws JsonProcessingException {
      int observatoryId =
              given()
                      .when()
                      .param("name","Jodrell Bank")
                      .get("observatories")
                      .then()
                      .statusCode(200)
                      .body(
                              "$.size()", equalTo(1)
                      )
                      .extract().jsonPath().getInt("[0].dbid");

      Observatory lookHere =
              given()
                      .when()
                      .get("observatories/" + observatoryId)
                      .then()
                      .statusCode(200)
                      .log().body()
                      .extract().as(Observatory.class, raObjectMapper);

      ProposalCycle newCycle = new ProposalCycle();
      newCycle.setObservatory(lookHere);
      newCycle.setTitle("Test observing cycle");
      newCycle.setSubmissionDeadline(new Date());
      newCycle.setObservationSessionStart(new Date());
      newCycle.setObservationSessionEnd(new Date());
      newCycle.setTac(new TAC());

      String body = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(newCycle);
      //System.out.println("New proposal cycle looks like this: " + body);

      given()
              .when()
              .body(body)
              .contentType(JSON)
              .post("proposalCycles/")
              .then()
              .statusCode(200)
              .log().all();

   }

   @Test
   void testGetCycles() {
      given()
            .when()
            .get("proposalCycles")
            .then()
            .statusCode(200)
            .body(
                  "$.size()", greaterThan(0)
            );
   }

   @Test
   void testGetTAC() {
      long cycleId = given()
            .when()
            .get("proposalCycles")
            .then()
            .statusCode(200)
            .body(
                  "$.size()", greaterThanOrEqualTo(1)
            )
            .extract().jsonPath().getLong("[0].dbid");

      TAC tac =
            given()
                  .when()
                  .get("proposalCycles/"+cycleId+"/TAC")
                  .then()
                  .statusCode(200)
                  .log().body()
                  .extract().as(TAC.class, raObjectMapper);
  // actually this is fine - ids will be quite low in db as each table indexed separately   assertTrue(tac.getMembers().get(0).getId() > 1, "the id from the database should be more than one" );

   }

}
