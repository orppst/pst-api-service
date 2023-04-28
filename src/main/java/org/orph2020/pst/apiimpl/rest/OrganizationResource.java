package org.orph2020.pst.apiimpl.rest;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.prop.Organization;
import org.ivoa.dm.proposal.prop.WikiDataId;
import org.ivoa.dm.ivoa.Ivorn;
import org.orph2020.pst.common.json.ObjectIdentifier;

import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Produces(MediaType.APPLICATION_JSON)
@Path("organizations")
@Tag(name = "organizations")
public class OrganizationResource extends ObjectResourceBase {

    @GET
    @Operation(summary= "get all Organizations stored in the database")
    public List<ObjectIdentifier> getOrganizations() {
        return super.getObjects("SELECT o._id,o.name FROM Organization o ORDER BY o.name");
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

    @PUT
    @Path("{id}/name")
    @Consumes(MediaType.TEXT_PLAIN)
    @Operation(summary = "update an Organization's name")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateOrganisationName(@PathParam("id") Long id, String replacementName)
        throws WebApplicationException
    {
        Organization organization = super.findObject(Organization.class, id);

        organization.setName(replacementName);

        return super.responseWrapper(organization, 201);
    }

    @PUT
    @Path("{id}/address")
    @Consumes(MediaType.TEXT_PLAIN)
    @Operation(summary = "update an Organization's address")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateOrganisationAddress(@PathParam("id") Long id, String replacementAddress)
            throws WebApplicationException
    {
        Organization organization = super.findObject(Organization.class, id);

        organization.setAddress(replacementAddress);

        return super.responseWrapper(organization, 201);
    }

    @PUT
    @Path("{id}/ivoId")
    @Consumes(MediaType.TEXT_PLAIN)
    @Operation(summary = "update an Organization's ivoId")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateOrganisationIvoId(@PathParam("id") Long id, String replacementIvoId)
            throws WebApplicationException
    {
        Organization organization = super.findObject(Organization.class, id);

        organization.setIvoid(new Ivorn(replacementIvoId));

        return super.responseWrapper(organization, 201);
    }

    @PUT
    @Path("{id}/wikiId")
    @Consumes(MediaType.TEXT_PLAIN)
    @Operation(summary = "update an Organization's wikiId")
    @Transactional(rollbackOn = {WebApplicationException.class})
    public Response updateOrganisationWikiId(@PathParam("id") Long id, String replacementWikiId)
            throws WebApplicationException
    {
        Organization organization = super.findObject(Organization.class, id);

        organization.setWikiId(new WikiDataId(replacementWikiId));

        return super.responseWrapper(organization, 201);
    }

}
