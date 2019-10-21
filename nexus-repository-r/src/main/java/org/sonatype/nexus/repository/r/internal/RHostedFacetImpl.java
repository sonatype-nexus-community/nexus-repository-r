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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Enumeration;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Named;
import javax.mail.internet.InternetHeaders;

import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.r.RFacet;
import org.sonatype.nexus.repository.r.RHostedFacet;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchMetadata;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.transaction.UnitOfWork;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.compress.compressors.CompressorStreamFactory.GZIP;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_DEPENDS;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_IMPORTS;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_LICENSE;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_NEEDS_COMPILATION;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_PACKAGE;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_SUGGESTS;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_VERSION;
import static org.sonatype.nexus.repository.r.internal.RDescriptionUtils.extractDescriptionFromArchive;
import static org.sonatype.nexus.repository.r.internal.RFacetUtils.findAsset;
import static org.sonatype.nexus.repository.r.internal.RFacetUtils.saveAsset;
import static org.sonatype.nexus.repository.r.internal.RFacetUtils.toContent;

/**
 * {@link RHostedFacet} implementation.
 */
@Named
public class RHostedFacetImpl
    extends FacetSupport
    implements RHostedFacet
{
  @Override
  @TransactionalTouchMetadata
  public Content getPackagesGz(final String packagesGzPath) {
    checkNotNull(packagesGzPath);
    StorageTx tx = UnitOfWork.currentTx();

    Asset asset = findAsset(tx, tx.findBucket(getRepository()), packagesGzPath);
    if (asset == null) {
      return null;
    }
    if (asset.markAsDownloaded()) {
      tx.saveAsset(asset);
    }
    return toContent(asset, tx.requireBlob(asset.requireBlobRef()));
  }

  @Override
  @TransactionalStoreBlob
  public void putPackagesGz(final String path, final TempBlob content) throws IOException {
    checkNotNull(path);
    checkNotNull(content);

    StorageTx tx = UnitOfWork.currentTx();
    RFacet rFacet = facet(RFacet.class);

    Asset asset = rFacet.findOrCreateAsset(tx, path);

    saveAsset(tx, asset, content, "", null);
  }

  @Override
  @TransactionalTouchBlob
  public Content getArchive(final String archivePath) {
    checkNotNull(archivePath);
    StorageTx tx = UnitOfWork.currentTx();

    Asset asset = findAsset(tx, tx.findBucket(getRepository()), archivePath);
    if (asset == null) {
      return null;
    }
    if (asset.markAsDownloaded()) {
      tx.saveAsset(asset);
    }
    return toContent(asset, tx.requireBlob(asset.requireBlobRef()));
  }

  @Override
  public Asset upload(final String path, final Payload payload) throws IOException {
    checkNotNull(path);
    checkNotNull(payload);
    StorageFacet storageFacet = facet(StorageFacet.class);
    try (TempBlob tempBlob = storageFacet.createTempBlob(payload, RFacetUtils.HASH_ALGORITHMS)) {
      return doPutArchive(path, tempBlob, payload);
    }
  }

  @TransactionalStoreBlob
  protected Asset doPutArchive(final String path,
                               final TempBlob archiveContent,
                               final Payload payload) throws IOException
  {

    StorageTx tx = UnitOfWork.currentTx();
    RFacet rFacet = facet(RFacet.class);

    Map<String, String> attributes;
    try (InputStream is = archiveContent.get()) {
      attributes = extractDescriptionFromArchive(path, is);
    }

    Component component = rFacet.findOrCreateComponent(tx, path, attributes);
    Asset asset = rFacet.findOrCreateAsset(tx, component, path, attributes);
    saveAsset(tx, asset, archiveContent, payload);

    return asset;
  }

  @Override
  @TransactionalTouchMetadata
  public TempBlob buildPackagesGz(final String basePath) throws IOException {
    checkNotNull(basePath);
    try {
      StorageTx tx = UnitOfWork.currentTx();
      RPackagesBuilder packagesBuilder = new RPackagesBuilder(basePath);
      for (Asset asset : tx.browseAssets(tx.findBucket(getRepository()))) {
        packagesBuilder.append(asset);
      }
      CompressorStreamFactory compressorStreamFactory = new CompressorStreamFactory();
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      try (CompressorOutputStream cos = compressorStreamFactory.createCompressorOutputStream(GZIP, os)) {
        try (OutputStreamWriter writer = new OutputStreamWriter(cos, UTF_8)) {
          for (Entry<String, Map<String, String>> eachPackage : packagesBuilder.getPackageInformation().entrySet()) {
            writePackageInfo(writer, eachPackage.getValue());
          }
        }
      }
      StorageFacet storageFacet = getRepository().facet(StorageFacet.class);
      try (InputStream is = new ByteArrayInputStream(os.toByteArray())) {
        return storageFacet.createTempBlob(is, RFacetUtils.HASH_ALGORITHMS);
      }
    }
    catch (CompressorException e) {
      throw new IOException("Error compressing metadata", e);
    }
  }

  private void writePackageInfo(final OutputStreamWriter writer, final Map<String, String> packageInfo)
      throws IOException
  {
    InternetHeaders headers = new InternetHeaders();
    headers.addHeader(P_PACKAGE, packageInfo.get(P_PACKAGE));
    headers.addHeader(P_VERSION, packageInfo.get(P_VERSION));
    headers.addHeader(P_DEPENDS, packageInfo.get(P_DEPENDS));
    headers.addHeader(P_IMPORTS, packageInfo.get(P_IMPORTS));
    headers.addHeader(P_SUGGESTS, packageInfo.get(P_SUGGESTS));
    headers.addHeader(P_LICENSE, packageInfo.get(P_LICENSE));
    headers.addHeader(P_NEEDS_COMPILATION, packageInfo.get(P_NEEDS_COMPILATION));
    Enumeration<String> headerLines = headers.getAllHeaderLines();
    while (headerLines.hasMoreElements()) {
      String line = headerLines.nextElement();
      writer.write(line, 0, line.length());
      writer.write('\n');
    }
    writer.write('\n');
  }
}
