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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

/**
 * Configurationloader for Emailadapter. Loads the configuration for the
 * Emailadapter in various ways: from file, from JavaBean and from URL
 * 
 * The MailServerConfiguration (loaded) will be stored as in Property-Object
 * which must contain the following elements:
 * 
 * mailUser = user of the account mailUserPassword = password of the user to its
 * account mailHost = mail sever ip or name mail."imap/smtp".port = the port to
 * connect to depending on the protocoll used
 * 
 * Optional elements: maildebug = true/false = if debug information are desired.
 * mail."imap/smtp".timeout = integer - if timeout value should be used If
 * SSL/TLS is required and supported by the version of javax.mail used
 * mail.imap.starttls.enable = true/false mail.imap.socketFactory.fallback =
 * true/false mail.smtp.socketFactory.port = smtpport
 * mail.smtp.socketFactory.class = SSL_Factory, e.g.
 * javax.net.ssl.SSLSocketFactory
 * 
 * System properties can be added on demand. Uncomment appropriate lines
 * 
 * 
 */
public class ConfigLoader {

   /**
    * Property name for user.
    */
   public static final String USER_KEY = "mailUser";

   /**
    * Property name for password.
    */
   public static final String PASSWORD_KEY = "mailUserPwd";

   /**
    * Property name for host.
    */
   public static final String HOST_KEY = "mailHost";

   /**
    * Property name for folder.
    */
   public static final String FOLDER_KEY = "mailFolder";

   /**
    * Validates properties object. If one of the required properties is not set
    * it will return false
    * 
    * @param prop
    *              properties object containing mailserver configuration
    * @return boolean
    */
   public boolean validateConfig(Properties prop) {

      if (prop.getProperty(HOST_KEY, "").equals("")) {
         return false;
      }
      if (prop.getProperty("mail.imap.port", "").equals("")
            & prop.getProperty("mail.pop3.port", "").equals("")) {
         return false;
      }
      if (prop.getProperty("mail.smtp.port", "").equals("")) {
         return false;
      }
      if (prop.getProperty(FOLDER_KEY, "").equals("")) {
         return false;
      }
      if (prop.getProperty("mail.imap.starttls.enable", "").equals("true")) {
         if (!prop.getProperty("mail.imap.socketFactory.fallback", "").equals(
               "")
               ^ !prop.getProperty("mail.imap.socketFactory.class", "").equals(
                     "")
               ^ !prop.getProperty("mail.imap.socketFactory.port", "").equals(
                     "")
               ^ !prop.getProperty("SSL_FACTORY", "").equals("")
               ^ !prop.getProperty("mail.smtp.socketFactory.port").equals("")
               ^ !prop.getProperty("mail.smtp.socketFactory.class", "").equals(
                     "")
               ^ !prop.getProperty("mail.smtp.socketFactory.fallback", "")
                     .equals("")) {
            return false;
         }
      }
      if (prop.getProperty("mail.transport.protocol", "").equals("")) {
         return false;
      }
      return true;
   }

   /**
    * 
    * Fetch mailserver config from "location"
    * 
    * @param location
    *              String specifing the path to config
    * @throws IOException
    */
   public Properties fetchConfig(String location) throws IOException {
      // set up Variables for use within this function
      Properties prop = new Properties();
      InputStream input = null;

      try {
         if (location.startsWith("http://")) {
            // load config from URL
            URL url = new URL(location);
            URLConnection urlCon = url.openConnection();
            input = urlCon.getInputStream();
            prop.load(input);
         } else {
            // load config from file
            input = new FileInputStream(location);
            System.out.println(input.toString());
            prop.load(input);
         }
      } finally {
         if (input != null)
            input.close();
      }
      return prop;
   }

   /**
    * 
    * Reloads configuration for mail server from "location" on runtime thereby
    * it falls back on fetchConfig(location)
    * 
    * @param location
    *              String specifing path to config
    * @throws IOException
    * @throws MailAdapterError
    */
   public synchronized Properties refreshConfig(String location)
         throws IOException {
      return fetchConfig(location);
   }

   /**
    * 
    * Load configuration form a JavaBean into Properties object and return it
    * 
    * @param configBean
    *              JavaBean containing the required Properties
    * @return Properties Object defining mailserver & -connection properties
    */
   public Properties getPropertiesFromBean(MailAdapterConfigBean configBean) {

      Properties property = new Properties();

      // uncomment only if you know what you are doing
      // property = System.getProperties();

      // Debuging information:
      property.put("mail.debug", configBean.getMailDebug());

      // General Settings:
      property.put("mailHost", configBean.getMailHost());
      property.put("mailUser", configBean.getMailUser());
      property.put("mailUserPwd", configBean.getMailUserPwd());
      property.put("mailFolder", configBean.getMailFolder());

      // POP3 settings:
      property.put("mail.pop3.host", configBean.getMailHost());
      property.put("mail.pop3.port", configBean.getpopPort());

      // IMAP settings:
      property.put("mail.imap.host", configBean.getMailHost());
      property.put("mail.imap.port", configBean.getMailPort());
      property.put("mail.imap.timeout", configBean.getMailTimeOut());

      // SSL/TLS settins for IMAP:
      property.put("mail.imap.starttls.enable", configBean.getEnableTLS());
      property.put("mail.imap.socketFactory.fallback", configBean
            .getSocketFactoryFallback());
      property.put("mail.imap.socketFactory.class", configBean
            .getsSLSocketFactory());
      property.put("mail.imap.socketFactory.port", configBean.getMailPort());

      // SMTP settings:
      property.put("mail.smtp.host", configBean.getMailHost());
      property.put("mail.smtp.port", configBean.getsmtpPort());
      property.put("mail.transport.protocol", configBean
            .getTransportProtocoll());
      property.put("mail.smtp.auth", "true");

      // SSL/TLS settings for smtp -- shouldn't this be SMTPS then? :
      property.put("mail.smtp.socketFactory.port", configBean.getsmtpPort());
      property.put("mail.smtp.socketFactory.class", configBean
            .getsSLSocketFactory());
      property.put("mail.smtp.socketFactory.fallback", configBean
            .getSocketFactoryFallback());

      return property;
   }
}