-- The entire upgrade is one Liquibase transaction. Retain the source values
-- alongside the new mappings so the conversion can be audited.
DO $$
DECLARE
    bad_ids text;
    constraint_row record;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_sphere') THEN
        RAISE EXCEPTION 'The pg_sphere extension must be installed before migration';
    END IF;

    SELECT string_agg("ID"::text, ', ' ORDER BY "ID") INTO bad_ids
    FROM "pdm"."Field"
    WHERE "Field_SUBTYPE" <> 'proposal:TargetField';
    IF bad_ids IS NOT NULL THEN
        RAISE EXCEPTION 'Unsupported legacy field IDs require conversion: %', bad_ids;
    END IF;

    SELECT string_agg("ID"::text, ', ' ORDER BY "ID") INTO bad_ids
    FROM "pdm"."ObservingConstraint"
    WHERE "ObservingConstraint_SUBTYPE" NOT IN ('proposal:TimingWindow', 'proposal:SimultaneityConstraint');
    IF bad_ids IS NOT NULL THEN
        RAISE EXCEPTION 'Unsupported observing constraint IDs: %', bad_ids;
    END IF;

    SELECT string_agg(t."ID"::text, ', ' ORDER BY t."ID") INTO bad_ids
    FROM "pdm"."CelestialTarget" t
    LEFT JOIN "coords"."CoordSys" s ON s."ID" = t."sourceCoordinates_coordSys"
    LEFT JOIN "coords"."CoordFrame" f ON f."ID" = s."frame_ID"
    WHERE t."sourceCoordinates_lon_unit" <> 'degrees'
       OR t."sourceCoordinates_lat_unit" <> 'degrees'
       OR f."spaceRefFrame" IS DISTINCT FROM 'ICRS'
       OR f."equinox" IS NOT NULL
       OR t."positionEpoch" !~ '^J[0-9]{4}(\.[0-9]+)?$'
       OR t."sourceCoordinates_lon_value" NOT BETWEEN -360 AND 360
       OR (t."sourceCoordinates_lat_value" NOT BETWEEN -90 AND 90
           AND NOT (t."ID" = 5 AND t."sourceCoordinates_lon_value" = -13.792
                    AND t."sourceCoordinates_lat_value" = 274.688));
    IF bad_ids IS NOT NULL THEN
        RAISE EXCEPTION 'Unsupported coordinates, frame, units or epoch for celestial target IDs: %', bad_ids;
    END IF;

    IF EXISTS (SELECT 1 FROM "pdm"."Justification" WHERE length("text") > 20000) THEN
        RAISE EXCEPTION 'Justification text exceeds the 20000-character target limit';
    END IF;
    IF EXISTS (SELECT 1 FROM "pdm"."AbstractProposal" WHERE length("summary") > 1024) THEN
        RAISE EXCEPTION 'Proposal summary exceeds the 1024-character target limit';
    END IF;

    FOR constraint_row IN
        SELECT c.conrelid::regclass AS table_name, c.conname
        FROM pg_constraint c
        JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY(c.conkey)
        WHERE c.contype = 'c' AND a.attname = 'polarization'
          AND c.conrelid IN ('"pdm"."Filter"'::regclass, '"pdm"."ScienceSpectralWindow"'::regclass)
    LOOP
        EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I', constraint_row.table_name, constraint_row.conname);
    END LOOP;
END $$;

ALTER TABLE "pdm"."AbstractProposal" ALTER COLUMN "summary" TYPE varchar(1024);
ALTER TABLE "pdm"."Justification" ALTER COLUMN "text" TYPE varchar(20000);

ALTER TABLE "pdm"."AbstractProposal" ADD CONSTRAINT "AbstractProposal_subtype_current_check"
    CHECK ("AbstractProposal_SUBTYPE" IN ('proposal:ObservingProposal', 'proposalManagement:SubmittedProposal'));
ALTER TABLE "pdm"."Field" ADD CONSTRAINT "Field_subtype_current_check"
    CHECK ("Field_SUBTYPE" IN ('proposal:TargetField', 'proposal:SinglePointing'));
