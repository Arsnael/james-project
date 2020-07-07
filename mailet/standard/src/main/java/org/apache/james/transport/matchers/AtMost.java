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

import java.util.Collection;
import java.util.Optional;

import javax.mail.MessagingException;

import org.apache.james.core.MailAddress;
import org.apache.mailet.Attribute;
import org.apache.mailet.AttributeName;
import org.apache.mailet.AttributeUtils;
import org.apache.mailet.AttributeValue;
import org.apache.mailet.Mail;
import org.apache.mailet.base.GenericMatcher;
import org.apache.mailet.base.MailetUtil;

import com.google.common.collect.ImmutableList;

/**
 * Checks that a mail did at most X tries on a specific operation.
 *
 * If no retries have been performed previously, it sets up an attribute `AT_MOST_TRIES`
 * in the mail that will be incremented everything the check succeeds.
 *
 * The check fails when the defined X limit is reached.
 *
 * <p>The example below will match mail with at most 3 tries on the mailet</p>
 *
 * <pre><code>
 * &lt;mailet match=&quot;AtMost=3&quot; class=&quot;&lt;any-class&gt;&quot;&gt;
 * &lt;/mailet&gt;
 * </code></pre>
 */
public class AtMost extends GenericMatcher {
    static final AttributeName AT_MOST_TRIES = AttributeName.of("AT_MOST_TRIES");
    private Integer atMostRetries;

    @Override
    public void init() throws MessagingException {
        this.atMostRetries = MailetUtil.getInitParameterAsStrictlyPositiveInteger(getCondition());
    }

    @Override
    public Collection<MailAddress> match(Mail mail) throws MessagingException {
        return AttributeUtils.getValueAndCastFromMail(mail, AT_MOST_TRIES, Integer.class)
            .or(() -> Optional.of(0))
            .filter(retries -> retries < atMostRetries)
            .map(retries -> {
                mail.setAttribute(new Attribute(AT_MOST_TRIES, AttributeValue.of(retries + 1)));
                return mail.getRecipients();
            })
            .orElse(ImmutableList.of());
    }
}
