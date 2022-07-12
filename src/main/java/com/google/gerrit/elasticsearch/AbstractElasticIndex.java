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
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.gson.FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.google.common.flogger.FluentLogger;
import com.google.common.io.BaseEncoding;
import com.google.common.io.CharStreams;
import com.google.gerrit.common.Nullable;
import com.google.gerrit.elasticsearch.ElasticMapping.MappingProperties;
import com.google.gerrit.elasticsearch.builders.QueryBuilder;
import com.google.gerrit.elasticsearch.builders.SearchSourceBuilder;
import com.google.gerrit.elasticsearch.builders.XContentBuilder;
import com.google.gerrit.elasticsearch.bulk.DeleteRequest;
import com.google.gerrit.entities.converter.ProtoConverter;
import com.google.gerrit.exceptions.StorageException;
import com.google.gerrit.index.FieldDef;
import com.google.gerrit.index.FieldType;
import com.google.gerrit.index.Index;
import com.google.gerrit.index.QueryOptions;
import com.google.gerrit.index.Schema;
import com.google.gerrit.index.query.DataSource;
import com.google.gerrit.index.query.FieldBundle;
import com.google.gerrit.index.query.ListResultSet;
import com.google.gerrit.index.query.Predicate;
import com.google.gerrit.index.query.QueryParseException;
import com.google.gerrit.index.query.ResultSet;
import com.google.gerrit.proto.Protos;
import com.google.gerrit.server.config.SitePaths;
import com.google.gerrit.server.index.IndexUtils;
import com.google.gerrit.server.index.options.AutoFlush;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.MessageLite;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;

abstract class AbstractElasticIndex<K, V> implements Index<K, V> {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  protected static final String BULK = "_bulk";
  protected static final String MAPPINGS = "mappings";
  protected static final String ORDER = "order";
  protected static final String DESC_SORT_ORDER = "desc";
  protected static final String ASC_SORT_ORDER = "asc";
  protected static final String UNMAPPED_TYPE = "unmapped_type";
  protected static final String SEARCH = "_search";
  protected static final String SETTINGS = "settings";
  protected static final String HITS = "hits";
  protected static final String SORT = "sort";
  protected static final String ID = "id";
  protected static final String PIT = "_pit";
  protected static final String PIT_ID = "pit_id";
  protected static final String KEEP_ALIVE = "keep_alive";
  protected static final int PAGE_SIZE_MULTIPLIER = 10;

  static byte[] decodeBase64(String base64String) {
    return BaseEncoding.base64().decode(base64String);
  }

  protected static <T> List<T> decodeProtos(
      JsonObject doc, String fieldName, ProtoConverter<?, T> converter) {
    JsonArray field = doc.getAsJsonArray(fieldName);
    if (field == null) {
      return null;
    }
    return Streams.stream(field)
        .map(JsonElement::getAsString)
        .map(AbstractElasticIndex::decodeBase64)
        .map(bytes -> parseProtoFrom(bytes, converter))
        .collect(toImmutableList());
  }

  protected static <P extends MessageLite, T> T parseProtoFrom(
      byte[] bytes, ProtoConverter<P, T> converter) {
    P message = Protos.parseUnchecked(converter.getParser(), bytes);
    return converter.fromProto(message);
  }

  static String getContent(Response response) throws IOException {
    HttpEntity responseEntity = response.getEntity();
    String content = "";
    if (responseEntity != null) {
      InputStream contentStream = responseEntity.getContent();
      try (Reader reader = new InputStreamReader(contentStream, UTF_8)) {
        content = CharStreams.toString(reader);
      }
    }
    return content;
  }

  private final ElasticConfiguration config;
  private final Schema<V> schema;
  private final SitePaths sitePaths;
  private final String indexNameRaw;
  private final Map<String, String> refreshParam;

  protected final ElasticRestClientProvider client;
  protected final String indexName;
  protected final Gson gson;
  protected final ElasticQueryBuilder queryBuilder;

  AbstractElasticIndex(
      ElasticConfiguration config,
      SitePaths sitePaths,
      Schema<V> schema,
      ElasticRestClientProvider client,
      String indexName,
      AutoFlush autoFlush) {
    this.config = config;
    this.sitePaths = sitePaths;
    this.schema = schema;
    this.gson = new GsonBuilder().setFieldNamingPolicy(LOWER_CASE_WITH_UNDERSCORES).create();
    this.queryBuilder = new ElasticQueryBuilder();
    this.indexName = config.getIndexName(indexName, schema.getVersion());
    this.indexNameRaw = indexName;
    this.client = client;
    this.refreshParam =
        Map.of(
            "refresh",
            autoFlush == AutoFlush.ENABLED ? Boolean.TRUE.toString() : Boolean.FALSE.toString());
  }

