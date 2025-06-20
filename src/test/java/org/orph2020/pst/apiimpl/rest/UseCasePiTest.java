package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 13/04/2023 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.quarkus.test.security.oidc.UserInfo;
import io.restassured.internal.mapping.Jackson2Mapper;
import org.ivoa.dm.ivoa.RealQuantity;
import org.ivoa.dm.ivoa.StringIdentifier;
import org.ivoa.dm.proposal.prop.*;
import org.ivoa.dm.stc.coords.*;
import org.ivoa.vodml.stdtypes.Unit;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import org.orph2020.pst.apiimpl.entities.SubmissionConfiguration;
import org.orph2020.pst.common.json.ObjectIdentifier;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.*;
import static org.hamcrest.Matchers.*;

/**
 * tests the full Use case of a PI adding and submitting a proposal.
 * This test is intended to follow a realistic workflow that might be followed in creating a new proposal.
 * It can even test alternative workflows.
 */
@QuarkusTest
@TestSecurity(user="John Flamsteed", roles = "default-roles-orppst")
@OidcSecurity(claims = {
      @Claim(key = "email", value = "pi@unreal.not.email")
      ,@Claim(key = "sub", value = "bb0b065f-6dc3-4062-9b3e-525c1a1a9bec")
}, userinfo = {
      @UserInfo(key = "sub", value = "bb0b065f-6dc3-4062-9b3e-525c1a1a9bec")
})
public class UseCasePiTest {

    @Inject
    protected ObjectMapper mapper;
    private io.restassured.mapper.ObjectMapper raObjectMapper;
    private Unit ghz;
    private Unit degrees;
    private SpaceSys ICRS_SYS;
    private static final Logger LOGGER = Logger.getLogger("UseCasePiTest");

    @BeforeEach
    void setUp() {
        raObjectMapper = new Jackson2Mapper(((type, charset) -> mapper));
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
    @TestSecurity(user="John Flamsteed", roles = "default-roles-orppst")
    @OidcSecurity(claims = {
          @Claim(key = "email", value = "pi@unreal.not.email")
          ,@Claim(key = "sub", value = "bb0b065f-6dc3-4062-9b3e-525c1a1a9bec")
    }, userinfo = {
          @UserInfo(key = "sub", value = "bb0b065f-6dc3-4062-9b3e-525c1a1a9bec")
    })
    void testCreateProposal() throws IOException {

        String JSON_UTF16 = "application/json; charset=UTF-16";

        String randomText1 = "Spiderman likes cats";
        String randomText2 = "Batman prefers dogs";


        // check initial conditions
        given()
              .when()
              .get("proposals")
              .then()
              .statusCode(200)
              .body(
                    "$.size()", greaterThanOrEqualTo(1 )
              );



        //IMPL the principalInvestigator would usually be the logged-in person....
        //find the PI
        int personid = given()
                .when()
                .param("name","John Flamsteed")
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
                        "fullName", equalTo("John Flamsteed")
                ).extract().as(Person.class, raObjectMapper);


        //create minimal proposal
        ObservingProposal prop = (ObservingProposal) new ObservingProposal().withTitle("My New Proposal")
                .withKind(ProposalKind.STANDARD)
                .withSummary("search for something new")
                .withScientificJustification(
                        new Justification("scientific justification",
                                TextFormats.ASCIIDOC))
                .withTechnicalJustification(
                        new Justification("technical justification",
                                TextFormats.ASCIIDOC));

        prop.setInvestigators(List.of(
                new Investigator(principalInvestigator, InvestigatorKind.PI, false)));

        String propjson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(prop);

        Integer proposalid =
                given()
                        .contentType("application/json")
                        .body(propjson)
                        .when()
                        .post("/proposals")
                        .then()
                        .contentType(JSON)
                        .body("title", equalTo("My New Proposal"))
                        .extract()
                        .path("_id");

        System.out.println("proposalCode="+proposalid);


        int coiPersonId =
                given()
                    .when()
                    .param("name","George Airy")
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
                            "fullName", equalTo("George Airy")
                    ).extract().as(Person.class, raObjectMapper);

