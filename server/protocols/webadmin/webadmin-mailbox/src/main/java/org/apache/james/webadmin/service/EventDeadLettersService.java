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
import java.util.UUID;

import javax.inject.Inject;

import org.apache.james.event.json.EventSerializer;
import org.apache.james.mailbox.events.Event;
import org.apache.james.mailbox.events.EventBus;
import org.apache.james.mailbox.events.EventDeadLetters;
import org.apache.james.mailbox.events.Group;
import org.apache.james.task.Task;
import org.apache.james.webadmin.dto.ActionEvents;

import com.github.steveash.guavate.Guavate;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

public class EventDeadLettersService {
    private final EventDeadLetters deadLetters;
    private final EventBus eventBus;
    private final EventSerializer eventSerializer;

    @Inject
    public EventDeadLettersService(EventDeadLetters deadLetters, EventBus eventBus, EventSerializer eventSerializer) {
        this.deadLetters = deadLetters;
        this.eventBus = eventBus;
        this.eventSerializer = eventSerializer;
    }

    public List<String> listGroupsAsStrings() {
        return listGroups()
            .map(Group::asString)
            .collect(Guavate.toImmutableList())
            .block();
    }

    private Flux<Group> listGroups() {
        return deadLetters.groupsWithFailedEvents();
    }

    public List<String> listGroupsEventIdsAsStrings(Group group) {
        return listGroupEventIds(group)
            .map(Event.EventId::getId)
            .map(UUID::toString)
            .collect(Guavate.toImmutableList())
            .block();
    }

    private Flux<Event.EventId> listGroupEventIds(Group group) {
        return deadLetters.failedEventIds(group);
    }

    private Flux<Event> listGroupEvents(Group group) {
        return listGroupEventIds(group)
            .flatMap(eventId -> getEvent(group, eventId));
    }

    public String getSerializedEvent(Group group, Event.EventId eventId) {
        return getEvent(group, eventId)
            .map(eventSerializer::toJson)
            .block();
    }

    private Mono<Event> getEvent(Group group, Event.EventId eventId) {
        return deadLetters.failedEvent(group, eventId);
    }

    public void deleteEvent(Group group, Event.EventId eventId) {
        deadLetters.remove(group, eventId).block();
    }

    private Flux<Tuple2<Group, Event>> getGroupWithEvents(Group group) {
        return Flux.just(group).zipWith(listGroupEvents(group));
    }

    public Task createActionOnEventsTask(ActionEvents action) {
        Flux<Tuple2<Group, Event>> groupsWithEvents = listGroups().flatMap(group -> getGroupWithEvents(group));

        return createActionOnEventsTask(groupsWithEvents, action);
    }

    public Task createActionOnEventsTask(Group group, ActionEvents action) {
        return createActionOnEventsTask(getGroupWithEvents(group), action);
    }

    public Task createActionOnEventsTask(Group group, Event.EventId eventId, ActionEvents action) {
        Flux<Tuple2<Group, Event>> groupWithEvent = Flux.just(group).zipWith(getEvent(group, eventId));

        return createActionOnEventsTask(groupWithEvent, action);
    }

    private Task createActionOnEventsTask(Flux<Tuple2<Group, Event>> groupsWithEvents, ActionEvents action) {
        switch (action) {
            case reDeliver:
                return redeliverEvents(groupsWithEvents);
            default:
                throw new IllegalArgumentException(action + " is not a supported action");
        }
    }

    private Task redeliverEvents(Flux<Tuple2<Group, Event>> groupsWithEvents) {
        return new EventDeadLettersRedeliverTask(eventBus, groupsWithEvents);
    }
}
