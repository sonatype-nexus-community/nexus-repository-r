package org.sonatype.nexus.repository.r;

import java.util.Map;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageTx;

/**
 * General R facet
 */
@Facet.Exposed
public interface RFacet
    extends Facet
{
  /**
   * Find or Create Component
   *
   * @return Component
   */
  Component findOrCreateComponent(final StorageTx tx, final Bucket bucket, final Map<String, String> attributes);

  /**
   * Find or Create Asset
   *
   * @return CAsset
   */
  Asset findOrCreateAsset(final StorageTx tx, final Bucket bucket, final Component component, String path);
}
