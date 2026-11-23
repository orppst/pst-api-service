package org.orph2020.pst.apiimpl.entities;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.ivoa.dm.proposal.prop.CelestialPosition;
import org.ivoa.dm.proposal.prop.CelestialTarget;
import org.ivoa.dm.proposal.prop.Justification;
import org.ivoa.dm.proposal.prop.coords.Mjd;
import org.ivoa.vodml.stdtypes.Unit;
import org.javastro.ivoacore.pgsphere.types.Point;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestProfile(MigratedDatabaseTest.Profile.class)
@EnabledIfEnvironmentVariable(named = "PST_MIGRATION_TEST_JDBC_URL", matches = ".+")
class MigratedDatabaseTest {
    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            String url = System.getenv("PST_MIGRATION_TEST_JDBC_URL");
            if (url == null || url.isBlank()) {
                return Map.of();
            }
            return Map.of(
                    "quarkus.datasource.devservices.enabled", "false",
                    "quarkus.datasource.jdbc.url", url,
                    "quarkus.datasource.username", Objects.requireNonNull(System.getenv("PST_MIGRATION_TEST_USER")),
                    "quarkus.datasource.password", Objects.requireNonNull(System.getenv("PST_MIGRATION_TEST_PASSWORD")),
                    "quarkus.hibernate-orm.schema-management.strategy", "none",
                    "quarkus.liquibase.migrate-at-start", "true");
        }
    }

    @Inject
    EntityManager entityManager;

    @Test
    @TestTransaction
    void migratedAndNewCoordinatesRoundTrip() {
        CelestialTarget migrated = entityManager.find(CelestialTarget.class, 5L);
        assertEquals(274.688, migrated.getCoord().getSourceCoordinates().getAlpha(), 1e-8);
        assertEquals(-13.792, migrated.getCoord().getSourceCoordinates().getDelta(), 1e-8);
        assertEquals("degrees", migrated.getCoordUnit().value());
        assertEquals(51544.5, migrated.getPositionEpoch().value(), 1e-8);

        CelestialTarget created = new CelestialTarget(
                new CelestialPosition(new Point(120.0, -30.0), "ICRS", null),
                new Unit("degrees"), new Mjd(51544.5), null, null, null, null, "migration test");
        entityManager.persist(created);
        entityManager.flush();
        Long id = created.getId();
        entityManager.clear();

        Point restored = entityManager.find(CelestialTarget.class, id).getCoord().getSourceCoordinates();
        assertEquals(120.0, restored.getAlpha(), 1e-8);
        assertEquals(-30.0, restored.getDelta(), 1e-8);
    }

    @Test
    @TestTransaction
    void migratedAssociationsAndLongJustificationRemainUsable() {
        assertEquals(1018L, ((Number) entityManager.createNativeQuery(
                "select count(*) from \"pdm\".\"Observation_Target\"").getSingleResult()).longValue());
        assertEquals(342L, ((Number) entityManager.createNativeQuery(
                "select count(*) from \"pdm\".\"ObservationConfiguration_Observation\"").getSingleResult()).longValue());
        Number id = (Number) entityManager.createNativeQuery(
                "select \"ID\" from \"pdm\".\"Justification\" where length(\"text\") > 10000 order by \"ID\" limit 1")
                .getSingleResult();
        Justification justification = entityManager.find(Justification.class, id.longValue());
        assertTrue(justification.getText().length() > 10000);
        justification.setText("x".repeat(20000));
        entityManager.flush();
        entityManager.clear();
        assertEquals(20000, entityManager.find(Justification.class, id.longValue()).getText().length());
    }
}
