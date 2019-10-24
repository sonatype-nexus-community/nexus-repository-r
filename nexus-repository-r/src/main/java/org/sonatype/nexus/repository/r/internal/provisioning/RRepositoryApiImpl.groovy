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
package org.sonatype.nexus.repository.r.internal.provisioning

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import org.sonatype.nexus.blobstore.api.BlobStoreManager
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.storage.WritePolicy
import org.sonatype.nexus.script.plugin.RepositoryApi

import groovy.transform.CompileStatic

/**
 * @since 1.1.0
 */
@Named
@Singleton
@CompileStatic
class RRepositoryApiImpl
    implements RRepositoryApi
{
  @Inject
  RepositoryApi repositoryApi;

  @Nonnull
  Repository createRProxy(final String name,
                          final String remoteUrl,
                          final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME,
                          final boolean strictContentTypeValidation = false)
  {
    def configuration = repositoryApi.
        createProxy(name, 'r-proxy', remoteUrl, blobStoreName, strictContentTypeValidation)
    repositoryApi.createRepository(configuration)
  }

  @Nonnull
  Repository createRHosted(final String name,
                           final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME,
                           final boolean strictContentTypeValidation = false,
                           final WritePolicy writePolicy = WritePolicy.ALLOW)
  {
    def configuration = repositoryApi.
        createHosted(name, 'r-hosted', blobStoreName, writePolicy, strictContentTypeValidation)
    repositoryApi.createRepository(configuration)
  }

  @Nonnull
  Repository createRGroup(final String name,
                          final List<String> members,
                          final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME)
  {
    def configuration = repositoryApi.createGroup(name, 'r-group', blobStoreName, members as String[])
    repositoryApi.createRepository(configuration)
  }
}
