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
package org.sonatype.nexus.coreui.internal.hulk.rest

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.validation.groups.Default
import org.sonatype.nexus.validation.group.Create
import org.sonatype.nexus.validation.group.Update

import javax.ws.rs.Consumes
import javax.ws.rs.FormParam
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces

import org.sonatype.nexus.validation.Validate
import org.sonatype.nexus.security.role.Role
import org.sonatype.nexus.rest.Resource
import org.hibernate.validator.constraints.NotEmpty
import org.sonatype.goodies.common.ComponentSupport
import org.sonatype.nexus.hulk.bo.ReferenceXO
import org.sonatype.nexus.hulk.role.RoleXO
import org.sonatype.nexus.hulk.role.ScriptRole
import org.sonatype.nexus.security.SecuritySystem
import org.sonatype.nexus.security.authz.AuthorizationManager
import org.apache.shiro.authz.annotation.RequiresAuthentication
import org.apache.shiro.authz.annotation.RequiresPermissions

import com.codahale.metrics.annotation.ExceptionMetered
import com.codahale.metrics.annotation.Timed
import groovy.transform.PackageScope
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import java.util.List

import static javax.ws.rs.core.MediaType.APPLICATION_JSON
import static org.sonatype.nexus.security.user.UserManager.DEFAULT_SOURCE

/**
 * BREAD resource for managing {@link Script} instances.
 *
 * @since 3.0
 */
@Named
@Singleton
@Path(ScriptRoleResource.RESOURCE_URI)
@Api('role')
@Produces([APPLICATION_JSON])
@Consumes([APPLICATION_JSON])
class ScriptRoleResource
extends ComponentSupport
implements ScriptRole, Resource {
	public static final String RESOURCE_URI = '/role'

	@Inject
	SecuritySystem securitySystem

	@Inject
	List<AuthorizationManager> authorizationManagers

	@GET
	@Override
	@Timed
	@ExceptionMetered
	@ApiOperation('Retrieves all available roles')
	@RequiresPermissions('nexus:roles:read')
	@Path('/read')
	List<RoleXO> read() {
		return securitySystem.listRoles(DEFAULT_SOURCE).collect {input ->
			return convert(input)
		}
	}
	
	/*@GET
	@Timed
	@ExceptionMetered
	//@RequiresPermissions('nexus:roles:read')
	@ApiOperation('Retrieves role references form all available {@link AuthorizationManager}s')
	@Path('/readReferences')
	 List<ReferenceXO> readReferences() {
		return securitySystem.listRoles(DEFAULT_SOURCE).collect {input ->
			return new ReferenceXO(
					id: input.roleId,
					name: input.name
					)
		}
	}*/

	/*@GET
	@Timed
	@ExceptionMetered
	//@RequiresPermissions('nexus:roles:read')
	@ApiOperation('Retrieves available role sources')
	@Validate
	@Path('/readSources')
	public List<ReferenceXO> readSources() {
		return authorizationManagers.findResults {manager ->
			return manager.source == DEFAULT_SOURCE ? null : manager
		}.collect {manager ->
			return new ReferenceXO(
					id: manager.source,
					name: manager.source
					)
		}
	}*/
	
	/*@POST
	@Timed
	@ExceptionMetered
	//@RequiresPermissions('nexus:roles:read')
	@ApiOperation('Retrieves roles from specified source')
	@Path('/readFromSource')
	@Validate
	List<RoleXO> readFromSource(@NotEmpty final String source) {
		return securitySystem.listRoles(source).collect {input ->
			return convert(input)
		}
	}*/
	
	@POST
	@Timed
	@ExceptionMetered
	@RequiresAuthentication
	@RequiresPermissions('nexus:roles:create')
	@Validate(groups = [Create.class, Default.class])
	@ApiOperation('Creates a role')
	@Path('/create')
	RoleXO create(@NotNull @Valid final RoleXO roleXO){
		// HACK: Temporary validation for external role IDs to support editable text entry in combo box (LDAP only)
		/*if (roleXO.source == 'LDAP') {
			securitySystem.getAuthorizationManager(roleXO.source).getRole(roleXO.id)
		}*/
		return convert(securitySystem.getAuthorizationManager(DEFAULT_SOURCE).addRole(
				new Role(
				roleId: roleXO.id,
				source: DEFAULT_SOURCE,
				name: roleXO.name,
				description: roleXO.description,
				readOnly: false,
				privileges: roleXO.privileges,
				roles: roleXO.roles
				)
				))
	}
	
	@POST
	@Timed
	@ExceptionMetered
	@RequiresAuthentication
	@RequiresPermissions('nexus:roles:update')
	@ApiOperation('Updates a role')
	@Path('/update')
	@Validate(groups = [Update.class, Default.class])
	RoleXO update(@NotNull @Valid final RoleXO roleXO) {
		assert !roleXO.roles.contains(roleXO.id)
		return convert(securitySystem.getAuthorizationManager(DEFAULT_SOURCE).updateRole(
					new Role(
					roleId: roleXO.id,
					version: roleXO.version,
					source: DEFAULT_SOURCE,
					name: roleXO.name,
					description: roleXO.description,
					readOnly: false,
					privileges: roleXO.privileges,
					roles: roleXO.roles
					)
				))
	}

	@POST
	@Timed
	@ExceptionMetered
	@RequiresAuthentication
	@RequiresPermissions('nexus:roles:delete')
	@ApiOperation('Deletes a role')
	@Path('/remove')
	@Validate
	@Consumes("application/x-www-form-urlencoded")
	 void remove(@NotEmpty @FormParam("roleid")final String id) {
		securitySystem.getAuthorizationManager(DEFAULT_SOURCE).deleteRole(id)
	}


	/**
	 * Convert role to XO.
	 */
	@PackageScope
	RoleXO convert(final Role input) {
		return new RoleXO(
				id: input.roleId,
				version: input.version,
				//source: (input.source == DEFAULT_SOURCE || !input.source) ? 'Nexus' : input.source,
				name: input.name != null ? input.name : input.roleId,
				description: input.description != null ? input.description : input.roleId,
				//readOnly: input.readOnly,
				privileges: input.privileges,
				roles: input.roles
				)
	}
}
