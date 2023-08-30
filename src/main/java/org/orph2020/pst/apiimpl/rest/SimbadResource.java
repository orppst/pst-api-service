package org.orph2020.pst.apiimpl.rest;


import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestQuery;
import org.orph2020.pst.common.json.SimbadTargetResult;


@Produces(MediaType.APPLICATION_JSON)
@Path("simbad")
@Tag(name = "simbad")
public class SimbadResource {

    @GET
    @Operation(summary = "search the SIMBAD online catalogue for the given target name")
    @APIResponse(
            responseCode = "200"
    )
    public SimbadTargetResult simbadFindTarget(@RestQuery String targetName) throws WebApplicationException
    {
        //build the SIMBAD url with query string - for now only sim-id query, output format votable
        String baseUrl = "https://simbad.cds.unistra.fr/simbad/";
        String queryType = "sim-id";
        String outputFormat = "output.format=votable";
        String ident = "Ident=" + targetName;

        String theUrl = baseUrl
                .concat(queryType).concat("?")
                .concat(outputFormat).concat("&")
                .concat(ident);
        try
        {
            return voTableReader.convertToTarget(theUrl);
        }
        catch(Exception e)
        {
            throw new WebApplicationException(e.getMessage(), 500);
        }
    }
}
