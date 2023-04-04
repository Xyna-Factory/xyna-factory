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
package snmpAdapterDemon.snmp;

import snmpAdapterDemon.ConfigDataSender;
import snmpAdapterDemon.SnmpAgent;

import com.gip.xyna.demon.worker.SlavePool.CounterData;
import com.gip.xyna.utils.snmp.OID;
import com.gip.xyna.utils.snmp.agent.utils.EnumIntStringSnmpTable;
import com.gip.xyna.utils.snmp.agent.utils.OidSingleHandler;


/**
 * Snmp-Statistik-Ausgabe des SnmpAdapters
 *
 */
public class SnmpGeneral {
  
  public static final OID OID_GENERAL = new OID(".1.3.6.1.4.1.28747.1.13.2.1");
  
  private EnumIntStringSnmpTable<General> table;
  
  private static enum General {
    STATUS_LISTENER, REJECTED, REQUESTED, SENT, SENT_FAILED, SENT_REBUILD;
  }
  
  public SnmpGeneral() {
    table = new EnumIntStringSnmpTable<General>(General.class, OID_GENERAL );
  }
  
  private static class StatusLeaf implements EnumIntStringSnmpTable.Leaf {
    private SnmpAgent snmpAgent;
    /**
     * @param snmpAgent
     */
    public StatusLeaf(SnmpAgent snmpAgent) {
      this.snmpAgent = snmpAgent;
    }
    
    public Integer asInt() {
      return snmpAgent.getStatus().toInt();
    }
    public String asString() {
      return "listener status: "+snmpAgent.getStatus().toString();
    }
  }
  
  public void initialize(SnmpAgent snmpAgent, ConfigDataSender configDataSender ) {
    table.addLeaf( General.STATUS_LISTENER, new StatusLeaf(snmpAgent) );
    table.addLeaf( General.REJECTED, "rejected", snmpAgent.getSlaveCounter(CounterData.REJECTED) );
    table.addLeaf( General.REQUESTED, "requested", snmpAgent.getSlaveCounter(CounterData.REQUESTED) );
    table.addLeaf( General.SENT, "sent to CFG-Generator", configDataSender.getPcSucceeded() );
    table.addLeaf( General.SENT_FAILED, "failed to send to CFG-Generator", configDataSender.getPcFailed() );
    table.addLeaf( General.SENT_REBUILD, "socket rebuilds for CFG-Generator", configDataSender.getPcRebuilds() );
  }

  /**
   * @return
   */
  public OidSingleHandler getOidSingleHandler() {
    return table;
  }
  
}
