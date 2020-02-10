# 18. New JMAP specifications adoption

Date: 2020-02-06

## Status

Proposed

## Context

Historically, James has been an early adopter for the JMAP specification, and a first partial implementation was conducted when JMAP was just a draft. 
But with time, the IETF draft went with radical changes and the community could not keep this implementation up to date with the spec changes.

As of summer 2019, JMAP core ([RFC 8620](https://tools.ietf.org/html/rfc8620)) and JMAP mail ([RFC 8621](https://tools.ietf.org/html/rfc8621)) have been officially published. 
Thus we should implement these new specifications to clain JMAP support.

We need to keep in mind though that part of the community actively relies on the actual 'draft' implementation of JMAP existing in James. 

## Decision

We decided to do as follow:

* Rename packages `server/protocols/jmap*` and guice packages `server/container/guice/protocols/jmap*` to `jmap-draft`. `JMAPServer` should also be renamed to `JMAPDraftServer` (this has already been contributed [here](https://github.com/apache/james-project/pull/164), thanks to @cketti).
* Create a new `jmap` package, binding it on a new port.
* Implement the new JMAP request structure with the [echo](https://jmap.io/spec-core.html#the-coreecho-method) method
* Implement authentication and session of the new JMAP protocol
* Implement protocol-level error handling
* Duplicate and adapt existing mailbox methods of `jmap-draft` to `jmap`
* Duplicate and adapt existing email methods of `jmap-draft` to `jmap`
* Duplicate and adapt existing vacation methods of `jmap-draft` to `jmap`
* Support uploads/downloads

Then when we finish to port our existing methods to the new JMAP specifications, we can implement these new features:

* Identities
* EmailSubmission
* Push and queryChanges
* Threads

In order to ease testing and development speed, we decided to support `jmap` only on top of memory-guice at first. 
Then, we would add it for distributed-james Guice product, and other products might follow later.

We should ensure no changes is done to `jmap-draft` while implementing the new `jmap` one.

It's worth mentioning as well that we took the decision of writing this new implementation using `Scala`.

## Consequences

* Each feature implemented will respect the final specifications of JMAP
* Getting missing features that are necessary to deliver a better mailing experience with James, like push, query changes and threads 
* Separating the current implementation from the new one will allow existing `jmap-draft` clients to smoothly transition to `jmap`, then trigger the classic "deprecation-then-removal" process. 

## References

* A discussion around this already happened in September 2019 on the server-dev mailinglist: [JMAP protocol: Implementing RFC-8620 & RFC-8621](https://www.mail-archive.com/server-dev@james.apache.org/msg62072.html)
* JIRA: [JAMES-2884](https://issues.apache.org/jira/browse/JAMES-2884)