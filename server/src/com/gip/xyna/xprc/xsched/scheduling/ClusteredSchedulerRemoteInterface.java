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
package com.gip.xyna.xprc.xsched.scheduling;



import java.rmi.Remote;
import java.rmi.RemoteException;

import com.gip.xyna.xprc.xpce.ordersuspension.ResumeTarget;
import com.gip.xyna.xprc.xsched.SchedulingData;
import com.gip.xyna.xprc.xsched.XynaScheduler.ChangeSchedulingParameterStatus;



public interface ClusteredSchedulerRemoteInterface extends Remote {

  public void notifySchedulerRemotely() throws RemoteException;

  public Boolean resumeOrderRemotely(int binding, ResumeTarget target) throws RemoteException;
  
  public ChangeSchedulingParameterStatus changeSchedulingParameterRemotely(Long orderId, SchedulingData schedulingData, boolean replace) throws RemoteException;

}
