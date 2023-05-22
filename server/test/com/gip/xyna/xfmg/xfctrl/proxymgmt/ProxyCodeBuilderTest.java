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
package com.gip.xyna.xfmg.xfctrl.proxymgmt;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.code.ProxyCodeBuilder;

public class ProxyCodeBuilderTest {
  
  public static void main(String[] args) throws XynaException, NoSuchMethodException, SecurityException {
    
    
    String rmiUrl = "//"+"localhost"+":"+1099+"/XynaRMIChannel";

    
   
    ProxyRole proxyRole = ProxyRole.newProxyRole("First").
        //addRole(role).
        //addRight("START_ORDER").
        //addRight("WORKINGSET_MANAGEMENT").
        //addRight("xfmg.xfctrl.deploymentItems:*:*:*:*").
        //addRight("xfmg.xfctrl.dataModels:*:*").
        //addRight("xfmg.xfctrl.orderTypes:*:*:*:*").
        //addRight("xfmg.xfctrl.ApplicationDefinitionManagement:*:*:*").
        //addRight("xfmg.xfctrl.XynaProperties:*:abc.*").
        //addRight("xfmg.xfctrl.XynaProperties:read:*").
        addRight("xfmg.xfctrl.ApplicationManagement:*:*:*").
        withoutPublic().
        //addRight("xfmg.xfctrl.dataModels:*:*").
        //withDeprecated().
        buildProxyRole();
    
    
  
    
    ProxyCodeBuilder pcb = new ProxyCodeBuilder(proxyRole);
    pcb.createRmiProxy(rmiUrl, true);
    
    System.out.println( pcb.getInterface() );
    
    System.out.println( pcb.getImplementation() );
    
  }


}
