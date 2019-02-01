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

package org.apache.james.rrt.cassandra.migration;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.james.backends.cassandra.migration.Migration;
import org.apache.james.rrt.cassandra.CassandraMappingsSourcesDAO;
import org.apache.james.rrt.cassandra.CassandraRecipientRewriteTableDAO;
import org.apache.james.rrt.lib.Mapping;
import org.apache.james.rrt.lib.MappingSource;
import org.apache.james.task.Task;
import org.apache.james.task.TaskExecutionDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import reactor.core.publisher.Mono;

public class MappingsSourcesMigration implements Migration {
    private static final Logger LOGGER = LoggerFactory.getLogger(MappingsSourcesMigration.class);
    private static final String TYPE = "mappingsSourcesMigration";

    public static class AdditionalInformation implements TaskExecutionDetails.AdditionalInformation {
        private final AtomicLong successfulMappingsCount;
        private final AtomicLong errorMappingsCount;

        AdditionalInformation() {
            this.successfulMappingsCount = new AtomicLong(0);
            this.errorMappingsCount = new AtomicLong(0);
        }

        public long getSuccessfulMappingsCount() {
            return successfulMappingsCount.get();
        }

        public long getErrorMappingsCount() {
            return errorMappingsCount.get();
        }

        @JsonIgnore
        void incrementSuccessfulCount() {
            successfulMappingsCount.incrementAndGet();
        }

        @JsonIgnore
        void incrementErrorCount() {
            errorMappingsCount.incrementAndGet();
        }
    }

    private final CassandraRecipientRewriteTableDAO cassandraRecipientRewriteTableDAO;
    private final CassandraMappingsSourcesDAO cassandraMappingsSourcesDAO;
    private final AdditionalInformation additionalInformation;

    @Inject
    public MappingsSourcesMigration(CassandraRecipientRewriteTableDAO cassandraRecipientRewriteTableDAO,
                                    CassandraMappingsSourcesDAO cassandraMappingsSourcesDAO) {
        this.cassandraRecipientRewriteTableDAO = cassandraRecipientRewriteTableDAO;
        this.cassandraMappingsSourcesDAO = cassandraMappingsSourcesDAO;
        this.additionalInformation = new AdditionalInformation();
    }

    @Override
    public Result run() {
        return cassandraRecipientRewriteTableDAO.getAllMappings()
            .flatMap(this::migrate)
            .reduce(Result.COMPLETED, Task::combine)
            .doOnError(e -> LOGGER.error("Error while migrating mappings sources", e))
            .onErrorResume(e -> Mono.just(Result.PARTIAL))
            .block();
    }

    private Mono<Result> migrate(Pair<MappingSource, Mapping> mappingEntry) {
        return cassandraMappingsSourcesDAO.addMapping(mappingEntry.getRight(), mappingEntry.getLeft())
            .map(any -> Result.COMPLETED)
            .doOnSuccess(success -> additionalInformation.incrementSuccessfulCount())
            .doOnError(e -> {
                LOGGER.error("Error while performing migration of mappings sources", e);
                additionalInformation.incrementErrorCount();
            })
            .onErrorResume(e -> Mono.just(Result.PARTIAL));
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Optional<TaskExecutionDetails.AdditionalInformation> details() {
        return Optional.of(additionalInformation);
    }

    public AdditionalInformation getAdditionalInformation() {
        return additionalInformation;
    }
}
