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
package org.sonatype.nexus.repository.r.internal;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.view.Content;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_DEPENDS;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_IMPORTS;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_LICENSE;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_NEEDS_COMPILATION;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_PACKAGE;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_SUGGESTS;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_VERSION;
import static org.sonatype.nexus.repository.r.internal.RDescriptionUtils.extractDescriptionFromArchive;

public class RHostedFacetImplTest
    extends RepositoryFacetTestSupport<RHostedFacetImpl>
{

  static final String PACKAGE_NAME = "package.gz";

  static final String BASE_PATH = "packages/base/path/";

  static final String REAL_PACKAGE = "r-package.zip";

  static final String PACKAGE_PATH = BASE_PATH + PACKAGE_NAME;

  static final String REAL_PACKAGE_PATH = BASE_PATH + REAL_PACKAGE;

  static final String VERSION = "1.0.0";

  static final String DEPENDS = "a,b,c";

  static final String IMPORTS = "d,e,f";

  static final String SUGGESTS = "g,h,i";

  static final String LICENSE = "MIT";

  static final String NEEDS_COMPILATION = "true";

  @Mock
  TempBlob tempBlob;

  @Mock
  Content payload;

  @Mock
  AssetBlob assetBlob;

  @Mock
  Component component;

  @Override
  protected RHostedFacetImpl initialiseSystemUnderTest() {
    return new RHostedFacetImpl();
  }

  @Before
  public void setup() {
    when(formatAttributes.get(P_PACKAGE, String.class)).thenReturn(PACKAGE_NAME);
    when(formatAttributes.get(P_VERSION, String.class)).thenReturn(VERSION);
    when(formatAttributes.get(P_DEPENDS, String.class)).thenReturn(DEPENDS);
    when(formatAttributes.get(P_IMPORTS, String.class)).thenReturn(IMPORTS);
    when(formatAttributes.get(P_SUGGESTS, String.class)).thenReturn(SUGGESTS);
    when(formatAttributes.get(P_LICENSE, String.class)).thenReturn(LICENSE);
    when(formatAttributes.get(P_NEEDS_COMPILATION, String.class)).thenReturn(NEEDS_COMPILATION);
    when(asset.name()).thenReturn(PACKAGE_PATH);
  }


  @Test
  public void getPackagesReturnsPackage() throws Exception {
    assets.add(asset);
    Content packages = underTest.getPackages(PACKAGE_PATH);
    try (InputStream in = packages.openInputStream()) {
      Map<String, String> attributes = extractDescriptionFromArchive(PACKAGE_NAME, in);
      assertThat(attributes.get(P_PACKAGE), is(equalTo(PACKAGE_NAME)));
      assertThat(attributes.get(P_VERSION), is(equalTo(VERSION)));
      assertThat(attributes.get(P_DEPENDS), is(equalTo(DEPENDS)));
      assertThat(attributes.get(P_IMPORTS), is(equalTo(IMPORTS)));
      assertThat(attributes.get(P_SUGGESTS), is(equalTo(SUGGESTS)));
      assertThat(attributes.get(P_LICENSE), is(equalTo(LICENSE)));
      assertThat(attributes.get(P_NEEDS_COMPILATION), is(equalTo(NEEDS_COMPILATION)));
    }
  }

  @Test
  public void getPackagesReturnsCorrectContentType() throws Exception {
    Content packages = underTest.getPackages(PACKAGE_PATH);
    assertThat(packages.getContentType(), is(equalTo("application/x-gzip")));
  }

  @Test(expected = NullPointerException.class)
  public void failFastOnGetPackagesWithNull() throws Exception {
    underTest.getPackages(null);
  }

  @Test
  public void getArchive() throws Exception {
    Content archive = underTest.getArchive(PACKAGE_PATH);
    assertThat(archive, is(notNullValue()));
  }

  @Test(expected = NullPointerException.class)
  public void failFastOnGetArchiveWithNull() throws Exception {
    underTest.getArchive(null);
  }

  @Test
  public void nullWhenAssetNullOnGetArchive() throws Exception {
    when(storageTx.findAssetWithProperty(anyString(), anyString(), any(Bucket.class))).thenReturn(null);
    Content archive = underTest.getArchive(PACKAGE_PATH);
    assertThat(archive, is(nullValue()));
  }

  @Test
  public void markAssetAsDownloadedAndSaveOnGetArchive() throws Exception {
    when(asset.markAsDownloaded()).thenReturn(true);
    underTest.getArchive(PACKAGE_PATH);
    verify(storageTx).saveAsset(asset);
  }

  @Test
  public void doNotSaveWhenNotMarkedAsDownloaded() throws Exception {
    when(asset.markAsDownloaded()).thenReturn(false);
    underTest.getArchive(PACKAGE_PATH);
    verify(storageTx, never()).saveAsset(asset);
  }

  @Test
  public void putArchive() throws Exception {
    List<Component> list = ImmutableList.of(component);
    when(tempBlob.get()).thenReturn(getClass().getResourceAsStream(REAL_PACKAGE));
    when(asset.name()).thenReturn(REAL_PACKAGE_PATH);
    when(assetBlob.getBlob())
        .thenReturn(blob);
    doReturn(assetBlob)
        .when(storageTx).setBlob(any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        anyBoolean());
    when(storageTx.findComponents(any(), any()))
        .thenReturn(list);
    underTest.doPutArchive(REAL_PACKAGE_PATH, tempBlob, payload);
    verify(storageTx).saveAsset(asset);
  }
}
