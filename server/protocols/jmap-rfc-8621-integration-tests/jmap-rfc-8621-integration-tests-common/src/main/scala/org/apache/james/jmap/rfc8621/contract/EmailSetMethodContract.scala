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
package org.apache.james.jmap.rfc8621.contract

import java.nio.charset.StandardCharsets

import io.netty.handler.codec.http.HttpHeaderNames.ACCEPT
import io.restassured.RestAssured.{`given`, requestSpecification}
import io.restassured.http.ContentType.JSON
import javax.mail.Flags
import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import org.apache.http.HttpStatus.SC_OK
import org.apache.james.GuiceJamesServer
import org.apache.james.jmap.draft.JmapGuiceProbe
import org.apache.james.jmap.http.UserCredential
import org.apache.james.jmap.rfc8621.contract.Fixture.{ACCEPT_RFC8621_VERSION_HEADER, ACCOUNT_ID, ANDRE, ANDRE_PASSWORD, BOB, BOB_PASSWORD, DOMAIN, authScheme, baseRequestSpecBuilder}
import org.apache.james.mailbox.MessageManager.AppendCommand
import org.apache.james.mailbox.model.MailboxACL.{NO_RIGHTS, Right}
import org.apache.james.mailbox.model.{MailboxACL, MailboxId, MailboxPath, MessageId}
import org.apache.james.mime4j.dom.Message
import org.apache.james.modules.{ACLProbeImpl, MailboxProbeImpl}
import org.apache.james.utils.DataProbeImpl
import org.junit.jupiter.api.{BeforeEach, Test}

import scala.jdk.CollectionConverters._

trait EmailSetMethodContract {
  @BeforeEach
  def setUp(server: GuiceJamesServer): Unit = {
    server.getProbe(classOf[DataProbeImpl])
      .fluent
      .addDomain(DOMAIN.asString)
      .addUser(BOB.asString, BOB_PASSWORD)
      .addUser(ANDRE.asString, ANDRE_PASSWORD)

    requestSpecification = baseRequestSpecBuilder(server)
      .setAuth(authScheme(UserCredential(BOB, BOB_PASSWORD)))
      .build
  }

  def randomMessageId: MessageId

  @Test
  def shouldAddKeywords(server: GuiceJamesServer): Unit = {
    val message: Message = Fixture.createTestMessage

    val flags: Flags = new Flags(Flags.Flag.ANSWERED)

    val bobPath = MailboxPath.inbox(BOB)
    server.getProbe(classOf[MailboxProbeImpl]).createMailbox(bobPath)
    val messageId: MessageId = server.getProbe(classOf[MailboxProbeImpl]).appendMessage(BOB.asString(), bobPath, AppendCommand.builder()
      .withFlags(flags)
      .build(message))
      .getMessageId

    val request =
      s"""{
         |  "using": ["urn:ietf:params:jmap:core", "urn:ietf:params:jmap:mail"],
         |  "methodCalls": [
         |    ["Email/set", {
         |      "accountId": "$ACCOUNT_ID",
         |      "update": {
         |        "${messageId.serialize}":{
         |          "keywords": {
         |             "music": true
         |          }
         |        }
         |      }
         |    }, "c1"],
         |    ["Email/get",
         |     {
         |       "accountId": "$ACCOUNT_ID",
         |       "ids": ["${messageId.serialize}"],
         |       "properties": ["keywords"]
         |     },
         |     "c2"]]
         |}""".stripMargin

    val response = `given`
      .header(ACCEPT.toString, ACCEPT_RFC8621_VERSION_HEADER)
      .body(request)
    .when
      .post
    .`then`
      .statusCode(SC_OK)
      .contentType(JSON)
      .extract
      .body
      .asString

    assertThatJson(response)
      .inPath("methodResponses[1][1].list[0]")
      .isEqualTo(String.format(
        """{
          |   "id":"%s",
          |   "keywords": {
          |     "music": true
          |   }
          |}
      """.stripMargin, messageId.serialize))
  }

