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

import com.google.gerrit.entities.Change;
import com.google.gerrit.entities.Project;
import com.google.gerrit.exceptions.StorageException;
import com.google.gerrit.server.index.change.ChangeIndexDefinition;
import com.google.gerrit.server.query.change.AbstractQueryChangesTest;
import com.google.gerrit.testing.ConfigSuite;
import com.google.gerrit.testing.GerritTestName;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Repository;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;

public abstract class ElasticAbstractQueryChangesTest extends AbstractQueryChangesTest {
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

  @Rule public final GerritTestName testName = new GerritTestName();
  @Inject private ChangeIndexDefinition changeIndexDefinition;

  @After
  public void closeIndex() throws Exception {
    // Close the index after each test to prevent exceeding Elasticsearch's
    // shard limit (see Issue 10120).
    ElasticTestUtils.closeIndex(client, container, testName);
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
  public void testErrorResponseFromChangeIndex() throws Exception {
    Project.NameKey project = Project.nameKey("repo");
    TestRepository<Repository> repo = createAndOpenProject(project);
    Change c = insert(project, newChangeWithStatus(repo, Change.Status.NEW));
    gApi.changes().id(c.getProject().get(), c.getChangeId()).index();

    ElasticTestUtils.closeIndex(client, container, testName);
    StorageException thrown =
        assertThrows(
            StorageException.class,
            () -> gApi.changes().id(c.getProject().get(), c.getChangeId()).index());
    assertThat(thrown).hasMessageThat().contains("Failed to reindex change");
  }

  @Test
  public void testNumDocs() throws Exception {
    assertThat(changeIndexDefinition.getIndexCollection().getSearchIndex().numDocs())
        .isGreaterThan(-1);
  }
}
