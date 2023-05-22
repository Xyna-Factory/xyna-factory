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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.utils.misc.CompensatingStack;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_DuplicateTimeWindowNameException;
import com.gip.xyna.xprc.exceptions.XPRC_TimeWindowNotFoundInDatabaseException;
import com.gip.xyna.xprc.exceptions.XPRC_TimeWindowRemoteManagementException;
import com.gip.xyna.xprc.exceptions.XPRC_TimeWindowStillUsedException;
import com.gip.xyna.xprc.xsched.timeconstraint.cluster.TCMLocalImpl;
import com.gip.xyna.xprc.xsched.timeconstraint.cluster.TCMRemoteProxyImpl;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeConstraintWindow;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeConstraintWindowDefinition;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeConstraintWindowStorable;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeConstraintWindowStorableQueries;


/**
 * TimeConstraintManagementSteps ist eine Sammlung von kleinen Hilfsklassen f�r das TimeConstraintManagement,
 * damit das Hinzuf�gen, �ndern und L�schen von TimeContraintWindows sicher durchgef�hrt werden kann und 
 * im Fehlerfall die n�tigen Kompensationen durchgef�hrt werden.
 *
 */
public class TimeConstraintManagementSteps {

  public static class AddTimeWindow extends CompensatingStack {
    
    private TCMLocalImpl tcmLocal;
    private TCMRemoteProxyImpl tcmRemoteProxy;
    private TimeConstraintWindowStorableQueries tcwsQueries;

    public AddTimeWindow(TCMLocalImpl tcmLocal, TCMRemoteProxyImpl tcmRemoteProxy, TimeConstraintWindowStorableQueries tcwsQueries) {
      this.tcmLocal = tcmLocal;
      this.tcmRemoteProxy = tcmRemoteProxy;
      this.tcwsQueries = tcwsQueries;
    }
    
    public void addToDB(TimeConstraintWindow timeWindow) throws PersistenceLayerException, XPRC_DuplicateTimeWindowNameException {
      AddToDB addToDB = new AddToDB(tcmLocal,tcwsQueries,timeWindow);
      addStep(addToDB);
      addToDB.execute();
    }

    public void remoteActivate(TimeConstraintWindow timeWindow) throws XPRC_TimeWindowRemoteManagementException {
      RemoteActivate remoteActivate = new RemoteActivate(tcmRemoteProxy,timeWindow);
      addStep(remoteActivate);
      remoteActivate.execute();
    }

    public void localActivate(TimeConstraintWindow timeWindow) {
      LocalActivate localActivate = new LocalActivate(tcmLocal,timeWindow);
      addStep(localActivate);
      localActivate.execute();
    }
   
  }

  public static class RemoveTimeWindow extends CompensatingStack {
    
    private TCMLocalImpl tcmLocal;
    private TCMRemoteProxyImpl tcmRemoteProxy;
    private TimeConstraintWindowStorableQueries tcwsQueries;

    public RemoveTimeWindow(TCMLocalImpl tcmLocal, TCMRemoteProxyImpl tcmRemoteProxy, TimeConstraintWindowStorableQueries tcwsQueries) {
      this.tcmLocal = tcmLocal;
      this.tcmRemoteProxy = tcmRemoteProxy;
      this.tcwsQueries = tcwsQueries;
    }

    public void localDeactivate(String name, boolean force) throws XPRC_TimeWindowStillUsedException {
      LocalDeactivate localDeactivate = new LocalDeactivate(tcmLocal,name,force);
      addStep(localDeactivate);
      localDeactivate.execute();
    }

    public void remoteDeActivate(String name, boolean force) throws XPRC_TimeWindowStillUsedException {
      RemoteDeactivate remoteDeactivate = new RemoteDeactivate(tcmRemoteProxy,name,force);
      addStep(remoteDeactivate);
      remoteDeactivate.execute();
    }

    public void remove(String name) throws PersistenceLayerException {
      RemoveFromDB removeFromDB = new RemoveFromDB(tcwsQueries,name);
      addStep(removeFromDB);
      removeFromDB.execute();
    }
    
  }

