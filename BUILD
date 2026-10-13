load(
    "@com_googlesource_gerrit_bazlets//:gerrit_plugin.bzl",
    "gerrit_plugin",
    "gerrit_plugin_ext_test_deps",
    "gerrit_plugin_test_util",
    "gerrit_plugin_tests",
)
load("@rules_java//java:defs.bzl", "java_library")

ELASTICSEARCH_DEPS = [
    "com.fasterxml.jackson.core:jackson-core",
    "org.apache.httpcomponents:httpcore-nio",
    "org.apache.httpcomponents:httpasyncclient",
    "org.elasticsearch.client:elasticsearch-rest-client",
]

ELASTICSEARCH_TEST_DEPS = ELASTICSEARCH_DEPS + [
    "org.testcontainers:elasticsearch",
    "org.testcontainers:testcontainers",
]


gerrit_plugin(
    plugin = "index-elasticsearch",
    ext_deps = ELASTICSEARCH_DEPS,
    srcs = glob(["src/main/java/**/*.java"]),
    deps = [
        ":httpcomponents-neverlink",
        ":proto-converter-neverlink",
    ],
)

java_library(
    name = "httpcomponents-neverlink",
    neverlink = 1,
    exports = [
        "//lib/httpcomponents:httpclient",
        "//lib/httpcomponents:httpcore",
    ],
)

java_library(
    name = "proto-converter-neverlink",
    neverlink = 1,
    exports = [
        "//java/com/google/gerrit/entities/converter:converters",
        "//java/com/google/gerrit/entities/converter:proto_converter",
    ],
)

gerrit_plugin_test_util(
    name = "elasticsearch_test_utils",
    srcs = glob(
        ["src/test/java/**/*.java"],
        exclude = ["src/test/java/**/*Test.java"],
    ),
    ext_deps_label = ":index-elasticsearch__plugin_test_deps",
    plugin = "index-elasticsearch",
    visibility = ["//visibility:public"],
)

QUERY_TESTS_DEP = "//javatests/com/google/gerrit/server/query/%s:abstract_query_tests"

TYPES = [
    "account",
    "change",
    "group",
    "project",
]

SUFFIX = "sTest.java"

ABSTRACT_ELASTICSEARCH_TESTS = {i: "Elastic*Query" + i.capitalize() + SUFFIX for i in TYPES}

[gerrit_plugin_test_util(
    name = "abstract_elasticsearch_query_%ss_test" % name,
    srcs = glob(["src/test/java/com/google/gerrit/elasticsearch/" + src]),
    ext_deps_label = ":index-elasticsearch__plugin_test_deps",
    plugin = "index-elasticsearch",
    visibility = ["//visibility:public"],
    deps = [
        QUERY_TESTS_DEP % name,
        ":elasticsearch_test_utils",
    ],
) for name, src in ABSTRACT_ELASTICSEARCH_TESTS.items()]

ELASTICSEARCH_TESTS_V7 = {i: "ElasticV7Query" + i.capitalize() + SUFFIX for i in TYPES}

[gerrit_plugin_tests(
    name = "elasticsearch_query_%ss_test_V7" % name,
    size = "enormous",
    srcs = ["src/test/java/com/google/gerrit/elasticsearch/" + src],
    ext_deps_label = ":index-elasticsearch__plugin_test_deps",
    plugin = "index-elasticsearch",
    tags = [
        "docker",
        "elastic",
        "elastic_V7",
        "exclusive",
    ],
    deps = [
        QUERY_TESTS_DEP % name,
        ":elasticsearch_test_utils",
        ":abstract_elasticsearch_query_%ss_test" % name,
    ],
) for name, src in ELASTICSEARCH_TESTS_V7.items()]

ELASTICSEARCH_TESTS_V8 = {i: "ElasticV8Query" + i.capitalize() + SUFFIX for i in TYPES}

[gerrit_plugin_tests(
    name = "elasticsearch_query_%ss_test_V8" % name,
    size = "enormous",
    srcs = ["src/test/java/com/google/gerrit/elasticsearch/" + src],
    ext_deps_label = ":index-elasticsearch__plugin_test_deps",
    plugin = "index-elasticsearch",
    tags = [
        "docker",
        "elastic",
        "elastic_V8",
        "exclusive",
    ],
    deps = [
        QUERY_TESTS_DEP % name,
        ":elasticsearch_test_utils",
        ":abstract_elasticsearch_query_%ss_test" % name,
    ],
) for name, src in ELASTICSEARCH_TESTS_V8.items()]

gerrit_plugin_tests(
    plugin = "index-elasticsearch",
    size = "small",
    srcs = glob(
        ["src/test/java/**/*Test.java"],
        exclude = ["src/test/java/**/Elastic*Query*" + SUFFIX],
    ),
    tags = ["elastic"],
    ext_deps_label = ":index-elasticsearch__plugin_test_deps",
    deps = [
        ":elasticsearch_test_utils",
    ],
)

gerrit_plugin_ext_test_deps(
    ext_deps = ELASTICSEARCH_TEST_DEPS,
    plugin = "index-elasticsearch",
)
