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

import java.util.HashMap;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher.State;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HostedHandlersTest
    extends TestSupport
{
  public static final String PATH_VALUE = "Path-value";

  public static final String FILENAME_VALUE = "Filename-value";

  @Mock
  Context context;

  @Mock
  Repository repository;

  @Mock
  RHostedFacet rHostedFacet;

  @Mock
  Content content;

  @Mock
  Request request;

  @Mock
  Payload payload;

  AttributesMap attributesMap;

  State state;

  HashMap<String, String> tokens;

  HostedHandlers underTest;

  @Before
  public void setup() throws Exception {
    underTest = new HostedHandlers();
    initialiseTestFixtures();
    initialiseMockBehaviour();
    setupForGetPackagesTest();
    setupForGetArchiveTest();
  }

  @Test
  public void okWhenPackagesFound() throws Exception {
    assertStatus(underTest.getPackages, 200);
  }

  @Test
  public void notFoundWhenPackagesNotFound() throws Exception {
    when(rHostedFacet.getPackages(anyString())).thenReturn(null);
    assertStatus(underTest.getPackages, 404);
  }

  @Test(expected = IllegalStateException.class)
  public void validateTokenMatcherStateWhenGetPackages() throws Exception {
    passAttributesWithoutMatcherTo(underTest.getPackages);
  }

  @Test
  public void okWhenArchiveFound() throws Exception {
    assertStatus(underTest.getArchive, 200);
  }

  @Test
  public void notFoundWhenArchiveNotFound() throws Exception {
    when(rHostedFacet.getArchive(anyString())).thenReturn(null);
    assertStatus(underTest.getArchive, 404);
  }

  @Test(expected = IllegalStateException.class)
  public void validateTokenMatcherStateWhenGetArchive() throws Exception {
    passAttributesWithoutMatcherTo(underTest.getArchive);
  }

  @Test
  public void okWhenPut() throws Exception {
    assertStatus(underTest.putArchive, 200);
  }

  @Test
  public void repositoryUploadWhenPut() throws Exception {
    underTest.putArchive.handle(context);
    verify(rHostedFacet).upload(PATH_VALUE + "/" + FILENAME_VALUE, payload);
  }

  @Test(expected = IllegalStateException.class)
  public void validateTokenMatcherStateWhenPut() throws Exception {
    passAttributesWithoutMatcherTo(underTest.putArchive);
  }

  private void assertStatus(final Handler handler, final int status) throws Exception {
    Response response = handler.handle(context);
    assertThat(response.getStatus().getCode(), is(equalTo(status)));
  }

  private void passAttributesWithoutMatcherTo(final Handler handler) throws Exception {
    when(context.getAttributes()).thenReturn(new AttributesMap());
    handler.handle(context);
  }

  private void initialiseTestFixtures() {
    attributesMap = new AttributesMap();
    tokens = new HashMap<>();
    state = new TestState(tokens);
    attributesMap.set(State.class, state);
  }

  private void initialiseMockBehaviour() {
    when(context.getAttributes()).thenReturn(attributesMap);
    when(context.getRepository()).thenReturn(repository);
    when(context.getRequest()).thenReturn(request);
    when(request.getPayload()).thenReturn(payload);
    when(repository.facet(RHostedFacet.class)).thenReturn(rHostedFacet);
  }

  private void setupForGetPackagesTest() {
    tokens.put("path", PATH_VALUE);
    when(rHostedFacet.getPackages(anyString())).thenReturn(content);
  }

  private void setupForGetArchiveTest() {
    tokens.put("filename", FILENAME_VALUE);
    when(rHostedFacet.getArchive(anyString())).thenReturn(content);
  }

  class TestState
      implements State
  {

    private final Map<String, String> tokens;

    public TestState(final Map<String, String> tokens) {
      this.tokens = tokens;
    }

    @Override
    public String pattern() {
      return null;
    }

    @Override
    public Map<String, String> getTokens() {
      return tokens;
    }
  }
}
