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

import static org.mockito.Mockito.mock;

import org.apache.james.JsonSerializationVerifier;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.model.TestMessageId;
import org.apache.james.util.ClassLoaderUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecomputeMessageFastViewProjectionItemTaskSerializationTest {
    MessageFastViewProjectionCorrector corrector;
    MessageId.Factory messageIdFactory;
    RecomputeMessageFastViewProjectionItemTask.Factory factory;

    @BeforeEach
    void setUp() {
        messageIdFactory = new TestMessageId.Factory();
        corrector = mock(MessageFastViewProjectionCorrector.class);
        factory = new RecomputeMessageFastViewProjectionItemTask.Factory(corrector, new TestMessageId.Factory());
    }

    @Test
    void shouldMatchJsonSerializationContract() throws Exception {
        MessageId messageId = messageIdFactory.fromString("1");

        JsonSerializationVerifier.dtoModule(RecomputeMessageFastViewProjectionItemTask.module(factory))
            .bean(new RecomputeMessageFastViewProjectionItemTask(corrector, messageId))
            .json(ClassLoaderUtils.getSystemResourceAsString("json/recomputeMessage.task.json"))
            .verify();
    }
}
