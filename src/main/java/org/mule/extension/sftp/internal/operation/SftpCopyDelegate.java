/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.operation;

import org.mule.extension.sftp.api.FileAttributes;
import org.mule.extension.sftp.internal.config.FileConnectorConfig;

import java.net.URI;

/**
 * A delegate object for copying files
 *
 * @since 1.0
 */
@FunctionalInterface
public interface SftpCopyDelegate {

  /**
   * Performs the copy operation
   * 
   * @param config    the config which is parameterizing this operation
   * @param source    the attributes which describes the source file
   * @param targetUri the target uri
   * @param overwrite whether to overwrite the target file if it already exists
   */
  void doCopy(FileConnectorConfig config, FileAttributes source, URI targetUri, boolean overwrite);
}
