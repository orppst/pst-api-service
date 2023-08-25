package org.orph2020.pst.apiimpl.rest;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.ivoa.dm.ivoa.RealQuantity;
import org.ivoa.vodml.stdtypes.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.MediaType;

import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class TelescopeResourceTest {

    @Inject
    protected ObjectMapper mapper;

    private Integer observatoryId;

    @BeforeEach
    void setup() {

        observatoryId = given()
                .when()
                .param("name", "Jodrell Bank")
                .get("observatories")
                .then()
                .statusCode(200)
                .body(
                        "$.size()", equalTo(1)
                )
                .extract().jsonPath().getInt("[0].dbid");
    }


    @Test
    void testGetTelescopes() {
        given()
                .when()
                .get("observatories/" + observatoryId + "/telescopes")
                .then()
                .statusCode(200)
                .body(
                        "$.size()", greaterThan(0)
                );
    }

    @Test
    void testGetTelescope() {

        //here the test relies on observatoryId being that associated with Jodrell Bank

        Integer telescopeId =
                given()
                        .when()
                        .param("name", "Lovell")
                        .get("observatories/" + observatoryId + "/telescopes")
                        .then()
                        .statusCode(200)
                        .body(
                                "$.size()", equalTo(1)
                        )
                        .extract().jsonPath().getInt("[0].dbid");

        given()
                .when()
                .get("observatories/" + observatoryId + "/telescopes/" + telescopeId)
                .then()
                .statusCode(200)
                .body(
                        "name", equalTo("Lovell")
                );

        //test an invalid telescopeId
        given()
                .when()
                .get("observatories/" + observatoryId + "/telescopes/0")
                .then()
                .statusCode(404)
                .body(
                        containsString("Telescope with id: 0 not found")
                );
    }


    @Test
    void testUpdateTelescopeName() {
        String replacementName = "Beowulf";

        Integer telescopeId =
            given()
                    .when()
                    .get("observatories/" + observatoryId + "/telescopes")
                    .then()
                    .statusCode(200)
                    .body(
                            "$.size()", greaterThan(0)
                    )
                    .extract().jsonPath().getInt("[0].dbid");

        given()
                .body(replacementName)
                .header("Content-Type", MediaType.TEXT_PLAIN)
                .when()
                .put("observatories/" + observatoryId + "/telescopes/" + telescopeId + "/name")
                .then()
                .statusCode(201)
                .body(
                        containsString(replacementName)
                );
    }


    @Test
    void testUpdateTelescopeLocationXYZ() throws JsonProcessingException {

        RealQuantity x = RealQuantity.createRealQuantity((r) ->{
            r.value = 42.0;
            r.unit = new Unit("ft");
        });

        RealQuantity y = RealQuantity.createRealQuantity((r) ->{
            r.value = 87.0;
            r.unit = new Unit("m");
        });

        RealQuantity z = RealQuantity.createRealQuantity((r) ->{
            r.value = 99.0;
            r.unit = new Unit("m");
        });

        List<RealQuantity> xyz = new ArrayList<>();
        xyz.add(x);
        xyz.add(y);
        xyz.add(z);

        String textToCheck = "\"x\":{\"@type\":\"ivoa:RealQuantity\",\"unit\":{\"value\":\"ft\"},\"value\":42.0}";

        Integer telescopeId =
                given()
                        .when()
                        .get("observatories/" + observatoryId + "/telescopes")
                        .then()
                        .statusCode(200)
                        .body(
                                "$.size()", greaterThan(0)
                        )
                        .extract().jsonPath().getInt("[0].dbid");


        given()
                //api requires @type: RealQuantity in the JSON string - this is how to do it for a List
                .body(mapper.writerFor(new TypeReference<List<RealQuantity>>() {}).writeValueAsString(xyz))
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .when()
                .put("observatories/" + observatoryId + "/telescopes/" + telescopeId + "/location/xyz")
                .then()
                .statusCode(201)
                .body(
                        containsString(textToCheck)
                );
    }


}
