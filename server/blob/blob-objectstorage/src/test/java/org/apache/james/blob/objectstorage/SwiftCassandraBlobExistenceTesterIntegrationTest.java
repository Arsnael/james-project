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

package org.apache.james.blob.objectstorage;

import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.backends.cassandra.CassandraClusterExtension;
import org.apache.james.blob.api.BlobId;
import org.apache.james.blob.api.BlobStore;
import org.apache.james.blob.api.BucketName;
import org.apache.james.blob.api.HashBlobId;
import org.apache.james.blob.api.MetricableBlobStore;
import org.apache.james.blob.objectstorage.aws.DockerAwsS3Extension;
import org.apache.james.blob.objectstorage.cassandra.CassandraBlobExistenceModule;
import org.apache.james.blob.objectstorage.cassandra.CassandraBlobExistenceTester;
import org.apache.james.blob.objectstorage.cassandra.CassandraBlobExistenceTesterDAO;
import org.apache.james.blob.objectstorage.swift.Credentials;
import org.apache.james.blob.objectstorage.swift.Identity;
import org.apache.james.blob.objectstorage.swift.PassHeaderName;
import org.apache.james.blob.objectstorage.swift.SwiftTempAuthObjectStorage;
import org.apache.james.blob.objectstorage.swift.TenantName;
import org.apache.james.blob.objectstorage.swift.UserHeaderName;
import org.apache.james.blob.objectstorage.swift.UserName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

@ExtendWith(DockerAwsS3Extension.class)
class SwiftCassandraBlobExistenceTesterIntegrationTest implements BlobExistenceTesterIntegrationContract {
    static final HashBlobId.Factory BLOB_ID_FACTORY = new HashBlobId.Factory();
    private static final TenantName TENANT_NAME = TenantName.of("test");
    private static final UserName USER_NAME = UserName.of("tester");
    private static final Credentials PASSWORD = Credentials.of("testing");
    private static final Identity SWIFT_IDENTITY = Identity.of(TENANT_NAME, USER_NAME);

    @RegisterExtension
    static CassandraClusterExtension cassandraCluster = new CassandraClusterExtension(CassandraBlobExistenceModule.MODULE);

    BucketName defaultBucketName;
    private org.jclouds.blobstore.BlobStore blobStore;
    SwiftTempAuthObjectStorage.Configuration testConfig;
    ObjectStorageBlobStore objectStorageBlobStore;
    CassandraBlobExistenceTester cassandraBlobExistenceTester;
    BlobStore testee;

    @BeforeEach
    void setUp(DockerSwift dockerSwift, CassandraCluster cassandra) {
        CassandraBlobExistenceTesterDAO blobExistenceTesterDAO = new CassandraBlobExistenceTesterDAO(cassandra.getConf());
        cassandraBlobExistenceTester = new CassandraBlobExistenceTester(blobExistenceTesterDAO);
        defaultBucketName = BucketName.of("e4fc2427-f2aa-422a-a535-3df0d2a086c4");
        testConfig = SwiftTempAuthObjectStorage.configBuilder()
            .endpoint(dockerSwift.swiftEndpoint())
            .identity(SWIFT_IDENTITY)
            .credentials(PASSWORD)
            .tempAuthHeaderUserName(UserHeaderName.of("X-Storage-User"))
            .tempAuthHeaderPassName(PassHeaderName.of("X-Storage-Pass"))
            .build();
        BlobId.Factory blobIdFactory = blobIdFactory();
        ObjectStorageBlobStoreBuilder.ReadyToBuild blobStoreBuilder = ObjectStorageBlobStore
            .builder(testConfig)
            .blobIdFactory(blobIdFactory)
            .blobExistenceTester(new FakeBlobExistenceTester())
            .namespace(defaultBucketName);
        blobStore = blobStoreBuilder.getSupplier().get();
        objectStorageBlobStore = blobStoreBuilder.build();
        testee = new MetricableBlobStore(metricsTestExtension.getMetricFactory(), objectStorageBlobStore);
    }

    @AfterEach
    void tearDown() {
        objectStorageBlobStore.deleteAllBuckets().block();
        objectStorageBlobStore.close();
    }

    @Override
    public BlobStore testee() {
        return testee;
    }

    @Override
    public BlobId.Factory blobIdFactory() {
        return BLOB_ID_FACTORY;
    }

    @Override
    public BlobExistenceTester blobExistenceTester() {
        return cassandraBlobExistenceTester;
    }
}
