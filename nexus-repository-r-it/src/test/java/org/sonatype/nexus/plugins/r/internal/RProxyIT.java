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

import org.sonatype.goodies.httpfixture.server.fluent.Server;
import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.pax.exam.NexusPaxExamSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.testsuite.testsupport.NexusITSupport;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.sonatype.goodies.httpfixture.server.fluent.Behaviours.error;
import static org.sonatype.goodies.httpfixture.server.fluent.Behaviours.file;

public class RProxyIT
    extends RITSupport
{
  private RClient client;

  private Repository repository;

  private Server server;

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
    server = Server.withPort(0)
        .serve("/*").withBehaviours(error(200))
        .serve("/" + AGRICOLAE_PATH_FULL).withBehaviours(file(testData.resolveFile(AGRICOLAE_PKG_FILE_NAME_131_TGZ)))
        .serve("/" + PACKAGES_PATH_FULL).withBehaviours(file(testData.resolveFile(PACKAGES_FILE_NAME)))
        .start();
    repository = repos.createRProxy("r-proxy-test", server.getUrl().toExternalForm());
    client = rClient(repository);
  }

  @After
  public void tearDown() throws Exception {
    server.stop();
  }

  @Test
  public void fetchTgzPackageFile() throws Exception {
    assertSuccessResponseMatches(client.fetch(AGRICOLAE_PATH_FULL), AGRICOLAE_PKG_FILE_NAME_131_TGZ);
    final Asset asset = findAsset(repository, AGRICOLAE_PATH_FULL);
    Assert.assertThat(asset.name(), is(equalTo(AGRICOLAE_PATH_FULL)));
    Assert.assertThat(asset.contentType(), is(equalTo(CONTENT_TYPE_TGZ)));
    Assert.assertThat(asset.format(), is(equalTo(R_FORMAT_NAME)));
  }

  @Test
  public void fetchMetaData() throws Exception {
    assertSuccessResponseMatches(client.fetch(PACKAGES_PATH_FULL), PACKAGES_FILE_NAME);
    final Asset asset = findAsset(repository, PACKAGES_PATH_FULL);
    Assert.assertThat(asset.contentType(), is(equalTo(CONTENT_TYPE_GZIP)));
    Assert.assertThat(asset.format(), is(equalTo(R_FORMAT_NAME)));
  }

  @Test
  public void checkComponentCreated() throws Exception {
    final Component nullComponent = findComponent(repository, AGRICOLAE_PKG_NAME);
    Assert.assertThat(nullComponent, is(nullValue()));
    client.fetch(AGRICOLAE_PATH_FULL);

    final Component component = findComponent(repository, AGRICOLAE_PKG_NAME);
    Assert.assertThat(component.name(), is(equalTo(AGRICOLAE_PKG_NAME)));
    Assert.assertThat(component.format(), is(equalTo(R_FORMAT_NAME)));
    Assert.assertThat(component.group(), is(nullValue()));
    Assert.assertThat(component.version(), is(equalTo(AGRICOLAE_PKG_VERSION_131)));
  }

  @Test
  public void shouldCacheMetadata() throws Exception {
    client.fetch(PACKAGES_PATH_FULL);
    server.stop();
    assertSuccessResponseMatches(client.fetch(PACKAGES_PATH_FULL), PACKAGES_FILE_NAME);
  }

  @Test
  public void shouldCacheTgzPackageFile() throws Exception {
    client.fetch(AGRICOLAE_PATH_FULL);
    server.stop();
    assertSuccessResponseMatches(client.fetch(AGRICOLAE_PATH_FULL), AGRICOLAE_PKG_FILE_NAME_131_TGZ);
  }
}
