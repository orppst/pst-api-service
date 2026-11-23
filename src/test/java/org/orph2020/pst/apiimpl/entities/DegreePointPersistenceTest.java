package org.orph2020.pst.apiimpl.entities;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.ivoa.dm.proposal.prop.CelestialPosition;
import org.ivoa.dm.proposal.prop.CelestialTarget;
import org.ivoa.dm.proposal.prop.coords.Mjd;
import org.ivoa.vodml.stdtypes.Unit;
import org.javastro.ivoacore.pgsphere.types.Point;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class DegreePointPersistenceTest {
    @Inject
    EntityManager entityManager;

    @Test
    @TestTransaction
    void persistsDegreesAsPgSphereRadians() {
        CelestialTarget target = new CelestialTarget(
                new CelestialPosition(new Point(180.0, -30.0), "ICRS", null),
                new Unit("degrees"), new Mjd(51544.5), null, null, null, null, "test");
        entityManager.persist(target);
        entityManager.flush();
        Long id = target.getId();

        Number longitude = (Number) entityManager.createNativeQuery(
                        "select degrees(long(\"coord_sourceCoordinates\")) from \"pdm\".\"CelestialTarget\" where \"ID\" = :id")
                .setParameter("id", id).getSingleResult();
        assertEquals(180.0, longitude.doubleValue(), 1e-10);

        entityManager.clear();
        Point restored = entityManager.find(CelestialTarget.class, id).getCoord().getSourceCoordinates();
        assertEquals(180.0, restored.getAlpha(), 1e-10);
        assertEquals(-30.0, restored.getDelta(), 1e-10);
    }
}
