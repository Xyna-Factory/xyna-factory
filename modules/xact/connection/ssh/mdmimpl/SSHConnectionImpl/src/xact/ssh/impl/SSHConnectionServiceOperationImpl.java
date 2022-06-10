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
package xact.ssh.impl;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.UserType;


public class SSHConnectionServiceOperationImpl implements ExtendedDeploymentTask {
  
  
  private static Map<Long, TransientConnectionData> openConnections = Collections.synchronizedMap(new HashMap<Long, TransientConnectionData>());
  
  
  private static AtomicLong idGenerator = new AtomicLong(0);
  
  
  public void onDeployment() throws XynaException {
      SSHConnectionInstanceOperationImpl.substringLengthProperty.registerDependency(UserType.Service, "xact.ssh.SSHConnection");
  }

  
  public void onUndeployment() throws XynaException {
    for (TransientConnectionData transientData : openConnections.values()) {
      transientData.disconnect();
    }
    SSHConnectionInstanceOperationImpl.substringLengthProperty.unregister();
  }
  
  public static long registerOpenConnection(long id, TransientConnectionData transientConnectionData) {
    if( id == -1 ) {
      id = idGenerator.incrementAndGet();
    }
    openConnections.put(id, transientConnectionData);
    return id;
  }
   
  
  public static TransientConnectionData getTransientData(long id) {
    return openConnections.get(id);
  }
  
  public static void removeTransientData(long id) {
    openConnections.remove(id);
  }

  
  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.;
    return 3000L;
  }
  

  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    // Defines the behavior of the (un)deployment after reaching the timeout and if this service ignores a Thread.interrupt.
    // - BehaviorAfterOnUnDeploymentTimeout.EXCEPTION: Deployment will be aborted, while undeployment will log the exception and NOT abort.;
    // - BehaviorAfterOnUnDeploymentTimeout.IGNORE: (Un)Deployment will be continued in another thread asynchronously.;
    // - BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD: (Un)Deployment will be continued after calling Thread.stop on the thread.;
    //   executing the (Un)Deployment.
    // If null is returned, the factory default <IGNORE> will be used.
    return BehaviorAfterOnUnDeploymentTimeout.IGNORE;
  }
  
}
