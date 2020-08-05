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
package org.sonatype.nexus.repository.r.rest;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Path;

import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.r.api.RGroupRepositoryApiRequest;
import org.sonatype.nexus.repository.rest.GroupRepositoryApiRequestToConfigurationConverter;
import org.sonatype.nexus.repository.rest.api.AuthorizingRepositoryManager;
import org.sonatype.nexus.repository.rest.api.RepositoriesApiResourceBeta;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import io.swagger.annotations.Api;

/**
 * @since 1.1.17
 * @deprecated - prefer to use {@link RGroupRepositoriesApiResourceV1 } instead of Beta.
 */
@Named
@Singleton
@Path(RepositoriesApiResourceBeta.RESOURCE_URI + "/r/group")
@Deprecated
@Api(hidden = true)
public class RGroupRepositoriesApiResourceBeta
    extends RGroupRepositoriesApiResource
{
  @Inject
  public RGroupRepositoriesApiResourceBeta(final AuthorizingRepositoryManager authorizingRepositoryManager,
                                           final GroupRepositoryApiRequestToConfigurationConverter<RGroupRepositoryApiRequest> configurationAdapter,
                                           final ConstraintViolationFactory constraintViolationFactory,
                                           final RepositoryManager repositoryManager)
  {
    super(authorizingRepositoryManager, configurationAdapter, constraintViolationFactory, repositoryManager);
  }
}
