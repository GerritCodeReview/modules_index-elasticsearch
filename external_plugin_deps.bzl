load("//tools/bzl:maven_jar.bzl", "maven_jar")

def external_plugin_deps():
    maven_jar(
        name = "jackson-core",
        artifact = "com.fasterxml.jackson.core:jackson-core:2.11.3",
        sha1 = "c2351800432bdbdd8284c3f5a7f0782a352aa84a",
    )

    # Ensure artifacts compatibility by selecting them from the Bill Of Materials
    # https://search.maven.org/artifact/org.testcontainers/testcontainers/1.17.5/pom
    TESTCONTAINERS_VERSION = "1.17.5"

    maven_jar(
        name = "testcontainers",
        artifact = "org.testcontainers:testcontainers:" + TESTCONTAINERS_VERSION,
        sha1 = "7c5ad975fb789ecd09b1ee5f72907f48a300bc61",
    )

    maven_jar(
        name = "testcontainers-elasticsearch",
        artifact = "org.testcontainers:elasticsearch:" + TESTCONTAINERS_VERSION,
        sha1 = "060895f2fc6640ab4a6c383bc98c5c39ef644fbb",
    )

    maven_jar(
        name = "duct-tape",
        artifact = "org.rnorth.duct-tape:duct-tape:1.0.8",
        sha1 = "92edc22a9ab2f3e17c9bf700aaee377d50e8b530",
    )

    DOCKER_JAVA_VERS = "3.2.13"

    maven_jar(
        name = "docker-java-api",
        artifact = "com.github.docker-java:docker-java-api:" + DOCKER_JAVA_VERS,
        sha1 = "5817ef8f770cb7e740d590090bf352df9491f3c1",
    )

    maven_jar(
        name = "docker-java-transport",
        artifact = "com.github.docker-java:docker-java-transport:" + DOCKER_JAVA_VERS,
        sha1 = "e9d308d1822181a9d48c99739f5eca014ec89199",
    )

    maven_jar(
        name = "docker-java-transport-zerodep",
        artifact = "com.github.docker-java:docker-java-transport-zerodep:" + DOCKER_JAVA_VERS,
        sha1 = "4cbc2c09d6c264767a39624066987ed4a152bc68",
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
