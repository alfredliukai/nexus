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
package org.sonatype.nexus.coreui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.ws.rs.WebApplicationException;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.extdirect.model.StoreLoadParameters;
import org.sonatype.nexus.extdirect.model.StoreLoadParameters.Filter;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.BrowseResult;
import org.sonatype.nexus.repository.browse.BrowseService;
import org.sonatype.nexus.repository.maintenance.MaintenanceService;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.security.VariableResolverAdapterManager;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentMaintenance;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.security.BreadActions;
import org.sonatype.nexus.selector.CselExpressionValidator;
import org.sonatype.nexus.selector.JexlExpressionValidator;
import org.sonatype.nexus.selector.VariableSource;

import com.google.common.base.Suppliers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link ComponentComponent}.
 */
public class ComponentComponentTest
    extends TestSupport
{
  @Mock
  RepositoryManager repositoryManager;

  @Mock
  Repository repository;

  @Mock
  ComponentMaintenance componentMaintenance;

  @Mock
  ContentPermissionChecker contentPermissionChecker;

  @Mock
  VariableResolverAdapterManager variableResolverAdapterManager;

  @Mock
  VariableResolverAdapter variableResolverAdapter;

  @Mock
  StorageFacet storageFacet;

  @Mock
  StorageTx storageTx;

  @Mock
  MaintenanceService maintenanceService;

  @Mock
  BrowseService browseService;

  @Mock
  JexlExpressionValidator jexlExpressionValidator;

  @Mock
  CselExpressionValidator cselExpressionValidator;

  private ComponentComponent underTest;

  @Before
  public void setup() {
    underTest = new ComponentComponent();
    underTest.setRepositoryManager(repositoryManager);
    underTest.setContentPermissionChecker(contentPermissionChecker);
    underTest.setVariableResolverAdapterManager(variableResolverAdapterManager);
    underTest.setMaintenanceService(maintenanceService);
    underTest.setBrowseService(browseService);
    underTest.setJexlExpressionValidator(jexlExpressionValidator);
    underTest.setCselExpressionValidator(cselExpressionValidator);

    when(repositoryManager.get("testRepositoryName")).thenReturn(repository);
    when(repository.getName()).thenReturn("testRepositoryName");
    when(repository.getFormat()).thenReturn(new Format("testFormat") { });
    when(repository.facet(ComponentMaintenance.class)).thenReturn(componentMaintenance);
    when(repository.facet(StorageFacet.class)).thenReturn(storageFacet);
    when(variableResolverAdapterManager.get("testFormat")).thenReturn(variableResolverAdapter);
    when(storageFacet.txSupplier()).thenReturn(Suppliers.ofInstance(storageTx));
  }

  @Test
  public void testDeleteComponent_success() {
    Component component = mock(Component.class);
    Asset asset = mock(Asset.class);
    VariableSource variableSource = mock(VariableSource.class);
    when(variableResolverAdapter.fromAsset(asset)).thenReturn(variableSource);
    when(storageTx.findComponent(any(EntityId.class))).thenReturn(component);
    when(storageTx.browseAssets(component)).thenReturn(Collections.singletonList(asset));
    when(contentPermissionChecker.isPermitted("testRepositoryName", "testFormat", BreadActions.DELETE, variableSource))
        .thenReturn(true);
    underTest.deleteComponent("testComponentId", "testRepositoryName");
  }

  @Test
  public void testDeleteComponent_success_multipleAssets() {
    Component component = mock(Component.class);
    Asset asset = mock(Asset.class);
    VariableSource variableSource = mock(VariableSource.class);
    Asset asset2 = mock(Asset.class);
    VariableSource variableSource2 = mock(VariableSource.class);
    when(variableResolverAdapter.fromAsset(asset)).thenReturn(variableSource);
    when(variableResolverAdapter.fromAsset(asset2)).thenReturn(variableSource2);
    when(storageTx.findComponent(any(EntityId.class))).thenReturn(component);
    when(storageTx.browseAssets(component)).thenReturn(Arrays.asList(asset, asset2));
    when(contentPermissionChecker.isPermitted("testRepositoryName", "testFormat", BreadActions.DELETE, variableSource))
        .thenReturn(true);
    when(contentPermissionChecker.isPermitted("testRepositoryName", "testFormat", BreadActions.DELETE, variableSource2))
        .thenReturn(true);
    underTest.deleteComponent("testComponentId", "testRepositoryName");

    verify(maintenanceService).deleteComponent(repository, component);
  }

  @Test
  public void testDeleteAsset() {
    Asset asset = mock(Asset.class);
    VariableSource variableSource = mock(VariableSource.class);
    Bucket bucket = mock(Bucket.class);
    when(variableResolverAdapter.fromAsset(asset)).thenReturn(variableSource);
    when(storageTx.findBucket(repository)).thenReturn(bucket);
    when(storageTx.findAsset(new DetachedEntityId("testAssetId"), bucket)).thenReturn(asset);
    underTest.deleteAsset("testAssetId", "testRepositoryName");
    verify(maintenanceService).deleteAsset(repository, asset);
  }

  @Test
  public void testReadComponent() {
    Component component = mock(Component.class);
    Asset asset = mock(Asset.class);
    EntityMetadata entityMetadata = mock(EntityMetadata.class);
    VariableSource variableSource = mock(VariableSource.class);
    when(contentPermissionChecker.isPermitted(any(),any(), any(), any())).thenReturn(true);
    when(component.getEntityMetadata()).thenReturn(entityMetadata);
    when(entityMetadata.getId()).thenReturn(new DetachedEntityId("someid"));
    when(storageTx.findComponent(eq(new DetachedEntityId("someid")))).thenReturn(component);
    when(storageTx.browseAssets(component)).thenReturn(Arrays.asList(asset));
    when(variableResolverAdapter.fromAsset(asset)).thenReturn(variableSource);
    ComponentXO componentXO = underTest.readComponent("someid", "testRepositoryName");

    assertThat(componentXO, is(notNullValue()));
    assertThat(componentXO.getId(), is("someid"));
  }

  @Test
  public void testReadComponent_notFound() {
    Component component = mock(Component.class);
    when(storageTx.findComponent(eq(new DetachedEntityId("someid")))).thenReturn(component);
    when(storageTx.browseAssets(component)).thenReturn(null);
    try {
      underTest.readComponent("someid", "testRepositoryName");
      fail("Exception should have been thrown");
    }
    catch (WebApplicationException e) {
      assertThat(e.getResponse(), is(notNullValue()));
      assertThat(e.getResponse().getStatus(), is(404));
    }
  }

  @Test
  public void testReadComponentWithNoAssets_notFound() {
    when(storageTx.findComponent(eq(new DetachedEntityId("someid")))).thenReturn(null);
    try {
      underTest.readComponent("someid", "testRepositoryName");
      fail("Exception should have been thrown");
    }
    catch (WebApplicationException e) {
      assertThat(e.getResponse(), is(notNullValue()));
      assertThat(e.getResponse().getStatus(), is(404));
    }
  }

  @Test
  public void testReadAsset() {
    Asset asset = mock(Asset.class);
    EntityMetadata entityMetadata = mock(EntityMetadata.class);
    when(asset.getEntityMetadata()).thenReturn(entityMetadata);
    when(asset.attributes()).thenReturn(new NestedAttributesMap("attributes", new HashMap<>()));
    when(entityMetadata.getId()).thenReturn(new DetachedEntityId("someid"));
    when(contentPermissionChecker.isPermitted(any(),any(), any(), any())).thenReturn(true);

    when(browseService.getLastThirtyDays(asset)).thenReturn(10L);
    when(browseService.getAssetById(new DetachedEntityId("someid"), repository)).thenReturn(asset);
    when(browseService.getRepositoryBucketNames(repository))
        .thenReturn(Collections.singletonMap(new DetachedEntityId("someid"), "testBucketName"));
    AssetXO assetXO = underTest.readAsset("someid", "testRepositoryName");

    assertThat(assetXO, is(notNullValue()));
    assertThat(assetXO.getId(), is("someid"));
    assertThat(assetXO.getDownloadCount(), is(10L));
  }

  @Test
  public void testReadAsset_notFound() {
    when(storageTx.findAsset(eq(new DetachedEntityId("someid")), any())).thenReturn(null);
    try {
      underTest.readAsset("someid", "testRepositoryName");
      fail("Exception should have been thrown");
    }
    catch (WebApplicationException e) {
      assertThat(e.getResponse(), is(notNullValue()));
      assertThat(e.getResponse().getStatus(), is(404));
    }
  }

  @Test
  public void testPreviewAsset_jexl() {
    when(browseService.previewAssets(any(), any(), any(), any()))
        .thenReturn(new BrowseResult<Asset>(0, Collections.emptyList()));
    underTest.previewAssets(createParameters("jexl", "foo"));

    verify(jexlExpressionValidator).validate("foo");
  }

  @Test
  public void testPreviewAsset_csel() {
    when(browseService.previewAssets(any(), any(), any(), any()))
        .thenReturn(new BrowseResult<Asset>(0, Collections.emptyList()));
    underTest.previewAssets(createParameters("csel", "foo"));

    verify(cselExpressionValidator).validate("foo");
  }

  private StoreLoadParameters createParameters(final String type, final String expression) {
    StoreLoadParameters parameters = new StoreLoadParameters();
    List<Filter> filters = new ArrayList<>();
    filters.add(createFilter("repositoryName", "testRepositoryName"));
    filters.add(createFilter("type", type));
    filters.add(createFilter("expression", expression));
    parameters.setFilter(filters);
    return parameters;
  }

  private Filter createFilter(final String property, final String value) {
    Filter filter = new Filter();
    filter.setProperty(property);
    filter.setValue(value);
    return filter;
  }
}
