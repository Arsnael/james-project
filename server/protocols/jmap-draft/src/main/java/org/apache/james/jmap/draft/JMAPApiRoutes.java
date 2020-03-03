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
package org.apache.james.jmap.draft;

import static org.apache.james.jmap.HttpConstants.CONTENT_TYPE;
import static org.apache.james.jmap.HttpConstants.JSON_CONTENT_TYPE;
import static org.apache.james.jmap.HttpConstants.SC_BAD_REQUEST;
import static org.apache.james.jmap.HttpConstants.SC_INTERNAL_SERVER_ERROR;
import static org.apache.james.jmap.HttpConstants.SC_OK;
import static org.apache.james.jmap.draft.JMAPUrls.JMAP;

import java.io.IOException;

import javax.inject.Inject;

import org.apache.james.jmap.JMAPRoutes;
import org.apache.james.jmap.draft.exceptions.BadRequestException;
import org.apache.james.jmap.draft.exceptions.InternalErrorException;
import org.apache.james.jmap.draft.methods.RequestHandler;
import org.apache.james.jmap.draft.model.AuthenticatedRequest;
import org.apache.james.jmap.draft.model.InvocationRequest;
import org.apache.james.jmap.draft.model.InvocationResponse;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.metrics.api.MetricFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.server.HttpServerRoutes;

public class JMAPApiRoutes implements JMAPRoutes {
    public static final Logger LOGGER = LoggerFactory.getLogger(JMAPApiRoutes.class);

    private final ObjectMapper objectMapper;
    private final RequestHandler requestHandler;
    private final MetricFactory metricFactory;
    private final AuthenticationReactiveFilter authenticationReactiveFilter;

    @Inject
    public JMAPApiRoutes(RequestHandler requestHandler, MetricFactory metricFactory, AuthenticationReactiveFilter authenticationReactiveFilter) {
        this.requestHandler = requestHandler;
        this.metricFactory = metricFactory;
        this.authenticationReactiveFilter = authenticationReactiveFilter;
        this.objectMapper = new ObjectMapper();
        objectMapper.configure(Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
    }

    @Override
    public HttpServerRoutes define(HttpServerRoutes builder) {
        return builder.post(JMAP, this::post)
            .options(JMAP, CORS_CONTROL);
    }

    private Mono<Void> post(HttpServerRequest request, HttpServerResponse response) {
        return metricFactory.runPublishingTimerMetric("JMAP-request",
            authenticationReactiveFilter.authenticate(request)
                .flatMap(session -> post(request, response, session)));
    }

    private Mono<Void> post(HttpServerRequest request, HttpServerResponse response, MailboxSession session) {
        Flux<Object[]> responses =
            requestAsJsonStream(request)
                .map(InvocationRequest::deserialize)
                .map(invocationRequest -> AuthenticatedRequest.decorate(invocationRequest, session))
                .concatMap(this::handle)
                .map(InvocationResponse::asProtocolSpecification);

        return sendResponses(response, responses)
            .onErrorResume(BadRequestException.class, e -> handleBadRequest(response, e))
            .onErrorResume(InternalErrorException.class, e -> handleInternalError(response, e))
            .onErrorResume(e -> handleInternalError(response, e))
            .subscribeOn(Schedulers.elastic());
    }

    private Mono<Void> sendResponses(HttpServerResponse response, Flux<Object[]> responses) {
        return responses.collectList()
            .map(objects -> {
                try {
                    return objectMapper.writeValueAsString(objects);
                } catch (JsonProcessingException e) {
                    throw new InternalErrorException("error serialising JMAP API response json");
                }
            })
            .flatMap(json -> response.status(SC_OK)
                .header(CONTENT_TYPE, JSON_CONTENT_TYPE)
                .sendString(Mono.just(json))
                .then());
    }

    private Flux<? extends InvocationResponse> handle(AuthenticatedRequest request) {
        try {
            return Flux.fromStream(requestHandler.handle(request));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Flux<JsonNode[]> requestAsJsonStream(HttpServerRequest req) {
        return req.receive().aggregate().asInputStream()
            .map(inputStream -> {
                try {
                    return objectMapper.readValue(inputStream, JsonNode[][].class);
                } catch (IOException e) {
                    throw new BadRequestException("Error deserializing JSON", e);
                }
            })
            .flatMapMany(Flux::fromArray);
    }

    private Mono<Void> handleInternalError(HttpServerResponse response, Throwable e) {
        LOGGER.error("Internal error", e);
        return response.status(SC_INTERNAL_SERVER_ERROR).send();
    }

    private Mono<Void> handleBadRequest(HttpServerResponse response, BadRequestException e) {
        LOGGER.warn("Invalid authentication request received.", e);
        return response.status(SC_BAD_REQUEST).send();
    }
}
