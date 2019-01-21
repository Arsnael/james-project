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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.backends.cassandra.CassandraClusterExtension;
import org.apache.james.backends.cassandra.migration.Migration;
import org.apache.james.backends.cassandra.utils.CassandraUtils;
import org.apache.james.core.Domain;
import org.apache.james.rrt.cassandra.CassandraMappingsSourcesDAO;
import org.apache.james.rrt.cassandra.CassandraRRTModule;
import org.apache.james.rrt.cassandra.CassandraRecipientRewriteTableDAO;
import org.apache.james.rrt.lib.Mapping;
import org.apache.james.rrt.lib.MappingSource;
import org.apache.james.rrt.lib.MappingsImpl;
import org.apache.james.task.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.google.common.collect.ImmutableMap;

import reactor.core.publisher.Mono;

class MappingsSourcesMigrationTest {
    private static final String USER = "test";
    private static final String ADDRESS = "test@domain";
    private static final MappingSource SOURCE = MappingSource.fromUser(USER, Domain.LOCALHOST);
    private static final Mapping MAPPING = Mapping.alias(ADDRESS);

    @RegisterExtension
    static CassandraClusterExtension cassandraCluster = new CassandraClusterExtension(CassandraRRTModule.MODULE);

    private CassandraRecipientRewriteTableDAO cassandraRecipientRewriteTableDAO;
    private CassandraMappingsSourcesDAO cassandraMappingsSourcesDAO;

    private MappingsSourcesMigration migration;

    @BeforeEach
    void setUp(CassandraCluster cassandra) {
        cassandraRecipientRewriteTableDAO = new CassandraRecipientRewriteTableDAO(cassandra.getConf(), CassandraUtils.WITH_DEFAULT_CONFIGURATION);
        cassandraMappingsSourcesDAO = new CassandraMappingsSourcesDAO(cassandra.getConf());

        migration = new MappingsSourcesMigration(cassandraRecipientRewriteTableDAO, cassandraMappingsSourcesDAO);
    }

    @Test
    void emptyMigrationShouldSucceed() {
        assertThat(migration.run()).isEqualTo(Migration.Result.COMPLETED);
    }

    @Test
    void migrationShouldSucceedWithData() {
        cassandraRecipientRewriteTableDAO.addMapping(SOURCE, MAPPING);

        assertThat(migration.run()).isEqualTo(Task.Result.COMPLETED);
    }

    @Test
    void migrationShouldCreateMappingSourceFromMapping() {
        cassandraRecipientRewriteTableDAO.addMapping(SOURCE, MAPPING);

        migration.run();

        assertThat(cassandraMappingsSourcesDAO.retrieveSources(MAPPING).collectList().block())
            .containsExactly(SOURCE);
    }

    @Test
    void migrationShouldCreateMultipleMappingSourcesFromMappings() {
        MappingSource source2 = MappingSource.fromUser("bob", Domain.LOCALHOST);

        cassandraRecipientRewriteTableDAO.addMapping(SOURCE, MAPPING);
        cassandraRecipientRewriteTableDAO.addMapping(source2, MAPPING);

        migration.run();

        assertThat(cassandraMappingsSourcesDAO.retrieveSources(MAPPING).collectList().block())
            .containsOnly(SOURCE, source2);
    }

    @Test
    void migrationShouldReturnPartialWhenGetAllMappingsFromMappingsFail() {
        CassandraRecipientRewriteTableDAO cassandraRecipientRewriteTableDAO = mock(CassandraRecipientRewriteTableDAO.class);
        CassandraMappingsSourcesDAO cassandraMappingsSourcesDAO = mock(CassandraMappingsSourcesDAO.class);
        migration = new MappingsSourcesMigration(cassandraRecipientRewriteTableDAO, cassandraMappingsSourcesDAO);

        when(cassandraRecipientRewriteTableDAO.getAllMappings()).thenThrow(new RuntimeException());

        assertThat(migration.run()).isEqualTo(Migration.Result.PARTIAL);
    }

    @Test
    void migrationShouldReturnPartialAddMappingFails() {
        CassandraRecipientRewriteTableDAO cassandraRecipientRewriteTableDAO = mock(CassandraRecipientRewriteTableDAO.class);
        CassandraMappingsSourcesDAO cassandraMappingsSourcesDAO = mock(CassandraMappingsSourcesDAO.class);
        migration = new MappingsSourcesMigration(cassandraRecipientRewriteTableDAO, cassandraMappingsSourcesDAO);

        when(cassandraRecipientRewriteTableDAO.getAllMappings())
            .thenReturn(Mono.just(ImmutableMap.of(SOURCE, MappingsImpl.fromMappings(MAPPING))));
        when(cassandraMappingsSourcesDAO.addMapping(any(Mapping.class), any(MappingSource.class)))
            .thenThrow(new RuntimeException());

        assertThat(migration.run()).isEqualTo(Migration.Result.PARTIAL);
    }


}
