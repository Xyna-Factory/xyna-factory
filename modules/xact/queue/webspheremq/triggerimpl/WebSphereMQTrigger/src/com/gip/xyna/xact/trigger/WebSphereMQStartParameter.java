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



import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xact.exceptions.XACT_InvalidStartParameterCountException;
import com.gip.xyna.xact.exceptions.XACT_InvalidTriggerStartParameterValueException;
import com.gip.xyna.xact.trigger.exception.WebSphereMQTrigger_WrongQueueTypeException;
import com.gip.xyna.xdev.xfractmod.xmdm.StartParameter;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.*;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;



public class WebSphereMQStartParameter implements StartParameter {

  private static Logger _logger = CentralFactoryLogging.getLogger(WebSphereMQStartParameter.class);

  private static final String DESCRIPTION_UNIQUE_QUEUENAME = "unique queuename from queue management";
   private static final String DESCRIPTION_RECONNECTINTERVAL_ERROR =
    "interval in milliseconds in which retries occur, when trying to rebuild the connection " +
    "to the queue after an error.";
  private static final String DESCRIPTION_AUTORECONNECT =
    "true/false: should the trigger try to reconnect after an error";
  private static final String DESCRIPTION_RECEIVETIMEOUT =
    "timeout (milliseconds) when trying to get the next message from the queue";
  private static final String DESCRIPTION_RECEIVE_ASYNCHRONOUS = "receive messages asynchronously (default=false)";
  private static final String DESCRIPTION_USE_SSL = "use ssl encryption (default=false)";
  private static final String DESCRIPTION_XYNA_PROPERTIES_FOR_SSL_PREFIX = "prefix for the names of xyna properties" +
    " that will be used to determine ssl configurations; the actual property names will be" +
    " concatenations of that prefix and each of the following suffixes, respectively:" +
    " ['.cipher.suite', '.truststore.path', '.truststore.password', '.keystore.path', '.keystore.password']";
  private static final String DESCRIPTION_ERROR_QUEUENAME = "queue (unique queuename from queue management) to" +
    " which rejected messages will be sent";
  private static final String DESCRIPTION_ERROR_QUEUENAME_REDUNDANT = "queue (unique queuename from queue management)" +
      " to which rejected messages will be sent if sending to first error queue fails";  
  private static final String DESCRIPTION_USERNAME = "username for wmq manager (default=MUSR_MQADMIN)";
  private static final String DESCRIPTION_PASSWORD = "password for wmq manager (default is empty)";


  private String queueManager;
  private String hostname;
  private int port;
  private String queueName;
  private String channel;

  private Queue errorQueue;
  private Queue errorQueueRedundant;

  // key name with which the queue data (incl. actual, external queue name) is registered in the xyna factory
  private String xynaQueueMgmtQueueName;

  // milliseconds
  private int reconnectIntervalAfterError;
  private boolean autoReconnect;
  // milliseconds
  private int receiveTimeout;

  private boolean asyncReceive;

  private boolean useSSL = false;
  private String xynaPropertiesForSSLPrefix;

  private String userName = "MUSR_MQADMIN";
  private String password = "";
  

  public WebSphereMQStartParameter() {
  }


  public WebSphereMQStartParameter(String uniqueName, int reconnectIntervalAfterError, boolean autoReconnect,
         int receiveTimeout, boolean asyncReceive) throws XACT_InvalidTriggerStartParameterValueException {
    Queue queue = getStoredQueue(uniqueName);
    initQueueData(queue);

    this.reconnectIntervalAfterError = reconnectIntervalAfterError;
    this.autoReconnect = autoReconnect;
    this.receiveTimeout = receiveTimeout;
    this.asyncReceive = asyncReceive;
    this.xynaQueueMgmtQueueName = uniqueName;
  }


  public WebSphereMQStartParameter(String uniqueName, int reconnectIntervalAfterError, boolean autoReconnect,
         int receiveTimeout, boolean asyncReceive, String xynaPropertyPrefix)
                         throws XACT_InvalidTriggerStartParameterValueException {
    Queue queue = getStoredQueue(uniqueName);
    initQueueData(queue);
    this.reconnectIntervalAfterError = reconnectIntervalAfterError;
    this.autoReconnect = autoReconnect;
    this.receiveTimeout = receiveTimeout;
    this.asyncReceive = asyncReceive;
    this.xynaQueueMgmtQueueName = uniqueName;
    this.useSSL = true;
    this.xynaPropertiesForSSLPrefix = xynaPropertyPrefix;
  }


