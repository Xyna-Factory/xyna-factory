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
package com.gip.xyna.utils.mail;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

/**
 * 
 * 
 */
public class DummySSLSocketFactory extends SSLSocketFactory {
   private SSLSocketFactory factory;

   /**
    * DummySSLSocketFactory - create a very simple SSLSocket this class is not
    * very secure - please replace by a more sophisticated one. Same holds for
    * DummyTrustManager. Both accept/do their job without checking the input or
    * goal of their invocation - that is why they are called "Dummy".
    * 
    */
   public DummySSLSocketFactory() {
      System.out.println("DummySocketFactory instantiated");
      try {
         SSLContext sslcontext = SSLContext.getInstance("TLS");
         sslcontext.init(
               null, // No KeyManager required
               new TrustManager[] { new DummyTrustManager() },
               new java.security.SecureRandom());
         factory = (SSLSocketFactory) sslcontext.getSocketFactory();
      } catch (Exception ex) {
         ex.printStackTrace();
      }
   }

   public static SocketFactory getDefault() {
      return new DummySSLSocketFactory();
   }

   public Socket createSocket(Socket socket, String s, int i, boolean flag)
         throws IOException {
      return factory.createSocket(socket, s, i, flag);
   }

   public Socket createSocket(InetAddress inaddr, int i, InetAddress inaddr1,
         int j) throws IOException {
      return factory.createSocket(inaddr, i, inaddr1, j);
   }

   public Socket createSocket(InetAddress inaddr, int i) throws IOException {
      return factory.createSocket(inaddr, i);
   }

   public Socket createSocket(String s, int i, InetAddress inaddr, int j)
         throws IOException {
      return factory.createSocket(s, i, inaddr, j);
   }

   public Socket createSocket(String s, int i) throws IOException {
      return factory.createSocket(s, i);
   }

   public Socket createSocket() throws IOException {
      return factory.createSocket();
   }

   public String[] getDefaultCipherSuites() {
      return factory.getSupportedCipherSuites();
   }

   public String[] getSupportedCipherSuites() {
      return factory.getSupportedCipherSuites();
   }
}
