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

import static com.datastax.driver.core.querybuilder.QueryBuilder.bindMarker;
import static com.datastax.driver.core.querybuilder.QueryBuilder.delete;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static org.apache.james.blob.objectstorage.cassandra.table.CassandraBlobExistenceTesterTable.BLOB_ID;
import static org.apache.james.blob.objectstorage.cassandra.table.CassandraBlobExistenceTesterTable.BUCKET_NAME;
import static org.apache.james.blob.objectstorage.cassandra.table.CassandraBlobExistenceTesterTable.TABLE_NAME;

import javax.inject.Inject;

import org.apache.james.backends.cassandra.utils.CassandraAsyncExecutor;
import org.apache.james.blob.api.BlobId;
import org.apache.james.blob.objectstorage.ObjectStorageBucketName;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class CassandraBlobExistenceTesterDAO {
    private final BlobId.Factory blobIdFactory;
    private final CassandraAsyncExecutor cassandraAsyncExecutor;
    private final PreparedStatement insert;
    private final PreparedStatement select;
    private final PreparedStatement selectBucket;
    private final PreparedStatement delete;

    @Inject
    CassandraBlobExistenceTesterDAO(BlobId.Factory blobIdFactory, Session session) {
        this.blobIdFactory = blobIdFactory;
        this.cassandraAsyncExecutor = new CassandraAsyncExecutor(session);
        this.insert = prepareInsert(session);
        this.select = prepareSelect(session);
        this.selectBucket = prepareSelectBucket(session);
        this.delete = prepareDelete(session);
    }

    private PreparedStatement prepareInsert(Session session) {
        return session.prepare(insertInto(TABLE_NAME)
            .value(BUCKET_NAME, bindMarker(BUCKET_NAME))
            .value(BLOB_ID, bindMarker(BLOB_ID)));
    }

    private PreparedStatement prepareSelect(Session session) {
        return session.prepare(select()
            .from(TABLE_NAME)
            .where(eq(BUCKET_NAME, bindMarker(BUCKET_NAME)))
            .and(eq(BLOB_ID, bindMarker(BLOB_ID))));
    }

    private PreparedStatement prepareSelectBucket(Session session) {
        return session.prepare(select()
            .from(TABLE_NAME)
            .where(eq(BUCKET_NAME, bindMarker(BUCKET_NAME)))
            .allowFiltering());
    }

    private PreparedStatement prepareDelete(Session session) {
        return session.prepare(delete()
            .from(TABLE_NAME)
            .where(eq(BUCKET_NAME, bindMarker(BUCKET_NAME)))
            .and(eq(BLOB_ID, bindMarker(BLOB_ID))));
    }

    Mono<Void> addBlobExistence(ObjectStorageBucketName bucketName, BlobId blobId) {
        return cassandraAsyncExecutor.executeVoid(insert.bind()
            .setString(BUCKET_NAME, bucketName.asString())
            .setString(BLOB_ID, blobId.asString()));
    }

    Mono<Void> removeBlobExistence(ObjectStorageBucketName bucketName, BlobId blobId) {
        return cassandraAsyncExecutor.executeVoid(delete.bind()
            .setString(BUCKET_NAME, bucketName.asString())
            .setString(BLOB_ID, blobId.asString()));
    }

    Mono<Boolean> hasBlobExistence(ObjectStorageBucketName bucketName, BlobId blobId) {
        return cassandraAsyncExecutor.executeSingleRow(select.bind()
                .setString(BUCKET_NAME, bucketName.asString())
                .setString(BLOB_ID, blobId.asString()))
            .map(any -> true)
            .defaultIfEmpty(false);
    }

    Flux<BlobId> getBucketBlobIds(ObjectStorageBucketName bucketName) {
        return cassandraAsyncExecutor.executeRows(selectBucket.bind()
                .setString(BUCKET_NAME, bucketName.asString()))
            .map(row -> blobIdFactory.from(row.getString(BLOB_ID)));
    }
}
