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

package org.apache.james.webadmin.integration.postgres;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.with;
import static org.apache.james.data.UsersRepositoryModuleChooser.Implementation.DEFAULT;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.is;

import java.nio.charset.StandardCharsets;
import java.util.stream.IntStream;

import org.apache.james.GuiceJamesServer;
import org.apache.james.JamesServerBuilder;
import org.apache.james.JamesServerExtension;
import org.apache.james.PostgresJamesConfiguration;
import org.apache.james.PostgresJamesServerMain;
import org.apache.james.SearchConfiguration;
import org.apache.james.backends.postgres.PostgresExtension;
import org.apache.james.core.Username;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.modules.MailboxProbeImpl;
import org.apache.james.modules.TestJMAPServerModule;
import org.apache.james.probe.DataProbe;
import org.apache.james.utils.DataProbeImpl;
import org.apache.james.utils.WebAdminGuiceProbe;
import org.apache.james.webadmin.WebAdminUtils;
import org.apache.james.webadmin.routes.TasksRoutes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.github.fge.lambdas.Throwing;

import io.restassured.RestAssured;

class PostgresPopulateEmailQueryViewTaskIntegrationTest {
    @RegisterExtension
    static JamesServerExtension jamesServerExtension = new JamesServerBuilder<PostgresJamesConfiguration>(tmpDir ->
        PostgresJamesConfiguration.builder()
            .workingDirectory(tmpDir)
            .configurationFromClasspath()
            .searchConfiguration(SearchConfiguration.scanning())
            .usersRepository(DEFAULT)
            .eventBusImpl(PostgresJamesConfiguration.EventBusImpl.IN_MEMORY)
            .build())
        .extension(PostgresExtension.empty())
        .server(configuration -> PostgresJamesServerMain.createServer(configuration)
            .overrideWith(new TestJMAPServerModule()))
        .build();

    private static final String DOMAIN = "domain.tld";
    private static final Username BOB = Username.of("bob@" + DOMAIN);
    private static final String PASSWORD = "password";
    private static final String MAILBOX_NAME = "mailbox";
    private static final MailboxPath BOB_INBOX_PATH = MailboxPath.inbox(Username.of(BOB.asString()));
    private static final Username ALICE = Username.of("alice@" + DOMAIN);
    private static final MailboxPath ALICE_INBOX_PATH = MailboxPath.inbox(Username.of(ALICE.asString()));
    private static final Username CEDRIC = Username.of("cedric@" + DOMAIN);
    private static final MailboxPath CEDRIC_INBOX_PATH = MailboxPath.inbox(Username.of(CEDRIC.asString()));

    private MailboxProbeImpl mailboxProbe;

    @BeforeEach
    void setUp(GuiceJamesServer guiceJamesServer) throws Exception {
        DataProbe dataProbe = guiceJamesServer.getProbe(DataProbeImpl.class);
        mailboxProbe = guiceJamesServer.getProbe(MailboxProbeImpl.class);
        WebAdminGuiceProbe webAdminGuiceProbe = guiceJamesServer.getProbe(WebAdminGuiceProbe.class);

        RestAssured.requestSpecification = WebAdminUtils.buildRequestSpecification(webAdminGuiceProbe.getWebAdminPort())
            .build();

        dataProbe.addDomain(DOMAIN);
        dataProbe.addUser(BOB.asString(), PASSWORD);
        dataProbe.addUser(ALICE.asString(), PASSWORD);
        dataProbe.addUser(CEDRIC.asString(), PASSWORD);

        mailboxProbe.createMailbox(BOB_INBOX_PATH);
        addMessagesToMailbox(BOB, BOB_INBOX_PATH);

        mailboxProbe.createMailbox(ALICE_INBOX_PATH);
        addMessagesToMailbox(ALICE, ALICE_INBOX_PATH);

        mailboxProbe.createMailbox(CEDRIC_INBOX_PATH);
        addMessagesToMailbox(CEDRIC, CEDRIC_INBOX_PATH);

        IntStream.rangeClosed(1, 15)
            .forEach(mbxCounter -> {
                MailboxPath bobMailboxPath = MailboxPath.forUser(BOB, MAILBOX_NAME + mbxCounter);
                mailboxProbe.createMailbox(bobMailboxPath);
                addMessagesToMailbox(BOB, bobMailboxPath);

                MailboxPath aliceMailboxPath = MailboxPath.forUser(ALICE, MAILBOX_NAME + mbxCounter);
                mailboxProbe.createMailbox(aliceMailboxPath);
                addMessagesToMailbox(ALICE, aliceMailboxPath);

                MailboxPath cedricMailboxPath = MailboxPath.forUser(CEDRIC, MAILBOX_NAME + mbxCounter);
                mailboxProbe.createMailbox(cedricMailboxPath);
                addMessagesToMailbox(CEDRIC, cedricMailboxPath);
            });
    }

    private void addMessagesToMailbox(Username username, MailboxPath mailbox) {
        IntStream.rangeClosed(1, 50)
            .forEach(Throwing.intConsumer(ignored ->
                mailboxProbe.appendMessage(username.asString(), mailbox,
                    MessageManager.AppendCommand.builder()
                        .build(Message.Builder.of()
                            .setSubject("small message")
                            .setBody("small message for postgres", StandardCharsets.UTF_8)
                            .build()))));
    }

    @Test
    void populateEmailQueryViewTaskShouldWork() {
        String taskId = with()
            .post("/mailboxes?task=populateEmailQueryView")
            .jsonPath()
            .get("taskId");

        given()
            .basePath(TasksRoutes.BASE)
        .when()
            .get(taskId + "/await")
        .then()
            .body("status", is("completed"))
            .body("taskId", is(notNullValue()))
            .body("type", is("PopulateEmailQueryViewTask"))
            .body("additionalInformation.processedUserCount", is(3))
            .body("additionalInformation.processedMessageCount", is(2400))
            .body("additionalInformation.failedUserCount", is(0))
            .body("additionalInformation.failedMessageCount", is(0));
    }
}
