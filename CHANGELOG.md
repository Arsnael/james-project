# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)

## [unreleased]

### Important changes

 - Upgrade javax -> jakarta. See releated upgrade instructions.
 - Upgrade Java 11 -> 21. See related upgrade instructions.
 - API change: CassandraModule and all associated Cassandra*Module were renamed to CassandraDataDefinition and Cassandra*DataDefinition respectively.
 - API change : Mailet API has been cleaned up and no longer exposes the mailet config, exposing only the mailet's name instead.
 - API change : GenericMailet can have its config bound by constructor and this is the preferred way to initialize your mailets. `init` is still called but will eventually be removed in a later release. 
 - JAMES-2586: A performant Postgres based implementation of Apache James. Usable in a standalone context but can scale as well with components like RabbitMQ, OpenSearch and S3.

### Removals

 - JAMES-3946 WhiteList manager removals (#2299)

This had been repleaced by the Guice DropList extension.

 - JAMES-4025 Drop Jmap draft

Please use RFC-8621 implementation rather than this outdated draft.

 - JAMES-4065 drop habeas warrant mark mailet

### Security

- [FIX] JMX password auto-detection
- [FIX] Enforce CRLF as part of SMTP DATA transaction (#1876)
- [FIX] Set up JMX auth for Spring
- [FIX] Set up JMX auth filter for Guice
- Bump org.apache.commons:commons-configuration2 from 2.9.0 to 2.10.1 (#2147)
- [Fix] BouncyCastle 1.70 -> 1.77 (fixes multiple minor CVEs)
- [FIX] SMTP stack should recompute relaying rights upon PROXY message (#1933)
- JAMES-4032 DKIM SMTP hook (#2264)
- JAMES-4034 SMTP submission: validate FROM header (#2246)
- JAMES-4041 Fix OOM upon IMAP COPY (#2265)
- [UPGRADE] Logback 1.5.12 -> 1.5.15 (#2581)
- Bump commons-io to 2.17.0
- [S3 Metrics] Allow to customize S3 metrics prefix
- Upgrade jdkim library to 0.5
- JAMES-4104 [webadmin] Migrate from unmaintained SparkJava to active fork

The project also bumped dependencies to their latest version as of June 2025.

### New Features

- JAMES-3942 Audit trail
- JAMES-3897 Crowdsec integration for SMTP, IMAP
- JAMES-3964 Implement and test disabledCaps for SMTP
- JAMES-3962 JMAP Email/set: specific headers for body parts (#1801)
- JAMES-3960 Hints to ensure UID/ModSeq consistency in case of disaster
- JAMES-3959 Starting distributed James without OpenSearch (#1784)
- JAMES-3954 Implement RFC-9394 PARTIAL for IMAP FETCH/SEARCH
- JAMES-3944 JMAP FILTER: More features: forward, flag, discard, etc...
- JAMES-3930 ReadOnlyUsersLDAPRepository::test should allow authentication using localPart as login username
- Supporting several LDAP hosts
- JAMES 3822 SMTP FutureRelease Extension
- JAMES-2434 IsFromMailingList matcher (#1606)
- JAMES-3906 SSL hot reload for IMAP /SMTP
- JAMES-3908 Storage directive with several folder (#1575)
- JAMES-3909 Fully delete one user data webadmin endpoint
- JAMES-4047 DeconnectionRight mailet (#2325)
- JAMES-4044 LDAP matchers
- JAMES-3946 DropLists
- [ENHANCEMENT] Reprocess for a specific recipient (#2226)
- [ENHANCEMENT] Workqueue for the deleted message vault (#2131)
- JAMES-4092 Update webadmin to filter content from mail repository (#2600)
- JAMES-4090 Webadmin interface to close IMAP session for selected users
- JAMES-4077 Implementation of JMAP SearchSnippets (highlights)
- JAMES-3823 SMTP Require TLS Option (#2460)
- James-4099 Make mailbox path delimiter configurable
- JAMES-4098 ReadOnlyUsersLDAPRepository::isAdministrator should take into account the multiple administrators if configured
- James-4097 Allow disabling same-domain requirement when assigning rights (#2573)
- JAMES-3945 Sub-addressing support
- [ENHANCEMENT] Add a PartHasContentType matcher
- [ENHANCEMENT] Provide a core RecipientCountExceeds matcher (#2534)
- [FIX] Ability to split emails with large recipient counts (#2526)
- JAMES-4085 SSE-C Implement for S3 Object Storage
- JAMES-4091 Endpoint to list connected Users
- JAMES-2182 IMAP support for shared mailboxes
- JAMES-4081 Implement MailToAllUsers mailet (#2469)
- JAMES-4071 Task fix inconsistencies mailbox flags: messagedeleted, mailboxRecents
- JAMES-4069 Healthcheck for IMAP (#2401)
- JAMES-4068 Have several health checks in one rest calls (#2399)
- JAMES-3824 SMTP Extension for Message Transfer Priorities
- JAMES-4100 JMAP SearchSnippet/get implementation

### Improvements
 
 - JAMES-4125 rework mailet api
 - JAMES-4123 Renames CassandraModule to CassandraDataDefinition
 - JAMES-4103 Allow customizing MessageParser
 - JAMES-3967 RelayLimit: add error log
 - JAMES-3967 Store mails when relay is exceeded
 - [FIX] LdapRepositoryConfiguration should interoperate with UsersRepositoryImpl (#1855)
 - JAMES-3539 PushSubscription/get should return expired subscriptions (#1845)
 - [ENHANCEMENT] Allow to trust all certificates with S3 blobstore (#1846)
 - Numerous dependency updates
 - [IMPROVEMENT] JMAP: Display message/disposition-notification in main message body
 - JAMES-3965 DKIMSign: Use fileSystem in order to resolve privateKeyFilepath
 - JAMES-3963 Full text search: AND for combining phrase terms (#1810)
 - JAMES-3963 Sort From To Cc on display name first (#1811)
 - JAMES-3955 Increase consumer timeout for TaskManagerWorkQueue
 - JAMES-3955 WARNING logs upon closing RABBITMQ channels
 - JAMES-3955 Applicative timeouts before Rabbit timeouts
 - JAMES-3958 DKIM: Be resilient when updating mails with invalid headers (#1783)
 - JAMES-3874 LMTP should handle overQuota exceptions
 - JAMES-3944 Forwards should rewrite MAIL FROM
 - JAMES-3887 MailboxManager::createMailboxReactive should subscribe parent mailboes it creates (#1752)
 - JAMES-3828 ICALAttributeDTO: support missing uid, method and dtstamp fields
 - [IMPROVMENT] Add Cassandra statements to the logs (#1723)
 - JAMES-3925 Rework JMAP quota cleanup
 - JAMES-3926 Move mutualized quota DAOs to backend/cassandra
 - JAMES-3604 Enable to use quorum queues onto DTM work queue (#1708)
 - JAMES-3938 IMAP MOVE should not fail on empty mailbox (#1713)
 - [FIX] VerifySenderIdentity makes sense when auth is not announced (#1693)
 - [FIX] PropagateLookupRightListener should not fail on missing mailbox
 - [FIX] Some S3 like object storage, like MinIO, don't accept underscores in their bucket names
 - JAMES-3914 Sieve actions fails when several recipients
 - JAMES-3911 JPA: Prevent concurrent operations on the same EntityManager
 - [FIX] Avoid sending bounce when reprocessing (#2139)
 - [FIX] IMAP APPEND file leak
 - JAMES-4020 Fully comply with RFC-3501 Section 6.4.5 (#2123)
 - [LOGGING] Aborting JMAP upload is too verbose (#2125)
 - JAMES-4021 Relay for "abc@def"@domain.com
 - [FIX] AbstractValidRcptHandler fix error handling on invalid username (#2122)
 - [UPDATE] MIME4J 0.8.9 -> 0.8.11 (#2120)
 - [FIX] JMAP: Set configurable limits for /get /set objects (#2096)
 - JAMES-4019 ReactiveThrottler should handle better cancellation (#2104)
 - [ENHANCEMENT] Decrease log level for "NotSslRecordException"
 - JAMES-4018 SMTP RCPT TO should parse parameters once
 - [FIX] Ship mail over web webadmin routes into more apps (#2099)
 - [FIX] Try recover invalid ascii character in filename
 - [FIX] RSpamD should not fail when reporting empty body
 - JAMES-3534 Fixbug - Identity response alway return mayDelete=false when server set identity
 - [FIX] ImapIdleStateHandler should cancel ongoing requests firs
 - [FIX] cancel ongoing processing for inactive channels
 - [FIX] Modified server set identities may not be deletes (#2083)
 - [FIX] Calendar parsing: be less verbose
 - [IMPROVEMENT] Allow Email/set create to override attachment charset
 - JMAP - Delete/destroy should take a set as an input
 - [FIX] -openjpa.Multithreaded => -Dopenjpa.Multithreaded
 - [FIX] Avoid forwarding bounces
 - JAMES-4007 Manage IMAP litteral with Leak aware
 - JAMES-1717 Notification Registry - handle case Ints.checkedCast throws out of range when expireDate too far (#2051)
 - JAMES-4012 Lenient MDN parsing for human readable part (#2057)
 - [FIX] Lower log message in MaybeSender
 - JAMES-4013 Relay MDN/send
 - JAMES-3885 Webadmin route to change of username - support `force` parameter  - do not require old user to exist (#2054)
 - JAMES-4008 JMAP - Email/set - Should be able to save a draft with invalid email address (#2040)
 - JAMES-4009 StripAttachment should explicitly handle duplicates
 - [FIX] MailReceptionCheck do not send the mail before we actively listen to the results (#2038)
 - JMAP - EmailSubmission/set - Should not invoke onSuccessDestroyEmail/onSuccessUpdateEmail when not created (#2041)
 - JAMES-4006 Bouncer should pass DSN to bounce processor
 - JAMES-3998 Empty password should not lead to worrying error (#2026)
 - JAMES-4004 [FIX] Allow retries for S3 saves (#2025)
 - [FIX] Apply batch size to prefetch
 - JAMES-3995 Add a max number of items for EmailGet full reads (#2024)
 - [FIX] Use Netty dns async resolvers
 - [FIX] IMAP LEAK: CLOSE, UNSELECT
 - [FIX] Email/set should allow creating an email with attachment of a destroyed message
 - [FIX] multipart/alternative with attachment had no text value indexed
 - [FIX] Do not double index multipart/alternative
 - JAMES-3775 Pass SSL information to RspamD scanner mailet
 - JAMES-3991 Vacation handling should be case insensitive
 - [FIX] Lower log level of SSL handshake exceptions (#1998)
 - JAMES-3986 AttachmentFileNameIs should be decently tested (#1991)
 - [FIX] Apply RFC-5321 syntax validation for EHLO (#1979)
 - JAMES-3981 Remove double logs upon closed channels
 - [IMPROVEMENT] SPF mailet: add missing private network "192.168.0.0/16" and use correct CIDR for private range "172.16.0.0/12"
 - [ENHANCEMENT] Add Hostname header onto RspamD client
 - [FIX] Prevent creation of multiline mailbox names (#1973)
 - [ENHANCEMENT] Add SSL information into SMTP header
 - JAMES-3976 Email/import should accept empty keywords (#1962)
 - [FIX] OpenSearch should normalize base subject (#1910)
 - JAMES-3968 Fix mail loss due to RabbitMQ ack failure
 - [FIX] HookResultHook added to marker interfaces
 - JAMES-3581 Parsing TypeName should be more lenient while parsing server side data (#2343)
 - JAMES-4048 Record LDAP connection metrics
 - JAMES-4043 Improve literal handling (#2311)
 - JAMES-3994 Adopt Apache Pekko instead of Akka (#2300)
 - [FIX] registrations multimap made fully thread safe (#2291)
 - JAMES-3955 Software timeout before consumer timeout (#2284)
 - [IMPROVEMENT] Configure fastview projection miss threshold (#2276)
 - JAMES-4015 VacationMailet should sanitize broken Reply-To fields (#2275)
 - JAMES-4040 JMAP mailbox role spam => junk (#2261)
 - JAMES-4038 Relax EHLO validation for EMclient (#2262)
 - JAMES-4037 Fix NPE in IMAP LIST with APPENDLIMIT (#2257)
 - JAMES-4037 Resolve MailboxTyper for Spring
 - [METRICS] Expose useful S3 Client Metrics
 - JAMES-3539 Backend should clean expired Push subscriptions
 - [ENHANCEMENT] Metrics for rabbitmq channel pool (#2232)
 - JAMES-4027 Make all queues on Rabbitmq quorum queue when quorum option is enabled
 - JAMES-4029 Fix infinite loop with Bounce + forward
 - JAMES-4026 Fix inconsistency issue between imap and JMAP RFC-8621
 - [FIX] Filter too big values for Cassandra TTLs (#2156)
 - [ENHANCEMENT] Add User-Agent in JMAP logging context (#2604)
 - JAMES-4101 Wrap SMTp startTLS into a NETTY transaction
 - [FIX] Silent verbose SSL logs in SMTP (#2578)
 - [FIX] Silent Eof exception for webadmin (#2577)
 - [devscout] upgrade logback and crowdsec
 - [FIX] Preserve internal date on restored messages (#2574)
 - [FIX] Reject non strictly positive POP3 indexes (#2575)
 - [FIX] Cleanup EmailQueryView when populating it.
 - [FIX] POP3 UserCmdHandler should return error response when invalid username input
 - [FIX] FileNameIs matcher: Parsing for long content disposition filename (#2555)
 - [FIX] IMAP login parsing: handle IllegalArgumentException
 - [FIX] RspamD for large moves
 - [FIX] Prevent WebSocket data race (#2550)
 - JAMES-4096 Rename INBOX (#2549)
 - JAMES-3605 Implement DeletedMessageVaultWorkQueueReconnectionHandler
 - [FIX] DistributedDeletedMessageVaultDeletionCallback: add an applicative timeout before RabbitMQ consumer timeout
 - [FIX] Improve S3 MinIO support with MinIOGenerationAwareBlobId MinIO compatibility
 - JAMES-4095 Forbid deleting INBOX in IMAP (#2541)
 - [FIX] Support RFC-3848 service types in Received headers (#2536)
 - JAMES-4094 Trigger quota-ratio index update when modifying quota limits (#2538)
 - [FIX] IDLE - prevent data race upon answers on channel writes (#2525)
 - JAMES-3893 Identity routes should not return 500 code when invalid username (#2519)
 - [FIX] Filter out invalid user in LDAP
 - [FIX] Always schedule execution of new IMAP requests to the event loop
 - [FIX] No longer use recursivity in ReactiveThrottler::onRequestDone
 - [FIX] JAMES-4088 Ensure IMAP SELECT EXIST response is up to date (#2507)
 - JAMES-2456 Update Tika to version 3.0.0.0
 - JAMES-4087 Allow combining composite matchers
 - [ENHANCEMENT] Ensure the MDC context is carried over upon IMAP parsing errors
 - JAMES-4077 Back memory-app with Lucene
 - JAMES-3754 Update IMAP4 ID - return NIL, replace to empty list
 - [ENHANCEMENT] mailUserAgent in IMAP MDC
 - JAMES-3954 PartialRange might be reversed
 - [FIX] Prevent queue poisoning for spool
 - [FIX] Store add a missing flush
 - [FIX] Avoid NPE on partially written users in Cassandra users repository (#2458)
 - JAMES-4079 Fix RecipientRewriteTable adds duplicate mapping (#2449)
 - James 1409 - Change JPARecipientRewriteTable to store separate record per target address (#2444)
 - JAMES-3552 Better enforcements of JMAP session limits
 - AmqpForwardAttribute: support HA by allowing specifying several hosts (#2430)
 - [FIX] Handle XOAUTH2 for SMTP without initial response (#2428)
 - [FIX] WebSocket should return `Sec-WebSocket-Protocol: jmap` response header
 - AmqpForwardAttribute mailet: ignore errors when creating an exchange that already exists with a different configuration. (#2414)
 - James: unset JAVA_TOOL_OPTIONS env variable when running james-cli
 - JAMES-4050 Allow `%` and `*` characters in mailbox names
 - JAMES-4060: Fix UID FETCH command failing on empty mailbox (#2386)
 - Explicit commit after running reindex (#2381)
 - Improving James shutdown speed (#2380)
 - JAMES-4063 Update SMIMECheckSignature to use Filesystem to load crypto materials and handle multiple certificates
 - JAMES-4058 TextCalendarBodyToAttachment screw up text/calendar parts (#2384)
 - JAMES-4046 Upgrade Lucene to version 9.11.1 (#2373)
 - JAMES-4054 Add X-SMIME-Status in SMIMECheckSignature process (#2366)
 - JAMES-4052 Add optional user property to OpenSearch index (#2363)
 - JAMES-4052 Add quota details in quota indexed for enhanced dashboarding (#2361)
 - [IMPROVEMENT] JMAP use search index relevance as default sort
 - [ENHANCEMENT] Log cassandra table informations
 - [ENHANCEMENT] Add a simpler password mechanism to webadmin (#2759)
 - JAMES-3754 Relax Id command parsing (#2758)
 - [ENAHNCEMENT] Implement a MaxConnectionLifespanHandler SMTP handler
 - [ENHANCEMENT] DKIMVerify outputs logs and machine readable attributes
 - [ENHANCEMENT] Include filename in search snippet
 - JAMES-4135 Add the possibility to add custom IndexSettings to MailboxMappingFactory
 - JAMES-4316 Allow deleting all groups + management by mspping source
 - JAMES-3693 Redis clustering enhancements
 - JAMES-4057 Match attachment against file extensions (#2731)
 - JAMES-3539 Types field in push subscription should accept null value (#2723) and WebPushClient should accept more flexible 20x codes from Push server
 - JAMES-5431 add SanitizeMimeMessageId mailet  (#2718)
 - JAMES-4131 Configure a fallback bucket for S3 (#2719)
 - JAMES-2182 IMAP processors should catch InsufficientRightsException
 - [FIX] DSNBounce should add Auto-Submitted: auto-replied header (#2717)
 - JAMES-3819 Give tools to enforce RFC-8461 MTA-STS (#2697)
 - [ENHANCEMENT] JMAP MDC: make the original IP header configurable
 - [JAMES-4119] Core data migration tool from JPA to Postgres
 - JAMES-4115 FileMailRepository: drop cacheKey & refactor (#2662)
 - JAMES-4117 JMAP - Email/set create - support blobId in htmlBody + textBody properties (#2661)
 - JAMES-3962 JMAP Email/set: move `EmailHeader[]` from `bodyValues` to `htmlBody`/`textBody` (#2659)
 - JAMES-3788 Allow configuring if Proxy or SSL frames should be handled… (#2634)

### Performance

 - [FIX] S3BlobStoreDAO::readReactive is blocking on the driver thread
 - JAMES-3977 Backpressure for IMAP FETCH
 - [ENHANCEMENT] JMAP: limit parallel read in case of fastView miss
 - [ENHANCEMENT] JsoupHtmlTextExtractor: The use of concurrent structures is not needed
 - [ENHANCEMENT] Message content extractor: pre-size strings
 - [ENHANCEMENT] Computing preview: avoid decoding base64
 - [ENHANCEMENT] AESBlobStoreDAO: carry over the size
 - [FIX] Notify eventbus outside of the lock
 - JAMES-3995 Optimize Email/get
 - JAMES-4010 Ability to not index body (#2018)
 - JAMES-4001 File buffering for message storage (#2019)
 - [FIX] IMAP FETCH was pulling all messages into memory
 - [ENHANCEMENT] Apply RabbitMQ classic queue version 2 (#2243)
 - [ENHANCEMENT] Fully reactive RRT for JMAP
 - [ENHANCEMENT] Better reactify Identity methods
 - [FIX] Remove some blocking calls in IMAP event loop
 - [FIX] Leak upon unmanaged SMTP error (#2221)
 - JAMES-4023 Avoid blocking the SMTP Netty event loop
 - [PERF] Avoid calling Session::getAttachment on each SMTP line
 - [PERF] JMAP: Allow browsers to cache download blobs
 - [FIX] Email/set destroy should fire one event per impacted mailbox (#2576)
 - [FIX] Prevent dispatching empty flags update (#2572)
 - JAMES-3491 - JMAP WebSockets - support ping interval (#2561)
 - JAMES-4093 Traffic shaping to enable QOS on IMAP service
 - [PERF] Allow lowering EMail/set range threshold
 - [PERF] Avoid triggering quota updates on message move
 - [PERF] Leverage grouped events in order to reduce RabbitMQ chatter and aggregate JMAP changes
 - Mertics for Websocket usage
 - JAMES-3516 Rely on Cassandra for Thread lookups (#2750)
 - Improve IMAP partial body fetch (#2740)
 - [ENHANCEMENT] Allow setting a preview for TTL
 - [ENHANCEMENT] Document data tiering for Apache James distributed server

### Build

 - Publish build scans to develocity.apache.org

### Removal

 - [DROP] Remove Linshare third party connector (#2613

## [Unreleased 3.8.x]

No changes yet.

## [3.8.2] - 2025-02-05

### Security

- **CVE-2024-37358**: Denial of service through the use of IMAP literals
- **CVE-2024-45626**: Denial of service through JMAP HTML to text conversion

- [FIX] Prevent HtmlTextExtractor to generate asymmetric outputs
- [IMPROVEMENT] Better manage IMAP literals (3.8.x) (#2281)

### Bug fixes

- JAMES-4036 JMS mailQueue should silent interuptedExceptions upon shutdown
- JAMES-4041 Fix OOM upon IMAP COPY
- JAMES-4037 Resolve MailboxTyper for Spring (#2255)
- JAMES-4037 Fix NPE in IMAP LIST with APPENDLIMIT (#2258)
- JAMES-3955 Increase consumer timeout for TaskManagerWorkQueue
- JAMES-3955 WARNING logs upon closing channels
- JAMES-3955 Applicative timeouts before Rabbit timeouts
- JAMES-3955 Check for queues without consumer and resume consumption as needed
- JAMES-4023 Avoid blocking the SMTP Netty event loop (#2230)
- [FIX] -openjpa.Multithreaded => -Dopenjpa.Multithreaded
- JAMES-4006 Bouncer should pass DSN to bounce processor
- JAMES-3991 Vacation handling should be case insensitive
- [maven-release-plugin] prepare for next development iteration

## [3.8.1] - 2024-02-23

### Security

- **CVE-2023-51747**: SMTP smuggling in Apache James
- **CVE-2023-51518**: Privilege escalation via JMX pre-authentication deserialisation
- [FIX] JMX password auto-detection
- [FIX] Enforce CRLF as part of SMTP DATA transaction (#1876)
- [FIX] Set up JMX auth for Spring
- [FIX] Set up JMX auth filter for Guice

### Enhancements 

 - JAMES-3938 IMAP MOVE should not fail on empty mailbox (#1716)
 - JAMES-3604 Enable to use quorum queues onto DTM work queue (#1714)
 - JAMES-3924 Allow conditional Cassandra statement recording
 - JAMES-3934 Allow external scheduling for RabbitMQ mailQueue browse st… (#1682)
 - JAMES-3918 Force deletion of user mailboxes (#1608) (#1611)
 - FIX Proxy for SMTPS (#1594)
 - JAMES-3914 Sieve actions fails when several recipients
 - Small JPA enhancements

## [3.8.0] - 2023-05-17

This release brings the following significant changes:

 - Upgrade TCP protocols to Netty 4
 - Migrate IMAP protocol as reactive
 - Multiple additional IMAP extensions are implemented
 - Upgrade to Cassandra driver 4
 - Migrate to OpenSearch
 - Review our threading model to cap threads performing blocking tasks
 - Implement official JMAP quotas specification

### Removal

 - Remove server/blob/blob-gc (unused draft work, redundant with GC current bloom-filter based implementation)

### Deprecations

 - Deprecating custom JMAP quotas specification
 - [DEPRECATION] Deprecate JDBCMailRepository (#1237)

### Added

 - JAMES-3775 Rspamd extension, including per-user database management
 - JAMES-3810 Health check for the RabbitMQ MailQueue view
 - JAMES-3788 IMAP/SMTP compatibility with the proxy protocol (HAProxy)
 - JAMES-3802 Webadmin route to clean events of a dead letter group
 - JAMES-3784 EmptyErrorMailRepositoryHealthCheck
 - JAMES-3796 Guice support the custom task in extension
 - JAMES-3794 Implement ActiveMQ HealthCheck (#1105)
 - JAMES-3778 Extension example for publishing metrics into Graphite
 - JAMES-3757 IMAP/SMTP OIDC extensions should support impersonation (#1035)
 - JAMES-3756 Webadmin API for delegation
 - JAMES-3768 Allow disabling Cassandra Mail queue view (#1009)
 - JAMES-3758 Webadmin task to delete old emails from the user INBOX
 - JAMES-3755 IMAP/SMTP OIDC token introspection (#1006)
 - JAMES-3769 Search overrides to offload searches on OpenSearch to Cassandra when meaningful
 - JAMES-3715 Allow disabling graceful shutdown
 - JAMES-3724 Leak detection to prevent temporary file leaks
 - JAMES-3723 Allow to not consume emails upon reprocessing
 - JAMES-3830 Implement official JMAP quotas specification
 - JAMES-3841 Metrics for embedded ActiveMQ
 - JAMES-3867 Make IMAP modular (#1343) allows writing IMAP extensions
 - JAMES-3755 OIDC check token by user info (#1340)
 - JAMES-3850 [JMAP] Configure `urn:ietf:params:jmap:mail` `maxSizeAttac… (#1303)
 - Support for multiple IMAP RFCs:
   - Support for RFC-8514 Extension for savedate
   - Support for RFC-5258 LIST extensions
   - Support for RFC-5819 LIST STATUS
   - Support for RFC-8440 LIST MY RIGHTS
   - Support for RFC-9209 Quota spec update
   - Support for RFC-6154 Special use
   - Support for RFC-2971 IMAP ID spec
   - Support for RFC-7889 APPENDLIMIT
   - Support for RFC-7889 Special use
   - Support for RFC-8437 UNAUTHENTICATE
   - Support for RFC-8474 OBJECTID
   - Support for RFC-8508 IMAP REPLACE (#1354)
   - Fix RFC-5464 Capability typo (METADATA)
   - Support for XLIST
 - JAMES-3850 Implement urn:ietf:params:jmap:quota draft
 - JAMES-3842 Access SSLSession from Hooks and Handlers
 - JAMES-3292 More powerful task listing (#1263)
 - [REFACTORING] More flexible fetch groups for Mailbox metadata (#1347)
 - JAMES-3831 Implement urn:apache:james:params:jmap:mail:identity:sortorder extension
 - JAMES-3826 Introduce loading additional healthcheck mechanism
 - JAMES-3825 Task to clean up tasks (#1208)
 - JAMES-2656 - Add initial JPAMailRepository implementation (#1176)
 - JAMES-3885 - Change username (#1489)
 - JAMES-3893 Add a WebAdmin API allowing managing user identity
 - JAMES-2601 Implement a LDAP healthCheck (#1453)
 - JAMES-3419 JMAP EmailBodyPart individual headers (#1433)
 - JAMES-3379 Support header:Name:all syntax (JMAP)
 - JAMES-3880 MailRepositories: add a way to count retries (#1411)
 - JAMES-3756 JMAP APIs to manage delegation
 - JAMES-3867 Allow IMAP extensions configuration
 - JAMES-3533 JMAP: Email/parse
 - JAMES-3822 RFC-4865 Implement delayed sends in SMTP
 - Support `--generate-keystore` when running Guice James server (#1525)
 - [FIX] Improve code coverage for IMAP
 - JAMES-3905 LDAP should allow per user base DN (#1540)
 - JAMES-3904 Support LDAPS (#1536)
 - JAMES-3292 More filters for listing webadmin tasks (#1520)
 - JAMES-3899 WithStorageDirective: Support flag storage directives

### Changes

 - JAMES-3774 Migrate to Cassandra driver 4
 - JAMES-3711 Migrate to OpenSearch instead of ElasticSearch
 - JAMES-3797 Move SpamAssassin as an extension
 - JAMES-3775 Move ClamAV as an extension
 - JAMES-3711 Move ElasticSearch as a separate extension (#1110)
 - JAMES-3773 Migrate from Schedulers.elastic() to Schedulers.boundedElastic()
 - JAMES-3799 Get rid of Body FetchType
 - JAMES-3809 Get rid of cassandra.properties chunk.size.message.read in favor of batchsize.properties (#1148)
 - JAMES-3804 Improve error handling when mailetContainer misses a processor
 - JAMES-3806 S3BlobStoreDAO logs missing blob id if not found
 - JAMES-3799 Optimize memory requirements of SimpleMessageSearchIndex
 - JAMES-3792 Remote and Local delivery should log MIME MessageId
 - JAMES-3776 SMTP should log MIME MessageId in MDC
 - JAMES-3786 Mailbox index could support dedicated language (example)
 - JAMES-3774 JVM properties: io.netty.leakDetection.level
 - JAMES-3390 Allow Email/query to put inMailbox in its top level AND operators (#1060)
 - [Upgrade] Multiple dependencies updates
 - JAMES-3772 Migrate rabbitMQ client from commons-pool2 to reactor-pool
 - JAMES-3737 Reactive IMAP
 - JAMES-3747 Metrics for RabbitMQ channelPool
 - JAMES-3715 Migration to Netty 4
 - JAMES-3594 ReadOnlyLDAPUser adapt log message (#925)
 - Naming threads: Spooler threads should be named
 - Naming threads: RabbitMQ driver threads should be named
 - Naming threads: ElasticSearch driver threads should be named
 - JAMES-3775 Leverage saveDate for better period filtering for Rspamd report
 - JAMES-3756 Allow the use of OIDC without virtualHosting
 - [CLEAN_CODE] Remove commons-beanutils (un-used) in protocols-imap4
 - [UPGRADE] com.fasterxml.jackson.core:jackson-databind 2.13.3 -> 2.13.4.2
 - [UPGRADE] JSieve 0.7 -> 0.8 (#1294)
 - [UPGRADE] JDKIM 0.2 -> 0.3 (#1258)
 - JAMES-3836 Rework MailRepository loading
 - JAMES-3813 Module chooser for DeletedMessagesVault on Cassandra based… (#1195)
 - [UPGRADE] MIME4J 0.8.7 -> 0.8.9
 - JAMES-3895 Automatically provision default (IMAP) mailboxes (#1476)
 - JAMES-3892 Allow configuring the count of retries in LocalDelivery (#1467)
 - JAMES-3890 Allow parallel execution of safe tasks
 - JAMES-3694 RabbitMQ: Apply queue expiracy only for per-node queues
 - JAMES-3876 Load-balancing flag for Remote Delivery Gateways (#1403)
 - JAMES-3878 - Remove icu4j dependency (#1406)
 - JAMES-3829 Drop FST (#1545)
 
### Fixed

 - JAMES-3775 ClamAV support had been fixed
 - Various build time and build stability enhancements
 - JAMES-3810 Reliability for browsing large RabbitMQMailQueue
 - JAMES-3810 Avoid unacknowledged messages in RabbitMQMailQueue
 - JAMES-3803 RemoteDelivery uses different scheduler for dequeuing
   - (this could cause ActiveMQMailQueue to hang under load)
 - JAMES-3798 fix bounce exception when no date header is present (#1107)
 - JAMES-3791 Wrong sender for RemoteDelivery under load
 - Add a vhost configuration option to RabbitMQ
 - [IMPROVEMENT] Prevent RMI from doing System.gc every hour
 - [FIX] NettyImapSession race conditions
 - JAMES-2146 James should exit when startup sequence fails
 - JAMES-3783 (JMAP) multipart/alternative should display text/html last
 - JAMES-3773 Acquiring the Mailbox path lock on another dedicated Scheduler
 - JAMES-343 Mail: Fix resetting DSN parameters
 - JAMES-3431 Stricter validation for DSN ENVID (#1002)
 - JAMES-3751 IMAP SEARCH was ignoring MODSEQ for last range
   - Tests for RFC-4731 RFC-5182
 - JAMES-3753 Fix FlowedMessageUtils.deflow() (#972)
 - JAMES-3737 Don't encapsulate a Cassandra query in a lock
 - JAMES-3744 remove redundant hashmaps which lead to OOM
 - JAMES-3743 Mailbox/get failure upon negative quotas (#955)
 - JAMES-3733 Support multi EventBus when re-deliver events from the dea… (#933)
 - JAMES-3729 LocalDelivery error handling is non-standard (#936)
 - JAMES-3439 Email/set create should encode attachments in base64
 - JAMES-3722 Parse an arbitrary number of IMAP FETCH modifiers
 - JAMES-3722 SELECT do not supports CONDSTORE to be immediately followed by a ')'
 - JAMES-3722 SELECT + QRESYNC did not comply with formal syntax + Fix for IMAP SELECT QRESYNC known sequences application
 - [FIX] UidValidity generate fails for Long.MIN_VALUE
 - JAMES-3715 Fix a data race upon IDLE
 - JMAP Email/set create should use tooLarge when attempt to create an oversize mail (#918)
 - JAMES-3754 Date searching should align IMAP4rev2 specifications (#1360)
 - [FIX] EmailBodyPart filename should fallback to Content-Disposition field (#1348) (JMAP)
 - JAMES-3461 - Fix Mailbox/changes do not take isSubscribe changes into account (#1320)
 - JAMES-3861 EmailDelivery push selection for JMAP should be used only upon delivery, not for mail store interactions
 - JAMES-3852 Support subscriptions management on delegated mailboxes
 - [JMAP] Email/set create should add missing mimeMessageId and sentAt (#1286)
 - [FIX] Prevent stack overflow in FETCH command (#1289)
 - JAMES-3825 Cancel tasks upon graceful shutdown - waiting to the cancelled listener is completed (#1272)
 - JAMES-3827 Support From fields without domain parts in the email address (#1271)
 - [BUG] Handle CALENDAR objects without VEVENT gracefully (#1270)
 - JAMES-3811 Ability to cancel IMAP request execution upon closed connections (#1267)
 - JAMES-3840 Sanitize UTF-8 string after splitting (#1266)
 - JAMES-3835 EmailSubmission/set response is wrong (#1254)
 - JAMES-3828 Fix AttributeValue serialization for calendar related mailet
 - [FIX] S3: apply timeouts configuration (#1202)
 - JAMES-3898 VacationMailet should Q-Encode special characters (#1508)
 - [FIX] Subscribe/unsubscribe mailbox should be per user based (#1491)
 - [FIX] Prevent sending invalid content type upon downloads
 - JAMES-3440 Fixbug: Data race issue with JMAP email query view
 - JAMES-3440 Email/query (EmailQueryView usage case) should filter share mailboxes
 - JAMES-3886 Handle Mailbox counter updates failures more gracefully (#1445)
 - JAMES-3887 Provisioned mailboxes should be subscribed (#1444)
 - JAMES-3881 Docker packagings should locate jvm.properties
 - JAMES-3884 One ImapProcessor set per IMAP server (#1442)
 - [FIX] Misleeading error log in Host class (#1441)
 - [FIX] JMAP urn:ietf:params:jmap:submission submissionExtensions shoul… (#1424)
 - [FIX] OidcJwtTokenVerifier: be resilient upon missing kid
 - Don't add gateway if no gateway is configured (empty value)
 - JAMES-2791 Avoid incoherent mail repository count (#1537)
 - JAMES-3901 OpenSearch indexing should tolerate bad URL encoding for C… (#1527)
 - JAMES-3900 Ignore errors upon task manager polling updates (#1523)
 
### Documentation

 - JAMES-3734 Document database benchmark methodologies and base performances (#937)
 - [DOCUMENTATION] Correct Event Dead Letter webadmin routes documentation (#1184)

### Security

 - [UPGRADE] Spark 2.9.3 -> 2.9.4 (#1129)
 - JAMES-3789 Upgrade apache commons-configuration to 2.8.0
 - JAMES-3834 Configurable value for AES blobStore private key algorithm
 - JAMES-3834 Enhance UsersRepository with stronger hashing options
 - [UPGRADE] scala-library 2.13.7 -> 2.13.9 (#1210)
 - [UPGRADE] Bump jsoup from 1.15.1 to 1.15.3 (#1178)
 
### Performance

Multiple performance enhancements for Distributed server mailbox, IMAP, SMTP and JMAP.
 
 - JAMES-3773 Slightly improve S3BlobStoreDAO::readReactive
 - [FIX] MailboxFactory should not block when parentId
 - [PERF] RequestObject::using is better suited as a set
 - [PERF] Slightly improve Email/query deserialization
 - [PERF] Optimize further response serialization
 - [PERF] Optimize further Mailbox/get serialization
 - [PERF] Optimize further Email/get serialization
 - Tiny performance enhancement for JMAP RFC-8621
 - [REACTOR] TerminationSubscriber don't require a subscriber switch
 - [REACTOR] Reactify MessageManager::delete
 - [REACTOR] JMAP RFC-8621: Reactify Mailbox/set deletion
 - [REACTOR] JMAP RFC-8621: Reactify Email/set massive operations
 - [REACTOR] JMAP RFC-8621: Remove unneeded subscribeOn calls
 - [REACTOR] JMAP RFC-8621: Remove unneeded subscribeOn calls
 - [PERF] Rely more on UnsynchronizedByteArrayOutputStream
 - [PERF] MimeMessageInputStreamSource: prefer FileInputStream VS SharedFileInutStream
 - [PERF] Store byte sources should come up with pre-computed size
 - [PERF] Improve SMTP performance
 - [PERF] IMAP FETCH headers: Use a simpler BodyDescriptor builder
 - [PERF] ChannelImapResponseWriter: avoid double literal inputStream computation
 - [PERF] Reduce MimeBodyElement memory allocation
 - [PERF] Fasten BodyOffsetInputStream
 - [PERF] JMAP RFC-8621 Email/get full: do not parse mime message twice
 - JAMES-3765: Improve some IMAP commands (STORE, COPY, MOVE) performance
 - JAMES-3737 RabbitMQ unbinding is potentially blocking
 - [PERF] S3BlobStoreDAO: readBytes copies too much data
 - JAMES-3719 Reactive textual content extraction with Apache Tika
 - [PERF] Decode UTF-7 only if needed
 - [PERF] IMAP avoid memory allocation when parsing STATUS items
 - [PERF] IMAP improve status items parsing
 - [PERF] IMAP improve flags parsing
 - [PERF] IMAP use constants for CharValidator where immutable
 - [REFACTORING] Use Splitter::splitToStream (#989)
 - JAMES-3752 Allow disabling ImapChannelUpstreamHandler heartbeat handler
 - JAMES-3740 Compact primitive collections for UID <-> MSN mapping
 - JAMES-3749 Allow disable durability, publish confirms (RabbitMQ)
 - JAMES-3744 Generify UriPathTemplate and provide a non regex alternative
 - JAMES-3745 Use FastByteArrayOutputStream as a short lived object (#958)
 - [PERF] Reuse JwtTokenVerifier parsers (#956)
 - [PERF] Thread configuration for WebAdmin, redis and some reactor improvments (#950)
 - [PERF] IMAP LIST: Avoid potentiallyexpensive REGEX when not needed
 - JAMES-3715 Schedule IMAP IDLE heartbits on the Netty Event loop (#948)
 - [PERF] Allow disabling host information in protocol MDC (#928)
 - JAMES-3433 MimeMessageStore StoragePolicy should be the same on read and writes
 - [FIX] Avoid using LWT on non critical tables (#1356) (messagev3, mailbox)
 - [PERF] Allow disabling SERIAL read for non critical UID/ModSeq read operations
 - [PERF] Improve Cassandra rows interpretation in mailbox/cassandra
 - [PERF] Adopt CqlIdentifier accross the project
 - [PERF] More flexible fetch groups for Mailbox metadata (#1347)
 - JAMES-3863 JMAP OPTIONS should support caching
 - [PERF] Reduce memory allocation upon S3BlobStoreDAO::save(InputStream) (#1334)
 - JAMES-3828 Smarter duplication for AttributeValue
 - JAMES-3793 Add a setting to prevent S3BlobStoreDAO load too big objects in memory
 - [PERF] Improve Mailbox/get algorithm (#1164)
 - JAMES-3793 Prevent needless defensive copies within S3BlobStoreDAO (#1147)
 - JAMES-3793 Prevent Bytes concat upon reading FULL messages (#1152)
 - [FIX] Reactify attachments
 - JAMES-2643 Optimize listing mapping by destination on top of JPA (#1472)
 - JAMES-3882 Move messages into DeletedMessageVault asynchronously for … (#1436)
 - [PERF] JsoupHtmlTextExtractor without recursion (#1422)
 - [PERF] Fasten IMAP vanished
 - [PERF] Use UnsynchronizedBufferedInputStream
 - [PERF] UidMsnConverter should not call contains for the last UID
 - [PERF] Avoid converting mailbox list to map for all mailboxes (#1383)
 - JAMES-3872 Add a FetchType that support getting the attachments' metadata without fetching the body content
 - JAMES-3870 Group IMAP response line within TCP packets (#1364)
 - [PERF] Disable JMAP related listeners if JMAP is disabled
 - [PERF] Allow to disable ACLs
 - [PERF] Optimize IMAP ESEARCH options
 - JAMES-3777 Event sourcing snapshot, projection and incremental changes for JMAP filters
 - JAMES-3900 Snapshots for polled updates (#1533)
 - JAMES-3440 RFC-8621 support for emailQueryView before + inMailbox sorted by receivedAt
 - JAMES-2080 Allow turning off header indexing in OpenSearch (#1516)

## [Unreleased 3.7.x]

No changes yet.

## [3.7.6] - 2025-02-05

### Security

- **CVE-2024-37358**: Denial of service through the use of IMAP literals
- **CVE-2024-45626**: Denial of service through JMAP HTML to text conversion

- [FIX] Prevent HtmlTextExtractor to generate asymmetric outputs
- [IMPROVEMENT] Better manage IMAP literals (3.8.x) (#2281)

### Bug fixes

 - [BUILD] Fully drop glowroot
 - [FIX] Solve weave/rest-smtp-sink: Docker image manifest v2 schema 1 deprecation issue (#2152)
 - JAMES-3955 Increase consumer timeout for TaskManagerWorkQueue
 - JAMES-3955 WARNING logs upon closing channels
 - JAMES-3955 Applicative timeouts before Rabbit timeouts
 - JAMES-3955 Check for queues without consumer and resume consumption as needed

## [3.7.5] - 2024-02-23

### Security

- **CVE-2023-51747**: SMTP smuggling in Apache James
- **CVE-2023-51518**: Privilege escalation via JMX pre-authentication deserialisation
- [FIX] JMX password auto-detection
- [FIX] Enforce CRLF as part of SMTP DATA transaction (#1876)
- [FIX] Set up JMX auth for Spring
- [FIX] Set up JMX auth filter for Guice

### Enhancements


- JAMES-3604 Enable to use quorum queues onto DTM work queue (#1714)
- JAMES-3934 Allow external scheduling for RabbitMQ mailQueue browse st… (#1682)
- JAMES-3918 Force deletion of user mailboxes (#1608) (#1611)

## [3.7.4] - 2023-03-20

### CVE-2023-26269: Privilege escalation through unauthenticated JMX

Apache James distribution prior to release 3.7.4 allows privilege escalation through the use of JMX.

*Severity*: Moderate

*Mitigation*: We recommend turning on authentication on. If the CLI is unused we recommend turning JMX off.

Release 3.7.4 set up implicitly JMX authentication for Guice based products and addresses the underlying JMX exploits.

Upgrading to Apache James 3.7.4 is thus advised.

### Security

 - JAMES-3881 WARN if no JMX authentication is setup
 - JAMES-3881 Enable JMX password generation by default (guice)
 - JAMES-3881 Set a JMX password
 - JAMES-3881 -Djmx.remote.x.mlet.allow.getMBeansFromURL=false (#1460)
 - [UPGRADE] commons-fileupload 1.4 -> 1.5 (#1462)
 - JAMES-3881 Prevent CommonsBeanutils1 deserialization exploit [BACKPORT] (#1455)
 - JAMES-3881 Unregister LOG4J MBeans (#1459)

### Fixes

 - JAMES-3891 Graceful shutdown for queue consumers [3.7.x] (#1479)
 - JAMES-3881 Docker packagings should locate jvm.properties
 - JAMES-3891 ActiveMQCacheableMailQueue: discard emails not backed by a blob (#1464)
 - JAMES-3892 Allow configuring the count of retries in LocalDelivery [BACKPORT] (#1469)
 - JAMES-3886 Handle Mailbox counter updates failures more gracefully [3.7.x] (#1449)
 - JAMES-3875 JDBCMailRepository, cut off to long error messages

### Changes

 - JAMES-3890 Allow parallel execution of safe tasks (backport)

## [3.7.3] - 2022-12-30

### Security

 - CVE-2022-45935: Temporary File Information Disclosure in Apache JAMES
 - [UPGRADE] commons-text 1.9 -> 1.10 (#1291)
 - JAMES-3832 RemoteDelivery will do TLS host name verification when contacting remote mail servers
 - JAMES-3860 Rely on Files.createTempFile (#1325)

### Fixes

 - [FIX] Unregister gauge upon shutdown (#1251)
 - JAMES-3862 Switch to SLF4J 2.0.x compatible Log4j Adapter. Copy log4j-core dependency jar to appassembler lib dir. (#1333) (#1335)
 - JAMES-3859 start sequence ordering should take provisions into account
 - [FIX] ToSenderFolder needs to call .block on mono (#1317)
 - [BUILD] Fix SpamAssassin container on 3.7.x (#1330)
 
## [3.7.2] - 2022-10-06

### Security 

 - [UPGRADE] scala-library 2.13.7 -> 2.13.9
 - [CONF] Remove vendor URLs from smtpserver.xml default configuration examples (#1205)
 - [Upgrade] Maven slf4j 1.7.32 -> 2.0.1
 - [Upgrade] Maven netty4 4.1.72.Final -> 4.1.81.Final
 - [Upgrade] Maven logback 1.2.10 -> 1.4.0
 - [Upgrade] Maven org.jsoup:jsoup 1.14.3 -> 1.15.3
 - [Upgrade] Maven com.fasterxml.jackson.dataformat:jackson-dataformat-cbor 2.13.1 -> 2.13.4
 - [Upgrade] Maven org.apache.activemq:activemq-broker 5.16.3 -> 5.17.2
 - [Upgrade] Maven org.apache.commons:commons-configuration2 2.7 -> 2.8.0

### Fixes

 - JAMES-3810 Decrease slice browsing concurrency
 - JAMES-3810 Dequeuer should nack when fails to see if email was deleted
 - JAMES-3744 remove redundant hashmaps which lead to OOM
 - JAMES-3753 Fix FlowedMessageUtils.deflow() (#972)
 - JAMES-343 Mail: Fix resetting DSN parameters
 - JAMES-3431 Stricter validation for DSN ENVID (#1002)
 - JAMES-3803 RemoteDelivery uses different scheduler for dequeuing (#1121)
 - task/task-distributed - fixing NullPointerException when executeTask
 - JAMES-3784 WebAdmin: Provide RunningOptions (rateLimit) for Redeliver event task, Reprocessing mail task
 - JAMES-3784 HealthCheck /var/mail/error repository size
 - JAMES-3723 Allow to not consume emails upon reprocessing

## [3.7.1] - 2022-08-26

### Security

This release fixes CVE-2022-28220 `STARTTLS command injection in Apache JAMES`.

### Changes

 - [UPGRADE] Adopt MIME4J 0.8.7 (#961)
 - [UPGRADE] jackson 2.13.1 -> 2.13.2.2 fixes CVE-2020-36518 [3.7.x] (#982)

### Fixed 

 - JAMES-3720 Fix temporary file leaks in multiple places
 - JAMES-3731 Fix default configuration for rabbitmq regarding distributed images (#938)
 - JAMES-1862 Fix several issues with STARTTLS command injection detection [BACKPORT]
 - JAMES-3746 Backport memory leak for IMAP IDLE [3.7.x] (#962)
 - JAMES-3787 RemoteDelivery: Error upon enqueue lead to email loss
 - JAMES-3791 Remote Delivery uses a pool of SMTP sessions.
 - [3.7.x] Update docker-compose sample - Remove entrypoint
 - JAMES-3801 Nack errors upon dequeue
 - JAMES-3800 S3BlobStoreDAO should be explicit upon future cancellation
 - JAMES-3738 Generify encryption management in protocols

### Recommended upgrades

- Upgrade docker relevant: rabbitmq -> 3.9.18
- Upgrade docker relevant: tika -> 1.28.2 
- Upgrade docker relevant: spamassassin -> 3.4.6-1 T
- Upgrade docker relevant: nginx -> 1.22 

## [3.7.0] - 2022-03-01

### Added
 - JAMES-3524 Support symmetric encryption support on top of BlobStore
 - JAMES-3516 Work started toward supporting threads. (see upgrade instructions)
 - JAMES-2157 Introduce a HasMimeTypeAnySubPart matcher
 - JAMES-3588 Allow LMTP to be configured to execute the mailetcontainer (per recipient or grouped executions available)
 - JAMES-3491 JMAP: Configurable websocket url for JMAP configuration
 - JAMES-3574 LMTP: regular stack also should execute Message hooks
 - JAMES-3574 Test suite for LMTP protocol
 - JAMES-3581 JMAP: Allow writing custom State changes
 - JAMES-2330: Give ability to override AbstractConfigurableAsyncServer.buildSSLContext
 - JAMES-3573 Allow specifying DC in Cassandra configuration
 - JAMES-3520 JMAP - Implement MDN - RFC-9007 (#385)
 - JAMES-3532 JMAP: Implement Email/import
 - MAILBOX-405 CreateMissingParentsTask implementation
 - JAMES-3316 Allow to write custom extensions in JMAP session
 - JAMES-3534 JMAP support for Identity/set (part of RFC-8621)
 - JAMES-3487 Java property: MimeMessageInputStreamSource THRESHOLD (#755)
 - JAMES-3673 Separate trust store for S3 (#751)
 - JAMES-3671 Added glowroot instrumentation for POP3 protocol
 - JAMES-3670 Configurable restore location for messages from the Deleted Messages Vault
 - JAMES-3667 Add a WebAdmin route for verifying a user password. (#741)
 - JAMES-3669 Add an option to delay protocol responses on authentication failure, as basic protection against brute-force attacks (#746)
 - JAMES-3668 Added utility to load extra system properties from a configuration file on server start (#744)
 - JAMES-3539 Implement JMAP PushSubscriptions (as per RFC-8620)
 - JAMES-3440 EmailQueryView support for sort by receivedAt (#710)
 - JAMES-3078 Allow to disable user provisioning for JMAP (#708)
 - JAMES-3674 Support salting on top of James user password storage. See related upgrade instructions.
 - JAMES-3639 Allow to configure JMAP crypto with raw public key (#695)
 - JAMES-3657 Modular entity validation for webadmin-data (#678)
 - JAMES-3150 Garbage collection for deduplicated blobs using bloom filters
 - JAMES-3588 Mailet to propagate encoutered error (#655)
 - JAMES-3645 Allow RemoteDelivery to use SMTPS and fallback to SMTP (#632)
 - JAMES-3639 Allow use PEM keys for SSL, JMAP
 - JAMES-3640 Auto generate demo SSL PEM keys
 - JAMES-3638 Allow use PKCS12 keystore for SSL (#625)
 - JAMES-3297 Publish the number of items currently in the mailet pipeline as a metric (#650)
 - JAMES-3623 Provide a (multi-DC firendly) Distributed POP3 Application
 - JAMES-3544 Clean task for JMAP uploads
 - JAMES-3621 WebAdmin task to clear the content of a mailbox
 - JAMES-3516 Implement JMAP Thread/get (RFC-8621)
 - JAMES-3621 Mailbox webadmin routes - unseenMessageCount + messageCount
 - JAMES-3622 Compatibility with Cassandra 4.0.0
 - Adopt Scala checkstyle
 - JAMES-3618 Support LDAP for JPA guice based apps
 - JAMES-3608 email.send.max.size option for JMAP
 - JAMES-3610 SMTP/IMAP: unit for size related options
 - JAMES-3605 Reconnection handlers for RabbitMQ consumers
 - JAMES-3604 RabbitMQ connections should be cluster aware
 - JAMES-3607 Functional healthcheck exercising mail reception
 - JAMES-3693 Extension: mailets for rate limiting (Redis/Memory)
 - JAMES-3711 Implement a Requeue mailet
 - JAMES-3680 Implement support for OAUTH SASL authentication for IMAP and SMTP
 - JAMES-3680 Allow to disable SMTP/IMAP plain auth, SMTP requireSSL setting
 - JAMES-3680 LDAP support for James memory server
 - JAMES-3687 MailQueue written on top of Apache Pulsar
 - JAMES-3680 JMAP: Modularize authentication strategies and provide a XUserAuthenticationStrategy
 - JAMES-2912 RemoteDelivery: add a onSuccess processor (#776)
 - JAMES-3674 Support PBKDF2 as a strong password-hashing algorithm
 
### Removed
 - JAMES-3578 Drop Cassandra schema version prior version 8 (see upgrade instructions)
 - JAMES-3596 Drop spring app WAR plugin
 - JAMES-3621 Drop benchmarks and debian/rpm packaging (was unmaintained and broken)
 - Drop Swagger (#706)
 - JAMES-3646 Remove Maildir implementation (#661)
 - Remove GroupMembershipResolver and implement (#670)
 - JAMES-2979 Remove FileMailQueue (#660)
 - JAMES-3631 Drop no longer used MailRepository tables
 - Terminate Apache James HUPA
 - [REFACTORING] Remove unused BayesianAnalyzer and related class (#526)
 - JAMES-3261 Remove unused MPT scripts within /server/apps/spring (#775)
 
### Changed
 - JAMES-3621 Re-organise server application
   - Use JIB for docker images
   - Guice application should have a ZIP distribution
   - Collocate applications in server/apps folder
   - Example of JPA driver customization
   - Drop no longer needed projects
   - Add some manifest entries when using maven-jar-plugin (#510)
   - Fix related website links
   - Docker: expose volumes used for persistence with volumes
 - JAMES-3261 Update glowroot to 0.13.6
 - JAMES-3591 Warn against CassandraBlobStore usage (can be bypassed via an environment variable)
 - Upgrade to MIME4J latest 0.8.4 release (#459)
 - Cassandra implementation should depend on interfaces and ModSeqProvider
 - JAMES-3587 Deprecate MDCBuild::addContext (relies on potentially expensive implicit toString calls)
 - JAMES-3567 Distributed server should not rely on ActiveMQ
 - Adopt MIME4J 0.8.6 (#682)
 - UPGRADE jackson-dataformat-cbor 2.10.4 -> 2.11.4 (ES 7 driver) (#662)
 - JAMES-3647 Adopt eclipse-temurin:11-jre-focal docker image (#652)
 - JAMES-2968 Move "Time Spent in IMAP-*" Log Entry from INFO to DEBUG
 - JAMES-2287 Encode BlobId with base64 (#572)
 - JAMES-1862 IMAP plainAuthDisallowed should be true by default
 - [UPGRADE] Security upgrade: JSOUP 1.14.1 -> 1.14.2 to address CVE-2021-3771
 - [UPGRADE] Security upgrade: common-compress to 1.21
 - JAMES-2625 Remove stacktrace upon ClosedChannelException
 - JAMES-3261 Add some system properties for TLS (#588)
 - Multiple miscaleneous dependency upgrades
 - JAMES-3708 Stricter domain and address parsing
 - JAMES-3709 Refactoring POP3 command handlers to share common code
 - JAMES-3705 Refactoring POP3 handlers to be more extensible
 - JAMES-3704 Improve mail diagnostics on system edge
 - JAMES-3694 Add a TTL on RabbitMQ queues (optional)
 - JAMES-3690 Allow to restrict the host webadmin is listening on
 - JAMES-3680 JMAP capability should be aware of request URL prefixes (#807)
 - JAMES-3261 Add /root/extensions-jars as a volume for improved discovery (#774)
 - JAMES-3679 Set mailbox recent gc_grace_second to zero (#768)
 
### Performance
 - JAMES-3466 Provision default mailboxes only when listing all mailboxes
 - JAMES-3576 Further denormalize message table (see related upgrade instructions)
 - ResultUtils::haveValidContent is doing needless work
 - Optimise GetMessagesMethod::messagesNotFound
 - SimpleMailboxMessage: userFlags assignment was done twi
 - Prefer DefaultMessageBuilder for Mime message parsing
 - Pre-convert Cassandra rows to lower case (#501)
 - JAMES-3603 AmqpForwardAttribute should reuse RabbitMQ connections
 - ImapDateTimeFormatter can be static
 - JAMES-3491 Do not send two JMAP events upon new messages
 - JAMES-3599 Deliver events to all groups at once to reduce deserialization and messaging overhead
 - Remove regular expression usages where possible
 - Avoid doing JWT parsing twice
 - JAMES-3028 Do not execute downstream requests on AWS driver pool (#485)
 - JAMES-2989 JMAP Preview should not normalize spaces of the entire body (#479)
 - Use buffer output stream upon MessageManager::append
 - Reduce Cassandra chunk length for some read intensive tables
 - JAMES-3594 Migrate to UnboundId as a LDAP implementation. This allows performance gains. See related upgrade instructions.
 - General review of our reactive flows:
   - End to end reactive calls for both JMAP draft and JMAP RFC-8621
   - Migrate where possible from Mono.fatMap to flatMapIterable
   - MailboxChangeListener should be fully reactive
   - Bond Reactive listeners as Reactive
   - JAMES-2393 Allow writing reactive eventSourcing subscribers
   - GroupRegistration was doing (blocking) acks on the parallel pool
   - RabbitMQ receivers can be blocking
 - JAMES-2683 RabbitMQ: Use a single connection per James node
 - MailboxACL.union shortcuts
 - Cassandra: Use static TypeToken for complex CQL types
 - JMAP Draft JSON: Cache and reuse Object mappers for writing JMAP responses (#440)
 - Keywords::fromFlags should avoid intermediate collections
 - JsoupHtmlTextExtractor should use Collectors.joiner
 - JMAP draft was parsing, formatting then re-parsing JSON
 - JAMES-3028 Allow setting up S3 HTTP concurrency at the Netty level
 - JAMES-3028 S3Client should not be pooled
 - JAMES-3586 Cassandra BlobStore: Use LOCAL_ONE for optimistic consistency downgrades
 - JAMES-3107 Deprecate log p99 due to its performance impact
 - JAMES-3107 Switch to HDR histograms
 - Mailboxes metadata: Avoid O(n2) algorithm to compute hasChildren
 - JMAPServer should generate JMAP routes once
 - MessageViewFactory::toHeaderMap was unfolding headers twice
 - MessageResultImpl should use underlying MailboxMessage
 - FlagsFactory::createFlags needlessly call the builder
 - JAMES-3171 Mailbox/get + ids: Avoid reading subscriptions for all mailboxes
 - CassandraSubscriptionMapper should prepare its statements
 - CassandraMailboxSessionMapperFactory should not instantiate one mapper per request
 - Reactor: favor error suppliers (this avoids needlessly filling stacktraces)
 - JAMES-1965 JMAP Draft MessageFullViewFactory: Avoid performing HTML text extraction if not needed
 - JAMES-3407 Applicative read-repair: draw a random number only if needed
 - DefaultMailboxesProvisioner: Avoid re-opening a session
 - JMAP: Avoid MIME re-parsing when sending messages
 - JAMES-3078 Continuation Token signing was done on the Netty event loop thread
 - JAMES-3467 Avoid loading all domains for auto-detection when auto-detection is off
 - JAMES-3435 Cassandra: Allow to avoid LWT for messages operations via message.write.strong.consistency.unsafe
 - [PERFORMANCE] MessageUid::compareTo should not box values (#764)
 - [PERFORMANCE] MessageFullViewFactory should not always evaluate textual content of the message
 - [PERFORMANCE] defer expensive Monos in switchIfEmpty
 - [PERFORMANCE] Limit context switches
 - [PERF] Reactify UsersRepository::contains (#704)
 - JAMES-3652 Avoid locking in protocol task execution (#666)
 - JAMES-3196 Avoid MDC costs when not needed (#667)
 - JAMES-3630 quotaDetailsReactive should group quota limit reads
 - JAMES-3626 Better handle tombstones due to Cassandra empty collections
 - JAMES-3627 Prepared statements for applicable flags
 - [REFACTORING] Remove more REGEX usages (#587)
 - JAMES-3629 enqueuedMailsV4 to use frozen collections
 - [PERFORMANCE] IndexableMessage text field is never used
 - [PERFORMANCE] AttributeValue: object mapper can be static
 - JAMES-3613 IMAP + SMTP should compute transport MDC upon connection
 - [PERFORMANCE] DropWizardMetricFactory: optimize wrapping monos
 - [PERFORMANCE] Record SetMessagesProcessor metrics if executed
 - [PERFORMANCE] SetMessagesMethod metrics are redundant with processor one
 - [PERFORMANCE] Use lenient parsers for MIME4J in more places
 - [PERFORMANCE] CreationMessage: Simplify assertAtLeastOneValidRecipient
 - [PERFORMANCE] SetMessagesUpdateProcessor processing can be done lazily
 - [PERFORMANCE] Optimize Username parsing
 - [PERFORMANCE] JMAP: Fasten accept header parsing
 - [PERF] Re-use a regex in FileSystemBlobStrategy
 - JAMES-3713 Enable rules caching for DLP
 - JAMES-343 Performance: Use ImmutableMap copy upon DSN parameters lookup (#881)
 - [PERF] Improve reactive code for LocalDelivery
 - [PERF] AutomaticallySentMailDetectorImpl should rely on a lenient parser

### Fixed
 - JAMES-3589 Fix mailet processing logic upon partial matches by dropping Apache Camel mailetcontainer implementation
 - JAMES-3491 WebSocket should unregister resources on cancels
 - JAMES-3261 JPA-SMTP app: add missing loagback declarations
 - JAMES-3601 [ADR] Distributed Mail Queue Cleanup is now fully implemented
 - JAMES-3600 All JMAP calls should position Content-Length
 - JAMES-3597 JMAP: Exclude deleted messages from JMAP Email/query
 - JAMES-3594 Decrease verbosity of bad credential auth failures
 - JAMES-3595 Spooler processing starts before mailetContainer initialisation
 - JAMES-3492 ElasticSearch: Do not create indices if it already exists
 - JAMES-2886 Fix collection handling for PropertiesProvider
 - JAMES-2813 Long running tasks on the MemoryTaskManager generates stackTraces
 - JAMES-3592 Maildir tests (Unit and MPT) are not representative of Spring product
 - JAMES-3107 Fix some zeroed metrics
 - JAMES-3579 reject verifyIdentity param to true when authRequired is false in SMTP server configuration
 - JAMES-3491 JMAP webSocket can be used to mix responses and state changes
 - JAMES-3467 Domain cache should be refreshed periodically under read load
 - JAMES-3571 MimeMessageWrapper getSize was incorrect for empty messages
 - JAMES-3567 S3: explicitly specify version for netty-codec-http
 - JAMES-3569 preserves all email propertis on recipient rewrite
 - JAMES-3557 */changes: Fail explicitly when too much entries on a single change
 - JAMES-3558 JMAP Email/changes: moves should be considered as updates
 - JAMES-3525 Verify identity should also apply for unauthenticated users
 - JAMES-3458 JMAP Identity/get should support ids field
 - JAMES-3556 JMAP eventUrl s/closeAfter/closeafter/ (#379)
 - JAMES-3432 Upload routes was unstable (#374)
 - JAMES-3554 JMAP: remove pushState field from Server Sent Events
 - JAMES-3481 s/maxChanged/maxChanges
 - JAMES-3553 Disable read_repair_chance & read_repair_chance on table creation
 - JAMES-2884 Email/query s/comparator/sort/
 - JAMES-3256 Dis-ambiguate MailDispatcher error logs
 - JAMES-3522 JMAP routes should position WWW-Authenticate
 - JAMES-3677 BackReference should allow pointing to specific array elements (#765)
 - JAMES-3613 Avoid a NPE due to IMAP MDC (#766)
 - JAMES-2557 Sieve should cleanup email after sending them (#743)
 - JAMES-1618 Fix manage sieve implementation and test it with Thunderbird (#742)
 - JAMES-3600 Fix check Content-Length when ProvisioningTest (#738)
 - JAMES-3666 Fix DSNBounce exception when no Date header is present
 - JAMES-3477 Some email sent via the mailet context were never disposed (#712)
 - JAMES-1930 Configure Memory Users repository in Memory APP (#709)
 - JAMES-3516 Fix Thread/get error management
 - JAMES-3662 Accept CORS headers without the JMAP API restriction on "Accept" headers (#699)
 - JAMES-3369 JMAP EMail/get Fallback to text/plain when no HTML in multipart/alternative (#698)
 - JAMES-3661 Email/* should handle quota exceptions (#696)
 - Fix pom relativePath parent of apache-mailet-test module
 - JAMES-3660 Cassandra mailbox creation unstable when high concurency (#686)
 - MAILBOX-333 Avoid overQuotaMailing failures when no size limit (#676)
 - JAMES-1436 SwitchableLineBasedFrameDecoder: clean up cumulation buffer (#673)
 - Fix invalid json scope for james-json test-jar (#672)
 - JAMES-3655 Fix Quota extensions with delegation
 - JAMES-3477 Mail::duplicate did lead to file leak in various places (#668)
 - JAMES-3640 No longer ship crypto materials in default configuration
 - JAMES-3150 S3BlobStoreDAO listBlob paging (#643)
 - PROTOCOLS-106 CRLFTerminatedInputStream should sanitize lonely \n delimiters
- JAMES-3646 Sanitize some File based components  
   - FileMailRepository shoud reject URL outside of James root
   - SieveFileRepository should validate underlying files belong to its root
- JAMES-1862 Generalize STARTTLS sanitizing fix
- JAMES-1862 Prevent Session fixation via STARTTLS
- JAMES-3634 + JAMES-3635 Apply fuzzing to Apache James
   - Upgrade PrefixedRegex to RE2J
   - Fuzzed input throws String out of bound exception for FETCH
   - Prevent String OutOfBoundException for IMAP APPEND
   - Prevent infinite loop for IMAP STATUS command parser
   - Prevent infinite loop for IMAP APPEND command parser
- MAILBOX-347 NONE Password hashing is actually replace the password with a fixed string (#641)
- PROTOCOLS-118 Fixed continuation request not getting recognised by some clients (#640)
- MAILBOX-407 listShouldReturnEmptyListWhenNoMailboxes fails with NPE
- JAMES-2278 Fix the IMAP QRESYNC "out of bound" issue
- JAMES-1808 if (character > 128) should be changed to if (character >= 128) (#634)
- JAMES-1444 Using HasMailAttributeWithValueRegex matcher causes NPE during startup when JMX is enabled (#633)
- JAMES-3373 Download optional query parameters should comply with advertized URI templates
- JAMES-3440 JMAP RFC-8621: EmailQueryView position handling was wrong
- JAMES-3601 RabbitMQ mailQueue Cassandra projection: Stop browsing buckets concurrently (#577)
- JAMES-3625 JMAP session: Remove trailing / in session download url (#576)
- JAMES-3601 Bind ContentStartDAO as a singleton
- JAMES-3624 RFC 8887 (JMAP over WebSocket) Request needs property 'id' (is 'requestId')
- JAMES-3620 Memory leak at org.apache.james.protocols.smtp.core.AbstractHookableCmdHandler
- JAMES-3611 SearchUtil getBaseSubject do not sanitize empty subject
- JAMES-3714 Attachments of EML with inlined multiparts is badly handle (JMAP draft)
- [SONAR] Multiple warnings reported by Sonar had been solved
- JAMES-3708 Invalid mail address: NPE in RemoteDelivery
- JAMES-3712 Bounce should prefix bounced message
- JAMES-3709 NPE in POP3 TOP command handler
- JAMES-3432 Uploads should return JSON content type (#862)
- JAMES-3689 (MailQueue) fix removal by recipient in more cases (#825)
- JAMES-3676 SendMailHandler should manage all errors upon enqueue
- JAMES-3676 ReactorUtils.toInputStream should cancel publisher subscriptions upon partial reads
- JAMES-3678 CURRENT_HELO_MODE was also mixing connection and transaction state
- JAMES-3677 JMAP BackReference should allow pointing to specific array elements (#765)

### Documentation
 - JAMES-3405 Document Prometheus metric config (#373)
 - JAMES-3565 Documentation: fix packaging support matrix
 - JAMES-3255 Demo image now includes the james-cli utility
 - JAMES-3255 Use apache/james images
 - Better document THE release process (#337)
 - Video link for James joining ApacheCON 2021 (#663) (#740)
 - JAMES-2734 Document Sieve and ManageSieve (#745)
 - JAMES-3665 - Add Kubernetes support document (#718)
 - Split the distributed server documentation
 - Update project Roadmap
 - JAMES-3261 JPA: extra JDBC driver running the ZIPped apps (#684)
 - JAMES-3644 Document DKIM + SPF setup with James
 - Improvements of the README
 - Improvements for download page:
   - DOWNLOADS Remove warnings regarding cryptography
   - DOWNLOADS Remove archive reference from the top, fix archive link for server
   - DOWNLOADS Remove verify integrity section
   - DOWNLOADS Remove "miror"section
   - DOWNLOADS Remove dead link to Hupa
 - Imap tutorial can demo Thunderbird connection
 - Do not advertise James 2.3.2 on james.apache.org
 - [DOCUMENTATION] Fix missing content-type in upgrade schema version command
 - [DOCUMENTATION] Small polish of server/dev-build.html page (#645)
 - [DOCUMENTATION] mailbox/cassandra table structure and denormalization (#608)
 - JAMES-3389 Document email transfer routes
 - [SITE] Update copyright footer
 - [DOCUMENTATION] Install guide needs to refer to JRE 11
 - Rework James extension examples
 - Details guide to assemble your own tailor made James server
 - JAMES-3617 Document Prometheus/Grafana setup and provide dashboards (#549)
 - JAMES-3614 The homepage should comply with the ASF release policy
 - [DOCUMENTATION] Example for deploying MUA auto-configuration (#882)
 - [DOCUMENTATION] Refresh James "design goal page" (#857)
 - [CONTRIBUTING] Add a new committer section
 - JAMES-3680 Example detailing OIDC setup for IMAP, SMTP and JMAP
 - JAMES-3692 Write a security checklist (#835)
 - [SITE] Fill the security page following recent CVE announces
 
### Third party software
 - Upgrading to Apache Tika 1.26 is recommended
     - 1.25 and before are subject to CVE-2021-28657 CVE-2021-27906 CVE-2021-27807
     - 1.24 is subject to CVE-2020-9489
 - Upgrading to RabbitMQ 3.8.18 is recommended. According to [the changelog](https://www.rabbitmq.com/changelog.html) RabbitMQ prior this version is subject to several CVE:
     - https://tanzu.vmware.com/security/cve-2020-5419
     - https://tanzu.vmware.com/security/cve-2021-22117
     - https://tanzu.vmware.com/security/cve-2021-22116
     - [CVE-2021-32718](https://github.com/rabbitmq/rabbitmq-server/security/advisories/GHSA-c3hj-rg5h-2772)
     - [CVE-2021-32719](https://github.com/rabbitmq/rabbitmq-server/security/advisories/GHSA-5452-hxj4-773x)

### Miscellaneous
 - Mock SMTP version 0.5 (#733)
   - report mock email count directly instead of copy+count
   - http GET for mock email count
   - mock email DELETE returning cleared emails

## [3.6.2] - 2022-01-26

### Fixed

 - JAMES-3646 Rely on strong typing for file paths operations
 - Upgrade Log4J to 2.17.1 (CVE-2021-44832 + CVE-2021-45105)
 - Upgrade Logback to 1.2.9 (CVE-2021-42550)

## [3.6.1] - 2021-12-02

### Security

This release fixes the following vulnerability issues, that are present prior to 3.6.1:

 - *CVE-2021-38542*: Apache James vulnerable to STARTTLS command injection (IMAP and POP3)
 - *CVE-2021-40110*: Apache James IMAP vulnerable to a ReDoS
 - *CVE-2021-40111*: Apache James IMAP parsing Denial Of Service
 - *CVE-2021-40525*: Apache James: Sieve file storage vulnerable to path traversal attacks

### Fixed
- JAMES-3676 Avoid S3 connection leaks
- JAMES-3477 Mail::duplicate did lead to file leak in various places
- JAMES-3646 Sanitize some File based components  
   - Prevent directory traversal on top of maildir mailbox (#659)
   - FileMailRepository shoud reject URL outside of James root
   - SieveFileRepository should validate underlying files belong to its root
- JAMES-1862 Generalize STARTTLS sanitizing fix
- JAMES-1862 Prevent Session fixation via STARTTLS
- JAMES-3634 + JAMES-3635 Apply fuzzing to Apache James
   - Upgrade PrefixedRegex to RE2J
   - Fuzzed input throws String out of bound exception for FETCH
   - Prevent String OutOfBoundException for IMAP APPEND
   - Prevent infinite loop for IMAP STATUS command parser
   - Prevent infinite loop for IMAP APPEND command parser
- JAMES-3571 MimeMessageWrapper getSize was incorrect for empty messages
- JAMES-3525 verifyIdentity should not fail on null sender
- JAMES-3556 Fix JMAP eventUrl s/closeAfter/closeafter/
- JAMES-3432 JMAP Uploads could alter the underlying byte source
- JAMES-3537 (Email/set create should allow to attach mails)
- JAMES-3558 JMAP Email/changes: When created + updated return both
- JAMES-3558 JMAP Email/changes: moves should be considered as updates
- JAMES-3557 Changes collectors should be ordered
- JAMES-3277 Distinct uids before calling toRanges
- JAMES-3434 Refactoring: EmailSubmissionSetMethod should not rely on nested clases
- JAMES-3557 JMAP */changes: Increase default maxChanges 5 -> 256
- JAMES-3557 */changes: Fail explicitly when too much entries on a single change
- JAMES-3683 Upgrade to Log4J 2.16.0 (CVE-2021-44228 + CVE-2021-45046)

### Improvements
- JAMES-3261 ZIP packaging for Guice Apps

## [3.6.0] - 2021-03-16

### Added
- JAMES-2884 Partial Support for JMAP RFC-8621: The current implementation status allow reading mailboxes, emails, vacation responses.
  - JAMES-3457 Implement JMAP eventSource 
  - JAMES-3491 JMAP over websocket (RFC-8887)
  - JAMES-3470 JMAP RFC-8621 Email/changes + Mailbox/changes support
- JAMES-3117 Add PeriodicalHealthChecks for periodical calling all health checks
- JAMES-3143 WebAdmin endpoint to solve Cassandra message inconsistencies
- JAMES-3138 Webadmin endpoint to recompute users current quotas on top of Guice products
- JAMES-3296 Webadmin endpoint to rebuild RabbitMQMailQueue in the Distributed Server
- JAMES-3266 Offer an option to disable ElasticSearch in Distributed James product
- JAMES-3202 Reindex only outdated documents with the Mode option set to CORRECT in reindexing tasks
- JAMES-3405 Expose metrics of Guice servers over HTTP - enables easy Prometheus metrics collection
- JAMES-3407 Distributed server: Read-repairs for the mailbox entity
- JAMES-3428 Distributed server: Read-repairs for the mailbox counters entity
- JAMES-3139 Expose RabbitMQ channel & connection configuration
- JAMES-3441 Make possible and document Distributed Server setup with specialized instances
- JAMES-3337 Document the use of JWT
- JAMES-3399 Allow JSON logging with logback - enables structure logging with FluentBit
- JAMES-3396 WebAdmin should try to prevent RRT addresses redirection loops when possible
- JAMES-3402 JMAP MDN messages should have a Date header
- JAMES-3028 Distributed server: allow choosing whether blobs should be deduplicated
- JAMES-3196 CanSendFromImpl: enable to send email from aliases for SMTP and JMAP
- JAMES-3196 Add an IMAP SessionId to correlate logs
- JAMES-3502 DistributedServer: SSL and authentication support for RabbitMQ
- JAMES-3504 Metrics and log for POP3
- JAMES-3431 Optional DSN support
- JAMES-3202 Allow search index Reindexing without cleanup

### Changed
- Switch to Java 11 for build and run
- JAMES-2760 mailqueue.size.metricsEnabled should be false by default
- JAMES-3252 DomainList autoDetection should be turned off by default. Operators relying on implicit values for enabling DomainList autoDetection now needs to explicitly configure it.
- JAMES-3184 Throttling mechanism allow an admin to specify the throughput desired for a given WebAdmin task
- JAMES-3224 Configuration for Cassandra ConsistencyLevel.{QUORUM, SERIAL} (for multi-dc configuration)
- JAMES-3176 Rewritte MDN parsing with Parboiled scala (avoid asm library dependency clash within the Distributed Server)
- JAMES-3194 Rely on DTOConverter in TaskRoute
- JAMES-3430 Restructure message properties storage within Cassandra Mailbox. See upgrade instructions.
- JAMES-3435 Use EventSourcing to manage ACL - avoid SERIAL reads for ACL thus unlocking a performance enhancement for the Distributed James server. Read upgrade instructions.
- JAMES-2124 Sorts module declarations in reactors (thanks to Jean Helou)
- JAMES-3440 JMAP users can now avoid relying on ElasticSearch reads for basic listing operations thanks to the EmailQueryView 
- JAMES-3252 DomainList autoDection should be turned off by 
- JAMES-3192 Upgrade Apache configuration to 2.7
- JAMES-3492 Upgrade ElasticSearch dependency for DistributedServer to 7.10
- JAMES-2514 Upgrade Cassandra dependency for DistributedServer 3.11.3 -> 3.11.10
- JAMES-3497 Multiple dependencies upgrades
- JAMES-3499 Package LDAP in Distributed Server
- JAMES-3225 Set up of the Apache CI
- [REFACTORING] Switch most of the test suite to JUNIT 5

### Fixed
- JAMES-3305 Avoid crashes upon deserialization issues when consuming RabbitMQ messages, leverage dead-letter feature
- JAMES-3212 JMAP Handle subcrible/unsubcrible child's folder when update mailbox
- JAMES-3416 Fix ElasticSearch email address search
- JAMES-1677 Upgrade default hasing algorithm to SHA-512
- JAMES-3454 Use a callback mechanism to re-create RabbitMQ auto-delete queues upon reconnections
- JAMES-3296 Recover email sent during RabbitMQ outages
- JAMES-2046 SentDateComparator should fallback to Mimle4J parsers
- JAMES-3416 ElasticSearch address indexing fixes
- JAMES-3386 add test to ensure blank mailbox paths are not allowed in jmap draft
- MAILBOX-392 WebAdmin documentation: creation of mailboxes with '&' is allowed
- JAMES-3380 use non am/pm dependent hour format
- JAMES-2220 JMAP Draft: Flags update should not fail when a user is missing its Outbox
- JAMES-3364 DeletedMessageVault: deleting many messages dead-locks
- JAMES-3361 JMAP Draft: sharee should not be able to modify mailbox rights
- JAMES-3308 RabbitMQTerminationSubscriberTest should be thread safe
- JAMES-3177 Applicable flags updates needs to be thread safe (IMAP SELECT)
- JAMES-3309 Avoid a NPE in FetchProcessor when SelectedMailbox is unselected
- JAMES-3300 Fix default Cassandra LDAP configuration
- JAMES-3267 Stop forcefully delete ImapRequestFrameDecoder.decode temporary file
- JAMES-3167 Reactify MailboxMapper - unlocks better concurrency management
- JAMES-3170 Fix metric measurement upon reactor publisher replay
- JAMES-3213 Source ReplyTo in ICALToJsonAttribute
- JAMES-3204 Push limit to Cassandra backend when reading messages - before that message listing queries where always reading at least 5000 rows, and triggering other reads for these rows.
- JAMES-3201 ReIndexing enhancements
- JAMES-3179 Fix UpdatableTickingClock thread safety issue
- MAILBOX-405 Renaming too much mailboxes at once was failing on top of the Cassandra mailbox
- JAMES-3513 Wrong UID dispatched on the EventBus for StoreMessageIdManager::setInMailboxes
- JAMES-3512 DigestUtil: close base64 encoding stream
- JAMES-3487 Allow setting on*Exception parameters for Bounce
- JAMES-3511 Solve java.util.NoSuchElementException: heartbeatHandler
- JAMES-3507 Fix broken IMAP APPEND literalSizeLimit option preventing from buffering large requests to files
- JAMES-3438 des-ambiguity error message for Email/set create Content-Transfer-Encoding rejection
- JAMES-3477 Fix NPE when concurrently updating MimeMessage (always copy the message rather than using shared references, which might impact performance)
- JAMES-3444 Perform JMAP TransportChecks only when JMAP is enabled
- JAMES-3495 Cassandra mailbox: Reproduce and fix the null messageId bug
- JAMES-3490 maxUploadSize should come from configuration
- JAMES-1717 VacationMailet should not return answers when no or empty Reply-To header
- JAMES-1784 JMAP: Users with `_` in their names cannot download attachments

### Removed
 - HybridBlobStore. Introduced to fasten small blob access, its usage could be
 compared to a cache, but with a sub-optimal implementation (no eviction, default replication factor, no  circuit breaking).
 Use BlobStore cache instead.
 
### Performance
- JAMES-3295 Multiple IMAP performance enhancements for the Distributed Server. Some enhancement might transfer to other servers as well.
  - JAMES-3295 Use MessageManager::listMessagesMetadata more widely (IMAP)
  - JAMES-3265 IMAP FETCH reading lastUid and lastModseq should be optional
  - JAMES-3265 CassandraMessageMapper should limit modseq allocation upon flags updates
  - JAMES-3265 Impement a MessageMapper method to reset all recents
- JAMES-3263 Optimize RecipientRewriteTable::getMappingsForType
- JAMES-3458 Limit Cassandra statements when retrieving all quota limits
- JAMES-2037 CassandraMessageMapper::listAllMessageUids should not rely on ComposedMessageIdWithMetaData
- JAMES-3453 Specify explicitly lower safer defaults for Reactor flatMaps, filterWhens
- JAMES-3444 Allow moving JMAP mailets in a local-delivery processor - this enables calling `RecipientIsLocal` only one time in the mailet processing pipeline.
- JAMES-2037 Use Flux for MessageManager::search
- JAMES-3409 Better denormalize mailboxes within the Distributed Server. This enables reading only one table of the projection instead of two. Read repairs are implemented for keeping eventual consistency checks. Read upgrade instructions.
- JAMES-3433 Distributed Server: use caching blobstore only for frequently accessed data (callers can specify the level of performance they expect). This ensures the cache is read when it is useful.
- JAMES-3408 Limit concurrency when retrieving mailbox counters
- JAMES-3430 Restructure message properties storage within Cassandra Mailbox. See upgrade instructions.
- JAMES-3277 SetMessagesUpdateProcessor should read less mailboxes - enhance performance for JMAP-draft and JMAP RFC-8621.
- JAMES-3408 Enforce IMAP List not reading counters for Distributed James
- JAMES-3377 Remove unused text criterion - newly indexed mails indexed in ElasticSearch will take less space
- JAMES-3095 Avoid listing all subscriptions for each mailbox (IMAP)
- JAMES-2629 Use a future supplier in CassandraAsyncExecutor
- JAMES-2904 Avoid loading attachment when not needed (IMAP & JMAP) + attachment content streaming (JMAP)
- JAMES-3155 Limit the number of flags updated at the same time
- JAMES-3264 MAILBOX details are read 3 times upon indexing
- JAMES-3506 Avoid a full body read within VacationMailet
- JAMES-3508 Improved performance for IMAP APPEND
- JAMES-3506 SMTP performance enhancement
- JAMES-3505 Make mail remote delivery multi-threaded
- JAMES-3488 Support TLS 1.3
- JAMES-3484 Cassandra mailbox should group copies/moves

### Third party softwares
- James is no longer tested against Cassandra 3.11.3 but instead against Cassandra 3.11.10. Users are recommended to upgrade to this
version as well. See related upgrade instructions.

## [3.5.0] - 2020-04-06

### Added
- JAMES-2813 task management for Distributed James product. This enables several James servers to share a consistent view
of tasks being currently executed.
- JAMES-2563 Health check for ElasticSearch
- JAMES-2904 Authentication and SSL support for Cassandra backend
- JAMES-2904 Authentication and SSL support for ElasticSearch backend
- JAMES-3066 Add support alias when sending emails, with a "allowed From headers" webadmin endpoint
- JAMES-3062 HealthCheck for EventDeadLetters
- JAMES-3058 WebAdmin offline task to correct mailbox inconsistencies on top of Cassandra products
- JAMES-3105 WebAdmin offline task to recompute mailbox counters on top of Cassandra products
- JAMES-3072 WebAdmin endpoint to export mailbox backup

### Changed
  - Use of routing keys to collocate documents per mailbox
  - Under some configuration, html was not extracted before document indexing
  - Removed unnecessary fields from mailbox mapping
  - Disable dynamic mapping thanks to a change of the header structure
- Multiple changes have been made to enhance Distributed James indexing performance:
  - JAMES-2917 Use of routing keys to collocate documents per mailbox
  - JAMES-2910 Under some configuration, html was not extracted before document indexing
  - JAMES-2079 Removed unnecessary fields from mailbox mapping
  - JAMES-2078 Disable dynamic mapping thanks to a change of the header structure
  - Read related [upgrade instructions](upgrade-instructions.md)
- JAMES-2855 Multiple library/plugin/docker images/build tool upgrades
- JAMES-2981 By default the cassandra keyspace creation by James is now disabled by default. This allow to have credentials limited to a keyspace. It can be enabled by setting cassandra.keyspace.create=true in the cassandra.properties file.
- Usernames are assumed to be always lower cased. Many users recently complained about mails non received when sending to upper cased local recipients. We decided to simplify the handling of case for local recipients and users by always storing them lower cased.
- JAMES-2576 Unhealthy health checks now return HTTP 503 instead of 500, degraded now returns 200 instead of 500. See JAMES-2576.
- JAMES-2992 In order to fasten JMAP-draft message retrieval upon calls on properties expected to be fast to fetch, we now compute the preview and hasAttachment properties asynchronously and persist them in Cassandra to improve performance. See JAMES-2919.
- JAMES-2950 It is now forbidden to create new Usernames with the following set of characters in its local part : `"(),:; <>@\[]`, as we prefer it to stay simple to handle. However, the read of Usernames already existing with some of those characters is still allowed, to not introduce any breaking change. See JAMES-2950.
- JAMES-3040 Linshare blob export configuration and mechanism change.
- JAMES-3112 Differentiation between domain alias and domain mapping. Read upgrade instructions.
- JAMES-3122 Log4J2 adoption for Spring product. Log file configuration needs to be updated. See upgrade instructions.

### Fixed
- JAMES-2828 & JAMES-2929 bugs affecting JDBCMailRepository usage with PostgresSQL thanks to Jörg Thomas & Sergey B
- JAMES-2936 Creating a mailbox using consecutive delimiter character leads to creation of list of unnamed mailbox
- JAMES-2911 Unable to send mail from James using an SMTP gateway
- JAMES-2944 Inlined attachments should be wrapped in multipart/related by JMAP draft
- JAMES-2941 Return NO when an IMAP command unexpectedly fails
- JAMES-2943 Deleting auto detected domain should fail
- JAMES-2957 dlp.Dlp matcher should parse emails containing attachments
- JAMES-2958 Limit domain name size to not longer than 255 characters
- JAMES-2939 Prevent mixed case INBOX creation
- JAMES-2903 Rework default LOG4J log file for Spring
- JAMES-2739 fixed browse mails from queue over JMX
- JAMES-2375 DSNBounce mailet should provide a subject
- JAMES-2097 RemoteDelivery: Avoid retrying already succeeded recipients when sendPartial
- MAILBOX-392 Mailbox name validation upon mailbox creation is stricter: forbid `#&*%` and empty sub-mailboxes names.
- JAMES-2972 Incorrect attribute name in the mailet configuration thanks to jtconsol
- JAMES-2632 JMAP Draft GetMailboxes performance enhancements when retrieving all mailboxes of a user
- JAMES-2964 Forbid to create User quota/ Domain quota/ Global quota using negative number
- JAMES-3074 Fixing UidValidity generation, sanitizing of invalid values upon reads. Read upgrade instructions.

### Removed
- Classes marked as deprecated whose removal was planned after 3.4.0 release (See JAMES-2703). This includes:
  - SieveDefaultRepository. Please use SieveFileRepository instead.
  - JDBCRecipientRewriteTable, XMLRecipientRewriteTable, UsersRepositoryAliasingForwarding, JDBCAlias mailets. Please use RecipientRewriteTable mailet instead.
  - JDBCRecipientRewriteTable implementation. Please use JPARecipientRewriteTable instead.
  - JamesUsersJdbcRepository, DefaultUsersJdbcRepository. Please use JpaUsersRepository instead.
  - MailboxQuotaFixed matcher. Please use IsOverQuota instead.
- UsersFileRepository, which was marked as deprecated for years
  - We accordingly removed deprecated methods within UsersRepositoryManagementMBean exposed over JMX (unsetAlias, getAlias, unsetForwardAddress, getForwardAddress). RecipientRewriteTables should be used instead.
- JAMES-3016 RemoteDelivery now doesn't enable `allow8bitmime` property by default. 
This parameter could cause body content alteration leading to DKIM invalid DKIM signatures to be positioned. 
Thanks to Sergey B. for the report. 
More details about the property is at [java mail doc](https://javaee.github.io/javamail/docs/api/com/sun/mail/smtp/package-summary.html)
 - JAMES-3122 LogEnabled API in Spring product had been removed for Log4J2 adoption for Java 9+ runtime compatibility.
 - JAMES-3122 LogEnabled API in Spring product had been removed for Log4J2 adoption for Java 9+ runtime compatibility. 
 - HybridBlobStore. This will be removed after 3.6.0 release. Introduced to fasten small blob access, its usage could be
 compared to a cache, but with a sub-optimal implementation (no eviction, default replication factor, no  circuit breaking).
 Use BlobStore cache instead.
 - JAMES-3028 OpenStack's Swift support. It was very complex, slow and even slowed down AWS S3 implementation.
 
### Third party softwares
 - The distributed James server product (relying on Guice, Cassandra, ElasticSearch, RabbitMQ and optionally Swift) now needs at least RabbitMQ 3.8.1.
 - Tika prior 1.24 is subject to multiple CVEs. We recommend the upgrade.

## [3.4.0] - 2019-09-05
### Added
- Add in-memory docker image
- Support of AWS S3 as blobstore
- WebAdmin routes for mapping management (AddAddressMapping, AddRegexMapping, ListMappings), previously only manageable by CLI
- Metrics for Deleted Message Vault
- More metrics for BlobStore (new delete & deleteBucket configuration)

### Changed
- (experimental) New implementation of the deleted message vault on top of an object store, not relying anymore on a repository. This avoids exposing messages via webAdmin. Messages previously stored in the vault will be lost.
- Migration to ElasticSearch 6.3
- Blob export to LinShare
- Native DKIM support for outgoing mails. The mailets from james-jdkim have been included in main James project.

### Deprecated
- Zookeeper component. This will be removed after 3.4 release. If you have development skills, and are willing to maintain this component, please reach us.

### Removed
- Karaf OSGi implementation. It was not compiled nor tested for a year. We didn't get any feedback about that and we don't have the resources nor the skills to maintain it any more.

## [3.3.0] - 2019-03-26
### Added
- Metrics for BlobStore
- New Guice product using Cassandra RabbitMQ ElasticSearch, OpenStack Swift and optional LDAP dependency (experimental)
- JPA SMTP dockerFile contributed by [matzepan](https://github.com/matzepan)
- Listing healthchecks, thanks to [Madhu Bhat](https://github.com/kratostaine)
- Configuring the ElasticSearch clusterName
- Logging and Metrics now supports Elasticsearch 6 (previously only Elasticsearch 2 was supported)
- Implementation of the RabbitMQ EventBus
- DeadLetter APIs and memory implementation for storing events that failed delivery
- RecipientRewriteTable Aliases and associated WebAdmin routes
- EventBus DeadLetter reDeliver routes on top of WebAdmin
- EventBus DeadLetter Cassandra implementation
- WebAdmin routes for restoring and exporting deleted messages from the Deleted Messages Vault
- PreDeletionHook extension mechanism

### Fixed
- MAILBOX-350 Potential invalid UID <-> MSN mapping upon IMAP COPY
- Possibility to better zoom in Grafana boards
- default ElasticSearch shards & replica configured values
- Move & copy batch sizes are now loaded from configuration

### Changed
- WebAdmin ReIndexing API had been reworked
- MailboxListener and mailbox event system were reworked. Custom listeners will need to be adapted. Please see Upgrade instuctions.
- Docker images are now using a JRE instead of a JDK
- Replacement of the old mailbox event system with the EventBus
- Progressive use of [reactor](https://github.com/reactor/reactor) for concurrency management (in progress)

### Removed
- Drop HBase and JCR components (mailbox and server/data).

### Third party softwares
 - Tika prior 1.20 is subject to multiple CVEs. We recommend the upgrade

## [3.2.0] - 2018-11-14
### Added
- Mail filtering configured via the JMAP protocol
- WebAdmin exposed mail re-indexing tasks
- WebAdmin exposed health checks. This includes:
   - Possibility to perform a single healthcheck, thanks to [mschnitzler](https://github.com/mschnitzler)
   - Cassandra health checks thanks to [matzepan](https://github.com/matzepan)
- IMAP MOVE commend (RFC-6851) On top of JPA. Thanks to [mschnitzler](https://github.com/mschnitzler)
- JPA support for Sieve script storage thanks to [Sebast26](https://github.com/sebast26)
- Sieve script upload via the CLI thanks to [matzepan](https://github.com/matzepan)
- Mailet DOC: Exclude from documentation annotation thanks to [mschnitzler](https://github.com/mschnitzler)
- `cassandra.pooling.max.queue.size` configuration option Thanks to [matzepan](https://github.com/matzepan)
- `RecipentDomainIs` and `SenderDomainIs` matchers by [athulyaraj](https://github.com/athulyaraj)

### Changed
- Multiple libraries updates
- Migration from Cassandra 2 to Cassandra 3
- Mail::getSender was deprecated. Mail::getMaybeSender offers better Null Sender support. Java 8 default API method was used to not break compatibility.

### Deprecated
 - HBase and JCR components (mailbox and server/data). This will be removed as part of 3.3.0. If you have development skills, and are willing to maintain these components, please reach us.

### Removed
- Drop partially implemented Kafka distributed events

### Third party softwares
 - SpamAssassin prior 3.4.2 is subject to multiple CVEs. We recommend the upgrade
 - Tika prior 1.19.1 is subject to multiple CVEs. We recommend the upgrade

## [3.1.0] - 2018-07-31
### Added
- Delegating folders
- Introduce an object store
- Configurable listeners
- MDN (Message Disposition notification) parsing and handling
- SpamAssassin support with per user reports
- Search in attachments
- Data Leak Prevention
- JPA SMTP Guice product
- Cassandra migration process
- Structured logging
- RPM packaging (in addition to deb packaging)

### Changed
- Move to Java 8
- Improve Mail Repositories handling, including a nice web API
- Improve Mail Queues handling, including a nice web API
- Improve RRT (Recipient Rewrite Table) implementation
- Quota handling improvments, and in particular users can receive an email when they are near the limit of their quota
- Many performances enhancement, in particular on Cassandra backend
- Documentation updates

## [3.0.1] - 2017-10-20
### Changed
- Fix CVE-2017-12628: java deserialization issue exposed by JMX

## [3.0.0] - 2017-07-20
Too many untracked changes, sorry. But you can have a look at our latest news: [James posts](http://james.apache.org/posts.html)

## Before
Refer too [Old changelog](http://james.apache.org/server/2.3.0/changelog.html)
