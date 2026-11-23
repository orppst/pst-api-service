package org.orph2020.pst.apiimpl.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.quarkus.test.security.oidc.UserInfo;
import io.restassured.internal.mapping.Jackson2Mapper;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import org.ivoa.dm.ivoa.StringIdentifier;
import org.ivoa.dm.proposal.prop.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

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
                        "$.size()", greaterThanOrEqualTo(1)
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

    @Test
    void testExportProposalXml() {
        String proposalTitle = currentProposal().getTitle();

        given()
                .when()
                .get("proposals/" + proposalId + "/exportXml")
                .then()
                .statusCode(200)
                .header("Content-Disposition", containsString("proposal.xml"))
                .body(containsString(proposalTitle));
    }

    @Test
    void testExportZipIncludesXmlProposal() throws IOException {
        String proposalTitle = currentProposal().getTitle();
        Response response = given()
                .when()
                .get("proposals/" + proposalId + "/exportZip")
                .then()
                .statusCode(200)
                .extract()
                .response();

        boolean foundXmlEntry = false;
        String xmlEntryContent = null;

        try (ZipInputStream zipInputStream = new ZipInputStream(
                new ByteArrayInputStream(response.asByteArray()), StandardCharsets.UTF_8)) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (zipEntry.getName().endsWith(".xml")) {
                    foundXmlEntry = true;
                    xmlEntryContent = new String(zipInputStream.readAllBytes(), StandardCharsets.UTF_8);
                    break;
                }
            }
        }

        assertTrue(foundXmlEntry, "Expected exported zip to contain an XML proposal entry");
        assertNotNull(xmlEntryContent);
        assertTrue(xmlEntryContent.contains(proposalTitle));
    }

    @Test
    void testExportImportWithModifiedInvestigators() throws JsonProcessingException {
        //export example proposal them import and check it's there
        String importExportModifiedProposal = "Imported proposal with changed investigators";

        ObservingProposal exportedProposal =
                given()
                        .when()
                        .get("proposals/" + proposalId)
                        .then()
                        .statusCode(200)
                        .extract().as(ObservingProposal.class, raObjectMapper);

        exportedProposal.setTitle(importExportModifiedProposal);

        //Add a new investigator and organisation
        Investigator newInvestigator = getInvestigator();
        exportedProposal.addToInvestigators(newInvestigator);

        //Update details of an existing person, should create a new investigator with the same name!
        exportedProposal
                .getInvestigators()
                .get(0)
                .getPerson()
                .setEMail("modified-" + exportedProposal.getInvestigators().get(0).getPerson().getEMail());

        exportedProposal
                .getInvestigators()
                .get(0)
                .getPerson()
                .setFullName("Updated Person");

        //Import the altered proposal
        given()
                .body(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(exportedProposal))
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .when()
                .post("proposals/import")
                .then()
                .statusCode(200)
                .body(
                        containsString(importExportModifiedProposal)
                );

        //Check new investigator has been added to database
        given()
                .when()
                .param("name", "New Imported Person")
                .get("people")
                .then()
                .statusCode(200)
                .body(
                    containsString("\"name\":\"New Imported Person\"")
                );

        //Check a duplicate investigator has been added
        given()
                .when()
                .param("name", "Updated Person")
                .get("people")
                .then()
                .statusCode(200)
                .body(
                        "$.size()", equalTo(1)
                );

    }

    @Test
    void testImportWithMissingInvestigatorEmailReturns400() throws JsonProcessingException {
        ObservingProposal exportedProposal =
                given()
                        .when()
                        .get("proposals/" + proposalId)
                        .then()
                        .statusCode(200)
                        .extract().as(ObservingProposal.class, raObjectMapper);

        exportedProposal.setTitle("Import with missing email");

        // Set an investigator's email to null to trigger the 400
        exportedProposal.getInvestigators().get(0).getPerson().setEMail(null);

        given()
                .body(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(exportedProposal))
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .when()
                .post("proposals/import")
                .then()
                .statusCode(400);
    }

    private static Investigator getInvestigator() {
        Organization newOrg = new Organization();
        newOrg.setName("New Org");
        newOrg.setAddress("1 Avenue, A Town");
        Person newPerson = new Person();
        newPerson.setHomeInstitute(newOrg);
        newPerson.setEMail("a.n.other@unreal.not.email");
        newPerson.setFullName("New Imported Person");
        StringIdentifier orchidId = new StringIdentifier("8888-1234-5678-9012");
        newPerson.setOrcidId(orchidId);
        Investigator newInvestigator = new Investigator();
        newInvestigator.setPerson(newPerson);
        newInvestigator.setType(InvestigatorKind.COI);
        return newInvestigator;
    }

    private ObservingProposal currentProposal() {
        return given()
                .when()
                .get("proposals/" + proposalId)
                .then()
                .statusCode(200)
                .extract()
                .as(ObservingProposal.class, raObjectMapper);
    }

}
