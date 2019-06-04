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

package org.apache.james.backends.es;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.james.backends.es.search.ScrollIterable;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import com.google.common.annotations.VisibleForTesting;

public class DeleteByQueryPerformer {
    public static final TimeValue TIMEOUT = new TimeValue(60000);

    private final RestHighLevelClient client;
    private final ExecutorService executor;
    private final int batchSize;
    private final WriteAliasName aliasName;

    @VisibleForTesting
    public DeleteByQueryPerformer(RestHighLevelClient client, ExecutorService executor, int batchSize, WriteAliasName aliasName) {
        this.client = client;
        this.executor = executor;
        this.batchSize = batchSize;
        this.aliasName = aliasName;
    }

    public Future<Void> perform(QueryBuilder queryBuilder) {
        return executor.submit(() -> doDeleteByQuery(queryBuilder));
    }

    protected Void doDeleteByQuery(QueryBuilder queryBuilder) {
        new ScrollIterable(client, prepareSearch(queryBuilder))
            .stream()
            .map(searchResponse -> deleteRetrievedIds(client, searchResponse))
            .forEach(CompletableFuture::join);
        return null;
    }

    private SearchRequest prepareSearch(QueryBuilder queryBuilder) {
        return new SearchRequest(aliasName.getValue())
            .types(NodeMappingFactory.DEFAULT_MAPPING_NAME)
            .scroll(TIMEOUT)
            .source(searchSourceBuilder(queryBuilder));
    }

    private SearchSourceBuilder searchSourceBuilder(QueryBuilder queryBuilder) {
        return new SearchSourceBuilder()
            .query(queryBuilder)
            .size(batchSize);
    }

    private CompletableFuture<BulkResponse> deleteRetrievedIds(RestHighLevelClient client, SearchResponse searchResponse) {
        BulkRequest request = new BulkRequest();

        for (SearchHit hit : searchResponse.getHits()) {
            request.add(
                new DeleteRequest(aliasName.getValue())
                    .type(NodeMappingFactory.DEFAULT_MAPPING_NAME)
                    .id(hit.getId()));
        }
        ListenerToFuture<BulkResponse> listener = new ListenerToFuture<>();
        client.bulkAsync(request, listener);

        return listener.getFuture();
    }
}
