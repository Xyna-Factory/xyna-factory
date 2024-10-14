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

package com.gip.xyna.xprc.xpce.monitoring;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryPath;
import com.gip.xyna.utils.collections.maps.TimeoutMap;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.XynaFactoryManagement;
import com.gip.xyna.xfmg.exceptions.XFMG_NoSuchRevision;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xods.XynaFactoryManagementODS;
import com.gip.xyna.xfmg.xods.configuration.Configuration;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeManagement.OrderTypeUpdates;
import com.gip.xyna.xnwh.exceptions.XNWH_GeneralPersistenceLayerException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Connection;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaOrderServerExtension.ExecutionType;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.exceptions.XPRC_DESTINATION_NOT_FOUND;
import com.gip.xyna.xprc.exceptions.XPRC_ExecutionDestinationMissingException;
import com.gip.xyna.xprc.exceptions.XPRC_INVALID_MONITORING_TYPE;
import com.gip.xyna.xprc.xfractwfe.FractalStepHandlerManager;
import com.gip.xyna.xprc.xfractwfe.base.FractalProcessStep;
import com.gip.xyna.xprc.xfractwfe.base.Handler;
import com.gip.xyna.xprc.xfractwfe.base.XynaProcess;
import com.gip.xyna.xprc.xpce.XynaProcessCtrlExecution;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xpce.monitoring.EngineSpecificStepHandlerManager.DynamicStepHandlerFactory;
import com.gip.xyna.xprc.xpce.parameterinheritance.ParameterInheritanceManagement;
import com.gip.xyna.xprc.xpce.parameterinheritance.ParameterInheritanceManagement.ParameterType;
import com.gip.xyna.xprc.xpce.parameterinheritance.rules.InheritanceRule;
import com.gip.xyna.xprc.xpce.parameterinheritance.rules.XynaPropertyInheritanceRule;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive.ProcessStepHandlerType;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase;


public class MonitoringDispatcher extends FunctionGroup {
  
  public static final String DEFAULT_NAME = "MonitoringDispatcher";
  public static final Integer DEFAULT_MONITORING_LEVEL = MonitoringCodes.ERROR_MONITORING;
  //public static final int FUTUREEXECUTION_ID = XynaFactory.getInstance().getFutureExecution().nextId();

  private HashMap<DestinationKey, MonitoringDispatcherStorable> destinations;

  private final ReentrantLock codesLock = new ReentrantLock();

  private volatile boolean isInitialized = false;
  
  private ODS ods;

  static {
    ArrayList<XynaFactoryPath> dependencies = new ArrayList<XynaFactoryPath>();
    // wait for the configuration class to be loaded to be able to read properties for the default configuration
    dependencies.add(new XynaFactoryPath(XynaFactoryManagement.class, XynaFactoryManagementODS.class,
                                         Configuration.class));
    // wait for the step handler manager to be loaded to be able to pass step handlers to it
    dependencies.add(new XynaFactoryPath(XynaProcessing.class, XynaProcessCtrlExecution.class, FractalStepHandlerManager.class));
       
    //wait for the DependencyRegister to add a dependency on
    dependencies.add(new XynaFactoryPath(XynaFactoryManagement.class, XynaFactoryControl.class,
                                         DependencyRegister.class));
    addDependencies(MonitoringDispatcher.class, dependencies);
  }

  public MonitoringDispatcher() throws XynaException {
    ods = ODSImpl.getInstance();
  }


