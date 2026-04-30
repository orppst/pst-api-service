package org.orph2020.pst.apiimpl.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.quarkus.test.security.oidc.UserInfo;
import io.restassured.internal.mapping.Jackson2Mapper;
import org.ivoa.dm.proposal.management.Filter;
import org.ivoa.dm.proposal.management.ObservingMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static io.restassured.http.ContentType.JSON;

@QuarkusTest
@TestSecurity(user = "tacchair", roles = {"default-roles-orppst", "tac_admin", "obs_administration"})
@OidcSecurity(claims = {
        @Claim(key = "email", value = "tacchair@unreal.not.email"),
        @Claim(key = "sub", value = "b0f7b98e-ec1e-4cf9-844c-e9f192c97745")
}, userinfo = {
        @UserInfo(key = "sub", value = "b0f7b98e-ec1e-4cf9-844c-e9f192c97745")
})
public class ObservingModeResourceTest {

    @Inject
    protected ObjectMapper mapper;

    private io.restassured.mapper.ObjectMapper raObjectMapper;

    private long cycleId;
    private String baseUrl;

    @BeforeEach
    void setup() {
        raObjectMapper = new Jackson2Mapper((type, charset) -> mapper);

        cycleId = given()
                .when()
                .get("proposalCycles")
                .then()
                .statusCode(200)
                .body("$.size()", greaterThanOrEqualTo(1))
                .extract().jsonPath().getLong("[0].dbid");

        baseUrl = "proposalCycles/" + cycleId + "/observingModes";
    }

    @Test
    void testAddNewObservingMode() throws JsonProcessingException {
        // Get an existing mode to reuse its telescope/instrument/backend references
        List<ObservingMode> existingModes = given()
                .when()
                .get(baseUrl + "/objectList")
                .then()
                .statusCode(200)
                .body("$.size()", greaterThan(0))
                .extract().jsonPath().getList("$", ObservingMode.class);

        ObservingMode sourceMode = existingModes.get(0);
        Filter sourceFilter = sourceMode.getFilter();

        // Create a new filter
        Filter newFilter = new Filter(
                "TestFilter",
                "A test filter",
                sourceFilter.getFrequencyCoverage()
        );

        // Create a new mode using the same telescope/instrument/backend as the source
        ObservingMode newMode = new ObservingMode(
                "TestObservingMode",
                "A test observing mode",
                sourceMode.getTelescope(),
                sourceMode.getInstrument(),
                newFilter,
                sourceMode.getBackend()
        );

        String body = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(newMode);

        given()
                .when()
                .body(body)
                .contentType(JSON)
                .post(baseUrl)
                .then()
                .statusCode(200)
                .body("name", equalTo("TestObservingMode"))
                .body("description", equalTo("A test observing mode"));
    }

    @Test
    void testUpdateObservingMode() throws JsonProcessingException {
        // Get an existing mode
        List<ObservingMode> existingModes = given()
                .when()
                .get(baseUrl + "/objectList")
                .then()
                .statusCode(200)
                .body("$.size()", greaterThan(0))
                .extract().jsonPath().getList("$", ObservingMode.class);

        ObservingMode modeToUpdate = existingModes.get(0);
        long modeId = modeToUpdate.getId();

        // Create a replacement mode with updated name and description
        ObservingMode replacement = new ObservingMode(
                "UpdatedModeName",
                "Updated mode description",
                modeToUpdate.getTelescope(),
                modeToUpdate.getInstrument(),
                modeToUpdate.getFilter(),
                modeToUpdate.getBackend()
        );

        String body = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(replacement);

        given()
                .when()
                .body(body)
                .contentType(JSON)
                .put(baseUrl + "/" + modeId)
                .then()
                .statusCode(200)
                .body("name", equalTo("UpdatedModeName"))
                .body("description", equalTo("Updated mode description"));
    }

    @Test
    void testUpdateObservingModeFilter() throws JsonProcessingException {
        // Get an existing mode
        List<ObservingMode> existingModes = given()
                .when()
                .get(baseUrl + "/objectList")
                .then()
                .statusCode(200)
                .body("$.size()", greaterThan(0))
                .extract().jsonPath().getList("$", ObservingMode.class);

        ObservingMode mode = existingModes.get(0);
        long modeId = mode.getId();
        Filter existingFilter = mode.getFilter();

        // Create a replacement filter with updated name and description
        Filter replacementFilter = new Filter(
                "UpdatedFilterName",
                "Updated filter description",
                existingFilter.getFrequencyCoverage()
        );

        String body = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(replacementFilter);

        given()
                .when()
                .body(body)
                .contentType(JSON)
                .put(baseUrl + "/" + modeId + "/filter")
                .then()
                .statusCode(200)
                .body("name", equalTo("UpdatedFilterName"))
                .body("description", equalTo("Updated filter description"));
    }

    @Test
    void testDeleteObservingMode() throws JsonProcessingException {
        // First, create a new mode to delete so we don't break existing test data
        List<ObservingMode> existingModes = given()
                .when()
                .get(baseUrl + "/objectList")
                .then()
                .statusCode(200)
                .body("$.size()", greaterThan(0))
                .extract().jsonPath().getList("$", ObservingMode.class);

        ObservingMode sourceMode = existingModes.get(0);
        Filter sourceFilter = sourceMode.getFilter();

        Filter newFilter = new Filter(
                "FilterToDelete",
                "A filter that will be deleted",
                sourceFilter.getFrequencyCoverage()
        );

        ObservingMode modeToDelete = new ObservingMode(
                "ModeToDelete",
                "A mode that will be deleted",
                sourceMode.getTelescope(),
                sourceMode.getInstrument(),
                newFilter,
                sourceMode.getBackend()
        );

        String body = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(modeToDelete);

        ObservingMode createdMode = given()
                .when()
                .body(body)
                .contentType(JSON)
                .post(baseUrl)
                .then()
                .statusCode(200)
                .extract().as(ObservingMode.class, raObjectMapper);

        long newModeId = createdMode.getId();

        // Now delete it
        given()
                .when()
                .delete(baseUrl + "/" + newModeId)
                .then()
                .statusCode(204);

        // Verify it's no longer in the list
        given()
                .when()
                .get(baseUrl)
                .then()
                .statusCode(200)
                .body("$", not(hasItem(hasEntry("dbid", (int) newModeId))));
    }

}
