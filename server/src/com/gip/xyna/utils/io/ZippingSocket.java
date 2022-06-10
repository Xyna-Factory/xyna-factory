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
package com.gip.xyna.utils.io;



import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;



public class ZippingSocket extends WrappingSocket {
  //vgl https://stackoverflow.com/questions/2374374/java-rmi-ssl-compression-impossible

  private static final boolean nowrap = true;

  private InputStream ins;
  private OutputStream outs;
  private final int buffersize;

  public ZippingSocket(Socket innerSocket, int buffersize) {
    super(innerSocket);
    this.buffersize = buffersize;
  }


  @Override
  public InputStream getInputStream() throws IOException {
    if (ins == null) {
      ins = new InflaterInputStream(super.getInputStream(), new Inflater(nowrap), buffersize) {

        public int available() throws IOException {
          if (!inf.finished() && !inf.needsInput()) {
            return 1;
          } else {
            return in.available();
          }
        }


        public void close() throws IOException {
          super.close();
          inf.end();
        }
      };
    }
    return ins;
  }


  @Override
  public OutputStream getOutputStream() throws IOException {
    if (outs == null) {
      outs = new DeflaterOutputStream(super.getOutputStream(), new Deflater(Deflater.DEFAULT_COMPRESSION, nowrap), buffersize, true) {

        @Override
        public void close() throws IOException {
          super.close();
          def.end();
        }

      };
    }
    return outs;
  }


  @Override
  public synchronized void close() throws IOException {
    OutputStream o = getOutputStream();
    o.flush();
    o.close();
    super.close();
  }

}
