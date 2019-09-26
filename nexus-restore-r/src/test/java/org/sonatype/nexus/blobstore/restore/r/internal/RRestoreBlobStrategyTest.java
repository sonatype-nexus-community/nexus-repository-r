package org.sonatype.nexus.blobstore.restore.r.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.restore.RestoreBlobData;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.r.RRestoreFacet;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA256;

public class RRestoreBlobStrategyTest
    extends TestSupport
{
  private static final String TEST_BLOB_STORE_NAME = "test";

  private static final String ARCHIVE_PATH = "src/contrib/curl_4.2.tar.gz";

  private static final String PACKAGES_PATH = "src/contrib/PACKAGES";

  private static final String PACKAGES_GZ_PATH = "src/contrib/PACKAGES.gz";

  private static final String PACKAGES_RDS_PATH = "src/contrib/PACKAGES.rds";

  @Mock
  RepositoryManager repositoryManager;

  @Mock
  NodeAccess nodeAccess;

  @Mock
  BlobStoreManager blobStoreManager;

  @Mock
  Repository repository;

  @Mock
  StorageFacet storageFacet;

  @Mock
  RRestoreFacet rRestoreFacet;

  @Mock
  RRestoreBlobData rRestoreBlobData;

  @Mock
  private RestoreBlobData restoreBlobData;

  @Mock
  Blob blob;

  @Mock
  BlobStore blobStore;

  @Mock
  StorageTx storageTx;

  private byte[] blobBytes = "blobbytes".getBytes();

  private Properties properties = new Properties();

  private RRestoreBlobStrategy restoreBlobStrategy;

  @Before
  public void setup() {
    restoreBlobStrategy = new RRestoreBlobStrategy(nodeAccess, repositoryManager, blobStoreManager, new DryRunPrefix("dryrun"));

    when(repositoryManager.get(anyString())).thenReturn(repository);
    when(repository.facet(RRestoreFacet.class)).thenReturn(rRestoreFacet);
    when(repository.optionalFacet(RRestoreFacet.class)).thenReturn(Optional.of(rRestoreFacet));
    when(repository.optionalFacet(StorageFacet.class)).thenReturn(Optional.of(storageFacet));
    when(rRestoreBlobData.getBlobData()).thenReturn(restoreBlobData);
    when(rRestoreBlobData.getBlobData().getBlobName()).thenReturn(ARCHIVE_PATH);
    when(rRestoreBlobData.getBlobData().getRepository()).thenReturn(repository);
    when(storageFacet.txSupplier()).thenReturn(() -> storageTx);
    when(blob.getInputStream()).thenReturn(new ByteArrayInputStream(blobBytes));
    when(blobStoreManager.get(TEST_BLOB_STORE_NAME)).thenReturn(blobStore);
    when(restoreBlobData.getRepository()).thenReturn(repository);

    properties.setProperty("@BlobStore.created-by", "anonymous");
    properties.setProperty("size", "1330");
    properties.setProperty("@Bucket.repo-name", "r-proxy");
    properties.setProperty("creationTime", "1533220387218");
    properties.setProperty("@BlobStore.created-by-ip", "127.0.0.1");
    properties.setProperty("@BlobStore.content-type", "text/html");
    properties.setProperty("@BlobStore.blob-name", ARCHIVE_PATH);
    properties.setProperty("sha1", "0088eb478752a810f48f04d3cf9f46d2924e334a");
  }

  @Test
  public void testBlobDataIsCreated() {
    assertThat(restoreBlobStrategy.createRestoreData(restoreBlobData).getBlobData(), is(restoreBlobData));
  }

  @Test(expected = IllegalStateException.class)
  public void testIfBlobDataNameIsEmpty_ExceptionIsThrown() {
    when(rRestoreBlobData.getBlobData().getBlobName()).thenReturn("");
    restoreBlobStrategy.createRestoreData(restoreBlobData);
  }

  @Test
  public void testCorrectHashAlgorithmsAreSupported() {
    assertThat(restoreBlobStrategy.getHashAlgorithms(), containsInAnyOrder(SHA1));
  }

  @Test
  public void testAppropriatePathIsReturned() {
    assertThat(restoreBlobStrategy.getAssetPath(rRestoreBlobData), is(ARCHIVE_PATH));
  }

  @Test
  public void testPackageIsRestored() throws Exception {
    restoreBlobStrategy.restore(properties, blob, TEST_BLOB_STORE_NAME, false);
    verify(rRestoreFacet).assetExists(ARCHIVE_PATH);
    verify(rRestoreFacet).restore(any(AssetBlob.class), eq(ARCHIVE_PATH));
    verifyNoMoreInteractions(rRestoreFacet);
  }

  @Test
  public void testRestoreIsSkip_IfPackageExists() {
    when(rRestoreFacet.assetExists(ARCHIVE_PATH)).thenReturn(true);
    restoreBlobStrategy.restore(properties, blob, TEST_BLOB_STORE_NAME, false);

    verify(rRestoreFacet).assetExists(ARCHIVE_PATH);
    verify(rRestoreFacet).componentRequired(ARCHIVE_PATH);
    verifyNoMoreInteractions(rRestoreFacet);
  }

  @Test
  public void testComponentIsRequiredForGz() {
    boolean expected = true;
    when(rRestoreFacet.componentRequired(ARCHIVE_PATH)).thenReturn(expected);
    assertThat(restoreBlobStrategy.componentRequired(rRestoreBlobData), is(expected));
    verify(rRestoreFacet).componentRequired(ARCHIVE_PATH);
    verifyNoMoreInteractions(rRestoreFacet);
  }

  @Test
  public void testComponentQuery() throws IOException
  {
    Query query = restoreBlobStrategy.getComponentQuery(rRestoreBlobData);
  }
}
