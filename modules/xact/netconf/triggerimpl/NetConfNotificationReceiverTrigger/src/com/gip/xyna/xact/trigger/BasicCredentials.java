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

import com.gip.xyna.xact.trigger.NetConfConnection.HostKeyAuthMode;


public class BasicCredentials {

  private String netconf_username;
  private String netconf_password;
  private HostKeyAuthMode netconf_HostKeyAuthenticationMode; //Hostkey_Modus: "direct", "none" (default)
  private long netconf_replayinminutes;
  
  private List<SshjKeyAlgorithm> keyAlgorithms;
  private List<SshjMacFactory> macFactories;
  
  
  public void setUserame(String username) {
    netconf_username = username;
  };


  public void setPassword(String password) {
    netconf_password = password;
  };


  public String getUsername() {
    return netconf_username;
  };


  public String getPassword() {
    return netconf_password;
  };


  public void setHostKeyAuthenticationMode(HostKeyAuthMode authenticationmode) {
    netconf_HostKeyAuthenticationMode = authenticationmode;
  };


  public HostKeyAuthMode getHostKeyAuthenticationMode() {
    return netconf_HostKeyAuthenticationMode;
  };

  
  public void setReplayInMinutes(long replayinminutes) {
    netconf_replayinminutes = replayinminutes;
  };


  public long getReplayInMinutes() {
    return netconf_replayinminutes;
  }

  
  public List<SshjKeyAlgorithm> getKeyAlgorithms() {
    return keyAlgorithms;
  }

  
  public void setKeyAlgorithms(List<SshjKeyAlgorithm> keyAlgorithms) {
    this.keyAlgorithms = keyAlgorithms;
  }

  
  public List<SshjMacFactory> getMacFactories() {
    return macFactories;
  }

  
  public void setMacFactories(List<SshjMacFactory> macFactories) {
    this.macFactories = macFactories;
  };

}
