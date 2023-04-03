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

import java.io.*;

import org.apache.log4j.Logger;

/**
 * ConfigData transports the modem-data from the SnmpDemon to the ConfigFileGenerator
 *
 */
public class ConfigData {
  
  static Logger logger = Logger.getLogger(ConfigData.class.getName());

  private ConfigData() {/*Konstruktor darf nur intern verwendet werden*/}
  
  /**
   * @return ConfigDataBuilder to create an new ConfigData-Object
   */
  public static ConfigDataBuilder newConfigData() {
    return new ConfigDataBuilder();
  }

  private String mac;
  private String ipAddress;
  
  private String name;
  private String hwRev;
  private String vendor;
  private String bootr;
  private String swRev;
  private String model;
  
  private String protocol;
  private String tlv;
  
  private String devSwCurrentVers;
  
  private String dppguid;
  private String pktc;

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("ConfigData(");
    sb.append("mac=").append(mac).append(";");
    sb.append("ipAddress=").append(ipAddress).append(";");
    sb.append("vendor=").append(vendor).append(";");
    sb.append("model=").append(model).append(";");
    sb.append("hwRev=").append(hwRev).append(";");
    sb.append("swRev=").append(swRev).append(";");
    sb.append("protocol=").append(protocol).append(";");
    sb.append("tlv=").append(tlv).append(";");
    sb.append("dppguid=").append(dppguid).append(";");
    sb.append("pktc=").append(pktc).append(";");
    sb.append(")");
    return sb.toString();
  }

  /**
   * ConfigDataBuilder is an intelligent Constructor for a ConfigData-Object
   *
   */
  public static class ConfigDataBuilder {
    private ConfigData cd = new ConfigData();
    private String sysDescr;
    private String devTypeIdentifier;
    
    /**
     * Parses the sysDescr to (name, hwRev, vendor, bootr, swRev, model)
     * @param sysDescr
     * @return
     */
    public ConfigDataBuilder sysDescr(@SuppressWarnings("hiding") String sysDescr) {
      this.sysDescr = sysDescr;
      try {
        int idx1 = sysDescr.indexOf("<<");
        cd.name = sysDescr.substring(0,idx1); //Name vor den spitzen Klammern
        int idx2 = sysDescr.indexOf(">>");
        //String innerhalb der doppelten spitzen Klammern...
        String infos = sysDescr.substring(idx1+2, idx2);
        //...zerlegen anhand der Paare "KEY: value; " in einem einzigen Split
        String[] infoParts = infos.split(";|:");
        cd.hwRev =  infoParts[1].trim();
        cd.vendor = infoParts[3].trim();
        cd.bootr = infoParts[5].trim();
        cd.swRev = infoParts[7].trim();
        cd.model = infoParts[9].trim();
      } catch( RuntimeException e ) {
        e.printStackTrace();
      }
      return this;
    }

    /**
     * Parses the devTypeIdentifier to protocol and tlv
     * @param devTypeIdentifier
     * @return
     */
    public ConfigDataBuilder devTypeIdentifier(@SuppressWarnings("hiding") String devTypeIdentifier) {
      this.devTypeIdentifier = devTypeIdentifier;
      String [] parts = devTypeIdentifier.split(":",2);
      if( parts.length != 2 ) {
        throw new IllegalArgumentException("invalid devTypeIdentifier could not be splitted");
      }
      cd.protocol = parts[0];
      cd.tlv = parts[1];
      return this;
    }

    /**
     * Sets the mac
     * @param mac
     * @return
     */
    public ConfigDataBuilder mac(String mac) {
      cd.mac = mac;
      return this;
    }
    
    /**
     * Sets the ipAddress
     * @param ipAddress
     * @return
     */
    public ConfigDataBuilder ipAddress(String ipAddress) {
      cd.ipAddress = ipAddress;
      return this;
    }

    /**
     * Sets the devSwCurrentVers
     * @param devSwCurrentVers
     * @return
     */
    public ConfigDataBuilder devSwCurrentVers(String devSwCurrentVers) {
      cd.devSwCurrentVers = devSwCurrentVers;
      return this;
    }
    
    /**
     * Sets the dppguid
     * @param dppguid
     * @return
     */
    public ConfigDataBuilder dppguid(String dppguid) {
      cd.dppguid = dppguid;
      return this;
    }
    
    
        /**
     * Sets the pktc
     * @param pktc
     * @return
     */
    public ConfigDataBuilder pktc(String pktc) {
      cd.pktc = pktc;
      return this;
    }

   
    /**
     * Constructs the ConfigData-Object
     * @return new ConfigData-Object
     */
    public ConfigData build() {
      logger.trace( cd.toString()+" erhalten");
      return cd;
    }
  }

  
  /**
   * @return ConfigData in the ConfigFileGenerator Interface Format (Bytes)
   * @throws UnsupportedEncodingException
   */
  public byte[] formatForCfgFileGenerator( ) throws UnsupportedEncodingException {
    ConfigData cd=this;
    StringBuilder sb = new StringBuilder();
    sb.append("Start\t0000\tSnmpAdapter\t");
    sb.append(cd.ipAddress).append('\t');
    sb.append(cd.mac).append('\t');
    sb.append(cd.name).append('\t');
    sb.append(cd.hwRev).append('\t');
    sb.append(cd.vendor).append('\t');
    sb.append(cd.bootr).append('\t');
    sb.append(cd.swRev).append('\t');
    sb.append(cd.model).append('\t');
    sb.append(cd.protocol).append('\t');
    sb.append(cd.tlv).append('\t');
    sb.append(cd.devSwCurrentVers).append('\t');
    sb.append(cd.dppguid).append('\t');
    sb.append(cd.pktc).append('\t');
    sb.append("eol\n"); //Ende der Nachricht
    //Laenge der Nachricht eintragen:
    String len = String.valueOf( sb.length() );
    int lenl = len.length();
    for( int i=0; i<lenl; ++i ) {
      sb.setCharAt( i+4+6-lenl, len.charAt(i) );
    }
    return sb.toString().getBytes("UTF8"); //UTF-8, da ascii-127-compatible (und vom ConfigFileGenerator so erwartet)
  }

  public static ConfigData readFromString(String data) throws IOException {
    if( data == null ) {
      throw new IOException("Error while parsing ConfigData: Invalid stream length");
    }
    String[] cdsp = data.split("\t");
    if( cdsp.length < 17 || ! cdsp[16].equals("eol\n") ) {
      throw new IOException("Error while parsing ConfigData: Invalid stream end");
    }
    if( !cdsp[0].equals("Start") || !cdsp[2].equals("SnmpAdapter") ) {
      throw new IOException("Error while parsing ConfigData: Invalid stream beginning");
    }
    
    ConfigData cd = new ConfigData();
    String len = cdsp[1];
    if( Integer.parseInt(len) != data.length() ) {
      System.err.println( len + " " + data.length() );
      throw new IOException("Error while parsing ConfigData: Invalid stream length");
    }
    
    cd.ipAddress = cdsp[3];
    cd.mac = cdsp[4];
    cd.name = cdsp[5];
    cd.hwRev = cdsp[6];
    cd.vendor = cdsp[7];
    cd.bootr = cdsp[8];
    cd.swRev = cdsp[9];
    cd.model = cdsp[10];
    cd.protocol = cdsp[11];
    cd.tlv = cdsp[12];
    cd.devSwCurrentVers = cdsp[13];
    cd.dppguid = cdsp[14];
    cd.pktc = cdsp[15];
    return cd;
  }

  /**
   * Initializes an new ConfigData-Object from the data in the InputStream
   * @param in
   * @return
   * @throws IOException
   */
  public static ConfigData readFromInputStream(InputStream in) throws IOException {
    BufferedReader br = new BufferedReader( new InputStreamReader( in ) );
    String cds = br.readLine();
    if( cds == null ) {
      throw new IOException("Error while parsing ConfigData: Invalid stream length");
    }
    return readFromString( cds );
  }
 
  
  /**
   * @return the devTypeIdentifier
   */  
  public String getDevTypeIdentifier() {
    return protocol+":"+tlv;
  }

  /**
   * @return the sysDescr
   */  
  public String getSysDescr() {
    StringBuilder sb = new StringBuilder();
    sb.append(name);
    sb.append("<<");
    sb.append("HW_REV: ").append(hwRev).append("; ");
    sb.append("VENDOR: ").append(vendor).append("; ");
    sb.append("BOOTR: ").append(bootr).append("; ");
    sb.append("SW_REV: ").append(swRev).append("; ");
    sb.append("MODEL: ").append(model);
    sb.append(">>");
    return sb.toString();
  }

  /**
   * @return the mac    //System.err.println(sb.toString().replaceAll("\t","\\\\t"));

   */
  public String getMac() {
    return mac;
  }

  /**
   * @return the ipAddress
   */
  public String getIpAddress() {
    return ipAddress;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @return the hwRev
   */
  public String getHwRev() {
    return hwRev;
  }

  /**
   * @return the vendor
   */
  public String getVendor() {
    return vendor;
  }

  /**
   * @return the bootr
   */
  public String getBootr() {
    return bootr;
  }

  /**
   * @return the swRev
   */
  public String getSwRev() {
    return swRev;
  }

  /**
   * @return the model
   */
  public String getModel() {
    return model;
  }

  /**
   * @return the protocol
   */
  public String getProtocol() {
    return protocol;
  }

  /**
   * @return the tlv
   */
  public String getTlv() {
    return tlv;
  }

  /**
   * @return the devSwCurrentVers
   */
  public String getDevSwCurrentVers() {
    return devSwCurrentVers;
  }
  
  /**
   * @return the pktc-Type
   */
  public String getPktc() {
    return pktc;
  }
  
}
