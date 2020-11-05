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

import java.io.InputStream

import eu.timepit.refined.auto._
import javax.annotation.PreDestroy
import javax.inject.Inject
import javax.mail.internet.MimeMessage
import org.apache.james.jmap.core.CapabilityIdentifier.{CapabilityIdentifier, JMAP_CORE, JMAP_MAIL}
import org.apache.james.jmap.core.Invocation.{Arguments, MethodName}
import org.apache.james.jmap.core.{ClientId, Id, Invocation, ServerId, SetError, State}
import org.apache.james.jmap.core.SetError.SetErrorDescription
import org.apache.james.jmap.json.{EmailSubmissionSetSerializer, ResponseSerializer}
import org.apache.james.jmap.mail.EmailSubmissionSet.EmailSubmissionCreationId
import org.apache.james.jmap.mail.{EmailSubmissionCreationRequest, EmailSubmissionCreationResponse, EmailSubmissionId, EmailSubmissionSetRequest, EmailSubmissionSetResponse}
import org.apache.james.jmap.method.EmailSubmissionSetMethod.{LOGGER, MAIL_METADATA_USERNAME_ATTRIBUTE}
import org.apache.james.jmap.routes.{ProcessingContext, SessionSupplier}
import org.apache.james.lifecycle.api.{LifecycleUtil, Startable}
import org.apache.james.mailbox.model.{FetchGroup, MessageResult}
import org.apache.james.mailbox.{MailboxSession, MessageIdManager}
import org.apache.james.metrics.api.MetricFactory
import org.apache.james.queue.api.MailQueueFactory.SPOOL
import org.apache.james.queue.api.{MailQueue, MailQueueFactory}
import org.apache.james.server.core.{MailImpl, MimeMessageCopyOnWriteProxy, MimeMessageInputStreamSource}
import org.apache.mailet.{Attribute, AttributeName, AttributeValue}
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json._
import reactor.core.scala.publisher.{SFlux, SMono}
import reactor.core.scheduler.Schedulers

import scala.jdk.CollectionConverters._
import scala.util.Try

object EmailSubmissionSetMethod {
  val MAIL_METADATA_USERNAME_ATTRIBUTE: AttributeName = AttributeName.of("org.apache.james.jmap.send.MailMetaData.username")
  val LOGGER: Logger = LoggerFactory.getLogger(classOf[EmailSubmissionSetMethod])
}

case class EmailSubmissionCreationParseException(setError: SetError) extends Exception

