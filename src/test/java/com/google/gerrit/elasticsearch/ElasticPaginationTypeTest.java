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
import static org.junit.Assume.assumeTrue;

import com.google.gerrit.entities.Account;
import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.index.PaginationType;
import com.google.gerrit.lifecycle.LifecycleManager;
import com.google.gerrit.server.*;
import com.google.gerrit.server.account.*;
import com.google.gerrit.server.schema.SchemaCreator;
import com.google.gerrit.server.util.ManualRequestContext;
import com.google.gerrit.server.util.OneOffRequestContext;
import com.google.gerrit.server.util.RequestContext;
import com.google.gerrit.server.util.ThreadLocalRequestContext;
import com.google.gerrit.testing.ConfigSuite;
import com.google.gerrit.testing.GerritJUnit;
import com.google.gerrit.testing.GerritServerTests;
import com.google.gerrit.testing.GerritTestName;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.eclipse.jgit.lib.Config;
import org.junit.*;

public class ElasticPaginationTypeTest extends GerritServerTests {

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

  @ConfigSuite.Config
  public static Config nonePaginationType() {
    Config config = defaultConfig();
    config.setString("index", null, "paginationType", "NONE");
    return config;
  }

  @Rule public final GerritTestName testName = new GerritTestName();

  @Inject @ServerInitiated protected Provider<AccountsUpdate> accountsUpdate;

  @Inject protected AccountManager accountManager;

  @Inject protected GerritApi gApi;

  @Inject protected IdentifiedUser.GenericFactory userFactory;

  @Inject protected SchemaCreator schemaCreator;

  @Inject protected ThreadLocalRequestContext requestContext;

  @Inject protected OneOffRequestContext oneOffRequestContext;

  @Inject protected AuthRequest.Factory authRequestFactory;

  private LifecycleManager lifecycle;
  private Injector injector;
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

  @Before
  public void setUpInjector() throws Exception {
    lifecycle = new LifecycleManager();
    injector = ElasticTestUtils.createInjector(config, testName, container);
    lifecycle.add(injector);
    injector.injectMembers(this);
    lifecycle.start();
    ElasticTestUtils.createAllIndexes(injector);
    schemaCreator.create();
    Account.Id adminId = createAccount("admin", "Administrator", "admin@example.com", true);
    userFactory.create(adminId);
    requestContext.setContext(newRequestContext(adminId));
  }

  @After
  public void tearDownInjector() {
    if (lifecycle != null) {
      lifecycle.stop();
    }
    requestContext.setContext(null);
  }

  @Test
  public void paginationTypeNoneNotSupported() {
    assumeTrue(PaginationType.NONE == getCurrentPaginationType());

    assertPaginationNotSupported(
        () -> {
          gApi.accounts().query("notexisting").get();
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

  @Test
  public void paginationTypeOffsetOrSearchAfterSupported() throws Exception {
    assumeTrue(PaginationType.NONE != getCurrentPaginationType());

    assertThat(gApi.accounts().query("notexisting").get()).isEmpty();
    assertThat(gApi.projects().query("state:active").get()).isEmpty();
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

  private Account.Id createAccount(String username, String fullName, String email, boolean active)
      throws Exception {
    try (ManualRequestContext ctx = oneOffRequestContext.open()) {
      Account.Id id =
          accountManager.authenticate(authRequestFactory.createForUser(username)).getAccountId();
      if (email != null) {
        accountManager.link(id, authRequestFactory.createForEmail(email));
      }
      accountsUpdate
          .get()
          .update(
              "Update Test Account",
              id,
              u -> {
                u.setFullName(fullName).setPreferredEmail(email).setActive(active);
              });
      return id;
    }
  }

  private RequestContext newRequestContext(Account.Id requestUserId) {
    final CurrentUser requestUser = userFactory.create(requestUserId);
    return () -> requestUser;
  }

  private PaginationType getCurrentPaginationType() {
    return config.getEnum("index", null, "paginationType", PaginationType.OFFSET);
  }
}
