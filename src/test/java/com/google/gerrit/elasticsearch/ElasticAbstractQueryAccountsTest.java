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
import com.google.gerrit.server.index.account.AccountIndexDefinition;
import com.google.gerrit.server.query.account.AbstractQueryAccountsTest;
import com.google.gerrit.testing.ConfigSuite;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.eclipse.jgit.lib.Config;
import org.junit.AfterClass;
import org.junit.Test;

public abstract class ElasticAbstractQueryAccountsTest extends AbstractQueryAccountsTest {
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

  @Inject private AccountIndexDefinition accountIndexDefinition;

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

  @Test
  public void testNumDocs() throws Exception {
    assertThat(accountIndexDefinition.getIndexCollection().getSearchIndex().numDocs())
        .isGreaterThan(-1);
  }
}
