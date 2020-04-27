# 34. Mailbox API visibility and usage

Date: 2020-04-27

## Status

Accepted (lazy consensus)

## Context

For some reasons, all mailboxes implementations rely on some abstract classes located in `mailbox-store` module (like 
`StoreMailboxManager` and the `Mapper`s classes). For example, a `CassandraMailboxmanager` has to extend `StoreMailboxManager` 
(that implements `Mailboxmanager` from the `mailbox-api`) that requires the implementation of some `Mapper`s.

`Mapper`s are designed to provide low-level functions and methods on mailboxes. It's recurrent that we are tempted in 
James, outside of the `mailbox` modules, to rely on some of those classes located in `mailbox-store` to have an easier 
access on some user's mailboxes or messages. 

## Decision

We should never rely on classes defined in `mailbox-store` outside of the `mailbox` modules. The right way would be to 
always rely on the `Manager`s defined in `mailbox-api` module to access mailboxes and messages.

We should ensure the correctness of `Manager`s implementations by providing contract tests and not by sharing abstract 
classes.

## Consequences

The issue with this design is that in this day, we can't seem to be able to act on a single message without knowing a 
user it belongs to. It creates some issues for rebuilding a single message fast view projection, or the reindexation 
of a single message.

We will need to introduce some refactorings, like on the session, to be able to rely fully on `mailbox-api` in those cases.

## References

* [Discussions around rebuild a single message fast view projection](https://github.com/linagora/james-project/pull/3035#discussion_r363684700)

* [General mailing list discussion on the session refactoring](https://www.mail-archive.com/server-dev@james.apache.org/msg64120.html)
