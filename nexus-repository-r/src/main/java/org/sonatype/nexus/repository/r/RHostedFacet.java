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

import java.io.IOException;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;

/**
 * Persistence for R hosted.
 */
@Facet.Exposed
public interface RHostedFacet
    extends Facet
{
  /**
   * Retrieve PACKAGES.gz.
   *
   * @param packagesGzPath the full PACKAGES.gz path
   * @return simple package HTML
   */
  Content getPackagesGz(String packagesGzPath);

  /**
   * Store a PACKAGES.gz file at a particular path.
   *
   * @param packagesGzPath the upload path
   * @param content        the temp blob containing the PACKAGES.gz content
   */
  void putPackagesGz(String packagesGzPath, TempBlob content) throws IOException;

  /**
   * Retrieve package.
   *
   * @param archivePath the full archive path
   * @return the package content
   */
  Content getArchive(String archivePath);

  /**
   * Perform upload.
   *
   * @param path    the upload path
   * @param payload uploaded file content
   */
  Asset upload(String path, Payload payload) throws IOException;

  /**
   * Build metadata for path.
   *
   * @param basePath the path to build the metadata for
   * @return the metadata as a {@code TempBlob}
   */
  TempBlob buildPackagesGz(String basePath) throws IOException;
}
