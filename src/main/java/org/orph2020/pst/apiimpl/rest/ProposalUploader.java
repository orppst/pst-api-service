package org.orph2020.pst.apiimpl.rest;

import jakarta.ws.rs.WebApplicationException;
import org.ivoa.dm.ivoa.Ivorn;
import org.ivoa.dm.ivoa.RealQuantity;
import org.ivoa.dm.ivoa.StringIdentifier;
import org.ivoa.dm.proposal.prop.CelestialTarget;
import org.ivoa.dm.proposal.prop.Investigator;
import org.ivoa.dm.proposal.prop.InvestigatorKind;
import org.ivoa.dm.proposal.prop.Justification;
import org.ivoa.dm.proposal.prop.ObservingProposal;
import org.ivoa.dm.proposal.prop.Organization;
import org.ivoa.dm.proposal.prop.PerformanceParameters;
import org.ivoa.dm.proposal.prop.Person;
import org.ivoa.dm.proposal.prop.ProposalKind;
import org.ivoa.dm.proposal.prop.ScienceSpectralWindow;
import org.ivoa.dm.proposal.prop.SpectralWindowSetup;
import org.ivoa.dm.proposal.prop.Target;
import org.ivoa.dm.proposal.prop.TargetField;
import org.ivoa.dm.proposal.prop.TargetObservation;
import org.ivoa.dm.proposal.prop.TechnicalGoal;
import org.ivoa.dm.proposal.prop.TextFormats;
import org.ivoa.dm.proposal.prop.TimingWindow;
import org.ivoa.dm.proposal.prop.WikiDataId;
import org.ivoa.dm.stc.coords.Epoch;
import org.ivoa.dm.stc.coords.EquatorialPoint;
import org.ivoa.dm.stc.coords.PolStateEnum;
import org.ivoa.vodml.stdtypes.Unit;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.json.JSONArray;
import org.json.JSONObject;
import org.orph2020.pst.common.json.ObjectIdentifier;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@SuppressWarnings("VulnerableCodeUsages")
public class ProposalUploader {

    // the logger.
    private static final Logger logger =
        Logger.getLogger(ProposalUploader.class.getName());

    // hard coded filename for the proposal in json format.
    private static final String PROPOSAL_JSON_FILE_NAME = "proposal.json";

    // string to replace in time stamps.
    private static final String TIMESTAMP_CORRUPTION = "+0000";

    // string replacement to match ISO-8601 standard.
    private static final String TIMESTAMP_REPLACEMENT = "Z";

    /**
     * default constructor.
     */
    public ProposalUploader() {}

    /**
     * entrance method for uploading a proposal.
     *
     * @param fileUpload zip file.
     * @param updateSubmittedFlag if we should remove the submitted flag.
     * @param proposalResource: the proposal resource.
     * @param investigatorResource: the investigator resource.
     * @param observationResource: the observation resource.
     * @param personResource: the person resource.
     * @param technicalGoalResource: the technical resource.
     * @param supportingDocumentResource: the supporting document resource.
     * @throws WebApplicationException when:
     * no file is found: 400, or when some data appears that were not ready
     * for, or when the zip fails to read the file, or when some parsers fail.
     */
    public void uploadProposal(
            FileUpload fileUpload, String updateSubmittedFlag,
            ProposalResource proposalResource, PersonResource personResource,
            InvestigatorResource investigatorResource,
            TechnicalGoalResource technicalGoalResource,
            ObservationResource observationResource,
            SupportingDocumentResource supportingDocumentResource)
            throws WebApplicationException {
        byte[] proposalData = this.readFile(
            fileUpload, ProposalUploader.PROPOSAL_JSON_FILE_NAME);

        HashMap<Long, Target> targetIdMapToReal = new HashMap<>();
        HashMap<Long, TechnicalGoal> technicalMapToReal = new HashMap<>();

        // check for failed read in.
        if (proposalData == null) {
            throw new WebApplicationException("No proposal data was found.");
        }

        // convert to json for accessibility purposes.
        /* TODO NOTE, it seems this has vulnerabilities see below:
           Vulnerable API usage
            CVE-2022-45689 7.5 Out-of-bounds Write vulnerability
            CVE-2022-45690 7.5 Out-of-bounds Write vulnerability
            CVE-2022-45689 7.5 Out-of-bounds Write vulnerability
            CVE-2022-45690 7.5 Out-of-bounds Write vulnerability
         these are being ignored here for speed effectiveness.
         But it needs looking at.
         */
        JSONObject proposalJSON = new JSONObject(new String(proposalData));

        // logging for debug purposes.
        logger.info("proposal json looks like this: ");
        logger.info(proposalJSON);
        logger.info("proposal json ended");

        // create a proposal and save it so that we have a working id.
        ObservingProposal newProposal = new ObservingProposal();
        logger.info(newProposal);

        // save proposal specific data items.
        this.saveProposalSpecific(
            newProposal, proposalJSON,
            Boolean.parseBoolean(updateSubmittedFlag), proposalResource,
            personResource, investigatorResource);
        this.saveProposalTargets(
            newProposal, proposalJSON, targetIdMapToReal, proposalResource);
        this.saveProposalTechnicals(
            newProposal, proposalJSON, technicalMapToReal,
            technicalGoalResource);
        this.saveProposalObservations(
            newProposal, proposalJSON, targetIdMapToReal, technicalMapToReal,
            observationResource);
        this.saveJustifications(newProposal, proposalJSON);
        this.saveProposalDocuments(
            newProposal, proposalJSON, fileUpload, supportingDocumentResource);
    }

