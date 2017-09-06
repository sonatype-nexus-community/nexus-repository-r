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

import org.sonatype.nexus.repository.Facet;

/**
 * Facet implementing behavior for generating R PACKAGES metadata (not the actual R packages themselves). A typical
 * implementation will listen for changes to a repo and then rebuild the associated metadata.
 *
 * TODO: Update this to support other PACKAGES files (not just PACKAGES.gz) when this work is done across the project.
 */
@Facet.Exposed
public interface RPackagesBuilderFacet
{
  /**
   * Invalidates the metadata for this particular repository's PACKAGES.gz file at the specified path.
   *
   * @param basePath The base path of the PACKAGES.gz file to invalidate.
   */
  void invalidateMetadata(String basePath);
}
