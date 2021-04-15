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

package org.apache.james.blob.api;

import static org.apache.james.blob.api.BlobStoreDAOFixture.ELEVEN_KILOBYTES;
import static org.apache.james.blob.api.BlobStoreDAOFixture.EMPTY_BYTEARRAY;
import static org.apache.james.blob.api.BlobStoreDAOFixture.SHORT_BYTEARRAY;
import static org.apache.james.blob.api.BlobStoreDAOFixture.TEST_BLOB_ID;
import static org.apache.james.blob.api.BlobStoreDAOFixture.TEST_BUCKET_NAME;
import static org.apache.james.blob.api.BlobStoreDAOFixture.TWELVE_MEGABYTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import org.apache.james.util.concurrency.ConcurrentTestRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.io.ByteSource;

import reactor.core.publisher.Mono;

public interface ReadSaveBlobStoreDAOContract {

    BlobStoreDAO testee();

    static Stream<Arguments> blobs() {
        return Stream.of(new Object[]{"SHORT", SHORT_BYTEARRAY}, new Object[]{"LONG", ELEVEN_KILOBYTES}, new Object[]{"BIG", TWELVE_MEGABYTES})
            .map(Arguments::of);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource(value = "blobs")
    default void concurrentSaveBytesShouldReturnConsistentValues(String description, byte[] bytes) throws ExecutionException, InterruptedException {
        Mono.from(testee().save(TEST_BUCKET_NAME, TEST_BLOB_ID, bytes)).block();
        ConcurrentTestRunner.builder()
            .randomlyDistributedReactorOperations(
                (threadNumber, step) -> testee().save(TEST_BUCKET_NAME, TEST_BLOB_ID, bytes),
                (threadNumber, step) -> checkConcurrentSaveOperation(bytes)
            )
            .threadCount(10)
            .operationCount(20)
            .runSuccessfullyWithin(Duration.ofMinutes(10));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("blobs")
    default void concurrentSaveInputStreamShouldReturnConsistentValues(String description, byte[] bytes) throws ExecutionException, InterruptedException {
        Mono.from(testee().save(TEST_BUCKET_NAME, TEST_BLOB_ID, bytes)).block();
        ConcurrentTestRunner.builder()
            .randomlyDistributedReactorOperations(
                (threadNumber, step) -> testee().save(TEST_BUCKET_NAME, TEST_BLOB_ID, new ByteArrayInputStream(bytes)),
                (threadNumber, step) -> checkConcurrentSaveOperation(bytes)
            )
            .threadCount(10)
            .operationCount(20)
            .runSuccessfullyWithin(Duration.ofMinutes(10));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("blobs")
    default void concurrentSaveByteSourceShouldReturnConsistentValues(String description, byte[] bytes) throws ExecutionException, InterruptedException {
        Mono.from(testee().save(TEST_BUCKET_NAME, TEST_BLOB_ID, bytes)).block();
        ConcurrentTestRunner.builder()
            .randomlyDistributedReactorOperations(
                (threadNumber, step) -> testee().save(TEST_BUCKET_NAME, TEST_BLOB_ID, ByteSource.wrap(bytes)),
                (threadNumber, step) -> checkConcurrentSaveOperation(bytes)
            )
            .threadCount(2)
            .operationCount(2)
            .runSuccessfullyWithin(Duration.ofMinutes(2));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("blobs")
    default void testIsolation(String description, byte[] bytes) throws ExecutionException, InterruptedException {
        Mono.from(testee().save(TEST_BUCKET_NAME, TEST_BLOB_ID, bytes)).block();
        ConcurrentTestRunner.builder()
            .operation((threadNumber, step) -> {
                if (threadNumber == 1) {
                    testee().save(TEST_BUCKET_NAME, TEST_BLOB_ID, ByteSource.wrap(bytes));
                } else {
                    checkConcurrentSaveOperation(bytes);
                }
            })
            .threadCount(2)
            .operationCount(4)
            .runSuccessfullyWithin(Duration.ofMinutes(10));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("blobs")
    default void parrallel(String description, byte[] bytes) throws ExecutionException, InterruptedException {
        Mono.from(testee().save(TEST_BUCKET_NAME, TEST_BLOB_ID, bytes)).block();
        ConcurrentTestRunner.builder()
            .operation(
                (threadNumber, step) -> testee().save(TEST_BUCKET_NAME, new TestBlobId(UUID.randomUUID().toString()), ByteSource.wrap(bytes))
            )
            .threadCount(10)
            .operationCount(2)
            .runSuccessfullyWithin(Duration.ofMinutes(10));
    }

    default Mono<Void> checkConcurrentSaveOperation(byte[] expected) {
        return Mono.from(testee().readBytes(TEST_BUCKET_NAME, TEST_BLOB_ID))
            //assertj is very cpu-intensive, let's compute the assertion only when arrays are different
            .filter(bytes -> !Arrays.equals(bytes, expected))
            .doOnNext(bytes -> assertThat(bytes).isEqualTo(expected))
            .then();
    }

    default FilterInputStream getThrowingInputStream() {
        return new FilterInputStream(new ByteArrayInputStream(TWELVE_MEGABYTES)) {
            int failingThreshold = 5;
            int alreadyRead = 0;

            @Override
            public int read() throws IOException {
                if (alreadyRead < failingThreshold) {
                    alreadyRead++;
                    return super.read();
                } else {
                    throw new IOException("error on read");
                }
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                int remaining = b.length - off;
                int toRead = Math.min(remaining, len);
                for (int i = 0; i < toRead; i++) {
                    int value = read();
                    if (value != -1) {
                        b[off] = (byte) value;
                    }
                }
                return toRead;
            }

        };
    }

}
