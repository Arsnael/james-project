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

package org.apache.james.mailbox.postgres.mail.eventsourcing.acl;

import org.apache.james.event.acl.ACLUpdated;
import org.apache.james.event.acl.ACLUpdatedDTO;
import org.apache.james.eventsourcing.eventstore.dto.EventDTOModule;
import org.apache.james.json.DTOModule;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.postgres.PostgresMailboxId;

public interface ACLModule {
    String UPDATE_TYPE_NAME = "acl-updated";

    MailboxId.Factory mailboxIdFactory = new PostgresMailboxId.Factory();

    EventDTOModule<ACLUpdated, ACLUpdatedDTO> ACL_UPDATE =
        new DTOModule.Builder<>(ACLUpdated.class)
            .convertToDTO(ACLUpdatedDTO.class)
            .toDomainObjectConverter(dto -> dto.toEvent(mailboxIdFactory))
            .toDTOConverter(ACLUpdatedDTO::from)
            .typeName(UPDATE_TYPE_NAME)
            .withFactory(EventDTOModule::new);
}