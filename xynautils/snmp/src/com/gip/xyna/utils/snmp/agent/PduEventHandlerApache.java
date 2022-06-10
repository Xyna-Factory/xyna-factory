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



import org.apache.log4j.Logger;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.MessageException;
import org.snmp4j.PDU;
import org.snmp4j.ScopedPDU;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.MessageProcessingModel;
import org.snmp4j.mp.StatusInformation;
import org.snmp4j.security.SecurityModel;
import org.snmp4j.smi.OctetString;

import com.gip.xyna.utils.snmp.OID;
import com.gip.xyna.utils.snmp.SnmpAccessData;
import com.gip.xyna.utils.snmp.agent.SnmpAgent.SnmpAgentLogger;
import com.gip.xyna.utils.snmp.exception.SnmpRequestHandlerException;
import com.gip.xyna.utils.snmp.manager.SnmpContextImplApache;
import com.gip.xyna.utils.snmp.varbind.OIDVarBind;
import com.gip.xyna.utils.snmp.varbind.TimeTicksVarBind;
import com.gip.xyna.utils.snmp.varbind.UnsIntegerVarBind;
import com.gip.xyna.utils.snmp.varbind.VarBind;
import com.gip.xyna.utils.snmp.varbind.VarBindList;
import com.gip.xyna.utils.snmp.varbind.VarBindTypeConverterImplApache;



public class PduEventHandlerApache implements Runnable {

  static Logger logger = Logger.getLogger(PduEventHandlerApache.class.getName());

  private CommandResponderEvent event;
  private RequestHandler requestHandler;
  private SnmpAgentLogger snmpAgentLogger;
  private VarBindTypeConverterImplApache varBindTypeConverter;


  public PduEventHandlerApache(CommandResponderEvent event, RequestHandler requestHandler,
                               SnmpAgentLogger snmpAgentLogger) {
    this.event = event;
    this.requestHandler = requestHandler;
    this.snmpAgentLogger = snmpAgentLogger;
    varBindTypeConverter = new VarBindTypeConverterImplApache();
  }


  public void run() {
    PDU command = event.getPDU();

    final PDU response;
    if (event.getMessageDispatcher().getMessageProcessingModel(event.getMessageProcessingModel()) instanceof MPv3) {
      ScopedPDU scopedPdu = new ScopedPDU();
      scopedPdu.setContextEngineID(new OctetString(event.getStateReference().getContextEngineID()));
      scopedPdu.setContextName(new OctetString(event.getStateReference().getContextName()));
      response = scopedPdu;
    } else {
      response = new PDU();
    }

    response.setType(PDU.RESPONSE);
    response.setRequestID(command.getRequestID());
    response.setErrorIndex(0);
    response.setErrorStatus(PDU.noError);

    if (command.size() == 0) {
      //kein Fehler, leere Varbind-Liste zurueckgeben
    } else {
      VarBindList vbl = varBindTypeConverter.toVarBindList(command);

      String addr = event.getPeerAddress().toString();
      vbl.setReceivedFromHost(addr.substring(0, addr.indexOf('/')));
      vbl.setSnmpVersion(getSnmpVersion(event.getMessageProcessingModel()));

      if (event.getSecurityModel() == SecurityModel.SECURITY_MODEL_SNMPv1
                      || event.getSecurityModel() == SecurityModel.SECURITY_MODEL_SNMPv2c) {
        String community = new String(event.getSecurityName());
        //TODO community strings verwalten und vergleichen
      }
      VarBindList respvbl = new VarBindList();
      try {
        switch (command.getType()) {
          case PDU.GET :
            snmpAgentLogger.setType("Get");
            respvbl = requestHandler.snmpGet(vbl);
            break;
          case PDU.GETNEXT :
            snmpAgentLogger.setType("GetNext");
            respvbl = requestHandler.snmpGetNext(vbl);
            break;
          case PDU.SET :
            snmpAgentLogger.setType("Set");
            requestHandler.snmpSet(vbl);
            break;
          case PDU.INFORM : {
            snmpAgentLogger.setType("Inform");
            TrapVarBinds ti = getTrapVarBinds(vbl);
            OID trapOid = new OID(ti.getTrapOid().stringValue());
            requestHandler.snmpInform(trapOid, ti.getVBLWithoutPredefinedVarBinds());
            respvbl = vbl;
            break;
          }
          case PDU.TRAP : {
            snmpAgentLogger.setType("Trap");
            TrapVarBinds ti = getTrapVarBinds(vbl);
            OID trapOid = new OID(ti.getTrapOid().stringValue());
            requestHandler.snmpTrap(trapOid, ti.getVBLWithoutPredefinedVarBinds());
            return; //Sofortiger Abbruch, da keine Antwort verschickt wird
          }

          default :
            throw new IllegalStateException("Unhandled MessageType for " + command);
        }
        for (int i = 0, n = respvbl.size(); i < n; ++i) {
          response.add(respvbl.get(i).convert(varBindTypeConverter));
        }
        snmpAgentLogger.setSuccess();
      } catch (SnmpRequestHandlerException e) {
        response.setErrorIndex(e.getIndex());
        response.setErrorStatus(e.getState());
        snmpAgentLogger.setFailed(e);
      }
    }
    try {
      event.getMessageDispatcher().returnResponsePdu(event.getMessageProcessingModel(), event.getSecurityModel(),
                                                     event.getSecurityName(), event.getSecurityLevel(), response,
                                                     event.getMaxSizeResponsePDU(), event.getStateReference(),
                                                     new StatusInformation());
    } catch (MessageException e) {
      logger.error(e);
    }
  }
  
