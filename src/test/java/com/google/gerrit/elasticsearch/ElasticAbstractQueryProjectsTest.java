// Copyright (C) 2024 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;
import static com.google.gerrit.testing.GerritJUnit.assertThrows;

import com.google.gerrit.exceptions.StorageException;
import com.google.gerrit.extensions.api.projects.ProjectApi;
import com.google.gerrit.server.index.project.ProjectIndexDefinition;
import com.google.gerrit.server.query.project.AbstractQueryProjectsTest;
import com.google.gerrit.testing.ConfigSuite;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.eclipse.jgit.lib.Config;
import org.junit.AfterClass;
import org.junit.Test;

public abstract class ElasticAbstractQueryProjectsTest extends AbstractQueryProjectsTest {
  @ConfigSuite.Default
  public static Config defaultConfig() {
    return ElasticTestUtils.createConfig();
  }

  @ConfigSuite.Config
  public static Config searchAfterPaginationType() {
    Config config = defaultConfig();
    config.setString("index", null, "paginationType", "SEARCH_AFTER");
    return config;
  }

  private static ElasticContainer container;
  private static CloseableHttpAsyncClient client;

  protected static void startIndexService(ElasticVersion elasticVersion) {
    container = ElasticContainer.createAndStart(elasticVersion);
    client = ElasticTestUtils.createHttpAsyncClient(container);
    client.start();
  }

  @AfterClass
  public static void stopElasticsearchServer() {
    if (container != null) {
      container.stop();
    }
  }

  @Inject private ProjectIndexDefinition projectIndexDefinition;

  @Override
  protected void initAfterLifecycleStart() throws Exception {
    super.initAfterLifecycleStart();
    ElasticTestUtils.createAllIndexes(injector);
  }

  @Override
  protected Injector createInjector() {
    return ElasticTestUtils.createInjector(config, testName, container);
  }

  @Test
  public void testErrorResponseFromProjectIndex() throws Exception {
    ProjectApi project = gApi.projects().create("test");
    project.index(false);

    ElasticTestUtils.closeIndex(client, container, testName);
    StorageException thrown = assertThrows(StorageException.class, () -> project.index(false));
    assertThat(thrown).hasMessageThat().contains("Failed to replace project");
  }

  @Test
  public void testNumCount() throws Exception {
    assertThat(projectIndexDefinition.getIndexCollection().getSearchIndex().numDocs())
        .isGreaterThan(-1);
  }
}