    /**
     * saves the justifications.
     * @param newProposal: the new proposal to persist state in.
     * @param proposalJSON: the json object holding new data.
     */
    private void saveJustifications(
            ObservingProposal newProposal, JSONObject proposalJSON) {
        JSONObject scientificJustification =
            proposalJSON.optJSONObject("scientificJustification");
        JSONObject technicalJustification =
            proposalJSON.optJSONObject("technicalJustification");

        // attempt to fill in scientificJustification
        if (scientificJustification != null) {
            newProposal.setScientificJustification(new Justification(
                scientificJustification.getString("text"), TextFormats.RST));
            throw new WebApplicationException(
                "scientificJustification have not been implemented correctly." +
                    " Please contact devs."
            );
        }

        // attempt to fill in technicalJustification
        if (technicalJustification != null) {
            newProposal.setTechnicalJustification(new Justification(
                technicalJustification.getString("text"), TextFormats.RST));
            throw new WebApplicationException(
                "technicalJustification have not been implemented correctly." +
                    " Please contact devs."
            );
        }
    }

    /**
     * saves the proposal specific items, such as:
     *      (title, abstract, investigators).
     * @param newProposal: the new proposal to persist state in.
     * @param proposalJSON: the json object holding new data.
     * @param modifySubmitted: boolean stating if the submitted field should
     *                      be changed.
     * @param proposalResource: the proposal resource for saving to database.
     * @param personResource: the person resource to save to the database.
     * @param investigatorResource: the saving of investigators to the database.
     */
    private void saveProposalSpecific(
            ObservingProposal newProposal, JSONObject proposalJSON,
            boolean modifySubmitted, ProposalResource proposalResource,
            PersonResource personResource,
            InvestigatorResource investigatorResource) {
        /////////////////// simple strings.
        newProposal.setSummary(proposalJSON.getString("summary"));
        newProposal.setKind(
            ProposalKind.valueOf(proposalJSON.getString("kind")));
        newProposal.setTitle(proposalJSON.getString("title"));
        proposalResource.persistObject(newProposal);

        // check for the submitted flag.
        if (modifySubmitted) {
            newProposal.setSubmitted(proposalJSON.getBoolean("submitted"));
        }

        /*
        //////////////// arrays.
         Due to the vulnerabilities described below, wrapped with a try catch
         Vulnerable API usage
         Cx08fcacc9-cb99 7.5 Uncaught Exception vulnerability
         Cx08fcacc9-cb99 7.5 Uncaught Exception vulnerability
        */
        try {
            // array of related proposals.
            JSONArray relatedProposals =
                proposalJSON.optJSONArray("relatedProposals");
            if(relatedProposals != null && relatedProposals.length() != 0) {
                throw new WebApplicationException(
                    "Currently related proposals are not supported." +
                        " Please contact the devs.");
            }

            // array of investigators.
            JSONArray investigators =
                proposalJSON.optJSONArray("investigators");
            if(investigators != null && investigators.length() != 0) {
                for (int investigatorIndex = 0;
                        investigatorIndex < investigators.length();
                        investigatorIndex++) {
                    JSONObject investigator =
                        investigators.getJSONObject(investigatorIndex);
                    newProposal.addToInvestigators(createNewInvestigator(
                        investigator, newProposal.getId(),
                        personResource, investigatorResource));
                }
            }
        } catch (Exception e) {
            logger.error("failed with error: " + e.getMessage());
            //e.printStackTrace();
            throw new WebApplicationException(e.getMessage());
        }
    }

