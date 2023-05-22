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

package com.gip.xyna.xprc.xpce.dispatcher;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryPath;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateManagementImpl;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.PredefinedXynaStatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath.StatisticsNodeTraversal;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath.StatisticsPathPart;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath.UnknownPathOnTraversalHandling;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_UnsupportedPersistenceLayerFeatureException;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedCommand;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xprc.MIAbstractionLayer;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.exceptions.XPRC_DESTINATION_NOT_FOUND;
import com.gip.xyna.xprc.xfractwfe.specialpurpose.synchronization.TimeoutSynchronizationJavaDestination;
import com.gip.xyna.xprc.xpce.XynaProcessCtrlExecution;
import com.gip.xyna.xprc.xpce.cleanup.CleanupDispatcher;
import com.gip.xyna.xprc.xpce.cleanup.XynaCleanup;
import com.gip.xyna.xprc.xpce.execution.ExecutionDispatcher;
import com.gip.xyna.xprc.xpce.execution.XynaExecution;
import com.gip.xyna.xprc.xpce.planning.PlanningDispatcher;
import com.gip.xyna.xprc.xpce.planning.XynaPlanning;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase;
import com.gip.xyna.xprc.xsched.ordercancel.CancelJavaDestination;
import com.gip.xyna.xprc.xsched.ordercancel.KillStuckProcessDestination;
import com.gip.xyna.xprc.xsched.ordersuspension.ResumeMultipleOrdersJavaDestination;
import com.gip.xyna.xprc.xsched.ordersuspension.ResumeOrderJavaDestination;
import com.gip.xyna.xprc.xsched.ordersuspension.SuspendAllOrdersJavaDestination;
import com.gip.xyna.xprc.xsched.ordersuspension.SuspendOrderJavaDestination;
import com.gip.xyna.xprc.xsched.ordersuspension.SuspendOrdertypeJavaDestination;
import com.gip.xyna.xprc.xsched.ordersuspension.SuspendRevisionsJavaDestination;
import com.gip.xyna.xprc.xsched.scheduling.OrderStartTimeJavaDestination;



public abstract class XynaDispatcher extends FunctionGroup {

  public static final String DEFAULT_NAME = "Xyna Dispatcher";
  
  public static final Logger logger = CentralFactoryLogging.getLogger(XynaDispatcher.class);

  /**
   * Interne OrderTypes reservieren
   */
  private static Set<String> predefinedOrderTypes = new TreeSet<String>();
  /**
   * Interne OrderType-DestinationValues reservieren
   */
  private static Set<String> predefinedDestinationValueFqNames = new TreeSet<String>();
  
  /**
   * Speicher f�r Initialisierung in PlanningDispatcher, ExecutionDispatcher, CleanupDispatcher
   */
  protected static final HashMap<DestinationKey, DestinationValue[]> allDestinations = new HashMap<DestinationKey, DestinationValue[] >();
  protected static final int INDEX_PLANNING = 0;
  protected static final int INDEX_EXECUTION = 1;
  protected static final int INDEX_CLEANUP = 2;

  private static final SortedSet<String> internalOrdertypes = new TreeSet<String>(); //SortedSet for faster access
  /**
   * alle internen OrderTypes
   */
  public static final Set<String> INTERNAL_ORDER_TYPES = Collections.unmodifiableSet(internalOrdertypes);
  
  public static final DestinationValue DESTINATION_EMPTY_PLANNING = new FractalWorkflowDestination("EmptyPlanning");
  public static final DestinationValue DESTINATION_DEFAULT_PLANNING = new FractalWorkflowDestination("DefaultPlanning");
  public static final DestinationValue DESTINATION_EMPTY_WORKFLOW = new FractalWorkflowDestination("Empty");
  static {
    predefinedDestinationValueFqNames.add(DESTINATION_DEFAULT_PLANNING.getFQName());
    predefinedDestinationValueFqNames.add(DESTINATION_EMPTY_PLANNING.getFQName());
    predefinedDestinationValueFqNames.add(DESTINATION_EMPTY_WORKFLOW.getFQName());
  }

