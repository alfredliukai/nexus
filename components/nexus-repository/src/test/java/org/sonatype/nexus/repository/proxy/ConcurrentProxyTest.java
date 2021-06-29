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
package org.sonatype.nexus.repository.proxy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.sonatype.goodies.common.Time;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.goodies.testsupport.concurrent.ConcurrentRunner;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.cache.CacheController;
import org.sonatype.nexus.repository.cache.CacheControllerHolder;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.proxy.Cooperation.CooperatingFuture;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Request;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.io.ByteStreams.toByteArray;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.http.HttpMethods.GET;

/**
 * Concurrent {@link ProxyFacetSupport} tests.
 */
public class ConcurrentProxyTest
    extends TestSupport
{

  private static final int NUM_CLIENTS = 100;

  private static final int NUM_PATHS = 10;

  private static final int DOWNLOAD_DELAY_MILLIS = 1000;

  private static final int RANDOM_DELAY_MILLIS = 500;

  private static final Time PASSIVE_TIMEOUT = Time.seconds(60);

  private static final Time ACTIVE_TIMEOUT = Time.millis((DOWNLOAD_DELAY_MILLIS + RANDOM_DELAY_MILLIS) * 2);

  private static final String META_PREFIX = "meta/";

  private static final String ASSET_PREFIX = "asset/";

  private static final byte[] META_CONTENT = "META".getBytes(UTF_8);

  private static final byte[] ASSET_CONTENT = "ASSET".getBytes(UTF_8);

  @Mock
  Repository repository;

  @Mock
  CacheController cacheController;

  @Mock
  CacheControllerHolder cacheControllerHolder;

  @Mock
  CacheInfo cacheInfo;

  @Mock
  AttributesMap attributesMap;

  @Mock
  Request metaRequest;

  @Mock
  Context metaContext;

  @Mock
  Content metaContent;

  @Mock
  Content assetContent;

  Random random = new Random();

  Multiset<String> upstreamRequests = ConcurrentHashMultiset.create();

  Map<String, Content> storage = new ConcurrentHashMap<>();

  AtomicInteger cooperationExceptionCount = new AtomicInteger();

  @Spy
  ProxyFacetSupport underTest = new ProxyFacetSupport()
  {
    @Nullable
    @Override
    protected Content getCachedContent(final Context context) {
      return storage.get(context.getRequest().getPath());
    }

    @Override
    protected Content store(final Context context, final Content content) {
      storage.put(context.getRequest().getPath(), content);
      return content;
    }

    @Override
    protected void indicateVerified(final Context context, final Content content, final CacheInfo cacheInfo) {
      // no-op
    }

    @Override
    protected String getUrl(@Nonnull final Context context) {
      String path = context.getRequest().getPath();
      if (context.equals(metaContext)) {
        return META_PREFIX + path;
      }
      if (path.contains("indirect")) {
        // simulate formats which load index files to find URLs
        try (InputStream in = get(metaContext).openInputStream()) {
          return ASSET_PREFIX + path; // pretend we used the index
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
      return ASSET_PREFIX + path;
    }

    @Override
    protected Content fetch(final String url, final Context context, final Content stale) throws IOException {
      upstreamRequests.add(url);

      // mimic I/O time taken to download content
      LockSupport.parkNanos(Time.millis(DOWNLOAD_DELAY_MILLIS + random.nextInt(RANDOM_DELAY_MILLIS)).toNanos());

      if (url.startsWith(META_PREFIX)) {
        return metaContent;
      }
      if (url.contains("broken")) {
        throw new IOException("oops");
      }
      if (url.startsWith(ASSET_PREFIX)) {
        return assetContent;
      }
      return null;
    }
  };

  @Before
  public void setUp() throws Exception {
    when(metaRequest.getPath()).thenReturn("index.json");
    when(metaContext.getRequest()).thenReturn(metaRequest);

    when(attributesMap.get(CacheInfo.class)).thenReturn(cacheInfo);

    when(metaContent.getAttributes()).thenReturn(attributesMap);
    when(assetContent.getAttributes()).thenReturn(attributesMap);

    when(metaContent.openInputStream()).thenAnswer(invocation -> new ByteArrayInputStream(META_CONTENT));
    when(assetContent.openInputStream()).thenAnswer(invocation -> new ByteArrayInputStream(ASSET_CONTENT));

    when(cacheController.isStale(cacheInfo)).thenReturn(false);
    when(cacheControllerHolder.getContentCacheController()).thenReturn(cacheController);

    underTest.cacheControllerHolder = cacheControllerHolder;
    underTest.attach(repository);
  }

  Request randomRequest(final String path) {
    return new Request.Builder().action(GET).path(path + random.nextInt(NUM_PATHS)).build();
  }

  void verifyValidGet() throws IOException {
    Content content = underTest.get(new Context(repository, randomRequest("some/valid/indirect/path-")));
    try (InputStream in = content.openInputStream()) {
      assertThat(toByteArray(in), is(ASSET_CONTENT));
    }
  }

  void verifyBrokenGet() {
    try {
      underTest.get(new Context(repository, randomRequest("some/broken/indirect/path-")));
      fail("Expected IOException");
    }
    catch (IOException e) {
      assertThat(e.getMessage(), is("oops"));
    }
  }

  void verifyThreadLimit(Request request) throws IOException {
    try {
      underTest.get(new Context(repository, request));
    }
    catch (CooperationException e) {
      cooperationExceptionCount.incrementAndGet();
    }
  }

  @Test
  public void noDownloadCooperation() throws Exception {
    int iterations = 2;

    underTest.configureCooperation(false, Time.seconds(0), Time.seconds(0), 0);

    ConcurrentRunner runner = new ConcurrentRunner(iterations, 60);
    runner.addTask(NUM_CLIENTS, this::verifyValidGet);
    runner.addTask(NUM_CLIENTS, this::verifyBrokenGet);
    runner.go();

    assertThat(runner.getRunInvocations(), is(runner.getTaskCount() * runner.getIterations()));

    // there will be a lot of upstream index requests, potentially up to 2 * NUM_CLIENTS
    assertThat(upstreamRequests.count("meta/index.json"), is(greaterThan(NUM_CLIENTS)));
  }

  @Test
  public void downloadCooperation() throws Exception {
    int iterations = 2;

    underTest.configureCooperation(true, PASSIVE_TIMEOUT, ACTIVE_TIMEOUT, 2 * NUM_CLIENTS);

    ConcurrentRunner runner = new ConcurrentRunner(iterations, 60);
    runner.addTask(NUM_CLIENTS, this::verifyValidGet);
    runner.addTask(NUM_CLIENTS, this::verifyBrokenGet);
    runner.go();

    assertThat(runner.getRunInvocations(), is(runner.getTaskCount() * runner.getIterations()));

    // there will only be one upstream index request
    assertThat(upstreamRequests.count("meta/index.json"), is(1));

    upstreamRequests.elementSet().forEach(element -> {
      if (element.contains("broken")) {
        // each broken request will go upstream 'iterations' times,
        // as result is not cached, but is re-used via cooperation
        assertThat(upstreamRequests.count(element), is(iterations));
      }
      else {
        // each valid request should only go upstream once
        assertThat(upstreamRequests.count(element), is(1));
      }
    });
  }

  @Test
  @Ignore // temporary while fix is in flux
  public void limitCooperatingThreads() throws Exception {
    int threadLimit = 10;

    underTest.configureCooperation(true, PASSIVE_TIMEOUT, ACTIVE_TIMEOUT, threadLimit);

    Request request = new Request.Builder().action(GET).path("some/fixed/path").build();

    ConcurrentRunner runner = new ConcurrentRunner(1, 60);
    runner.addTask(NUM_CLIENTS, () -> verifyThreadLimit(request));
    runner.go();

    assertThat(runner.getRunInvocations(), is(runner.getTaskCount() * runner.getIterations()));

    // only one request should have made it upstream
    assertThat(upstreamRequests.count(ASSET_PREFIX + "some/fixed/path"), is(1));

    // majority of requests should have been cancelled to maintain thread limit
    assertThat(cooperationExceptionCount.get(), is(NUM_CLIENTS - threadLimit));
  }

  @Test
  public void downloadTimeoutsAreStaggered() throws Exception {
    CooperatingFuture<String> cooperatingFuture = new CooperatingFuture<>("testKey");

    long[] downloadTimeMillis = new long[10];

    long expectedGap = 200;

    downloadTimeMillis[0] = System.currentTimeMillis(); // first download
    for (int i = 1; i < downloadTimeMillis.length; i++) {

      // random sleep representing some client-side work
      LockSupport.parkNanos(Time.millis(random.nextInt((int) expectedGap)).toNanos());

      // staggered sleep should bring us close to the expected gap
      LockSupport.parkNanos(cooperatingFuture.staggerTimeout(Time.millis(expectedGap)).toNanos());

      downloadTimeMillis[i] = System.currentTimeMillis(); // next download
    }

    for (int i = 1; i < downloadTimeMillis.length; i++) {
      long actualGap = downloadTimeMillis[i] - downloadTimeMillis[i - 1];
      assertThat(actualGap, allOf(greaterThanOrEqualTo(expectedGap - 10), lessThanOrEqualTo(expectedGap + 10)));
    }
  }

}
