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
package org.sonatype.nexus.repository.r.internal

import javax.inject.Inject
import javax.inject.Provider

import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.RecipeSupport
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.attributes.AttributesFacet
import org.sonatype.nexus.repository.http.PartialFetchHandler
import org.sonatype.nexus.repository.httpclient.HttpClientFacet
import org.sonatype.nexus.repository.routing.RoutingRuleHandler
import org.sonatype.nexus.repository.search.SearchFacet
import org.sonatype.nexus.repository.security.SecurityHandler
import org.sonatype.nexus.repository.storage.StorageFacet
import org.sonatype.nexus.repository.storage.UnitOfWorkHandler
import org.sonatype.nexus.repository.view.ConfigurableViewFacet
import org.sonatype.nexus.repository.view.Context
import org.sonatype.nexus.repository.view.Route.Builder
import org.sonatype.nexus.repository.view.handlers.BrowseUnsupportedHandler
import org.sonatype.nexus.repository.view.handlers.ConditionalRequestHandler
import org.sonatype.nexus.repository.view.handlers.ContentHeadersHandler
import org.sonatype.nexus.repository.view.handlers.ExceptionHandler
import org.sonatype.nexus.repository.view.handlers.HandlerContributor
import org.sonatype.nexus.repository.view.handlers.TimingHandler
import org.sonatype.nexus.repository.view.matchers.ActionMatcher
import org.sonatype.nexus.repository.view.matchers.logic.LogicMatchers
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher

import static org.sonatype.nexus.repository.http.HttpMethods.GET
import static org.sonatype.nexus.repository.http.HttpMethods.HEAD
import static org.sonatype.nexus.repository.http.HttpMethods.PUT

/**
 * Support for R recipes.
 */
abstract class RRecipeSupport
    extends RecipeSupport
{
  @Inject
  Provider<RSecurityFacet> securityFacet

  @Inject
  Provider<ConfigurableViewFacet> viewFacet

  @Inject
  Provider<StorageFacet> storageFacet

  @Inject
  Provider<SearchFacet> searchFacet

  @Inject
  Provider<AttributesFacet> attributesFacet

  @Inject
  ExceptionHandler exceptionHandler

  @Inject
  TimingHandler timingHandler

  @Inject
  SecurityHandler securityHandler

  @Inject
  RoutingRuleHandler routingRuleHandler

  @Inject
  PartialFetchHandler partialFetchHandler

  @Inject
  ConditionalRequestHandler conditionalRequestHandler

  @Inject
  ContentHeadersHandler contentHeadersHandler

  @Inject
  UnitOfWorkHandler unitOfWorkHandler

  @Inject
  BrowseUnsupportedHandler browseUnsupportedHandler

  @Inject
  HandlerContributor handlerContributor

  @Inject
  Provider<RComponentMaintenance> componentMaintenanceFacet

  @Inject
  Provider<HttpClientFacet> httpClientFacet

  @Inject
  Provider<org.sonatype.nexus.repository.r.RRestoreFacet> rRestoreFacet;

  @Inject
  Provider<org.sonatype.nexus.repository.r.RFacet> rFacet;

  protected RRecipeSupport(final Type type, final Format format) {
    super(type, format)
  }

  Closure assetKindHandler = { Context context, AssetKind value ->
    context.attributes.set(AssetKind, value)
    return context.proceed()
  }

  /**
   * Matcher for packages.gz mapping.
   */
  static Builder packagesGzMatcher() {
    new Builder().matcher(
        LogicMatchers.and(
            new ActionMatcher(GET, HEAD),
            packagesGzTokenMatcher()
        ))
  }

  /**
   * Matcher for all packages mapping.
   */
  static Builder packagesMatcher() {
    new Builder().matcher(
        LogicMatchers.and(
            new ActionMatcher(GET, HEAD),
            packagesTokenMatcher()
        ))
  }

  /**
   * Matcher for all .rds metadata mapping.
   */
  static Builder metadataRdsMatcher() {
    new Builder().matcher(
        LogicMatchers.and(
            new ActionMatcher(GET, HEAD),
            metadataRdsTokenMatcher()
        ))
  }

  /**
   * Matcher for archive mapping.
   */
  static Builder archiveMatcher() {
    new Builder().matcher(
        LogicMatchers.and(
            new ActionMatcher(GET, HEAD),
            allFilesTokenMatcher()
        ))
  }

  /**
   * Matcher for upload mapping.
   */
  static Builder uploadMatcher() {
    new Builder().matcher(
        LogicMatchers.and(
            new ActionMatcher(PUT),
            allFilesTokenMatcher()
        ))
  }

  /**
   * Token matcher for PACKAGES.gz files.
   */
  static TokenMatcher packagesGzTokenMatcher() {
    return new TokenMatcher('/{path:.+}/PACKAGES.gz')
  }

  /**
   * Token matcher for all PACKAGES files.
   */
  static TokenMatcher packagesTokenMatcher() {
    return new TokenMatcher('/{path:.+}/PACKAGES{extension:.*}')
  }

  /**
   * Token matcher for .rds metadata files.
   */
  static TokenMatcher metadataRdsTokenMatcher() {
    return new TokenMatcher('/{path:.+}/{filename:.+}.rds')
  }

  /**
   * Token matcher for all files.
   */
  static TokenMatcher allFilesTokenMatcher() {
    return new TokenMatcher('/{path:.+}/{filename:.+}')
  }
}
