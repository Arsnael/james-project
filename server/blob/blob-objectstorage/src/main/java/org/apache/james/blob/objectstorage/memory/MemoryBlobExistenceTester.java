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

package org.apache.james.blob.objectstorage.memory;

import org.apache.james.blob.api.BlobId;
import org.apache.james.blob.objectstorage.BlobExistenceTester;
import org.apache.james.blob.objectstorage.ObjectStorageBucketName;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import reactor.core.publisher.Mono;

public class MemoryBlobExistenceTester implements BlobExistenceTester {
    private final Multimap<ObjectStorageBucketName, BlobId> blobIds;

    public MemoryBlobExistenceTester() {
        this.blobIds = Multimaps.synchronizedSetMultimap(HashMultimap.create());
    }

    @Override
    public Mono<Boolean> exists(ObjectStorageBucketName bucketName, BlobId blobId) {
        preconditionsChecks(bucketName, blobId);

        return Mono.just(blobIds.containsEntry(bucketName, blobId));
    }

    @Override
    public Mono<Void> persist(ObjectStorageBucketName bucketName, BlobId blobId) {
        preconditionsChecks(bucketName, blobId);

        return Mono.fromRunnable(() -> blobIds.put(bucketName, blobId));
    }

    @Override
    public Mono<Void> delete(ObjectStorageBucketName bucketName, BlobId blobId) {
        preconditionsChecks(bucketName, blobId);

        return Mono.fromRunnable(() -> blobIds.remove(bucketName, blobId));
    }

    @Override
    public Mono<Void> deleteBucket(ObjectStorageBucketName bucketName) {
        Preconditions.checkNotNull(bucketName, "The bucketName can't be null");

        return Mono.fromRunnable(() -> blobIds.removeAll(bucketName));
    }

    private void preconditionsChecks(ObjectStorageBucketName bucketName, BlobId blobId) {
        Preconditions.checkNotNull(bucketName, "The bucketName can't be null");
        Preconditions.checkNotNull(blobId, "The blobId can't be null");
    }
}
