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
package com.gip.xyna.xfmg.xopctrl.usermanagement;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.exceptions.XFMG_PasswordExpiredException;
import com.gip.xyna.xfmg.exceptions.XFMG_UserAuthenticationFailedException;
import com.gip.xyna.xfmg.exceptions.XFMG_UserIsLockedException;



public abstract class UserAuthentificationMethod {
    
  private final static Logger logger = CentralFactoryLogging.getLogger(UserAuthentificationMethod.class);
  
  public abstract Role authenticateUserInternally(String username, String password) throws XFMG_UserAuthenticationFailedException, XFMG_UserIsLockedException;
  
  protected String username;
  
  
  public Role authenticateUser(String username, String password) throws XFMG_UserAuthenticationFailedException, XFMG_UserIsLockedException {
    this.username = username;
    return authenticateUserInternally(username, password);
  }

  
  public static final List<UserAuthentificationMethod> generateAuthenticationMethods(String username) {
    try {
      List<Domain> domains = XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement().getDomainsForUser(username);
      if (domains == null || domains.size() <= 0) {
        return new ArrayList<UserAuthentificationMethod>();
      } else {
        return generateAuthenticationMethods(domains);
      }
    } catch (XynaException e) {
      return new ArrayList<UserAuthentificationMethod>();
    }
  }
  
  
  public static final List<UserAuthentificationMethod> generateAuthenticationMethods(List<Domain> domains) {
    List<UserAuthentificationMethod> methods = new ArrayList<UserAuthentificationMethod>();
    for (Domain domain : domains) {
      methods.add(domain.generateAuthenticationMethod(domain));
    }
    return methods;
  }
  
  
  public static final Role startAuthenticationProcess(String user, String password) throws XFMG_UserAuthenticationFailedException, XFMG_UserIsLockedException {
    List<UserAuthentificationMethod> methods = generateAuthenticationMethods(user);
    return executeAuthentificationMethods(user, password, methods);
  }


  public static final Role executeAuthentificationMethods(String user, String password, List<UserAuthentificationMethod> methods)
      throws XFMG_UserAuthenticationFailedException, XFMG_UserIsLockedException {
    for (UserAuthentificationMethod userAuthentificationMethod : methods) {
      try {
        Role authenticatedRole = userAuthentificationMethod.authenticateUser(user, password);
        if (authenticatedRole == null) {
          continue;
        } else {
          return authenticatedRole;
        }
      } catch (XFMG_PasswordExpiredException e) {
        throw e;
      } catch (XFMG_UserIsLockedException e) {
        throw e;
      } catch (Throwable t) {
        logger.debug("Error while trying to authenticate, continuing with next method if available", t);
        continue;
      }
    }
    
    throw new XFMG_UserAuthenticationFailedException(user);
  }
  
}