  @Test
  def shouldNotAddKeywordWhenFalseValue(server: GuiceJamesServer): Unit = {
    val message: Message = Fixture.createTestMessage

    val flags: Flags = new Flags(Flags.Flag.ANSWERED)

    val bobPath = MailboxPath.inbox(BOB)
    server.getProbe(classOf[MailboxProbeImpl]).createMailbox(bobPath)
    val messageId: MessageId = server.getProbe(classOf[MailboxProbeImpl]).appendMessage(BOB.asString(), bobPath, AppendCommand.builder()
      .withFlags(flags)
      .build(message))
      .getMessageId

    val request =
      s"""{
         |  "using": ["urn:ietf:params:jmap:core", "urn:ietf:params:jmap:mail"],
         |  "methodCalls": [
         |    ["Email/set", {
         |      "accountId": "$ACCOUNT_ID",
         |      "update": {
         |        "${messageId.serialize}":{
         |          "keywords": {
         |             "music": true,
         |             "movie": false
         |          }
         |        }
         |      }
         |    }, "c1"],
         |    ["Email/get",
         |     {
         |       "accountId": "$ACCOUNT_ID",
         |       "ids": ["${messageId.serialize}"],
         |       "properties": ["keywords"]
         |     },
         |     "c2"]]
         |}""".stripMargin

    val response = `given`
      .header(ACCEPT.toString, ACCEPT_RFC8621_VERSION_HEADER)
      .body(request)
    .when
      .post
    .`then`
      .statusCode(SC_OK)
      .contentType(JSON)
      .extract
      .body
      .asString

    assertThatJson(response)
      .inPath("methodResponses[1][1].list[0]")
      .isEqualTo(String.format(
        """{
          |   "id":"%s",
          |   "keywords": {
          |     "$Answered": true
          |   }
          |}
      """.stripMargin, messageId.serialize))
  }

  @Test
  def shouldNotAddKeywordWhenInvalidKeyword(server: GuiceJamesServer): Unit = {
    val message: Message = Fixture.createTestMessage

    val flags: Flags = new Flags(Flags.Flag.ANSWERED)

    val bobPath = MailboxPath.inbox(BOB)
    server.getProbe(classOf[MailboxProbeImpl]).createMailbox(bobPath)
    val messageId: MessageId = server.getProbe(classOf[MailboxProbeImpl]).appendMessage(BOB.asString(), bobPath, AppendCommand.builder()
      .withFlags(flags)
      .build(message))
      .getMessageId

    val request =
      s"""{
         |  "using": ["urn:ietf:params:jmap:core", "urn:ietf:params:jmap:mail"],
         |  "methodCalls": [
         |    ["Email/set", {
         |      "accountId": "$ACCOUNT_ID",
         |      "update": {
         |        "${messageId.serialize}":{
         |          "keywords": {
         |             "mus*c": true
         |          }
         |        }
         |      }
         |    }, "c1"],
         |    ["Email/get",
         |     {
         |       "accountId": "$ACCOUNT_ID",
         |       "ids": ["${messageId.serialize}"],
         |       "properties": ["keywords"]
         |     },
         |     "c2"]]
         |}""".stripMargin

    val response = `given`
      .header(ACCEPT.toString, ACCEPT_RFC8621_VERSION_HEADER)
      .body(request)
    .when
      .post
    .`then`
      .statusCode(SC_OK)
      .contentType(JSON)
      .extract
      .body
      .asString

    assertThatJson(response)
      .inPath("methodResponses[1][1].list[0]")
      .isEqualTo(String.format(
        """{
          |   "id":"%s",
          |   "keywords": {
          |     "$Answered": true
          |   }
          |}
      """.stripMargin, messageId.serialize))
  }

  @Test
  def shouldNotAddKeywordWhenMessageIdNonExisted(server: GuiceJamesServer): Unit = {
    val message: Message = Fixture.createTestMessage

    val flags: Flags = new Flags(Flags.Flag.ANSWERED)

    val bobPath = MailboxPath.inbox(BOB)
    server.getProbe(classOf[MailboxProbeImpl]).createMailbox(bobPath)
    val messageId: MessageId = server.getProbe(classOf[MailboxProbeImpl]).appendMessage(BOB.asString(), bobPath, AppendCommand.builder()
      .withFlags(flags)
      .build(message))
      .getMessageId

    val request =
      s"""{
         |  "using": ["urn:ietf:params:jmap:core", "urn:ietf:params:jmap:mail"],
         |  "methodCalls": [
         |    ["Email/set", {
         |      "accountId": "$ACCOUNT_ID",
         |      "update": {
         |        "${messageId.serialize}":{
         |          "keywords": {
         |             "music": true,
         |             "movie": false
         |          }
         |        }
         |      }
         |    }, "c1"],
         |    ["Email/get",
         |     {
         |       "accountId": "$ACCOUNT_ID",
         |       "ids": ["${messageId.serialize}"],
         |       "properties": ["keywords"]
         |     },
         |     "c2"]]
         |}""".stripMargin

    val response = `given`
      .header(ACCEPT.toString, ACCEPT_RFC8621_VERSION_HEADER)
      .body(request)
    .when
      .post
    .`then`
      .statusCode(SC_OK)
      .contentType(JSON)
      .extract
      .body
      .asString

    assertThatJson(response)
      .inPath("methodResponses[1][1].list[0]")
      .isEqualTo(String.format(
        """{
          |   "id":"%s",
          |   "keywords": {
          |     "$Answered": true
          |   }
          |}
      """.stripMargin, messageId.serialize))
  }

