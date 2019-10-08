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
package org.sonatype.nexus.plugins.r.internal.cleanup;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.sonatype.nexus.pax.exam.NexusPaxExamSupport;
import org.sonatype.nexus.plugins.r.internal.RClient;
import org.sonatype.nexus.plugins.r.internal.fixtures.RepositoryRuleR;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.testsuite.testsupport.NexusITSupport;
import org.sonatype.nexus.testsuite.testsupport.cleanup.CleanupITSupport;

import org.apache.http.entity.ByteArrayEntity;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.sonatype.nexus.plugins.r.internal.RITSupport.AGRICOLAE_PKG_FILE_NAME_101_TARGZ;
import static org.sonatype.nexus.plugins.r.internal.RITSupport.AGRICOLAE_PKG_FILE_NAME_121_TARGZ;
import static org.sonatype.nexus.plugins.r.internal.RITSupport.AGRICOLAE_PKG_FILE_NAME_131_TARGZ;
import static org.sonatype.nexus.plugins.r.internal.RITSupport.PKG_GZ_PATH;
import static org.sonatype.nexus.plugins.r.internal.RITSupport.TARGZ_EXT;
import static org.sonatype.nexus.repository.http.HttpStatus.OK;
import static org.sonatype.nexus.testsuite.testsupport.FormatClientSupport.status;

public class CleanupTaskRIT
    extends CleanupITSupport
{
  public static final String[] NAMES = {AGRICOLAE_PKG_FILE_NAME_101_TARGZ};

  public Repository repository;

  @Rule
  public RepositoryRuleR repos = new RepositoryRuleR(() -> repositoryManager);

  @Configuration
  public static Option[] configureNexus() {
    return NexusPaxExamSupport.options(
        NexusITSupport.configureNexusBase(),
        nexusFeature("org.sonatype.nexus.plugins", "nexus-repository-r")
    );
  }

  @Before
  public void setup() {
    testData.addDirectory(NexusPaxExamSupport.resolveBaseFile("target/test-classes/r"));
    repository = repos.createRHosted(testName.getMethodName());
    deployArtifacts(NAMES);
  }

  @Test
  public void cleanupByLastBlobUpdated() throws Exception {
    assertLastBlobUpdatedComponentsCleanedUp(repository, (long) NAMES.length,
        () -> deployArtifacts(AGRICOLAE_PKG_FILE_NAME_131_TARGZ), 1L);
  }

  @Test
  public void cleanupByLastDownloaded() throws Exception {
    assertLastDownloadedComponentsCleanedUp(repository, (long) NAMES.length,
        () -> deployArtifacts(AGRICOLAE_PKG_FILE_NAME_131_TARGZ), 1L);
  }

  @Test
  public void cleanupByRegex() throws Exception {
    assertCleanupByRegex(repository, NAMES.length, "bin.*1.[0,3]-1.tar.gz",
        () -> deployArtifacts(AGRICOLAE_PKG_FILE_NAME_121_TARGZ, AGRICOLAE_PKG_FILE_NAME_131_TARGZ), 1L);
  }

  @Test
  public void cleanupByLastBlobUpdatedAndLastDownloadedPolicies() throws Exception {
    String[] versionsOfComponentsToKeep = {AGRICOLAE_PKG_FILE_NAME_121_TARGZ};

    assertLastBlobUpdatedAndLastDownloadedComponentsCleanUp(
        repository,
        (long) NAMES.length,
        () -> deployArtifacts(AGRICOLAE_PKG_FILE_NAME_131_TARGZ),
        () -> deployArtifacts(versionsOfComponentsToKeep),
        versionsOfComponentsToKeep);
  }

  @Override
  protected boolean componentMatchesByVersion(final Component component, final String version) {
    return version
        .equals(format("%s_%s%s", component.name().toLowerCase(), component.version().toLowerCase(), TARGZ_EXT));
  }

  private int deployArtifacts(final String... names) {
    try {
      RClient client = new RClient(clientBuilder().build(),
          clientContext(),
          resolveUrl(nexusUrl, format("/repository/%s/", repository.getName())).toURI()
      );

      for (String name : names) {
        assertThat(
            status(client
                .putAndClose(format("%s/%s", PKG_GZ_PATH, name), new ByteArrayEntity(getBytesFromTestData(name)))),
            is(OK));
      }

      return names.length;
    }
    catch (Exception e) {
      log.error("", e);
    }
    return 0;
  }

  private byte[] getBytesFromTestData(String path) throws IOException {
    final File file = testData.resolveFile(path);
    return Files.readAllBytes(Paths.get(file.getAbsolutePath()));
  }
}
