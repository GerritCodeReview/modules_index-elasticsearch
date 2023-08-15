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
import static com.google.gerrit.elasticsearch.ElasticConfiguration.DEFAULT_USERNAME;
import static com.google.gerrit.elasticsearch.ElasticConfiguration.KEY_PASSWORD;
import static com.google.gerrit.elasticsearch.ElasticConfiguration.KEY_PREFIX;
import static com.google.gerrit.elasticsearch.ElasticConfiguration.KEY_SERVER;
import static com.google.gerrit.elasticsearch.ElasticConfiguration.KEY_USERNAME;
import static com.google.gerrit.elasticsearch.ElasticConfiguration.SECTION_ELASTICSEARCH;
import static com.google.gerrit.testing.GerritJUnit.assertThrows;
import static java.util.stream.Collectors.toList;

import com.google.common.collect.ImmutableList;
import com.google.gerrit.index.IndexConfig;
import com.google.inject.ProvisionException;
import java.util.Arrays;
import org.apache.http.HttpHost;
import org.eclipse.jgit.lib.Config;
import org.junit.Test;

public class ElasticConfigurationTest {
  @Test
  public void singleServerNoOtherConfig() throws Exception {
    Config cfg = newConfig();
    ElasticConfiguration esCfg = newElasticConfig(cfg);
    assertHosts(esCfg, "http://elastic:1234");
    assertThat(esCfg.username).isNull();
    assertThat(esCfg.password).isNull();
    assertThat(esCfg.prefix).isEmpty();
  }

  @Test
  public void serverWithoutPortSpecified() throws Exception {
    Config cfg = new Config();
    cfg.setString(SECTION_ELASTICSEARCH, null, KEY_SERVER, "http://elastic");
    ElasticConfiguration esCfg = newElasticConfig(cfg);
    assertHosts(esCfg, "http://elastic:9200");
  }

  @Test
  public void prefix() throws Exception {
    Config cfg = newConfig();
    cfg.setString(SECTION_ELASTICSEARCH, null, KEY_PREFIX, "myprefix");
    ElasticConfiguration esCfg = newElasticConfig(cfg);
    assertThat(esCfg.prefix).isEqualTo("myprefix");
  }

  @Test
  public void withAuthentication() throws Exception {
    Config cfg = newConfig();
    cfg.setString(SECTION_ELASTICSEARCH, null, KEY_USERNAME, "myself");
    cfg.setString(SECTION_ELASTICSEARCH, null, KEY_PASSWORD, "s3kr3t");
    ElasticConfiguration esCfg = newElasticConfig(cfg);
    assertThat(esCfg.username).isEqualTo("myself");
    assertThat(esCfg.password).isEqualTo("s3kr3t");
  }

  @Test
  public void withAuthenticationPasswordOnlyUsesDefaultUsername() throws Exception {
    Config cfg = newConfig();
    cfg.setString(SECTION_ELASTICSEARCH, null, KEY_PASSWORD, "s3kr3t");
    ElasticConfiguration esCfg = newElasticConfig(cfg);
    assertThat(esCfg.username).isEqualTo(DEFAULT_USERNAME);
    assertThat(esCfg.password).isEqualTo("s3kr3t");
  }

  @Test
  public void multipleServers() throws Exception {
    Config cfg = new Config();
    cfg.setStringList(
        SECTION_ELASTICSEARCH,
        null,
        KEY_SERVER,
        ImmutableList.of("http://elastic1:1234", "http://elastic2:1234"));
    ElasticConfiguration esCfg = newElasticConfig(cfg);
    assertHosts(esCfg, "http://elastic1:1234", "http://elastic2:1234");
  }

  @Test
  public void noServers() throws Exception {
    assertProvisionException(new Config(), "No valid Elasticsearch servers configured");
  }

  @Test
  public void singleServerInvalid() throws Exception {
    Config cfg = new Config();
    cfg.setString(SECTION_ELASTICSEARCH, null, KEY_SERVER, "foo");
    assertProvisionException(cfg, "No valid Elasticsearch servers configured");
  }

  @Test
  public void multipleServersIncludingInvalid() throws Exception {
    Config cfg = new Config();
    cfg.setStringList(
        SECTION_ELASTICSEARCH, null, KEY_SERVER, ImmutableList.of("http://elastic1:1234", "foo"));
    ElasticConfiguration esCfg = newElasticConfig(cfg);
    assertHosts(esCfg, "http://elastic1:1234");
  }

  @Test
  public void unsupportedPaginationTypeNone() {
    Config cfg = new Config();
    cfg.setString("index", null, "paginationType", "NONE");
    assertProvisionException(
        cfg, "The 'index.paginationType = NONE' configuration is not supported by Elasticsearch");
  }

  private static Config newConfig() {
    Config config = new Config();
    config.setString(SECTION_ELASTICSEARCH, null, KEY_SERVER, "http://elastic:1234");
    return config;
  }

  private static ElasticConfiguration newElasticConfig(Config cfg) {
    return new ElasticConfiguration(cfg, IndexConfig.fromConfig(cfg).build());
  }

  private void assertHosts(ElasticConfiguration cfg, Object... hostURIs) throws Exception {
    assertThat(Arrays.asList(cfg.getHosts()).stream().map(HttpHost::toURI).collect(toList()))
        .containsExactly(hostURIs);
  }

  private void assertProvisionException(Config cfg, String msg) {
    ProvisionException thrown = assertThrows(ProvisionException.class, () -> newElasticConfig(cfg));
    assertThat(thrown).hasMessageThat().contains(msg);
  }
}