  @Override
  public void insert(V obj) {
    replace(obj);
  }

  @Override
  public Schema<V> getSchema() {
    return schema;
  }

  @Override
  public void close() {
    // Do nothing. Client is closed by the provider.
  }

  @Override
  public void markReady(boolean ready) {
    IndexUtils.setReady(sitePaths, indexNameRaw, schema.getVersion(), ready);
  }

  @Override
  public void delete(K id) {
    String uri = getURI(BULK);
    Response response = postRequestWithRefreshParam(uri, getDeleteActions(id));
    int statusCode = response.getStatusLine().getStatusCode();
    if (statusCode != HttpStatus.SC_OK) {
      throw new StorageException(
          String.format("Failed to delete %s from index %s: %s", id, indexName, statusCode));
    }
  }

  @Override
  public void deleteAll() {
    // Delete the index, if it exists.
    String endpoint = indexName + client.adapter().indicesExistParams();
    Response response = performRequest("HEAD", endpoint);
    int statusCode = response.getStatusLine().getStatusCode();
    if (statusCode == HttpStatus.SC_OK) {
      response = performRequest("DELETE", indexName);
      statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != HttpStatus.SC_OK) {
        throw new StorageException(
            String.format("Failed to delete index %s: %s", indexName, statusCode));
      }
    }

