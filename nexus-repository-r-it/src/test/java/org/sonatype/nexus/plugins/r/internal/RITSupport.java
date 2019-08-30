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

import java.net.URL;

import javax.annotation.Nonnull;

import org.sonatype.nexus.pax.exam.NexusPaxExamSupport;
import org.sonatype.nexus.plugins.r.internal.fixtures.RepositoryRuleR;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.testsuite.testsupport.RepositoryITSupport;

import org.junit.Rule;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * Support class for R ITs.
 *
 * @since 1.1.0
 */
public class RITSupport
    extends RepositoryITSupport
{
  public static final String R_FORMAT_NAME = "r";

  public static final String PKG_PATH = "bin/macosx/el-capitan/contrib/3.6";

  public static final String AGRICOLAE_PKG_NAME = "agricolae";

  public static final String PACKAGES_NAME = "PACKAGES";

  public static final String AGRICOLAE_PKG_VERSION_101 = "1.0-1";

  public static final String AGRICOLAE_PKG_VERSION_121 = "1.2-1";

  public static final String AGRICOLAE_PKG_VERSION_131 = "1.3-1";

  public static final String TARGZ_EXT = ".tar.gz";

  public static final String TGZ_EXT = ".tgz";

  public static final String GZ_EXT = ".gz";

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

  public static final String PACKAGES_FILE_NAME = String.format("%s%s", PACKAGES_NAME, GZ_EXT);

  public static final String AGRICOLAE_PATH_FULL = String.format("%s/%s", PKG_PATH, AGRICOLAE_PKG_FILE_NAME_131_TGZ);

  public static final String PACKAGES_PATH_FULL = String.format("%s/%s", PKG_PATH, PACKAGES_FILE_NAME);

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

  protected RClient rClient(final URL repositoryUrl) throws Exception {
    return new RClient(
        clientBuilder(repositoryUrl).build(),
        clientContext(),
        repositoryUrl.toURI()
    );
  }
}
