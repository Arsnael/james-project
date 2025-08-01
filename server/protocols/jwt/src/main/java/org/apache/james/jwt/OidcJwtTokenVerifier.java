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

package org.apache.james.jwt;

import java.net.URL;
import java.util.Optional;

import org.apache.james.jwt.introspection.IntrospectionEndpoint;
import org.apache.james.jwt.introspection.TokenIntrospectionResponse;
import org.reactivestreams.Publisher;

import reactor.core.publisher.Mono;

public class OidcJwtTokenVerifier {
    public static final CheckTokenClient CHECK_TOKEN_CLIENT = new DefaultCheckTokenClient();

    public static Optional<String> verifySignatureAndExtractClaim(String jwtToken, URL jwksURL, String claimName) {
        return new JwtTokenVerifier(JwksPublicKeyProvider.of(jwksURL))
            .verifyAndExtractClaim(jwtToken, claimName, String.class);
    }

    public static Publisher<String> verifyWithIntrospection(String jwtToken, URL jwksURL, String claimName, IntrospectionEndpoint introspectionEndpoint) {
        return Mono.fromCallable(() -> verifySignatureAndExtractClaim(jwtToken, jwksURL, claimName))
            .flatMap(optional -> optional.map(Mono::just).orElseGet(Mono::empty))
            .flatMap(claimResult -> Mono.from(CHECK_TOKEN_CLIENT.introspect(introspectionEndpoint, jwtToken))
                .filter(TokenIntrospectionResponse::active)
                .filter(tokenIntrospectionResponse -> tokenIntrospectionResponse.claimByPropertyName(claimName)
                    .map(claim -> claim.equals(claimResult))
                    .orElse(false))
                .map(activeResponse -> claimResult));
    }

    public static Publisher<String> verifyWithUserinfo(String jwtToken, URL jwksURL, String claimName, URL userinfoEndpoint) {
        return Mono.fromCallable(() -> verifySignatureAndExtractClaim(jwtToken, jwksURL, claimName))
            .flatMap(optional -> optional.map(Mono::just).orElseGet(Mono::empty))
            .flatMap(claimResult -> Mono.from(CHECK_TOKEN_CLIENT.userInfo(userinfoEndpoint, jwtToken))
                .filter(userinfoResponse -> userinfoResponse.claimByPropertyName(claimName)
                    .map(claim -> claim.equals(claimResult))
                    .orElse(false))
                .map(userinfoResponse -> claimResult));
    }
}
