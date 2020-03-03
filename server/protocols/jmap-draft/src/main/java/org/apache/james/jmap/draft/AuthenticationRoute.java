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

import java.io.IOException;
import java.util.function.BiFunction;

import javax.servlet.http.HttpServletResponse;

import org.apache.james.core.Username;
import org.apache.james.jmap.draft.api.AccessTokenManager;
import org.apache.james.jmap.draft.api.SimpleTokenFactory;
import org.apache.james.jmap.draft.api.SimpleTokenManager;
import org.apache.james.jmap.draft.exceptions.BadRequestException;
import org.apache.james.jmap.draft.exceptions.InternalErrorException;
import org.apache.james.jmap.draft.model.AccessTokenRequest;
import org.apache.james.jmap.draft.model.AccessTokenResponse;
import org.apache.james.jmap.draft.model.ContinuationTokenRequest;
import org.apache.james.jmap.draft.model.ContinuationTokenResponse;
import org.apache.james.jmap.draft.model.EndPointsResponse;
import org.apache.james.metrics.api.MetricFactory;
import org.apache.james.metrics.api.TimeMetric;
import org.apache.james.user.api.UsersRepository;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

public class AuthenticationRoute implements BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> {
    public static final String JSON_CONTENT_TYPE = "application/json";
    public static final String JSON_CONTENT_TYPE_UTF8 = "application/json; charset=UTF-8";

    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationServlet.class);

    private final ObjectMapper mapper;
    private final UsersRepository usersRepository;
    private final SimpleTokenManager simpleTokenManager;
    private final AccessTokenManager accessTokenManager;
    private final SimpleTokenFactory simpleTokenFactory;
    private final MetricFactory metricFactory;

    public AuthenticationRoute(ObjectMapper mapper, UsersRepository usersRepository, SimpleTokenManager simpleTokenManager, AccessTokenManager accessTokenManager, SimpleTokenFactory simpleTokenFactory, MetricFactory metricFactory) {
        this.mapper = mapper;
        this.usersRepository = usersRepository;
        this.simpleTokenManager = simpleTokenManager;
        this.accessTokenManager = accessTokenManager;
        this.simpleTokenFactory = simpleTokenFactory;
        this.metricFactory = metricFactory;
    }

    @Override
    public Publisher<Void> apply(HttpServerRequest request, HttpServerResponse response) {
        TimeMetric timeMetric = metricFactory.timer("JMAP-authentication-post");
        try {
            assertJsonContentType(request);
            assertAcceptJsonOnly(request);

            return deserialize(request)
                .flatMap(objectRequest -> {
                    if (request instanceof ContinuationTokenRequest) {
                        return handleContinuationTokenRequest((ContinuationTokenRequest)objectRequest, response);
                    } else if (request instanceof AccessTokenRequest) {
                        return handleAccessTokenRequest((AccessTokenRequest)objectRequest, response);
                    } else {
                        throw new RuntimeException();
                    }
                })
                .onErrorResume(BadRequestException.class, e -> {
                    LOG.warn("Invalid authentication request received.", e);
                    return response.status(HttpServletResponse.SC_BAD_REQUEST).send();
                });

        } catch (InternalErrorException e) {
            LOG.error("Internal error", e);
            response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).send();
        } finally {
            timeMetric.stopAndPublish();
        }
    }

    private Mono<Object> deserialize(HttpServerRequest req) {
        return req.receive().aggregate().asInputStream()
            .map(inputStream -> {
                try {
                    return mapper.readValue(inputStream, Object.class);
                } catch (IOException e) {
                    throw new BadRequestException("Request can't be deserialized", e);
                }
            });
    }

    private void assertJsonContentType(HttpServerRequest req) {
        if (! req.requestHeaders().get("ContentType").equals(JSON_CONTENT_TYPE_UTF8)) {
            throw new BadRequestException("Request ContentType header must be set to: " + JSON_CONTENT_TYPE_UTF8);
        }
    }

    private void assertAcceptJsonOnly(HttpServerRequest req) {
        String accept = req.requestHeaders().get("Accept");
        if (accept == null || ! accept.contains(JSON_CONTENT_TYPE)) {
            throw new BadRequestException("Request Accept header must be set to JSON content type");
        }
    }

    private Mono<Void> handleContinuationTokenRequest(ContinuationTokenRequest request, HttpServerResponse resp) {
        resp.setContentType(JSON_CONTENT_TYPE_UTF8);
        try {
            ContinuationTokenResponse continuationTokenResponse = ContinuationTokenResponse
                .builder()
                .continuationToken(simpleTokenFactory.generateContinuationToken(request.getUsername()))
                .methods(ContinuationTokenResponse.AuthenticationMethod.PASSWORD)
                .build();
            mapper.writeValue(resp.getOutputStream(), continuationTokenResponse);
        } catch (Exception e) {
            throw new InternalErrorException("Error while responding to continuation token", e);
        }
    }

    private Mono<Void> handleAccessTokenRequest(AccessTokenRequest request, HttpServerResponse resp) {
        switch (simpleTokenManager.getValidity(request.getToken())) {
            case EXPIRED:
                returnRestartAuthentication(resp);
                break;
            case INVALID:
                LOG.warn("Use of an invalid ContinuationToken : {}", request.getToken().serialize());
                returnUnauthorizedResponse(resp);
                break;
            case OK:
                manageAuthenticationResponse(request, resp);
                break;
        }
    }

    private Mono<Void> returnAccessTokenResponse(HttpServerResponse resp, Username username) throws IOException {
        resp.setContentType(JSON_CONTENT_TYPE_UTF8);
        resp.setStatus(HttpServletResponse.SC_CREATED);
        AccessTokenResponse response = AccessTokenResponse
            .builder()
            .accessToken(accessTokenManager.grantAccessToken(username))
            .api(JMAPUrls.JMAP)
            .eventSource("/notImplemented")
            .upload(JMAPUrls.UPLOAD)
            .download(JMAPUrls.DOWNLOAD)
            .build();
        mapper.writeValue(resp.getOutputStream(), response);
    }

    private Mono<Void> returnEndPointsResponse(HttpServerResponse resp) throws IOException {
        resp.setContentType(JSON_CONTENT_TYPE_UTF8);
        resp.setStatus(HttpServletResponse.SC_OK);
        EndPointsResponse response = EndPointsResponse
            .builder()
            .api(JMAPUrls.JMAP)
            .eventSource("/notImplemented")
            .upload(JMAPUrls.UPLOAD)
            .download(JMAPUrls.DOWNLOAD)
            .build();
        mapper.writeValue(resp.getOutputStream(), response);
    }

}
