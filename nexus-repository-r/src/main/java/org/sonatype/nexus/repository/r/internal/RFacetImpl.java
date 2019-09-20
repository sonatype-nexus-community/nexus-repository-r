package org.sonatype.nexus.repository.r.internal;

import java.util.Map;

import javax.inject.Named;

import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.r.RFacet;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageTx;

import static org.sonatype.nexus.repository.r.internal.RAttributes.P_PACKAGE;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_VERSION;
import static org.sonatype.nexus.repository.r.internal.RFacetUtils.findAsset;
import static org.sonatype.nexus.repository.r.internal.RFacetUtils.findComponent;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

/**
 * {@link RFacet} implementation.
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
  public Asset findOrCreateAsset(final StorageTx tx, final Component component, final String path, final Map<String, String> attributes) {
    Bucket bucket = tx.findBucket(getRepository());
    Asset asset = tx.findAssetWithProperty(P_NAME, path, bucket);
    if (asset == null) {
      asset = tx.createAsset(bucket, component);
      asset.name(path);
      for (Map.Entry attribute : attributes.entrySet()) {
        asset.formatAttributes().set(attribute.getKey().toString(), attribute.getValue());
      }
      tx.saveAsset(asset);
    }

    return asset;
  }

  @Override
  public Asset findOrCreateMetadata(final StorageTx tx, final String path, final Map<String, String> attributes) {
    Bucket bucket = tx.findBucket(getRepository());
    Asset asset = findAsset(tx, bucket, path);
    if (asset == null) {
      asset = tx.createAsset(bucket, getRepository().getFormat());
      asset.name(path);
      for (Map.Entry attribute : attributes.entrySet()) {
        asset.formatAttributes().set(attribute.getKey().toString(), attribute.getValue());
      }
      tx.saveAsset(asset);
    }

    return asset;
  }
}
