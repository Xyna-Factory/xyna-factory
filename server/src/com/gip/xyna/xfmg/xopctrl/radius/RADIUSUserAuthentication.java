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
package com.gip.xyna.xfmg.xopctrl.radius;


import com.gip.xyna.XynaFactory;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.exceptions.XFMG_UserAuthenticationFailedException;
import com.gip.xyna.xfmg.xopctrl.usermanagement.AuthenticationResult;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Domain;
import com.gip.xyna.xfmg.xopctrl.usermanagement.DomainName;
import com.gip.xyna.xfmg.xopctrl.usermanagement.OrderBackedUserAuthentication;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.User;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserName;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.XynaOrderServerExtension;


public class RADIUSUserAuthentication extends OrderBackedUserAuthentication {

  public final static String ORDER_CONTEXT_KEX_RADIUS_PASSWORD = "xfmg.xopctrl.radius.password";
  
  public RADIUSUserAuthentication(Domain domain) {
    super(domain);
  }


  public XynaOrder generateAuthOrder(String username, String password, Domain domain, int retry) {
    RADIUSDomainSpecificData rdsd = (RADIUSDomainSpecificData) domain.getDomainSpecificData();
    RADIUSServer server = rdsd.getServerList().get(retry % rdsd.getServerList().size());
    UserName usernameXo = new UserName(username);
    DomainName domainnameXo = new DomainName(domain.getName());
    RADIUSConnectionConfig config = new RADIUSConnectionConfig(server, domain.getMaxRetries(), domain.getMaxRetries());
    GeneralXynaObject inputPayload = new Container(usernameXo, domainnameXo, config);
    XynaOrderCreationParameter xocp = new XynaOrderCreationParameter(rdsd.getAssociatedOrdertype(), 10, inputPayload);
    XynaOrderServerExtension xo = new XynaOrderServerExtension(xocp);
    xo.setNewOrderContext();
    xo.getOrderContext().set(ORDER_CONTEXT_KEX_RADIUS_PASSWORD, password);
    return xo;
  }


  public Role handleResponse(AuthenticationResult result) throws XFMG_UserAuthenticationFailedException {
    if (result.wasSuccesfull()) {
      try {
        User user = XynaFactory.getPortalInstance().getXynaMultiChannelPortalPortal().getUser(username);
        // success
        return XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement().resolveRole(user.getRole());
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
