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
import java.nio.charset.StandardCharsets;
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
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

@ExtendWith(DockerAwsS3Extension.class)
class CassandraBlobExistenceTesterIntegrationTest implements MetricableBlobStoreContract {
    static final HashBlobId.Factory BLOB_ID_FACTORY = new HashBlobId.Factory();
    static final BucketName CUSTOM = BucketName.of("custom");
    static final byte[] OTHER_SHORT_BYTEARRAY = "other_bytearray".getBytes(StandardCharsets.UTF_8);

    @RegisterExtension
    static CassandraClusterExtension cassandraCluster = new CassandraClusterExtension(CassandraBlobExistenceModule.MODULE);

    ObjectStorageBlobStore objectStorageBlobStore;
    AwsS3ObjectStorage awsS3ObjectStorage;
    CassandraBlobExistenceTester cassandraBlobExistenceTester;
    BlobStore testee;

    @BeforeEach
    void setUp(DockerAwsS3Container dockerAwsS3, CassandraCluster cassandra) {
        CassandraBlobExistenceTesterDAO blobExistenceTesterDAO = new CassandraBlobExistenceTesterDAO(cassandra.getConf());
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

    @Test
    void saveBytesShouldSaveBlobIdInBlobExistenceTester() {
        BucketName defaultBucketName = testee.getDefaultBucketName();

        BlobId blobId = testee().save(defaultBucketName, SHORT_BYTEARRAY).block();

        assertThat(cassandraBlobExistenceTester.exists(ObjectStorageBucketName.of(defaultBucketName), blobId).block())
            .isTrue();
    }

    @Test
    void saveInputStreamShouldSaveBlobIdInBlobExistenceTester() {
        BucketName defaultBucketName = testee.getDefaultBucketName();

        BlobId blobId = testee().save(defaultBucketName, new ByteArrayInputStream(SHORT_BYTEARRAY)).block();

        assertThat(cassandraBlobExistenceTester.exists(ObjectStorageBucketName.of(defaultBucketName), blobId).block())
            .isTrue();
    }

    @Test
    void saveBigInputStreamShouldSaveBlobIdInBlobExistenceTester() {
        BucketName defaultBucketName = testee.getDefaultBucketName();

        BlobId blobId = testee().save(defaultBucketName, new ByteArrayInputStream(TWELVE_MEGABYTES)).block();

        assertThat(cassandraBlobExistenceTester.exists(ObjectStorageBucketName.of(defaultBucketName), blobId).block())
            .isTrue();
    }

    @Test
    void saveSameBlobInDifferentBucketsShouldSaveSameTheirBlobWithDifferentBucketIdInBlobExistenceTester() {
        BucketName defaultBucketName = testee.getDefaultBucketName();

        BlobId blobId = testee().save(defaultBucketName, SHORT_BYTEARRAY).block();
        testee().save(CUSTOM, SHORT_BYTEARRAY).block();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(cassandraBlobExistenceTester.exists(ObjectStorageBucketName.of(defaultBucketName), blobId).block())
                .isTrue();
            softly.assertThat(cassandraBlobExistenceTester.exists(ObjectStorageBucketName.of(CUSTOM), blobId).block())
                .isTrue();
        });
    }

    @Test
    void saveShouldBeAbleToSaveBlobIDsOfMultipleDataInBlobExistenceTester() {
        BucketName defaultBucketName = testee.getDefaultBucketName();

        BlobId blobIdDefault = testee().save(defaultBucketName, SHORT_BYTEARRAY).block();
        BlobId blobIdCustom = testee().save(CUSTOM, new ByteArrayInputStream(OTHER_SHORT_BYTEARRAY)).block();
        BlobId blobIdBigStream = testee().save(defaultBucketName, new ByteArrayInputStream(TWELVE_MEGABYTES)).block();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(cassandraBlobExistenceTester.exists(ObjectStorageBucketName.of(defaultBucketName), blobIdDefault).block())
                .isTrue();
            softly.assertThat(cassandraBlobExistenceTester.exists(ObjectStorageBucketName.of(CUSTOM), blobIdCustom).block())
                .isTrue();
            softly.assertThat(cassandraBlobExistenceTester.exists(ObjectStorageBucketName.of(defaultBucketName), blobIdBigStream).block())
                .isTrue();
        });
    }

    @Test
    void saveShouldBeAbleToSaveBlobAgainAfterDelete() {
        BucketName defaultBucketName = testee.getDefaultBucketName();

        BlobId blobId = testee().save(defaultBucketName, SHORT_BYTEARRAY).block();

        testee().delete(defaultBucketName, blobId).block();

        testee().save(defaultBucketName, SHORT_BYTEARRAY).block();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(cassandraBlobExistenceTester.exists(ObjectStorageBucketName.of(defaultBucketName), blobId).block())
                .isTrue();
            softly.assertThat(testee().read(defaultBucketName, blobId))
                .hasSameContentAs(new ByteArrayInputStream(SHORT_BYTEARRAY));
        });

    }

    @Test
    void deleteShouldDeleteBlobIdInBlobExistenceTester() {
        BucketName defaultBucketName = testee.getDefaultBucketName();

        BlobId blobId = testee().save(defaultBucketName, SHORT_BYTEARRAY).block();

        testee().delete(defaultBucketName, blobId).block();

        assertThat(cassandraBlobExistenceTester.exists(ObjectStorageBucketName.of(defaultBucketName), blobId).block())
            .isFalse();
    }

    @Test
    void deleteShouldNotDeleteOtherBlobIdsFromSameBucketInBlobExistenceTester() {
        BucketName defaultBucketName = testee.getDefaultBucketName();

        BlobId blobIdDefault = testee().save(defaultBucketName, SHORT_BYTEARRAY).block();
        BlobId blobIdCustom = testee().save(defaultBucketName, OTHER_SHORT_BYTEARRAY).block();

        testee().delete(defaultBucketName, blobIdDefault).block();

        assertThat(cassandraBlobExistenceTester.exists(ObjectStorageBucketName.of(defaultBucketName), blobIdCustom).block())
            .isTrue();
    }

    @Test
    void deleteShouldNotDeleteOtherBlobIdsFromOtherBucketsInBlobExistenceTester() {
        BucketName defaultBucketName = testee.getDefaultBucketName();

        BlobId blobIdDefault = testee().save(defaultBucketName, SHORT_BYTEARRAY).block();
        BlobId blobIdCustom = testee().save(CUSTOM, OTHER_SHORT_BYTEARRAY).block();

        testee().delete(defaultBucketName, blobIdDefault).block();

        assertThat(cassandraBlobExistenceTester.exists(ObjectStorageBucketName.of(CUSTOM), blobIdCustom).block())
            .isTrue();
    }

    @Test
    void deleteShouldNotDeleteSameBlobIdsFromOtherBucketsInBlobExistenceTester() {
        BucketName defaultBucketName = testee.getDefaultBucketName();

        BlobId blobIdDefault = testee().save(defaultBucketName, SHORT_BYTEARRAY).block();
        BlobId blobIdCustom = testee().save(CUSTOM, SHORT_BYTEARRAY).block();

        testee().delete(defaultBucketName, blobIdDefault).block();

        assertThat(cassandraBlobExistenceTester.exists(ObjectStorageBucketName.of(CUSTOM), blobIdCustom).block())
            .isTrue();
    }

    @Test
    void deleteBucketShouldDeleteAllBlobIdsOfABucketInBlobExistenceTester() {
        BucketName defaultBucketName = testee.getDefaultBucketName();

        BlobId blobIdDefault = testee().save(defaultBucketName, SHORT_BYTEARRAY).block();
        BlobId blobIdCustom = testee().save(defaultBucketName, OTHER_SHORT_BYTEARRAY).block();

        testee().deleteBucket(defaultBucketName).block();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(cassandraBlobExistenceTester.exists(ObjectStorageBucketName.of(defaultBucketName), blobIdDefault).block())
                .isFalse();
            softly.assertThat(cassandraBlobExistenceTester.exists(ObjectStorageBucketName.of(defaultBucketName), blobIdCustom).block())
                .isFalse();
        });
    }

    @Test
    void deleteBucketShouldDeleteAllBlobIdsWithDifferentBucketsInBlobExistenceTester() {
        BucketName defaultBucketName = testee.getDefaultBucketName();

        BlobId blobIdDefault = testee().save(defaultBucketName, SHORT_BYTEARRAY).block();
        BlobId blobIdCustom = testee().save(CUSTOM, OTHER_SHORT_BYTEARRAY).block();

        testee().deleteBucket(defaultBucketName).block();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(cassandraBlobExistenceTester.exists(ObjectStorageBucketName.of(defaultBucketName), blobIdDefault).block())
                .isFalse();
            softly.assertThat(cassandraBlobExistenceTester.exists(ObjectStorageBucketName.of(CUSTOM), blobIdCustom).block())
                .isFalse();
        });
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
        //Hence we are guarantied the blob exists

        assertThat(testee().read(defaultBucketName, blobId))
            .hasSameContentAs(new ByteArrayInputStream(SHORT_BYTEARRAY));
    }
}
