package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 13/04/2023 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.internal.mapping.Jackson2Mapper;
import io.restassured.response.Response;
import org.ivoa.dm.ivoa.RealQuantity;
import org.ivoa.dm.ivoa.StringIdentifier;
import org.ivoa.dm.proposal.prop.*;
import org.ivoa.dm.proposal.prop.Point;
import org.ivoa.dm.stc.coords.*;
import org.ivoa.vodml.stdtypes.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import java.util.ArrayList;
import java.util.Arrays;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static io.restassured.http.ContentType.TEXT;
import static org.hamcrest.Matchers.*;

/**
 * tests the full Use case of a PI adding and submitting a proposal.
 * This test is intended to follow a realistic workflow that might be followed in creating a new proposal.
 * It can even test alternative workflows.
 */
@QuarkusTest
public class UseCasePiTest {

    @Inject
    protected ObjectMapper mapper;
    private io.restassured.mapper.ObjectMapper raObjectMapper;
    private Unit ghz;
    private Unit degrees;
    private SpaceSys ICRS_SYS;

    @BeforeEach
    void setUp() {
        raObjectMapper = new Jackson2Mapper(((type, charset) -> {
            return mapper;
        }));
        ghz = new Unit("ghz");
        degrees = new Unit("degrees");
        ICRS_SYS = given()
              .when()
              .get("spaceSystems/ICRS")
              .then()
              .statusCode(200)
              .extract().as(SpaceSys.class, raObjectMapper);
    }

