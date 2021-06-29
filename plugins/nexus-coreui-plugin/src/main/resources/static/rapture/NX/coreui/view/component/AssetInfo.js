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
/*global Ext, NX*/

/**
 * Asset info panel.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.component.AssetInfo', {
  extend: 'NX.view.info.Panel',
  alias: 'widget.nx-coreui-component-assetinfo',
  requires: [
    'NX.I18n',
    'NX.coreui.util.RepositoryUrls'
  ],

  /**
   * model to display
   */
  assetModel: null,

  /**
   * @public
   * @param {Object} assetModel the asset to display
   */
  setAssetModel: function (assetModel) {
    var me = this,
        info = {};
    me.assetModel = assetModel;

    // display common data
    var contentType = assetModel.get('contentType');
    var size = assetModel.get('size');
    info[NX.I18n.get('Assets_Info_Path')] = NX.coreui.util.RepositoryUrls.asRepositoryLink(
        assetModel, assetModel.get('format'));
    info[NX.I18n.get('Assets_Info_ContentType')] = contentType;
    info[NX.I18n.get('Assets_Info_FileSize')] = Ext.util.Format.fileSize(size);
    info[NX.I18n.get('Assets_Info_Blob_Created')] = assetModel.get('blobCreated');
    info[NX.I18n.get('Assets_Info_Blob_Updated')] = assetModel.get('blobUpdated');
    info[NX.I18n.get('Assets_Info_Downloaded_Count')] = assetModel.get('downloadCount') + ' '
            + NX.I18n.get('Assets_Info_Downloaded_Unit');
    info[NX.I18n.get('Assets_Info_Last_Downloaded')] = assetModel.get('lastDownloaded');
    info[NX.I18n.get('Assets_Info_Locally_Cached')] = contentType !== 'unknown' && size > 0;
    info[NX.I18n.get('Assets_Info_BlobRef')] = assetModel.get('blobRef');
    info[NX.I18n.get('Assets_Info_ContainingRepositoryName')] = assetModel.get('containingRepositoryName');

    info[NX.I18n.get('Assets_Info_UploadedBy')] = assetModel.get('createdBy');
    info[NX.I18n.get('Assets_Info_UploadedIp')] = assetModel.get('createdByIp');

    me.showInfo(info);
  }

});
