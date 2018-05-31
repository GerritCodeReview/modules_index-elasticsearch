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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.gson.FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.codec.binary.Base64.decodeBase64;

import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import com.google.gerrit.elasticsearch.builders.QueryBuilder;
import com.google.gerrit.elasticsearch.builders.SearchSourceBuilder;
import com.google.gerrit.elasticsearch.bulk.DeleteRequest;
import com.google.gerrit.index.FieldDef;
import com.google.gerrit.index.FieldType;
import com.google.gerrit.index.Index;
import com.google.gerrit.index.QueryOptions;
import com.google.gerrit.index.Schema;
import com.google.gerrit.index.query.DataSource;
import com.google.gerrit.index.query.FieldBundle;
import com.google.gerrit.index.query.Predicate;
import com.google.gerrit.index.query.QueryParseException;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.config.SitePaths;
import com.google.gerrit.server.index.IndexUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gwtorm.protobuf.ProtobufCodec;
import com.google.gwtorm.server.OrmException;
import com.google.gwtorm.server.ResultSet;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.eclipse.jgit.lib.Config;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractElasticIndex<K, V> implements Index<K, V> {
  private static final Logger log = LoggerFactory.getLogger(AbstractElasticIndex.class);

  protected static final String BULK = "_bulk";
  protected static final String IGNORE_UNMAPPED = "ignore_unmapped";
  protected static final String ORDER = "order";
  protected static final String SEARCH = "_search";

  protected static <T> List<T> decodeProtos(
      JsonObject doc, String fieldName, ProtobufCodec<T> codec) {
    JsonArray field = doc.getAsJsonArray(fieldName);
    if (field == null) {
      return null;
    }
    return FluentIterable.from(field)
        .transform(i -> codec.decode(decodeBase64(i.toString())))
        .toList();
  }

  static String getContent(Response response) throws IOException {
    HttpEntity responseEntity = response.getEntity();
    String content = "";
    if (responseEntity != null) {
      InputStream contentStream = responseEntity.getContent();
      try (Reader reader = new InputStreamReader(contentStream)) {
        content = CharStreams.toString(reader);
      }
    }
    return content;
  }

  private final Schema<V> schema;
  private final SitePaths sitePaths;
  private final String indexNameRaw;
  private final RestClient client;

  protected final String indexName;
  protected final Gson gson;
  protected final ElasticQueryBuilder queryBuilder;

  AbstractElasticIndex(
      @GerritServerConfig Config cfg,
      SitePaths sitePaths,
      Schema<V> schema,
      ElasticRestClientBuilder clientBuilder,
      String indexName) {
    this.sitePaths = sitePaths;
    this.schema = schema;
    this.gson = new GsonBuilder().setFieldNamingPolicy(LOWER_CASE_WITH_UNDERSCORES).create();
    this.queryBuilder = new ElasticQueryBuilder();
    this.indexName =
        String.format(
            "%s%s_%04d",
            Strings.nullToEmpty(cfg.getString("elasticsearch", null, "prefix")),
            indexName,
            schema.getVersion());
    this.indexNameRaw = indexName;
    this.client = clientBuilder.build();
  }

  @Override
  public Schema<V> getSchema() {
    return schema;
  }

  @Override
  public void close() {
    try {
      client.close();
    } catch (IOException e) {
      // Ignored.
    }
  }

  @Override
  public void markReady(boolean ready) throws IOException {
    IndexUtils.setReady(sitePaths, indexNameRaw, schema.getVersion(), ready);
  }

  @Override
  public void delete(K c) throws IOException {
    String uri = getURI(indexNameRaw, BULK);
    Response response = postRequest(addActions(c), uri, getRefreshParam());
    int statusCode = response.getStatusLine().getStatusCode();
    if (statusCode != HttpStatus.SC_OK) {
      throw new IOException(
          String.format("Failed to delete %s from index %s: %s", c, indexName, statusCode));
    }
  }

  @Override
  public void deleteAll() throws IOException {
    // Delete the index, if it exists.
    Response response = client.performRequest("HEAD", indexName);
    int statusCode = response.getStatusLine().getStatusCode();
    if (statusCode == HttpStatus.SC_OK) {
      response = client.performRequest("DELETE", indexName);
      statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != HttpStatus.SC_OK) {
        throw new IOException(
            String.format("Failed to delete index %s: %s", indexName, statusCode));
      }
    }

    // Recreate the index.
    response = performRequest("PUT", getMappings(), indexName, Collections.emptyMap());
    statusCode = response.getStatusLine().getStatusCode();
    if (statusCode != HttpStatus.SC_OK) {
      String error = String.format("Failed to create index %s: %s", indexName, statusCode);
      throw new IOException(error);
    }
  }

  protected abstract String addActions(K c);

  protected abstract String getMappings();

  protected abstract String getId(V v);

  protected String delete(String type, K c) {
    String id = c.toString();
    return new DeleteRequest(id, indexNameRaw, type).toString();
  }

  protected abstract V fromDocument(JsonObject doc, Set<String> fields);

  protected FieldBundle toFieldBundle(JsonObject doc) {
    Map<String, FieldDef<V, ?>> allFields = getSchema().getFields();
    ListMultimap<String, Object> rawFields = ArrayListMultimap.create();
    for (Map.Entry<String, JsonElement> element : doc.get("fields").getAsJsonObject().entrySet()) {
      checkArgument(
          allFields.containsKey(element.getKey()), "Unrecognized field " + element.getKey());
      FieldType<?> type = allFields.get(element.getKey()).getType();
      Iterable<JsonElement> innerItems =
          element.getValue().isJsonArray()
              ? element.getValue().getAsJsonArray()
              : Collections.singleton(element.getValue());
      for (JsonElement inner : innerItems) {
        if (type == FieldType.EXACT || type == FieldType.FULL_TEXT || type == FieldType.PREFIX) {
          rawFields.put(element.getKey(), inner.getAsString());
        } else if (type == FieldType.INTEGER || type == FieldType.INTEGER_RANGE) {
          rawFields.put(element.getKey(), inner.getAsInt());
        } else if (type == FieldType.LONG) {
          rawFields.put(element.getKey(), inner.getAsLong());
        } else if (type == FieldType.TIMESTAMP) {
          rawFields.put(element.getKey(), new Timestamp(inner.getAsLong()));
        } else if (type == FieldType.STORED_ONLY) {
          rawFields.put(element.getKey(), Base64.decodeBase64(inner.getAsString()));
        } else {
          throw FieldType.badFieldType(type);
        }
      }
    }
    return new FieldBundle(rawFields);
  }

  protected String toAction(String type, String id, String action) {
    JsonObject properties = new JsonObject();
    properties.addProperty("_id", id);
    properties.addProperty("_index", indexName);
    properties.addProperty("_type", type);

    JsonObject jsonAction = new JsonObject();
    jsonAction.add(action, properties);
    return jsonAction.toString() + System.lineSeparator();
  }

  protected void addNamedElement(String name, JsonObject element, JsonArray array) {
    JsonObject arrayElement = new JsonObject();
    arrayElement.add(name, element);
    array.add(arrayElement);
  }

  protected Map<String, String> getRefreshParam() {
    Map<String, String> params = new HashMap<>();
    params.put("refresh", "true");
    return params;
  }

  protected String getSearch(SearchSourceBuilder searchSource, JsonArray sortArray) {
    JsonObject search = new JsonParser().parse(searchSource.toString()).getAsJsonObject();
    search.add("sort", sortArray);
    return gson.toJson(search);
  }

  protected JsonArray getSortArray(String idFieldName) {
    JsonObject properties = new JsonObject();
    properties.addProperty(ORDER, "asc");
    properties.addProperty(IGNORE_UNMAPPED, true);

    JsonArray sortArray = new JsonArray();
    addNamedElement(idFieldName, properties, sortArray);
    return sortArray;
  }

  protected String getURI(String type, String request) throws UnsupportedEncodingException {
    String encodedType = URLEncoder.encode(type, UTF_8.toString());
    String encodedIndexName = URLEncoder.encode(indexName, UTF_8.toString());
    return encodedIndexName + "/" + encodedType + "/" + request;
  }

  protected Response postRequest(Object payload, String uri, Map<String, String> params)
      throws IOException {
    return performRequest("POST", payload, uri, params);
  }

  private Response performRequest(
      String method, Object payload, String uri, Map<String, String> params) throws IOException {
    String payloadStr = payload instanceof String ? (String) payload : payload.toString();
    HttpEntity entity = new NStringEntity(payloadStr, ContentType.APPLICATION_JSON);
    return client.performRequest(method, uri, params, entity);
  }

  protected class ElasticQuerySource implements DataSource<V> {
    private final QueryOptions opts;
    private final String search;
    private final String index;

    ElasticQuerySource(Predicate<V> p, QueryOptions opts, String index, JsonArray sortArray)
        throws QueryParseException {
      this.opts = opts;
      this.index = index;
      QueryBuilder qb = queryBuilder.toQueryBuilder(p);
      SearchSourceBuilder searchSource =
          new SearchSourceBuilder()
              .query(qb)
              .from(opts.start())
              .size(opts.limit())
              .fields(Lists.newArrayList(opts.fields()));
      search = getSearch(searchSource, sortArray);
    }

    @Override
    public int getCardinality() {
      return 10;
    }

    @Override
    public ResultSet<V> read() throws OrmException {
      return readImpl((doc) -> AbstractElasticIndex.this.fromDocument(doc, opts.fields()));
    }

    @Override
    public ResultSet<FieldBundle> readRaw() throws OrmException {
      return readImpl(AbstractElasticIndex.this::toFieldBundle);
    }

    private <T> ResultSet<T> readImpl(Function<JsonObject, T> mapper) throws OrmException {
      try {
        List<T> results = Collections.emptyList();
        String uri = getURI(index, SEARCH);
        Response response =
            performRequest(HttpPost.METHOD_NAME, search, uri, Collections.emptyMap());
        StatusLine statusLine = response.getStatusLine();
        if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
          String content = getContent(response);
          JsonObject obj =
              new JsonParser().parse(content).getAsJsonObject().getAsJsonObject("hits");
          if (obj.get("hits") != null) {
            JsonArray json = obj.getAsJsonArray("hits");
            results = Lists.newArrayListWithCapacity(json.size());
            for (int i = 0; i < json.size(); i++) {
              T mapperResult = mapper.apply(json.get(i).getAsJsonObject());
              if (mapperResult != null) {
                results.add(mapperResult);
              }
            }
          }
        } else {
          log.error(statusLine.getReasonPhrase());
        }
        final List<T> r = Collections.unmodifiableList(results);
        return new ResultSet<T>() {
          @Override
          public Iterator<T> iterator() {
            return r.iterator();
          }

          @Override
          public List<T> toList() {
            return r;
          }

          @Override
          public void close() {
            // Do nothing.
          }
        };
      } catch (IOException e) {
        throw new OrmException(e);
      }
    }
  }
}
