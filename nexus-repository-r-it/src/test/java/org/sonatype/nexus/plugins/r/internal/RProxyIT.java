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
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentMaintenance;
import org.sonatype.nexus.testsuite.testsupport.NexusITSupport;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.sonatype.goodies.httpfixture.server.fluent.Behaviours.error;
import static org.sonatype.goodies.httpfixture.server.fluent.Behaviours.file;
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
        .serve("/" + AGRICOLAE_PATH_FULL_131_TGZ)
        .withBehaviours(file(testData.resolveFile(AGRICOLAE_PKG_FILE_NAME_131_TGZ)))
        .serve("/" + AGRICOLAE_PATH_FULL_131_TARGZ)
        .withBehaviours(file(testData.resolveFile(AGRICOLAE_PKG_FILE_NAME_131_TARGZ)))
        .serve("/" + PACKAGES_GZ_PATH_FULL)
        .withBehaviours(file(testData.resolveFile(PACKAGES_GZ_FILE_NAME)))
        .serve("/" + PACKAGES_RDS_PATH_FULL)
        .withBehaviours(file(testData.resolveFile(PACKAGES_RDS_FILE_NAME)))
        .serve("/" + ARCHIVE_RDS_PATH_FULL)
        .withBehaviours(file(testData.resolveFile(ARCHIVE_RDS_FILE_NAME)))
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
    assertSuccessResponseMatches(client.fetch(AGRICOLAE_PATH_FULL_131_TGZ), AGRICOLAE_PKG_FILE_NAME_131_TGZ);
    final Asset asset = findAsset(repository, AGRICOLAE_PATH_FULL_131_TGZ);
    Assert.assertThat(asset.name(), is(equalTo(AGRICOLAE_PATH_FULL_131_TGZ)));
    Assert.assertThat(asset.contentType(), is(equalTo(CONTENT_TYPE_X_TGZ)));
    Assert.assertThat(asset.format(), is(equalTo(R_FORMAT_NAME)));
  }

  @Test
  public void fetchMetaData() throws Exception {
    assertSuccessResponseMatches(client.fetch(PACKAGES_GZ_PATH_FULL), PACKAGES_GZ_FILE_NAME);
    final Asset assetPackagesGz = findAsset(repository, PACKAGES_GZ_PATH_FULL);
    Assert.assertEquals(PACKAGES_GZ_PATH_FULL, assetPackagesGz.name());
    Assert.assertThat(assetPackagesGz.contentType(), is(equalTo(CONTENT_TYPE_X_GZIP)));
    Assert.assertThat(assetPackagesGz.format(), is(equalTo(R_FORMAT_NAME)));
    Assert.assertThat(assetPackagesGz.attributes().child(R_FORMAT_NAME).get(P_ASSET_KIND), is(equalTo(PACKAGES_KIND)));

    assertSuccessResponseMatches(client.fetch(PACKAGES_RDS_PATH_FULL), PACKAGES_RDS_FILE_NAME);
    final Asset assetPackagesRds = findAsset(repository, PACKAGES_RDS_PATH_FULL);
    Assert.assertEquals(PACKAGES_RDS_PATH_FULL, assetPackagesRds.name());
    Assert.assertThat(assetPackagesRds.contentType(), is(equalTo(CONTENT_TYPE_X_XZ))); //PACKAGE.rds is compressed in XZ
    Assert.assertThat(assetPackagesRds.format(), is(equalTo(R_FORMAT_NAME)));
    Assert.assertThat(assetPackagesRds.attributes().child(R_FORMAT_NAME).get(P_ASSET_KIND), is(equalTo(PACKAGES_KIND)));

    assertSuccessResponseMatches(client.fetch(ARCHIVE_RDS_PATH_FULL), ARCHIVE_RDS_FILE_NAME);
    final Asset assetArchiveRds = findAsset(repository, ARCHIVE_RDS_PATH_FULL);
    Assert.assertEquals(ARCHIVE_RDS_PATH_FULL, assetArchiveRds.name());
    Assert.assertThat(assetArchiveRds.contentType(), is(equalTo(CONTENT_TYPE_GZIP)));
    Assert.assertThat(assetArchiveRds.format(), is(equalTo(R_FORMAT_NAME)));
    Assert.assertThat(assetArchiveRds.attributes().child(R_FORMAT_NAME).get(P_ASSET_KIND),
        is(equalTo(RDS_METADATA_KIND)));
  }

  @Test
  public void checkComponentCreated() throws Exception {
    final Component nullComponent = findComponent(repository, AGRICOLAE_PKG_NAME);
    Assert.assertThat(nullComponent, is(nullValue()));
    client.fetch(AGRICOLAE_PATH_FULL_131_TGZ);

    final Component component = findComponent(repository, AGRICOLAE_PKG_NAME);
    Assert.assertThat(component.name(), is(equalTo(AGRICOLAE_PKG_NAME)));
    Assert.assertThat(component.format(), is(equalTo(R_FORMAT_NAME)));
    Assert.assertThat(component.group(), is(nullValue()));
    Assert.assertThat(component.version(), is(equalTo(AGRICOLAE_PKG_VERSION_131)));
  }

  @Test
  public void shouldCachePackageGz() throws Exception {
    client.fetch(PACKAGES_GZ_PATH_FULL);
    server.stop();
    assertSuccessResponseMatches(client.fetch(PACKAGES_GZ_PATH_FULL), PACKAGES_GZ_FILE_NAME);
  }

  @Test
  public void shouldCachePackageRds() throws Exception {
    client.fetch(PACKAGES_RDS_PATH_FULL);
    server.stop();
    assertSuccessResponseMatches(client.fetch(PACKAGES_RDS_PATH_FULL), PACKAGES_RDS_FILE_NAME);
  }

  @Test
  public void shouldCacheTgzPackageFile() throws Exception {
    client.fetch(AGRICOLAE_PATH_FULL_131_TGZ);
    server.stop();
    assertSuccessResponseMatches(client.fetch(AGRICOLAE_PATH_FULL_131_TGZ), AGRICOLAE_PKG_FILE_NAME_131_TGZ);
  }

  @Test
  public void testDeletingRemainingAssetAlsoDeletesComponent() throws IOException {
    client.fetch(AGRICOLAE_PATH_FULL_131_TGZ);

    final Asset asset = findAsset(repository, AGRICOLAE_PATH_FULL_131_TGZ);
    assertNotNull(asset);
    assertNotNull(asset.componentId());

    final Component component = findComponentById(repository, asset.componentId());
    assertNotNull(component);
    assertEquals(1, findAssetsByComponent(repository, component).size());

    ComponentMaintenance maintenanceFacet = repository.facet(ComponentMaintenance.class);
    maintenanceFacet.deleteAsset(asset.getEntityMetadata().getId(), true);

    assertNull(findAsset(repository, AGRICOLAE_PATH_FULL_131_TGZ));
    assertNull(findComponentById(repository, asset.componentId()));
  }

  @Test
  public void testDeletingAssetWhenMultipleExistDoesNotDeleteComponent() throws IOException {
    client.fetch(AGRICOLAE_PATH_FULL_131_TARGZ);
    client.fetch(AGRICOLAE_PATH_FULL_131_TGZ);

    final Asset assetTgz = findAsset(repository, AGRICOLAE_PATH_FULL_131_TGZ);
    assertNotNull(assetTgz);
    assertNotNull(assetTgz.componentId());

    final Asset assetTargz = findAsset(repository, AGRICOLAE_PATH_FULL_131_TARGZ);
    assertNotNull(assetTargz);
    assertNotNull(assetTargz.componentId());

    final Component component = findComponentById(repository, assetTargz.componentId());
    assertNotNull(component);
    assertEquals(2, findAssetsByComponent(repository, component).size());

    ComponentMaintenance maintenanceFacet = repository.facet(ComponentMaintenance.class);
    maintenanceFacet.deleteAsset(assetTargz.getEntityMetadata().getId(), true);

    assertNull(findAsset(repository, AGRICOLAE_PKG_FILE_NAME_131_TARGZ));
    assertNotNull(findComponentById(repository, assetTargz.componentId()));
  }

  @Test
  public void testDeletingComponentDeletesAllAssociatedAssets() throws IOException {
    client.fetch(AGRICOLAE_PATH_FULL_131_TGZ);

    final Asset asset = findAsset(repository, AGRICOLAE_PATH_FULL_131_TGZ);
    assertNotNull(asset);
    assertNotNull(asset.componentId());

    final Component component = findComponentById(repository, asset.componentId());
    assertNotNull(component);

    ComponentMaintenance maintenanceFacet = repository.facet(ComponentMaintenance.class);
    maintenanceFacet.deleteComponent(component.getEntityMetadata().getId(), true);

    assertNull(findAsset(repository, AGRICOLAE_PATH_FULL_131_TGZ));
    assertNull(findComponentById(repository, asset.componentId()));
  }
}