    /**
     * creates a new investigator from a json investigator.
     * @param investigator json investigator.
     * @param proposalCode: the associated proposal code.
     * @param personResource: the person resource to save to database.
     * @param investigatorResource: the investigator resource to save
     *                           to database.
     * @return new investigator object.
     */
    private Investigator createNewInvestigator(
            JSONObject investigator, Long proposalCode,
            PersonResource personResource,
            InvestigatorResource investigatorResource) {
        // create new investigator.
        Investigator newInvestigator = new Investigator();

        if (investigator.get("forPhD") != null) {
            newInvestigator.setForPhD(investigator.optBoolean("forPhD"));
        }
        newInvestigator.setType(InvestigatorKind.valueOf(
            investigator.getString("type")));

        // create person
        Person newPerson = new Person();
        JSONObject jsonPerson = investigator.getJSONObject("person");
        newPerson.setEMail(jsonPerson.getString("eMail"));
        newPerson.setFullName(jsonPerson.getString("fullName"));
        newPerson.setOrcidId(new StringIdentifier(
            jsonPerson.getJSONObject("orcidId").getString("value")));
        newPerson.setXmlId(String.valueOf(jsonPerson.getInt("_id")));

        // create new institute
        JSONObject orgJSON = jsonPerson.optJSONObject("homeInstitute");

        //TODO: Ensure this org is in the database and correctly referenced
        if(orgJSON == null) {
            logger.info("Home institute is a reference, do nothing");
        } else {
            Organization org = new Organization();
            org.setAddress(orgJSON.getString("address"));
            org.setIvoid(new Ivorn(
                    orgJSON.getJSONObject("ivoid").getString("value")));
            org.setName(orgJSON.getString("name"));
            org.setXmlId(String.valueOf(orgJSON.getInt("_id")));

            if (orgJSON.optString("wikiId") != null) {
                org.setWikiId(new WikiDataId(orgJSON.optString("wikiId")));
            }

            // update investigator and person
            newPerson.setHomeInstitute(org);
        }
        newInvestigator.setPerson(newPerson);

        // update database positions if required
        if (foundPerson(
                newPerson.getFullName(), newPerson.getOrcidId().value(),
                personResource)) {
            personResource.createPerson(newPerson);
        }
        investigatorResource.addPersonAsInvestigator(
            proposalCode, newInvestigator);

        return newInvestigator;
    }

    /**
     * tries to find a person within the database already.
     *
     * @param fullName: person full name to find.
     * @param orcid: the orcid of said person.
     * @param personResource: the person resource to save to the database.
     * @return boolean, true if found, false otherwise.
     */
    private boolean foundPerson(
            String fullName, String orcid, PersonResource personResource) {
        logger.info("full name = " + fullName);
        List<ObjectIdentifier> possiblePeeps =
            personResource.getPeople(fullName);
        for (ObjectIdentifier possiblePeep: possiblePeeps) {
            // TODO not sure about this check.
            if (possiblePeep.code != null && possiblePeep.code.equals(orcid)) {
                return true;
            }
        }
        return false;
    }

    /**
     * saves the proposal's targets. NOTE only CelestialTarget's
     * are working at moment.
     * @param newProposal: the new proposal to persist state in.
     * @param proposalJSON: the json object holding new data.
     * @param targetIdMapToReal: map for the json and real ids to real targets.
     * @param proposalResource: the proposal resource to save to the database.
     */
    private void saveProposalTargets(
            ObservingProposal newProposal, JSONObject proposalJSON,
            HashMap<Long, Target> targetIdMapToReal,
            ProposalResource proposalResource) {
        // array of targets.
        JSONArray targets = proposalJSON.optJSONArray("targets");
        if(targets != null && targets.length() != 0) {
            for (int targetIndex = 0; targetIndex < targets.length();
                    targetIndex++) {
                JSONObject jsonTarget = targets.getJSONObject(targetIndex);
                String type = jsonTarget.getString("@type");
                switch (type) {
                    case "proposal:CelestialTarget":
                        this.createCelestialTarget(
                            jsonTarget, newProposal, targetIdMapToReal,
                            proposalResource);
                        break;
                    case "proposal:SolarSystemTarget":
                        throw new WebApplicationException(
                            "dont know what to do with solar system type." +
                                " Please contact the devs.");
                    default:
                        throw new WebApplicationException(
                            "dont recognise this target type." +
                                " Please contact the devs.");
                }
            }
        }
    }

    /**
     * builds a celestial target.
     *
     * @param newProposal: the new proposal to persist state in.
     * @param jsonTarget: the json object holding new target data.
     * @param targetIdMapToReal: map for the json and real ids to real targets.
     * @param proposalResource: the proposal resource to save to the database.
     */
    private void createCelestialTarget(
            JSONObject jsonTarget, ObservingProposal newProposal,
            HashMap<Long, Target> targetIdMapToReal,
            ProposalResource proposalResource) {
        CelestialTarget cTarget = new CelestialTarget();

        // name
        cTarget.setSourceName(
            jsonTarget.getString("sourceName"));

        // set the states.
        this.setParallax(jsonTarget, cTarget);
        this.setSourceCoordinates(jsonTarget, cTarget);
        this.setPmRA(jsonTarget, cTarget);
        this.setSourceVelocity(jsonTarget, cTarget);
        this.setPmDec(jsonTarget, cTarget);
        this.setPositionEpoch(jsonTarget, cTarget);

        // save to database
        cTarget = proposalResource.addNewChildObject(
            newProposal, cTarget, newProposal::addToTargets);

        // track ids for when observations come about.
        targetIdMapToReal.put(
            jsonTarget.getLong("_id"), cTarget);
    }

