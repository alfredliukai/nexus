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
/*global Ext*/

/**
 * File descriptor warning controller, handles showing messages.
 *
 * @since 3.5
 */
Ext.define('NX.coreui.controller.FileDescriptorWarnings', {
  extend: 'NX.app.Controller',

  requires: [
    'NX.I18n',
    'NX.Permissions'
  ],
  refs: [
    {
      ref: 'fileDescriptorWarning',
      selector: '#nx-file-descriptor-warning'
    }
  ],

  /**
   * @override
   */
  init: function() {

    var me = this;

    me.listen({
      controller: {
        '#State': {
          changed: me.stateChanged
        }
      }
    });
  },

  stateChanged: function() {
    var me = this,
        warningPanel = me.getFileDescriptorWarning(),
        fileDescriptorLimitOk = NX.State.getValue('file_descriptor_limit', {})['limitOk'],
        fileDescriptorCount = NX.State.getValue('file_descriptor_limit', {})['count'],
        fileDescriptorRecommended = NX.State.getValue('file_descriptor_limit', {})['recommended'],
        user = NX.State.getUser();

    if (warningPanel) {
      if (user && user.administrator && !fileDescriptorLimitOk) {
        warningPanel.setTitle(NX.I18n.render(me, 'File_Descriptor_Warning', fileDescriptorCount, fileDescriptorRecommended));
        warningPanel.show();
      }
      else {
        warningPanel.hide();
      }
    }
  }
});
