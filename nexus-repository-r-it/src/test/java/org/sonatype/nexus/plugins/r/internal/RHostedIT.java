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
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentMaintenance;

import org.apache.http.HttpResponse;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.sonatype.nexus.plugins.r.internal.RITConfig.configureRWithMetadataProcessingInterval;
import static org.sonatype.nexus.repository.http.HttpStatus.BAD_REQUEST;
import static org.sonatype.nexus.repository.http.HttpStatus.NOT_FOUND;
import static org.sonatype.nexus.repository.r.internal.util.PackageValidator.NOT_VALID_EXTENSION_ERROR_MESSAGE;
import static org.sonatype.nexus.repository.r.internal.util.PackageValidator.NOT_VALID_PATH_ERROR_MESSAGE;

public class RHostedIT
    extends RITSupport
{
  private RClient client;

  private Repository repository;

  @Configuration
  public static Option[] configureNexus() {
    return configureRWithMetadataProcessingInterval();
  }

  @Before
  public void setUp() throws Exception {
    BaseUrlHolder.set(this.nexusUrl.toString());
    repository = repos.createRHosted("r-hosted-test");
    client = createRClient(repository);
    uploadPackages(AGRICOLAE_121_TARGZ, AGRICOLAE_131_TGZ);
  }

  @Test
  public void testPackageUpload() throws Exception
  {
    //Verify DB contains data about uploaded component and asset
    assertThat(componentAssetTestHelper
        .componentExists(repository, AGRICOLAE_121_TARGZ.packageName, AGRICOLAE_121_TARGZ.packageVersion), is(true));

    //Verify Asset is created.
    assertThat( componentAssetTestHelper
        .assetExists(repository, AGRICOLAE_121_TARGZ.fullPath), is(true));
  }

  @Test
  public void testUploadFailedWrongPackageExtension() throws Exception
  {
    HttpResponse httpResponse = uploadSinglePackage(AGRICOLAE_131_XXX);
    assertThat(httpResponse.getStatusLine().getStatusCode(), is(BAD_REQUEST));
    assertThat(httpResponse.getStatusLine().getReasonPhrase(), is(NOT_VALID_EXTENSION_ERROR_MESSAGE));
    assertThat(componentAssetTestHelper
        .assetExists(repository,  AGRICOLAE_131_XXX.fullPath), is(false));
  }

  @Test
  public void testUploadFailedWrongPackagePath() throws Exception
  {
    HttpResponse httpResponse = uploadSinglePackage(AGRICOLAE_131_TARGZ_WRONG_PATH);
    assertThat(httpResponse.getStatusLine().getStatusCode(), is(BAD_REQUEST));
    assertThat(httpResponse.getStatusLine().getReasonPhrase(), is(NOT_VALID_PATH_ERROR_MESSAGE));
    assertThat( componentAssetTestHelper
        .assetExists(repository,  AGRICOLAE_131_TARGZ_WRONG_PATH.fullPath), is(false));
  }

  @Test
  public void testFetchNotSupportedMetadata() throws Exception
  {
    HttpResponse resp = client.fetch(PACKAGES_RDS.fullPath);
    assertThat(resp.getStatusLine().getStatusCode(), is(NOT_FOUND));
    assertThat(resp.getStatusLine().getReasonPhrase(), is("This metadata type is not supported for now."));
  }

  @Test
  public void testFetchPackage() throws Exception
  {
    HttpResponse resp = client.fetch(AGRICOLAE_131_TGZ.fullPath);
    assertThat(resp.getEntity().getContentType().getValue(), equalTo(AGRICOLAE_131_TGZ.contentType));
    assertSuccessResponseMatches(resp, AGRICOLAE_131_TGZ.filename);
  }

  @Test
  public void testFetchNotExistingPackage() throws Exception
  {
    assertThat(client.fetch(AGRICOLAE_131_TARGZ.fullPath).getStatusLine().getStatusCode(), is(NOT_FOUND));
  }

  @Test
  public void testMetadataProcessing() throws Exception
  {
    // Uploading package with same name and lower version that should be skipped in src metadata
    uploadSinglePackage(AGRICOLAE_101_TARGZ);

    Thread.sleep(METADATA_PROCESSING_WAIT_INTERVAL_MILLIS);

    final String agricolae121Content =
        new String(Files.readAllBytes(testData.resolveFile(PACKAGES_AGRICOLAE_121_FILENAME).toPath()));
    final String agricolae131Content =
        new String(Files.readAllBytes(testData.resolveFile(PACKAGES_AGRICOLAE_131_FILENAME).toPath()));

    // Verify PACKAGES(metadata) contain appropriate content about source R package (version 1.0-1 is skipped)
    final InputStream contentSrc = client.fetch(PACKAGES_SRC_GZ.fullPath).getEntity().getContent();
    verifyTextGzipContent(is(equalTo(agricolae121Content)), contentSrc);
    assertThat(componentAssetTestHelper
        .assetExists(repository, PACKAGES_SRC_GZ.fullPath), is(true));

    // Verify PACKAGES(metadata) contain appropriate content about bin R package
    final InputStream contentBin = client.fetch(PACKAGES_BIN_GZ.fullPath).getEntity().getContent();
    verifyTextGzipContent(is(equalTo(agricolae131Content)), contentBin);
    assertThat(componentAssetTestHelper
        .assetExists(repository, PACKAGES_BIN_GZ.fullPath), is(true));

    // Verify PACKAGES(metadata) is clean if component has been deleted
    ComponentMaintenance maintenanceFacet = repository.facet(ComponentMaintenance.class);
    final List<Component> allComponents = RITSupport.getAllComponents(repository);
    allComponents.stream().forEach(component ->  maintenanceFacet.deleteComponent(component.getEntityMetadata().getId(), true));

    Thread.sleep(METADATA_PROCESSING_WAIT_INTERVAL_MILLIS);

    final InputStream contentSrcAfterDelete = client.fetch(PACKAGES_SRC_GZ.fullPath).getEntity().getContent();
    verifyTextGzipContent(is(equalTo("")), contentSrcAfterDelete);
    assertThat(componentAssetTestHelper
        .assetExists(repository, PACKAGES_SRC_GZ.fullPath), is(true));

    final InputStream contentBinAfterDelete = client.fetch(PACKAGES_BIN_GZ.fullPath).getEntity().getContent();
    verifyTextGzipContent(is(equalTo("")), contentBinAfterDelete);
    assertThat(componentAssetTestHelper
        .assetExists(repository, PACKAGES_BIN_GZ.fullPath), is(true));
  }

  @Test
  public void testDeletingRemainingAssetAlsoDeletesComponent() {
    assertThat(componentAssetTestHelper
          .assetExists(repository, AGRICOLAE_121_TARGZ.fullPath), is(true));
    assertThat(componentAssetTestHelper
        .componentExists(repository, AGRICOLAE_121_TARGZ.packageName, AGRICOLAE_121_TARGZ.packageVersion), is(true));

    componentAssetTestHelper.removeAsset(repository,  AGRICOLAE_121_TARGZ.fullPath);

    assertThat(componentAssetTestHelper
        .assetExists(repository, AGRICOLAE_121_TARGZ.fullPath), is(false));

    assertThat(componentAssetTestHelper
        .componentExists(repository, AGRICOLAE_121_TARGZ.packageName, AGRICOLAE_121_TARGZ.packageVersion), is(false));
  }

  @Test
  public void testDeletingComponentDeletesAllAssociatedAssets() {
    final Asset asset = findAsset(repository, AGRICOLAE_121_TARGZ.fullPath);
    assertNotNull(asset);
    assertNotNull(asset.componentId());

    final Component component = findComponentById(repository, asset.componentId());
    assertNotNull(component);

    ComponentMaintenance maintenanceFacet = repository.facet(ComponentMaintenance.class);
    maintenanceFacet.deleteComponent(component.getEntityMetadata().getId(), true);

    assertThat(componentAssetTestHelper.assetExists(repository, AGRICOLAE_121_TARGZ.fullPath), is(false));
    assertNull(findComponentById(repository, asset.componentId()));
  }

  private void uploadPackages(TestPackage... packages) throws IOException {
    assertThat(componentAssetTestHelper.countComponents(repository), is(0));
    for (TestPackage testPackage : packages) {
      uploadSinglePackage(testPackage);
    }
    assertThat(componentAssetTestHelper.countComponents(repository), is(packages.length));
  }

  private HttpResponse uploadSinglePackage(TestPackage testPackage) throws IOException {
    return client.putAndClose(testPackage.fullPath, fileToHttpEntity(testPackage.filename));
  }
}
