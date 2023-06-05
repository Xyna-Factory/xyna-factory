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
package com.gip.xyna.xprc.xsched.vetos.cache.cluster;

import java.rmi.RemoteException;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProvider;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProvider.InvalidIDException;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProviderTools;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProviderTools.RMIRunnableNoException;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProviderTools.RMIRunnableNoResultNoException;

public class VCP_RemoteImpl implements VCP_RemoteInterface {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(VCP_RemoteImpl.class);

  private long rmiInterfaceId;
  private static final long RMI_IS_CLOSED = -1;
  private final RMIClusterProvider clusterProvider;
  
  
  public VCP_RemoteImpl(RMIClusterProvider clusterProvider, VCP_Remote local) {
    this.clusterProvider = clusterProvider;
    this.rmiInterfaceId = clusterProvider.addRMIInterface("RemoteVetoManagement", local);
  }

  public void closeRMI() {
    long temp = rmiInterfaceId;
    if( temp != RMI_IS_CLOSED ) {
      rmiInterfaceId = RMI_IS_CLOSED;
      clusterProvider.removeRMIInterface(temp, 500 );
    }
  }
  
  public VetoResponse processRemoteVetoRequest(VetoRequest vetoRequest) throws RemoteException {
    try {
      List<VetoResponse> vrs = RMIClusterProviderTools.executeAndCumulateNoException(
          clusterProvider, rmiInterfaceId, new RemoteVetoProcessor(vetoRequest), null);
      
      if( vrs.size() != 1 ) {
        return VetoResponse.failed(vetoRequest.getId(), "Incorrect number of cumulated responses "+ vrs.size());
      }
      return vrs.get(0);
      
    } catch (InvalidIDException e) {
      if (rmiInterfaceId == RMI_IS_CLOSED) {
        return VetoResponse.failed(vetoRequest.getId(), "Rmi is closed");
      } else {
        //nicht erwartet
        return VetoResponse.failed(vetoRequest.getId(), "Rmi failed "+e.getMessage());
      }
    }
  }
  
  private static class RemoteVetoProcessor implements RMIRunnableNoException<VetoResponse, VCP_RemoteInterface> {

    private VetoRequest vetoRequest;

    public RemoteVetoProcessor(VetoRequest vetoRequest) {
      this.vetoRequest = vetoRequest;
    }

    public VetoResponse execute(VCP_RemoteInterface clusteredInterface) throws RemoteException {
      return clusteredInterface.processRemoteVetoRequest(vetoRequest);
    }
    
  }

  @Override
  public void replicate(ReplicateVetoRequest replicateVetoRequest) throws RemoteException {
    try {
      RMIClusterProviderTools.executeNoException(
          clusterProvider, rmiInterfaceId, new Replicator(replicateVetoRequest) );
    } catch (InvalidIDException e) {
      if (rmiInterfaceId == RMI_IS_CLOSED) {
        logger.warn("Rmi is closed");
      } else {
        //nicht erwartet
        logger.warn("Rmi failed "+e.getMessage(), e);
      }
    }
  }

  private static class Replicator implements RMIRunnableNoResultNoException<VCP_RemoteInterface> {

    private ReplicateVetoRequest replicateVetoRequest;

    public Replicator(ReplicateVetoRequest replicateVetoRequest) {
      this.replicateVetoRequest = replicateVetoRequest;
    }

    public void execute(VCP_RemoteInterface clusteredInterface) throws RemoteException {
      clusteredInterface.replicate(replicateVetoRequest);
    }
    
  }

}
