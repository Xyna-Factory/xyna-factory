/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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
package com.gip.xyna.xact.filter;

import com.gip.xyna.xact.trigger.ActiveMQTriggerConnection;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;

import xact.queue.CorrelationId;
import xact.queue.MessageProperties;
import xact.queue.Property;
import xact.queue.QueueMessage;

import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

import org.apache.log4j.Logger;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;

public class ActiveMQForwardingFilter extends ConnectionFilter<ActiveMQTriggerConnection> {

  private static final XynaPropertyBoolean useLegacyOrdertype 
    = new XynaPropertyBoolean("xact.activemq.filter.forwarding.useLegacyOrdertype", false)
      .setDefaultDocumentation(DocumentationLanguage.EN,
                               "User ordertypename with external queue name and prefix xact.activemq.filter.forwarding.");

  private static Logger logger = CentralFactoryLogging.getLogger(ActiveMQForwardingFilter.class);
  private static String LEGACY_ORDERTYPE_PREFIX = "xact.activemq.filter.forwarding.";
  private static String ORDERTYPE_PREFIX = "xact.activemq.filter.forwarding.ProcessActiveMQMessage.queue=";

  /**
   * Analyzes TriggerConnection and creates XynaOrder if it accepts the connection.
   * This method returns a FilterResponse object, which includes the XynaOrder if the filter is responsible for the request.
   * # If this filter is not responsible the returned object must be: FilterResponse.notResponsible()
   * # If this filter is responsible the returned object must be: FilterResponse.responsible(XynaOrder order)
   * # If this filter is responsible but the request is handled without creating a XynaOrder the 
   *   returned object must be: FilterResponse.responsibleWithoutXynaorder()
   * # If this filter is responsible but the request should be handled by an older version of the filter in another application version, the returned
   *    object must be: FilterResponse.responsibleButTooNew().
   * @param tc
   * @return FilterResponse object
   * @throws XynaException caused by errors reading data from triggerconnection or having an internal error.
   *         Results in onError() being called by Xyna Processing.
   */
  public FilterResponse createXynaOrder(ActiveMQTriggerConnection tc) throws XynaException {
    if (logger.isDebugEnabled()) {
      logger.debug("Received Active MQ message on queue " + tc.getXynaQueueMgmtQueueName() + ", " + tc.getExternalQueueName());
    }
    if (logger.isTraceEnabled()) {
      logger.trace("Message: " + tc.getMessage());
    }

    XynaOrder xynaOrder = null;
    try {
      Message untypedMessage = tc.getMessage();
      String msg;
      long jmsTimestamp = untypedMessage.getJMSTimestamp();
      if (untypedMessage instanceof TextMessage) {
        TextMessage tm = (TextMessage) untypedMessage;
        if (logger.isDebugEnabled()) {
          logger.debug("Received TextMessage - JMS TIMESTAMP =" + jmsTimestamp);
        }
        msg = tm.getText();
      } else if (untypedMessage instanceof BytesMessage) {
        BytesMessage bm = (BytesMessage) untypedMessage;
        if (logger.isDebugEnabled()) {
          logger.debug("Received BytesMessage - JMS TIMESTAMP =" + jmsTimestamp);
        }
        msg = bm.readUTF();
      } else {
        logger.error("Dequeued message is not instance of TextMessage or BytesMessage (received: "
            + (untypedMessage != null ? untypedMessage.getClass() : "null") + ")");
        return FilterResponse.responsibleWithoutXynaorder();
      }

      QueueMessage queueMessage = new QueueMessage();
      queueMessage.setMessage(msg);
      
      MessageProperties messageProperties = new MessageProperties();
      messageProperties.setCorrelationId(new CorrelationId( tc.getMessage().getJMSCorrelationID()));
      messageProperties.setProperties( getProperties(tc.getMessage()) );
      queueMessage.setMessageProperties(messageProperties);
      
      String orderTypeToBeStarted = "";
      if (useLegacyOrdertype.get()) {
        String queueNameStr = tc.getExternalQueueName();
        String adjustedQueueName = queueNameStr.substring(0, 1).toUpperCase() + queueNameStr.substring(1);
        orderTypeToBeStarted = LEGACY_ORDERTYPE_PREFIX + adjustedQueueName;
      } else {
        String queueNameStr = tc.getXynaQueueMgmtQueueName();
        orderTypeToBeStarted = ORDERTYPE_PREFIX + queueNameStr;
      }

      if (logger.isDebugEnabled()) {
        logger.debug("Going to start Order Type " + orderTypeToBeStarted);
      }
      DestinationKey destKey = new DestinationKey(orderTypeToBeStarted);
      xynaOrder = new XynaOrder(destKey, queueMessage);
      return FilterResponse.responsible(xynaOrder);

    } catch (JMSException e) {
      logger.error("Error in Filter", e);
      if (e.getLinkedException() != null) {
        logger.error("Linked exception: ", e.getLinkedException());
      }
      throw new XynaException("Error in Filter", e);
    }
  }
    
    private List<Property> getProperties(Message message) throws JMSException {
      Enumeration<?> enums = message.getPropertyNames();
      if( enums == null || ! enums.hasMoreElements() ) {
        return null;
      }
      List<Property> properties = new ArrayList<Property>();
      while( enums.hasMoreElements() ) {
        String key = String.valueOf(enums.nextElement());
        properties.add( new Property( key, message.getStringProperty(key) ) );
      }
      return properties;
    }

  /**
   * Called when above XynaOrder returns successfully.
   * @param response by XynaOrder returned GeneralXynaObject
   * @param tc corresponding triggerconnection
   */
  public void onResponse(GeneralXynaObject response, ActiveMQTriggerConnection tc) {
    //TODO implementation
    //TODO update dependency xml file
  }

  /**
   * Called when above XynaOrder returns with error or if an XynaException occurs in generateXynaOrder().
   * @param e
   * @param tc corresponding triggerconnection
   */
  public void onError(XynaException[] e, ActiveMQTriggerConnection tc) {
    //TODO implementation
    //TODO update dependency xml file
  }

  /**
   * @return description of this filter
   */
  public String getClassDescription() {
    //TODO implementation
    //TODO update dependency xml file
    return null;
  }

  /**
   * Called once for each filter instance when it is deployed and again on each classloader change (e.g. when changing corresponding implementation jars).
   * @param triggerInstance trigger instance this filter instance is registered to
   */
  public void onDeployment(EventListener triggerInstance) {
    super.onDeployment(triggerInstance);
  }

  /**
   * Called once for each filter instance when it is undeployed and again on each classloader change (e.g. when changing corresponding implementation jars).
   * @param triggerInstance trigger instance this filter instance is registered to
   */
  public void onUndeployment(EventListener triggerInstance) {
    super.onUndeployment(triggerInstance);
  }

}
