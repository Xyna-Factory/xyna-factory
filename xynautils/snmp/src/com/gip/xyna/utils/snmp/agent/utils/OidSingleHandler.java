/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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
import com.gip.xyna.utils.snmp.agent.utils.AbstractOidSingleHandler.SnmpCommand;
import com.gip.xyna.utils.snmp.varbind.VarBind;



public interface OidSingleHandler {

  /**
   * Kann der OidSingleHandler den SnmpRequest fuer die angebenen OID ausfuehren? Unbekannte OIDs muessen abgelehnt
   * werden, ebenso SET-Operationen fuer ReadOnly-Objekte.
   * @param snmpCommand
   * @param oid
   * @return
   */
  public boolean matches(SnmpCommand snmpCommand, OID oid);


  /**
   * Bearbeitung des GET-Requests
   * @param oid
   * @param i
   * @return Ergebnis-VarBind
   */
  public VarBind get(OID oid, int i);


  /**
   * Bearbeitung des GET_NEXT-Requests
   * @param oid
   * @param varBind
   * @param i
   * @return Ergebnis-VarBind
   */
  public VarBind getNext(OID oid, VarBind varBind, int i);


  /**
   * Bearbeitung des SET-Requests
   * @param oid
   * @param varBind zu setzende Daten
   * @param i
   */
  public void set(OID oid, VarBind varBind, int i);


  /**
   * FIXME macht einzelne bearbeitung fuer informs und traps sinn?? Bearbeitung des INFORM-Requests
   * @param oid
   * @param varBind zu setzende Daten
   * @param i
   */
  public void inform(OID oid, int i);


  /**
   * FIXME macht einzelne bearbeitung fuer informs und traps sinn?? Bearbeitung des TRAP-Requests
   * @param oid
   * @param varBind zu setzende Daten
   * @param i
   */
  public void trap(OID oid, int i);

}
