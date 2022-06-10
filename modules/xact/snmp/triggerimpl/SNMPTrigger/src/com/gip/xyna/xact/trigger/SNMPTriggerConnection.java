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
package com.gip.xyna.xact.trigger;



import org.apache.log4j.Logger;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.MessageException;
import org.snmp4j.PDU;
import org.snmp4j.ScopedPDU;
import org.snmp4j.mp.MessageProcessingModel;
import org.snmp4j.mp.StatusInformation;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OctetString;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.snmp.OID;
import com.gip.xyna.utils.snmp.SnmpAccessData;
import com.gip.xyna.utils.snmp.agent.PduEventHandlerApache;
import com.gip.xyna.utils.snmp.agent.PduEventHandlerApache.TrapVarBinds;
import com.gip.xyna.utils.snmp.agent.RequestHandler;
import com.gip.xyna.utils.snmp.agent.SnmpAgent.SnmpAgentLogger;
import com.gip.xyna.utils.snmp.exception.NoMatchingOidHandlerFoundException;
import com.gip.xyna.utils.snmp.exception.SnmpRequestHandlerException;
import com.gip.xyna.utils.snmp.varbind.VarBindList;
import com.gip.xyna.utils.snmp.varbind.VarBindTypeConverterImplApache;
import com.gip.xyna.xact.trigger.snmp.SNMPTRIGGER_ResponseException;
import com.gip.xyna.xdev.xfractmod.xmdm.TriggerConnection;
import com.gip.xyna.xprc.XynaOrder;



public class SNMPTriggerConnection extends TriggerConnection {

  private static final long serialVersionUID = -8925555434950225243L;

  private static Logger logger = CentralFactoryLogging.getLogger(SNMPTriggerConnection.class);


  private transient CommandResponderEvent event;
  private transient VarBindTypeConverterImplApache varBindTypeConverter = new VarBindTypeConverterImplApache();

  private transient SnmpAgentLogger snmpAgentLogger = new SnmpAgentLogger() { // Dummy Implementation, no null-check necessary

    public void setLastReceived(int lastReceived) {/* dummy */
    }


    public void setLastSent(int lastSent) {/* dummy */
    }


    public void setType(String string) {/* dummy */
    }


    public void setFailed(SnmpRequestHandlerException e) {/* dummy */
    }


    public void setSuccess() {/* dummy */
    }
  };

  private transient SNMPTrigger trigger;


  public SNMPTriggerConnection(CommandResponderEvent e, SNMPTrigger trigger) {
    this.event = e;
    this.trigger = trigger;
    if (logger.isDebugEnabled()) {
      logger.debug("got connection " + e);
    }
  }


  public SNMPTrigger getTrigger() {
    return trigger;
  }


  public void setVarBindTypeConverter(VarBindTypeConverterImplApache varBindTypeConverter) {
    this.varBindTypeConverter = varBindTypeConverter;
  }


  public void setSnmpAgentLogger(SnmpAgentLogger snmpAgentLogger) {
    this.snmpAgentLogger = snmpAgentLogger;
  }


  public CommandResponderEvent getCommandResponderEvent() {
    return event;
  }


  public PDU getPDU() {
    return event.getPDU();
  }


