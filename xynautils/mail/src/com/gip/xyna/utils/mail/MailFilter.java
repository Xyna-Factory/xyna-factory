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

import java.io.IOException;
import java.security.Security;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.search.SearchTerm;
import com.gip.xyna.utils.mail.MailAuthenticator;

/**
 * MailFilter MainClass to search for something within emails or folders.
 * Filtering can be performed either using the javax.mail.Search (SearchTerm)
 * via searchMessage() on the MailServer itself or offline based on regrex using
 * configFilter() and applyFilter(). The latter can only be applied ot Body,
 * Sender, Receiver, Subject and Date.
 * 
 * 
 */
public class MailFilter {

   public static String[] partToMatch;

   public MailFilter() {
      partToMatch[0] = "Body";
      partToMatch[1] = "Sender";
      partToMatch[2] = "Receiver";
      partToMatch[3] = "Date";
      partToMatch[4] = "Subject";
   }

   /**
    * configureFilter(String) Define FilterParameters here
    * 
    * @param filter
    *              String regrex to look for
    * @return pattern pattern compiled pattern to be used in filter
    */
   public Pattern configureFilter(String filter) {
      // Filter muss ein regulaerer Ausdruck sein
      Pattern pattern = Pattern.compile(filter);
      return (pattern);
   }

   /**
    * applyFilter(Message[], String, Pattern) Applies filter and pattern for set
    * of Message instances
    * 
    * @param messages
    *              Messages-array, might have length 0.
    * @param part
    *              String = Messagepart to search in for the pattern.
    * @param pattern
    *              Pattern, complied with configureFilter() to match within
    *              Message-Array
    * @return Vector of Message instances which match pattern
    */
   public Vector<Message> applyFilter(Message[] messages, String part,
         Pattern pattern) throws MailAdapterError {

      if (messages == null) {
         throw new IllegalArgumentException(
               "Paramter messages must not be null!");
      }

      Vector<Message> retVal = new Vector<Message>();
      Matcher matcher = null;

      int msgCount = messages.length;
      String data = "";
      System.out.println("Messages found: " + msgCount);
      try {
         for (int i = 0; i <= msgCount; i++) {
            if (part == partToMatch[0]) {
               data = messages[i].getContent().toString();
            } else if (part == partToMatch[1]) {
               data = messages[i].getFrom().toString();
            } else if (part == partToMatch[2]) {
               data = messages[i].getAllRecipients().toString();
            } else if (part == partToMatch[3]) {
               data = messages[i].getSentDate().toString();
            } else if (part == partToMatch[4]) {
               data = messages[i].getSubject().toString();
            }

            matcher = pattern.matcher(data);
            if (matcher.matches()) {
               System.out.println("Message found!");
               retVal.add(messages[i]);
               break;
            }
         }
      } catch (IOException iox) {
         throw new MailAdapterError(MailAdapterError.MailInputOutputError
               + iox.getMessage() + "\nStacktrace: \n" + iox.getStackTrace()
               + "\n Variables: \n" + part, messages[0].toString(), pattern
               .toString());
      } catch (MessagingException mgx) {
         throw new MailAdapterError(MailAdapterError.MailMessagingError
               + mgx.getMessage() + "\n Stacktrace: \n" + mgx.getStackTrace()
               + "\n Variables: \n " + part, messages[0].toString(), pattern
               .toString());
      }
      return retVal;
   }

   /**
    * searchMessage(String, SearchTerm, String, Properties);
    * 
    * @param folder
    *              String Mailbox in which should be searched for SearchTerm
    * @param searchterm
    *              SearchTerm (javax.mail) to look for in folder
    * @param protocoll
    *              String Protocol to use to connect to the mail server
    * @param prop
    *              Properties object containing the mail server/user
    *              configuration
    * @return Message[] arry of Messages returned which contain the SearchTerm
    * @throws NoSuchProviderException
    * @throws MessagingException
    *               Search for certain messages - simplest way.
    */
   public Message[] searchMessage(String folder, SearchTerm searchterm,
         String protocoll, Properties prop) throws MailAdapterError {
      Folder folder2 = null;
      Store store;
      Message returnmessages[];

      Session mailSession = null;
      if (prop.getProperty("mail.imap.starttls.enable").toString() == "true") {
         Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
         MailAuthenticator auth = new MailAuthenticator();
         Security.setProperty("ssl.SocketFactory.provider", prop.getProperty(
               "mail.imap.socketFactory.class", ""));
         mailSession = Session.getDefaultInstance(prop, auth);
      } else {
         mailSession = Session.getDefaultInstance(prop, null);
      }
      try {
         store = mailSession.getStore(protocoll);
         folder2 = store.getFolder(folder);

         returnmessages = folder2.search(searchterm);
         store.close();
      } catch (MessagingException mgx) {
         throw new MailAdapterError(MailAdapterError.MAE_221 + mgx.getMessage()
               + "\nStacktrace:\n" + mgx.getStackTrace() + "\n Variables: \n"
               + folder, searchterm.toString(), prop.getProperty("mailServer",
               ""));
      }

      return (returnmessages);
   }
}
