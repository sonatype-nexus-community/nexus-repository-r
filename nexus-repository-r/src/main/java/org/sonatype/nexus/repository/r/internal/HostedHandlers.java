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

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher.State;

import static org.sonatype.nexus.repository.r.internal.RPathUtils.filename;
import static org.sonatype.nexus.repository.r.internal.RPathUtils.packagesPath;
import static org.sonatype.nexus.repository.r.internal.RPathUtils.path;

/**
 * R hosted handlers.
 */
@Named
@Singleton
public final class HostedHandlers
    extends ComponentSupport
{
  /**
   * Handle request for packages.
   */
  final Handler getPackages = context -> {
    State state = context.getAttributes().require(TokenMatcher.State.class);
    String path = packagesPath(path(state));
    Content content = context.getRepository().facet(RHostedFacet.class).getPackages(path);
    if (content != null) {
      return HttpResponses.ok(content);
    }
    return HttpResponses.notFound();
  };

  /**
   * Handle request for archive.
   */
  final Handler getArchive = context -> {
    State state = context.getAttributes().require(TokenMatcher.State.class);
    String path = path(path(state), filename(state));
    Content content = context.getRepository().facet(RHostedFacet.class).getArchive(path);
    if (content != null) {
      return HttpResponses.ok(content);
    }
    return HttpResponses.notFound();
  };

  /**
   * Handle request for upload.
   */
  final Handler putArchive = context -> {
    State state = context.getAttributes().require(TokenMatcher.State.class);
    String path = path(path(state), filename(state));
    context.getRepository().facet(RHostedFacet.class).upload(path, context.getRequest().getPayload());
    return HttpResponses.ok();
  };
}
