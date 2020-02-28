# 19. Reactor-netty adoption for JMAP server implementation

Date: 2020-02-28

## Status

Accepted (lazy consensus)

## Context

After we decided to adopt and implement the last specifications of JMAP (see 
[new JMAP specifications adoption ADR](https://github.com/apache/james-project/blob/master/src/adr/0018-jmap-new-specs.md)), 
it was agreed that we would need to be able to serve both `jmap-draft` and the new `jmap` with a reactive server. 

## Decision

We decided to use [reactor-netty](https://github.com/reactor/reactor-netty) for the following reasons:

* It's a reactive server
* It's using [Reactor](https://projectreactor.io/), which is the same technology that we use in the rest of our codebase
* Implementing JMAP do not require high level HTTP server features

## Consequences

* Porting current `jmap-draft` to use a `reactor-netty` server instead of a [Jetty server](https://www.eclipse.org/jetty/)
* The `reactor-netty` server should serve as well the new `jmap` implementation
* Should be possible to refactor and get end-to-end reactive operations with it

## References

* JIRA: [JAMES-3078](https://issues.apache.org/jira/browse/JAMES-3078)
* JMAP new specifications adoption ADR: https://github.com/apache/james-project/blob/master/src/adr/0018-jmap-new-specs.md