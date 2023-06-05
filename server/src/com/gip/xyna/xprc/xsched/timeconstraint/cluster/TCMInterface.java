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

import java.rmi.Remote;
import java.rmi.RemoteException;

import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_TimeWindowNotFoundInDatabaseException;
import com.gip.xyna.xprc.exceptions.XPRC_TimeWindowStillUsedException;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeConstraintWindowDefinition;


public interface TCMInterface extends Remote {

  
  /**
   * TimeWindow soll zum Schedulen verwendet werden
   * ein evtl. bereits bestehendes TimeWindow muss ersetzt werden, daher muss in jedem Fall aus DB gelesen werden 
   * @param name
   * @throws RemoteException
   * @throws PersistenceLayerException
   * @throws XPRC_TimeWindowNotFoundInDatabaseException
   */
  public void activateTimeWindow(String name) throws RemoteException, PersistenceLayerException, XPRC_TimeWindowNotFoundInDatabaseException;
  
  
  /**
   * TimeWindow darf nicht mehr zum Schedulen verwendet werden
   * Bei Force == false wird zuerst geprüft, ob TimeWindow noch in Verwendung ist;
   * wenn ja, wird XPRC_Scheduler_TimeWindowStillUsedException geworfen
   * Aus DB wird nicht gelöscht
   * @param name
   * @param force
   * @throws RemoteException
   * @throws XPRC_TimeWindowStillUsedException
   */
  public void deactivateTimeWindow(String name, boolean force) throws RemoteException, XPRC_TimeWindowStillUsedException;
  
  
  /**
   * Undo der Methode deactivateTimeWindow; 
   * liest im Gegensatz zu activateTimeWindow muss ein bestehendes TimeWindow nicht ersetzt werden 
   * @param name
   * @throws RemoteException
   * @throws PersistenceLayerException 
   * @throws XPRC_TimeWindowNotFoundInDatabaseException 
   */
  public void undoDeactivateTimeWindow(String name) throws RemoteException, PersistenceLayerException, XPRC_TimeWindowNotFoundInDatabaseException;


  
  public void activateTimeWindow(TimeConstraintWindowDefinition definition) throws RemoteException;
  
  
}
