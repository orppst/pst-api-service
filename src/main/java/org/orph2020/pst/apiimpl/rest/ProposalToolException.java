package org.orph2020.pst.apiimpl.rest;

import java.io.Serializable;

public class ProposalToolException extends Exception implements Serializable
{
    private static final long serialVersionUID = 1L;

    public ProposalToolException() {
        super();
    }

    public ProposalToolException(String message) {
        super(message);
    }

    public ProposalToolException(String message, Exception e){
        super(message, e);
    }

}
