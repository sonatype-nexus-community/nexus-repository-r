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
package org.sonatype.nexus.repository.r.internal.provisioning;

import java.util.List;

import org.sonatype.nexus.common.script.ScriptApi;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.r.internal.RFormat;
import org.sonatype.nexus.repository.storage.WritePolicy;

/**
 * @since 1.1.0
 */
public interface RRepositoryApi
    extends ScriptApi
{
  default String getName() {
    return "repository_" + RFormat.NAME;
  }

  /**
   * Create an R proxy repository.
   *
   * @param name                        The name of the new Repository
   * @param remoteUrl                   The url of the external proxy for this Repository
   * @param blobStoreName               The BlobStore the Repository should use
   * @param strictContentTypeValidation Whether or not the Repository should enforce strict content types
   * @return the newly created Repository
   */
  Repository createRProxy(final String name,
                          final String remoteUrl,
                          final String blobStoreName,
                          final boolean strictContentTypeValidation);

  /**
   * Create an R hosted repository.
   *
   * @param name                        The name of the new Repository
   * @param blobStoreName               The BlobStore the Repository should use
   * @param strictContentTypeValidation Whether or not the Repository should enforce strict content types
   * @param writePolicy                 The {@link WritePolicy} for the Repository
   * @return the newly created Repository
   */
  Repository createRHosted(final String name,
                           final String blobStoreName,
                           final boolean strictContentTypeValidation,
                           final WritePolicy writePolicy);

  /**
   * Create an R group repository.
   *
   * @param name          The name of the new Repository
   * @param members       The names of the Repositories in the group
   * @param blobStoreName The BlobStore the Repository should use
   * @return the newly created Repository
   */
  Repository createRGroup(final String name,
                          final List<String> members,
                          final String blobStoreName);
}
