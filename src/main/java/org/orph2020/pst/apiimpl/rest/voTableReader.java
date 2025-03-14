package org.orph2020.pst.apiimpl.rest;

import org.ivoa.dm.ivoa.RealQuantity;
import org.ivoa.dm.proposal.prop.CelestialTarget;
import org.ivoa.dm.proposal.prop.Target;
import org.ivoa.dm.stc.coords.EquatorialPoint;
import org.ivoa.dm.stc.coords.SpaceSys;
import org.ivoa.vodml.stdtypes.Unit;
import org.orph2020.pst.common.json.SimbadTargetResult;
import uk.ac.starlink.table.*;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.votable.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class voTableReader {


    public static List<Target> convertToListOfTargets(String resource) throws Exception {
        List<Target> targets = new ArrayList<>();

        //Assumes "resource" points to a VOTable obtained from a SIMBAD query

        try (StarTable starTable = loadStarTable(resource)) {
            int nCol = starTable.getColumnCount();

            if (nCol == 0) {
                throw new Exception("error: table has zero columns");
            }
            else if (nCol < 3) {
                throw new Exception("error: table is required to have at least 3 columns");
            }

            long nRow = getRows(starTable);

            if (nRow == -1) {
                throw new Exception("error: unable to determine number of rows");
            }

            if (nRow == 0) {
                throw new Exception("error: table has zero rows");
            }

            //printStarTable(starTable);
            //need to extract RA,DEC from the starTable

            // find the indices of the columns representing RA/DEC, use on the row data

            int idIndex = findNameColumnIndex(starTable);

            if (idIndex == -1) {
                throw new Exception("error: unable to determine id column");
            }

            int raIndex = findColumnIndex(starTable, "RA_d");
            int decIndex = findColumnIndex(starTable, "DEC_d");

            //target name either found in column "TYPED_ID" meaning the SIMBAD query input or --
            // found in column "MAIN_ID" which is the name as stored in the database.

            for (int i = 1; i <= nRow; i++) {
                String name = (String) starTable.getCell(i, idIndex);
                double raDegrees =  (double) starTable.getCell(i, raIndex);
                double decDegrees =  (double) starTable.getCell(i, decIndex);

                EquatorialPoint equatorialPoint = new EquatorialPoint(
                        new RealQuantity(raDegrees, new Unit("degrees")),
                        new RealQuantity(decDegrees, new Unit("degrees")),
                        new SpaceSys()
                );



                //EquatorialPoint sourceCoordinates, Epoch positionEpoch, RealQuantity pmRA, RealQuantity pmDec, RealQuantity parallax, RealQuantity sourceVelocity, String sourceName
                targets.add(new CelestialTarget());
            }

        }

        return targets;
    }


    private static StarTable loadStarTable( String resourceLocation ) throws IOException {
        return new StarTableFactory().makeStarTable( resourceLocation);
    }

    private static TableSequence loadTableSequence( String resourceLocation ) throws IOException {
        return new StarTableFactory().makeStarTables(DataSource.makeDataSource( resourceLocation ));
    }


    //attempt to find an identifying name column index
    private static int findNameColumnIndex(StarTable starTable) {
        int resultId = findColumnIndex(starTable, "ID");

        int resultName = findColumnIndex(starTable, "NAME");

        int resultMainId = findColumnIndex(starTable, "MAIN_ID");

        return resultId > -1 ? resultId : resultName > -1 ? resultName : resultMainId;
    }

    private static int
    findColumnIndex(StarTable starTable, String columnName)  {
        int iCol = 0;
        int nCol = starTable.getColumnCount();

        while (iCol < nCol && !starTable.getColumnInfo(iCol).getName().equals(columnName)) {
            iCol++;
        }

        if (iCol >= nCol) {
            return -1;
        }

        return iCol;
    }

    private static long getRows(StarTable starTable) throws Exception {
        long nRow = starTable.getRowCount(); //can return a -1 if row count is not easily determined

        if (nRow == -1) {
            //determine row count using a 'RowSequence' - initial position is before the first row
            RowSequence rowSequence = starTable.getRowSequence();
            nRow = 0;
            while (rowSequence.next()) {
                nRow++;
            }
            rowSequence.close();
        }

        return nRow;
    }
}
