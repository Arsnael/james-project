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

import org.apache.james.blob.api.BlobId;

import reactor.core.publisher.Mono;

/**
 * This class allows to avoid persisting two times the same blob, resulting in performance improvement and cloud provider
 * cost reduction for ObjectStorageBlobStore.
 * Implementations should hide failure to the caller, defaulting to {@link FakeBlobExistenceTester} behavior. This results
 * in the blob being stored several times which is an acceptable performance degradation.
 */
public interface BlobExistenceTester {

    /**
     * Implementations should not propagate failures. Default to false upon failure.
     */
    Mono<Boolean> exists(ObjectStorageBucketName bucketName, BlobId blobId);

    /**
     * Implementations should not propagate failures.
     */
    Mono<Void> persist(ObjectStorageBucketName bucketName, BlobId blobId);

    Mono<Void> delete(ObjectStorageBucketName bucketName, BlobId blobId);

    Mono<Void> truncateData();
}
