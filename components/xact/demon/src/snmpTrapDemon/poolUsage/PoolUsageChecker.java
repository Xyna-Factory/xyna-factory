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
package snmpTrapDemon.poolUsage;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.gip.xyna.demon.DemonProperties;
import com.gip.xyna.utils.snmp.manager.SnmpContext;
import com.gip.xyna.utils.snmp.varbind.IntegerVarBind;
import com.gip.xyna.utils.snmp.varbind.StringVarBind;
import com.gip.xyna.utils.snmp.varbind.VarBindList;

/**
 * PoolUsageChecker prüft, ob die Schwellwerte eingehalten werden.
 * Falls dies nicht der Fall ist, wird eine Warnmeldung im Log ausgegeben 
 * und eine Trap verschickt.
 */
public class PoolUsageChecker {
  private static final String OID_poolUsage = ".1.3.6.1.4.1.28747.1.13.3.1";
  private static final String OID_poolUsageEntry = OID_poolUsage + ".1.1";
  private static final String OID_poolUsageCmts = OID_poolUsageEntry+".2";
  private static final String OID_poolUsagePoolType = OID_poolUsageEntry+".3";
  //private static final String OID_poolUsageSize = OID_poolUsageEntry+".4";
  //private static final String OID_poolUsageUsedSize = OID_poolUsageEntry+".5";
  private static final String OID_poolUsageUsage = OID_poolUsageEntry+".6";
  private static final String OID_poolUsageThreshold = OID_poolUsageEntry+".7";
  private static final String OID_poolUsageTrap = OID_poolUsage+".2";
  
  static Logger logger = Logger.getLogger(PoolUsageChecker.class.getName());

  private long startTime;
  private PoolUsageTable poolUsageTable;
  private PoolUsageThreshold poolUsageThreshold;
  private AtomicInteger trapCounter;
  private int numTraps;
  private SnmpContext trapsender;

  /**
   * Konstruktor
   * @param poolUsageTable
   * @param trapsender
   * @param poolUsageThreshold
   * @param trapCounter
   */
  public PoolUsageChecker(PoolUsageTable poolUsageTable, SnmpContext trapsender, PoolUsageThreshold poolUsageThreshold, AtomicInteger trapCounter) {
    this.startTime = DemonProperties.getLongProperty( "start.time" );
    this.trapsender = trapsender;
    this.poolUsageTable = poolUsageTable;
    this.poolUsageThreshold = poolUsageThreshold;
    this.trapCounter = trapCounter;
  }
 
  /**
   * Verschicken der Trap
   */
  private void sendTrap(String cmts, String poolType, float usedFraction, float threshold ) {
    trapCounter.incrementAndGet();
    ++numTraps;
    long upTime = System.currentTimeMillis()-startTime;
    VarBindList vbl = new VarBindList();
    vbl.add( new StringVarBind( OID_poolUsageCmts+".0", cmts ) );
    vbl.add( new StringVarBind( OID_poolUsagePoolType+".0", poolType ) );
    vbl.add( new IntegerVarBind( OID_poolUsageUsage+".0",  (int)(usedFraction*1000) ) );
    vbl.add( new IntegerVarBind( OID_poolUsageThreshold+".0", (int)(threshold*1000) ) );
    
    trapsender.trap( OID_poolUsageTrap, upTime, vbl, "PoolUsage "+usedFraction+" for "+cmts+" and "+poolType );
  }
 
  /**
   * Prüfung, ob Schwellenwerte überschritten sind
   */
  public int checkThreshold() {
    logger.debug( "checkThreshold" );
    numTraps = 0;
    
    for( PoolUsageTable.Entry pue : poolUsageTable.getEntries() ) {
      if( pue.getPoolID() >= 100000 ) { //poolID=-1: Nur Summen betrachten
        if( pue.getUsedFraction() > poolUsageThreshold.getLowestThreshold() ) {
          check( pue );
        }
      }
    }
    return numTraps;
  }
  
  /**
   * Prüfung der Schwellenwerte
   * Ausgabe als Warnung ins Log, Verschicken einer Trap
   */
  public void check(PoolUsageTable.Entry poolUsageEntry) {
    String sharedNetworkID = poolUsageEntry.getSharedNetworkID();
    String poolTypeID = poolUsageEntry.getPoolTypeID();
    float usedFraction = poolUsageEntry.getUsedFraction();
    
    float threshold = poolUsageThreshold.getThreshold(sharedNetworkID,poolTypeID);

    if( usedFraction > threshold ) {
      String sharedNetwork = poolUsageTable.getSharedNetwork(sharedNetworkID);
      String poolType = poolUsageTable.getPoolType(poolTypeID);
      
      logger.warn( "PoolUsage "+usedFraction+" for "+sharedNetwork+" and "+poolType+" above threshold "+threshold );
      sendTrap(sharedNetwork,poolType,usedFraction,threshold);
    }
  }

}
