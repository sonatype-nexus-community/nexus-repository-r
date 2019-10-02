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

import java.util.Map;
import java.util.regex.Pattern;

import javax.inject.Named;

import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.r.RFacet;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageTx;

import static org.sonatype.nexus.repository.r.internal.AssetKind.ARCHIVE;
import static org.sonatype.nexus.repository.r.internal.AssetKind.PACKAGES;
import static org.sonatype.nexus.repository.r.internal.AssetKind.RDS_METADATA;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_PACKAGE;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_VERSION;
import static org.sonatype.nexus.repository.r.internal.RFacetUtils.findAsset;
import static org.sonatype.nexus.repository.r.internal.RFacetUtils.findComponent;
import static org.sonatype.nexus.repository.r.internal.RPathUtils.PATTERN_METADATA_RDS;
import static org.sonatype.nexus.repository.r.internal.RPathUtils.PATTERN_PACKAGES;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

/**
 * {@link RFacet} implementation.
 *
 * @since 1.0.next
 */
@Named
public class RFacetImpl
    extends FacetSupport
    implements RFacet
{
  @Override
  public Component findOrCreateComponent(final StorageTx tx,
                                         final Map<String, String> attributes)
  {
    String name = attributes.get(P_PACKAGE);
    String version = attributes.get(P_VERSION);

    Component component = findComponent(tx, getRepository(), name, version);
    if (component == null) {
      Bucket bucket = tx.findBucket(getRepository());
      component = tx.createComponent(bucket, getRepository().getFormat()).name(name).version(version);
      tx.saveComponent(component);
    }

    return component;
  }

  @Override
  public Asset findOrCreateAsset(final StorageTx tx,
                                 final Component component,
                                 final String path,
                                 final Map<String, String> attributes)
  {
    Bucket bucket = tx.findBucket(getRepository());
    Asset asset = tx.findAssetWithProperty(P_NAME, path, bucket);
    if (asset == null) {
      asset = tx.createAsset(bucket, component);
      asset.name(path);
      for (Map.Entry attribute : attributes.entrySet()) {
        asset.formatAttributes().set(attribute.getKey().toString(), attribute.getValue());
      }
      asset.formatAttributes().set(P_ASSET_KIND, getAssetKind(path));
      tx.saveAsset(asset);
    }

    return asset;
  }

  @Override
  public Asset findOrCreateMetadata(final StorageTx tx, final String path) {
    Bucket bucket = tx.findBucket(getRepository());
    Asset asset = findAsset(tx, bucket, path);
    if (asset == null) {
      asset = tx.createAsset(bucket, getRepository().getFormat());
      asset.name(path);
      asset.formatAttributes().set(P_ASSET_KIND, getAssetKind(path));
      tx.saveAsset(asset);
    }

    return asset;
  }

  private String getAssetKind(final String path) {
    String assetKind = ARCHIVE.name();
    if (Pattern.compile(PATTERN_PACKAGES).matcher(path).matches()) {
      assetKind = PACKAGES.name();
    }
    else if (Pattern.compile(PATTERN_METADATA_RDS).matcher(path).matches()) {
      assetKind = RDS_METADATA.name();
    }

    return assetKind;
  }
}
