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

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.apache.log4j.Logger;


public class TreeStringBuilder {

  public String buildTreeStringWithUnpooledHosts(List<LocationTreeQueryLine> lines, List<LocationTreeQueryLine> unpooledHosts) throws RemoteException {
    String LINE_SEPERATOR = System.getProperty("line.separator");
    if (lines.size() < 1) {
      return "<root/>";
    }
    
    StringBuilder s = new StringBuilder();
    s.append(LINE_SEPERATOR).append(" <root> ");
    ListIterator<LocationTreeQueryLine> lineIterator = lines.listIterator();
    
    while (isStandortGruppeNextElement(lineIterator)) {
      appendStandortGruppen(lineIterator, lineIterator.next(), s, unpooledHosts);
    }
    
    s.append(LINE_SEPERATOR).append(" </root> ").append(LINE_SEPERATOR);
    
    Logger.getLogger("LineLogger").warn(s.toString());
    return s.toString();
  }
  
  
  public String buildTreeString(List<LocationTreeQueryLine> lines) throws RemoteException {
    return buildTreeStringWithUnpooledHosts(lines, null);
  }
  
  
  private boolean isStandortGruppeNextElement(ListIterator<LocationTreeQueryLine> lineIterator) {
    if (!lineIterator.hasNext()) {
      return false;
    }
    LocationTreeQueryLine nextLine = lineIterator.next();
    try {
      return isStandortGruppe(nextLine);
    } finally {
      lineIterator.previous();
    }    
  }
  
  private boolean isStandortGruppe(LocationTreeQueryLine line) {
    return (line.standortgruppeid != null &&
            !line.standortgruppeid.equals("") &&
            !line.standortgruppeid.equals("null"));
  }
   
  
  private void appendStandortGruppen(ListIterator<LocationTreeQueryLine> lineIterator, LocationTreeQueryLine currentElement, StringBuilder s, List<LocationTreeQueryLine> unpooledHosts) {
    Logger.getLogger("LineLogger").warn("appendStandortGruppen");
    
    openStandortGruppe(s, currentElement);
    if (isSharedNetwork(currentElement)) {
      appendSharedNetwork(lineIterator, currentElement, s, unpooledHosts);
    }
    while (isNextElementSharedNetworkInSameStandortGruppe(lineIterator, currentElement)) {
      appendSharedNetwork(lineIterator, lineIterator.next(), s, unpooledHosts);
    }
    closeStandortGruppe(s);
  }
  
  
  private boolean isSharedNetwork(LocationTreeQueryLine line) {
    return line.sharedNetworkId != null &&
           !line.sharedNetworkId.equals("") &&
           !line.sharedNetworkId.equals("null");
  }
  
  private boolean isSameStandortGruppe(LocationTreeQueryLine line1, LocationTreeQueryLine line2) {
    return line1.standortgruppeid.trim().equals(line2.standortgruppeid.trim());
  }
  
  private boolean isNextElementSharedNetworkInSameStandortGruppe(ListIterator<LocationTreeQueryLine> lineIterator, LocationTreeQueryLine currentLine) {
    if (!lineIterator.hasNext()) {
      return false;
    }
    LocationTreeQueryLine nextLine = lineIterator.next();
    try {
      return isSharedNetwork(nextLine) && isSameStandortGruppe(currentLine, nextLine);
    } finally {
      lineIterator.previous();
    }    
  }
  
  
  private void appendSharedNetwork(ListIterator<LocationTreeQueryLine> lineIterator, LocationTreeQueryLine currentLine, StringBuilder s, List<LocationTreeQueryLine> unpooledHosts) {
    Logger.getLogger("LineLogger").warn("appendSharedNetwork");
    openSharedNetwork(s, currentLine);
    if (isSubnet(currentLine)) {
      appendSubnet(lineIterator, currentLine, s, unpooledHosts);
    }
    while (isNextElementSubnetInSameSharedNetwork(lineIterator, currentLine)) {
      appendSubnet(lineIterator,lineIterator.next(), s, unpooledHosts);
    }
    closeSharedNetwork(s);
  }
  

