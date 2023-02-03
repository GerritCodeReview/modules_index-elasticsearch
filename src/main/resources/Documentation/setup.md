# Setup

* Install index-elasticsearch module

Install the index-elasticsearch.jar into the `$GERRIT_SITE/lib` directory.

Add the index-elasticsearch module to `$GERRIT_SITE/etc/gerrit.config` as follows:

```ini
[gerrit]
  installIndexModule = com.google.gerrit.elasticsearch.ElasticIndexModule
```

When installing the module on Gerrit replicas, use following example:

```ini
[gerrit]
  installIndexModule = com.google.gerrit.elasticsearch.ReplicaElasticIndexModule
```

For further information and supported options, refer to [config](config.html)
documentation.

[Back to @PLUGIN@ documentation index][index]

[index]: index.html
