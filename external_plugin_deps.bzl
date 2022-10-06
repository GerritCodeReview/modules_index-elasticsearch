load("//tools/bzl:maven_jar.bzl", "maven_jar")

def external_plugin_deps():
    maven_jar(
        name = "jackson-core",
        artifact = "com.fasterxml.jackson.core:jackson-core:2.11.3",
        sha1 = "c2351800432bdbdd8284c3f5a7f0782a352aa84a",
    )

    # Ensure artifacts compatibility by selecting them from the Bill Of Materials
    # https://search.maven.org/artifact/org.testcontainers/testcontainers/1.16.3/pom
    TESTCONTAINERS_VERSION = "1.16.3"

    maven_jar(
        name = "testcontainers",
        artifact = "org.testcontainers:testcontainers:" + TESTCONTAINERS_VERSION,
        sha1 = "a5d2ad12ebe0d4b66e4fd9f4657571213b5f64c3",
    )

    maven_jar(
        name = "testcontainers-elasticsearch",
        artifact = "org.testcontainers:elasticsearch:" + TESTCONTAINERS_VERSION,
        sha1 = "5831f0645ed3184df96ab9568f0c4347b8ff87bd",
    )

    maven_jar(
        name = "duct-tape",
        artifact = "org.rnorth.duct-tape:duct-tape:1.0.8",
        sha1 = "92edc22a9ab2f3e17c9bf700aaee377d50e8b530",
    )

    DOCKER_JAVA_VERS = "3.2.12"

    maven_jar(
        name = "docker-java-api",
        artifact = "com.github.docker-java:docker-java-api:" + DOCKER_JAVA_VERS,
        sha1 = "6716b0483926adbcce0fd4636664e711debafc11",
    )

    maven_jar(
        name = "docker-java-transport",
        artifact = "com.github.docker-java:docker-java-transport:" + DOCKER_JAVA_VERS,
        sha1 = "88ad0cd6d67c075a5f622dbf7cb930ef4b1c008f",
    )

    maven_jar(
        name = "docker-java-transport-zerodep",
        artifact = "com.github.docker-java:docker-java-transport-zerodep:" + DOCKER_JAVA_VERS,
        sha1 = "131e656a25622f7288aaac531e7d3290d3e8936d",
    )

    # Match version used in docker-java-transport
    # https://search.maven.org/artifact/com.github.docker-java/docker-java-transport/3.2.12/pom
    maven_jar(
        name = "jna",
        artifact = "net.java.dev.jna:jna:5.8.0",
        sha1 = "3551d8d827e54858214107541d3aff9c615cb615",
    )

    # Match jackson.version from docker-java
    # https://search.maven.org/artifact/com.github.docker-java/docker-java-parent/3.2.12/pom
    maven_jar(
        name = "jackson-annotations",
        artifact = "com.fasterxml.jackson.core:jackson-annotations:2.10.3",
        sha1 = "0f63b3b1da563767d04d2e4d3fc1ae0cdeffebe7",
    )

    # When upgrading elasticsearch-rest-client, also upgrade httpcore-nio
    # and httpasyncclient as necessary. Consider also the other
    # org.apache.httpcomponents dependencies in core.
    maven_jar(
        name = "elasticsearch-rest-client",
        artifact = "org.elasticsearch.client:elasticsearch-rest-client:8.3.2",
        sha1 = "bb5cb3dbd82ea75a6d49b9011ca5b1d125b30f00",
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
