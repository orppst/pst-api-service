package org.orph2020.pst;
/*
 * Created on 24/03/2022 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.apache.commons.io.FileUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.ivoa.dm.proposal.management.ProposalCycle;
import org.ivoa.dm.proposal.prop.*;
import org.jboss.logging.Logger;
import org.orph2020.pst.apiimpl.entities.SubjectMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@ApplicationScoped
public class AppLifecycleBean {

    @PersistenceContext
    protected EntityManager em;  // exists for the application lifetime no need to close

    @ConfigProperty(name = "supporting-documents.store-root")
    String documentStoreRoot;

    private static final Logger LOGGER = Logger.getLogger("ListenerBean");


    @Transactional
    void onStart(@Observes StartupEvent ev) {
        LOGGER.info("The application is starting...");
        LOGGER.info("initializing Database");
        Long i = em.createQuery("select count(o) from Observatory o", Long.class).getSingleResult();
        if(i.intValue() == 0) {

           // add the example proposals.
            FullExample fullExample = new FullExample();
            List<ProposalCycle> cycles = fullExample.getManagementModel().getContent(ProposalCycle.class);
            LocalDate now = LocalDate.now();
            for (ProposalCycle cycle : cycles) {
                cycle.setSubmissionDeadline(new Date(now.plusWeeks(2).atStartOfDay().atOffset(ZoneOffset.UTC).toEpochSecond()*1000));
                cycle.setObservationSessionStart(new Date(now.plusMonths(2).atStartOfDay().atOffset(ZoneOffset.UTC).toEpochSecond()*1000));
                cycle.setObservationSessionEnd(new Date(now.plusMonths(6).atStartOfDay().atOffset(ZoneOffset.UTC).toEpochSecond()*1000));
            }
            fullExample.saveTodB(em);

            ObservingProposal pr = fullExample.getProposalModel().getContent(ObservingProposal.class).get(0);
            //here we create document store directories that would be normally created
            // in the implementation of API call "createProposal"
            // FIXME not sure if justifications are copied too...
            try {
                Files.createDirectories(Paths.get(
                        documentStoreRoot,
                        "proposals",
                        pr.getId().toString(),
                        "supportingDocuments"
                ));
                Files.createDirectories(Paths.get(
                        documentStoreRoot,
                        "proposals",
                        pr.getId().toString(),
                        "justifications",
                        "scientific"
                ));
                Files.createDirectories(Paths.get(
                        documentStoreRoot,
                        "proposals",
                        pr.getId().toString(),
                        "justifications",
                        "technical"
                ));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        //only try to populate the SubjectMap if not already done
        TypedQuery<SubjectMap> sq = em.createQuery("select o from SubjectMap o where o.uid = 'bb0b065f-6dc3-4062-9b3e-525c1a1a9bec'", SubjectMap.class);
        if(sq.getResultList().isEmpty()) {

            TypedQuery<Person> pq = em.createQuery("select o from Person o", Person.class);
            for (Person p : pq.getResultList()) {
                switch (p.getEMail()) {
                    case "pi@unreal.not.email":

                        em.persist(new SubjectMap(p, "bb0b065f-6dc3-4062-9b3e-525c1a1a9bec"));
                        break;
                    case "reviewer@unreal.not.email":
                        em.persist(new SubjectMap(p, "dda2fd0b-8bb4-4dd1-a216-f75087f3d946"));
                        break;
                    case "tacchair@unreal.not.email":
                        em.persist(new SubjectMap(p, "b0f7b98e-ec1e-4cf9-844c-e9f192c97745"));
                        break;
                    case "coi@unreal.not.email":
                        em.persist(new SubjectMap(p, "33767eee-35a1-4fef-b32a-f9b6fa6b36e6"));
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


