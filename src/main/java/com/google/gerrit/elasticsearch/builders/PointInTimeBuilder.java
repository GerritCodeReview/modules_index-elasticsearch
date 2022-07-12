// Copyright (C) 2022 The Android Open Source Project, 2009-2015 Elasticsearch
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

package com.google.gerrit.elasticsearch.builders;

import com.google.gerrit.elasticsearch.TimeValue;
import java.io.IOException;

/** A trimmed down and modified version of org.elasticsearch.search.builder.PointInTimeBuilder. */
public final class PointInTimeBuilder {
  private final String id;
  private TimeValue keepAlive;

  public PointInTimeBuilder(String id, TimeValue keepAlive) {
    this.id = id;
    this.keepAlive = keepAlive;
  }

  public void innerToXContent(XContentBuilder builder) throws IOException {
    builder.startObject("pit");
    builder.field("id", id);
    if (keepAlive != null) {
      builder.field("keep_alive", keepAlive);
    }
    builder.endObject();
  }

  /** Returns the id of this point in time */
  public String getId() {
    return id;
  }

  /** Returns the keep alive time of this point in time */
  public TimeValue getKeepAlive() {
    return keepAlive;
  }
}
