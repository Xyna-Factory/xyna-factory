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

import java.util.Arrays;
import java.util.Vector;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;

/**
 * 
 * 
 */
public class MockFolder extends Folder {

   private Store store = null;

   private Vector<Message> messages = null;

   protected MockFolder(Store arg0) {
      super(arg0);
      store = arg0;
   }

   @Override
   public void appendMessages(Message[] arg0) {
      messages.addAll(Arrays.asList(arg0));
   }

   @Override
   public void close(boolean arg0) {
      System.out.println("MockFolder:close");
   }

   @Override
   public boolean create(int arg0) {
      throw new UnsupportedOperationException("MockFolder.create not supported");
   }

   @Override
   public boolean delete(boolean arg0) {
      throw new UnsupportedOperationException("MockFolder.delete not supported");
   }

   @Override
   public boolean exists() {
      return true;
   }

   @Override
   public Message[] expunge() {
      return new Message[0];
   }

   @Override
   public Folder getFolder(String arg0) {
      return this;
   }

   @Override
   public String getFullName() {
      return "INBOX";
   }

   @Override
   public Message getMessage(int arg0) throws MessagingException {
      try {
         return messages.get(arg0);
      } catch (ArrayIndexOutOfBoundsException e) {
         throw new MessagingException(e.getMessage());
      }
   }

   @Override
   public Message[] getMessages() {
      return messages.toArray(new Message[] {});
   }

   @Override
   public int getMessageCount() {
      return messages.size();
   }

   @Override
   public String getName() {
      return "INBOX";
   }

   @Override
   public Folder getParent() {
      throw new UnsupportedOperationException(
            "MockFolder.getParent not supported");
   }

   @Override
   public Flags getPermanentFlags() {
      throw new UnsupportedOperationException(
            "MockFolder.getPermantenFlags not supported");
   }

   @Override
   public char getSeparator() {
      throw new UnsupportedOperationException(
            "MockFolder.getSeparator not supported");
   }

   @Override
   public int getType() {
      return Folder.HOLDS_MESSAGES;
   }

   @Override
   public boolean hasNewMessages() {
      return (messages.size() > 0);
   }

   @Override
   public boolean isOpen() {
      return (((MockIMAPStore) getStore()).getOwnerEmail() != null);
   }

   @Override
   public Folder[] list(String arg0) {
      return new Folder[] { this };
   }

   @Override
   public void open(int arg0) {
      String owner = ((MockIMAPStore) store).getOwnerEmail();
      messages = MockMailServer.getMessages(owner);
      System.out.println("MockFolder:open");
   }

   @Override
   public boolean renameTo(Folder arg0) {
      throw new UnsupportedOperationException(
            "MockFolder.renameTo not supported");
   }

}
