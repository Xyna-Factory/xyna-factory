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
package com.gip.xyna.xfmg.xopctrl.usermanagement;


import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.exceptions.XFMG_UserAuthenticationFailedException;
import com.gip.xyna.xfmg.exceptions.XFMG_UserIsLockedException;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;


public class LocalUserAuthentication extends UserAuthentificationMethod {

  @Override
  public Role authenticateUserInternally(String user, String password) throws XFMG_UserAuthenticationFailedException, XFMG_UserIsLockedException {
    try {
      User authenticatedUser = XynaFactory.getPortalInstance().getFactoryManagementPortal().authenticateHashed(user, password);
      if (authenticatedUser == null) {
        throw new XFMG_UserAuthenticationFailedException(user);
      }
    
      return XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement()
                      .resolveRole(authenticatedUser.getRole());
    } catch (PersistenceLayerException e) {
      throw new XFMG_UserAuthenticationFailedException(user,e);
    }
  }

}
