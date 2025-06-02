package org.orph2020.pst.apiimpl.rest;

import jakarta.enterprise.context.RequestScoped;
import org.apache.commons.io.FileUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.ivoa.dm.proposal.prop.SupportingDocument;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 *  This is a convenience class bean to help with file I/O and bookkeeping for the document store
 *  of individual proposals
 */
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
                "justifications"
        ));

        //copy the LaTex main file for Justifications to the proposal store
        Files.copy(
                Paths.get("src/main/data/mainTemplate.tex"),
                Paths.get(proposalStoreRoot, proposalCode.toString(),
                        "justifications/main.tex"),
                REPLACE_EXISTING
        );

        //copy the LaTex header file template for Justifications to the proposal store
        Files.copy(
                Paths.get("src/main/data/justificationsHeaderTemplate.tex"),
                Paths.get(proposalStoreRoot, proposalCode.toString(),
                        "justifications/justificationsHeaderTemplate.tex"),
                REPLACE_EXISTING
        );
    }

    /**
     * Removes the subdirectory specified, including its subdirectories.
     * We use this to clean up the document store when a proposal is deleted such that the
     * string parameter must refer to the top-level subdirectory for that proposal.
     * @param proposalDirectory the top-level subdirectory for the proposal being deleted
     * @throws IOException if deletion fails
     */
    public void removeStorePath(String proposalDirectory) throws IOException {
        //this delete is recursive
        FileUtils.deleteDirectory(fetchFile(proposalDirectory));
    }

    /**
     * Copies the contents of directory 'source' to the directory 'destination', this includes subdirectories
     * and files (intention is that 'source' and 'destination' are unique identifiers for proposals)
     * @param source a string representing the source directory or path (not including the store root)
     * @param destination a string representing the destination directory or path (not including the store root)
     * @param supportingDocuments the list of supporting documents from the CLONED proposal to update
     *                            their 'locations' to use the 'destination' path - can be empty
     * @throws IOException if the copy operation fails
     */
    public void copyStore(String source, String destination, List<SupportingDocument> supportingDocuments)
            throws IOException {
        FileUtils.copyDirectory(fetchFile(source), fetchFile(destination));
        supportingDocuments.forEach(s ->
            s.setLocation(s.getLocation().replace(
                    "proposals/" + source,"proposals/" + destination
            ))
        );
    }


    /**
     * Convenience method to fetch the file given from this DocumentStore may refer to a directory
     * (note: 'filePath' can refer to a non-existent file, it will be created)
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
     * Write a given string to the given file. This will overwrite any existing file.
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
     * @param fileExtensions an optional list of file extensions can be left empty or null to list all files
     * @return Set of strings containing the files found
     * @throws IOException thrown by Files.list()
     */
    public Set<String> listFilesIn(String filePath, List<String> fileExtensions) throws IOException {
        try (Stream<Path> stream = Files.list(Paths.get(proposalStoreRoot, filePath))) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(java.nio.file.Path::getFileName)
                    .map(java.nio.file.Path::toString)
                    .filter(f -> {
                        if (fileExtensions != null && !fileExtensions.isEmpty()) {
                            return fileExtensions.stream().anyMatch(f::endsWith);
                        } else {
                            return true;
                        }
                    })
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
