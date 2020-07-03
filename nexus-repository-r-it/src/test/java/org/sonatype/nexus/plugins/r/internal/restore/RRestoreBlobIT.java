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
package org.sonatype.nexus.plugins.r.internal.restore;

import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.httpfixture.server.fluent.Server;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.restore.RestoreBlobStrategy;
import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.pax.exam.NexusPaxExamSupport;
import org.sonatype.nexus.plugins.r.internal.RClient;
import org.sonatype.nexus.plugins.r.internal.RITSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.repository.r.internal.RFormat;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.testsuite.testsupport.blobstore.restore.BlobstoreRestoreTestHelper;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.sonatype.goodies.httpfixture.server.fluent.Behaviours.file;
import static org.sonatype.nexus.blobstore.api.BlobAttributesConstants.HEADER_PREFIX;
import static org.sonatype.nexus.blobstore.api.BlobStore.BLOB_NAME_HEADER;
import static org.sonatype.nexus.blobstore.api.BlobStore.CONTENT_TYPE_HEADER;
import static org.sonatype.nexus.plugins.r.internal.RITConfig.configureRBase;
import static org.sonatype.nexus.repository.storage.Bucket.REPO_NAME_HEADER;

public class RRestoreBlobIT
    extends RITSupport
{
  @Inject
  private BlobstoreRestoreTestHelper testHelper;

  @Inject
  @Named(RFormat.NAME)
  private RestoreBlobStrategy rRestoreBlobStrategy;

  private static final String HOSTED_REPO_NAME = "r-hosted";

  private static final String PROXY_REPO_NAME = "r-proxy";

  private Server proxyServer;

  private RClient hostedClient;

  private RClient proxyClient;

  private Repository hostedRepository;

  private Repository proxyRepository;

  @Configuration
  public static Option[] configureNexus() {
    return NexusPaxExamSupport.options(
        configureRBase(),
        nexusFeature("org.sonatype.nexus.plugins", "nexus-restore-r")
    );
  }

  @Before
  public void setup() throws Exception {
    BaseUrlHolder.set(this.nexusUrl.toString());
    hostedRepository = repos.createRHosted(HOSTED_REPO_NAME);
    hostedClient = createRClient(hostedRepository);

    proxyServer = Server.withPort(0)
        .serve("/" + AGRICOLAE_131_TARGZ.fullPath)
        .withBehaviours(file(testData.resolveFile(AGRICOLAE_131_TARGZ.filename)))
        .start();

    proxyRepository = repos.createRProxy(PROXY_REPO_NAME, "http://localhost:" + proxyServer.getPort() + "/");
    proxyClient = createRClient(proxyRepository);

    assertThat(hostedClient.put(AGRICOLAE_131_TARGZ.fullPath,
        fileToHttpEntity(AGRICOLAE_131_TARGZ.filename)).getStatusLine().getStatusCode(), is(HttpStatus.OK));
    assertThat(proxyClient.fetch(AGRICOLAE_131_TARGZ.fullPath).getStatusLine().getStatusCode(), is(HttpStatus.OK));
  }

  @After
  public void tearDown() throws Exception {
    if (proxyServer != null) {
      proxyServer.stop();
    }
  }

  @Test
  public void testMetadataRestoreWhenBothAssetsAndComponentsAreMissing() throws Exception {
    verifyMetadataRestored(testHelper::simulateComponentAndAssetMetadataLoss);
  }

  @Test
  public void testMetadataRestoreWhenOnlyAssetsAreMissing() throws Exception {
    verifyMetadataRestored(testHelper::simulateAssetMetadataLoss);
  }

  @Test
  public void testMetadataRestoreWhenOnlyComponentsAreMissing() throws Exception {
    verifyMetadataRestored(testHelper::simulateComponentMetadataLoss);
  }

  @Test
  public void testNotDryRunRestore()
  {
    runBlobRestore(false);
    testHelper.assertAssetInRepository(proxyRepository, AGRICOLAE_131_TARGZ.fullPath);
  }

  @Test
  public void testDryRunRestore()
  {
    runBlobRestore(true);
    testHelper.assertAssetNotInRepository(proxyRepository, AGRICOLAE_131_TARGZ.fullPath);
  }

  private void runBlobRestore(final boolean isDryRun) {
    Asset asset;
    Blob blob;
    try (StorageTx tx = getStorageTx(proxyRepository)) {
      tx.begin();
      asset = tx.findAssetWithProperty(AssetEntityAdapter.P_NAME, AGRICOLAE_131_TARGZ.fullPath,
          tx.findBucket(proxyRepository));
      assertThat(asset, Matchers.notNullValue());
      blob = tx.getBlob(asset.blobRef());
    }
    testHelper.simulateAssetMetadataLoss();
    Properties properties = new Properties();
    properties.setProperty(HEADER_PREFIX + REPO_NAME_HEADER, proxyRepository.getName());
    properties.setProperty(HEADER_PREFIX + BLOB_NAME_HEADER, asset.name());
    properties.setProperty(HEADER_PREFIX + CONTENT_TYPE_HEADER, asset.contentType());

    rRestoreBlobStrategy.restore(properties, blob, BlobStoreManager.DEFAULT_BLOBSTORE_NAME, isDryRun);
  }

  private void verifyMetadataRestored(final Runnable metadataLossSimulation) throws Exception {
    metadataLossSimulation.run();

    testHelper.runRestoreMetadataTask();

    testHelper.assertComponentInRepository(hostedRepository, AGRICOLAE_131_TARGZ.packageName);
    testHelper.assertComponentInRepository(proxyRepository, AGRICOLAE_131_TARGZ.packageName);

    testHelper.assertAssetMatchesBlob(hostedRepository, AGRICOLAE_131_TARGZ.fullPath);
    testHelper.assertAssetMatchesBlob(proxyRepository, AGRICOLAE_131_TARGZ.fullPath);

    testHelper.assertAssetAssociatedWithComponent(hostedRepository, AGRICOLAE_131_TARGZ.packageName,
        AGRICOLAE_131_TARGZ.fullPath);
    testHelper.assertAssetAssociatedWithComponent(proxyRepository, AGRICOLAE_131_TARGZ.packageName,
        AGRICOLAE_131_TARGZ.fullPath);

    assertThat(hostedClient.get(AGRICOLAE_131_TARGZ.fullPath).getStatusLine().getStatusCode(), is(HttpStatus.OK));
    assertThat(proxyClient.get(AGRICOLAE_131_TARGZ.fullPath).getStatusLine().getStatusCode(), is(HttpStatus.OK));
  }
}
