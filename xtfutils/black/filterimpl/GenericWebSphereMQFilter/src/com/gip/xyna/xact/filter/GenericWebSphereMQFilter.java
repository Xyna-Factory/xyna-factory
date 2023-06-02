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
package com.gip.xyna.xact.filter;

import javax.jms.BytesMessage;
import javax.jms.TextMessage;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.trigger.WebSphereMQTriggerConnection;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;

import xact.WebSphereMQ.datatypes.WebSphereMQMessage;


public class GenericWebSphereMQFilter extends ConnectionFilter<WebSphereMQTriggerConnection> {

  public static class Constant {
    public static class XynaPropertyName {
      //public static final String DESTINATION_KEY = "xtf.utils.genericWebSphereMQFilter.destinationKey";
      public static final String DESTINATION_KEY_BASE = "xtf.utils.genericWebSphereMQFilter.destinationKey.base";
    }
  }

  private static Logger _logger = CentralFactoryLogging.getLogger(GenericWebSphereMQFilter.class);

  /**
   * Analyzes TriggerConnection and creates XynaOrder if it accepts the connection.
   * The method return a FilterResponse object, which can include the XynaOrder if the filter is responsibleb for the request.
   * # If this filter is not responsible the returned object must be: FilterResponse.notResponsible()
   * # If this filter is responsible the returned object must be: FilterResponse.responsible(XynaOrder order)
   * # If this filter is responsible but it handle the request without creating a XynaOrder the
   *   returned object must be: FilterResponse.responsibleWithoutXynaorder()
   * # If this filter is responsible but the version of this filter is too new the returned
   *    object must be: FilterResponse.responsibleButTooNew(). The trigger will try an older version of the filter.
   * @param tc
   * @return FilterResponse object
   * @throws XynaException caused by errors reading data from triggerconnection or having an internal error.
   *         results in onError() being called by Xyna Processing.
   */
  public FilterResponse createXynaOrder(WebSphereMQTriggerConnection tc) throws XynaException {
    //return FilterResponse.notResponsible() if next filter should be tried
    _logger.info("#### GenericWebsphereMQFilter: Generate Xyna order called");

    XynaOrder xynaOrder = null;

    try {
      String msg = null;
      if (tc.getMessage() instanceof TextMessage) {
        TextMessage tm = (TextMessage) tc.getMessage();
        msg = tm.getText();

        _logger.debug("JMS TIMESTAMP =" + tm.getJMSTimestamp());
        _logger.debug("Received TextMessage from Queue.");
      }
      else if (tc.getMessage() instanceof BytesMessage) {
        BytesMessage bm = (BytesMessage) tc.getMessage();
        msg = bm.readUTF();

        _logger.debug("JMS TIMESTAMP =" + bm.getJMSTimestamp());
        _logger.debug("Received BytesMessage from Queue.");
      }
      else {
        _logger.error("Dequeued message is not instance of TextMessage or BytesMessage: "
                      + tc.getMessage().toString().replace("" + '\n', " ### "));
        return FilterResponse.responsibleWithoutXynaorder();
      }

      //QueueName queueName = new QueueName();
      //queueName.setQueueName(tc.getXynaQueueMgmtQueueName());

      WebSphereMQMessage mqMsg = new WebSphereMQMessage();
      mqMsg.setMessage(msg);
      mqMsg.setCorrelationID(tc.getMessage().getJMSCorrelationID());

      String queueNameStr = tc.getXynaQueueMgmtQueueName();
      String adjustedQueueName = queueNameStr.substring(0, 1).toUpperCase() + queueNameStr.substring(1);
      String wfName = getXynaProperty(Constant.XynaPropertyName.DESTINATION_KEY_BASE) + "." +
                      adjustedQueueName;
      _logger.info("Going to start WF: " + wfName);
      DestinationKey destKey = new DestinationKey(wfName);
      //xynaOrder = new XynaOrder(destKey, mqMsg, queueName);
      xynaOrder = new XynaOrder(destKey, mqMsg);
      return FilterResponse.responsible(xynaOrder);
    }
    catch (Exception e) {
      _logger.error("", e);
      throw new XynaException("Error in Filter", e);
    }
    //return FilterResponse.notResponsible();
  }

  /**
   * called when above XynaOrder returns successfully.
   * @param response by XynaOrder returned XynaObject
   * @param tc corresponding triggerconnection
   */
  public void onResponse(XynaObject response, WebSphereMQTriggerConnection tc) {
    //TODO implementation
    //TODO update dependency xml file
  }

  /**
   * called when above XynaOrder returns with error or if an XynaException occurs in generateXynaOrder().
   * @param e
   * @param tc corresponding triggerconnection
   */
  public void onError(XynaException[] e, WebSphereMQTriggerConnection tc) {
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


  private static String getXynaProperty(String propname) {
    try {
      String val = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().
                      getConfiguration().getProperty(propname);
      _logger.debug("Got value for property '" + propname + "': " + val);
      if ((val == null) || (val.trim().length() < 1)) {
        throw new RuntimeException("Xyna property '" + propname + "' is not set.");
      }
      return val;
    }
    catch (RuntimeException e) {
      _logger.error("", e);
      throw e;
    }
    catch (Exception e) {
      _logger.error("", e);
      throw new RuntimeException("Error while trying to read xyna property '" + propname + "'.");
    }
  }

}
