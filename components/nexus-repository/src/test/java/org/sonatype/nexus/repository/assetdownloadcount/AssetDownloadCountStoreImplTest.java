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
package org.sonatype.nexus.repository.assetdownloadcount;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.orient.freeze.DatabaseFreezeChangeEvent;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;
import org.sonatype.nexus.repository.assetdownloadcount.internal.AssetDownloadCountEntityAdapter;
import org.sonatype.nexus.repository.assetdownloadcount.internal.AssetDownloadCountStoreImpl;
import org.sonatype.nexus.repository.assetdownloadcount.internal.AssetDownloadHistoricDataCleaner;
import org.sonatype.nexus.repository.assetdownloadcount.internal.CacheRemovalListener;

import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.SubjectRunnable;
import org.apache.shiro.util.ThreadContext;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AssetDownloadCountStoreImplTest
    extends TestSupport
{
  private static final long[] DAILY_COUNTS = new long[] { 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L, 14L,
      15L, 16L, 17L, 18L, 19L, 20L, 21L, 22L, 23L, 24L, 25L, 26L, 27L, 28L, 29L, 30L };

  private static final long[] MONTHLY_COUNTS = new long[] { 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L,
      14L };

  private static final String REPO_NAME = "reponame";

  private static final String ASSET_NAME = "assetname";

  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory("test");

  @Mock
  private AssetDownloadCountEntityAdapter assetDownloadCountEntityAdapter;

  @Mock
  private AssetDownloadHistoricDataCleaner dataCleaner;

  @Mock
  private Subject subject;

  private CacheRemovalListener cacheRemovalListener;

  private AssetDownloadCountStoreImpl underTest;

  @Before
  public void setUp() throws Exception {
    when(subject.getPrincipal()).thenReturn("admin");
    when(subject.associateWith(any(Runnable.class)))
        .thenAnswer(invoc -> new SubjectRunnable(subject, (Runnable) invoc.getArguments()[0]));
    ThreadContext.bind(subject);

    cacheRemovalListener = new CacheRemovalListener(database.getInstanceProvider(), assetDownloadCountEntityAdapter);
    underTest = new AssetDownloadCountStoreImpl(database.getInstanceProvider(), true, 10, 10,
        assetDownloadCountEntityAdapter, dataCleaner, cacheRemovalListener);
    underTest.start();
  }

  @After
  public void cleanup() {
    ThreadContext.unbindSubject();
  }

  @Test
  public void testGetDailyCount() {
    DateTime date = DateTime.now();
    when(assetDownloadCountEntityAdapter.getCount(any(), eq(REPO_NAME), eq(ASSET_NAME), eq(DateType.DAY), eq(date)))
        .thenReturn(100L);
    assertThat(underTest.getDailyCount(REPO_NAME, ASSET_NAME, date), is(100L));
  }

  @Test
  public void testGetMonthlyCount() {
    DateTime date = DateTime.now();
    when(assetDownloadCountEntityAdapter.getCount(any(), eq(REPO_NAME), eq(ASSET_NAME), eq(DateType.MONTH), eq(date)))
        .thenReturn(100L);
    assertThat(underTest.getMonthlyCount(REPO_NAME, ASSET_NAME, date), is(100L));
  }

  @Test
  public void testGetDailyCounts() {
    DateTime date = DateTime.now();
    when(assetDownloadCountEntityAdapter.getCounts(any(), eq(REPO_NAME), eq(ASSET_NAME), eq(DateType.DAY)))
        .thenReturn(DAILY_COUNTS);
    assertThat(underTest.getDailyCounts(REPO_NAME, ASSET_NAME), is(DAILY_COUNTS));
  }

  @Test
  public void testGetMonthlyCounts() {
    DateTime date = DateTime.now();
    when(assetDownloadCountEntityAdapter.getCounts(any(), eq(REPO_NAME), eq(ASSET_NAME), eq(DateType.MONTH)))
        .thenReturn(MONTHLY_COUNTS);
    assertThat(underTest.getMonthlyCounts(REPO_NAME, ASSET_NAME), is(MONTHLY_COUNTS));
  }

  @Test
  public void testIncrementCount() throws Exception {
    underTest = new AssetDownloadCountStoreImpl(database.getInstanceProvider(), true, 1, 1,
        assetDownloadCountEntityAdapter, dataCleaner, cacheRemovalListener);
    underTest.incrementCount(REPO_NAME, ASSET_NAME);
    underTest.incrementCount(REPO_NAME, ASSET_NAME + 1);
    Thread.sleep(100);
    verify(assetDownloadCountEntityAdapter).incrementCount(any(), eq(REPO_NAME), eq(ASSET_NAME), eq(1L));
  }

  @Test
  public void testIncrementCount_validateCaching() throws Exception {
    underTest = new AssetDownloadCountStoreImpl(database.getInstanceProvider(), true, 10, 1,
        assetDownloadCountEntityAdapter, dataCleaner, cacheRemovalListener);

    //increment 10 items, that will fill the queue, but not overflow it
    for (int i = 0 ; i < 10 ; i++) {
      underTest.incrementCount(REPO_NAME, ASSET_NAME + i);
    }

    Thread.sleep(100);

    //should be nothing on disk yet
    verify(assetDownloadCountEntityAdapter, never()).incrementCount(any(), anyString(), anyString(), anyLong());

    //increment 4 more times
    for (int i = 0 ; i < 4 ; i++) {
      underTest.incrementCount(REPO_NAME, ASSET_NAME + (10 + i));
    }

    Thread.sleep(100);

    //should have stuffed 4 things in db
    for (int i = 0 ; i < 4 ; i++) {
      verify(assetDownloadCountEntityAdapter).incrementCount(any(), eq(REPO_NAME), eq(ASSET_NAME + i), eq(1L));
    }
  }

  @Test
  public void testIncrementCount_validateCacheDrops_dbFrozen() throws Exception {
    underTest = new AssetDownloadCountStoreImpl(database.getInstanceProvider(), true, 1, 1,
        assetDownloadCountEntityAdapter, dataCleaner, cacheRemovalListener);

    cacheRemovalListener.onDatabaseFreezeChangeEvent(new DatabaseFreezeChangeEvent(true));

    underTest.incrementCount(REPO_NAME, ASSET_NAME + "a");
    underTest.incrementCount(REPO_NAME, ASSET_NAME + "b");

    Thread.sleep(100);

    // should be nothing on disk
    verify(assetDownloadCountEntityAdapter, never()).incrementCount(any(), anyString(), anyString(), anyLong());
  }

  @Test
  public void testSetMonthlyVulnerableCount() {
    DateTime date = DateTime.now();
    underTest.setMonthlyVulnerableCount(REPO_NAME, date, 50L);
    verify(assetDownloadCountEntityAdapter)
        .setCount(any(), eq(REPO_NAME), eq(DateType.MONTH_WHOLE_REPO_VULNERABLE), eq(date), eq(50L));
  }

  @Test
  public void testSetMonthlyCount() {
    DateTime date = DateTime.now();
    underTest.setMonthlyCount(REPO_NAME, date, 50L);
    verify(assetDownloadCountEntityAdapter)
        .setCount(any(), eq(REPO_NAME), eq(DateType.MONTH_WHOLE_REPO), eq(date), eq(50L));
  }

  @Test
  public void testGetMonthlyVulnerableCountsAllTime() {
    when(assetDownloadCountEntityAdapter.getCounts(any(), eq(REPO_NAME), eq(DateType.MONTH_WHOLE_REPO_VULNERABLE)))
        .thenReturn(MONTHLY_COUNTS);
    assertThat(underTest.getMonthlyVulnerableCounts(REPO_NAME), is(MONTHLY_COUNTS));
  }

  @Test
  public void testGetMonthlyCountsAllTime() {
    when(assetDownloadCountEntityAdapter.getCounts(any(), eq(REPO_NAME), eq(DateType.MONTH_WHOLE_REPO)))
        .thenReturn(MONTHLY_COUNTS);
    assertThat(underTest.getMonthlyCounts(REPO_NAME), is(MONTHLY_COUNTS));
  }

  @Test
  public void testGetLastThirtyDays() {
    when(assetDownloadCountEntityAdapter.getCounts(any(), eq(REPO_NAME), eq(ASSET_NAME), eq(DateType.DAY))).thenReturn(
        new long[] { 100L, 0L });

    long count = underTest.getLastThirtyDays(REPO_NAME, ASSET_NAME);
    assertThat(count, is(100L));

    underTest.incrementCount(REPO_NAME, ASSET_NAME);

    count = underTest.getLastThirtyDays(REPO_NAME, ASSET_NAME);
    assertThat(count, is(101L));
  }
}
