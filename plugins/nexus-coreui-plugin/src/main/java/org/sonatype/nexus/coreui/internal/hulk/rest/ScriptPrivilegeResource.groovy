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
import javax.annotation.Nullable

import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.validation.Valid
import javax.validation.constraints.NotNull
import javax.validation.groups.Default

import org.apache.shiro.authz.annotation.RequiresAuthentication
import org.apache.shiro.authz.annotation.RequiresPermissions
import org.hibernate.validator.constraints.NotEmpty
import org.sonatype.goodies.common.ComponentSupport
import org.sonatype.nexus.extdirect.model.PagedResponse
import org.sonatype.nexus.extdirect.model.StoreLoadParameters
import org.sonatype.nexus.hulk.privilege.PrivilegeTypeXO
import org.sonatype.nexus.hulk.privilege.PrivilegeXO
import org.sonatype.nexus.hulk.privilege.FormFieldXO
import org.sonatype.nexus.hulk.privilege.ScriptPrivilege

import org.sonatype.nexus.hulk.bo.ReferenceXO
import org.sonatype.nexus.rest.Resource
import org.sonatype.nexus.security.SecuritySystem
import org.sonatype.nexus.security.authz.AuthorizationManager
import org.sonatype.nexus.security.privilege.Privilege
import org.sonatype.nexus.security.privilege.PrivilegeDescriptor
import com.codahale.metrics.annotation.ExceptionMetered
import com.codahale.metrics.annotation.Timed
import org.sonatype.nexus.validation.Validate
import org.sonatype.nexus.validation.group.Create
import org.sonatype.nexus.validation.group.Update

import groovy.transform.PackageScope

import static javax.ws.rs.core.MediaType.APPLICATION_JSON
import com.google.common.collect.Maps
import com.softwarementors.extjs.djn.config.annotations.DirectMethod

import java.util.List

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation

import static com.google.common.base.Preconditions.checkArgument
import static org.sonatype.nexus.security.user.UserManager.DEFAULT_SOURCE


/**
 * BREAD resource for managing {@link Script} instances.
 *
 * @since 3.0
 */
