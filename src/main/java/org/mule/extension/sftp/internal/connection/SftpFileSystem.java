/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.connection;

import org.mule.extension.sftp.api.*;
import org.mule.extension.sftp.api.command.*;
import org.mule.extension.sftp.api.lock.URLPathLock;
import org.mule.extension.sftp.api.lock.UriLock;
import org.mule.extension.sftp.internal.command.*;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lock.LockFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.mule.extension.sftp.api.exceptions.FileError.DISCONNECTED;
import static org.mule.extension.sftp.api.util.UriUtils.createUri;
import static org.mule.extension.sftp.internal.SftpUtils.normalizePath;
import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

/**
 * Implementation of {@link AbstractFileSystem} for files residing on a SFTP server
 *
 * @since 1.0
 */
public class SftpFileSystem extends AbstractExternalFileSystem {

  public static final String ROOT = "/";
  public static final Logger LOGGER = LoggerFactory.getLogger(SftpFileSystem.class);

  private static String resolveBasePath(String basePath) {
    if (isBlank(basePath)) {
      return "";
    }
    return createUri(ROOT, basePath).getPath();
  }

  protected final SftpClient client;
  protected final CopyCommand copyCommand;
  protected final CreateDirectoryCommand createDirectoryCommand;
  protected final DeleteCommand deleteCommand;
  protected final SftpListCommand listCommand;
  protected final MoveCommand moveCommand;
  protected final SftpReadCommand readCommand;
  protected final RenameCommand renameCommand;
  protected final WriteCommand writeCommand;
  private final LockFactory lockFactory;

  public SftpFileSystem(SftpClient client, String basePath, LockFactory lockFactory) {
    super(resolveBasePath(basePath));
    this.client = client;
    this.lockFactory = lockFactory;

    copyCommand = new SftpCopyCommand(this, client);
    createDirectoryCommand = new SftpCreateDirectoryCommand(this, client);
    deleteCommand = new SftpDeleteCommand(this, client);
    moveCommand = new SftpMoveCommand(this, client);
    readCommand = new SftpReadCommand(this, client);
    listCommand = new SftpListCommand(this, client, (SftpReadCommand) readCommand);
    renameCommand = new SftpRenameCommand(this, client);
    writeCommand = new SftpWriteCommand(this, client);
    client.setOwner(this);
  }

  public void disconnect() {
    client.disconnect();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void changeToBaseDir() {
    if (!isBlank(getBasePath())) {
      client.changeWorkingDirectory(getBasePath());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getBasePath() {
    return normalizePath(super.getBasePath());
  }

  public InputStream retrieveFileContent(FileAttributes filePayload) {
    return client.getFileContent(filePayload.getPath());
  }

  public SftpFileAttributes readFileAttributes(String filePath) {
    return getReadCommand().readAttributes(filePath);
  }

  protected boolean isConnected() {
    return client.isConnected();
  }

  /**
   * {@inheritDoc}
   */
  protected UriLock createLock(URI uri) {
    return new URLPathLock(toURL(uri), lockFactory);
  }

  private URL toURL(URI uri) {
    try {
      return new URL("ftp", client.getHost(), client.getPort(), uri != null ? uri.getPath() : EMPTY);
    } catch (MalformedURLException e) {
      throw new MuleRuntimeException(createStaticMessage("Could not get URL for SFTP server"), e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CopyCommand getCopyCommand() {
    return copyCommand;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CreateDirectoryCommand getCreateDirectoryCommand() {
    return createDirectoryCommand;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DeleteCommand getDeleteCommand() {
    return deleteCommand;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SftpListCommand getListCommand() {
    return listCommand;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MoveCommand getMoveCommand() {
    return moveCommand;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SftpReadCommand getReadCommand() {
    return readCommand;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RenameCommand getRenameCommand() {
    return renameCommand;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WriteCommand getWriteCommand() {
    return writeCommand;
  }

  /**
   * Validates the underlying connection to the remote server
   *
   * @return a {@link ConnectionValidationResult}
   */
  public ConnectionValidationResult validateConnection() {
    if (!isConnected()) {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Connection validation failed.");
      }
      return failure("Connection is stale", new SftpConnectionException("Connection is stale", DISCONNECTED));
    }
    try {
      changeToBaseDir();
    } catch (Exception e) {
      LOGGER.error("Error occurred while changing to base directory {}", getBasePath(), e);
      return failure("Configured workingDir is unavailable", e);
    }
    return success();
  }

  public SftpClient getClient() {
    return client;
  }
}
