package org.sonatype.nexus.plugins.r.internal;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.goodies.httpfixture.server.api.Behaviour;
import org.sonatype.goodies.httpfixture.server.fluent.Behaviours;
import org.sonatype.goodies.httpfixture.server.fluent.Server;
import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.pax.exam.NexusPaxExamSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.routing.RoutingRule;
import org.sonatype.nexus.repository.routing.RoutingRuleHandler;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;
import org.sonatype.nexus.testsuite.testsupport.NexusITSupport;
import org.sonatype.nexus.testsuite.testsupport.fixtures.RoutingRuleRule;
import org.sonatype.nexus.testsuite.testsupport.raw.RawClient;

import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class RRoutingRuleIT
    extends RITSupport
{
  private static Server proxyServer;

  @Inject
  private RoutingRuleStore ruleStore;

  @Rule
  public RoutingRuleRule routingRules = new RoutingRuleRule(() -> ruleStore);

  private Map<String, BehaviourSpy> serverPaths = new HashMap<>();

  @Configuration
  public static Option[] configureNexus() {
    return NexusPaxExamSupport.options(
        NexusITSupport.configureNexusBase(),
        nexusFeature("org.sonatype.nexus.plugins", "nexus-repository-r")
    );
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
    String allowedPackagePath = "github.com/allowed/example/@v/agricolae_1.0-1.tar.gz";
    String blockedZipPath = "github.com/blocked/example/@v/agricolae_1.0-1.tar.gz";

    configure(allowedPackagePath, packageFileName);
    configure(blockedZipPath, packageFileName);

    List<String> allowedPaths = singletonList(allowedPackagePath);
    List<String> blockedPaths = singletonList(blockedZipPath);

    EntityId routingRuleId = createRoutingRule("r-rule", ".*/blocked/.*");
    Repository proxyRepo =
        repos.createRProxy("test-r-blocking-proxy", proxyServer.getUrl().toString());
    RawClient client = rawClient(proxyRepo);

    configureRule(proxyRepo, routingRuleId);

    testProxy(proxyRepo, blockedPaths, allowedPaths, client);
  }

  private EntityId createRoutingRule(final String name, final String matcher) {
    RoutingRule rule = routingRules.create(name, matcher);
    return EntityHelper.id(rule);
  }

  private void configureRule(final Repository repository, final EntityId routingRuleId) throws Exception {
    org.sonatype.nexus.repository.config.Configuration configuration = repository.getConfiguration();
    configuration.setRoutingRuleId(routingRuleId);
    repositoryManager.update(configuration);
  }

  private void testProxy(
      final Repository proxyRepo,
      final List<String> blockedPaths,
      final List<String> allowedPaths, final RawClient client)
  {
    blockedPaths.stream().forEach(blockedPath -> {
      assertGetBlocked(client, proxyRepo, blockedPath);
      assertNoRequests(proxyRepo, blockedPath);
    });
    allowedPaths.stream()
        .forEach(allowedPath -> assertGetResponseStatus(client, proxyRepo, allowedPath, 200));
  }

  private void assertGetBlocked(final RawClient client, final Repository repository, final String path) {
    assertGetResponseStatus(client, repository, path, 403);
  }

  private void assertGetResponseStatus(
      final RawClient client,
      final Repository repository,
      final String path,
      final int responseCode)
  {
    try (CloseableHttpResponse response = client.get(path)) {
      StatusLine statusLine = response.getStatusLine();
      assertThat("Repository:" + repository.getName() + " Path:" + path, statusLine.getStatusCode(), is(responseCode));
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void assertNoRequests(final Repository proxy, final String reqPath) {
    URI uri = proxy.facet(ProxyFacet.class).getRemoteUrl().resolve(reqPath);
    BehaviourSpy spy = serverPaths.get(uri.getPath());
    assertNotNull("Missing spy for " + reqPath, spy);
    assertFalse("Unexpected request: " + uri.toString(),
        spy.requestUris.stream().anyMatch(path -> uri.toString().endsWith(path)));
  }

  private void configure(final String path, final String fileName) {
    File file = resolveTestFile(fileName);
    configureBehaviour('/' + path, Behaviours.file(file));
  }

  private void configureBehaviour(final String path, final Behaviour behaviour) {
    BehaviourSpy spy = new BehaviourSpy(behaviour);
    proxyServer.serve(path).withBehaviours(spy);
    serverPaths.put(path, spy);
  }

  private static class BehaviourSpy
      implements Behaviour
  {
    List<String> requestUris = new ArrayList<>();

    Behaviour delegate;

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
