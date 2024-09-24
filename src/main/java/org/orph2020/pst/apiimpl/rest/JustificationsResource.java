package org.orph2020.pst.apiimpl.rest;


import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.ivoa.dm.proposal.prop.Justification;
import org.ivoa.dm.proposal.prop.ObservingProposal;
import org.ivoa.dm.proposal.prop.TextFormats;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

/*
    Dev note: there are two "types" of Justification: 'scientific' and 'technical', and these
    are NOT separate Java Classes. They are different Justification members of the
    ObservingProposal class. The Rest end-points specify "which" one we are using, and we deal
    with this is in the implementation. It is HIGHLY UNLIKELY that more "types" of Justification
    will be added as members to the ObservingProposal class, so simple 'if' and 'switch'
    constructs are okay to use.
 */

@Path("proposals/{proposalCode}/justifications")
public class JustificationsResource extends ObjectResourceBase {

    @ConfigProperty(name= "supporting-documents.store-root")
    String documentStoreRoot;

    //common file name for the Tex file for Latex type Justifications
    String texFileName = "main.tex";

    //convenience function to get the specific Justification
    // - note that here the return value can be null
    private Justification getWhichJustification(Long proposalCode, String which) {
        ObservingProposal observingProposal = findObject(ObservingProposal.class, proposalCode);
        return switch (which) {
            case "technical" -> observingProposal.getTechnicalJustification();
            case "scientific" -> observingProposal.getScientificJustification();
            default -> throw new WebApplicationException(
                    String.format("Justifications are either 'technical' or 'scientific', I got '%s'", which),
                    400
            );
        };
    }

    //convenience function to write a latex string to file - assume calling function has checked
    //that the format of the Justification is LATEX, thus is sending a "Latex" string
    private void writeLatexToFile(String latexText, Long proposalCode, String which)
    throws IOException {

        //use rest end-point path for the storage location
        String filePath = documentStoreRoot
                + "/proposals/"
                + proposalCode
                + "/justifications/"
                + which
                + "/" + texFileName;

        //To consider: BufferedWriter for performance?

        //either create the file and write to it, or overwrite the existing file
        try (FileWriter fw = new FileWriter(filePath)) {
            fw.write(latexText);
        }
    }

    @GET
    @Path("{which}")
    @Operation(summary = "get the technical or scientific justification associated with the ObservingProposal specified by 'proposalCode'")
    public Justification getJustification(@PathParam("proposalCode") Long proposalCode,
                                          @PathParam("which") String which)
            throws WebApplicationException
    {
        ObservingProposal observingProposal = findObject(ObservingProposal.class, proposalCode);

        //avoid returning nulls to frontend clients
        return switch (which) {
            case "technical" -> {
                Justification technical = observingProposal.getTechnicalJustification();
                yield Objects.requireNonNullElseGet(technical,
                        () -> new Justification("", TextFormats.ASCIIDOC));
            }
            case "scientific" -> {
                Justification scientific = observingProposal.getScientificJustification();
                yield Objects.requireNonNullElseGet(scientific,
                        () -> new Justification("", TextFormats.ASCIIDOC));
            }
            default -> throw new WebApplicationException(
                    String.format("Justifications are either 'technical' or 'scientific', I got '%s'", which),
                    400
            );
        };
    }


    @PUT
    @Operation( summary = "update a technical or scientific Justification in the ObservingProposal specified by the 'proposalCode'")
    @Path("{which}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn={WebApplicationException.class})
    public Justification updateJustification(
            @PathParam("proposalCode") long proposalCode,
            @PathParam("which") String which,
            Justification incoming )
            throws WebApplicationException
    {
        Justification justification = getWhichJustification(proposalCode, which);

        if (justification == null)
            throw new WebApplicationException(
                    String.format(
                            "The Proposal has no existing %s justification, please 'add' one",
                            which
                    )
            );

        justification.updateUsing(incoming);

        em.merge(justification);

        //if justification format is Latex then update the *.tex file
        if (justification.getFormat() == TextFormats.LATEX) {
            try {
                writeLatexToFile(justification.getText(), proposalCode, which);
            } catch (IOException e) {
                //if we can't write the update to the *.tex file then we should roll back
                //the database transaction - otherwise may have mismatch between the database string
                //and the *.tex file.
                throw new WebApplicationException(e.getMessage());
            }
        }

        return justification;
    }


    @POST
    @Operation( summary = "add a technical or scientific Justification to the ObservingProposal specified by the 'proposalCode'")
    @Path("{which}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional(rollbackOn={WebApplicationException.class})
    public Justification addJustification(
            @PathParam("proposalCode") long proposalCode,
            @PathParam("which") String which,
            Justification incoming )
            throws WebApplicationException
    {
        ObservingProposal proposal = findObject(ObservingProposal.class, proposalCode);
        Justification justification = getWhichJustification(proposalCode, which);

        if (justification != null) {
            throw new WebApplicationException(
                    String.format(
                            "Proposal has an existing %s Justification, please use the 'update' method instead",
                            which
                    )
            );
        }

        Justification persisted =
                addNewChildObject(
                        proposal,
                        incoming,
                        which.equals("technical") ?
                                proposal::setTechnicalJustification :
                                proposal::setScientificJustification
        );

        //create the directory for latex stuff even if the Justification is not latex format
        try {
            System.out.println(Files.createDirectories(Paths.get(
                    documentStoreRoot,
                    "proposals",
                    Long.toString(proposalCode),
                    "justifications",
                    which)
            ));
            //create and write the string to file if it is latex format
            if (persisted.getFormat() == TextFormats.LATEX) {
                writeLatexToFile(persisted.getText(), proposalCode, which);
            }
        } catch (IOException e) {
            //if we can't write the justification text to the *.tex file then we should roll back
            //the database transaction - otherwise may have mismatch between the database string
            //and the *.tex file.
            throw new WebApplicationException(e.getMessage());
        }


        return persisted;
    }
}
