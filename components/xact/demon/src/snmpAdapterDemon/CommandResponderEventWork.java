/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 GIP SmartMercial GmbH, Germany
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
package snmpAdapterDemon;

import java.util.Random;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.MessageException;
import org.snmp4j.PDU;
import org.snmp4j.mp.StatusInformation;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;

import com.gip.xyna.demon.DemonProperties;
import com.gip.xyna.demon.worker.SlaveWork;

import snmpAdapterDemon.ConfigData.ConfigDataBuilder;
import snmpAdapterDemon.SnmpAdapterDemon.SimpleSnmp;

/**
 * Diese Klasse implementiert die Verarbeitung der von den MTA gesendeten und vom SnmpAdapterDemon empfangenen snmp-Pakete
 */
public class CommandResponderEventWork implements SlaveWork<SimpleSnmp,CommandResponderEvent> {
   
  final static Logger logger = Logger.getLogger(CommandResponderEventWork.class);
  private static Random random = new Random();
  
  private CommandResponderEvent event;

  public CommandResponderEventWork(CommandResponderEvent event) {
    this.event = event;
  }

  /**
   * @see com.gip.xyna.demon.worker.SlaveWork#work(java.lang.Object)
   * 
   * Methode wir bei TRAP mit return verlassen
   */
  public boolean work(SimpleSnmp simpleSnmp) {
    PDU command = event.getPDU();
    
    boolean success = false;//default
    int pduErrorStatus=PDU.noError;//default
    
    String mac;
    try{
      String incommingSnmpTrapValue = searchVarBindStringValue( OIDs.OID_snmpTrapOID_1_3_6_1_6_3_1_1_4_1_0, 1, command, false,"Type of snmp trap  not found." );
      String pktcType=determinePktcType(incommingSnmpTrapValue);
      String firstInformOID=getDemonPktcPropertyAndThrowExceptionWhenNotFound(pktcType, "MAC");
      mac = searchVarBindStringValue(firstInformOID, 5,  command,  true, null);
      if (mac.length()!=17){
        logger.error("got ifPhysAddress with length!=12: PDU="+ command);
      }    
    } catch (Exception e){
      try {      
        mac = searchVarBindStringValue(OIDs.OID_ifPhysAddress_1_3_6_1_2_1_2_2_1_6_1, 5,  command,  true, null);
        if (mac.length()!=17){
          logger.error("got ifPhysAddress with length!=12: PDU="+ command);
        }
      } catch( OIDNotFoundException e2 ) {
        mac = "?";
      }      
    }
    
    try {
      NDC.push( mac ); //mac in NDC fürs Logging eintragen

      if( command.getType() == PDU.TRAP ) {
        logger.warn("got Trap: "+ command.toString() );
        return false; //Trap ist unerwartet, deswegen false, es darf keine Antwort gesendet werden, deshalb hier schon RETURN!!!!!!!
      } 
      else if( command.getType() == PDU.INFORM ) {         
        try {
          processSnmpInform(command);
          success = true; //Anfrage korrekt beantwortet 
        }
        catch (Exception e) {              
          logger.info("Error processing snmp inform", e);
        }
      }
      else { //no INFORM, no TRAP
        logger.error("got no Inform: "+ command.toString() );
        pduErrorStatus=PDU.noSuchName;
      }

      try {        
        //Antwort rücksenden:
        PDU response = new PDU(command);
        response.setType(PDU.RESPONSE);
        response.setRequestID( command.getRequestID());
        response.setErrorIndex(0);
        response.setErrorStatus(pduErrorStatus);

        simpleSnmp.getSnmp().getMessageDispatcher().returnResponsePdu(event.getMessageProcessingModel(),
                                                                      event.getSecurityModel(),
                                                                      event.getSecurityName(),
                                                                      event.getSecurityLevel(),
                                                                      response,
                                                                      event.getMaxSizeResponsePDU(),
                                                                      event.getStateReference(),
                                                                      new StatusInformation() );
      } catch (MessageException e) {
        logger.error( "Error while sending response", e );
        success = false;
      }    
      return success;
    } finally {
      NDC.pop();
    }
  }
  
