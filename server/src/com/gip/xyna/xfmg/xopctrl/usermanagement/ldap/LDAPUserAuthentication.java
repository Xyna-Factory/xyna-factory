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
package com.gip.xyna.xfmg.xopctrl.usermanagement.ldap;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.exceptions.XFMG_UserAuthenticationFailedException;
import com.gip.xyna.xfmg.xopctrl.usermanagement.AuthenticationResult;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Domain;
import com.gip.xyna.xfmg.xopctrl.usermanagement.DomainName;
import com.gip.xyna.xfmg.xopctrl.usermanagement.OrderBackedUserAuthentication;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserName;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;


public class LDAPUserAuthentication extends OrderBackedUserAuthentication {

  public final static String ORDER_CONTEXT_KEY_LDAP_PASSWORD = "xfmg.xopctrl.ldap.password";
  
  
  public LDAPUserAuthentication(Domain domain) {
    super(domain);
  }

  
  public XynaOrder generateAuthOrder(String username, String password, Domain domain, int retry) {
    LDAPDomainSpecificData rdsd = (LDAPDomainSpecificData) domain.getDomainSpecificData();
    LDAPServer server = rdsd.getServerList().get(retry % rdsd.getServerList().size());
    UserName usernameXo = new UserName(username);
    DomainName domainnameXo = new DomainName(domain.getName());
    GeneralXynaObject inputPayload = new Container(usernameXo, domainnameXo, server);
    DestinationKey dk = new DestinationKey(rdsd.getAssociatedOrdertype(), rdsd.getRuntimeContext());
    XynaOrderCreationParameter xocp = new XynaOrderCreationParameter(dk, inputPayload);
    XynaOrderServerExtension xo = new XynaOrderServerExtension(xocp);
    xo.setNewOrderContext();
    xo.getOrderContext().set(ORDER_CONTEXT_KEY_LDAP_PASSWORD, password);
    return xo;
  }


  public Role handleResponse(AuthenticationResult result) throws XFMG_UserAuthenticationFailedException {
    if (result.wasSuccesfull()) {
      try {
        // success or retry depending on return value
        return XynaFactory.getPortalInstance().getXynaMultiChannelPortalPortal().getRole(result.getRole(), UserManagement.PREDEFINED_LOCALDOMAIN_NAME);
      } catch (PersistenceLayerException e) {
        // abort
        throw new XFMG_UserAuthenticationFailedException(username);
      }
    }
    // retry
    return null;
  }


  public Role handleError(Throwable exception) throws XFMG_UserAuthenticationFailedException {
    // retry
    return null;
  }
  
}
