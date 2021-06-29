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
package org.sonatype.nexus.repository.rest.internal.resources.doc;


import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;
import org.sonatype.nexus.repository.rest.api.RepositoryXO;
import org.sonatype.nexus.repository.rest.internal.resources.RepositoriesResource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * Swagger documentation for {@link RepositoriesResource}
 *
 * @since 3.9
 */
@Api(value = "repositories")
public interface RepositoriesResourceDoc
{

  @POST
  @Path("getRepository")
  @ApiOperation("List repository by repositoryId")
  @Consumes("application/x-www-form-urlencoded")
  RepositoryXO getRepository(@NotEmpty @FormParam("repositoryId") final String repositoryId);
	
 /* @GET
  @Path("getRepositories")
  @ApiOperation("List repositories")
  List<RepositoryXO> getRepositories();*/
  
  @POST
  @Path("create")
  @ApiOperation("create repository")
  RepositoryXO create(final @NotNull @Valid RepositoryXO repositoryXO) throws Exception;
  
  @POST
  @Path("update")
  @ApiOperation("update repository")
  RepositoryXO update(final @NotNull @Valid RepositoryXO repositoryXO) throws Exception;

  @POST
  @Path("remove")
  @ApiOperation("remove repository")
  @Consumes("application/x-www-form-urlencoded")
  void remove(@NotEmpty  @FormParam("repositoryId") final String repositoryId) throws Exception;
  
}
