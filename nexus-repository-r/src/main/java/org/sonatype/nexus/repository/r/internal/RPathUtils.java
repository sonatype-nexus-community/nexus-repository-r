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

import java.util.regex.Pattern;

import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;

import org.apache.commons.lang3.StringUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.r.internal.AssetKind.ARCHIVE;
import static org.sonatype.nexus.repository.r.internal.AssetKind.PACKAGES;
import static org.sonatype.nexus.repository.r.internal.AssetKind.RDS_METADATA;

/**
 * Utility methods for working with R routes and paths.
 */
public final class RPathUtils
{
  static final String PATTERN_PACKAGES = ".*/.+/PACKAGES.*";

  static final String PATTERN_METADATA_RDS = ".*/.+/.+.rds";

  static final String PATTERN_ARCHIVE = ".*/.+/.+.(.zip|.tgz|.tar.gz)";

  /**
   * Returns the {@link TokenMatcher.State} for the content.
   */
  public static TokenMatcher.State matcherState(final Context context) {
    return context.getAttributes().require(TokenMatcher.State.class);
  }

  /**
   * Returns the filename from a {@link TokenMatcher.State}.
   */
  public static String filename(final TokenMatcher.State state) {
    return match(state, "filename");
  }

  /**
   * Returns the name from a {@link TokenMatcher.State}.
   */
  public static String path(final TokenMatcher.State state) {
    return match(state, "path");
  }

  /**
   * Utility method encapsulating getting a particular token by name from a matcher, including preconditions.
   */
  private static String match(final TokenMatcher.State state, final String name) {
    checkNotNull(state);
    String result = state.getTokens().get(name);
    checkNotNull(result);
    return result;
  }

  /**
   * Builds a path to an archive for a particular path and filename.
   */
  public static String path(final String path, final String filename) {
    return StringUtils.appendIfMissing(path, "/") + filename;
  }

  /**
   * Builds a path to a package.gz for a particular path.
   */
  public static String packagesGzPath(final String path) {
    return path + "/PACKAGES.gz";
  }

  /**
   * Extracts full path from {@link Context}
   */
  public static String extractFullPath(final Context context) {
    return removeInitialSlashFromPath(context.getRequest().getPath());
  }

  /**
   * Removes slash if path starts with it
   */
  public static String removeInitialSlashFromPath(final String path) {
    return StringUtils.stripStart(path, "/");
  }

  /**
   * Extracts full path from {@link Context}
   */
  public static String cutFilenameFromPath(final String path) {
    final int pathEndCharIndex = StringUtils.stripEnd(path, "/").lastIndexOf('/');
    return path.substring(0, pathEndCharIndex);
  }

  /**
   * Determines asset kind by it's path
   */
  public static AssetKind getAssetKind(final String path) {
    AssetKind assetKind = ARCHIVE;
    if (Pattern.compile(PATTERN_PACKAGES).matcher(path).matches()) {
      assetKind = PACKAGES;
    }
    else if (Pattern.compile(PATTERN_METADATA_RDS).matcher(path).matches()) {
      assetKind = RDS_METADATA;
    }

    return assetKind;
  }

  /**
   * Determines if it's a valid path for archive
   */
  public static boolean isValidArchivePath(final String path) {
    return Pattern.compile(PATTERN_ARCHIVE).matcher(path).matches();
  }

  private RPathUtils() {
    // empty
  }
}
