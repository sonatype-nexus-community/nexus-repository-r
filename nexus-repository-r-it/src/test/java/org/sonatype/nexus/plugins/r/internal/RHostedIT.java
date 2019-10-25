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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFileExtend;
import static org.sonatype.nexus.repository.http.HttpStatus.BAD_REQUEST;
import static org.sonatype.nexus.repository.http.HttpStatus.NOT_FOUND;
import static org.sonatype.nexus.repository.r.internal.PackageValidator.NOT_VALID_EXTENSION_ERROR_MESSAGE;
import static org.sonatype.nexus.repository.r.internal.PackageValidator.NOT_VALID_PATH_ERROR_MESSAGE;

public class RHostedIT
    extends RITSupport
{
  private RClient client;

  private Repository repository;

  @Configuration
  public static Option[] configureNexus() {
    return NexusPaxExamSupport.options(
        NexusITSupport.configureNexusBase(),
        nexusFeature("org.sonatype.nexus.plugins", "nexus-repository-r"),
        editConfigurationFileExtend(NEXUS_PROPERTIES_FILE, "nexus.r.packagesBuilder.interval",
            String.valueOf(METADATA_PROCESSING_DELAY_MILLIS))
    );
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
    Component component = findComponent(repository, AGRICOLAE_121_TARGZ.packageName);
    assertThat(component.name(), is(equalTo(AGRICOLAE_121_TARGZ.packageName)));
    assertThat(component.version(), is(equalTo(AGRICOLAE_121_TARGZ.packageVersion)));
    assertThat(component.group(), is(equalTo(AGRICOLAE_121_TARGZ.basePath)));

    //Verify Asset is created.
    Asset asset = findAsset(repository, AGRICOLAE_121_TARGZ.fullPath);
    assertThat(asset.name(), is(equalTo(AGRICOLAE_121_TARGZ.fullPath)));
    assertThat(asset.format(), is(equalTo(R_FORMAT_NAME)));
  }

  @Test
  public void testUploadFailedWrongPackageExtension() throws Exception
  {
    HttpResponse httpResponse = uploadSinglePackage(AGRICOLAE_131_XXX);
    assertThat(httpResponse.getStatusLine().getStatusCode(), is(BAD_REQUEST));
    assertThat(httpResponse.getStatusLine().getReasonPhrase(), is(NOT_VALID_EXTENSION_ERROR_MESSAGE));
    assertNull(findAsset(repository, AGRICOLAE_131_XXX.fullPath));
  }

  @Test
  public void testUploadFailedWrongPackagePath() throws Exception
  {
    HttpResponse httpResponse = uploadSinglePackage(AGRICOLAE_131_TARGZ_WRONG_PATH);
    assertThat(httpResponse.getStatusLine().getStatusCode(), is(BAD_REQUEST));
    assertThat(httpResponse.getStatusLine().getReasonPhrase(), is(NOT_VALID_PATH_ERROR_MESSAGE));
    assertNull(findAsset(repository, AGRICOLAE_131_TARGZ_WRONG_PATH.fullPath));
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
    assertNotNull(findAsset(repository, PACKAGES_SRC_GZ.fullPath));

    // Verify PACKAGES(metadata) contain appropriate content about bin R package
    final InputStream contentBin = client.fetch(PACKAGES_BIN_GZ.fullPath).getEntity().getContent();
    verifyTextGzipContent(is(equalTo(agricolae131Content)), contentBin);
    assertNotNull(findAsset(repository, PACKAGES_BIN_GZ.fullPath));

    // Verify PACKAGES(metadata) is clean if component has been deleted
    List<Component> components = getAllComponents(repository);
    ComponentMaintenance maintenanceFacet = repository.facet(ComponentMaintenance.class);
    components.forEach(component -> maintenanceFacet.deleteComponent(component.getEntityMetadata().getId()));

    Thread.sleep(METADATA_PROCESSING_WAIT_INTERVAL_MILLIS);

    final InputStream contentSrcAfterDelete = client.fetch(PACKAGES_SRC_GZ.fullPath).getEntity().getContent();
    verifyTextGzipContent(is(equalTo("")), contentSrcAfterDelete);
    assertNotNull(findAsset(repository, PACKAGES_SRC_GZ.fullPath));

    final InputStream contentBinAfterDelete = client.fetch(PACKAGES_BIN_GZ.fullPath).getEntity().getContent();
    verifyTextGzipContent(is(equalTo("")), contentBinAfterDelete);
    assertNotNull(findAsset(repository, PACKAGES_BIN_GZ.fullPath));
  }

  @Test
  public void testDeletingRemainingAssetAlsoDeletesComponent() {
    final Asset asset = findAsset(repository, AGRICOLAE_121_TARGZ.fullPath);
    assertNotNull(asset);
    assertNotNull(asset.componentId());

    final Component component = findComponentById(repository, asset.componentId());
    assertNotNull(component);
    assertEquals(1, findAssetsByComponent(repository, component).size());

    ComponentMaintenance maintenanceFacet = repository.facet(ComponentMaintenance.class);
    maintenanceFacet.deleteAsset(asset.getEntityMetadata().getId(), true);

    assertNull(findAsset(repository, AGRICOLAE_121_TARGZ.fullPath));
    assertNull(findComponentById(repository, asset.componentId()));
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

    assertNull(findAsset(repository, AGRICOLAE_121_TARGZ.fullPath));
    assertNull(findComponentById(repository, asset.componentId()));
  }

  private void uploadPackages(TestPackage... packages) throws IOException {
    assertThat(getAllComponents(repository), hasSize(0));
    for (TestPackage testPackage : packages) {
      uploadSinglePackage(testPackage);
    }
    assertThat(getAllComponents(repository), hasSize(packages.length));
  }

  private HttpResponse uploadSinglePackage(TestPackage testPackage) throws IOException {
    return client.putAndClose(testPackage.fullPath, fileToHttpEntity(testPackage.filename));
  }
}
