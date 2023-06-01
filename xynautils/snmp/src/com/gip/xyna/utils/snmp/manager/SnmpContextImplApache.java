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

package com.gip.xyna.utils.snmp.manager;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.snmp4j.AbstractTarget;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.SNMP4JSettings;
import org.snmp4j.ScopedPDU;
import org.snmp4j.Snmp;
import org.snmp4j.UserTarget;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.log.Log4jLogFactory;
import org.snmp4j.log.LogFactory;
import org.snmp4j.mp.MPv1;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.AuthMD5;
import org.snmp4j.security.AuthSHA;
import org.snmp4j.security.PrivAES128;
import org.snmp4j.security.PrivDES;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import com.gip.xyna.utils.snmp.SnmpAccessData;
import com.gip.xyna.utils.snmp.exception.SnmpManagerException;
import com.gip.xyna.utils.snmp.exception.SnmpResponseException;
import com.gip.xyna.utils.snmp.timeoutmodel.TimeoutModel;
import com.gip.xyna.utils.snmp.varbind.VarBindList;
import com.gip.xyna.utils.snmp.varbind.VarBindTypeConverterImplApache;


/**
 * Implementierung des SnmpContext-Interfaces fuer Apaches Snmp4j.
 * 
 * Der SocketTimeout bestimmt, wie haeufig das Lesen aus dem Socket unterbrochen 
 * wird. Mit einem niedrigen SocketTimeout ist das snmpContext.close() schneller.
 *
 */
public final class SnmpContextImplApache implements SnmpContext {
  public static final int[] OID_upTime = SnmpConstants.sysUpTime.toIntArray();
  public static final int[] OID_snmpTrap = SnmpConstants.snmpTrapOID.toIntArray();

  static Logger logger = Logger.getLogger(SnmpContextImplApache.class.getName());

  private Snmp snmp;
  private AbstractTarget target;

  private SnmpAccessData snmpAccessData;

  private VarBindTypeConverterImplApache varBindTypeConverter;
  
  static {
    LogFactory.setLogFactory(new Log4jLogFactory());
  }
  
  /**
   * Standard-Konstruktor mit socketTimeout = 1000
   * @param snmpAccessData
   * @throws IOException
   */
  public SnmpContextImplApache(final SnmpAccessData snmpAccessData ) throws IOException {
    this(snmpAccessData,1000);
  }
 
  /**
   * Konstruktor mit konfigurierbarem SocketTimeout + SrcIpAdresse/Port
   * 
   * @param snmpAccessData
   * @param socketTimeout SocketTimeout s
   * @param String srcAddressPort 
   * @throws IOException
   */
  public SnmpContextImplApache(final SnmpAccessData snmpAccessData, int socketTimeout, String srcAddressPort ) throws IOException {
    this.snmpAccessData = snmpAccessData;
    Address targetAddress = GenericAddress.parse("udp:"+snmpAccessData.getHost()+"/"+snmpAccessData.getPort() );
    DefaultUdpTransportMapping transport = new DefaultUdpTransportMapping(new UdpAddress(srcAddressPort));
    transport.setSocketTimeout(socketTimeout);
    
    snmp = new Snmp();
    snmp.addTransportMapping(transport);
    snmp.getMessageDispatcher().addCommandResponder(snmp); //wichtig, da sonst nicht automatisch bestimmte fehlermeldungen verarbeitet werden
    

       
    if (snmpAccessData.isSNMPv1()) {
      CommunityTarget ct = new CommunityTarget();
      ct.setCommunity(new OctetString(snmpAccessData.getCommunity()));
      ct.setVersion(SnmpConstants.version1);
      target = ct;
      snmp.getMessageDispatcher().addMessageProcessingModel(new MPv1());
    } else if (snmpAccessData.isSNMPv2c() ) {
      CommunityTarget ct = new CommunityTarget();
      ct.setCommunity(new OctetString(snmpAccessData.getCommunity()));
      ct.setVersion(SnmpConstants.version2c);
      target = ct;
      snmp.getMessageDispatcher().addMessageProcessingModel(new MPv2c());
    } else if (snmpAccessData.isSNMPv3()) {
      initV3(snmp, snmpAccessData);
      
      UserTarget ut = new UserTarget();      
      if (snmpAccessData.getAuthenticationPassword() != null) {
        if (snmpAccessData.getPrivacyPassword() != null) {
          ut.setSecurityLevel(SecurityLevel.AUTH_PRIV);
        } else {
          ut.setSecurityLevel(SecurityLevel.AUTH_NOPRIV);
        }
       // ut.setSecurityModel(SecurityModel.SECURITY_MODEL_USM);
      } else {
        ut.setSecurityLevel(SecurityLevel.NOAUTH_NOPRIV);
      //  ut.setSecurityModel(SecurityModel.SECURITY_MODEL_ANY);
      }
      ut.setSecurityName(new OctetString(snmpAccessData.getUsername()));
      target = ut;

      target.setVersion(SnmpConstants.version3);

      if (logger.isDebugEnabled()) { 
        OctetString os = new OctetString(snmp.getLocalEngineID());
        logger.debug("local EngineId: " + os.toHexString() + " - " + os.toString()); 
      }
    } else {
      throw new IllegalArgumentException( "SnmpAccessData with unknown version"); 
    }

    varBindTypeConverter = new VarBindTypeConverterImplApache();

    TimeoutModel tm = snmpAccessData.getTimeoutModel();
    snmp.setTimeoutModel( new ApacheTimeoutModel(tm) );
    
    target.setAddress(targetAddress);
    target.setRetries(tm.getRetries());
    
    
    snmp.listen();
  }
  