ALTER TABLE "pdm"."Observation" ADD CONSTRAINT "Observation_subtype_current_check"
    CHECK ("Observation_SUBTYPE" IN ('proposal:CalibrationObservation', 'proposal:TargetObservation'));
ALTER TABLE "pdm"."ObservingConstraint" ADD CONSTRAINT "ObservingConstraint_subtype_current_check"
    CHECK ("ObservingConstraint_SUBTYPE" IN ('proposal:TimingWindow', 'proposal:SimultaneityConstraint'));
ALTER TABLE "pdm"."ObservingPlatform" ADD CONSTRAINT "ObservingPlatform_subtype_current_check"
    CHECK ("ObservingPlatform_SUBTYPE" IN ('proposalManagement:Telescope', 'proposalManagement:TelescopeArray'));
ALTER TABLE "pdm"."Organization" ADD CONSTRAINT "Organization_subtype_current_check"
    CHECK ("Organization_SUBTYPE" IN ('proposal:Organization', 'proposalManagement:Observatory'));
ALTER TABLE "pdm"."ResourceBlock" ADD CONSTRAINT "ResourceBlock_subtype_current_check"
    CHECK ("ResourceBlock_SUBTYPE" IN ('proposalManagement:ResourceBlock', 'proposalManagement:AllocatedBlock'));
ALTER TABLE "pdm"."Target" ADD CONSTRAINT "Target_subtype_current_check"
    CHECK ("Target_SUBTYPE" IN ('proposal:CelestialTarget', 'proposal:SolarSystemTarget'));

ALTER TABLE "pdm"."Filter" ADD COLUMN "legacy_polarization" varchar(255);
UPDATE "pdm"."Filter" SET "legacy_polarization" = "polarization", "polarization" = 'CIRCULAR';
ALTER TABLE "pdm"."ScienceSpectralWindow" ADD COLUMN "legacy_polarization" varchar(255);
UPDATE "pdm"."ScienceSpectralWindow" SET "legacy_polarization" = "polarization", "polarization" = 'CIRCULAR';
ALTER TABLE "pdm"."Filter" ADD CONSTRAINT "Filter_polarization_current_check" CHECK ("polarization" IN ('LINEAR', 'CIRCULAR'));
ALTER TABLE "pdm"."ScienceSpectralWindow" ADD CONSTRAINT "ScienceSpectralWindow_polarization_current_check" CHECK ("polarization" IN ('LINEAR', 'CIRCULAR'));

ALTER TABLE "pdm"."CelestialTarget" RENAME COLUMN "positionEpoch" TO "legacy_positionEpoch";
ALTER TABLE "pdm"."CelestialTarget"
    ADD COLUMN "positionEpoch" double precision,
    ADD COLUMN "coordUnit" varchar(255),
    ADD COLUMN "coord_equinox" double precision,
    ADD COLUMN "coord_referenceFrame" varchar(255),
    ADD COLUMN "coord_sourceCoordinates" spoint,
    ALTER COLUMN "legacy_positionEpoch" DROP NOT NULL,
    ALTER COLUMN "sourceCoordinates_lon_value" DROP NOT NULL,
    ALTER COLUMN "sourceCoordinates_lon_unit" DROP NOT NULL,
    ALTER COLUMN "sourceCoordinates_lat_value" DROP NOT NULL,
    ALTER COLUMN "sourceCoordinates_lat_unit" DROP NOT NULL;

UPDATE "pdm"."CelestialTarget"
SET "positionEpoch" = 51544.5 + (substring("legacy_positionEpoch" FROM 2)::double precision - 2000) * 365.25,
    "coordUnit" = 'degrees',
    "coord_referenceFrame" = 'ICRS',
    "coord_sourceCoordinates" = spoint(
        radians(CASE WHEN "ID" = 5 AND "sourceCoordinates_lon_value" = -13.792 AND "sourceCoordinates_lat_value" = 274.688
                     THEN "sourceCoordinates_lat_value" ELSE "sourceCoordinates_lon_value" END),
        radians(CASE WHEN "ID" = 5 AND "sourceCoordinates_lon_value" = -13.792 AND "sourceCoordinates_lat_value" = 274.688
                     THEN "sourceCoordinates_lon_value" ELSE "sourceCoordinates_lat_value" END));
