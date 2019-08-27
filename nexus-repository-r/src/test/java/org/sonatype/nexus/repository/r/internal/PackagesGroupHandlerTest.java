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

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.goodies.testsupport.junit.TestDataRule;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.Status;
import org.sonatype.nexus.repository.view.ViewFacet;

import org.junit.Before;
import org.junit.Rule;
import org.mockito.Mock;

import static java.lang.System.getProperty;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PackagesGroupHandlerTest
    extends TestSupport
{
  @Mock
  Context context;

/*  @Mock
  GroupHandler.DispatchedRepositories dispatchedRepositories;*/

  @Mock
  Repository repository;

  @Mock
  Repository repository2;

  Path path = Paths.get(new File(getProperty("basedir", "")).getAbsolutePath(), "src/test/resources");

  @Rule
  public final TestDataRule testData = new TestDataRule(path.toFile());


  File packages;

  List<Repository> members;

  PackagesGroupHandler underTest;

  @Before
  public void setup() throws Exception {
    underTest = new PackagesGroupHandler();
    members = new ArrayList<>();
    packages = testData.resolveFile("org/sonatype/nexus/repository/r/internal/PACKAGES.gz");

    when(context.getRepository()).thenReturn(repository);

    setupRepository(repository);
    setupRepository(repository2);

    members.add(repository);
  }

  // Commented out until GroupHandler.DispactchedRepositories is made public
/*  @Test
  public void okWhenGetPackagesWithSingleResponse() throws Exception {
    Response response = underTest.doGet(context, dispatchedRepositories);
    assertThat(response.getStatus().getCode(), is(equalTo(200)));
  }

  @Test
  public void okWhenGetPackagesWithMultipleResponses() throws Exception {
    members.add(repository2);
    Response response = underTest.doGet(context, dispatchedRepositories);
    assertThat(response.getStatus().getCode(), is(equalTo(200)));
  }

  @Test
  public void notFoundWhenNoResponses() throws Exception {
    members.clear();
    Response response = underTest.doGet(context, dispatchedRepositories);
    assertThat(response.getStatus().getCode(), is(equalTo(404)));
  }*/

  private void setupRepository(final Repository repository) throws Exception {
    ViewFacet viewFacet = mock(ViewFacet.class);
    Response response = mock(Response.class);
    Payload payload = mock(Payload.class);
    GroupFacet groupFacet = mock(GroupFacet.class);
    when(groupFacet.members()).thenReturn(members);
    when(repository.facet(GroupFacet.class)).thenReturn(groupFacet);
    when(repository.facet(ViewFacet.class)).thenReturn(viewFacet);
    when(viewFacet.dispatch(any(), any())).thenReturn(response);
    when(response.getPayload()).thenReturn(payload);
    when(payload.openInputStream()).thenReturn(new FileInputStream(packages));
    when(response.getStatus()).thenReturn(new Status(true, 200));
  }
}
