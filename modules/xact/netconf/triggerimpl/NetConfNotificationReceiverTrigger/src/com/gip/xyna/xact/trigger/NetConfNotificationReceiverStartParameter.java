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


import java.util.List;
import java.util.Map;

import com.gip.xyna.utils.misc.Documentation;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.xact.exceptions.XACT_InvalidStartParameterCountException;
import com.gip.xyna.xact.exceptions.XACT_InvalidTriggerStartParameterValueException;
import com.gip.xyna.xact.trigger.NetConfConnection.HostKeyAuthMode;
import com.gip.xyna.xdev.xfractmod.xmdm.EnhancedStartParameter;
import com.gip.xyna.xdev.xfractmod.xmdm.StartParameter;


public class NetConfNotificationReceiverStartParameter extends EnhancedStartParameter {

  public static final long CloseConnectionList_RequestInterval = 1000; //RequestInterval (in ms) to check "All connections closed - Ready to stop trigger"
  public static final long Receive_RequestInterval = 100; //RequestInterval (in ms) for an empty notification queue (Trigger method "receive")
  public static final long Receive_NetConfOperation = 100; //RequestInterval (in ms) for an empty OutputQueueNetConfOperation List
  public static final long command_delay_before = 5000; //Delay between "client_hello" and "subscription_notification" (in ms)
  public static final long command_delay_after = 200; //Delay between "client_goodbye" and "closeNetconfConnection" (in ms)
  public static final long buffer_updatetime_offset = 500; //Maximum expected delay (in ms) between packets of the same notification 
  public static final long PushDelimiter_RequestInterval = 100; //RequestInterval (in ms) in push_delimiter (to proof maximum expected delay): PushDelimiter_RequestInterval < buffer_updatetime_offset
  public static final long buffer_maxlength = 1000000; //Maximum length of buffer for a continuous notification stream

  public static final int SessionTimeOut = 60000; //SSH-Session Timeout
  public static final String SECURE_STORE_DESTINATION_NETCONF = "netconf";
  
  
  private static final String DESCRIPTION_PORT = "Port of NetConf Call-Home-Feature";
  private static final String DESCRIPTION_USERNAME = "NetConf Credentials - Username (Example: \"netconf\")";
  private static final String DESCRIPTION_PASSWORD_KEY = "<key> for the actual NetConf Credentials Password. Must be set for " +
          "Host Key Authentication Mode 'direct'. The password corresponding to <key> needs to have been registered " +
          "in the xyna secure storage with <key> and keytype 'netconf', e.g. with:  " +
          "./xynafactory.sh securestore -keytype netconf -key <key> -value <password>";
  private static final String DESCRIPTION_HOSTKEY_MODUS = "Host Key Authentication Mode (Allowed Modes: " +
                                                          "'direct', 'none'). Value 'direct' requires password to be set";
  private static final String DESCRIPTION_REPLAYINMINUTES = "Replay time of subscription in minutes (Default value: 0)";
  private static final String DESCRIPTION_KEY_ALGORITHMS = "Optional colon-separated list of key algorithms for ssh connection, " +
                                                           "allowed values = " + SshjKeyAlgorithm.getDescription();
  private static final String DESCRIPTION_MAC_FACTORIES = "Optional colon-separated list of message authentication code factories " +
                                                           "for ssh connection, allowed values = " + SshjMacFactory.getDescription();

  public static final StringParameter<Integer> PARAM_PORT = StringParameter.typeInteger("port").mandatory().
    documentation(Documentation.en(DESCRIPTION_PORT).build()).build();
  public static final StringParameter<String> PARAM_USERNAME = StringParameter.typeString("username").mandatory().
    documentation(Documentation.en(DESCRIPTION_USERNAME).build()).build();
  public static final StringParameter<String> PARAM_PASSWORD_KEY = StringParameter.typeString("password_key").optional().
    documentation(Documentation.en(DESCRIPTION_PASSWORD_KEY).build()).build();
  public static final StringParameter<HostKeyAuthMode> PARAM_HOST_KEY_MODE =
    StringParameter.typeEnum(HostKeyAuthMode.class, "hostkey_mode").optional().defaultValue(HostKeyAuthMode.none).
    documentation(Documentation.en(DESCRIPTION_HOSTKEY_MODUS).build()).build();
  public static final StringParameter<Integer> PARAM_REPLAYINMINUTES = StringParameter.typeInteger("replay_in_minutes").
    optional().defaultValue(0).documentation(Documentation.en(DESCRIPTION_REPLAYINMINUTES).build()).build();
  public static final StringParameter<String> PARAM_KEY_ALGO = StringParameter.typeString("key_algorithms").optional().
    documentation(Documentation.en(DESCRIPTION_KEY_ALGORITHMS).build()).build();
  public static final StringParameter<String> PARAM_MAC_FACTORIES = StringParameter.typeString("mac_factories").optional().
    documentation(Documentation.en(DESCRIPTION_MAC_FACTORIES).build()).build();

