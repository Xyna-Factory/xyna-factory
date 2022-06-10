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
package com.gip.xyna.utils.mail;

import javax.mail.Folder;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;

/**
 * 
 * 
 */
public class MockIMAPStore extends Store {

   private MockFolder folder = null;
   private String ownerAddress = null;

   public MockIMAPStore(Session session, URLName url) {
      super(session, url);
   }

   @Override
   public void connect(String host, String user, String password) {
      folder = new MockFolder(this);
      ownerAddress = user + "@" + host;
      System.out.println("MockIMAPStore:connect to host '" + host + " (user: "
            + user + ", password: " + password + ")");
   }

   public String getOwnerEmail() {
      return ownerAddress;
   }

   @Override
   public Folder getDefaultFolder() {
      return folder;
   }

   @Override
   public Folder getFolder(String arg0) {
      return getDefaultFolder();
   }

   @Override
   public Folder getFolder(URLName arg0) {
      return getDefaultFolder();
   }

   @Override
   public void close() {
      System.out.println("MockIMAPStore:close");
   }

}
