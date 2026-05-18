/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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
package xact.sftp.impl;


import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.XMOM.base.IP;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.exceptions.XACT_TriggerNotFound;
import com.gip.xyna.xact.trigger.Trigger;
import com.gip.xyna.xact.trigger.TriggerInformation.TriggerInstanceInformation;
import com.gip.xyna.xact.trigger.TriggerInstanceIdentification;
import com.gip.xyna.xact.trigger.TriggerInstanceStorable.TriggerInstanceState;
import com.gip.xyna.xact.trigger.XynaActivationTrigger;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xmcp.xfcli.AXynaCommand;
import com.gip.xyna.xmcp.xfcli.CLIRegistry;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;

import base.Port;
import xact.sftp.OneTimeCredentialCollision;
import xact.sftp.Password;
import xact.sftp.SFTPTriggerAccessServiceOperation;
import xact.sftp.Username;
import xact.sftp.cli.generated.OverallInformationProvider;


public class SFTPTriggerAccessServiceOperationImpl implements ExtendedDeploymentTask, SFTPTriggerAccessServiceOperation {
  
  private final static String SFTP_TRIGGER_CLASS_NAME = "com.gip.xyna.xact.trigger.SSHTrigger";
  private final static String ADD_CREDENTIALS_METHOD_NAME = "addOneTimeCredentials";

  public void onDeployment() throws XynaException {
    List<Class<? extends AXynaCommand>> commands;
    try {
      commands = OverallInformationProvider.getCommands();
      for (Class<? extends AXynaCommand> command : commands) {
        CLIRegistry.getInstance().registerCLICommand(command);
      }
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("could not register cli commands.", e);
    }
  }

  public void onUndeployment() throws XynaException {
  }

  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.
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

  public void addOneTimeCredentials(Username username, Password password, IP ip, Port port) throws XynaException, OneTimeCredentialCollision {
    addOneTimeCredentials(username.getUsername(), password.getPassword(), ip.getValue(), String.valueOf(port.getValue()));
  }
  
  public static void addOneTimeCredentials(String username, String password, String ip, String port) throws XynaException, OneTimeCredentialCollision {
    try {
      EventListener<?,?> el = getFirstEnabledTriggerInstanceInSameRevisionOrAbove();
      java.lang.reflect.Method m = el.getClass().getDeclaredMethod(ADD_CREDENTIALS_METHOD_NAME, String.class, String.class, String.class, String.class);
      Boolean success = (Boolean) m.invoke(el, username, password, ip, port);
      if (!success) {
        throw new OneTimeCredentialCollision(username);
      }
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }
  
  public static EventListener<?,?> getFirstEnabledTriggerInstanceInSameRevisionOrAbove() throws PersistenceLayerException, XACT_TriggerNotFound {
    Long revision = ((ClassLoaderBase)SFTPTriggerAccessServiceOperationImpl.class.getClassLoader()).getRevision();
    Set<Long> allRelevantRevisions = new HashSet<Long>();
    allRelevantRevisions.add(revision);
    RuntimeContextDependencyManagement rcdm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
    rcdm.getParentRevisionsRecursivly(revision, allRelevantRevisions);
    XynaActivationTrigger xat = XynaFactory.getInstance().getActivation().getActivationTrigger();
    for (Long aRelevantRevision : allRelevantRevisions) {
      Collection<TriggerInstanceInformation> tiis = xat.getTriggerInstanceInformation(aRelevantRevision);
      for (TriggerInstanceInformation tii : tiis) {
        if (tii.getState() == TriggerInstanceState.ENABLED) {
          Trigger trigger;
          try {
            trigger = xat.getTrigger(aRelevantRevision, tii.getTriggerName(), true);
            if (trigger.getFQTriggerClassName().equals(SFTP_TRIGGER_CLASS_NAME)) {
              return xat.getTriggerInstance(new TriggerInstanceIdentification(tii.getTriggerName(), aRelevantRevision, tii.getTriggerInstanceName()));
            }
          } catch (XACT_TriggerNotFound e) {
            continue;
          }
        }
      }
    }
    throw new RuntimeException("No enabled TriggerInstance (" + SFTP_TRIGGER_CLASS_NAME + ") in revision " + revision + " or it's parents.");
  }
  

}
