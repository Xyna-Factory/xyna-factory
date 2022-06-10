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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.gip.xyna.utils.mail.MailAdapterConfigBean;
import com.gip.xyna.utils.mail.ConfigReaderIntf;

/**
 * 
 * This class summarizes different methods to read the config for the
 * interaction with a mail server into a PropertiesObject which is requried to
 * set up the connection with the mailserver.
 * 
 * 
 */
public class MailAdapterConfigReader implements ConfigReaderIntf {

   /**
    * 
    * reads config from an URL or an File into a ConfigBean which is returned
    * 
    * @param configResourceLocation
    *              String - specifies path to config
    * @return ConfigBean configBean JavaBean containing the configuration for
    *         interaction with mailserver
    */
   public ConfigBean readConfig(String configResourceLocation) {

      MailAdapterConfigBean configBean = new MailAdapterConfigBean();

      try {
         InputStream ins = null;
         if (configResourceLocation.startsWith("http://")) {
            URL url = new URL(configResourceLocation);
            URLConnection urlCon = url.openConnection();
            ins = urlCon.getInputStream();
         } else {
            try {
               ins = new FileInputStream(configResourceLocation);
            } catch (IOException ix) {
               System.out
                     .println("Failed getting resource from file. Trying classpath ...");
               ins = this.getClass()
                     .getResourceAsStream(configResourceLocation);
            }
         }
         if (null == ins) {
            System.out.println("Cannot load resouce " + configResourceLocation);
            throw new Exception("Error: Cannot load " + configResourceLocation);
         }

         // read content of file
         BufferedReader br = new BufferedReader(new InputStreamReader(ins));

         String line = null;
         Pattern pat = Pattern.compile("\\s*([.\\w]+)\\s*=\\s*([^\\s]+).*");
         Matcher m = null;
         String methodName;
         while (null != (line = br.readLine())) {
            m = pat.matcher(line);
            if (m.matches()) {
               try {
                  methodName = m.group(1);
                  methodName = "set"
                        + Character.toUpperCase((methodName.charAt(0)))
                        + methodName.substring(1);

                  CharSequence t = "This.";
                  if (methodName.contains(t) == true) {
                     methodName = "setMailDummy";

                  }

                  Method mth = MailAdapterConfigBean.class.getMethod(
                        methodName, new Class[] { String.class });
                  mth.invoke(configBean, new Object[] { m.group(2) });
               } catch (Exception e) {
                  e.printStackTrace();
               }
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
      return configBean;
   }
}
