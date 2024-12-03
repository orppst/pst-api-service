package org.orph2020.pst.apiimpl.rest;

import java.nio.file.InvalidPathException;

public class SupportingDocumentsStore extends DocumentStore
{
    public SupportingDocumentsStore(Long proposalId) throws InvalidPathException {
        super(proposalId, "supportingDocuments");
    }

}
