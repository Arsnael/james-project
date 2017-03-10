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

package org.apache.james.webadmin.routes;

import static org.assertj.core.api.Assertions.assertThat;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.config.EncoderConfig.encoderConfig;
import static com.jayway.restassured.config.RestAssuredConfig.newConfig;
import static org.apache.james.webadmin.WebAdminServer.NO_CONFIGURATION;
import static org.hamcrest.CoreMatchers.is;

import org.apache.james.mailbox.inmemory.quota.InMemoryPerUserMaxQuotaManager;
import org.apache.james.mailbox.model.Quota;
import org.apache.james.webadmin.WebAdminServer;
import org.apache.james.webadmin.utils.JsonTransformer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.http.ContentType;

public class GlobalQuotaRoutesTest {

    private WebAdminServer webAdminServer;
    private InMemoryPerUserMaxQuotaManager maxQuotaManager;

    @Before
    public void setUp() throws Exception {
        maxQuotaManager = new InMemoryPerUserMaxQuotaManager();
        webAdminServer = new WebAdminServer(new GlobalQuotaRoutes(maxQuotaManager, new JsonTransformer()));
        webAdminServer.configure(NO_CONFIGURATION);
        webAdminServer.await();

        RestAssured.requestSpecification = new RequestSpecBuilder()
            .setContentType(ContentType.JSON)
            .setAccept(ContentType.JSON)
            .setConfig(newConfig().encoderConfig(encoderConfig().defaultContentCharset(Charsets.UTF_8)))
            .setPort(webAdminServer.getPort().toInt())
            .build();
    }

    @After
    public void stop() {
        webAdminServer.destroy();
    }

    @Test
    public void getCountQuotaCountShouldReturnUnlimitedByDefault() {
        given()
            .get(GlobalQuotaRoutes.COUNT_ENDPOINT)
        .then()
            .statusCode(200)
            .body(is(String.valueOf(Quota.UNLIMITED)));
    }

    @Test
    public void getCountShouldReturnStoredValue() throws Exception{
        int value = 42;
        maxQuotaManager.setDefaultMaxMessage(value);

        given()
            .get(GlobalQuotaRoutes.COUNT_ENDPOINT)
        .then()
            .statusCode(200)
            .body(is(String.valueOf(value)));
    }

    @Test
    public void putCountShouldRejectInvalid() throws Exception {
        given()
            .body("invalid")
            .put(GlobalQuotaRoutes.COUNT_ENDPOINT)
        .then()
            .statusCode(400);
    }

    @Test
    public void putCountShouldRejectNegative() throws Exception {
        given()
            .body("-1")
            .put(GlobalQuotaRoutes.COUNT_ENDPOINT)
        .then()
            .statusCode(400);
    }

    @Test
    public void putCountShouldAcceptValidValue() throws Exception {
        given()
            .body("42")
            .put(GlobalQuotaRoutes.COUNT_ENDPOINT)
        .then()
            .statusCode(204);

        assertThat(maxQuotaManager.getDefaultMaxMessage()).isEqualTo(42);
    }

    @Test
    public void deleteCountShouldSetQuotaToUnlimited() throws Exception {
        maxQuotaManager.setDefaultMaxMessage(42);

        given()
            .delete(GlobalQuotaRoutes.COUNT_ENDPOINT)
        .then()
            .statusCode(204);

        assertThat(maxQuotaManager.getDefaultMaxMessage()).isEqualTo(Quota.UNLIMITED);
    }

    @Test
    public void getSizeQuotaCountShouldReturnUnlimitedByDefault() {
        given()
            .get(GlobalQuotaRoutes.SIZE_ENDPOINT)
        .then()
            .statusCode(200)
            .body(is(String.valueOf(Quota.UNLIMITED)));
    }

    @Test
    public void getSizeShouldReturnStoredValue() throws Exception{
        int value = 42;
        maxQuotaManager.setDefaultMaxStorage(value);

        given()
            .get(GlobalQuotaRoutes.SIZE_ENDPOINT)
        .then()
            .statusCode(200)
            .body(is(String.valueOf(value)));
    }

    @Test
    public void putSizeShouldRejectInvalid() throws Exception {
        given()
            .body("invalid")
            .put(GlobalQuotaRoutes.SIZE_ENDPOINT)
        .then()
            .statusCode(400);
    }

    @Test
    public void putSizeShouldRejectNegative() throws Exception {
        given()
            .body("-1")
            .put(GlobalQuotaRoutes.SIZE_ENDPOINT)
        .then()
            .statusCode(400);
    }

    @Test
    public void putSizeShouldAcceptValidValue() throws Exception {
        given()
            .body("42")
            .put(GlobalQuotaRoutes.SIZE_ENDPOINT)
        .then()
            .statusCode(204);

        assertThat(maxQuotaManager.getDefaultMaxStorage()).isEqualTo(42);
    }

    @Test
    public void deleteSizeShouldSetQuotaToUnlimited() throws Exception {
        maxQuotaManager.setDefaultMaxStorage(42);

        given()
            .delete(GlobalQuotaRoutes.COUNT_ENDPOINT)
        .then()
            .statusCode(204);

        assertThat(maxQuotaManager.getDefaultMaxMessage()).isEqualTo(Quota.UNLIMITED);
    }

    @Test
    public void getQuotaShouldReturnBothWhenValueSpecified() throws Exception {
        maxQuotaManager.setDefaultMaxStorage(42);
        maxQuotaManager.setDefaultMaxMessage(52);

        given()
            .get(GlobalQuotaRoutes.QUOTA_ENDPOINT)
        .then()
            .statusCode(200)
            .body(is("{\"count\":52,\"size\":42}"));
    }

    @Test
    public void getQuotaShouldReturnBothDefaultValues() throws Exception {
        given()
            .get(GlobalQuotaRoutes.QUOTA_ENDPOINT)
        .then()
            .statusCode(200)
            .body(is("{\"count\":-1,\"size\":-1}"));
    }

    @Test
    public void getQuotaShouldReturnBothWhenNoCount() throws Exception {
        maxQuotaManager.setDefaultMaxStorage(42);

        given()
            .get(GlobalQuotaRoutes.QUOTA_ENDPOINT)
        .then()
            .statusCode(200)
            .body(is("{\"count\":-1,\"size\":42}"));
    }

    @Test
    public void getQuotaShouldReturnBothWhenNoSize() throws Exception {
        maxQuotaManager.setDefaultMaxMessage(42);

        given()
            .get(GlobalQuotaRoutes.QUOTA_ENDPOINT)
        .then()
            .statusCode(200)
            .body(is("{\"count\":42,\"size\":-1}"));
    }

    @Test
    public void putQuotaShouldUpdateBothQuota() throws Exception {
        given()
            .body("{\"count\":52,\"size\":42}")
            .put(GlobalQuotaRoutes.QUOTA_ENDPOINT)
        .then()
            .statusCode(204);

        assertThat(maxQuotaManager.getDefaultMaxMessage()).isEqualTo(52);
        assertThat(maxQuotaManager.getDefaultMaxStorage()).isEqualTo(42);
    }

    @Test
    public void putQuotaShouldBaAbleToRemoveBothQuota() throws Exception {
        given()
            .body("{\"count\":-1,\"size\":-1}")
            .put(GlobalQuotaRoutes.QUOTA_ENDPOINT)
        .then()
            .statusCode(204);

        assertThat(maxQuotaManager.getDefaultMaxMessage()).isEqualTo(Quota.UNLIMITED);
        assertThat(maxQuotaManager.getDefaultMaxStorage()).isEqualTo(Quota.UNLIMITED);
    }

}
