// Copyright (C) 2018 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.gerrit.elasticsearch;

import com.google.common.flogger.FluentLogger;
import java.nio.file.Path;
import org.apache.http.HttpHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

/* Helper class for running ES integration tests in docker container */
public class ElasticContainer extends ElasticsearchContainer {
  private static FluentLogger logger = FluentLogger.forEnclosingClass();

  public static ElasticContainer createAndStart(ElasticVersion version) {
    @SuppressWarnings("resource")
    ElasticContainer container = new ElasticContainer(version);
    try {
      Path certs = Path.of("/usr/share/elasticsearch/config/certs");
      String customizedCertPath = certs.resolve("http_ca_customized.crt").toString();
      String sslKeyPath = certs.resolve("elasticsearch.key").toString();
      String sslCrtPath = certs.resolve("elasticsearch.crt").toString();
      container =
          (ElasticContainer)
              container
                  .withPassword(ElasticsearchContainer.ELASTICSEARCH_DEFAULT_PASSWORD)
                  .withEnv("xpack.security.enabled", "true")
                  .withEnv("xpack.security.http.ssl.enabled", "true")
                  .withEnv("xpack.security.http.ssl.key", sslKeyPath)
                  .withEnv("xpack.security.http.ssl.certificate", sslCrtPath)
                  .withEnv("xpack.security.http.ssl.certificate_authorities", customizedCertPath)
                  // Create our own cert so that the gerrit-ci docker hostname
                  // matches the certificate subject. Otherwise we get an error like:
                  // Host name '10.0.1.1' does not match the certificate subject provided by the
                  // peer (CN=4932da9bab1d)
                  .withCopyToContainer(
                      Transferable.of(
                          "#!/bin/bash\n"
                              + "mkdir -p "
                              + certs.toString()
                              + ";"
                              + "openssl req -x509 -newkey rsa:4096 -keyout "
                              + sslKeyPath
                              + " -out "
                              + sslCrtPath
                              + " -days 365 -nodes -subj \"/CN="
                              + container.getHost()
                              + "\";"
                              + "openssl x509 -outform der -in "
                              + sslCrtPath
                              + " -out "
                              + customizedCertPath
                              + "; chown -R elasticsearch "
                              + certs.toString(),
                          555),
                      "/usr/share/elasticsearch/generate-certs.sh")
                  // because we need to generate the certificates before Elasticsearch starts, the
                  // entry command has to be adjusted accordingly
                  .withCommand(
                      "sh",
                      "-c",
                      "/usr/share/elasticsearch/generate-certs.sh && /usr/local/bin/docker-entrypoint.sh")
                  .withCertPath(customizedCertPath);
      container.start();
    } catch (ContainerLaunchException e) {
      logger.atSevere().log(
          "Failed to launch elastic container. Logs from container :\n%s", container.getLogs());
      throw e;
    }
    return container;
  }

  private static DockerImageName getImageName(ElasticVersion version) {
    DockerImageName image = DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch");
    switch (version) {
      case V7:
        return image.withTag("7.17.24");
      case V8:
        return image.withTag("8.15.2");
    }
    throw new IllegalStateException("No tests for version: " + version.name());
  }

  private ElasticContainer(ElasticVersion version) {
    super(getImageName(version));
    withEnv("action.destructive_requires_name", "false");
  }

  @Override
  protected Logger logger() {
    return LoggerFactory.getLogger("org.testcontainers");
  }

  public HttpHost getHttpHost() {
    String protocol = caCertAsBytes().isPresent() ? "https://" : "http://";
    return HttpHost.create(protocol + getHttpHostAddress());
  }
}
