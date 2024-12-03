package org.orph2020.pst.apiimpl.rest;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JustificationStore extends DocumentStore{

    public JustificationStore(Long proposalId, String which) throws InvalidPathException {
        super(proposalId, "justifications/" + which);
    }

    public void writeLatexStringToFile(String latexString) throws IOException{
        //either create the file and write to it, or just overwrite the existing file
        try (FileWriter fw = new FileWriter(fetchFile("main.tex"))) {
            fw.write(latexString);
        }
    }

    public Set<String> listResourceFiles() throws IOException {
        try (Stream<Path> stream = Files.list(Paths.get(storeRootPath.toString()))) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(java.nio.file.Path::getFileName)
                    .map(java.nio.file.Path::toString)
                    .filter(string -> string.endsWith(".bib")
                            || string.endsWith(".png")
                            || string.endsWith(".jpg")
                            || string.endsWith(".jpeg")
                            || string.endsWith(".eps")
                    )
                    .collect(Collectors.toSet());
        }

    }
}