    /**
     * sets the target position epoch.
     *
     * @param jsonTarget the json target containing the position epoch.
     * @param cTarget the target.
     */
    private void setPositionEpoch(
        JSONObject jsonTarget, CelestialTarget cTarget) {
        JSONObject jsonPE = jsonTarget.optJSONObject("positionEpoch");
        if (jsonPE != null) {
            cTarget.setPositionEpoch(new Epoch(jsonPE.getString("value")));
        }
    }

    /**
     * sets the target source velocity.
     *
     * @param jsonTarget the json target containing the source velocity.
     * @param cTarget the target.
     */
    private void setSourceVelocity(
            JSONObject jsonTarget, CelestialTarget cTarget) {
        JSONObject jsonSV = jsonTarget.optJSONObject("sourceVelocity");
        if (jsonSV != null) {
            RealQuantity sv = this.createRealQuantity(jsonSV);
            cTarget.setSourceVelocity(sv);
        }
    }

    /**
     * sets the target pmdec.
     *
     * @param jsonTarget the json target containing the pmdec.
     * @param cTarget the target.
     */
    private void setPmDec(
        JSONObject jsonTarget, CelestialTarget cTarget) {
        JSONObject jsonPmdec = jsonTarget.optJSONObject("pmDec");
        if (jsonPmdec != null) {
            RealQuantity pmdec = this.createRealQuantity(jsonPmdec);
            cTarget.setPmDec(pmdec);
        }
    }

    /**
     * sets the target pmra.
     *
     * @param jsonTarget the json target containing the pmra.
     * @param cTarget the target.
     */
    private void setPmRA(
            JSONObject jsonTarget, CelestialTarget cTarget) {
        JSONObject jsonPmra = jsonTarget.optJSONObject("pmRA");
        if (jsonPmra != null) {
            RealQuantity pmra = this.createRealQuantity(jsonPmra);
            cTarget.setPmRA(pmra);
        }
    }

    /**
     * creates anew real quantity.
     *
     * @param json the json to convert into a real quantity.
     * @return a real quantity object.
     */
    private RealQuantity createRealQuantity(JSONObject json) {
        if (json != null) {
            return new RealQuantity(
                json.getDouble("value"),
                new Unit(json.getJSONObject("unit").getString("value"))
            );
        }
        return null;
    }

    /**
     * sets the source coords.
     *
     * @param jsonTarget the json target containing the source coords.
     * @param cTarget the target.
     */
    private void setSourceCoordinates(
            JSONObject jsonTarget, CelestialTarget cTarget) {
        // source coordinates
        JSONObject jsonSourceCoordinates =
            jsonTarget.optJSONObject("sourceCoordinates");
        if (jsonSourceCoordinates != null) {
            String type = jsonSourceCoordinates.getString("@type");
            logger.info("type is " + type);
            switch(type) {
                case "coords:EquatorialPoint":
                    this.createEquatorialPoint(jsonSourceCoordinates, cTarget);
                    break;
                case "coords:RealCartesianPoint":
                case "coords:SphericalPoint":
                case "coords:GenericPoint":
                case "coords:LonLatPoint":
                case "coords:CartesianPoint":
                    throw new WebApplicationException(
                        "Have not implemented this type of point. Please" +
                            " contact the devs.");
                default:
                    throw new WebApplicationException(
                        "Dont recognise this source coordinate type." +
                            " Please contact the devs.");
            }
        }
    }

    /**
     * creates a Equatorial Point.
     *
     * @param jsonSourceCoordinates: json data
     * @param cTarget: the target to add the point to.
     */
    private void createEquatorialPoint(
            JSONObject jsonSourceCoordinates, CelestialTarget cTarget) {
        EquatorialPoint coords = new EquatorialPoint();

        // coords sys
        String coordSystem =
            jsonSourceCoordinates.optString("coordSys");
        logger.info("coordSystem is " + coordSystem);
        if (coordSystem != null && !coordSystem.isEmpty()) {
            throw new WebApplicationException(
                "dont know how to handle coordsystems. " +
                    "Please contact the devs");
        }

        // longitude
        JSONObject jsonLongitude =
            jsonSourceCoordinates.optJSONObject("lon");
        if (jsonLongitude != null) {
            RealQuantity lon =
                this.createRealQuantity(jsonLongitude);
            coords.setLon(lon);
        }

        // latitude
        JSONObject jsonLatitude =
            jsonSourceCoordinates.optJSONObject("lon");
        if (jsonLatitude != null) {
            RealQuantity lat =
                this.createRealQuantity(jsonLatitude);
            coords.setLat(lat);
        }

        // update target
        cTarget.setSourceCoordinates(coords);
    }

