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

import static org.apache.james.jmap.HttpConstants.ACCEPT;
import static org.apache.james.jmap.HttpConstants.CONTENT_TYPE;
import static org.apache.james.jmap.HttpConstants.JSON_CONTENT_TYPE;
import static org.apache.james.jmap.HttpConstants.JSON_CONTENT_TYPE_UTF8;
import static org.apache.james.jmap.HttpConstants.SC_CREATED;
import static org.apache.james.jmap.HttpConstants.SC_FORBIDDEN;
import static org.apache.james.jmap.HttpConstants.SC_NO_CONTENT;
import static org.apache.james.jmap.HttpConstants.SC_OK;
import static org.apache.james.jmap.HttpConstants.SC_UNAUTHORIZED;

import java.io.IOException;

import javax.inject.Inject;

import org.apache.james.core.Username;
import org.apache.james.jmap.JMAPRoutes;
import org.apache.james.jmap.api.access.AccessToken;
import org.apache.james.jmap.draft.api.AccessTokenManager;
import org.apache.james.jmap.draft.api.SimpleTokenFactory;
import org.apache.james.jmap.draft.api.SimpleTokenManager;
import org.apache.james.jmap.draft.exceptions.BadRequestException;
import org.apache.james.jmap.draft.exceptions.InternalErrorException;
import org.apache.james.jmap.draft.exceptions.UnauthorizedException;
import org.apache.james.jmap.draft.model.AccessTokenRequest;
import org.apache.james.jmap.draft.model.AccessTokenResponse;
import org.apache.james.jmap.draft.model.ContinuationTokenRequest;
import org.apache.james.jmap.draft.model.ContinuationTokenResponse;
import org.apache.james.jmap.draft.model.EndPointsResponse;
import org.apache.james.metrics.api.MetricFactory;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.api.UsersRepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.server.HttpServerRoutes;

