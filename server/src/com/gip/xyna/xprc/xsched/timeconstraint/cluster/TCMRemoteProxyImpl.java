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
package com.gip.xyna.xprc.xsched.timeconstraint.cluster;

import java.rmi.RemoteException;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xclusteringservices.ClusterContext;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProvider;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProvider.InvalidIDException;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProviderTools;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProviderTools.RMIRunnableNoResult;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_TimeWindowNotFoundInDatabaseException;
import com.gip.xyna.xprc.exceptions.XPRC_TimeWindowStillUsedException;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeConstraintWindowDefinition;


/**
 * TCMRemoteProxyImpl:
 * Implementierung des Interfaces TCMInterface, die die notwendigen Änderungen remote durchführt.
 */
public class TCMRemoteProxyImpl implements TCMInterface {

  private RMIClusterProvider clusterInstance;
  private volatile long clusteredTCMInterfaceId;

  private static final long RMI_IS_CLOSED = -1;
  private volatile boolean rmiConnected;
  private TCMRemoteEndpointImpl tcmRemoteEndpointImpl;
  private ClusterContext rmiClusterContext;
  
  /**
   * @param rmiClusterContext 
   * @param tcmRemoteEndpointImpl
   */
  public TCMRemoteProxyImpl(ClusterContext rmiClusterContext, TCMRemoteEndpointImpl tcmRemoteEndpointImpl) {
    this.rmiClusterContext = rmiClusterContext;
    this.tcmRemoteEndpointImpl = tcmRemoteEndpointImpl;
  }
  
  /**
   * Auf <code>true</code> zu setzen, wenn der Remote-Knoten erreicht werden kann
   * @param rmiConnected
   */
  public synchronized void setRmiConnected(boolean rmiConnected) {
    this.rmiConnected = rmiConnected;
    if( rmiConnected ) {
      if( clusterInstance == null ) {
        clusterInstance = ((RMIClusterProvider) rmiClusterContext.getClusterInstance());
        clusteredTCMInterfaceId = clusterInstance.addRMIInterface("RemoteTimeConstraintManagement", tcmRemoteEndpointImpl);
      }
    }
  }

  public void activateTimeWindow(String name) throws PersistenceLayerException,
  XPRC_TimeWindowNotFoundInDatabaseException {
    if( ! rmiConnected ) {
      return;
    }
    MultiPurposeTimeWindowRunnable mptwr = new MultiPurposeTimeWindowRunnable(name, MultiPurpose.Activate );
    try {
      RMIClusterProviderTools.execute(clusterInstance, clusteredTCMInterfaceId, mptwr );
    } catch (InvalidIDException e) {
      handleInvalidIDException(e);
    } catch (PersistenceLayerException e) {
      throw e;
    } catch( XPRC_TimeWindowNotFoundInDatabaseException e ) {
      throw e;
    } catch( XynaException e ) {
      //kann hier nicht vorkommen
      throw new RuntimeException(e);
    }
  }
  
  public void activateTimeWindow(TimeConstraintWindowDefinition definition) {
    if( ! rmiConnected ) {
      return;
    }
    MultiPurposeTimeWindowRunnable mptwr = new MultiPurposeTimeWindowRunnable(definition, MultiPurpose.Activate );
    try {
      RMIClusterProviderTools.execute(clusterInstance, clusteredTCMInterfaceId, mptwr );
    } catch (InvalidIDException e) {
      handleInvalidIDException(e);
    } catch( XynaException e ) {
      //kann hier nicht vorkommen
      throw new RuntimeException(e);
    }
  }


  public void deactivateTimeWindow(String name, boolean force) throws XPRC_TimeWindowStillUsedException {
    if( ! rmiConnected ) {
      return;
    }
    MultiPurpose purpose = force ? MultiPurpose.ForceDeactivate : MultiPurpose.Deactivate;
    MultiPurposeTimeWindowRunnable mptwr = new MultiPurposeTimeWindowRunnable(name, purpose );
    try {
      RMIClusterProviderTools.execute(clusterInstance, clusteredTCMInterfaceId, mptwr );
    } catch (InvalidIDException e) {
      handleInvalidIDException(e);
    } catch( XPRC_TimeWindowStillUsedException e ) {
      throw e;
    } catch( XynaException e ) {
      //kann hier nicht vorkommen
      throw new RuntimeException(e);
    }
  }

  public void undoDeactivateTimeWindow(String name) throws PersistenceLayerException, XPRC_TimeWindowNotFoundInDatabaseException {
    if( ! rmiConnected ) {
      return;
    }
    MultiPurposeTimeWindowRunnable mptwr = new MultiPurposeTimeWindowRunnable(name, MultiPurpose.UndoDeactivate );
    try {
      RMIClusterProviderTools.execute(clusterInstance, clusteredTCMInterfaceId, mptwr );
    } catch (InvalidIDException e) {
      handleInvalidIDException(e);
    } catch (PersistenceLayerException e) {
      throw e;
    } catch( XPRC_TimeWindowNotFoundInDatabaseException e ) {
      throw e;
    } catch( XynaException e ) {
      //kann hier nicht vorkommen
      throw new RuntimeException(e);
    }
  }

  
  private void handleInvalidIDException(InvalidIDException e) {
    if (clusteredTCMInterfaceId == RMI_IS_CLOSED) {
      //closed rmi        
    } else {
      //nicht erwartet
      throw new RuntimeException(e);
    }
  }
  
  private static enum MultiPurpose {
    Activate, Deactivate, ForceDeactivate, UndoDeactivate;
  }

  private static class MultiPurposeTimeWindowRunnable implements RMIRunnableNoResult<TCMInterface, XynaException> {
    
    private String name;
    private TimeConstraintWindowDefinition definition;
    private MultiPurpose multiPurpose;

    public MultiPurposeTimeWindowRunnable(String name, MultiPurpose multiPurpose) {
      this.name = name;
      this.multiPurpose = multiPurpose;
    }

    public MultiPurposeTimeWindowRunnable(TimeConstraintWindowDefinition definition, MultiPurpose multiPurpose) {
      this.definition = definition;
      this.multiPurpose = multiPurpose;
    }

    public void execute(TCMInterface clusteredInterface) throws RemoteException, XynaException {
      switch (multiPurpose) {
        case Activate:
          if( name != null ) {
            clusteredInterface.activateTimeWindow(name);
          } else {
            clusteredInterface.activateTimeWindow(definition);
          }
          break;
        case Deactivate:
          clusteredInterface.deactivateTimeWindow(name, false);
          break;
        case ForceDeactivate:
          clusteredInterface.deactivateTimeWindow(name, true);
          break;
        case UndoDeactivate:
          clusteredInterface.undoDeactivateTimeWindow(name);
          break;
      }
    }

  }

}
