Configuration
=============

[[index]]
=== Section index

[[index.maxLimit]]index.maxLimit::
+
Maximum limit to allow for search queries. Requesting results above this
limit will truncate the list (but will still set `_more_changes` on
result lists). Set to 0 for no limit.
This value should not exceed the `index.max_result_window` value configured
on the Elasticsearch server.
If a value is not configured during site initialization, defaults to
10000, which is the default value of `index.max_result_window` in Elasticsearch.

[[elasticsearch]]
=== Section elasticsearch

WARNING: Support for Elasticsearch is still experimental and is not recommended
for production use. For compatibility information, please refer to the
link:https://www.gerritcodereview.com/elasticsearch.html[project homepage,role=external,window=_blank].

Note that when Gerrit is configured to use Elasticsearch, the Elasticsearch
server(s) must be reachable during the site initialization.

[[elasticsearch.prefix]]elasticsearch.prefix::
+
This setting can be used to prefix index names to allow multiple Gerrit
instances in a single Elasticsearch cluster. Prefix `gerrit1_` would result in a
change index named `gerrit1_changes_0001`.
+
Not set by default.

[[elasticsearch.server]]elasticsearch.server::
+
Elasticsearch server URI in the form `http[s]://hostname:port`. The `port` is
optional and defaults to `9200` if not specified.
+
At least one server must be specified. May be specified multiple times to
configure multiple Elasticsearch servers.
+
Note that the site initialization program only allows to configure a single
server. To configure multiple servers the `gerrit.config` file must be edited
manually.

[[elasticsearch.numberOfShards]]elasticsearch.numberOfShards::
+
Sets the number of shards to use per index. Refer to the
link:https://www.elastic.co/guide/en/elasticsearch/reference/current/index-modules.html#_static_index_settings[
Elasticsearch documentation,role=external,window=_blank] for details.
+
Defaults to 1.

[[elasticsearch.numberOfReplicas]]elasticsearch.numberOfReplicas::
+
Sets the number of replicas to use per index. Refer to the
link:https://www.elastic.co/guide/en/elasticsearch/reference/current/index-modules.html#dynamic-index-settings[
Elasticsearch documentation,role=external,window=_blank] for details.
+
Defaults to 1.

[[elasticsearch.maxResultWindow]]elasticsearch.maxResultWindow::
+
Sets the maximum value of `from + size` for searches to use per index. Refer to the
link:https://www.elastic.co/guide/en/elasticsearch/reference/current/index-modules.html#dynamic-index-settings[
Elasticsearch documentation,role=external,window=_blank] for details.
+
Defaults to 10000.

[[elasticsearch.connectTimeout]]elasticsearch.connectTimeout::
+
Sets the timeout for connecting to elasticsearch.
+
Defaults to `1 second`.

[[elasticsearch.socketTimeout]]elasticsearch.socketTimeout::
+
Sets the timeout for the underlying connection. For more information, refer to
link:#httpd.idleTimeout[`httpd.idleTimeout`].
+
Defaults to `30 seconds`.

==== Elasticsearch Security

When security is enabled in Elasticsearch, the username and password must be provided.
Note that the same username and password are used for all servers.

For further information about Elasticsearch security, please refer to
link:https://www.elastic.co/guide/en/elasticsearch/reference/current/security-getting-started.html[the documentation,role=external,window=_blank].
This is the current documentation link. Select another Elasticsearch version
from the dropdown menu available on that page if need be.

[[elasticsearch.username]]elasticsearch.username::
+
Username used to connect to Elasticsearch.
+
If a password is set, defaults to `elastic`, otherwise not set by default.

[[elasticsearch.password]]elasticsearch.password::
+
Password used to connect to Elasticsearch.
+
Not set by default.
