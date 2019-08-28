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
import static org.sonatype.nexus.repository.http.HttpStatus.OK;
import static org.sonatype.nexus.testsuite.testsupport.FormatClientSupport.status;

public class CleanupTaskRIT
    extends CleanupITSupport
{
  public static final String PATH = "bin/macosx/el-capitan/contrib/3.6";

  public static final String AGRICOLAE = "agricolae";

  public static final String AGRICOLAE_VERSION_101 = "1.0-1";

  public static final String AGRICOLAE_VERSION_121 = "1.2-1";

  public static final String AGRICOLAE_VERSION_131 = "1.3-1";

  public static final String TARGZ = ".tar.gz";

  public static final String AGRICOLAE_FILE_NAME_101_TARGZ = format("%s_%s%s", AGRICOLAE, AGRICOLAE_VERSION_101, TARGZ);

  public static final String AGRICOLAE_FILE_NAME_121_TARGZ = format("%s_%s%s", AGRICOLAE, AGRICOLAE_VERSION_121, TARGZ);

  public static final String AGRICOLAE_FILE_NAME_131_TARGZ = format("%s_%s%s", AGRICOLAE, AGRICOLAE_VERSION_131, TARGZ);

  public static final String[] VERSIONS = {AGRICOLAE_FILE_NAME_101_TARGZ};

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
    deployArtifacts(VERSIONS);
  }

  @Test
  public void cleanupByLastBlobUpdated() throws Exception {
    assertLastBlobUpdatedComponentsCleanedUp(repository, (long) VERSIONS.length,
        () -> deployArtifacts(AGRICOLAE_FILE_NAME_131_TARGZ), 1L);
  }

  @Test
  public void cleanupByLastDownloaded() throws Exception {
    assertLastDownloadedComponentsCleanedUp(repository, (long) VERSIONS.length,
        () -> deployArtifacts(AGRICOLAE_FILE_NAME_131_TARGZ), 1L);
  }

  @Test
  public void cleanupByRegex() throws Exception {
    assertCleanupByRegex(repository, VERSIONS.length, "bin.*1.[0,3]-1.tar.gz",
        () -> deployArtifacts(AGRICOLAE_FILE_NAME_121_TARGZ, AGRICOLAE_FILE_NAME_131_TARGZ), 1L);
  }

  private int deployArtifacts(final String... pathToFile) {
    try {
      RClient client = new RClient(clientBuilder().build(),
          clientContext(),
          resolveUrl(nexusUrl, format("/repository/%s/", repository.getName())).toURI()
      );

      for (String name : pathToFile) {
        assertThat(status(client.put(format("%s/%s", PATH, name), new ByteArrayEntity(getBytesFromTestData(name)))),
            is(OK));
      }

      return pathToFile.length;
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
