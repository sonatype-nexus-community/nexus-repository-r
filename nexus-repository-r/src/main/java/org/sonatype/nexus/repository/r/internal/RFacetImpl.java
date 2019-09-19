package org.sonatype.nexus.repository.r.internal;

import java.util.Map;

import javax.inject.Named;

import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.r.RFacet;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;

import static org.sonatype.nexus.repository.r.internal.AssetKind.ARCHIVE;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_PACKAGE;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_VERSION;
import static org.sonatype.nexus.repository.r.internal.RFacetUtils.findAsset;
import static org.sonatype.nexus.repository.r.internal.RFacetUtils.findComponent;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;

/**
 * {@link RFacet} implementation.
 */
@Named
public class RFacetImpl
    extends FacetSupport
    implements RFacet
{
  @Override
  @TransactionalStoreBlob
  public Component findOrCreateComponent(final StorageTx tx,
                                         final Bucket bucket,
                                         final Map<String, String> attributes)
  {
    String name = attributes.get(P_PACKAGE);
    String version = attributes.get(P_VERSION);

    Component component = findComponent(tx, getRepository(), name, version);
    if (component == null) {
      component = tx.createComponent(bucket, getRepository().getFormat()).name(name).version(version);
      tx.saveComponent(component);
    }

    return component;
  }

  @Override
  @TransactionalStoreBlob
  public Asset findOrCreateAsset(final StorageTx tx, final Bucket bucket, final Component component, String path) {
    Asset asset = findAsset(tx, bucket, path);
    if (asset == null) {
      asset = tx.createAsset(bucket, component);
      asset.name(path);
      asset.formatAttributes().set(P_ASSET_KIND, ARCHIVE.name());
      tx.saveAsset(asset);
    }

    return asset;
  }
}
