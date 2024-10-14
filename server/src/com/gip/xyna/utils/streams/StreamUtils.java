/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 *
 */
public class StreamUtils {

  private static final int BUFFERSIZE = 8*1024; //default in Buffered{In,Out}putStream
  
  public interface BufferWorthy { //Marker Interface, damit die Streams beim Kopieren gebuffert werden
    
  }

  /**
   * Kopieren der Daten im InputStream in den OutputStream
   * @param is
   * @param os
   * @throws IOException
   */
  public static void copy(InputStream is, OutputStream os) throws IOException {
    if (is instanceof FileInputStream || is instanceof BufferWorthy) {
      is = new BufferedInputStream(is);
    }
    if (os instanceof FileOutputStream || os instanceof BufferWorthy) {
      os = new BufferedOutputStream(os);
    }
    byte[] buffer = new byte[BUFFERSIZE];
    int bytes = is.read(buffer);
    while( bytes >=0 ) {
      os.write(buffer,0,bytes);
      bytes = is.read(buffer);
    }
    os.flush();
  }
  
}
