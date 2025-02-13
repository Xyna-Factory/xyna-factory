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

package com.gip.xyna.xprc.xsched.capacities;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

import com.gip.xyna.xprc.xsched.CapacityInformation;
import com.gip.xyna.xprc.xsched.ExtendedCapacityUsageInformation;
import com.gip.xyna.xprc.xsched.scheduling.CapacityDemand;



public interface ClusteredCapacityManagementInterface extends Remote {

  public Map<String, CapacityInformation> listLocalCapacities() throws RemoteException;

  public CapacityInformation getLocalCapacityInformation( String capName ) throws RemoteException;
  
  public ExtendedCapacityUsageInformation getLocalExtendedCapacityUsageInformation() throws RemoteException;
  
  public void refreshLocalCapacityCache( String capName ) throws RemoteException;
  
  public void moveAllFreeCapacitiesToBinding( String capName, int binding ) throws RemoteException;

  public void communicateLocalDemand(int binding, List<CapacityDemand> demand) throws RemoteException;
  
}
