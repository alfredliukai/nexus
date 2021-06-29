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
package org.sonatype.nexus.hulk.privilege

import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces

import org.hibernate.validator.constraints.NotEmpty
import org.sonatype.nexus.extdirect.model.PagedResponse
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
@Path(ScriptPrivilege.RESOURCE_URI)
@Produces([APPLICATION_JSON])
@Consumes([APPLICATION_JSON])
interface ScriptPrivilege {
	public static final String RESOURCE_URI = '/privilege'

	@POST
	@Path('read')
	PagedResponse<PrivilegeXO> read(StoreLoadParameters parameters)
	
	//@GET
	//@Path('readReferences')
	//List<ReferenceXO> readReferences()
	
	//@POST
	//@Path('extractPage')
	//PagedResponse<PrivilegeXO> extractPage(final StoreLoadParameters parameters, final List<PrivilegeXO> xos)
	
	/*@GET
	@Path('readTypes')
	List<PrivilegeTypeXO> readTypes()*/
	
	@POST
	@Path('create')
	PrivilegeXO create(final @NotNull @Valid PrivilegeXO privilege)
	
	@POST
	@Path('update')
	PrivilegeXO update(final @NotNull @Valid PrivilegeXO privilege)
	
	@POST
	@Path('remove')
	void remove(@NotEmpty final String id)
}

