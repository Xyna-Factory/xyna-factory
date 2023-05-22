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

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.URLName;
import javax.mail.internet.MimeMessage;

/**
 * 
 * 
 */
public class MockTransport extends Transport {

   public MockTransport(Session arg0, URLName arg1) {
      super(arg0, arg1);
   }

   @Override
   public void sendMessage(Message message, Address[] addresses)
         throws MessagingException {
      System.out.println("Sending message '" + message.getSubject() + "'");
      for (Address address : addresses) {
         MockMailServer.addMessage(address.toString(), (MimeMessage) message);
      }
   }

   @Override
   public void connect() {
      System.out.println("MockTransport:connect");
   }

   @Override
   public void connect(String host, int port, String user, String password) {
      System.out.println("MockTransport:connect to host '" + host
            + "' using port " + port + "(user: " + user + ", password: "
            + password + ")");
   }

   @Override
   public void connect(String host, String user, String password) {
      System.out.println("MockTransport:connect to host '" + host + "' (user: "
            + user + ", password: " + password + ")");
   }

   @Override
   public void close() {
      System.out.println("MockTransport:close");
   }

}
