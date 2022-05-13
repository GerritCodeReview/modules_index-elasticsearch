load("@rules_java//java:defs.bzl", "java_library")
load("//tools/bzl:junit.bzl", "junit_tests")
load("//javatests/com/google/gerrit/acceptance:tests.bzl", "acceptance_tests")
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
    ],
)

java_library(
    name = "elasticsearch_test_utils",
    testonly = True,
    srcs = [],
    visibility = ["//visibility:public"],
    runtime_deps = [
        ":index-elasticsearch__plugin",
        "//java/com/google/gerrit/index",
        "//lib:guava",
        "//lib:jgit",
        "//lib:junit",
        "//lib/guice",
        "//lib/httpcomponents:httpcore",
        "@docker-java-api//jar",
        "@docker-java-transport//jar",
        "@jackson-annotations//jar",
        "@testcontainers-elasticsearch//jar",
        "@testcontainers//jar",
    ],
)

ELASTICSEARCH_DEPS = [
    ":elasticsearch_test_utils",
    "//java/com/google/gerrit/testing:gerrit-test-util",
    "//lib/guice",
    "//lib:jgit",
]

HTTP_TEST_DEPS = [
    "@httpasyncclient//jar",
    "//lib/httpcomponents:httpclient",
]

QUERY_TESTS_DEP = "//javatests/com/google/gerrit/server/query/%s:abstract_query_tests"

TYPES = [
    "account",
    "change",
    "group",
    "project",
]

SUFFIX = "sTest.java"

ELASTICSEARCH_TESTS_V7 = {i: "ElasticV7Query" + i.capitalize() + SUFFIX for i in TYPES}

ELASTICSEARCH_TAGS = [
    "docker",
    "elastic",
]

junit_tests(
    name = "index-elasticsearch_tests",
    size = "small",
    srcs = glob(
        ["src/test/java/**/*Test.java"],
        exclude = ["Elastic*Query*" + SUFFIX],
    ),
    tags = ["elastic"],
    deps = PLUGIN_DEPS + PLUGIN_TEST_DEPS + [
        ":index-elasticsearch__plugin",
        "//java/com/google/gerrit/testing:gerrit-test-util",
        "//lib:guava",
        "//lib:jgit",
        "//lib/guice",
        "//lib/httpcomponents:httpcore",
        "//lib/truth",
        ":elasticsearch_test_utils",
    ],
)
