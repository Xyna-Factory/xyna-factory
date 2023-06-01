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
package com.gip.xyna.utils.misc;

import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

public class XynaProperties {

   private static final HashMap<String, String> defaults = new HashMap<String, String>();

   /**
    * Name of the system property over which the property file can be reached
    */
   public static final String XYNA_SYSTEM_PROPERTY = "xynaProps";

   private Properties properties = null;

   private static XynaProperties me = null;

   private XynaProperties() {
      loadProperties(XYNA_SYSTEM_PROPERTY);
   }

   public static synchronized XynaProperties getInstance() {
      if (me == null) {
         me = new XynaProperties();
      }
      return me;
   }

   /**
    * Liefert alle Properties zurück
    * 
    * @return
    */
   public Properties getProperties() {
      return properties;
   }

   /**
    * Get all properties which starts with the given prefix
    * 
    * @param prefix
    * @return
    */
   public Properties getProperties(String prefix) {
      Properties props = new Properties();
      Enumeration<?> keys = properties.propertyNames();
      while (keys.hasMoreElements()) {
         String currentKey = (String) keys.nextElement();
         if (currentKey.startsWith(prefix)) {
            props.setProperty(currentKey, properties.getProperty(currentKey));
         }
      }
      return props;
   }

   public void reloadProperties() {
      loadProperties(XYNA_SYSTEM_PROPERTY);
   }

   /**
    * Load properties
    */
   private void loadProperties(String propertyName) {
      properties = new Properties();
      InputStream is = null;
      try {
         URL propertyfile = null;
         try {
            propertyfile = new URL("file", "", System.getProperty(propertyName));
            is = propertyfile.openStream();
            properties.load(is);
         } catch (Exception e) {
            // TODO: SysLog exception
         }
      } finally {
         try {
            if (is != null) {
               is.close();
            }
         } catch (Exception e) {
            // TODO: SysLog exception
         }
      }
   }

   /**
    * Ausgabe einer bestimmten Property
    * 
    * @param key
    * @return
    */
   public String getProperty(String key) {
      String propValue = properties.getProperty(key);
      if (propValue == null) {
         return defaults.get(key);
      }
      return propValue;
   }

   /**
    * Ausgabe einer bestimmten Property
    * 
    * @param prefix
    * @param key
    * @return
    */
   public String getProperty(String prefix, String key) {
      String propValue = properties.getProperty(prefix + "." + key);
      if (propValue == null) {
         return defaults.get(prefix + "." + key);
      }
      return propValue;
   }

   /**
    * Set default values
    * 
    * @param name
    * @param value
    * @param string
    */
   public void addDefault(String prefix, String name, String value) {
      defaults.put(prefix + "." + name, value);
   }

}
