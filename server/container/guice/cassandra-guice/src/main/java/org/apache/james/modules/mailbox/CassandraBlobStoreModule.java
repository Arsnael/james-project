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

package org.apache.james.modules.mailbox;

import org.apache.james.backends.cassandra.components.CassandraModule;
import org.apache.james.blob.api.BlobStore;
import org.apache.james.blob.api.MetricableBlobStore;
import org.apache.james.blob.cassandra.CassandraBlobModule;
import org.apache.james.blob.cassandra.CassandraBlobStore;
import org.apache.james.blob.cassandra.CassandraDefaultBucketDAO;
import org.apache.james.blob.objectstorage.BlobExistenceTester;
import org.apache.james.blob.objectstorage.cassandra.CassandraBlobExistenceModule;
import org.apache.james.blob.objectstorage.cassandra.CassandraBlobExistenceTester;
import org.apache.james.blob.objectstorage.cassandra.CassandraBlobExistenceTesterDAO;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

public class CassandraBlobStoreModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(CassandraDefaultBucketDAO.class).in(Scopes.SINGLETON);
        bind(CassandraBlobStore.class).in(Scopes.SINGLETON);
        bind(CassandraBlobExistenceTesterDAO.class).in(Scopes.SINGLETON);
        bind(CassandraBlobExistenceTester.class).in(Scopes.SINGLETON);

        bind(BlobStore.class)
            .annotatedWith(Names.named(MetricableBlobStore.BLOB_STORE_IMPLEMENTATION))
            .to(CassandraBlobStore.class);

        bind(BlobExistenceTester.class).to(CassandraBlobExistenceTester.class);

        Multibinder<CassandraModule> cassandraDataDefinitions = Multibinder.newSetBinder(binder(), CassandraModule.class);
        cassandraDataDefinitions.addBinding().toInstance(CassandraBlobModule.MODULE);
        cassandraDataDefinitions.addBinding().toInstance(CassandraBlobExistenceModule.MODULE);
    }
}
