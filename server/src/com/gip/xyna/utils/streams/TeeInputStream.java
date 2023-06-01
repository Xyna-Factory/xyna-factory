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

package com.gip.xyna.utils.streams;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 *  InputStream (T-St�ck) ist ein Wrapper f�r einen InputStream, 
 *  der alle gelesenen Daten in den im Konstruktor �bergebenen OutputStream ausgibt
 */
public class TeeInputStream extends InputStream {
  
  private InputStream is;
  private OutputStream os;

  /**
   * @param is Source
   * @param os Output
   */
  public TeeInputStream( InputStream is, OutputStream os ) {
    this.is = is;
    this.os = os;
  }

  @Override
  public int read() throws IOException {
    int r = is.read();
    if( r != -1 ) {
      os.write(r);
    }
    return r;
  }

  @Override
  public int read(byte b[], int off, int len) throws IOException {
    int r = is.read(b,off,len);
    if( r >= 0 ) {
      os.write(b,off,r);
    }
    return r;
  }

}
