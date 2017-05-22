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

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Named;

import org.sonatype.nexus.repository.cache.CacheController;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.proxy.ProxyFacetSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchMetadata;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;
import org.sonatype.nexus.transaction.UnitOfWork;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.r.internal.AssetKind.PACKAGES;
import static org.sonatype.nexus.repository.r.internal.AssetKind.ARCHIVE;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_PACKAGE;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_VERSION;
import static org.sonatype.nexus.repository.r.internal.RDescriptionUtils.extractDescriptionFromArchive;
import static org.sonatype.nexus.repository.r.internal.RFacetUtils.findAsset;
import static org.sonatype.nexus.repository.r.internal.RFacetUtils.findComponent;
import static org.sonatype.nexus.repository.r.internal.RFacetUtils.saveAsset;
import static org.sonatype.nexus.repository.r.internal.RFacetUtils.toContent;
import static org.sonatype.nexus.repository.r.internal.RPathUtils.filename;
import static org.sonatype.nexus.repository.r.internal.RPathUtils.matcherState;
import static org.sonatype.nexus.repository.r.internal.RPathUtils.packagesPath;
import static org.sonatype.nexus.repository.r.internal.RPathUtils.path;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;

/**
 * R {@link ProxyFacet} implementation.
 */
@Named
public class RProxyFacetImpl
    extends ProxyFacetSupport
{
  // HACK: Workaround for known CGLIB issue, forces an Import-Package for org.sonatype.nexus.repository.config
  @Override
  protected void doValidate(final Configuration configuration) throws Exception {
    super.doValidate(configuration);
  }

  @Nullable
  @Override
  protected Content getCachedContent(final Context context) throws IOException {
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    TokenMatcher.State matcherState = matcherState(context);
    switch (assetKind) {
      case PACKAGES:
        return getAsset(packagesPath(path(matcherState)));
      case ARCHIVE:
        return getAsset(path(path(matcherState), filename(matcherState)));
      default:
        throw new IllegalStateException();
    }
  }

  @Override
  protected Content store(final Context context, final Content content) throws IOException {
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    TokenMatcher.State matcherState = matcherState(context);
    switch (assetKind) {
      case PACKAGES:
        return putPackages(path(matcherState), content);
      case ARCHIVE:
        return putArchive(path(matcherState), filename(matcherState), content);
      default:
        throw new IllegalStateException();
    }
  }

  @Override
  protected void indicateVerified(final Context context, final Content content, final CacheInfo cacheInfo)
      throws IOException
  {
    setCacheInfo(content, cacheInfo);
  }

  @TransactionalTouchMetadata
  public void setCacheInfo(final Content content, final CacheInfo cacheInfo) throws IOException {
    StorageTx tx = UnitOfWork.currentTx();

    Asset asset = Content.findAsset(tx, tx.findBucket(getRepository()), content);
    if (asset == null) {
      log.debug(
          "Attempting to set cache info for non-existent R asset {}", content.getAttributes().require(Asset.class)
      );
      return;
    }

    log.debug("Updating cacheInfo of {} to {}", asset, cacheInfo);
    CacheInfo.applyToAsset(asset, cacheInfo);
    tx.saveAsset(asset);
  }

  private Content putArchive(final String path, final String filename, final Content content) throws IOException {
    StorageFacet storageFacet = facet(StorageFacet.class);
    try (TempBlob tempBlob = storageFacet.createTempBlob(content.openInputStream(), RFacetUtils.HASH_ALGORITHMS)) {
      return doPutArchive(path, filename, tempBlob, content);
    }
  }

  @TransactionalStoreBlob
  protected Content doPutArchive(final String path,
                                 final String filename,
                                 final TempBlob archiveContent,
                                 final Payload payload) throws IOException
  {
    StorageTx tx = UnitOfWork.currentTx();
    Bucket bucket = tx.findBucket(getRepository());

    String assetPath = path(path, filename);

    Map<String, String> attributes;
    try (InputStream is = archiveContent.get()) {
      attributes = extractDescriptionFromArchive(filename, is);
    }

    String name = attributes.get(P_PACKAGE);
    String version = attributes.get(P_VERSION);

    Component component = findComponent(tx, getRepository(), name, version);
    if (component == null) {
      component = tx.createComponent(bucket, getRepository().getFormat()).name(name).version(version);
    }
    tx.saveComponent(component);

    Asset asset = findAsset(tx, bucket, assetPath);
    if (asset == null) {
      asset = tx.createAsset(bucket, component);
      asset.name(assetPath);
      asset.formatAttributes().set(P_ASSET_KIND, ARCHIVE.name());
    }

    // TODO: Make this a bit more robust (could be problematic if keys are removed in later versions, or if keys clash)
    for (Entry<String, String> entry : attributes.entrySet()) {
      asset.formatAttributes().set(entry.getKey(), entry.getValue());
    }

    return saveAsset(tx, asset, archiveContent, payload);
  }

  private Content putPackages(final String path, final Content content) throws IOException {
    StorageFacet storageFacet = facet(StorageFacet.class);
    try (TempBlob tempBlob = storageFacet.createTempBlob(content.openInputStream(), RFacetUtils.HASH_ALGORITHMS)) {
      return doPutPackages(path, tempBlob, content);
    }
  }

  @TransactionalStoreBlob
  protected Content doPutPackages(final String path,
                                  final TempBlob packagesContent,
                                  final Payload payload) throws IOException
  {
    StorageTx tx = UnitOfWork.currentTx();
    Bucket bucket = tx.findBucket(getRepository());

    String assetPath = packagesPath(path);

    Asset asset = findAsset(tx, bucket, assetPath);
    if (asset == null) {
      asset = tx.createAsset(bucket, getRepository().getFormat());
      asset.name(assetPath);
      asset.formatAttributes().set(P_ASSET_KIND, PACKAGES.name());
    }
    return saveAsset(tx, asset, packagesContent, payload);
  }

  @TransactionalTouchBlob
  protected Content getAsset(final String name) {
    StorageTx tx = UnitOfWork.currentTx();

    Asset asset = findAsset(tx, tx.findBucket(getRepository()), name);
    if (asset == null) {
      return null;
    }
    if (asset.markAsDownloaded()) {
      tx.saveAsset(asset);
    }
    return toContent(asset, tx.requireBlob(asset.requireBlobRef()));
  }

  @Override
  protected String getUrl(@Nonnull final Context context) {
    return context.getRequest().getPath().substring(1);
  }

  @Override
  protected CacheController getCacheController(final Context context) {
    final AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    return checkNotNull(cacheControllerHolder.get(assetKind.getCacheType()));
  }
}
