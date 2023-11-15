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

package org.apache.james.mailbox.postgres.mail;

import javax.persistence.EntityManagerFactory;

import org.apache.james.backends.jpa.JPAConfiguration;
import org.apache.james.backends.jpa.JpaTestCluster;
import org.apache.james.backends.postgres.PostgresExtension;
import org.apache.james.backends.postgres.PostgresModule;
import org.apache.james.backends.postgres.utils.SimpleJamesPostgresConnectionFactory;
import org.apache.james.mailbox.postgres.JPAMailboxFixture;
import org.apache.james.mailbox.postgres.PostgresMailboxSessionMapperFactory;
import org.apache.james.mailbox.postgres.user.PostgresSubscriptionModule;
import org.apache.james.mailbox.store.mail.model.MapperProvider;
import org.apache.james.mailbox.store.mail.model.MessageMoveTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.RegisterExtension;

class JpaMessageMoveTest extends MessageMoveTest {
    @RegisterExtension
    static PostgresExtension postgresExtension = PostgresExtension.withoutRowLevelSecurity(PostgresModule.aggregateModules(
        PostgresMailboxModule.MODULE,
        PostgresSubscriptionModule.MODULE));

    static final JpaTestCluster JPA_TEST_CLUSTER = JpaTestCluster.create(JPAMailboxFixture.MAILBOX_PERSISTANCE_CLASSES);

    @Override
    protected MapperProvider createMapperProvider() {
        EntityManagerFactory entityManagerFactory = JPA_TEST_CLUSTER.getEntityManagerFactory();

        JPAConfiguration jpaConfiguration = JPAConfiguration.builder()
            .driverName("driverName")
            .driverURL("driverUrl")
            .build();

        PostgresMailboxSessionMapperFactory mapperFactory = new PostgresMailboxSessionMapperFactory(entityManagerFactory,
            new JPAUidProvider(entityManagerFactory),
            new JPAModSeqProvider(entityManagerFactory),
            jpaConfiguration,
            new SimpleJamesPostgresConnectionFactory(postgresExtension.getConnectionFactory()));

        return new JPAMapperProvider(JPA_TEST_CLUSTER, mapperFactory);
    }
    
    @AfterEach
    void cleanUp() {
        JPA_TEST_CLUSTER.clear(JPAMailboxFixture.MAILBOX_TABLE_NAMES);
    }

}
