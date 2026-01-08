/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 */
package com.gip.xyna.xact.trigger.tlvdecoding.radius;



import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;



/**
 * Reader for TLV:s with DOCSIS-length encoding.
 *
 * @author Fredrik Lindberg / GIP Exyr GmbH
 */
public final class RadiusTlvReader implements TlvReader {

  private static Logger logger = Logger.getLogger(RadiusTlvReader.class);

  private static final int MAX_TLV_LENGTH = 255;

  private final InputStream inputStream;


  public RadiusTlvReader(final InputStream inputStream) {
    if (inputStream == null) {
      throw new IllegalArgumentException("Input stream may not be null.");
    }
    this.inputStream = inputStream;
  }


  public void close() {
    try {
      inputStream.close();
    } catch (IOException e) {
      logger.warn("Failed to close input stream.");
    }
  }


  public Tlv read() throws TlvReaderException {
    int typeEncoding = readTypeEncoding();
    while (typeEncoding == 0) {
      typeEncoding = readTypeEncoding();
    }
    if (typeEncoding < 0) {
      return null; // end of stream reached
    }
    int length = readLength() - 2;//bei RADIUS sind Typ und Laenge im Laengenwert enthalten und muessen daher abgezogen werden
    List<Byte> value = readValue(length);
    return new Tlv(typeEncoding, value);
  }


  private int readByteValue() throws TlvReaderException {
    int byteValue;
    try {
      byteValue = inputStream.read();
    } catch (IOException e) {
      throw new TlvReaderException("Unexpected error.", e);
    }
    if (byteValue < 0) {
      throw new TlvReaderException("End of stream reached prematurely.");
    }
    return byteValue;
  }


  private int readTypeEncoding() throws TlvReaderException {
    try {
      return inputStream.read();
    } catch (IOException e) {
      throw new TlvReaderException("Unexpected error.", e);
    }
  }


  private int readLength() throws TlvReaderException {
    int length = readByteValue();
    if (length > MAX_TLV_LENGTH) {
      throw new TlvReaderException("Expected length shorter or equal to <" + MAX_TLV_LENGTH + ">, but was: <" + length + ">.");
    }
    return length;
  }


  private List<Byte> readValue(int nrBytes) throws TlvReaderException {
    List<Byte> bytes = new ArrayList<Byte>(nrBytes);
    try {
      for (int i = 0; i < nrBytes; ++i) {
        bytes.add((byte) readByteValue());
      }
    } catch (TlvReaderException e) {
      throw new TlvReaderException("Read of <" + nrBytes + "> bytes failed after read of <" + bytes.size() + "> bytes.", e);
    }
    return bytes;
  }
}
