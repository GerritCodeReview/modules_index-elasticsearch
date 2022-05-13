load("//tools/bzl:junit.bzl", "junit_tests")
load(
    "//tools/bzl:plugin.bzl",
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

junit_tests(
    name = "index-elasticsearch_tests",
    size = "small",
    srcs = glob(["src/test/java/**/*Test.java"]),
    tags = ["elastic"],
    deps = PLUGIN_TEST_DEPS + [
        ":index-elasticsearch__plugin",
    ],
)
