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
package com.gip.xyna.utils.snmp.agent;



import com.gip.xyna.utils.snmp.OID;
import com.gip.xyna.utils.snmp.exception.SnmpRequestHandlerException;
import com.gip.xyna.utils.snmp.varbind.VarBindList;



public interface RequestHandler {

  public static final int NO_ERROR = 0;
  public static final int NO_SUCH_NAME = 2; //oid von agent nicht verstanden/nicht unterstuetzt
  public static final int BAD_VALUE = 3;
  public static final int GENERAL_ERROR = 5;
  public static final int INVALID_VALUE = 10;


  VarBindList snmpGet(VarBindList vbl) throws SnmpRequestHandlerException;


  VarBindList snmpGetNext(VarBindList vbl) throws SnmpRequestHandlerException;


  void snmpSet(VarBindList vbl) throws SnmpRequestHandlerException;


  void snmpInform(OID informOid, VarBindList vbl) throws SnmpRequestHandlerException;


  void snmpTrap(OID trapOid, VarBindList vbl) throws SnmpRequestHandlerException;


  void close();


}
