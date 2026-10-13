# Build

This plugin is built with Bazel in-tree build. This plugin depends on the Elasticsearch Java Low
Level REST Client (abbreviated as LLRC by the ES dev team) for integration with an Elasticsearch
cluster. The LLRC is licensed as Apache v2 (even after ES itself has moved to SSPL) and is
compatible with all ES versions. See the [LLRC
docs](https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/8.15/java-rest-low-usage-maven.html)
for more information.

Two build modes are supported: Standalone and in Gerrit tree. The standalone build mode is
recommended, as this mode doesn't require the Gerrit tree to exist locally.

### Packaged runtime JAR allowlist test

This plugin tracks the set of third-party runtime JARs that are bundled into the plugin JAR.
A deterministic, version-agnostic manifest is generated from the plugin’s runtime classpath and
compared against the checked-in allowlist:

`@PLUGIN@_third_party_runtime_jars.allowlist.txt`

This acts as a guardrail to detect unintended changes to the packaged runtime dependency set.

To run the check (standalone or in Gerrit tree):

```bash
bazelisk test //:@PLUGIN@_dependency_allowlist_test
```

#### Updating the allowlist

If the test fails because the packaged third-party JAR set changed, the plugin’s bundled runtime
dependencies have changed.

If the change is expected and has been reviewed, refresh the allowlist:

```bash
bazelisk build //:@PLUGIN@_third_party_runtime_jars.allowlist.txt
cp bazel-bin/@PLUGIN@_third_party_runtime_jars.allowlist.txt \
  @PLUGIN@_third_party_runtime_jars.allowlist.txt
```

Commit the updated allowlist along with the dependency change.

### Build standalone

Clone the plugin:

```
  git clone https://gerrit.googlesource.com/plugins/@PLUGIN@
  cd @PLUGIN@
```

Issue the command:

```
  bazel build :all
```

The output is created in

```
  bazel-bin/@PLUGIN@.jar
```

### Build in Gerrit tree

Clone or link this plugin to the plugins directory of Gerrit's source tree, and issue the command:

```
  git clone https://gerrit.googlesource.com/gerrit
  git clone https://gerrit.googlesource.com/plugins/@PLUGIN@
  cd gerrit/plugins
  ln -s ../../@PLUGIN@ .
```

Then link the plugin's module fragment into Gerrit's `plugins` directory, replacing the
placeholder file provided by Gerrit. This fragment exposes the plugin's Bazel module and its
external dependencies to the Gerrit root module when building in-tree.

```
  cd gerrit/plugins
  rm external_plugin_deps.MODULE.bazel
  ln -s @PLUGIN@/external_plugin_deps.MODULE.bazel .
```

From the Gerrit source tree run:

```
  bazel build plugins/@PLUGIN@
```

The output is created in:

```
  bazel-bin/plugins/@PLUGIN@/@PLUGIN@.jar
```

To execute the tests run either of:

```
  bazel test plugins/@PLUGIN@/...
  bazel test --test_tag_filters=@PLUGIN@ //...
```

## Integration test

There are two different ways to run tests for this module. You can either run only the tests
provided by the module or you can run all Gerrit core acceptance tests with the indexing backend set
to this module.

To run only the tests provided by this plugin:

```sh
bazelisk test plugins/@PLUGIN@/...
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
