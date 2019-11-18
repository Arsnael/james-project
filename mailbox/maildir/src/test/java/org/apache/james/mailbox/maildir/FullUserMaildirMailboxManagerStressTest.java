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

package org.apache.james.mailbox.maildir;

import java.io.File;

import org.apache.james.mailbox.MailboxManagerStressContract;
import org.apache.james.mailbox.events.EventBus;
import org.apache.james.mailbox.store.StoreMailboxManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

class FullUserMaildirMailboxManagerStressTest implements MailboxManagerStressContract<StoreMailboxManager> {
    @TempDir
    File tmpFolder;

    private StoreMailboxManager mailboxManager;

    @Override
    public StoreMailboxManager getManager() {
        return mailboxManager;
    }

    @BeforeEach
    void setUp() {
        this.mailboxManager = provideManager();
    }

    private StoreMailboxManager provideManager() {
        try {
            return MaildirMailboxManagerProvider.createMailboxManager("/%fulluser", tmpFolder);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public EventBus retrieveEventBus(StoreMailboxManager mailboxManager) {
        return mailboxManager.getEventBus();
    }
}