    /**
     * sets parallax.
     *
     * @param jsonTarget the json containing the target.
     * @param cTarget the new target.
     */
    private void setParallax(JSONObject jsonTarget, CelestialTarget cTarget) {
        JSONObject jsonParallax =
            jsonTarget.optJSONObject("parallax");
        if (jsonParallax != null) {
            RealQuantity parallax = this.createRealQuantity(jsonParallax);
            cTarget.setParallax(parallax);
        }
    }

    /**
     * saves the proposal's technical goals.
     * @param newProposal: the new proposal to persist state in.
     * @param proposalJSON: the json object holding new data.
     * @param technicalMapToReal: map between ids for the technicals in json
     *                            vs real technical goals.
     * @param technicalGoalResource: the resource to save technical goals to
     *                             the database.
     */
    private void saveProposalTechnicals(
            ObservingProposal newProposal, JSONObject proposalJSON,
            HashMap<Long, TechnicalGoal> technicalMapToReal,
            TechnicalGoalResource technicalGoalResource) {
        JSONArray jsonTechnicalGoals =
            proposalJSON.getJSONArray("technicalGoals");
        if (jsonTechnicalGoals != null && jsonTechnicalGoals.length() != 0) {
            for (int tgIndex = 0; tgIndex < jsonTechnicalGoals.length();
                 tgIndex++) {
                JSONObject jsonTechnicalGoal =
                    jsonTechnicalGoals.getJSONObject(tgIndex);

                // create the basic technical goal.
                TechnicalGoal tg = technicalGoalResource.addNewChildObject(
                    newProposal, createNewTechnicalGoal(
                    jsonTechnicalGoal), newProposal::addToTechnicalGoals);
                handleSpectralWindows(
                    jsonTechnicalGoal, tg, technicalGoalResource);

                // update maps.
                technicalMapToReal.put(jsonTechnicalGoal.getLong("_id"), tg);
            }
        }
    }

    /**
     * builds a new technical goal from json.
     *
     * @param jsonTechnicalGoal the json to build the technical goal from.
     * @return the new technical goal.
     */
    private TechnicalGoal createNewTechnicalGoal(
            JSONObject jsonTechnicalGoal) {
        TechnicalGoal goal = new TechnicalGoal();

        // performance params.
        JSONObject jsonPerformanceParams =
            jsonTechnicalGoal.getJSONObject("performance");
        if (jsonPerformanceParams != null) {
            PerformanceParameters pp = new PerformanceParameters();
            setDesiredDynamicRange(pp, jsonPerformanceParams);
            setDesiredLargestScale(pp, jsonPerformanceParams);
            setRepresentativeSpectralPoint(pp, jsonPerformanceParams);
            setDesiredSensitivity(pp, jsonPerformanceParams);
            setDesiredAngularResolution(pp, jsonPerformanceParams);
            goal.setPerformance(pp);
        }
        return goal;
    }

    /**
     * handles windows.
     *
     * @param jsonTechnicalGoal: the json.
     * @param goal: the technical goal.
     * @param technicalGoalResource: the resource to save technical goals to
     *                            the database.
     */
    private void handleSpectralWindows(
            JSONObject jsonTechnicalGoal, TechnicalGoal goal,
            TechnicalGoalResource technicalGoalResource) {
        // handle the windows.
        JSONArray jsonSpectrum = jsonTechnicalGoal.getJSONArray("spectrum");
        if (jsonSpectrum != null && jsonSpectrum.length() != 0) {

            for (int spectrumIndex = 0; spectrumIndex < jsonSpectrum.length();
                 spectrumIndex++) {
                JSONObject jsonSsw = jsonSpectrum.getJSONObject(spectrumIndex);
                JSONObject jsonSp =
                    jsonSsw.getJSONObject("spectralWindowSetup");
                logger.info("json sp is " + jsonSp);
                ScienceSpectralWindow ssw = new ScienceSpectralWindow();
                SpectralWindowSetup sw = new SpectralWindowSetup();
                setStart(sw, jsonSp);
                setEnd(sw, jsonSp);
                setSpectralResolution(sw, jsonSp);
                setExpectedSpectralLines(ssw, jsonSp);

                // set easy ones.
                sw.setIsSkyFrequency(jsonSp.getBoolean("isSkyFrequency"));
                sw.setPolarization(PolStateEnum.valueOf(
                    jsonSp.getString("polarization")));

                // set the objects correctly.
                ssw.setSpectralWindowSetup(sw);

                // update database.
                technicalGoalResource.addNewChildObject(
                    goal, ssw, goal::addToSpectrum);
            }
        }
    }

