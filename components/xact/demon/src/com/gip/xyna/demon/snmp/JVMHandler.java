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

import com.gip.xyna.demon.DemonProperties;
import com.gip.xyna.utils.snmp.OID;
import com.gip.xyna.utils.snmp.agent.utils.EnumIntStringSnmpTable;
import com.gip.xyna.utils.snmp.agent.utils.OidSingleHandler;

public class JVMHandler {
  
  public static final OID OID_JVM = new OID(".1.3.6.1.4.1.28747.1.12.2");

  private EnumIntStringSnmpTable<Jvm> table;
  
  private static enum Jvm {
    DEMON,NAME,THREADS,UPTIME,MEMORY;
  }
  private String demonIndex;
  
  public JVMHandler(String demonIndex) {
    this.demonIndex = demonIndex;
    table = new DemonEnumIntStringSnmpTable<Jvm>(Jvm.class, OID_JVM, demonIndex );
    initialize();
  }
    
  private static class JvmThreadsLeaf implements EnumIntStringSnmpTable.Leaf {
    public Integer asInt() {
      return Thread.activeCount();
    }
    public String asString() {
      return "active threads "+Thread.activeCount();
    }
  }
  private static class JvmUptimeLeaf implements EnumIntStringSnmpTable.Leaf {
    private long startTime;
    public JvmUptimeLeaf(long startTime) {
      this.startTime = startTime;
    }
    public Integer asInt() {
      return (int)((System.currentTimeMillis()-startTime)/10); //TimeTicks: 100stel Sekunden
    }
    public String asString() {
      return "upTime in cs "+asInt();
    }
  }
  private static class JvmMemoryLeaf implements EnumIntStringSnmpTable.Leaf {
    private long lastGC;
    private int memory;
    public Integer asInt() {
      //GarbageCollection begrenzen, auf alle 5 Sekunden einmal
      long now = System.currentTimeMillis();
      if( now - lastGC > 5000 ) {
        Runtime.getRuntime().gc(); //GarbageCollection ist n�tig, damit ein 
        //sinnvoller Wert f�r den ben�tigten Speicher berechnet werden kann
        memory = (int)(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
        lastGC = now;
      }
      return memory; 
    }
    public String asString() {
      return "used memory in bytes "+asInt();
    }
  }
  
  
  private void initialize() {
    String javaVersion = System.getProperties().getProperty("java.version");
    table.addLeaf(Jvm.DEMON,   "demonIndex "+demonIndex,  Integer.valueOf(demonIndex) );   
    table.addLeaf(Jvm.NAME,    "JVM "+javaVersion,        stringToInt(javaVersion) );
    table.addLeaf(Jvm.THREADS, new JvmThreadsLeaf() );
    table.addLeaf(Jvm.UPTIME,  new JvmUptimeLeaf( DemonProperties.getLongProperty( "start.time" ) ) );
    table.addLeaf(Jvm.MEMORY,  new JvmMemoryLeaf() );
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
    if( len <= 10 ) {
      return Integer.valueOf(s); 
    }
    return Integer.valueOf( s.substring(len-10) ); 
  }
  
  /**
   * @return
   */
  public OidSingleHandler getOidSingleHandler() {
    return table;
  }

}
