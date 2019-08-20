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

import org.sonatype.nexus.repository.Facet;
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
   * Retrieve packages.
   *
   * @param packagesPath the full packages path
   * @return simple package HTML
   */
  Content getPackages(String packagesPath);

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
  void upload(String path, Payload payload) throws IOException;
}