    /**
     * creates the expected spectral lines for the technical goal.
     *
     * @param ssw the spectral window container.
     * @param jsonSp the json.
     */
    private void setExpectedSpectralLines(
            ScienceSpectralWindow ssw, JSONObject jsonSp) {
        JSONArray lines = jsonSp.optJSONArray("expectedSpectralLine");
        if(lines != null && lines.length() != 0) {
            throw new WebApplicationException(
                "dont know what to do with these for " + ssw +
                    ". Please contact the devs.");
        }
    }

    /**
     * sets the start of a spectrum from json.
     * @param sw the spectrum params object.
     * @param jsonSp: the json.
     */
    private void setStart(
        SpectralWindowSetup sw, JSONObject jsonSp) {
        JSONObject jsonStart = jsonSp.optJSONObject("start");
        if (jsonStart != null) {
            RealQuantity start = this.createRealQuantity(jsonStart);
            sw.setStart(start);
        }
    }

    /**
     * sets the end of a spectrum from json.
     * @param sw the spectrum params object.
     * @param jsonSp: the json.
     */
    private void setEnd(
        SpectralWindowSetup sw, JSONObject jsonSp) {
        JSONObject jsonEnd = jsonSp.optJSONObject("end");
        if (jsonEnd != null) {
            RealQuantity end = this.createRealQuantity(jsonEnd);
            sw.setEnd(end);
        }
    }

    /**
     * sets the SpectralResolution of a spectrum from json.
     * @param sw the spectrum params object.
     * @param jsonSp: the json.
     */
    private void setSpectralResolution(
        SpectralWindowSetup sw, JSONObject jsonSp) {
        JSONObject jsonSpectralResolution =
            jsonSp.optJSONObject("spectralResolution");
        if (jsonSpectralResolution != null) {
            RealQuantity spectralResolution =
                this.createRealQuantity(jsonSpectralResolution);
            sw.setSpectralResolution(spectralResolution);
        }
    }

    /**
     * sets the DesiredDynamicRange of a performance params from json.
     * @param pp the performance params object.
     * @param jsonPP: the json.
     */
    private void setDesiredDynamicRange(
            PerformanceParameters pp, JSONObject jsonPP) {
        JSONObject jsonDesiredDynamicRange =
            jsonPP.optJSONObject("desiredDynamicRange");
        if (jsonDesiredDynamicRange != null) {
            RealQuantity ddr =
                this.createRealQuantity(jsonDesiredDynamicRange);
            pp.setDesiredDynamicRange(ddr);
        }
    }

    /**
     * sets the DesiredLargestScale of a performance params from json.
     * @param pp the performance params object.
     * @param jsonPP: the json.
     */
    private void setDesiredLargestScale(
        PerformanceParameters pp, JSONObject jsonPP) {
        JSONObject jsonDesiredLargestScale =
            jsonPP.optJSONObject("desiredLargestScale");
        if (jsonDesiredLargestScale != null) {
            RealQuantity dls =
                this.createRealQuantity(jsonDesiredLargestScale);
            pp.setDesiredLargestScale(dls);
        }
    }

    /**
     * sets the RepresentativeSpectralPoint of a performance params from json.
     * @param pp the performance params object.
     * @param jsonPP: the json.
     */
    private void setRepresentativeSpectralPoint(
        PerformanceParameters pp, JSONObject jsonPP) {
        JSONObject jsonRepresentativeSpectralPoint =
            jsonPP.optJSONObject("representativeSpectralPoint");
        if (jsonRepresentativeSpectralPoint != null) {
            RealQuantity rsp =
                this.createRealQuantity(jsonRepresentativeSpectralPoint);
            pp.setRepresentativeSpectralPoint(rsp);
        }
    }

    /**
     * sets the DesiredSensitivity of a performance params from json.
     * @param pp the performance params object.
     * @param jsonPP: the json.
     */
    private void setDesiredSensitivity(
        PerformanceParameters pp, JSONObject jsonPP) {
        JSONObject jsonDesiredSensitivity =
            jsonPP.optJSONObject("desiredSensitivity");
        if (jsonDesiredSensitivity != null) {
            RealQuantity ds =
                this.createRealQuantity(jsonDesiredSensitivity);
            pp.setDesiredSensitivity(ds);
        }
    }

    /**
     * sets the DesiredAngularResolution of a performance params from json.
     * @param pp the performance params object.
     * @param jsonPP: the json.
     */
    private void setDesiredAngularResolution(
        PerformanceParameters pp, JSONObject jsonPP) {
        JSONObject jsonDesiredAngularResolution =
            jsonPP.optJSONObject("desiredAngularResolution");
        if (jsonDesiredAngularResolution != null) {
            RealQuantity dar =
                this.createRealQuantity(jsonDesiredAngularResolution);
            pp.setDesiredAngularResolution(dar);
        }
    }

