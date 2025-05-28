package org.orph2020.pst.apiimpl.entities.polarisModeService;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;
import org.orph2020.pst.apiimpl.entities.MODES;

import java.util.Arrays;

@Singleton // Marks this class as a CDI singleton
public class PolarisModeService {

    private static final Logger LOGGER =
        Logger.getLogger(PolarisModeService.class);
    private static final String POLARIS_MODE_ENV_NAME =
        "POLARIS_TELESCOPE_MODE";

    private MODES currentPolarisMode;

    /**
     * This method will be called once when the application starts,
     * after the service has been constructed.
     */
    @PostConstruct
    public void initialisePolarisMode() {
        String environmentMode = System.getenv(POLARIS_MODE_ENV_NAME);

        if (environmentMode == null) {
            this.currentPolarisMode = MODES.RADIO;
            LOGGER.warn(
                "No environment variable was set for " +
                POLARIS_MODE_ENV_NAME + ", so defaulting to RADIO setting.");
        } else {
            try {
                // Use fromText helper for case-insensitive matching
                this.currentPolarisMode = MODES.fromText(environmentMode);
                LOGGER.info(
                    "Polaris mode initialized from environment variable to: " +
                    this.currentPolarisMode.getText());
            } catch (IllegalArgumentException e) {
                this.currentPolarisMode = MODES.RADIO;
                LOGGER.error(
                    "Invalid mode detected from environment variable '" +
                    environmentMode +
                    "'. Setting to RADIO. Valid modes are: " +
                    Arrays.toString(MODES.values()), e);
            }
        }
    }

    /**
     * Gets the current application-wide Polaris mode.
     * @return The current MODES value.
     */
    public MODES getPolarisMode() {
        return currentPolarisMode;
    }

    /**
     * Sets the application-wide Polaris mode.
     * @param newMode The new MODES value to set.
     */
    public void setPolarisMode(MODES newMode) {
        if (newMode != null && this.currentPolarisMode != newMode) {
            this.currentPolarisMode = newMode;
            LOGGER.info("Polaris mode updated to: " + newMode.getText());
        }
    }
}
