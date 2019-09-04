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
package org.sonatype.nexus.plugins.r.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.goodies.httpfixture.server.api.Behaviour;
import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.routing.RoutingRule;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;
import org.sonatype.nexus.testsuite.testsupport.fixtures.RoutingRuleRule;

import org.junit.Rule;

/**
 * Support class for R Routing Rule ITs.
 *
 * @since 1.1.0
 */
public class RRoutingRuleITSupport
    extends RITSupport
{
  @Inject
  private RoutingRuleStore ruleStore;

  @Rule
  public RoutingRuleRule routingRules = new RoutingRuleRule(() -> ruleStore);

  protected EntityId createBlockedRoutingRule(final String name, final String matcher) {
    RoutingRule rule = routingRules.create(name, matcher);
    return EntityHelper.id(rule);
  }

  protected void attachRuleToRepository(final Repository repository, final EntityId routingRuleId) throws Exception {
    org.sonatype.nexus.repository.config.Configuration configuration = repository.getConfiguration();
    configuration.setRoutingRuleId(routingRuleId);
    repositoryManager.update(configuration);
  }

  protected static class BehaviourSpy
      implements Behaviour
  {
    private Behaviour delegate;

    List<String> requestUris = new ArrayList<>();

    BehaviourSpy(final Behaviour delegate) {
      this.delegate = delegate;
    }

    @Override
    public boolean execute(
        final HttpServletRequest request,
        final HttpServletResponse response,
        final Map<Object, Object> ctx) throws Exception
    {
      requestUris.add(request.getRequestURI() + '?' + request.getQueryString());
      return delegate.execute(request, response, ctx);
    }
  }
}
