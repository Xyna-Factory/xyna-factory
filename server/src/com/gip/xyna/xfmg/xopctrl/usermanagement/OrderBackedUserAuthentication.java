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
package com.gip.xyna.xfmg.xopctrl.usermanagement;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.exceptions.XFMG_UserAuthenticationFailedException;
import com.gip.xyna.xfmg.exceptions.XFMG_UserIsLockedException;
import com.gip.xyna.xfmg.xopctrl.usermanagement.AuthenticationResult;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserAuthentificationMethod;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xsched.XynaThreadFactory;


public abstract class OrderBackedUserAuthentication extends UserAuthentificationMethod {

  private final static Logger logger = CentralFactoryLogging.getLogger(OrderBackedUserAuthentication.class);
  
  private final static ExecutorService executor = Executors.newCachedThreadPool(new XynaThreadFactory("Authenticator", 3));
  
  private Domain domain;
  
  public OrderBackedUserAuthentication(Domain domain) {
    this.domain = domain;
  }
  
  
  @Override
  public Role authenticateUserInternally(String username, String password) throws XFMG_UserAuthenticationFailedException, XFMG_UserIsLockedException {
    int tries = 0;
    //FIXME keine retries auf gleichen ldap server, wenn result sich nicht �ndern kann
    do {
      Future<AuthenticationResult> execution = executor.submit(new OrderExecution(username, password, domain, tries));
      try {
        AuthenticationResult result = execution.get(domain.getConnectionTimeout(), TimeUnit.SECONDS);
        return handleResponse(result);
      } catch (TimeoutException e) {
        execution.cancel(true);
      } catch (InterruptedException e) {
        execution.cancel(true);
      } catch (ExecutionException e) {
        Role role = handleError(e.getCause());
        if (role != null) {
          return role;
        }
      } finally {
        tries++;
      }
    } while (tries < domain.getMaxRetries());
    throw new XFMG_UserAuthenticationFailedException(username);
  }
  
  
  public abstract XynaOrder generateAuthOrder(String username, String password, Domain domain, int retry);
  
  public abstract Role handleResponse(AuthenticationResult result) throws XFMG_UserAuthenticationFailedException; // TODO only XFMG_UserAuthException?
  
  // RESULT != null, RETRY == null or THROW for abort
  public abstract Role handleError(Throwable exception) throws XFMG_UserAuthenticationFailedException; // TODO only XFMG_UserAuthException?
  
  
  private class OrderExecution implements Callable<AuthenticationResult> {

    private String username;
    private String password;
    private Domain domain;
    private int retries;
    
    
    private OrderExecution(String username, String password, Domain domain, int retries) {
      this.username = username;
      this.password = password;
      this.retries = retries;
      this.domain = domain;
    }
    
    
    public AuthenticationResult call() throws Exception {
      XynaOrder order = OrderBackedUserAuthentication.this.generateAuthOrder(username, password, domain, retries);
      XynaOrderServerExtension xose = new XynaOrderServerExtension(order);
      return (AuthenticationResult)XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().startOrderSynchronous(xose).getOutputPayload();
    }
    
  }

}
