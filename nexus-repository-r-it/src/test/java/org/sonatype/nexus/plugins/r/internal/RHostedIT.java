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
import java.nio.file.Files;
import java.util.List;

import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.pax.exam.NexusPaxExamSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentMaintenance;
import org.sonatype.nexus.testsuite.testsupport.NexusITSupport;

import org.apache.http.HttpResponse;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.sonatype.nexus.repository.http.HttpStatus.BAD_REQUEST;
import static org.sonatype.nexus.repository.http.HttpStatus.NOT_FOUND;

public class RHostedIT
    extends RITSupport
{
  private RClient client;

  private Repository repository;

  @Configuration
  public static Option[] configureNexus() {
    return NexusPaxExamSupport.options(
        NexusITSupport.configureNexusBase(),
        nexusFeature("org.sonatype.nexus.plugins", "nexus-repository-r")
    );
  }

  @Before
  public void setUp() throws Exception {
    BaseUrlHolder.set(this.nexusUrl.toString());
    repository = repos.createRHosted("r-hosted-test");
    client = createRClient(repository);
    uploadPackages(AGRICOLAE_PKG_FILE_NAME_121_TARGZ, AGRICOLAE_PKG_FILE_NAME_131_TGZ);
  }

  @Test
  public void testPackageUpload() throws Exception
  {
    //Verify DB contains data about uploaded component and asset
    Component component = findComponent(repository, AGRICOLAE_PKG_NAME);
    assertThat(component.name(), is(equalTo(AGRICOLAE_PKG_NAME)));
    assertThat(component.version(), is(equalTo(AGRICOLAE_PKG_VERSION_121)));

    //Verify Asset is created.
    Asset asset = findAsset(repository, AGRICOLAE_PATH_FULL_121_TARGZ);
    assertThat(asset.name(), is(equalTo(AGRICOLAE_PATH_FULL_121_TARGZ)));
    assertThat(asset.format(), is(equalTo(R_FORMAT_NAME)));
  }

  @Test
  public void testUploadFailedWrongPackageExtension() throws Exception
  {
    assertThat(uploadSinglePackage(AGRICOLAE_PKG_FILE_NAME_WRONG_EXTENSION_XXX).getStatusLine().getStatusCode(),
        is(BAD_REQUEST));
    assertNull(findAsset(repository, AGRICOLAE_PATH_FULL_WRONG_EXTENSION_XXX));
  }

  @Test
  public void testFetchPackage() throws Exception
  {
    HttpResponse resp = client.fetch(AGRICOLAE_PATH_FULL_131_TGZ);
    assertThat(resp.getEntity().getContentType().getValue(), equalTo(CONTENT_TYPE_X_TGZ));
    assertSuccessResponseMatches(resp, AGRICOLAE_PKG_FILE_NAME_131_TGZ);
  }

  @Test
  public void testFetchWrongExtensionNotFoundPackage() throws Exception
  {
    assertThat(client.fetch(AGRICOLAE_PATH_FULL_WRONG_EXTENSION_XXX).getStatusLine().getStatusCode(), is(NOT_FOUND));
  }

  @Test
  public void testMetadataProcessing() throws Exception
  {
    final String agricolae121Content =
        new String(Files.readAllBytes(testData.resolveFile(PACKAGES_AGRICOLAE_121_NAME).toPath()));
    final String agricolae131Content =
        new String(Files.readAllBytes(testData.resolveFile(PACKAGES_AGRICOLAE_131_NAME).toPath()));

    //Verify PACKAGES(metadata) contain appropriate content about R package.
    final InputStream content = client.fetch(PACKAGES_GZ_PATH_FULL).getEntity().getContent();
    verifyTextGzipContent(is(equalTo(agricolae131Content)), content);

    //Verify PACKAGES(metadata) is clean if component has been deleted
    List<Component> components = getAllComponents(repository);
    ComponentMaintenance maintenanceFacet = repository.facet(ComponentMaintenance.class);
    maintenanceFacet.deleteComponent(components.get(1).getEntityMetadata().getId());

    final InputStream contentAfterDelete = client.fetch(PACKAGES_GZ_PATH_FULL).getEntity().getContent();
    verifyTextGzipContent(is(equalTo(agricolae121Content)), contentAfterDelete);
  }

  @Test
  public void testDeletingRemainingAssetAlsoDeletesComponent() {
    final Asset asset = findAsset(repository, AGRICOLAE_PATH_FULL_121_TARGZ);
    assertNotNull(asset);
    assertNotNull(asset.componentId());

    final Component component = findComponentById(repository, asset.componentId());
    assertNotNull(component);
    assertEquals(1, findAssetsByComponent(repository, component).size());

    ComponentMaintenance maintenanceFacet = repository.facet(ComponentMaintenance.class);
    maintenanceFacet.deleteAsset(asset.getEntityMetadata().getId(), true);

    assertNull(findAsset(repository, AGRICOLAE_PATH_FULL_121_TARGZ));
    assertNull(findComponentById(repository, asset.componentId()));
  }

  @Test
  public void testDeletingAssetWhenMultipleExistDoesNotDeleteComponent() throws IOException {
    uploadSinglePackage(AGRICOLAE_PKG_FILE_NAME_131_TARGZ);

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
  public void testDeletingComponentDeletesAllAssociatedAssets() {
    final Asset asset = findAsset(repository, AGRICOLAE_PATH_FULL_121_TARGZ);
    assertNotNull(asset);
    assertNotNull(asset.componentId());

    final Component component = findComponentById(repository, asset.componentId());
    assertNotNull(component);

    ComponentMaintenance maintenanceFacet = repository.facet(ComponentMaintenance.class);
    maintenanceFacet.deleteComponent(component.getEntityMetadata().getId(), true);

    assertNull(findAsset(repository, AGRICOLAE_PATH_FULL_121_TARGZ));
    assertNull(findComponentById(repository, asset.componentId()));
  }

  private void uploadPackages(String... names) throws IOException {
    assertThat(getAllComponents(repository), hasSize(0));
    for (String name : names) {
      uploadSinglePackage(name);
    }
    assertThat(getAllComponents(repository), hasSize(names.length));
  }

  private HttpResponse uploadSinglePackage(String name) throws IOException {
    return client.put(format("%s/%s", PKG_GZ_PATH, name), fileToHttpEntity(name));
  }
}
