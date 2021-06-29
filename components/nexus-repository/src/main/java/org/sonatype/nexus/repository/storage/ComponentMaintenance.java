/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.repository.storage;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Facet;

/**
 * Exposes manual component maintenance operations.
 *
 * @since 3.0
 */
@Facet.Exposed
public interface ComponentMaintenance
    extends Facet
{
  /**
   * Deletes a component from storage.
   */
  void deleteComponent(EntityId componentId);

  /**
   * Deletes a component and maybe the associated blobs.
   *
   * @param componentId entity id of the component to delete
   * @param deleteBlobs should blob deletion be requested
   *
   * @since 3.9
   */
  void deleteComponent(EntityId componentId, boolean deleteBlobs);

  /**
   * Deletes an asset from storage.
   */
  void deleteAsset(EntityId assetId);

  /**
   * Deletes an asset and maybe the associated blob.
   *
   * @param assetId entity id of the asset to delete
   * @param deleteBlob should blob deletion be requested
   *
   * @since 3.9
   */
  void deleteAsset(EntityId assetId, boolean deleteBlob);
}
