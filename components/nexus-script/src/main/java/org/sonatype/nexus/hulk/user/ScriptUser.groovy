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
package org.sonatype.nexus.hulk.user

import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.ws.rs.BeanParam
import javax.ws.rs.Consumes
import javax.ws.rs.FormParam
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import org.hibernate.validator.constraints.NotEmpty
import org.sonatype.nexus.extdirect.model.StoreLoadParameters
import org.sonatype.nexus.hulk.bo.ReferenceXO

import static javax.ws.rs.core.MediaType.APPLICATION_JSON
import static javax.ws.rs.core.MediaType.TEXT_PLAIN

import java.util.List

import javax.annotation.Nullable

/**
 * Public API for managing Scripts. Provides BREAD capabilities.
 * 
 * @since 3.0
 */
@Path(ScriptUser.RESOURCE_URI)
@Produces([APPLICATION_JSON])
@Consumes([APPLICATION_JSON])
interface ScriptUser {
	public static final String RESOURCE_URI = '/user'

	/*@POST
	@Path("read")
	List<UserXO> read(@Nullable final StoreLoadParameters parameters)*/
	
	 @POST
	 @Path("read")
	 @Consumes("application/x-www-form-urlencoded")
	 List<UserXO> read(@Nullable  @FormParam("userid") final String userid)

	/*@GET
	@Path("readSources")
	List<ReferenceXO> readSources()*/

	/*@GET
	@Path("readAccount")
	UserAccountXO readAccount()*/

	@POST
	@Path("create")
	UserXO create(@NotNull @Valid  final UserXO userXO)
	
	@POST
	@Path("update")
	UserXO update(@NotNull @Valid final UserXO userXO)
	
	@POST
	@Path("updateRoleMappings")
	UserXO updateRoleMappings(@NotNull @Valid final UserRoleMappingsXO userRoleMappingsXO)
	
	/*@POST
	@Path("updateAccount")
	UserAccountXO updateAccount(@NotNull @Valid final UserAccountXO userAccountXO)*/
	
	/*@POST
	@Path("changePassword")
	void changePassword(@NotEmpty final String authToken,
		@NotEmpty final String userId,
		@NotEmpty final String password)*/
	
	@POST
	@Path("remove")
	@Consumes("application/x-www-form-urlencoded")
	void remove(@NotEmpty  @FormParam("userid") final String userid)
}