  /**
   * Initialization: Create the internal HashMaps and locks, read from the property store for default configuration and
   * read the destinations from the persistence layer.
   */
  public void init() throws XynaException {

    if (logger.isInfoEnabled()) {
      logger.info("Initializing FunctionGroup " + getClass().getSimpleName());
    }
    ods.registerStorable(MonitoringDispatcherStorable.class);
    destinations = new HashMap<DestinationKey, MonitoringDispatcherStorable>();
    isInitialized = true; //shutdown ist nun durchführbar 
    
    XynaFactory
        .getInstance()
        .getFactoryManagementPortal()
        .getXynaFactoryControl()
        .getDependencyRegister()
        .addDependency(DependencySourceType.XYNAPROPERTY, XynaProperty.XYNA_DEFAULT_MONITORING_LEVEL.getPropertyName(),
                       DependencySourceType.XYNAFACTORY, DEFAULT_NAME);
    XynaFactory
        .getInstance()
        .getFactoryManagementPortal()
        .getXynaFactoryControl()
        .getDependencyRegister()
        .addDependency(DependencySourceType.XYNAPROPERTY, XynaProperty.MONITORING_DIRECTPERSISTENCE,
                       DependencySourceType.XYNAFACTORY, DEFAULT_NAME);

    
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask(MonitoringDispatcher.class, "MonitoringDispatcher").
      after(WorkflowDatabase.FUTURE_EXECUTION_ID).
      after(RevisionManagement.class).
      before( XynaProcessing.FUTUREEXECUTIONID_ORDER_EXECUTION ).
      execAsync( new Runnable() { public void run() { initHandler(); } });
  }
  
  private void initHandler() {
            codesLock.lock();
            try {
              loadDestinations();

              XynaFactory.getInstance().getProcessing().getWorkflowEngine().getStepHandlerManager().addFactory(DEFAULT_NAME, new DynamicStepHandlerFactory() {
                
                private final Handler preHandler = new AuditDataHandler(ProcessStepHandlerType.PREHANDLER);
                private final Handler postHandler = new AuditDataHandler(ProcessStepHandlerType.POSTHANDLER);
                private final Handler errorHandler = new AuditDataHandler(ProcessStepHandlerType.ERRORHANDLER);
                private final Handler preCompensationHandler = new AuditDataHandler(ProcessStepHandlerType.PRECOMPENSATION);
                private final Handler postCompensationHandler = new AuditDataHandler(ProcessStepHandlerType.POSTCOMPENSATION);
                
                public Handler createHandler(XynaOrderServerExtension xose, ProcessStepHandlerType ht) {
                  try {
                    DestinationValue dv = obtainExecutionDestination(xose.getDestinationKey());
                    if (dv.getDestinationType() == ExecutionType.XYNA_FRACTAL_WORKFLOW &&
                        xose.getMonitoringCode() > 15) {
                      switch (ht) {
                        case PREHANDLER :
                          return preHandler;
                        case POSTHANDLER :
                          return postHandler;
                        case ERRORHANDLER:
                          return errorHandler;
                        case PRECOMPENSATION:
                          return preCompensationHandler;
                        case POSTCOMPENSATION :
                          return postCompensationHandler;
                        default :
                          return null;
                      }
                    } else {
                      return null;
                    }
                  } catch (XPRC_DESTINATION_NOT_FOUND e) {
                    logger.debug("Failed to resolve destinationValue for " + xose.getDestinationKey(), e);
                    return null;
                  }
                }

                private DestinationValue obtainExecutionDestination(DestinationKey dk) throws XPRC_DESTINATION_NOT_FOUND {
                  return XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution()
                                  .getExecutionDestination(dk);
                }
                
                public Object extractCachingParameter(XynaOrderServerExtension xose) {
                  return xose.getMonitoringCode() == null ? 0 : xose.getMonitoringCode();
                }
              });
            } catch (PersistenceLayerException e) {
              throw new RuntimeException(e);
            } finally {
              codesLock.unlock();
            }
  }

  /**
   * Shuts down the monitoring dispatcher: Do not listen to property changes any more and store the destinations to the
   * persistence layer
   */
  public void shutdown() throws XynaException {
    if (!isInitialized) {
      return;
    }
    logger.info("Shutting down FunctionGroup " + getClass().getSimpleName());
    storeDestinations();
  }
  
  //propertyname -> lastlogmessage-timestamp. timeoutmap damit kein memoryleak entsteht
  private final TimeoutMap<String, Long> invalidPropertyValuesLogs = new TimeoutMap<>();
  