  @Test
  def shouldNotAddKeywordWhenItDefaultLikeAnswered(server: GuiceJamesServer): Unit = {
    val message: Message = Fixture.createTestMessage

    val flags: Flags = new Flags(Flags.Flag.ANSWERED)

    val bobPath = MailboxPath.inbox(BOB)
    server.getProbe(classOf[MailboxProbeImpl]).createMailbox(bobPath)
    val messageId: MessageId = server.getProbe(classOf[MailboxProbeImpl]).appendMessage(BOB.asString(), bobPath, AppendCommand.builder()
      .withFlags(flags)
      .build(message))
      .getMessageId

    val request =
      s"""{
         |  "using": ["urn:ietf:params:jmap:core", "urn:ietf:params:jmap:mail"],
         |  "methodCalls": [
         |    ["Email/set", {
         |      "accountId": "$ACCOUNT_ID",
         |      "update": {
         |        "${messageId.serialize}":{
         |          "keywords": {
         |             "music": true,
         |             "movie": false
         |          }
         |        }
         |      }
         |    }, "c1"],
         |    ["Email/get",
         |     {
         |       "accountId": "$ACCOUNT_ID",
         |       "ids": ["${messageId.serialize}"],
         |       "properties": ["keywords"]
         |     },
         |     "c2"]]
         |}""".stripMargin

    val response = `given`
      .header(ACCEPT.toString, ACCEPT_RFC8621_VERSION_HEADER)
      .body(request)
    .when
      .post
    .`then`
      .statusCode(SC_OK)
      .contentType(JSON)
      .extract
      .body
      .asString

    assertThatJson(response)
      .inPath("methodResponses[1][1].list[0]")
      .isEqualTo(String.format(
        """{
          |   "id":"%s",
          |   "keywords": {
          |     "$Answered": true
          |   }
          |}
      """.stripMargin, messageId.serialize))
  }

  @Test
  def shouldNotAddKeywordWhenInvalidMessageId(server: GuiceJamesServer): Unit = {
    val message: Message = Fixture.createTestMessage

    val flags: Flags = new Flags(Flags.Flag.ANSWERED)

    val bobPath = MailboxPath.inbox(BOB)
    server.getProbe(classOf[MailboxProbeImpl]).createMailbox(bobPath)
    val messageId: MessageId = server.getProbe(classOf[MailboxProbeImpl]).appendMessage(BOB.asString(), bobPath, AppendCommand.builder()
      .withFlags(flags)
      .build(message))
      .getMessageId

    val request =
      s"""{
         |  "using": ["urn:ietf:params:jmap:core", "urn:ietf:params:jmap:mail"],
         |  "methodCalls": [
         |    ["Email/set", {
         |      "accountId": "$ACCOUNT_ID",
         |      "update": {
         |        "${messageId.serialize}1":{
         |          "keywords": {
         |             "music": true,
         |             "movie": false
         |          }
         |        }
         |      }
         |    }, "c1"],
         |    ["Email/get",
         |     {
         |       "accountId": "$ACCOUNT_ID",
         |       "ids": ["${messageId.serialize}"],
         |       "properties": ["keywords"]
         |     },
         |     "c2"]]
         |}""".stripMargin

    val response = `given`
      .header(ACCEPT.toString, ACCEPT_RFC8621_VERSION_HEADER)
      .body(request)
    .when
      .post
    .`then`
      .statusCode(SC_OK)
      .contentType(JSON)
      .extract
      .body
      .asString

    assertThatJson(response)
      .inPath("methodResponses[1][1].list[0]")
      .isEqualTo(String.format(
        """{
          |   "id":"%s",
          |   "keywords": {
          |     "$Answered": true
          |   }
          |}
      """.stripMargin, messageId.serialize))
  }

  @Test
  def shouldNotAddKeywordWhenItNonExposed(server: GuiceJamesServer): Unit = {
    val message: Message = Fixture.createTestMessage

    val flags: Flags = new Flags(Flags.Flag.ANSWERED)

    val bobPath = MailboxPath.inbox(BOB)
    server.getProbe(classOf[MailboxProbeImpl]).createMailbox(bobPath)
    val messageId: MessageId = server.getProbe(classOf[MailboxProbeImpl]).appendMessage(BOB.asString(), bobPath, AppendCommand.builder()
      .withFlags(flags)
      .build(message))
      .getMessageId

    val request =
      String.format(s"""{
         |  "using": ["urn:ietf:params:jmap:core", "urn:ietf:params:jmap:mail"],
         |  "methodCalls": [
         |    ["Email/set", {
         |      "accountId": "$ACCOUNT_ID",
         |      "update": {
         |        "${messageId.serialize}":{
         |          "keywords": {
         |             "%s": true,
         |             "%s": true
         |          }
         |        }
         |      }
         |    }, "c1"],
         |    ["Email/get",
         |     {
         |       "accountId": "$ACCOUNT_ID",
         |       "ids": ["${messageId.serialize}"],
         |       "properties": ["keywords"]
         |     },
         |     "c2"]]
         |}""".stripMargin, "$Recent", "$Deleted")

    val response = `given`
      .header(ACCEPT.toString, ACCEPT_RFC8621_VERSION_HEADER)
      .body(request)
    .when
      .post
    .`then`
      .statusCode(SC_OK)
      .contentType(JSON)
      .extract
      .body
      .asString

    assertThatJson(response)
      .inPath("methodResponses[1][1].list[0]")
      .isEqualTo(String.format(
        """{
          |   "id":"%s",
          |   "keywords": {}
          |}
      """.stripMargin, messageId.serialize))
  }

