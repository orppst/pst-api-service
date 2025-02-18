package org.orph2020.pst.apiimpl.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.quarkus.test.security.oidc.UserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orph2020.pst.apiimpl.entities.telescopeService.Field;
import org.orph2020.pst.apiimpl.entities.telescopeService.Instrument;
import org.orph2020.pst.apiimpl.entities.telescopeService.TelescopeConfigService;

import java.util.ArrayList;

/**
 * this test suite tests specific xmls that have exhibited issues in the past.
 * ensuring they work as expected.
 */
@QuarkusTest
@TestSecurity(user="John Flamsteed", roles = "default-roles-orppst")
@OidcSecurity(claims = {
    @Claim(key = "email", value = "pi@unreal.not.email")
    ,@Claim(key = "sub", value = "bb0b065f-6dc3-4062-9b3e-525c1a1a9bec")
}, userinfo = {
    @UserInfo(key = "sub", value = "bb0b065f-6dc3-4062-9b3e-525c1a1a9bec")
})
public class TestTelescopeConfigService {

    // the telescope service.
    private TelescopeConfigService telescopeService;

    /**
     * the setup used by each test.
     */
    @BeforeEach
    void setup() {
        telescopeService = new TelescopeConfigService();
        telescopeService.initDB();
    }

    /**
     * ensures we're picking up the correct number of telescopes given the
     * current setup.
     */
    @Test
    void testPickingUpExpectedNTelescopes() {
        assert telescopeService.getTelescopes().values().size() == 14;
    }

    /**
     * ensures we've picked up the SALT telescope, as this is used in later
     * tests.
     */
    @Test
    void testVerifySaltTelescopeExists() {
        assert telescopeService.getTelescopes().get("SALT") != null;
    }

    /**
     * ensures we've picked up the lco telescope as it is used in later tests.
     */
    @Test
    void testVerifyLcoTelescopeExists() {
        assert telescopeService.getTelescopes().get("LCO") != null;
    }

    /**
     * test that the salt telescope only has 3 instruments. as
     * <a href="https://github.com/orppst/telescope-config-data/blob/main/figures/salt.pdf">...</a>
     * shows that there is potential for a 4th to be picked up, but the
     * implication is that it shouldn't be.
     */
    @Test
    void testSaltTelescopeInstrumentsQuantity() {
        assert telescopeService.getTelescopes().get("SALT").
            getInstruments().values().size() == 3;
    }

    /**
     * test that the salt telescope only has the correct 3 instruments. as
     * <a href="https://github.com/orppst/telescope-config-data/blob/main/figures/salt.pdf">...</a>
     * shows that there is potential for a 4th to be picked up, but the
     * implication is that it shouldn't be.
     */
    @Test
    void testSaltTelescopeInstrumentsNames() {
        ArrayList<String> names = new ArrayList<>();
        for (Instrument instrument:
            telescopeService.getTelescopes().get("SALT").getInstruments()
                .values()) {
            names.add(instrument.getName());
        }
        assert names.contains("Salticam");
        assert names.contains("RSS");
        assert names.contains("HRS");
        assert !names.contains("BVIT");
    }

    /**
     * test that the salt telescope only has the same url for its url element.
     */
    @Test
    void testSaltURL() {
        for (Instrument instrument:
            telescopeService.getTelescopes().get("SALT").getInstruments()
                .values()) {
            assert instrument.getElements().get("instrumentUrl").
                getValues().get(0).equals("http://pysalt.salt.ac.za/proposal_calls/current/ProposalCall.html");
        }
    }

    /**
     * test that the salt telescope's salicam filter contains all the filters
     * found via the manual search so far. The manual search is displayed here:
     * <a href="https://github.com/orppst/telescope-config-data/blob/main/figures/salt.pdf">...</a>
     * and shows that there is 24 elements of the filter for salicam.
     */
    @Test
    void testSaltFilterOptionsLength() {
        ArrayList<String> options = telescopeService.getTelescopes().get(
            "SALT").getInstruments().get("Salticam").getElements().get(
                "instrumentFilter").getValues();
        assert options.size() == 24;
    }

    /**
     * test that there are only 2 elements in the salt telescope's salicam
     * instrument. This is because the manual exploration exposed a 3rd
     * possible element (instrumentReadOut) as seen in:
     * <a href="https://github.com/orppst/telescope-config-data/blob/main/figures/salt.pdf">...</a>
     */
    @Test
    void testSaltElementsLength() {
        assert telescopeService.getTelescopes().get(
            "SALT").getInstruments().get("Salticam").getElements().size() == 2;
    }

    /**
     * ensure the 2 elements do not contain the floating instrumentReadOut
     * element as found during manual exploration of the xml:
     * <a href="https://github.com/orppst/telescope-config-data/blob/main/figures/salt.pdf">...</a>
     */
    @Test
    void testSaltElementsNotContainingFloater() {
        assert !telescopeService.getTelescopes().get(
            "SALT").getInstruments().get("Salticam").getElements()
            .containsKey("instrumentReadOut");
    }

    /**
     * check that the lco telescope has a boolean flag for its
     * "1m Imaging (Sinistro)" instrument's setting "telescopeHours".
     */
    @Test
    void testTelescopeWithBoolean() {
        assert telescopeService.getTelescopes().get(
            "LCO").getInstruments().get("1m Imaging (Sinistro)").
                getElements().get("telescopeHours").getType() ==
            Field.TYPES.BOOLEAN;
    }

    /**
     * test that the commented out instruments within the LCO telescope have not
     * been captured. even though they're reported in other sections.
     */
    @Test
    void testTelescopeWithInstrumentCommentedOut() {
        assert telescopeService.getTelescopes().get(
                "LCO").getInstruments().get("2m Imaging (Spectral)") == null;
        assert telescopeService.getTelescopes().get(
            "LCO").getInstruments().get("2m Spectroscopy (Floyds)") == null;
    }

    /**
     * test that the LCO telescope has an "instrument comments" element which
     * is a text box format.
     */
    @Test
    void testTelescopeWithTexField() {
        assert telescopeService.getTelescopes().get(
                "LCO").getInstruments().get("1m Imaging (Sinistro)").
            getElements().get("instrumentComments").getType() ==
            Field.TYPES.TEXT;
    }
}