  /**
   * Sets monitoring settings for the passed XynaOrder according to specific configuration (per destination key) or
   * according to the default settings
   */
  public void dispatch(final XynaOrderServerExtension xo) throws XPRC_DESTINATION_NOT_FOUND {
    DestinationKey key = xo.getDestinationKey();

    // dont reassign a new monitoring code when dispatching again
    if (xo.monitoringLevelAlreadyDiscovered()) {
      return;
    }
    
    ParameterInheritanceManagement parameterInheritanceMgmt = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getParameterInheritanceManagement();
    InheritanceRule rule = parameterInheritanceMgmt.getPreferredMonitoringLevelRule(xo);

    try {
      if (rule == null || rule.getValueAsInt() == null) {
        xo.setMonitoringLevel(XynaProperty.XYNA_DEFAULT_MONITORING_LEVEL.get());
        if (logger.isDebugEnabled()) {
          logger
              .debug("No monitoring code found for order type " + key.getOrderType() + ", using default <" + xo.getMonitoringCode() + ">.");
        }
      } else {
        Integer monitoringLevel = rule.getValueAsInt();
        if (logger.isDebugEnabled()) {
          logger.debug("Using monitoring code <" + monitoringLevel + "> for order type " + key.getOrderType());
        }
        xo.setMonitoringLevel(monitoringLevel);
      }
    } catch (NumberFormatException e) {
      if (rule instanceof XynaPropertyInheritanceRule) {
        String propertyName = ((XynaPropertyInheritanceRule) rule).getPropertyName();
        Long last = invalidPropertyValuesLogs.get(propertyName);
        if (last == null) {
          last = 0L;
        }
        long now = System.currentTimeMillis();
        long maxLogInterval = 3600L * 1000 * 6;
        if (now - last > maxLogInterval) {
          invalidPropertyValuesLogs.replace2(propertyName, now, 2 * maxLogInterval);
          logger.warn("Could not use value of Xyna Property " + propertyName + " as a Monitoring Level used by "
              + xo.getDestinationKey().getOrderType() + ", because it is not a number (logmessage will not repeat for some time).");
        }
        xo.setMonitoringLevel(XynaProperty.XYNA_DEFAULT_MONITORING_LEVEL.get());
      } else {
        throw e; //not expected
      }
    }

    xo.setMonitoringLevelAlreadyDiscovered(true);
  }


  private void updateArchive() throws PersistenceLayerException {
    if (isPersistenceDirect()) {
      try {
        storeDestinations();
      } catch (PersistenceLayerException e) {
        throw new XNWH_GeneralPersistenceLayerException(e.getMessage(), e);
      }
    }
  }


  private boolean isPersistenceDirect() {
    String persistence =
        XynaFactory.getInstance().getFactoryManagement().getProperty(XynaProperty.MONITORING_DIRECTPERSISTENCE);
    if (persistence != null) {
      try {
        return Boolean.parseBoolean(persistence);
      } catch (Throwable e) {
        return true;
      }
    } else {
      return true;
    }
  }


  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  /**
   * Get the currently defined monitoring level for the given destination key or null if not defined explicitly.
   * @param dk
   * @return the current monitoring level according to {@link MonitoringCodes}
   */
  public Integer getMonitoringLevel(final DestinationKey dk) {
    codesLock.lock();
    try {
      MonitoringDispatcherStorable resultStorable = destinations.get(dk);
      if (resultStorable == null) {
        return null;
      }
      return resultStorable.getMonitoringlevel();
    } finally {
      codesLock.unlock();
    }

  }


  private void setMonitoringLevel(final DestinationKey dk, final Integer code, final boolean save)
                  throws XPRC_INVALID_MONITORING_TYPE, PersistenceLayerException,
                  XPRC_ExecutionDestinationMissingException {

    Integer oldCode = getMonitoringLevel(dk);

    if (oldCode != null && oldCode.equals(code)) {
      logger.info("Monitoring level <" + code + "> already set for order type " + dk.getOrderType());
      return;
    }

    if (!(MonitoringCodes.getAllValidMonitoringLevels().contains(code))) {
      throw new XPRC_INVALID_MONITORING_TYPE(code);
    }

    Long revision;
    try {
      revision = XynaFactory.getInstance().getFactoryManagementPortal().getXynaFactoryControl().getRevisionManagement().getRevision(dk.getRuntimeContext());
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }
    if (oldCode == null) {
      oldCode = XynaProperty.XYNA_DEFAULT_MONITORING_LEVEL.get();
    }
    if (save) {
      codesLock.lock();
      try {
        destinations.put(dk, new MonitoringDispatcherStorable(dk, code, revision));
        updateArchive();
      } finally {
        codesLock.unlock();
      }
    }
    
    //Regel für das eigene Monitoringlevel entfernen
    ParameterInheritanceManagement parameterInheritanceMgmt = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getParameterInheritanceManagement();
    try {
      parameterInheritanceMgmt.removeInheritanceRule(ParameterType.MonitoringLevel, dk, null);
    } catch (XFMG_NoSuchRevision e) {
      throw new RuntimeException(e);
    }

    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderTypeManagement()
        .updateInCache(revision, dk.getOrderType(), OrderTypeUpdates.setMonitoringLevel(code));
  }


