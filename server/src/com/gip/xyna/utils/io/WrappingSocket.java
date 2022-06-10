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
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;



public class WrappingSocket extends Socket {

  final Socket innerSocket;


  public WrappingSocket(Socket innerSocket) {
    this.innerSocket = innerSocket;
    try {
      //leerer konstruktor erzeugt unbenötigten kram in oberklasse...
      super.close();
    } catch (IOException e) {
      throw new RuntimeException();
    }
  }


  @Override
  public void bind(SocketAddress bindpoint) throws IOException {
    innerSocket.bind(bindpoint);
  }


  @Override
  public synchronized void close() throws IOException {
    innerSocket.close();
  }


  @Override
  public void connect(SocketAddress endpoint, int timeout) throws IOException {
    innerSocket.connect(endpoint, timeout);
  }


  @Override
  public void connect(SocketAddress endpoint) throws IOException {
    innerSocket.connect(endpoint);
  }


  @Override
  public SocketChannel getChannel() {
    return innerSocket.getChannel();
  }


  @Override
  public InetAddress getInetAddress() {
    return innerSocket.getInetAddress();
  }


  @Override
  public InputStream getInputStream() throws IOException {
    return innerSocket.getInputStream();
  }


  @Override
  public boolean getKeepAlive() throws SocketException {
    return innerSocket.getKeepAlive();
  }


  @Override
  public InetAddress getLocalAddress() {
    return innerSocket.getLocalAddress();
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
  public boolean getOOBInline() throws SocketException {
    return innerSocket.getOOBInline();
  }


  @Override
  public OutputStream getOutputStream() throws IOException {
    return innerSocket.getOutputStream();
  }


  @Override
  public int getPort() {
    return innerSocket.getPort();
  }


  @Override
  public synchronized int getReceiveBufferSize() throws SocketException {
    return innerSocket.getReceiveBufferSize();
  }


  @Override
  public SocketAddress getRemoteSocketAddress() {
    return innerSocket.getRemoteSocketAddress();
  }


  @Override
  public boolean getReuseAddress() throws SocketException {
    return innerSocket.getReuseAddress();
  }


  @Override
  public synchronized int getSendBufferSize() throws SocketException {
    return innerSocket.getSendBufferSize();
  }


  @Override
  public int getSoLinger() throws SocketException {
    return innerSocket.getSoLinger();
  }


  @Override
  public synchronized int getSoTimeout() throws SocketException {
    return innerSocket.getSoTimeout();
  }


  @Override
  public boolean getTcpNoDelay() throws SocketException {
    return innerSocket.getTcpNoDelay();
  }


  @Override
  public int getTrafficClass() throws SocketException {
    return innerSocket.getTrafficClass();
  }


  @Override
  public boolean isBound() {
    return innerSocket.isBound();
  }


  @Override
  public boolean isClosed(){
    return innerSocket.isClosed();
  }


  @Override
  public boolean isConnected() {
    return innerSocket.isConnected();
  }


  @Override
  public boolean isInputShutdown() {
    return innerSocket.isInputShutdown();
  }


  @Override
  public boolean isOutputShutdown() {
    return innerSocket.isOutputShutdown();
  }


  @Override
  public void sendUrgentData(int data) throws IOException {
    innerSocket.sendUrgentData(data);
  }


  @Override
  public void setKeepAlive(boolean on) throws SocketException {
    innerSocket.setKeepAlive(on);
  }


  @Override
  public void setOOBInline(boolean on) throws SocketException {
    innerSocket.setOOBInline(on);
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
  public synchronized void setSendBufferSize(int size) throws SocketException {
    innerSocket.setSendBufferSize(size);
  }


  @Override
  public void setSoLinger(boolean on, int linger) throws SocketException {
    innerSocket.setSoLinger(on, linger);
  }


  @Override
  public synchronized void setSoTimeout(int timeout) throws SocketException {
    innerSocket.setSoTimeout(timeout);
  }


  @Override
  public void setTcpNoDelay(boolean on) throws SocketException {
    innerSocket.setTcpNoDelay(on);
  }


  @Override
  public void setTrafficClass(int tc) throws SocketException {
    innerSocket.setTrafficClass(tc);
  }


  @Override
  public void shutdownInput() throws IOException {
    innerSocket.shutdownInput();
  }


  @Override
  public void shutdownOutput() throws IOException {
    innerSocket.shutdownOutput();
  }


  @Override
  public String toString() {
    return innerSocket.toString();
  }


}