  private boolean isNextElementSubnetInSameSharedNetwork(ListIterator<LocationTreeQueryLine> lineIterator, LocationTreeQueryLine currentLine) {
    if (!lineIterator.hasNext()) {
      return false;
    }
    LocationTreeQueryLine nextLine = lineIterator.next();
    try {
      return isSubnet(nextLine) && isSameSharedNetwork(currentLine, nextLine);
    } finally {
      lineIterator.previous();
    }    
  }
  
  private boolean isSubnet(LocationTreeQueryLine line) {
    return (line.subnetid != null &&
            !line.subnetid.equals("") &&
            !line.subnetid.equals("null"));
  }
  
  private boolean isSameSharedNetwork(LocationTreeQueryLine line1, LocationTreeQueryLine line2) {
    return line1.sharedNetworkId.trim().equals(line2.sharedNetworkId.trim());
  }
  
  
  
  
  private void appendSubnet(ListIterator<LocationTreeQueryLine> lineIterator, LocationTreeQueryLine currentLine, StringBuilder s, List<LocationTreeQueryLine> unpooledHosts) {
    Logger.getLogger("LineLogger").warn("appendSubnet");

    openSubnet(s, currentLine);
    if (isPool(currentLine)) {
      appendPool(lineIterator, currentLine, s);
    }
    while (isNextElementPoolInSameSubnet(lineIterator, currentLine)) {
      appendPool(lineIterator, lineIterator.next(), s);
    }
    // do we have unpooledHosts for that Subnet
    if (unpooledHosts != null &&
        unpooledHosts.size() > 0) {
      List<LocationTreeQueryLine> unpooledHostsInCurrentSubnet = new ArrayList<LocationTreeQueryLine>();
      for (LocationTreeQueryLine unpooledHost : unpooledHosts) {
        if (unpooledHost.subnetid.equals(currentLine.subnetid)) {
          unpooledHostsInCurrentSubnet.add(unpooledHost);
        }
      }
      if (unpooledHostsInCurrentSubnet.size() > 0) {
        appendUnPool(unpooledHostsInCurrentSubnet, s);
      }
    }
    // add an unpooled pool for that
    closeSubnet(s);
  }
  
  
  private boolean isNextElementPoolInSameSubnet(ListIterator<LocationTreeQueryLine> lineIterator, LocationTreeQueryLine currentLine) {
    if (!lineIterator.hasNext()) {
      return false;
    }
    LocationTreeQueryLine nextLine = lineIterator.next();
    try {
      return isPool(nextLine) && isSameSubnet(currentLine, nextLine);
    } finally {
      lineIterator.previous();
    }    
  }
  
  private boolean isPool(LocationTreeQueryLine line) {
    return (line.poolID != null &&
            !line.poolID.equals("") &&
            !line.poolID.equals("null"));
  }
  
  private boolean isSameSubnet(LocationTreeQueryLine line1, LocationTreeQueryLine line2) {
    return line1.subnetid.trim().equals(line2.subnetid.trim());
  }
  
  
  private void appendPool(ListIterator<LocationTreeQueryLine> lineIterator, LocationTreeQueryLine currentLine, StringBuilder s) {
    Logger.getLogger("LineLogger").warn("appendPool");
    
    openPool(s, currentLine);
    if (isHost(currentLine)) {
      appendHost(lineIterator, currentLine, s);
    }
    while (isNextElementHostInSamePool(lineIterator, currentLine)) {
      appendHost(lineIterator, lineIterator.next(), s);
    }
    closePool(s);
  }
  
  
  private void appendUnPool(List<LocationTreeQueryLine> unpooledHosts, StringBuilder s) {
    Logger.getLogger("LineLogger").warn("appendPool");
    
    openUnPool(s);
    for (LocationTreeQueryLine unpooledHost : unpooledHosts) {
      appendHost(null, unpooledHost, s);
    }
    closePool(s);
  }
  
  
  private boolean isHost(LocationTreeQueryLine line) {
    return (line.staticHostId != null &&
            !line.staticHostId.equals("") &&
            !line.staticHostId.equals("null"));
  }
  
