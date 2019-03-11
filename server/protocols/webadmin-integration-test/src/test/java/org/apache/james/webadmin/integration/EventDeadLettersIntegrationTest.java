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

package org.apache.james.webadmin.integration;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static io.restassured.RestAssured.with;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.james.CassandraJmapTestRule;
import org.apache.james.DockerCassandraRule;
import org.apache.james.GuiceJamesServer;
import org.apache.james.mailbox.DefaultMailboxes;
import org.apache.james.mailbox.events.Event;
import org.apache.james.mailbox.events.Group;
import org.apache.james.mailbox.events.MailboxListener;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.probe.MailboxProbe;
import org.apache.james.modules.MailboxProbeImpl;
import org.apache.james.probe.DataProbe;
import org.apache.james.utils.DataProbeImpl;
import org.apache.james.utils.WebAdminGuiceProbe;
import org.apache.james.webadmin.WebAdminUtils;
import org.apache.james.webadmin.routes.EventDeadLettersRoutes;
import org.apache.james.webadmin.routes.TasksRoutes;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import com.google.inject.multibindings.Multibinder;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

public class EventDeadLettersIntegrationTest {

    public static class RetryEventsListenerGroup extends Group {}

    public class RetryEventsListener implements MailboxListener.GroupMailboxListener {
        final Group GROUP = new RetryEventsListenerGroup();

        private int retriesBeforeSuccess;
        private Map<Event.EventId, Integer> retries;
        private List<Event> successfulEvents;

        private void init() {
            this.retriesBeforeSuccess = 0;
            this.retries = new HashMap<>();
            this.successfulEvents = new ArrayList<>();
        }

        RetryEventsListener() {
            init();
        }

        @Override
        public Group getDefaultGroup() {
            return GROUP;
        }

        @Override
        public void event(Event event) throws Exception {
            int currentRetries = extractEventRetries(event).orElse(0);

            if (currentRetries < retriesBeforeSuccess) {
                retries.put(event.getEventId(), currentRetries + 1);
                throw new RuntimeException("throw to trigger retry");
            } else {
                retries.remove(event.getEventId());
                successfulEvents.add(event);
            }
        }

        private Optional<Integer> extractEventRetries(Event event) {
            if (retries.containsKey(event.getEventId())) {
                return Optional.of(retries.get(event.getEventId()));
            }
            return Optional.empty();
        }

        List<Event> getSuccessfulEvents() {
            return successfulEvents;
        }

        void setRetriesBeforeSuccess(int retriesBeforeSuccess) {
            this.retriesBeforeSuccess = retriesBeforeSuccess;
        }

        void clear() {
            init();
        }
    }

    private static final String DOMAIN = "domain.tld";
    private static final String BOB = "bob@" + DOMAIN;
    private static final String BOB_PASSWORD = "bobPassword";
    private static final String EVENTS_ACTION = "reDeliver";
    private static final String SERIALIZED_GROUP = new RetryEventsListenerGroup().asString();

    private static final MailboxPath MAILBOX_PATH = MailboxPath.forUser(BOB, DefaultMailboxes.INBOX);

    RetryEventsListener retryEventsListener = new RetryEventsListener();

    @ClassRule
    public static DockerCassandraRule cassandra = new DockerCassandraRule();

    @Rule
    public CassandraJmapTestRule cassandraJmapTestRule = CassandraJmapTestRule.defaultTestRule();

    private GuiceJamesServer guiceJamesServer;
    private MailboxProbe mailboxProbe;

