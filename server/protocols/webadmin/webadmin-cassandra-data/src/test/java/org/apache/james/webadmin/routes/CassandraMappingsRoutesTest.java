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

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static io.restassured.RestAssured.with;
import static org.apache.james.webadmin.WebAdminServer.NO_CONFIGURATION;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.james.metrics.logger.DefaultMetricFactory;
import org.apache.james.rrt.cassandra.CassandraMappingsSourcesDAO;
import org.apache.james.rrt.cassandra.migration.MappingsSourcesMigration;
import org.apache.james.task.MemoryTaskManager;
import org.apache.james.task.Task;
import org.apache.james.webadmin.WebAdminServer;
import org.apache.james.webadmin.WebAdminUtils;
import org.apache.james.webadmin.service.CassandraMappingsService;
import org.apache.james.webadmin.service.CassandraMappingsSolveInconsistenciesTask;
import org.apache.james.webadmin.utils.ErrorResponder;
import org.apache.james.webadmin.utils.JsonTransformer;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.restassured.RestAssured;
import reactor.core.publisher.Mono;

public class CassandraMappingsRoutesTest {

    private WebAdminServer webAdminServer;
    private MappingsSourcesMigration mappingsSourcesMigration;
    private CassandraMappingsSourcesDAO cassandraMappingsSourcesDAO;
    private MemoryTaskManager taskManager;

    private static final String MAPPINGS_ACTION = "solveInconsistencies";

    @Before
    public void setUp() throws Exception {
        cassandraMappingsSourcesDAO = mock(CassandraMappingsSourcesDAO.class);
        mappingsSourcesMigration = mock(MappingsSourcesMigration.class);

        CassandraMappingsService cassandraMappingsService = new CassandraMappingsService(mappingsSourcesMigration, cassandraMappingsSourcesDAO);

        when(cassandraMappingsSourcesDAO.truncateTable()).thenReturn(Mono.empty());
        when(mappingsSourcesMigration.run()).thenReturn(Task.Result.COMPLETED);

        JsonTransformer jsonTransformer = new JsonTransformer();
        taskManager = new MemoryTaskManager();
        webAdminServer = WebAdminUtils.createWebAdminServer(
            new DefaultMetricFactory(),
            new CassandraMappingsRoutes(cassandraMappingsService, taskManager, jsonTransformer),
            new TasksRoutes(taskManager, jsonTransformer));

        webAdminServer.configure(NO_CONFIGURATION);
        webAdminServer.await();

        RestAssured.requestSpecification = WebAdminUtils.buildRequestSpecification(webAdminServer)
            .setBasePath(CassandraMappingsRoutes.ROOT_PATH)
            .build();
    }

    @After
    public void tearDown() {
        webAdminServer.destroy();
        taskManager.stop();
    }

    @Test
    public void postMappingsActionWithSolvedInconsistenciesQueryParamShouldCreateATask() {
        given()
            .queryParam("action", MAPPINGS_ACTION)
        .when()
            .post()
        .then()
            .statusCode(HttpStatus.CREATED_201)
            .header("Location", is(notNullValue()))
            .body("taskId", is(notNullValue()));
    }

    @Test
    public void postMappingsActionWithSolvedInconsistenciesQueryParamShouldHaveSuccessfulCompletedTask() {
        String taskId = with()
            .queryParam("action", MAPPINGS_ACTION)
            .post()
            .jsonPath()
            .get("taskId");

        given()
            .basePath(TasksRoutes.BASE)
        .when()
            .get(taskId + "/await")
        .then()
            .body("status", is("completed"))
            .body("taskId", is(notNullValue()))
            .body("type", is(CassandraMappingsSolveInconsistenciesTask.TYPE))
            .body("startedDate", is(notNullValue()))
            .body("submitDate", is(notNullValue()))
            .body("completedDate", is(notNullValue()));
    }

    @Test
    public void postMappingsActionShouldRejectInvalidActions() {
        given()
            .queryParam("action", "invalid-action")
        .when()
            .post()
        .then()
            .statusCode(HttpStatus.BAD_REQUEST_400)
            .body("statusCode", is(400))
            .body("type", is(ErrorResponder.ErrorType.INVALID_ARGUMENT.getType()))
            .body("message", is(CassandraMappingsRoutes.INVALID_ACTION_ARGUMENT_REQUEST));
    }

    @Test
    public void postMappingsActionShouldRequireAction() {
        when()
            .post()
        .then()
            .statusCode(HttpStatus.BAD_REQUEST_400)
            .body("statusCode", is(400))
            .body("type", is(ErrorResponder.ErrorType.INVALID_ARGUMENT.getType()))
            .body("message", is(CassandraMappingsRoutes.INVALID_ACTION_ARGUMENT_REQUEST));
    }

    @Test
    public void postMappingsActionTaskShouldFailIfSomethingGoesWrong() {
        when(cassandraMappingsSourcesDAO.truncateTable()).thenThrow(new RuntimeException());

        String taskId = with()
            .queryParam("action", MAPPINGS_ACTION)
            .post()
            .jsonPath()
            .get("taskId");

        given()
            .basePath(TasksRoutes.BASE)
        .when()
            .get(taskId + "/await")
        .then()
            .body("status", is("failed"));
    }
}
