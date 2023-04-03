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
package dhcpAdapterDemon;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

import snmpTrapDemon.leases.MacAddress;
import snmpTrapDemon.poolUsage.IPAddress;
import dhcpAdapterDemon.types.DhcpAction;

/**
 * DhcpData transports the dhcp-data from the Dhcpd to the DhcpAdapterDemon
 * 
 */
public class DhcpData implements Cloneable{
  
  private static SimpleDateFormat leaseTimerFormatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
  private static long RESERVED_LEASES_LEASE_TIME = 0;
  private static final String DOUBLE_SLASH_TOKEN = "##########";
  
  static Logger logger = Logger.getLogger(DhcpData.class.getName());
  private IPAddress ciaddr;
  private MacAddress chaddr;
  private String startTime;
  private String endTime;
  private String giaddr;
  private String vci;
  private String remoteId;
  private String dppInstance;
  private DhcpAction action;
  private String hostname;
  private String type;
  private String option43;
  private String circuitid;
  private String discover;
  private String offer;
  private String dnsEntry;
  private boolean isReserved;
  
  private DhcpData() {/*Konstruktor darf nur intern verwendet werden*/}
  
  /**
   * @return DhcpDataBuilder to create an new DhcpData-Object
   */
  public static DhcpDataBuilder newDhcpData() {
    return new DhcpDataBuilder();
  }

  public void setDiscover(String discover)
  {
    this.discover=discover;
  }
  
