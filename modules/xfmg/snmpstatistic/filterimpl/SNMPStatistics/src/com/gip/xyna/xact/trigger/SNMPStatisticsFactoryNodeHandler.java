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

package com.gip.xyna.xact.trigger;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

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
import com.gip.xyna.xfmg.xfctrl.nodemgmt.FactoryNodeInformation;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.FactoryNodeCaller.FactoryNodeCallerStatus;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.NodeManagement;

@Deprecated
public class SNMPStatisticsFactoryNodeHandler  extends AbstractSNMPStatisticsHandler {

  public SNMPStatisticsFactoryNodeHandler(AbstractSNMPStatisticsHandler nextHandler) {
    super(nextHandler);
  }
  
  private static final Logger logger = CentralFactoryLogging.getLogger(SNMPStatisticsHandler.class);

  private static final OID base = new OID("1.3.6.1.4.1.28747.1.11.4.5"); // TODO aus konstanten zusammenbauen -
  // konstanten für departments etc anlegen.


  private static final NodeManagement nodeMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getNodeManagement();
  private List<String> nodeNames = new ArrayList<>();
  
  @Override
  protected void updateMap(int j) {
    List<FactoryNodeInformation> nodes = nodeMgmt.listFactoryNodes(false);
    
    if (nodes == null) return;
    
    SortedSet<String> currentNodeNames = new TreeSet<String>();
    for (FactoryNodeInformation fni : nodes) {
        currentNodeNames.add(fni.getName());
    }

    synchronized (nodeNames) {
        currentNodeNames.addAll(nodeNames);
    
        if (nodeNames.size() != currentNodeNames.size()) {
            nodeNames = new ArrayList<String>(currentNodeNames);
            if (logger.isDebugEnabled()) logger.debug("updating map for nodenames: " + nodeNames.size());
            
            SortedSet<OID> oidWalkNew = new TreeSet<OID>();
            //oidWalkNew.add(base.append(new OID("1"))); // index
            for (int i = 1; i <= nodeNames.size(); ++i) {
                oidWalkNew.add(base.append(new OID("1." +  String.valueOf(i) )));
            }
            //oidWalkNew.add(base.append(new OID("2"))); // names
            for (int i = 1; i <= nodeNames.size(); ++i) {
                oidWalkNew.add(base.append(new OID("2." +  String.valueOf(i) )));
            }
            //oidWalkNew.add(base.append(new OID("3"))); // instanceId
            for (int i = 1; i <= nodeNames.size(); ++i) {
                oidWalkNew.add(base.append(new OID("3." +  String.valueOf(i) )));
            }
            //oidWalkNew.add(base.append(new OID("4"))); // description
            for (int i = 1; i <= nodeNames.size(); ++i) {
                oidWalkNew.add(base.append(new OID("4." +  String.valueOf(i) )));
            }
            //oidWalkNew.add(base.append(new OID("5"))); // remoteAccessType
            for (int i = 1; i <= nodeNames.size(); ++i) {
                oidWalkNew.add(base.append(new OID("5." +  String.valueOf(i) )));
            }
            //oidWalkNew.add(base.append(new OID("6"))); // waitingForConnection
            for (int i = 1; i <= nodeNames.size(); ++i) {
                oidWalkNew.add(base.append(new OID("6." +  String.valueOf(i) )));
            }
            //oidWalkNew.add(base.append(new OID("7"))); // waitingForResult
            for (int i = 1; i <= nodeNames.size(); ++i) {
                oidWalkNew.add(base.append(new OID("7." +  String.valueOf(i) )));
            }
            //oidWalkNew.add(base.append(new OID("8"))); // status
            for (int i = 1; i <= nodeNames.size(); ++i) {
                oidWalkNew.add(base.append(new OID("8." +  String.valueOf(i) )));
            }
            setOIDWalk(oidWalkNew);
        }
    }
    
  }


  @Override
  public VarBind get(OID oid, int i) {
 
    SNMPVarTypeLegacy valueType = SNMPVarTypeLegacy.UNDEFINED;
    long statValueLong = 0;
    String statValueString = null;
    String logString = null;
    FactoryNodeInformation fni = null;
    
    if (logger.isDebugEnabled()) logger.debug("get oid: " + oid + " i: " + i);

    if (oid.startsWith(base)) {
      OID post = oid.subOid(base.length());

      if (post.length() == 1) {
         valueType = SNMPVarTypeLegacy.UNDEFINED;
      }
      
      if ( (post.length() == 2) ) {
        
        int idx = post.getIntIndex(1);

        synchronized (nodeNames) {
            if (idx <= 0 || idx > nodeNames.size()) {
                throw new SnmpRequestHandlerException(RequestHandler.NO_SUCH_NAME, i);
            }
        }
        
        String nodeName;
        synchronized (nodeNames) {
            nodeName = nodeNames.get(idx-1);
        }
        switch ( post.getIntIndex(0) ) {
          case 1: //index
            logString = "index";
            statValueLong = idx;
            valueType = SNMPVarTypeLegacy.UNSIGNED_INTEGER;
            break;
            
          case 2: // name
            statValueString = nodeName;
            logString = "name";
            valueType = SNMPVarTypeLegacy.OCTET_STRING;
            break;
            
          case 3: // instanceId
            fni = nodeMgmt.getFactoryNodeInformationByName(nodeName, false);
            statValueLong = fni != null ? fni.getInstanceId() : 0;
            logString = "instanceId";
            valueType = SNMPVarTypeLegacy.UNSIGNED_INTEGER;
            break;
          
          case 4: // description
            fni = nodeMgmt.getFactoryNodeInformationByName(nodeName, false);
            statValueString = fni != null ? fni.getDescription() : "";
            logString = "description";
            valueType = SNMPVarTypeLegacy.OCTET_STRING;
            break;

          case 5: // remoteAccessType
            fni = nodeMgmt.getFactoryNodeInformationByName(nodeName, false);
            statValueString = fni != null ? fni.getRemoteAccessType() : "";
            logString = "remoteAccessType";
            valueType = SNMPVarTypeLegacy.OCTET_STRING;
            break;

          case 6: // waitingForConnection
            fni = nodeMgmt.getFactoryNodeInformationByName(nodeName, true);
            statValueLong = fni != null ? fni.getWaitingForConnectivity() : 0;
            logString = "waitingForConnection";
            valueType = SNMPVarTypeLegacy.UNSIGNED_INTEGER;
            break;

          case 7: // waitingForResult
            fni = nodeMgmt.getFactoryNodeInformationByName(nodeName, true);
            statValueLong = fni != null ? fni.getWaitingForResult() : 0;
            logString = "waitingForResult";
            valueType = SNMPVarTypeLegacy.UNSIGNED_INTEGER;
            break;

          case 8: // status
            fni = nodeMgmt.getFactoryNodeInformationByName(nodeName, true);
            FactoryNodeCallerStatus status = fni != null ? fni.getStatus() : FactoryNodeCallerStatus.Unused;
            switch (status) {
                case Unused:
                    statValueLong = 0;
                    break;
                case Connecting:
                    statValueLong = 1;
                    break;
                case Idle:
                    statValueLong = 2;
                    break;
                case Connected:
                    statValueLong = 3;
                    break;
                default:
                    statValueLong = 4;
            }
            logString = "status";
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
        if (logger.isDebugEnabled()) logger.debug("got value " + statValueLong + " for key " + logString);
        return new UnsIntegerVarBind(oid.toString(), statValueLong);

      case OCTET_STRING:
        if (logger.isDebugEnabled()) logger.debug("got value " + statValueString + " for key " + logString);
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
