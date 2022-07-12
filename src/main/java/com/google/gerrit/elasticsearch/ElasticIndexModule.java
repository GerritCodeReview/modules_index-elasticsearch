// Copyright (C) 2014 The Android Open Source Project
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.index.project.ProjectIndex;
import com.google.gerrit.server.ModuleImpl;
import com.google.gerrit.server.index.AbstractIndexModule;
import com.google.gerrit.server.index.VersionManager;
import com.google.gerrit.server.index.account.AccountIndex;
import com.google.gerrit.server.index.change.ChangeIndex;
import com.google.gerrit.server.index.group.GroupIndex;
import com.google.gerrit.server.index.options.AutoFlush;
import com.google.inject.Inject;
import java.util.Map;

@ModuleImpl(name = AbstractIndexModule.INDEX_MODULE)
public class ElasticIndexModule extends AbstractIndexModule {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final AutoFlush autoFlush;

  @VisibleForTesting
  public static ElasticIndexModule singleVersionWithExplicitVersions(
      Map<String, Integer> versions, int threads, boolean slave) {
    return new ElasticIndexModule(versions, threads, slave, AutoFlush.ENABLED);
  }

  public static ElasticIndexModule singleVersionWithExplicitVersions(
      Map<String, Integer> versions, int threads, boolean slave, AutoFlush autoFlush) {
    return new ElasticIndexModule(versions, threads, slave, autoFlush);
  }

  @Inject
  public ElasticIndexModule() {
    this(null, 0, false, AutoFlush.ENABLED);
  }

  protected ElasticIndexModule(
      Map<String, Integer> singleVersions, int threads, boolean slave, AutoFlush autoFlush) {
    super(singleVersions, threads, slave);
    this.autoFlush = autoFlush;
  }

  @Override
  public void configure() {
    logger.atInfo().log("Gerrit index backend set to ElasticSearch");
    super.configure();
    install(ElasticRestClientProvider.module());
    bind(AutoFlush.class).toInstance(autoFlush);
  }

  @Override
  protected Class<? extends AccountIndex> getAccountIndex() {
    return ElasticAccountIndex.class;
  }

  @Override
  protected Class<? extends ChangeIndex> getChangeIndex() {
    return ElasticChangeIndex.class;
  }

  @Override
  protected Class<? extends GroupIndex> getGroupIndex() {
    return ElasticGroupIndex.class;
  }

  @Override
  protected Class<? extends ProjectIndex> getProjectIndex() {
    return ElasticProjectIndex.class;
  }

  @Override
  protected Class<? extends VersionManager> getVersionManager() {
    return ElasticIndexVersionManager.class;
  }
}