@Named
@Singleton
@Path('/privilege')
@Api('privilege')
@Produces([APPLICATION_JSON])
@Consumes([APPLICATION_JSON])
class ScriptPrivilegeResource
extends ComponentSupport
implements ScriptPrivilege, Resource {

	@Inject
	SecuritySystem securitySystem

	@Inject
	List<PrivilegeDescriptor> privilegeDescriptors;
	
	@POST
	@Override
	@Timed
	@ExceptionMetered
	@ApiOperation('Retrieves privileges')
	//@RequiresPermissions('nexus:privileges:read')
	@Path('/read')
	PagedResponse<PrivilegeXO> read(StoreLoadParameters parameters) {
		return extractPage(parameters, securitySystem.listPrivileges().collect { input ->
			return convert(input)
		})
	}
	
	/*@GET
	@Override
	@Timed
	@ExceptionMetered
	@ApiOperation('Retrieves privileges and extracts name and id fields only')
	//@RequiresPermissions('nexus:privileges:read')
	@Path('/readReferences')
	List<PrivilegeTypeXO>  readReferences() {
		return securitySystem.listPrivileges().collect { Privilege privilege ->
			return new ReferenceXO(id: privilege.id, name: privilege.name)
		}
	}*/

	/**
	 * Return only those records matching the given parameters. Will apply a filter and/or sort to client-exposed
	 * properties of {@link PrivilegeXO}: name, description, permission, type.
	 */
	//@RequiresPermissions('nexus:privileges:read')
	//@POST
	//@ApiOperation('Retrieves privileges and extracts name and id fields only')
	PagedResponse<PrivilegeXO> extractPage(final StoreLoadParameters parameters, final List<PrivilegeXO> xos) {
		log.trace("requesting page with parameters: $parameters and size of: ${xos.size()}")

		checkArgument(!parameters.start || parameters.start < xos.size(), "Requested to skip more results than available")

		List<PrivilegeXO> result = xos.collect()
		if(parameters.filter) {
			def filter = parameters.filter.first().value
			result = xos.findResults{ PrivilegeXO xo ->
				(xo.name.contains(filter) || xo.description.contains(filter) || xo.permission.contains(filter) ||
						xo.type.contains(filter)) ? xo : null
			}
		}

		if(parameters.sort) {
			//assume one sort, not multiple props
			int order = parameters.sort[0].direction == 'ASC' ? 0 : 1
			String sort = parameters.sort[0].property
			result = result.sort { a, b ->
				def desc = a."$sort" <=> b."$sort"
				order ? -desc : desc
			}
		}

		int size = result.size()
		int potentialFinalIndex = parameters.start + parameters.limit
		int finalIndex = (size > potentialFinalIndex) ? potentialFinalIndex : size
		Range indices = (parameters.start..<finalIndex)
		return new PagedResponse(size, result[indices])
	}
	
	/*@GET
	@Timed
	@ExceptionMetered
	@ApiOperation('Retrieve available privilege types')
	//@RequiresPermissions('nexus:privileges:read')
	@Path('/readTypes')
	List<PrivilegeTypeXO> readTypes() {
		return privilegeDescriptors.collect { descriptor ->
			new PrivilegeTypeXO(
					id: descriptor.type,
					name: descriptor.name,
					formFields: descriptor.formFields?.collect { FormFieldXO.create(it) }
					)
		}
	}*/
	
	@POST
	@Timed
	@ExceptionMetered
	//@RequiresAuthentication
	//@RequiresPermissions('nexus:privileges:create')
	@Validate(groups = [Create.class, Default.class])
	@ApiOperation('Creates a privilege')
	@Path('/create')
	PrivilegeXO create(final @NotNull @Valid PrivilegeXO privilege) {
		AuthorizationManager authorizationManager = securitySystem.getAuthorizationManager(DEFAULT_SOURCE)
		privilege.id = privilege.name; // Use name as privilege ID (note: eventually IDs should go away in favor of names)
		return convert(authorizationManager.addPrivilege(convert(privilege)));
	}

	@POST
	@Timed
	@ExceptionMetered
	//@RequiresAuthentication
	//@RequiresPermissions('nexus:privileges:update')
	@Validate(groups = [Update.class, Default.class])
	@ApiOperation('Updates a privilege')
	@Path('/update')
	PrivilegeXO update(final @NotNull @Valid PrivilegeXO privilege) {
		AuthorizationManager authorizationManager = securitySystem.getAuthorizationManager(DEFAULT_SOURCE)
		return convert(authorizationManager.updatePrivilege(convert(privilege)));
	}
	
	@POST
	@Timed
	@ExceptionMetered
	//@RequiresAuthentication
	//@RequiresPermissions('nexus:privileges:delete')
	@Validate
	@ApiOperation('Deletes a privilege, if is not readonly')
	@Path('/remove')
	void remove(@NotEmpty final String id) {
		AuthorizationManager authorizationManager = securitySystem.getAuthorizationManager(DEFAULT_SOURCE)
		if (authorizationManager.getPrivilege(id)?.readOnly) {
			throw new IllegalAccessException("Privilege [${id}] is readonly and cannot be deleted")
		}
		authorizationManager.deletePrivilege(id)
	}


	/**
	 * Convert privilege to XO.
	 */
	@PackageScope
	PrivilegeXO convert(final Privilege input) {
		return new PrivilegeXO(
				id: input.id,
				version: input.version,
				name: input.name != null ? input.name : input.id,
				description: input.description != null ? input.description : input.id,
				type: input.type,
				readOnly: input.readOnly,
				properties: Maps.newHashMap(input.properties),
				permission: input.permission
				)
	}

	/**
	 * Convert XO to privilege.
	 */
	@PackageScope
	Privilege convert(final PrivilegeXO input) {
		return new Privilege(
				id: input.id,
				version: input.version,
				name: input.name,
				description: input.description,
				type: input.type,
				properties: Maps.newHashMap(input.properties)
				)
	}
}