        //create a new Investigator
        Investigator coiInvestigator =
                new Investigator(coiPerson, InvestigatorKind.COI, true);

        //convert to a JSON string
        String jsonCoiInvestigator =
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(coiInvestigator);

        //add new COI Investigator to the proposal
        given()
                .body(jsonCoiInvestigator)
                .contentType(JSON_UTF16)
                .when()
                .post("proposals/"+proposalid+"/investigators")
                .then()
                .contentType(JSON)
                .body(containsString("\"type\":\"COI\",\"forPhD\":true"))
                .extract().response();

        //change the "scientific" Justification to LATEX format, and load text from data file 'test-with-figures.tex'
        String latexString = Files.readString(Paths.get("src/test/data/test-with-figures.tex"));

        Justification updateJustification = new Justification(
                latexString, TextFormats.LATEX
        );

        given()
                .body(updateJustification)
                .contentType(JSON_UTF16)
                .put("proposals/" + proposalid + "/justifications/scientific")
                .then()
                .body(containsString("\\begin{document}"));

        final File earthImage = new File("src/test/data/earth_profile.jpg");
        final File hhg2g_dp = new File("src/test/data/hhg2g_dp.jpg");
        final File refs = new File("src/test/data/refs.bib");

        given()
                .multiPart("document", earthImage)
                .when()
                .post("proposals/" + proposalid + "/justifications/resourceFile")
                .then()
                .body(containsString("File earth_profile.jpg saved"));

        given()
                .multiPart("document", hhg2g_dp)
                .when()
                .post("proposals/" + proposalid + "/justifications/resourceFile")
                .then()
                .body(containsString("File hhg2g_dp.jpg saved"));

        given()
                .multiPart("document", refs)
                .when()
                .post("proposals/" + proposalid + "/justifications/resourceFile")
                .then()
                .body(containsString("File refs.bib saved"));

        //this won't work as 'latexmk' needs to be installed in the container running these tests
        //call the api function that calls "latexmk"
//        given()
//                .param("warningsAsErrors", "true")
//                .when()
//                .get("/proposals/" + proposalid + "/justifications/scientific/latexPdf")
//                .then()
//                .body(containsString("file saved as: scientific-justification.pdf"));


        //add a new SupportingDocument to the proposal
       //FIXME real use case here will actually upload the document to the document store with http POST form multipart

        final File testFile = new File("./README.md");

        //this test checks that the title we get in the response is that we set in the request
        given()
                .multiPart("document", testFile)
                .multiPart("title", randomText1)
                .when()
                .post("proposals/"+proposalid+"/supportingDocuments")
                .then()
                .contentType(JSON)
                .body(
                        containsString(randomText1)
                );

        int supportingDocumentId =
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
        given()
                .body(randomText2)
                .contentType(TEXT)
                .put("proposals/"+proposalid+"/supportingDocuments/"+
                        supportingDocumentId+"/title")
                .then()
                .body(
                        containsString(randomText2)
                )
                .extract().response();

        //use the API to clean up the file - note any intermediate directories will remain on your system
        given()
                .when()
                .delete("proposals/"+proposalid+"/supportingDocuments/"+
                        supportingDocumentId)
                .then()
                .statusCode(204);


        // add a related proposal - the related proposal should already exist and/or be submitted?
        Integer relatedProposalId =
              given()
                    .when()
                    .param("title","%Observing%") // note only searching for title in part as other tests change this
                    .get("proposals")
                    .then()
                    .statusCode(200)
                    .body("$.size()", equalTo(1))
                    .extract().jsonPath().getInt("[0].code");



          given()
                .when()
                .get("proposals/"+relatedProposalId)
                .then()
                .statusCode(200)
                .extract().as(ObservingProposal.class, raObjectMapper);

        given()
              .contentType("text/plain")
              .body(relatedProposalId) //IMPL - just sending the ID - however, when the related proposal contains more fields then this will need some JSON...
              .when()
              .put("/proposals/"+ proposalid +"/relatedProposals")
              .then()
              .statusCode(201)
              ;

