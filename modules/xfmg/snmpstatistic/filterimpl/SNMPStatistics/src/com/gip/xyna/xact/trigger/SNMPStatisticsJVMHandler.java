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

import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.snmp.OID;
import com.gip.xyna.utils.snmp.agent.RequestHandler;
import com.gip.xyna.utils.snmp.exception.SnmpRequestHandlerException;
import com.gip.xyna.utils.snmp.varbind.StringVarBind;
import com.gip.xyna.utils.snmp.varbind.UnsIntegerVarBind;
import com.gip.xyna.utils.snmp.varbind.VarBind;
import com.gip.xyna.xfmg.statistics.XynaStatisticsLegacy.SNMPVarTypeLegacy;


@Deprecated
public class SNMPStatisticsJVMHandler  extends AbstractSNMPStatisticsHandler {

  public SNMPStatisticsJVMHandler(AbstractSNMPStatisticsHandler nextHandler) {
    super(nextHandler);
  }


  private static final Logger logger = CentralFactoryLogging.getLogger(SNMPStatisticsHandler.class);

  private static final OID base = new OID("1.3.6.1.4.1.28747.1.11.4.2"); // TODO aus konstanten zusammenbauen -
  // konstanten f�r departments etc anlegen.

  private final static Runtime runtime = Runtime.getRuntime();
  
  @Override
  protected void updateMap(int i) {
    SortedSet<OID> oidWalkNew = new TreeSet<OID>();
    if (oidWalk.size() < 4) {
      oidWalkNew.add(base.append(new OID("1"))); // memoryUsed
      oidWalkNew.add(base.append(new OID("1.1"))); // memoryUsed
      oidWalkNew.add(base.append(new OID("2"))); // memoryFree
      oidWalkNew.add(base.append(new OID("2.1"))); // memoryFree
      oidWalkNew.add(base.append(new OID("3"))); // upTime
      oidWalkNew.add(base.append(new OID("3.1"))); // upTime
      setOIDWalk(oidWalkNew);
    }
  }


  @Override
  public VarBind get(OID oid, int i) {
    SNMPVarTypeLegacy valueType = SNMPVarTypeLegacy.UNDEFINED;
    long statValueLong = 0;
    String statValueString = null;
    String logString = null;

    if (oid.startsWith(base)) {
      OID post = oid.subOid(base.length());

      if (post.length() == 1) {
        switch ( post.getIntIndex(0) ) {
          case 1:
            statValueString = "JVM memory used";
            logString = "memory used in kB";
            valueType = SNMPVarTypeLegacy.OCTET_STRING;
            break;
            
          case 2:
            statValueString = "JVM memory free";
            logString = "memory free in kB";
            valueType = SNMPVarTypeLegacy.OCTET_STRING;
            break;
            
          case 3:
            statValueString = "up time in seconds";
            logString = "upTime";
            valueType = SNMPVarTypeLegacy.OCTET_STRING;
            break;
          
          default:
            valueType = SNMPVarTypeLegacy.UNDEFINED;
            break;
        }
      }
      
      if ( (post.length() == 2) && (post.getIntIndex(1) == 1) ) {
        long freeMem = 0;
        
        switch ( post.getIntIndex(0) ) {
          case 1:
            long totalMem = runtime.totalMemory();
            freeMem = runtime.freeMemory();
            long usedMem = totalMem - freeMem;
            statValueLong = usedMem / 1024;
            logString = "memory used";
            valueType = SNMPVarTypeLegacy.UNSIGNED_INTEGER;
            break;
            
          case 2:
            freeMem = runtime.freeMemory();
            statValueLong = freeMem / 1024;
            logString = "memory free";
            valueType = SNMPVarTypeLegacy.UNSIGNED_INTEGER;
            break;
            
          case 3:
            statValueLong = XynaFactory.getInstance().getFactoryManagement().getXynaStatisticsLegacy().getUptime() / 1000;
            logString = "up time";
            valueType = SNMPVarTypeLegacy.UNSIGNED_INTEGER;
            break;
          
          default:
            valueType = SNMPVarTypeLegacy.UNDEFINED;
            break;
        }
      }
    }
    
    switch ( valueType ) {
      case UNSIGNED_INTEGER:
        logger.debug("got value " + statValueLong + " for key " + logString);
        return new UnsIntegerVarBind(oid.toString(), statValueLong);

      case OCTET_STRING:
        logger.debug("got value " + statValueString + " for key " + logString);
        return new StringVarBind(oid.toString(), statValueString);

      default:
        // property konnte nicht ausgelesen werden nicht
        throw new SnmpRequestHandlerException(RequestHandler.NO_SUCH_NAME, i);
    }
  }


  @Override
  protected OID getBase() {
    return base;
  }
}
