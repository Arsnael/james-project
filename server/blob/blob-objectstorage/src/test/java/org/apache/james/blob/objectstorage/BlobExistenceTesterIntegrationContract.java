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
import java.util.Random;

import org.apache.james.blob.api.BlobId;
import org.apache.james.blob.api.BucketName;
import org.apache.james.blob.api.MetricableBlobStoreContract;
import org.apache.james.util.concurrency.ConcurrentTestRunner;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

public interface BlobExistenceTesterIntegrationContract extends MetricableBlobStoreContract {
    BucketName CUSTOM = BucketName.of("custom");
    byte[] OTHER_SHORT_BYTEARRAY = "other_bytearray".getBytes(StandardCharsets.UTF_8);
    Random RANDOM = new Random();

    BlobExistenceTester blobExistenceTester();

    @Test
    default void saveBytesShouldSaveBlobIdInBlobExistenceTester() {
        BucketName defaultBucketName = testee().getDefaultBucketName();

        BlobId blobId = testee().save(defaultBucketName, SHORT_BYTEARRAY).block();

        assertThat(blobExistenceTester().exists(ObjectStorageBucketName.of(defaultBucketName), blobId).block())
            .isTrue();
    }

    @Test
    default void saveInputStreamShouldSaveBlobIdInBlobExistenceTester() {
        BucketName defaultBucketName = testee().getDefaultBucketName();

        BlobId blobId = testee().save(defaultBucketName, new ByteArrayInputStream(SHORT_BYTEARRAY)).block();

        assertThat(blobExistenceTester().exists(ObjectStorageBucketName.of(defaultBucketName), blobId).block())
            .isTrue();
    }

    @Test
    default void saveBigInputStreamShouldSaveBlobIdInBlobExistenceTester() {
        BucketName defaultBucketName = testee().getDefaultBucketName();

        BlobId blobId = testee().save(defaultBucketName, new ByteArrayInputStream(TWELVE_MEGABYTES)).block();

        assertThat(blobExistenceTester().exists(ObjectStorageBucketName.of(defaultBucketName), blobId).block())
            .isTrue();
    }