  public static void setReservedLeasesLeaseTime( int timeInSeconds ) {
    RESERVED_LEASES_LEASE_TIME = timeInSeconds * 1000L;
  }
   
  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("DhcpData(");
    sb.append("ciaddr=").append(ciaddr).append(";");
    sb.append("chaddr=").append(chaddr).append(";");
    sb.append("startTime=").append(startTime).append(";");
    sb.append("endTime=").append(endTime).append(";");
    sb.append("giaddr=").append(giaddr).append(";");
    sb.append("vci=").append(vci).append(";");
    sb.append("remoteId=").append(remoteId).append(";");
    sb.append("isReserved=").append(isReserved).append(";");
    sb.append("dppInstance=").append(dppInstance).append(";");
    sb.append("action=").append(action).append(";");
    sb.append("option43=").append(option43).append(";");
    sb.append("circuitid=").append(circuitid).append(";");
    sb.append("discover=").append(discover).append(";");
    sb.append("offer=").append(offer).append(";");
    sb.append("dnsEntry=").append(dnsEntry).append(";");
    sb.append("isReserved=").append(isReserved).append(";");
    sb.append(")");
    return sb.toString();
  }

  /**
   * DhcpDataBuilder is an intelligent Constructor for a DhcpData-Object
   *
   */
  public static class DhcpDataBuilder {
    private DhcpData cd = new DhcpData();
    
    public DhcpData fromString( String string ) throws IOException {
      InputStream is = new ByteArrayInputStream(string.getBytes());
      cd = DhcpData.readFromInputStream( is );
      is.close();
      return cd;
    }

    public DhcpDataBuilder ipMac(IPAddress ip, MacAddress mac) {
      cd.ciaddr = ip;
      cd.chaddr = mac;
      return this;
    }

    public DhcpDataBuilder times(String start, String end) {
      cd.startTime = start;
      cd.endTime = end;
      return this;
    }

    public DhcpDataBuilder giaddr(String giaddr) {
      cd.giaddr = giaddr;
      return this;
    }

    public DhcpDataBuilder vci(String vci) {
      cd.vci = vci;
      return this;
    }
    
    public DhcpDataBuilder remoteId(String remoteId) {
      cd.remoteId = remoteId;
      return this;
    }
    
    public DhcpDataBuilder dppInstance(String dppInstance) {
      cd.dppInstance = dppInstance;
      return this;
    }
    
    public DhcpDataBuilder action(String action) {
      cd.action = DhcpAction.parse(action);
      return this;
    }
    public DhcpDataBuilder action(DhcpAction action) {
      cd.action = action;
      return this;
    }

    public DhcpData build() {
      cd.hostname = cd.chaddr.toHex();
      cd.type = inferType(cd.vci);
      return cd;
    }
    
    private static String inferType( String vci ) {
      if( vci == null ) {
        return null;
      }
      int posE = vci.indexOf(':');
      int posA = vci.indexOf('"')+1; //wenn vorhanden: sollte 0 sein
      return posE != -1 ? vci.substring(posA,posE) : "";
    }
    
    
  }

  /**
   * Writes the data of the given ConfigData-Object to the OutputStream
   * @param os
   * @param cd
   * @throws IOException
   */
  public static void writeToOutputStream(OutputStream os, DhcpData cd) throws IOException {
    
    if(cd.discover!=null)writeToOutputStreamNewFormat(os, cd); //falls neues Feld verwendet wird, neue Methode verwenden
    
    StringBuilder sb = new StringBuilder(1000);
    sb.append("Start\t");
    sb.append("0000\t");
    sb.append("Adapter\t");
    sb.append(cd.ciaddr).append('\t');
    append(sb, cd.chaddr );
    append(sb, cd.startTime);
    append(sb, cd.endTime);
    append(sb, cd.giaddr);
    append(sb, cd.vci);
    append(sb, cd.remoteId);
    append(sb, cd.dppInstance);
    append(sb, cd.action.getCode());
    sb.append("eol\n"); //Ende der Nachricht
    String len = String.valueOf( sb.length() );
    int lenl = len.length();
    for( int i=0; i<lenl; ++i ) {
      sb.setCharAt( i+4+6-lenl, len.charAt(i) );
    }
    //System.err.println(sb.toString().replaceAll("\t","\\\\t"));
    os.write(sb.toString().getBytes()); 
  }
  
  /**
   * Writes the data of the given ConfigData-Object to the OutputStream
   * @param os
   * @param cd
   * @throws IOException
   */
  public static void writeToOutputStreamNewFormat(OutputStream os, DhcpData cd) throws IOException {
    StringBuilder sb = new StringBuilder(1000);
    sb.append("Start\t");
    sb.append("0000\t");
    sb.append("Adapter\t");
    sb.append(cd.ciaddr).append('\t');
    append(sb, cd.chaddr );
    append(sb, cd.startTime);
    append(sb, cd.endTime);
    append(sb, cd.giaddr);
    append(sb, cd.vci);
    append(sb, cd.remoteId);
    append(sb, cd.dppInstance);
    append(sb, cd.action.getCode());
    append(sb, cd.option43);
    append(sb, cd.circuitid);
    append(sb, cd.discover);
    append(sb, cd.offer);
    append(sb, cd.dnsEntry);
    append(sb, String.valueOf(cd.isReserved));
    sb.append("eol\n"); //Ende der Nachricht
    String len = String.valueOf( sb.length() );
    int lenl = len.length();
    for( int i=0; i<lenl; ++i ) {
      sb.setCharAt( i+4+6-lenl, len.charAt(i) );
    }
    //System.err.println(sb.toString().replaceAll("\t","\\\\t"));
    os.write(sb.toString().getBytes()); 
  }

  
  
  /**
   * @param sb
   * @param s
   */
  private static final void append(StringBuilder sb, String s ) {
    if( s != null ) {
      sb.append(s);
    }
    sb.append('\t');
  }
  /**
   * @param sb
   * @param mac
   */
  private static void append(StringBuilder sb, MacAddress mac) {
    if( mac != null ) {
      sb.append(mac.toString());
    }
    sb.append('\t');
  }

  public static DhcpData readFromString(String data) throws IOException {
    String[] cdsp = data.split("\t");
    
    if(cdsp.length>16) // falls neues Format andere Methode verwenden
    {
      return readFromStringNewFormat(data);
    }
    
    DhcpData cd = new DhcpData();
    if( cdsp.length < 13 || ! cdsp[12].equals("eol\n") ) {
      throw new IOException("Error while parsing DhcpData: Invalid stream end");
    }
    if( !cdsp[0].equals("Start") || !cdsp[2].equals("Adapter") ) {
      throw new IOException("Error while parsing DhcpData: Invalid stream beginning");
    }
    
    String len = cdsp[1];
    if( Integer.parseInt(len) != data.length() ) {
      logger.error( len + "<->" + data.length() );
      throw new IOException("Error while parsing DhcpData: Invalid stream length");
    }
 
    try {
      cd.ciaddr = IPAddress.parse(cdsp[3]);
      cd.chaddr = MacAddress.parse(cdsp[4]);
      cd.startTime = cdsp[5];
      
      // aktuelle Zeit als Quickfix
      SimpleDateFormat sdfmt = new SimpleDateFormat();
      sdfmt.applyPattern("yyyy/MM/dd HH:mm:ss");
      cd.startTime=sdfmt.format(new Date(System.currentTimeMillis()-100)); 

      // Ende Quickfix
      
      cd.endTime = cdsp[6];
      cd.giaddr = cdsp[7];
      cd.vci = cdsp[8];
      cd.remoteId = cdsp[9];
      cd.dppInstance = cdsp[10];
      cd.action = DhcpAction.parse(cdsp[11]);      
      cd.hostname = cd.chaddr.toHex();
      cd.type = DhcpDataBuilder.inferType(cd.vci);
      
      if( cd.startTime != null && cd.startTime.startsWith("1970") ) {
        changeForStaticHosts(cd);
      }
      return cd;   
    } catch( Exception e ) { //alle Fehler beim Parsen
      throw new IOException( e.getMessage(), e );
    }
  }
  

  public static DhcpData readFromStringNewFormat(String data) throws IOException {
    String[] cdsp = data.split("\t");
    
    DhcpData cd = new DhcpData();
    if( cdsp.length < 19 || ! cdsp[18].equals("eol\n") ) {
      throw new IOException("Error while parsing DhcpData: Invalid stream end");
    }
    if( !cdsp[0].equals("Start") || !cdsp[2].equals("Adapter") ) {
      throw new IOException("Error while parsing DhcpData: Invalid stream beginning");
    }
    
    String len = cdsp[1];
    if( Integer.parseInt(len) != data.length() ) {
      logger.error( len + "<->" + data.length() );
      throw new IOException("Error while parsing DhcpData: Invalid stream length");
    }
 
    try {
      cd.ciaddr = IPAddress.parse(cdsp[3]);
      cd.chaddr = MacAddress.parse(cdsp[4]);
      cd.startTime = cdsp[5];
      
      // aktuelle Zeit als Quickfix
      SimpleDateFormat sdfmt = new SimpleDateFormat();
      sdfmt.applyPattern("yyyy/MM/dd HH:mm:ss");
      cd.startTime=sdfmt.format(new Date(System.currentTimeMillis()-100)); 

      // Ende Quickfix
      
      cd.endTime = cdsp[6];
      cd.giaddr = cdsp[7];
      cd.vci = cdsp[8];
      cd.remoteId = cdsp[9];
      cd.dppInstance = cdsp[10];
      cd.action = DhcpAction.parse(cdsp[11]);  
      cd.option43 = cdsp[12];
      cd.circuitid = cdsp[13];
      cd.discover = cdsp[14];
      cd.offer = cdsp[15];
      cd.dnsEntry = cdsp[16];
      cd.isReserved = Boolean.parseBoolean(cdsp[17]);
      
      cd.hostname = cd.chaddr.toHex();
      cd.type = DhcpDataBuilder.inferType(cd.vci);
      
      if( cd.startTime != null && cd.startTime.startsWith("1970") ) {
        changeForStaticHosts(cd);
      }
      return cd;   
    } catch( Exception e ) { //alle Fehler beim Parsen
      throw new IOException( e.getMessage(), e );
    }
  }

  
  /**
   * 
   * @param cd
   */
  private static synchronized void changeForStaticHosts(DhcpData cd) {
    //synchronized wegen Benutzung von SimpleDateFormat
    cd.isReserved = true;
    long now = System.currentTimeMillis();
    cd.startTime = leaseTimerFormatter.format(new Date(now));
    cd.endTime = leaseTimerFormatter.format(new Date(now+ RESERVED_LEASES_LEASE_TIME ));
  }

  /**
   * parses the remoteId
   * Bugfix für den Lastgenerator: Auch Format "00:11:02:33:00:55" (mit "") wird akzeptiert
   * @param remoteId Expected to be null or in format 00:11:02:33:00:55.
   * @return
   */
  private static MacAddress parseRemoteId(String remoteId) {
    if( remoteId == null || remoteId.length() == 0 ) {
      return null;
    }
    try {      
      // Spezialfall: MAC in als Zeichen in Binaerform, wobei 1 Zeichen und letztes Zeichen
      // Anfuehrungszeichen sind und abgeschnitten werden. Die Zeichenfolge "\\" wird durch "\" ersetzt
      // "\" wird entfernt                   
      if((remoteId != null) && (remoteId.length() >= 8) && (remoteId.length() <= 14)) {
        // Check ob "\" Zeichen vorhanden sind 
        // "\'\'\'\'\'\'"  Es können max. 6 Escape-Zeichen vorhanden sein        
        if(remoteId.contains("\\")) {
          remoteId = remoteId.replace("\\\\", DOUBLE_SLASH_TOKEN);   //  "\\"   -> DOUBLE_SLASH_TOKEN
          remoteId = remoteId.replace("\\", "");               // "\"  ->  ""
          remoteId = remoteId.replace(DOUBLE_SLASH_TOKEN, "\\");     // DOUBLE_SLASH_TOKEN  ->  "\\"                          
        }
        if((remoteId.length() == 8) && remoteId.startsWith("\"") && remoteId.endsWith("\"")) {
          remoteId = remoteId.substring(1, 7);
        }
      }
      // Spezialfall: MAC als 6 Zeichen in Binaerform  
      if(remoteId != null && remoteId.length() == 6) {
        byte[] byteArray;
        try {
          byteArray = remoteId.getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException e) {
          logger.error("the charset : UTF-8 is not supported");
          try {
            byteArray = remoteId.getBytes("US-ASCII");
          }
          catch (UnsupportedEncodingException ex) {
            logger.error("the charset : US-ASCII is not supported");
            throw new IllegalArgumentException();
          } 
        }
        int[] parts = new int[] {byteArray[0],byteArray[1],byteArray[2],byteArray[3],byteArray[4],byteArray[5]};         
        return new MacAddress(parts);
      }      
      return MacAddress.parse(remoteId);
    } catch( IllegalArgumentException e ) {
      String trunc = remoteId.replace("\"","").trim();
      try {
        return MacAddress.parse(trunc);
      } catch( IllegalArgumentException e2 ) {
        //dieser Fehler sollte nicht auftreten
        logger.warn("Ignored error while parsing DhcpData: Unparseable remoteId \""+remoteId+"\"");
        return null;
      }
    }
  }

  /**
   * Initializes an new ConfigData-Object from the data in the InputStream
   * @param in
   * @return
   * @throws IOException
   */
  public static DhcpData readFromInputStream(InputStream in) throws IOException {
    BufferedReader br = new BufferedReader( new InputStreamReader( in ) );
    String cds = br.readLine();
    if( cds == null ) {
      throw new IOException("Error while parsing DhcpData: Invalid stream length");
    }
    return readFromString( cds );
  }

  
  /**
   * @return the hostname
   */
  public String getHostname() {
    return hostname;
  }
  
  /**
   * @return the ciaddr
   */
  public IPAddress getCiaddr() {
    return ciaddr;
  }

  /**
   * @return the chaddr
   */
  public MacAddress getChaddr() {
    return chaddr;
  }

  /**
   * @return the startTime
   */
  public String getStartTime() {
    return startTime;
  }

  /**
   * @return the endTime
   */
  public String getEndTime() {
    return endTime;
  }

  /**
   * @return the giaddr
   */
  public String getGiaddr() {
    return giaddr;
  }

  /**
   * @return the vci
   */
  public String getVci() {
    return vci;
  }

  /**
   * @return the remoteId
   */
  public String getRemoteId() {
    return remoteId;
  }

  /**
   * @return the dppInstance
   */
  public String getDppInstance() {
    return dppInstance;
  }

  /**
   * @return the action
   */
  public DhcpAction getAction() {
    return action;
  }

  /**
   * @return the type
   */
  public String getType() {
    return type;
  }

  public boolean isInsert() {
    return action == DhcpAction.DHCPREQUEST_NEW || action ==  DhcpAction.DHCPREQUEST_RENEW;
  }

  public boolean isDelete() {
    return action == DhcpAction.DHCPRELEASE || action == DhcpAction.LEASEEXPIRE;
  }

  /**
   * @return
   */
  public boolean isReserved() {
    return isReserved;
  }
  
  public String getDiscover() {
    return discover;
  }

  
  public String getOffer() {
    return offer;
  }

  public String getOption43()
  {
    return option43;
  }
  
  public String getCircuitid()
  {
    return circuitid;
  }
  
  public String getDNSEntry()
  {
    return dnsEntry;
  }
  
  
  @Override
  public Object clone() throws CloneNotSupportedException {
    return super.clone();//TODO: ggf. Arrays Clonen, falls notwendig, aktuell werden diese allerdings nicht geaendert/manipuliert
  }


}
