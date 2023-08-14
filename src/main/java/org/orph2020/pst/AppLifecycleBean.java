package org.orph2020.pst;
/*
 * Created on 24/03/2022 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.ivoa.dm.proposal.management.ProposalCycle;
import org.ivoa.dm.proposal.prop.EmerlinExample;
import org.ivoa.dm.proposal.prop.ObservingProposal;
import org.ivoa.dm.proposal.prop.Person;
import org.jboss.logging.Logger;
import org.orph2020.pst.apiimpl.entities.SubjectMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class AppLifecycleBean {

    @PersistenceContext
    protected EntityManager em;  // exists for the application lifetime no need to close


    private static final Logger LOGGER = Logger.getLogger("ListenerBean");


    @Transactional
    void onStart(@Observes StartupEvent ev) {
        LOGGER.info("The application is starting...");
        LOGGER.info("initializing Database");
        Long i = em.createQuery("select count(o) from Observatory o", Long.class).getSingleResult();
        if(i.intValue() == 0) {
            EmerlinExample ex = new EmerlinExample();
            ProposalCycle cy = ex.getCycle();
            cy.persistRefs(em);
            em.persist(cy);
            ObservingProposal pr = ex.getProposal();
            pr.persistRefs(em);
            em.persist(pr);
        }

        TypedQuery<Person> pq = em.createQuery("select o from Person o", Person.class);
        for (Person p : pq.getResultList())
        {
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

    void onStop(@Observes ShutdownEvent ev) {
        LOGGER.info("The application is stopping...");
    }

}


