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

import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryPortal;
import com.gip.xyna.utils.snmp.OID;
import com.gip.xyna.utils.snmp.agent.RequestHandler;
import com.gip.xyna.utils.snmp.exception.SnmpRequestHandlerException;
import com.gip.xyna.utils.snmp.varbind.IntegerVarBind;
import com.gip.xyna.utils.snmp.varbind.OIDVarBind;
import com.gip.xyna.utils.snmp.varbind.StringVarBind;
import com.gip.xyna.utils.snmp.varbind.UnsIntegerVarBind;
import com.gip.xyna.utils.snmp.varbind.VarBind;
import com.gip.xyna.xfmg.statistics.XynaStatisticsLegacy;
import com.gip.xyna.xfmg.statistics.XynaStatisticsLegacy.SNMPVarTypeLegacy;
import com.gip.xyna.xfmg.statistics.XynaStatisticsLegacy.StatisticsReportEntryLegacy;


@Deprecated
public class SNMPStatisticsGenericHandler extends AbstractSNMPStatisticsHandler {

  private final static XynaStatisticsLegacy xynaStatistics = XynaFactory.getInstance().getFactoryManagement().getXynaStatisticsLegacy();
  
  public SNMPStatisticsGenericHandler(AbstractSNMPStatisticsHandler nextHandler) {
    super(nextHandler);
    
    SortedMap<OID, String> perMap = OIDManagement.getInstance().getMapForScope(base);
    Map<String, StatisticsReportEntryLegacy[]> stats = xynaStatistics.getStatisticsReadOnly();
    
    mapStringInteger.clear();
    mapIntegerString.clear();

    SortedSet<OID> oidWalk = new TreeSet<OID>();
    oidWalk.clear();

    for (Entry<OID, String> oe : perMap.entrySet()) {
      int row = oe.getKey().getIntIndex(oe.getKey().length() - 1);
      String s = oe.getValue();
      StatisticsReportEntryLegacy[] value = stats.get(s);

      if (value != null) {
        mapStringInteger.put(s, row);
        mapIntegerString.put(row, s);

        oidWalk.add(base.append(new OID("1")).append(OID.encodeFromString(s)));
        oidWalk.add(base.append(new OID("2." + row)));

        for (int v = 1; v <= value.length; v++) {
          oidWalk.add(base.append(new OID("2")).append(row, v));
          oidWalk.add(base.append(new OID("2")).append(row, v, 1));
        }
      }
    }

    setOIDWalk(oidWalk);

  }


  private static final Logger logger = CentralFactoryLogging.getLogger(SNMPStatisticsHandler.class);

  private static final OID base = new OID("1.3.6.1.4.1.28747.1.11.4.4"); // TODO aus konstanten zusammenbauen -
  // konstanten f�r departments etc anlegen.

  private final static XynaFactoryPortal factory = XynaFactory.getPortalInstance();
  