  @Test
  def shouldNotUpdateInDelegatedMailboxesWhenReadOnly(server: GuiceJamesServer): Unit = {
    val andreMailbox: String = "andrecustom"
    val andrePath = MailboxPath.forUser(ANDRE, andreMailbox)
    server.getProbe(classOf[MailboxProbeImpl]).createMailbox(andrePath)
    val message: Message = Message.Builder
      .of
      .setSender(BOB.asString())
      .setFrom(ANDRE.asString())
      .setSubject("test")
      .setBody("testmail", StandardCharsets.UTF_8)
      .build
    val messageId: MessageId = server.getProbe(classOf[MailboxProbeImpl])
      .appendMessage(ANDRE.asString, andrePath, AppendCommand.from(message))
      .getMessageId
    server.getProbe(classOf[ACLProbeImpl])
      .replaceRights(andrePath, BOB.asString, MailboxACL.Rfc4314Rights.of(Set(Right.Read).asJava))

    val request =
      s"""{
         |  "using": [
         |    "urn:ietf:params:jmap:core",
         |    "urn:ietf:params:jmap:mail"],
         |  "methodCalls": [
         |  ["Email/set",
         |    {
         |      "accountId": "$ACCOUNT_ID",
         |      "ids": ["${messageId.serialize}"],
         |      "update": {
         |        "${messageId.serialize}":{
         |          "keywords": {
         |             "music": true
         |          }
         |        }
         |      }
         |    },
         |    "c1"],
         |    ["Email/get",
         |     {
         |       "accountId": "$ACCOUNT_ID",
         |       "ids": ["${messageId.serialize}"],
         |       "properties": ["keywords"]
         |     },
         |     "c2"]]
         |}""".stripMargin

    val response = `given`
      .header(ACCEPT.toString, ACCEPT_RFC8621_VERSION_HEADER)
      .body(request)
    .when
      .post
    .`then`
      .statusCode(SC_OK)
      .contentType(JSON)
      .extract
      .body
      .asString

    assertThatJson(response)
      .inPath("methodResponses[1][1].list[0]")
      .isEqualTo(String.format(
        """{
          |   "id":"%s",
          |   "keywords": {}
          |}
      """.stripMargin, messageId.serialize))
  }

  @Test
  def shouldNotUpdateInDelegatedMailboxesWhenNoRight(server: GuiceJamesServer): Unit = {
    val andreMailbox: String = "andrecustom"
    val andrePath = MailboxPath.forUser(ANDRE, andreMailbox)
    server.getProbe(classOf[MailboxProbeImpl]).createMailbox(andrePath)
    val message: Message = Message.Builder
      .of
      .setSender(BOB.asString())
      .setFrom(ANDRE.asString())
      .setSubject("test")
      .setBody("testmail", StandardCharsets.UTF_8)
      .build
    val messageId: MessageId = server.getProbe(classOf[MailboxProbeImpl])
      .appendMessage(ANDRE.asString, andrePath, AppendCommand.from(message))
      .getMessageId
    server.getProbe(classOf[ACLProbeImpl])
      .replaceRights(andrePath, BOB.asString, NO_RIGHTS)

    val request =
      s"""{
         |  "using": [
         |    "urn:ietf:params:jmap:core",
         |    "urn:ietf:params:jmap:mail"],
         |  "methodCalls": [
         |  ["Email/set",
         |    {
         |      "accountId": "$ACCOUNT_ID",
         |      "ids": ["${messageId.serialize}"],
         |      "update": {
         |        "${messageId.serialize}":{
         |          "keywords": {
         |             "music": true
         |          }
         |        }
         |      }
         |    },
         |    "c1"],
         |    ["Email/get",
         |     {
         |       "accountId": "$ACCOUNT_ID",
         |       "ids": ["${messageId.serialize}"],
         |       "properties": ["keywords"]
         |     },
         |     "c2"]]
         |}""".stripMargin

    val response = `given`
      .header(ACCEPT.toString, ACCEPT_RFC8621_VERSION_HEADER)
      .body(request)
    .when
      .post
    .`then`
      .statusCode(SC_OK)
      .contentType(JSON)
      .extract
      .body
      .asString

    assertThatJson(response)
      .inPath("methodResponses[1][1]")
      .isEqualTo(
        s"""{
          |   "accountId":"$ACCOUNT_ID",
          |   "state":"000001",
          |   "list":[],
          |   "notFound":["1"]
          |}
      """.stripMargin)
  }