    @Test
    void testCreateProposal() throws JsonProcessingException {

        String JSON_UTF16 = "application/json; charset=UTF-16";

        String randomText1 = "Spiderman likes cats";
        String randomText2 = "Batman prefers dogs";
        String randomText3 = "Wonderwoman drinks pints";
        String randomText4 = "Superman licks windows";


        // che4ck initial conditions
        given()
              .when()
              .get("proposals")
              .then()
              .statusCode(200)
              .body(
                    "$.size()", equalTo(1)
              );



        //IMPL the principalInvestigator would usually be the logged-in person....
        //find the PI
        Integer personid = given()
                .when()
                .param("name","PI")
                .get("people")
                .then()
                .statusCode(200)
                .body(
                        "$.size()", equalTo(1)
                )
                .extract().jsonPath().getInt("[0].dbid"); //note does not actually use JSONPath syntax! https://github.com/rest-assured/rest-assured/wiki/Usage#json-using-jsonpath

        System.out.println("personId="+personid);
        //get the PI person
        Person principalInvestigator = given()
                .when()
                .get("people/"+personid)
                .then()
                .statusCode(200)
                .body(
                        "fullName", equalTo("PI")
                ).extract().as(Person.class, raObjectMapper);



        ObservingProposal prop = new ObservingProposal().withTitle("My New Proposal")
                .withKind(ProposalKind.STANDARD)
                .withSummary("search for something new")
                .withScientificJustification(new Justification("scientific justification", TextFormats.ASCIIDOC))
                .withTechnicalJustification(new Justification("technical justification", TextFormats.ASCIIDOC))
                ;

        prop.setInvestigators(Arrays.asList(new Investigator(InvestigatorKind.PI,false,principalInvestigator)));

        //create minimal proposal
        String propjson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(prop);
        Integer proposalid =
                given()
                        .contentType("application/json; charset=UTF-16")
                        .body(propjson)
                        .when()
                        .post("/proposals")
                        .then()
                        .contentType(JSON)
                        .body("title", equalTo("My New Proposal"))
                        .extract()
                        .path("_id");

        System.out.println("proposalCode="+proposalid);

        Response response = given().when()
                .get("/proposals/"+String.valueOf(proposalid))
                .then()
                .statusCode(200)
                .body("title", equalTo("My New Proposal"))
                .extract().response()
                ;

        Integer coiPersonId =
                given()
                    .when()
                    .param("name","CO-I")
                    .get("people")
                    .then()
                    .statusCode(200)
                    .body(
                            "$.size()", equalTo(1)
                    )
                    .extract().jsonPath().getInt("[0].dbid");

        //get the Person object
        Person coiPerson =
                given()
                    .when()
                    .get("people/"+coiPersonId)
                    .then()
                    .statusCode(200)
                    .body(
                            "fullName", equalTo("CO-I")
                    ).extract().as(Person.class, raObjectMapper);

        //create a new Investigator
        Investigator coiInvestigator = new Investigator(InvestigatorKind.COI, true, coiPerson);

        //convert to a JSON string
        String jsonCoiInvestigator = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(coiInvestigator);

        //add new COI Investigator to the proposal
        Response response1 =
                given()
                        .body(jsonCoiInvestigator)
                        .contentType(JSON_UTF16)
                        .when()
                        .put("proposals/"+proposalid+"/investigators")
                        .then()
                        .contentType(JSON)
                        .body(containsString("\"type\":\"COI\",\"forPhD\":true"))
                        .extract().response();


        //add a new SupportingDocument to the proposal
       //FIXME real use case here will actually upload the document to the document store with http POST form multipart

        SupportingDocument supportingDocument = new SupportingDocument(randomText1,
                "void://fake/path/to/nonexistent/file");

        String jsonSupportingDocument =
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(supportingDocument);

        Response response3 =
                given()
                        .body(jsonSupportingDocument)
                        .contentType(JSON_UTF16)
                        .when()
                        .post("proposals/"+proposalid+"/supportingDocuments")
                        .then()
                        .contentType(JSON)
                        .body(
                                containsString(randomText1)
                        )
                        .extract().response();


        Integer supportingDocumentId =
                given()
                        .when()
                        .param("title", randomText1)
                        .get("proposals/"+proposalid+"/supportingDocuments")
                        .then()
                        .body(
                                "$.size()", equalTo(1)
                        )
                        .extract().jsonPath().getInt("[0].dbid");

        //change the title of the SupportingDocument
        Response response4 =
                given()
                        .body(randomText2)
                        .contentType(TEXT)
                        .put("proposals/"+proposalid+"/supportingDocuments/"+supportingDocumentId+"/title")
                        .then()
                        .body(
                                containsString(randomText2)
                        )
                        .extract().response();


        // add a related proposal - the related proposal should already exist and/or be submitted?
        Integer relatedProposalId =
              given()
                    .when()
                    .param("title","%title%") // note only searching for title in part as other tests change this
                    .get("proposals")
                    .then()
                    .statusCode(200)
                    .body("$.size()", equalTo(1))
                    .extract().jsonPath().getInt("[0].dbid");


        ObservingProposal otherProposal =
              given()
                    .when()
                    .get("proposals/"+relatedProposalId)
                    .then()
                    .statusCode(200)
                    .extract().as(ObservingProposal.class, raObjectMapper);
        RelatedProposal relatedProposal = new RelatedProposal(otherProposal);

        given()
              .contentType("text/plain")
              .body(relatedProposalId) //IMPL - just sending the ID - however, when the related proposal contains more fields then this will need some JSON...
              .when()
              .put("/proposals/"+String.valueOf(proposalid)+"/relatedProposals")
              .then()
              .statusCode(201)
              ;


       // add Observations

        CelestialTarget target = CelestialTarget.createCelestialTarget((c) -> {
            c.sourceName = "imaginativeSourceName";
            c.sourceCoordinates = new EquatorialPoint(
                  new RealQuantity(12.5, degrees),
                  new RealQuantity(78.4, degrees),
                  ICRS_SYS);
            c.positionEpoch = new Epoch("J2000.0");
        });

        CelestialTarget createdTarget =
              given()
                    .body(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(target))
                    .contentType(JSON_UTF16)
                    .when()
                    .post("proposals/"+proposalid+"/targets")
                    .then()
                    .contentType(JSON)
//                    .log().body()
                    .extract().as(CelestialTarget.class,raObjectMapper);

        Field createdField =
              given()
                    .body(mapper.writeValueAsString(new TargetField("the field")))
                    .contentType(JSON_UTF16)
                    .when()
                    .post("proposals/"+proposalid+"/fields")
                    .then()
                    .contentType(JSON)
                    .log().body()
                    .extract().as(Field.class,raObjectMapper);

        TechnicalGoal technicalGoal = TechnicalGoal.createTechnicalGoal((g) -> {
            g.performance = PerformanceParameters.createPerformanceParameters((p) -> {
                p.desiredAngularResolution = new RealQuantity(13.0, new Unit("arcsec"));
                p.desiredLargestScale = new RealQuantity(0.05, degrees);
                p.representativeSpectralPoint = new RealQuantity(1.9, ghz);
            });
            g.spectrum = Arrays.asList(ScienceSpectralWindow.createScienceSpectralWindow((ssw) -> {
                ssw.index = 98;
                ssw.spectralWindowSetup = SpectralWindowSetup.createSpectralWindowSetup((sw) -> {
                    sw.start = new RealQuantity(1.4, ghz);
                    sw.end = new RealQuantity(2.2, ghz);
                    sw.spectralResolution = new RealQuantity(0.3, ghz);
                    sw.isSkyFrequency = false;
                    sw.polarization = PolStateEnum.LR;
                });
            }), ScienceSpectralWindow.createScienceSpectralWindow((ssw) -> {
                ssw.index = 99;
                ssw.expectedSpectralLine = Arrays.asList(ExpectedSpectralLine.createExpectedSpectralLine((sl) -> {
                    sl.restFrequency = new RealQuantity(1.8472, ghz);
                    sl.description = "ALIENS";
                    sl.splatalogId = new StringIdentifier("1000101");
                }));
                ssw.spectralWindowSetup = SpectralWindowSetup.createSpectralWindowSetup((sw) -> {
                    sw.start = new RealQuantity(1.84, ghz);
                    sw.end = new RealQuantity(1.89, ghz);
                    sw.spectralResolution = new RealQuantity(120.0, new Unit("khz"));
                    sw.isSkyFrequency = false;
                    sw.polarization = PolStateEnum.PP;
                });
            }));
        });

        TechnicalGoal createdTechGoal =
              given()
                    .body(mapper.writeValueAsString(technicalGoal))
                    .contentType(JSON_UTF16)
                    .when()
                    .post("proposals/"+proposalid+"/technicalGoals")
                    .then()
                    .contentType(JSON)
                    .log().body()
                    .extract().as(TechnicalGoal.class,raObjectMapper);

        //copied and edited from the EmerlinExample
        TargetObservation targetObservation =
        TargetObservation.createTargetObservation((t) -> {

            t.target = createdTarget;
            t.field = createdField;
            t.technicalGoal = createdTechGoal;

        });

        Response response5 =
                given()
                        .body(mapper.writeValueAsString(targetObservation))
                        .contentType(JSON)
                        .post("proposals/"+proposalid+"/observations")
                        .then()
                        .statusCode(201)
                        .extract().response();



       //finally submit the proposal.

       Integer cycleId = given()
             .when()
             .get("proposalCycles")
             .then()
             .statusCode(200)
             .body(
                   "$.size()", equalTo(1)
             )
             .extract().jsonPath().getInt("[0].dbid"); //note does not actually use JSONPath syntax! https://github.com/rest-assured/rest-assured/wiki/Usage#json-using-jsonpath

       given()
             .contentType("text/plain")
             .body(proposalid)
             .when()
             .put("/proposalCycles/"+String.valueOf(cycleId)+"/submittedProposals")
             .then()
             .statusCode(201)
       ;

       // take a look at what is there now

       given().when().get("proposals").then().log().body();
    }



}
