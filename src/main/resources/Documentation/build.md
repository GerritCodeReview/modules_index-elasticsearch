# Build

This plugin is built with Bazel in-tree build.

## Build in Gerrit tree

Create a symbolic link of the repository source to the Gerrit source
tree plugins/index-elasticsearch directory, and the external_plugin_deps.bzl
dependencies linked to plugins/external_plugin_deps.bzl.

Example:

```sh
git clone https://gerrit.googlesource.com/gerrit
git clone https://gerrit.googlesource.com/modules/index-elasticsearch
cd gerrit/plugins
ln -s ../../index-elasticsearch index-elasticsearch
ln -sf ../../external_plugin_deps.bzl .
```

From the Gerrit source tree issue the command `bazelisk build plugins/index-elasticsearch`.

Example:

```sh
bazelisk build plugins/index-elasticsearch
```

The libModule jar file is created under `bazel-bin/plugins/index-elasticsearch/index-elasticsearch.jar`

## Integration test

There are two different ways to run tests for this module. You can either run only the tests
provided by the module or you can run all Gerrit core acceptance tests with the indexing backend set
to this module.

To run only the tests provided by this plugin:

```sh
bazelisk test plugins/index-elasticsearch/...
```

Gerrit acceptance tests allow the execution with an alternate implementation of
the indexing backend using the `GERRIT_INDEX_MODULE` environment variable.

```sh
bazelisk test --test_env=GERRIT_INDEX_MODULE=com.google.gerrit.elasticsearch.ElasticIndexModule //...
```

## IDE setup

This project can be imported into the Eclipse IDE.
Add the plugin name to the `CUSTOM_PLUGINS` and to the
`CUSTOM_PLUGINS_TEST_DEPS` set in Gerrit core in
`tools/bzl/plugins.bzl`, and execute:

```
  ./tools/eclipse/project.py
```

More information about Bazel can be found in the [Gerrit
documentation](../../../Documentation/dev-bazel.html).

[Back to @PLUGIN@ documentation index][index]

[index]: index.html