  public SnmpContextImplApache(final SnmpAccessData snmpAccessData, int socketTimeout ) throws IOException {
    this(snmpAccessData, socketTimeout, "0.0.0.0/0" );
  }
  
  public Snmp getSnmp() {
    return snmp;
  }
   
  
  public static void initV3(Snmp snmp, SnmpAccessData snmpAccessData) {
    SecurityProtocols.getInstance().addDefaultProtocols();

    byte[] localEngineId = null;
    if (snmpAccessData.getEngineId() != null) {
      localEngineId = MPv3.createLocalEngineID(new OctetString(snmpAccessData.getEngineId()));
    } else {
      localEngineId = MPv3.createLocalEngineID();
    }

    USM usm = new USM(SecurityProtocols.getInstance(), new OctetString(localEngineId), 0);
    SecurityModels.getInstance().addSecurityModel(usm);

    MPv3 mpv3 = new MPv3(usm);
    if (logger.isDebugEnabled()) {      
      logger.debug("enterpriseid=" + SNMP4JSettings.getEnterpriseID());
    }
    //mpv3.addEngineID(address, engineID)
    snmp.getMessageDispatcher().addMessageProcessingModel(mpv3);     

    if (snmpAccessData.getUsername() != null) {
      OctetString userOctetString = new OctetString(snmpAccessData.getUsername());
      snmp.getUSM().addUser(userOctetString, createUsmUserV3(userOctetString, snmpAccessData));
    }
  }


  private static UsmUser createUsmUserV3(OctetString userOctetString, SnmpAccessData snmpAccessData) {
    OID authProtocol = null;
    OID privProtocol = null;
    if (snmpAccessData.getAuthenticationProtocol() != null) {
      if (snmpAccessData.getAuthenticationProtocol().equals(SnmpAccessData.SHA1)) {
        authProtocol = AuthSHA.ID;
      } else if (snmpAccessData.getAuthenticationProtocol().equals(SnmpAccessData.MD5)) {
        authProtocol = AuthMD5.ID;
      } else {
        throw new IllegalArgumentException("Authentication protocol " + snmpAccessData.getAuthenticationProtocol()
                        + " not supported");
      }
    }
    if (snmpAccessData.getPrivacyProtocol() != null) {
      if (snmpAccessData.getPrivacyProtocol().equals(SnmpAccessData.AES128)) {
        privProtocol = PrivAES128.ID;
      } else if (snmpAccessData.getPrivacyProtocol().equals(SnmpAccessData.DES56)) {
        privProtocol = PrivDES.ID;
      } else {
        throw new IllegalArgumentException("Privacy protocol " + snmpAccessData.getPrivacyProtocol() + " not supported");
      }
    }
    
    OctetString authenticationOctetString = snmpAccessData.getAuthenticationPassword() != null ? new OctetString(snmpAccessData.getAuthenticationPassword()) : null;
    OctetString privacyOctetString = snmpAccessData.getPrivacyPassword() != null ? new OctetString(snmpAccessData.getPrivacyPassword()) : null;
    return new UsmUser(userOctetString, authProtocol, 
                       authenticationOctetString,
                       privProtocol, 
                       privacyOctetString);
  }


  /**
   * Umsetzung des TimeoutModel auf das TimeoutModel im Apache snmp4j
   */
  private static class ApacheTimeoutModel implements org.snmp4j.TimeoutModel {

    private TimeoutModel tm;
    public ApacheTimeoutModel(TimeoutModel tm) {
      this.tm = tm;
    }
    public long getRequestTimeout(int totalNumberOfRetries,long targetTimeout) {
      return tm.getRequestTimeout();
    }
    public long getRetryTimeout(int retryCount, int totalNumberOfRetries, long targetTimeout) {
      return tm.getRetryTimeout(retryCount);
    }
  }
  
