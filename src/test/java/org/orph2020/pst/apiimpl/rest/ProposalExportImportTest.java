package org.orph2020.pst.apiimpl.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.internal.mapping.Jackson2Mapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import org.ivoa.dm.proposal.prop.ObservingProposal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
@TestSecurity(user = "pi", roles = "default-roles-orppst")
public class ProposalExportImportTest {
    @Inject
    protected ObjectMapper mapper;
    private Integer proposalId;
    private io.restassured.mapper.ObjectMapper raObjectMapper;

    @BeforeEach
    void setup() {
        raObjectMapper = new Jackson2Mapper(((type, charset) -> {
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
                .extract().jsonPath().getInt("[0].code");
    }

    @Test
    void testExportThenImportProposal() {
        //export example proposal them import and check it's there
        String importedProposalName = "the proposal title";
/*
        ObservingProposal exportedProposal =
                 given()
                        .when()
                        .get("proposals/" + proposalId)
                        .then()
                        .statusCode(200)
                        .extract().as(ObservingProposal.class, raObjectMapper);

        exportedProposal.setTitle(importedProposalName);
*/

        String exportedProposal = given()
                .when()
                .get("proposals/" + proposalId)
                .then()
                .statusCode(200)
                .extract().asString();

        given()
                .body(exportedProposal)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .when()
                .post("proposals/import")
                .then()
                .statusCode(200)
                .body(
                        containsString(importedProposalName)
                );

    }


}
