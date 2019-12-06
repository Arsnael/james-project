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

package org.apache.james.mailbox.store.search;

import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.inmemory.manager.InMemoryIntegrationResources;
import org.junit.jupiter.api.Disabled;

class SimpleMessageSearchIndexTest extends AbstractMessageSearchIndexTest {

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
            .searchIndex(preInstanciationStage -> new SimpleMessageSearchIndex(
                preInstanciationStage.getMapperFactory(),
                preInstanciationStage.getMapperFactory(),
                new PDFTextExtractor()))
            .noPreDeletionHooks()
            .storeQuotaManager()
            .build();

        storeMailboxManager = resources.getMailboxManager();
        messageIdManager = resources.getMessageIdManager();
        messageSearchIndex = resources.getSearchIndex();
    }

    /**
     * 32 tests out of 54 are failing
     */

    @Disabled
    @Override
    public void flagIsSetShouldReturnUidOfMessageMarkedAsRecentWhenUsedWithFlagRecent() throws MailboxException {
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
    public void addressShouldReturnUidHavingRightRecipientWhenBccIsSpecified() throws Exception {
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
    public void headerContainsShouldReturnUidsOfMessageHavingThisHeaderWithTheSpecifiedValue() throws Exception {
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
    public void headerExistsShouldReturnUidsOfMessageHavingThisHeader() throws Exception {
    }

    @Disabled
    @Override
    public void modSeqLessThanShouldReturnUidsOfMessageHavingAGreaterModSeq() throws Exception {
    }

    @Disabled
    @Override
    public void addressShouldReturnUidHavingRightRecipientWhenCcIsSpecified() throws Exception {
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
    public void sortOnDisplayToShouldWork() throws Exception {
    }

    @Disabled
    @Override
    public void flagIsUnSetShouldReturnUidOfMessageNotMarkedAsRecentWhenUsedWithFlagRecent() throws MailboxException {
    }

    @Disabled
    @Override
    public void bodyContainsShouldReturnUidOfMessageContainingTheApproximativeText() throws MailboxException {
    }

    @Disabled
    @Override
    public void headerDateBeforeShouldWork() throws Exception {
    }

    @Disabled
    @Override
    public void sortOnSentDateShouldWork() throws Exception {
    }

    @Disabled
    @Override
    public void addressShouldReturnUidHavingRightRecipientWhenToIsSpecified() throws Exception {
    }

    @Disabled
    @Override
    public void sortOnToShouldWork() throws Exception {
    }

    @Disabled
    @Override
    public void sortOnDisplayFromShouldWork() throws Exception {
    }

    @Disabled
    @Override
    public void revertSortingShouldReturnElementsInAReversedOrder() throws Exception {
    }

    @Disabled
    @Override
    public void headerDateAfterShouldWork() throws Exception {
    }

    @Disabled
    @Override
    public void mailsContainsShouldIncludeMailHavingAttachmentsMatchingTheRequest() throws Exception {
    }

    @Disabled
    @Override
    public void sortOnSubjectShouldWork() throws Exception {
    }

    @Disabled
    @Override
    public void modSeqGreaterThanShouldReturnUidsOfMessageHavingAGreaterModSeq() throws Exception {
    }

    @Disabled
    @Override
    public void notShouldReturnResultsThatDoNotMatchAQuery() throws Exception {
    }

    @Disabled
    @Override
    public void headerDateOnShouldWork() throws Exception {
    }

    @Disabled
    @Override
    public void sortOnSizeShouldWork() throws Exception {
    }

    @Disabled
    @Override
    public void sortShouldOrderMessages() throws Exception {
    }

    @Disabled
    @Override
    public void multimailboxSearchShouldReturnUidOfMessageWithExpectedFromInTwoMailboxes() throws MailboxException {
    }

    @Disabled
    @Override
    public void searchWithTextShouldReturnMailsWhenTextBodyMatchesAndNonContinuousWords() throws Exception {
    }

    @Disabled
    @Override
    public void searchWithTextShouldReturnMailsWhenHtmlBodyMatchesAndNonContinuousWords() throws Exception {
    }

    @Disabled
    @Override
    public void searchWithTextShouldReturnMailsWhenTextBodyWithExtraUnindexedWords() throws Exception {
    }

    @Disabled
    @Override
    public void searchWithTextShouldReturnMailsWhenHtmlBodyMatchesWithStemming() throws Exception {
    }

    @Disabled
    @Override
    public void headerWithDotsShouldBeIndexed() throws MailboxException {
    }
}