  public void close() {
    try {
      snmp.close();
    } catch (IOException e) {
      logger.error("Error while closing SNMP-Manager", e);
    }
  }

  @Override
  public String toString() {
    if (snmpAccessData == null) {
      return "uninitialized SnmpContext";
    } else {
      return "SnmpContext(" + snmpAccessData.toString() + ")";
    }
  }
  
  private PDU createPDU(int type) {
    PDU pdu = null;
    if( snmpAccessData.isSNMPv3() ) {
        pdu = new ScopedPDU();
    } else {
        pdu = new PDU(); 
    }
    pdu.setType(type);
    return pdu;
  }

  public void set(final VarBindList vbl, final String caller) {
    logger.debug("set: "+ caller);
    PDU setPdu = createPDU(PDU.SET);
    addVarbinds(setPdu, vbl);
    sendPdu(setPdu,caller);
  }

  public VarBindList get(final VarBindList vbl, final String caller) {
    logger.debug("get: "+ caller);
    PDU getPdu = createPDU(PDU.GET);
    addVarbinds(getPdu, vbl);
    return sendPdu(getPdu,caller);
  }

  public VarBindList getNext(final VarBindList vbl, final String caller) {
    logger.debug("getNext: "+ caller);
    PDU getNextPdu = createPDU(PDU.GETNEXT);
    addVarbinds(getNextPdu, vbl);
    return sendPdu(getNextPdu,caller);
  }

  public void inform(String trapOid, long upTime, final VarBindList varBindList, final String caller) {
    logger.debug("inform: "+ caller);
    PDU informPdu = createPDU(PDU.INFORM);
    informPdu.add( new VariableBinding( new OID(OID_upTime), new TimeTicks(upTime/10) ) );
    informPdu.add( new VariableBinding( new OID(OID_snmpTrap), new OID(trapOid) ) );
    if( varBindList != null ) {
      addVarbinds(informPdu, varBindList);
    }
    sendPdu(informPdu,caller);
  }
  
  public void trap(String trapOid, long upTime, VarBindList varBindList, String caller) {
    logger.debug("trap: "+ caller);
    PDU trapPdu = createPDU(PDU.TRAP);
    trapPdu.add( new VariableBinding( new OID(OID_upTime), new TimeTicks(upTime/10) ) );
    trapPdu.add( new VariableBinding( new OID(OID_snmpTrap), new OID(trapOid) ) );
    if( varBindList != null ) {
      addVarbinds(trapPdu, varBindList);
    }
    try {
      snmp.send(trapPdu, target); //keine Antwort auswertbar
    } catch (IOException e) {
      logger.error(e);
    }
  }

  private void addVarbinds(final PDU pdu, final VarBindList varBindList) {
    for(int i=0, n=varBindList.size(); i<n; ++i) {
      pdu.add( varBindList.get(i).convert(varBindTypeConverter) );
      logger.trace(varBindList.get(i));
    }
  }

  private VarBindList sendPdu(final PDU pdu, final String operation) {
    try {
      //long start = System.currentTimeMillis();
      ResponseEvent response = snmp.send(pdu, target);
      //long end = System.currentTimeMillis();
      //logger.trace( (end-start) +" ms");
      //extract the response PDU (could be null if timed out)
      PDU responsePDU = response.getResponse();
      if( responsePDU == null ) {
        throw new SnmpManagerException("no answer", operation);
      }
      if( responsePDU.getErrorStatus() != SnmpConstants.SNMP_ERROR_SUCCESS ) {
        throw new SnmpResponseException(responsePDU.getErrorIndex(), responsePDU.getErrorStatus(), operation );
      }
      if (snmpAccessData.getVersion().equals(SnmpAccessData.VERSION_3)) {
        checkV3ErrorOids(responsePDU);
      }

      VarBindList vbl = varBindTypeConverter.toVarBindList(responsePDU);
      return vbl;
    } catch (IOException e) {
      logger.error(e);
      return null;
    }
  }
  