  public static final DestinationKey DESTINATION_KEY_CANCEL = createInternalOrder(
    CancelJavaDestination.CANCEL_DESTINATION, new CancelJavaDestination() );

  public static final DestinationKey DESTINATION_KEY_KILL_STUCK_PROC = createInternalOrder(
    KillStuckProcessDestination.KILL_STUCK_PROC_DESTINATION, new KillStuckProcessDestination() );

  public static final DestinationKey DESTINATION_KEY_SUSPEND = createInternalOrder(
    SuspendOrderJavaDestination.SUSPEND_DESTINATION, new SuspendOrderJavaDestination() );

  public static final DestinationKey DESTINATION_KEY_SUSPEND_ALL = createInternalOrder(
    SuspendAllOrdersJavaDestination.SUSPEND_ALL_DESTINATION, new SuspendAllOrdersJavaDestination(), false );

  public static final DestinationKey DESTINATION_KEY_RESUME = createInternalOrder(
    ResumeOrderJavaDestination.RESUME_DESTINATION, new ResumeOrderJavaDestination() );

  public static final DestinationKey DESTINATION_KEY_TIMEOUT_SYNCHRONIZATION = createInternalOrder(
    TimeoutSynchronizationJavaDestination.TIMEOUT_SYNCHRONIZATION_DESTINATION, new TimeoutSynchronizationJavaDestination() );

  public static final DestinationKey DESTINATION_KEY_RESUME_MULTIPLE = createInternalOrder(
    ResumeMultipleOrdersJavaDestination.RESUME_MULTIPLE_DESTINATION, new ResumeMultipleOrdersJavaDestination() );
  
  public static final DestinationKey DESTINATION_KEY_SUSPEND_ORDERTYPE = createInternalOrder(
    SuspendOrdertypeJavaDestination.SUSPEND_ORDERTYPE_DESTINATION, new SuspendOrdertypeJavaDestination() );
  
  public static final DestinationKey DESTINATION_KEY_SUSPEND_REVISIONS = createInternalOrder(
    SuspendRevisionsJavaDestination.SUSPEND_REVISIONS_DESTINATION, new SuspendRevisionsJavaDestination() );
  
  public static final DestinationKey DESTINATION_KEY_ORDER_START_TIME = createInternalOrder( 
    OrderStartTimeJavaDestination.ORDER_START_TIME_DESTINATION, new OrderStartTimeJavaDestination() );

  public static final DestinationKey DESTINATION_KEY_REDIRECTION =
      createInternalOrder(MIAbstractionLayer.ORDERTYPE, new MIAbstractionLayer());

  
  private static DestinationKey createInternalOrder( String orderType, DestinationValue execution ) {
    return createInternalOrder( orderType, DESTINATION_EMPTY_PLANNING, execution, DESTINATION_EMPTY_WORKFLOW, true );
  }


  private static DestinationKey createInternalOrder( String orderType, DestinationValue execution, boolean allowedForBackup ) {
    return createInternalOrder( orderType, DESTINATION_EMPTY_PLANNING, execution, DESTINATION_EMPTY_WORKFLOW, allowedForBackup );
  }


  private static DestinationKey createInternalOrder(String orderType, DestinationValue planning,
                                                    DestinationValue execution, DestinationValue cleanup,
                                                    final boolean allowedForBackup) {
    DestinationKey dk;
    if (!allowedForBackup) {
      dk = new DestinationKey(orderType) {
        private static final long serialVersionUID = 1L;
        public boolean isAllowedForBackup() {
          return allowedForBackup;
        }
      };
    } else {
      dk = new DestinationKey(orderType);
    }
    predefinedOrderTypes.add(dk.getOrderType());
    predefinedDestinationValueFqNames.add(execution.getFQName());
    DestinationValue[] dvs = new DestinationValue[3];
    dvs[INDEX_PLANNING] = planning;
    dvs[INDEX_EXECUTION] = execution;
    dvs[INDEX_CLEANUP] = cleanup;      
    allDestinations.put( dk, dvs ); 
    internalOrdertypes.add(orderType);
    return dk;
  }
                    
 
  static {
    try {
      ArrayList<XynaFactoryPath> l =
          new ArrayList<XynaFactoryPath>(Arrays.asList(new XynaFactoryPath[] {
              new XynaFactoryPath(XynaProcessing.class, XynaProcessCtrlExecution.class, XynaPlanning.class),
              new XynaFactoryPath(XynaProcessing.class, XynaProcessCtrlExecution.class, XynaCleanup.class),
              new XynaFactoryPath(XynaProcessing.class, XynaProcessCtrlExecution.class, XynaExecution.class)}));
      addDependencies(PlanningDispatcher.class, l);
      addDependencies(CleanupDispatcher.class, l);
      addDependencies(ExecutionDispatcher.class, l);
    } catch (Throwable t) {
      Department.handleThrowable(t);
      logger.error("", t);
    }
  }

