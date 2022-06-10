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
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.ServerSocketChannel;

public class WrappingServerSocket extends ServerSocket {
  
  private final ServerSocket innerSocket;
  
  public WrappingServerSocket(ServerSocket innerSocket) throws IOException {
    this.innerSocket = innerSocket;
    super.close();
  }

  @Override
  public Socket accept() throws IOException {
    return innerSocket.accept();
  }

  @Override
  public void bind(SocketAddress endpoint, int backlog) throws IOException {
    innerSocket.bind(endpoint, backlog);
  }

  @Override
  public void bind(SocketAddress endpoint) throws IOException {
    innerSocket.bind(endpoint);
  }

  @Override
  public void close() throws IOException {
    innerSocket.close();
  }

  @Override
  public ServerSocketChannel getChannel() {
    return innerSocket.getChannel();
  }

  @Override
  public InetAddress getInetAddress() {
    return innerSocket.getInetAddress();
  }

  @Override
  public int getLocalPort() {
    return innerSocket.getLocalPort();
  }

  @Override
  public SocketAddress getLocalSocketAddress() {
    return innerSocket.getLocalSocketAddress();
  }

  @Override
  public synchronized int getReceiveBufferSize() throws SocketException {
    return innerSocket.getReceiveBufferSize();
  }

  @Override
  public boolean getReuseAddress() throws SocketException {
    return innerSocket.getReuseAddress();
  }

  @Override
  public synchronized int getSoTimeout() throws IOException {
    return innerSocket.getSoTimeout();
  }

  @Override
  public boolean isBound() {
    return innerSocket.isBound();
  }

  @Override
  public boolean isClosed() {
    return innerSocket.isClosed();
  }

  @Override
  public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
    innerSocket.setPerformancePreferences(connectionTime, latency, bandwidth);
  }

  @Override
  public synchronized void setReceiveBufferSize(int size) throws SocketException {
    innerSocket.setReceiveBufferSize(size);
  }

  @Override
  public void setReuseAddress(boolean on) throws SocketException {
    innerSocket.setReuseAddress(on);
  }

  @Override
  public synchronized void setSoTimeout(int timeout) throws SocketException {
    innerSocket.setSoTimeout(timeout);
  }

  @Override
  public String toString() {
    return innerSocket.toString();
  }

}
