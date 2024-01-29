package org.orph2020.pst.apiimpl.rest;

import jakarta.ws.rs.WebApplicationException;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ProposalUploader {

    // the logger.
    private static final Logger logger =
        Logger.getLogger(ProposalUploader.class.getName());

    // hard coded filename for the proposal in json format.
    static String PROPOSAL_JSON_FILE_NAME = "proposal.json";

    /**
     * default constructor with logger.
     *
     */
    public ProposalUploader() {}

    /**
     * entrance method for uploading a proposal.
     *
     * @param fileUpload zip file.
     * @param updateSubmittedFlag if we should remove the submitted flag.
     * @throws WebApplicationException when:
     * no file is found: 400
     */
    public static void uploadProposal(
            FileUpload fileUpload, String updateSubmittedFlag)
            throws WebApplicationException {
        byte[] proposalData = ProposalUploader.readFile(
            fileUpload, ProposalUploader.PROPOSAL_JSON_FILE_NAME);
        JSONObject proposalJSON = new JSONObject(new String(proposalData));
        logger.info("proposal json looks like this: ");
        logger.info(proposalJSON);
        logger.info("proposal json ended");
    }

    /**
     * method for locating a given file within a zip file.
     *
     * @param fileUpload the zip file.
     * @param filename the file to locate.
     * @return a byte [] with the data, or null if no file was found.
     */
    private static byte[] readFile(FileUpload fileUpload, String filename) {
        try {
            FileInputStream fileInputStream = new FileInputStream(
                fileUpload.uploadedFile().toFile());
            ZipInputStream zipInputStream = new ZipInputStream(fileInputStream);
            ZipEntry entry = zipInputStream.getNextEntry();

            // cycle the entries until either found or no more entries.
            while (entry != null) {
                // test filename for name we're looking for
                if (entry.getName().equals(filename)) {
                    int uncompressedSize = Math.toIntExact(entry.getSize());

                    // build buffer with uncompressed size.
                    byte[] fileData = new byte[uncompressedSize];
                    int bytesRead = zipInputStream.read(
                        fileData, 0, uncompressedSize);
                    if (bytesRead != uncompressedSize && bytesRead != -1) {
                        throw new WebApplicationException(
                            "Failed to read the correct amount of data. read "
                                + bytesRead + " instead", 400);
                    }

                    // close the streams before handing back the results.
                    zipInputStream.close();
                    fileInputStream.close();

                    // return raw data
                    return fileData;
                }

                // not right file, move on
                entry = zipInputStream.getNextEntry();
            }

            // no file found.
            zipInputStream.close();
            fileInputStream.close();
            return null;

        } catch (FileNotFoundException e) {
            throw new WebApplicationException("file not found error", 400);
        } catch (IOException e) {
            throw new WebApplicationException(e, 400);
        }
    }
}