  private boolean isSamePool(LocationTreeQueryLine line1, LocationTreeQueryLine line2) {
    return line1.poolID.trim().equals(line2.poolID.trim());
  }
  
  
  private boolean isNextElementHostInSamePool(ListIterator<LocationTreeQueryLine> lineIterator, LocationTreeQueryLine currentLine) {
    if (!lineIterator.hasNext()) {
      return false;
    }
    LocationTreeQueryLine nextLine = lineIterator.next();
    try {
      return isHost(nextLine) && isSamePool(currentLine, nextLine);
    } finally {
      lineIterator.previous();
    }    
  }
  
  
  private void appendHost(ListIterator<LocationTreeQueryLine> lineIterator, LocationTreeQueryLine currentLine, StringBuilder s) {
    Logger.getLogger("LineLogger").warn("appendHost");
    insertHost(s, currentLine);
  }
  
  
  private void openStandortGruppe(StringBuilder s, LocationTreeQueryLine line) {
    s.append("\n  <standortGruppe ");

    s.append(" Standortgruppe=\"").append(line.standortgruppe).append("\"");
    s.append(" Name=\"").append(line.standortgruppe).append("\"");
    s.append(" StandortGruppeID=\"").append(line.standortgruppeid).append("\"");
    s.append(" StandortID=\"").append(line.standortId).append("\"");

    s.append(" label=\"").append(line.standortgruppe).append("\"");
    s.append(" > ");
  }


  private void closeStandortGruppe(StringBuilder s) {
    s.append("\n  </standortGruppe>");
  }


  private boolean isEmpty(String value) {
    if (value == null) {
      return true;
    }
    if (value.trim().length() < 1) {
      return true;
    }
    if (value.trim().equals("null")) {
      return true;
    }
    return false;
  }


  private boolean openSharedNetwork(StringBuilder s, LocationTreeQueryLine line) {
    if (isEmpty(line.sharedNetworkId)) {
      return false;
    }
    s.append("\n    <sharedNetwork ");

    s.append(" SharedNetworkID=\"").append(line.sharedNetworkId).append("\"");
    s.append(" CpeDns=\"").append(line.cpedns).append("\"");
    s.append(" SharedNetwork=\"").append(line.sharednetwork).append("\"");
    s.append(" CpeDnsID=\"").append(line.cpeDnsId).append("\"");
    s.append(" Standort=\"").append(line.standort).append("\"");
    s.append(" StandortID=\"").append(line.standortId).append("\"");
    s.append(" StandortGruppeID=\"").append(line.standortgruppeid).append("\"");
    s.append(" LinkAddresses=\"").append(line.linkAddresses).append("\"");
    s.append(" MigrationState=\"").append(line.sharedNetworkMigrationState).append("\"");

    s.append(" label=\"").append(line.sharednetwork).append(" (").append(line.standort).append(")").append("\"");
    s.append(" > ");
    return true;
  }


  private void closeSharedNetwork(StringBuilder s) {
    s.append("\n    </sharedNetwork>");
  }


  private boolean openSubnet(StringBuilder s, LocationTreeQueryLine line) {
    if (isEmpty(line.subnetid)) {
      return false;
    }
    s.append("\n      <subnet ");

    s.append(" SubnetID=\"").append(line.subnetid).append("\"");
    s.append(" Mask=\""+ line.mask+ "\"");
    s.append(" SharedNetwork=\"").append(line.sharednetwork).append("\"");
    s.append(" SharedNetworkID=\"").append(line.sharedNetworkId).append("\"");
    s.append(" Subnet=\"").append(line.subnet).append("\"");
    s.append(" FixedAttributes=\"").append(line.fixedattributes).append("\"");
    s.append(" Attributes=\"").append(line.attributes).append("\"");
    s.append(" MigrationState=\"").append(line.subnetMigrationState).append("\"");

    s.append(" label=\"").append(line.subnet).append("\"");
    s.append(" > ");
    return true;
  }


