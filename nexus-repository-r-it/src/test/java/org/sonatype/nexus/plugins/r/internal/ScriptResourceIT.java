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

import javax.ws.rs.client.Client;
import javax.ws.rs.core.UriBuilder;

import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.pax.exam.NexusPaxExamSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.rest.client.RestClientConfiguration;
import org.sonatype.nexus.script.ScriptClient;
import org.sonatype.nexus.script.ScriptXO;
import org.sonatype.nexus.testsuite.testsupport.NexusITSupport;

import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

import static java.lang.String.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ScriptResourceIT
    extends RITSupport
{
  private ScriptClient scriptClient;

  @Configuration
  public static Option[] configureNexus() {
    return NexusPaxExamSupport.options(
        NexusITSupport.configureNexusBase(),
        nexusFeature("org.sonatype.nexus.plugins", "nexus-repository-r")
    );
  }

  @Before
  public void setup() throws Exception {
    BaseUrlHolder.set(this.nexusUrl.toString());

    final CloseableHttpClient build = clientBuilder().build();
    Client client = restClientFactory.create(
        RestClientConfiguration.DEFAULTS.withHttpClient(() -> build));
    client.register(new BasicAuthentication("admin", "admin123"));
    scriptClient = restClientFactory.proxy(ScriptClient.class, client,
        UriBuilder.fromUri(nexusUrl.toURI()).path("service/rest").build());
  }

  @Test
  public void createRProxyScript() {
    String repoName = "r-proxy-repository";
    String scriptName = "r-proxy-script";
    String content = format("repository.createRProxy('%s','http://someurl')", repoName);

    scriptClient.add(new ScriptXO(scriptName, content, "groovy"));
    scriptClient.run(scriptName, "");
    Repository repo = repositoryManager.get(repoName);
    assertThat(repo.getFormat().getValue(), is("r"));
    assertThat(repo.getName(), is(repoName));
    assertThat(repo.getType().getValue(), is("proxy"));
  }

  @Test
  public void createRHostedScript() {
    String repoName = "r-hosted-repository";
    String scriptName = "r-hosted-script";
    String content = format("repository.createRHosted('%s')", repoName);

    scriptClient.add(new ScriptXO(scriptName, content, "groovy"));
    scriptClient.run(scriptName, "");
    Repository repo = repositoryManager.get(repoName);
    assertThat(repo.getFormat().getValue(), is("r"));
    assertThat(repo.getName(), is(repoName));
    assertThat(repo.getType().getValue(), is("hosted"));
  }

  @Test
  public void createRGroupScript() {
    String proxyName = "r-proxy-for-group";
    String hostedName = "r-hosted-for-group";
    String repoName = "r-script-group-repo";
    String scriptName = "r-group-script";
    String content =
        format("repository.createRGroup('%s', Arrays.asList('%s', '%s'))", repoName, proxyName, hostedName);

    repos.createRProxy(proxyName, "http://someurl");
    repos.createRHosted(hostedName);

    scriptClient.add(new ScriptXO(scriptName, content, "groovy"));
    scriptClient.run(scriptName, "");
    Repository repo = repositoryManager.get(repoName);
    assertThat(repo.getFormat().getValue(), is("r"));
    assertThat(repo.getName(), is(repoName));
    assertThat(repo.getType().getValue(), is("group"));
  }
}
