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

import static com.google.common.truth.Truth.assertThat;
import static com.google.gerrit.testing.GerritJUnit.assertThrows;
import static org.junit.Assume.assumeTrue;

import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.gerrit.acceptance.config.GerritConfig;
import com.google.gerrit.exceptions.StorageException;
import com.google.gerrit.extensions.api.groups.GroupApi;
import com.google.gerrit.server.query.group.AbstractQueryGroupsTest;
import com.google.gerrit.testing.ConfigSuite;
import com.google.gerrit.testing.GerritTestName;
import com.google.inject.Injector;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.eclipse.jgit.lib.Config;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

public class ElasticV7QueryGroupsTest extends AbstractQueryGroupsTest {
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

  @BeforeClass
  public static void startIndexService() {
    container = ElasticContainer.createAndStart(ElasticVersion.V7_16);
    client = HttpAsyncClients.createDefault();
    client.start();
  }

  @AfterClass
  public static void stopElasticsearchServer() {
    if (container != null) {
      container.stop();
    }
  }

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
  public void testErrorResponseFromGroupIndex() throws Exception {
    GroupApi group = gApi.groups().create("test");
    group.index();

    ElasticTestUtils.closeIndex(client, container, testName);
    StorageException thrown = assertThrows(StorageException.class, () -> group.index());
    assertThat(thrown).hasMessageThat().contains("Failed to replace group");
  }

  @Rule
  public GerritTestName testName = new GerritTestName();

  @Override
  protected void setUpDatabase() throws Exception {
    assumeTrue(testName.equals("testErrorResponseFromGroupIndexWithPaginationTypeNone"));
    super.setUpDatabase();
  }

  @Test
  @GerritConfig(name = "index.paginationType", value = "NONE")
  public void testErrorResponseFromGroupIndexWithPaginationTypeNone() throws Exception {
    Exception thrown = assertThrows(UncheckedExecutionException.class, () -> {
      super.setUpDatabase();
    });
    assertThat(thrown.getCause()).isInstanceOf(IllegalArgumentException.class);
    assertThat(thrown).hasMessageThat().contains("PaginationType NONE is not supported by Elasticsearch");
  }
}
