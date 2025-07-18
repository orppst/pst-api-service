package org.orph2020.pst.apiimpl;


/*
 * Created on 18/07/2025 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import org.ivoa.dm.proposal.management.ProposalCycle;

/**
 * The interface for a proposal code generator.
 */
public interface ProposalCodeGenerator {
   /**
    * generate a unique proposal code.
    * @return The string representation of the proposal code.
    */
   public String generateProposalCode(ProposalCycle proposalCycle);
}
