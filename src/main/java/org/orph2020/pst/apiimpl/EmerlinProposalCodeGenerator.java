package org.orph2020.pst.apiimpl;


/*
 * Created on 18/07/2025 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import jakarta.enterprise.context.ApplicationScoped;
import org.ivoa.dm.proposal.management.ProposalCycle;

@ApplicationScoped
public class EmerlinProposalCodeGenerator implements ProposalCodeGenerator {

   @Override
   public String generateProposalCode(ProposalCycle proposalCycle) {
      return proposalCycle.getCode()+proposalCycle.getAllocatedProposals().size()+1;//FIXME this needs to be something that actually matches what emerlin needs.
   }
}
