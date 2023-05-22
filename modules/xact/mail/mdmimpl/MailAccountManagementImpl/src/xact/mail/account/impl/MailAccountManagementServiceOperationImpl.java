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
package xact.mail.account.impl;


import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;

import xact.mail.MailAccount;
import xact.mail.account.MailAccountAlreadyRegisteredException;
import xact.mail.account.MailAccountData;
import xact.mail.account.MailAccountManagementServiceOperation;
import xact.mail.account.MailAccountNotRegisteredException;
import xact.mail.account.MailAccountParameter;
import xact.mail.account.MailAccountStorage;
import xact.mail.account.MailAccountStorageException;
import xact.mail.account.MailAccountStorageFailedException;
import xact.mail.account.cli.generated.OverallInformationProvider;


public class MailAccountManagementServiceOperationImpl implements ExtendedDeploymentTask, MailAccountManagementServiceOperation {

  @Override
  public void onDeployment() throws XynaException {
    //do something on deployment, if required
    // This is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
    MailAccountStorage.getInstance().init();
    OverallInformationProvider.onDeployment();
  }

  @Override
  public void onUndeployment() throws XynaException {
    //do something on undeployment, if required
    // This is executed again on each classloader-unload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
    OverallInformationProvider.onUndeployment();
  }

  @Override
  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.
    return null;
  }

  @Override
  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    // Defines the behavior of the (un)deployment after reaching the timeout and if this service ignores a Thread.interrupt.
    // - BehaviorAfterOnUnDeploymentTimeout.EXCEPTION: Deployment will be aborted, while undeployment will log the exception and NOT abort.
    // - BehaviorAfterOnUnDeploymentTimeout.IGNORE: (Un)Deployment will be continued in another thread asynchronously.
    // - BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD: (Un)Deployment will be continued after calling Thread.stop on the thread.
    //   executing the (Un)Deployment.
    // If null is returned, the factory default <IGNORE> will be used.
    return null;
  }

  public MailAccount registerMailAccount(MailAccountParameter mailAccountParameter) throws MailAccountAlreadyRegisteredException, MailAccountStorageFailedException {
    String name = mailAccountParameter.getName();
    MailAccountData mad = MailAccountConverter.convertFromXmom(mailAccountParameter);
    try {
      MailAccountStorage.getInstance().addNewMailAccount(mad);
      return new MailAccount(name);
      
    } catch (PersistenceLayerException e) {
      throw new MailAccountStorageFailedException(name, e);
    } catch (MailAccountStorageException e) {
      throw new MailAccountAlreadyRegisteredException(e.getName(), e);
    }
    
  }

  public MailAccountParameter showMailAccount(MailAccount mailAccount) throws MailAccountNotRegisteredException, MailAccountStorageFailedException {
    MailAccountData mad = MailAccountStorage.getInstance().getMailAccount(mailAccount.getName());
    if( mad == null ) {
      throw new MailAccountNotRegisteredException(mailAccount.getName());
    }
    return MailAccountConverter.convertToXmom(mad);
  }

  public void unregisterMailAccount(MailAccount mailAccount) throws MailAccountNotRegisteredException, MailAccountStorageFailedException {
    try {
      MailAccountStorage.getInstance().removeMailAccount(mailAccount.getName());
    } catch (PersistenceLayerException e) {
      throw new MailAccountStorageFailedException(mailAccount.getName(), e);
    } catch (MailAccountStorageException e) {
      throw new MailAccountNotRegisteredException(e.getName(), e);
    }
  }
  
 

  
}