    @Before
    public void setUp() throws Exception {
        retryEventsListener.clear();
        guiceJamesServer = cassandraJmapTestRule.jmapServer(cassandra.getModule())
            .overrideWith(new WebAdminConfigurationModule())
            .overrideWith(binder -> Multibinder.newSetBinder(binder, MailboxListener.GroupMailboxListener.class).addBinding().toInstance(retryEventsListener));
        guiceJamesServer.start();

        DataProbe dataProbe = guiceJamesServer.getProbe(DataProbeImpl.class);
        mailboxProbe = guiceJamesServer.getProbe(MailboxProbeImpl.class);
        WebAdminGuiceProbe webAdminGuiceProbe = guiceJamesServer.getProbe(WebAdminGuiceProbe.class);

        RestAssured.requestSpecification = WebAdminUtils.buildRequestSpecification(webAdminGuiceProbe.getWebAdminPort())
            .build();

        dataProbe.addDomain(DOMAIN);
        dataProbe.addUser(BOB, BOB_PASSWORD);
    }

    @After
    public void tearDown() {
        guiceJamesServer.stop();
    }

    private void generateInitialEvent() {
        mailboxProbe.createMailbox(MAILBOX_PATH.getNamespace(), MAILBOX_PATH.getUser(), MAILBOX_PATH.getName());
    }

    private String retrieveFailedEventId() {
        ArrayList<String> response = when()
            .get(EventDeadLettersRoutes.BASE_PATH + "/groups/" + SERIALIZED_GROUP)
        .then()
            .statusCode(HttpStatus.OK_200)
            .contentType(ContentType.JSON)
            .extract()
            .as(ArrayList.class);

        return response.get(0);
    }

    @Test
    public void failedEventShouldBeStoredInRetryListenerFailedEventsList() {
        retryEventsListener.setRetriesBeforeSuccess(4);
        generateInitialEvent();

        when()
            .get(EventDeadLettersRoutes.BASE_PATH + "/groups/" + SERIALIZED_GROUP)
        .then()
            .statusCode(HttpStatus.OK_200)
            .contentType(ContentType.JSON)
            .body(".", hasSize(1));
    }

    @Test
    public void successfulEventShouldNotBeStoredInRetryListenerFailedEventsList() {
        retryEventsListener.setRetriesBeforeSuccess(3);
        generateInitialEvent();

        when()
            .get(EventDeadLettersRoutes.BASE_PATH + "/groups")
        .then()
            .statusCode(HttpStatus.OK_200)
            .contentType(ContentType.JSON)
            .body(".", hasSize(0));
    }

    @Test
    public void failedEventGroupShouldBeStoredInDeadLetter() {
        retryEventsListener.setRetriesBeforeSuccess(4);
        generateInitialEvent();

        when()
            .get(EventDeadLettersRoutes.BASE_PATH + "/groups")
        .then()
            .statusCode(HttpStatus.OK_200)
            .contentType(ContentType.JSON)
            .body(".", containsInAnyOrder(SERIALIZED_GROUP));
    }

    @Test
    public void failedEventShouldBeStoredInDeadLetter() {
        retryEventsListener.setRetriesBeforeSuccess(4);
        generateInitialEvent();

        String failedEventId = retrieveFailedEventId();

        when()
            .get(EventDeadLettersRoutes.BASE_PATH + "/groups/" + SERIALIZED_GROUP + "/" + failedEventId)
        .then()
            .statusCode(HttpStatus.OK_200)
            .contentType(ContentType.JSON)
            .body(".", notNullValue());
    }

    @Test
    public void multipleFailedEventShouldBeStoredInDeadLetter() {
        retryEventsListener.setRetriesBeforeSuccess(4);
        generateInitialEvent();
        mailboxProbe.createMailbox("#bobox", BOB, DefaultMailboxes.INBOX);

        when()
            .get(EventDeadLettersRoutes.BASE_PATH + "/groups/" + SERIALIZED_GROUP)
        .then()
            .statusCode(HttpStatus.OK_200)
            .contentType(ContentType.JSON)
            .body(".", hasSize(2));
    }

    @Test
    public void failedEventShouldNotBeInDeadLetterAfterBeingDeleted() {
        retryEventsListener.setRetriesBeforeSuccess(4);
        generateInitialEvent();

        String failedEventId = retrieveFailedEventId();

        when()
            .delete(EventDeadLettersRoutes.BASE_PATH + "/groups/" + SERIALIZED_GROUP + "/" + failedEventId)
        .then()
            .statusCode(HttpStatus.NO_CONTENT_204);

        when()
            .get(EventDeadLettersRoutes.BASE_PATH + "/groups/" + SERIALIZED_GROUP + "/" + failedEventId)
        .then()
            .statusCode(HttpStatus.NOT_FOUND_404);
    }

