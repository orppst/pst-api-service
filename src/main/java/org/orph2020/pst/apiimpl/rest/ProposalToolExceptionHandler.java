package org.orph2020.pst.apiimpl.rest;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ProposalToolExceptionHandler implements ExceptionMapper<ProposalToolException>
{
    @Override
    public Response toResponse(ProposalToolException e)
    {
        return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
    }
}
