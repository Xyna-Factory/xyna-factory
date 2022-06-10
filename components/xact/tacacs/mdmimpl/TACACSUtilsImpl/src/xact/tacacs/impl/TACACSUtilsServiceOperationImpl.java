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
package xact.tacacs.impl;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.apache.log4j.Logger;

import xact.tacacs.TACACSLoginFailed;
import xact.tacacs.TACACSUtilsServiceOperation;
import xact.tacacs.tk.Tacacs;
import base.Credentials;
import base.Host;
import base.Port;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;


public class TACACSUtilsServiceOperationImpl implements ExtendedDeploymentTask, TACACSUtilsServiceOperation {
    
    private static Logger logger = Logger.getLogger(TACACSUtilsServiceOperationImpl.class);
    private static final String TACACS_ENCRYPTION_KEY_PROPERTY = "xact.tacacs.encryptionKey";
    private static final String TACACS_MY_IP = "xact.tacacs.myOwnIp";

  public void onDeployment() throws XynaException {
    // TODO do something on deployment, if required
    // This is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  public void onUndeployment() throws XynaException {
    // TODO do something on undeployment, if required
    // This is executed again on each classloader-unload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.;
    return null;
  }

  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    // Defines the behavior of the (un)deployment after reaching the timeout and if this service ignores a Thread.interrupt.
    // - BehaviorAfterOnUnDeploymentTimeout.EXCEPTION: Deployment will be aborted, while undeployment will log the exception and NOT abort.
    // - BehaviorAfterOnUnDeploymentTimeout.IGNORE: (Un)Deployment will be continued in another thread asynchronously.
    // - BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD: (Un)Deployment will be continued after calling Thread.stop on the thread.
    //   executing the (Un)Deployment.
    // If null is returned, the factory default <IGNORE> will be used.
    return null;
  }

  public void performTACACSLogin(Credentials credentials, Host host, Port port) throws TACACSLoginFailed {
      Tacacs tac = new Tacacs();
      tac.setHostname(host.getHostname());
      Tacacs.setMyIp(XynaFactory.getInstance().getXynaMultiChannelPortal().getProperty(TACACS_MY_IP));
      
      String key = XynaFactory.getInstance().getXynaMultiChannelPortal().getProperty(TACACS_ENCRYPTION_KEY_PROPERTY);
      
      if ((key == null) || (key.trim().equals(""))) {
          throw new TACACSLoginFailed("Error: Please specify the TACACS+ encryption key in XynaProperty \"" + TACACS_ENCRYPTION_KEY_PROPERTY + "\"!");
      }
      
      tac.setKey(key);
      tac.setPortNumber(port.getValue());
      try {
          boolean success = tac.isAuthenticated(credentials.getUsername(),credentials.getPassword());
          if (success) {
              logger.info("TACACS-Login for user \"" + credentials.getUsername() + "\" successful!");
          }
          else {
              logger.info("TACACS-Login for user \"" + credentials.getUsername() + "\" failed!");
              throw new TACACSLoginFailed("The TACACS+ login failed because of invalid credentials.");
          }
          
      } catch (NoSuchAlgorithmException e) {
          logger.error("Error while encrypting or decrypting TACACS+ communication.");
          throw new TACACSLoginFailed("Error while encrypting or decrypting TACACS+ communication.", e);
      } catch (IOException e) {
          logger.error("Error while processing stream for TACACS+ communication.");
          throw new TACACSLoginFailed("Error while processing stream for TACACS+ communication.", e);
      }
  }

}
