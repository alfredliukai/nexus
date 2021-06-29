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
package org.sonatype.nexus.blobstore.s3.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Persistent properties file stored in AWS S3.
 *
 * @since 3.6.1
 */
public class S3PropertiesFile
    extends Properties
{
  private static final Logger log = LoggerFactory.getLogger(S3PropertiesFile.class);

  private final AmazonS3 s3;

  private final String bucket;

  private final String key;

  public S3PropertiesFile(final AmazonS3 s3, final String bucket, final String key) {
    this.s3 = checkNotNull(s3);
    this.bucket = checkNotNull(bucket);
    this.key = checkNotNull(key);
  }

  public void load() throws IOException {
    log.debug("Loading: {}/{}", bucket, key);

    try (S3Object object = s3.getObject(bucket, key)) {
      try (InputStream inputStream = object.getObjectContent()) {
        load(inputStream);
      }
    }
  }

  public void store() throws IOException {
    log.debug("Storing: {}/{}", bucket, key);

    ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();
    store(bufferStream, null);
    byte[] buffer = bufferStream.toByteArray();

    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentLength(buffer.length);
    s3.putObject(bucket, key, new ByteArrayInputStream(buffer), metadata);
  }

  public boolean exists(){
	  /**
	   * hulk上的s3对于doesObjectExist接口的实现是这么一个逻辑
	   * 如果这个bucket不属于你，现在的代码是直接返回的403，就会存在抛异常的情况，所以判断逻辑我们进行如下处理
	   */
	  try{
		  return s3.doesObjectExist(bucket, key);
	  }catch(Exception e){
		  return false;
	  }
  }

  public void remove() throws IOException {
    s3.deleteObject(bucket, key);
  }

  public String toString() {
    return getClass().getSimpleName() + "{" +
        "bucket=" + bucket +
        ", key=" + key +
        '}';
  }

  @Override
  public boolean equals(Object object) {
    if (object instanceof S3PropertiesFile) {
      S3PropertiesFile other = (S3PropertiesFile) object;
      return
        s3.equals(other.s3) &&
        bucket.equals(other.bucket) &&
        key.equals(other.key) &&
        super.equals(object);
    }
    else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return
      s3.hashCode() +
      bucket.hashCode() +
      key.hashCode() +
      super.hashCode();
  }
}
