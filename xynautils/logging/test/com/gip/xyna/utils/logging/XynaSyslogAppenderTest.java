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
package com.gip.xyna.utils.logging;

import java.util.Properties;

import junit.framework.TestCase;

import org.apache.log4j.NDC;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 
 */
public class XynaSyslogAppenderTest extends TestCase {

   private static final String LOG4J_PROPERTIES = System
         .getProperty("user.dir")
         + "/test/com/gip/xyna/utils/logging" + "/log4j2.xml";

   public void testSplitMessage() {
      Properties props = System.getProperties();
      props.setProperty("log4j.configurationFile", LOG4J_PROPERTIES);
      Logger logger = LogManager.getLogger("xyna.utils.logging");
      StringBuffer message = new StringBuffer();
      for (int i = 0; i < XynaSyslogAppender.MESSAGE_MAX_SIZE; i++) {
         message.append(i + " ");
      }
      logger.info(message);

   }
   
   public void testSplitException() {
     Properties props = System.getProperties();
     props.setProperty("log4j.configurationFile", LOG4J_PROPERTIES);
     Logger logger = LogManager.getLogger("xyna.utils.logging");
     IllegalArgumentException exception = new IllegalArgumentException("Argument is illegal");
     NDC.push("context");
     logger.error("Exception during execution", exception);
     NDC.pop();
     NDC.remove();
   }

}
