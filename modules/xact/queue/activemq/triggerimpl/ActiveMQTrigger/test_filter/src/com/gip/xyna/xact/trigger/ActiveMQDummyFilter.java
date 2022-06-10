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

package com.gip.xyna.xact.trigger;

import javax.jms.TextMessage;

import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import org.apache.log4j.Logger;


public class ActiveMQDummyFilter extends ConnectionFilter<ActiveMQTriggerConnection> {

  private static Logger _logger = CentralFactoryLogging.getLogger(ActiveMQDummyFilter.class);

  /**
   * analyzes TriggerConnection and creates XynaOrder if it accepts the connection.
   * if this filter does not return a XynaOrder, Xyna Processing will call generateXynaOrder()
   * of the next Filter registered for the Trigger
   * @param tc
   * @return XynaOrder which will be started by Xyna Processing. null if this Filter doesn't accept the connection
   * @throws XynaException caused by errors reading data from triggerconnection or having an internal error.
   *         results in onError() being called by Xyna Processing.
   * @throws InterruptedException if onError() should not be called.
   *        (e.g. if for a http trigger connection this filter decides, it wants to return a 500 servererror,
   *         and not call any workflow)
   */
  public XynaOrder generateXynaOrder(ActiveMQTriggerConnection tc) throws XynaException, InterruptedException {
    _logger.info("###### Entering ActiveMQDummyFilter.generateXynaOrder()");

    TextMessage msg = null;
    try {
      msg = (TextMessage) tc.getMessage();
    }
    catch (Exception e) {
      throw new RuntimeException("Received JMS message is no text message");
    }
    try {
      _logger.info("########## Received message with corrID = " + msg.getJMSCorrelationID() +
                   ", text = " + msg.getText());
    }
    catch (Exception e) {
      _logger.error("", e);
      throw new RuntimeException(e);
    }
    return null;
  }


  /**
   * called when above XynaOrder returns successfully.
   * @param response by XynaOrder returned XynaObject
   * @param tc corresponding triggerconnection
   */
  public void onResponse(XynaObject response, ActiveMQTriggerConnection tc) {
    //TODO implementation
    //TODO update dependency xml file
  }

  /**
   * called when above XynaOrder returns with error or if an XynaException occurs in generateXynaOrder().
   * @param e
   * @param tc corresponding triggerconnection
   */
  public void onError(XynaException[] e, ActiveMQTriggerConnection tc) {


    _logger.debug("Filter class loader: " + ActiveMQTriggerConnection.class.getClassLoader().toString());

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

}
