package org.orph2020.pst.apiimpl.rest;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.io.FileUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.ivoa.dm.proposal.prop.SupportingDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 *  This is a convenience class bean to help with file I/O and bookkeeping for the document store
 *  of individual proposals
 */
@ApplicationScoped
public class ProposalDocumentStore {

     static final Logger logger = LoggerFactory.getLogger(ProposalDocumentStore.class.getName());

    @ConfigProperty(name = "document-store.root")
    String proposalStoreRoot;

    @ConfigProperty(name = "document-store.supportingDocuments-path")
    String supportingDocumentsPath;

    @ConfigProperty(name = "document-store.justifications-path")
    String justificationsPath;


    /**
     * Creates the subdirectory structure for this Store from the given parameter.
     * This should be called only once from the "createProposal" API method
     * @param proposalCode the related proposal ID for which you are creating the store
     * @throws IOException I/O exception
     */
    public void createStorePaths(Long proposalCode) throws IOException {

        //creates all non-existent parent directories
        Files.createDirectories(Paths.get(
                proposalStoreRoot,
                proposalCode.toString(),
                supportingDocumentsPath
        ));
    }

    /**
     * Creates the working directory for 'latexmk' to produce PDF output from the given Justifications data.
     * @param proposalCode the ID of the proposal
     * @param proposalTitle the title of the proposal (inserted into the header)
     * @param observingCycleName the cycle name (or code) to be inserted when a proposal is submitted; can be null
     * @param scientificText the user supplied text for the scientific justification
     * @param technicalText the user supplied text for the technical justification
     * @param referencesFilename the user supplied '.bib' file
     * @return the path to the created working directory
     * @throws IOException thrown from read/write operations
     */
    public String createLatexWorkingDirectory(
            Long proposalCode,
            String proposalTitle,
            String observingCycleName,
            String scientificText,
            String technicalText,
            String templateFilename,
            String referencesFilename
    ) throws IOException {

        //creates all non-existent parent directories
        Files.createDirectories(Paths.get(
                proposalStoreRoot,
                proposalCode.toString(),
                justificationsPath
        ));

        //copy the LaTex main file for Justifications to the working directory
        Files.copy(
                Objects.requireNonNull(ProposalDocumentStore.class.getResourceAsStream("/" + templateFilename)),
                Paths.get(proposalStoreRoot, proposalCode.toString(),
                        justificationsPath, "main.tex"),
                REPLACE_EXISTING
        );

        //copy the journal abbreviation definitions tex file to the working directory
        Files.copy(
               Objects.requireNonNull(
                       ProposalDocumentStore.class.getResourceAsStream("/astronomyJournalAbbreviations.tex")),
                Paths.get(proposalStoreRoot, proposalCode.toString(),
                        justificationsPath, "astronomyJournalAbbreviations.tex"),
                REPLACE_EXISTING
        );

        //copy the bibliography style file for Justifications to the working directory
        Files.copy(
                Objects.requireNonNull(ProposalDocumentStore.class.getResourceAsStream("/polaris.bst")),
                Paths.get(proposalStoreRoot, proposalCode.toString(),
                        justificationsPath, "polaris.bst"),
                REPLACE_EXISTING
        );

        //copies and modifies the header tex file into working directory (observingCycleName can be null)
        insertTitleAndCycleCodeIntoHeaderTex(proposalCode, proposalTitle, observingCycleName);

        //copy the <references.bib> to "refs.bib" in the working directory (if it exists)
        if (referencesFilename != null) {
            Files.copy(
                    Paths.get(proposalStoreRoot, proposalCode.toString(),
                            supportingDocumentsPath, referencesFilename),
                    Paths.get(proposalStoreRoot, proposalCode.toString(),
                            justificationsPath, "refs.bib")
            );
        }

        writeStringToFile(scientificText, proposalCode + "/" + justificationsPath
                + "scientificJustification.tex");

        writeStringToFile(technicalText, proposalCode + "/" + justificationsPath
                + "technicalJustification.tex");

        //image files are found using the '\graphicspath' latex command in "main.tex"

        return proposalStoreRoot + proposalCode + "/" + justificationsPath;
    }

    public String getStoreRoot() {
        return proposalStoreRoot;
    }

    public String getSupportingDocumentsPath(Long proposalCode) {
        return proposalCode.toString() + "/" + supportingDocumentsPath;
    }

    public String getJustificationsPath(Long proposalCode) {
        return proposalCode.toString() + "/" + justificationsPath;
    }

    /**
     * Recursively removes the subdirectory specified
     * @param proposalDirectory the (sub)directory being deleted
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
                    proposalStoreRoot + source,proposalStoreRoot + destination
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
     * Throws a Runtime Exception if the move failed
     */
    public void moveFile(File file, String saveFileAs) throws RuntimeException {
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

       final File dest = fetchFile(saveFileAs);
        logger.debug("Moving file {}  exists {} to {}", file, file.exists(), dest );
        if(dest.exists()) {
            boolean result = dest.delete();
            if(!result)
                System.out.println("Delete existing file");
            else
                System.out.println("Failed? to delete existing file");
        }
       else
           System.out.println("File " + dest.getAbsolutePath() + " does not exist");

       try {
          Files.move(file.toPath(),dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
       } catch (DirectoryNotEmptyException e) {
           System.err.println("Target directory is not empty.");
       } catch (SecurityException e) {
           System.err.println("Security exception.");
       } catch (UnsupportedOperationException e) {
           System.err.println("Unsupported operation.");
       } catch (FileAlreadyExistsException e) {
           System.err.println("File already exists.");
       } catch (IOException e) {
          throw new RuntimeException(e);

       }
    }

    /**
     * Write a given string to the given file. This will overwrite an existing file or create a new file.
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


    private void insertTitleAndCycleCodeIntoHeaderTex(
            Long proposalCode,
            String proposalTitle,
            String observingProposalName
    )
            throws IOException {
        String proposalTitleTarget = "PROPOSAL-TITLE-HERE";
        String cycleCodeTarget = "CYCLE-ID-HERE";

        //read from this file
        try (InputStream is = Objects.requireNonNull(
                ProposalDocumentStore.class.getResourceAsStream("/justificationsHeaderTemplate.tex"))) {

           try (InputStreamReader isr = new InputStreamReader(is);
                BufferedReader reader = new BufferedReader(isr)) {
              String templateText = reader.lines().collect(Collectors.joining(System.lineSeparator()));

              String headerText = observingProposalName != null ?
                    templateText.replace(proposalTitleTarget, proposalTitle)
                          .replace(cycleCodeTarget, observingProposalName)
                    :
                    templateText.replace(proposalTitleTarget, proposalTitle);

              writeStringToFile(headerText, proposalCode + "/" + justificationsPath + "justificationsHeader.tex");
           }
        }
    }
}
