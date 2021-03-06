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

import com.google.gerrit.server.query.group.AbstractQueryGroupsTest;
import com.google.gerrit.testing.ConfigSuite;
import com.google.inject.Injector;
import org.eclipse.jgit.lib.Config;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class ElasticV7QueryGroupsTest extends AbstractQueryGroupsTest {
  @ConfigSuite.Default
  public static Config defaultConfig() {
    return ElasticTestUtils.createConfig();
  }

  private static ElasticContainer container;

  @BeforeClass
  public static void startIndexService() {
    if (container == null) {
      // Only start Elasticsearch once
      container = ElasticContainer.createAndStart(ElasticVersion.V7_8);
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
}
