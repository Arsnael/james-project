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

package org.apache.james.mailbox.elasticsearch.v6.search;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.james.backends.es.v6.AliasName;
import org.apache.james.backends.es.v6.ReadAliasName;
import org.apache.james.backends.es.v6.search.ScrollIterable;
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.elasticsearch.v6.json.JsonMessageConstants;
import org.apache.james.mailbox.elasticsearch.v6.query.QueryConverter;
import org.apache.james.mailbox.elasticsearch.v6.query.SortConverter;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.model.SearchQuery;
import org.apache.james.mailbox.store.search.MessageSearchIndex;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.document.DocumentField;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

public class ElasticSearchSearcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticSearchSearcher.class);
    private static final TimeValue TIMEOUT = new TimeValue(60000);

    private final RestHighLevelClient client;
    private final QueryConverter queryConverter;
    private final int size;
    private final MailboxId.Factory mailboxIdFactory;
    private final MessageId.Factory messageIdFactory;
    private final AliasName aliasName;

    public ElasticSearchSearcher(RestHighLevelClient client, QueryConverter queryConverter, int size,
                                 MailboxId.Factory mailboxIdFactory, MessageId.Factory messageIdFactory,
                                 ReadAliasName aliasName) {
        this.client = client;
        this.queryConverter = queryConverter;
        this.size = size;
        this.mailboxIdFactory = mailboxIdFactory;
        this.messageIdFactory = messageIdFactory;
        this.aliasName = aliasName;
    }

    public Stream<MessageSearchIndex.SearchResult> search(Collection<MailboxId> mailboxIds, SearchQuery query,
                                                          Optional<Long> limit) {
        SearchRequest searchRequest = getSearchRequest(mailboxIds, query, limit);
        Stream<MessageSearchIndex.SearchResult> pairStream = new ScrollIterable(client, searchRequest).stream()
            .flatMap(this::transformResponseToUidStream);

        return limit.map(pairStream::limit)
            .orElse(pairStream);
    }

    private SearchRequest getSearchRequest(Collection<MailboxId> users, SearchQuery query, Optional<Long> limit) {
        return query.getSorts()
            .stream()
            .reduce(
                new SearchRequest(aliasName.getValue())
                    .scroll(TIMEOUT),
                (searchRequest, sort) -> searchRequest.source(getSearchSourceBuilder(users, query, limit, SortConverter.convertSort(sort))),
                (partialResult1, partialResult2) -> partialResult1);
    }

    private SearchSourceBuilder getSearchSourceBuilder(Collection<MailboxId> users, SearchQuery query, Optional<Long> limit, SortBuilder sort) {
        return new SearchSourceBuilder()
            .query(queryConverter.from(users, query))
            .size(computeRequiredSize(limit))
            .storedFields(ImmutableList.of(JsonMessageConstants.MAILBOX_ID, JsonMessageConstants.UID, JsonMessageConstants.MESSAGE_ID))
            .sort(sort);
    }

    private int computeRequiredSize(Optional<Long> limit) {
        return limit.map(value -> Math.min(value.intValue(), size))
            .orElse(size);
    }

    private Stream<MessageSearchIndex.SearchResult> transformResponseToUidStream(SearchResponse searchResponse) {
        return Arrays.stream(searchResponse.getHits().getHits())
            .map(this::extractContentFromHit)
            .filter(Optional::isPresent)
            .map(Optional::get);
    }

    private Optional<MessageSearchIndex.SearchResult> extractContentFromHit(SearchHit hit) {
        DocumentField mailboxId = hit.field(JsonMessageConstants.MAILBOX_ID);
        DocumentField uid = hit.field(JsonMessageConstants.UID);
        Optional<DocumentField> id = retrieveMessageIdField(hit);
        if (mailboxId != null && uid != null) {
            Number uidAsNumber = uid.getValue();
            return Optional.of(
                new MessageSearchIndex.SearchResult(
                    id.map(field -> messageIdFactory.fromString(field.getValue())),
                    mailboxIdFactory.fromString(mailboxId.getValue()),
                    MessageUid.of(uidAsNumber.longValue())));
        } else {
            LOGGER.warn("Can not extract UID, MessageID and/or MailboxId for search result {}", hit.getId());
            return Optional.empty();
        }
    }

    private Optional<DocumentField> retrieveMessageIdField(SearchHit hit) {
        if (hit.getFields().keySet().contains(JsonMessageConstants.MESSAGE_ID)) {
            return Optional.ofNullable(hit.field(JsonMessageConstants.MESSAGE_ID));
        } else {
            return Optional.empty();
        }
    }

}
