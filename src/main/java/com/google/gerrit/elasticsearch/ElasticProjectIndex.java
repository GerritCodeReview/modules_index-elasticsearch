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

import com.google.common.collect.ImmutableSet;
import com.google.gerrit.elasticsearch.ElasticMapping.Mapping;
import com.google.gerrit.elasticsearch.bulk.BulkRequest;
import com.google.gerrit.elasticsearch.bulk.IndexRequest;
import com.google.gerrit.elasticsearch.bulk.UpdateRequest;
import com.google.gerrit.entities.Project;
import com.google.gerrit.exceptions.StorageException;
import com.google.gerrit.index.QueryOptions;
import com.google.gerrit.index.Schema;
import com.google.gerrit.index.project.ProjectData;
import com.google.gerrit.index.project.ProjectField;
import com.google.gerrit.index.project.ProjectIndex;
import com.google.gerrit.index.query.DataSource;
import com.google.gerrit.index.query.Predicate;
import com.google.gerrit.index.query.QueryParseException;
import com.google.gerrit.server.config.SitePaths;
import com.google.gerrit.server.index.IndexUtils;
import com.google.gerrit.server.index.options.AutoFlush;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.project.ProjectState;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import java.util.Optional;
import java.util.Set;
import org.apache.http.HttpStatus;
import org.elasticsearch.client.Response;

public class ElasticProjectIndex extends AbstractElasticIndex<Project.NameKey, ProjectData>
    implements ProjectIndex {
  static class ProjectMapping {
    Mapping projects;

    ProjectMapping(Schema<ProjectData> schema, ElasticQueryAdapter adapter) {
      this.projects = ElasticMapping.createMapping(schema, adapter);
    }
  }

  static final String PROJECTS = "projects";

  private final ProjectMapping mapping;
  private final Provider<ProjectCache> projectCache;
  private final Schema<ProjectData> schema;

  @Inject
  ElasticProjectIndex(
      ElasticConfiguration cfg,
      SitePaths sitePaths,
      Provider<ProjectCache> projectCache,
      ElasticRestClientProvider client,
      AutoFlush autoFlush,
      @Assisted Schema<ProjectData> schema) {
    super(cfg, sitePaths, schema, client, PROJECTS, autoFlush, ProjectIndex.ENTITY_TO_KEY);
    this.projectCache = projectCache;
    this.schema = schema;
    this.mapping = new ProjectMapping(schema, client.adapter());
  }

  @Override
  public void replace(ProjectData projectState) {
    BulkRequest bulk =
        new IndexRequest(projectState.getProject().getName(), indexName)
            .add(new UpdateRequest<>(schema, projectState, ImmutableSet.of()));

    String uri = getURI(BULK);
    Response response = postRequestWithRefreshParam(uri, bulk);
    int statusCode = response.getStatusLine().getStatusCode();
    if (hasErrors(response) || statusCode != HttpStatus.SC_OK) {
      throw new StorageException(
          String.format(
              "Failed to replace project %s in index %s: %s",
              projectState.getProject().getName(), indexName, statusCode));
    }
  }

  @Override
  public DataSource<ProjectData> getSource(Predicate<ProjectData> p, QueryOptions opts)
      throws QueryParseException {
    JsonArray sortArray = getSortArray(ProjectField.NAME_SPEC.getName());
    return new ElasticQuerySource(p, opts.filterFields(IndexUtils::projectFields), sortArray);
  }

  @Override
  protected String getDeleteActions(Project.NameKey nameKey) {
    return getDeleteRequest(nameKey);
  }

  @Override
  protected String getMappings() {
    return getMappingsForSingleType(mapping.projects);
  }

  @Override
  protected String getId(ProjectData projectState) {
    return projectState.getProject().getName();
  }

  @Override
  protected ProjectData fromDocument(JsonObject json, Set<String> fields) {
    JsonElement source = json.get("_source");
    if (source == null) {
      source = json.getAsJsonObject().get("fields");
    }

    Project.NameKey nameKey =
        Project.nameKey(
            source.getAsJsonObject().get(ProjectField.NAME_SPEC.getName()).getAsString());
    Optional<ProjectState> state = projectCache.get().get(nameKey);
    if (!state.isPresent()) {
      return null;
    }
    return state.get().toProjectData();
  }
}
