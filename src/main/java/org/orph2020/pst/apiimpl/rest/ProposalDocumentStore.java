package org.orph2020.pst.apiimpl.rest;

import jakarta.enterprise.context.RequestScoped;
import org.apache.commons.io.FileUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequestScoped
public class ProposalDocumentStore {

    @ConfigProperty(name = "document-store.proposals.root")
    String proposalStoreRoot;


    /**
     * Creates the subdirectory structure for this Store from the given parameter.
     * This should be called only once from the "createProposal" API method
     * @param proposalCode the related proposal ID for which you are creating the store
     * @throws IOException I/O exception
     */
    public void createStorePaths(Long proposalCode) throws IOException {
        Files.createDirectories(Paths.get(
                proposalStoreRoot,
                proposalCode.toString(),
                "supportingDocuments"
        ));

        Files.createDirectories(Paths.get(
                proposalStoreRoot,
                proposalCode.toString(),
                "justifications/scientific"
        ));

        Files.createDirectories(Paths.get(
                proposalStoreRoot,
                proposalCode.toString(),
                "justifications/technical"
        ));
    }

    /**
     * Removes the subdirectory, including its subdirectories, specified.
     * We use this to clean up the document store when a proposal is deleted such that the
     * string parameter must refer to the top-level subdirectory for that proposal.
     * @param proposalDirectory the top-level subdirectory for the proposal being deleted
     * @throws IOException if deletion fails
     */
    public void removeStorePath(String proposalDirectory) throws IOException {
        //this is a recursive delete
        FileUtils.deleteDirectory(fetchFile(proposalDirectory));
    }


    /**
     * Convenience method to fetch the file given from this DocumentStore, may refer to a directory
     * (note: 'filePath' can refer to a non-existent file)
     * @param filePath filename of the file to fetch, can have optional parent paths
     * @return the file identified by the filepath in this DocumentStore
     */
    public File fetchFile(String filePath) {
        return new File(proposalStoreRoot, filePath);
    }

    /**
     * Convenience method to move a file to the path given
     * @param file "External" file to save to this DocumentStore (typically from a file upload)
     * @param saveFileAs the filePath you are saving the file as, note you are responsible for providing the
     *                   correct path of the subdirectories.
     * @return boolean status of the save
     */
    public Boolean moveFile(File file, String saveFileAs) throws RuntimeException {
        // renameTo does not create intermediate subdirectories so we need to check the input
        int lastSlash = saveFileAs.lastIndexOf("/");
        if (lastSlash != -1) {
            File subdirectories = fetchFile(saveFileAs.substring(0, lastSlash));
            if (!subdirectories.exists()) {
                if (!subdirectories.mkdirs()) {
                    throw new RuntimeException("Unable to create parent directory: " +
                            subdirectories.getAbsolutePath());
                }
            }
        }
        return file.renameTo(fetchFile(saveFileAs));
    }

    /**
     * Write a given string to the given file
     * @param theString the string you wish to write to file
     * @param filePath the path of the subdirectories to the file to which you will be writing
     * @throws IOException I/O exception from the writer object
     */
    public void writeStringToFile(String theString, String filePath) throws IOException{
        //either create the file and write to it, or just overwrite the existing file
        try (FileWriter fw = new FileWriter(fetchFile(filePath))) {
            fw.write(theString);
        }
    }

    /**
     * List files in the given directory, optionally provide a non-empty array of specific file
     * extension strings to look for
     * @param filePath where to look
     * @param fileExtensions optional list of file extensions, can be left empty to list all files
     * @return Set of strings containing the files found
     * @throws IOException thrown by Files.list()
     */
    public Set<String> listFilesIn(String filePath, String[] fileExtensions) throws IOException {
        try (Stream<Path> stream = Files.list(Paths.get(proposalStoreRoot, filePath))) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(java.nio.file.Path::getFileName)
                    .map(java.nio.file.Path::toString)
                    .filter(f -> Arrays.stream(fileExtensions).anyMatch(f::endsWith))
                    .collect(Collectors.toSet());
        }

    }

    /**
     * Convenience method to delete a file or directory from this ProposalDocumentStore
     * @param filePath the subdirectory path to the file you wish to remove from this store
     * @return boolean status of the deletion
     */
    public Boolean deleteFile(String filePath) {
        return fetchFile(filePath).delete();
    }

}