  public static class ChangeTimeWindow extends CompensatingStack {
    
    private TCMLocalImpl tcmLocal;
    private TCMRemoteProxyImpl tcmRemoteProxy;
    private TimeConstraintWindowStorableQueries tcwsQueries;

    public ChangeTimeWindow(TCMLocalImpl tcmLocal, TCMRemoteProxyImpl tcmRemoteProxy, TimeConstraintWindowStorableQueries tcwsQueries) {
      this.tcmLocal = tcmLocal;
      this.tcmRemoteProxy = tcmRemoteProxy;
      this.tcwsQueries = tcwsQueries;
    }

    public void replaceInDB(TimeConstraintWindow timeWindow) throws PersistenceLayerException, XPRC_TimeWindowNotFoundInDatabaseException {
      ReplaceInDB replaceInDB = new ReplaceInDB(tcmLocal,tcwsQueries,timeWindow);
      addStep(replaceInDB);
      replaceInDB.execute();
    }

    public void remoteActivateWithoutCompensation(TimeConstraintWindow timeWindow) throws XPRC_TimeWindowRemoteManagementException {
      RemoteActivate remoteActivate = new RemoteActivate(tcmRemoteProxy,timeWindow);
      remoteActivate.execute();
    }

    public void localActivateWithoutCompensation(TimeConstraintWindow timeWindow) {
      LocalActivate localActivate = new LocalActivate(tcmLocal,timeWindow);
      localActivate.execute();
    }

    public void compensateRemoteActivate(String name) {
      try {
        tcmRemoteProxy.activateTimeWindow(name);
      } catch (PersistenceLayerException e) {
        handleExceptionInCompensate("compensateRemoteActivate", e);
      } catch (XPRC_TimeWindowNotFoundInDatabaseException e) {
        handleExceptionInCompensate("compensateRemoteActivate",e);
      }
    }

    public void compensateLocalActivate(String name) {
      try {
        tcmLocal.activateTimeWindow(name);
      } catch (PersistenceLayerException e) {
        handleExceptionInCompensate("compensateLocalActivate",e);
      } catch (XPRC_TimeWindowNotFoundInDatabaseException e) {
        handleExceptionInCompensate("compensateLocalActivate",e);
      }
    }
    
  }

 
  private static class AddToDB implements CompensatingStack.CompensatableStep {
    private static Logger logger = CentralFactoryLogging.getLogger(AddToDB.class);
    private TCMLocalImpl tcmLocal;
    private TimeConstraintWindowStorableQueries tcwsQueries;
    private TimeConstraintWindow timeWindow;
    private String name;
    private List<TimeConstraintWindowStorable> storables;
    private boolean existingInDB = false;
    
    public AddToDB(TCMLocalImpl tcmLocal, TimeConstraintWindowStorableQueries tcwsQueries, TimeConstraintWindow timeWindow) {
      this.tcmLocal = tcmLocal;
      this.tcwsQueries = tcwsQueries;
      this.timeWindow = timeWindow;
      this.name = timeWindow.getName();
    }

    public void execute() throws XPRC_DuplicateTimeWindowNameException, PersistenceLayerException {
      storables = timeWindow.toStorables();
      ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
      try {
        List<TimeConstraintWindowStorable> existing = tcwsQueries.loadAllByName(con, name);
        if( existing != null && ! existing.isEmpty()) {
          existingInDB = true;
          throw new XPRC_DuplicateTimeWindowNameException(name);
        }
        if( timeWindow.isPersistent() ) {
          con.persistCollection(storables);
          con.commit();
        }
      } finally {
        finallyClose(con);
      }
    }
    
