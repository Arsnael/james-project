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

package org.apache.james.webadmin.condition.user;

import jakarta.inject.Inject;

import org.apache.james.core.Username;
import org.apache.james.user.api.UsersRepositoryException;
import org.apache.james.webadmin.UserCondition;
import org.apache.james.webadmin.service.UserMailboxesService;

public class HasNoMailboxesCondition implements UserCondition {
    public static final String HAS_NO_MAILBOXES_PARAM = "hasNoMailboxes";

    private final UserMailboxesService userMailboxesService;

    @Inject
    public HasNoMailboxesCondition(UserMailboxesService userMailboxesService) {
        this.userMailboxesService = userMailboxesService;
    }

    @Override
    public boolean test(Username username) {
        try {
            return userMailboxesService.listMailboxes(username, UserMailboxesService.Options.Force).isEmpty();
        } catch (UsersRepositoryException e) {
            throw new RuntimeException(e);
        }
    }
}
