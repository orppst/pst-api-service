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

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;

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
                    SubjectMap m = new SubjectMap(p, "bb0b065f-6dc3-4062-9b3e-525c1a1a9bec");
                    em.persist(m);
            }

        }


    }

    void onStop(@Observes ShutdownEvent ev) {
        LOGGER.info("The application is stopping...");
    }

}


