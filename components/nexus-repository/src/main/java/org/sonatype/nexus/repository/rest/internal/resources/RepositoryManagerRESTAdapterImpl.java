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
package org.sonatype.nexus.repository.rest.internal.resources;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.groups.Default;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.sonatype.nexus.rapture.PasswordPlaceholder;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.rest.api.RepositoryXO;
import org.sonatype.nexus.repository.security.RepositoryAdminPermission;
import org.sonatype.nexus.repository.security.RepositoryContentSelectorPermission;
import org.sonatype.nexus.repository.security.RepositoryViewPermission;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.selector.SelectorManager;
import org.sonatype.nexus.validation.Validate;
import org.sonatype.nexus.validation.group.Create;
import org.sonatype.nexus.validation.group.Update;

//import com.codahale.metrics.annotation.ExceptionMetered;
//import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Streams;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.sonatype.nexus.repository.http.HttpStatus.FORBIDDEN;
import static org.sonatype.nexus.repository.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.sonatype.nexus.security.BreadActions.BROWSE;
import static org.sonatype.nexus.security.BreadActions.READ;
import static org.sonatype.nexus.security.BreadActions.EDIT;
import static org.sonatype.nexus.security.BreadActions.ADD;
import static org.sonatype.nexus.security.BreadActions.DELETE;

/**
 * An implementation of the {@link RepositoryManagerRESTAdapter}
 *
 * @since 3.4
 */
@Named
public class RepositoryManagerRESTAdapterImpl implements RepositoryManagerRESTAdapter {
	private final RepositoryManager repositoryManager;

	private final SecurityHelper securityHelper;

	private final SelectorManager selectorManager;

	@Inject
	public RepositoryManagerRESTAdapterImpl(final RepositoryManager repositoryManager,
			final SecurityHelper securityHelper, final SelectorManager selectorManager) {
		this.repositoryManager = checkNotNull(repositoryManager);
		this.securityHelper = checkNotNull(securityHelper);
		this.selectorManager = checkNotNull(selectorManager);
	}

	@Override
	public Repository getRepository(final String repositoryId) {
		if (repositoryId == null) {
			throw new WebApplicationException("repositoryId is required.", UNPROCESSABLE_ENTITY);
		}
		Repository repository = ofNullable(repositoryManager.get(repositoryId))
				.orElseThrow(() -> new NotFoundException("Unable to locate repository with id " + repositoryId));

		if (userCanBrowseRepository(repository)) {
			// browse implies complete access to the repository.
			return repository;
		} else if (userCanViewRepository(repository)) {
			// user knows the repository exists but does not have the
			// appropriate permission to browse, return a 403
			throw new WebApplicationException(FORBIDDEN);
		} else {
			// User does not know the repository exists because they can not
			// VIEW or BROWSE, return a 404
			throw new NotFoundException("Unable to locate repository with id " + repositoryId);
		}
	}

	@Override
	public List<Repository> getRepositories() {
		return Streams.stream(repositoryManager.browse()).filter(this::userCanBrowseRepository)
				.collect(Collectors.toList());
	}

	@Override
	@RequiresAuthentication
	@Validate
	public Repository create(RepositoryXO repositoryXO) throws Exception {
		if (userHasAddPermission(repositoryXO)) {
			return repositoryManager.create(convert(repositoryXO));
		} else {
			throw new WebApplicationException(FORBIDDEN);
		}
	}

