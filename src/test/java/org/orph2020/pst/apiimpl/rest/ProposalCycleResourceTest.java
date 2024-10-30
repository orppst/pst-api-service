package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 21/04/2023 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.internal.mapping.Jackson2Mapper;
import org.ivoa.dm.proposal.management.TAC;
import org.ivoa.dm.proposal.prop.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import  static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
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
