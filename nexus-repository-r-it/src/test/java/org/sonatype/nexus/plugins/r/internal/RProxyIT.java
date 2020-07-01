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

import org.sonatype.goodies.httpfixture.server.fluent.Server;
import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.pax.exam.NexusPaxExamSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.testsuite.testsupport.NexusITSupport;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.sonatype.goodies.httpfixture.server.fluent.Behaviours.error;
import static org.sonatype.goodies.httpfixture.server.fluent.Behaviours.file;
import static org.sonatype.nexus.repository.http.HttpStatus.NOT_FOUND;

public class RProxyIT
    extends RITSupport
{
  private RClient client;

  private Repository repository;

  private Server server;

  @Configuration
  public static Option[] configureNexus() {
    return NexusPaxExamSupport.options(
        NexusITSupport.configureNexusBase(),
        nexusFeature("org.sonatype.nexus.plugins", "nexus-repository-r")
    );
  }

  @Before
  public void setup() throws Exception {
    BaseUrlHolder.set(this.nexusUrl.toString());
    server = Server.withPort(0)
        .serve("/*")
        .withBehaviours(error(NOT_FOUND))
        .serve("/" + AGRICOLAE_131_TGZ.fullPath)
        .withBehaviours(file(testData.resolveFile(AGRICOLAE_131_TGZ.filename)))
        .serve("/" + AGRICOLAE_131_TARGZ.fullPath)
        .withBehaviours(file(testData.resolveFile(AGRICOLAE_131_TARGZ.filename)))
        .serve("/" + AGRICOLAE_131_XXX.fullPath)
        .withBehaviours(file(testData.resolveFile(AGRICOLAE_131_XXX.filename)))
        .serve("/" + PACKAGES_SRC_GZ.fullPath)
        .withBehaviours(file(testData.resolveFile(PACKAGES_SRC_GZ.filename)))
        .serve("/" + PACKAGES_RDS.fullPath)
        .withBehaviours(file(testData.resolveFile(PACKAGES_RDS.filename)))
        .serve("/" + ARCHIVE_RDS.fullPath)
        .withBehaviours(file(testData.resolveFile(ARCHIVE_RDS.filename)))
        .start();
    repository = repos.createRProxy("r-proxy-test", server.getUrl().toExternalForm());
    client = rClient(repository);
  }

  @After
  public void tearDown() throws Exception {
    server.stop();
  }

  @Test
  public void fetchTgzPackageFile() throws Exception {
    assertSuccessResponseMatches(client.fetch(AGRICOLAE_131_TGZ.fullPath), AGRICOLAE_131_TGZ.filename);
    componentAssetTestHelper.assetExists(repository,AGRICOLAE_131_TGZ.fullPath);
  }

  @Test
  public void fetchMetaData() throws Exception {
    assertSuccessResponseMatches(client.fetch(PACKAGES_SRC_GZ.fullPath), PACKAGES_SRC_GZ.filename);
    componentAssetTestHelper.assetExists(repository,PACKAGES_SRC_GZ.fullPath);

    assertSuccessResponseMatches(client.fetch(PACKAGES_RDS.fullPath), PACKAGES_RDS.filename);
    componentAssetTestHelper.assetExists(repository,PACKAGES_RDS.fullPath);

    assertSuccessResponseMatches(client.fetch(ARCHIVE_RDS.fullPath), ARCHIVE_RDS.filename);
    componentAssetTestHelper.assetExists(repository,ARCHIVE_RDS.fullPath);
  }

  @Test
  public void checkComponentCreated() throws Exception {
    assertThat(componentAssetTestHelper
        .componentExists(repository, AGRICOLAE_131_TGZ.packageName, AGRICOLAE_131_TGZ.packageVersion), is(false));

    client.fetch(AGRICOLAE_131_TGZ.fullPath);

    assertThat(componentAssetTestHelper
        .componentExists(repository, AGRICOLAE_131_TGZ.packageName, AGRICOLAE_131_TGZ.packageVersion), is(true));

  }

  @Test
  public void shouldCachePackageGz() throws Exception {
    client.fetch(PACKAGES_SRC_GZ.fullPath);
    server.stop();
    assertSuccessResponseMatches(client.fetch(PACKAGES_SRC_GZ.fullPath), PACKAGES_SRC_GZ.filename);
  }

  @Test
  public void shouldCachePackageRds() throws Exception {
    client.fetch(PACKAGES_RDS.fullPath);
    server.stop();
    assertSuccessResponseMatches(client.fetch(PACKAGES_RDS.fullPath), PACKAGES_RDS.filename);
  }

  @Test
  public void shouldCacheTgzPackageFile() throws Exception {
    client.fetch(AGRICOLAE_131_TGZ.fullPath);
    server.stop();
    assertSuccessResponseMatches(client.fetch(AGRICOLAE_131_TGZ.fullPath), AGRICOLAE_131_TGZ.filename);
  }

  @Test
  public void testDeletingRemainingAssetAlsoDeletesComponent() throws IOException {
    client.fetch(AGRICOLAE_131_TGZ.fullPath);

    assertThat(componentAssetTestHelper
        .assetExists(repository, AGRICOLAE_131_TGZ.fullPath), is(true));
    assertThat(componentAssetTestHelper
        .componentExists(repository, AGRICOLAE_131_TGZ.packageName, AGRICOLAE_131_TGZ.packageVersion), is(true));


    componentAssetTestHelper.removeAsset(repository, AGRICOLAE_131_TGZ.fullPath);

    assertThat(componentAssetTestHelper
        .assetExists(repository, AGRICOLAE_131_TGZ.fullPath), is(false));
    assertThat(componentAssetTestHelper
        .componentExists(repository, AGRICOLAE_131_TGZ.packageName, AGRICOLAE_131_TGZ.packageVersion), is(false));
  }

  //@Test
  //public void testDeletingComponentDeletesAllAssociatedAssets() throws IOException {
  //  client.fetch(AGRICOLAE_131_TGZ.fullPath);
  //
  //  assertThat(componentAssetTestHelper
  //      .assetExists(repository, AGRICOLAE_131_TGZ.fullPath), is(true));
  //  assertThat(componentAssetTestHelper
  //      .componentExists(repository, AGRICOLAE_131_TGZ.packageName, AGRICOLAE_131_TGZ.packageVersion), is(true));
  //
  //
  //  componentAssetTestHelper.deleteComponent(repository, AGRICOLAE_131_TGZ.basePath, AGRICOLAE_131_TGZ.packageName,AGRICOLAE_131_TGZ.packageVersion);
  //
  //  assertThat(componentAssetTestHelper
  //      .assetExists(repository, AGRICOLAE_131_TGZ.fullPath), is(false));
  //  assertThat(componentAssetTestHelper
  //      .componentExists(repository, AGRICOLAE_131_TGZ.packageName, AGRICOLAE_131_TGZ.packageVersion), is(false));
  //}
}
