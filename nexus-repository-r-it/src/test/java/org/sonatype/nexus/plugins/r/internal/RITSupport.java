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

public class RITSupport
    extends RepositoryITSupport
{
  public static final String R = "r";

  public static final String PATH = "bin/macosx/el-capitan/contrib/3.6";

  public static final String AGRICOLAE = "agricolae";

  public static final String AGRICOLAE_VERSION = "1.3-1";

  public static final String TGZ = ".tgz";

  public static final String GZ = ".gz";

  public static final String AGRICOLAE_FILE_NAME = String.format("%s_%s%s", AGRICOLAE, AGRICOLAE_VERSION, TGZ);

  public static final String AGRICOLAE_FULL = String.format("%s/%s", PATH, AGRICOLAE_FILE_NAME);

  public static final String CONTENT_TYPE_TGZ = "application/x-tgz";

  public static final String CONTENT_TYPE_GZIP = "application/x-gzip";

  public static final String PACKAGES = "PACKAGES";

  public static final String PACKAGES_FILE_NAME = String.format("%s%s", PACKAGES, GZ);

  public static final String PACKAGES_FULL = String.format("%s/%s", PATH, PACKAGES_FILE_NAME);

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
