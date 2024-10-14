load("//tools/bzl:maven_jar.bzl", "maven_jar")

def external_plugin_deps():
    maven_jar(
        name = "jackson-core",
        artifact = "com.fasterxml.jackson.core:jackson-core:2.11.3",
        sha1 = "c2351800432bdbdd8284c3f5a7f0782a352aa84a",
    )

    # Ensure artifacts compatibility by selecting them from the Bill Of Materials
    # https://search.maven.org/artifact/org.testcontainers/testcontainers/1.19.7/pom
    TESTCONTAINERS_VERSION = "1.19.7"

    maven_jar(
        name = "testcontainers",
        artifact = "org.testcontainers:testcontainers:" + TESTCONTAINERS_VERSION,
        sha1 = "2dd7b1497fc444755582b0efc88636c4d299601f",
    )

    maven_jar(
        name = "testcontainers-elasticsearch",
        artifact = "org.testcontainers:elasticsearch:" + TESTCONTAINERS_VERSION,
        sha1 = "8cd9f4ae67c9299143eb718541ff544b66273283",
    )

    maven_jar(
        name = "duct-tape",
        artifact = "org.rnorth.duct-tape:duct-tape:1.0.8",
        sha1 = "92edc22a9ab2f3e17c9bf700aaee377d50e8b530",
    )

    DOCKER_JAVA_VERS = "3.3.6"

    maven_jar(
        name = "docker-java-api",
        artifact = "com.github.docker-java:docker-java-api:" + DOCKER_JAVA_VERS,
        sha1 = "8e152880bfe595c81a25501e21a6d7b1d4df97be",
    )

    maven_jar(
        name = "docker-java-transport",
        artifact = "com.github.docker-java:docker-java-transport:" + DOCKER_JAVA_VERS,
        sha1 = "0d536d16a297f9139b833955390a3d581e336e67",
    )

    maven_jar(
        name = "docker-java-transport-zerodep",
        artifact = "com.github.docker-java:docker-java-transport-zerodep:" + DOCKER_JAVA_VERS,
        sha1 = "c9cde0239ce03376f6dfd0465bd461853af22196",
    )

    # Match version used in docker-java-transport
    # https://search.maven.org/artifact/com.github.docker-java/docker-java-transport/3.3.6/pom
    maven_jar(
        name = "jna",
        artifact = "net.java.dev.jna:jna:5.13.0",
        sha1 = "1200e7ebeedbe0d10062093f32925a912020e747",
    )

    # Match jackson.version from docker-java
    # https://search.maven.org/artifact/com.github.docker-java/docker-java-parent/3.3.6/pom
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
        artifact = "org.elasticsearch.client:elasticsearch-rest-client:8.15.2",
        sha1 = "ac92c1b9a542982b9a12683b608989e1a99e864c",
    )

    # elasticsearch-rest-client explicitly depends on this version
    maven_jar(
        name = "httpasyncclient",
        artifact = "org.apache.httpcomponents:httpasyncclient:4.1.5",
        sha1 = "cd18227f1eb8e9a263286c1d7362ceb24f6f9b32",
    )

    # elasticsearch-rest-client explicitly depends on this version
    maven_jar(
        name = "httpcore-nio",
        artifact = "org.apache.httpcomponents:httpcore-nio:4.4.16",
        sha1 = "cd21c80a9956be48c4c1cfd2f594ba02857d0927",
    )
