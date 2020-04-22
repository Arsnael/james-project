/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.mailbox.quota.task;

import java.time.Instant;

import org.apache.james.json.DTOModule;
import org.apache.james.server.task.json.dto.AdditionalInformationDTO;
import org.apache.james.server.task.json.dto.AdditionalInformationDTOModule;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

public class RecomputeCurrentQuotasTaskAdditionalInformationDTO implements AdditionalInformationDTO {
    private static RecomputeCurrentQuotasTaskAdditionalInformationDTO fromDomainObject(RecomputeCurrentQuotasTask.Details details, String type) {
        return new RecomputeCurrentQuotasTaskAdditionalInformationDTO(
            type,
            details.getProcessedQuotaRoots(),
            details.getFailedQuotaRoots(),
            details.timestamp());
    }

    public static final AdditionalInformationDTOModule<RecomputeCurrentQuotasTask.Details, RecomputeCurrentQuotasTaskAdditionalInformationDTO> MODULE =
        DTOModule
            .forDomainObject(RecomputeCurrentQuotasTask.Details.class)
            .convertToDTO(RecomputeCurrentQuotasTaskAdditionalInformationDTO.class)
            .toDomainObjectConverter(RecomputeCurrentQuotasTaskAdditionalInformationDTO::toDomainObject)
            .toDTOConverter(RecomputeCurrentQuotasTaskAdditionalInformationDTO::fromDomainObject)
            .typeName(RecomputeCurrentQuotasTask.RECOMPUTE_CURRENT_QUOTAS.asString())
            .withFactory(AdditionalInformationDTOModule::new);

    private final String type;
    private final long processedQuotaRoots;
    private final ImmutableList<String> failedQuotaRoots;
    private final Instant timestamp;

    public RecomputeCurrentQuotasTaskAdditionalInformationDTO(@JsonProperty("type") String type,
                                                              @JsonProperty("processedQuotaRoots") long processedQuotaRoots,
                                                              @JsonProperty("failedQuotaRoots") ImmutableList<String> failedQuotaRoots,
                                                              @JsonProperty("timestamp") Instant timestamp) {
        this.type = type;
        this.processedQuotaRoots = processedQuotaRoots;
        this.failedQuotaRoots = failedQuotaRoots;
        this.timestamp = timestamp;
    }

    public long getProcessedQuotaRoots() {
        return processedQuotaRoots;
    }

    public ImmutableList<String> getFailedQuotaRoots() {
        return failedQuotaRoots;
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String getType() {
        return type;
    }

    private RecomputeCurrentQuotasTask.Details toDomainObject() {
        return new RecomputeCurrentQuotasTask.Details(timestamp,
            processedQuotaRoots,
            failedQuotaRoots);
    }
}
