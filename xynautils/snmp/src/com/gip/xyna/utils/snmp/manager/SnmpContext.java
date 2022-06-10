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
package com.gip.xyna.utils.snmp.manager;

import com.gip.xyna.utils.snmp.varbind.VarBindList;

/**
 * SNMP Context.
 */
public interface SnmpContext {

  /**
   * Sends the varbinds to the agent.
   * @param varBindList the varbinds list to be sent.
   * @param caller debugging information.
   * @throws SNMPManagerException if errors while sending (e.g. timeout).
   * @throws SNMPResponseException if errors signaled by the agent.
   */
  void set(VarBindList varBindList, String caller);

  /**
   * Gets the varbinds from the agent.
   * @param varBindList the varbinds list to be sent.
   * @param caller debugging information.
   * @throws SNMPManagerException if errors while sending (e.g. timeout).
   * @throws SNMPResponseException if errors signaled by the agent.
   * @return the get response.
   */
  VarBindList get(VarBindList varBindList, String caller);

  /**
   * Gets the next varbind list from the agent.
   * @param varBindList the varbinds list to be sent.
   * @param caller debugging information.
   * @throws SNMPManagerException if errors while sending (e.g. timeout).
   * @throws SNMPResponseException if errors signaled by the agent.
   * @return the get response.
   */
  VarBindList getNext(VarBindList varBindList, String caller);

  /**
   * Sends a Trap-Request.
   * @param trapOid the trap
   * @param upTime upTime of the system in ms
   * @param varBindList the optional varbinds list to be sent.
   * @param caller debugging information.
   * @throws SNMPManagerException if errors while sending (e.g. timeout).
   * @throws SNMPResponseException if errors signaled by the agent.
   */
  void inform(String trapOid, long upTime, VarBindList varBindList, String caller);
  
  /**
   * Sends a Trap-Request.
   * @param trapOid the trap
   * @param upTime upTime of the system in ms
   * @param varBindList the optional varbinds list to be sent.
   * @param caller debugging information.
   */
  void trap(String trapOid, long upTime, VarBindList varBindList, String caller);
  
  /**
   * Closes the connection.
   */
  void close();
}