  @Test
  def shouldAddInDelegatedMailboxesWhenHadAtLeastWriteRight(server: GuiceJamesServer): Unit = {
    val andreMailbox: String = "andrecustom"
    val andrePath = MailboxPath.forUser(ANDRE, andreMailbox)
    server.getProbe(classOf[MailboxProbeImpl]).createMailbox(andrePath)
    val message: Message = Message.Builder
      .of
      .setSender(BOB.asString())
      .setFrom(ANDRE.asString())
      .setSubject("test")
      .setBody("testmail", StandardCharsets.UTF_8)
      .build
    val messageId: MessageId = server.getProbe(classOf[MailboxProbeImpl])
      .appendMessage(ANDRE.asString, andrePath, AppendCommand.from(message))
      .getMessageId
    server.getProbe(classOf[ACLProbeImpl])
      .replaceRights(andrePath, BOB.asString, MailboxACL.Rfc4314Rights.of(Set(Right.Write, Right.Read).asJava))

    val request =
      s"""{
         |  "using": [
         |    "urn:ietf:params:jmap:core",
         |    "urn:ietf:params:jmap:mail"],
         |  "methodCalls": [
         |  ["Email/set",
         |    {
         |      "accountId": "$ACCOUNT_ID",
         |      "ids": ["${messageId.serialize}"],
         |      "update": {
         |        "${messageId.serialize}":{
         |          "keywords": {
         |             "music": true
         |          }
         |        }
         |      }
         |    },
         |    "c1"],
         |    ["Email/get",
         |     {
         |       "accountId": "$ACCOUNT_ID",
         |       "ids": ["${messageId.serialize}"],
         |       "properties": ["keywords"]
         |     },
         |     "c2"]]
         |}""".stripMargin

    val response = `given`
      .header(ACCEPT.toString, ACCEPT_RFC8621_VERSION_HEADER)
      .body(request)
    .when
      .post
    .`then`
      .statusCode(SC_OK)
      .contentType(JSON)
      .extract
      .body
      .asString

    assertThatJson(response)
      .inPath("methodResponses[1][1].list[0]")
      .isEqualTo(String.format(
        """{
          |   "id":"%s",
          |   "keywords": {
          |     "music":true
          |   }
          |}
      """.stripMargin, messageId.serialize))
  }

  @Test
  def emailSetShouldDestroyEmail(server: GuiceJamesServer): Unit = {
    val mailboxProbe = server.getProbe(classOf[MailboxProbeImpl])
    mailboxProbe.createMailbox(MailboxPath.inbox(BOB))
    val messageId: MessageId = mailboxProbe
      .appendMessage(BOB.asString, MailboxPath.inbox(BOB),
        AppendCommand.from(
          buildTestMessage))
      .getMessageId

    val request =
      s"""{
         |  "using": [
         |    "urn:ietf:params:jmap:core",
         |    "urn:ietf:params:jmap:mail"],
         |  "methodCalls": [
         |    ["Email/set", {
         |      "accountId": "29883977c13473ae7cb7678ef767cbfbaffc8a44a6e463d971d23a65c1dc4af6",
         |      "destroy": ["${messageId.serialize}"]
         |    }, "c1"],
         |    ["Email/get", {
         |      "accountId": "29883977c13473ae7cb7678ef767cbfbaffc8a44a6e463d971d23a65c1dc4af6",
         |      "ids": ["${messageId.serialize}"]
         |    }, "c2"]
         |  ]
         |}""".stripMargin

    val response = `given`
      .header(ACCEPT.toString, ACCEPT_RFC8621_VERSION_HEADER)
      .body(request)
    .when
      .post
    .`then`
      .statusCode(SC_OK)
      .contentType(JSON)
      .extract
      .body
      .asString

    assertThatJson(response).isEqualTo(
      s"""{
         |    "sessionState": "75128aab4b1b",
         |    "methodResponses": [
         |      ["Email/set", {
         |        "accountId": "29883977c13473ae7cb7678ef767cbfbaffc8a44a6e463d971d23a65c1dc4af6",
         |        "newState": "000001",
         |        "destroyed": ["${messageId.serialize}"]
         |      }, "c1"],
         |      ["Email/get", {
         |        "accountId": "29883977c13473ae7cb7678ef767cbfbaffc8a44a6e463d971d23a65c1dc4af6",
         |        "state": "000001",
         |        "list": [],
         |        "notFound": ["${messageId.serialize}"]
         |      }, "c2"]
         |    ]
         |}""".stripMargin)
  }

