package org.orph2020.pst.apiimpl.rest;
/*
 * Created on 13/04/2023 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.orph2020.pst.apiimpl.entities.SubjectMap;

import javax.persistence.TypedQuery;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("subjectMap")
@Tag(name="mapping between AAI user ids and People")
@Produces(MediaType.APPLICATION_JSON)
public class SubjectMapResource extends ObjectResourceBase {

    @GET
    @Path("{id}")
    @Operation(summary = "get the SubjectMap specified by the 'id'")
    public SubjectMap subjectMap(@PathParam("id") String id)
    {
        TypedQuery<SubjectMap> q = em.createQuery("select o from SubjectMap o where o.uid = :uid", SubjectMap.class);
        q.setParameter("uid", id);
        List<SubjectMap> res = q.getResultList();
        if (res.isEmpty()){
            return new SubjectMap(null, id );
        }
        else {
            return res.get(0);
        }

    }

}
