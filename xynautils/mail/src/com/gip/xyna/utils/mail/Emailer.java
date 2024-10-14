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

import java.security.Security;

import java.util.Properties;
import java.util.Vector;

import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Transport;
import javax.mail.Flags;
import javax.mail.Folder;

import java.util.Date;

import com.gip.xyna.utils.mail.MailAuthenticator;

// TODO test security
/**
 * Mail sending and receiving utility sendEmail(args)sends emails to specified
 * destination getEmail(args) get emails from specified destination
 * expungePostbox(args) deletes mails in specified mailbox folder Defines
 * various variables and force defaults in constructor.
 * 
 * 
 */
public class Emailer {

   private static final String SSL_FACTORY = "com.gip.xyna.utils.mail.DummySSLSocketFactory";

   /**
    * Initializes a new Emailer. For use properties are required. These can be
    * read via ConfigLoader. See ConfigLoader describtion for more infomation
    * about needed properties.
    */
   public Emailer() {
   }

   /**
    * get Emails from the MailBox
    * 
    * @param protocol
    *              an email receiving protocol (eg. imap or pop3)
    * @param prop
    *              configuration properties for the email access
    * @return
    * @throws NoSuchProviderException
    * @throws MessagingException
    */
   public Vector<Message> getEmail(Protocol protocol, Properties prop)
         throws MessagingException {
      checkProperties(prop);
      MailAuthenticator auth = getMailAuthenticator(prop);
      Session session = Session.getDefaultInstance(prop, auth);
      Store store = session.getStore(protocol.toString());
      Vector<Message> returnmessages = new Vector<Message>();
      try {
         store.connect(prop.getProperty(ConfigLoader.HOST_KEY), prop
               .getProperty(ConfigLoader.USER_KEY), prop
               .getProperty(ConfigLoader.PASSWORD_KEY));
         Folder folder = store.getFolder(prop.getProperty(
               ConfigLoader.FOLDER_KEY, "INBOX"));
         folder.open(Folder.READ_ONLY);
         for (int i = 0; i < folder.getMessageCount(); i++) {
            returnmessages.add(i, folder.getMessage(i));
         }
      } finally {
         store.close();
      }
      return returnmessages;
   }

   /**
    * Check if security is enabled and retrun matching MailAuthenticator.
    * 
    * @param prop
    * @return a MailAuthenticator if security is enabled, else null.
    */
   private MailAuthenticator getMailAuthenticator(Properties prop) {
      if (prop.getProperty("mail.imap.starttls.enable").equals("true")) {
         Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
         MailAuthenticator auth = new MailAuthenticator();
         Security.setProperty("ssl.SocketFactory.provider", SSL_FACTORY);
         return auth;
      }
      return null;
   }

   /**
    * Sends Email from "From" to "To" Parameters for the mailheader and the mail
    * 
    * @param from
    *              EmailMessageHeader From String
    * @param to
    *              EmailMessageHeader To String
    * @param subject
    *              EmailMessageHeader Subject String
    * @param body
    *              EmailMessageHeader Body String
    * @param prop
    *              Properties object containing mailserverconfig
    * @throws AddressException
    * @throws MessagingException
    * @throws MessagingException
    */
   public void sendEmail(String from, String to, String subject, String body,
         Properties prop) throws MessagingException {
      checkProperties(prop);
      MailAuthenticator auth = getMailAuthenticator(prop);
      Session mailSession = Session.getDefaultInstance(prop, auth);
      Store store = null;
      try {
         // create Mail
         MimeMessage message = new MimeMessage(mailSession);
         message.setFrom(new InternetAddress(from));
         message
               .addRecipient(Message.RecipientType.TO, new InternetAddress(to));
         message.setSubject(subject);
         message.setSentDate(new Date());
         message.setText(body);
         message.saveChanges();
         store = mailSession.getStore(prop
               .getProperty("mail.transport.protocol"));
         store.connect(prop.getProperty(ConfigLoader.HOST_KEY), prop
               .getProperty(ConfigLoader.USER_KEY), ConfigLoader.PASSWORD_KEY);

         /*
          * // initiate transport // FIXME following three lines worked in real
          * world but not in tests with JUnit - strange ;-) Transport transport =
          * mailSession.getTransport(); transport.connect(mailHost, mailUser,
          * mailUserPwd); transport.sendMessage(message, message
          * .getRecipients(Message.RecipientType.TO));
          */
         Transport.send(message, message
               .getRecipients(Message.RecipientType.TO));
      } finally {
         store.close();
      }
   }

   /**
    * Delete Specified mails. For all set counter1=0; counter2 = totalnumber;
    * 
    * @param mailboxfolder
    *              String describing name folder to delete mails in
    * @param counter1
    *              first message to delete, may be 0
    * @param counter2
    *              last meesage to delete, may be 0
    * @param prop
    *              Properties object containing config for mailserver
    * @throws NoSuchProviderException
    * @throws MessagingException
    */
   public void expungePostbox(String mailboxfolder, int counter1, int counter2,
         Protocol protocol, Properties prop) throws MessagingException {
      // TODO why use parameter for Protocol instead of properties?
      checkProperties(prop);
      MailAuthenticator auth = getMailAuthenticator(prop);
      Session mailSession = Session.getDefaultInstance(prop, auth);
      Store store = mailSession.getStore(protocol.toString());
      try {
         // open Mailbox and folder
         store.connect(prop.getProperty(ConfigLoader.HOST_KEY), prop
               .getProperty(ConfigLoader.USER_KEY), prop
               .getProperty(ConfigLoader.PASSWORD_KEY));
         Folder folder = store.getFolder(mailboxfolder);
         folder.open(Folder.READ_WRITE);

         // kill content by setting DELETED-Flag
         folder.setFlags(counter1, counter2, new Flags(Flags.Flag.DELETED),
               true);
         folder.expunge();
      } finally {
         store.close();
      }
   }

   /**
    * Set flags for Message in certain Folders
    * 
    * @param flag
    *              javax.mail.Flag - MessageFlag to be set
    * @param firstMessage
    *              int - Number of first message to set flag to
    * @param lastMessage
    *              int - Number of last message to set flat to
    * @param folderName
    *              String - Name of the folder to find messages in
    * @param prop
    *              Properties object - containing configuration for interaction
    *              with mail server
    * @throws MessagingException
    */
   public void setMessageFlag(Flags flag, int firstMessage, int lastMessage,
         String folderName, Properties prop) throws MessagingException {
      checkProperties(prop);
      MailAuthenticator auth = getMailAuthenticator(prop);
      Session mailSession = Session.getDefaultInstance(prop, auth);
      Store store = mailSession.getStore(prop
            .getProperty("mail.transport.protocol"));
      try {
         // open Mailbox and folder
         store.connect(prop.getProperty(ConfigLoader.HOST_KEY), prop
               .getProperty(ConfigLoader.USER_KEY), prop
               .getProperty(ConfigLoader.PASSWORD_KEY));
         Folder folder = store.getFolder(folderName);
         folder.open(Folder.READ_WRITE);

         // kill content by setting DELETED-Flag
         folder.setFlags(firstMessage, lastMessage, flag, true);
         folder.expunge();
      } finally {
         store.close();
      }

   }

   /**
    * Validate properties.
    * 
    * @param prop
    */
   private void checkProperties(Properties prop) {
      ConfigLoader config = new ConfigLoader();
      if (!config.validateConfig(prop)) {
         System.err.println("Configuration not correct - aborting");
         // TODO: throw Exception
      }
   }
}