  private static void traceTrapVBL(VarBindList vbl) {
    if (logger.isTraceEnabled()) {
      logger.trace("got following vbl entries:");
      for (int i = 0; i<vbl.size(); i++) {
        logger.trace("  " + i + ". " + vbl.get(i).getObjectIdentifier() + ": " + vbl.get(i).getValue());
      }
    }
  }
  
  private static final String OID_UPTIME_STRING = new OID(SnmpContextImplApache.OID_upTime).getOid();
  private static final String OID_SNMPTRAP_STRING= new OID(SnmpContextImplApache.OID_snmpTrap).getOid();


  public static TrapVarBinds getTrapVarBinds(VarBindList vbl) {
    if (vbl.size() < 2) {
      traceTrapVBL(vbl);
      throw new SnmpRequestHandlerException(RequestHandler.GENERAL_ERROR, 0);
    }
    VarBind vb0 = vbl.get(0);
    VarBind vb1 = vbl.get(1);
    TimeTicksVarBind uptime = null;
    OIDVarBind trap = null;
    if (vb0.getObjectIdentifier().equals(OID_UPTIME_STRING)) {
      uptime = (TimeTicksVarBind) vb0;
    } else {
      traceTrapVBL(vbl);
     
      throw new SnmpRequestHandlerException(RequestHandler.GENERAL_ERROR, 0);
    }
    if (vb1.getObjectIdentifier().equals(OID_SNMPTRAP_STRING)) {
      trap = (OIDVarBind) vb1;
    } else {
      traceTrapVBL(vbl);
      throw new SnmpRequestHandlerException(RequestHandler.GENERAL_ERROR, 0);
    }
    VarBindList vblWithoutPredefinedVarBinds = new VarBindList();
    vblWithoutPredefinedVarBinds.setReceivedFromHost(vbl.getReceivedFromHost());
    vblWithoutPredefinedVarBinds.setSnmpVersion(vbl.getSnmpVersion());
    for (int i = 2; i < vbl.size(); i++) {
      vblWithoutPredefinedVarBinds.add(vbl.get(i));
    }
    return new TrapVarBinds(uptime, trap, vblWithoutPredefinedVarBinds);
  }


  public static class TrapVarBinds {

    private TimeTicksVarBind uptime;
    private OIDVarBind trap;
    private VarBindList vblWithoutPredefinedVarBinds;


    public TrapVarBinds(TimeTicksVarBind uptime, OIDVarBind trap, VarBindList vblWithoutPredefinedVarBinds) {
      this.uptime = uptime;
      this.trap = trap;
      this.vblWithoutPredefinedVarBinds = vblWithoutPredefinedVarBinds;
    }


    public VarBindList getVBLWithoutPredefinedVarBinds() {
      return vblWithoutPredefinedVarBinds;
    }


    public OIDVarBind getTrapOid() {
      return trap;
    }

  }


  /**
   * @param messageProcessingModel
   * @return
   */
  private String getSnmpVersion(int messageProcessingModel) {
    switch (messageProcessingModel) {
      case MessageProcessingModel.MPv1 :
        return SnmpAccessData.VERSION_1;
      case MessageProcessingModel.MPv2c :
        return SnmpAccessData.VERSION_2c;
      case MessageProcessingModel.MPv3 :
        return SnmpAccessData.VERSION_3;
      default :
        return "unknown";
    }
  }

}
