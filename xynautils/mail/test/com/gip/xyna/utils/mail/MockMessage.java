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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.Multipart;

public class MockMessage extends Message {

   private Vector<Address> froms = new Vector<Address>();

   private HashMap<RecipientType, Vector<Address>> recipient = new HashMap<RecipientType, Vector<Address>>();

   private String body = null;

   private String subject = null;

   @Override
   public void addFrom(Address[] arg0) {
      froms.addAll(Arrays.asList(arg0));
   }

   @Override
   public void addRecipients(RecipientType arg0, Address[] arg1) {
      Vector<Address> recipientForType = recipient.get(arg0);
      if (recipientForType == null) {
         recipientForType = new Vector<Address>();
      }
      recipientForType.addAll(Arrays.asList(arg1));
      recipient.put(arg0, recipientForType);
   }

   @Override
   public Flags getFlags() {
      throw new UnsupportedOperationException(
            "MockMessage.getFlags is not supported");
   }

   @Override
   public Address[] getFrom() {
      return froms.toArray(new Address[0]);
   }

   @Override
   public Date getReceivedDate() {
      throw new UnsupportedOperationException(
            "MockMessage.getReceivedData is not supported");
   }

   @Override
   public Address[] getAllRecipients() {
      Set<RecipientType> recipientTypes = recipient.keySet();
      Vector<Address> allRecipients = new Vector<Address>();
      Iterator<RecipientType> iter = recipientTypes.iterator();
      while (iter.hasNext()) {
         allRecipients.addAll(recipient.get(iter.next()));
      }
      return allRecipients.toArray(new Address[0]);
   }

   @Override
   public Address[] getRecipients(RecipientType arg0) {
      return recipient.get(arg0).toArray(new Address[0]);
   }

   @Override
   public Date getSentDate() {
      throw new UnsupportedOperationException(
            "MockMessage.getSentData is not supported");
   }

   @Override
   public String getSubject() {
      return subject;
   }

   @Override
   public Message reply(boolean arg0) {
      throw new UnsupportedOperationException(
            "MockMessage.reply is not supported");
   }

   @Override
   public void saveChanges() {
      throw new UnsupportedOperationException(
            "MockMessage.saveChanges is not supported");
   }

   @Override
   public void setFlags(Flags arg0, boolean arg1) {
      throw new UnsupportedOperationException(
            "MockMessage.setFlags is not supported");
   }

   @Override
   public void setFrom() {
      throw new UnsupportedOperationException(
            "MockMessage.setFrom is not supported");
   }

   @Override
   public void setFrom(Address arg0) {
      froms.add(arg0);
   }

   @Override
   public void setRecipient(RecipientType arg0, Address arg1) {
      Vector<Address> v = new Vector<Address>();
      v.add(arg1);
      recipient.put(arg0, v);
   }

   @Override
   public void setRecipients(RecipientType arg0, Address[] arg1) {
      Vector<Address> v = new Vector<Address>();
      v.addAll(Arrays.asList(arg1));
      recipient.put(arg0, v);
   }

   @Override
   public void setSentDate(Date arg0) {
      throw new UnsupportedOperationException(
            "MockMessage.setSentData is not supported");
   }

   @Override
   public void setSubject(String arg0) {
      subject = arg0;
   }

   public void addHeader(String arg0, String arg1) {
      throw new UnsupportedOperationException(
            "MockMessage.addHeader is not supported");
   }

   @SuppressWarnings("unchecked")
   public Enumeration getAllHeaders() {
      throw new UnsupportedOperationException(
            "MockMessage.getAllHeaders is not supported");
   }

   public Object getContent() {
      return body;
   }

   public String getContentType() {
      throw new UnsupportedOperationException(
            "MockMessage.getContentType is not supported");
   }

   public DataHandler getDataHandler() {
      throw new UnsupportedOperationException(
            "MockMessage.getDataHandler is not supported");
   }

   public String getDescription() {
      throw new UnsupportedOperationException(
            "MockMessage.getDescription is not supported");
   }

   public String getDisposition() {
      throw new UnsupportedOperationException(
            "MockMessage.getDisposition is not supported");
   }

   public String getFileName() {
      throw new UnsupportedOperationException(
            "MockMessage.getFileName is not supported");
   }

   public String[] getHeader(String arg0) {
      throw new UnsupportedOperationException(
            "MockMessage.getHeader is not supported");
   }

   public InputStream getInputStream() {
      throw new UnsupportedOperationException(
            "MockMessage.getInputStream is not supported");
   }

   public int getLineCount() {
      throw new UnsupportedOperationException(
            "MockMessage.getLineCount is not supported");
   }

   @SuppressWarnings("unchecked")
   public Enumeration getMatchingHeaders(String[] arg0) {
      throw new UnsupportedOperationException(
            "MockMessage.getMatchingHeaders is not supported");
   }

   @SuppressWarnings("unchecked")
   public Enumeration getNonMatchingHeaders(String[] arg0) {
      throw new UnsupportedOperationException(
            "MockMessage.getNonMatchingHeaders is not supported");
   }

   public int getSize() {
      throw new UnsupportedOperationException(
            "MockMessage.getSize is not supported");
   }

   public boolean isMimeType(String arg0) {
      throw new UnsupportedOperationException(
            "MockMessage.isMimeType is not supported");
   }

   public void removeHeader(String arg0) {
      throw new UnsupportedOperationException(
            "MockMessage.removeHeader is not supported");
   }

   public void setContent(Multipart arg0) {
      throw new UnsupportedOperationException(
            "MockMessage.setContent(Multipart) is not supported");
   }

   public void setContent(Object arg0, String arg1) {
      throw new UnsupportedOperationException(
            "MockMessage.setContent(Object, String) is not supported");
   }

   public void setDataHandler(DataHandler arg0) {
      throw new UnsupportedOperationException(
            "MockMessage.setDataHandler is not supported");
   }

   public void setDescription(String arg0) {
      throw new UnsupportedOperationException(
            "MockMessage.setDescription is not supported");
   }

   public void setDisposition(String arg0) {
      throw new UnsupportedOperationException(
            "MockMessage.setDisposition is not supported");
   }

   public void setFileName(String arg0) {
      throw new UnsupportedOperationException(
            "MockMessage.setFileName is not supported");
   }

   public void setHeader(String arg0, String arg1) {
      throw new UnsupportedOperationException(
            "MockMessage.setHeader is not supported");
   }

   public void setText(String arg0) {
      body = arg0;
   }

   public void writeTo(OutputStream arg0) {
      throw new UnsupportedOperationException(
            "MockMessage.writeTo is not supported");
   }

}
