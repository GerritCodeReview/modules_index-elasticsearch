// Copyright (C) 2022 The Android Open Source Project
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

import com.google.common.flogger.FluentLogger;
import com.google.gson.JsonArray;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

public class PointInTimeSearch {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  protected static final String MIN_PIT_SUPPORTED_ES_VERSION = "7.10";

  public String id;
  public TimeValue keepAlive;
  public JsonArray searchAfter;

  public PointInTimeSearch(String id, TimeValue keepAlive, JsonArray searchAfter) {
    this.id = id;
    this.keepAlive = keepAlive;
    this.searchAfter = searchAfter;
  }

  public static boolean usePit(ElasticConfiguration config, ElasticRestClientProvider client) {
    if (!config.enablePit) {
      return false;
    }
    String elasticVersion = client.elasticVersion();
    boolean isPitSupported = isPitSupported(elasticVersion);
    if (!isPitSupported) {
      logger.atWarning().log(
          String.format(
              "Point in Time is enabled in elasticsearch config,"
                  + " but cannot be used as current elasticsearch version %s is older than %s",
              elasticVersion, MIN_PIT_SUPPORTED_ES_VERSION));
    }
    return isPitSupported;
  }

  public static boolean isPitSupported(String elasticVersion) {
    if ((new DefaultArtifactVersion(elasticVersion))
            .compareTo(new DefaultArtifactVersion(MIN_PIT_SUPPORTED_ES_VERSION))
        >= 0) {
      return true;
    }
    return false;
  }
}
