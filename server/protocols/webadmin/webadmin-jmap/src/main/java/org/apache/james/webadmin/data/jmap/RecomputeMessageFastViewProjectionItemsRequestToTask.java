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

import javax.inject.Inject;

import org.apache.james.mailbox.model.MessageId;
import org.apache.james.task.Task;
import org.apache.james.webadmin.tasks.TaskFromRequestRegistry;

import spark.Request;

public class RecomputeMessageFastViewProjectionItemsRequestToTask extends TaskFromRequestRegistry.TaskRegistration {
    @Inject
    RecomputeMessageFastViewProjectionItemsRequestToTask(MessageFastViewProjectionCorrector corrector,
                                                         MessageId.Factory factory) {
        super(Constants.TASK_REGISTRATION_KEY,
            request -> toTask(corrector, factory, request));
    }

    private static Task toTask(MessageFastViewProjectionCorrector corrector, MessageId.Factory factory, Request request) {
        MessageId messageId = factory.fromString(request.params("messageId"));
        return new RecomputeMessageFastViewProjectionItemsTask(corrector, messageId);
    }
}
