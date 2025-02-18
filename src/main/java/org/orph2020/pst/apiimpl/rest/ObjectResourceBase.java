package org.orph2020.pst.apiimpl.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.arc.ArcUndeclaredThrowableException;
import org.ivoa.vodml.jaxb.XmlIdManagement;
import org.jboss.logging.Logger;
import org.orph2020.pst.common.json.ObjectIdentifier;

import jakarta.inject.Inject;
import jakarta.persistence.*;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

abstract public class ObjectResourceBase {
    // exists for the application lifetime no need to close
    @PersistenceContext
    protected EntityManager em;

    @Inject
    protected ObjectMapper mapper;


    protected static final String NON_ASSOCIATE_ID =
            "%s with id: %d is not associated with the %s with id: %d";

    protected static final String NON_ASSOCIATE_NAME =
            "%s with identifier: %s is not associated with the %s with id: %d";

    protected List<ObjectIdentifier> getObjectIdentifiers(String queryStr){
        List<ObjectIdentifier> result = new ArrayList<>();
        Query query = em.createQuery(queryStr);
        List<Object[]> results = query.getResultList();
        for (Object[] r : results)
        {
            result.add(new ObjectIdentifier((Long) r[0], (String) r[1]));
        }

        return result;
    }

    // Uses the three parameter ObjectIdentifier constructor
    protected List<ObjectIdentifier> getObjectIdentifiersAlt(Query query){
        List<ObjectIdentifier> result = new ArrayList<>();
        List<Object[]> results = query.getResultList();
        for (Object[] r : results) {
            result.add(new ObjectIdentifier((Long)r[0], (String)r[1], (String)r[2]));
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

    protected <T,S> T findChildByQuery(Class<S> parentType, Class<T> childType, String childParameter,
                                     long parentId, long childId)
    {

        String qlString = "select child from " + parentType.getName()
                + " parent join parent." + childParameter + " child "
                + " where parent._id = :pid and child._id = :cid";

        //notice that the parameter 'childParameter' is NOT created from user input i.e., can't
        //be used for QL injection.
        TypedQuery<T> q = em.createQuery(
                qlString,
                childType
        );

        q.setParameter("pid", parentId);
        q.setParameter("cid", childId);
        return q.getSingleResult();
    }



    protected <T> T queryObject(TypedQuery<T> q)
          throws WebApplicationException
    {
        try {
            return q.getSingleResult(); //either returns a result or throws, does not return null

        } catch (NoResultException e) { //make the exception friendlier
            //FIXME: this does not give the qlString and there is no 'getQueryString()' for TypedQuery<T>
            String ERR_NOT_FOUND = "%s does not find object";
            throw new WebApplicationException(String.format(ERR_NOT_FOUND, q), 404);
        }
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

    protected Response emptyResponse204() {
        return Response.noContent().build();
    }


    protected <T> T persistObject(T object)
            throws WebApplicationException
    {
        try {
            em.persist(object);
        } catch (PersistenceException e) {
            throw new WebApplicationException(e.getMessage(), 400);
        }
        //FIXME general SQL errors are not being fed back to the client
        //perhaps this is an insight https://stackoverflow.com/questions/77116340/catching-database-errors-exceptions-in-quarkus-orm-with-panache
        return object; //responseWrapper(object, 201);
    }

    protected <T> Response removeObject(Class<T> type, Long id)
            throws WebApplicationException
    {
        T object = findObject(type, id);
        em.remove(object);
        return emptyResponse204();
    }

    protected <T> Response mergeObject(T object)
        throws IllegalArgumentException
    {
        em.merge(object); //throws IllegalArgumentException if object is not valid for merging

        return responseWrapper(object, 201);
    }

    protected <T,S> S addNewChildObject(T parent, S child, Consumer<S> adder)
    {
        em.persist(child);
        adder.accept(child);
        em.merge(parent);
        return child;
    }

    protected <T,S> Response deleteChildObject(T parent, S child, Consumer<S> remover)
    {

        em.remove(child);
        remover.accept(child);
        em.merge(parent);
        return emptyResponse204();
    }


    //--------------------------------------------------------------------------------
    // Error Mapper
    //--------------------------------------------------------------------------------
    @Provider
    public static class ErrorMapper implements ExceptionMapper<RuntimeException> {

        private static final Logger LOGGER = Logger.getLogger("ObjectResource");
        @Inject
        ObjectMapper objectMapper;

        @Override
        public Response toResponse(RuntimeException e) {
            LOGGER.error(e.getMessage(), e);

            int code = 500;
            String message = e.getMessage();
            String type = e.getClass().getName();
            if (e instanceof WebApplicationException) {
                code = ((WebApplicationException) e).getResponse().getStatus();
            }
            //IMPL this is a bit of a hack looking at the observed behaviour otherwise
            else if (e instanceof ArcUndeclaredThrowableException ){
                if (e.getCause() instanceof jakarta.transaction.RollbackException)
                {
                    Throwable cause = e.getCause().getCause();
                    type = cause.getClass().getName();
                    message = cause.getMessage();
                }
            }

            ObjectNode exceptionJson = objectMapper.createObjectNode();
            exceptionJson.put("exceptionType", type);
            exceptionJson.put("statusCode", code);

            if (e.getMessage() != null) {
                exceptionJson.put("message",message);
            }

            return Response.status(code).entity(exceptionJson).build();
        }
    }


}
