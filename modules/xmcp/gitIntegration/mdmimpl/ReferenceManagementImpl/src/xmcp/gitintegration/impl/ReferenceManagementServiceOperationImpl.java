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
package xmcp.gitintegration.impl;


import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;

import base.File;
import base.math.IntegerNumber;

import java.util.List;

import xmcp.gitintegration.Reference;
import xmcp.gitintegration.ReferenceData;
import xmcp.gitintegration.RemoveReferenceData;
import xmcp.gitintegration.cli.generated.OverallInformationProvider;
import xmcp.gitintegration.storage.ReferenceStorage;
import xprc.xpce.Workspace;
import xmcp.gitintegration.ReferenceManagementServiceOperation;


public class ReferenceManagementServiceOperationImpl implements ExtendedDeploymentTask, ReferenceManagementServiceOperation {

  public void onDeployment() throws XynaException {
    // TODO do something on deployment, if required
    // This is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
    ReferenceStorage.init();
    OverallInformationProvider.onDeployment();
  }

  public void onUndeployment() throws XynaException {
    // TODO do something on undeployment, if required
    // This is executed again on each classloader-unload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
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

  public void addReference(ReferenceData referenceData3) {
    // Implemented as code snippet!
  }

  public List<? extends ReferenceData> listReferences(Workspace workspace2) {
    // Implemented as code snippet!
    return null;
  }

  public void removeReference(RemoveReferenceData removeReferenceData5) {
    // Implemented as code snippet!
  }

  @Override
  public List<? extends File> triggerReferences(List<? extends Reference> arg0, List<? extends File> arg1, IntegerNumber arg2) {
    // TODO Auto-generated method stub
    return null;
  }

}