  public void setMonitoringLevel(DestinationKey dk, Integer code) throws XPRC_INVALID_MONITORING_TYPE, PersistenceLayerException, XPRC_ExecutionDestinationMissingException {
    setMonitoringLevel(dk, code, true);
  }


  public void removeMonitoringLevel(DestinationKey dk) throws PersistenceLayerException, XPRC_DESTINATION_NOT_FOUND {
    codesLock.lock();
    try {
      destinations.remove(dk);
      updateArchive();
    } finally {
      codesLock.unlock();
    }

    Long revision;
    try {
      revision = XynaFactory.getInstance().getFactoryManagementPortal().getXynaFactoryControl().getRevisionManagement()
          .getRevision(dk.getRuntimeContext());
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderTypeManagement()
        .updateInCache(revision, dk.getOrderType(), OrderTypeUpdates.setMonitoringLevel(null));
  }

  public Integer getDefaultMonitoringLevel() {
    return XynaProperty.XYNA_DEFAULT_MONITORING_LEVEL.get();
  }


  /**
   * Returns a copy of the monitoring level map. The result is not supposed to be modified since it is only a copy of
   * the cache
   */
  public Map<DestinationKey, Integer> getAllMonitoringLevels() {
    codesLock.lock();
    try {
      Map<DestinationKey, Integer> resultMap = new HashMap<DestinationKey, Integer>();
      for(Entry<DestinationKey, MonitoringDispatcherStorable> entry : destinations.entrySet()) {
        resultMap.put(entry.getKey(), entry.getValue().getMonitoringlevel());
      }
      return resultMap;
    } finally {
      codesLock.unlock();
    }
  }
  
  
  private void loadDestinations() throws PersistenceLayerException {
    Collection<MonitoringDispatcherStorable> list = null;
    
    Connection con = ods.openConnection(ODSConnectionType.HISTORY); //FIXME Falls Zugriff auf Datenbank, wegen Deadlocks in Verbindung mit dem umgebenden Javalock aufpassen
    try {
      list = con.loadCollection(MonitoringDispatcherStorable.class);
    } finally  {
      con.closeConnection();
    }
    destinations.clear();
    if(list != null) {
      RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      for(MonitoringDispatcherStorable entry : list) {
        RuntimeContext runtimeContext;
        try {
          runtimeContext = revisionManagement.getRuntimeContext(entry.getRevision());
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          logger.warn("Can't get runtimeContext for revision " + entry.getRevision(), e);
          continue;
        }
        DestinationKey key = new DestinationKey(entry.getOrderType(), runtimeContext);
        key.setCompensate(entry.getCompensate());
        destinations.put(key, entry);
      }
    }
  }
  
  private void storeDestinations() throws PersistenceLayerException {   
    Connection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      con.deleteAll(MonitoringDispatcherStorable.class);
      con.persistCollection(destinations.values());
      con.commit();
    } finally  {
      con.closeConnection();
    }
  }
  

  private static class AuditDataHandler extends Handler {
    
    private final static Logger logger = CentralFactoryLogging.getLogger(MonitoringDispatcher.class);
    private final ProcessStepHandlerType type;
    private final OrderArchive oa;

    public AuditDataHandler(ProcessStepHandlerType type) {
      this.type = type;
      oa = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive();
    }


