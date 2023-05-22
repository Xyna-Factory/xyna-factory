/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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

import java.net.SocketAddress;
import java.nio.channels.Selector;
import java.util.concurrent.TimeUnit;


public class SocketChannelCreationParameter {
  
  SocketAddress address;
  String name;
  KeepAliveHandler keepAliveHandler;
  long keepAliveInterval;
  TimeUnit keepAliveUnit;
  Selector selector; // TODO several selectors with several ops each?
  int ops;
  
  private SocketChannelCreationParameter() {
  }
  
  public static SocketChannelCreationParameter create() {
    return new SocketChannelCreationParameter();
  }
  
  public SocketChannelCreationParameter address(SocketAddress address) {
    this.address = address;
    return this;
  }
  
  public SocketChannelCreationParameter keepAlive(KeepAliveHandler keepAliveHandler, long keepAliveInterval, TimeUnit keepAliveUnit) {
    this.keepAliveHandler = keepAliveHandler;
    this.keepAliveInterval = keepAliveInterval;
    this.keepAliveUnit = keepAliveUnit;
    return this;
  }
  
  public SocketChannelCreationParameter name(String name) {
    this.name = name;
    return this;
  }
  
  public SocketChannelCreationParameter selector(Selector selector, int operationCode) {
    this.selector = selector;
    this.ops = operationCode;
    return this;
  }
  
  boolean hasKeepAliveHandling() {
    return keepAliveHandler != null;
  }
  
  boolean hasSelector() {
    return selector != null;
  }
  
  void validate() throws IllegalArgumentException {
    if (address == null) {
      throw new IllegalArgumentException("SocketAddress has to be set");
    }
    if (name == null) {
      throw new IllegalArgumentException("Managed socket needs a name");
    }
  }
  
}
