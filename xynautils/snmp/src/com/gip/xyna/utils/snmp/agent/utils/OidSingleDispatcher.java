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



import java.util.ArrayList;

import com.gip.xyna.utils.snmp.OID;
import com.gip.xyna.utils.snmp.agent.RequestHandler;
import com.gip.xyna.utils.snmp.agent.utils.AbstractOidSingleHandler.SnmpCommand;
import com.gip.xyna.utils.snmp.exception.NoMatchingOidHandlerFoundException;
import com.gip.xyna.utils.snmp.varbind.VarBind;



/**
 * Der OidSingleDispatcher verteilt einzelne VarBinds eines ankommenden Request an {@link OidSingleHandler}, die dann
 * den Request bearbeiten. OidSingle im Namen besagt, dass jeder VarBind einzeln bearbeitet wird, es gibt nicht die
 * uebliche Gruppierung in einer Transaktion.
 */
public class OidSingleDispatcher {

  ArrayList<OidSingleHandler> oidHandler = new ArrayList<OidSingleHandler>();


  /**
   * registriert den uebergebenen OidSingleHandler, damit entsprechende OIDs bearbeitet werden koennen
   * @param oidSingleHandler
   */
  public void add(OidSingleHandler oidSingleHandler) {
    if (oidSingleHandler == null) {
      throw new IllegalArgumentException("oidSingleHandler is null");
    }
    oidHandler.add(oidSingleHandler);
  }


  /**
   * entfernt den OidSingleHandler, entsprechende OIDs werden dann nicht mehr bearbeitet
   * @param oidSingleHandler
   */
  public void remove(OidSingleHandler oidSingleHandler) {
    oidHandler.remove(oidSingleHandler);
  }


  /**
   * SNMP-Request GET
   * @param varBind
   * @param i
   * @return
   */
  public VarBind get(VarBind varBind, int i) {
    OID oid = new OID(varBind.getObjectIdentifier());
    for (OidSingleHandler oh : oidHandler) {
      if (oh.matches(SnmpCommand.GET, oid)) {
        return oh.get(oid, i);
      }
    }
    //unbekannte Oid
    throw new NoMatchingOidHandlerFoundException(RequestHandler.NO_SUCH_NAME, i);
  }


  /**
   * SNMP-Request GET_NEXT
   * @param varBind
   * @param i
   * @return
   */
  public VarBind getNext(VarBind varBind, int i) {
    OID oid = new OID(varBind.getObjectIdentifier());
    for (OidSingleHandler oh : oidHandler) {
      if (oh.matches(SnmpCommand.GET_NEXT, oid)) {
        return oh.getNext(oid, varBind, i);
      }
    }
    //unbekannte Oid
    throw new NoMatchingOidHandlerFoundException(RequestHandler.NO_SUCH_NAME, i);
  }


  /**
   * SNMP-Request SET
   * @param varBind
   * @param i
   */
  public void set(VarBind varBind, int i) {
    OID oid = new OID(varBind.getObjectIdentifier());
    for (OidSingleHandler oh : oidHandler) {
      if (oh.matches(SnmpCommand.SET, oid)) {
        oh.set(oid, varBind, i);
        return;
      }
    }
    //unbekannte Oid
    throw new NoMatchingOidHandlerFoundException(RequestHandler.NO_SUCH_NAME, i);
  }


  /**
   * FIXME macht das fr traps und informs so sinn? SNMP-Request INFORM
   * @param varBind
   * @param i
   */
  public void inform(VarBind varBind, int i) {
    OID oid = new OID(varBind.getObjectIdentifier());
    for (OidSingleHandler oh : oidHandler) {
      if (oh.matches(SnmpCommand.INFORM, oid)) {
        oh.inform(oid, i);
        return;
      }
    }
    //unbekannte Oid
    throw new NoMatchingOidHandlerFoundException(RequestHandler.NO_SUCH_NAME, i);
  }


  /**
   * FIXME macht das fr traps und informs so sinn? SNMP-Request TRAP
   * @param varBind
   * @param i
   */
  public void trap(VarBind varBind, int i) {
    OID oid = new OID(varBind.getObjectIdentifier());
    for (OidSingleHandler oh : oidHandler) {
      if (oh.matches(SnmpCommand.TRAP, oid)) {
        oh.trap(oid, i);
        return;
      }
    }
    //unbekannte Oid
    throw new NoMatchingOidHandlerFoundException(RequestHandler.NO_SUCH_NAME, i);
  }
}
