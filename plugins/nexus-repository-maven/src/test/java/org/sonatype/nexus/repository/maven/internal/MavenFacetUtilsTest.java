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
package org.sonatype.nexus.repository.maven.internal;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageTestUtil;


import org.junit.Test;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_BASE_VERSION;
import static org.sonatype.nexus.repository.maven.internal.MavenFacetUtils.COMPONENT_VERSION_COMPARATOR;
import static org.sonatype.nexus.repository.maven.internal.MavenFacetUtils.isRelease;
import static org.sonatype.nexus.repository.maven.internal.MavenFacetUtils.isSnapshot;
import static org.sonatype.nexus.repository.storage.StorageTestUtil.createBucket;
import static org.sonatype.nexus.repository.storage.StorageTestUtil.createComponent;

public class MavenFacetUtilsTest
{
  @Test
  public void testComponentVersionComparator_Release() {
    List<Component> sorted = Stream
        .of(createComponent("1.1", "1.1"), createComponent("1.2", "1.2"), createComponent("1.0", "1.0"))
        .sorted(COMPONENT_VERSION_COMPARATOR)
        .collect(toList());

    assertThat(sorted.get(0).version(), equalTo("1.0"));
    assertThat(sorted.get(1).version(), equalTo("1.1"));
    assertThat(sorted.get(2).version(), equalTo("1.2"));
  }

  @Test
  public void testComponentVersionComparator_Snapshot() {
    List<Component> sorted = Stream
        .of(createComponent("1.1-20170919.212404-2", "1.1"), createComponent("1.1-20170919.212405-3", "1.1"),
            createComponent("1.1-20170919.212403-1", "1.1"))
        .sorted(COMPONENT_VERSION_COMPARATOR)
        .collect(toList());

    assertThat(sorted.get(0).version(), equalTo("1.1-20170919.212403-1"));
    assertThat(sorted.get(1).version(), equalTo("1.1-20170919.212404-2"));
    assertThat(sorted.get(2).version(), equalTo("1.1-20170919.212405-3"));
  }

  @Test
  public void testIsSnapshot() {
    assertFalse(isSnapshot(createComponent("1.1", "1.1")));
    assertTrue(isSnapshot(createComponent("1.1-20170918.215642-1", "1.1-SNAPSHOT")));
  }

  @Test
  public void testIsRelease() {
    assertTrue(isRelease(createComponent("1.1", "1.1")));
    assertFalse(isRelease(createComponent("1.1-20170918.215642-1", "1.1-SNAPSHOT")));
  }

  private Component createComponent(final String version, final String baseVersion) {
    Bucket bucket = createBucket("repo");
    Component component = StorageTestUtil.createComponent(bucket, "a", "b", version);
    component.attributes().child(Maven2Format.NAME).set(P_BASE_VERSION, baseVersion);
    return component;
  }
}
