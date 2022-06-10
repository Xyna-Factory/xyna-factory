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
package com.gip.xyna.utils.snmp.agent.utils;

import com.gip.xyna.utils.snmp.OID;
import com.gip.xyna.utils.snmp.agent.RequestHandler;
import com.gip.xyna.utils.snmp.exception.SnmpRequestHandlerException;
import com.gip.xyna.utils.snmp.varbind.VarBind;

/**
 * Handler, der sich um die Bearbeitung der Snmp-Requests zu einer OID 
 * (oder eines Zweiges unter der OID) kuemmert. 
 * Nur Get und GetNext muss man implementieren. Set, Trap und Inform machen
 * standardmaessig nichts
 *
 */
public abstract class AbstractOidSingleHandler implements OidSingleHandler {
  
  /**
   * Dem OidSingleHandler bekannte Snmp-Requests
   *
   */
  public enum SnmpCommand {
    GET,
    GET_NEXT,
    SET,
    INFORM,
    TRAP;
  }
 
  
  /**
   * Bearbeitung des SET-Requests
   * @param oid
   * @param varBind zu setzende Daten
   * @param i
   */
  public void set(OID oid, VarBind varBind, int i) {
    //nichts, kann man ueberschreiben, falls noetig
    throw new SnmpRequestHandlerException(RequestHandler.NO_SUCH_NAME, i);
  }
  
  /**
   * Bearbeitung des INFORM-Requests
   * @param oid
   * @param varBind zu setzende Daten
   * @param i
   */
  public void inform(OID oid, int i) {
    //nichts, kann man ueberschreiben, falls noetig
    throw new SnmpRequestHandlerException(RequestHandler.NO_SUCH_NAME, i);
  }
  
  /**
   * Bearbeitung des TRAP-Requests
   * @param oid
   * @param varBind zu setzende Daten
   * @param i
   */
  public void trap(OID oid, int i) {
    //nichts, kann man ueberschreiben, falls noetig
    throw new SnmpRequestHandlerException(RequestHandler.NO_SUCH_NAME, i);
  }
}