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
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchMetadata;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;
import org.sonatype.nexus.transaction.UnitOfWork;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.compress.compressors.CompressorStreamFactory.GZIP;
import static org.sonatype.nexus.repository.r.internal.AssetKind.ARCHIVE;
import static org.sonatype.nexus.repository.r.internal.AssetKind.PACKAGES;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_DEPENDS;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_IMPORTS;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_LICENSE;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_NEEDS_COMPILATION;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_PACKAGE;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_SUGGESTS;
import static org.sonatype.nexus.repository.r.internal.RAttributes.P_VERSION;
import static org.sonatype.nexus.repository.r.internal.RDescriptionUtils.extractDescriptionFromArchive;
import static org.sonatype.nexus.repository.r.internal.RFacetUtils.findAsset;
import static org.sonatype.nexus.repository.r.internal.RFacetUtils.findComponent;
import static org.sonatype.nexus.repository.r.internal.RFacetUtils.saveAsset;
import static org.sonatype.nexus.repository.r.internal.RFacetUtils.toContent;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;

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
  public Content getPackages(final String packagesPath) {
    checkNotNull(packagesPath);
    StorageTx tx = UnitOfWork.currentTx();

    Asset asset = findAsset(tx, tx.findBucket(getRepository()), packagesPath);
    if (asset == null) {
      return null;
    }
    if (asset.markAsDownloaded()) {
      tx.saveAsset(asset);
    }
    return toContent(asset, tx.requireBlob(asset.requireBlobRef()));
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
  public void upload(final String path, final Payload payload) throws IOException {
    checkNotNull(path);
    checkNotNull(payload);
    StorageFacet storageFacet = facet(StorageFacet.class);
    try (TempBlob tempBlob = storageFacet.createTempBlob(payload, RFacetUtils.HASH_ALGORITHMS)) {
      doPutArchive(path, tempBlob, payload);
    }
  }

  @Override
  @TransactionalStoreBlob
  public void putPackages(final String path, final TempBlob content) throws IOException {
    checkNotNull(path);
    checkNotNull(content);

    StorageTx tx = UnitOfWork.currentTx();
    Bucket bucket = tx.findBucket(getRepository());

    Asset asset = findAsset(tx, bucket, path);
    if (asset == null) {
      asset = tx.createAsset(bucket, getRepository().getFormat());
      asset.name(path);
      asset.formatAttributes().set(P_ASSET_KIND, PACKAGES.name());
    }

    saveAsset(tx, asset, content, "", null);
  }

  @Override
  public TempBlob buildMetadata(final String path) throws IOException {
    checkNotNull(path);
    try {
      // TODO: Change this to use a temp file or other alternative mechanism (at least until the blob store functions
      // are expanded to allow us to create a blob by writing into an output stream as well).
      StorageTx tx = UnitOfWork.currentTx();
      RPackagesBuilder packagesBuilder = new RPackagesBuilder(path + "PACKAGES.gz");
      for (Asset asset : tx.browseAssets(tx.findBucket(getRepository()))) {
        packagesBuilder.append(asset);
      }
      CompressorStreamFactory compressorStreamFactory = new CompressorStreamFactory();
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      try (CompressorOutputStream cos = compressorStreamFactory.createCompressorOutputStream(GZIP, os)) {
        try (OutputStreamWriter writer = new OutputStreamWriter(cos, UTF_8)) {
          for (Entry<String, Map<String, String>> eachPackage : packagesBuilder.getPackageInformation().entrySet()) {
            Map<String, String> packageInfo = eachPackage.getValue();
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

  @TransactionalStoreBlob
  protected void doPutArchive(final String path,
                              final TempBlob archiveContent,
                              final Payload payload) throws IOException
  {
    checkNotNull(path);
    checkNotNull(archiveContent);
    checkNotNull(payload);
    StorageTx tx = UnitOfWork.currentTx();
    Bucket bucket = tx.findBucket(getRepository());

    Map<String, String> attributes;
    try (InputStream is = archiveContent.get()) {
      attributes = extractDescriptionFromArchive(path, is);
    }

    String name = attributes.get(P_PACKAGE);
    String version = attributes.get(P_VERSION);

    Component component = findComponent(tx, getRepository(), name, version);
    if (component == null) {
      component = tx.createComponent(bucket, getRepository().getFormat()).name(name).version(version);
    }
    tx.saveComponent(component);

    Asset asset = findAsset(tx, bucket, path);
    if (asset == null) {
      asset = tx.createAsset(bucket, component);
      asset.name(path);
      asset.formatAttributes().set(P_ASSET_KIND, ARCHIVE.name());
    }

    // TODO: Make this a bit more robust (could be problematic if keys are removed in later versions, or if keys clash)
    for (Entry<String, String> entry : attributes.entrySet()) {
      asset.formatAttributes().set(entry.getKey(), entry.getValue());
    }

    saveAsset(tx, asset, archiveContent, payload);
  }
}