    @Test
    public void failedEventShouldNotBeInDeadLettersAfterSuccessfulRedelivery() {
        retryEventsListener.setRetriesBeforeSuccess(4);
        generateInitialEvent();

        String failedEventId = retrieveFailedEventId();

        String taskId = with()
            .queryParam("action", EVENTS_ACTION)
        .post(EventDeadLettersRoutes.BASE_PATH + "/groups/" + SERIALIZED_GROUP + "/" + failedEventId)
            .jsonPath()
            .get("taskId");

        given()
            .basePath(TasksRoutes.BASE)
        .when()
            .get(taskId + "/await")
        .then()
            .body("status", is("completed"))
            .body("additionalInformation.successfulRedeliveriesCount", is(1))
            .body("additionalInformation.failedRedeliveriesCount", is(0))
            .body("additionalInformation.group", is(SERIALIZED_GROUP))
            .body("additionalInformation.eventId", is(failedEventId));

        when()
            .get("/events/deadLetter/groups/" + SERIALIZED_GROUP + "/" + failedEventId)
        .then()
            .statusCode(HttpStatus.NOT_FOUND_404);
    }

    @Test
    public void failedEventShouldBeCorrectlyProcessedByListenerAfterSuccessfulRedelivery() {
        retryEventsListener.setRetriesBeforeSuccess(5);
        generateInitialEvent();

        String failedEventId = retrieveFailedEventId();

        String taskId = with()
            .queryParam("action", EVENTS_ACTION)
        .post(EventDeadLettersRoutes.BASE_PATH + "/groups/" + SERIALIZED_GROUP + "/" + failedEventId)
            .jsonPath()
            .get("taskId");

        given()
            .basePath(TasksRoutes.BASE)
        .when()
            .get(taskId + "/await")
        .then()
            .body("status", is("completed"))
            .body("additionalInformation.successfulRedeliveriesCount", is(1))
            .body("additionalInformation.failedRedeliveriesCount", is(0))
            .body("additionalInformation.group", is(SERIALIZED_GROUP))
            .body("additionalInformation.eventId", is(failedEventId));

        assertThat(retryEventsListener.getSuccessfulEvents()).hasSize(1);
    }

    @Test
    public void multipleFailedEventsShouldNotBeInDeadLettersAfterSuccessfulGroupRedelivery() {
        retryEventsListener.setRetriesBeforeSuccess(4);
        generateInitialEvent();
        mailboxProbe.createMailbox("#bobox", BOB, DefaultMailboxes.INBOX);

        String taskId = with()
            .queryParam("action", EVENTS_ACTION)
        .post(EventDeadLettersRoutes.BASE_PATH + "/groups/" + SERIALIZED_GROUP)
            .jsonPath()
            .get("taskId");

        given()
            .basePath(TasksRoutes.BASE)
        .when()
            .get(taskId + "/await")
        .then()
            .body("status", is("completed"))
            .body("additionalInformation.successfulRedeliveriesCount", is(2))
            .body("additionalInformation.failedRedeliveriesCount", is(0))
            .body("additionalInformation.group", is(SERIALIZED_GROUP));

        when()
            .get(EventDeadLettersRoutes.BASE_PATH + "/groups/" + SERIALIZED_GROUP)
        .then()
            .statusCode(HttpStatus.OK_200)
            .contentType(ContentType.JSON)
            .body(".", hasSize(0));
    }

