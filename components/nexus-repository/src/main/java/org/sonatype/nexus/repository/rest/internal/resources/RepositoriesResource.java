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


import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.hibernate.validator.constraints.NotEmpty;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.rest.api.RepositoryStatusXO;
import org.sonatype.nexus.repository.rest.api.RepositoryXO;
import org.sonatype.nexus.repository.rest.internal.resources.doc.RepositoriesResourceDoc;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.validation.Validate;

import groovy.transform.PackageScope;
import io.swagger.annotations.ApiOperation;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sonatype.nexus.repository.rest.internal.resources.RepositoriesResource.RESOURCE_URI;

/**
 * @since 3.9
 */
@Named
@Singleton
@Path(RESOURCE_URI)
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class RepositoriesResource implements Resource, RepositoriesResourceDoc {
	// public static final String RESOURCE_URI = BETA_API_PREFIX +
	// "/repositories";
	public static final String RESOURCE_URI = "/repositories";

	private final RepositoryManagerRESTAdapter repositoryManagerRESTAdapter;

	@Inject
	ProxyType proxyType;

	@Inject
	public RepositoriesResource(final RepositoryManagerRESTAdapter repositoryManagerRESTAdapter) {
		this.repositoryManagerRESTAdapter = checkNotNull(repositoryManagerRESTAdapter);
	}

	@POST
	@RequiresAuthentication
	@Path("getRepository")
	@Consumes("application/x-www-form-urlencoded")
	public RepositoryXO getRepository(@NotEmpty @FormParam("repositoryId") final String repositoryId) {
		return convert(repositoryManagerRESTAdapter.getRepository(repositoryId));
	}

	/*@GET
	@Path("getRepositories")
	public List<RepositoryXO> getRepositories() {
		return repositoryManagerRESTAdapter.getRepositories().stream().map(RepositoryXO::fromRepository)
				.collect(toList());
	}*/

	@POST
	@RequiresAuthentication
	@Path("create")
	@Validate
	public RepositoryXO create(final @NotNull @Valid RepositoryXO repositoryXO) throws Exception {
		// TODO Auto-generated method stub
		return convert(repositoryManagerRESTAdapter.create(repositoryXO));
	}

	@Override
	public RepositoryXO update(final @NotNull @Valid RepositoryXO repositoryXO) throws Exception {
		return convert(repositoryManagerRESTAdapter.update(repositoryXO));
	}
	
	@POST
	@RequiresAuthentication
	@Override
	@ApiOperation("Deletes a repository")
	@Path("/remove")
	@Validate
	@Consumes("application/x-www-form-urlencoded")
	public void remove(@NotEmpty @FormParam("repositoryId") final String repositoryId) throws Exception {
		repositoryManagerRESTAdapter.remove(repositoryId);
	}

	/**
	 * convert repository to XO
	 * 
	 * @param repository
	 * @return
	 */
	@PackageScope
	RepositoryXO convert(final Repository repository) {
		RepositoryXO repositoryXO = new RepositoryXO();
		repositoryXO.setName(repository.getName());
		repositoryXO.setType(repository.getType().getValue());
		repositoryXO.setFormat(repository.getFormat().getValue());
		repositoryXO.setOnline(repository.getConfiguration().isOnline());
		repositoryXO.setRecipe(repository.getConfiguration().getRecipeName());
		repositoryXO.setAttributes(repository.getConfiguration().getAttributes());
		repositoryXO.setUrl(repository.getUrl());
		repositoryXO.setStatus(buildStatus(repository));
		return repositoryXO;
	}

	@PackageScope
	RepositoryStatusXO buildStatus(final Repository repository) {
		RepositoryStatusXO statusXO = new RepositoryStatusXO();
		statusXO.setRepositoryName(repository.getName());
		statusXO.setOnline(repository.getConfiguration().isOnline());
		if (proxyType == repository.getType()) {
			// undo
			// 还不清楚如何做
			// update by liukai
		}
		return statusXO;
	}

}
