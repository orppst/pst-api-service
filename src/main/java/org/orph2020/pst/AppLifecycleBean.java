package org.orph2020.pst;
/*
 * Created on 24/03/2022 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.ivoa.dm.proposal.prop.EmerlinExample;
import org.jboss.logging.Logger;
import org.orph2020.pst.apiimpl.rest.ProposalResource;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceContext;
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
            em.persist(ex.getCycle());
            em.persist(ex.getProposal());
        }

    }

    void onStop(@Observes ShutdownEvent ev) {
        LOGGER.info("The application is stopping...");
    }

}


