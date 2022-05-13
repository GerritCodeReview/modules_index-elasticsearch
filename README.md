# Index backend for Gerrit, based on ElasticSearch

Indexing backend libModule for [Gerrit Code Review](https://gerritcodereview.com)
based on [ElasticSearch](https://www.elastic.co/elasticsearch/).

This module was original part of Gerrit core and then extracted into a separate
component from v3.5.0-rc3 as part of [Change-Id: Ib7b5167ce](https://gerrit-review.googlesource.com/c/gerrit/+/323676).

## How to build

This libModule is built like a Gerrit in-tree plugin, using Bazelisk. See the
[build instructions](src/main/resources/Documentation/build.md) for more details.

## Setup

* Install index-elasticsearch module

Install the index-elasticsearch.jar into the `$GERRIT_SITE/lib` directory.

Add the index-elasticsearch module to `$GERRIT_SITE/etc/gerrit.config` as follows:

```ini
[gerrit]
  installIndexModule = com.google.gerrit.elasticsearch.ElasticIndexModule
```

When installing the module on Gerrit replicas, use following example:

```ini
[gerrit]
  installIndexModule = com.google.gerrit.elasticsearch.ReplicaElasticIndexModule
```

For further information and supported options, refer to [config](src/main/resources/Documentation/config.md)
documentation.

## Integration test

Gerrit acceptance tests allow the execution with an alternate implementation of
the indexing backend using the `GERRIT_INDEX_MODULE` environment variable.

```sh
bazel test --test_env=GERRIT_INDEX_MODULE=com.google.gerrit.elasticsearch.ElasticIndexModule //...
```