  private String name;

  private static ODS ods;

  private Map<DestinationKey, DestinationValue> destinations = 
                  new HashMap<DestinationKey, DestinationValue>();
  private Set<DestinationKey> customDestinations = new HashSet<DestinationKey>();

  private ReadWriteLock lock = new ReentrantReadWriteLock();
  private static DependencyRegister dependencyRegister; //statisch, weil es mehrere dispatcher-instanzen gibt

  static {
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    
    fExec.addTask("XynaDispatcher_static", "XynaDispatcher.initDependencyRegister").
    after(DependencyRegister.ID_FUTURE_EXECUTION).
    execAsync(new Runnable() { public void run() { initDependencyRegister(); } });
  }
  
  private static void initDependencyRegister() {
    dependencyRegister =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDependencyRegister();
  }



  public XynaDispatcher(String name) throws XynaException {
    super();
    this.name = name;
  }


  private static boolean initialized = false;


  @Override
  public void init() throws XynaException {

    if (!initialized) {
      initialized = true;
    } else {
      return;
    }

    ods = ODSImpl.getInstance();
    ods.registerStorable(DispatcherDestinationStorable.class);
    
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask(XynaDispatcher.class, "XynaDispatcher").
          after("XynaDispatcher_static").after(RevisionManagement.class, RuntimeContextDependencyManagement.class, DeploymentItemStateManagementImpl.class).
          execAsync(new Runnable() { public void run() { initLoadAllDispatchersFromFile(); } });

  }
  
