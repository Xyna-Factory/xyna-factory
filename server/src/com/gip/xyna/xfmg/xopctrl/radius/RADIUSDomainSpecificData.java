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
package com.gip.xyna.xfmg.xopctrl.radius;


import java.util.List;

import com.gip.xyna.xfmg.xopctrl.DomainTypeSpecificData;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement;


public class RADIUSDomainSpecificData implements DomainTypeSpecificData {
  
  private static final long serialVersionUID = 7177523157271609582L;
  
  private List<RADIUSServer> serverList;
  
  private String associatedOrdertype;
  
  
  public RADIUSDomainSpecificData() {
    super();
  }
  
  
  public RADIUSDomainSpecificData(String associatedOrdertype, List<RADIUSServer> serverList) {
    this();
    this.associatedOrdertype = associatedOrdertype;
    this.serverList = serverList;    
  }
  
  
  public List<RADIUSServer> getServerList() {
   return serverList; 
  }

  
  public void setServerList(List<RADIUSServer> serverList) {
    this.serverList = serverList; 
  }
  
  
  public String getAssociatedOrdertype() {
    return associatedOrdertype; 
   }

   
   public void setAssociatedOrdertype(String associatedOrdertype) {
     this.associatedOrdertype = associatedOrdertype; 
   }


  public void appendInformation(StringBuilder output) {
    output.append(UserManagement.INDENT_FOR_DOMAIN_SPECIFIC_DATA)
          .append("AssociatedOrderType: ").append(getAssociatedOrdertype()).append("\n");
    List<RADIUSServer> servers = getServerList();
    for (RADIUSServer server : servers) {
      output.append(UserManagement.INDENT_FOR_DOMAIN_SPECIFIC_DATA)
            .append(server.getIp().getValue()).append(":").append(server.getPort().getValue())
            .append(" - ").append(server.getPresharedKey().getKey()).append("\n");
    }
  }
  
  
}
