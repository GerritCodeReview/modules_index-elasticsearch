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

import com.google.gerrit.exceptions.StorageException;
import com.google.gerrit.server.query.account.AbstractQueryAccountsTest;
import com.google.gerrit.testing.ConfigSuite;
import com.google.inject.Injector;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.eclipse.jgit.lib.Config;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ElasticV7QueryAccountsTest extends AbstractQueryAccountsTest {
  @ConfigSuite.Default
  public static Config pitEnabled() {
    Config cfg = ElasticTestUtils.createConfig();
    cfg.setBoolean("elasticsearch", null, "enablePit", true);
    return cfg;
  }

  @ConfigSuite.Default
  public static Config pitDisabled() {
    Config cfg = ElasticTestUtils.createConfig();
    cfg.setBoolean("elasticsearch", null, "enablePit", false);
    return cfg;
  }

  private static ElasticContainer container;
  private static CloseableHttpAsyncClient client;

  @BeforeClass
  public static void startIndexService() {
    if (container == null) {
      // Only start Elasticsearch once
      container = ElasticContainer.createAndStart(ElasticVersion.V7_10);
      client = HttpAsyncClients.createDefault();
      client.start();
    }
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
  public void testErrorResponseFromAccountIndex() throws Exception {
    gApi.accounts().self().index();

    ElasticTestUtils.closeIndex(client, container, testName);
    StorageException thrown =
        assertThrows(StorageException.class, () -> gApi.accounts().self().index());
    assertThat(thrown).hasMessageThat().contains("Failed to replace account");
  }
}
