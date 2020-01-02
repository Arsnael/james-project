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

import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.backends.cassandra.CassandraClusterExtension;
import org.apache.james.blob.api.BlobId;
import org.apache.james.blob.objectstorage.BlobExistenceTester;
import org.apache.james.blob.objectstorage.BlobExistenceTesterContract;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.RegisterExtension;

class CassandraBlobExistenceTesterTest implements BlobExistenceTesterContract {
    @RegisterExtension
    static CassandraClusterExtension cassandraCluster = new CassandraClusterExtension(CassandraBlobExistenceModule.MODULE);

    BlobExistenceTester testee;

    @BeforeEach
    void setUp(CassandraCluster cassandra) {
        CassandraBlobExistenceTesterDAO blobExistenceTesterDAO = new CassandraBlobExistenceTesterDAO(cassandra.getConf());
        testee = new CassandraBlobExistenceTester(blobExistenceTesterDAO);
    }

    @Override
    public BlobExistenceTester testee() {
        return testee;
    }

    @Override
    public BlobId.Factory blobIdFactory() {
        return BLOB_ID_FACTORY;
    }

    @Override
    @Disabled("We truncate the all table upon delete bucket to avoid a full scan on it")
    public void deleteBucketShouldNotDeleteEntriesOfOtherBuckets() {

    }
}
