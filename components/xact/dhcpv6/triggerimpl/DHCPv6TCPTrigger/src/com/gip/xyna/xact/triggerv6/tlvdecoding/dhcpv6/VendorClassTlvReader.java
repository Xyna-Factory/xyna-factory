/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package com.gip.xyna.xact.triggerv6.tlvdecoding.dhcpv6;



import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;



/**
 * Reader for TLV:s with DOCSIS-length encoding.
 * 
 * @author Fredrik Lindberg / GIP Exyr GmbH
 */
public final class VendorClassTlvReader implements TlvReader {

  private static Logger logger = Logger.getLogger(VendorClassTlvReader.class);

  private static final int MAX_TLV_LENGTH = 254;

  private InputStream inputStream;
  private final Set<Integer> paddingTypeEncodings;

  boolean streamempty = false;


  public VendorClassTlvReader(InputStream inputStream, final Set<Integer> paddingTypeEncodings) {
    if (inputStream == null) {
      throw new IllegalArgumentException("Input stream may not be null.");
    }
    else if (paddingTypeEncodings == null) {
      throw new IllegalArgumentException("Padding type encodings may not be null.");
    }


    this.inputStream = inputStream;

    try {
      int enterprisenr = readEnterpriseNr();
      if (enterprisenr < 0) {
        streamempty = true;
      }
    }
    catch (TlvReaderException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }


    this.paddingTypeEncodings = validateAndMakeUnmodifiable(paddingTypeEncodings);

  }


  private Set<Integer> validateAndMakeUnmodifiable(final Set<Integer> paddingTypeEncodings) {
    Set<Integer> result = new HashSet<Integer>();
    for (Integer typeEncoding : paddingTypeEncodings) {
      if (typeEncoding == null) {
        throw new IllegalArgumentException("Padding type encodings contains a null type encoding..");
      }
      else if (typeEncoding < 0 || typeEncoding > 65536) {
        throw new IllegalArgumentException(
                                           "Padding type encodings contains an invalid type encoding: <" + typeEncoding + ">.");
      }
      result.add(typeEncoding);
    }
    return Collections.unmodifiableSet(result);
  }


  public void close() {
    try {
      inputStream.close();
    }
    catch (IOException e) {
      logger.warn("Failed to close input stream.");
    }
  }


  public Tlv read() throws TlvReaderException {
    /*
     * int typeEncoding = readTypeEncoding(); if (typeEncoding < 0 || streamempty) { return null; // end of stream
     * reached } else if (this.paddingTypeEncodings.contains(typeEncoding)) { return new Tlv(typeEncoding, new
     * ArrayList<Byte>()); } int length = readLength(); List<Byte> value = readValue(length); return new
     * Tlv(typeEncoding, value);
     */
    return null;
  }


  public Tlv read(int typeEncoding, int length) throws TlvReaderException {
    if (typeEncoding < 0) {
      return null; // end of stream reached
    }
    else if (this.paddingTypeEncodings.contains(typeEncoding)) {
      return new Tlv(typeEncoding, new ArrayList<Byte>());
    }
    List<Byte> value = readValue(length);
    return new Tlv(typeEncoding, value);
  }


  private int readOptionLength() throws TlvReaderException // reads seperate length of options value
  {
    int length = readByteValue() * 256 + readByteValue(); // Längenfeld 2 Byte lang
    if (length > MAX_TLV_LENGTH) {
      throw new TlvReaderException(
                                   "Expected length shorter or equal to <" + MAX_TLV_LENGTH + ">, but was: <" + length + ">.");
    }
    return length;

  }


  private int readEnterpriseNr() throws TlvReaderException {
    int enr = -1;
    try {
      enr = (inputStream.read() * 256 * 256 * 256 + inputStream.read()) * 256 * 256 + inputStream.read() * 256 + inputStream
                      .read();
    }
    catch (IOException e) {
      throw new TlvReaderException("Unexpected error.", e);
    }
    return enr;
  }


  private int readByteValue() throws TlvReaderException {
    int byteValue;
    try {
      byteValue = inputStream.read();
    }
    catch (IOException e) {
      throw new TlvReaderException("Unexpected error.", e);
    }
    if (byteValue < 0) {
      throw new TlvReaderException("End of stream reached prematurely.");
    }
    return byteValue;
  }


  private int readTypeEncoding() throws TlvReaderException {
    try {
      return (inputStream.read() * 256 + inputStream.read()); // Type 2 Byte lang

    }
    catch (IOException e) {
      throw new TlvReaderException("Unexpected error.", e);
    }
  }


  private int readLength() throws TlvReaderException {
    int length = readByteValue() * 256 + readByteValue(); // Längenfeld 2 Byte lang
    if (length > MAX_TLV_LENGTH) {
      throw new TlvReaderException(
                                   "Expected length shorter or equal to <" + MAX_TLV_LENGTH + ">, but was: <" + length + ">.");
    }
    return length;
  }


  private List<Byte> readValue(int nrBytes) throws TlvReaderException {
    List<Byte> bytes = new ArrayList<Byte>(nrBytes);
    try {
      for (int i = 0; i < nrBytes; ++i) {
        bytes.add((byte) readByteValue());
      }
    }
    catch (TlvReaderException e) {
      throw new TlvReaderException(
                                   "Read of <" + nrBytes + "> bytes failed after read of <" + bytes.size() + "> bytes.",
                                   e);
    }
    return bytes;
  }
}
