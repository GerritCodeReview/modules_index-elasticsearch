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

import com.google.common.collect.ImmutableSet;
import com.google.gerrit.elasticsearch.ElasticMapping.Mapping;
import com.google.gerrit.elasticsearch.bulk.BulkRequest;
import com.google.gerrit.elasticsearch.bulk.IndexRequest;
import com.google.gerrit.elasticsearch.bulk.UpdateRequest;
import com.google.gerrit.entities.Account;
import com.google.gerrit.exceptions.StorageException;
import com.google.gerrit.index.QueryOptions;
import com.google.gerrit.index.Schema;
import com.google.gerrit.index.query.DataSource;
import com.google.gerrit.index.query.Predicate;
import com.google.gerrit.index.query.QueryParseException;
import com.google.gerrit.server.account.AccountCache;
import com.google.gerrit.server.account.AccountState;
import com.google.gerrit.server.config.SitePaths;
import com.google.gerrit.server.index.IndexUtils;
import com.google.gerrit.server.index.account.AccountField;
import com.google.gerrit.server.index.account.AccountIndex;
import com.google.gerrit.server.index.options.AutoFlush;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import java.util.Set;
import org.apache.http.HttpStatus;
import org.elasticsearch.client.Response;

public class ElasticAccountIndex extends AbstractElasticIndex<Account.Id, AccountState>
    implements AccountIndex {
  static class AccountMapping {
    final Mapping accounts;

    AccountMapping(Schema<AccountState> schema, ElasticQueryAdapter adapter) {
      this.accounts = ElasticMapping.createMapping(schema, adapter);
    }
  }

  private static final String ACCOUNTS = "accounts";

  private final AccountMapping mapping;
  private final Provider<AccountCache> accountCache;
  private final Schema<AccountState> schema;

  @Inject
  ElasticAccountIndex(
      ElasticConfiguration cfg,
      SitePaths sitePaths,
      Provider<AccountCache> accountCache,
      ElasticRestClientProvider client,
      AutoFlush autoFlush,
      @Assisted Schema<AccountState> schema) {
    super(cfg, sitePaths, schema, client, ACCOUNTS, autoFlush, AccountIndex.ENTITY_TO_KEY);
    this.accountCache = accountCache;
    this.mapping = new AccountMapping(schema, client.adapter());
    this.schema = schema;
  }

  @Override
  public void replace(AccountState as) {
    BulkRequest bulk =
        new IndexRequest(getId(as), indexName)
            .add(new UpdateRequest<>(schema, as, ImmutableSet.of()));

    String uri = getURI(BULK);
    Response response = postRequestWithRefreshParam(uri, bulk);
    int statusCode = response.getStatusLine().getStatusCode();
    if (hasErrors(response) || statusCode != HttpStatus.SC_OK) {
      throw new StorageException(
          String.format(
              "Failed to replace account %s in index %s: %s",
              as.account().id(), indexName, statusCode));
    }
  }

  @Override
  public DataSource<AccountState> getSource(Predicate<AccountState> p, QueryOptions opts)
      throws QueryParseException {
    boolean useLegacyNumericFields = schema.hasField(AccountField.ID_FIELD_SPEC);
    JsonArray sortArray =
        getSortArray(
            useLegacyNumericFields
                ? AccountField.ID_FIELD_SPEC.getName()
                : AccountField.ID_STR_FIELD_SPEC.getName());
    return new ElasticQuerySource(
        p, opts.filterFields(o -> IndexUtils.accountFields(o, useLegacyNumericFields)), sortArray);
  }

  @Override
  protected String getDeleteActions(Account.Id a) {
    return getDeleteRequest(a);
  }

  @Override
  protected String getMappings() {
    return getMappingsForSingleType(mapping.accounts);
  }

  @Override
  protected String getId(AccountState as) {
    return as.account().id().toString();
  }

  @Override
  protected AccountState fromDocument(JsonObject json, Set<String> fields) {
    JsonElement source = json.get("_source");
    if (source == null) {
      source = json.getAsJsonObject().get("fields");
    }

    Account.Id id =
        Account.id(
            source
                .getAsJsonObject()
                .get(
                    schema.hasField(AccountField.ID_FIELD_SPEC)
                        ? AccountField.ID_FIELD_SPEC.getName()
                        : AccountField.ID_STR_FIELD_SPEC.getName())
                .getAsInt());
    // Use the AccountCache rather than depending on any stored fields in the document (of which
    // there shouldn't be any). The most expensive part to compute anyway is the effective group
    // IDs, and we don't have a good way to reindex when those change.
    // If the account doesn't exist return an empty AccountState to represent the missing account
    // to account the fact that the account exists in the index.
    return accountCache.get().getEvenIfMissing(id);
  }
}
