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

import javax.annotation.Nonnull;

import org.sonatype.nexus.repository.cache.CacheControllerHolder;
import org.sonatype.nexus.repository.cache.CacheControllerHolder.CacheType;

/**
 * Asset kinds for R.
 */
enum AssetKind
{
  PACKAGES(CacheControllerHolder.METADATA, true),
  RDS_METADATA(CacheControllerHolder.METADATA, true),
  ARCHIVE(CacheControllerHolder.CONTENT, false);

  private final CacheType cacheType;

  private final boolean isMetadata;

  AssetKind(final CacheType cacheType, final boolean isMetadata) {
    this.cacheType = cacheType;
    this.isMetadata = isMetadata;
  }

  @Nonnull
  public CacheType getCacheType() {
    return cacheType;
  }

  public boolean isMetadata() {
    return isMetadata;
  }
}
