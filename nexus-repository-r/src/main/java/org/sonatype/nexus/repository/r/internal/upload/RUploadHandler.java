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
package org.sonatype.nexus.repository.r.internal.upload;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.r.RHostedFacet;
import org.sonatype.nexus.repository.r.internal.RFormat;
import org.sonatype.nexus.repository.rest.UploadDefinitionExtension;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.upload.AssetUpload;
import org.sonatype.nexus.repository.upload.ComponentUpload;
import org.sonatype.nexus.repository.upload.UploadDefinition;
import org.sonatype.nexus.repository.upload.UploadFieldDefinition;
import org.sonatype.nexus.repository.upload.UploadFieldDefinition.Type;
import org.sonatype.nexus.repository.upload.UploadHandlerSupport;
import org.sonatype.nexus.repository.upload.UploadResponse;
import org.sonatype.nexus.repository.view.PartPayload;
import org.sonatype.nexus.rest.ValidationErrorsException;
import org.sonatype.nexus.transaction.UnitOfWork;

import static org.sonatype.nexus.repository.r.internal.RPathUtils.isValidArchivePath;
import static org.sonatype.nexus.repository.r.internal.RPathUtils.path;
import static org.sonatype.nexus.repository.r.internal.RPathUtils.removeInitialSlashFromPath;

/**
 * @since 1.1.0
 */
@Singleton
@Named(RFormat.NAME)
public class RUploadHandler
    extends UploadHandlerSupport
{
  private final VariableResolverAdapter variableResolverAdapter;

  private final ContentPermissionChecker contentPermissionChecker;

  private UploadDefinition definition;

  final static String PATH_ID = "pathId";

  final static String PACKAGE_PATH_DISPLAY = "Package Path";

  @Inject
  public RUploadHandler(@Named("simple") final VariableResolverAdapter variableResolverAdapter,
                        final ContentPermissionChecker contentPermissionChecker,
                        final Set<UploadDefinitionExtension> uploadDefinitionExtensions)
  {
    super(uploadDefinitionExtensions);
    this.variableResolverAdapter = variableResolverAdapter;
    this.contentPermissionChecker = contentPermissionChecker;
  }

  @Override
  public UploadResponse handle(final Repository repository, final ComponentUpload upload) throws IOException {
    final AssetUpload assetUpload = upload.getAssetUploads().get(0);
    final PartPayload payload = assetUpload.getPayload();
    final Map<String, String> fields = assetUpload.getFields();
    final String uploadPath = removeInitialSlashFromPath(fields.get(PATH_ID));
    final String assetPath = path(uploadPath, payload.getName());

    ensurePermitted(repository.getName(), RFormat.NAME, assetPath, Collections.emptyMap());
    validateUploadRequest(assetPath);

    try {
      UnitOfWork.begin(repository.facet(StorageFacet.class).txSupplier());
      Asset asset = repository.facet(RHostedFacet.class).upload(assetPath, payload);
      return new UploadResponse(asset);
    }
    finally {
      UnitOfWork.end();
    }
  }

  @Override
  public UploadDefinition getDefinition() {
    if (definition == null) {
      List<UploadFieldDefinition> assetField = Arrays.asList(
          new UploadFieldDefinition(PATH_ID, PACKAGE_PATH_DISPLAY, null, false, Type.STRING, null));
      definition = getDefinition(RFormat.NAME, false, Collections.emptyList(), assetField, null);
    }
    return definition;
  }

  @Override
  public VariableResolverAdapter getVariableResolverAdapter() {
    return variableResolverAdapter;
  }

  @Override
  public ContentPermissionChecker contentPermissionChecker() {
    return contentPermissionChecker;
  }

  private void validateUploadRequest(final String path) {
    if (!isValidArchivePath(path)) {
      throw new ValidationErrorsException("Extension not zip, tar.gz or tgz, or wrong upload path.");
    }
  }

}
