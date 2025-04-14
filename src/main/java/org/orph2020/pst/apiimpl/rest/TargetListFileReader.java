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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

public class TargetListFileReader {


    public static List<Target> readTargetListFile(File theFile, SpaceSys spaceSys) {

        List<Target> result = new ArrayList<>();

        //open file to read and extract its contents into a List of Targets
        try {
            Scanner theScanner = new Scanner(theFile);

            //name, ra, dec, [pmra, pmdec, plx, rv]

            while (theScanner.hasNextLine()) {
                String[] tokens = theScanner.nextLine().split(",");

                if (tokens.length < 3) {
                    throw new WebApplicationException(
                            String.format("need at least 'name,ra,dec' in target list file %s", theFile.getName())
                    );
                }

                if (tokens.length == 4) {
                    throw new WebApplicationException(
                            String.format("missing data (proper motion value?) in target lists file %s", theFile.getName())
                    );
                }

                String targetName = tokens[0];
                Double targetRA = Double.valueOf(tokens[1]);
                Double targetDEC = Double.valueOf(tokens[2]);

                CelestialTarget target = CelestialTarget.createCelestialTarget(c -> {
                    c.sourceName = targetName;
                    c.sourceCoordinates = new EquatorialPoint(
                            new RealQuantity(targetRA, new Unit("degrees")),
                            new RealQuantity(targetDEC, new Unit("degrees")),
                            spaceSys
                    );
                    c.positionEpoch = new Epoch("J2000.0");

                    if (tokens.length > 3) {
                        c.pmRA = Objects.equals(tokens[3], "") ? null :
                                new RealQuantity(Double.valueOf(tokens[3]), new Unit("mas.yr-1"));
                        c.pmDec = Objects.equals(tokens[4], "") ? null :
                                new RealQuantity(Double.valueOf(tokens[4]), new Unit("mas.yr-1"));
                    }

                    if (tokens.length > 5) {
                        c.parallax = Objects.equals(tokens[5], "") ? null :
                                new RealQuantity(Double.valueOf(tokens[5]), new Unit("mas"));

                        if (tokens.length == 7) {
                            c.sourceVelocity = Objects.equals(tokens[6], "") ? null :
                                    new RealQuantity(Double.valueOf(tokens[6]), new Unit("km.s-1"));
                        }
                    }
                });

                result.add(target);
            }

        } catch (FileNotFoundException e) {
            throw new WebApplicationException(e);
        }

        return result;
    }
}
