package org.orph2020.pst.apiimpl.rest;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.prop.Organization;
import org.orph2020.pst.common.json.ObjectIdentifier;

import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Produces(MediaType.APPLICATION_JSON)
@Path("organizations")
@Tag(name = "proposal-tool-organizations")
public class OrganizationResource extends ObjectResourceBase {

    @GET
    @Operation(summary= "get all Organizations stored in the database")
    public List<ObjectIdentifier> getOrganizations() {
        return super.getObjects("SELECT o._id,o.name FROM Organization o ORDER BY o.fullName");
    }

    @GET
    @Path("{id}")
    @Operation(summary = "get the Organization specified by the 'id'")
    public Organization getOrganization(@PathParam("id") Long id)
        throws WebApplicationException
    {
        return super.findObject(Organization.class, id);
    }

    @POST
    @Operation(summary = "create a new Organization in the database")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response createOrganization(Organization organization)
        throws WebApplicationException
    {
        return super.persistObject(organization);
    }

    @DELETE
    @Path("{id}")
    @Operation(summary = "delete the Organization specified by the 'id' from the database")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response deleteOrganization(@PathParam("id") Long id)
        throws WebApplicationException
    {
        return super.removeObject(Organization.class, id);
    }

}