  private void initLoadAllDispatchersFromFile() {
    try {
      loadAllDispatchersFromFile();
    } catch (XynaException e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public void shutdown() throws XynaException {
    // save destinations
    // no need to save on shutdown, we'll save every change
  }


  public abstract void dispatch(XynaOrderServerExtension xo) throws XynaException;
  
  
  public Map<DestinationKey, DestinationValue> getDestinations() {
    lock.readLock().lock();
    try {
      return new HashMap<DestinationKey, DestinationValue>(destinations);
    } finally {
      lock.readLock().unlock();
    }
  }

  //ACHTUNG, es wird z.b. im orderinputsourcemanamgement davon ausgegangen, dass hier immer die gleichen instanzen zur�ckgegeben werden
  public DestinationValue getDestination(DestinationKey dk) throws XPRC_DESTINATION_NOT_FOUND {
    return getDestinationInternal(dk, true).getSecond();
  }
  
  public RuntimeContext getRuntimeContextDefiningOrderType(DestinationKey dk) throws XPRC_DESTINATION_NOT_FOUND {
    return getDestinationInternal(dk, true).getFirst();
  }
  
  public DestinationValue getDestination(DestinationKey dk, boolean followRuntimeContextDependencies) throws XPRC_DESTINATION_NOT_FOUND {
    return getDestinationInternal(dk, followRuntimeContextDependencies).getSecond();
  }
  
  public Set<DestinationKey> getAllCustomDestinations() {
    return Collections.unmodifiableSet(customDestinations);
  }
  
  public boolean isDefined(DestinationKey dk) {
    lock.readLock().lock();
    try {
      return destinations.containsKey(dk);
    } finally {
      lock.readLock().unlock();
    }
  }

  
  private Pair<RuntimeContext, DestinationValue> getDestinationInternal(DestinationKey dk, boolean followRuntimeContextDependencies) throws XPRC_DESTINATION_NOT_FOUND {

    if (dk == null) {
      throw new IllegalArgumentException("Cannot obtain destination for destination key <null>!");
    }

    lock.readLock().lock();
    try {
      DestinationValue result = destinations.get(dk);
      if (result != null) {
        return Pair.of(dk.getRuntimeContext(), result);
      } else if (followRuntimeContextDependencies) {
        //in den abh�ngigen Revisions suchen
        RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
        Long revision = revMgmt.getRevision(dk.getRuntimeContext());
        Set<Long> dependencies = new HashSet<Long>();
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
            .getDependenciesRecursivly(revision, dependencies);
        for (long dep : dependencies) {
          RuntimeContext runtimeContext = revMgmt.getRuntimeContext(dep);
          result = destinations.get(new DestinationKey(dk.getOrderType(), runtimeContext));
          if (result != null) {
            return Pair.of(runtimeContext, result);
          }
        }
        
      }
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e); //sollte nicht auftreten
    } finally {
      lock.readLock().unlock();
    }
    
    //kein DestinationValue gefunden
    String runtimeContext = "";
    if (!RevisionManagement.DEFAULT_WORKSPACE.equals(dk.getRuntimeContext())) {
      runtimeContext = " [" + dk.getRuntimeContext() + "]";
    }
    throw new XPRC_DESTINATION_NOT_FOUND(dk.getOrderType() 
        + (runtimeContext.length() > 0 ? " in runtime context" + runtimeContext : ""), name);
  }


  private static enum DESTINATION_RESULT {
    ALREADY_EXISTS, OK, NOT_OVERWRITTEN;
  }

  /**
   * speichert nicht im file
   */
  private DESTINATION_RESULT setDestination(final DestinationKey dk, DestinationValue dv, boolean overwrite,
                                            boolean custom) throws PersistenceLayerException {
    if (logger.isDebugEnabled()) {
      logger.debug(name + " setting " + (custom ? "custom " : "") + "destination " + dk.getOrderType() + " => "
          + dv.getFQName());
    } 
    RuntimeContextDependencyManagement rtCtxDepMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
    RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    lock.writeLock().lock();
    try {
      DestinationValue oldDest = destinations.get(dk);
      if (oldDest != null) {
        if (oldDest.equals(dv)) {
          if (logger.isDebugEnabled()) {
            logger.debug("Destination already exists.");
          }
          return DESTINATION_RESULT.ALREADY_EXISTS;
        }
      }
      if (overwrite || oldDest == null) {
        destinations.put(dk, dv);
        if (custom) {
          customDestinations.add(dk);
          saveDestinationToFile(dk, dv);
        }
        Long parentRev;
        try {
          parentRev = revMgmt.getRevision(dk.getRuntimeContext());
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          throw new RuntimeException("Invalid destination key.", e);
        }
          //alte Dependency entfernen
          if (oldDest != null && !isPredefined(oldDest) && !isPredefined(dk)) {
            String originalFqName = getOriginalFqName(oldDest.getFQName(), oldDest.resolveRevision(dk));
            
            Set<Long> revisions = rtCtxDepMgmt.getAllRevisionsDefiningXMOMObject(originalFqName, parentRev);
            for (Long rev : revisions) {
                dependencyRegister.removeDependency(DependencySourceType.WORKFLOW, originalFqName, rev,
                                                    DependencySourceType.ORDERTYPE, dk.getOrderType(), parentRev);
            }
          }
        
          //neue Dependency anlegen
          if (!isPredefined(dv) && !isPredefined(dk)) {
            String originalFqName = getOriginalFqName(dv.getFQName(), dv.resolveRevision(dk));
  
            //Falls der Workflow bisher nicht deployed war, existieren im DependecyRegister evtl.
            //Eintr�ge mit dem fqClassName z.B. falls setDestination ausgef�hrt worden ist, 
            //bevor der Workflow deployed wurde.
            //Dann muss jetzt in allen Abh�ngigkeiten der fqClassName durch den originalFqName
            //ersetzt werden.
            Set<Long> revisions = rtCtxDepMgmt.getAllRevisionsDefiningXMOMObject(originalFqName, parentRev);
            
            for (Long rev : revisions) {
              if (dependencyRegister.getDependencyNode(dv.getFQName(), DependencySourceType.WORKFLOW, rev) != null) {
                dependencyRegister.exchangeFqClassNameForXmlName(dv.getFQName(), originalFqName, rev);
              }
              
              //neue Dependency eintragen
              dependencyRegister.addDependency(DependencySourceType.WORKFLOW, originalFqName, rev,
                                               DependencySourceType.ORDERTYPE, dk.getOrderType(), parentRev);
            }
          }

        registerStatisticsIfNecessary(dk);
        executeHandlerSet(dk, dv);
        return DESTINATION_RESULT.OK;

      }
    } finally {
      lock.writeLock().unlock();
    }
    return DESTINATION_RESULT.NOT_OVERWRITTEN;
  }


  /**
   * Bestimmt dem originalFqName zum fqClassName. Kann der originalFqName nicht
   * bestimmt werden, weil der Workflow noch nicht deployed ist, wird der fqClassName
   * zur�ckgegeben.
   * @param fqClassName
   * @param revision
   * @return
   */
  private String getOriginalFqName(String fqClassName, Long revision) {
    WorkflowDatabase wdb = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getWorkflowDatabase();
    String originalFqName = wdb.getXmlName(fqClassName, revision);

    if (originalFqName == null){
      originalFqName = fqClassName; //Wf ist nicht deployed -> fqClassName beibehalten
    }
    
    return originalFqName;
  }
  
  /**
   * speichert nicht im file
   * @return gibt true zur�ck, wenn der eintrag gesetzt wurde und false, falls overwrite = false war und der
   *         destinationkey bereits belegt war.
   */
  public boolean setDestination(DestinationKey dk, DestinationValue dv, boolean overwrite) {
    try {
      return setDestination(dk, dv, overwrite, false) != DESTINATION_RESULT.NOT_OVERWRITTEN;
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }


  protected abstract void registerStatisticsIfNecessary(DestinationKey dk);


  /**
   * speichert im file, falls die destination nicht bereits schon mit dem gleichen destinationvalue vorhanden ist
   * @return gibt zur�ck, ob in file gespeichert wurde
   */
  public boolean setCustomDestination(DestinationKey dk, DestinationValue dv) throws PersistenceLayerException {     
    return setDestination(dk, dv, true, true) == DESTINATION_RESULT.OK;
  }


  private void loadAllDispatchersFromFile() throws PersistenceLayerException {
    XynaDispatcher[] dispatchers =
        new XynaDispatcher[] {
            XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaPlanning()
                .getPlanningDispatcher(),
            XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution()
                .getExecutionEngineDispatcher(),
            XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaCleanup()
                .getCleanupEngineDispatcher()};
    Map<String, XynaDispatcher> mapDispatcherNameToDispatcher = new HashMap<String, XynaDispatcher>();
    for (XynaDispatcher d : dispatchers) {
      mapDispatcherNameToDispatcher.put(d.getDefaultName(), d);
    }
    ODSConnection historyConnection = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      Collection<DispatcherDestinationStorable> allDestinations =
          historyConnection.loadCollection(DispatcherDestinationStorable.class);
      for (DispatcherDestinationStorable dds: allDestinations) {
        XynaDispatcher dispatcher = mapDispatcherNameToDispatcher.get(dds.getDispatcherName());
        if (dispatcher == null) {
          throw new RuntimeException("Unsupported dispatcher name in configuration file: '" + dds.getDispatcherName() + "'");
        }
        DestinationValue dv;
        Long revision = dds.getRevision();
        switch (dds.getDestinationTypeAsEnum()) {
          case XYNA_FRACTAL_WORKFLOW :
            dv = new FractalWorkflowDestination(dds.getDestinationValue());
            break;
          case JAVA_DESTINATION :
            throw new RuntimeException("Java destinations may not be configured.");
          case SERVICE_DESTINATION :
            throw new RuntimeException("Service destinations may not be configured.");
          default :
            throw new RuntimeException("Unknown execution type: '" + dds.getDestinationType() + "'");
        }
        RuntimeContext runtimeContext;
        try {
          runtimeContext = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
                          .getRuntimeContext(revision);
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          logger.warn("Can't get runtimeContext for revision " + revision, e);
          continue;
        }
        DestinationKey destKey = new DestinationKey(dds.getDestinationKey(), runtimeContext);
        dispatcher.setDestination(destKey, dv, true);
        dispatcher.customDestinations.add(destKey);
      }
    } finally {
      historyConnection.closeConnection();
    }
  }


  private void saveDestinationToFile(DestinationKey dk, DestinationValue dv) throws PersistenceLayerException {
    long revision;
    try {
      revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRevision(dk.getRuntimeContext());
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
      throw new RuntimeException(e1);
    }
    ODSConnection historyConnection = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      Collection<DispatcherDestinationStorable> allDispatcherDestinationsWithSameKeyForThisDispatcher = new ArrayList<DispatcherDestinationStorable>();
      try {
        PreparedQuery<DispatcherDestinationStorable> query = DispatcherDestinationStorable
                        .getAllDestinationsWithDestinationKeyForThisDispatcher(historyConnection);
        allDispatcherDestinationsWithSameKeyForThisDispatcher = historyConnection
                        .query(query, new Parameter(dk.getOrderType(), getDefaultName()), Integer.MAX_VALUE);

      } catch (XNWH_UnsupportedPersistenceLayerFeatureException e) {
        Collection<DispatcherDestinationStorable> allDispatcherDestinations = historyConnection
                        .loadCollection(DispatcherDestinationStorable.class);
        if (allDispatcherDestinations != null && allDispatcherDestinations.size() > 0) {
          for (DispatcherDestinationStorable next : allDispatcherDestinations) {
            if (next.getDestinationKey() != null && next.getDestinationKey().equals(dk.getOrderType()) && next
                            .getDispatcherName().equals(getDefaultName())) {
              allDispatcherDestinationsWithSameKeyForThisDispatcher.add(next);
            }
          }
        }
      }
      if (allDispatcherDestinationsWithSameKeyForThisDispatcher.size() == 0) {
        historyConnection.persistObject(new DispatcherDestinationStorable(dk.getOrderType(), getDefaultName(), dv
                        .getDestinationType(), dv.getFQName(), revision));
      } else {
        boolean found = false;
        for (DispatcherDestinationStorable oldDestination : allDispatcherDestinationsWithSameKeyForThisDispatcher) {
          if (oldDestination.getRevision() != null && oldDestination.getRevision().equals(revision)) {

            historyConnection.persistObject(new DispatcherDestinationStorable(oldDestination.getId(),
                                    dk.getOrderType(), getDefaultName(), dv.getDestinationType(), dv.getFQName(),
                                    revision));
            found = true;
          }
        }
        
       if(!found) {
         historyConnection.persistObject(new DispatcherDestinationStorable(dk.getOrderType(), getDefaultName(), dv.getDestinationType(), dv.getFQName(),
                                                                           revision));
       }
      }
      historyConnection.commit();
    } finally {
      historyConnection.closeConnection();
    }
  }


