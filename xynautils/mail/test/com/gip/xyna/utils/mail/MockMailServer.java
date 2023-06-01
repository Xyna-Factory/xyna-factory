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
package com.gip.xyna.utils.mail;

import java.util.HashMap;
import java.util.Vector;

import javax.mail.Message;

/**
 * 
 * 
 */
public class MockMailServer {

   private static HashMap<String, Vector<Message>> inbox = new HashMap<String, Vector<Message>>();

   public static void addMessage(String address, Message message) {
      Vector<Message> inboxForUser = inbox.get(address);
      if (inboxForUser == null) {
         inboxForUser = new Vector<Message>();
      }
      inboxForUser.add(message);
      inbox.put(address, inboxForUser);
   }

   public static Vector<Message> getMessages(String address) {
      Vector<Message> inboxForUser = inbox.get(address);
      if (inboxForUser == null) {
         return new Vector<Message>();
      }
      return inboxForUser;
   }

   public static void clear() {
      inbox.clear();
   }
}