    @Test
    default void saveSameBlobInDifferentBucketsShouldSaveSameTheirBlobWithDifferentBucketIdInBlobExistenceTester() {
        BucketName defaultBucketName = testee().getDefaultBucketName();

        BlobId blobId = testee().save(defaultBucketName, SHORT_BYTEARRAY).block();
        testee().save(CUSTOM, SHORT_BYTEARRAY).block();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(blobExistenceTester().exists(ObjectStorageBucketName.of(defaultBucketName), blobId).block())
                .isTrue();
            softly.assertThat(blobExistenceTester().exists(ObjectStorageBucketName.of(CUSTOM), blobId).block())
                .isTrue();
        });
    }

    @Test
    default void saveShouldBeAbleToSaveBlobIDsOfMultipleDataInBlobExistenceTester() {
        BucketName defaultBucketName = testee().getDefaultBucketName();

        BlobId blobIdDefault = testee().save(defaultBucketName, SHORT_BYTEARRAY).block();
        BlobId blobIdCustom = testee().save(CUSTOM, new ByteArrayInputStream(OTHER_SHORT_BYTEARRAY)).block();
        BlobId blobIdBigStream = testee().save(defaultBucketName, new ByteArrayInputStream(TWELVE_MEGABYTES)).block();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(blobExistenceTester().exists(ObjectStorageBucketName.of(defaultBucketName), blobIdDefault).block())
                .isTrue();
            softly.assertThat(blobExistenceTester().exists(ObjectStorageBucketName.of(CUSTOM), blobIdCustom).block())
                .isTrue();
            softly.assertThat(blobExistenceTester().exists(ObjectStorageBucketName.of(defaultBucketName), blobIdBigStream).block())
                .isTrue();
        });
    }

    @Test
    default void saveShouldBeAbleToSaveBlobAgainAfterDelete() {
        BucketName defaultBucketName = testee().getDefaultBucketName();

        BlobId blobId = testee().save(defaultBucketName, SHORT_BYTEARRAY).block();

        testee().delete(defaultBucketName, blobId).block();

        testee().save(defaultBucketName, SHORT_BYTEARRAY).block();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(blobExistenceTester().exists(ObjectStorageBucketName.of(defaultBucketName), blobId).block())
                .isTrue();
            softly.assertThat(testee().read(defaultBucketName, blobId))
                .hasSameContentAs(new ByteArrayInputStream(SHORT_BYTEARRAY));
        });

    }

    @Test
    default void deleteShouldDeleteBlobIdInBlobExistenceTester() {
        BucketName defaultBucketName = testee().getDefaultBucketName();

        BlobId blobId = testee().save(defaultBucketName, SHORT_BYTEARRAY).block();

        testee().delete(defaultBucketName, blobId).block();

        assertThat(blobExistenceTester().exists(ObjectStorageBucketName.of(defaultBucketName), blobId).block())
            .isFalse();
    }

    @Test
    default void deleteShouldNotDeleteOtherBlobIdsFromSameBucketInBlobExistenceTester() {
        BucketName defaultBucketName = testee().getDefaultBucketName();

        BlobId blobIdDefault = testee().save(defaultBucketName, SHORT_BYTEARRAY).block();
        BlobId blobIdCustom = testee().save(defaultBucketName, OTHER_SHORT_BYTEARRAY).block();

        testee().delete(defaultBucketName, blobIdDefault).block();

        assertThat(blobExistenceTester().exists(ObjectStorageBucketName.of(defaultBucketName), blobIdCustom).block())
            .isTrue();
    }

    @Test
    default void deleteShouldNotDeleteOtherBlobIdsFromOtherBucketsInBlobExistenceTester() {
        BucketName defaultBucketName = testee().getDefaultBucketName();

        BlobId blobIdDefault = testee().save(defaultBucketName, SHORT_BYTEARRAY).block();
        BlobId blobIdCustom = testee().save(CUSTOM, OTHER_SHORT_BYTEARRAY).block();

        testee().delete(defaultBucketName, blobIdDefault).block();

        assertThat(blobExistenceTester().exists(ObjectStorageBucketName.of(CUSTOM), blobIdCustom).block())
            .isTrue();
    }

    @Test
    default void deleteShouldNotDeleteSameBlobIdsFromOtherBucketsInBlobExistenceTester() {
        BucketName defaultBucketName = testee().getDefaultBucketName();

        BlobId blobIdDefault = testee().save(defaultBucketName, SHORT_BYTEARRAY).block();
        BlobId blobIdCustom = testee().save(CUSTOM, SHORT_BYTEARRAY).block();

        testee().delete(defaultBucketName, blobIdDefault).block();

        assertThat(blobExistenceTester().exists(ObjectStorageBucketName.of(CUSTOM), blobIdCustom).block())
            .isTrue();
    }

    @Test
    default void deleteBucketShouldDeleteAllBlobIdsOfABucketInBlobExistenceTester() {
        BucketName defaultBucketName = testee().getDefaultBucketName();

        BlobId blobIdDefault = testee().save(defaultBucketName, SHORT_BYTEARRAY).block();
        BlobId blobIdCustom = testee().save(defaultBucketName, OTHER_SHORT_BYTEARRAY).block();

        testee().deleteBucket(defaultBucketName).block();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(blobExistenceTester().exists(ObjectStorageBucketName.of(defaultBucketName), blobIdDefault).block())
                .isFalse();
            softly.assertThat(blobExistenceTester().exists(ObjectStorageBucketName.of(defaultBucketName), blobIdCustom).block())
                .isFalse();
        });
    }

    @Test
    default void deleteBucketShouldDeleteAllBlobIdsWithDifferentBucketsInBlobExistenceTester() {
        BucketName defaultBucketName = testee().getDefaultBucketName();

        BlobId blobIdDefault = testee().save(defaultBucketName, SHORT_BYTEARRAY).block();
        BlobId blobIdCustom = testee().save(CUSTOM, OTHER_SHORT_BYTEARRAY).block();

        testee().deleteBucket(defaultBucketName).block();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(blobExistenceTester().exists(ObjectStorageBucketName.of(defaultBucketName), blobIdDefault).block())
                .isFalse();
            softly.assertThat(blobExistenceTester().exists(ObjectStorageBucketName.of(CUSTOM), blobIdCustom).block())
                .isFalse();
        });
    }

    @RepeatedTest(100)
    default void deleteShouldNotAffectSubsequentSaveConsistency() throws Exception {
        BucketName defaultBucketName = testee().getDefaultBucketName();
        BlobId blobId = blobIdFactory().forPayload(SHORT_BYTEARRAY);

        int operationCount = 5;
        ConcurrentTestRunner.builder()
            .reactorOperation(((threadNumber, step) -> {
                boolean isLast = step == operationCount;
                // Guaranty the last operation to be a 'store'
                if (isLast) {
                    return testee().save(defaultBucketName, SHORT_BYTEARRAY);
                }
                // Otherwise mix store and delete
                if (Math.abs(RANDOM.nextInt()) % 2 == 0) {
                    return testee().save(defaultBucketName, SHORT_BYTEARRAY);
                }
                return testee().delete(defaultBucketName, blobId);
            }))
            .threadCount(5)
            .operationCount(operationCount)
            .runSuccessfullyWithin(Duration.ofMinutes(2));

        assertThat(testee().read(defaultBucketName, blobId))
            .hasSameContentAs(new ByteArrayInputStream(SHORT_BYTEARRAY));
    }
}