ALTER TABLE "pdm"."CelestialTarget"
    ALTER COLUMN "positionEpoch" SET NOT NULL,
    ALTER COLUMN "coordUnit" SET NOT NULL;

ALTER TABLE "pdm"."Telescope"
    ADD COLUMN "location_refpos" varchar(255),
    ALTER COLUMN "location_x_value" DROP NOT NULL,
    ALTER COLUMN "location_y_value" DROP NOT NULL,
    ALTER COLUMN "location_z_value" DROP NOT NULL,
    ALTER COLUMN "location_x_unit" DROP NOT NULL,
    ALTER COLUMN "location_y_unit" DROP NOT NULL,
    ALTER COLUMN "location_z_unit" DROP NOT NULL;
UPDATE "pdm"."Telescope" SET "location_refpos" = 'GEOCENTER';

ALTER TABLE "pdm"."ExpectedSpectralLine"
    ALTER COLUMN "restFrequency_value" DROP NOT NULL,
    ALTER COLUMN "restFrequency_unit" DROP NOT NULL;
ALTER TABLE "pdm"."Filter"
    ALTER COLUMN "frequencyCoverage_end_value" DROP NOT NULL,
    ALTER COLUMN "frequencyCoverage_end_unit" DROP NOT NULL,
    ALTER COLUMN "frequencyCoverage_start_value" DROP NOT NULL,
    ALTER COLUMN "frequencyCoverage_start_unit" DROP NOT NULL,
    ALTER COLUMN "frequencyCoverage_spectralResolution_value" DROP NOT NULL,
    ALTER COLUMN "frequencyCoverage_spectralResolution_unit" DROP NOT NULL,
    ALTER COLUMN "frequencyCoverage_isSkyFrequency" DROP NOT NULL;
ALTER TABLE "pdm"."PerformanceParameters"
    ALTER COLUMN "representativeSpectralPoint_value" DROP NOT NULL,
    ALTER COLUMN "representativeSpectralPoint_unit" DROP NOT NULL;

ALTER TABLE "pdm"."ProposalCycle"
    ALTER COLUMN "observationSessionStart" TYPE timestamptz USING "observationSessionStart" AT TIME ZONE 'UTC',
    ALTER COLUMN "observationSessionEnd" TYPE timestamptz USING "observationSessionEnd" AT TIME ZONE 'UTC',
    ALTER COLUMN "submissionDeadline" TYPE timestamptz USING "submissionDeadline" AT TIME ZONE 'UTC';
ALTER TABLE "pdm"."ProposalReview"
    ALTER COLUMN "reviewDate" TYPE timestamptz USING "reviewDate" AT TIME ZONE 'UTC';
ALTER TABLE "pdm"."SubmittedProposal"
    ALTER COLUMN "submissionDate" TYPE timestamptz USING "submissionDate" AT TIME ZONE 'UTC',
    ALTER COLUMN "reviewsCompleteDate" TYPE timestamptz USING "reviewsCompleteDate" AT TIME ZONE 'UTC';
ALTER TABLE "pdm"."TimingWindow"
    ALTER COLUMN "startTime" TYPE timestamptz USING "startTime" AT TIME ZONE 'UTC',
    ALTER COLUMN "endTime" TYPE timestamptz USING "endTime" AT TIME ZONE 'UTC';

ALTER TABLE "public"."Observation_Target" SET SCHEMA "pdm";
ALTER TABLE "public"."ObservationConfiguration_Observation" SET SCHEMA "pdm";

CREATE TABLE "pdm"."SinglePointing" (
    "ID" bigint NOT NULL PRIMARY KEY REFERENCES "pdm"."Field" ("ID"),
    "centre_equinox" double precision,
    "centre_referenceFrame" varchar(255),
    "centre_sourceCoordinates" spoint
);
