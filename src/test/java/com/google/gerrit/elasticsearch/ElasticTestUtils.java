// Copyright (C) 2016 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertWithMessage;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.testcontainers.elasticsearch.ElasticsearchContainer.ELASTICSEARCH_DEFAULT_PASSWORD;

import com.google.gerrit.index.IndexDefinition;
import com.google.gerrit.server.LibModuleType;
import com.google.gerrit.testing.GerritTestName;
import com.google.gerrit.testing.InMemoryModule;
import com.google.gerrit.testing.IndexConfig;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import java.util.Collection;
import java.util.UUID;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jgit.lib.Config;

public final class ElasticTestUtils {
  private static final String ELASTIC_USERNAME = "elastic";
  private static final String ELASTIC_PASSWORD = ELASTICSEARCH_DEFAULT_PASSWORD;

  public static void configure(Config config, ElasticContainer container, String prefix) {
    config.setString("index", null, "type", "elasticsearch");
    config.setString("elasticsearch", null, "server", container.getHttpHost().toURI());
    config.setString("elasticsearch", null, "prefix", prefix);
    config.setInt("index", null, "maxLimit", 10000);
    if (container.caCertAsBytes().isPresent()) {
      config.setString("elasticsearch", null, "username", ELASTIC_USERNAME);
      config.setString("elasticsearch", null, "password", ELASTIC_PASSWORD);
    }
  }

  public static void createAllIndexes(Injector injector) {
    Collection<IndexDefinition<?, ?, ?>> indexDefs =
        injector.getInstance(Key.get(new TypeLiteral<Collection<IndexDefinition<?, ?, ?>>>() {}));
    for (IndexDefinition<?, ?, ?> indexDef : indexDefs) {
      indexDef.getIndexCollection().getSearchIndex().deleteAll();
    }
  }

  public static Config getConfig(ElasticVersion version) {
    ElasticContainer container = ElasticContainer.createAndStart(version);
    String indicesPrefix = UUID.randomUUID().toString();
    Config cfg = new Config();
    configure(cfg, container, indicesPrefix);
    return cfg;
  }

  public static Config createConfig() {
    Config cfg = IndexConfig.create();

    // For some reason enabling the staleness checker increases the flakiness of the Elasticsearch
    // tests. Hence disable the staleness checker.
    cfg.setBoolean("index", null, "autoReindexIfStale", false);

    return cfg;
  }

  public static void configureElasticModule(Config elasticsearchConfig) {
    elasticsearchConfig.setString(
        "index",
        null,
        "install" + LibModuleType.INDEX_MODULE_TYPE.getConfigKey(),
        "com.google.gerrit.elasticsearch.ElasticIndexModule");
  }

  public static class ElasticContainerTestModule extends AbstractModule {
    private final ElasticContainer container;

    ElasticContainerTestModule(ElasticContainer container) {
      this.container = container;
    }

    @Override
    protected void configure() {
      bind(ElasticRestClientProvider.class).to(ElasticContainerRestClientProvider.class);
      bind(ElasticContainer.class).toInstance(container);
    }
  }

  public static Injector createInjector(
      Config config, GerritTestName testName, ElasticContainer container) {
    Config elasticsearchConfig = new Config(config);
    ElasticTestUtils.configureElasticModule(elasticsearchConfig);
    InMemoryModule.setDefaults(elasticsearchConfig);
    String indicesPrefix = testName.getSanitizedMethodName();
    ElasticTestUtils.configure(elasticsearchConfig, container, indicesPrefix);
    return Guice.createInjector(
        new ElasticContainerTestModule(container), new InMemoryModule(elasticsearchConfig));
  }

  public static CloseableHttpAsyncClient createHttpAsyncClient(ElasticContainer container) {
    HttpAsyncClientBuilder builder = HttpAsyncClients.custom();
    if (container.caCertAsBytes().isPresent()) {
      CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
      credentialsProvider.setCredentials(
          AuthScope.ANY, new UsernamePasswordCredentials(ELASTIC_USERNAME, ELASTIC_PASSWORD));
      builder
          .setSSLContext(container.createSslContextFromCa())
          .setDefaultCredentialsProvider(credentialsProvider);
    }
    return builder.build();
  }

  public static void closeIndex(
      CloseableHttpAsyncClient client, ElasticContainer container, GerritTestName testName)
      throws Exception {
    HttpResponse response =
        client
            .execute(
                new HttpPost(
                    String.format(
                        "%s/%s*/_close",
                        container.getHttpHost().toURI(), testName.getSanitizedMethodName())),
                HttpClientContext.create(),
                null)
            .get(5, MINUTES);
    int statusCode = response.getStatusLine().getStatusCode();
    assertWithMessage(
            "response status code should be %s, but was %s. Full response was %s",
            HttpStatus.SC_OK, statusCode, EntityUtils.toString(response.getEntity()))
        .that(statusCode)
        .isEqualTo(HttpStatus.SC_OK);
  }

  private ElasticTestUtils() {
    // hide default constructor
  }
}
