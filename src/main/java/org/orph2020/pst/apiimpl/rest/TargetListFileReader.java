package org.orph2020.pst.apiimpl.rest;

import jakarta.ws.rs.WebApplicationException;
import org.ivoa.dm.ivoa.RealQuantity;
import org.ivoa.dm.proposal.prop.CelestialTarget;
import org.ivoa.dm.proposal.prop.Target;
import org.ivoa.dm.stc.coords.Epoch;
import org.ivoa.dm.stc.coords.EquatorialPoint;
import org.ivoa.dm.stc.coords.SpaceSys;
import org.ivoa.vodml.stdtypes.Unit;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class TargetListFileReader {

    private static HashMap<String, Integer> getHeaderIndices(String headerLine) {
        HashMap<String, Integer> headerIndices = new HashMap<>();
        String[] headers = headerLine.substring(1).split(",");

        String[] headerNames = new String[] {"name", "ra", "dec", "pmra", "pmdec", "plx", "rv"};

        List<String> headerList = new ArrayList<>(Arrays.asList(headerNames));

        for (String header : headers) {
            if (List.of(headerNames).contains(header.toLowerCase())) {
                headerIndices.put(header.toLowerCase(), headerIndices.size());
                headerList.remove(header.toLowerCase());
            } else {
                throw new WebApplicationException("Unrecognised header name: " + header, 400);
            }
        }

        StringBuilder formatErrors = new StringBuilder();

        if (headerList.contains("name")) {
            formatErrors.append("Target 'name' is required as a column header.\n");
        }

        if (headerList.contains("ra") || headerList.contains("dec")) {
            formatErrors.append("Both 'ra' and 'dec' coordinates are required as column headers.\n");
        }

        if (!headerList.contains("pmra") && headerList.contains("pmdec") ||
                headerList.contains("pmra") && !headerList.contains("pmdec")) {
            formatErrors.append("Please provide both 'pmra' and 'pmdec' as column headers, not just one.\n");
        }

        if (!formatErrors.isEmpty()) {
            throw new WebApplicationException("You have file formatting errors:\n" + formatErrors, 400);
        }

        return headerIndices;
    }


    public static List<Target> readTargetListFile(
            File theFile,
            SpaceSys spaceSys,
            List<String> existingNames
    ) throws WebApplicationException
    {

        List<Target> result = new ArrayList<>();

        //open file to read and extract its contents into a List of Targets
        try {
            Scanner theScanner = new Scanner(theFile);

            if (theScanner.hasNextLine()) {
                String headerLine = theScanner.nextLine();
                if (headerLine.startsWith("#")) {
                    HashMap<String,Integer> headerIndices = getHeaderIndices(headerLine);

                    HashMap<Integer, String> nonUniqueNames = new HashMap<>();

                    List<String> tableTargetNames = new ArrayList<>();

                    int rowCount = 0;

                    while (theScanner.hasNextLine()) {
                        String[] tokens = theScanner.nextLine().split(",");

                        if (tokens.length != headerIndices.size()) {
                            int row = rowCount + 1;
                            throw new WebApplicationException(
                                    "Expected " + headerIndices.size() + " columns but got "
                                            + tokens.length + " at row " + row, 400
                            );
                        }

                        String targetName = tokens[headerIndices.get("name")];

                        if (existingNames.contains(targetName) || tableTargetNames.contains(targetName)) {
                            //the name is not unique, collect the offending name and row count to feed back to user
                            int row = rowCount + 1;
                            nonUniqueNames.put(row, targetName);
                        }

                        tableTargetNames.add(targetName);

                        Double targetRA = Double.valueOf(tokens[headerIndices.get("ra")]);
                        Double targetDEC = Double.valueOf(tokens[headerIndices.get("dec")]);

                        CelestialTarget target = CelestialTarget.createCelestialTarget(c -> {
                            c.sourceName = targetName;
                            c.sourceCoordinates = new EquatorialPoint(
                                    new RealQuantity(targetRA, new Unit("degrees")),
                                    new RealQuantity(targetDEC, new Unit("degrees")),
                                    spaceSys
                            );
                            c.positionEpoch = new Epoch("J2000.0");

                            //optionals
                            //notice that although the column may exist, the data entry may be null (represented
                            //by an empty string)
                            if (headerIndices.containsKey("pmra")) {
                                c.pmRA = Objects.equals(tokens[headerIndices.get("pmra")], "") ? null :
                                        new RealQuantity(
                                                Double.valueOf(tokens[headerIndices.get("pmra")]),
                                                new Unit("mas.yr-1")
                                        );
                            }

                            if (headerIndices.containsKey("pmdec")) {
                                c.pmDec = Objects.equals(tokens[headerIndices.get("pmdec")], "") ? null :
                                        new RealQuantity(
                                                Double.valueOf(tokens[headerIndices.get("pmdec")]),
                                                new Unit("mas.yr-1")
                                        );
                            }

                            if (headerIndices.containsKey("plx")) {
                                c.parallax = Objects.equals(tokens[headerIndices.get("plx")], "") ? null :
                                        new RealQuantity(
                                                Double.valueOf(tokens[headerIndices.get("plx")]),
                                                new Unit("mas")
                                        );
                            }

                            if (headerIndices.containsKey("rv")) {
                                c.sourceVelocity = Objects.equals(tokens[headerIndices.get("rv")], "") ? null :
                                        new RealQuantity(
                                                Double.valueOf(tokens[headerIndices.get("rv")]),
                                                new Unit("km.s-1")
                                        );
                            }
                        });

                        result.add(target);
                        rowCount++;
                    }

                    if (rowCount == 0) {
                        throw new WebApplicationException("No data rows in uploaded file", 400);
                    }

                    if (!nonUniqueNames.isEmpty()) {
                        StringBuilder nonUniqueNamesBuilder = new StringBuilder();
                        nonUniqueNamesBuilder
                                .append("Unable to store target list as there are non-unique names at the following rows:\n");

                        for (Map.Entry<Integer, String> entry : nonUniqueNames.entrySet()) {
                            nonUniqueNamesBuilder
                                    .append(entry.getKey()).append(": ")
                                    .append(entry.getValue()).append("\n");
                        }

                        throw new WebApplicationException(nonUniqueNamesBuilder.toString(), 400);
                    }

                } else {
                    throw new WebApplicationException(
                            "Missing header line (#...) in file " + theFile.getName(), 400
                    );
                }
            } else {
                throw new WebApplicationException("File is empty", 400);
            }
        } catch (FileNotFoundException e) {
            throw new WebApplicationException(e.getMessage(), 500);
        }

        return result;
    }
}
