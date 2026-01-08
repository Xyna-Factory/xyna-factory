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



import java.io.ByteArrayInputStream;
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
public final class VendorTlvReader implements TlvReader {

  private static Logger logger = Logger.getLogger(VendorTlvReader.class);

  private static final int MAX_TLV_LENGTH = 254;

  private InputStream inputStream;
  private final Set<Integer> paddingTypeEncodings;


  public VendorTlvReader(InputStream inputStream, final Set<Integer> paddingTypeEncodings) {
    if (inputStream == null) {
      throw new IllegalArgumentException("Input stream may not be null.");
    } else if (paddingTypeEncodings == null) {
      throw new IllegalArgumentException("Padding type encodings may not be null.");
    }

    this.inputStream = this.convertStream(inputStream);
    this.paddingTypeEncodings = validateAndMakeUnmodifiable(paddingTypeEncodings);
  }


  // Hexformat in normalen Bytestream umwandeln
  private InputStream convertStream(InputStream i) {
    List<Byte> head = new ArrayList<Byte>();
    List<Byte> tmp = new ArrayList<Byte>();
    List<Byte> res = new ArrayList<Byte>();

    int a;
    boolean header = true; // bis : abschneiden
    boolean docsis3 = false;

    try {
      while ((a = i.read()) != -1) {
        if (header == false) {
          tmp.add((byte) a);
          if (docsis3)
            throw new TlvReaderException("no arguments expected after : with docsis3.0");
        }

        if (a == 58) {
          header = false;
          if ((head.size() == 9) && head.get(6) == 51 && head.get(7) == 46 && head.get(8) == 48) // TODO noch etwas
                                                                                                 // ungenau
          {
            docsis3 = true;
          }
        }
        if (header) {
          head.add((byte) a);
        }
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (TlvReaderException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }


    String conv;
    int x, y;

    for (int z = 0; z < tmp.size(); z = z + 2) {
      x = (int) tmp.get(z);
      y = (int) tmp.get(z + 1);

      conv = String.valueOf((char) x) + String.valueOf((char) y);
      res.add((byte) Integer.parseInt(conv, 16));
    }

    byte[] arg = new byte[res.size()];
    for (int z = 0; z < arg.length; z++) {
      arg[z] = res.get(z);
    }

    InputStream out = new ByteArrayInputStream(arg);
    return out;
  }


  private Set<Integer> validateAndMakeUnmodifiable(final Set<Integer> paddingTypeEncodings) {
    Set<Integer> result = new HashSet<Integer>();
    for (Integer typeEncoding : paddingTypeEncodings) {
      if (typeEncoding == null) {
        throw new IllegalArgumentException("Padding type encodings contains a null type encoding..");
      } else if (typeEncoding < 0 || typeEncoding > 255) {
        throw new IllegalArgumentException("Padding type encodings contains an invalid type encoding: <" + typeEncoding + ">.");
      }
      result.add(typeEncoding);
    }
    return Collections.unmodifiableSet(result);
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
    if (typeEncoding < 0) {
      return null; // end of stream reached
    } else if (this.paddingTypeEncodings.contains(typeEncoding)) {
      return new Tlv(typeEncoding, new ArrayList<Byte>());
    }
    int length = readLength();
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
