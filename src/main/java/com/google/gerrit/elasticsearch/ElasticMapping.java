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

import com.google.common.collect.ImmutableMap;
import com.google.gerrit.index.FieldType;
import com.google.gerrit.index.Schema;
import com.google.gerrit.index.SchemaFieldDefs.SchemaField;
import com.google.gson.annotations.SerializedName;
import java.util.Map;

class ElasticMapping {

  protected static final String TIMESTAMP_FIELD_TYPE = "date";
  protected static final String TIMESTAMP_FIELD_FORMAT = "date_optional_time";

  static Mapping createMapping(Schema<?> schema, ElasticQueryAdapter adapter) {
    ElasticMapping.Builder mapping = new ElasticMapping.Builder(adapter);
    for (SchemaField<?, ?> field : schema.getSchemaFields().values()) {
      String name = field.getName();
      FieldType<?> fieldType = field.getType();
      if (fieldType == FieldType.EXACT) {
        mapping.addExactField(name);
      } else if (fieldType == FieldType.TIMESTAMP) {
        mapping.addTimestamp(name);
      } else if (fieldType == FieldType.INTEGER
          || fieldType == FieldType.INTEGER_RANGE
          || fieldType == FieldType.LONG) {
        mapping.addNumber(name);
      } else if (fieldType == FieldType.FULL_TEXT) {
        mapping.addStringWithAnalyzer(name, "custom_with_char_filter");
      } else if (fieldType == FieldType.PREFIX) {
        mapping.addStringWithAnalyzer(name, "keyword_tokenizer");
      } else if (fieldType == FieldType.STORED_ONLY) {
        mapping.addString(name);
      } else {
        throw new IllegalStateException("Unsupported field type: " + fieldType.getName());
      }
    }
    mapping.addSourceIncludes(
        schema.getSchemaFields().values().stream()
            .filter(f -> f.isStored())
            .map(f -> f.getName())
            .toArray(String[]::new));
    return mapping.build();
  }

  static class Builder {
    private final ElasticQueryAdapter adapter;
    private final ImmutableMap.Builder<String, FieldProperties> fields =
        new ImmutableMap.Builder<>();
    private final ImmutableMap.Builder<String, String[]> sourceIncludes =
        new ImmutableMap.Builder<>();

    Builder(ElasticQueryAdapter adapter) {
      this.adapter = adapter;
    }

    Mapping build() {
      Mapping mapping = new Mapping();
      mapping.properties = fields.build();
      mapping.source = sourceIncludes.build();
      return mapping;
    }

    Builder addExactField(String name) {
      fields.put(name, new FieldProperties(adapter.exactFieldType()));
      return this;
    }

    Builder addTimestamp(String name) {
      FieldProperties properties = new FieldProperties(TIMESTAMP_FIELD_TYPE);
      properties.type = TIMESTAMP_FIELD_TYPE;
      properties.format = TIMESTAMP_FIELD_FORMAT;
      fields.put(name, properties);
      return this;
    }

    Builder addNumber(String name) {
      fields.put(name, new FieldProperties("long"));
      return this;
    }

    Builder addString(String name) {
      fields.put(name, new FieldProperties(adapter.stringFieldType()));
      return this;
    }

    Builder addStringWithAnalyzer(String name, String analyzer) {
      FieldProperties key = new FieldProperties(adapter.stringFieldType());
      key.analyzer = analyzer;
      fields.put(name, key);
      return this;
    }

    Builder addSourceIncludes(String[] includes) {
      sourceIncludes.put("includes", includes);
      return this;
    }

    Builder add(String name, String type) {
      fields.put(name, new FieldProperties(type));
      return this;
    }
  }

  static class Mapping {
    @SerializedName("_source")
    Map<String, String[]> source;

    Map<String, FieldProperties> properties;
  }

  static class FieldProperties {
    String type;
    String index;
    String format;
    String analyzer;
    Map<String, FieldProperties> fields;

    FieldProperties(String type) {
      this.type = type;
    }
  }
}
