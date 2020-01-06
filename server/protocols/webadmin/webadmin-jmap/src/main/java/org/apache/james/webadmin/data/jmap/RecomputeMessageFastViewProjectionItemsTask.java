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

package org.apache.james.webadmin.data.jmap;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.james.json.DTOModule;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.server.task.json.dto.TaskDTO;
import org.apache.james.server.task.json.dto.TaskDTOModule;
import org.apache.james.task.Task;
import org.apache.james.task.TaskExecutionDetails;
import org.apache.james.task.TaskType;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RecomputeMessageFastViewProjectionItemsTask implements Task {
    static final TaskType TASK_TYPE = TaskType.of("RecomputeMessageFastViewProjectionItemsTask");

    public static class Factory {
        private final MessageFastViewProjectionCorrector corrector;
        private final MessageId.Factory messageIdFactory;

        @Inject
        public Factory(MessageFastViewProjectionCorrector corrector, MessageId.Factory messageIdFactory) {
            this.corrector = corrector;
            this.messageIdFactory = messageIdFactory;
        }

        public RecomputeMessageFastViewProjectionItemsTask create(RecomputeMessageFastViewTaskDTO dto) {
            MessageId messageId = messageIdFactory.fromString(dto.getMessageId());
            return new RecomputeMessageFastViewProjectionItemsTask(corrector, messageId);
        }
    }

    public static class AdditionalInformation implements TaskExecutionDetails.AdditionalInformation {
        private static AdditionalInformation from(MessageId messageId) {
            return new AdditionalInformation(messageId,
                Clock.systemUTC().instant());
        }

        private final MessageId messageId;
        private final Instant timestamp;

        public AdditionalInformation(MessageId messageId, Instant timestamp) {
            this.messageId = messageId;
            this.timestamp = timestamp;
        }

        public String getMessageId() {
            return messageId.serialize();
        }

        @Override
        public Instant timestamp() {
            return timestamp;
        }
    }

    public static class RecomputeMessageFastViewTaskDTO implements TaskDTO {
        private final String type;
        private final String messageId;

        public RecomputeMessageFastViewTaskDTO(@JsonProperty("type") String type,
                                               @JsonProperty("messageId") String messageId) {
            this.type = type;
            this.messageId = messageId;
        }

        @Override
        public String getType() {
            return type;
        }

        public String getMessageId() {
            return messageId;
        }
    }

    public static TaskDTOModule<RecomputeMessageFastViewProjectionItemsTask, RecomputeMessageFastViewTaskDTO> module(RecomputeMessageFastViewProjectionItemsTask.Factory factory) {
        return DTOModule
            .forDomainObject(RecomputeMessageFastViewProjectionItemsTask.class)
            .convertToDTO(RecomputeMessageFastViewTaskDTO.class)
            .toDomainObjectConverter(factory::create)
            .toDTOConverter((task, type) -> new RecomputeMessageFastViewTaskDTO(type, task.messageId.serialize()))
            .typeName(TASK_TYPE.asString())
            .withFactory(TaskDTOModule::new);
    }

    private final MessageFastViewProjectionCorrector corrector;
    private final MessageFastViewProjectionCorrector.Progress progress;
    private final MessageId messageId;

    RecomputeMessageFastViewProjectionItemsTask(MessageFastViewProjectionCorrector corrector, MessageId messageId) {
        this.corrector = corrector;
        this.messageId = messageId;
        this.progress = new MessageFastViewProjectionCorrector.Progress();
    }

    @Override
    public Result run() {
        //TODO
        return Result.COMPLETED;
    }

    @Override
    public TaskType type() {
        return TASK_TYPE;
    }

    @Override
    public Optional<TaskExecutionDetails.AdditionalInformation> details() {
        return Optional.of(AdditionalInformation.from(messageId));
    }
}
