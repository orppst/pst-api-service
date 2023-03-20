package org.orph2020.pst.apiimpl.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jboss.logging.Logger;
import org.orph2020.pst.common.json.ObjectIdentifier;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.ArrayList;
import java.util.List;

abstract public class ObjectResourceBase {
    @PersistenceContext
    protected EntityManager em;  // exists for the application lifetime no need to close
    protected List<ObjectIdentifier> getObjects(String queryStr){
        List<ObjectIdentifier> result = new ArrayList<>();
        Query query = em.createQuery(queryStr);
        List<Object[]> results = query.getResultList();
        for (Object[] r : results)
        {
            result.add(new ObjectIdentifier((Long) r[0], (String) r[1]));
        }

        return result;
    }

    //--------------------------------------------------------------------------------
    // Error Mapper
    //--------------------------------------------------------------------------------
    @Provider
    public static class ErrorMapper implements ExceptionMapper<Exception> {

        private static final Logger LOGGER = Logger.getLogger("ListenerBean");
        @Inject
        ObjectMapper objectMapper;

        @Override
        public Response toResponse(Exception e) {
            LOGGER.error("Failed to handle request", e);

            int code = 500;
            if (e instanceof WebApplicationException) {
                code = ((WebApplicationException) e).getResponse().getStatus();
            }

            ObjectNode exceptionJson = objectMapper.createObjectNode();
            exceptionJson.put("exceptionType", e.getClass().getName());
            exceptionJson.put("statusCode", code);

            if (e.getMessage() != null) {
                exceptionJson.put("error", e.getMessage());
            }

            return Response.status(code).entity(exceptionJson).build();
        }
    }


}
