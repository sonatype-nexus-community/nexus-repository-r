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

import java.io.InputStream;
import java.nio.file.Files;

import org.sonatype.goodies.httpfixture.server.fluent.Server;
import org.sonatype.nexus.pax.exam.NexusPaxExamSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.security.subject.FakeAlmightySubject;
import org.sonatype.nexus.testsuite.testsupport.NexusITSupport;

import org.apache.http.HttpResponse;
import org.apache.shiro.util.ThreadContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.sonatype.goodies.httpfixture.server.fluent.Behaviours.error;
import static org.sonatype.goodies.httpfixture.server.fluent.Behaviours.file;
import static org.sonatype.nexus.repository.http.HttpStatus.NOT_FOUND;
import static org.sonatype.nexus.repository.http.HttpStatus.OK;
import static org.sonatype.nexus.testsuite.testsupport.FormatClientSupport.status;

public class RGroupIT
    extends RITSupport
{
  private RClient hostedClient;

  private RClient groupClient;

  private Server remote;

  private Repository repoGroup;

  private Repository repoProxy;

  private Repository repoHosted;

  @Configuration
  public static Option[] configureNexus() {
    return NexusPaxExamSupport.options(
        NexusITSupport.configureNexusBase(),
        nexusFeature("org.sonatype.nexus.plugins", "nexus-repository-r")
    );
  }

  @Before
  public void setUp() throws Exception {
    ThreadContext.bind(FakeAlmightySubject.forUserId("disabled-security"));
    remote = Server.withPort(0)
        .serve("/*").withBehaviours(error(NOT_FOUND))
        .serve("/" + AGRICOLAE_PATH_FULL_121_TARGZ)
        .withBehaviours(file(testData.resolveFile(AGRICOLAE_PKG_FILE_NAME_121_TARGZ)))
        .start();

    repoProxy = repos.createRProxy(testName.getMethodName() + "-proxy", remote.getUrl().toExternalForm());
    repoHosted = repos.createRHosted(testName.getMethodName() + "-hosted");
    repoGroup = repos.createRGroup(testName.getMethodName() + "-group", repoHosted.getName(), repoProxy.getName());

    hostedClient = createRClient(repoHosted);
    groupClient = createRClient(repoGroup);

    assertThat(status(hostedClient.putAndClose(AGRICOLAE_PATH_FULL_131_TARGZ,
        fileToHttpEntity(AGRICOLAE_PKG_FILE_NAME_131_TARGZ))), is(OK));
  }

  @After
  public void tearDown() throws Exception {
    remote.stop();
  }

  @Test
  public void whenRequestUnknownR_ShouldReturnError() throws Exception {
    assertThat(status(groupClient.fetch(DOES_NOT_EXIST_PKG_TGZ)), is(NOT_FOUND));
  }

  @Test
  public void whenRequestValidRPackageFromProxy_ShouldReturnSuccess() throws Exception {
    final HttpResponse response = groupClient.fetch(AGRICOLAE_PATH_FULL_121_TARGZ);
    assertSuccessResponseMatches(response, AGRICOLAE_PKG_FILE_NAME_121_TARGZ);
  }

  @Test
  public void whenRequestValidRPackageFromHosted_ShouldReturnSuccess() throws Exception {
    final HttpResponse response = groupClient.fetch(AGRICOLAE_PATH_FULL_131_TARGZ);
    assertSuccessResponseMatches(response, AGRICOLAE_PKG_FILE_NAME_131_TARGZ);
  }

  @Test
  public void whenRequestMetadataFromGroup_ShouldReturnSuccess() throws Exception {
    final String agricolae131Content =
        new String(Files.readAllBytes(testData.resolveFile(PACKAGES_AGRICOLAE_131_NAME).toPath()));

    final InputStream content = groupClient.fetch(PACKAGES_GZ_PATH_FULL).getEntity().getContent();
    verifyTextGzipContent(is(equalTo(agricolae131Content)), content);
  }
}
