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

package org.apache.james.mailbox.postgres.mail;

import java.time.Duration;
import java.util.function.Function;

import org.apache.james.core.Username;
import org.apache.james.mailbox.acl.ACLDiff;
import org.apache.james.mailbox.exception.UnsupportedRightException;
import org.apache.james.mailbox.model.Mailbox;
import org.apache.james.mailbox.model.MailboxACL;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.UidValidity;
import org.apache.james.mailbox.model.search.MailboxQuery;
import org.apache.james.mailbox.postgres.mail.dao.PostgresMailboxDAO;
import org.apache.james.mailbox.store.mail.MailboxMapper;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

public class PostgresMailboxMapper implements MailboxMapper {
    private final PostgresMailboxDAO postgresMailboxDAO;

    public PostgresMailboxMapper(PostgresMailboxDAO postgresMailboxDAO) {
        this.postgresMailboxDAO = postgresMailboxDAO;
    }

    @Override
    public Mono<Mailbox> create(MailboxPath mailboxPath, UidValidity uidValidity) {
        return postgresMailboxDAO.create(mailboxPath,uidValidity);
    }

    @Override
    public Mono<MailboxId> rename(Mailbox mailbox) {
        return postgresMailboxDAO.rename(mailbox);
    }

    @Override
    public Mono<Void> delete(Mailbox mailbox) {
        return postgresMailboxDAO.delete(mailbox.getMailboxId());
    }

    @Override
    public Mono<Mailbox> findMailboxByPath(MailboxPath mailboxName) {
        return postgresMailboxDAO.findMailboxByPath(mailboxName)
            .map(Function.identity());
    }

    @Override
    public Mono<Mailbox> findMailboxById(MailboxId mailboxId) {
        return postgresMailboxDAO.findMailboxById(mailboxId)
            .map(Function.identity());
    }

    @Override
    public Flux<Mailbox> findMailboxWithPathLike(MailboxQuery.UserBound query) {
        return postgresMailboxDAO.findMailboxWithPathLike(query)
            .map(Function.identity());
    }

    @Override
    public Mono<Boolean> hasChildren(Mailbox mailbox, char delimiter) {
        return postgresMailboxDAO.hasChildren(mailbox, delimiter);
    }

    @Override
    public Flux<Mailbox> list() {
        return postgresMailboxDAO.getAll()
            .map(Function.identity());
    }

    public Flux<Mailbox> findNonPersonalMailboxes(Username userName, MailboxACL.Right right) {
        return postgresMailboxDAO.findMailboxesByUsername(userName)
            .filter(postgresMailbox -> postgresMailbox.getACL().getEntries().get(MailboxACL.EntryKey.createUserEntryKey(userName)).contains(right))
            .map(Function.identity());
    }

    @Override
    public Mono<ACLDiff> updateACL(Mailbox mailbox, MailboxACL.ACLCommand mailboxACLCommand) {
        return postgresMailboxDAO.getACL(mailbox.getMailboxId())
            .flatMap(pairMailboxACLAndVersion -> {
                try {
                    MailboxACL newACL = pairMailboxACLAndVersion.getLeft().apply(mailboxACLCommand);
                    return postgresMailboxDAO.upsertACL(mailbox.getMailboxId(), newACL, pairMailboxACLAndVersion.getRight())
                        .thenReturn(ACLDiff.computeDiff(pairMailboxACLAndVersion.getLeft(), newACL));
                } catch (UnsupportedRightException e) {
                    throw new RuntimeException(e);
                }
            }).retryWhen(Retry.backoff(3, Duration.ofMillis(100))
                .filter(throwable -> throwable instanceof PostgresACLUpsertException));
    }

    @Override
    public Mono<ACLDiff> setACL(Mailbox mailbox, MailboxACL mailboxACL) {
        return postgresMailboxDAO.upsertACL(mailbox.getMailboxId(), mailboxACL)
            .thenReturn(ACLDiff.computeDiff(mailbox.getACL(), mailboxACL));
    }
}
