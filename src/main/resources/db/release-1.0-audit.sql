-- Read-only inventory of legacy data needed to plan the 1.0 schema migration.
-- This expects the schema in initial.sql and deliberately fails if it has drifted.
BEGIN TRANSACTION READ ONLY;

SELECT 'table row counts' AS section, 'pdm.CelestialTarget' AS item, count(*) AS rows
FROM "pdm"."CelestialTarget"
UNION ALL SELECT 'table row counts', 'pdm.Field', count(*) FROM "pdm"."Field"
UNION ALL SELECT 'table row counts', 'pdm.Point', count(*) FROM "pdm"."Point"
UNION ALL SELECT 'table row counts', 'pdm.Ellipse', count(*) FROM "pdm"."Ellipse"
UNION ALL SELECT 'table row counts', 'pdm.Polygon', count(*) FROM "pdm"."Polygon"
UNION ALL SELECT 'table row counts', 'public.Polygon_points', count(*) FROM "public"."Polygon_points"
UNION ALL SELECT 'table row counts', 'public.Observation_Target', count(*) FROM "public"."Observation_Target"
UNION ALL SELECT 'table row counts', 'public.ObservationConfiguration_Observation', count(*) FROM "public"."ObservationConfiguration_Observation"
ORDER BY item;

SELECT 'field subtypes' AS section, "Field_SUBTYPE", count(*) AS rows
FROM "pdm"."Field"
GROUP BY "Field_SUBTYPE" ORDER BY "Field_SUBTYPE";

SELECT 'legacy field geometry' AS section, f."ID", f."Field_SUBTYPE",
       p."centre_coordSys", (e."ID" IS NOT NULL) AS has_ellipse,
       (g."ID" IS NOT NULL) AS has_polygon
FROM "pdm"."Field" f
LEFT JOIN "pdm"."Point" p ON p."ID" = f."ID"
LEFT JOIN "pdm"."Ellipse" e ON e."ID" = f."ID"
LEFT JOIN "pdm"."Polygon" g ON g."ID" = f."ID"
WHERE p."ID" IS NOT NULL OR e."ID" IS NOT NULL OR g."ID" IS NOT NULL
   OR f."Field_SUBTYPE" NOT IN ('proposal:TargetField', 'proposal:SinglePointing')
ORDER BY f."ID";

SELECT 'observation constraint subtypes' AS section, "ObservingConstraint_SUBTYPE", count(*) AS rows
FROM "pdm"."ObservingConstraint"
GROUP BY "ObservingConstraint_SUBTYPE" ORDER BY "ObservingConstraint_SUBTYPE";

SELECT 'legacy celestial target coordinates' AS section, t."ID",
       t."positionEpoch", t."sourceCoordinates_lon_value", t."sourceCoordinates_lon_unit",
       t."sourceCoordinates_lat_value", t."sourceCoordinates_lat_unit",
       t."sourceCoordinates_coordSys", s."CoordSys_SUBTYPE", s."ASTROCOORDSYSTEM_ID",
       s."frame_ID", f."CoordFrame_SUBTYPE", f."spaceRefFrame", f."equinox"
FROM "pdm"."CelestialTarget" t
LEFT JOIN "coords"."CoordSys" s ON s."ID" = t."sourceCoordinates_coordSys"
LEFT JOIN "coords"."CoordFrame" f ON f."ID" = s."frame_ID"
ORDER BY t."ID";

SELECT 'coordinate systems' AS section, s."ID", s."CoordSys_SUBTYPE",
       s."ASTROCOORDSYSTEM_ID", s."frame_ID", s."coordSpace_ID", s."pixelSpace_ID",
       f."CoordFrame_SUBTYPE", f."spaceRefFrame", f."equinox",
       f."refPosition_ID", f."refDirection_ID", f."planetaryEphem", f."timescale"
FROM "coords"."CoordSys" s
LEFT JOIN "coords"."CoordFrame" f ON f."ID" = s."frame_ID"
ORDER BY s."ID";

SELECT 'reference locations' AS section, "ID", "RefLocation_SUBTYPE",
       "position", "epoch", "position_coordSys", "velocity_coordSys"
FROM "coords"."RefLocation" ORDER BY "ID";

SELECT 'telescope coordinate systems' AS section, "ID", "location_coordSys",
       "location_x_unit", "location_y_unit", "location_z_unit"
FROM "pdm"."Telescope" ORDER BY "ID";

SELECT 'filter polarization' AS section, "polarization", count(*) AS rows
FROM "pdm"."Filter" GROUP BY "polarization" ORDER BY "polarization";

SELECT 'spectral window polarization' AS section, "polarization", count(*) AS rows
FROM "pdm"."ScienceSpectralWindow" GROUP BY "polarization" ORDER BY "polarization";

SELECT 'filter polarization requiring review' AS section, "ID", "polarization"
FROM "pdm"."Filter"
WHERE "polarization" IS NOT NULL AND "polarization" NOT IN ('LINEAR', 'CIRCULAR')
ORDER BY "ID";

SELECT 'spectral window polarization requiring review' AS section, "ID", "polarization"
FROM "pdm"."ScienceSpectralWindow"
WHERE "polarization" IS NOT NULL AND "polarization" NOT IN ('LINEAR', 'CIRCULAR')
ORDER BY "ID";

SELECT 'review comments longer than new mapping' AS section, "ID", char_length("comment") AS characters
FROM "pdm"."ProposalReview"
WHERE char_length("comment") > 1024 ORDER BY "ID";

ROLLBACK;
