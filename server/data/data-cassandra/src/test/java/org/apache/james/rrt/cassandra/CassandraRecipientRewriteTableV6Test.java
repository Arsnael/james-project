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

package org.apache.james.rrt.cassandra;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.apache.james.backends.cassandra.utils.CassandraUtils;
import org.apache.james.backends.cassandra.versions.CassandraSchemaVersionDAO;
import org.apache.james.backends.cassandra.versions.SchemaVersion;
import org.apache.james.rrt.lib.AbstractRecipientRewriteTable;

public class CassandraRecipientRewriteTableV6Test extends AbstractCassandraRecipientRewriteTableTest {
    private static final SchemaVersion SCHEMA_VERSION_V6 = new SchemaVersion(6);

    @Override
    protected AbstractRecipientRewriteTable getRecipientRewriteTable() throws Exception {
        CassandraSchemaVersionDAO mockCassandraSchemaVersionDAO = mock(CassandraSchemaVersionDAO.class);

        CassandraRecipientRewriteTable rrt = new CassandraRecipientRewriteTable(
            new CassandraRecipientRewriteTableDAO(cassandra.getConf(), CassandraUtils.WITH_DEFAULT_CONFIGURATION),
            new CassandraMappingsSourcesDAO(cassandra.getConf()),
            mockCassandraSchemaVersionDAO);
        rrt.configure(new DefaultConfigurationBuilder());

        when(mockCassandraSchemaVersionDAO.getCurrentSchemaVersion()).thenReturn(CompletableFuture.completedFuture(Optional.of(SCHEMA_VERSION_V6)));

        return rrt;
    }
}
