/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.api.exceptions;

import org.mule.extension.sftp.internal.BaseFileSystemOperations;
import org.mule.extension.sftp.internal.FileConnectorConfig;
import org.mule.extension.sftp.internal.connection.FileSystem;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;
import static org.mule.extension.sftp.api.exceptions.FileError.*;

/**
 * Errors that can be thrown in the
 * {@link BaseFileSystemOperations#doRead(FileConnectorConfig, FileSystem, String, MediaType, boolean)} operation.
 *
 * @since 1.0
 */
public class FileReadErrorTypeProvider implements ErrorTypeProvider {

  @Override
  public Set<ErrorTypeDefinition> getErrorTypes() {
    return unmodifiableSet(new HashSet<>(asList(ILLEGAL_PATH, ACCESS_DENIED, FILE_LOCK)));
  }
}

