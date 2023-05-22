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
import com.gip.xyna.utils.snmp.OID;
import com.gip.xyna.utils.snmp.agent.RequestHandler;
import com.gip.xyna.utils.snmp.agent.utils.OidSingleDispatcher;
import com.gip.xyna.utils.snmp.exception.SnmpRequestHandlerException;
import com.gip.xyna.utils.snmp.varbind.VarBindList;



/**
 */
public class DefaultRequestHandler implements RequestHandler {

  private static Logger logger = CentralFactoryLogging.getLogger(DefaultRequestHandler.class);

  private OidSingleDispatcher oidSingleDispatcher;


  public DefaultRequestHandler(OidSingleDispatcher oidSingleDispatcher) {
    this.oidSingleDispatcher = oidSingleDispatcher;
  }

  public void close() {
    // ntbd
  }

  public VarBindList snmpGet(VarBindList vbl) throws SnmpRequestHandlerException {
    if (logger.isTraceEnabled()) {
      logger.trace("snmpGet " + vbl);
    }
    try {
      VarBindList response = new VarBindList();
      for (int i = 0; i < vbl.size(); ++i) {
        response.add(oidSingleDispatcher.get(vbl.get(i), i));
      }
      return response;
    } catch (SnmpRequestHandlerException e) {
      throw e;
    } catch (RuntimeException e) {
      logger.error("Exception in snmpGet", e);
      throw new SnmpRequestHandlerException(RequestHandler.GENERAL_ERROR, 0);
    }
  }

  public VarBindList snmpGetNext(VarBindList vbl) throws SnmpRequestHandlerException {
    if (logger.isTraceEnabled()) {
      logger.trace("snmpGetNext " + vbl);
    }
    try {
      VarBindList response = new VarBindList();
      for (int i = 0; i < vbl.size(); ++i) {
        response.add(oidSingleDispatcher.getNext(vbl.get(i), i));
      }
      return response;
    } catch (SnmpRequestHandlerException e) {
      throw e;
    } catch (RuntimeException e) {
      logger.error("Exception in snmpGetNext", e);
      throw new SnmpRequestHandlerException(RequestHandler.GENERAL_ERROR, 0);
    }
  }

  public void snmpInform(OID informOID, VarBindList vbl) throws SnmpRequestHandlerException {
    if (logger.isTraceEnabled()) {
      logger.trace("snmpInform " + vbl);
    }
    throw new SnmpRequestHandlerException(RequestHandler.NO_SUCH_NAME, 0);
  }

  public void snmpSet(VarBindList vbl) throws SnmpRequestHandlerException {
    if (logger.isTraceEnabled()) {
      logger.trace("snmpSet " + vbl);
    }
    try {
      for (int i = 0; i < vbl.size(); ++i) {
        oidSingleDispatcher.set(vbl.get(i), i);
      }
    } catch (SnmpRequestHandlerException e) {
      throw e;
    } catch (RuntimeException e) {
      logger.error("Exception in snmpSet", e);
      throw new SnmpRequestHandlerException(RequestHandler.GENERAL_ERROR, 0);
    }
  }

  public void snmpTrap(OID trapOid, VarBindList vbl) throws SnmpRequestHandlerException {
    if (logger.isTraceEnabled()) {
      logger.trace("snmpTrap " + vbl);
    }
    try {
      for (int i = 0; i < vbl.size(); ++i) {
        oidSingleDispatcher.trap(vbl.get(i), i);
      }
    } catch (SnmpRequestHandlerException e) {
      throw e;
    } catch (RuntimeException e) {
      logger.error("Exception in snmpTrap", e);
      throw new SnmpRequestHandlerException(RequestHandler.GENERAL_ERROR, 0);
    }
  }
}
