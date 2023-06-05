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
package com.gip.xyna.demon.snmp;

import org.apache.log4j.Logger;

import com.gip.xyna.demon.Demon;
import com.gip.xyna.demon.DemonProperties;
import com.gip.xyna.demon.DemonSignal;
import com.gip.xyna.utils.snmp.OID;
import com.gip.xyna.utils.snmp.agent.utils.EnumIntStringSnmpTable;
import com.gip.xyna.utils.snmp.agent.utils.OidSingleHandler;


/**
 * 
 *
 */
public class StatusHandler {
  static Logger logger = Logger.getLogger(StatusHandler.class.getName());
  
  public static final OID OID_STATUS = new OID(".1.3.6.1.4.1.28747.1.12.1");

  private static final String UNKNOWN = "unknown";
  
  private DemonEnumIntStringSnmpTable<Status> table;
  
  private static enum Status {
    DEMON,NAME,STATUS,SIGNAL,VERSION,BUILD_DATE;
  }
  private Demon demon;
 
  public StatusHandler(Demon demon) {
    this.demon = demon;
    table = new DemonEnumIntStringSnmpTable<Status>(Status.class, OID_STATUS, demon.getIndex() );
    initialize();
  }
  
  private static class DemonNameLeaf implements EnumIntStringSnmpTable.Leaf {
    private Demon demon;
    private Integer demonIndex;
    public DemonNameLeaf(Demon demon) {
      this.demon = demon;
      this.demonIndex = Integer.valueOf( demon.getIndex() );
    }
    public Integer asInt() {
      return demonIndex;
    }
    public String asString() {
      return demon.getName();
    }
    public boolean canSet() {
      return false;
    } 
  }
  private static class DemonStatusLeaf implements EnumIntStringSnmpTable.Leaf {
    private Demon demon;
    public DemonStatusLeaf(Demon demon) {
      this.demon = demon;
    }
    public Integer asInt() {
      return demon.getStatus().toInt();
    }
    public String asString() {
      return demon.getStatus().toString();
    }
  }
  private static class DemonSignalLeaf implements EnumIntStringSnmpTable.WriteableLeaf {
    private Demon demon;
    private DemonSignal lastSignal;
    public DemonSignalLeaf(Demon demon) {
      this.demon = demon;
    }
    public Integer asInt() {
      return lastSignal == null ? -1 : lastSignal.toInt();
    }
    public String asString() {
      return lastSignal == null ? "no signal received" : "last received signal: " + lastSignal.name();
    }
    public boolean canSet() {
      return true;
    }
    public boolean fromInt(Integer value) {
      return setSignal( DemonSignal.fromInt(value) );
    }
    public boolean fromString(String value) {
      try {
        return setSignal( DemonSignal.valueOf( value.toUpperCase() ) );
      } catch( IllegalArgumentException e ) {
        //falschen String als Fehler zurück
        return false;
      }
    }
    private boolean setSignal(DemonSignal signal) {
      if( signal == null ) {
        return false;
      }
      lastSignal = signal;
      demon.setSignal( signal );
      return true;
    }
  }
  
  private void initialize() {
    String buildVersion = DemonProperties.getProperty(DemonProperties.BUILD_VERSION);
    String buildDate = DemonProperties.getProperty(DemonProperties.BUILD_DATE);
    table.addLeaf(Status.DEMON,      "demonIndex "+demon.getIndex(), Integer.valueOf(demon.getIndex() ) );   
    table.addLeaf(Status.NAME,       new DemonNameLeaf( demon ) );
    table.addLeaf(Status.STATUS,     new DemonStatusLeaf( demon ));
    table.addLeaf(Status.SIGNAL,     new DemonSignalLeaf( demon ));
    table.addLeaf(Status.VERSION,    "Version: "+buildVersion, getIntVersion(buildVersion) );
    table.addLeaf(Status.BUILD_DATE, "BuildDate: "+buildDate, getIntBuildDate(buildDate) );
  }

  /**
   * @param version 
   * @return Version als Integer
   */
  private Integer getIntVersion(String version) {
    if( version == null || version.length() == 0 || version.equals(UNKNOWN) ) {
      return Integer.valueOf(-1);
    }
    try {
      //erwartetes Format 0.2.2.32 -> 202032; 1.2.3.4 -> 1203004
      String[] parts = version.split("\\.");
      int v = 0;
      v += Integer.parseInt(parts[0]);
      v *= 10;
      v += Integer.parseInt(parts[1]);
      v *= 100;
      v += Integer.parseInt(parts[2]);
      v *= 1000;
      v += Integer.parseInt(parts[3]);
      return Integer.valueOf(v);
    } catch( Exception e ) {/*ignorieren*/}
    logger.info( "anormal version \""+version+"\" found");
    return stringToInt( version );
  }

  /**
   * @param buildDate 
   * @return BuildDate als Integer
   */
  private Integer getIntBuildDate(String buildDate) {
    if( buildDate == null || buildDate.equals(UNKNOWN) ) {
      return Integer.valueOf(-1);
    }
    if( buildDate.length() == 13 && buildDate.startsWith("20") ) {
      //normales buildDate, Format 20091110_1145
      String bd = buildDate.substring(2); //Substring, da ansonsten zu groß für Integer
      bd = bd.replaceAll("_",""); //Trenner zwischen Datum und Uhrzeit
      try {
        return Integer.valueOf( bd );
      } catch( NumberFormatException e ) {/*ignorieren*/}
    }
    logger.info( "anormal buildDate \""+buildDate+"\" found");
    return stringToInt( buildDate );
  }

  /**
   * @param string
   * @return
   */
  private Integer stringToInt(String string) {
    String s = string.replaceAll("[^0-9]*","");
    int len = s.length();
    if( len == 0 ) {
      return Integer.valueOf(-1);
    }
    if (len <= 10) {
      return Long.valueOf(s).intValue();
    }
    return Long.valueOf(s.substring(len - 10)).intValue();
  }
 
  /**
   * @return
   */
  public OidSingleHandler getOidSingleHandler() {
    return table;
  }
  
}