public class AuthenticationRoutes implements JMAPRoutes {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationRoutes.class);

    private final ObjectMapper mapper;
    private final UsersRepository usersRepository;
    private final SimpleTokenManager simpleTokenManager;
    private final AccessTokenManager accessTokenManager;
    private final SimpleTokenFactory simpleTokenFactory;
    private final MetricFactory metricFactory;
    private final AuthenticationReactiveFilter authenticationReactiveFilter;

    @Inject
    public AuthenticationRoutes(ObjectMapper mapper, UsersRepository usersRepository, SimpleTokenManager simpleTokenManager, AccessTokenManager accessTokenManager, SimpleTokenFactory simpleTokenFactory, MetricFactory metricFactory, AuthenticationReactiveFilter authenticationReactiveFilter) {
        this.mapper = mapper;
        this.usersRepository = usersRepository;
        this.simpleTokenManager = simpleTokenManager;
        this.accessTokenManager = accessTokenManager;
        this.simpleTokenFactory = simpleTokenFactory;
        this.metricFactory = metricFactory;
        this.authenticationReactiveFilter = authenticationReactiveFilter;
    }

    @Override
    public Logger logger() {
        return LOGGER;
    }

    @Override
    public HttpServerRoutes define(HttpServerRoutes builder) {
        return builder
            .post("/authentication", this::post)
            .get("/authentication", this::returnEndPointsResponse)
            .delete("/authentication", this::delete)
            .options("/authentication", CORS_CONTROL);
    }

    private Mono<Void> post(HttpServerRequest request, HttpServerResponse response) {
        return metricFactory.runPublishingTimerMetric("JMAP-authentication-post",
            Mono.just(request)
                .map(this::assertJsonContentType)
                .map(this::assertAcceptJsonOnly)
                .flatMap(this::deserialize)
                .flatMap(objectRequest -> {
                    if (objectRequest instanceof ContinuationTokenRequest) {
                        return handleContinuationTokenRequest((ContinuationTokenRequest) objectRequest, response);
                    } else if (objectRequest instanceof AccessTokenRequest) {
                        return handleAccessTokenRequest((AccessTokenRequest) objectRequest, response);
                    } else {
                        throw new RuntimeException();
                    }
                })
                .onErrorResume(BadRequestException.class, e -> handleBadRequest(response, e))
                .onErrorResume(InternalErrorException.class, e -> handleInternalError(response, e)))
                .subscribeOn(Schedulers.elastic());
    }

    private Mono<Void> returnEndPointsResponse(HttpServerRequest req, HttpServerResponse resp) {
        try {
            return authenticationReactiveFilter.authenticate(req)
                .then(resp.status(SC_OK)
                .header(CONTENT_TYPE, JSON_CONTENT_TYPE_UTF8)
                    .sendString(Mono.just(mapper.writeValueAsString(EndPointsResponse
                        .builder()
                        .api(JMAPUrls.JMAP)
                        .eventSource("/notImplemented")
                        .upload(JMAPUrls.UPLOAD)
                        .download(JMAPUrls.DOWNLOAD)
                        .build())))
                    .then())
                .onErrorResume(BadRequestException.class, e -> handleBadRequest(resp, e))
                .onErrorResume(InternalErrorException.class, e -> handleInternalError(resp, e))
                .onErrorResume(UnauthorizedException.class, e -> handleAuthenticationFailure(resp, e))
                .subscribeOn(Schedulers.elastic());
        } catch (JsonProcessingException e) {
            throw new InternalErrorException("Error serializing endpoint response", e);
        }
    }

    private Mono<Void> delete(HttpServerRequest req, HttpServerResponse resp) {
        String authorizationHeader = req.requestHeaders().get("Authorization");

        return authenticationReactiveFilter.authenticate(req)
            .then(accessTokenManager.revoke(AccessToken.fromString(authorizationHeader)))
            .then(resp.status(SC_NO_CONTENT).then())
            .onErrorResume(UnauthorizedException.class, e -> handleAuthenticationFailure(resp, e))
            .subscribeOn(Schedulers.elastic());
    }

    private HttpServerRequest assertJsonContentType(HttpServerRequest req) {
        if (! req.requestHeaders().get(CONTENT_TYPE).equals(JSON_CONTENT_TYPE_UTF8)) {
            throw new BadRequestException("Request ContentType header must be set to: " + JSON_CONTENT_TYPE_UTF8);
        }
        return req;
    }

    private HttpServerRequest assertAcceptJsonOnly(HttpServerRequest req) {
        String accept = req.requestHeaders().get(ACCEPT);
        if (accept == null || ! accept.contains(JSON_CONTENT_TYPE)) {
            throw new BadRequestException("Request Accept header must be set to JSON content type");
        }
        return req;
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

    private Mono<Void> handleContinuationTokenRequest(ContinuationTokenRequest request, HttpServerResponse resp) {
        resp.header(CONTENT_TYPE, JSON_CONTENT_TYPE_UTF8);
        try {
            ContinuationTokenResponse continuationTokenResponse = ContinuationTokenResponse
                .builder()
                .continuationToken(simpleTokenFactory.generateContinuationToken(request.getUsername()))
                .methods(ContinuationTokenResponse.AuthenticationMethod.PASSWORD)
                .build();
            return resp.sendString(Mono.just(mapper.writeValueAsString(continuationTokenResponse)))
                .then();
        } catch (Exception e) {
            throw new InternalErrorException("Error while responding to continuation token", e);
        }
    }

    private Mono<Void> handleAccessTokenRequest(AccessTokenRequest request, HttpServerResponse resp) {
        SimpleTokenManager.TokenStatus validity = simpleTokenManager.getValidity(request.getToken());
        switch (validity) {
            case EXPIRED:
                return returnRestartAuthentication(resp);
            case INVALID:
                LOGGER.warn("Use of an invalid ContinuationToken : {}", request.getToken().serialize());
                return returnUnauthorizedResponse(resp);
            case OK:
                return manageAuthenticationResponse(request, resp);
            default:
                throw new InternalErrorException(String.format("Validity %s is not implemented", validity));
        }
    }

    private Mono<Void> manageAuthenticationResponse(AccessTokenRequest request, HttpServerResponse resp) {
        Username username = Username.of(request.getToken().getUsername());

        return authenticate(request, username)
            .flatMap(success -> {
                if (success) {
                    return returnAccessTokenResponse(resp, username);
                } else {
                    LOGGER.info("Authentication failure for {}", username);
                    return returnUnauthorizedResponse(resp);
                }
            });
    }

    private Mono<Boolean> authenticate(AccessTokenRequest request, Username username) {
        return Mono.fromCallable(() -> {
            try {
                return usersRepository.test(username, request.getPassword());
            } catch (UsersRepositoryException e) {
                LOGGER.error("Error while trying to validate authentication for user '{}'", username, e);
                return false;
            }
        });
    }

    private Mono<Void> returnAccessTokenResponse(HttpServerResponse resp, Username username) {
        return accessTokenManager.grantAccessToken(username)
            .map(accessToken -> AccessTokenResponse
                .builder()
                .accessToken(accessToken)
                .api(JMAPUrls.JMAP)
                .eventSource("/notImplemented")
                .upload(JMAPUrls.UPLOAD)
                .download(JMAPUrls.DOWNLOAD)
                .build())
            .flatMap(accessTokenResponse -> {
                try {
                    return resp.status(SC_CREATED)
                        .header(CONTENT_TYPE, JSON_CONTENT_TYPE_UTF8)
                        .sendString(Mono.just(mapper.writeValueAsString(accessTokenResponse)))
                        .then();
                } catch (JsonProcessingException e) {
                    throw new InternalErrorException("Could not serialize access token response", e);
                }
            });
    }

    private Mono<Void> returnUnauthorizedResponse(HttpServerResponse resp) {
        return resp.status(SC_UNAUTHORIZED).send().then();
    }

    private Mono<Void> returnRestartAuthentication(HttpServerResponse resp) {
        return resp.status(SC_FORBIDDEN).send().then();
    }
}