	@RequiresAuthentication
	@Validate
	public Repository update(RepositoryXO repositoryXO) throws Exception {
		Repository repository = repositoryManager.get(repositoryXO.getName());
		// proxy仓库这块，存在httpclient中的authentication,这块存在密码部分
		if (userHasEditPermission(repository)) {
			// 这一堆的判断都是为了代理仓库中password不修改进行编写的
			Map<String, Map<String, Object>> attributes = repositoryXO.getAttributes();
			if (attributes != null) {
				Map<String, Object> httpClientAttributes = attributes.get("httpclient");
				if (httpClientAttributes != null) {
					String password = (String) httpClientAttributes.get("password");
					if (PasswordPlaceholder.is(password)) {
						Map<String, Map<String, Object>> rattributes = repository.getConfiguration().getAttributes();
						if (rattributes != null) {
							Map<String, Object> rhttpClientAttributes = rattributes.get("httpclient");
							if (rhttpClientAttributes != null) {
								rhttpClientAttributes.put("password", password);
							}
						}
					}
				}
			}
			Configuration updatedConfiguration = repository.getConfiguration().copy();
			updatedConfiguration.setOnline(repositoryXO.getOnline());
			updatedConfiguration.setAttributes(repositoryXO.getAttributes());
			return repositoryManager.update(updatedConfiguration);
		} else {
			throw new WebApplicationException(FORBIDDEN);
		}
	}

	@Override
	@RequiresAuthentication
	@Validate
	public void remove(String name) throws Exception {
		Repository repository = repositoryManager.get(name);
		if (userHasDeletePermission(repository)) {
			repositoryManager.delete(name);
		} else {
			throw new WebApplicationException(FORBIDDEN);
		}
	}

	private boolean userCanViewRepository(final Repository repository) {
		return userHasReadPermission(repository) || userHasAnyContentSelectorAccess(repository);
	}

	private boolean userCanBrowseRepository(final Repository repository) {
		return userHasBrowsePermissions(repository) || userHasAnyContentSelectorAccess(repository);
	}

	private boolean userHasBrowsePermissions(final Repository repository) {
		return securityHelper.anyPermitted(
				new RepositoryViewPermission(repository.getFormat().getValue(), repository.getName(), BROWSE));
	}

	private boolean userHasReadPermission(final Repository repository) {
		return securityHelper.anyPermitted(
				new RepositoryViewPermission(repository.getFormat().getValue(), repository.getName(), READ));
	}

	private boolean userHasAnyContentSelectorAccess(final Repository repository) {
		return selectorManager.browse().stream()
				.anyMatch(sc -> securityHelper.anyPermitted(new RepositoryContentSelectorPermission(sc.getName(),
						repository.getFormat().getValue(), repository.getName(), singletonList(BROWSE))));
	}

	/***
	 * added by liukai
	 * 
	 * @param repository
	 * @return
	 */
	private boolean userHasEditPermission(final Repository repository) {
		return securityHelper.anyPermitted(
				new RepositoryAdminPermission(repository.getFormat().getValue(), repository.getName(), EDIT));
	}

	private boolean userHasEditPermission(final RepositoryXO repositoryXO) {
		return securityHelper
				.anyPermitted(new RepositoryAdminPermission(repositoryXO.getFormat(), repositoryXO.getName(), EDIT));
	}

	private boolean userHasAddPermission(final Repository repository) {
		return securityHelper.anyPermitted(
				new RepositoryAdminPermission(repository.getFormat().getValue(), repository.getName(), ADD));
	}

	private boolean userHasAddPermission(final RepositoryXO repositoryXO) {
		return securityHelper
				.anyPermitted(new RepositoryAdminPermission(repositoryXO.getFormat(), repositoryXO.getName(), ADD));
	}

	private boolean userHasDeletePermission(final Repository repository) {
		return securityHelper.anyPermitted(
				new RepositoryAdminPermission(repository.getFormat().getValue(), repository.getName(), DELETE));
	}

	private Configuration convert(final RepositoryXO repositoryXO) {
		Configuration configuration = new Configuration();
		configuration.setRepositoryName(repositoryXO.getName());
		configuration.setRecipeName(repositoryXO.getRecipe());
		configuration.setOnline(repositoryXO.getOnline());
		configuration.setAttributes(repositoryXO.getAttributes());
		return configuration;
	}

}
