package org.orph2020.pst.apiimpl.rest;


import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.proposal.management.Filter;
import org.ivoa.dm.proposal.management.Observatory;

import java.util.ArrayList;
import java.util.List;

/*
 semantics: Filters are "owned" or contained by Observatories.
 */

@Produces(MediaType.APPLICATION_JSON)
@Path("observatories/{observatoryId}/filters")
@Tag(name = "observatory-filters")
public class FilterResource extends ObjectResourceBase{

    @GET
    @Operation(summary = "get all the Filters associated with the given Observatory")
    public List<Filter> getObservatoryFilters(@PathParam("observatoryId") Long observatoryId)
    {
        //return findObject(Observatory.class, observatoryId).getFilters();
        return new ArrayList<>();
    }



}
