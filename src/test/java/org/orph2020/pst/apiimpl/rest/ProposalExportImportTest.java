package org.orph2020.pst.apiimpl.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.quarkus.test.security.oidc.UserInfo;
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
@OidcSecurity(claims = {
      @Claim(key = "email", value = "pi@unreal.not.email")
      ,@Claim(key = "sub", value = "bb0b065f-6dc3-4062-9b3e-525c1a1a9bec")
}, userinfo = {
      @UserInfo(key = "sub", value = "bb0b065f-6dc3-4062-9b3e-525c1a1a9bec")
})
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
    void testExportThenImportProposal() throws JsonProcessingException {
        //export example proposal them import and check it's there
        String importExportProposalName = "Import of exported proposal";

        ObservingProposal exportedProposal =
                 given()
                        .when()
                        .get("proposals/" + proposalId)
                        .then()
                        .statusCode(200)
                        .extract().as(ObservingProposal.class, raObjectMapper);

        exportedProposal.setTitle(importExportProposalName);

        given()
                .body(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(exportedProposal))
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .when()
                .post("proposals/import")
                .then()
                .statusCode(200)
                .body(
                        containsString(importExportProposalName)
                );

    }


}