  private void closeSubnet(StringBuilder s) {
    s.append("\n      </subnet >");
  }
  

  private void insertHost(StringBuilder s, LocationTreeQueryLine line) {
    s.append("\n        <staticHost ");

    s.append(" StaticHostID=\"").append(line.staticHostId).append("\"");
    s.append(" Cpe_mac=\"").append(line.cpemac).append("\"");
    s.append(" RemoteId=\"").append(line.remoteid).append("\"");
    s.append(" Deployed1=\"").append(line.deployed1).append("\"");
    s.append(" Deployed2=\"").append(line.deployed2).append("\"");
    s.append(" SubnetID=\"").append(line.subnetid).append("\"");
    s.append(" Subnet=\"").append(line.subnet).append("\"");
    s.append(" PoolID=\"").append(line.poolID).append("\"");
    s.append(" PoolTypeID=\"").append(line.poolTypeID).append("\"");
    s.append(" PoolType=\"").append(line.pooltype).append("\"");
    s.append(" Ip=\"").append(line.ip).append("\"");
    s.append(" Hostname=\"").append(line.hostname).append("\"");
    s.append(" ConfigDescr=\"").append(line.configdescr).append("\"");
    s.append(" AssignedPoolID=\"").append(line.assignedPoolID).append("\"");
    s.append(" DesiredPoolType=\"").append(line.desiredPoolType).append("\"");
    s.append(" Dns=\"").append(line.hostDnsList).append("\"");
    s.append(" DynamicDnsActive=\"").append(line.dynamicDnsActive).append("\"");
    
    s.append(" label=\"").append(line.ip).append(" (").append(line.cpemac).append(")").append("\"");

    s.append(" />");
  }



  private void openPool(StringBuilder s, LocationTreeQueryLine line) {
    s.append("\n        <pool ");

    s.append(" SubnetID=\"").append(line.subnetid).append("\"");
    s.append(" PoolTypeID=\"").append(line.poolTypeID).append("\"");
    s.append(" PoolID=\"").append(line.poolID).append("\"");
    s.append(" RangeStop=\"").append(line.rangestop).append("\"");
    s.append(" RangeStart=\"").append(line.rangestart).append("\"");
    s.append(" Subnet=\"").append(line.subnet).append("\"");
    s.append(" PoolType=\"").append(line.pooltype).append("\"");
    s.append(" IsDeployed=\"").append(line.isDeployed).append("\"");
    s.append(" TargetState=\"").append(line.targetState).append("\"");
    s.append(" UseForStatistics=\"").append(line.useForStatistics).append("\"");
    s.append(" Exclusions=\"").append(line.exclusions).append("\"");
    s.append(" MigrationState=\"").append(line.poolMigrationState).append("\"");

    s.append(" label=\"").append(line.rangestart).append(" (").append(line.pooltype).append(")").append("\"");
    
    s.append(" >");
  }
  
  
  private void openUnPool(StringBuilder s) {
    s.append("\n        <pool ");

    s.append(" SubnetID=\"0\"");
    s.append(" PoolTypeID=\"0\"");
    s.append(" PoolID=\"0\"");
    s.append(" RangeStop=\"0\"");
    s.append(" RangeStart=\"0\"");
    s.append(" Subnet=\"0\"");
    s.append(" PoolType=\"0\"");
    s.append(" IsDeployed=\"no\"");
    s.append(" TargetState=\"inactive\"");
    s.append(" UseForStatistics=\"no\"");
    s.append(" Exclusions=\"\"");

    s.append(" label=\"UNPOOLED\" ");
    
    s.append(" >");
  }
  
  
  private void closePool(StringBuilder s) {
    s.append("        </pool> \n");
  }

}