    @Override
    public void handle(XynaProcess process, FractalProcessStep<?> pstep) {
      XynaOrderServerExtension xo = process.getCorrelatedXynaOrder();
      try {
        oa.updateAuditData(xo, pstep, type);
      } catch (XynaException e) {
        logger.error("problem updating database", e);
      }
    }

  }
  
  
  @Persistable(primaryKey= MonitoringDispatcherStorable.COL_ID, tableName = MonitoringDispatcherStorable.TABLENAME)
  public static class MonitoringDispatcherStorable extends Storable<MonitoringDispatcherStorable> {

  
    private static final long serialVersionUID = 7051229027026949381L;
    
    public static final String TABLENAME = "monitoringdispatcher";
    public static final String COL_ID = "id"; 
    public static final String COL_ORDERTYPE = "orderType";
    public static final String COL_COMPENSATE = "compensate";
    public static final String COL_MONITORINGLEVEL = "monitoringlevel";
    public static final String COL_REVISION = "revision";
  
    @Column(name = COL_ID)
    private Long id;
    
    @Column(name = COL_ORDERTYPE)
    private String orderType;
    
    @Column(name = COL_COMPENSATE)
    private Boolean compensate;
    
    @Column(name = COL_MONITORINGLEVEL)
    private Integer monitoringlevel;
    
    @Column(name = COL_REVISION)
    private Long revision = RevisionManagement.REVISION_DEFAULT_WORKSPACE;
    
    public MonitoringDispatcherStorable() {
    }
    
    
    public MonitoringDispatcherStorable(DestinationKey destinationKey, Integer monitoringlevel, Long revision) {
      this.id = XynaFactory.getInstance().getIDGenerator().getUniqueId();
      this.orderType = destinationKey.getOrderType();
      this.compensate = destinationKey.isCompensate();
      this.monitoringlevel = monitoringlevel;
      if (revision == null) {
        throw new IllegalArgumentException("Revision may not be null");
      }
      this.revision = revision;
    }
    
    public MonitoringDispatcherStorable(String orderType, Boolean compensate, Integer monitoringlevel, Long id, Long revision) {
      if(XynaFactory.getInstance().finishedInitialization()) {
        throw new RuntimeException("Unallowed call of constructor.");
      }
      this.id = id;
      this.orderType = orderType;
      this.compensate = compensate;
      this.monitoringlevel = monitoringlevel;
      if (revision == null) {
        throw new IllegalArgumentException("Revision may not be null");
      }
      this.revision = revision;
    }
    
    public String getOrderType() {
      return orderType;
    }

    public void setOrderType(String orderType) {
      this.orderType = orderType;
    }

    public Boolean getCompensate() {
      return compensate;
    }

    public void setCompensate(Boolean compensate) {
      this.compensate = compensate;
    }

    public Integer getMonitoringlevel() {
      return monitoringlevel;
    }

    public void setMonitoringlevel(Integer monitoringlevel) {
      this.monitoringlevel = monitoringlevel;
    }

    @Override
    public ResultSetReader<? extends MonitoringDispatcherStorable> getReader() {
      return new ResultSetReader<MonitoringDispatcherStorable>() {

        public MonitoringDispatcherStorable read(ResultSet rs)
            throws SQLException {
          MonitoringDispatcherStorable result = new MonitoringDispatcherStorable();
          result.id = rs.getLong(COL_ID);
          result.revision = rs.getLong(COL_REVISION);
          result.compensate = rs.getBoolean(COL_COMPENSATE);
          result.monitoringlevel = rs.getInt(COL_MONITORINGLEVEL);
          result.orderType = rs.getString(COL_ORDERTYPE);
          return result;
        }
      };
    }

    @Override
    public Object getPrimaryKey() {
      return id;
    }

    
    public Long getId() {
      return id;
    }

    public Long getRevision() {
      return revision;
    }

    
    @Override
    public <U extends MonitoringDispatcherStorable> void setAllFieldsFromData(U data) {
      MonitoringDispatcherStorable cast = data;
      orderType = cast.orderType;
      compensate = cast.compensate;
      monitoringlevel = cast.monitoringlevel;
      id = cast.id;
      revision = cast.revision;
    }
  }

}