  private void checkV3ErrorOids(PDU pdu) {
    if (pdu.size() == 0) {
      return;
    }
    VariableBinding vb = pdu.get(0);
    if (vb == null) {
      return;
    }
    OID oid = vb.getOid();
    //FIXME fehlerbehandlung verbessern. evtl nicht nur runtimeexceptions werfen? SnmpConstants liefert methoden...
    if (SnmpConstants.snmpInASNParseErrs.equals(oid)) {
      throw new SnmpResponseException(oid + " " + SnmpConstants.mpErrorMessage(SnmpConstants.SNMP_MP_PARSE_ERROR), 0, 7001);
    } else if (SnmpConstants.snmpInBadCommunityNames.equals(oid)) {
      throw new SnmpResponseException(oid + " " + SnmpConstants.mpErrorMessage(SnmpConstants.SNMP_MP_COMMUNITY_ERROR), 0, 7002);
    } else if (SnmpConstants.snmpInBadCommunityUses.equals(oid)) {
      throw new SnmpResponseException(oid + " " + SnmpConstants.mpErrorMessage(SnmpConstants.SNMP_MP_COMMUNITY_ERROR), 0, 7003);
    } else if (SnmpConstants.snmpInBadVersions.equals(oid)) {
      throw new SnmpResponseException(oid + " " + "bad version", 0, 7004);
    } else if (SnmpConstants.snmpInPkts.equals(oid)) {
      throw new SnmpResponseException(oid + " ", 0, 7005);
    } else if (SnmpConstants.snmpInvalidMsgs.equals(oid)) {
      throw new SnmpResponseException(oid + " "  + SnmpConstants.mpErrorMessage(SnmpConstants.SNMP_MP_INVALID_MESSAGE) , 0, 7006);
    } else if (SnmpConstants.snmpProxyDrops.equals(oid)) {
      throw new SnmpResponseException(oid + " ", 0, 7007);
    } else if (SnmpConstants.snmpSetSerialNo.equals(oid)) {
      throw new SnmpResponseException(oid + " ", 0, 7008);
    } else if (SnmpConstants.snmpSilentDrops.equals(oid)) {
      throw new SnmpResponseException(oid + " ", 0, 7009);
    } else if (SnmpConstants.snmpUnavailableContexts.equals(oid)) {
      throw new SnmpResponseException(oid + " " + SnmpConstants.mpErrorMessage(SnmpConstants.SNMP_MP_UNAVAILABLE_CONTEXT), 0, 7010);
    } else if (SnmpConstants.snmpUnknownContexts.equals(oid)) {
      throw new SnmpResponseException(oid + " " + SnmpConstants.mpErrorMessage(SnmpConstants.SNMP_MP_UNKNOWN_CONTEXT) , 0, 7011);
    } else if (SnmpConstants.snmpUnknownPDUHandlers.equals(oid)) {
      throw new SnmpResponseException(oid + " "  + SnmpConstants.mpErrorMessage(SnmpConstants.SNMP_MP_UNKNOWN_PDU_HANDLERS), 0, 7012);
    } else if (SnmpConstants.snmpUnknownSecurityModels.equals(oid)) {
      throw new SnmpResponseException(oid + " "  + SnmpConstants.mpErrorMessage(SnmpConstants.SNMP_MP_UNSUPPORTED_SECURITY_MODEL), 0, 7013);
    } else if (SnmpConstants.authenticationFailure.equals(oid)) {
      throw new SnmpResponseException(oid + " "  + SnmpConstants.SNMP_ERROR_MESSAGES[SnmpConstants.SNMP_ERROR_AUTHORIZATION_ERROR] , 0, 7014);
    } else if (SnmpConstants.usmStatsDecryptionErrors.equals(oid)) {
      throw new SnmpResponseException(oid + " " + SnmpConstants.usmErrorMessage(SnmpConstants.SNMPv3_USM_DECRYPTION_ERROR) , 0, 7015);
    } else if (SnmpConstants.usmStatsNotInTimeWindows.equals(oid)) {
      throw new SnmpResponseException(oid + " " + SnmpConstants.usmErrorMessage(SnmpConstants.SNMPv3_USM_NOT_IN_TIME_WINDOW), 0, 7016);
    } else if (SnmpConstants.usmStatsUnknownEngineIDs.equals(oid)) {
      throw new SnmpResponseException(oid + " " + SnmpConstants.usmErrorMessage(SnmpConstants.SNMPv3_USM_UNKNOWN_ENGINEID), 0, 7017);
    } else if (SnmpConstants.usmStatsUnknownUserNames.equals(oid)) {
      throw new SnmpResponseException(oid + " "  + SnmpConstants.usmErrorMessage(SnmpConstants.SNMPv3_USM_UNKNOWN_SECURITY_NAME), 0, 7018);
    } else if (SnmpConstants.usmStatsUnsupportedSecLevels.equals(oid)) {
      throw new SnmpResponseException(oid + " "  + SnmpConstants.usmErrorMessage(SnmpConstants.SNMPv3_USM_UNSUPPORTED_SECURITY_LEVEL), 0, 7019);
    } else if (SnmpConstants.usmStatsWrongDigests.equals(oid)) {
      throw new SnmpResponseException(oid + " "  + SnmpConstants.usmErrorMessage(SnmpConstants.SNMPv3_USM_AUTHENTICATION_FAILURE), 0, 7020);
    }
  }
  
}
