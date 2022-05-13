package com.google.gerrit.elasticsearch.init;

import com.google.common.collect.Iterables;
import com.google.gerrit.index.IndexType;
import com.google.gerrit.index.SchemaDefinitions;
import com.google.gerrit.pgm.init.api.ConsoleUI;
import com.google.gerrit.pgm.init.api.InitFlags;
import com.google.gerrit.pgm.init.api.InitStep;
import com.google.gerrit.pgm.init.api.Section;
import com.google.gerrit.server.config.SitePaths;
import com.google.gerrit.server.index.IndexModule;
import com.google.gerrit.server.index.IndexUtils;
import com.google.inject.Inject;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class InitElasticSearchIndex implements InitStep {
  private final ConsoleUI ui;
  private final Section index;
  private final SitePaths site;
  private final InitFlags initFlags;
  private final Section gerrit;
  private final Section.Factory sections;

  @Inject
  InitElasticSearchIndex(
      ConsoleUI ui, Section.Factory sections, SitePaths site, InitFlags initFlags) {
    this.ui = ui;
    this.index = sections.get("index", null);
    this.gerrit = sections.get("gerrit", null);
    this.site = site;
    this.initFlags = initFlags;
    this.sections = sections;
  }

  @Override
  public void run() throws IOException {
    ui.header("Index");
    IndexType type =
        new IndexType(
            index.select("Type", "type", IndexType.getDefault(), IndexType.getKnownTypes()));

    Section elasticsearch = sections.get("elasticsearch", null);
    elasticsearch.string("Index Prefix", "prefix", "gerrit_");
    elasticsearch.string("Server", "server", "http://localhost:9200");
    index.string("Result window size", "maxLimit", "10000");

    if ((site.isNew || isEmptySite()) && type.isLucene()) {
      for (SchemaDefinitions<?> def : IndexModule.ALL_SCHEMA_DEFS) {
        IndexUtils.setReady(site, def.getName(), def.getLatest().getVersion(), true);
      }
    } else {
      String message =
          String.format(
              "\nThe index must be %sbuilt before starting Gerrit:\n"
                  + "  java -jar gerrit.war reindex -d site_path\n",
              site.isNew ? "" : "re");
      ui.message(message);
      initFlags.autoStart = false;
    }
  }

  private boolean isEmptySite() {
    try (DirectoryStream<Path> files =
        Files.newDirectoryStream(site.resolve(gerrit.get("basePath")))) {
      return Iterables.isEmpty(files);
    } catch (IOException e) {
      return true;
    }
  }
}
