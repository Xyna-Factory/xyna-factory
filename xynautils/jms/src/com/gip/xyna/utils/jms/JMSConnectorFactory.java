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
package com.gip.xyna.utils.jms;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.jms.JMSException;

/**
 * Factory for providing different kind of JMS connector. The type of the
 * Connector depends on the content of a property file.
 * <p>
 * Three types of JMS connector are separated:
 * <p>
 * 1. Connectors to in-memory queues where application and queue are in the same
 * container of the application server
 * <p>
 * The property file must contain the property
 * <p>
 * queue_connection_factory = JNDI-Name of the connection factory (can be found
 * in OC4J -> Administration -> JMS Connection Factories)
 * <p>
 * 2. Connectors to in-memory queues where application and queue are in
 * different containers or application server
 * <p>
 * The property file must contain the properties
 * <p>
 * initial_context_factory= for example
 * com.evermind.server.ApplicationClientInitialContextFactory
 * <p>
 * url= an url for the form opmn:ormi://10.0.0.173:6003:xyna_soa
 * <p>
 * username= user name for the application server
 * <p>
 * password= password for the application server
 * <p>
 * queue_connection_factory = JNDI-Name of the connection factory
 * <p>
 * 3. Connectors to Oracle advanced queues (AQ)
 * <p>
 * The property file must contain the properties
 * <p>
 * host= hostname or ip of the database server (eg. 10.0.0.142)
 * <p>
 * port= port on the database server (eg. 1521)
 * <p>
 * sid=database name
 * <p>
 * username= database user
 * <p>
 * password= passwort of database user
 * 
 * 
 * @see java.util.Properties
 * @deprecated use jmsqueueutils
 * 
 */
public class JMSConnectorFactory {

   private static final String INITIAL_CONTEXT_FACTORY = "initial_context_factory";
   private static final String URL = "url";
   private static final String USERNAME = "username";
   private static final String PASSWORD = "password";
   private static final String QUEUE_CONNECTION_FACTORY = "queue_connection_factory";
   private static final String HOST = "host";
   private static final String PORT = "port";
   private static final String SID = "sid";

   private static JMSConnectorFactory me = null;

   private JMSConnectorFactory() {

   }

   public static JMSConnectorFactory getInstance() {
      if (me == null) {
         me = new JMSConnectorFactory();
      }
      return me;
   }

   public JMSConnector getConnector(InputStream inStream) throws JMSException {
      Properties properties = new Properties();
      try {
         properties.load(inStream);
      } catch (IOException e) {
         JMSException exp = new JMSException(e.getMessage());
         exp.setLinkedException(e);
         throw exp;
      }
      return selectConcreteConnector(properties);
   }

   public JMSConnector getConnector(Properties properties) throws JMSException {
      return selectConcreteConnector(properties);
   }

   private JMSConnector selectConcreteConnector(Properties properties)
         throws JMSException {
      if (properties == null)
         return null;
      String connection_factory = properties
            .getProperty(QUEUE_CONNECTION_FACTORY);
      if (connection_factory == null)
         return new AQConnector(properties.getProperty(HOST), properties
               .getProperty(PORT), properties.getProperty(SID), properties
               .getProperty(USERNAME), properties.getProperty(PASSWORD));
      String context_factory = properties.getProperty(INITIAL_CONTEXT_FACTORY);
      if (context_factory == null)
         return new InMemoryQueueConnector(connection_factory);
      return new InMemoryQueueConnector(context_factory, properties.getProperty(URL),
            properties.getProperty(USERNAME), properties.getProperty(PASSWORD),
            connection_factory);
   }

}
