// Copyright (C) 2023 The Android Open Source Project
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

import com.google.common.collect.ImmutableMap;
import com.google.gerrit.acceptance.AbstractDaemonTest;
import com.google.gerrit.acceptance.config.GerritConfig;
import com.google.gerrit.entities.GroupReference;
import com.google.gerrit.index.IndexDefinition;
import com.google.gerrit.testing.GerritJUnit;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import java.io.IOException;
import java.util.Collection;
import java.util.stream.Stream;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.Description;

public class ElasticPaginationTypeTest extends AbstractDaemonTest {

  private static ElasticContainer container;

  private final String NOT_SUPPORTED_PAGINATION_TEST_NAME = "paginationTypeNone";

  @BeforeClass
  public static void startIndexService() {
    container = ElasticContainer.createAndStart(ElasticVersion.V7_16);
    CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
    client.start();
  }

  @AfterClass
  public static void stopElasticsearchServer() {
    if (container != null) {
      container.stop();
    }
  }

  @Override
  protected void beforeTest(Description description) throws Exception {
    try {
      String elasticUrl =
          "http://"
              + container.getHttpHost().getHostName()
              + ":"
              + container.getHttpHost().getPort();
      baseConfig.setString("elasticsearch", null, "server", elasticUrl);
      baseConfig.setString("elasticsearch", null, "prefix", "pagtest_");
      baseConfig.setString("index", null, "type", "elasticsearch");
      super.beforeTest(description);
    } catch (Exception e) {
      if (!description.getMethodName().equals(NOT_SUPPORTED_PAGINATION_TEST_NAME)) {
        throw e;
      }
    }
  }

  @Override
  public void startEventRecorder() {
    if (!description.getMethodName().equals(NOT_SUPPORTED_PAGINATION_TEST_NAME)) {
      super.startEventRecorder();
    }
  }

  @Override
  public Module createModule() {
    return ElasticIndexModule.singleVersionWithExplicitVersions(ImmutableMap.of(), 0, false);
  }

  @Override
  protected void afterGerritStartup() {
    createAllIndices();
    reindexGroups();
  }

  @Test
  @GerritConfig(name = "index.paginationType", value = "SEARCH_AFTER")
  public void paginationTypeSearchAfter() throws Exception {
    assertAllIndicesAreQueryable();
  }

  @Test
  @GerritConfig(name = "index.paginationType", value = "OFFSET")
  public void paginationTypeOffset() throws Exception {
    assertAllIndicesAreQueryable();
  }

  @GerritConfig(name = "index.paginationType", value = "NONE")
  public void paginationTypeNone() throws Exception {
    assertPaginationNotSupported(
        () -> {
          gApi.accounts().query("username:admin").get();
        });
    assertPaginationNotSupported(
        () -> {
          gApi.projects().query("state:active").get();
        });
    assertPaginationNotSupported(
        () -> {
          gApi.groups().query("is:visibletoall").get();
        });
    assertPaginationNotSupported(
        () -> {
          gApi.changes().query("is:open").get();
        });
  }

  private void assertAllIndicesAreQueryable() throws Exception {
    assertThat(gApi.accounts().query("username:admin").get()).isNotNull();
    assertThat(gApi.projects().query("state:active").get()).isNotNull();
    assertThat(gApi.groups().query("is:visibletoall").get()).isEmpty();
    assertThat(gApi.changes().query("is:open").get()).isEmpty();
  }

  private void assertPaginationNotSupported(GerritJUnit.ThrowingRunnable runnable) {
    Exception thrown = assertThrows(RuntimeException.class, runnable);
    assertCauseIsIllegalArgumentException(thrown);
    assertThat(thrown)
        .hasMessageThat()
        .contains("PaginationType NONE is not supported by Elasticsearch");
  }

  private void assertCauseIsIllegalArgumentException(Exception thrown) {
    if (thrown.getCause() == null) {
      assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    } else {
      assertThat(thrown.getCause()).isInstanceOf(IllegalArgumentException.class);
    }
  }

  private void createAllIndices() {
    Collection<IndexDefinition<?, ?, ?>> indexDefs =
        server
            .getTestInjector()
            .getInstance(Key.get(new TypeLiteral<Collection<IndexDefinition<?, ?, ?>>>() {}));
    for (IndexDefinition<?, ?, ?> indexDef : indexDefs) {
      indexDef.getIndexCollection().getSearchIndex().deleteAll();
    }
  }

  private void reindexGroups() {
    Stream<GroupReference> allGroupReferences;
    try {
      allGroupReferences = groups.getAllGroupReferences();
    } catch (ConfigInvalidException | IOException e) {
      throw new IllegalStateException("Unable to reindex groups, tests may fail", e);
    }
    allGroupReferences.forEach(group -> groupIndexer.index(group.getUUID()));
  }
}
