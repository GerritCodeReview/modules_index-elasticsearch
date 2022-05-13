load("@rules_java//java:defs.bzl", "java_library")
load("//tools/bzl:junit.bzl", "junit_tests")
load("//javatests/com/google/gerrit/acceptance:tests.bzl", "acceptance_tests")
load(
    "//tools/bzl:plugin.bzl",
    "PLUGIN_DEPS",
    "PLUGIN_TEST_DEPS",
    "gerrit_plugin",
)

gerrit_plugin   (
    name = "index-elasticsearch",
    srcs = glob(["src/main/java/**/*.java"]),
    deps = [
        "//java/com/google/gerrit/common:annotations",
        "//java/com/google/gerrit/entities",
        "//java/com/google/gerrit/exceptions",
        "//java/com/google/gerrit/extensions:api",
        "//java/com/google/gerrit/index",
        "//java/com/google/gerrit/index:query_exception",
        "//java/com/google/gerrit/index/project",
        "//java/com/google/gerrit/lifecycle",
        "//java/com/google/gerrit/proto",
        "//java/com/google/gerrit/server",
        "//lib:gson",
        "//lib:guava",
        "//lib:jgit",
        "//lib:protobuf",
        "//lib/commons:lang",
        "@elasticsearch-rest-client//jar",
        "//lib/flogger:api",
        "//lib/guice",
        "//lib/guice:guice-assistedinject",
        "@httpasyncclient//jar",
        "//lib/httpcomponents:httpclient",
        "//lib/httpcomponents:httpcore",
        "@httpcore-nio//jar",
        "@jackson-core//jar",
    ],
)

java_library(
    name = "elasticsearch_test_utils",
    testonly = True,
    srcs = [],
    visibility = ["//visibility:public"],
    deps = [
        "//java/com/google/gerrit/index",
        "//lib:guava",
        "//lib:jgit",
        "//lib:junit",
        "//lib/guice",
        "//lib/httpcomponents:httpcore",
        "//lib/jackson:jackson-annotations",
        "//lib/testcontainers",
        "//lib/testcontainers:docker-java-api",
        "//lib/testcontainers:docker-java-transport",
        "@testcontainers-elasticsearch//jar",
        ":index-elasticsearch__plugin",
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
        "//java/com/google/gerrit/testing:gerrit-test-util",
        "//lib:guava",
        "//lib:jgit",
        "//lib/guice",
        "//lib/httpcomponents:httpcore",
        "//lib/truth",
        ":elasticsearch_test_utils",
    ],
)
