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
package com.gip.xyna.xmcp.xfcli.impl;

import java.io.OutputStream;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.RMIManagement.RMIParameter;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.ProxyInformation;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.ProxyManagement;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.ProxyRole;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.ProxyRole.ProxyRoleBuilder;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement;
import com.gip.xyna.xmcp.xfcli.CommandLineWriter;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Creatermiproxy;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;



public class CreatermiproxyImpl extends XynaCommandImplementation<Creatermiproxy> {

  
  public void execute(OutputStream statusOutputStream, Creatermiproxy payload) throws XynaException {
    CommandLineWriter clw = (CommandLineWriter)statusOutputStream;
    
    ProxyRole pr = buildProxyRole(payload);
    RMIParameter parameter = new RMIParameter( pr.getName()+"Proxy", 
        payload.getRegistryHost(), 
        parseInt( payload.getRegistryPort(), 0),
        parseInt( payload.getPort(), 0)
        );
    
    ProxyManagement pm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getProxyManagement();
    ProxyInformation pi = pm.createRmiProxy(pr, parameter, payload.getDocumentation());
    
    StringBuilder sb = new StringBuilder();
    sb.append( "Created proxy ").append(pi.getName()).append(" ");
    sb.append("with ").append( pi.getNumberOfProxyMethods() ).append(" methods ");
    sb.append("for ").append( pi.getNumberOfRights() ).append(" rights. ");
    sb.append("Proxy ").append(pi.getRMIParameter().getUrl()).append(" uses communication port ").append(pi.getRMIParameter().getCommunicationPort()).append(". ");
    
    clw.writeLineToCommandLine( sb.toString() ); 
  }
  
  private int parseInt(String string, int defaultValue) {
    if( string == null || string.length() == 0 ) {
      return defaultValue;
    }
    return Integer.parseInt(string);
  }

  private ProxyRole buildProxyRole(Creatermiproxy payload) throws PersistenceLayerException {
    UserManagement um = XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement();
    
    ProxyRoleBuilder prb = ProxyRole.newProxyRole(payload.getProxyName());
    
    if( payload.getRoles() != null ) {
      for( String roleName : payload.getRoles() ) {
        Role role = um.getRole(roleName);
        if (role == null) {
          throw new RuntimeException("Role " + roleName + " not found.");
        }
        prb.addRole(role);
      }
    }
    if( payload.getRights() != null ) {
      for( String right : payload.getRights() ) {
        prb.addRight(right);
      }
    }
    
    if( payload.getNopublic() ) {
      prb.withoutPublic();
    }
    if( payload.getDeprecated() ) {
      prb.withDeprecated();
    }
    
    return prb.buildProxyRole(um);
  }

}
