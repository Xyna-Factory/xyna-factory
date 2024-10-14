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
package xact.tcp;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class StaticMessageKeepAliveHandler implements KeepAliveHandler {
  
  private final ByteBuffer message;
  
  public StaticMessageKeepAliveHandler(String message) throws UnsupportedEncodingException {
    this(message.getBytes()); // Constants.DEFAULT_ENCODING ?
  }
  
  public StaticMessageKeepAliveHandler(String message, String charsetName) throws UnsupportedEncodingException {
    this(message.getBytes(charsetName));
  }
  
  public StaticMessageKeepAliveHandler(byte[] message) {
    this(ByteBuffer.wrap(message));
  }

  public StaticMessageKeepAliveHandler(ByteBuffer message) {
    this.message = message;
  }
  
  public void keepAlive(SocketChannel channel) {
    try {
      if (channel.isOpen() && channel.isConnected()) {
        channel.write(message);
      }
    } catch (IOException e) {
      // TODO log or runtime
    }
  }
  
}
