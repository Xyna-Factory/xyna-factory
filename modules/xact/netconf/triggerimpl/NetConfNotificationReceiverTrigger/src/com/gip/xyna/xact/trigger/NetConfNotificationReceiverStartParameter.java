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

package com.gip.xyna.xact.trigger;



import com.gip.xyna.xdev.xfractmod.xmdm.StartParameter;
import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xact.exceptions.XACT_InvalidStartParameterCountException;
import com.gip.xyna.xact.exceptions.XACT_InvalidTriggerStartParameterValueException;

import org.apache.log4j.Logger;


public class NetConfNotificationReceiverStartParameter implements StartParameter {

  private static Logger logger = CentralFactoryLogging.getLogger(NetConfNotificationReceiverStartParameter.class);

  // List of static parameters
  public static final long CloseConnectionList_RequestInterval = 1000; //RequestInterval (in ms) to check "All connections closed - Ready to stop trigger"
  public static final long Receive_RequestInterval = 100; //RequestInterval (in ms) for an empty notification queue (Trigger method "receive")
  public static final long Receive_NetConfOperation = 100; //RequestInterval (in ms) for an empty OutputQueueNetConfOperation List
  public static final long command_delay_before = 5000; //Delay between "client_hello" and "subscription_notification" (in ms)
  public static final long command_delay_after = 200; //Delay between "client_goodbye" and "closeNetconfConnection" (in ms)
  public static final long buffer_updatetime_offset = 500; //Maximum expected delay (in ms) between packets of the same notification 
  public static final long PushDelimiter_RequestInterval = 100; //RequestInterval (in ms) in push_delimiter (to proof maximum expected delay): PushDelimiter_RequestInterval < buffer_updatetime_offset
  public static final long buffer_maxlength = 1000000; //Maximum length of buffer for a continuous notification stream

  public static final int SessionTimeOut = 60000; //SSH-Session Timeout

  private int Port;
  private String Username;
  private String Password;
  private String Filter_Targe_WF;
  private String HostKeyAuthenticationMode; //Hostkey_Modus: "direct", "none" (default)
  private long ReplayInMinutes; //Replay time of subscription in minutes. Default is 0.


  // the empty constructor may not be removed or throw exceptions! additional ones are possible, though.
  public NetConfNotificationReceiverStartParameter() {
  }


  public NetConfNotificationReceiverStartParameter(int port, String username, String password, String Filter_Targe_WF,
                                                   String hostkeymodus, long replayinminutes) {
    this.Port = port;
    this.Username = username;
    this.Password = password;
    this.Filter_Targe_WF = Filter_Targe_WF;
    this.HostKeyAuthenticationMode = hostkeymodus;
    this.ReplayInMinutes = replayinminutes;
  }