  /**
   * Is called by XynaProcessing with the parameters provided by the deployer
   * @return StartParameter Instance which is used to instantiate corresponding Trigger
   */
  public StartParameter build(String... args) throws XACT_InvalidTriggerStartParameterValueException,
      XACT_InvalidStartParameterCountException {
    if (args == null || args.length < 4) {
      throw new XACT_InvalidStartParameterCountException();
    }
    int reconnectionInterval;
    try {
      reconnectionInterval = Integer.valueOf(args[1]);
    }
    catch (NumberFormatException e) {
      throw new XACT_InvalidTriggerStartParameterValueException(args[1], e);
    }
    int timeout;
    try {
      timeout = Integer.valueOf(args[3]);
    }
    catch (NumberFormatException e) {
      throw new XACT_InvalidTriggerStartParameterValueException(args[3], e);
    }

    if (args.length == 4) {
      return new WebSphereMQStartParameter(args[0], reconnectionInterval, Boolean.valueOf(args[2]),
                                          timeout, false);
    }
    else if (args.length == 5) {
      return new WebSphereMQStartParameter(args[0], reconnectionInterval, Boolean.valueOf(args[2]),
                                        timeout, Boolean.valueOf(args[4]));
    }
    else if (args.length == 7) {
      boolean doUseSSL = Boolean.valueOf(args[5]);
      if (!doUseSSL) {
        return new WebSphereMQStartParameter(args[0], reconnectionInterval, Boolean.valueOf(args[2]),
                                             timeout, Boolean.valueOf(args[4]));
      }
      return new WebSphereMQStartParameter(args[0], reconnectionInterval, Boolean.valueOf(args[2]),
                                        timeout, Boolean.valueOf(args[4]), args[6]);
    }
    else if (args.length == 8) {
      WebSphereMQStartParameter params;
      boolean doUseSSL = Boolean.valueOf(args[5]);
      if (!doUseSSL) {
        params = new WebSphereMQStartParameter(args[0], reconnectionInterval, Boolean.valueOf(args[2]),
                                               timeout, Boolean.valueOf(args[4]));
      } else {
        params = new WebSphereMQStartParameter(args[0], reconnectionInterval, Boolean.valueOf(args[2]),
                                               timeout, Boolean.valueOf(args[4]), args[6]);
      }
      params.errorQueue = getStoredQueue(args[7]);
      params.errorQueueRedundant = null;
      return params;
    }
    else if (args.length == 9) {
      WebSphereMQStartParameter params;
      boolean doUseSSL = Boolean.valueOf(args[5]);
      if (!doUseSSL) {
        params = new WebSphereMQStartParameter(args[0], reconnectionInterval, Boolean.valueOf(args[2]),
                                               timeout, Boolean.valueOf(args[4]));
      } else {
        params = new WebSphereMQStartParameter(args[0], reconnectionInterval, Boolean.valueOf(args[2]),
                                               timeout, Boolean.valueOf(args[4]), args[6]);
      }
      params.errorQueue = getStoredQueue(args[7]);
      params.errorQueueRedundant = getStoredQueue(args[8]);
      return params;
    } else if (args.length == 11) {
      WebSphereMQStartParameter params;
      boolean doUseSSL = Boolean.valueOf(args[5]);
      if (!doUseSSL) {
        params = new WebSphereMQStartParameter(args[0], reconnectionInterval, Boolean.valueOf(args[2]),
                                               timeout, Boolean.valueOf(args[4]));
      } else {
        params = new WebSphereMQStartParameter(args[0], reconnectionInterval, Boolean.valueOf(args[2]),
                                               timeout, Boolean.valueOf(args[4]), args[6]);
      }
      params.errorQueue = getStoredQueue(args[7]);
      params.errorQueueRedundant = getStoredQueue(args[8]);
      params.userName = args[9];
      params.password = args[10];
      return params;
    }
    throw new XACT_InvalidStartParameterCountException();
  }


  public int getReconnectIntervalAfterError() {
    return reconnectIntervalAfterError;
  }


  public boolean isAutoReconnect() {
    return autoReconnect;
  }


  public int getReceiveTimeout() {
    return receiveTimeout;
  }


  public String getQueueManager() {
    return queueManager;
  }


  public String getHostname() {
    return hostname;
  }


  public int getPort() {
    return port;
  }


  public String getQueueName() {
    return queueName;
  }


  public String getChannel() {
    return channel;
  }

  public String getXynaQueueMgmtQueueName() {
    return xynaQueueMgmtQueueName;
  }

  public boolean receiveAsynchronously() {
    return asyncReceive;
  }

  public boolean getUseSSL() {
    return useSSL;
  }

  public String getXynaPropertiesForSSLPrefix() {
    return xynaPropertiesForSSLPrefix;
  }

  public Queue getErrorQueue() {
    return errorQueue;
  }

  public Queue getErrorQueueRedundant() {
    return errorQueueRedundant;
  }



  private String getParameterDescriptionAsString() {
    StringBuffer sb = new StringBuffer();
    sb.append("Valid Parameters are:\n");
    int cnt = 1;
    for (String[] pd : getParameterDescriptions()) {
      sb.append("  ").append(cnt).append(". possible parameter set:\n");
      for (String p : pd) {
        sb.append("     o ").append(p).append("\n");
      }
      cnt++;
    }
    return sb.toString();
  }


