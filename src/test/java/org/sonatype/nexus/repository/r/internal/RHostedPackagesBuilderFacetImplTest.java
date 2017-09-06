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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetCreatedEvent;
import org.sonatype.nexus.repository.storage.AssetDeletedEvent;
import org.sonatype.nexus.repository.storage.AssetUpdatedEvent;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class RHostedPackagesBuilderFacetImplTest
    extends TestSupport
{
  static final String REPOSITORY_NAME = "repository-name";

  static final String PACKAGES_GZ_PATH = "/some/path/PACKAGES.gz";

  static final String ASSET_PATH = "/some/path/asset.gz";

  static final String ASSET_BASE_PATH = "/some/path/";

  @Mock
  Repository repository;

  @Mock
  EventManager eventManager;

  @Mock
  AssetCreatedEvent assetCreatedEvent;

  @Mock
  AssetDeletedEvent assetDeletedEvent;

  @Mock
  AssetUpdatedEvent assetUpdatedEvent;

  @Mock
  Asset asset;

  RHostedPackagesBuilderFacetImpl underTest;

  @Before
  public void setUp() throws Exception {
    when(repository.getName()).thenReturn(REPOSITORY_NAME);

    underTest = new RHostedPackagesBuilderFacetImpl(eventManager, 1L);
    underTest.attach(repository);
  }

  @Test
  public void testAssetDeletedEventHandledCorrectly() {
    when(assetDeletedEvent.isLocal()).thenReturn(true);
    when(assetDeletedEvent.getRepositoryName()).thenReturn(REPOSITORY_NAME);
    when(assetDeletedEvent.getAsset()).thenReturn(asset);
    when(asset.name()).thenReturn(ASSET_PATH);

    underTest.on(assetDeletedEvent);

    ArgumentCaptor<RMetadataInvalidationEvent> eventCaptor = ArgumentCaptor.forClass(RMetadataInvalidationEvent.class);
    verify(eventManager).post(eventCaptor.capture());

    RMetadataInvalidationEvent event = eventCaptor.getValue();
    assertThat(event.getRepositoryName(), is(REPOSITORY_NAME));
    assertThat(event.getBasePath(), is(ASSET_BASE_PATH));
  }

  @Test
  public void testAssetDeletedEventIgnoredForDifferentNode() {
    when(assetDeletedEvent.isLocal()).thenReturn(false);
    when(assetDeletedEvent.getRepositoryName()).thenReturn(REPOSITORY_NAME);
    when(assetDeletedEvent.getAsset()).thenReturn(asset);
    when(asset.name()).thenReturn(ASSET_PATH);

    underTest.on(assetDeletedEvent);

    verifyNoMoreInteractions(eventManager);
  }

  @Test
  public void testAssetDeletedEventIgnoredForDifferentRepository() {
    when(assetDeletedEvent.isLocal()).thenReturn(true);
    when(assetDeletedEvent.getRepositoryName()).thenReturn("foo");
    when(assetDeletedEvent.getAsset()).thenReturn(asset);
    when(asset.name()).thenReturn(ASSET_PATH);

    underTest.on(assetDeletedEvent);

    verifyNoMoreInteractions(eventManager);
  }

  @Test
  public void testAssetDeletedEventIgnoredForPackagesGzAsset() {
    when(assetDeletedEvent.isLocal()).thenReturn(true);
    when(assetDeletedEvent.getRepositoryName()).thenReturn(REPOSITORY_NAME);
    when(assetDeletedEvent.getAsset()).thenReturn(asset);
    when(asset.name()).thenReturn(PACKAGES_GZ_PATH);

    underTest.on(assetDeletedEvent);

    verifyNoMoreInteractions(eventManager);
  }

  @Test
  public void testAssetCreatedEventHandledCorrectly() {
    when(assetCreatedEvent.isLocal()).thenReturn(true);
    when(assetCreatedEvent.getRepositoryName()).thenReturn(REPOSITORY_NAME);
    when(assetCreatedEvent.getAsset()).thenReturn(asset);
    when(asset.name()).thenReturn(ASSET_PATH);

    underTest.on(assetCreatedEvent);

    ArgumentCaptor<RMetadataInvalidationEvent> eventCaptor = ArgumentCaptor.forClass(RMetadataInvalidationEvent.class);
    verify(eventManager).post(eventCaptor.capture());

    RMetadataInvalidationEvent event = eventCaptor.getValue();
    assertThat(event.getRepositoryName(), is(REPOSITORY_NAME));
    assertThat(event.getBasePath(), is(ASSET_BASE_PATH));
  }

  @Test
  public void testAssetCreatedEventIgnoredForDifferentNode() {
    when(assetCreatedEvent.isLocal()).thenReturn(false);
    when(assetCreatedEvent.getRepositoryName()).thenReturn(REPOSITORY_NAME);
    when(assetCreatedEvent.getAsset()).thenReturn(asset);
    when(asset.name()).thenReturn(ASSET_PATH);

    underTest.on(assetCreatedEvent);

    verifyNoMoreInteractions(eventManager);
  }

  @Test
  public void testAssetCreatedEventIgnoredForDifferentRepository() {
    when(assetCreatedEvent.isLocal()).thenReturn(true);
    when(assetCreatedEvent.getRepositoryName()).thenReturn("foo");
    when(assetCreatedEvent.getAsset()).thenReturn(asset);
    when(asset.name()).thenReturn(ASSET_PATH);

    underTest.on(assetCreatedEvent);

    verifyNoMoreInteractions(eventManager);
  }

  @Test
  public void testAssetCreatedEventIgnoredForPackagesGzAsset() {
    when(assetCreatedEvent.isLocal()).thenReturn(true);
    when(assetCreatedEvent.getRepositoryName()).thenReturn(REPOSITORY_NAME);
    when(assetCreatedEvent.getAsset()).thenReturn(asset);
    when(asset.name()).thenReturn(PACKAGES_GZ_PATH);

    underTest.on(assetCreatedEvent);

    verifyNoMoreInteractions(eventManager);
  }

  @Test
  public void testAssetUpdatedEventHandledCorrectly() {
    when(assetUpdatedEvent.isLocal()).thenReturn(true);
    when(assetUpdatedEvent.getRepositoryName()).thenReturn(REPOSITORY_NAME);
    when(assetUpdatedEvent.getAsset()).thenReturn(asset);
    when(asset.name()).thenReturn(ASSET_PATH);

    underTest.on(assetUpdatedEvent);

    ArgumentCaptor<RMetadataInvalidationEvent> eventCaptor = ArgumentCaptor.forClass(RMetadataInvalidationEvent.class);
    verify(eventManager).post(eventCaptor.capture());

    RMetadataInvalidationEvent event = eventCaptor.getValue();
    assertThat(event.getRepositoryName(), is(REPOSITORY_NAME));
    assertThat(event.getBasePath(), is(ASSET_BASE_PATH));
  }

  @Test
  public void testAssetUpdatedEventIgnoredForDifferentNode() {
    when(assetUpdatedEvent.isLocal()).thenReturn(false);
    when(assetUpdatedEvent.getRepositoryName()).thenReturn(REPOSITORY_NAME);
    when(assetUpdatedEvent.getAsset()).thenReturn(asset);
    when(asset.name()).thenReturn(ASSET_PATH);

    underTest.on(assetUpdatedEvent);

    verifyNoMoreInteractions(eventManager);
  }

  @Test
  public void testAssetUpdatedEventIgnoredForDifferentRepository() {
    when(assetUpdatedEvent.isLocal()).thenReturn(true);
    when(assetUpdatedEvent.getRepositoryName()).thenReturn("foo");
    when(assetUpdatedEvent.getAsset()).thenReturn(asset);
    when(asset.name()).thenReturn(ASSET_PATH);

    underTest.on(assetUpdatedEvent);

    verifyNoMoreInteractions(eventManager);
  }

  @Test
  public void testAssetUpdatedEventIgnoredForPackagesGzAsset() {
    when(assetUpdatedEvent.isLocal()).thenReturn(true);
    when(assetUpdatedEvent.getRepositoryName()).thenReturn(REPOSITORY_NAME);
    when(assetUpdatedEvent.getAsset()).thenReturn(asset);
    when(asset.name()).thenReturn(PACKAGES_GZ_PATH);

    underTest.on(assetUpdatedEvent);

    verifyNoMoreInteractions(eventManager);
  }
}
