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
package org.sonatype.nexus.repository.r;

import java.util.Map;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageTx;

/**
 * General R facet
 *
 * @since 1.1.next
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
  Component findOrCreateComponent(final StorageTx tx, final Map<String, String> attributes);

  /**
   * Find or Create Asset
   *
   * @return Asset
   */
  Asset findOrCreateAsset(final StorageTx tx, final Component component, final String path, final Map<String, String> attributes);

  /**
   * Find or Create Asset without Component
   *
   * @return Asset
   */
  Asset findOrCreateAsset(final StorageTx tx, final String path);
}
