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

package org.apache.james.jmap.method

import eu.timepit.refined.auto._
import javax.inject.Inject
import org.apache.james.jmap.json.Serializer
import org.apache.james.jmap.mail._
import org.apache.james.jmap.model.Invocation.{Arguments, MethodName}
import org.apache.james.jmap.model.State.INSTANCE
import org.apache.james.jmap.model.{Invocation, MailboxFactory}
import org.apache.james.jmap.utils.quotas.{QuotaLoader, QuotaLoaderWithPreloadedDefaultFactory}
import org.apache.james.mailbox.exception.MailboxNotFoundException
import org.apache.james.mailbox.model.search.MailboxQuery
import org.apache.james.mailbox.model.{MailboxId, MailboxMetaData}
import org.apache.james.mailbox.{MailboxManager, MailboxSession}
import org.apache.james.metrics.api.MetricFactory
import org.reactivestreams.Publisher
import play.api.libs.json.{JsError, JsObject, JsSuccess}
import reactor.core.scala.publisher.{SFlux, SMono}
import reactor.core.scheduler.Schedulers

class MailboxGetMethod @Inject() (serializer: Serializer,
                                  mailboxManager: MailboxManager,
                                  quotaFactory : QuotaLoaderWithPreloadedDefaultFactory,
                                  mailboxFactory: MailboxFactory,
                                  metricFactory: MetricFactory) extends Method {
  override val methodName: MethodName = MethodName("Mailbox/get")

  object MailboxGetResults {
    def found(mailbox: Mailbox): MailboxGetResults = MailboxGetResults(List(mailbox), NotFound(Nil))
    def notFound(mailboxId: MailboxId): MailboxGetResults = MailboxGetResults(Nil, NotFound(List(mailboxId)))
  }

  case class MailboxGetResults(mailboxes: List[Mailbox], notFound: NotFound) {
    def merge(other: MailboxGetResults): MailboxGetResults = MailboxGetResults(this.mailboxes ++ other.mailboxes, this.notFound.merge(other.notFound))
  }

  override def process(invocation: Invocation, mailboxSession: MailboxSession): Publisher[Invocation] = {
    metricFactory.decoratePublisherWithTimerMetricLogP99(JMAP_RFC8621_PREFIX + methodName.value,
      asMailboxGetRequest(invocation.arguments)
        .flatMap(mailboxGetRequest => getMailboxes(mailboxGetRequest, mailboxSession)
          .reduce(MailboxGetResults(Nil, NotFound(Nil)), (result1: MailboxGetResults, result2: MailboxGetResults) => result1.merge(result2))
          .map(mailboxes => MailboxGetResponse(
            accountId = mailboxGetRequest.accountId,
            state = INSTANCE,
            list = mailboxes.mailboxes.sortBy(_.sortOrder),
            notFound = mailboxes.notFound))
          .map(mailboxGetResponse => Invocation(
            methodName = methodName,
            arguments = Arguments(serializer.serialize(mailboxGetResponse).as[JsObject]),
            methodCallId = invocation.methodCallId))))
  }

  private def asMailboxGetRequest(arguments: Arguments): SMono[MailboxGetRequest] = {
    serializer.deserializeMailboxGetRequest(arguments.value) match {
      case JsSuccess(mailboxGetRequest, _) => SMono.just(mailboxGetRequest)
      case errors: JsError => SMono.raiseError(new IllegalArgumentException(serializer.serialize(errors).toString))
    }
  }

  private def getMailboxes(mailboxGetRequest: MailboxGetRequest, mailboxSession: MailboxSession): SFlux[MailboxGetResults] = mailboxGetRequest.ids match {
    case None => getAllMailboxes(mailboxSession).map(MailboxGetResults.found)
    case Some(ids) => SFlux.fromIterable(ids.value)
      .flatMap(id => getMailboxByIdOrThrow(id, mailboxSession))
  }

  private def getMailboxByIdOrThrow(mailboxId: MailboxId, mailboxSession: MailboxSession): SMono[MailboxGetResults] =
    quotaFactory.loadFor(mailboxSession)
      .subscribeOn(Schedulers.elastic)
      .flatMap(quotaLoader => mailboxFactory.create(mailboxId, mailboxSession, quotaLoader)
        .flatMap {
          case Left(error) => error match {
            case _: MailboxNotFoundException => SMono.just(MailboxGetResults.notFound(mailboxId))
            case error => SMono.raiseError(error)
          }
          case scala.Right(mailbox) => SMono.just(MailboxGetResults.found(mailbox))
        })

  private def getAllMailboxes(mailboxSession: MailboxSession): SFlux[Mailbox] = {
    quotaFactory.loadFor(mailboxSession)
      .subscribeOn(Schedulers.elastic)
      .flatMapMany(quotaLoader =>
        getAllMailboxesMetaData(mailboxSession).flatMapMany(mailboxesMetaData =>
          SFlux.fromIterable(mailboxesMetaData)
            .flatMap(mailboxMetaData =>
              getMailboxOrThrow(mailboxMetaData = mailboxMetaData,
                mailboxSession = mailboxSession,
                allMailboxesMetadata = mailboxesMetaData,
                quotaLoader = quotaLoader))))
  }

  private def getAllMailboxesMetaData(mailboxSession: MailboxSession): SMono[Seq[MailboxMetaData]] =
    SFlux.fromPublisher(mailboxManager.search(MailboxQuery.builder.matchesAllMailboxNames.build, mailboxSession))
      .collectSeq()

  private def getMailboxOrThrow(mailboxSession: MailboxSession,
                                allMailboxesMetadata: Seq[MailboxMetaData],
                                mailboxMetaData: MailboxMetaData,
                                quotaLoader: QuotaLoader): SMono[Mailbox] =
    mailboxFactory.create(mailboxMetaData = mailboxMetaData,
      mailboxSession = mailboxSession,
      allMailboxesMetadata = allMailboxesMetadata,
      quotaLoader = quotaLoader)
      .flatMap {
        case Left(error) => SMono.raiseError(error)
        case scala.Right(mailbox) => SMono.just(mailbox)
      }
}
