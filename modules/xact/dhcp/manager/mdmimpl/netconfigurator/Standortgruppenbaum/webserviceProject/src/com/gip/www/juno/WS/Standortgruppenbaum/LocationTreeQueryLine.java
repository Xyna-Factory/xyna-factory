/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package com.gip.www.juno.WS.Standortgruppenbaum;


public class LocationTreeQueryLine {
  String standortgruppe = "";
  String standort = "";
  String cpedns = "";
  String sharednetwork = "";
  String subnet = "";
  String fixedattributes = "";
  String attributes = "";
  String rangestart = "";
  String rangestop = "";
  String pooltype = "";
  String hostname = "";
  String cpemac = "";
  String ip = "";
  String remoteid = "";
  String deployed1 = "";
  String deployed2 = "";
  String configdescr = "";
  String cpeDnsId = "";
  String sharedNetworkId = ""; 
  String standortgruppeid = "";
  String standortId = "";
  String subnetid = "";
  String poolTypeID = "";
  String poolID = "";
  String staticHostId = "";
  String mask = "";
  String targetState = "";
  String isDeployed = "";
  String useForStatistics = "";
  String exclusions = "";
  String assignedPoolID = "";
  String desiredPoolType = "";
  String hostDnsList = "";
  String linkAddresses = "";
  String dynamicDnsActive = "";
  String sharedNetworkMigrationState = "";
  String subnetMigrationState = "";
  String poolMigrationState = "";
  
  
  public String toString() {
    StringBuilder s = new StringBuilder("\n QueryLine {");
    
    s.append("  standortgruppe = ").append(standortgruppe );
    s.append(", standort = ").append(standort);
    s.append(", cpedns = ").append(cpedns);
    s.append(", sharednetwork = ").append(sharednetwork);
    s.append(", linkAddresses = ").append(linkAddresses);
    s.append(", subnet = ").append(subnet);
    s.append(", subnetid = ").append(subnetid);
    s.append(", fixedattributes = ").append(fixedattributes);
    s.append(", attributes = ").append(attributes);
    s.append(", rangestart = ").append(rangestart);
    s.append(", rangestop = ").append(rangestop);
    s.append(", pooltype = ").append(pooltype);
    s.append(", hostname = ").append(hostname);
    s.append(", cpemac = ").append(cpemac);
    s.append(", remoteid = ").append(remoteid);
    s.append(", ip = ").append(ip);
    s.append(", deployed1 = ").append(deployed1);
    s.append(", deployed2 = ").append(deployed2);
    s.append(", configdescr = ").append(configdescr);

    s.append("  } \n");
    return s.toString();
  }
}
