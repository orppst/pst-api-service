package org.orph2020.pst.apiimpl.entities;

import org.javastro.ivoacore.pgsphere.types.Point;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DegreePointJavaTypeTest {
    @Test
    void convertsDegreesToPgSphereRadiansAndBack() {
        Point point = new Point(180.0, -30.0);
        String databaseValue = DegreePointJavaType.INSTANCE.asString(point);
        Point restored = DegreePointJavaType.INSTANCE.fromString(databaseValue);

        assertEquals(Math.PI, Double.parseDouble(databaseValue.substring(1, databaseValue.indexOf(','))), 1e-12);
        assertEquals(180.0, restored.getAlpha(), 1e-12);
        assertEquals(-30.0, restored.getDelta(), 1e-12);
    }
}
