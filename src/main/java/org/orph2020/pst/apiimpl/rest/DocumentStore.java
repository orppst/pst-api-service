package org.orph2020.pst.apiimpl.rest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

abstract class DocumentStore {

    /*
        This class isn't a Bean so trying to use annotation:
            @ConfigProperty(name="document-store.root")
        is meaningless; results in 'documentStoreRoot' being null
     */
    String documentStoreRoot = "/tmp/documentStore/";

    protected final Path storeRootPath;

    public DocumentStore(Long proposalId, String subdirectories)
            throws InvalidPathException {

        //here we assume the 'proposalId' is unique

        storeRootPath = Paths.get(
                documentStoreRoot,
                "proposals",
                proposalId.toString(),
                subdirectories
        );
    }

    /**
     * Creates the directory structure from the given constructor parameters.
     * This should be called only once for each unique constructor parameter pairs
     * in the "createProposal" API method
     *
     * @throws IOException I/O exception
     */
    public void createDirectories() throws IOException {
        Files.createDirectories(storeRootPath);
    }

    /**
     * Convenience method to fetch the file given from this DocumentStore, may refer to a directory
     * (note: 'filePath' can refer to a non-existent file when "saving" a new file)
     * @param filePath filename of the file to fetch, can have optional parent paths
     * @return the file identified by the filepath in this DocumentStore
     */
    public File fetchFile(String filePath) {
        return new File(
                storeRootPath.toString(),
                filePath
        );
    }

    /**
     * Convenience method to move a file to the root path defined in this DocumentStore
     * @param file "External" file to save to this DocumentStore (typically from a file upload)
     * @param saveFileAs the filename you are saving the file as to this DocumentStore
     * @return boolean status of the save
     */
    public Boolean moveFile(File file, String saveFileAs) throws RuntimeException {
        // renameTo does not create intermediate subdirectories so we need to check the input
        int lastSlash = saveFileAs.lastIndexOf("/");
        if (lastSlash != -1) {
            File subdirectories = fetchFile(saveFileAs.substring(0, lastSlash));
            if (subdirectories.exists()) {
                throw new RuntimeException(subdirectories.getAbsolutePath() + " already exists");
            }
            if (!subdirectories.mkdirs()) {
                throw new RuntimeException(subdirectories.getAbsolutePath() + " not created");
            }
        }
        return file.renameTo(fetchFile(saveFileAs));
    }

    /**
     * Convenience method to delete a file or directory from this DocumentStore
     * @param filePath String of the partial path to the file or subdirectory to delete from this DocumentStore
     * @return boolean status of the deletion
     */
    public Boolean deleteFile(String filePath) {
        return fetchFile(filePath).delete();
    }
}
