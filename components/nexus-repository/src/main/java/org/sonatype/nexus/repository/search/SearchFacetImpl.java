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
package org.sonatype.nexus.repository.search;

import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.orient.entity.AttachedEntityId;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.BucketEntityAdapter;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentEntityAdapter;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.collect.ImmutableMap;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.impl.ODocument;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static org.sonatype.nexus.repository.FacetSupport.State.STARTED;
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.REPOSITORY_NAME;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_BUCKET;

/**
 * Default {@link SearchFacet} implementation.
 *
 * Depends on presence of a {@link StorageFacet} attached to {@link Repository}.
 *
 * @since 3.0
 */
@Named
public class SearchFacetImpl
    extends FacetSupport
    implements SearchFacet
{
  private static final String COMPONENTS_IN_BUCKET = String.format(
      "select from %s where %s = :bucket", ComponentEntityAdapter.DB_CLASS, P_BUCKET);

  private final SearchService searchService;

  private final Map<String, ComponentMetadataProducer> componentMetadataProducers;

  private final ComponentEntityAdapter componentEntityAdapter;

  private final BucketEntityAdapter bucketEntityAdapter;

  private Map<String, Object> repositoryMetadata;

  @Inject
  public SearchFacetImpl(final SearchService searchService,
                         final Map<String, ComponentMetadataProducer> componentMetadataProducers,
                         final ComponentEntityAdapter componentEntityAdapter,
                         final BucketEntityAdapter bucketEntityAdapter)
  {
    this.searchService = checkNotNull(searchService);
    this.componentMetadataProducers = checkNotNull(componentMetadataProducers);
    this.componentEntityAdapter = checkNotNull(componentEntityAdapter);
    this.bucketEntityAdapter = checkNotNull(bucketEntityAdapter);
  }

  @Override
  protected void doInit(Configuration configuration) throws Exception {
    repositoryMetadata = ImmutableMap.of(REPOSITORY_NAME, getRepository().getName());
    super.doInit(configuration);
  }

  @Override
  @Guarded(by = STARTED)
  public void rebuildIndex() {
    log.info("Rebuilding index of repository {}", getRepository().getName());
    searchService.rebuildIndex(getRepository());
    UnitOfWork.begin(facet(StorageFacet.class).txSupplier());
    try {
      rebuildComponentIndex();
    }
    finally {
      UnitOfWork.end();
    }
  }

  @Transactional
  protected void rebuildComponentIndex() {
    StorageTx tx = UnitOfWork.currentTx();
    Bucket bucket = tx.findBucket(getRepository());
    if (bucket != null) {
      ORID bucketId = bucketEntityAdapter.recordIdentity(bucket);
      Iterable<ODocument> docs = tx.browse(COMPONENTS_IN_BUCKET, ImmutableMap.of(P_BUCKET, bucketId));
      bulkPut(transform(filter(docs, Objects::nonNull), this::componentId));
    }
  }

  @Override
  @Guarded(by = STARTED)
  public void put(final EntityId componentId) {
    checkNotNull(componentId);
    String json = json(componentId);
    if (json != null) {
      searchService.put(getRepository(), identifier(componentId), json);
    }
  }

  @Override
  @Guarded(by = STARTED)
  public void bulkPut(final Iterable<EntityId> componentIds) {
    checkNotNull(componentIds);
    searchService.bulkPut(getRepository(), componentIds, this::identifier, this::json);
  }

  @Override
  @Guarded(by = STARTED)
  public void delete(final EntityId componentId) {
    checkNotNull(componentId);
    searchService.delete(getRepository(), identifier(componentId));
  }

  @Override
  @Guarded(by = STARTED)
  public void bulkDelete(final Iterable<EntityId> componentIds) {
    checkNotNull(componentIds);
    searchService.bulkDelete(getRepository(), transform(componentIds, this::identifier));
  }

  @Override
  protected void doStart() throws Exception {
    searchService.createIndex(getRepository());
  }

  @Override
  protected void doDelete() {
    searchService.deleteIndex(getRepository());
  }

  /**
   * Converts a component's Orient record id to its {@link EntityId}.
   */
  @Nullable
  private EntityId componentId(@Nullable final ODocument doc) {
    return doc != null ? new AttachedEntityId(componentEntityAdapter, doc.getIdentity()) : null;
  }

  /**
   * Looks for a {@link ComponentMetadataProducer} specific to the component {@link Format}.
   * If one is not available will use a default one ({@link DefaultComponentMetadataProducer}).
   */
  private ComponentMetadataProducer producer(final Component component) {
    checkNotNull(component);
    String format = component.format();
    ComponentMetadataProducer producer = componentMetadataProducers.get(format);
    if (producer == null) {
      producer = componentMetadataProducers.get("default");
    }
    checkState(producer != null, "Could not find a component metadata producer for format: %s", format);
    return producer;
  }

  /**
   * Returns the document identifier in the repository's index for the given component.
   */
  @Nullable
  private String identifier(@Nullable final EntityId componentId) {
    return componentId != null ? componentId.getValue() : null;
  }

  /**
   * Returns the JSON document representing the given component in the repository's index.
   */
  @Nullable
  @Transactional
  protected String json(@Nullable final EntityId componentId) {
    if (componentId != null) {
      StorageTx tx = UnitOfWork.currentTx();
      Component component = componentEntityAdapter.read(tx.getDb(), componentId);
      if (component != null) {
        Iterable<Asset> assets = tx.browseAssets(component);
        return producer(component).getMetadata(component, assets, repositoryMetadata);
      }
    }
    return null;
  }
}
