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
import static com.google.gerrit.testing.GerritJUnit.assertThrows;

import org.junit.Test;

public class ElasticVersionTest {
  @Test
  public void supportedVersion() throws Exception {
    assertThat(ElasticVersion.forVersion("7.0.0-alpha1")).isEqualTo(ElasticVersion.V7);
    assertThat(ElasticVersion.forVersion("7.0.0-beta1")).isEqualTo(ElasticVersion.V7);
    assertThat(ElasticVersion.forVersion("7.0.0-rc2")).isEqualTo(ElasticVersion.V7);
    assertThat(ElasticVersion.forVersion("7.0.10")).isEqualTo(ElasticVersion.V7);
    assertThat(ElasticVersion.forVersion("7.16.0")).isEqualTo(ElasticVersion.V7);
    assertThat(ElasticVersion.forVersion("7.16.1")).isEqualTo(ElasticVersion.V7);
    assertThat(ElasticVersion.forVersion("7.17.0")).isEqualTo(ElasticVersion.V7);
    assertThat(ElasticVersion.forVersion("7.17.1")).isEqualTo(ElasticVersion.V7);

    assertThat(ElasticVersion.forVersion("8.0.0-alpha1")).isEqualTo(ElasticVersion.V8);
    assertThat(ElasticVersion.forVersion("8.0.0-beta1")).isEqualTo(ElasticVersion.V8);
    assertThat(ElasticVersion.forVersion("8.0.0-rc2")).isEqualTo(ElasticVersion.V8);
    assertThat(ElasticVersion.forVersion("8.0.10")).isEqualTo(ElasticVersion.V8);
    assertThat(ElasticVersion.forVersion("8.9.0")).isEqualTo(ElasticVersion.V8);
    assertThat(ElasticVersion.forVersion("8.9.1")).isEqualTo(ElasticVersion.V8);
    assertThat(ElasticVersion.forVersion("8.9.2")).isEqualTo(ElasticVersion.V8);
    assertThat(ElasticVersion.forVersion("8.15.0")).isEqualTo(ElasticVersion.V8);
    assertThat(ElasticVersion.forVersion("8.15.1")).isEqualTo(ElasticVersion.V8);
    assertThat(ElasticVersion.forVersion("8.15.2")).isEqualTo(ElasticVersion.V8);
  }

  @Test
  public void unsupportedVersion() throws Exception {
    ElasticVersion.UnsupportedVersion thrown =
        assertThrows(
            ElasticVersion.UnsupportedVersion.class, () -> ElasticVersion.forVersion("4.0.0"));
    assertThat(thrown)
        .hasMessageThat()
        .contains(
            "Unsupported version: [4.0.0]. Supported versions: "
                + ElasticVersion.supportedVersions());
  }
}
