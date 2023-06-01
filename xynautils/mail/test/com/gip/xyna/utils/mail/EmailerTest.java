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

import java.io.IOException;
import java.util.Properties;
import java.util.Vector;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;

import junit.framework.TestCase;

public class EmailerTest extends TestCase {

   private static final String HOST = "here.com";

   private static final String USER = "user";

   private static final String FROM = "sender" + "@" + "here.com";

   private static final String TO = USER + "@" + HOST;

   private static final String SUBJECT = "TestMail";

   private static final String BODY = "send by JUnit via EMailer";

   private Emailer mailer = null;

   private Properties props = null;

   public void setUp() {
      mailer = new Emailer();
      props = new Properties();
      props.put("mailHost", HOST);
      props.put("mailUser", USER);
      props.put("mailUserPwd", "pwd");
      props.put("mailFolder", "INBOX");
      props.put("mail.transport.protocol", "imap");
      props.put("mail.store.protocol", "imap");

      props.put("mail.imap.starttls.enable", "true");
      props.put("mail.imap.port", "993");
      props.put("mail.imap.socketFactory.port", "993");
      props.put("mail.imap.socketFactory.class",
            "javax.net.ssl.SSLSocketFactory");
      props.put("mail.imap.socketFactory.fallback", "false");

      props.put("mail.smtp.auth", "true");
      props.put("mail.smtp.port", "465");
      props.put("mail.smtp.socketFactory.port", "465");
      props.put("mail.smtp.socketFactory.class",
            "javax.net.ssl.SSLSocketFactory");
      props.put("mail.smtp.socketFactory.fallback", "false");
   }

   public void tearDown() {
      MockMailServer.clear();
   }

   public void testSendSMTPMail() throws AddressException,
         NoSuchProviderException, MessagingException, IOException {
      mailer.sendEmail(FROM, TO, SUBJECT, BODY, props);
      Vector<Message> messages = MockMailServer.getMessages(TO);
      assertEquals("Number of send messages", 1, messages.size());
      MimeMessage msg = (MimeMessage) messages.get(0);
      assertEquals("Subject of the send message", SUBJECT, msg.getSubject());
      InternetAddress[] froms = (InternetAddress[]) msg.getFrom();
      assertEquals("Sender of the message", FROM, froms[0].getAddress());
      InternetAddress[] tos = (InternetAddress[]) msg.getAllRecipients();
      assertEquals("Receiver of the message", TO, tos[0].getAddress());
      assertEquals("Body of the send message", BODY,
            ((String) msg.getContent()).trim());
   }

   public void testReceiveIMAPMail() throws MessagingException, IOException {
      // prepare
      for (int i = 0; i < 4; i++) {
         MockMessage msg = new MockMessage();
         msg.setFrom(new InternetAddress(FROM));
         msg.setRecipient(RecipientType.TO, new InternetAddress(TO));
         msg.setSubject(SUBJECT);
         msg.setText(BODY);
         MockMailServer.addMessage(TO, msg);
      }
      assertEquals("Number of mails in MockMailServer", 4, MockMailServer
            .getMessages(TO).size());

      // call
      Vector<Message> messages = mailer.getEmail(Protocol.IMAP, props);
      assertEquals("Number of received messages", 4, messages.size());
      for (int i = 0; i < messages.size(); i++) {
         assertNotNull(messages.get(i));
         assertEquals("Sender of received message " + i, FROM, messages.get(i)
               .getFrom()[0].toString());
         assertEquals("Receiver of received message " + i, TO, messages.get(i)
               .getAllRecipients()[0].toString());
         assertEquals("Body of received message " + i, BODY, messages.get(i)
               .getContent());
         assertEquals("Subject of received message " + i, SUBJECT, messages
               .get(i).getSubject());
      }
   }

   /*
    * public void testConfigLoadervalidate(){ ConfigLoader config = new
    * ConfigLoader(); boolean test; MailAdapterConfigBean configBean = new
    * MailAdapterConfigBean(); test =
    * config.validateConfig(config.getPropertiesFromBean(configBean));
    * assertEquals("Testing ConfigLoader.validateConfig() with status: ", true,
    * test); }
    */
}