    /**
     * saves the proposal's observations.
     * @param newProposal: the new proposal to persist state in.
     * @param proposalJSON: the json object holding new data.
     * @param targetIdMapToReal: map for the json and real ids to real targets.
     * @param technicalMapToReal: map between ids for the technicals in json
     *                            vs real technical goals.
     * @param observationResource: the resource to save observations to the
     *                          database.
     */
    private void saveProposalObservations(
            ObservingProposal newProposal, JSONObject proposalJSON,
            HashMap<Long, Target> targetIdMapToReal,
            HashMap<Long, TechnicalGoal> technicalMapToReal,
            ObservationResource observationResource) {
        JSONArray jsonObservations = proposalJSON.optJSONArray("observations");
        if(jsonObservations != null && jsonObservations.length() != 0) {
            for (int observationIndex = 0;
                 observationIndex < jsonObservations.length();
                 observationIndex++) {
                JSONObject jsonObservation =
                    jsonObservations.getJSONObject(observationIndex);
                String type = jsonObservation.getString("@type");
                switch (type) {
                    case "proposal:TargetObservation":
                        this.createTargetObservation(
                            newProposal, jsonObservation, targetIdMapToReal,
                            technicalMapToReal, observationResource);
                        break;
                    case "proposal:CalibrationObservation":
                        throw new WebApplicationException(
                            "have not done CalibrationObservation observation" +
                                " type. Please contact devs.");
                    default:
                        throw new WebApplicationException(
                            "do not recognise this observation type. " +
                                "Please contact devs.");
                }
            }
        }
    }

    /**
     * builds a target observation.
     *
     * @param newProposal: the new proposal to persist state in.
     * @param jsonObservation: contains the json observation data.
     * @param targetIdMapToReal: map for the json and real ids to real targets.
     * @param technicalMapToReal: map between ids for the technicals in json
     *                            vs real technical goals.
     * @param observationResource: the resource to save observations to the
     *                          database.
     */
    private void createTargetObservation(
        ObservingProposal newProposal, JSONObject jsonObservation,
        HashMap<Long, Target> targetIdMapToReal,
        HashMap<Long, TechnicalGoal> technicalMapToReal,
        ObservationResource observationResource
    ) {
        TargetObservation observation =
            this.createTargetObservation(
                jsonObservation, targetIdMapToReal,
                technicalMapToReal);
        observationResource.addNewChildObject(
            newProposal, observation,
            newProposal::addToObservations);
        this.saveConstraints(
            observation,
            jsonObservation.getJSONArray("constraints"),
            observationResource
        );
    }

    /**
     * saves constraints.
     * NOTE: Be aware that the JSOn seems to be a bit corrupted in that it does
     * not follow the ISO instant formatter that formats or parses an
     * instant in UTC, such as '2011-12-03T10:15:30Z' which is the ISO-8601
     * instant format. The JSON on the other hand returns a "+0000" instead of
     * a "Z" which causes issues for the parsers. SO ABS has had to add a
     * replacement string to resolve this problem.
     *
     * @param observation: the observation object.
     * @param jsonConstraints: json containing the constraints.
     * @param observationResource: the resource to save observations to the
     *                          database.
     */
    private void saveConstraints(
            TargetObservation observation, JSONArray jsonConstraints,
            ObservationResource observationResource) {
        if(jsonConstraints != null && jsonConstraints.length() != 0) {
            for (int constraintIndex = 0;
                 constraintIndex < jsonConstraints.length();
                 constraintIndex++) {
                JSONObject jsonConstraint =
                    jsonConstraints.getJSONObject(constraintIndex);
                String type = jsonConstraint.getString("@type");
                switch (type) {
                    case "proposal:TimingWindow":
                        this.createTimingWindow(
                            observation, jsonConstraint, observationResource);
                        break;
                    case "proposal:TimingConstraint":
                    case "proposal:SimultaneityConstraint":
                    case "proposal:PointingConstaint":
                        throw new WebApplicationException(
                            "have not done these other types of" +
                                " constraint. Please contact devs.");
                    default:
                        throw new WebApplicationException(
                            "dont recognise this type of constraint. " +
                                "Please contact the devs."
                        );
                }
            }
        }
    }

    /**
     * builds a timing window.
     *
     * @param observation: the observation object.
     * @param jsonConstraint: json containing the constraint.
     * @param observationResource: the resource to save observations to the
     *                          database.
     */
    private void createTimingWindow(
            TargetObservation observation, JSONObject jsonConstraint,
            ObservationResource observationResource) {
        TimingWindow window = new TimingWindow();
        window.setNote(jsonConstraint.getString("note"));
        window.setIsAvoidConstraint(
            jsonConstraint.getBoolean("isAvoidConstraint"));

        // sort out end time.
        Instant endInstant = Instant.parse(
            jsonConstraint.getString("endTime").replace(
                TIMESTAMP_CORRUPTION, TIMESTAMP_REPLACEMENT));
        Date end = Date.from(endInstant);
        window.setEndTime(end);

        // sort out start time.
        Instant startInstant = Instant.parse(
            jsonConstraint.getString("startTime").replace(
                TIMESTAMP_CORRUPTION, TIMESTAMP_REPLACEMENT));
        Date start = Date.from(startInstant);
        window.setStartTime(start);

        // add to database.
        observationResource.addNewChildObject(
            observation, window, observation::addToConstraints);
    }