  public static final List<StringParameter<?>> allParameters =
      StringParameter.asList(PARAM_PORT, PARAM_USERNAME, PARAM_PASSWORD_KEY, PARAM_HOST_KEY_MODE, PARAM_REPLAYINMINUTES,
                             PARAM_KEY_ALGO, PARAM_MAC_FACTORIES);

  private int port;
  private String username;
  private String passwordKey;
  private HostKeyAuthMode hostKeyAuthenticationMode;
  private long replayInMinutes; //Replay time of subscription in minutes. Default is 0.
  private List<SshjKeyAlgorithm> keyAlgorithms;
  private List<SshjMacFactory> macFactories;

  
  @Override
  public StartParameter build(Map<String, Object> map) throws XACT_InvalidTriggerStartParameterValueException {
    NetConfNotificationReceiverStartParameter ret = new NetConfNotificationReceiverStartParameter();
    ret.port = PARAM_PORT.getFromMap(map);
    ret.username = PARAM_USERNAME.getFromMap(map);
    ret.passwordKey = PARAM_PASSWORD_KEY.getFromMap(map);
    ret.hostKeyAuthenticationMode = PARAM_HOST_KEY_MODE.getFromMap(map);
    if ((ret.hostKeyAuthenticationMode == HostKeyAuthMode.direct) && (ret.passwordKey == null)) {
      throw new XACT_InvalidTriggerStartParameterValueException("Password must be set for host key auth mode 'direct'");
    }
    ret.replayInMinutes = PARAM_REPLAYINMINUTES.getFromMap(map);
    String keyAlgos = PARAM_KEY_ALGO.getFromMap(map);
    if (keyAlgos == null) {
      ret.keyAlgorithms = SshjKeyAlgorithm.getDefaults();
    } else {
      ret.keyAlgorithms = SshjKeyAlgorithm.parseColonSeparatedNameList(keyAlgos);
    }
    String macs = PARAM_MAC_FACTORIES.getFromMap(map);
    if (macs == null) {
      ret.macFactories = SshjMacFactory.getDefaults();
    } else {
      ret.macFactories = SshjMacFactory.parseColonSeparatedNameList(macs);
    }
    return ret;
  }


  @Override
  public List<String> convertToNewParameters(List<String> list)
      throws XACT_InvalidStartParameterCountException, XACT_InvalidTriggerStartParameterValueException {
    throw new XACT_InvalidTriggerStartParameterValueException("Start parameters must be provided in the following format: " + 
                                                              "<key1>=<value1> <key2>=<value2> ...");
  }


  @Override
  public List<StringParameter<?>> getAllStringParameters() {
    return allParameters;
  }
  
  /**
  * 
  * @return array of valid lists of descriptions of parameters. example: if parameters (A,B) and (A,C,D)
  *    are valid, then this method should return new String[]{{"descriptionA", "descriptionB"},
  *     {"descriptionA", "descriptionC", "descriptionD"}}
  */
  @Override
  public String[][] getParameterDescriptions() {
    return new String[][] { {
        DESCRIPTION_PORT, DESCRIPTION_USERNAME, DESCRIPTION_PASSWORD_KEY, DESCRIPTION_HOSTKEY_MODUS,
        DESCRIPTION_REPLAYINMINUTES, DESCRIPTION_KEY_ALGORITHMS, DESCRIPTION_MAC_FACTORIES
      } };
  }
  

  public int getPort() {
    return this.port;
  }


  public String getUsername() {
    return this.username;
  }


  public String getSecureStorageKey() {
    return this.passwordKey;
  }


  public HostKeyAuthMode getHostKeyAuthenticationMode() {
    return this.hostKeyAuthenticationMode;
  }

  public long getReplayInMinutes() {
    return this.replayInMinutes;
  }
  
  public List<SshjKeyAlgorithm> getKeyAlgorithms() {
    return keyAlgorithms;
  }
  
  public List<SshjMacFactory> getMacFactories() {
    return macFactories;
  }

}
