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

package org.apache.james.transport.matchers;

import static org.apache.james.transport.matchers.AtMostFailureRetries.MAX_FAILURE_RETRIES;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;

import javax.mail.MessagingException;

import org.apache.james.core.MailAddress;
import org.apache.mailet.Attribute;
import org.apache.mailet.AttributeValue;
import org.apache.mailet.base.test.FakeMail;
import org.apache.mailet.base.test.FakeMatcherConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AtMostFailureRetriesTest {
    private static final String CONDITION = "2";

    private AtMostFailureRetries matcher;
    private MailAddress testRecipient;

    private FakeMail createFakeMail(int atMostRetries) throws MessagingException {
        return FakeMail.builder()
            .name("test-message")
            .attribute(new Attribute(MAX_FAILURE_RETRIES, AttributeValue.of(atMostRetries)))
            .recipient(testRecipient)
            .build();
    }

    @BeforeEach
    void setup() throws MessagingException {
        this.matcher = new AtMostFailureRetries();
        FakeMatcherConfig matcherConfig = FakeMatcherConfig.builder()
            .matcherName("AtMostFailureRetries")
            .condition(CONDITION)
            .build();

        matcher.init(matcherConfig);
        testRecipient = new MailAddress("test@james.apache.org");
    }

    @Test
    void shouldMatchWhenNoRetries() throws MessagingException {
        FakeMail fakeMail = createFakeMail(0);

        Collection<MailAddress> actual = matcher.match(fakeMail);

        assertThat(actual).containsOnly(testRecipient);
    }

    @Test
    void shouldNotMatchWhenMaximumRetriesReached() throws MessagingException {
        FakeMail fakeMail = createFakeMail(3);
        Collection<MailAddress> actual = matcher.match(fakeMail);

        assertThat(actual).isEmpty();
    }

    @Test
    void shouldMatchWhenEqualToAtMost() throws MessagingException {
        FakeMail fakeMail = createFakeMail(1);

        Collection<MailAddress> actual = matcher.match(fakeMail);

        assertThat(actual).containsOnly(testRecipient);
    }
}
