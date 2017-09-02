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
import java.util.TreeMap;

import org.sonatype.nexus.repository.storage.Asset;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.unmodifiableMap;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_DEPENDS;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_IMPORTS;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_LICENSE;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_NEEDS_COMPILATION;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_PACKAGE;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_SUGGESTS;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_VERSION;

/**
 * Builds the contents of a PACKAGES file based on the provided assets, taking into account the greatest version of a
 * particular package that is available in a (hosted) repository.
 *
 * Note that this maintains all pertinent information for the "latest" version of each package in memory, though the
 * actual amount of information for each package is rather small.
 *
 * TODO: Ensure that we build this information and actually cache it, rather than doing this for each request.
 */
public class RPackagesBuilder
{
  /**
   * The greatest version of each package currently encountered during this run.
   */
  private final Map<String, RPackageVersion> packageVersions = new HashMap<>();

  /**
   * The package information as of the last time we processed the greatest version of this package. A {@code TreeMap} is
   * used to maintain ordering by package name.
   */
  private final Map<String, Map<String, String>> packageInformation = new TreeMap<>();

  /**
   * The path to the PACKAGES file that is being generated.
   */
  private final String packagesPath;

  /**
   * Constructor.
   *
   * @param packagesPath The path to the PACKAGES file that is being generated.
   */
  public RPackagesBuilder(final String packagesPath) {
    this.packagesPath = checkNotNull(packagesPath);
  }

  /**
   * Processes an asset, updating the greatest version and details for the package if appropriate.
   *
   * @param asset The asset to process.
   */
  public void append(final Asset asset) {
    String base = basePath(packagesPath);
    String packageName = asset.formatAttributes().get(P_PACKAGE, String.class);

    // is this asset at this particular path?
    if (base.equals(basePath(asset.name()))) {

      // have we seen a greater version of this already?
      RPackageVersion newVersion = new RPackageVersion(asset.formatAttributes().get(P_VERSION, String.class));
      RPackageVersion oldVersion = packageVersions.getOrDefault(packageName, newVersion);
      if (newVersion.compareTo(oldVersion) >= 0) {

        // if so, use the most recent information instead and update the greatest version encountered
        Map<String, String> newInformation = new HashMap<>();
        newInformation.put(P_PACKAGE, asset.formatAttributes().get(P_PACKAGE, String.class));
        newInformation.put(P_VERSION, asset.formatAttributes().get(P_VERSION, String.class));
        newInformation.put(P_DEPENDS, asset.formatAttributes().get(P_DEPENDS, String.class));
        newInformation.put(P_IMPORTS, asset.formatAttributes().get(P_IMPORTS, String.class));
        newInformation.put(P_SUGGESTS, asset.formatAttributes().get(P_SUGGESTS, String.class));
        newInformation.put(P_LICENSE, asset.formatAttributes().get(P_LICENSE, String.class));
        newInformation.put(P_NEEDS_COMPILATION, asset.formatAttributes().get(P_NEEDS_COMPILATION, String.class));

        packageVersions.put(packageName, newVersion);
        packageInformation.put(packageName, newInformation);
      }
    }
  }

  /**
   * Returns an unmodifiable map containing the package information to write to a PACKAGES file. The iteration order
   * of the map's keys is such that the package names will be returned in sorted order.
   *
   * @return The map of package information, keyed by package name.
   */
  public Map<String, Map<String, String>> getPackageInformation() {
    return unmodifiableMap(packageInformation);
  }

  /**
   * Returns a base path for a particular path (the path excluding the filename and last trailing slash).
   *
   * @param path The input path.
   * @return The base path.
   */
  private String basePath(final String path) {
    return path.substring(0, path.lastIndexOf('/'));
  }
}
