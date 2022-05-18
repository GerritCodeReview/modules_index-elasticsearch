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

import com.google.gerrit.index.IndexDefinition;
import com.google.gerrit.server.LibModuleType;
import com.google.gerrit.testing.GerritTestName;
import com.google.gerrit.testing.InMemoryModule;
import com.google.gerrit.testing.IndexConfig;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import java.util.Collection;
import java.util.UUID;
import org.eclipse.jgit.lib.Config;

public final class ElasticTestUtils {
  public static void configure(Config config, ElasticContainer container, String prefix) {
    String hostname = container.getHttpHost().getHostName();
    int port = container.getHttpHost().getPort();
    config.setString("index", null, "type", "elasticsearch");
    config.setString("elasticsearch", null, "server", "http://" + hostname + ":" + port);
    config.setString("elasticsearch", null, "prefix", prefix);
    config.setInt("index", null, "maxLimit", 10000);
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

  public static Injector createInjector(
      Config config, GerritTestName testName, ElasticContainer container) {
    Config elasticsearchConfig = new Config(config);
    ElasticTestUtils.configureElasticModule(elasticsearchConfig);
    InMemoryModule.setDefaults(elasticsearchConfig);
    String indicesPrefix = testName.getSanitizedMethodName();
    ElasticTestUtils.configure(elasticsearchConfig, container, indicesPrefix);
    return Guice.createInjector(new InMemoryModule(elasticsearchConfig));
  }

  private ElasticTestUtils() {
    // hide default constructor
  }
}
