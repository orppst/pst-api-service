package org.orph2020.pst.apiimpl.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.quarkus.test.security.oidc.UserInfo;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestSecurity(user="John Flamsteed", roles = "default-roles-orppst")
@OidcSecurity(claims = {
        @Claim(key = "email", value = "pi@unreal.not.email")
        ,@Claim(key = "sub", value = "bb0b065f-6dc3-4062-9b3e-525c1a1a9bec")
}, userinfo = {
        @UserInfo(key = "sub", value = "bb0b065f-6dc3-4062-9b3e-525c1a1a9bec")
})
public class PersonResourceTest {

    @Test
    void testGetPeople() {
        given()
                .when()
                .get("people")
                .then()
                .statusCode(200)
                .body(
                        "$.size()", greaterThan(0)
                );
    }
   @Test
   void testFilterPeople() {
      given()
            .when()
            .param("name","John Flamsteed")
            .get("people")
            .then()
            .statusCode(200)
            .body(
                  "$.size()", equalTo(1)
            );
   }

    @Test
    void testGetPerson()  {

        Integer personId =
            given()
                    .when()
                    .param("name","John Flamsteed")
                    .get("people")
                    .then()
                    .statusCode(200)
                    .body(
                            "$.size()", equalTo(1)
                    ).extract().jsonPath().getInt("[0].dbid");


        given()
                .when()
                .get("people/"+personId)
                .then()
                .statusCode(200)
                .body(
                        containsString("\"fullName\":\"John Flamsteed\"")
                );
    }
}
