package org.orph2020.pst.apiimpl.entities;

import org.javastro.ivoacore.pgsphere.PointJavaType;
import org.javastro.ivoacore.pgsphere.types.Point;

/**
 * Converts application coordinates in degrees to and from pgSphere radians.
 */
public class DegreePointJavaType extends PointJavaType {
    public static final DegreePointJavaType INSTANCE = new DegreePointJavaType();

    private DegreePointJavaType() {
        super(Point.class);
    }

    @Override
    public Point fromString(CharSequence value) {
        Point point = super.fromString(value);
        return point == null ? null : new Point(Math.toDegrees(point.getAlpha()), Math.toDegrees(point.getDelta()));
    }

    @Override
    public String asString(Point point) {
        return "(" + Math.toRadians(point.getAlpha()) + ", " + Math.toRadians(point.getDelta()) + ")";
    }
}
