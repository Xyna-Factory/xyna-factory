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
import java.util.Set;
import java.util.SortedMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryPortal;
import com.gip.xyna.utils.snmp.OID;
import com.gip.xyna.utils.snmp.agent.RequestHandler;
import com.gip.xyna.utils.snmp.exception.SnmpRequestHandlerException;
import com.gip.xyna.utils.snmp.varbind.OIDVarBind;
import com.gip.xyna.utils.snmp.varbind.StringVarBind;
import com.gip.xyna.utils.snmp.varbind.UnsIntegerVarBind;
import com.gip.xyna.utils.snmp.varbind.VarBind;
import com.gip.xyna.xfmg.statistics.XynaStatisticsLegacy.SNMPVarTypeLegacy;
import com.gip.xyna.xmcp.Channel;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchiveStatisticsStorable;


@Deprecated
public class SNMPStatisticsOrdersHandler extends AbstractSNMPStatisticsHandler {

  private static final Logger logger = CentralFactoryLogging.getLogger(SNMPStatisticsHandler.class);

  private static final OID base = new OID("1.3.6.1.4.1.28747.1.11.4.3"); // TODO aus konstanten zusammenbauen -
                                                                         //      konstanten f�r departments etc anlegen.

  private final static XynaFactoryPortal factory = XynaFactory.getPortalInstance();


  public SNMPStatisticsOrdersHandler(AbstractSNMPStatisticsHandler nextHandler) {
    super(nextHandler);

    // TODO : Fill table of orderstatistics with empty entries for OIDs found in OIDmapping
    SortedMap<OID, String> perMap = OIDManagement.getInstance().getMapForScope(base);
    
    mapStringInteger.clear();
    mapIntegerString.clear();
    
    synchronized (oidWalk) {
      oidWalk.clear();

      for (Entry<OID, String> e : perMap.entrySet()) {
        int row = e.getKey().getIntIndex(e.getKey().length() - 1);
        String s = e.getValue();

        mapStringInteger.put(s, row);
        mapIntegerString.put(row, s);
        oidWalk.add(base.append(new OID("1")).append(OID.encodeFromString(s)));
        oidWalk.add(base.append(new OID("2." + row)));
        oidWalk.add(base.append(new OID("2." + row + ".1")));
        oidWalk.add(base.append(new OID("2." + row + ".1.1")));
        oidWalk.add(base.append(new OID("2." + row + ".2")));
        oidWalk.add(base.append(new OID("2." + row + ".2.1")));
        oidWalk.add(base.append(new OID("2." + row + ".3")));
        oidWalk.add(base.append(new OID("2." + row + ".3.1")));
        oidWalk.add(base.append(new OID("2." + row + ".4")));
        oidWalk.add(base.append(new OID("2." + row + ".4.1")));
      }
    }
  }


  // TODO: Ausgabe in einer Art von Tabelle
  @Override
  protected void updateMap(int i) {
    Channel mcp = factory.getXynaMultiChannelPortalPortal();
    
    // if the call statistics count has increased, then add those new order types to our maps
    Map<String, OrderArchiveStatisticsStorable> statsMap = mcp.getCompleteCallStatistics();
    if (previousMapCount != statsMap.size()) {
      OIDManagement oidMan = OIDManagement.getInstance();

      Set<String> calls = statsMap.keySet();

      synchronized (oidWalk) {
        for (String s : calls) {
          if (!mapStringInteger.containsKey(s)) {
            OID oidEntry = oidMan.getOidForName(base.append(2), s);
            int row = oidEntry.getIntIndex(oidEntry.length() - 1);

            mapStringInteger.put(s, row);
            mapIntegerString.put(row, s);
            oidWalk.add(base.append(new OID("1")).append(OID.encodeFromString(s)));
            oidWalk.add(base.append(new OID("2." + row)));
            oidWalk.add(base.append(new OID("2." + row + ".1")));
            oidWalk.add(base.append(new OID("2." + row + ".1.1")));
            oidWalk.add(base.append(new OID("2." + row + ".2")));
            oidWalk.add(base.append(new OID("2." + row + ".2.1")));
            oidWalk.add(base.append(new OID("2." + row + ".3")));
            oidWalk.add(base.append(new OID("2." + row + ".3.1")));
            oidWalk.add(base.append(new OID("2." + row + ".4")));
            oidWalk.add(base.append(new OID("2." + row + ".4.1")));
          }
        }
      }

      previousMapCount = calls.size();
    }
  }


