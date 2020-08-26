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
package org.sonatype.nexus.repository.r.internal.hosted;

import java.util.Objects;

import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.WritePolicy;
import org.sonatype.nexus.repository.storage.WritePolicySelector;

import static org.sonatype.nexus.repository.r.internal.AssetKind.PACKAGES;
import static org.sonatype.nexus.repository.r.internal.AssetKind.RDS_METADATA;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;
import static org.sonatype.nexus.repository.storage.WritePolicy.ALLOW;
import static org.sonatype.nexus.repository.storage.WritePolicy.ALLOW_ONCE;

public class RWritePolicySelector implements WritePolicySelector
{
  @Override
  public WritePolicy select(final Asset asset, final WritePolicy configured) {
    if (ALLOW_ONCE == configured) {
      final String assetKind = asset.formatAttributes().get(P_ASSET_KIND, String.class);
      if (Objects.equals(PACKAGES.name(), assetKind) ||
          Objects.equals(RDS_METADATA.name(), assetKind)) {
        return ALLOW;
      }
    }
    return configured;
  }
}
