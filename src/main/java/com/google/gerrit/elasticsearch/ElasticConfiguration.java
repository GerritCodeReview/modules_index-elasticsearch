// Copyright (C) 2017 The Android Open Source Project
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

import static com.google.common.base.MoreObjects.firstNonNull;

import com.google.common.base.Strings;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.index.IndexConfig;
import com.google.gerrit.index.PaginationType;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.inject.Inject;
import com.google.inject.ProvisionException;
import com.google.inject.Singleton;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.http.HttpHost;
import org.eclipse.jgit.lib.Config;
import org.elasticsearch.client.RestClientBuilder;

@Singleton
public class ElasticConfiguration {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  static final String SECTION_ELASTICSEARCH = "elasticsearch";
  static final String KEY_PASSWORD = "password";
  static final String KEY_USERNAME = "username";
  static final String KEY_PREFIX = "prefix";
  static final String KEY_SERVER = "server";
  static final String KEY_NUMBER_OF_SHARDS = "numberOfShards";
  static final String KEY_NUMBER_OF_REPLICAS = "numberOfReplicas";
  static final String KEY_MAX_RESULT_WINDOW = "maxResultWindow";
  static final String KEY_CODEC = "codec";
  static final String KEY_CONNECT_TIMEOUT = "connectTimeout";
  static final String KEY_SOCKET_TIMEOUT = "socketTimeout";

  static final String DEFAULT_CODEC = "default";
  static final String DEFAULT_PORT = "9200";
  static final String DEFAULT_USERNAME = "elastic";
  static final int DEFAULT_NUMBER_OF_SHARDS = 1;
  static final int DEFAULT_NUMBER_OF_REPLICAS = 1;
  static final int DEFAULT_MAX_RESULT_WINDOW = Integer.MAX_VALUE;
  static final int DEFAULT_CONNECT_TIMEOUT = RestClientBuilder.DEFAULT_CONNECT_TIMEOUT_MILLIS;
  static final int DEFAULT_SOCKET_TIMEOUT = RestClientBuilder.DEFAULT_SOCKET_TIMEOUT_MILLIS;

  private final Config cfg;
  private final List<HttpHost> hosts;

  final String username;
  final String password;
  final int numberOfShards;
  final int numberOfReplicas;
  final int maxResultWindow;
  final String codec;
  final int connectTimeout;
  final int socketTimeout;
  final String prefix;

  @Inject
  ElasticConfiguration(@GerritServerConfig Config cfg, IndexConfig indexConfig) {
    if (PaginationType.NONE == indexConfig.paginationType()) {
      throw new ProvisionException(
          "The 'index.paginationType = NONE' configuration is not supported by Elasticsearch");
    }

    this.cfg = cfg;
    this.password = cfg.getString(SECTION_ELASTICSEARCH, null, KEY_PASSWORD);
    this.username =
        password == null
            ? null
            : firstNonNull(
                cfg.getString(SECTION_ELASTICSEARCH, null, KEY_USERNAME), DEFAULT_USERNAME);
    this.prefix = Strings.nullToEmpty(cfg.getString(SECTION_ELASTICSEARCH, null, KEY_PREFIX));
    this.numberOfShards =
        cfg.getInt(SECTION_ELASTICSEARCH, null, KEY_NUMBER_OF_SHARDS, DEFAULT_NUMBER_OF_SHARDS);
    this.numberOfReplicas =
        cfg.getInt(SECTION_ELASTICSEARCH, null, KEY_NUMBER_OF_REPLICAS, DEFAULT_NUMBER_OF_REPLICAS);
    this.maxResultWindow =
        cfg.getInt(SECTION_ELASTICSEARCH, null, KEY_MAX_RESULT_WINDOW, DEFAULT_MAX_RESULT_WINDOW);
    this.codec = firstNonNull(cfg.getString(SECTION_ELASTICSEARCH, null, KEY_CODEC), DEFAULT_CODEC);
    this.connectTimeout =
        (int)
            cfg.getTimeUnit(
                SECTION_ELASTICSEARCH,
                null,
                KEY_CONNECT_TIMEOUT,
                DEFAULT_CONNECT_TIMEOUT,
                TimeUnit.MILLISECONDS);
    this.socketTimeout =
        (int)
            cfg.getTimeUnit(
                SECTION_ELASTICSEARCH,
                null,
                KEY_SOCKET_TIMEOUT,
                DEFAULT_SOCKET_TIMEOUT,
                TimeUnit.MILLISECONDS);
    this.hosts = new ArrayList<>();
    for (String server : cfg.getStringList(SECTION_ELASTICSEARCH, null, KEY_SERVER)) {
      try {
        URI uri = new URI(server);
        int port = uri.getPort();
        HttpHost httpHost =
            new HttpHost(
                uri.getHost(), port == -1 ? Integer.valueOf(DEFAULT_PORT) : port, uri.getScheme());
        this.hosts.add(httpHost);
      } catch (URISyntaxException | IllegalArgumentException e) {
        logger.atSevere().log("Invalid server URI %s: %s", server, e.getMessage());
      }
    }

    if (hosts.isEmpty()) {
      throw new ProvisionException("No valid Elasticsearch servers configured");
    }

    logger.atInfo().log("Elasticsearch servers: %s", hosts);
  }

  Config getConfig() {
    return cfg;
  }

  HttpHost[] getHosts() {
    return hosts.toArray(new HttpHost[hosts.size()]);
  }

  String getIndexName(String name, int schemaVersion) {
    return String.format("%s%s_%04d", prefix, name, schemaVersion);
  }

  int getNumberOfShards() {
    return numberOfShards;
  }
}
