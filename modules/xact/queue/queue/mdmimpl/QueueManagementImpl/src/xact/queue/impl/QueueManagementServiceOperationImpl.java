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
package xact.queue.impl;



import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.QueueManagement;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrderServerExtension;

import xact.queue.NoSuchQueueException;
import xact.queue.Queue;
import xact.queue.QueueManagementServiceOperation;
import xact.queue.QueueName;



public class QueueManagementServiceOperationImpl implements ExtendedDeploymentTask, QueueManagementServiceOperation {


  public void onDeployment() throws XynaException {
    // This is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  public void onUndeployment() throws XynaException {
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


  public Queue getQueue(XynaOrderServerExtension order, QueueName queueName) throws NoSuchQueueException {
    Long revision = order.getRevision();
    Set<Long> dependencyRevisions = new HashSet<Long>();
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
        .getDependenciesRecursivly(revision, dependencyRevisions);
    dependencyRevisions.add(revision);
    try {
      return (Queue) getQueueManagement().buildQueueInstance(dependencyRevisions, queueName.getName());
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new NoSuchQueueException(queueName.getName(), e);
    }
  }


  public List<? extends QueueName> listQueueNames() {
    try {
      List<QueueName> queueNames = new ArrayList<QueueName>();
      Collection<com.gip.xyna.xfmg.xfctrl.queuemgmnt.Queue> queues = getQueueManagement().listQueues();
      for( com.gip.xyna.xfmg.xfctrl.queuemgmnt.Queue q : queues ) {
        queueNames.add( new QueueName(q.getUniqueName()) );
      }
      return queueNames;
    } catch( PersistenceLayerException e ) {
      throw new RuntimeException(e);
    }
  }

  public void returnQueue(Queue queue) {
    //FIXME
    //queue.getImplementationOfInstanceMethods().getInstanceOperationInstance().close();
  }

  private QueueManagement getQueueManagement() {
    return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getQueueManagement();
  }
}
