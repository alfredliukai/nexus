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
package org.sonatype.nexus.blobstore.restore;

import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.api.BlobStoreUsageChecker;
import org.sonatype.nexus.blobstore.file.FileBlobAttributes;
import org.sonatype.nexus.blobstore.file.FileBlobStore;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.blobstore.restore.DefaultIntegrityCheckStrategy.DEFAULT_NAME;
import static org.sonatype.nexus.blobstore.restore.RestoreMetadataTaskDescriptor.BLOB_STORE_NAME_FIELD_ID;
import static org.sonatype.nexus.blobstore.restore.RestoreMetadataTaskDescriptor.INTEGRITY_CHECK;
import static org.sonatype.nexus.blobstore.restore.RestoreMetadataTaskDescriptor.RESTORE_BLOBS;
import static org.sonatype.nexus.blobstore.restore.RestoreMetadataTaskDescriptor.TYPE_ID;
import static org.sonatype.nexus.blobstore.restore.RestoreMetadataTaskDescriptor.UNDELETE_BLOBS;

public class RestoreMetadataTaskTest
    extends TestSupport
{
  RestoreMetadataTask underTest;

  @Mock
  BlobStoreManager blobStoreManager;

  @Mock
  RepositoryManager repositoryManager;

  @Mock
  RestoreBlobStrategy restoreBlobStrategy;

  @Mock
  Repository repository;

  @Mock
  FileBlobStore fileBlobStore;

  @Mock
  Blob blob;

  @Mock
  Format mavenFormat;

  @Mock
  BlobStoreUsageChecker blobstoreUsageChecker;

  @Mock
  DryRunPrefix dryRunPrefix;

  @Mock
  StorageTx storageTx;

  @Mock
  DefaultIntegrityCheckStrategy defaultIntegrityCheckStrategy;

  @Mock
  IntegrityCheckStrategy testIntegrityCheckStrategy;

  Map<String, IntegrityCheckStrategy> integrityCheckStrategies;

  BlobId blobId;

  FileBlobAttributes blobAttributes;

  TaskConfiguration configuration;

  @Before
  public void setup() throws Exception {
    integrityCheckStrategies = spy(new HashMap<>());
    integrityCheckStrategies.put(Maven2Format.NAME, testIntegrityCheckStrategy);
    integrityCheckStrategies.put(DEFAULT_NAME, defaultIntegrityCheckStrategy);

    underTest = new RestoreMetadataTask(blobStoreManager, repositoryManager,
        ImmutableMap.of("maven2", restoreBlobStrategy), blobstoreUsageChecker, dryRunPrefix, integrityCheckStrategies);

    reset(integrityCheckStrategies); // reset this mock so we more easily verify calls

    configuration = new TaskConfiguration();
    configuration.setString(BLOB_STORE_NAME_FIELD_ID, "test");
    configuration.setId("test");
    configuration.setTypeId(TYPE_ID);

    when(repositoryManager.get("maven-central")).thenReturn(repository);
    when(repository.getFormat()).thenReturn(mavenFormat);
    when(mavenFormat.getValue()).thenReturn("maven2");

    URL resource = Resources
        .getResource("test-restore/content/vol-1/chp-1/86e20baa-0bca-4915-a7dc-9a4f34e72321.properties");
    blobAttributes = new FileBlobAttributes(Paths.get(resource.toURI()));
    blobAttributes.load();
    blobId = new BlobId("86e20baa-0bca-4915-a7dc-9a4f34e72321");
    when(fileBlobStore.getBlobIdStream()).thenReturn(Stream.of(blobId));
    when(blobStoreManager.get("test")).thenReturn(fileBlobStore);

    when(fileBlobStore.get(blobId, true)).thenReturn(blob);
    when(fileBlobStore.getBlobAttributes(blobId)).thenReturn(blobAttributes);

    when(dryRunPrefix.get()).thenReturn("");
  }

  @Test
  public void testRestoreMetadata() throws Exception {
    configuration.setBoolean(RESTORE_BLOBS, true);
    configuration.setBoolean(UNDELETE_BLOBS, true);
    configuration.setBoolean(INTEGRITY_CHECK, false);
    underTest.configure(configuration);

    underTest.execute();

    ArgumentCaptor<Properties> propertiesArgumentCaptor = ArgumentCaptor.forClass(Properties.class);
    verify(restoreBlobStrategy).restore(propertiesArgumentCaptor.capture(), eq(blob), eq("test"), eq(false));
    verify(fileBlobStore).maybeUndeleteBlob(blobstoreUsageChecker, blobId, blobAttributes, false);
    Properties properties = propertiesArgumentCaptor.getValue();

    assertThat(properties.getProperty("@BlobStore.blob-name"), is("org/codehaus/plexus/plexus/3.1/plexus-3.1.pom"));
  }

  @Test
  public void testRestoreMetadataNoUnDelete() throws Exception {
    configuration.setBoolean(RESTORE_BLOBS, true);
    configuration.setBoolean(UNDELETE_BLOBS, false);
    configuration.setBoolean(INTEGRITY_CHECK, false);
    underTest.configure(configuration);

    underTest.execute();

    verify(restoreBlobStrategy).restore(any(), eq(blob), eq("test"), eq(false));
    verify(fileBlobStore, never()).maybeUndeleteBlob(any(), any(), any(), eq(false));
  }

  @Test
  public void testRestoreMetadata_BlobIsMarkedAsDeleted() throws Exception {
    configuration.setBoolean(RESTORE_BLOBS, true);
    configuration.setBoolean(UNDELETE_BLOBS, true);
    configuration.setBoolean(INTEGRITY_CHECK, false);
    underTest.configure(configuration);

    blobAttributes.setDeleted(true);

    underTest.execute();

    verify(restoreBlobStrategy, never()).restore(any(), any(), any());
    verify(fileBlobStore).maybeUndeleteBlob(any(), any(), any(), eq(false));
  }

  @Test
  public void testNoRestoreMetadataNoUnDeleteNoIntegrityCheck() throws Exception {
    configuration.setBoolean(RESTORE_BLOBS, false);
    configuration.setBoolean(UNDELETE_BLOBS, false);
    configuration.setBoolean(INTEGRITY_CHECK, false);

    underTest.configure(configuration);

    underTest.execute();

    verifyZeroInteractions(blobStoreManager);
  }

  @Test
  public void testIntegrityCheck_BlobStoreDoesNotExist() throws Exception {
    configuration.setBoolean(RESTORE_BLOBS, false);
    configuration.setBoolean(UNDELETE_BLOBS, false);
    configuration.setBoolean(INTEGRITY_CHECK, true);
    underTest.configure(configuration);

    when(blobStoreManager.get(anyString())).thenReturn(null);

    underTest.execute();

    verifyZeroInteractions(repositoryManager);
  }

  @Test
  public void testIntegrityCheck_SkipGroupRepositories() throws Exception {
    configuration.setBoolean(RESTORE_BLOBS, false);
    configuration.setBoolean(UNDELETE_BLOBS, false);
    configuration.setBoolean(INTEGRITY_CHECK, true);
    underTest.configure(configuration);

    when(repository.getType()).thenReturn(new GroupType());
    when(repositoryManager.browseForBlobStore(any())).thenReturn(singletonList(repository));

    underTest.execute();

    verifyZeroInteractions(integrityCheckStrategies);
  }

  @Test
  public void testIntegrityCheck_DefaultStrategy() throws Exception {
    configuration.setBoolean(RESTORE_BLOBS, false);
    configuration.setBoolean(UNDELETE_BLOBS, false);
    configuration.setBoolean(INTEGRITY_CHECK, true);
    underTest.configure(configuration);

    when(repositoryManager.browseForBlobStore(any())).thenReturn(singletonList(repository));

    // this should return the DefaultIntegrityCheckStrategy
    when(mavenFormat.getValue()).thenReturn("foo");

    underTest.execute();

    verify(defaultIntegrityCheckStrategy).check(any(), any(), any());
    verifyZeroInteractions(testIntegrityCheckStrategy);
  }

  @Test
  public void testIntegrityCheck() throws Exception {
    configuration.setBoolean(RESTORE_BLOBS, false);
    configuration.setBoolean(UNDELETE_BLOBS, false);
    configuration.setBoolean(INTEGRITY_CHECK, true);
    underTest.configure(configuration);

    when(repositoryManager.browseForBlobStore(any())).thenReturn(singletonList(repository));

    underTest.execute();

    verifyZeroInteractions(defaultIntegrityCheckStrategy);
    verify(testIntegrityCheckStrategy).check(eq(repository), eq(fileBlobStore), any());
  }
}
