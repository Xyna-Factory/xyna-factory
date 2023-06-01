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
package com.gip.xyna.xmcp.xfcli.impl;

import java.io.File;
import java.io.OutputStream;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.ProxyManagement;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.ProxyRole;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.ProxyRole.ProxyRoleBuilder;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement;
import com.gip.xyna.xmcp.xfcli.CommandLineWriter;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Creatermiproxyinterface;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;



public class CreatermiproxyinterfaceImpl extends XynaCommandImplementation<Creatermiproxyinterface> {

  public void execute(OutputStream statusOutputStream, Creatermiproxyinterface payload) throws XynaException {
    CommandLineWriter clw = (CommandLineWriter)statusOutputStream;
    
    ProxyRole pr = buildProxyRole(payload);
    
    String targetDir = payload.getTargetDirectory();
    if( targetDir == null ) {
      targetDir = ".";
    }
    
    ProxyManagement pm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getProxyManagement();
    Pair<String, File> pair = pm.createRmiProxyInterface(pr, new File(targetDir) );
    
    if( payload.getShowSourceOnly() ) {
      clw.writeLineToCommandLine( pair.getFirst() );
    } else {
      clw.writeLineToCommandLine( "Created interface in jar "+pair.getSecond() );
    }
  }

  private ProxyRole buildProxyRole(Creatermiproxyinterface payload) throws PersistenceLayerException {
    UserManagement um = XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement();
    
    ProxyRoleBuilder prb = ProxyRole.newProxyRole(payload.getProxyName());
    
    if( payload.getRoles() != null ) {
      for( String roleName : payload.getRoles() ) {
        prb.addRole(um.getRole(roleName) );
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
