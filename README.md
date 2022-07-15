# Index backend for Gerrit, based on ElasticSearch

Indexing backend libModule for [Gerrit Code Review](https://gerritcodereview.com)
based on [ElasticSearch](https://www.elastic.co/elasticsearch/).

This module was originally part of Gerrit core and then extracted into a separate
component from v3.5.0-rc3 as part of [Change-Id: Ib7b5167ce](https://gerrit-review.googlesource.com/c/gerrit/+/323676).

## How to build

This libModule is built like a Gerrit in-tree plugin, using Bazelisk. See the
[build instructions](src/main/resources/Documentation/build.md) for more details.

## Setup

See the [setup instructions](src/main/resources/Documentation/setup.md) for how to install the
index-elasticsearch module.

For further information and supported options, refer to the [config](src/main/resources/Documentation/config.md)
documentation.

## Integration test

This libModule runs tests like a Gerrit in-tree plugin, using Bazelisk. See the
[test instructions](src/main/resources/Documentation/build.md#Integration-test) for more details.
