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
package com.gip.xyna.xprc.xsched.timeconstraint;

import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_DuplicateTimeWindowNameException;
import com.gip.xyna.xprc.exceptions.XPRC_TimeWindowNotFoundInDatabaseException;
import com.gip.xyna.xprc.exceptions.XPRC_TimeWindowRemoteManagementException;
import com.gip.xyna.xprc.exceptions.XPRC_TimeWindowStillUsedException;
import com.gip.xyna.xprc.xsched.SchedulingData;
import com.gip.xyna.xprc.xsched.scheduling.SchedulingOrder;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeConstraintWindowDefinition;


public interface TimeConstraintManagementInterface {

  /**
   * Muss der Auftrag auf StartTime warten? (geschlossenes Zeitfenster, EarliestStartTimestamp gesetzt)
   * @param schedulingData
   * @return true, wenn Auftrag warten muss
   */
  public boolean hasToWaitForStartTime(SchedulingData schedulingData);

  /**
   * Anlegen eines neuen Zeitfensters
   */
  public void addTimeWindow(TimeConstraintWindowDefinition timeWindow) throws XPRC_DuplicateTimeWindowNameException, PersistenceLayerException, XPRC_TimeWindowNotFoundInDatabaseException, XPRC_TimeWindowRemoteManagementException;

  /**
   * Entfernen eines Zeitfensters
   */
  public void removeTimeWindow(String name, boolean force) throws PersistenceLayerException, XPRC_TimeWindowStillUsedException;

  /**
   * Ersetzen des Zeitfensters durch die neuen Daten
   */
  public void changeTimeWindow(TimeConstraintWindowDefinition definition) throws PersistenceLayerException, XPRC_TimeWindowNotFoundInDatabaseException, XPRC_TimeWindowRemoteManagementException;

  
  /**
   * Eintragen der Order in die TimeConstraint-�berwachung
   */
  public void addWaitingOrder(SchedulingOrder so, boolean checkWaiting);

}
