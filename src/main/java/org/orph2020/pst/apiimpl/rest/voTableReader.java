package org.orph2020.pst.apiimpl.rest;

import org.orph2020.pst.common.json.SimbadTargetResult;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.votable.*;

import java.net.URL;

public class voTableReader {

    private static int
    findColumnIndex(StarTable starTable, String columnName) throws Exception {
        int iCol = 0;
        int nCol = starTable.getColumnCount();

        while (iCol < nCol && !starTable.getColumnInfo(iCol).getName().equals(columnName))
        {
            iCol++;
        }

        if (iCol >= nCol)
        {
            throw new Exception(String.format("error: cannot find %s column in the table", columnName));
        }

        return iCol;
    }

    private static long getRows(StarTable starTable) throws Exception {
        long nRow = starTable.getRowCount(); //can return a -1 if row count is not easily determined

        if (nRow == -1)
        {
            //determine row count using a 'RowSequence' - initial position is before the first row
            RowSequence rowSequence = starTable.getRowSequence();
            nRow = 0;
            while (rowSequence.next())
            {
                nRow++;
            }
            rowSequence.close();
        }

        if (nRow == 0)
        {
            throw new Exception("error: table has no row data");
        }
        return nRow;
    }


    public static SimbadTargetResult convertToTarget(String theUrl) throws Exception {
        VOTableBuilder voTableBuilder = new VOTableBuilder();
        DataSource dataSource = DataSource.makeDataSource(new URL(theUrl));
        StoragePolicy storagePolicy = StoragePolicy.getDefaultPolicy();

        try (StarTable starTable = voTableBuilder.makeStarTable(dataSource, false, storagePolicy))
        {
            int nCol = starTable.getColumnCount();

            if (nCol == 0)
            {
                throw new Exception("error: table has zero columns");
            }
            else if (nCol < 3)
            {
                throw new Exception("error: table is required to have at least 3 columns");
            }

            long nRow = getRows(starTable);

            if (nRow > 1)
            {
                System.out.println("warning: table has more than one row - using the first row only");
            }

            //printStarTable(starTable);
            //need to extract RA,DEC from the starTable

            // find the indices of the columns representing RA/DEC, use on the row data

            int raIndex = findColumnIndex(starTable, "RA_d");
            int decIndex = findColumnIndex(starTable, "DEC_d");

            //target name either found in column "TYPED_ID" meaning the SIMBAD query input or --
            // found in column "MAIN_ID" which is the name as stored in the database.

            String rawName;

            try
            {
                int nameIndex = findColumnIndex(starTable, "TYPED_ID");
                rawName = (String) starTable.getCell(0, nameIndex);
            }
            catch (Exception e1)
            {
                try
                {
                    int nameIndex = findColumnIndex(starTable, "MAIN_ID");
                    rawName = (String) starTable.getCell(0, nameIndex);
                }
                catch (Exception e2)
                {
                    throw new Exception("error: no name data could be found in the table");
                }
            }

            //format the name to be lower case with no whitespace
            String targetName = rawName.replaceAll("\\s", "").toLowerCase();

            double raDegrees = (double) starTable.getCell(0, raIndex);
            double decDegrees = (double) starTable.getCell(0, decIndex);

            return new SimbadTargetResult(targetName, "ICRS", "J2000", raDegrees, decDegrees);
        }
    }



/*    public static
    void saxProcessing(String theUrl) throws Exception {
        TableHandler tableHandler = new TableHandler() {

            long rowCount;

            @Override
            public void startTable(StarTable metadata) throws SAXException {
                //metadata does not contain actual row data - hence the name
                try {
                    printStarTable(metadata);
                } catch (IOException e)
                {
                    System.out.println(e.getMessage());
                }
            }

            @Override
            public void rowData(Object[] row) throws SAXException {
                rowCount++;
                System.out.println(Arrays.toString(row));
            }

            @Override
            public void endTable() throws SAXException {
                System.out.println( rowCount + " rows");
            }
        };

        TableContentHandler tableContentHandler = new TableContentHandler( true );
        tableContentHandler.setTableHandler(tableHandler);

        XMLReader xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();

        xmlReader.setContentHandler(tableContentHandler);

        xmlReader.parse( theUrl );
    }*/

/*    public static
    void domProcessing(String theUrl) throws IOException, SAXException
    {
        VOElement top = new VOElementFactory().makeVOElement(new URL(theUrl));

        NodeList resources = top.getElementsByTagName( "RESOURCE" );
        Element resource = (Element) resources.item(0);

        VOElement vResource = (VOElement) resource;
        VOElement[] tables = vResource.getChildrenByName( "TABLE");
        TableElement tableElement = (TableElement) tables[0];

        try (StarTable starTable = new VOStarTable( tableElement )){
            printStarTable(starTable);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }*/

/*    private static void
    printStarTable(StarTable starTable) throws IOException {
        int nCol = starTable.getColumnCount();
        for ( int iCol = 0; iCol < nCol; iCol++) {
            String colName = starTable.getColumnInfo( iCol ).getName();
            System.out.print( colName + "\t");
        }
        System.out.println();

        for (RowSequence rSeq = starTable.getRowSequence(); rSeq.next();) {
            Object[] row = rSeq.getRow();
            for (int iCol = 0; iCol < nCol; iCol++) {
                System.out.print( row[iCol] + "\t\t");
            }
            System.out.println();
        }
    }*/
}
