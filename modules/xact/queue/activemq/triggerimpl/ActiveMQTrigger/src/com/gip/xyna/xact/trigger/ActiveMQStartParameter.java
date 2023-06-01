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

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xact.exceptions.XACT_InvalidStartParameterCountException;
import com.gip.xyna.xact.exceptions.XACT_InvalidTriggerStartParameterValueException;
import com.gip.xyna.xdev.xfractmod.xmdm.StartParameter;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.*;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;


public class ActiveMQStartParameter implements StartParameter {

  private static Logger logger = CentralFactoryLogging.getLogger(ActiveMQStartParameter.class);

  private String hostname;
  private int port;
  private String queueName;
  //milliseconds
  private int reconnectIntervalAfterError = 3000;
  private boolean autoReconnect = true;
  //milliseconds
  private int receiveTimeout = 3000;

  private boolean useSsl = false;
  private String keystore = null;
  private String keystorePassword = null;
  private String truststore = null;
  private String truststorePassword = null;


  // the empty constructor may not be removed or throw exceptions! additional ones are possible, though.
  public ActiveMQStartParameter() {
  }


  public ActiveMQStartParameter(String uniqueName) throws XACT_InvalidTriggerStartParameterValueException {
    Queue queue = getStoredQueue(uniqueName);
    ActiveMQConnectData connData = null;
    try {
      connData = (ActiveMQConnectData) queue.getConnectData();
    }
    catch (Exception e) {
      throw new XACT_InvalidTriggerStartParameterValueException(uniqueName, e);
    }

    this.hostname = connData.getHostname();
    this.port = connData.getPort();
    this.queueName = queue.getExternalName();
  }


  public ActiveMQStartParameter(String uniqueName, int reconnectIntervalAfterError, boolean autoReconnect,
                                int receiveTimeout) throws XACT_InvalidTriggerStartParameterValueException {
    this(uniqueName);
    this.reconnectIntervalAfterError = reconnectIntervalAfterError;
    this.autoReconnect = autoReconnect;
    this.receiveTimeout = receiveTimeout;
  }


  public ActiveMQStartParameter(String uniqueName, int reconnectIntervalAfterError, boolean autoReconnect,
                                int receiveTimeout, boolean useSsl, String keystore, String keystorePassword,
                                String truststore, String truststorePassword)
                                throws XACT_InvalidTriggerStartParameterValueException {
    this(uniqueName, reconnectIntervalAfterError, autoReconnect, receiveTimeout);
    this.useSsl = useSsl;
    this.keystore = keystore;
    this.keystorePassword = keystorePassword;
    this.truststore = truststore;
    this.truststorePassword = truststorePassword;
  }


  /**
  * Is called by XynaProcessing with the parameters provided by the deployer
  * @return StartParameter Instance which is used to instantiate corresponding Trigger
  */
  public StartParameter build(String ... args) throws XACT_InvalidStartParameterCountException,
                                                      XACT_InvalidTriggerStartParameterValueException {
    if (args.length < 1) {
      throw new XACT_InvalidStartParameterCountException();
    }
    if (args.length == 1) {
      return new ActiveMQStartParameter(args[0]);
    }
    int reconnectInterval = 0;
    int timeout = 0;
    try {
      reconnectInterval = Integer.parseInt(args[1]);
    }
    catch (Exception e) {
      throw new XACT_InvalidTriggerStartParameterValueException(args[1], e);
    }
    try {
      timeout = Integer.parseInt(args[3]);
    }
    catch (Exception e) {
      throw new XACT_InvalidTriggerStartParameterValueException(args[3], e);
    }

    if (args.length == 4) {
      return new ActiveMQStartParameter(args[0], reconnectInterval, Boolean.valueOf(args[2]), timeout);
    }
    if (args.length == 9) {
      return new ActiveMQStartParameter(args[0], reconnectInterval, Boolean.valueOf(args[2]), timeout,
                                        Boolean.valueOf(args[4]), args[5], args[6], args[7], args[8]);
    }

    throw new XACT_InvalidStartParameterCountException();
  }



  private static final String DESCRIPTION_UNIQUENAME =
        "Unique name of queue as registered in xyna queuemanagement";
  private static final String DESCRIPTION_RECONNECTINTERVAL_ERROR = "interval in milliseconds in which " +
        "retries occur, when trying to rebuild the connection to the queue after an error. (default = 3000)";
  private static final String DESCRIPTION_AUTORECONNECT =
        "true/false: should the trigger try to reconnect after an error (default = true)";
  private static final String DESCRIPTION_RECEIVETIMEOUT =
        "timeout (milliseconds) when trying to get the next message from the queue (default = 3000)";
  private static final String DESCRIPTION_USE_SSL =
        "true/false: should ssl connection be used (default: false)";
  private static final String DESCRIPTION_KEYSTORE =
        "filename (including path) of keystore that will be used for ssl connection " +
        "(WARNING: Global Java system property will be set with that value!)";
  private static final String DESCRIPTION_KEYSTORE_PASSWORD = "password for keystore " +
        "(WARNING: Global Java system property will be set with that value!)";
  private static final String DESCRIPTION_TRUSTSTORE =
        "filename (including path) of truststore that will be used for ssl connection " +
        "(WARNING: Global Java system property will be set with that value!)";
  private static final String DESCRIPTION_TRUSTSTORE_PASSWORD = "password for truststore " +
        "(WARNING: Global Java system property will be set with that value!)";


  /**
  *
  * @return array of valid lists of descriptions of parameters. example: if parameters (A,B) and (A,C,D)
  *    are valid, then this method should return new String[]{{"descriptionA", "descriptionB"},
  *     {"descriptionA", "descriptionC", "descriptionD"}}
  */
  public String[][] getParameterDescriptions() {
    return new String[][]{
                    {DESCRIPTION_UNIQUENAME},
                    {DESCRIPTION_UNIQUENAME, DESCRIPTION_RECONNECTINTERVAL_ERROR, DESCRIPTION_AUTORECONNECT,
                      DESCRIPTION_RECEIVETIMEOUT},
                    {DESCRIPTION_UNIQUENAME, DESCRIPTION_RECONNECTINTERVAL_ERROR, DESCRIPTION_AUTORECONNECT,
                        DESCRIPTION_RECEIVETIMEOUT, DESCRIPTION_USE_SSL, DESCRIPTION_KEYSTORE,
                        DESCRIPTION_KEYSTORE_PASSWORD, DESCRIPTION_TRUSTSTORE, DESCRIPTION_TRUSTSTORE_PASSWORD}
    };
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
      cnt ++;
    }
    return sb.toString();
  }


  private static Queue getStoredQueue(String uniqueName) throws XACT_InvalidTriggerStartParameterValueException {
    try {
      QueueManagement mgmt = new QueueManagement();
      Queue ret = mgmt.getQueue(uniqueName);

      logger.debug("Got Stored Queue: " + ret.toString());
      if (ret.getQueueType() != QueueType.ACTIVE_MQ) {
        throw new XACT_InvalidTriggerStartParameterValueException(uniqueName,
              new RuntimeException("Wrong queue type"));
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


  public String getHostname() {
    return hostname;
  }


  public int getPort() {
    return port;
  }


  public String getQueueName() {
    return queueName;
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

  public boolean isUseSsl() {
    return useSsl;
  }


  public String getKeystore() {
    return keystore;
  }


  public String getKeystorePassword() {
    return keystorePassword;
  }


  public String getTruststore() {
    return truststore;
  }


  public String getTruststorePassword() {
    return truststorePassword;
  }

}