class EmailSubmissionSetMethod @Inject()(serializer: EmailSubmissionSetSerializer,
                                         messageIdManager: MessageIdManager,
                                         mailQueueFactory: MailQueueFactory[_ <: MailQueue],
                                         val metricFactory: MetricFactory,
                                         val sessionSupplier: SessionSupplier) extends MethodRequiringAccountId[EmailSubmissionSetRequest] with Startable {
  override val methodName: MethodName = MethodName("EmailSubmission/set")
  override val requiredCapabilities: Set[CapabilityIdentifier] = Set(JMAP_CORE, JMAP_MAIL)
  var queue: MailQueue = null

  sealed trait CreationResult {
    def emailSubmissionCreationId: EmailSubmissionCreationId
  }
  case class CreationSuccess(emailSubmissionCreationId: EmailSubmissionCreationId, emailSubmissionCreationResponse: EmailSubmissionCreationResponse) extends CreationResult
  case class CreationFailure(emailSubmissionCreationId: EmailSubmissionCreationId, exception: Throwable) extends CreationResult {
    def asSetError: SetError = exception match {
      case e: EmailSubmissionCreationParseException => e.setError
      case e: Exception =>
        e.printStackTrace()
        SetError.serverFail(SetErrorDescription(exception.getMessage))
    }
  }
  case class CreationResults(created: Seq[CreationResult]) {
    def retrieveCreated: Map[EmailSubmissionCreationId, EmailSubmissionCreationResponse] = created
      .flatMap(result => result match {
        case success: CreationSuccess => Some(success.emailSubmissionCreationId, success.emailSubmissionCreationResponse)
        case _ => None
      })
      .toMap
      .map(creation => (creation._1, creation._2))


    def retrieveErrors: Map[EmailSubmissionCreationId, SetError] = created
      .flatMap(result => result match {
        case failure: CreationFailure => Some(failure.emailSubmissionCreationId, failure.asSetError)
        case _ => None
      })
      .toMap
  }

  def init(): Unit = {
    queue = mailQueueFactory.createQueue(SPOOL)
  }

  @PreDestroy def dispose(): Unit = {
    Try(queue.close())
      .recover(e => LOGGER.debug("error closing queue", e))
  }

  override def doProcess(capabilities: Set[CapabilityIdentifier], invocation: InvocationWithContext, mailboxSession: MailboxSession, request: EmailSubmissionSetRequest): SMono[InvocationWithContext] = {
    for {
      createdResults <- create(request, mailboxSession, invocation.processingContext)
    } yield InvocationWithContext(
      invocation = Invocation(
        methodName = invocation.invocation.methodName,
        arguments = Arguments(serializer.serializeEmailSubmissionSetResponse(EmailSubmissionSetResponse(
            accountId = request.accountId,
            newState = State.INSTANCE,
            created = Some(createdResults._1.retrieveCreated).filter(_.nonEmpty),
            notCreated = Some(createdResults._1.retrieveErrors).filter(_.nonEmpty)))
          .as[JsObject]),
        methodCallId = invocation.invocation.methodCallId),
      processingContext = createdResults._2)
  }

  override def getRequest(mailboxSession: MailboxSession, invocation: Invocation): SMono[EmailSubmissionSetRequest] =
    asEmailSubmissionSetRequest(invocation.arguments)

  private def asEmailSubmissionSetRequest(arguments: Arguments): SMono[EmailSubmissionSetRequest] =
    serializer.deserializeEmailSubmissionSetRequest(arguments.value) match {
      case JsSuccess(emailSubmissionSetRequest, _) => SMono.just(emailSubmissionSetRequest)
      case errors: JsError => SMono.raiseError(new IllegalArgumentException(ResponseSerializer.serialize(errors).toString))
    }

  private def create(request: EmailSubmissionSetRequest,
                     session: MailboxSession,
                     processingContext: ProcessingContext): SMono[(CreationResults, ProcessingContext)] = {
    SFlux.fromIterable(request.create
      .getOrElse(Map.empty)
      .view)
      .foldLeft((CreationResults(Nil), processingContext)){
        (acc : (CreationResults, ProcessingContext), elem: (EmailSubmissionCreationId, JsObject)) => {
          val (emailSubmissionCreationId, jsObject) = elem
          val (creationResult, updatedProcessingContext) = createSubmission(session, emailSubmissionCreationId, jsObject, acc._2)
          (CreationResults(acc._1.created :+ creationResult), updatedProcessingContext)
        }
      }
      .subscribeOn(Schedulers.elastic())
  }

  private def createSubmission(mailboxSession: MailboxSession,
                            emailSubmissionCreationId: EmailSubmissionCreationId,
                            jsObject: JsObject,
                            processingContext: ProcessingContext): (CreationResult, ProcessingContext) = {
    parseCreate(jsObject)
      .flatMap(emailSubmissionCreationRequest => sendEmail(mailboxSession, emailSubmissionCreationRequest))
      .flatMap(creationResponse => recordCreationIdInProcessingContext(emailSubmissionCreationId, processingContext, creationResponse.id)
        .map(context => (creationResponse, context)))
      .fold(e => (CreationFailure(emailSubmissionCreationId, e), processingContext),
        creationResponseWithUpdatedContext => {
          (CreationSuccess(emailSubmissionCreationId, creationResponseWithUpdatedContext._1), creationResponseWithUpdatedContext._2)
        })
  }

  private def parseCreate(jsObject: JsObject): Either[EmailSubmissionCreationParseException, EmailSubmissionCreationRequest] =
    EmailSubmissionCreationRequest.validateProperties(jsObject)
      .flatMap(validJsObject => Json.fromJson(validJsObject)(serializer.emailSubmissionCreationRequestReads) match {
        case JsSuccess(creationRequest, _) => Right(creationRequest)
        case JsError(errors) => Left(EmailSubmissionCreationParseException(emailSubmissionSetError(errors)))
      })

  private def emailSubmissionSetError(errors: collection.Seq[(JsPath, collection.Seq[JsonValidationError])]): SetError =
    errors.head match {
      case (path, Seq()) => SetError.invalidArguments(SetErrorDescription(s"'$path' property in EmailSubmission object is not valid"))
      case (path, Seq(JsonValidationError(Seq("error.path.missing")))) => SetError.invalidArguments(SetErrorDescription(s"Missing '$path' property in EmailSubmission object"))
      case (path, Seq(JsonValidationError(Seq(message)))) => SetError.invalidArguments(SetErrorDescription(s"'$path' property in EmailSubmission object is not valid: $message"))
      case (path, _) => SetError.invalidArguments(SetErrorDescription(s"Unknown error on property '$path'"))
    }

  private def sendEmail(mailboxSession: MailboxSession,
                        request: EmailSubmissionCreationRequest): Either[Throwable, EmailSubmissionCreationResponse] = {
    val message: Either[Exception, MessageResult] = messageIdManager.getMessage(request.emailId, FetchGroup.FULL_CONTENT, mailboxSession)
      .asScala
      .toList
      .headOption
      .toRight(MessageNotFoundException(request.emailId))

    message.flatMap(m => {
      val submissionId = EmailSubmissionId.generate
      toMimeMessage(submissionId.value, m.getFullContent.getInputStream)
        .flatMap(mimeMessage => {
          Try(queue.enQueue(MailImpl.builder()
              .name(submissionId.value)
              .addRecipients(request.envelope.rcptTo.map(_.email).asJava)
              .sender(request.envelope.mailFrom.email)
              .mimeMessage(mimeMessage)
              .addAttribute(new Attribute(MAIL_METADATA_USERNAME_ATTRIBUTE, AttributeValue.of(mailboxSession.getUser.asString())))
              .build()))
            .map(_ => EmailSubmissionCreationResponse(submissionId))
        }).toEither
    })
  }

  private def toMimeMessage(name: String, inputStream: InputStream): Try[MimeMessage] = {
    val source = new MimeMessageInputStreamSource(name, inputStream)
    // if MimeMessageCopyOnWriteProxy throws an error in the constructor we
    // have to manually care disposing our source.
    Try(new MimeMessageCopyOnWriteProxy(source))
      .recover(e => {
        LifecycleUtil.dispose(source)
        throw e
      })
  }

  private def recordCreationIdInProcessingContext(emailSubmissionCreationId: EmailSubmissionCreationId,
                                                  processingContext: ProcessingContext,
                                                  emailSubmissionId: EmailSubmissionId) = {
    for {
      creationId <- Id.validate(emailSubmissionCreationId)
      serverAssignedId <- Id.validate(emailSubmissionId.value)
    } yield {
      processingContext.recordCreatedId(ClientId(creationId), ServerId(serverAssignedId))
    }
  }
}
