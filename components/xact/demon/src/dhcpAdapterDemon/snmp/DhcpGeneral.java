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
package dhcpAdapterDemon.snmp;

import com.gip.xyna.utils.snmp.OID;
import com.gip.xyna.utils.snmp.agent.utils.EnumIntStringSnmpTable;
import com.gip.xyna.utils.snmp.agent.utils.OidSingleHandler;

import dhcpAdapterDemon.DhcpDbStack;
import dhcpAdapterDemon.SocketMaster;


public class DhcpGeneral {
  
  public static final OID OID_GENERAL = new OID(".1.3.6.1.4.1.28747.1.13.1.2");
  
  private EnumIntStringSnmpTable<General> table;
  
  private static enum General {
    PORT, STATUS_LISTENER, REJECTED, REQUESTED, SUCCEEDED, FAILED;
  }
  
  public DhcpGeneral() {
    table = new EnumIntStringSnmpTable<General>(General.class, OID_GENERAL );
  }
  
  private static class PortLeaf implements EnumIntStringSnmpTable.Leaf {
    private SocketMaster socketMaster;
    public PortLeaf(SocketMaster socketMaster) {
      this.socketMaster = socketMaster;
    }
    public Integer asInt() {
      return socketMaster.getPort();
    }
    public String asString() {
      return "port: "+socketMaster.getPort();
    }
  }

  private static class StatusLeaf implements EnumIntStringSnmpTable.Leaf {
    private SocketMaster socketMaster;
    public StatusLeaf(SocketMaster socketMaster) {
      this.socketMaster = socketMaster;
    }
    public Integer asInt() {
      return socketMaster.getStatus().toInt();
    }
    public String asString() {
      return "listener status: "+socketMaster.getStatus().toString();
    }
  }
  
  private static class RejectedLeaf implements EnumIntStringSnmpTable.Leaf {
    public Integer asInt() {
      return Integer.valueOf(0);
    }
    public String asString() {
      return "this implementation can't reject";
    }
  }

  public void initialize(SocketMaster socketMaster, DhcpDbStack dhcpDbStack) {
    table.addLeaf( General.PORT, new PortLeaf(socketMaster) );
    table.addLeaf( General.STATUS_LISTENER, new StatusLeaf(socketMaster) );
    table.addLeaf( General.REJECTED, new RejectedLeaf() );
    table.addLeaf( General.REQUESTED, "requested", dhcpDbStack.getCounter( DhcpDbStack.Counters.REQUESTED ) );
    table.addLeaf( General.SUCCEEDED, "succeeded",dhcpDbStack.getCounter( DhcpDbStack.Counters.SUCCEEDED )  );
    table.addLeaf( General.FAILED, "failed", dhcpDbStack.getCounter( DhcpDbStack.Counters.FAILED ) );
  }
    
  /**
   * @return
   */
  public OidSingleHandler getOidSingleHandler() {
    return table;
  }

  
}
