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

import java.io.IOException;
import java.io.UncheckedIOException;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.event.EventAware.Asynchronous;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.storage.AssetCreatedEvent;
import org.sonatype.nexus.repository.storage.AssetDeletedEvent;
import org.sonatype.nexus.repository.storage.AssetEvent;
import org.sonatype.nexus.repository.storage.AssetUpdatedEvent;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.repository.r.internal.AssetKind.ARCHIVE;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;

/**
 * Implementation of {@link RPackagesBuilderFacet} targeted for use with hosted repositories. Uses event dispatching to
 * ensure that we do not have race conditions when processing and rebuilding metadata, and also imposes a waiting period
 * to batch metadata updates.
 */
@Named
public class RHostedPackagesBuilderFacetImpl
    extends FacetSupport
    implements RPackagesBuilderFacet, Asynchronous
{
  /**
   * The name of the metadata file (PACKAGES.gz) that we generate on invalidation.
   */
  private static final String PACKAGES_GZ = "PACKAGES.gz";

  /**
   * The event manager to use when posting new events.
   */
  private final EventManager eventManager;

  /**
   * The interval in milliseconds to wait between rebuilds.
   */
  private final long interval;

  /**
   * The flag indicating if we are currently waiting to rebuild.
   */
  private boolean waiting;

  /**
   * Constructor.
   *
   * @param eventManager The event manager to use when posting new events.
   * @param interval The interval in milliseconds to wait between rebuilds.
   */
  @Inject
  public RHostedPackagesBuilderFacetImpl(final EventManager eventManager,
                                         @Named("${nexus.r.packagesBuilder.interval:-60000}") final long interval)
  {
    this.eventManager = checkNotNull(eventManager);
    this.interval = interval;
  }

  /**
   * Handles {@code AssetDeletedEvent} events concurrently, requesting a metadata invalidation and rebuild if warranted
   * by the event contents.
   *
   * @param deleted The deletion event to handle.
   */
  @Subscribe
  @Guarded(by = STARTED)
  @AllowConcurrentEvents
  public void on(final AssetDeletedEvent deleted) {
    if (shouldInvalidate(deleted)) {
      invalidateMetadata(basePath(deleted.getAsset().name()));
    }
  }

  /**
   * Handles {@code AssetCreatedEvent} events concurrently, requesting a metadata invalidation and rebuild if warranted
   * by the event contents.
   *
   * @param created The creation event to handle.
   */
  @Subscribe
  @Guarded(by = STARTED)
  @AllowConcurrentEvents
  public void on(final AssetCreatedEvent created) {
    if (shouldInvalidate(created)) {
      invalidateMetadata(basePath(created.getAsset().name()));
    }
  }

  /**
   * Handles {@code AssetUpdatedEvent} events concurrently, requesting a metadata invalidation and rebuild if warranted
   * by the event contents.
   *
   * @param updated The update event to handle.
   */
  @Subscribe
  @Guarded(by = STARTED)
  @AllowConcurrentEvents
  public void on(final AssetUpdatedEvent updated) {
    if (shouldInvalidate(updated)) {
      invalidateMetadata(basePath(updated.getAsset().name()));
    }
  }

  /**
   * Handles metadata invalidation for this repository given the specified base path. Internally this is handled by
   * posting a {@code RMetadataInvalidationEvent} to the event manager which then triggers the actual metadata
   * regeneration. Note that the regeneration may not be instantaneous (and that the waiting period is configurable).
   *
   * @param basePath The base path of the PACKAGES.gz file to invalidate.
   */
  @Override
  public void invalidateMetadata(final String basePath) {
    if (!waiting) {
      eventManager.post(new RMetadataInvalidationEvent(getRepository().getName(), basePath));
    }
  }

  /**
   * Listen for invalidation of the metadata, wait a configured time and then rebuild. The waiting allows throwing away
   * of subsequent events to reduce the number of rebuilds if multiple RPMs are being uploaded. This method must NOT be
   * allowed to process concurrent events as race conditions may result when rebuilding the data.
   */
  @Subscribe
  public void on(final RMetadataInvalidationEvent event) {
    if (getRepository().getName().equals(event.getRepositoryName())) {
      try {
        waiting = true;
        Thread.sleep(interval);
      }
      catch (InterruptedException e) {
        log.warn("R invalidation thread interrupted on repository {}, proceeding with invalidation",
            getRepository().getName());
      }
      waiting = false;
      log.info("Rebuilding R PACKAGES.gz metadata for repository {}", getRepository().getName());
      UnitOfWork.begin(getRepository().facet(StorageFacet.class).txSupplier());
      try {
        rebuildMetadata(event.getBasePath());
      }
      finally {
        UnitOfWork.end();
      }
    }
  }

  /**
   * Rebuilds the metadata within a particular transaction, overwriting the PACKAGES.gz content with the generated
   * metadata once completed.
   *
   * @param basePath The base path to rebuild the metadata for.
   */
  protected void rebuildMetadata(final String basePath) {
    RHostedFacet hostedFacet = getRepository().facet(RHostedFacet.class);
    try (TempBlob packagesContent = hostedFacet.buildMetadata(basePath)) {
      hostedFacet.putPackages(basePath + PACKAGES_GZ, packagesContent);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Returns whether or not an asset event should result in an invalidation request. In order for the event to mandate
   * that we rebuild metadata, it must both refer to this particular repository, and it also must refer to an uploaded
   * archive and not to one of the metadata files itself. This helps ensure that we don't end up responding to metadata
   * changes when rebuilding metadata and end up in a loop.
   *
   * @param assetEvent The asset event to process.
   * @return true if an archive, false if a packages file
   */
  private boolean shouldInvalidate(final AssetEvent assetEvent) {
    return assetEvent.isLocal() &&
        getRepository().getName().equals(assetEvent.getRepositoryName()) &&
        ARCHIVE.name().equals(assetEvent.getAsset().formatAttributes().get(P_ASSET_KIND, String.class));
  }

  /**
   * Returns a base path for a particular path (the path excluding the filename and last trailing slash).
   *
   * @param path The input path.
   * @return The base path.
   */
  private String basePath(final String path) {
    return path.substring(0, path.lastIndexOf('/') + 1);
  }
}
