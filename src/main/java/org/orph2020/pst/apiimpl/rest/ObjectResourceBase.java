package org.orph2020.pst.apiimpl.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ivoa.vodml.jaxb.XmlIdManagement;
import org.jboss.logging.Logger;
import org.orph2020.pst.common.json.ObjectIdentifier;

import javax.inject.Inject;
import javax.persistence.EntityExistsException;
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

    @Inject
    protected ObjectMapper mapper;


    protected static final String NON_ASSOCIATE_ID =
            "%s with id: %d is not associated with the %s with id: %d";

    protected static final String NON_ASSOCIATE_NAME =
            "%s with identifier: %s is not associated with the %s with id: %d";

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

    protected <T> T findObject(Class<T> type, Long id)
        throws WebApplicationException
    {
        T object = em.find(type, id);
        if (object == null) {
            String ERR_NOT_FOUND = "%s with id: %d not found";
            throw new WebApplicationException(String.format(ERR_NOT_FOUND, type.toString(), id), 404);
        }

        return object;
    }

    protected XmlIdManagement findObjectInList(Long id, List<? extends XmlIdManagement> objects) {
        return objects.stream()
                .filter(o -> String.valueOf(id).equals(o.getXmlId()))
                .findAny()
                .orElse(null);
    }

    protected <T> String writeAsJsonString(T object)
            throws WebApplicationException
    {
        String jsonObject;
        try {
            jsonObject = mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new WebApplicationException(e.getMessage(), 422);
        }
        return jsonObject;
    }

    protected <T> Response responseWrapper(T object, int statusCode) {
        return Response.ok(writeAsJsonString(object)).status(statusCode).build();
    }

    protected <T> Response persistObject(T object)
            throws WebApplicationException
    {
        try {
            em.persist(object);
        } catch (EntityExistsException e) {
            throw new WebApplicationException(e.getMessage(), 400);
        }

        return responseWrapper(object, 201);
    }

    protected <T> Response removeObject(Class<T> type, Long id)
            throws WebApplicationException
    {
        T object = findObject(type, id);
        em.remove(object);
        return Response.ok().status(204).build();
    }

    protected <T> Response mergeObject(T object)
        throws IllegalArgumentException
    {
        em.merge(object); //throws IllegalArgumentException if object is not valid for merging

        return responseWrapper(object, 201);
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
