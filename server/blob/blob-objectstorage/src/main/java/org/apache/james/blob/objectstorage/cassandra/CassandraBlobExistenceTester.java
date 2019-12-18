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

import javax.inject.Inject;

import org.apache.james.blob.api.BlobId;
import org.apache.james.blob.objectstorage.BlobExistenceTester;
import org.apache.james.blob.objectstorage.ObjectStorageBucketName;

import com.google.common.base.Preconditions;

import reactor.core.publisher.Mono;

public class CassandraBlobExistenceTester implements BlobExistenceTester {
    private final CassandraBlobExistenceTesterDAO cassandraBlobExistenceTesterDAO;

    @Inject
    public CassandraBlobExistenceTester(CassandraBlobExistenceTesterDAO cassandraBlobExistenceTesterDAO) {
        this.cassandraBlobExistenceTesterDAO = cassandraBlobExistenceTesterDAO;
    }

    @Override
    public Mono<Boolean> exists(ObjectStorageBucketName bucketName, BlobId blobId) {
        preconditionsChecks(bucketName, blobId);

        return cassandraBlobExistenceTesterDAO.hasBlobExistence(bucketName, blobId);
    }

    @Override
    public Mono<Void> persist(ObjectStorageBucketName bucketName, BlobId blobId) {
        preconditionsChecks(bucketName, blobId);

        return cassandraBlobExistenceTesterDAO.addBlobExistence(bucketName, blobId);
    }

    @Override
    public Mono<Void> delete(ObjectStorageBucketName bucketName, BlobId blobId) {
        preconditionsChecks(bucketName, blobId);

        return cassandraBlobExistenceTesterDAO.removeBlobExistence(bucketName, blobId);
    }

    @Override
    public Mono<Void> deleteBucket(ObjectStorageBucketName bucketName) {
        Preconditions.checkNotNull(bucketName, "The bucketName can't be null");

        return cassandraBlobExistenceTesterDAO.getBucketBlobIds(bucketName)
            .flatMap(blobId -> delete(bucketName, blobId))
            .then();
    }

    private void preconditionsChecks(ObjectStorageBucketName bucketName, BlobId blobId) {
        Preconditions.checkNotNull(bucketName, "The bucketName can't be null");
        Preconditions.checkNotNull(blobId, "The blobId can't be null");
    }
}