  /**
   * Processes SNMP Inform. Particulary send data to the ConfigFileGenerator
   * @param pdu
   * @throws OIDNotFoundException
   * @throws PropertyNotFoundException
   * @throws PropertyWrongConfiguredException
   */
  private void processSnmpInform(PDU pdu) throws OIDNotFoundException, PropertyNotFoundException, PropertyWrongConfiguredException {
    
    try{      
      String incommingSnmpTrapValue = searchVarBindStringValue( OIDs.OID_snmpTrapOID_1_3_6_1_6_3_1_1_4_1_0, 1, pdu, false,"Type of snmp trap  not found." );
        if( logger.isTraceEnabled() ) {
        logger.trace("Order in, PDU " + pdu);
      } else if( logger.isDebugEnabled() ) {
        logger.debug("Order in. Snmp trap oid="+incommingSnmpTrapValue);
      }
      String pktcType=determinePktcType(incommingSnmpTrapValue);
      String firstInformOID=getDemonPktcPropertyAndThrowExceptionWhenNotFound(pktcType, "IncommingSNMPTrap.1");
      if (firstInformOID. equals(incommingSnmpTrapValue)){ //First Inform: Send data to ConfigFileGenerator  
        if (findVariableBinding(OIDs.OID_sysDescr_1_3_6_1_2_1_1_1_0, 2, pdu)!=null){
          ConfigData configData=mapSnmpDataToConfigData(pktcType, pdu);
          ConfigDataSender.getInstance().send( configData); 
        } else {
          logger.debug( "Second inform (without system description)  ignored.");
        }
      } else { //Status Inform: Log Status, if configured
        String statusOID=DemonProperties.getProperty( pktcType+ ".IncommingSNMPTrap.2.Status");
        if (statusOID!=null){//statusOID configured
          String statusDecode = getDemonPktcPropertyAndThrowExceptionWhenNotFound(pktcType,"IncommingSNMPTrap.2.Status.Decode");
          String statusValue = searchVarBindStringValue(statusOID, 1, pdu, false, "Status not found." );
          String decodeStatus=decode(statusValue, statusDecode);
          logger.info("Status inform: "+decodeStatus);
        }
      }
    } catch (PktcNotFoundException e){
      logger.debug("Snmp trap type silently ignored." );
    }
  }

  private String getDemonPktcPropertyAndThrowExceptionWhenNotFound(String pktcType, String key) throws PropertyNotFoundException  {
    String value=DemonProperties.getProperty(pktcType+"."+key);
    if (value==null){
      throw new PropertyNotFoundException(pktcType+"."+key);
    }
    return value;
  }

    private String determinePktcType(String incommingSnmpTrapValue) throws PropertyNotFoundException, PktcNotFoundException  {
    String pktcTypeString=DemonProperties.getProperty("PKTC.Types");
    assert(pktcTypeString!=null);
    final String[] pktcType=pktcTypeString.split(",");
    for (String pktc:pktcType){
      String firstInformOID=getDemonPktcPropertyAndThrowExceptionWhenNotFound(pktc,"IncommingSNMPTrap.1");
      String secondInformOID=getDemonPktcPropertyAndThrowExceptionWhenNotFound(pktc, "IncommingSNMPTrap.2");
      if (incommingSnmpTrapValue.equals(firstInformOID) ||incommingSnmpTrapValue.equals(secondInformOID)) {
        return pktc;
      }
    }
    throw new PktcNotFoundException(incommingSnmpTrapValue);
   }
    
  private static String decode(String statusValue, String statusDecode) throws PropertyWrongConfiguredException {
    String[] statusValueArray=statusDecode.split(",");
    if((statusValueArray.length)%2!=1){
      throw new PropertyWrongConfiguredException("Odd number of comma separated values expected in statusDecode="+statusDecode+".");
    }
    for(int i=0; i<statusValueArray.length-1; i=i+2){
      if(statusValueArray[i].equals(statusValue)){
        return(statusValueArray[i+1]);
      }        
    }
    return statusValueArray[statusValueArray.length-1];
  }

