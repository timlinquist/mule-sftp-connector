/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.api;

import org.mule.extension.sftp.api.connection.ConnectionSource;
import org.mule.extension.sftp.api.connection.ManagerBasedConnectionSource;
import org.mule.extension.sftp.api.connection.StaticConnectionSource;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.slf4j.Logger;

import java.io.InputStream;
import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Abstraction that extends from {@link AbstractFileInputStreamSupplier}, this supplier can be used when you need a
 * {@link org.mule.extension.sftp.api.FileSystem} in order to get the file attributes and the file content. This class implements
 * the logic to retrieve and release the needed fileSystems, and the developer will be given the fileSystem to get the attributes
 * and contents without to worry about how to get and retrieve connections.
 *
 * @param <T> the type used to retrieve file contents and attributes
 */
public abstract class AbstractConnectedFileInputStreamSupplier<T extends org.mule.extension.sftp.api.FileSystem>
    extends AbstractFileInputStreamSupplier {

  private static final Logger LOGGER = getLogger(AbstractConnectedFileInputStreamSupplier.class);
  private ConnectionSource<T> connectionSource;
  private boolean contentProvided = false;
  private boolean contentConnectionReleased = false;

  public AbstractConnectedFileInputStreamSupplier(org.mule.extension.sftp.api.FileAttributes attributes,
                                                  Long timeBetweenSizeCheck,
                                                  ConnectionSource<T> connectionSource) {
    super(attributes, timeBetweenSizeCheck);
    this.connectionSource = connectionSource;
  }

  public AbstractConnectedFileInputStreamSupplier(org.mule.extension.sftp.api.FileAttributes attributes,
                                                  Long timeBetweenSizeCheck, T fileSystem) {
    this(attributes, timeBetweenSizeCheck, new StaticConnectionSource<>(fileSystem));
  }

  public AbstractConnectedFileInputStreamSupplier(org.mule.extension.sftp.api.FileAttributes attributes,
                                                  ConnectionManager connectionManager,
                                                  Long timeBetweenSizeCheck, FileConnectorConfig config) {
    this(attributes, timeBetweenSizeCheck, new ManagerBasedConnectionSource<>(config, connectionManager));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected final org.mule.extension.sftp.api.FileAttributes getUpdatedAttributes() {
    try {
      T fileSystem = connectionSource.getConnection();
      org.mule.extension.sftp.api.FileAttributes updatedFileAttributes = getUpdatedAttributes(fileSystem);
      releaseConnection();
      if (updatedFileAttributes == null) {
        LOGGER.error(String.format(FILE_NO_LONGER_EXISTS_MESSAGE, attributes.getPath()));
      }
      return updatedFileAttributes;
    } catch (ConnectionException e) {
      throw new MuleRuntimeException(createStaticMessage("Could not obtain connection to fetch file " + attributes.getPath()),
                                     e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected final InputStream getContentInputStream() {
    try {
      InputStream content = getContentInputStream(connectionSource.getConnection());
      contentProvided = true;
      return content;
    } catch (MuleRuntimeException e) {
      if (fileWasDeleted(e)) {
        onFileDeleted(e);
      }
      throw e;
    } catch (ConnectionException e) {
      throw new MuleRuntimeException(createStaticMessage("Could not obtain connection to fetch file " + attributes.getPath()),
                                     e);
    }
  }

  /**
   * If the content of the file was retrieved, this method will release the connection used to get that content.
   */
  public void releaseConnectionUsedForContentInputStream() {
    if (contentProvided && !contentConnectionReleased) {
      releaseConnection();
      contentConnectionReleased = true;
    }
  }

  private void releaseConnection() {
    connectionSource.releaseConnection();
  }

  /**
   * If the content of the file was retrieved, this method will return the {@link org.mule.extension.sftp.api.FileSystem} used to
   * retrieve that content.
   *
   * @return an {@link Optional} that contains the {@link org.mule.extension.sftp.api.FileSystem} or {@link Optional#empty()}
   * @throws ConnectionException
   */
  public Optional<T> getConnectionUsedForContentInputStream() throws ConnectionException {
    return contentProvided && !contentConnectionReleased ? of(connectionSource.getConnection()) : empty();
  }

  /**
   * Gets the updated attributes of the file.
   *
   * @param fileSystem the {@link org.mule.extension.sftp.api.FileSystem} to be used to gather the updated attributes
   * @return the updated attributes according to the path of the variable attributes passed in the constructor
   */
  protected abstract FileAttributes getUpdatedAttributes(T fileSystem);

  /**
   * Gets the {@link InputStream} of the file described by the attributes passed to the constructor
   *
   * @param fileSystem the {@link FileSystem} to be used to get the content of the file
   * @return the {@link InputStream} of the file
   */
  protected abstract InputStream getContentInputStream(T fileSystem);

  /**
   * This method will be called when a {@link MuleRuntimeException} is thrown while retrieving the content of the file and its
   * implementation will return whether the file was deleted or not based on the exception thrown.
   *
   * @param e the thrown {@link MuleRuntimeException}
   * @return whether the exception implies that the file to be read was deleted
   */
  protected abstract boolean fileWasDeleted(MuleRuntimeException e);

}
