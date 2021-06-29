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
package org.sonatype.nexus.hulk.role

import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.ws.rs.Consumes
import javax.ws.rs.FormParam
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces

import org.hibernate.validator.constraints.NotEmpty
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
@Path(ScriptRole.RESOURCE_URI)
@Produces([APPLICATION_JSON])
@Consumes([APPLICATION_JSON])
interface ScriptRole {
	public static final String RESOURCE_URI = '/role'

	@GET
	@Path("read")
	List<RoleXO> read()
	
	/*@GET
	@Path("readReferences")
	List<ReferenceXO> readReferences()*/
	
	/*@GET
	@Path("readSources")
	List<ReferenceXO> readSources()
	
	@POST
	@Path("readFromSource")
	List<RoleXO> readFromSource(@NotEmpty final String source)*/
	
	@POST
	@Path("create")
	RoleXO create(@NotNull @Valid final RoleXO roleXO)
	
	@POST
	@Path("update")
	RoleXO update(@NotNull @Valid final RoleXO roleXO)
	
	@POST
	@Path("remove")
	@Consumes("application/x-www-form-urlencoded")
	void remove(@NotEmpty  @FormParam("roleid") final String id)
	
}

