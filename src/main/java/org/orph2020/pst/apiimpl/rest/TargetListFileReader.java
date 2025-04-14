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

        String[] headerNames = new String[] {"NAME", "RA", "DEC", "PMRA", "PMDEC", "PLX", "RV"};

        List<String> headerList = new ArrayList<>(Arrays.asList(headerNames));

        for (String header : headers) {
            if (List.of(headerNames).contains(header)) {
                headerIndices.put(header, headerIndices.size());
                headerList.remove(header);
            } else {
                throw new WebApplicationException("Unrecognised header name: " + header, 400);
            }
        }

        if (headerList.contains("NAME")) {
            throw new WebApplicationException("Target 'NAME' is required as a column header", 400);
        }

        if (headerList.contains("RA") || headerList.contains("DEC")) {
            throw new WebApplicationException("Both 'RA' and 'DEC' coordinates are required as column headers", 400);
        }

        if (!headerList.contains("PMRA") && headerList.contains("PMDEC") ||
                headerList.contains("PMRA") && !headerList.contains("PMDEC")) {
            throw new WebApplicationException(
                    "If you specify a Proper Motion in one coordinate you must also specify it in the other " +
                    "i.e., provide both 'PMRA' and 'PMDEC' as column headers",
                    400
            );
        }

        return headerIndices;
    }


    public static List<Target> readTargetListFile(
            File theFile,
            SpaceSys spaceSys,
            List<String> existingNames,
            Integer maxNumOfTargets
    )
            throws WebApplicationException
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

                    while (theScanner.hasNextLine() && rowCount < maxNumOfTargets) {
                        String[] tokens = theScanner.nextLine().split(",");

                        if (tokens.length != headerIndices.size()) {
                            throw new WebApplicationException(
                                    "Expected " + headerIndices.size() + " columns but got "
                                            + tokens.length + " at row count " + rowCount, 400
                            );
                        }

                        String targetName = tokens[headerIndices.get("NAME")];

                        if (existingNames.contains(targetName) || tableTargetNames.contains(targetName)) {
                            //the name is not unique, collect the offending name and row count to feed back to user
                            int row = rowCount + 1;
                            nonUniqueNames.put(row, targetName);
                        }

                        tableTargetNames.add(targetName);

                        Double targetRA = Double.valueOf(tokens[headerIndices.get("RA")]);
                        Double targetDEC = Double.valueOf(tokens[headerIndices.get("DEC")]);

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
                            if (headerIndices.containsKey("PMRA")) {
                                c.pmRA = Objects.equals(tokens[headerIndices.get("PMRA")], "") ? null :
                                        new RealQuantity(
                                                Double.valueOf(tokens[headerIndices.get("PMRA")]),
                                                new Unit("mas.yr-1")
                                        );
                            }

                            if (headerIndices.containsKey("PMDEC")) {
                                c.pmDec = Objects.equals(tokens[headerIndices.get("PMDEC")], "") ? null :
                                        new RealQuantity(
                                                Double.valueOf(tokens[headerIndices.get("PMDEC")]),
                                                new Unit("mas.yr-1")
                                        );
                            }

                            if (headerIndices.containsKey("PLX")) {
                                c.parallax = Objects.equals(tokens[headerIndices.get("PLX")], "") ? null :
                                        new RealQuantity(
                                                Double.valueOf(tokens[headerIndices.get("PLX")]),
                                                new Unit("mas")
                                        );
                            }

                            if (headerIndices.containsKey("RV")) {
                                c.sourceVelocity = Objects.equals(tokens[headerIndices.get("RV")], "") ? null :
                                        new RealQuantity(
                                                Double.valueOf(tokens[headerIndices.get("RV")]),
                                                new Unit("km.s-1")
                                        );
                            }
                        });

                        result.add(target);
                        rowCount++;
                    }

                    if (rowCount == 0) {
                        throw new WebApplicationException(
                                "It's not that I'm questioning your intelligence but you just sent me a file with no data, so yeah ...",
                                400
                        );
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

                    if (rowCount > maxNumOfTargets - existingNames.size()) {
                        throw new WebApplicationException(
                                "Number of Targets limited to " + maxNumOfTargets
                                        + " per Proposal. You currently have "
                                        + existingNames.size() + " targets, and are attempting to add"
                                        + rowCount + " targets.", 400
                        );
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
