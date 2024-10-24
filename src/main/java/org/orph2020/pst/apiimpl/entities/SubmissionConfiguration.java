package org.orph2020.pst.apiimpl.entities;
/*
 * Created on 22/10/2024 by Paul Harrison (paul.harrison@manchester.ac.uk).
 */

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.ivoa.dm.proposal.management.ObservationConfiguration;

import java.util.List;

/**
 * Specify the submission parameters in the API.
 */
@Schema(
      description = "The submission configuration in terms of IDs "
)
public class SubmissionConfiguration {
    public long proposalId;
    public List<ObservationConfigMapping> config;



    public SubmissionConfiguration(long proposalId, List<ObservationConfigMapping> config) {
               
        this.proposalId = proposalId;
        this.config = config;
    }

    @Schema(
          description = "the mapping between a list of observationIDs and ObservationMode id "
    )
    public static class ObservationConfigMapping {
        public List<Long> observationIds;
        public long modeId;

        public ObservationConfigMapping(List<Long> observationIds, long modeId) {
            this.observationIds = observationIds;
            this.modeId = modeId;
        }
    }
}

