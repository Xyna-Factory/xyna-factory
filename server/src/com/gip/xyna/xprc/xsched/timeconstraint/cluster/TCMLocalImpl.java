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

import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_TimeWindowNotFoundInDatabaseException;
import com.gip.xyna.xprc.exceptions.XPRC_TimeWindowStillUsedException;
import com.gip.xyna.xprc.xsched.timeconstraint.AllTimeConstraintWindows;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeConstraintWindow;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeConstraintWindowDefinition;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeConstraintWindowStorable;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeConstraintWindowStorableQueries;


public class TCMLocalImpl implements TCMInterface {

  private static Logger logger = CentralFactoryLogging.getLogger(TCMLocalImpl.class);
  
  private AllTimeConstraintWindows allTimeConstraintWindows;
  private TimeConstraintWindowStorableQueries tcwsQueries;

  public TCMLocalImpl(AllTimeConstraintWindows allTimeConstraintWindows,
                      TimeConstraintWindowStorableQueries tcwsQueries) {
    this.allTimeConstraintWindows = allTimeConstraintWindows;
    this.tcwsQueries = tcwsQueries;
  }

  public void activateTimeWindow(String name) throws PersistenceLayerException, XPRC_TimeWindowNotFoundInDatabaseException {
    //TimeWindow aus DB lesen
    TimeConstraintWindow timeWindow = readTimeConstraintWindowFromDB(name);
    //und in allTimeConstraintWindows eintragen
    activateTimeWindow(timeWindow);
  }
  
  public void activateTimeWindow(TimeConstraintWindowDefinition definition) {
    //TimeWindow direkt erstellen
    TimeConstraintWindow timeWindow = new TimeConstraintWindow(definition);
    //und in allTimeConstraintWindows eintragen
    activateTimeWindow(timeWindow);
  }

  public void activateTimeWindow(TimeConstraintWindow timeWindow) {
    //entweder neu in allTimeConstraintWindows eintragen
    boolean added = allTimeConstraintWindows.addTimeWindow(timeWindow);
    if( !added ) {
      //oder existierendes Zeitfenster ersetzen 
      allTimeConstraintWindows.replaceTimeWindow(timeWindow);
    }
  }
 
  public void deactivateTimeWindow(String name, boolean force) throws XPRC_TimeWindowStillUsedException {
    TimeConstraintWindow tw = allTimeConstraintWindows.getLockedTimeWindow(name);
    try {
      if( tw == null ) {
        return; //existiert nicht == deaktiviert
      } 
      if( !force ) {
        checkTimeWindowInUse(tw);
      }
      allTimeConstraintWindows.removeTimeWindow(name);
    } finally {
      if( tw != null ) {
        tw.unlock();
      }
    }
  }
  
  public void undoDeactivateTimeWindow(String name) throws PersistenceLayerException, XPRC_TimeWindowNotFoundInDatabaseException {
    //TimeWindow aus DB lesen
    TimeConstraintWindow timeWindow = readTimeConstraintWindowFromDB(name);
    //und in allTimeConstraintWindows eintragen
    allTimeConstraintWindows.addTimeWindow(timeWindow);
  }
  
  private void checkTimeWindowInUse(TimeConstraintWindow tw) throws XPRC_TimeWindowStillUsedException {
    //Prüfung, ob Zeitfenster derzeit verwendet wird 
    //(nur möglich, wenn Zeitfenster geschlossen ist und Aufträge darauf warten)
    //TODO wie prüfen wenn offen?
    if( tw.hasWaitingOrders() ) {
      throw new XPRC_TimeWindowStillUsedException(tw.getName());
    }
  }

  private TimeConstraintWindow readTimeConstraintWindowFromDB(String name) throws PersistenceLayerException, XPRC_TimeWindowNotFoundInDatabaseException {
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
    try { 
      List<TimeConstraintWindowStorable> storables = tcwsQueries.loadAllByName(con, name);
      if( storables.isEmpty() ) {
        throw new XPRC_TimeWindowNotFoundInDatabaseException(name);
      }
      TimeConstraintWindowDefinition tcwd = TimeConstraintWindowDefinition.fromStorables(name, storables);
      if( logger.isDebugEnabled() ) {
        logger.debug( "Read following definition for TimeWindow "+name+": "+tcwd.getTimeWindowDefinition() );
      }
      return new TimeConstraintWindow(tcwd);
    } finally {
      finallyClose(con);
    }
  }
  
  private void finallyClose(ODSConnection con) {
    if( con != null ) {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Failed to close connection", e);
      }
    }
  }
  
}
