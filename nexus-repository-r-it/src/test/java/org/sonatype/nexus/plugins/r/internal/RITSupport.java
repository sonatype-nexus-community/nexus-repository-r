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
import org.apache.http.entity.ByteArrayEntity;
import org.apache.tika.io.IOUtils;
import org.hamcrest.Matcher;
import org.junit.Rule;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.apache.commons.compress.compressors.CompressorStreamFactory.GZIP;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Support class for R ITs.
 *
 * @since 1.1.0
 */
public class RITSupport
    extends RepositoryITSupport
{
  public static final String R_FORMAT_NAME = "r";

  public static final String PKG_GZ_PATH = "bin/macosx/el-capitan/contrib/3.6";

  public static final String PKG_RDS_PATH = "src/contrib";

  public static final String AGRICOLAE_PKG_NAME = "agricolae";

  public static final String PACKAGES_NAME = "PACKAGES";

  public static final String PACKAGES_GZ_KIND = "PACKAGES_GZ";

  public static final String PACKAGES_RDS_KIND = "PACKAGES_RDS";

  public static final String PACKAGES_AGRICOLAE_121_NAME = "PACKAGES_agricolae_121";

  public static final String PACKAGES_AGRICOLAE_131_NAME = "PACKAGES_agricolae_131";

  public static final String AGRICOLAE_PKG_VERSION_101 = "1.0-1";

  public static final String AGRICOLAE_PKG_VERSION_121 = "1.2-1";

  public static final String AGRICOLAE_PKG_VERSION_131 = "1.3-1";

  public static final String TARGZ_EXT = ".tar.gz";

  public static final String TGZ_EXT = ".tgz";

  public static final String GZ_EXT = ".gz";

  public static final String RDS_EXT = ".rds";

  public static final String DOES_NOT_EXIST_PKG_TGZ = format("%s/%s%s", PKG_GZ_PATH, "doesnt_exist", TGZ_EXT);

  public static final String AGRICOLAE_PKG_FILE_NAME_131_TGZ = format("%s_%s%s",
      AGRICOLAE_PKG_NAME, AGRICOLAE_PKG_VERSION_131, TGZ_EXT);

  public static final String AGRICOLAE_PKG_FILE_NAME_101_TARGZ = format("%s_%s%s",
      AGRICOLAE_PKG_NAME, AGRICOLAE_PKG_VERSION_101, TARGZ_EXT);

  public static final String AGRICOLAE_PKG_FILE_NAME_121_TARGZ = format("%s_%s%s",
      AGRICOLAE_PKG_NAME, AGRICOLAE_PKG_VERSION_121, TARGZ_EXT);

  public static final String AGRICOLAE_PKG_FILE_NAME_131_TARGZ = format("%s_%s%s",
      AGRICOLAE_PKG_NAME, AGRICOLAE_PKG_VERSION_131, TARGZ_EXT);

  public static final String CONTENT_TYPE_TGZ = "application/x-tgz";

  public static final String CONTENT_TYPE_GZIP = "application/x-gzip";

  public static final String CONTENT_TYPE_RDS = "application/x-xz";

  public static final String PACKAGES_GZ_FILE_NAME = format("%s%s", PACKAGES_NAME, GZ_EXT);

  public static final String PACKAGES_RDS_FILE_NAME = format("%s%s", PACKAGES_NAME, RDS_EXT);

  public static final String AGRICOLAE_PATH_FULL_131_TGZ =
      String.format("%s/%s", PKG_GZ_PATH, AGRICOLAE_PKG_FILE_NAME_131_TGZ);

  public static final String AGRICOLAE_PATH_FULL_131_TARGZ =
      String.format("%s/%s", PKG_GZ_PATH, AGRICOLAE_PKG_FILE_NAME_131_TARGZ);

  public static final String AGRICOLAE_PATH_FULL_121_TARGZ =
      String.format("%s/%s", PKG_GZ_PATH, AGRICOLAE_PKG_FILE_NAME_121_TARGZ);

  public static final String PACKAGES_GZ_PATH_FULL = format("%s/%s", PKG_GZ_PATH, PACKAGES_GZ_FILE_NAME);

  public static final String PACKAGES_RDS_PATH_FULL = format("%s/%s", PKG_RDS_PATH, PACKAGES_RDS_FILE_NAME);

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

  private Path getFilePathByName(String fileName){
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
}