  /**
   * ruft requestHandler.x auf, je nachdem um was für einen snmp event es sich handelt und gibt die
   * im requestHandler gebaute xynaorder zurück.
   * @param requestHandler
   * @return
   * @throws XynaException
   */
  public XynaOrder handleEvent(XynaOrderSnmpRequestHandler requestHandler) throws XynaException, InterruptedException {
    if (event == null) {
      throw new XynaException("event not available");
    }
    XynaOrder order = null;
    PDU command = event.getPDU();

    VarBindList vbl = varBindTypeConverter.toVarBindList(command);
    String addr = event.getPeerAddress().toString();
    vbl.setReceivedFromHost(addr.substring(0, addr.indexOf('/')));
    vbl.setSnmpVersion(getSnmpVersion(event.getMessageProcessingModel()));

    switch (command.getType()) {
      case PDU.GET :
        snmpAgentLogger.setType("Get");
        order = requestHandler.snmpGet(vbl);
        break;
      case PDU.GETNEXT :
        snmpAgentLogger.setType("GetNext");
        order = requestHandler.snmpGetNext(vbl);
        break;
      case PDU.SET :
        snmpAgentLogger.setType("Set");
        order = requestHandler.snmpSet(vbl);
        break;
      case PDU.INFORM : {
        snmpAgentLogger.setType("Inform");
        TrapVarBinds ti = PduEventHandlerApache.getTrapVarBinds(vbl);
        order = requestHandler.snmpInform(new OID(ti.getTrapOid().stringValue()), ti.getVBLWithoutPredefinedVarBinds());
        break;
      }
      case PDU.TRAP : {
        snmpAgentLogger.setType("Trap");
        TrapVarBinds ti = PduEventHandlerApache.getTrapVarBinds(vbl);
        order = requestHandler.snmpTrap(new OID(ti.getTrapOid().stringValue()), ti.getVBLWithoutPredefinedVarBinds());
        break;
      }
      default :
        throw new IllegalStateException("Unsupported MessageType for " + command);
    }

    return order;
  }


  public void sendResponse(VarBindList respvbl) throws XynaException {
    if (event == null) {
      throw new XynaException("event not available");
    }
    PDU command = event.getPDU();
    PDU response = createResponsePdu(command.getRequestID());

    if (command.size() == 0) {
      // TODO
    } else {
      if (command.getType() == PDU.TRAP) {
        return;
      }

      if (respvbl != null) {
        for (int i = 0, n = respvbl.size(); i < n; ++i) {
          response.add(respvbl.get(i).convert(varBindTypeConverter));
        }
      }
      snmpAgentLogger.setSuccess();

    }
    try {
      event.getMessageDispatcher().returnResponsePdu(event.getMessageProcessingModel(), event.getSecurityModel(),
                                                     event.getSecurityName(), event.getSecurityLevel(), response,
                                                     event.getMaxSizeResponsePDU(), event.getStateReference(),
                                                     new StatusInformation());
      event = null;
    } catch (MessageException e) {
      throw new SNMPTRIGGER_ResponseException(e);
    }

  }


  /**
   * ruft die methode vom request handler auf, die dem snmp event entspricht und verschickt danach die snmp response
   * zurück an den client. nicht geeignet um für einen snmp request eine xynaorder zu erstellen. dafür besser anderes
   * handleEvent benutzen.
   * @param requestHandler
   * @throws XynaException
   * @throws InterruptedException falls antwort erfolgreich verschickt wurde
   */
  public void handleEvent(RequestHandler requestHandler) throws XynaException, InterruptedException {
    if (event == null) {
      throw new XynaException("event not available");
    }

    // PERFORMANCE: bei mehreren filtern passieren hier ggfs dinge mehrfach, da sollte man schauen, ob es sinn hat, das
    // etwas zu ändern.
    //TODO bei v1/v2 community überprüfen
    PDU command = event.getPDU();
    PDU response = createResponsePdu(command.getRequestID());

    if (command.size() == 0) {
      // kein Fehler, leere Varbind-Liste zurückgeben
    } else {
      VarBindList vbl = varBindTypeConverter.toVarBindList(command);

      String addr = event.getPeerAddress().toString();
      vbl.setReceivedFromHost(addr.substring(0, addr.indexOf('/')));
      vbl.setSnmpVersion(getSnmpVersion(event.getMessageProcessingModel()));

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
            TrapVarBinds ti = PduEventHandlerApache.getTrapVarBinds(vbl);
            requestHandler.snmpInform(new OID(ti.getTrapOid().stringValue()), ti.getVBLWithoutPredefinedVarBinds());
            respvbl = vbl;
            break;
          }
          case PDU.TRAP : {
            snmpAgentLogger.setType("Trap");
            TrapVarBinds ti = PduEventHandlerApache.getTrapVarBinds(vbl);
            requestHandler.snmpTrap(new OID(ti.getTrapOid().stringValue()), ti.getVBLWithoutPredefinedVarBinds());
            throw new InterruptedException("trap handled successfully"); // Sofortiger Abbruch, da keine Antwort
            // verschickt wird
          }
          default :
            throw new IllegalStateException("Unsupported MessageType for " + command);
        }

