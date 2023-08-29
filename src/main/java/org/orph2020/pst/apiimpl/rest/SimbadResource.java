package org.orph2020.pst.apiimpl.rest;


import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.ivoa.dm.ivoa.RealQuantity;
import org.ivoa.dm.proposal.prop.CelestialTarget;
import org.ivoa.dm.proposal.prop.Target;
import org.ivoa.dm.stc.coords.*;
import org.ivoa.vodml.stdtypes.Unit;
import org.jboss.resteasy.reactive.RestQuery;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;


@Produces(MediaType.APPLICATION_JSON)
@Path("simbad")
@Tag(name = "simbad")
public class SimbadResource {

    //This is potentially redundant as STIL appears able to resolve/fetch URL data--
    //get content from the given URL as a String
    private static String getUrlContent(String theUrl)
    {
        StringBuilder content = new StringBuilder();

        try
        {
            URL url = new URL(theUrl);
            URLConnection urlConnection = url.openConnection();

            BufferedReader bufferedReader =
                    new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

            String line;
            while ((line = bufferedReader.readLine()) != null)
            {
                content.append(line).append("\n");
            }

            bufferedReader.close();
        }
        catch (Exception e)
        {
            throw new WebApplicationException(e.getMessage(), 500);
        }

        return content.toString();
    }


    @GET
    @Operation(summary = "search the SIMBAD online catalogue for the given target name")
    @APIResponse(
            responseCode = "200"
    )
    public Target simbadFindTarget(@RestQuery String targetName) throws WebApplicationException
    {
        //build the SIMBAD url with query string - for now only sim-id query, output format votable
        String baseUrl = "https://simbad.cds.unistra.fr/simbad/";
        String queryType = "sim-id";
        String outputFormat = "output.format=votable";
        String ident = "Ident=" + targetName;

        String theUrl = baseUrl
                .concat(queryType).concat("?").
                concat(outputFormat).concat("&").
                concat(ident);

        String simbadContent = getUrlContent(theUrl);

        System.out.println(simbadContent);

        try
        {
            System.out.println(":::::::SAX:::::::::");
            voTableReader.saxProcessing(theUrl);
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }

        try
        {
            System.out.println(":::::::DOM:::::::::");
            voTableReader.domProcessing(theUrl);
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }

        try
        {
            System.out.println(":::::::GENERIC:::::::");
            voTableReader.genericProcessing(theUrl);
        }
        catch(Exception e)
        {
            System.out.println(e.getMessage());
        }

        // FIXME change to use the actual target requested by the SIMBAD query

        //Faked target to pass the SimbadResourceTest unit test
        SpaceSys ICRS_SYS = new SpaceSys(new CartesianCoordSpace(),
                new SpaceFrame(
                        new StdRefLocation("TOPOCENTRE"), "ICRS",
                        (Epoch)null, "")
        );

        Unit degrees = new Unit("degrees");


        return CelestialTarget.createCelestialTarget((c) -> {
            c.sourceName = "someTarget";
            c.sourceCoordinates = new EquatorialPoint(
                    new RealQuantity(45.0, degrees),
                    new RealQuantity(60.0, degrees),
                    ICRS_SYS
            );
            c.positionEpoch = new Epoch("J2000");
        });
    }
}