    /**
     * creates a target observation.
     * @param jsonObservation: the json object holding new data.
     * @param targetIdMapToReal: map for the json and real ids to real.
     * @param technicalMapToReal: map between ids for the technicals in json
     *                            vs real.
     */
    private TargetObservation createTargetObservation(
            JSONObject jsonObservation,
            HashMap<Long, Target> targetIdMapToReal,
            HashMap<Long, TechnicalGoal> technicalMapToReal) {
        TargetObservation observation = new TargetObservation();

        // field
        TargetField field = new TargetField();
        field.setName(jsonObservation.getJSONObject("field").getString("name"));
        field.setXmlId("1");
        observation.setField(field);

        // target
        observation.setTarget(
            targetIdMapToReal.get(jsonObservation.getLong("target")));

        // technical goal.
        observation.setTechnicalGoal(
            technicalMapToReal.get(jsonObservation.getLong("technicalGoal")));

        return observation;
    }

    /**
     * saves the proposal's supporting documents.
     * @param newProposal: the new proposal to persist state in.
     * @param proposalJSON: the json object holding new data.
     * @param fileUpload: the zip file containing supporting documents.
     * @param supportingDocumentResource: the supporting doc resource.
     */
    private void saveProposalDocuments(
            ObservingProposal newProposal, JSONObject proposalJSON,
            FileUpload fileUpload,
            SupportingDocumentResource supportingDocumentResource) {
        JSONArray supportingDocuments =
            proposalJSON.optJSONArray("supportingDocuments");
        if(supportingDocuments != null && supportingDocuments.length() != 0) {
            for (int supDocIndex = 0;
                 supDocIndex < supportingDocuments.length();
                 supDocIndex++) {
                JSONObject jsonDoc =
                    supportingDocuments.getJSONObject(supDocIndex);
                String docTitle = jsonDoc.getString("title");
                String docTitleToSearchFor = "SUPPORTING_DOC_" + docTitle;
                byte[] docData = this.readFile(
                    fileUpload, docTitleToSearchFor);
                try {
                    supportingDocumentResource.uploadSupportingDocumentFromZip(
                        newProposal, docData, docTitle);
                } catch (IOException e) {
                    throw new WebApplicationException(
                        "the writing of the supporting doc to disk failed" +
                            " for reason: " + e.getMessage()
                    );
                }
            }
        }
    }

    /**
     * method for locating a given file within a zip file.
     *
     * @param fileUpload the zip file.
     * @param filename the file to locate.
     * @return a byte [] with the data, or null if no file was found.
     * <p>
     * NOTE: The reason why we use a byte array instead of files, is that in
     * a central resources such as a server, utilising the disk drive opens up
     * conflicts with file names, and clean up processes, and slows down
     * processing with IO access. Utilising RAM keeps everything self-contained.
     */
    private byte[] readFile(FileUpload fileUpload, String filename) {
        try {
            FileInputStream fileInputStream = new FileInputStream(
                fileUpload.uploadedFile().toFile());
            ZipInputStream zipInputStream = new ZipInputStream(fileInputStream);
            ZipEntry entry = zipInputStream.getNextEntry();

            // cycle the entries until either found or no more entries.
            while (entry != null) {
                // test filename for name we're looking for
                if (entry.getName().equals(filename)) {
                    int uncompressedSize = Math.toIntExact(entry.getSize());

                    // build buffer with uncompressed size.
                    byte[] fileData = new byte[uncompressedSize];
                    int bytesRead = zipInputStream.read(
                        fileData, 0, uncompressedSize);
                    if (bytesRead != uncompressedSize && bytesRead != -1) {
                        throw new WebApplicationException(
                            "Failed to read the correct amount of data. read "
                                + bytesRead + " instead of " +
                                uncompressedSize + "bytes", 400);
                    }

                    // close the streams before handing back the results.
                    zipInputStream.close();
                    fileInputStream.close();

                    // return raw data
                    return fileData;
                }

                // not right file, move on
                entry = zipInputStream.getNextEntry();
            }

            // no file found.
            zipInputStream.close();
            fileInputStream.close();
            return null;

        } catch (FileNotFoundException e) {
            throw new WebApplicationException("file not found error", 400);
        } catch (IOException e) {
            throw new WebApplicationException(e, 400);
        }
    }
}