  @Override
  public VarBind get(OID oid, int i) {

    SNMPVarTypeLegacy valueType = SNMPVarTypeLegacy.UNDEFINED;
    long statValueLong = 0;
    OID statValueOID = null;
    String statValueString = null;
    String logString = null;
    String orderTypeName = null;

    if (oid.startsWith(base)) {

      // update the strings so the mapping gets updated
      updateMap(i);

      OID post = oid.subOid(base.length());

      if (post.length() > 0) {
        switch (post.getIntIndex(0)) {
          case 1 : //listing
            OID orderType = post.subOid(1);
            orderTypeName = orderType.decodeToString();
            logString = orderTypeName;

            if (mapStringInteger.containsKey(orderTypeName)) {
              Integer idx = mapStringInteger.get(orderTypeName);

              if (idx != null) {
                statValueOID = base.append(2, idx);
                valueType = SNMPVarTypeLegacy.OBJECT_IDENTIFIER;
              }
            }

            break;

          case 2 : //values
            if (post.length() > 1) {
              if (mapIntegerString.containsKey(post.getIntIndex(1))) {
                statValueString = mapIntegerString.get(post.getIntIndex(1));
                orderTypeName = statValueString;
                logString = statValueString;
                valueType = SNMPVarTypeLegacy.OCTET_STRING;
              } else {
                valueType = SNMPVarTypeLegacy.UNDEFINED;
                break;
              }
            }

            if (post.length() > 2) {
              switch (post.getIntIndex(2)) {
                case 1 :
                  statValueString = "total orders";
                  logString = logString.concat(" total");
                  valueType = SNMPVarTypeLegacy.OCTET_STRING;
                  break;

                case 2 :
                  statValueString = "successful executed orders";
                  logString = logString.concat(" win");
                  valueType = SNMPVarTypeLegacy.OCTET_STRING;
                  break;

                case 3 :
                  statValueString = "orders timed out";
                  logString = logString.concat(" timed out");
                  valueType = SNMPVarTypeLegacy.OCTET_STRING;
                  break;

                case 4 :
                  statValueString = "errors";
                  logString = logString.concat(" errors");
                  valueType = SNMPVarTypeLegacy.OCTET_STRING;
                  break;

                default :
                  valueType = SNMPVarTypeLegacy.UNDEFINED;
                  break;
              }
            }

            if (post.length() > 3) {
              if (post.getIntIndex(3) == 1) {
                Channel mcp = factory.getXynaMultiChannelPortalPortal();
                Map<String, OrderArchiveStatisticsStorable> stats = mcp.getCompleteCallStatistics();
                OrderArchiveStatisticsStorable stat = stats.get(orderTypeName);
                Long x;

                statValueLong = 0;
                valueType = SNMPVarTypeLegacy.UNSIGNED_INTEGER;

                if (stat != null) {
                  switch (post.getIntIndex(2)) {
                    case 1 :
                      x = stat.getCalls();
                      break;

                    case 2 :
                      x = stat.getFinished();
                      break;

                    case 3 :
                      x = stat.getTimeouts();
                      break;

                    case 4 :
                      x = stat.getErrors();
                      break;

                    default :
                      x = 0L;
                      valueType = SNMPVarTypeLegacy.UNDEFINED;
                      break;
                  }
                } else {
                  x = 0L;
                }

                if (x != null) {
                  statValueLong = x;
                }
              }
            }

            break;

          default :
            valueType = SNMPVarTypeLegacy.UNDEFINED;
            break;
        }
      }
    }

    switch (valueType) {
      case UNSIGNED_INTEGER :
        logger.debug("got value " + statValueLong + " for key " + logString);
        return new UnsIntegerVarBind(oid.toString(), statValueLong);

      case OCTET_STRING :
        logger.debug("got value " + statValueString + " for key " + logString);
        return new StringVarBind(oid.toString(), statValueString);

      case OBJECT_IDENTIFIER :
        logger.debug("got value " + statValueOID + " for key " + logString);
        return new OIDVarBind(oid.toString(), statValueOID.toString());

      default :
        // property konnte nicht ausgelesen werden nicht
        throw new SnmpRequestHandlerException(RequestHandler.NO_SUCH_NAME, i);
    }

  }


  @Override
  protected OID getBase() {
    return base;
  }

}
