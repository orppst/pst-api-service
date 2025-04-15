package org.orph2020.pst;
/*
 * Created on 24/03/2022 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.io.FileUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.ivoa.dm.proposal.management.ProposalCycle;
import org.ivoa.dm.proposal.management.Telescope;
import org.ivoa.dm.proposal.prop.*;
import org.jboss.logging.Logger;
import org.orph2020.pst.apiimpl.entities.SubjectMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import org.orph2020.pst.apiimpl.entities.opticalTelescopeService.XmlReaderService;
import org.orph2020.pst.apiimpl.rest.ProposalDocumentStore;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

@ApplicationScoped
public class AppLifecycleBean {

    // constant environmental value for where telescope data is located.
    private static final String POLARIS_MODE_ENV_NAME =
        "POLARIS_TELESCOPE_MODE";

    // makes a string based enum with text. to help iterate.
    public enum MODES {
        OPTICAL("OPTICAL", 0),
        RADIO("RADIO", 1),
        BOTH("BOTH", 2);

        private final String text;
        private final int number;

        MODES(String text, int number) {
            this.text = text;
            this.number = number;
        }

        public String getText() {
            return text;
        }
        public int getValue() { return number; }
    }

    // exists for the application lifetime no need to close
    @PersistenceContext
    protected EntityManager em;

    @ConfigProperty(name = "document-store.proposals.root")
    String documentStoreRoot;

    @Inject
    ProposalDocumentStore proposalDocumentStore;

    // reader for xml files
    XmlReaderService xmlReader;

    // store for the polaris mode
    int polarisMode;

    private static final Logger LOGGER = Logger.getLogger("ListenerBean");

    // produces the array of telescopes.
    @Singleton
    @Produces
    XmlReaderService produceTelescopes() {
        return this.xmlReader;
    }

    // produces the holder for the polaris mode.
    @Singleton
    @Produces
    int mode() { return this.polarisMode; }


    @Transactional
    void onStart(@Observes StartupEvent ev) {
        LOGGER.info("The application is starting...");
        this.initialiseDatabase();
        this.initialiseOpticalTelescopeDatabase();
        this.initialisePolarisMode();
    }

    /**
     * processes the environment variable for the polaris mode.
     */
    private void initialisePolarisMode() {
        String environmentMode = System.getenv(POLARIS_MODE_ENV_NAME);

        // handle when no environment variable is set.
        if(environmentMode == null) {
            this.polarisMode = MODES.RADIO.number;
            LOGGER.log(
                Logger.Level.WARN,
                "no environment variable was set for" +
                POLARIS_MODE_ENV_NAME + ", so defaulting to RADIO setting.");
        }

        switch (MODES.valueOf(environmentMode)) {
            case RADIO -> this.polarisMode = MODES.RADIO.number;
            case OPTICAL -> this.polarisMode = MODES.OPTICAL.number;
            case BOTH -> this.polarisMode = MODES.BOTH.number;
            default -> {
                LOGGER.error("NO valid mode was detected. setting to radio.");
                this.polarisMode = MODES.RADIO.number;
            }
        }
    }

    /**
     * builds the optic telescope database.
     */
    private void initialiseOpticalTelescopeDatabase() {
        xmlReader = new XmlReaderService();
        xmlReader.read();

        // add telescopes to the proposal management system.
        for (String telescopeName: xmlReader.getTelescopes().keySet()) {
            Telescope managementTelescope = new Telescope();
            managementTelescope.setName(telescopeName);
            em.persist(managementTelescope);
        }
    }

    /**
     * builds the basic database that contains proposals and such.
     */
    private void initialiseDatabase() {
        Long i = em.createQuery(
            "select count(o) from Observatory o", Long.class).getSingleResult();
        if(i.intValue() == 0) {
            LOGGER.info("initializing Database");
            // add the example proposals.
            FullExample fullExample = new FullExample();
            List<ProposalCycle> cycles = fullExample.getManagementModel().
                getContent(ProposalCycle.class);
            LocalDate now = LocalDate.now();
            for (ProposalCycle cycle : cycles) {
                cycle.setSubmissionDeadline(
                    new Date(now.plusWeeks(2).atStartOfDay().atOffset(
                        ZoneOffset.UTC).toEpochSecond()*1000));
                cycle.setObservationSessionStart(
                    new Date(now.plusMonths(2).atStartOfDay().atOffset(
                        ZoneOffset.UTC).toEpochSecond()*1000));
                cycle.setObservationSessionEnd(
                    new Date(now.plusMonths(6).atStartOfDay().atOffset(
                        ZoneOffset.UTC).toEpochSecond()*1000));
            }
            fullExample.saveTodB(em);

            for(ObservingProposal pr: fullExample.getProposalModel().getContent(
                ObservingProposal.class))
                try {
                    proposalDocumentStore.createStorePaths(pr.getId());
                } catch (IOException e) {
                    LOGGER.error(e);
                    throw new RuntimeException(e);
                }
        }

        //only try to populate the SubjectMap if not already done
        TypedQuery<SubjectMap> sq = em.createQuery(
            "select o from SubjectMap o where o.uid = " +
                "'bb0b065f-6dc3-4062-9b3e-525c1a1a9bec'", SubjectMap.class);
        if(sq.getResultList().isEmpty()) {

            TypedQuery<Person> pq = em.createQuery(
                "select o from Person o", Person.class);
            for (Person p : pq.getResultList()) {
                switch (p.getEMail()) {
                    case "pi@unreal.not.email":

                        em.persist(new SubjectMap(p,
                            "bb0b065f-6dc3-4062-9b3e-525c1a1a9bec"));
                        break;
                    case "reviewer@unreal.not.email":
                        em.persist(new SubjectMap(p,
                            "dda2fd0b-8bb4-4dd1-a216-f75087f3d946"));
                        break;
                    case "tacchair@unreal.not.email":
                        em.persist(new SubjectMap(p,
                            "b0f7b98e-ec1e-4cf9-844c-e9f192c97745"));
                        break;
                    case "coi@unreal.not.email":
                        em.persist(new SubjectMap(p,
                            "33767eee-35a1-4fef-b32a-f9b6fa6b36e6"));
                        break;
                    default:
                        //do nothing
                        break;
                }

            }
        }
    }

    void onStop(@Observes ShutdownEvent ev) {

        LOGGER.info("The application is stopping...");

        LOGGER.info("Deleting document store...");

        //*****************************************************
        //This code obliterates the document store - okay for dev, but prod?
        File documentStorePath = new File(documentStoreRoot);
        try {
            FileUtils.deleteDirectory(documentStorePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //*****************************************************

        LOGGER.info("Deleted document store");
    }

}


