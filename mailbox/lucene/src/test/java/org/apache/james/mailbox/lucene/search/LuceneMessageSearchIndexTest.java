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

package org.apache.james.mailbox.lucene.search;

import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.inmemory.InMemoryId;
import org.apache.james.mailbox.inmemory.InMemoryMessageId;
import org.apache.james.mailbox.inmemory.manager.InMemoryIntegrationResources;
import org.apache.james.mailbox.store.search.AbstractMessageSearchIndexTest;
import org.apache.lucene.store.RAMDirectory;
import org.junit.jupiter.api.Disabled;

import com.github.fge.lambdas.Throwing;

class LuceneMessageSearchIndexTest extends AbstractMessageSearchIndexTest {

    @Override
    protected void await() {
    }

    @Override
    protected void initializeMailboxManager() {
        InMemoryIntegrationResources resources = InMemoryIntegrationResources.builder()
            .preProvisionnedFakeAuthenticator()
            .fakeAuthorizator()
            .inVmEventBus()
            .defaultAnnotationLimits()
            .defaultMessageParser()
            .listeningSearchIndex(Throwing.function(preInstanciationStage -> new LuceneMessageSearchIndex(
                preInstanciationStage.getMapperFactory(), new InMemoryId.Factory(), new RAMDirectory(),
                new InMemoryMessageId.Factory(),
                preInstanciationStage.getSessionProvider())))
            .noPreDeletionHooks()
            .storeQuotaManager()
            .build();

        storeMailboxManager = resources.getMailboxManager();
        messageIdManager = resources.getMessageIdManager();
        messageSearchIndex = resources.getSearchIndex();
    }

    @Disabled
    @Override
    public void uidShouldreturnEveryThing() throws Exception {
    }

    @Disabled
    @Override
    public void sortOnCcShouldWork() throws Exception {
    }

    @Disabled
    @Override
    public void sortOnFromShouldWork() throws Exception {
    }

    @Disabled
    @Override
    public void orShouldReturnResultsMatchinganyRequests() throws Exception {
    }

    @Disabled
    @Override
    public void internalDateBeforeShouldReturnMessagesBeforeAGivenDate() throws Exception {
    }

    @Disabled
    @Override
    public void internalDateAfterShouldReturnMessagesAfterAGivenDate() throws Exception {
    }

    @Disabled
    @Override
    public void youShouldBeAbleToSpecifySeveralCriterionOnASingleQuery() throws Exception {
    }

    @Disabled
    @Override
    public void modSeqLessThanShouldReturnUidsOfMessageHavingAGreaterModSeq() throws Exception {
    }

    @Disabled
    @Override
    public void andShouldReturnResultsMatchingBothRequests() throws Exception {
    }

    @Disabled
    @Override
    public void addressShouldReturnUidHavingRightExpeditorWhenFromIsSpecified() throws Exception {
    }

    @Disabled
    @Override
    public void bodyContainsShouldReturnUidOfMessageContainingTheApproximativeText() throws MailboxException {
    }

    @Disabled
    @Override
    public void sortOnDisplayFromShouldWork() throws Exception {
    }

    @Disabled
    @Override
    public void mailsContainsShouldIncludeMailHavingAttachmentsMatchingTheRequest() throws Exception {
    }

    @Disabled
    @Override
    public void modSeqGreaterThanShouldReturnUidsOfMessageHavingAGreaterModSeq() throws Exception {
    }

    @Disabled
    @Override
    public void modSeqEqualsShouldReturnUidsOfMessageHavingAGivenModSeq() throws Exception {
    }

    @Disabled
    @Override
    public void multimailboxSearchShouldReturnUidOfMessageMarkedAsSeenInTwoMailboxes() throws MailboxException {
    }

    @Disabled
    @Override
    public void multimailboxSearchShouldReturnUidOfMessageMarkedAsSeenInAllMailboxes() throws MailboxException {
    }

    @Disabled("Lucene implementation is not handling mail addresses with names")
    @Override
    public void sortOnToShouldWork() {
    }
}
