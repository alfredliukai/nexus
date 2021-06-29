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
package org.sonatype.nexus.bootstrap.jetty;

import org.eclipse.jetty.http.HttpScheme;
import org.eclipse.jetty.server.HttpConfiguration;

/**
 * Connector configuration should be registered as service by plugin requesting dedicated connectors, and unregistered
 * once the connector is not needed.
 *
 * @since 3.0
 */
public interface ConnectorConfiguration
{
  /**
   * The required connector scheme.
   */
  HttpScheme getScheme();

  /**
   * The required connector port.
   */
  int getPort();

  /**
   * Allows implementation to customize the default configuration for needed connector or replace it completely.
   */
  HttpConfiguration customize(HttpConfiguration configuration);
}
