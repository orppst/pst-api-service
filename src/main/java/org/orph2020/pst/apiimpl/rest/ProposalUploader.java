package org.orph2020.pst.apiimpl.rest;

import jakarta.ws.rs.WebApplicationException;
import org.ivoa.dm.ivoa.Ivorn;
import org.ivoa.dm.ivoa.RealQuantity;
import org.ivoa.dm.ivoa.StringIdentifier;
import org.ivoa.dm.proposal.prop.CelestialTarget;
import org.ivoa.dm.proposal.prop.Investigator;
import org.ivoa.dm.proposal.prop.InvestigatorKind;
import org.ivoa.dm.proposal.prop.ObservingProposal;
import org.ivoa.dm.proposal.prop.Organization;
import org.ivoa.dm.proposal.prop.PerformanceParameters;
import org.ivoa.dm.proposal.prop.Person;
import org.ivoa.dm.proposal.prop.ProposalKind;
import org.ivoa.dm.proposal.prop.ScienceSpectralWindow;
import org.ivoa.dm.proposal.prop.SpectralWindowSetup;
import org.ivoa.dm.proposal.prop.TechnicalGoal;
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
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ProposalUploader {

    // the logger.
    private static final Logger logger =
        Logger.getLogger(ProposalUploader.class.getName());

    // hard coded filename for the proposal in json format.
    private static final String PROPOSAL_JSON_FILE_NAME = "proposal.json";

    // stored to help data movement. contains the different resources needed
    private PersonResource personResource;
    private InvestigatorResource investigatorResource;
    private ProposalResource proposalResource;
    private TechnicalGoalResource technicalGoalResource;
    private ObservationResource observationResource;

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
     * @throws WebApplicationException when:
     * no file is found: 400
     */
    public void uploadProposal(
            FileUpload fileUpload, String updateSubmittedFlag,
            ProposalResource proposalResource, PersonResource personResource,
            InvestigatorResource investigatorResource,
            TechnicalGoalResource technicalGoalResource,
            ObservationResource observationResource)
            throws WebApplicationException {
        byte[] proposalData = this.readFile(
            fileUpload, ProposalUploader.PROPOSAL_JSON_FILE_NAME);

        // for easier movement, moved to class level scope.
        this.personResource = personResource;
        this.investigatorResource = investigatorResource;
        this.proposalResource = proposalResource;
        this.technicalGoalResource = technicalGoalResource;
        this.observationResource = observationResource;

        HashMap<Long, Long> targetIdMapToReal = new HashMap<>();
        HashMap<Long, Long> targetIdMapToJSON = new HashMap<>();
        HashMap<Long, Long> technicalMapToReal = new HashMap<>();
        HashMap<Long, Long> technicalMapToJSON = new HashMap<>();

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
            Boolean.parseBoolean(updateSubmittedFlag));
        this.saveProposalTargets(
            newProposal, proposalJSON, targetIdMapToReal, targetIdMapToJSON);
        this.saveProposalTechnicals(
            newProposal, proposalJSON, technicalMapToReal, technicalMapToJSON);
        this.saveProposalObservations(
            newProposal, proposalJSON, targetIdMapToReal, targetIdMapToJSON,
            technicalMapToReal, technicalMapToJSON);
        this.saveProposalTimingWindows(newProposal, proposalJSON);
        this.saveProposalDocuments(newProposal, proposalJSON, fileUpload);
    }

    /**
     * saves the proposal specific items, such as:
     *      (title, abstract, investigators).
     * @param newProposal: the new proposal to persist state in.
     * @param proposalJSON: the json object holding new data.
     * @param modifySubmitted: boolean stating if the submitted field should
     *                      be changed.
     */
    private void saveProposalSpecific(
            ObservingProposal newProposal, JSONObject proposalJSON,
            boolean modifySubmitted) {
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
                    "Currently related proposals are not supported.");
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
                        investigator, newProposal.getId()));
                }
            }
        } catch (Exception e) {
            logger.error("failed with error: " + e.getMessage());
            e.printStackTrace();
            throw new WebApplicationException(e.getMessage());
        }
    }

    /**
     * creates a new investigator from a json investigator.
     * @param investigator json investigator.
     * @param proposalCode: the associated proposal code.
     * @return new investigator object.
     */
    private Investigator createNewInvestigator(
            JSONObject investigator, Long proposalCode) {
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
        JSONObject orgJSON = jsonPerson.getJSONObject("homeInstitute");
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
        newInvestigator.setPerson(newPerson);

        // update database positions if required
        if (foundPerson(newPerson.getFullName(),
                        newPerson.getOrcidId().value())) {
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
     * @return boolean, true if found, false otherwise.
     */
    private boolean foundPerson(String fullName, String orcid) {
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
     * @param targetIdMapToReal: map for the json and real ids to real.
     * @param targetIdMapToJSON: map for the real and json ids, to json.
     */
    private void saveProposalTargets(
            ObservingProposal newProposal, JSONObject proposalJSON,
            HashMap<Long, Long> targetIdMapToReal,
            HashMap<Long, Long> targetIdMapToJSON) {
        // array of targets.
        JSONArray targets = proposalJSON.optJSONArray("targets");
        if(targets != null && targets.length() != 0) {
            for (int targetIndex = 0; targetIndex < targets.length();
                    targetIndex++) {
                JSONObject jsonTarget = targets.getJSONObject(targetIndex);
                String type = jsonTarget.getString("@type");
                switch (type) {
                    case "proposal:CelestialTarget":
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
                            jsonTarget.getLong("_id"), cTarget.getId());
                        targetIdMapToJSON.put(cTarget.getId(), 
                            jsonTarget.getLong("_id"));
                        break;
                    default:
                        throw new WebApplicationException(
                            "dont recognise this target type.");
                }
            }
        }
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
                new Unit(json.getJSONObject(
                    "unit").getString("value"))
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
                    EquatorialPoint coords = new EquatorialPoint();

                    // coords sys
                    String coordSystem =
                        jsonSourceCoordinates.optString("coordSys");
                    logger.info("coordSystem is " + coordSystem);
                    if (coordSystem != null && !coordSystem.equals("")) {
                        throw new WebApplicationException(
                            "dont know how to handle coordsystems.");
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
                    break;
                default:
                    throw new WebApplicationException(
                        "Dont recognise this source coordinate type.");
            }
        }
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
     * @param technicalMapToJSON: map between ids for the technicals in json
     *                         vs real.
     * @param technicalMapToReal: map between ids for the technicals in json
     *                            vs real.
     */
    private void saveProposalTechnicals(
            ObservingProposal newProposal, JSONObject proposalJSON,
            HashMap<Long, Long> technicalMapToReal,
            HashMap<Long, Long> technicalMapToJSON) {
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
                handleSpectralWindows(jsonTechnicalGoal, tg);

                // update maps.
                technicalMapToJSON.put(
                    tg.getId(), jsonTechnicalGoal.getLong("_id"));
                technicalMapToReal.put(
                    jsonTechnicalGoal.getLong("_id"), tg.getId());
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
     */
    private void handleSpectralWindows(
            JSONObject jsonTechnicalGoal, TechnicalGoal goal) {
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
                "dont know what to do with these");
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
     * @param targetIdMapToReal: map for the json and real ids to real.
     * @param targetIdMapToJSON: map for the real and json ids, to json.
     * @param technicalMapToJSON: map between ids for the technicals in json
     *                            vs real.
     * @param technicalMapToReal: map between ids for the technicals in json
     *                            vs real.
     */
    private void saveProposalObservations(
            ObservingProposal newProposal, JSONObject proposalJSON,
            HashMap<Long, Long> targetIdMapToReal,
            HashMap<Long, Long> targetIdMapToJSON,
            HashMap<Long, Long> technicalMapToReal,
            HashMap<Long, Long> technicalMapToJSON) {

    }

    /**
     * saves the proposal's spectral timing windows.
     * @param newProposal: the new proposal to persist state in.
     * @param proposalJSON: the json object holding new data.
     */
    private void saveProposalTimingWindows(
        ObservingProposal newProposal, JSONObject proposalJSON) {

    }

    /**
     * saves the proposal's supporting documents.
     * @param newProposal: the new proposal to persist state in.
     * @param proposalJSON: the json object holding new data.
     * @param fileUpload: the zip file containing supporting documents.
     */
    private void saveProposalDocuments(
            ObservingProposal newProposal, JSONObject proposalJSON,
            FileUpload fileUpload) {

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
                                + bytesRead + " instead", 400);
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
