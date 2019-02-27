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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

    public List<String> listGroupsAsString() {
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

    private Mono<List<Event>> listGroupEvents(Group group) {
        return listGroupEventIds(group)
            .flatMap(eventId -> getEvent(group, eventId))
            .collect(Guavate.toImmutableList());
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

    public Task createActionOnEventsTask(ActionEvents action) {
        Map<Group, List<Event>> groupsWithEvents = new HashMap<>();

        listGroups()
            .map(group -> listGroupEvents(group).map(events -> groupsWithEvents.put(group, events)))
            .blockLast();

        return createActionOnEventsTask(groupsWithEvents, action);
    }

    public Task createActionOnEventsTask(Group group, ActionEvents action) {
        List<Event> events = listGroupEvents(group).block();

        return createActionOnEventsTask(ImmutableMap.of(group, events), action);
    }

    public Task createActionOnEventsTask(Group group, Event.EventId eventId, ActionEvents action) {
        Event event = getEvent(group, eventId).block();

        return createActionOnEventsTask(ImmutableMap.of(group, ImmutableList.of(event)), action);
    }

    private Task createActionOnEventsTask(Map<Group, List<Event>> groupsWithEvents, ActionEvents action) {
        switch (action) {
            case reDeliver:
                return redeliverEvents(groupsWithEvents);
            default:
                throw new IllegalArgumentException(action + " is not a supported action");
        }
    }

    private Task redeliverEvents(Map<Group, List<Event>> groupsWithEvents) {
        return new EventDeadLettersRedeliverTask(eventBus, groupsWithEvents);
    }
}
