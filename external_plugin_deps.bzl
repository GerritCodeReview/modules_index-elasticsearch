load("//tools/bzl:maven_jar.bzl", "maven_jar")

TESTCONTAINERS_VERSION = "1.15.3"

# Ensure artifacts compatibility by selecting them from the Bill Of Materials
# https://search.maven.org/artifact/net.openhft/chronicle-bom/2.20.191/pom
def external_plugin_deps():
    # When upgrading elasticsearch-rest-client, also upgrade httpcore-nio
    # and httpasyncclient as necessary in tools/nongoogle.bzl. Consider
    # also the other org.apache.httpcomponents dependencies in
    # WORKSPACE.
    maven_jar(
        name = "elasticsearch-rest-client",
        artifact = "org.elasticsearch.client:elasticsearch-rest-client:7.8.1",
        sha1 = "59feefe006a96a39f83b0dfb6780847e06c1d0a8",
    )

    maven_jar(
        name = "testcontainers-elasticsearch",
        artifact = "org.testcontainers:elasticsearch:" + TESTCONTAINERS_VERSION,
        sha1 = "595e3a50f59cd3c1d281ca6c1bc4037e277a1353",
    )

    # elasticsearch-rest-client explicitly depends on this version
    maven_jar(
        name = "httpasyncclient",
        artifact = "org.apache.httpcomponents:httpasyncclient:4.1.4",
        sha1 = "f3a3240681faae3fa46b573a4c7e50cec9db0d86",
    )

    # elasticsearch-rest-client explicitly depends on this version
    maven_jar(
        name = "httpcore-nio",
        artifact = "org.apache.httpcomponents:httpcore-nio:4.4.12",
        sha1 = "84cd29eca842f31db02987cfedea245af020198b",
    )

    maven_jar(
        name = "jackson-core",
        artifact = "com.fasterxml.jackson.core:jackson-core:2.11.3",
        sha1 = "c2351800432bdbdd8284c3f5a7f0782a352aa84a",
    )

    maven_jar(
        name = "httpasyncclient",
        artifact = "org.apache.httpcomponents:httpasyncclient:4.1.4",
        sha1 = "f3a3240681faae3fa46b573a4c7e50cec9db0d86",
    )