  // TODO: es wird nicht ber�cksichtigt, dass sich die anzahl der werte in dem array der statistik �ndern kann
  @Override
  protected void updateMap(int i) {
    Map<String, StatisticsReportEntryLegacy[]> stats = xynaStatistics.getStatisticsReadOnly();
    
    if ( previousMapCount != stats.size() ) {
      OIDManagement oidMan = OIDManagement.getInstance();
      
      synchronized (oidWalk) {
        for (Entry<String, StatisticsReportEntryLegacy[]> e : stats.entrySet()) {
          String s = e.getKey();
          StatisticsReportEntryLegacy[] value = e.getValue();

          if (!mapStringInteger.containsKey(s)) {
            OID oidEntry = oidMan.getOidForName(base.append(2), s);
            int row = oidEntry.getIntIndex(oidEntry.length() - 1);

            mapStringInteger.put(s, row);
            mapIntegerString.put(row, s);

            oidWalk.add(base.append(new OID("1")).append(OID.encodeFromString(s)));
            oidWalk.add(base.append(new OID("2." + row)));

            for (int v = 1; v <= value.length; v++) {
              oidWalk.add(base.append(new OID("2")).append(row, v));
              oidWalk.add(base.append(new OID("2")).append(row, v, 1));
            }
          }
        }
      }

      previousMapCount = stats.size();
    }
  }
  
  
  @Override
  public VarBind get(OID oid, int i) {
    SNMPVarTypeLegacy valueType = SNMPVarTypeLegacy.UNDEFINED;
    long statValueLong = 0;
    int statValueInt = 0;
    OID statValueOID = null;
    String statValueString = null;
    String logString = null;

    updateMap(i);
    
    if (oid.startsWith(base)) {
      OID post = oid.subOid(base.length());

      if (post.length() > 0) {
        OID statisticsName = post.subOid(1);
        String statisticsNameString = null;
        
        switch (post.getIntIndex(0)) {
          case 1 : //listing
            statisticsNameString = statisticsName.decodeToString();
            logString = statisticsNameString;
            
            if ( mapStringInteger.containsKey(statisticsNameString) ) {
              Integer idx = mapStringInteger.get(statisticsNameString);
              
              if (idx != null) {
                statValueOID = base.append(2, idx);
                valueType = SNMPVarTypeLegacy.OBJECT_IDENTIFIER;
              }
            }
            
            break;
            
          case 2 :  //values
            if (post.length() > 1) {
              int postIndex1 = post.getIntIndex(1);
              
              if (mapIntegerString.containsKey(postIndex1)) {
                statValueString = mapIntegerString.get(postIndex1);
                statisticsNameString = statValueString;
                logString = statisticsNameString;
                valueType = SNMPVarTypeLegacy.OCTET_STRING;
              }
              else {
                valueType = SNMPVarTypeLegacy.UNDEFINED;
                break;
              }
            }
            
            StatisticsReportEntryLegacy[] value = xynaStatistics.getStatistics(statisticsNameString);
            
            if (post.length() > 2) {
              int postIndex2 = post.getIntIndex(2);
              
              if (postIndex2 <= value.length) {
                StatisticsReportEntryLegacy entry = value[postIndex2-1];
                statValueString = entry.getDescription();
                logString = logString.concat(" keyName " + (postIndex2-1));
                valueType = SNMPVarTypeLegacy.OCTET_STRING;
              } else {
                valueType = SNMPVarTypeLegacy.UNDEFINED;
                break;
              }
            }
            
            if (post.length() > 3) {              
              if (post.getIntIndex(3) == 1) {
                int postIndex2 = post.getIntIndex(2);

                if (postIndex2 <= value.length) {
                  StatisticsReportEntryLegacy entry = value[postIndex2-1];
                  
                  switch (entry.getType()) {
                    case INTEGER:
                      statValueInt = (Integer)entry.getValue();
                      valueType = SNMPVarTypeLegacy.INTEGER;
                      break;
                      
                    case UNSIGNED_INTEGER:
                      statValueLong = (Long)entry.getValue();
                      valueType = SNMPVarTypeLegacy.UNSIGNED_INTEGER;
                      break;
                      
                    case OCTET_STRING:
                      statValueString = (String)entry.getValue();
                      valueType = SNMPVarTypeLegacy.OCTET_STRING;
                      break;
                      
                    case OBJECT_IDENTIFIER:
                      statValueOID = (OID)entry.getValue();
                      valueType = SNMPVarTypeLegacy.OBJECT_IDENTIFIER;
                      break;
                      
                    case NULL:
                      break;
                      
                    default:
                      valueType = SNMPVarTypeLegacy.UNDEFINED;
                      break;
                  }
                  
                  logString = logString.concat(" value " + (postIndex2-1));
                }
              }
              else {
                valueType = SNMPVarTypeLegacy.UNDEFINED;
                break;
              }
            }
            
            break;
            
          default:
            valueType = SNMPVarTypeLegacy.UNDEFINED;
            break;
        }
      }
    }

    switch ( valueType ) {
      case INTEGER:
        logger.debug("got value " + statValueInt + " for key " + logString);
        return new IntegerVarBind(oid.toString(), statValueInt);
      
      case UNSIGNED_INTEGER:
        logger.debug("got value " + statValueLong + " for key " + logString);
        return new UnsIntegerVarBind(oid.toString(), statValueLong);

      case OCTET_STRING:
        logger.debug("got value " + statValueString + " for key " + logString);
        return new StringVarBind(oid.toString(), statValueString);

      case OBJECT_IDENTIFIER:
        logger.debug("got value " + statValueOID + " for key " + logString);
        return new OIDVarBind(oid.toString(), statValueOID.toString());

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