        //add a list of Targets to the Proposal from a plain text file

        final File targetsPlainText = new File("src/test/data/targetListTest.txt");

        given()
                .multiPart("document", targetsPlainText)
                .when()
                .post("proposals/" + proposalid + "/targets/uploadList")
                .then()
                .body(containsString("\"sourceName\":\"gamma\","));

        // add a list of targets from an VOTable xml file
        final File targetsVOTableXml = new File("src/test/data/targetListTest.xml");

        given()
                .multiPart("document", targetsVOTableXml)
                .when()
                .post("proposals/" + proposalid + "/targets/uploadList")
                .then()
                .body(containsString("\"sourceName\":\"UCAC4 025-003178\","));

        //add a list of targets from an ecsv file
        final File targetsECSV = new File("src/test/data/targetListTest.ecsv");

        given()
                .multiPart("document", targetsECSV)
                .when()
                .post("proposals/" + proposalid + "/targets/uploadList")
                .then()
                .body(containsString("\"sourceName\":\"zeta\","));


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
                    //.log().body()
                    .extract().as(Field.class,raObjectMapper);

        TechnicalGoal technicalGoal = TechnicalGoal.createTechnicalGoal((g) -> {
            g.performance = PerformanceParameters.createPerformanceParameters((p) -> {
                p.desiredAngularResolution = new RealQuantity(13.0, new Unit("arcsec"));
                p.desiredLargestScale = new RealQuantity(0.05, degrees);
                p.representativeSpectralPoint = new RealQuantity(1.9, ghz);
            });
            g.spectrum = Arrays.asList(ScienceSpectralWindow.createScienceSpectralWindow(
                    (ssw) -> ssw.spectralWindowSetup =
                            SpectralWindowSetup.createSpectralWindowSetup((sw) -> {
                sw.start = new RealQuantity(1.4, ghz);
                sw.end = new RealQuantity(2.2, ghz);
                sw.spectralResolution = new RealQuantity(0.3, ghz);
                sw.isSkyFrequency = false;
                sw.polarization = PolStateEnum.LR;
            })), ScienceSpectralWindow.createScienceSpectralWindow((ssw) -> {
                ssw.expectedSpectralLine = Collections.singletonList(
                        ExpectedSpectralLine.createExpectedSpectralLine((sl) -> {
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
                    //.log().body()
                    .extract().as(TechnicalGoal.class,raObjectMapper);

        //create a target observation
        TargetObservation targetObservation =
        TargetObservation.createTargetObservation((t) -> {
            t.target = List.of(createdTarget);
            t.field = createdField;
            t.technicalGoal = createdTechGoal;
        });

        //post the target observation to the proposal

         given()
              .body(mapper.writeValueAsString(targetObservation))
              .contentType(JSON)
              //.log().body()
              .post("proposals/" + proposalid + "/observations")
              .then()
              //.log().body() //IMPL to print out the response
              .statusCode(201)
              .extract().response();



        long observationId =
                given()
                        .when()
                        .get("proposals/"+proposalid+"/observations")
                        .then()
                        .statusCode(200)
                        .body("$.size()", greaterThan(0))
                        .extract().jsonPath().getLong("[0].dbid");

        //create a TimingWindow constraint - notice we do not allow 'null' Dates
        TimingWindow timingWindow = TimingWindow.createTimingWindow((tw) -> {
            tw.startTime = new Date(0); // posix epoch
            tw.endTime = new Date(1000); // 1 second after posix epoch
            tw.isAvoidConstraint = false;
            tw.note = "number 1";
        });

        //post the timing window to the observation
        long constraintId =
                given()
                .body(mapper.writeValueAsString(timingWindow))
                .contentType(JSON)
                .post("proposals/"+proposalid+"/observations/"+observationId+"/constraints")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .extract().as(TimingWindow.class,raObjectMapper).getId();

        //check the timing window values
        given()
                .when()
                .get("proposals/"+proposalid+"/observations/"+observationId+
                        "/constraints/"+constraintId)
                .then()
                .statusCode(200)
                .body("note", equalTo("number 1"))
                .body("isAvoidConstraint", equalTo(false));

        //create a new timing window to update the one just posted
        TimingWindow timingWindowUpdate = TimingWindow.createTimingWindow((tw) -> {
            tw.startTime = new Date(1000);
            tw.endTime = new Date(5000);
            tw.note = "number 1 update";
            tw.isAvoidConstraint = true;
        });

        //update the TimingWindow with new values
        given()
                .body(mapper.writeValueAsString(timingWindowUpdate))
                .contentType(JSON)
                .put("proposals/"+proposalid+"/observations/"+observationId+
                        "/timingWindows/"+constraintId)
                .then()
                .statusCode(200);

        //check the TimingWindow values
        //issues with Dates, differences in the:
        // 1. format of the date-time string;
        // 2. and in the timezones (the string from Date seems to use BST)
        given()
                .when()
                .get("proposals/"+proposalid+"/observations/"+observationId+
                        "/constraints/"+constraintId)
                .then()
                .statusCode(200)
                //.body("startTime", equalTo(new Date(0)))
                //.body("endTime", equalTo(new Date(1000)))
                .body("note", equalTo("number 1 update"))
                .body("isAvoidConstraint", equalTo(true));


        //clone the observation
        Observation oldObservation = given()
              .when()
              .get("proposals/" + proposalid + "/observations/" + observationId)
              .then()
              .statusCode(200)
              .contentType(JSON)
              .extract().as(Observation.class, raObjectMapper);


        given()
            .body(mapper.writeValueAsString(oldObservation))
            .contentType(JSON)
            .post("proposals/"+proposalid+"/observations")
            .then()
            .statusCode(201)
            .extract().response();



       // get the current cycle

       int cycleId = given()
             .when()
             .get("proposalCycles")
             .then()
             .statusCode(200)
             .body(
                   "$.size()", greaterThanOrEqualTo(1)
             )
             .extract().jsonPath().getInt("[0].dbid"); //note does not actually use JSONPath syntax! https://github.com/rest-assured/rest-assured/wiki/Usage#json-using-jsonpath

        LOGGER.info("getting observing mode");
        int obsModeId = given()
                .when()
                .get("/proposalCycles/"+cycleId+"/observingModes")
                .then()
                .statusCode(200).body("$.size()", greaterThanOrEqualTo(1))
                .extract()
                .jsonPath().getInt("[0].dbid");

        List<Long> obsIds = given().when().get("proposals/" + proposalid + "/observations/")
              .then()
              .statusCode(200)
              .log().body()
              .extract().jsonPath().getList(".", ObjectIdentifier.class).stream().map(f -> f.dbid).toList();

        SubmissionConfiguration submittedConfig = new SubmissionConfiguration(proposalid, List.of(new SubmissionConfiguration.ObservationConfigMapping(obsIds, obsModeId)));

        //submit the proposal.
        LOGGER.info("submitting proposal");
        given()
             .contentType(JSON)
             .body(mapper.writeValueAsString(submittedConfig))
             .when()
             .post("/proposalCycles/"+cycleId+"/submittedProposals")
             .then()
             .statusCode(200);

        LOGGER.info("list submitted proposals");
        // check we can see at least 1 submitted proposal
        int submittedId = given()
                .when()
                .get("/proposalsSubmitted?cycleId=" + cycleId)
                .then()
                .statusCode(200)
                .body("$.size()", greaterThanOrEqualTo(1))
                .extract()
                .jsonPath().getInt("[0].code");

        LOGGER.info("withdraw submitted proposal id=" + submittedId);
        given()
                .when()
                .delete("/proposalsSubmitted/" + submittedId + "/withdraw?cycleId=" + cycleId)
                .then()
                .statusCode(200);

        // take a look at what is there now
        LOGGER.info("List of proposals");
        given().when().get("proposals").then().log().body();
        LOGGER.info("List of submitted proposals");
        given().when().get("proposalsSubmitted").then().log().body();

    }



}