    public void compensate() throws PersistenceLayerException, XPRC_TimeWindowNotFoundInDatabaseException {
      if( existingInDB ) {
        //Dies ist hier kein Rollback:
        //Wenn wider Erwarten ein TimeWindow nur in der DB und nicht auch im TimeConstraintManagement
        //existiert, wird dieses nun im TimeConstraintManagement erg�nzt.
        tcmLocal.activateTimeWindow(name);
      }
      if( timeWindow.isPersistent() ) {
        //Rollback des DB-Inserts
        ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
        try {
          con.delete(storables);
          con.commit();
        } finally {
          finallyClose(con);
        }
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
  
  private static class RemoveFromDB implements CompensatingStack.CompensatableStep {
    private static Logger logger = CentralFactoryLogging.getLogger(AddToDB.class);
    private TimeConstraintWindowStorableQueries tcwsQueries;
    private String name;
    private List<TimeConstraintWindowStorable> storables;
    
    public RemoveFromDB(TimeConstraintWindowStorableQueries tcwsQueries, String name) {
      this.tcwsQueries = tcwsQueries;
      this.name = name;
    }

    public void execute() throws PersistenceLayerException {
      ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
      try {
        storables = tcwsQueries.loadAllByName(con, name);
        con.delete(storables);
        con.commit();
      } finally {
        finallyClose(con);
      }
    }
    
    public void compensate() throws PersistenceLayerException {
      ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
      try {
        con.persistCollection(storables);
        con.commit();
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

  private static class ReplaceInDB implements CompensatingStack.CompensatableStep {
    private static Logger logger = CentralFactoryLogging.getLogger(AddToDB.class);
    private TimeConstraintWindowStorableQueries tcwsQueries;
    private TimeConstraintWindow timeWindow;
    private String name;
    private List<TimeConstraintWindowStorable> newStorables;
    private List<TimeConstraintWindowStorable> existingStorables;
    
    public ReplaceInDB(TCMLocalImpl tcmLocal, TimeConstraintWindowStorableQueries tcwsQueries, TimeConstraintWindow timeWindow) {
      this.tcwsQueries = tcwsQueries;
      this.timeWindow = timeWindow;
      this.name = timeWindow.getName();
    }

    public void execute() throws XPRC_TimeWindowNotFoundInDatabaseException, PersistenceLayerException {
      ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
      try {
        existingStorables = tcwsQueries.loadAllByName(con, name);
        if( existingStorables == null || existingStorables.isEmpty() ) {
          throw new XPRC_TimeWindowNotFoundInDatabaseException(name);
        }
        con.delete(existingStorables);
        newStorables = timeWindow.toStorables();
        con.persistCollection(newStorables);
        con.commit();
      } finally {
        finallyClose(con);
      }
    }
    
    public void compensate() throws PersistenceLayerException {
      ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
      try {
        con.delete(newStorables);
        con.persistCollection(existingStorables);
        con.commit();
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

  
  private static class RemoteActivate implements CompensatingStack.CompensatableStep {
    private TCMRemoteProxyImpl tcmRemoteProxy;
    private TimeConstraintWindow timeWindow;
    
    public RemoteActivate(TCMRemoteProxyImpl tcmRemoteProxy, TimeConstraintWindow timeWindow) {
      this.tcmRemoteProxy = tcmRemoteProxy;
      this.timeWindow = timeWindow;
    }

    public void execute() throws XPRC_TimeWindowRemoteManagementException {
      try {
        if( timeWindow.isPersistent() ) {
          tcmRemoteProxy.activateTimeWindow(timeWindow.getName());
        } else {
          tcmRemoteProxy.activateTimeWindow(timeWindow.getDefinition());
        }
      } catch( Exception e ) {
        throw new XPRC_TimeWindowRemoteManagementException(timeWindow.getName(), e.getMessage(), e);
      }
    }

    public void compensate() throws XPRC_TimeWindowStillUsedException {
      //remote deaktivieren
      tcmRemoteProxy.deactivateTimeWindow(timeWindow.getName(),true);
    }
    
  }

  private static class LocalActivate implements CompensatingStack.CompensatableStep {
    private TCMLocalImpl tcmLocal;
    private TimeConstraintWindow timeWindow;

    public LocalActivate(TCMLocalImpl tcmLocal, TimeConstraintWindow timeWindow) {
      this.tcmLocal = tcmLocal;
      this.timeWindow = timeWindow;
    }

    public void execute() {
      tcmLocal.activateTimeWindow(timeWindow);
    }
    
    public void compensate() throws XPRC_TimeWindowStillUsedException {
      String name = timeWindow.getName();
      //lokal deaktivieren
      tcmLocal.deactivateTimeWindow(name,true);
    }
    
  }
  
  
  private static class LocalDeactivate implements CompensatingStack.CompensatableStep {
    private static Logger logger = CentralFactoryLogging.getLogger(AddToDB.class);
    private TCMLocalImpl tcmLocal;
    private String name;
    private boolean force;

    public LocalDeactivate(TCMLocalImpl tcmLocal, String name, boolean force) {
      this.tcmLocal = tcmLocal;
      this.name = name;
      this.force = force;
    }

    public void execute() throws XPRC_TimeWindowStillUsedException {
      tcmLocal.deactivateTimeWindow(name, force);
    }

    public void compensate() {
      try {
        tcmLocal.undoDeactivateTimeWindow(name);
      } catch (PersistenceLayerException e) {
        logger.warn("undo of LocalDectivate "+name+" failed ", e );
      } catch (XPRC_TimeWindowNotFoundInDatabaseException e) {
        logger.warn("undo of LocalDectivate "+name+" failed ", e );
      }
    }
    
  }
  
  private static class RemoteDeactivate implements CompensatingStack.CompensatableStep {
    private static Logger logger = CentralFactoryLogging.getLogger(AddToDB.class);
    private TCMRemoteProxyImpl tcmRemoteProxy;
    private String name;
    private boolean force;

    public RemoteDeactivate(TCMRemoteProxyImpl tcmRemoteProxy, String name, boolean force) {
      this.tcmRemoteProxy = tcmRemoteProxy;
      this.name = name;
      this.force = force;
    }

    public void execute() throws XPRC_TimeWindowStillUsedException {
      tcmRemoteProxy.deactivateTimeWindow(name, force);
    }

    public void compensate() {
      try {
        tcmRemoteProxy.undoDeactivateTimeWindow(name);
      } catch (PersistenceLayerException e) {
        logger.warn("undo of RemoteDeactivate "+name+" failed ", e );
      } catch (XPRC_TimeWindowNotFoundInDatabaseException e) {
        logger.warn("undo of RemoteDeactivate "+name+" failed ", e );
      }
    }
    
  }

  public static class RecreateTimeWindows {
    private static Logger logger = CentralFactoryLogging.getLogger(RecreateTimeWindows.class);
    
    private TCMLocalImpl tcmLocal;

    public RecreateTimeWindows(TCMLocalImpl tcmLocal) {
      this.tcmLocal = tcmLocal;
    }

    /**
     * @return
     * @throws PersistenceLayerException 
     */
    public int recreateTimeWindows() throws PersistenceLayerException {
      ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
      try {
        Collection<TimeConstraintWindowStorable> all = con.loadCollection(TimeConstraintWindowStorable.class);
        if( all.isEmpty() ) {
          return 0;
        }
        Map<String,ArrayList<TimeConstraintWindowStorable>> allMap = CollectionUtils.group( all, new GetName() );
        int count = 0;
        for( Map.Entry<String,ArrayList<TimeConstraintWindowStorable>> entry : allMap.entrySet() ) {
          String name = entry.getKey();
          TimeConstraintWindowDefinition tcwd = TimeConstraintWindowDefinition.fromStorables(name, entry.getValue() );
          if( logger.isDebugEnabled() ) {
            logger.debug( "Read following definition for TimeWindow "+name+": "+tcwd.getTimeWindowDefinition() );
          }
          TimeConstraintWindow tcw = new TimeConstraintWindow(tcwd);
          tcmLocal.activateTimeWindow(tcw);
          ++count;
        }

        return count;

      } finally {
        finallyClose(con);
      }
    }

    private static class GetName implements Transformation<TimeConstraintWindowStorable,String> {
      public String transform(TimeConstraintWindowStorable from) {
        return from.getName();
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

}
