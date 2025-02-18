package org.orph2020.pst.apiimpl.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.quarkus.test.security.oidc.UserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orph2020.pst.apiimpl.entities.telescopeService.TelescopeConfigService;

@QuarkusTest
@TestSecurity(user="John Flamsteed", roles = "default-roles-orppst")
@OidcSecurity(claims = {
    @Claim(key = "email", value = "pi@unreal.not.email")
    ,@Claim(key = "sub", value = "bb0b065f-6dc3-4062-9b3e-525c1a1a9bec")
}, userinfo = {
    @UserInfo(key = "sub", value = "bb0b065f-6dc3-4062-9b3e-525c1a1a9bec")
})
public class TestTelescopeConfigService {

    private TelescopeConfigService telescopeService;

    @BeforeEach
    void setup() {
        telescopeService = new TelescopeConfigService();
        telescopeService.initDB();
    }

    @Test
    void testPickingUpExpectedNTelescopes() {
        assert telescopeService.getTelescopes().values().size() == 14;
    }

    @Test
    void testVerifySaltTelescopeExists() {
        assert telescopeService.getTelescopes().get("SALT") != null;
    }




}