  @Test
  def emailSetDestroyShouldFailWhenInvalidMessageId(server: GuiceJamesServer): Unit = {
    val request =
      s"""{
         |  "using": [
         |    "urn:ietf:params:jmap:core",
         |    "urn:ietf:params:jmap:mail"],
         |  "methodCalls": [
         |    ["Email/set", {
         |      "accountId": "29883977c13473ae7cb7678ef767cbfbaffc8a44a6e463d971d23a65c1dc4af6",
         |      "destroy": ["invalid"]
         |    }, "c1"]]
         |}""".stripMargin

    val response = `given`
      .header(ACCEPT.toString, ACCEPT_RFC8621_VERSION_HEADER)
      .body(request)
    .when
      .post
    .`then`
      .statusCode(SC_OK)
      .contentType(JSON)
      .extract
      .body
      .asString

    assertThatJson(response).isEqualTo(
      s"""{
         |    "sessionState": "75128aab4b1b",
         |    "methodResponses": [
         |      ["Email/set", {
         |        "accountId": "29883977c13473ae7cb7678ef767cbfbaffc8a44a6e463d971d23a65c1dc4af6",
         |        "newState": "000001",
         |        "notDestroyed": {
         |          "invalid": {
         |            "type": "invalidArguments",
         |            "description": "invalid is not a messageId: For input string: \\"invalid\\""
         |          }
         |        }
         |      }, "c1"]]
         |}""".stripMargin)
  }

  @Test
  def emailSetDestroyShouldFailWhenMessageIdNotFound(server: GuiceJamesServer): Unit = {
    val messageId = randomMessageId

    val request =
      s"""{
         |  "using": [
         |    "urn:ietf:params:jmap:core",
         |    "urn:ietf:params:jmap:mail"],
         |  "methodCalls": [
         |    ["Email/set", {
         |      "accountId": "29883977c13473ae7cb7678ef767cbfbaffc8a44a6e463d971d23a65c1dc4af6",
         |      "destroy": ["${messageId.serialize}"]
         |    }, "c1"]]
         |}""".stripMargin

    val response = `given`
      .header(ACCEPT.toString, ACCEPT_RFC8621_VERSION_HEADER)
      .body(request)
    .when
      .post
    .`then`
      .statusCode(SC_OK)
      .contentType(JSON)
      .extract
      .body
      .asString

    assertThatJson(response).isEqualTo(
      s"""{
         |    "sessionState": "75128aab4b1b",
         |    "methodResponses": [
         |      ["Email/set", {
         |        "accountId": "29883977c13473ae7cb7678ef767cbfbaffc8a44a6e463d971d23a65c1dc4af6",
         |        "newState": "000001",
         |        "notDestroyed": {
         |          "${messageId.serialize}": {
         |            "type": "notFound",
         |            "description": "Cannot find message with messageId: ${messageId.serialize}"
         |          }
         |        }
         |      }, "c1"]]
         |}""".stripMargin)
  }

  @Test
  def emailSetDestroyShouldFailWhenMailDoesNotBelongToUser(server: GuiceJamesServer): Unit = {
    val mailboxProbe = server.getProbe(classOf[MailboxProbeImpl])
    mailboxProbe.createMailbox(MailboxPath.inbox(ANDRE))

    val messageId: MessageId = mailboxProbe
      .appendMessage(ANDRE.asString, MailboxPath.inbox(ANDRE),
        AppendCommand.from(
          buildTestMessage))
      .getMessageId

    val request =
      s"""{
         |  "using": [
         |    "urn:ietf:params:jmap:core",
         |    "urn:ietf:params:jmap:mail"],
         |  "methodCalls": [
         |    ["Email/set", {
         |      "accountId": "29883977c13473ae7cb7678ef767cbfbaffc8a44a6e463d971d23a65c1dc4af6",
         |      "destroy": ["${messageId.serialize}"]
         |    }, "c1"]]
         |}""".stripMargin

    val response = `given`
      .header(ACCEPT.toString, ACCEPT_RFC8621_VERSION_HEADER)
      .body(request)
    .when
      .post
    .`then`
      .statusCode(SC_OK)
      .contentType(JSON)
      .extract
      .body
      .asString

    assertThatJson(response).isEqualTo(
      s"""{
         |    "sessionState": "75128aab4b1b",
         |    "methodResponses": [
         |      ["Email/set", {
         |        "accountId": "29883977c13473ae7cb7678ef767cbfbaffc8a44a6e463d971d23a65c1dc4af6",
         |        "newState": "000001",
         |        "notDestroyed": {
         |          "${messageId.serialize}": {
         |            "type": "notFound",
         |            "description": "Cannot find message with messageId: ${messageId.serialize}"
         |          }
         |        }
         |      }, "c1"]
         |    ]
         |}""".stripMargin)
  }

