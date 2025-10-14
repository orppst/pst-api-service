package org.orph2020.pst.apiimpl.rest;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.io.FileUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.ivoa.dm.proposal.prop.SupportingDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
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
     * @throws URISyntaxException thrown when trying to access resource files as Files
     */
    public String createLatexWorkingDirectory(
            Long proposalCode,
            String proposalTitle,
            String observingCycleName,
            String scientificText,
            String technicalText,
            String referencesFilename
    ) throws IOException, URISyntaxException {

        //creates all non-existent parent directories
        Files.createDirectories(Paths.get(
                proposalStoreRoot,
                proposalCode.toString(),
                justificationsPath
        ));

        //copy the LaTex main file for Justifications to the working directory
        Files.copy(
                Objects.requireNonNull(ProposalDocumentStore.class.getResourceAsStream("/mainTemplate.tex")),
                Paths.get(proposalStoreRoot, proposalCode.toString(),
                        justificationsPath, "main.tex"),
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

        //copies and modifies the <references>.bib to "refs.bib" in the working directory (if it exists)
        if (referencesFilename != null) {
            copyAndModifyReferences(proposalCode, referencesFilename);
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

       final File dest = fetchFile(saveFileAs);
        logger.debug("Moving file {}  exists {} to {}", file, file.exists(), dest );
       try {
          Files.move(file.toPath(),dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
          return true;
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
            throws IOException, URISyntaxException {
        String proposalTitleTarget = "PROPOSAL-TITLE-HERE";
        String cycleCodeTarget = "CYCLE-ID-HERE";

        //read from this file
        File templateHeader = new File(Objects.requireNonNull(
                ProposalDocumentStore.class.getResource("/justificationsHeaderTemplate.tex")).toURI()
        );

        String templateText = new String(Files.readAllBytes(templateHeader.toPath()));

        String headerText = observingProposalName != null ?
                templateText.replace(proposalTitleTarget, proposalTitle)
                        .replace(cycleCodeTarget, observingProposalName)
                :
                templateText.replace(proposalTitleTarget, proposalTitle);

        writeStringToFile(headerText, proposalCode + "/" + justificationsPath + "justificationsHeader.tex");
    }



    private void copyAndModifyReferences(Long proposalCode, String referencesFilename)
            throws IOException {
        // I don't know who said it first but acronyms really are the bane of modern life!

        // Automatically exported references tend to give journal acronyms as a latex macro
        // e.g. 'journal = {\mnras}' meaning you must have that macro, \mnras, defined in your
        // .tex file/s somewhere to print the journal title however you want it printed.
        // This is fine for articles were you're in control of the bibliography; you simply
        // provide the macro definition. As we are providing a generalised main.tex template
        // this is not so straightforward.
        // We either have to define any and all possible journal acronyms as macros in the
        // 'main.tex' template, this is impractical,
        // --OR--
        // ask users to provide the macro definitions for the references they have cited,
        // taking up precious character space in their justifications (and is tedious, and even possible?)
        // --OR--
        // we programmatically edit the references here, replacing any "journal = {\xxx}" with
        // "journal = XXX", but there are exceptions - see below
        // None of these "solutions" are ideal.

        // Rather disappointingly the macro for "Astrophysics and Space Science" is not "\ass" but "\apss"
        // because generally Astrophysics is shortened to "Ap". So in general the string "ap" needs to be
        // replaced with "Ap" not "AP".
        // Even so, there are exceptions to this exception. For example, the acronym for
        // "Astronomy & Astrophysics" is "A&A" rather than "AAp", the macro for which is "\aap".
        // This is further complicated by "A&A" also having "review" and "supplementary" versions;
        // "\aapr" and "\aaps" respectively.
        // Another exception is the macro "\nat", which should be replaced with "Nature" not "NAT".
        // There are potentially other exceptions. So what could possibly go wrong? :P


        //get the original references.bib upload
        File references = fetchFile(proposalCode + "/" + supportingDocumentsPath + referencesFilename);

        Scanner theScanner = new Scanner(references);
        StringBuilder newContent = new StringBuilder();

        while (theScanner.hasNextLine()) {
            String line = theScanner.nextLine();
            // I LOVE Java regex strings, they're SO intuitive!
            // We're matching for ' journal = {\<macro>},' where there could be zero or more spaces
            if (line.matches(" *journal *= *\\{\\\\[a-z]+},$")) {
                String acronym = line.substring(line.indexOf("{\\") + 1, line.lastIndexOf("}"));
                switch (acronym) {
                    case "\\aap":
                        newContent.append(line.replace("\\aap", "A&A"));
                        break;
                    case "\\aapr":
                        newContent.append(line.replace("\\aapr", "A&A Rev."));
                        break;
                    case "\\aaps":
                        newContent.append(line.replace("\\aaps", "A&A Sup."));
                        break;
                    case "\\jcap":
                        //Journal of Cosmology and Astroparticle Physics (avoids "AP" -> "Ap" issue)
                        newContent.append(line.replace("\\jcap", "JCAP"));
                        break;
                    case "\\nat":
                        newContent.append(line.replace("\\nat", "Nature"));
                        break;
                    default: {
                        String newLine = line.replace(
                                acronym, acronym.substring(acronym.indexOf("\\") + 1).toUpperCase());
                        if (acronym.contains("ap")) {
                            newContent.append(newLine.replace("AP", "Ap"));
                        } else {
                            newContent.append(newLine);
                        }
                        break;
                    }
                }
            } else {
                newContent.append(line);
            }
            newContent.append("\n");
        }
        theScanner.close();

        //write result to the justifications working directory
        writeStringToFile(newContent.toString(),
                proposalCode + "/" + justificationsPath + "refs.bib");
    }


}
