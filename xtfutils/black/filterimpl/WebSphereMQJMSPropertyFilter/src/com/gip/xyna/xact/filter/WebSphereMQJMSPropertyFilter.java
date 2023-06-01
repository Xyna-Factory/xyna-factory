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

package com.gip.xyna.xact.filter;

import java.io.UnsupportedEncodingException;
import java.util.Enumeration;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

import org.apache.log4j.Logger;

import xact.WebSphereMQ.datatypes.JMSProperty;
import xact.WebSphereMQ.datatypes.JMSPropertyList;
import xact.WebSphereMQ.datatypes.WebSphereMQMessage;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.trigger.WebSphereMQTriggerConnection;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;

public class WebSphereMQJMSPropertyFilter extends ConnectionFilter<WebSphereMQTriggerConnection> {

  private static final long serialVersionUID = 1L;


  public static class Constant {
    public static class XynaPropertyName {
      public static final String DESTINATION_KEY_BASE = "xtf.utils.queue.filter.destinationKey.base";
    }
  }

  private static Logger _logger = CentralFactoryLogging.getLogger(WebSphereMQJMSPropertyFilter.class);

  
  /**
   * Analyzes TriggerConnection and creates XynaOrder if it accepts the connection. The method return a FilterResponse
   * object, which can include the XynaOrder if the filter is responsibleb for the request. # If this filter is not
   * responsible the returned object must be: FilterResponse.notResponsible() # If this filter is responsible the
   * returned object must be: FilterResponse.responsible(XynaOrder order) # If this filter is responsible but it handle
   * the request without creating a XynaOrder the returned object must be: FilterResponse.responsibleWithoutXynaorder()
   * # If this filter is responsible but the version of this filter is too new the returned object must be:
   * FilterResponse.responsibleButTooNew(). The trigger will try an older version of the filter.
   * @param tc
   * @return FilterResponse object
   * @throws XynaException caused by errors reading data from triggerconnection or having an internal error. results in
   *           onError() being called by Xyna Processing.
   */
  public FilterResponse createXynaOrder(WebSphereMQTriggerConnection tc) throws XynaException {

    _logger.info("Received WebSphere MQ message on queue " + tc.getXynaQueueMgmtQueueName());

    XynaOrder xynaOrder = null;
    try {
      Message untypedMessage = tc.getMessage();
      String msg;
      long jmsTimestamp = untypedMessage.getJMSTimestamp();
      if (untypedMessage instanceof TextMessage) {
        TextMessage tm = (TextMessage) untypedMessage;
        if (_logger.isDebugEnabled()) {
          _logger.debug("Received TextMessage - JMS TIMESTAMP =" + jmsTimestamp);
        }
        msg = tm.getText();
      } else if (untypedMessage instanceof BytesMessage) {
          BytesMessage bm = (BytesMessage) untypedMessage;
          if (_logger.isDebugEnabled()) {
            _logger.debug("Received BytesMessage - JMS TIMESTAMP =" + jmsTimestamp);
          }
          //msg = bm.readUTF();
          byte[] byteData = null;
          byteData = new byte[(int) bm.getBodyLength()];
          bm.readBytes(byteData);
          bm.reset();
          msg = new String(byteData, "UTF-8");
        } else {
        _logger.error("Dequeued message is not instance of TextMessage or BytesMessage (received: "
            + (untypedMessage != null ? untypedMessage.getClass() : "null") + ")");
        return FilterResponse.responsibleWithoutXynaorder();
      }

      JMSPropertyList propList = buildPropertyList(untypedMessage);

      WebSphereMQMessage mqMsg = new WebSphereMQMessage();
      mqMsg.setMessage(msg);
      mqMsg.setCorrelationID(untypedMessage.getJMSCorrelationID());
      try {
        String msgId = untypedMessage.getJMSMessageID();
        mqMsg.setJMSMessageID(msgId);
      } catch (Throwable e) {
        // TODO insert comment why this would be reasonable
        Department.handleThrowable(e);
        _logger.warn("Failed to determine JMS Message ID", e);
      }

      String queueNameStr = tc.getXynaQueueMgmtQueueName();
      String adjustedQueueName = queueNameStr.substring(0, 1).toUpperCase() + queueNameStr.substring(1);
      String orderTypeToBeStarted =
          getXynaProperty(Constant.XynaPropertyName.DESTINATION_KEY_BASE) + "." + adjustedQueueName;

      _logger.info("Going to start Order Type " + orderTypeToBeStarted);
      DestinationKey destKey = new DestinationKey(orderTypeToBeStarted);
      xynaOrder = new XynaOrder(destKey, mqMsg, propList);
      return FilterResponse.responsible(xynaOrder);

    } catch (JMSException e) {
      _logger.error("Error in Filter", e);
      if (e.getLinkedException() != null) {
        _logger.error("Linked exception: ", e.getLinkedException());
      }
      throw new XynaException("Error in Filter", e);
    } catch (UnsupportedEncodingException e) {
       _logger.error("UnsupportedEncoding", e);
       throw new RuntimeException("UnsupportedEncoding",e);
    }

  }


  private JMSPropertyList buildPropertyList(Message msg) throws JMSException {
    JMSPropertyList ret = new JMSPropertyList();
    Enumeration<?> names = msg.getPropertyNames();
    while (names.hasMoreElements()) {
      Object obj = names.nextElement();
      if (obj instanceof String) {
        String name = (String) obj;
        String val = msg.getStringProperty(name);
        JMSProperty prop = new JMSProperty();
        prop.setJMSPropertyName(name);
        prop.setJMSPropertyValue(val);
        ret.addToData(prop);
      }
    }
    return ret;
  }


  /**
   * called when above XynaOrder returns successfully.
   * @param response by XynaOrder returned XynaObject
   * @param tc corresponding triggerconnection
   */
  public void onResponse(XynaObject response, WebSphereMQTriggerConnection tc) {
  }

  /**
   * called when above XynaOrder returns with error or if an XynaException occurs in generateXynaOrder().
   */
  public void onError(XynaException[] e, WebSphereMQTriggerConnection tc) {
  }

  /**
   * @return description of this filter
   */
  public String getClassDescription() {
    return null;
  }


  private static String getXynaProperty(String propname) {
    String val =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration()
            .getProperty(propname);
    _logger.debug("Got value for property '" + propname + "': " + val);
    if ((val == null) || (val.trim().length() < 1)) {
      throw new RuntimeException("Xyna property '" + propname + "' is not set.");
    }
    return val;
  }

}
