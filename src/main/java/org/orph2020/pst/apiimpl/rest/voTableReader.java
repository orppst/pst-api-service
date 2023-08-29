/*
 FIXME: WIP - simply tests the Starlink Tables Infrastructure Library using the Generic, SAX and DOM interfaces
                we need to extract the relevant information to create a proposal::Target
 */

package org.orph2020.pst.apiimpl.rest;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.votable.*;


import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;

public class voTableReader {

    private static void
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
    }

    public static
    void genericProcessing(String theUrl) throws Exception {
        VOTableBuilder voTableBuilder = new VOTableBuilder();
        DataSource dataSource = DataSource.makeDataSource(new URL(theUrl));
        StoragePolicy storagePolicy = StoragePolicy.getDefaultPolicy();

        try (StarTable starTable = voTableBuilder.makeStarTable(dataSource, false, storagePolicy))
        {
            printStarTable(starTable);
        }
        catch(Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

    public static
    void saxProcessing(String theUrl) throws Exception {
        TableHandler tableHandler = new TableHandler() {

            long rowCount;

            @Override
            public void startTable(StarTable metadata) throws SAXException {
                rowCount = 0;
                System.out.println("Table: " + metadata.getName());

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
    }

    public static
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
    }
}
