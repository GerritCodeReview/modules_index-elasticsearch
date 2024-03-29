# Configuration

## Section index

### index.maxLimit

Maximum limit to allow for search queries. Requesting results above this limit will truncate the
list (but will still set `_more_changes` on result lists). Set to 0 for no limit. This value
should not exceed the `index.max_result_window` value configured on the Elasticsearch server. If a
value is not configured during site initialization, defaults to 10000, which is the default value
of `index.max_result_window` in Elasticsearch.

### index.paginationType

The pagination type to use when index queries are repeated to obtain the next set of results.
Supported values are: `OFFSET` and `SEARCH_AFTER`. For more information, refer to
[`index.paginationType`](https://gerrit-review.googlesource.com/Documentation/config-gerrit.html#index.paginationType).

Defaults to `OFFSET`.
Note: paginationType `NONE` is not supported and Gerrit will not start if it is configured (results
in `ProvisionException`).

## Section elasticsearch

For compatibility information, please refer to the [project homepage](https://www.gerritcodereview.com/elasticsearch.html).

Note that when Gerrit is configured to use Elasticsearch, the Elasticsearch
server(s) must be reachable during the site initialization.

### elasticsearch.prefix

This setting can be used to prefix index names to allow multiple Gerrit instances in a single
Elasticsearch cluster. Prefix `gerrit1_` would result in a change index named
`gerrit1_changes_0001`.

Not set by default.

### elasticsearch.server

Elasticsearch server URI in the form `http[s]://hostname:port`. The `port` is optional and defaults
to `9200` if not specified.

At least one server must be specified. May be specified multiple times to configure multiple
Elasticsearch servers.

Note that the site initialization program only allows to configure a single
server. To configure multiple servers the `gerrit.config` file must be edited
manually.

### elasticsearch.numberOfShards

Sets the number of shards to use per index. Refer to the
[Elasticsearch documentation](https://www.elastic.co/guide/en/elasticsearch/reference/current/index-modules.html#_static_index_settings) for details.

Defaults to 1.

### elasticsearch.numberOfReplicas

Sets the number of replicas to use per index. Refer to the
[Elasticsearch documentation](https://www.elastic.co/guide/en/elasticsearch/reference/current/index-modules.html#dynamic-index-settings) for details.

Defaults to 1.

### elasticsearch.maxResultWindow

Sets the maximum value of `from + size` for searches to use per index. Refer to the
[Elasticsearch documentation](https://www.elastic.co/guide/en/elasticsearch/reference/current/index-modules.html#dynamic-index-settings) for details.

Defaults to 10000.

### elasticsearch.connectTimeout

Sets the timeout for connecting to elasticsearch.

Defaults to `1 second`.

### elasticsearch.socketTimeout

Sets the timeout for the underlying connection. For more information, refer to
[`httpd.idleTimeout`](https://gerrit-documentation.storage.googleapis.com/Documentation/3.5.2/config-gerrit.html#httpd.idleTimeout).

Defaults to `30 seconds`.

## Elasticsearch Security

When security is enabled in Elasticsearch, the username and password must be provided. Note that
the same username and password are used for all servers.

For further information about Elasticsearch security, please refer to
[the documentation](https://www.elastic.co/guide/en/elasticsearch/reference/current/security-getting-started.html). This is the current documentation link. Select another Elasticsearch version from the dropdown menu available on that page if need be.

### elasticsearch.username

Username used to connect to Elasticsearch.

If a password is set, defaults to `elastic`, otherwise not set by default.

### elasticsearch.password

Password used to connect to Elasticsearch.

Not set by default.

### elasticsearch.codec

Sets the codec to be used for the index data. For further information about supported codecs, please refer to the static index setting
[index.codec](https://www.elastic.co/guide/en/elasticsearch/reference/current/index-modules.html#index-codec).

Defaults to `default`.

[Back to @PLUGIN@ documentation index][index]

[index]: index.html
