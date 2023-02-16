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
import org.apache.http.HttpHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

/* Helper class for running ES integration tests in docker container */
public class ElasticContainer extends ElasticsearchContainer {
  private static FluentLogger logger = FluentLogger.forEnclosingClass();
  private static final int ELASTICSEARCH_DEFAULT_PORT = 9200;

  public static ElasticContainer createAndStart(ElasticVersion version) {
    ElasticContainer container = new ElasticContainer(version);
    try {
      container.start();
    } catch (ContainerLaunchException e) {
      logger.atSevere().log(
          "Failed to launch elastic container. Logs from container :\n%s", container.getLogs());
      throw e;
    }
    return container;
  }

  private static String getImageName(ElasticVersion version) {
    switch (version) {
      case V7_16:
        return "docker.elastic.co/elasticsearch/elasticsearch:7.16.2";
    }
    throw new IllegalStateException("No tests for version: " + version.name());
  }

  private ElasticContainer(ElasticVersion version) {
    super(DockerImageName.parse(getImageName(version)));
  }

  @Override
  protected Logger logger() {
    return LoggerFactory.getLogger("org.testcontainers");
  }

  public HttpHost getHttpHost() {
    return new HttpHost(getContainerIpAddress(), getMappedPort(ELASTICSEARCH_DEFAULT_PORT));
  }
}
