load("@rules_java//java:defs.bzl", "java_library")
load("//tools/bzl:junit.bzl", "junit_tests")
load(
    "//tools/bzl:plugin.bzl",
    "PLUGIN_DEPS",
    "PLUGIN_TEST_DEPS",
    "gerrit_plugin",
)

gerrit_plugin(
    name = "index-elasticsearch",
    srcs = glob(["src/main/java/**/*.java"]),
    deps = [
        "@elasticsearch-rest-client//jar",
        "@httpasyncclient//jar",
        "@httpcore-nio//jar",
        "@jackson-core//jar",
        "@maven-artifact//jar",
    ],
)

ELASTICSEARCH_DEPS = [
    "@docker-java-api//jar",
    "@docker-java-transport//jar",
    "@duct-tape//jar",
    "@httpasyncclient//jar",
    "@jackson-annotations//jar",
    "@jackson-core//jar",
    "@jna//jar",
    "@testcontainers-elasticsearch//jar",
    "@testcontainers//jar",
]

java_library(
    name = "index-elasticsearch__plugin_test_deps",
    testonly = True,
    srcs = [],
    visibility = ["//visibility:public"],
    exports = ELASTICSEARCH_DEPS,
)

java_library(
    name = "elasticsearch_test_utils",
    testonly = True,
    srcs = glob(
        ["src/test/java/**/*.java"],
        exclude = ["src/test/java/**/*Test.java"],
    ),
    visibility = ["//visibility:public"],
    deps = ELASTICSEARCH_DEPS + PLUGIN_DEPS + PLUGIN_TEST_DEPS + [
        ":index-elasticsearch__plugin",
    ],
)

QUERY_TESTS_DEP = "//javatests/com/google/gerrit/server/query/%s:abstract_query_tests"

ACCOUNT_QUERY_TESTS_DEP = "//javatests/com/google/gerrit/server/query/account:abstract_query_tests"

TYPES = [
    "account",
    "change",
    "group",
    "project",
]

SUFFIX = "sTest.java"

ELASTICSEARCH_TESTS_V7 = {i: "ElasticV7Query" + i.capitalize() + SUFFIX for i in TYPES}

[junit_tests(
    name = "elasticsearch_query_%ss_test_V7" % name,
    size = "enormous",
    srcs = ["src/test/java/com/google/gerrit/elasticsearch/" + src],
    tags = [
        "docker",
        "elastic",
        "exclusive",
    ],
    deps = ELASTICSEARCH_DEPS + PLUGIN_TEST_DEPS + [
        QUERY_TESTS_DEP % name,
        ":elasticsearch_test_utils",
        ":index-elasticsearch__plugin",
    ],
) for name, src in ELASTICSEARCH_TESTS_V7.items()]

junit_tests(
    name = "index-elasticsearch_tests",
    size = "small",
    srcs = glob(
        ["src/test/java/**/*Test.java"],
        exclude = ["src/test/java/**/Elastic*Query*" + SUFFIX],
    ),
    tags = ["elastic"],
    deps = PLUGIN_TEST_DEPS + [
        ":index-elasticsearch__plugin",
    ],
)