        if (respvbl != null) {
          for (int i = 0, n = respvbl.size(); i < n; ++i) {
            response.add(respvbl.get(i).convert(varBindTypeConverter));
          }
        } else {
          throw new SnmpRequestHandlerException(RequestHandler.GENERAL_ERROR, 0);
        }
        snmpAgentLogger.setSuccess();
      } catch (NoMatchingOidHandlerFoundException e) {
        snmpAgentLogger.setFailed(e);
        return; // oid wurde von requesthandler nicht als gültig erkannt => nächsten filter versuchen
      } catch (SnmpRequestHandlerException e) {
        response.setErrorIndex(e.getIndex());
        response.setErrorStatus(e.getState());
        snmpAgentLogger.setFailed(e);
        logger.debug("snmp request failed", e);
      } catch (RuntimeException e) {
        response.setErrorIndex(0);
        response.setErrorStatus(RequestHandler.GENERAL_ERROR);
        //snmpAgentLogger.setFailed(e);
        logger.error("snmp request failed unexpectedly", e);
      }
    }
    try {
      event.getMessageDispatcher().returnResponsePdu(event.getMessageProcessingModel(), event.getSecurityModel(),
                                                     event.getSecurityName(), event.getSecurityLevel(), response,
                                                     event.getMaxSizeResponsePDU(), event.getStateReference(),
                                                     new StatusInformation());
      event = null;
      throw new InterruptedException("response send successfully");
    } catch (MessageException e) {
      throw new SNMPTRIGGER_ResponseException(e);
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


  private PDU createResponsePdu(Integer32 requestId) {
    PDU response;
    if (event.getPDU() instanceof ScopedPDU) {
      ScopedPDU epdu = (ScopedPDU)event.getPDU();
      ScopedPDU scopedPdu = new ScopedPDU();
      scopedPdu.setContextEngineID(new OctetString(epdu.getContextEngineID()));
      scopedPdu.setContextName(new OctetString(epdu.getContextName()));
      response = scopedPdu;
    } else {
      response = new PDU();
    }
    response.setType(PDU.RESPONSE);
    response.setRequestID(requestId);
    response.setErrorIndex(0);
    response.setErrorStatus(PDU.noError);
    return response;
  }


  protected void sendError(int errorStatus, Exception... exceptions) throws XynaException {
    if (event == null) {
      throw new XynaException("event not available");
    }
    int type = event.getPDU().getType();
    boolean sendResponse = false;
    switch (type) {
      case PDU.GET :
      case PDU.GETBULK :
      case PDU.GETNEXT :
      case PDU.SET :
      case PDU.INFORM :
        sendResponse = true;
        break;
      // case PDU.NOTIFICATION : break;  das gleiche wie TRAP
      case PDU.REPORT :
      case PDU.RESPONSE :
      case PDU.TRAP :
      case PDU.V1TRAP :
      default :
        break;
    }
    if (sendResponse) {
      PDU command = event.getPDU();
      PDU response = createResponsePdu(command.getRequestID());

      response.setErrorStatus(errorStatus);
      //TODO fehlermeldung? aus exceptions
      try {
        event.getMessageDispatcher().returnResponsePdu(event.getMessageProcessingModel(), event.getSecurityModel(),
                                                       event.getSecurityName(), event.getSecurityLevel(), response,
                                                       event.getMaxSizeResponsePDU(), event.getStateReference(),
                                                       new StatusInformation());
      } catch (MessageException f) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < exceptions.length; i++) {
          sb.append(exceptions[i].getMessage());
          if (i > 0) {
            sb.append(" and\n ");
          }
        }
        logger.warn("Could not send response after error: " + sb.toString(), f);
      }
    }

  }


}
