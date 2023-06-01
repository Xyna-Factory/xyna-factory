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

package com.gip.xyna.coherence.standalone;



import java.rmi.RemoteException;

import com.gip.xyna.coherence.CacheController;
import com.gip.xyna.coherence.CacheControllerFactory;
import com.gip.xyna.coherence.coherencemachine.interconnect.InterconnectCalleeProviderFactory;
import com.gip.xyna.coherence.coherencemachine.interconnect.NodeConnectionProviderFactory;
import com.gip.xyna.coherence.coherencemachine.interconnect.rmi.RMIConnectionClientParameters;
import com.gip.xyna.coherence.coherencemachine.interconnect.rmi.RMIConnectionParametersServer;
import com.gip.xyna.coherence.remote.CacheControllerRemoteInterfaceFactory;
import com.gip.xyna.coherence.remote.CacheControllerRemoteInterfaceWithInit;



public class CoherenceNode {

  private final CacheController controller;
  private final CacheControllerRemoteInterfaceWithInit remoteInterface;


  public CoherenceNode(int portForCacheControllerRemoteInterface) {
    controller = CacheControllerFactory.newCacheController();
    remoteInterface =
        CacheControllerRemoteInterfaceFactory.getCacheControllerRemoteInterface(controller,
                                                                                portForCacheControllerRemoteInterface);
  }


  public void init(RMIConnectionParametersServer parameters) {
    controller
        .addCallee(InterconnectCalleeProviderFactory.getInstance().getRMIProvider(parameters,
                                                                                  controller.getRMIClassLoader()));
    try {
      remoteInterface.init();
    } catch (RemoteException e) {
      throw new RuntimeException(e);
    }
  }


  public void shutdown() {
    try {
      remoteInterface.shutdown();
    } catch (RemoteException e) {
      throw new RuntimeException(e);
    }
    controller.shutdown();
  }


  public void createNewCluster() {
    controller.setupNewCluster();
  }


  public void connectToClusterRMI(RMIConnectionClientParameters target) {
    controller
        .connectToClusterLocally(NodeConnectionProviderFactory.getInstance().getRMIConnectionProvider(target,
                                                                                                      controller));
  }


  public CacheController getController() {
    return controller;
  }

}