    @Test
    public void multipleFailedEventsShouldBeCorrectlyProcessedByListenerAfterSuccessfulGroupRedelivery() {
        retryEventsListener.setRetriesBeforeSuccess(5);
        generateInitialEvent();
        mailboxProbe.createMailbox("#bobox", BOB, DefaultMailboxes.INBOX);

        String taskId = with()
            .queryParam("action", EVENTS_ACTION)
        .post(EventDeadLettersRoutes.BASE_PATH + "/groups/" + SERIALIZED_GROUP)
            .jsonPath()
            .get("taskId");

        given()
            .basePath(TasksRoutes.BASE)
        .when()
            .get(taskId + "/await")
        .then()
            .body("status", is("completed"))
            .body("additionalInformation.successfulRedeliveriesCount", is(2))
            .body("additionalInformation.failedRedeliveriesCount", is(0))
            .body("additionalInformation.group", is(SERIALIZED_GROUP));

        assertThat(retryEventsListener.getSuccessfulEvents()).hasSize(2);
    }

    @Test
    public void multipleFailedEventsShouldNotBeInDeadLettersAfterSuccessfulAllRedelivery() {
        retryEventsListener.setRetriesBeforeSuccess(4);
        generateInitialEvent();
        mailboxProbe.createMailbox("#bobox", BOB, DefaultMailboxes.INBOX);

        String taskId = with()
            .queryParam("action", EVENTS_ACTION)
        .post(EventDeadLettersRoutes.BASE_PATH)
            .jsonPath()
            .get("taskId");

        given()
            .basePath(TasksRoutes.BASE)
        .when()
            .get(taskId + "/await")
        .then()
            .body("status", is("completed"))
            .body("additionalInformation.successfulRedeliveriesCount", is(2))
            .body("additionalInformation.failedRedeliveriesCount", is(0));

        when()
            .get(EventDeadLettersRoutes.BASE_PATH + "/groups/" + SERIALIZED_GROUP)
        .then()
            .statusCode(HttpStatus.OK_200)
            .contentType(ContentType.JSON)
            .body(".", hasSize(0));
    }

    @Test
    public void multipleFailedEventsShouldBeCorrectlyProcessedByListenerAfterSuccessfulAllRedelivery() {
        retryEventsListener.setRetriesBeforeSuccess(5);
        generateInitialEvent();
        mailboxProbe.createMailbox("#bobox", BOB, DefaultMailboxes.INBOX);

        String taskId = with()
            .queryParam("action", EVENTS_ACTION)
        .post(EventDeadLettersRoutes.BASE_PATH)
            .jsonPath()
            .get("taskId");

        given()
            .basePath(TasksRoutes.BASE)
        .when()
            .get(taskId + "/await")
        .then()
            .body("status", is("completed"))
            .body("additionalInformation.successfulRedeliveriesCount", is(2))
            .body("additionalInformation.failedRedeliveriesCount", is(0));

        assertThat(retryEventsListener.getSuccessfulEvents()).hasSize(2);
    }

    @Test
    @Ignore("JAMES-2666 not working yet, need to fix the redeliver task")
    public void failedEventShouldStillBeInDeadLettersAfterFailedRedelivery() {
        retryEventsListener.setRetriesBeforeSuccess(8);
        generateInitialEvent();

        String failedEventId = retrieveFailedEventId();

        String taskId = with()
            .queryParam("action", EVENTS_ACTION)
        .post(EventDeadLettersRoutes.BASE_PATH + "/groups/" + SERIALIZED_GROUP + "/" + failedEventId)
            .jsonPath()
            .get("taskId");

        given()
            .basePath(TasksRoutes.BASE)
        .when()
            .get(taskId + "/await")
        .then()
            .body("status", is("failed"))
            .body("additionalInformation.successfulRedeliveriesCount", is(0))
            .body("additionalInformation.failedRedeliveriesCount", is(1))
            .body("additionalInformation.group", is(SERIALIZED_GROUP))
            .body("additionalInformation.eventId", is(failedEventId));

        when()
            .get(EventDeadLettersRoutes.BASE_PATH + "/groups/" + SERIALIZED_GROUP + "/" + failedEventId)
        .then()
            .statusCode(HttpStatus.OK_200)
            .contentType(ContentType.JSON)
            .body(".", notNullValue());
    }
}