  /**
  * Is called by XynaProcessing with the parameters provided by the deployer
  * @return StartParameter Instance which is used to instantiate corresponding Trigger
  */
  public StartParameter build(String... args)
      throws XACT_InvalidStartParameterCountException, XACT_InvalidTriggerStartParameterValueException {
    Integer port = 4334; // Default Port value for NetConf call-home-feature
    String username = ""; // Default NetConf username
    String password = ""; //Default NetConf password
    String Filter_Targe_WF = ""; // Default Filter Target-WF
    String hostkeymodus = "none"; // Default hostkey_modus
    long replayinminutes = 0; // Default replay time

    if (!((args.length == 6) | (args.length == 5))) {
      throw new XACT_InvalidStartParameterCountException();
    } else {
      if (args.length == 6) {
        try {
          port = Integer.parseInt(args[0]);
        } catch (Exception e) {
          throw new XACT_InvalidTriggerStartParameterValueException(args[0], e);
        }
        try {
          username = args[1];
        } catch (Exception e) {
          throw new XACT_InvalidTriggerStartParameterValueException(args[1], e);
        }
        try {
          password = args[2];
        } catch (Exception e) {
          throw new XACT_InvalidTriggerStartParameterValueException(args[2], e);
        }
        try {
          Filter_Targe_WF = args[3];
        } catch (Exception e) {
          throw new XACT_InvalidTriggerStartParameterValueException(args[3], e);
        }
        try {
          hostkeymodus = args[4];
          if (!hostkeymodus.equalsIgnoreCase("none")) {
            throw new XACT_InvalidTriggerStartParameterValueException(args[4]);
          }
        } catch (Exception e) {
          throw new XACT_InvalidTriggerStartParameterValueException(args[4], e);
        }
        try {
          replayinminutes = Integer.parseInt(args[5]);
        } catch (Exception e) {
          throw new XACT_InvalidTriggerStartParameterValueException(args[5], e);
        }
      } else {
        try {
          port = Integer.parseInt(args[0]);
        } catch (Exception e) {
          throw new XACT_InvalidTriggerStartParameterValueException(args[0], e);
        }
        try {
          username = args[1];
        } catch (Exception e) {
          throw new XACT_InvalidTriggerStartParameterValueException(args[1], e);
        }
        try {
          Filter_Targe_WF = args[2];
        } catch (Exception e) {
          throw new XACT_InvalidTriggerStartParameterValueException(args[2], e);
        }
        try {
          hostkeymodus = args[3];
          if (!((hostkeymodus.equalsIgnoreCase("direct"))
              | (hostkeymodus.equalsIgnoreCase("none")) | (hostkeymodus.equalsIgnoreCase("test")))) {
            throw new XACT_InvalidTriggerStartParameterValueException(args[3]);
          }
        } catch (Exception e) {
          throw new XACT_InvalidTriggerStartParameterValueException(args[3], e);
        }
        try {
          replayinminutes = Integer.parseInt(args[4]);
        } catch (Exception e) {
          throw new XACT_InvalidTriggerStartParameterValueException(args[4], e);
        }
        password = "";
      }
    }
    return new NetConfNotificationReceiverStartParameter(port, username, password, Filter_Targe_WF, hostkeymodus, replayinminutes);
  }


  private static final String DESCRIPTION_PORT = "Port of NetConf Call-Home-Feature (Default value: 4334)";
  private static final String DESCRIPTION_USERNAME = "NetConf Credentials - Username (Example: \"netconf\")";
  private static final String DESCRIPTION_PASSWORD = "NetConf Credentials - Password (Example: \"netconf\")";
  private static final String DESCRIPTION_FILTER_TARGET_WF =
      "Target-Workflow for Notification-Filter (Example: \"test.testWF.EventReceived\")";
  private static final String DESCRIPTION_HOSTKEY_MODUS_I =
      "Host Key Authentication Mode (Allowed Modes: \"direct\", \"none\")";
  private static final String DESCRIPTION_HOSTKEY_MODUS_II = "Host Key Authentication Mode (Allowed Modes: \"none\")";
  private static final String DESCRIPTION_REPLAYINMINUTES = "Replay time of subscription in minutes (Default value: 0)";

  /**
  * 
  * @return array of valid lists of descriptions of parameters. example: if parameters (A,B) and (A,C,D)
  *    are valid, then this method should return new String[]{{"descriptionA", "descriptionB"},
  *     {"descriptionA", "descriptionC", "descriptionD"}}
  */
  public String[][] getParameterDescriptions() {
    return new String[][] {
        {DESCRIPTION_PORT, DESCRIPTION_USERNAME, DESCRIPTION_PASSWORD, DESCRIPTION_FILTER_TARGET_WF, DESCRIPTION_HOSTKEY_MODUS_II, DESCRIPTION_REPLAYINMINUTES},
        {DESCRIPTION_PORT, DESCRIPTION_USERNAME, DESCRIPTION_FILTER_TARGET_WF, DESCRIPTION_HOSTKEY_MODUS_I, DESCRIPTION_REPLAYINMINUTES}};
  }


  public int getPort() {
    return this.Port;
  }


  public String getUsername() {
    return this.Username;
  }


  public String getPassword() {
    return this.Password;
  }


  public String getFilterTargetWF() {
    return this.Filter_Targe_WF;
  }


  public String getHostKeyAuthenticationMode() {
    return this.HostKeyAuthenticationMode;
  }

  public long getReplayInMinutes() {
    return this.ReplayInMinutes;
  }

}
