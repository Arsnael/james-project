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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.time.Duration;

import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.backends.cassandra.CassandraClusterExtension;
import org.apache.james.blob.api.BlobId;
import org.apache.james.blob.api.BlobStore;
import org.apache.james.blob.api.BucketName;
import org.apache.james.blob.api.HashBlobId;
import org.apache.james.blob.api.MetricableBlobStore;
import org.apache.james.blob.api.MetricableBlobStoreContract;
import org.apache.james.blob.objectstorage.aws.AwsS3AuthConfiguration;
import org.apache.james.blob.objectstorage.aws.AwsS3ObjectStorage;
import org.apache.james.blob.objectstorage.aws.DockerAwsS3Container;
import org.apache.james.blob.objectstorage.aws.DockerAwsS3Extension;
import org.apache.james.blob.objectstorage.cassandra.CassandraBlobExistenceModule;
import org.apache.james.blob.objectstorage.cassandra.CassandraBlobExistenceTester;
import org.apache.james.blob.objectstorage.cassandra.CassandraBlobExistenceTesterDAO;
import org.apache.james.util.concurrency.ConcurrentTestRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

@ExtendWith(DockerAwsS3Extension.class)
class CassandraBlobExistenceTesterIntegrationTest implements MetricableBlobStoreContract {
    private static final HashBlobId.Factory BLOB_ID_FACTORY = new HashBlobId.Factory();

    @RegisterExtension
    static CassandraClusterExtension cassandraCluster = new CassandraClusterExtension(CassandraBlobExistenceModule.MODULE);

    ObjectStorageBlobStore objectStorageBlobStore;
    AwsS3ObjectStorage awsS3ObjectStorage;
    CassandraBlobExistenceTester cassandraBlobExistenceTester;
    BlobStore testee;

    @BeforeEach
    void setUp(DockerAwsS3Container dockerAwsS3, CassandraCluster cassandra) {
        CassandraBlobExistenceTesterDAO blobExistenceTesterDAO = new CassandraBlobExistenceTesterDAO(BLOB_ID_FACTORY, cassandra.getConf());
        cassandraBlobExistenceTester = new CassandraBlobExistenceTester(blobExistenceTesterDAO);
        awsS3ObjectStorage = new AwsS3ObjectStorage(new FakeBlobExistenceTester());
        AwsS3AuthConfiguration configuration = AwsS3AuthConfiguration.builder()
            .endpoint(dockerAwsS3.getEndpoint())
            .accessKeyId(DockerAwsS3Container.ACCESS_KEY_ID)
            .secretKey(DockerAwsS3Container.SECRET_ACCESS_KEY)
            .build();

        ObjectStorageBlobStoreBuilder.ReadyToBuild builder = ObjectStorageBlobStore
            .builder(configuration)
            .blobIdFactory(BLOB_ID_FACTORY)
            .blobExistenceTester(cassandraBlobExistenceTester)
            .blobPutter(awsS3ObjectStorage.putBlob(configuration));

        objectStorageBlobStore = builder.build();
        testee = new MetricableBlobStore(metricsTestExtension.getMetricFactory(), objectStorageBlobStore);
    }

    @AfterEach
    void tearDown() {
        objectStorageBlobStore.deleteAllBuckets().block();
        objectStorageBlobStore.close();
        awsS3ObjectStorage.tearDown();
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
    @Disabled("JAMES-2829 Unstable with scality/S3 impl")
    public void readShouldNotReadPartiallyWhenDeletingConcurrentlyBigBlob() {

    }

    @Override
    @Disabled("JAMES-2838 Unstable with scality/S3 impl")
    public void readBytesShouldNotReadPartiallyWhenDeletingConcurrentlyBigBlob() {

    }

    @RepeatedTest(100)
    void deleteShouldNotAffectSubsequentSaveConsistency() throws Exception {
        BucketName defaultBucketName = testee.getDefaultBucketName();
        BlobId blobId = blobIdFactory().forPayload(SHORT_BYTEARRAY);

        ConcurrentTestRunner.builder()
            .reactorOperation(((threadNumber, step) -> {
                if (step % 2 == 0) {
                    return testee().save(defaultBucketName, SHORT_BYTEARRAY);
                }
                return testee().delete(defaultBucketName, blobId);
            }))
            .threadCount(5)
            .operationCount(5)
            .runSuccessfullyWithin(Duration.ofMinutes(2));

        // As the number of operation is odd, all threads end by adding the blob
        // Hence we are guarantied the blob exists

        assertThat(testee().read(defaultBucketName, blobId))
            .hasSameContentAs(new ByteArrayInputStream(SHORT_BYTEARRAY));
    }
}
