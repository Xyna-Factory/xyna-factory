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

package xact.jms.activemq.service;

import org.apache.log4j.Logger;

import com.gip.xyna.utils.exceptions.ActiveMQAdapterException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;

import xact.jms.activemq.datatypes.CorrelationID;
import xact.jms.activemq.datatypes.TimeoutInMillis;
import xact.jms.activemq.datatypes.XynaQueueMgmtQueueName;


public class CSReceiveFromActiveMQ {

  private static Logger _logger = Logger.getLogger(CSSendToActiveMQ.class);


  protected String getCodedServiceName() {
    return "receiveFromActiveMQ";
  }


  public Container execute(XynaQueueMgmtQueueName xynaQueueMgmtQueueName, CorrelationID correlationID,
                           TimeoutInMillis timeoutInMillis) throws XynaException {
    try {
      _logger.debug("### Entering Coded service " + getCodedServiceName());
      return executeImpl(xynaQueueMgmtQueueName, correlationID, timeoutInMillis);
    }
    catch (Throwable t) {
      _logger.error("Error in coded service " + getCodedServiceName(), t);
      throw new ActiveMQAdapterException("Error in coded service " + getCodedServiceName() +
                                                     ":" + t.getMessage(), t);
    }
  }


  private Container executeImpl(XynaQueueMgmtQueueName xynaQueueMgmtQueueName, CorrelationID correlationID,
                                TimeoutInMillis timeoutInMillis) throws XynaException {
    // Implemented as code snippet!
    return null;
  }

}
