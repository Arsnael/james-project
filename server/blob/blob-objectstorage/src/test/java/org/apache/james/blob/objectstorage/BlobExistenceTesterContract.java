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

import static org.apache.james.blob.objectstorage.BlobExistenceTesterFixture.BUCKET_NAME;
import static org.apache.james.blob.objectstorage.BlobExistenceTesterFixture.OTHER_BUCKET_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.apache.james.blob.api.BlobId;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

public interface BlobExistenceTesterContract {
    BlobExistenceTester testee();

    BlobId.Factory blobIdFactory();

    @Test
    default void existsShouldThrowWhenNullBucketName() {
        assertThatThrownBy(() -> testee().exists(null, blobIdFactory().from("12345")).block())
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    default void existsShouldThrowWhenNullBlobId() {
        assertThatThrownBy(() -> testee().exists(BUCKET_NAME, null).block())
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    default void existsShouldReturnFalseWhenNonExistingPair() {
        assertThat(testee().exists(BUCKET_NAME, blobIdFactory().from("12345")).block())
            .isEqualTo(false);
    }

    @Test
    default void existsShouldReturnFalseWhenExistingBucketNameAndNotBlobId() {
        testee().persist(BUCKET_NAME, blobIdFactory().from("12345")).block();

        assertThat(testee().exists(BUCKET_NAME, blobIdFactory().from("67890")).block())
            .isEqualTo(false);
    }

    @Test
    default void existsShouldReturnFalseWhenExistingBlobIdAndNotBucketName() {
        BlobId blobId = blobIdFactory().from("12345");
        testee().persist(BUCKET_NAME, blobId).block();

        assertThat(testee().exists(OTHER_BUCKET_NAME, blobId).block())
            .isEqualTo(false);
    }

    @Test
    default void existsShouldReturnTrueWhenExistingPersistedPair() {
        BlobId blobId = blobIdFactory().from("12345");
        testee().persist(BUCKET_NAME, blobId).block();

        assertThat(testee().exists(BUCKET_NAME, blobId).block())
            .isEqualTo(true);
    }

    @Test
    default void persistShouldThrowWhenNullBucketName() {
        assertThatThrownBy(() -> testee().persist(null, blobIdFactory().from("12345")).block())
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    default void persistShouldThrowWhenNullBlobId() {
        assertThatThrownBy(() -> testee().persist(BUCKET_NAME, null).block())
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    default void persistShouldBeIdempotent() {
        BlobId blobId = blobIdFactory().from("12345");

        testee().persist(BUCKET_NAME, blobId).block();
        testee().persist(BUCKET_NAME, blobId).block();

        assertThat(testee().exists(BUCKET_NAME, blobId).block())
            .isEqualTo(true);
    }

    @Test
    default void deleteShouldThrowWhenNullBucketName() {
        assertThatThrownBy(() -> testee().delete(null, blobIdFactory().from("12345")).block())
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    default void deleteShouldThrowWhenNullBlobId() {
        assertThatThrownBy(() -> testee().delete(BUCKET_NAME, null).block())
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    default void deleteShouldDeletePreviouslyPersistedPair() {
        BlobId blobId = blobIdFactory().from("12345");
        testee().persist(BUCKET_NAME, blobId).block();

        testee().delete(BUCKET_NAME, blobId).block();

        assertThat(testee().exists(BUCKET_NAME, blobId).block())
            .isEqualTo(false);
    }

    @Test
    default void deleteShouldBeIdempotent() {
        BlobId blobId = blobIdFactory().from("12345");
        testee().persist(BUCKET_NAME, blobId).block();

        testee().delete(BUCKET_NAME, blobId).block();
        testee().delete(BUCKET_NAME, blobId).block();

        assertThat(testee().exists(BUCKET_NAME, blobId).block())
            .isEqualTo(false);
    }

    @Test
    default void deleteShouldNotDeleteOtherBlobIdsInSameBucket() {
        BlobId blobId1 = blobIdFactory().from("12345");
        testee().persist(BUCKET_NAME, blobId1).block();

        BlobId blobId2 = blobIdFactory().from("67890");
        testee().persist(BUCKET_NAME, blobId2).block();

        testee().delete(BUCKET_NAME, blobId1).block();

        SoftAssertions.assertSoftly(softly -> {
            assertThat(testee().exists(BUCKET_NAME, blobId1).block())
                .isEqualTo(false);
            assertThat(testee().exists(BUCKET_NAME, blobId2).block())
                .isEqualTo(true);
        });
    }

    @Test
    default void deleteShouldNotDeleteOtherBlobIdsInOtherBucket() {
        BlobId blobId1 = blobIdFactory().from("12345");
        testee().persist(BUCKET_NAME, blobId1).block();

        BlobId blobId2 = blobIdFactory().from("67890");
        testee().persist(OTHER_BUCKET_NAME, blobId2).block();

        testee().delete(BUCKET_NAME, blobId1).block();

        SoftAssertions.assertSoftly(softly -> {
            assertThat(testee().exists(BUCKET_NAME, blobId1).block())
                .isEqualTo(false);
            assertThat(testee().exists(OTHER_BUCKET_NAME, blobId2).block())
                .isEqualTo(true);
        });
    }

    @Test
    default void deleteBucketShouldThrowWhenNullBucketName() {
        assertThatThrownBy(() -> testee().deleteBucket(null).block())
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    default void deleteBucketShouldDeleteBlobIdsReferencedInTheBucket() {
        BlobId blobId = blobIdFactory().from("12345");
        testee().persist(BUCKET_NAME, blobId).block();

        testee().deleteBucket(BUCKET_NAME).block();

        assertThat(testee().exists(BUCKET_NAME, blobId).block())
            .isEqualTo(false);
    }

    @Test
    default void deleteBucketShouldBeIdempotent() {
        BlobId blobId = blobIdFactory().from("12345");
        testee().persist(BUCKET_NAME, blobId).block();

        testee().deleteBucket(BUCKET_NAME).block();
        testee().deleteBucket(BUCKET_NAME).block();

        assertThat(testee().exists(BUCKET_NAME, blobId).block())
            .isEqualTo(false);
    }

    @Test
    default void deleteBucketShouldDeleteAllEntriesOfABucket() {
        BlobId blobId1 = blobIdFactory().from("12345");
        testee().persist(BUCKET_NAME, blobId1).block();

        BlobId blobId2 = blobIdFactory().from("67890");
        testee().persist(BUCKET_NAME, blobId2).block();

        testee().deleteBucket(BUCKET_NAME).block();

        SoftAssertions.assertSoftly(softly -> {
            assertThat(testee().exists(BUCKET_NAME, blobId1).block())
                .isEqualTo(false);
            assertThat(testee().exists(BUCKET_NAME, blobId2).block())
                .isEqualTo(false);
        });
    }

    @Test
    default void deleteBucketShouldNotDeleteEntriesOfOtherBuckets() {
        BlobId blobId1 = blobIdFactory().from("12345");
        testee().persist(BUCKET_NAME, blobId1).block();

        BlobId blobId2 = blobIdFactory().from("67890");
        testee().persist(OTHER_BUCKET_NAME, blobId2).block();

        testee().deleteBucket(BUCKET_NAME).block();

        SoftAssertions.assertSoftly(softly -> {
            assertThat(testee().exists(BUCKET_NAME, blobId1).block())
                .isEqualTo(false);
            assertThat(testee().exists(OTHER_BUCKET_NAME, blobId2).block())
                .isEqualTo(true);
        });
    }
}