  private void deleteDestinationFromFile(DestinationKey dk, Long revision) throws PersistenceLayerException {
    ODSConnection historyConnection = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      try {
        PreparedCommand command = DispatcherDestinationStorable.getCommandDeleteByDestinationKeyAndRevision(historyConnection);
        Parameter params = new Parameter(dk.getOrderType(), revision);
        historyConnection.executeDML(command, params);
      } catch (XNWH_UnsupportedPersistenceLayerFeatureException e) {
        Collection<DispatcherDestinationStorable> toBeDeleted = null;
        Collection<DispatcherDestinationStorable> dispatcherDestinations =
            historyConnection.loadCollection(DispatcherDestinationStorable.class);
    
        if (dispatcherDestinations != null && dispatcherDestinations.size() > 0) {
          for (DispatcherDestinationStorable next : dispatcherDestinations) {
            if (next.getDestinationKey() != null && next.getDestinationKey().equals(dk.getOrderType())) {
              if (next.getRevision() != null && next.getRevision().equals(revision)) {
                if (toBeDeleted == null) {
                  toBeDeleted = new ArrayList<DispatcherDestinationStorable>();
                }
                toBeDeleted.add(next);
              }
            }
          }
          if (toBeDeleted != null) {
            historyConnection.delete(toBeDeleted);
          }
        }
      }
      historyConnection.commit();
    } finally {
      historyConnection.closeConnection();
    }
  }


  public void removeDestination(DestinationKey dk) {
    if (logger.isDebugEnabled()) {
      logger.debug(name + " removing destination " + dk.getOrderType());
    }
    lock.writeLock().lock();
    try {
      destinations.remove(dk);
    } finally {
      lock.writeLock().unlock();
    }
    executeHandlerRemove(dk);
  }
  

  public void removeCustomDestination(DestinationKey dk, DestinationValue dv) throws PersistenceLayerException {
    if (logger.isDebugEnabled()) {
      logger.debug(name + " removing custom destination " + dk.getOrderType());
    }
    RuntimeContextDependencyManagement rtCtxDepMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
    RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    long revision;
    try {
      revision = revMgmt.getRevision(dk.getRuntimeContext());
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }
    lock.writeLock().lock();
    try {
      if (isPredefined(dk)) {
        return;
      }

      if (!isPredefined(dv)) {
        String originalFqName = getOriginalFqName(dv.getFQName(), dv.resolveRevision(dk));
        Set<Long> revisions = rtCtxDepMgmt.getAllRevisionsDefiningXMOMObject(originalFqName, revision);
        for (Long rev : revisions) {
          dependencyRegister.removeDependency(DependencySourceType.WORKFLOW, originalFqName, rev, DependencySourceType.ORDERTYPE, dk.getOrderType(), revision);  
        }
      }
      
      executeHandlerRemove(dk);
      if (!isCustom(dk)) {
        removeDestination(dk);
        return;
      }
      destinations.remove(dk);
      customDestinations.remove(dk);
      deleteDestinationFromFile(dk, revision);
    } finally {
      lock.writeLock().unlock();
    }
  }


  // see below
  public boolean isPredefined(DestinationKey dk) {
    if (dk == null) {
      return false;
    }
    return predefinedOrderTypes.contains(dk.getOrderType());
  }


  // those should never contain or point to a modeled WF, the DeploymentManager will be ignoring those
  public boolean isPredefined(DestinationValue dv) {
    if (dv == null) {
      return false;
    }
    return predefinedDestinationValueFqNames.contains(dv.getFQName());
  }


  public enum CallStatsType implements StatisticsPathPart {
    STARTED("TotalCalls", "Total count of calls of this order type."),
    FAILED("Errors", "Count of orders of this order type that failed."),
    FINISHED("Success", "Count of orders of this order type that were successfully."),
    TIMEOUT("Timeouts", "Count of orders of this order type that were canceled due to a scheduling timeout.");

    private String statsSuffix;
    private String description;
    private CallStatsType(String statsSuffix, String description) {
      this.statsSuffix = statsSuffix;
      this.description = description;
    }
    public String getDescription() {
      return description;
    }
    public String getStatisticsSuffix() {
      return statsSuffix;
    }
    public String getPartName() {
      return statsSuffix;
    }
    public StatisticsNodeTraversal getStatisticsNodeTraversal() {
      return StatisticsNodeTraversal.SINGLE;
    }
    
    public static CallStatsType getCallStatsTypeByPathPart(String pathPart) {
      for (CallStatsType cst : values()) {
        if (cst.statsSuffix.equals(pathPart)) {
          return cst;
        }
      }
      return null;
    }
    public UnknownPathOnTraversalHandling getUnknownPathHandling() {
      return UnknownPathOnTraversalHandling.THROW_IF_ANY;
    }

  }

  
  public static final String WORKING_SET_APPLICATION_NAME = "WorkingSet";
  public static final String AGGREGATION_APPLICATION_NAME = "All";
  public static final String APPLICATION_NAME_PREFIX = "Application-";
  
  public static final String ORDERTYPE_STATISTICS_PATH_PART_NAME = "OrderType";
  public static final String APPLICATIONNAME_STATISTICS_PATH_PART_NAME = "ApplicationName";

  public static StatisticsPath getBaseCallStatsPath() {
    return PredefinedXynaStatisticsPath.ORDERSTATISTICS;
  }
  
  public static StatisticsPath getSpecificCallStatsPath(String ordertype, String applicationName) {
    StatisticsPath path = getBaseCallStatsPath();
    if (applicationName == null || applicationName.equals(WORKING_SET_APPLICATION_NAME)) {
      path = path.append(WORKING_SET_APPLICATION_NAME);
    } else if (applicationName.equals(AGGREGATION_APPLICATION_NAME)) {
      path = path.append(AGGREGATION_APPLICATION_NAME);
    } else {
      path = path.append(APPLICATION_NAME_PREFIX + applicationName);
    }
    path = path.append(ordertype);
    return path;
  }
  
  public static StatisticsPath getSpecificCallStatsAttributePath(String ordertype, String applicationName, CallStatsType type) {
    return getSpecificCallStatsPath(ordertype, applicationName).append(type);
  }


  public boolean isCustom(DestinationKey key) {
    lock.readLock().lock();
    try {
      return customDestinations.contains(key);
    } finally {
      lock.readLock().unlock();
    }
  }

  public void activateCustomDestinations(DestinationValue destination) {
    RuntimeContextDependencyManagement rtCtxDepMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
    RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    lock.readLock().lock();
    try {
      Iterator<DestinationKey> iterator = customDestinations.iterator();
      while (iterator.hasNext()) {
        DestinationKey dk = iterator.next();
        if (isPredefined(dk)) {
          continue;
        }
        DestinationValue dv = destinations.get(dk);
        if (destination.equals(dv)) {
          //neue Dependency anlegen
          Set<Long> allRevisions = dv.resolveAllRevisions(dk);
          for (Long aRevisions : allRevisions) {
            String originalFqName = getOriginalFqName(dv.getFQName(), aRevisions);

            //Falls der Workflow bisher nicht deployed war, existieren im DependecyRegister evtl.
            //Eintr�ge mit dem fqClassName z.B. falls setDestination ausgef�hrt worden ist, 
            //bevor der Workflow deployed wurde.
            //Dann muss jetzt in allen Abh�ngigkeiten der fqClassName durch den originalFqName
            //ersetzt werden.
            try {
              Long parentRev = revMgmt.getRevision(dk.getRuntimeContext());
              
              Set<Long> revisions = rtCtxDepMgmt.getAllRevisionsDefiningXMOMObject(originalFqName, parentRev);
              for (Long rev : revisions) {
                if (dependencyRegister.getDependencyNode(dv.getFQName(), DependencySourceType.WORKFLOW, rev) != null) {
                  dependencyRegister.exchangeFqClassNameForXmlName(dv.getFQName(), originalFqName, rev);
                }
    
                //neue Dependency eintragen
                dependencyRegister.addDependency(DependencySourceType.WORKFLOW, originalFqName, rev,
                                                 DependencySourceType.ORDERTYPE, dk.getOrderType(),
                                                 parentRev);
                
              }
            } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
              logger.warn("Dependency for custom destination " + dk.getOrderType() + " could not be saved in dependencyregister.", e);
            }
          }
        }
      }
    } finally {
      lock.readLock().unlock();
    }
  }
  

  public interface DestinationChangedHandler {

    void set(DestinationKey dk, DestinationValue dv);


    void remove(DestinationKey dk);
  }


  private ConcurrentMap<DestinationChangedHandler, Boolean> changeHandler = new ConcurrentHashMap<DestinationChangedHandler, Boolean>();


  public void registerCallbackHandler(DestinationChangedHandler dch) {
    changeHandler.put(dch, Boolean.TRUE);
  }


  public void unregisterCallbackHandler(DestinationChangedHandler dch) {
    changeHandler.remove(dch);
  }


  private void executeHandlerSet(DestinationKey dk, DestinationValue dv) {
    for (DestinationChangedHandler dch : changeHandler.keySet()) {
      dch.set(dk, dv);
    }
  }


  private void executeHandlerRemove(DestinationKey dk) {
    for (DestinationChangedHandler dch : changeHandler.keySet()) {
      dch.remove(dk);
    }
  }


}
