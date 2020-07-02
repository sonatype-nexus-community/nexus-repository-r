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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.sonatype.goodies.httpfixture.server.fluent.Behaviours;
import org.sonatype.goodies.httpfixture.server.fluent.Server;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Repository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.sonatype.nexus.plugins.r.internal.RITConfig.configureRBase;

/**
 * @since 1.1.0
 */
public class RRoutingRuleIT
    extends RRoutingRuleITSupport
{
  private static Server proxyServer;

  private Map<String, BehaviourSpy> serverPaths = new HashMap<>();

  @Configuration
  public static Option[] configureNexus() {
    return configureRBase();
  }

  @Before
  public void startup() throws Exception {
    proxyServer = Server.withPort(0).start();
  }

  @After
  public void shutdown() throws Exception {
    if (proxyServer != null) {
      proxyServer.stop();
    }
  }

  @Test
  public void testBlockedRoutingRule() throws Exception {
    String packageFileName = "agricolae_1.0-1.tar.gz";
    String allowedPackagePath = "bin/macosx/el-capitan/contrib/1.0/agricolae_1.0-1.tar.gz";
    String blockedPackagePath = "bin/macosx/el-capitan/contrib/1.1/agricolae_1.0-1.tar.gz";

    configureProxyBehaviour(allowedPackagePath, packageFileName);
    configureProxyBehaviour(blockedPackagePath, packageFileName);

    EntityId routingRuleId = createBlockedRoutingRule("r-blocking-rule", ".*/1.1/.*");
    Repository proxyRepo = repos.createRProxy("test-r-blocking-proxy", proxyServer.getUrl().toString());
    RClient client = rClient(proxyRepo);

    attachRuleToRepository(proxyRepo, routingRuleId);

    assertGetResponseStatus(client, proxyRepo, blockedPackagePath, 403);
    assertGetResponseStatus(client, proxyRepo, allowedPackagePath, 200);
    assertNoRequests(blockedPackagePath);
  }

  private void assertNoRequests(final String reqPath) {
    BehaviourSpy spy = serverPaths.get(reqPath);
    assertNotNull("Missing spy for " + reqPath, spy);
    assertFalse("Unexpected request: " + reqPath,
        spy.requestUris.stream().anyMatch(reqPath::endsWith));
  }

  private void configureProxyBehaviour(final String proxyPath, final String fileName) {
    File file = resolveTestFile(fileName);
    BehaviourSpy spy = new BehaviourSpy(Behaviours.file(file));
    proxyServer.serve("/" + proxyPath).withBehaviours(spy);
    serverPaths.put(proxyPath, spy);
  }
}
