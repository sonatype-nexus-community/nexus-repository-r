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
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentMaintenance;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.sonatype.goodies.httpfixture.server.fluent.Behaviours.error;
import static org.sonatype.goodies.httpfixture.server.fluent.Behaviours.file;
import static org.sonatype.nexus.plugins.r.internal.RITConfig.configureRBase;
import static org.sonatype.nexus.repository.http.HttpStatus.NOT_FOUND;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;

public class RProxyIT
    extends RITSupport
{
  private RClient client;

  private Repository repository;

  private Server server;

  @Configuration
  public static Option[] configureNexus() {
    return configureRBase();
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
    final Asset asset = findAsset(repository, AGRICOLAE_131_TGZ.fullPath);
    Assert.assertThat(asset.name(), is(equalTo(AGRICOLAE_131_TGZ.fullPath)));
    Assert.assertThat(asset.contentType(), is(equalTo(CONTENT_TYPE_X_TGZ)));
    Assert.assertThat(asset.format(), is(equalTo(R_FORMAT_NAME)));
  }

  @Test
  public void fetchMetaData() throws Exception {
    assertSuccessResponseMatches(client.fetch(PACKAGES_SRC_GZ.fullPath), PACKAGES_SRC_GZ.filename);
    final Asset assetPackagesGz = findAsset(repository, PACKAGES_SRC_GZ.fullPath);
    Assert.assertEquals(PACKAGES_SRC_GZ.fullPath, assetPackagesGz.name());
    Assert.assertThat(assetPackagesGz.contentType(), is(equalTo(PACKAGES_SRC_GZ.contentType)));
    Assert.assertThat(assetPackagesGz.format(), is(equalTo(R_FORMAT_NAME)));
    Assert.assertThat(assetPackagesGz.attributes().child(R_FORMAT_NAME).get(P_ASSET_KIND), is(equalTo(PACKAGES_KIND)));

    assertSuccessResponseMatches(client.fetch(PACKAGES_RDS.fullPath), PACKAGES_RDS.filename);
    final Asset assetPackagesRds = findAsset(repository, PACKAGES_RDS.fullPath);
    Assert.assertEquals(PACKAGES_RDS.fullPath, assetPackagesRds.name());
    Assert.assertThat(assetPackagesRds.contentType(), is(equalTo(PACKAGES_RDS.contentType)));
    Assert.assertThat(assetPackagesRds.format(), is(equalTo(R_FORMAT_NAME)));
    Assert.assertThat(assetPackagesRds.attributes().child(R_FORMAT_NAME).get(P_ASSET_KIND), is(equalTo(PACKAGES_KIND)));

    assertSuccessResponseMatches(client.fetch(ARCHIVE_RDS.fullPath), ARCHIVE_RDS.filename);
    final Asset assetArchiveRds = findAsset(repository, ARCHIVE_RDS.fullPath);
    Assert.assertEquals(ARCHIVE_RDS.fullPath, assetArchiveRds.name());
    Assert.assertThat(assetArchiveRds.contentType(), is(equalTo(ARCHIVE_RDS.contentType)));
    Assert.assertThat(assetArchiveRds.format(), is(equalTo(R_FORMAT_NAME)));
    Assert.assertThat(assetArchiveRds.attributes().child(R_FORMAT_NAME).get(P_ASSET_KIND),
        is(equalTo(RDS_METADATA_KIND)));
  }

  @Test
  public void checkComponentCreated() throws Exception {
    final Component nullComponent = findComponent(repository, AGRICOLAE_131_TGZ.packageName);
    Assert.assertThat(nullComponent, is(nullValue()));
    client.fetch(AGRICOLAE_131_TGZ.fullPath);

    final Component component = findComponent(repository, AGRICOLAE_131_TGZ.packageName);
    Assert.assertThat(component.name(), is(equalTo(AGRICOLAE_131_TGZ.packageName)));
    Assert.assertThat(component.format(), is(equalTo(R_FORMAT_NAME)));
    Assert.assertThat(component.group(), is(AGRICOLAE_131_TGZ.basePath));
    Assert.assertThat(component.version(), is(equalTo(AGRICOLAE_131_TGZ.packageVersion)));
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

    final Asset asset = findAsset(repository, AGRICOLAE_131_TGZ.fullPath);
    assertNotNull(asset);
    assertNotNull(asset.componentId());

    componentAssetTestHelper.removeAsset(repository, AGRICOLAE_131_TGZ.fullPath);

    assertNull(findAsset(repository, AGRICOLAE_131_TGZ.fullPath));
    assertNull(findComponentById(repository, asset.componentId()));
  }

  @Test
  public void testDeletingComponentDeletesAllAssociatedAssets() throws IOException {
    client.fetch(AGRICOLAE_131_TGZ.fullPath);

    final Asset asset = findAsset(repository, AGRICOLAE_131_TGZ.fullPath);
    assertNotNull(asset);
    assertNotNull(asset.componentId());

    final Component component = findComponentById(repository, asset.componentId());
    assertNotNull(component);

    ComponentMaintenance maintenanceFacet = repository.facet(ComponentMaintenance.class);
    maintenanceFacet.deleteComponent(component.getEntityMetadata().getId(), true);

    assertNull(findAsset(repository, AGRICOLAE_131_TGZ.fullPath));
    assertNull(findComponentById(repository, asset.componentId()));
  }
}