  @Test
  def emailSetDestroyShouldFailWhenForbidden(server: GuiceJamesServer): Unit = {
    val mailboxProbe = server.getProbe(classOf[MailboxProbeImpl])

    val andreMailbox: String = "andrecustom"
    val path = MailboxPath.forUser(ANDRE, andreMailbox)
    mailboxProbe.createMailbox(path)

    val messageId: MessageId = mailboxProbe
      .appendMessage(ANDRE.asString, path,
        AppendCommand.from(
          buildTestMessage))
      .getMessageId

    server.getProbe(classOf[ACLProbeImpl])
      .replaceRights(path, BOB.asString, new MailboxACL.Rfc4314Rights(Right.Read))

    val request =
      s"""{
         |  "using": [
         |    "urn:ietf:params:jmap:core",
         |    "urn:ietf:params:jmap:mail"],
         |  "methodCalls": [
         |    ["Email/set", {
         |      "accountId": "29883977c13473ae7cb7678ef767cbfbaffc8a44a6e463d971d23a65c1dc4af6",
         |      "destroy": ["${messageId.serialize}"]
         |    }, "c1"]]
         |}""".stripMargin

    val response = `given`
      .header(ACCEPT.toString, ACCEPT_RFC8621_VERSION_HEADER)
      .body(request)
    .when
      .post
    .`then`
      .statusCode(SC_OK)
      .contentType(JSON)
      .extract
      .body
      .asString

    assertThatJson(response).isEqualTo(
      s"""{
         |    "sessionState": "75128aab4b1b",
         |    "methodResponses": [
         |      ["Email/set", {
         |        "accountId": "29883977c13473ae7cb7678ef767cbfbaffc8a44a6e463d971d23a65c1dc4af6",
         |        "newState": "000001",
         |        "notDestroyed": {
         |          "${messageId.serialize}": {
         |            "type": "notFound",
         |            "description": "Cannot find message with messageId: ${messageId.serialize}"
         |          }
         |        }
         |      }, "c1"]
         |    ]
         |}""".stripMargin)
  }

  @Test
  def emailSetDestroyShouldDestroyEmailWhenShareeHasDeleteRight(server: GuiceJamesServer): Unit = {
    val mailboxProbe = server.getProbe(classOf[MailboxProbeImpl])

    val andreMailbox: String = "andrecustom"
    val path = MailboxPath.forUser(ANDRE, andreMailbox)
    mailboxProbe.createMailbox(path)

    val messageId: MessageId = mailboxProbe
      .appendMessage(ANDRE.asString, path,
        AppendCommand.from(
          buildTestMessage))
      .getMessageId

    server.getProbe(classOf[ACLProbeImpl])
      .replaceRights(path, BOB.asString, new MailboxACL.Rfc4314Rights(Right.Read, Right.DeleteMessages))

    val request =
      s"""{
         |  "using": [
         |    "urn:ietf:params:jmap:core",
         |    "urn:ietf:params:jmap:mail"],
         |  "methodCalls": [
         |    ["Email/set", {
         |      "accountId": "29883977c13473ae7cb7678ef767cbfbaffc8a44a6e463d971d23a65c1dc4af6",
         |      "destroy": ["${messageId.serialize}"]
         |    }, "c1"],
         |    ["Email/get", {
         |      "accountId": "29883977c13473ae7cb7678ef767cbfbaffc8a44a6e463d971d23a65c1dc4af6",
         |      "ids": ["${messageId.serialize}"]
         |    }, "c2"]
         |  ]
         |}""".stripMargin

    val response = `given`
      .header(ACCEPT.toString, ACCEPT_RFC8621_VERSION_HEADER)
      .body(request)
    .when
      .post
    .`then`
      .statusCode(SC_OK)
      .contentType(JSON)
      .extract
      .body
      .asString

    assertThatJson(response).isEqualTo(
      s"""{
         |    "sessionState": "75128aab4b1b",
         |    "methodResponses": [
         |      ["Email/set", {
         |        "accountId": "29883977c13473ae7cb7678ef767cbfbaffc8a44a6e463d971d23a65c1dc4af6",
         |        "newState": "000001",
         |        "destroyed": ["${messageId.serialize}"]
         |      }, "c1"],
         |      ["Email/get", {
         |        "accountId": "29883977c13473ae7cb7678ef767cbfbaffc8a44a6e463d971d23a65c1dc4af6",
         |        "state": "000001",
         |        "list": [],
         |        "notFound": ["${messageId.serialize}"]
         |      }, "c2"]
         |    ]
         |}""".stripMargin)
  }

