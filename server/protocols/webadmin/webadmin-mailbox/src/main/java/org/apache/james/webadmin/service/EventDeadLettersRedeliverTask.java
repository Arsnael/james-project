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

package org.apache.james.webadmin.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;

import org.apache.james.mailbox.events.Event;
import org.apache.james.mailbox.events.EventBus;
import org.apache.james.mailbox.events.Group;
import org.apache.james.task.Task;
import org.apache.james.task.TaskExecutionDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class EventDeadLettersRedeliverTask implements Task {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventDeadLettersRedeliverTask.class);
    public static final String TYPE = "eventDeadLettersRedeliverTask";

    public static class AdditionalInformation implements TaskExecutionDetails.AdditionalInformation {
        private final long successfulRedeliveriesCount;
        private final long failedRedeliverieCount;

        AdditionalInformation(long successfulRedeliveriesCount, long failedRedeliverieCount) {
            this.successfulRedeliveriesCount = successfulRedeliveriesCount;
            this.failedRedeliverieCount = failedRedeliverieCount;
        }

        public long getSuccessfulRedeliveriesCount() {
            return successfulRedeliveriesCount;
        }

        public long getFailedRedeliveriesCount() {
            return failedRedeliverieCount;
        }
    }

    private final EventBus eventBus;
    private final Map<Group, List<Event>> groupsWithEvents;
    private final AtomicLong successfulRedeliveriesCount;
    private final AtomicLong failedRedeliverieCount;

    @Inject
    EventDeadLettersRedeliverTask(EventBus eventBus, Map<Group, List<Event>> groupsWithEvents) {
        this.eventBus = eventBus;
        this.groupsWithEvents = groupsWithEvents;
        this.successfulRedeliveriesCount = new AtomicLong(0L);
        this.failedRedeliverieCount = new AtomicLong(0L);
    }

    @Override
    public Result run() {
        return Flux.fromIterable(groupsWithEvents.entrySet())
            .flatMap(entry -> redeliverGroupEvents(entry.getKey(), entry.getValue()))
            .reduce(Result.COMPLETED, Task::combine)
            .onErrorResume(e -> {
                LOGGER.error("Error while redelivering events", e);
                return Mono.just(Result.PARTIAL);
            })
            .block();
    }

    private Mono<Result> redeliverGroupEvents(Group group, List<Event> events) {
        return Flux.fromStream(events.stream())
            .flatMap(event -> redeliverEvent(group, event))
            .reduce(Result.COMPLETED, Task::combine);
    }

    private Mono<Result> redeliverEvent(Group group, Event event) {
        return eventBus.reDeliver(group, event)
            .then(Mono.fromCallable(() -> {
                successfulRedeliveriesCount.incrementAndGet();
                return Result.COMPLETED;
            }))
            .onErrorResume(e -> {
                LOGGER.error("Error while performing redelivery of event: {} for group: {}",
                    group.asString(), event.getEventId().toString(), e);
                failedRedeliverieCount.incrementAndGet();
                return Mono.just(Result.PARTIAL);
            });
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Optional<TaskExecutionDetails.AdditionalInformation> details() {
        return Optional.of(createAdditionalInformation());
    }

    AdditionalInformation createAdditionalInformation() {
        return new AdditionalInformation(
            successfulRedeliveriesCount.get(),
            failedRedeliverieCount.get());
    }
}
