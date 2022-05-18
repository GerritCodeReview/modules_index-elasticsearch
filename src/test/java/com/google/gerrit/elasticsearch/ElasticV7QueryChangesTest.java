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

import static com.google.common.truth.TruthJUnit.assume;
import static java.util.concurrent.TimeUnit.MINUTES;
import com.google.gerrit.entities.Change;
import com.google.gerrit.server.change.ChangeInserter;
import com.google.gerrit.server.index.change.ChangeField;
import com.google.gerrit.server.query.change.AbstractQueryChangesTest;
import com.google.gerrit.testing.ConfigSuite;
import com.google.gerrit.testing.GerritTestName;
import com.google.gerrit.testing.InMemoryRepositoryManager.Repo;
import com.google.inject.Injector;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.lib.Config;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

public class ElasticV7QueryChangesTest extends AbstractQueryChangesTest {
  @ConfigSuite.Default
  public static Config defaultConfig() {
    return ElasticTestUtils.createConfig();
  }

  private static ElasticContainer container;
  private static CloseableHttpAsyncClient client;

  @BeforeClass
  public static void startIndexService() {
    if (container == null) {
      // Only start Elasticsearch once
      container = ElasticContainer.createAndStart(ElasticVersion.V7_8);
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

  @Rule public final GerritTestName testName = new GerritTestName();

  @After
  public void closeIndex() throws Exception {
    // Close the index after each test to prevent exceeding Elasticsearch's
    // shard limit (see Issue 10120).
    client
        .execute(
            new HttpPost(
                String.format(
                    "http://%s:%d/%s*/_close",
                    container.getHttpHost().getHostName(),
                    container.getHttpHost().getPort(),
                    testName.getSanitizedMethodName())),
            HttpClientContext.create(),
            null)
        .get(5, MINUTES);
  }

  @Override
  protected void initAfterLifecycleStart() throws Exception {
    super.initAfterLifecycleStart();
    ElasticTestUtils.createAllIndexes(injector);
  }

  @Test
  @Override
  public void byTopic() throws Exception {

    TestRepository<Repo> repo = createProject("repo");
    ChangeInserter ins1 = newChangeWithTopic(repo, "feature1");
    Change change1 = insert(repo, ins1);

    ChangeInserter ins2 = newChangeWithTopic(repo, "feature2");
    Change change2 = insert(repo, ins2);

    ChangeInserter ins3 = newChangeWithTopic(repo, "Cherrypick-feature2");
    Change change3 = insert(repo, ins3);

    ChangeInserter ins4 = newChangeWithTopic(repo, "feature2-fixup");
    Change change4 = insert(repo, ins4);

    ChangeInserter ins5 = newChangeWithTopic(repo, "https://gerrit.local");
    Change change5 = insert(repo, ins5);

    ChangeInserter ins6 = newChangeWithTopic(repo, "git_gerrit_training");
    Change change6 = insert(repo, ins6);

    Change change_no_topic = insert(repo, newChange(repo));

    assertQuery("intopic:foo");
    assertQuery("intopic:feature1", change1);
    assertQuery("intopic:feature2", change4, change3, change2);
    assertQuery("topic:feature2", change2);
    assertQuery("intopic:feature2", change4, change3, change2);
    assertQuery("intopic:fixup", change4);
    assertQuery("intopic:gerrit", change6, change5);
    assertQuery("topic:\"\"", change_no_topic);
    assertQuery("intopic:\"\"", change_no_topic);

    assume().that(getSchema().hasField(ChangeField.PREFIX_TOPIC)).isTrue();
    // change3 is considered by ES in prefixtopic:feature query, see
    // https://www.elastic.co/guide/en/elasticsearch/reference/8.2/query-dsl-match-query-phrase-prefix.html
    // assertQuery("prefixtopic:feature", change4, change2, change1);
    assertQuery("prefixtopic:feature", change4, change3, change2, change1);
    assertQuery("prefixtopic:Cher", change3);
    assertQuery("prefixtopic:feature22");
  }

  @Override
  protected Injector createInjector() {
    return ElasticTestUtils.createInjector(config, testName, container);
  }
}