  /**
   * @return array of valid lists of descriptions of parameters. example: if parameters (A,B) and (A,C,D) are valid,
   *         then this method should return new String[]{{"descriptionA", "descriptionB"}, {"descriptionA",
   *         "descriptionC", "descriptionD"}}
   */
  public String[][] getParameterDescriptions() {
    return new String[][] {
        {DESCRIPTION_UNIQUE_QUEUENAME, DESCRIPTION_RECONNECTINTERVAL_ERROR,
            DESCRIPTION_AUTORECONNECT, DESCRIPTION_RECEIVETIMEOUT},
        {DESCRIPTION_UNIQUE_QUEUENAME, DESCRIPTION_RECONNECTINTERVAL_ERROR,
            DESCRIPTION_AUTORECONNECT, DESCRIPTION_RECEIVETIMEOUT,
            DESCRIPTION_RECEIVE_ASYNCHRONOUS},
        {DESCRIPTION_UNIQUE_QUEUENAME, DESCRIPTION_RECONNECTINTERVAL_ERROR,
            DESCRIPTION_AUTORECONNECT, DESCRIPTION_RECEIVETIMEOUT,
            DESCRIPTION_RECEIVE_ASYNCHRONOUS,
            DESCRIPTION_USE_SSL, DESCRIPTION_XYNA_PROPERTIES_FOR_SSL_PREFIX},
        {DESCRIPTION_UNIQUE_QUEUENAME, DESCRIPTION_RECONNECTINTERVAL_ERROR,
            DESCRIPTION_AUTORECONNECT, DESCRIPTION_RECEIVETIMEOUT,
            DESCRIPTION_RECEIVE_ASYNCHRONOUS,
            DESCRIPTION_USE_SSL, DESCRIPTION_XYNA_PROPERTIES_FOR_SSL_PREFIX,
            DESCRIPTION_ERROR_QUEUENAME},
            {DESCRIPTION_UNIQUE_QUEUENAME, DESCRIPTION_RECONNECTINTERVAL_ERROR,
              DESCRIPTION_AUTORECONNECT, DESCRIPTION_RECEIVETIMEOUT,
              DESCRIPTION_RECEIVE_ASYNCHRONOUS,
              DESCRIPTION_USE_SSL, DESCRIPTION_XYNA_PROPERTIES_FOR_SSL_PREFIX,
              DESCRIPTION_ERROR_QUEUENAME,
            DESCRIPTION_ERROR_QUEUENAME_REDUNDANT},
        {DESCRIPTION_UNIQUE_QUEUENAME, DESCRIPTION_RECONNECTINTERVAL_ERROR, 
              DESCRIPTION_AUTORECONNECT, DESCRIPTION_RECEIVETIMEOUT,
              DESCRIPTION_RECEIVE_ASYNCHRONOUS, DESCRIPTION_USE_SSL,
              DESCRIPTION_XYNA_PROPERTIES_FOR_SSL_PREFIX,
              DESCRIPTION_ERROR_QUEUENAME, DESCRIPTION_ERROR_QUEUENAME_REDUNDANT, 
              DESCRIPTION_USERNAME, DESCRIPTION_PASSWORD}};
  }


  private static Queue getStoredQueue(String uniqueName) throws XACT_InvalidTriggerStartParameterValueException {
    if (uniqueName == null || uniqueName.trim().length() == 0) {
      return null;
    }
    try {
      QueueManagement mgmt = new QueueManagement();
      Queue ret = mgmt.getQueue(uniqueName);

      _logger.debug("Got Stored Queue: " + ret.toString());
      if (ret.getQueueType() != QueueType.WEBSPHERE_MQ) {
        throw new XACT_InvalidTriggerStartParameterValueException(uniqueName,
            new WebSphereMQTrigger_WrongQueueTypeException(uniqueName));
      }
      return ret;
    }
    catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new XACT_InvalidTriggerStartParameterValueException(uniqueName, e);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  private void initQueueData(Queue queue) throws XACT_InvalidTriggerStartParameterValueException {
    WebSphereMQConnectData connData = null;
    try {
      connData = (WebSphereMQConnectData) queue.getConnectData();
    }
    catch (Exception e) {
      throw new XACT_InvalidTriggerStartParameterValueException(queue.getUniqueName(),
            new WebSphereMQTrigger_WrongQueueTypeException(queue.getUniqueName()));
    }
    this.hostname = connData.getHostname();
    this.channel = connData.getChannel();
    this.port = connData.getPort();
    this.queueManager = connData.getQueueManager();
    this.queueName = queue.getExternalName();
  }


  public String getUserName() {
    return userName;
  }
  
  public String getPassword() {
    return password;
  }

}