  @Test
  def emailSetDestroyShouldDestroyEmailWhenMovedIntoAnotherMailbox(server: GuiceJamesServer): Unit = {
    val mailboxProbe = server.getProbe(classOf[MailboxProbeImpl])

    val andreMailbox: String = "andrecustom"
    val andrePath = MailboxPath.forUser(ANDRE, andreMailbox)
    val bobPath = MailboxPath.inbox(BOB)
    mailboxProbe.createMailbox(andrePath)
    val mailboxId: MailboxId = mailboxProbe.createMailbox(bobPath)

    val messageId: MessageId = mailboxProbe
      .appendMessage(ANDRE.asString, andrePath,
        AppendCommand.from(
          buildTestMessage))
      .getMessageId

    server.getProbe(classOf[ACLProbeImpl])
      .replaceRights(andrePath, BOB.asString, new MailboxACL.Rfc4314Rights(Right.Insert))

    server.getProbe(classOf[JmapGuiceProbe])
      .setInMailboxes(messageId, BOB, mailboxId)

    val request =
      s"""{
         |  "using": [
         |    "urn:ietf:params:jmap:core",
         |    "urn:ietf:params:jmap:mail"],
         |  "methodCalls": [
         |    ["Email/set", {
         |      "accountId": "29883977c13473ae7cb7678ef767cbfbaffc8a44a6e463d971d23a65c1dc4af6",
         |      "destroy": ["${messageId.serialize}"]
         |    }, "c1"],
         |    ["Email/get", {
         |      "accountId": "29883977c13473ae7cb7678ef767cbfbaffc8a44a6e463d971d23a65c1dc4af6",
         |      "ids": ["${messageId.serialize}"]
         |    }, "c2"]
         |  ]
         |}""".stripMargin

    val response = `given`
      .header(ACCEPT.toString, ACCEPT_RFC8621_VERSION_HEADER)
      .body(request)
    .when
      .post
    .`then`
      .statusCode(SC_OK)
      .contentType(JSON)
      .extract
      .body
      .asString

    assertThatJson(response).isEqualTo(
      s"""{
         |    "sessionState": "75128aab4b1b",
         |    "methodResponses": [
         |      ["Email/set", {
         |        "accountId": "29883977c13473ae7cb7678ef767cbfbaffc8a44a6e463d971d23a65c1dc4af6",
         |        "newState": "000001",
         |        "destroyed": ["${messageId.serialize}"]
         |      }, "c1"],
         |      ["Email/get", {
         |        "accountId": "29883977c13473ae7cb7678ef767cbfbaffc8a44a6e463d971d23a65c1dc4af6",
         |        "state": "000001",
         |        "list": [],
         |        "notFound": ["${messageId.serialize}"]
         |      }, "c2"]
         |    ]
         |}""".stripMargin)
  }

  @Test
  def emailSetDestroySuccessAndFailureCanBeMixed(server: GuiceJamesServer): Unit = {
    val mailboxProbe = server.getProbe(classOf[MailboxProbeImpl])
    mailboxProbe.createMailbox(MailboxPath.inbox(BOB))
    val path = MailboxPath.inbox(BOB)

    val messageId: MessageId = mailboxProbe
      .appendMessage(BOB.asString, path,
        AppendCommand.from(
          buildTestMessage))
      .getMessageId

    val request =
      s"""{
         |  "using": [
         |    "urn:ietf:params:jmap:core",
         |    "urn:ietf:params:jmap:mail"],
         |  "methodCalls": [
         |    ["Email/set", {
         |      "accountId": "29883977c13473ae7cb7678ef767cbfbaffc8a44a6e463d971d23a65c1dc4af6",
         |      "destroy": [
         |        "${messageId.serialize}",
         |        "invalid"
         |      ]
         |    }, "c1"]]
         |}""".stripMargin

    val response = `given`
      .header(ACCEPT.toString, ACCEPT_RFC8621_VERSION_HEADER)
      .body(request)
    .when
      .post
    .`then`
      .statusCode(SC_OK)
      .contentType(JSON)
      .extract
      .body
      .asString

    assertThatJson(response).isEqualTo(
      s"""{
         |    "sessionState": "75128aab4b1b",
         |    "methodResponses": [
         |      ["Email/set", {
         |        "accountId": "29883977c13473ae7cb7678ef767cbfbaffc8a44a6e463d971d23a65c1dc4af6",
         |        "newState": "000001",
         |        "destroyed": ["${messageId.serialize}"],
         |        "notDestroyed": {
         |          "invalid": {
         |            "type": "invalidArguments",
         |            "description": "invalid is not a messageId: For input string: \\"invalid\\""
         |          }
         |        }
         |      }, "c1"]
         |    ]
         |}""".stripMargin)
  }

  private def buildTestMessage = {
    Message.Builder
      .of
      .setSubject("test")
      .setBody("testmail", StandardCharsets.UTF_8)
      .build
  }
}
