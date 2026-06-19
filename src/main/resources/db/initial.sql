-- liquibase formatted sql

-- changeset pharriso:1764756371789-1 splitStatements:false
CREATE TABLE "coords"."CoordSpace" ("ID" BIGINT NOT NULL, "CoordSpace_SUBTYPE" VARCHAR(32) NOT NULL, "handedness" VARCHAR(255), CONSTRAINT "CoordSpace_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-2 splitStatements:false
CREATE TABLE "coords"."CoordFrame" ("ID" BIGINT NOT NULL, "refDirection_ID" BIGINT, "refPosition_ID" BIGINT, "CoordFrame_SUBTYPE" VARCHAR(32) NOT NULL, "equinox" VARCHAR(255), "planetaryEphem" VARCHAR(255), "spaceRefFrame" VARCHAR(255), "timescale" VARCHAR(255), CONSTRAINT "CoordFrame_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-3 splitStatements:false
CREATE TABLE "coords"."CoordSys" ("ASTROCOORDSYSTEM_ID" BIGINT, "ID" BIGINT NOT NULL, "coordSpace_ID" BIGINT, "frame_ID" BIGINT, "pixelSpace_ID" BIGINT, "CoordSys_SUBTYPE" VARCHAR(32) NOT NULL, CONSTRAINT "CoordSys_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-4 splitStatements:false
CREATE TABLE "pdm"."AbstractProposal" ("ID" BIGINT NOT NULL, "scientificJustification_ID" BIGINT, "technicalJustification_ID" BIGINT, "AbstractProposal_SUBTYPE" VARCHAR(64) NOT NULL, "kind" VARCHAR(255) NOT NULL, "summary" VARCHAR(255) NOT NULL, "title" VARCHAR(255) NOT NULL, CONSTRAINT "AbstractProposal_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-5 splitStatements:false
CREATE TABLE "pdm"."CalibrationObservation" ("ID" BIGINT NOT NULL, "intent" VARCHAR(255) NOT NULL, CONSTRAINT "CalibrationObservation_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-6 splitStatements:false
CREATE TABLE "pdm"."CommitteeMember" ("ID" BIGINT NOT NULL, "TAC_ID" BIGINT, "member" BIGINT NOT NULL, "role" VARCHAR(255) NOT NULL, CONSTRAINT "CommitteeMember_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-7 splitStatements:false
CREATE TABLE "pdm"."Filter" ("frequencyCoverage_end_value" FLOAT8 NOT NULL, "frequencyCoverage_isSkyFrequency" BOOLEAN NOT NULL, "frequencyCoverage_spectralResolution_value" FLOAT8 NOT NULL, "frequencyCoverage_start_value" FLOAT8 NOT NULL, "ID" BIGINT NOT NULL, "description" VARCHAR(255) NOT NULL, "frequencyCoverage_end_unit" VARCHAR(255) NOT NULL, "frequencyCoverage_spectralResolution_unit" VARCHAR(255) NOT NULL, "frequencyCoverage_start_unit" VARCHAR(255) NOT NULL, "name" VARCHAR(255) NOT NULL, "polarization" VARCHAR(255), CONSTRAINT "Filter_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-8 splitStatements:false
CREATE TABLE "pdm"."Instrument" ("ID" BIGINT NOT NULL, "OBSERVATORY_ID" BIGINT, "description" VARCHAR(255), "kind" VARCHAR(255) NOT NULL, "name" VARCHAR(255) NOT NULL, "reference" VARCHAR(255), "wikiId" VARCHAR(255), CONSTRAINT "Instrument_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-9 splitStatements:false
CREATE TABLE "pdm"."Investigator" ("forPhD" BOOLEAN, "ABSTRACTPROPOSAL_ID" BIGINT, "ID" BIGINT NOT NULL, "person" BIGINT NOT NULL, "type" VARCHAR(255) NOT NULL, CONSTRAINT "Investigator_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-10 splitStatements:false
CREATE TABLE "pdm"."Justification" ("ID" BIGINT NOT NULL, "text" VARCHAR(4096) NOT NULL, "format" VARCHAR(255) NOT NULL, CONSTRAINT "Justification_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-11 splitStatements:false
CREATE TABLE "pdm"."ScienceSpectralWindow" ("spectralWindowSetup_end_value" FLOAT8, "spectralWindowSetup_isSkyFrequency" BOOLEAN, "spectralWindowSetup_spectralResolution_value" FLOAT8, "spectralWindowSetup_start_value" FLOAT8, "spectrum_ORDER" INTEGER, "ID" BIGINT NOT NULL, "TECHNICALGOAL_ID" BIGINT, "polarization" VARCHAR(255), "spectralWindowSetup_end_unit" VARCHAR(255), "spectralWindowSetup_spectralResolution_unit" VARCHAR(255), "spectralWindowSetup_start_unit" VARCHAR(255), CONSTRAINT "ScienceSpectralWindow_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-12 splitStatements:false
CREATE TABLE "pdm"."ObservingMode" ("ID" BIGINT NOT NULL, "PROPOSALCYCLE_ID" BIGINT, "backend" BIGINT NOT NULL, "filter_ID" BIGINT, "instrument" BIGINT NOT NULL, "telescope" BIGINT NOT NULL, "description" VARCHAR(255) NOT NULL, "name" VARCHAR(255) NOT NULL, CONSTRAINT "ObservingMode_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-13 splitStatements:false
CREATE TABLE "pdm"."ProposalCycle" ("ID" BIGINT NOT NULL, "availableResources_ID" BIGINT, "observationSessionEnd" TIMESTAMP WITHOUT TIME ZONE NOT NULL, "observationSessionStart" TIMESTAMP WITHOUT TIME ZONE NOT NULL, "observatory" BIGINT NOT NULL, "submissionDeadline" TIMESTAMP WITHOUT TIME ZONE, "tac_ID" BIGINT, "code" VARCHAR(255), "instructions" VARCHAR(255), "title" VARCHAR(255) NOT NULL, CONSTRAINT "ProposalCycle_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-14 splitStatements:false
CREATE TABLE "pdm"."ResourceBlock" ("ID" BIGINT NOT NULL, "mode" BIGINT NOT NULL, "resource_ID" BIGINT, "ResourceBlock_SUBTYPE" VARCHAR(64) NOT NULL, CONSTRAINT "ResourceBlock_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-15 splitStatements:false
CREATE TABLE "pdm"."TechnicalGoal" ("technicalGoals_ORDER" INTEGER, "ABSTRACTPROPOSAL_ID" BIGINT, "ID" BIGINT NOT NULL, "performance_ID" BIGINT, CONSTRAINT "TechnicalGoal_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-16 splitStatements:false
CREATE TABLE "public"."SubjectMap" ("_id" BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL, "person_id" BIGINT, "uid" VARCHAR(255), CONSTRAINT "SubjectMap_pkey" PRIMARY KEY ("_id"));

-- changeset pharriso:1764756371789-17 splitStatements:false
ALTER TABLE "coords"."CoordFrame" ADD CONSTRAINT "CoordFrame_refDirection_ID_key" UNIQUE ("refDirection_ID");

-- changeset pharriso:1764756371789-18 splitStatements:false
ALTER TABLE "coords"."CoordFrame" ADD CONSTRAINT "CoordFrame_refPosition_ID_key" UNIQUE ("refPosition_ID");

-- changeset pharriso:1764756371789-19 splitStatements:false
ALTER TABLE "coords"."CoordSys" ADD CONSTRAINT "CoordSys_coordSpace_ID_key" UNIQUE ("coordSpace_ID");

-- changeset pharriso:1764756371789-20 splitStatements:false
ALTER TABLE "coords"."CoordSys" ADD CONSTRAINT "CoordSys_frame_ID_key" UNIQUE ("frame_ID");

-- changeset pharriso:1764756371789-21 splitStatements:false
ALTER TABLE "coords"."CoordSys" ADD CONSTRAINT "CoordSys_pixelSpace_ID_key" UNIQUE ("pixelSpace_ID");

-- changeset pharriso:1764756371789-22 splitStatements:false
ALTER TABLE "pdm"."AbstractProposal" ADD CONSTRAINT "AbstractProposal_scientificJustification_ID_key" UNIQUE ("scientificJustification_ID");

-- changeset pharriso:1764756371789-23 splitStatements:false
ALTER TABLE "pdm"."AbstractProposal" ADD CONSTRAINT "AbstractProposal_technicalJustification_ID_key" UNIQUE ("technicalJustification_ID");

-- changeset pharriso:1764756371789-24 splitStatements:false
ALTER TABLE "pdm"."ObservingMode" ADD CONSTRAINT "ObservingMode_filter_ID_key" UNIQUE ("filter_ID");

-- changeset pharriso:1764756371789-25 splitStatements:false
ALTER TABLE "pdm"."ProposalCycle" ADD CONSTRAINT "ProposalCycle_availableResources_ID_key" UNIQUE ("availableResources_ID");

-- changeset pharriso:1764756371789-26 splitStatements:false
ALTER TABLE "pdm"."ProposalCycle" ADD CONSTRAINT "ProposalCycle_tac_ID_key" UNIQUE ("tac_ID");

-- changeset pharriso:1764756371789-27 splitStatements:false
ALTER TABLE "pdm"."ResourceBlock" ADD CONSTRAINT "ResourceBlock_resource_ID_key" UNIQUE ("resource_ID");

-- changeset pharriso:1764756371789-28 splitStatements:false
ALTER TABLE "pdm"."TechnicalGoal" ADD CONSTRAINT "TechnicalGoal_performance_ID_key" UNIQUE ("performance_ID");

-- changeset pharriso:1764756371789-29 splitStatements:false
ALTER TABLE "public"."SubjectMap" ADD CONSTRAINT "SubjectMap_person_id_key" UNIQUE ("person_id");

-- changeset pharriso:1764756371789-30 splitStatements:false
CREATE SEQUENCE  IF NOT EXISTS "public"."AbstractProposal_SEQ" AS bigint START WITH 1 INCREMENT BY 50 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1;

-- changeset pharriso:1764756371789-31 splitStatements:false
CREATE SEQUENCE  IF NOT EXISTS "public"."AllocatedProposal_SEQ" AS bigint START WITH 1 INCREMENT BY 50 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1;

-- changeset pharriso:1764756371789-32 splitStatements:false
CREATE SEQUENCE  IF NOT EXISTS "public"."AllocationGrade_SEQ" AS bigint START WITH 1 INCREMENT BY 50 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1;

-- changeset pharriso:1764756371789-33 splitStatements:false
CREATE SEQUENCE  IF NOT EXISTS "public"."AvailableResources_SEQ" AS bigint START WITH 1 INCREMENT BY 50 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1;

-- changeset pharriso:1764756371789-34 splitStatements:false
CREATE SEQUENCE  IF NOT EXISTS "public"."Axis_SEQ" AS bigint START WITH 1 INCREMENT BY 50 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1;

-- changeset pharriso:1764756371789-35 splitStatements:false
CREATE SEQUENCE  IF NOT EXISTS "public"."Backend_SEQ" AS bigint START WITH 1 INCREMENT BY 50 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1;

-- changeset pharriso:1764756371789-36 splitStatements:false
CREATE SEQUENCE  IF NOT EXISTS "public"."CommitteeMember_SEQ" AS bigint START WITH 1 INCREMENT BY 50 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1;

-- changeset pharriso:1764756371789-37 splitStatements:false
CREATE SEQUENCE  IF NOT EXISTS "public"."CoordFrame_SEQ" AS bigint START WITH 1 INCREMENT BY 50 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1;

-- changeset pharriso:1764756371789-38 splitStatements:false
CREATE SEQUENCE  IF NOT EXISTS "public"."CoordSpace_SEQ" AS bigint START WITH 1 INCREMENT BY 50 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1;

-- changeset pharriso:1764756371789-39 splitStatements:false
CREATE SEQUENCE  IF NOT EXISTS "public"."CoordSys_SEQ" AS bigint START WITH 1 INCREMENT BY 50 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1;

-- changeset pharriso:1764756371789-40 splitStatements:false
CREATE SEQUENCE  IF NOT EXISTS "public"."ExpectedSpectralLine_SEQ" AS bigint START WITH 1 INCREMENT BY 50 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1;

-- changeset pharriso:1764756371789-41 splitStatements:false
CREATE SEQUENCE  IF NOT EXISTS "public"."Field_SEQ" AS bigint START WITH 1 INCREMENT BY 50 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1;

-- changeset pharriso:1764756371789-42 splitStatements:false
CREATE SEQUENCE  IF NOT EXISTS "public"."Filter_SEQ" AS bigint START WITH 1 INCREMENT BY 50 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1;

-- changeset pharriso:1764756371789-43 splitStatements:false
CREATE SEQUENCE  IF NOT EXISTS "public"."Instrument_SEQ" AS bigint START WITH 1 INCREMENT BY 50 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1;

-- changeset pharriso:1764756371789-44 splitStatements:false
CREATE SEQUENCE  IF NOT EXISTS "public"."Investigator_SEQ" AS bigint START WITH 1 INCREMENT BY 50 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1;

-- changeset pharriso:1764756371789-45 splitStatements:false
CREATE SEQUENCE  IF NOT EXISTS "public"."Justification_SEQ" AS bigint START WITH 1 INCREMENT BY 50 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1;

-- changeset pharriso:1764756371789-46 splitStatements:false
CREATE SEQUENCE  IF NOT EXISTS "public"."ObservationConfiguration_SEQ" AS bigint START WITH 1 INCREMENT BY 50 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1;

-- changeset pharriso:1764756371789-47 splitStatements:false
CREATE SEQUENCE  IF NOT EXISTS "public"."Observation_SEQ" AS bigint START WITH 1 INCREMENT BY 50 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1;

-- changeset pharriso:1764756371789-48 splitStatements:false
CREATE SEQUENCE  IF NOT EXISTS "public"."ObservingConstraint_SEQ" AS bigint START WITH 1 INCREMENT BY 50 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1;

-- changeset pharriso:1764756371789-49 splitStatements:false
CREATE SEQUENCE  IF NOT EXISTS "public"."ObservingMode_SEQ" AS bigint START WITH 1 INCREMENT BY 50 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1;

-- changeset pharriso:1764756371789-50 splitStatements:false
CREATE SEQUENCE  IF NOT EXISTS "public"."ObservingPlatform_SEQ" AS bigint START WITH 1 INCREMENT BY 50 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1;

-- changeset pharriso:1764756371789-51 splitStatements:false
CREATE SEQUENCE  IF NOT EXISTS "public"."Organization_SEQ" AS bigint START WITH 1 INCREMENT BY 50 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1;

-- changeset pharriso:1764756371789-52 splitStatements:false
CREATE SEQUENCE  IF NOT EXISTS "public"."PerformanceParameters_SEQ" AS bigint START WITH 1 INCREMENT BY 50 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1;

-- changeset pharriso:1764756371789-53 splitStatements:false
CREATE SEQUENCE  IF NOT EXISTS "public"."Person_SEQ" AS bigint START WITH 1 INCREMENT BY 50 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1;

-- changeset pharriso:1764756371789-54 splitStatements:false
CREATE SEQUENCE  IF NOT EXISTS "public"."ProposalCycle_SEQ" AS bigint START WITH 1 INCREMENT BY 50 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1;

-- changeset pharriso:1764756371789-55 splitStatements:false
CREATE SEQUENCE  IF NOT EXISTS "public"."ProposalReview_SEQ" AS bigint START WITH 1 INCREMENT BY 50 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1;

-- changeset pharriso:1764756371789-56 splitStatements:false
CREATE SEQUENCE  IF NOT EXISTS "public"."RefLocation_SEQ" AS bigint START WITH 1 INCREMENT BY 50 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1;

-- changeset pharriso:1764756371789-57 splitStatements:false
CREATE SEQUENCE  IF NOT EXISTS "public"."RelatedProposal_SEQ" AS bigint START WITH 1 INCREMENT BY 50 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1;

-- changeset pharriso:1764756371789-58 splitStatements:false
CREATE SEQUENCE  IF NOT EXISTS "public"."ResourceBlock_SEQ" AS bigint START WITH 1 INCREMENT BY 50 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1;

-- changeset pharriso:1764756371789-59 splitStatements:false
CREATE SEQUENCE  IF NOT EXISTS "public"."ResourceType_SEQ" AS bigint START WITH 1 INCREMENT BY 50 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1;

-- changeset pharriso:1764756371789-60 splitStatements:false
CREATE SEQUENCE  IF NOT EXISTS "public"."Resource_SEQ" AS bigint START WITH 1 INCREMENT BY 50 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1;

-- changeset pharriso:1764756371789-61 splitStatements:false
CREATE SEQUENCE  IF NOT EXISTS "public"."Reviewer_SEQ" AS bigint START WITH 1 INCREMENT BY 50 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1;

-- changeset pharriso:1764756371789-62 splitStatements:false
CREATE SEQUENCE  IF NOT EXISTS "public"."ScienceSpectralWindow_SEQ" AS bigint START WITH 1 INCREMENT BY 50 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1;

-- changeset pharriso:1764756371789-63 splitStatements:false
CREATE SEQUENCE  IF NOT EXISTS "public"."SupportingDocument_SEQ" AS bigint START WITH 1 INCREMENT BY 50 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1;

-- changeset pharriso:1764756371789-64 splitStatements:false
CREATE SEQUENCE  IF NOT EXISTS "public"."TAC_SEQ" AS bigint START WITH 1 INCREMENT BY 50 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1;

-- changeset pharriso:1764756371789-65 splitStatements:false
CREATE SEQUENCE  IF NOT EXISTS "public"."Target_SEQ" AS bigint START WITH 1 INCREMENT BY 50 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1;

-- changeset pharriso:1764756371789-66 splitStatements:false
CREATE SEQUENCE  IF NOT EXISTS "public"."TechnicalGoal_SEQ" AS bigint START WITH 1 INCREMENT BY 50 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1;

-- changeset pharriso:1764756371789-67 splitStatements:false
CREATE SEQUENCE  IF NOT EXISTS "public"."TelescopeArrayMember_SEQ" AS bigint START WITH 1 INCREMENT BY 50 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1;

-- changeset pharriso:1764756371789-68 splitStatements:false
CREATE TABLE "coords"."Axis" ("cyclic" BOOLEAN, "domainMax_value" FLOAT8, "domainMin_value" FLOAT8, "length" INTEGER, "COORDSPACE_ID" BIGINT, "ID" BIGINT NOT NULL, "Axis_SUBTYPE" VARCHAR(32) NOT NULL, "domainMax_unit" VARCHAR(255), "domainMin_unit" VARCHAR(255), "name" VARCHAR(255), CONSTRAINT "Axis_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-69 splitStatements:false
CREATE TABLE "coords"."RefLocation" ("ID" BIGINT NOT NULL, "position_coordSys" BIGINT, "velocity_coordSys" BIGINT, "RefLocation_SUBTYPE" VARCHAR(32) NOT NULL, "epoch" VARCHAR(255), "position" VARCHAR(255), CONSTRAINT "RefLocation_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-70 splitStatements:false
CREATE TABLE "pdm"."AllocatedBlock" ("allocation_ORDER" INTEGER, "ALLOCATEDPROPOSAL_ID" BIGINT, "ID" BIGINT NOT NULL, "grade" BIGINT NOT NULL, CONSTRAINT "AllocatedBlock_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-71 splitStatements:false
CREATE TABLE "pdm"."AllocatedProposal" ("ID" BIGINT NOT NULL, "PROPOSALCYCLE_ID" BIGINT, "submitted" BIGINT NOT NULL, CONSTRAINT "AllocatedProposal_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-72 splitStatements:false
CREATE TABLE "pdm"."AllocationGrade" ("ID" BIGINT NOT NULL, "PROPOSALCYCLE_ID" BIGINT, "description" VARCHAR(255) NOT NULL, "name" VARCHAR(255) NOT NULL, CONSTRAINT "AllocationGrade_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-73 splitStatements:false
CREATE TABLE "pdm"."AvailableResources" ("ID" BIGINT NOT NULL, CONSTRAINT "AvailableResources_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-74 splitStatements:false
CREATE TABLE "pdm"."Backend" ("parallel" BOOLEAN NOT NULL, "ID" BIGINT NOT NULL, "OBSERVATORY_ID" BIGINT, "name" VARCHAR(255) NOT NULL, CONSTRAINT "Backend_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-75 splitStatements:false
CREATE TABLE "pdm"."CelestialTarget" ("parallax_value" FLOAT8, "pmDec_value" FLOAT8, "pmRA_value" FLOAT8, "sourceCoordinates_lat_value" FLOAT8 NOT NULL, "sourceCoordinates_lon_value" FLOAT8 NOT NULL, "sourceVelocity_value" FLOAT8, "ID" BIGINT NOT NULL, "sourceCoordinates_coordSys" BIGINT, "parallax_unit" VARCHAR(255), "pmDec_unit" VARCHAR(255), "pmRA_unit" VARCHAR(255), "positionEpoch" VARCHAR(255) NOT NULL, "sourceCoordinates_lat_unit" VARCHAR(255) NOT NULL, "sourceCoordinates_lon_unit" VARCHAR(255) NOT NULL, "sourceVelocity_unit" VARCHAR(255), CONSTRAINT "CelestialTarget_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-76 splitStatements:false
CREATE TABLE "pdm"."Ellipse" ("pAMajor_value" FLOAT8 NOT NULL, "semiMajor_value" FLOAT8 NOT NULL, "semiMinor_value" FLOAT8 NOT NULL, "ID" BIGINT NOT NULL, "pAMajor_unit" VARCHAR(255) NOT NULL, "semiMajor_unit" VARCHAR(255) NOT NULL, "semiMinor_unit" VARCHAR(255) NOT NULL, CONSTRAINT "Ellipse_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-77 splitStatements:false
CREATE TABLE "pdm"."ExpectedSpectralLine" ("expectedSpectralLine_ORDER" INTEGER, "restFrequency_value" FLOAT8 NOT NULL, "ID" BIGINT NOT NULL, "SCIENCESPECTRALWINDOW_ID" BIGINT, "description" VARCHAR(255) NOT NULL, "restFrequency_unit" VARCHAR(255) NOT NULL, "splatalogId" VARCHAR(255) NOT NULL, "transition" VARCHAR(255), CONSTRAINT "ExpectedSpectralLine_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-78 splitStatements:false
CREATE TABLE "pdm"."Field" ("ABSTRACTPROPOSAL_ID" BIGINT, "ID" BIGINT NOT NULL, "Field_SUBTYPE" VARCHAR(64) NOT NULL, "name" VARCHAR(255) NOT NULL, CONSTRAINT "Field_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-79 splitStatements:false
CREATE TABLE "pdm"."Observation" ("observations_ORDER" INTEGER, "ABSTRACTPROPOSAL_ID" BIGINT, "ID" BIGINT NOT NULL, "field" BIGINT NOT NULL, "technicalGoal" BIGINT NOT NULL, "Observation_SUBTYPE" VARCHAR(64) NOT NULL, CONSTRAINT "Observation_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-80 splitStatements:false
CREATE TABLE "pdm"."ObservationConfiguration" ("ID" BIGINT NOT NULL, "SUBMITTEDPROPOSAL_ID" BIGINT, "mode" BIGINT NOT NULL, CONSTRAINT "ObservationConfiguration_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-81 splitStatements:false
CREATE TABLE "pdm"."Observatory" ("ID" BIGINT NOT NULL, "homePage" VARCHAR(255), CONSTRAINT "Observatory_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-82 splitStatements:false
CREATE TABLE "pdm"."ObservingConstraint" ("constraints_ORDER" INTEGER, "ID" BIGINT NOT NULL, "OBSERVATION_ID" BIGINT, "ObservingConstraint_SUBTYPE" VARCHAR(64) NOT NULL, CONSTRAINT "ObservingConstraint_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-83 splitStatements:false
CREATE TABLE "pdm"."ObservingPlatform" ("ID" BIGINT NOT NULL, "ObservingPlatform_SUBTYPE" VARCHAR(64) NOT NULL, CONSTRAINT "ObservingPlatform_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-84 splitStatements:false
CREATE TABLE "pdm"."ObservingProposal" ("ID" BIGINT NOT NULL, CONSTRAINT "ObservingProposal_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-85 splitStatements:false
CREATE TABLE "pdm"."Organization" ("ID" BIGINT NOT NULL, "Organization_SUBTYPE" VARCHAR(64) NOT NULL, "address" VARCHAR(255) NOT NULL, "ivoid" VARCHAR(255), "name" VARCHAR(255) NOT NULL, "wikiId" VARCHAR(255), CONSTRAINT "Organization_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-86 splitStatements:false
CREATE TABLE "pdm"."PerformanceParameters" ("desiredAngularResolution_value" FLOAT8, "desiredDynamicRange_value" FLOAT8, "desiredLargestScale_value" FLOAT8, "desiredSensitivity_value" FLOAT8, "representativeSpectralPoint_value" FLOAT8 NOT NULL, "ID" BIGINT NOT NULL, "desiredAngularResolution_unit" VARCHAR(255), "desiredDynamicRange_unit" VARCHAR(255), "desiredLargestScale_unit" VARCHAR(255), "desiredSensitivity_unit" VARCHAR(255), "representativeSpectralPoint_unit" VARCHAR(255) NOT NULL, CONSTRAINT "PerformanceParameters_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-87 splitStatements:false
CREATE TABLE "pdm"."Person" ("ID" BIGINT NOT NULL, "homeInstitute" BIGINT NOT NULL, "eMail" VARCHAR(255) NOT NULL, "fullName" VARCHAR(255) NOT NULL, "orcidId" VARCHAR(255), CONSTRAINT "Person_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-88 splitStatements:false
CREATE TABLE "pdm"."Point" ("ID" BIGINT NOT NULL, "centre_coordSys" BIGINT, CONSTRAINT "Point_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-89 splitStatements:false
CREATE TABLE "pdm"."PointingConstaint" ("ID" BIGINT NOT NULL, CONSTRAINT "PointingConstaint_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-90 splitStatements:false
CREATE TABLE "pdm"."Polygon" ("ID" BIGINT NOT NULL, CONSTRAINT "Polygon_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-91 splitStatements:false
CREATE TABLE "pdm"."ProposalReview" ("score" FLOAT8 NOT NULL, "technicalFeasibility" BOOLEAN NOT NULL, "ID" BIGINT NOT NULL, "SUBMITTEDPROPOSAL_ID" BIGINT, "reviewDate" TIMESTAMP WITHOUT TIME ZONE NOT NULL, "reviewer" BIGINT NOT NULL, "comment" VARCHAR(2048) NOT NULL, CONSTRAINT "ProposalReview_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-92 splitStatements:false
CREATE TABLE "pdm"."RelatedProposal" ("ABSTRACTPROPOSAL_ID" BIGINT, "ID" BIGINT NOT NULL, "proposal" BIGINT NOT NULL, CONSTRAINT "RelatedProposal_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-93 splitStatements:false
CREATE TABLE "pdm"."Resource" ("amount" FLOAT8 NOT NULL, "resources_ORDER" INTEGER, "AVAILABLERESOURCES_ID" BIGINT, "ID" BIGINT NOT NULL, "type" BIGINT NOT NULL, CONSTRAINT "Resource_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-94 splitStatements:false
CREATE TABLE "pdm"."ResourceType" ("ID" BIGINT NOT NULL, "name" VARCHAR(255) NOT NULL, "unit" VARCHAR(255) NOT NULL, CONSTRAINT "ResourceType_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-95 splitStatements:false
CREATE TABLE "pdm"."Reviewer" ("ID" BIGINT NOT NULL, "person" BIGINT NOT NULL, CONSTRAINT "Reviewer_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-96 splitStatements:false
CREATE TABLE "pdm"."SimultaneityConstraint" ("ID" BIGINT NOT NULL, CONSTRAINT "SimultaneityConstraint_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-97 splitStatements:false
CREATE TABLE "pdm"."SolarSystemTarget" ("ID" BIGINT NOT NULL, CONSTRAINT "SolarSystemTarget_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-98 splitStatements:false
CREATE TABLE "pdm"."SubmittedProposal" ("successful" BOOLEAN NOT NULL, "ID" BIGINT NOT NULL, "PROPOSALCYCLE_ID" BIGINT, "reviewsCompleteDate" TIMESTAMP WITHOUT TIME ZONE NOT NULL, "submissionDate" TIMESTAMP WITHOUT TIME ZONE NOT NULL, "proposalCode" VARCHAR(255) NOT NULL, CONSTRAINT "SubmittedProposal_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-99 splitStatements:false
CREATE TABLE "pdm"."SupportingDocument" ("supportingDocuments_ORDER" INTEGER, "ABSTRACTPROPOSAL_ID" BIGINT, "ID" BIGINT NOT NULL, "location" VARCHAR(255) NOT NULL, "title" VARCHAR(255) NOT NULL, CONSTRAINT "SupportingDocument_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-100 splitStatements:false
CREATE TABLE "pdm"."TAC" ("ID" BIGINT NOT NULL, CONSTRAINT "TAC_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-101 splitStatements:false
CREATE TABLE "pdm"."Target" ("ABSTRACTPROPOSAL_ID" BIGINT, "ID" BIGINT NOT NULL, "Target_SUBTYPE" VARCHAR(64) NOT NULL, "sourceName" VARCHAR(255) NOT NULL, CONSTRAINT "Target_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-102 splitStatements:false
CREATE TABLE "pdm"."TargetField" ("ID" BIGINT NOT NULL, CONSTRAINT "TargetField_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-103 splitStatements:false
CREATE TABLE "pdm"."TargetObservation" ("ID" BIGINT NOT NULL, CONSTRAINT "TargetObservation_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-104 splitStatements:false
CREATE TABLE "pdm"."Telescope" ("location_x_value" FLOAT8 NOT NULL, "location_y_value" FLOAT8 NOT NULL, "location_z_value" FLOAT8 NOT NULL, "ID" BIGINT NOT NULL, "OBSERVATORY_ID" BIGINT, "location_coordSys" BIGINT, "location_x_unit" VARCHAR(255) NOT NULL, "location_y_unit" VARCHAR(255) NOT NULL, "location_z_unit" VARCHAR(255) NOT NULL, "name" VARCHAR(255) NOT NULL, "wikiId" VARCHAR(255), CONSTRAINT "Telescope_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-105 splitStatements:false
CREATE TABLE "pdm"."TelescopeArray" ("ID" BIGINT NOT NULL, "OBSERVATORY_ID" BIGINT, "name" VARCHAR(255) NOT NULL, CONSTRAINT "TelescopeArray_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-106 splitStatements:false
CREATE TABLE "pdm"."TelescopeArrayMember" ("ID" BIGINT NOT NULL, "TELESCOPEARRAY_ID" BIGINT, "telescope" BIGINT NOT NULL, CONSTRAINT "TelescopeArrayMember_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-107 splitStatements:false
CREATE TABLE "pdm"."TimingConstraint" ("isAvoidConstraint" BOOLEAN, "ID" BIGINT NOT NULL, "note" VARCHAR(255), CONSTRAINT "TimingConstraint_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-108 splitStatements:false
CREATE TABLE "pdm"."TimingWindow" ("ID" BIGINT NOT NULL, "endTime" TIMESTAMP WITHOUT TIME ZONE, "startTime" TIMESTAMP WITHOUT TIME ZONE, CONSTRAINT "TimingWindow_pkey" PRIMARY KEY ("ID"));

-- changeset pharriso:1764756371789-109 splitStatements:false
CREATE TABLE "public"."ObservationConfiguration_Observation" ("ObservationConfiguration_ID" BIGINT NOT NULL, "observation_ID" BIGINT NOT NULL);

-- changeset pharriso:1764756371789-110 splitStatements:false
CREATE TABLE "public"."Observation_Target" ("Observation_ID" BIGINT NOT NULL, "target_ID" BIGINT NOT NULL);

-- changeset pharriso:1764756371789-111 splitStatements:false
CREATE TABLE "public"."Polygon_points" ("points_lat_value" FLOAT8, "points_lon_value" FLOAT8, "containerId" BIGINT NOT NULL, "points_coordSys" BIGINT, "points_lat_unit" VARCHAR(255), "points_lon_unit" VARCHAR(255));

-- changeset pharriso:1764756371789-112 splitStatements:false
ALTER TABLE "coords"."CoordFrame" ADD CONSTRAINT "FK5g6eymwmagke7475lk25u0lj5" FOREIGN KEY ("refPosition_ID") REFERENCES "coords"."RefLocation" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-113 splitStatements:false
ALTER TABLE "coords"."RefLocation" ADD CONSTRAINT "FK70rt29klb1v4otp21c4h1te1g" FOREIGN KEY ("position_coordSys") REFERENCES "coords"."CoordSys" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-114 splitStatements:false
ALTER TABLE "coords"."Axis" ADD CONSTRAINT "FKbo11opyp1csmxjyly4uk0q4ul" FOREIGN KEY ("COORDSPACE_ID") REFERENCES "coords"."CoordSpace" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-115 splitStatements:false
ALTER TABLE "coords"."CoordSys" ADD CONSTRAINT "FKcnb4ae4b0p3blmi3gd6y6cy3o" FOREIGN KEY ("coordSpace_ID") REFERENCES "coords"."CoordSpace" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-116 splitStatements:false
ALTER TABLE "coords"."CoordSys" ADD CONSTRAINT "FKfqnfwy8wdnv9ekex9e5ukq4wt" FOREIGN KEY ("frame_ID") REFERENCES "coords"."CoordFrame" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-117 splitStatements:false
ALTER TABLE "coords"."RefLocation" ADD CONSTRAINT "FKg0pwn54yk2d0p501kui11p9c0" FOREIGN KEY ("velocity_coordSys") REFERENCES "coords"."CoordSys" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-118 splitStatements:false
ALTER TABLE "coords"."CoordSys" ADD CONSTRAINT "FKni0w4gitato7rhboph62tyqxk" FOREIGN KEY ("ASTROCOORDSYSTEM_ID") REFERENCES "coords"."CoordSys" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-119 splitStatements:false
ALTER TABLE "coords"."CoordFrame" ADD CONSTRAINT "FKpd6a5tktqqvn7qrmh1r9o6368" FOREIGN KEY ("refDirection_ID") REFERENCES "coords"."RefLocation" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-120 splitStatements:false
ALTER TABLE "coords"."CoordSys" ADD CONSTRAINT "FKs9dcgbc69g9w20fjxkkyc9i7g" FOREIGN KEY ("pixelSpace_ID") REFERENCES "coords"."CoordSpace" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-121 splitStatements:false
ALTER TABLE "pdm"."TelescopeArray" ADD CONSTRAINT "FK1e1rgtssp0o3co8euellpuqj9" FOREIGN KEY ("ID") REFERENCES "pdm"."ObservingPlatform" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-122 splitStatements:false
ALTER TABLE "pdm"."ProposalCycle" ADD CONSTRAINT "FK33jf654m8bdbymf2nhjdqteka" FOREIGN KEY ("availableResources_ID") REFERENCES "pdm"."AvailableResources" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-123 splitStatements:false
ALTER TABLE "pdm"."ProposalCycle" ADD CONSTRAINT "FK3gw62aicy25lb499wmct1ide4" FOREIGN KEY ("tac_ID") REFERENCES "pdm"."TAC" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-124 splitStatements:false
ALTER TABLE "pdm"."Investigator" ADD CONSTRAINT "FK3jwb1nm21cpmsa9va4s6la570" FOREIGN KEY ("person") REFERENCES "pdm"."Person" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-125 splitStatements:false
ALTER TABLE "pdm"."SolarSystemTarget" ADD CONSTRAINT "FK43fej7bak44ffg5td2gik81w5" FOREIGN KEY ("ID") REFERENCES "pdm"."Target" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-126 splitStatements:false
ALTER TABLE "pdm"."ProposalReview" ADD CONSTRAINT "FK47wchhgmknle8gua1ne3nnrjr" FOREIGN KEY ("reviewer") REFERENCES "pdm"."Reviewer" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-127 splitStatements:false
ALTER TABLE "pdm"."Observatory" ADD CONSTRAINT "FK4dyky5x7p6tkansbhu7y45n3x" FOREIGN KEY ("ID") REFERENCES "pdm"."Organization" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-128 splitStatements:false
ALTER TABLE "pdm"."Observation" ADD CONSTRAINT "FK54rkr1oeuxfjr411bm243e95d" FOREIGN KEY ("ABSTRACTPROPOSAL_ID") REFERENCES "pdm"."AbstractProposal" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-129 splitStatements:false
ALTER TABLE "pdm"."Telescope" ADD CONSTRAINT "FK5ct4onam3xqh3eqxifrsq4x78" FOREIGN KEY ("ID") REFERENCES "pdm"."ObservingPlatform" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-130 splitStatements:false
ALTER TABLE "pdm"."ProposalReview" ADD CONSTRAINT "FK5jek0h7qmoxfv4k0diaxv7mhm" FOREIGN KEY ("SUBMITTEDPROPOSAL_ID") REFERENCES "pdm"."SubmittedProposal" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-131 splitStatements:false
ALTER TABLE "pdm"."AllocatedProposal" ADD CONSTRAINT "FK5of84tjeqef68lrk9fx6xauuj" FOREIGN KEY ("submitted") REFERENCES "pdm"."SubmittedProposal" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-132 splitStatements:false
ALTER TABLE "pdm"."TechnicalGoal" ADD CONSTRAINT "FK6f45xnvlg0uhsqoh1hry3t018" FOREIGN KEY ("performance_ID") REFERENCES "pdm"."PerformanceParameters" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-133 splitStatements:false
ALTER TABLE "pdm"."SubmittedProposal" ADD CONSTRAINT "FK78guh9bgirgxh6bhw50v8c7d" FOREIGN KEY ("PROPOSALCYCLE_ID") REFERENCES "pdm"."ProposalCycle" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-134 splitStatements:false
ALTER TABLE "pdm"."AbstractProposal" ADD CONSTRAINT "FK78jal9lhl27iej0b7brrobf3o" FOREIGN KEY ("technicalJustification_ID") REFERENCES "pdm"."Justification" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-135 splitStatements:false
ALTER TABLE "pdm"."CelestialTarget" ADD CONSTRAINT "FK7rin0aebpa0vpw2rnd9xvo7qo" FOREIGN KEY ("ID") REFERENCES "pdm"."Target" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-136 splitStatements:false
ALTER TABLE "pdm"."ObservingMode" ADD CONSTRAINT "FK7y4mapib6l5smcxiuup5y5j8o" FOREIGN KEY ("telescope") REFERENCES "pdm"."ObservingPlatform" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-137 splitStatements:false
ALTER TABLE "pdm"."TelescopeArrayMember" ADD CONSTRAINT "FK89lglrpdl29qcor5ad1lnv8ws" FOREIGN KEY ("telescope") REFERENCES "pdm"."Telescope" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-138 splitStatements:false
ALTER TABLE "pdm"."Telescope" ADD CONSTRAINT "FK90397g8ru0u9e2g802re7ot7o" FOREIGN KEY ("location_coordSys") REFERENCES "coords"."CoordSys" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-139 splitStatements:false
ALTER TABLE "pdm"."CommitteeMember" ADD CONSTRAINT "FK90j6hq8p4mfe744mimdfd3rmr" FOREIGN KEY ("TAC_ID") REFERENCES "pdm"."TAC" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-140 splitStatements:false
ALTER TABLE "pdm"."AllocatedBlock" ADD CONSTRAINT "FK95o2gu7ytej2mxcpg4f9ao1p2" FOREIGN KEY ("grade") REFERENCES "pdm"."AllocationGrade" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-141 splitStatements:false
ALTER TABLE "pdm"."Observation" ADD CONSTRAINT "FK9d518u6vyeunjetq93g3n8lgo" FOREIGN KEY ("field") REFERENCES "pdm"."Field" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-142 splitStatements:false
ALTER TABLE "pdm"."RelatedProposal" ADD CONSTRAINT "FK9h38bvp97sb3ibjw042texq5h" FOREIGN KEY ("ABSTRACTPROPOSAL_ID") REFERENCES "pdm"."AbstractProposal" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-143 splitStatements:false
ALTER TABLE "pdm"."Ellipse" ADD CONSTRAINT "FK9ptf9hni6h956irj22uk97rcw" FOREIGN KEY ("ID") REFERENCES "pdm"."Field" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-144 splitStatements:false
ALTER TABLE "pdm"."ProposalCycle" ADD CONSTRAINT "FKb2yha42s0kkulqnwmm23um3yd" FOREIGN KEY ("observatory") REFERENCES "pdm"."Observatory" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-145 splitStatements:false
ALTER TABLE "pdm"."AllocatedBlock" ADD CONSTRAINT "FKbh0ou3ys4f2y0wwwrraj9s4cn" FOREIGN KEY ("ID") REFERENCES "pdm"."ResourceBlock" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-146 splitStatements:false
ALTER TABLE "pdm"."TechnicalGoal" ADD CONSTRAINT "FKbixfcj8c0h41hn7hjk5o3vhry" FOREIGN KEY ("ABSTRACTPROPOSAL_ID") REFERENCES "pdm"."AbstractProposal" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-147 splitStatements:false
ALTER TABLE "pdm"."Person" ADD CONSTRAINT "FKbu2ysxtoodiciramxk7i1d7gi" FOREIGN KEY ("homeInstitute") REFERENCES "pdm"."Organization" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-148 splitStatements:false
ALTER TABLE "pdm"."Field" ADD CONSTRAINT "FKbwnqbqdqe5mecvqa2elltpao5" FOREIGN KEY ("ABSTRACTPROPOSAL_ID") REFERENCES "pdm"."AbstractProposal" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-149 splitStatements:false
ALTER TABLE "pdm"."RelatedProposal" ADD CONSTRAINT "FKbxg5o1ls733yjihhr135eoxsq" FOREIGN KEY ("proposal") REFERENCES "pdm"."AbstractProposal" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-150 splitStatements:false
ALTER TABLE "pdm"."AllocatedBlock" ADD CONSTRAINT "FKc40sye36w461gk4tt82ya9k7x" FOREIGN KEY ("ALLOCATEDPROPOSAL_ID") REFERENCES "pdm"."AllocatedProposal" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-151 splitStatements:false
ALTER TABLE "pdm"."AllocatedProposal" ADD CONSTRAINT "FKd792pmw47a58j2xkb5nv1k1lq" FOREIGN KEY ("PROPOSALCYCLE_ID") REFERENCES "pdm"."ProposalCycle" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-152 splitStatements:false
ALTER TABLE "pdm"."TargetField" ADD CONSTRAINT "FKd9ewwrkocwhs84c1223yem44u" FOREIGN KEY ("ID") REFERENCES "pdm"."Field" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-153 splitStatements:false
ALTER TABLE "pdm"."TimingConstraint" ADD CONSTRAINT "FKdcqxqlk2y4ib9cgho367pfv3s" FOREIGN KEY ("ID") REFERENCES "pdm"."ObservingConstraint" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-154 splitStatements:false
ALTER TABLE "pdm"."PointingConstaint" ADD CONSTRAINT "FKdjlde6pl7smumfksvba81chv4" FOREIGN KEY ("ID") REFERENCES "pdm"."ObservingConstraint" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-155 splitStatements:false
ALTER TABLE "pdm"."Observation" ADD CONSTRAINT "FKdotjn5m8njktgkmlpyh1u2bwv" FOREIGN KEY ("technicalGoal") REFERENCES "pdm"."TechnicalGoal" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-156 splitStatements:false
ALTER TABLE "pdm"."Resource" ADD CONSTRAINT "FKdx99cx9vol2mat6rcbsc6e8rt" FOREIGN KEY ("AVAILABLERESOURCES_ID") REFERENCES "pdm"."AvailableResources" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-157 splitStatements:false
ALTER TABLE "pdm"."ObservingMode" ADD CONSTRAINT "FKe4w39rgfkgdcf31xpf15icftm" FOREIGN KEY ("backend") REFERENCES "pdm"."Backend" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-158 splitStatements:false
ALTER TABLE "pdm"."Investigator" ADD CONSTRAINT "FKfmhmirxyh706j43a5174g49at" FOREIGN KEY ("ABSTRACTPROPOSAL_ID") REFERENCES "pdm"."AbstractProposal" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-159 splitStatements:false
ALTER TABLE "pdm"."ExpectedSpectralLine" ADD CONSTRAINT "FKg7y3t5r7v56r5fsw7j4mqhenh" FOREIGN KEY ("SCIENCESPECTRALWINDOW_ID") REFERENCES "pdm"."ScienceSpectralWindow" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-160 splitStatements:false
ALTER TABLE "pdm"."CalibrationObservation" ADD CONSTRAINT "FKge5n690d5max1xhaph3rj920c" FOREIGN KEY ("ID") REFERENCES "pdm"."Observation" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-161 splitStatements:false
ALTER TABLE "pdm"."Telescope" ADD CONSTRAINT "FKgha7oxkud1hs3tq81mhgld3hd" FOREIGN KEY ("OBSERVATORY_ID") REFERENCES "pdm"."Observatory" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-162 splitStatements:false
ALTER TABLE "pdm"."Point" ADD CONSTRAINT "FKgof568xe81t9nccip9tbqw094" FOREIGN KEY ("centre_coordSys") REFERENCES "coords"."CoordSys" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-163 splitStatements:false
ALTER TABLE "pdm"."ObservationConfiguration" ADD CONSTRAINT "FKgu3dcf1sxb5ui68tq9u9tb1dv" FOREIGN KEY ("SUBMITTEDPROPOSAL_ID") REFERENCES "pdm"."SubmittedProposal" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-164 splitStatements:false
ALTER TABLE "pdm"."SimultaneityConstraint" ADD CONSTRAINT "FKh9e5wxe3fh1w6665575df9y75" FOREIGN KEY ("ID") REFERENCES "pdm"."TimingConstraint" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-165 splitStatements:false
ALTER TABLE "pdm"."CommitteeMember" ADD CONSTRAINT "FKi770s8i2riavg11jwrfao084b" FOREIGN KEY ("member") REFERENCES "pdm"."Reviewer" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-166 splitStatements:false
ALTER TABLE "pdm"."ResourceBlock" ADD CONSTRAINT "FKivjwb61pnx063q342ahvdgr5n" FOREIGN KEY ("mode") REFERENCES "pdm"."ObservingMode" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-167 splitStatements:false
ALTER TABLE "pdm"."AllocationGrade" ADD CONSTRAINT "FKj1jkyonwwx7xeud42p7ik975w" FOREIGN KEY ("PROPOSALCYCLE_ID") REFERENCES "pdm"."ProposalCycle" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-168 splitStatements:false
ALTER TABLE "pdm"."Backend" ADD CONSTRAINT "FKjhf0er3xqvpb3640j0grlxjyf" FOREIGN KEY ("OBSERVATORY_ID") REFERENCES "pdm"."Observatory" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-169 splitStatements:false
ALTER TABLE "pdm"."Resource" ADD CONSTRAINT "FKk0uvh5p6cr8van4fk9haw3oc7" FOREIGN KEY ("type") REFERENCES "pdm"."ResourceType" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-170 splitStatements:false
ALTER TABLE "pdm"."SubmittedProposal" ADD CONSTRAINT "FKklhr712diwu6wnrhy1r5swgw0" FOREIGN KEY ("ID") REFERENCES "pdm"."AbstractProposal" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-171 splitStatements:false
ALTER TABLE "pdm"."ObservingMode" ADD CONSTRAINT "FKkm51ghhtf6vca9fc1mye3qio9" FOREIGN KEY ("instrument") REFERENCES "pdm"."Instrument" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-172 splitStatements:false
ALTER TABLE "pdm"."ObservingConstraint" ADD CONSTRAINT "FKkuq65qssxmyp6413ddgtgpgdj" FOREIGN KEY ("OBSERVATION_ID") REFERENCES "pdm"."Observation" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-173 splitStatements:false
ALTER TABLE "pdm"."TelescopeArrayMember" ADD CONSTRAINT "FKlf5xmcxw7akjuxoh5m9ll0oj0" FOREIGN KEY ("TELESCOPEARRAY_ID") REFERENCES "pdm"."TelescopeArray" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-174 splitStatements:false
ALTER TABLE "pdm"."SupportingDocument" ADD CONSTRAINT "FKli4h36kt4kcx468ppflrrxbpw" FOREIGN KEY ("ABSTRACTPROPOSAL_ID") REFERENCES "pdm"."AbstractProposal" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-175 splitStatements:false
ALTER TABLE "pdm"."TimingWindow" ADD CONSTRAINT "FKm4j9qru31oug27sm4icwc6y4x" FOREIGN KEY ("ID") REFERENCES "pdm"."TimingConstraint" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-176 splitStatements:false
ALTER TABLE "pdm"."Point" ADD CONSTRAINT "FKmpdw28kyjhcncyy6g8g3cut6y" FOREIGN KEY ("ID") REFERENCES "pdm"."Field" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-177 splitStatements:false
ALTER TABLE "pdm"."ScienceSpectralWindow" ADD CONSTRAINT "FKmx2sm679926huiy3mpw7nedea" FOREIGN KEY ("TECHNICALGOAL_ID") REFERENCES "pdm"."TechnicalGoal" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-178 splitStatements:false
ALTER TABLE "pdm"."ObservingMode" ADD CONSTRAINT "FKnt4tn2q4w250jd21aab4x107c" FOREIGN KEY ("PROPOSALCYCLE_ID") REFERENCES "pdm"."ProposalCycle" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-179 splitStatements:false
ALTER TABLE "pdm"."AbstractProposal" ADD CONSTRAINT "FKo753fqs3hddd9p8yeh4l17p6y" FOREIGN KEY ("scientificJustification_ID") REFERENCES "pdm"."Justification" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-180 splitStatements:false
ALTER TABLE "pdm"."CelestialTarget" ADD CONSTRAINT "FKook4snv2p0ckumni4r6iwys1w" FOREIGN KEY ("sourceCoordinates_coordSys") REFERENCES "coords"."CoordSys" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-181 splitStatements:false
ALTER TABLE "pdm"."TargetObservation" ADD CONSTRAINT "FKpiwgw3vy54wqelr98rb9we53t" FOREIGN KEY ("ID") REFERENCES "pdm"."Observation" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-182 splitStatements:false
ALTER TABLE "pdm"."ResourceBlock" ADD CONSTRAINT "FKq1jwnb129v7yetwk5baucwr54" FOREIGN KEY ("resource_ID") REFERENCES "pdm"."Resource" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-183 splitStatements:false
ALTER TABLE "pdm"."ObservingMode" ADD CONSTRAINT "FKq7992fapno4l751wif2ojmowa" FOREIGN KEY ("filter_ID") REFERENCES "pdm"."Filter" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-184 splitStatements:false
ALTER TABLE "pdm"."ObservationConfiguration" ADD CONSTRAINT "FKqx17gnmtnm6lqiq7plhbk9ah" FOREIGN KEY ("mode") REFERENCES "pdm"."ObservingMode" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-185 splitStatements:false
ALTER TABLE "pdm"."Instrument" ADD CONSTRAINT "FKr82ormqqjuhb9ymnov2d9dlee" FOREIGN KEY ("OBSERVATORY_ID") REFERENCES "pdm"."Observatory" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-186 splitStatements:false
ALTER TABLE "pdm"."Reviewer" ADD CONSTRAINT "FKrbiie2ioba5222a2mxl250xv4" FOREIGN KEY ("person") REFERENCES "pdm"."Person" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-187 splitStatements:false
ALTER TABLE "pdm"."Polygon" ADD CONSTRAINT "FKrr6n0319hmsl0oqne7hdvdwfk" FOREIGN KEY ("ID") REFERENCES "pdm"."Field" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-188 splitStatements:false
ALTER TABLE "pdm"."ObservingProposal" ADD CONSTRAINT "FKs72yi6y9y35ugneskjm88fylk" FOREIGN KEY ("ID") REFERENCES "pdm"."AbstractProposal" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-189 splitStatements:false
ALTER TABLE "pdm"."TelescopeArray" ADD CONSTRAINT "FKsricyi5n7pxgpr12kljrtl0q5" FOREIGN KEY ("OBSERVATORY_ID") REFERENCES "pdm"."Observatory" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-190 splitStatements:false
ALTER TABLE "pdm"."Target" ADD CONSTRAINT "FKt6wd1513bcurjgopx4jbyeirn" FOREIGN KEY ("ABSTRACTPROPOSAL_ID") REFERENCES "pdm"."AbstractProposal" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-191 splitStatements:false
ALTER TABLE "public"."ObservationConfiguration_Observation" ADD CONSTRAINT "FK5fxi7y2r3ogeqese6v3cijpmb" FOREIGN KEY ("observation_ID") REFERENCES "pdm"."Observation" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-192 splitStatements:false
ALTER TABLE "public"."Polygon_points" ADD CONSTRAINT "FK5raw1i6j46rxgupoc6dxrf2u1" FOREIGN KEY ("containerId") REFERENCES "pdm"."Polygon" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-193 splitStatements:false
ALTER TABLE "public"."Polygon_points" ADD CONSTRAINT "FKdkoq07m8d17sgo87oe83l7x5v" FOREIGN KEY ("points_coordSys") REFERENCES "coords"."CoordSys" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-194 splitStatements:false
ALTER TABLE "public"."Observation_Target" ADD CONSTRAINT "FKee9rjrgbg159d20vbyc0duc9m" FOREIGN KEY ("target_ID") REFERENCES "pdm"."Target" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-195 splitStatements:false
ALTER TABLE "public"."ObservationConfiguration_Observation" ADD CONSTRAINT "FKovbvgsbg9sv7it2167l2r19v0" FOREIGN KEY ("ObservationConfiguration_ID") REFERENCES "pdm"."ObservationConfiguration" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-196 splitStatements:false
ALTER TABLE "public"."Observation_Target" ADD CONSTRAINT "FKqjr3ry7uig2bi8h6wk4bfofpm" FOREIGN KEY ("Observation_ID") REFERENCES "pdm"."Observation" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset pharriso:1764756371789-197 splitStatements:false
ALTER TABLE "public"."SubjectMap" ADD CONSTRAINT "FKtogn9po7gau8ne7wd82v3agqh" FOREIGN KEY ("person_id") REFERENCES "pdm"."Person" ("ID") ON UPDATE NO ACTION ON DELETE NO ACTION;

