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
package com.gip.xyna.xact.trigger;

import javax.jms.JMSException;
import javax.jms.TextMessage;

import org.apache.log4j.Logger;

import xact.jms.JMSTextMessage;
import xprc.synchronization.CorrelationId;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;

public class JMSForwardingFilter extends ConnectionFilter<ActiveMQTriggerConnection> {

  private static final long serialVersionUID = 2530692844577054285L;
  private static Logger logger = CentralFactoryLogging.getLogger(JMSForwardingFilter.class);

  private static final String FORWARDING_TO_WORKFLOW = "xact.jms.ForwardDequeuedJMSMessage";
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
    if (tc.getMessage() instanceof TextMessage) {
      TextMessage tm = (TextMessage)tc.getMessage();
      try {
        return new XynaOrder(new DestinationKey(FORWARDING_TO_WORKFLOW), new CorrelationId(tm.getJMSCorrelationID()), new JMSTextMessage(tm.getText()));
      } catch (JMSException e) {
        throw new XynaException("error reading dequeued message", e);
      }
    }
    return null;
  }

  /**
   * called when above XynaOrder returns successfully.
   * @param response by XynaOrder returned XynaObject
   * @param tc corresponding triggerconnection
   */
  public void onResponse(XynaObject response, ActiveMQTriggerConnection tc) {
    //no response
  }

  /**
   * called when above XynaOrder returns with error or if an XynaException occurs in generateXynaOrder().
   * @param e
   * @param tc corresponding triggerconnection
   */
  public void onError(XynaException[] e, ActiveMQTriggerConnection tc) {
    if (e.length == 1) {
      logger.error(null, e[0]);
    } else {
      logger.error(null, new XynaException("several errors occured").initCauses(e));
    }
  }

  /**
   * @return description of this filter
   */
  public String getClassDescription() {
    return "forwards all dequeued textmessages to the workflow " + FORWARDING_TO_WORKFLOW;
  }

}
