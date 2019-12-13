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

package org.apache.james.blob.objectstorage.cassandra;

import static org.apache.james.blob.objectstorage.BlobExistenceTesterFixture.BLOB_ID_FACTORY;
import static org.apache.james.blob.objectstorage.BlobExistenceTesterFixture.BUCKET_NAME;
import static org.apache.james.blob.objectstorage.BlobExistenceTesterFixture.OTHER_BUCKET_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.backends.cassandra.CassandraClusterExtension;
import org.apache.james.blob.api.BlobId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class CassandraBlobExistenceTesterDAOTest {

    @RegisterExtension
    static CassandraClusterExtension cassandraCluster = new CassandraClusterExtension(CassandraBlobExistenceModule.MODULE);

    CassandraBlobExistenceTesterDAO testee;

    @BeforeEach
    void setUp(CassandraCluster cassandra) {
        testee = new CassandraBlobExistenceTesterDAO(BLOB_ID_FACTORY, cassandra.getConf());
    }

    @Test
    void hasBlobExistenceShouldReturnFalseIfBlobDoesNotExist() {
        assertThat(testee.hasBlobExistence(BUCKET_NAME, BLOB_ID_FACTORY.randomId()).block())
            .isEqualTo(false);
    }

    @Test
    void hasBlobExistenceShouldReturnTrueIfBlobExists() {
        BlobId blobId = BLOB_ID_FACTORY.from("12345");
        testee.addBlobExistence(BUCKET_NAME, blobId).block();

        assertThat(testee.hasBlobExistence(BUCKET_NAME, blobId).block())
            .isEqualTo(true);
    }

    @Test
    void hasBlobExistenceShouldReturnFalseIfSameBucketButDifferentBlob() {
        BlobId blobId = BLOB_ID_FACTORY.from("12345");
        testee.addBlobExistence(BUCKET_NAME, blobId).block();

        assertThat(testee.hasBlobExistence(BUCKET_NAME, BLOB_ID_FACTORY.randomId()).block())
            .isEqualTo(false);
    }

    @Test
    void hasBlobExistenceShouldReturnFalseIfDifferentBucketButSameBlob() {
        BlobId blobId = BLOB_ID_FACTORY.from("12345");
        testee.addBlobExistence(BUCKET_NAME, blobId).block();

        assertThat(testee.hasBlobExistence(OTHER_BUCKET_NAME, blobId).block())
            .isEqualTo(false);
    }

    @Test
    void removeBlobExistenceShouldNotThrowWhenBlobMissing() {
        assertThatCode(() -> testee.removeBlobExistence(BUCKET_NAME, BLOB_ID_FACTORY.randomId()).block())
            .doesNotThrowAnyException();
    }

    @Test
    void removeBlobExistenceShouldDeleteBlob() {
        BlobId blobId = BLOB_ID_FACTORY.from("12345");
        testee.addBlobExistence(BUCKET_NAME, blobId).block();
        testee.removeBlobExistence(BUCKET_NAME, blobId).block();

        assertThat(testee.hasBlobExistence(BUCKET_NAME, blobId).block())
            .isEqualTo(false);
    }

    @Test
    void getBucketBlobIdsShouldReturnEmptyWhenNoBucket() {
        assertThat(testee.getBucketBlobIds(BUCKET_NAME).collectList().block())
            .isEmpty();
    }

    @Test
    void getBucketBlobIdsShouldReturnBlobIdFromCorrespondingBucket() {
        BlobId blobId = BLOB_ID_FACTORY.from("12345");
        testee.addBlobExistence(BUCKET_NAME, blobId).block();

        assertThat(testee.getBucketBlobIds(BUCKET_NAME).collectList().block())
            .containsExactly(blobId);
    }

    @Test
    void getBucketBlobIdsShouldReturnMultipleBlobIdsFromCorrespondingBucket() {
        BlobId blobId1 = BLOB_ID_FACTORY.from("12345");
        testee.addBlobExistence(BUCKET_NAME, blobId1).block();

        BlobId blobId2 = BLOB_ID_FACTORY.from("67890");
        testee.addBlobExistence(BUCKET_NAME, blobId2).block();

        assertThat(testee.getBucketBlobIds(BUCKET_NAME).collectList().block())
            .containsExactly(blobId1, blobId2);
    }

    @Test
    void getBucketBlobIdsShouldNotReturnBlobIdsFromOtherBuckets() {
        BlobId blobId1 = BLOB_ID_FACTORY.from("12345");
        testee.addBlobExistence(BUCKET_NAME, blobId1).block();

        BlobId blobId2 = BLOB_ID_FACTORY.from("12345");
        testee.addBlobExistence(OTHER_BUCKET_NAME, blobId2).block();

        assertThat(testee.getBucketBlobIds(BUCKET_NAME).collectList().block())
            .containsExactly(blobId1);
    }

    @Test
    void getBucketBlobIdsShouldNotReturnDeletedBlobId() {
        BlobId blobId1 = BLOB_ID_FACTORY.from("12345");
        testee.addBlobExistence(BUCKET_NAME, blobId1).block();

        testee.removeBlobExistence(BUCKET_NAME, blobId1).block();

        assertThat(testee.getBucketBlobIds(BUCKET_NAME).collectList().block())
            .isEmpty();
    }
}
