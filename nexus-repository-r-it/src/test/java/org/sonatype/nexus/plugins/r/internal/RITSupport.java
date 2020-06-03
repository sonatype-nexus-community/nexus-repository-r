/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2017-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.plugins.r.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.annotation.Nonnull;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.pax.exam.NexusPaxExamSupport;
import org.sonatype.nexus.plugins.r.internal.fixtures.RepositoryRuleR;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.testsuite.testsupport.RepositoryITSupport;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.tika.io.IOUtils;
import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.Rule;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.apache.commons.compress.compressors.CompressorStreamFactory.GZIP;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Support class for R ITs.
 *
 * @since 1.1.0
 */
public class RITSupport
    extends RepositoryITSupport
{

  // Interval to wait for metadata is processed
  public static final long METADATA_PROCESSING_WAIT_INTERVAL_MILLIS = 1000L;

  public static final String R_FORMAT_NAME = "r";

  public static final String PKG_BIN_PATH = "bin/macosx/el-capitan/contrib/3.6";

  public static final String PKG_SRC_PATH = "src/contrib";

  public static final String PKG_SRC_META_PATH = "src/contrib/Meta";

  public static final String PACKAGES_AGRICOLAE_121_FILENAME = "PACKAGES_agricolae_121";

  public static final String PACKAGES_AGRICOLAE_131_FILENAME = "PACKAGES_agricolae_131";

  public static final String CONTENT_TYPE_X_TGZ = "application/x-tgz";

  public static final String CONTENT_TYPE_X_GZIP = "application/x-gzip";

  public static final String CONTENT_TYPE_GZIP = "application/gzip";

  public static final String CONTENT_TYPE_X_XZ = "application/x-xz";

  public static final String TARGZ_EXT = ".tar.gz";

  public static final String TGZ_EXT = ".tgz";

  public static final String GZ_EXT = ".gz";

  public static final String RDS_EXT = ".rds";

  public static final String RDS_METADATA_KIND = "RDS_METADATA";

  public static final String PACKAGES_KIND = "PACKAGES";

  public static final TestPackage AGRICOLAE_131_TGZ = new TestPackage(
      "agricolae_1.3-1.tgz",
      PKG_BIN_PATH,
      "agricolae",
      "1.3-1",
      TGZ_EXT,
      CONTENT_TYPE_X_TGZ
  );

  public static final TestPackage AGRICOLAE_131_TARGZ = new TestPackage(
      "agricolae_1.3-1.tar.gz",
      PKG_SRC_PATH,
      "agricolae",
      "1.3-1",
      TARGZ_EXT,
      CONTENT_TYPE_X_GZIP
  );

  public static final TestPackage AGRICOLAE_121_TARGZ = new TestPackage(
      "agricolae_1.2-1.tar.gz",
      PKG_SRC_PATH,
      "agricolae",
      "1.2-1",
      TARGZ_EXT,
      CONTENT_TYPE_X_GZIP
  );

  public static final TestPackage AGRICOLAE_101_TARGZ = new TestPackage(
      "agricolae_1.0-1.tar.gz",
      PKG_SRC_PATH,
      "agricolae",
      "1.0-1",
      TARGZ_EXT,
      CONTENT_TYPE_X_GZIP
  );

  public static final TestPackage AGRICOLAE_131_XXX = new TestPackage(
      "agricolae_1.3-1.xxx",
      PKG_SRC_PATH,
      "agricolae",
      "1.3-1",
      ".xxx", // Wrong extension for R package
      CONTENT_TYPE_X_GZIP
  );

  public static final TestPackage AGRICOLAE_131_TARGZ_WRONG_PATH = new TestPackage(
      "agricolae_1.3-1.tar.gz",
      "wrongpath",
      "agricolae",
      "1.3-1",
      TARGZ_EXT,
      CONTENT_TYPE_X_GZIP
  );

  public static final TestPackage PACKAGES_SRC_GZ = new TestPackage(
      "PACKAGES.gz",
      PKG_SRC_PATH,
      null,
      null,
      GZ_EXT,
      CONTENT_TYPE_X_GZIP
  );

  public static final TestPackage PACKAGES_BIN_GZ = new TestPackage(
      "PACKAGES.gz",
      PKG_BIN_PATH,
      null,
      null,
      GZ_EXT,
      CONTENT_TYPE_X_GZIP
  );

  public static final TestPackage PACKAGES_RDS = new TestPackage(
      "PACKAGES.rds",
      PKG_SRC_PATH,
      null,
      null,
      RDS_EXT,
      CONTENT_TYPE_X_XZ // PACKAGE.rds is compressed in xz
  );

  public static final TestPackage ARCHIVE_RDS = new TestPackage(
      "archive.rds",
      PKG_SRC_META_PATH,
      null,
      null,
      RDS_EXT,
      CONTENT_TYPE_GZIP // archive.rds is compressed in gzip
  );

  public static final TestPackage DOES_NOT_EXIST_TGZ = new TestPackage(
      "doesnotexist.tgz",
      PKG_BIN_PATH,
      null,
      null,
      TGZ_EXT,
      CONTENT_TYPE_X_TGZ
  );

  @Rule
  public RepositoryRuleR repos = new RepositoryRuleR(() -> repositoryManager);

  public RITSupport() {
    testData.addDirectory(NexusPaxExamSupport.resolveBaseFile("target/test-classes/r"));
  }

  @Nonnull
  protected RClient rClient(final Repository repository) throws Exception {
    checkNotNull(repository);
    final URL repositoryUrl = repositoryBaseUrl(repository);
    return rClient(repositoryUrl);
  }

  @Nonnull
  protected RClient createRClient(final Repository repository) throws Exception {
    return new RClient(
        clientBuilder().build(),
        clientContext(),
        resolveUrl(nexusUrl, format("/repository/%s/", repository.getName())).toURI()
    );
  }

  protected RClient rClient(final URL repositoryUrl) throws Exception {
    return new RClient(
        clientBuilder(repositoryUrl).build(),
        clientContext(),
        repositoryUrl.toURI()
    );
  }

  protected HttpEntity fileToHttpEntity(String name) throws IOException {
    return new ByteArrayEntity(Files.readAllBytes(getFilePathByName(name)));
  }

  protected void verifyTextGzipContent(Matcher<String> expectedContent, InputStream is) throws Exception {
    try (InputStream cin = new CompressorStreamFactory().createCompressorInputStream(GZIP, is)) {
      final String downloadedPackageData = IOUtils.toString(cin);
      assertThat(downloadedPackageData, expectedContent);
    }
  }

  private Path getFilePathByName(String fileName) {
    return Paths.get(testData.resolveFile(fileName).getAbsolutePath());
  }

  protected Component findComponentById(final Repository repository, final EntityId componentId) {
    try (StorageTx tx = getStorageTx(repository)) {
      tx.begin();
      return tx.findComponent(componentId);
    }
  }

  protected List<Asset> findAssetsByComponent(final Repository repository, final Component component) {
    try (StorageTx tx = getStorageTx(repository)) {
      tx.begin();
      return IteratorUtils.toList(tx.browseAssets(component).iterator());
    }
  }

  protected void assertGetResponseStatus(
      final RClient client,
      final Repository repository,
      final String path,
      final int responseCode) throws IOException
  {
    try (CloseableHttpResponse response = client.get(path)) {
      StatusLine statusLine = response.getStatusLine();
      Assert.assertThat("Repository:" + repository.getName() + " Path:" + path,
          statusLine.getStatusCode(),
          is(responseCode));
    }
  }

  public static class TestPackage
  {
    public final String filename;

    public final String basePath;

    public final String fullPath;

    public final String packageName;

    public final String packageVersion;

    public final String extension;

    public final String contentType;

    public TestPackage(
        final String filename,
        final String basePath,
        final String packageName,
        final String packageVersion,
        final String extension,
        final String contentType)
    {
      this.filename = filename;
      this.basePath = basePath;
      this.fullPath = basePath + "/" + filename;
      this.packageName = packageName;
      this.packageVersion = packageVersion;
      this.extension = extension;
      this.contentType = contentType;
    }
  }
}
