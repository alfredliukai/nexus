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
package org.sonatype.nexus.repository.rest.api

import java.util.Map

import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern

import org.hibernate.validator.constraints.NotBlank
import org.hibernate.validator.constraints.NotEmpty
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.config.UniqueRepositoryName
import org.sonatype.nexus.validation.constraint.NamePatternConstants
import org.sonatype.nexus.validation.group.Create

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.transform.builder.Builder

/**
 * Repository transfer object for REST APIs.
 *
 * @since 3.9
 */
@CompileStatic
@Builder
@ToString(includePackage = false, includeNames = true)
@EqualsAndHashCode
class RepositoryXO
{
  @Pattern(regexp = NamePatternConstants.REGEX, message = NamePatternConstants.MESSAGE)
  @NotEmpty
  @UniqueRepositoryName(groups = Create)
  String name

  String format

  String type

  String url
  
  @NotBlank(groups = Create)
  String recipe

  @NotNull
  Boolean online

  @NotEmpty
  Map<String, Map<String, Object>> attributes


  RepositoryStatusXO status

  static RepositoryXO fromRepository(final Repository repository) {
    return builder()
        .name(repository.getName())
        .type(repository.getType().getValue())
        .format(repository.getFormat().getValue())
		.online(repository.getConfiguration().isOnline())
		.recipe(repository.getConfiguration().getRecipeName())
		.attributes(repository.getConfiguration().copy().attributes)
        .url(repository.getUrl())
		.status(new RepositoryStatusXO( 
			repositoryName: repository.name,
			online: repository.configuration.online))
        .build()
  }
}