  private ConfigData mapSnmpDataToConfigData(String pktc, PDU pdu) throws OIDNotFoundException, PropertyNotFoundException {
    
    String addr =  this.event.getPeerAddress().toString(); //Datenanreicherung aus this
    String ipAddr = addr.substring(0,addr.indexOf('/') );
    
    ConfigDataBuilder builder=ConfigData.newConfigData();
    
    //ModemType-unabhängiges Mapping:
    builder.sysDescr(searchVarBindStringValue( OIDs.OID_sysDescr_1_3_6_1_2_1_1_1_0, 2, pdu, false,"System description not found." ));// System Description-> name,hwRev, vendor,bootr,swRev, model
    builder.ipAddress( ipAddr );
    builder. dppguid(createDppGuid() );
    
     //ModemType-abhängiges Mapping:
    String devTypeIdentifierOID=getDemonPktcPropertyAndThrowExceptionWhenNotFound(pktc, "DevTypeIdentifier");
    String devSwCurrentVersionOID=getDemonPktcPropertyAndThrowExceptionWhenNotFound(pktc, "DevSwCurrentVersion");
    String macOID=getDemonPktcPropertyAndThrowExceptionWhenNotFound(pktc, "MAC");
    builder.devTypeIdentifier( searchVarBindStringValue( devTypeIdentifierOID, 4, pdu, false, "Type identifier not found." ) );
    builder.devSwCurrentVers( searchVarBindStringValue( devSwCurrentVersionOID, 3, pdu, false, "Device software current version not found." ) );
    builder.mac(searchVarBindStringValue(macOID , 5, pdu, true,"Mac address not found.") );
    builder.pktc(pktc);
    
    return builder.build();
  }

  private String createDppGuid() {
    StringBuilder sb = new StringBuilder();
    sb.append(Integer.toHexString(random.nextInt()));
    sb.append(Integer.toHexString(random.nextInt()));
    sb.append(".cfg");
    return sb.toString();
  }

  private String getOid(VariableBinding vb) {
    if( vb == null ) {
      return null;
    }
    return "."+vb.getOid().toString();
  }

  /**
   * Suche nach dem StringValue zu der angegbenen Oid 
   * @param oid
   * @param expectedPosition Position der Daten in der PDU, korrekte Position erspart Suchaufwand 
   * @param pdu
   * @param forceHexFormat octetString wird immer im Hex Format ausgegeben
   * @return
   * @throws OIDNotFoundException falls die Oid nicht gefunden werden konnte
   */
  private String searchVarBindStringValue(String oid, int expectedPosition, PDU pdu, boolean forceHexFormat, String notFoundMessage ) throws OIDNotFoundException {
    VariableBinding vb = findVariableBinding(oid, expectedPosition, pdu);
    if( vb == null ) {
      if (notFoundMessage!=null){
        logger.debug(notFoundMessage);
        throw new OIDNotFoundException(oid+":"+notFoundMessage);
      } else {
        throw new OIDNotFoundException(oid);
      }
    }
    Variable v = vb.getVariable();
    if( v instanceof OctetString ) {
      if (forceHexFormat){
         return ((OctetString)v).toHexString();
      } else {
        return ((OctetString)v).toString();
      }
    } else if( v instanceof org.snmp4j.smi.OID ) {
      return "."+v.toString();
    } else if( v instanceof org.snmp4j.smi.Integer32 ) {
      return v.toString();
    } else {
      logger.error( "falscher Variable-Typ " + v.getClass().getName() +" for oid "+oid);
      return v.toString();
    }
  }

  /**
   * sucht Variable Binding in PDU
   * @param oid
   * @param expectedPosition
   * @param pdu
   * @return VariableBinding, falls vorhanden, null sonst
   */
  private VariableBinding findVariableBinding(String oid, int expectedPosition, PDU pdu) {
    VariableBinding vb = null;
    if( expectedPosition < pdu.size() ) {
      vb = pdu.get(expectedPosition);
    }
    if( vb == null || ! oid.equals( getOid(vb) ) ) {
      //VarBind nicht an erwarteter Stelle gefunden, daher in kompletter Liste suchen
      vb = null; //bisherigen Eintrag löschen
      for( int v=0; v<pdu.size(); ++v ) {
        if( oid.equals( getOid(pdu.get(v)) ) ) {
          vb = pdu.get(v);
          break;
        }
      }
    }
    return vb;
  }
  
  public static class OIDNotFoundException extends Exception {
    private static final long serialVersionUID = 1L;
    public OIDNotFoundException(String oid) {
      super( oid );
    }
  }
  
  public static class PropertyNotFoundException extends Exception {
    private static final long serialVersionUID = 1L;
    public PropertyNotFoundException(String key) {
      super( "Property(key="+key+") not found." );
    }
  }
  
  public static class PropertyWrongConfiguredException extends Exception {
    private static final long serialVersionUID = 1L;
    public PropertyWrongConfiguredException(String message) {
      super( message );
    }
  }
  
  public static class PktcNotFoundException extends Exception {
    private static final long serialVersionUID = 1L;
    public PktcNotFoundException(String oid) {
      super( oid );
    }
  }
  
}