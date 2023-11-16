/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp.internal.util;

import org.junit.Test;
import org.mule.extension.sftp.internal.stream.LazyStreamSupplier;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import static org.mule.extension.sftp.internal.util.StreamProxyUtil.getInputStreamFromStreamFactory;

public class StreamProxyUtilTest {

  @Test
  public void testGetInputStreamFromStreamFactory() {
    LazyStreamSupplier supplier = new LazyStreamSupplier(() -> new ByteArrayInputStream("test".getBytes()));

    InputStream proxyStream = getInputStreamFromStreamFactory(supplier);
    assertNotNull("Proxy stream should not be null", proxyStream);

    byte[] buffer = new byte[4];
    try {
      int bytesRead = proxyStream.read(buffer);
      assertEquals("Should read 4 bytes", 4, bytesRead);
      assertEquals("Read content should match", "test", new String(buffer));
    } catch (Exception e) {
      fail("Exception should not be thrown while reading from proxy stream");
    }
  }
}