    // Recreate the index.
    String indexCreationFields = concatJsonString(getSettings(), getMappings());
    response = performRequest("PUT", indexName, indexCreationFields);
    statusCode = response.getStatusLine().getStatusCode();
    if (statusCode != HttpStatus.SC_OK) {
      String error = String.format("Failed to create index %s: %s", indexName, statusCode);
      throw new StorageException(error);
    }
  }

  protected abstract String getDeleteActions(K id);

  protected abstract String getMappings();

  private String getSettings() {
    return gson.toJson(ImmutableMap.of(SETTINGS, ElasticSetting.createSetting(config)));
  }

  protected abstract String getId(V v);

  protected String getMappingsForSingleType(MappingProperties properties) {
    return getMappingsFor(properties);
  }

  protected String getMappingsFor(MappingProperties properties) {
    JsonObject mappings = new JsonObject();

    mappings.add(MAPPINGS, gson.toJsonTree(properties));
    return gson.toJson(mappings);
  }

  protected String getDeleteRequest(K id) {
    return new DeleteRequest(id.toString(), indexName).toString();
  }

  protected abstract V fromDocument(JsonObject doc, Set<String> fields);

  protected FieldBundle toFieldBundle(JsonObject doc) {
    Map<String, FieldDef<V, ?>> allFields = getSchema().getFields();
    ListMultimap<String, Object> rawFields = ArrayListMultimap.create();
    for (Map.Entry<String, JsonElement> element :
        doc.get(client.adapter().rawFieldsKey()).getAsJsonObject().entrySet()) {
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
          rawFields.put(element.getKey(), decodeBase64(inner.getAsString()));
        } else {
          throw FieldType.badFieldType(type);
        }
      }
    }
    return new FieldBundle(rawFields);
  }

  protected boolean hasErrors(Response response) {
    try {
      String contentType = response.getEntity().getContentType().getValue();
      Preconditions.checkState(
          contentType.equals(ContentType.APPLICATION_JSON.toString()),
          String.format("Expected %s, but was: %s", ContentType.APPLICATION_JSON, contentType));
      String responseStr = EntityUtils.toString(response.getEntity());
      JsonObject responseJson = (JsonObject) new JsonParser().parse(responseStr);
      return responseJson.get("errors").getAsBoolean();
    } catch (IOException e) {
      throw new StorageException(e);
    }
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

  protected String getSearch(SearchSourceBuilder searchSource, JsonArray sortArray) {
    JsonObject search = new JsonParser().parse(searchSource.toString()).getAsJsonObject();
    search.add("sort", sortArray);
    return gson.toJson(search);
  }

  protected JsonArray getSortArray(String idFieldName) {
    JsonObject properties = new JsonObject();
    properties.addProperty(ORDER, ASC_SORT_ORDER);

    JsonArray sortArray = new JsonArray();
    addNamedElement(idFieldName, properties, sortArray);
    return sortArray;
  }

  protected String getURI(String request) {
    try {
      return URLEncoder.encode(indexName, UTF_8.toString()) + "/" + request;
    } catch (UnsupportedEncodingException e) {
      throw new StorageException(e);
    }
  }

  protected Response postRequestWithRefreshParam(String uri, Object payload) {
    return performRequest("POST", uri, payload, refreshParam);
  }

  private String concatJsonString(String target, String addition) {
    return target.substring(0, target.length() - 1) + "," + addition.substring(1);
  }

  private Response performRequest(String method, String uri) {
    return performRequest(method, uri, null);
  }

  private Response performRequest(String method, String uri, @Nullable Object payload) {
    return performRequest(method, uri, payload, Collections.emptyMap());
  }

  private Response performRequest(
      String method, String uri, @Nullable Object payload, Map<String, String> params) {
    Request request = createRequest(method, uri, payload, params);
    try {
      // TODO: remove this debug message
      logger.atSevere().log("uri:" + uri + " payload:" + payload + " params:" + params);

      return client.get().performRequest(request);
    } catch (IOException e) {
      throw new StorageException(e);
    }
  }

  private void performRequestAsync(
      String method, String uri, @Nullable Object payload, Map<String, String> params) {
    Request request = createRequest(method, uri, payload, params);

    client.get().performRequestAsync(request, new AsyncResponseListener(request));
  }

  private Request createRequest(
      String method, String uri, @Nullable Object payload, Map<String, String> params) {
    Request request = new Request(method, uri.startsWith("/") ? uri : "/" + uri);
    if (payload != null) {
      String payloadStr = payload instanceof String ? (String) payload : payload.toString();
      request.setEntity(new NStringEntity(payloadStr, ContentType.APPLICATION_JSON));
    }
    for (Map.Entry<String, String> entry : params.entrySet()) {
      request.addParameter(entry.getKey(), entry.getValue());
    }
    return request;
  }

  protected class AsyncResponseListener implements ResponseListener {
    Request request;

    public AsyncResponseListener(Request request) {
      this.request = request;
    }

    @Override
    public void onSuccess(Response response) {}

    @Override
    public void onFailure(Exception exception) {
      logger.atSevere().log(String.format("%s failed!", request), exception);
    }
  }

  public class PointInTimeSearch {
    public String id;
    public TimeValue keepAlive;
    public JsonArray searchAfter;

    public PointInTimeSearch(String id, TimeValue keepAlive, JsonArray searchAfter) {
      this.id = id;
      this.keepAlive = keepAlive;
      this.searchAfter = searchAfter;
    }
  }

  protected class ElasticQuerySource implements DataSource<V> {
    private final QueryOptions opts;
    QueryBuilder qb;
    JsonArray sortArray;
    boolean isPitSupported;

    ElasticQuerySource(Predicate<V> p, QueryOptions opts, JsonArray sortArray)
        throws QueryParseException {
      this.opts = opts;
      this.sortArray = sortArray;
      qb = queryBuilder.toQueryBuilder(p);
      if ((new DefaultArtifactVersion(client.elasticVersion().toString()))
              .compareTo(new DefaultArtifactVersion("7.10"))
          >= 0) {
        isPitSupported = true;
      } else {
        logger.atWarning().log(
            "PIT is enabled in elasticsearch config, but cannot be used as"
                + " elasticsearch version should be 7.10+");
      }
    }

    @Override
    public int getCardinality() {
      return 10;
    }

    @Override
    public ResultSet<V> read() {
      return readImpl(doc -> AbstractElasticIndex.this.fromDocument(doc, opts.fields()));
    }

    @Override
    public ResultSet<FieldBundle> readRaw() {
      return readImpl(AbstractElasticIndex.this::toFieldBundle);
    }

    private <T> ResultSet<T> readImpl(Function<JsonObject, T> mapper) {
      try {
        return buildResults(config.usePit && isPitSupported ? readImplPit() : readImpl(), mapper);
      } catch (IOException e) {
        throw new StorageException(e);
      }
    }

    private JsonArray readImpl() throws IOException {
      JsonObject hitsObj = search(opts.limit(), Optional.empty()).getAsJsonObject(HITS);
      if (hitsObj.get(HITS) != null) {
        return hitsObj.getAsJsonArray(HITS);
      }
      return new JsonArray();
    }

    private JsonArray readImplPit() throws IOException {
      PointInTimeSearch pit = null;
      try {
        JsonObject results;
        JsonObject hitsObj;
        JsonArray jsonResults = new JsonArray();
        TimeValue pitKeepAlive = new TimeValue(config.pitKeepAliveSecs, TimeUnit.SECONDS);
        pit = new PointInTimeSearch(createPointInTime(pitKeepAlive), pitKeepAlive, new JsonArray());
        // PIT searches can be paginated only when 'from' parameter is 0
        List<Integer> pageSizes =
            opts.start() > 0
                ? Arrays.asList(opts.limit())
                : pageSizeGenerator(config.pitPageSize, opts.limit(), PAGE_SIZE_MULTIPLIER);
        for (int size : pageSizes) {
          int searchSize = size + 1;
          results = search(searchSize, Optional.of(pit));
          hitsObj = results.getAsJsonObject(HITS);
          if (hitsObj.get(HITS) != null) {
            JsonArray hits = hitsObj.getAsJsonArray(HITS);
            int hitsSize = hits.size();
            if (hitsSize == 0) {
              break;
            }
            if (hitsSize < searchSize) {
              jsonResults.addAll(hits);
              break;
            }
            hits.remove(hitsSize - 1);
            jsonResults.addAll(hits);
            JsonObject lastHit = hits.get(hits.size() - 1).getAsJsonObject();
            if (lastHit.get(SORT) != null) {
              pit.searchAfter = lastHit.getAsJsonArray(SORT);
            }
            if (results.get(PIT_ID) != null) {
              pit.id = results.getAsJsonPrimitive(PIT_ID).getAsString();
            }
          }
        }
        return jsonResults;
      } finally {
        if (pit != null && pit.id != null) {
          deletePointInTime(pit.id);
        }
      }
    }

    private JsonObject search(int size, Optional<PointInTimeSearch> pitSearch) throws IOException {
      String uri = getURI(SEARCH);
      SearchSourceBuilder searchSource =
          new SearchSourceBuilder(client.adapter())
              .query(qb)
              .from(opts.start())
              .size(size)
              .fields(Lists.newArrayList(opts.fields()));
      if (pitSearch.isPresent()) {
        uri = "/" + SEARCH;
        PointInTimeSearch pit = pitSearch.get();
        searchSource.pointInTime(pit.id, pit.keepAlive).trackTotalHits(false);
        if (pit.searchAfter.size() > 0) {
          searchSource.searchAfter(pit.searchAfter);
        }
      }
      String search = getSearch(searchSource, sortArray);
      Response response = performRequest(HttpGet.METHOD_NAME, uri, search, Collections.emptyMap());
      StatusLine statusLine = response.getStatusLine();
      if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
        return new JsonParser().parse(getContent(response)).getAsJsonObject();
      }
      logger.atSevere().log(statusLine.getReasonPhrase());
      return new JsonObject();
    }

    private <T> ResultSet<T> buildResults(JsonArray jsonResults, Function<JsonObject, T> mapper) {
      ImmutableList.Builder<T> results = ImmutableList.builderWithExpectedSize(jsonResults.size());
      for (int i = 0; i < jsonResults.size(); i++) {
        T mapperResult = mapper.apply(jsonResults.get(i).getAsJsonObject());
        if (mapperResult != null) {
          results.add(mapperResult);
        }
      }
      return new ListResultSet<>(results.build());
    }

    private String createPointInTime(TimeValue keepAlive) {
      try {
        String uri = getURI(PIT);
        Map<String, String> params = new HashMap<>();
        params.put(KEEP_ALIVE, keepAlive.toString());
        Response response = performRequest(HttpPost.METHOD_NAME, uri, null, params);
        StatusLine statusLine = response.getStatusLine();
        if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
          String content = getContent(response);
          JsonObject obj = new JsonParser().parse(content).getAsJsonObject();
          if (obj.get(ID) != null) {
            return obj.get(ID).getAsString();
          }
        } else {
          logger.atSevere().log(statusLine.getReasonPhrase());
        }
      } catch (IOException e) {
        throw new StorageException(e);
      }
      return null;
    }

    private void deletePointInTime(String id) {
      try {
        String uri = "/" + PIT;
        XContentBuilder builder = new XContentBuilder();
        builder.startObject();
        builder.field(ID, id);
        builder.endObject();
        performRequestAsync(HttpDelete.METHOD_NAME, uri, builder.string(), Collections.emptyMap());
      } catch (IOException e) {
        throw new StorageException(e);
      }
    }

    private List<Integer> pageSizeGenerator(int initialSize, int limit, int multiplier) {
      // REST client will throw an IOException if the size of response is more than REST client's
      // default response buffer limit of 100MB. We choose the max size of a request as 30k as the
      // response size is ~80M for 30k change index docs.
      int max_size = 30000;
      if (initialSize >= limit) {
        return Arrays.asList(limit);
      }
      long size = initialSize;
      long totalSize = 0;
      List<Integer> sizes = new ArrayList<>();
      while (size != 0 && totalSize <= limit) {
        sizes.add((int) size);
        totalSize += size;
        size = size * multiplier;
        if (size > max_size) {
          size = max_size;
        }
        if (totalSize + size > limit) {
          size = limit - totalSize;
        }
      }
      return sizes;
    }
  }
}
