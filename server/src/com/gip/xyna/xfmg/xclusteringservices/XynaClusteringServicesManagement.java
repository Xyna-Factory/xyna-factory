/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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

package com.gip.xyna.xfmg.xclusteringservices;



import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.gip.xyna.Department;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.FutureExecution.FutureExecutionTaskDependencyException;
import com.gip.xyna.FutureExecutionTask;
import com.gip.xyna.Section;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.exceptions.XFMG_ClusterComponentConfigurationException;
import com.gip.xyna.xfmg.exceptions.XFMG_ClusterConnectionException;
import com.gip.xyna.xfmg.exceptions.XFMG_ClusterInitializationException;
import com.gip.xyna.xfmg.exceptions.XFMG_ClusterProviderFilesNotFoundException;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidConnectionParametersForClusterProviderException;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidStartParametersForClusterProviderException;
import com.gip.xyna.xfmg.exceptions.XFMG_JarFolderNotFoundException;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownClusterInstanceIDException;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownClusterProviderException;
import com.gip.xyna.xfmg.xfctrl.RMIManagement;
import com.gip.xyna.xfmg.xfctrl.classloading.ClusterProviderClassLoader;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.XynaRunnable;
import com.gip.xyna.xprc.XynaThreadPoolExecutor;



public class XynaClusteringServicesManagement extends Section implements XynaClusteringServicesManagementInterface {

  public static final int DEFAULT_BINDING_NO_CLUSTER = 0;
  public static final int INITIAL_CLUSTER_BINDING = 1;

  public static final long DEFAULT_CLUSTER_INSTANCE_ID_NOT_CONFIGURED = -1;

  private enum ClusterCreationType {
    CREATE, JOIN, RESTORE;
  }

  private interface ClusterProviderFactory {

    public ClusterProvider createProvider();


    public String getName();
  }

  private static class RMIClusterProviderFactory implements ClusterProviderFactory {

    public ClusterProvider createProvider() {
      return new RMIClusterProvider();
    }


    public String getName() {
      return RMIClusterProvider.TYPENAME;
    }
  }

  private class MgmtClusterStateChangeHandler implements ClusterStateChangeHandler {

    
    private XynaThreadPoolExecutor threadpool;
    
    private final long clusterInstanceId;
    private volatile ClusterState plannedClusterState;
    private volatile ClusterState currentClusterState;
    
    
    //bei nach dem serverstart neu konfigurierten clusterinstances nicht mit false initialisieren
    private volatile boolean globalReadyForChange = XynaClusteringServicesManagement.this.globalReadyForChange;
    private volatile boolean onChangeRunning = false;
    
    public MgmtClusterStateChangeHandler(final long clusterInstanceId) {
      this.clusterInstanceId = clusterInstanceId;
      
      threadpool = new XynaThreadPoolExecutor(1, 1, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {

          public Thread newThread(Runnable r) {
            return new Thread(r, XynaClusteringServicesManagement.class.getSimpleName() + "ChangeHandlerThread" + clusterInstanceId);
          }

        }, XynaClusteringServicesManagement.class.getSimpleName() + "ThreadPool" + clusterInstanceId);
      
    }

    public void setGlobalReadyForChange(boolean value) {
      globalReadyForChange = value;
    }


    public boolean isReadyForChange(ClusterState newState) {
      if (!globalReadyForChange) {
        if (logger.isInfoEnabled()) {
          logger.info("MgmtClusterStateChangeHandler.isReadyForChange(" + newState + ") for id=" + clusterInstanceId
              + " returns false, caused by globalReadyForChange = false");
        }
        return false;
      }
      List<ClusterStateChangeHandler> specificStateChangeHandlers;
      synchronized (stateChangeHandlers) {
        specificStateChangeHandlers = stateChangeHandlers.get(clusterInstanceId);
      }
      for (ClusterStateChangeHandler csch : specificStateChangeHandlers) {
        if (logger.isTraceEnabled()) {
          logger.trace("calling isReadyForChange for clusterStateChangeHandler " + csch);
        }
        boolean ready = csch.isReadyForChange(newState);
        if (!ready) {
          if (logger.isInfoEnabled()) {
            logger.info("MgmtClusterStateChangeHandler.isReadyForChange(" + newState + ") for id=" + clusterInstanceId
                + " returns false,caused by " + csch.getClass().getName());
          }
          return false;
        }
      }
      if (logger.isDebugEnabled()) {
        logger.debug("MgmtClusterStateChangeHandler.isReadyForChange(" + newState + ") for id=" + clusterInstanceId
            + " returns true");
      }
      return true;
    }
    
    //FIXME klarer strukturieren/dokumentieren, wer wann welche methode aufruft. am besten isReadyFoChange und readyForChangeMethoden besser benamen.

    public void onChange(final ClusterState newState) {
      tryOnChange(newState, false);
    }
    
    private synchronized void tryOnChange(ClusterState newState, boolean checkAgainIsReady) {
      if( newState == null ) {
        return; //kein weiterer onChange geplant
      }
      if( currentClusterState == newState ) {
        if (logger.isDebugEnabled()) {
          logger.debug("MgmtClusterStateChangeHandler.tryOnChange(" + newState + ") for id=" + clusterInstanceId +" is already in target state: nothing to do");
        }
        return;
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug("MgmtClusterStateChangeHandler.tryOnChange(" + newState + ") for id=" + clusterInstanceId);
        }
        plannedClusterState = newState;
      }
     
      
      boolean isReadyForChange = true;
      //wenn vom clusterprovider "onChange" aufgerufen wird, sollte nicht nochmal isReadyForChange überprüft werden!
      //dann ist der cluster zustandsübergang ja bereits passiert!
      if (checkAgainIsReady) {
        isReadyForChange = isReadyForChange(newState);
      }
      if( isReadyForChange && !onChangeRunning) {
        //im eigenen thread ausführen, damit nicht statechangehandler rekursiv aufgerufen werden.
        //logger.debug("tryOnChange("+newState+") called from", new Exception() );
        onChangeRunning = true;
        threadpool.execute(new OnChange(newState) );
        plannedClusterState = null;
      } else {
        //tryOnChange muss bald wieder aufgerufen werden
        threadpool.execute(new WaitForOnChange() );
      }
    }

    public void tryOnChange() {
      tryOnChange(plannedClusterState, true);
    }

    private class WaitForOnChange extends XynaRunnable {
      public void run() {
        try {
          Thread.sleep(1000); //TODO feste Konstante anpassen
        }
        catch (InterruptedException e) {
          //ignorieren, dann war die Wartezeit halt kürzer
        }
        tryOnChange();
      }
    }
     
    private class OnChange extends XynaRunnable {
  
      private ClusterState newClusterState;

      public OnChange(ClusterState newClusterState) {
        this.newClusterState = newClusterState;
      }

      public void run() {
        boolean success = false;
        try {
          List<ClusterStateChangeHandler> specificStateChangeHandlers;
          synchronized (stateChangeHandlers) {
            specificStateChangeHandlers = stateChangeHandlers.get(clusterInstanceId);
          }

          FutureExecution futureExecutionOnChangeHandler;
          synchronized (futureExecutionsOnChangeHandler) {

            futureExecutionOnChangeHandler = new FutureExecution("OnChange-"+clusterInstanceId+"-"+System.currentTimeMillis() );
            futureExecutionsOnChangeHandler.put(clusterInstanceId, futureExecutionOnChangeHandler);
            if( logger.isDebugEnabled() ) {
              logger.debug( "OnChange called for "+specificStateChangeHandlers );
            }
            for (ClusterStateChangeHandler handler : specificStateChangeHandlers) {
              try {
                handler.onChange(newClusterState);
              } catch (RuntimeException e) {
                logger.warn("XynaClusteringServicesManagement: Could not execute ClusterStateChangeHandler " + handler, e);
              }
            }
          }
          futureExecutionsOnChangeHandler.remove(clusterInstanceId);
          if( logger.isDebugEnabled() ) {
            logger.debug( futureExecutionOnChangeHandler.toString() );
          }
           
          try {
            futureExecutionOnChangeHandler.finishedRegistrationProcess();
            success = true;
          } catch (FutureExecutionTaskDependencyException e) {
            logger.warn("Some ClusterStateChangeHandlers could not be executed.", e);
          }
        } catch (Throwable t) {
          Department.handleThrowable(t);
          logger.error("unexpected error in ClusterStateChange Thread", t);
        } finally {
          onChangeRunning = false;
          if( success ) {
            currentClusterState = newClusterState;
          } else {
            currentClusterState = null; //OnChange ist nicht regulär fertig geworden, daher ist Zustand unbestimmt
          }
        }
      }

    }
    
  }

  @Persistable(primaryKey = ClusterComponentStorable.COL_COMPONENTNAME, tableName = ClusterComponentStorable.TABLENAME)
  public static class ClusterComponentStorable extends Storable<ClusterComponentStorable> {

    private static final long serialVersionUID = 1L;
    public static final String COL_COMPONENTNAME = "componentname";
    public static final String TABLENAME = "clustercomponent";
    public static final String COL_CLUSTERID = "clusterid";

    private static final ResultSetReader<ClusterComponentStorable> reader = new ClusterComponentResultSetReader();

    @Column(name = COL_COMPONENTNAME)
    private String componentname;

    @Column(name = COL_CLUSTERID)
    private long clusterid;


    public ClusterComponentStorable() {
      clusterid = -1;
    }


    public ClusterComponentStorable(String componentName, long clusterId) {
      this.componentname = componentName;
      this.clusterid = clusterId;
    }


    @Override
    public Object getPrimaryKey() {
      return componentname;
    }


    public String getComponentname() {
      return componentname;
    }


    public long getClusterid() {
      return clusterid;
    }


    @Override
    public ResultSetReader<? extends ClusterComponentStorable> getReader() {
      return reader;
    }


    @Override
    public <U extends ClusterComponentStorable> void setAllFieldsFromData(U data) {
      ClusterComponentStorable cast = data;
      this.clusterid = cast.clusterid;
      this.componentname = cast.componentname;
    }


    private static class ClusterComponentResultSetReader implements ResultSetReader<ClusterComponentStorable> {

      public ClusterComponentStorable read(ResultSet rs) throws SQLException {
        ClusterComponentStorable ccs = new ClusterComponentStorable();
        ccs.clusterid = rs.getLong(COL_CLUSTERID);
        ccs.componentname = rs.getString(COL_COMPONENTNAME);
        return ccs;
      }
      
    }

    @Override
    public String toString() {
      return "ClusterComponent("+componentname+","+clusterid+")";
    }

  }

  @Persistable(primaryKey = ClusterInstanceStorable.COL_CLUSTERID, tableName = ClusterInstanceStorable.TABLENAME)
  public static class ClusterInstanceStorable extends Storable<ClusterInstanceStorable> {

    private static final long serialVersionUID = 1L;
    public static final String TABLENAME = "clusterinstance";
    public static final String COL_CLUSTERID = "clusterid";
    public static final String COL_CLUSTERTYPE = "clustertype";
    public static final String COL_INTERNALCLUSTERID = "internalclusterid";
    public static final String COL_DESCRIPTION = "description";

    @Column(name = COL_CLUSTERID)
    private long clusterid;

    @Column(name = COL_CLUSTERTYPE)
    private String clustertype;

    @Column(name = COL_INTERNALCLUSTERID)
    private long internalclusterid;

    @Column(name = COL_DESCRIPTION)
    private String description;


    public ClusterInstanceStorable() {
    }


    public ClusterInstanceStorable(String clusterType, long id, long internalClusterId, String description) {
      this.clusterid = id;
      this.clustertype = clusterType;
      this.internalclusterid = internalClusterId;
      this.description = description;
    }


    @Override
    public Object getPrimaryKey() {
      return clusterid;
    }


    public long getClusterid() {
      return clusterid;
    }


    public String getClustertype() {
      return clustertype;
    }


    public long getInternalclusterid() {
      return internalclusterid;
    }


    public String getDescription() {
      return description;
    }


    @Override
    public ResultSetReader<? extends ClusterInstanceStorable> getReader() {
      return new ClusterInstanceStorableReader();
    }


    private static class ClusterInstanceStorableReader implements ResultSetReader<ClusterInstanceStorable> {

      public ClusterInstanceStorable read(ResultSet rs) throws SQLException {
        ClusterInstanceStorable cis = new ClusterInstanceStorable();
        cis.clusterid = rs.getLong(COL_CLUSTERID);
        cis.clustertype = rs.getString(COL_CLUSTERTYPE);
        cis.internalclusterid = rs.getLong(COL_INTERNALCLUSTERID);
        cis.description = rs.getString(COL_DESCRIPTION);
        return cis;
      }

    }


    @Override
    public <U extends ClusterInstanceStorable> void setAllFieldsFromData(U data) {
      ClusterInstanceStorable cast = data;
      this.clusterid = cast.clusterid;
      this.clustertype = cast.clustertype;
      this.internalclusterid = cast.internalclusterid;
      this.description = cast.description;
    }

  }

  @Persistable(primaryKey = ClusterProviderStorable.COL_CLUSTERTYPE, tableName = ClusterProviderStorable.TABLENAME)
  public static class ClusterProviderStorable extends Storable<ClusterProviderStorable> {

    private static final long serialVersionUID = 1L;
    private static final ResultSetReader<ClusterProviderStorable> reader = new ClusterProviderResultSetReader();
    public static final String TABLENAME = "clusterprovider";
    public static final String COL_CLUSTERTYPE = "clustertype";

    @Column(name = COL_CLUSTERTYPE)
    private String clustertype;


    public ClusterProviderStorable() {
    }


    public ClusterProviderStorable(String clusterType) {
      this.clustertype = clusterType;
    }


    @Override
    public Object getPrimaryKey() {
      return clustertype;
    }


    public String getClustertype() {
      return clustertype;
    }


    @Override
    public ResultSetReader<? extends ClusterProviderStorable> getReader() {
      return reader;
    }


    @Override
    public <U extends ClusterProviderStorable> void setAllFieldsFromData(U data) {
      ClusterProviderStorable cast = data;
      this.clustertype = cast.clustertype;
    }


    private static class ClusterProviderResultSetReader implements ResultSetReader<ClusterProviderStorable> {

      public ClusterProviderStorable read(ResultSet rs) throws SQLException {
        ClusterProviderStorable cps = new ClusterProviderStorable();
        cps.clustertype = rs.getString(COL_CLUSTERTYPE);
        return cps;
      }
      
    }

  }


  public static final String DEFAULT_NAME = "Xyna Clustering Services";

  private static volatile XynaClusteringServicesManagement _instance;
  private static Object instanceLock = new Object();
  private static AtomicBoolean initialized = new AtomicBoolean();

  private Map<Long, FutureExecution> futureExecutionsOnChangeHandler = new HashMap<Long, FutureExecution>();
  //componentname -> component
  private Map<String, Clustered> clusterableComponents;
  //clusterprovider-typename -> providerfactory
  private Map<String, ClusterProviderFactory> registeredClusterProviders;
  private Map<Long, ClusterProvider> clusterInstances;
  private boolean globalReadyForChange = false;
  private Map<Long, MgmtClusterStateChangeHandler> providerStateChangeHandlers;
  private Map<Long, CopyOnWriteArrayList<ClusterStateChangeHandler>> stateChangeHandlers;
  private AtomicLong currentClusterInstanceId;
  private ODS ods;
  private volatile boolean savedClusterComponentsEnabled = false;


  private XynaClusteringServicesManagement() throws XynaException {
    super();
  }


  public static void clearInstance() {
    _instance = null; // for test purposes
    initialized.set(false);
  }


  public static XynaClusteringServicesManagement getInstance() {
    if (_instance == null) {
      synchronized (instanceLock) {
        if (_instance == null) {
          try {
            _instance = new XynaClusteringServicesManagement();
          } catch (XynaException e) {
            throw new RuntimeException("Failed to create " + XynaClusteringServicesManagement.class.getSimpleName(), e);
          }
        }
      }
    }
    return _instance;
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  @Override
  protected void init() throws XynaException {

    if (!initialized.compareAndSet(false, true)) {
      return;
    }
    clusterableComponents = new HashMap<String, Clustered>();
    registeredClusterProviders = new HashMap<String, ClusterProviderFactory>();
    clusterInstances = new HashMap<Long, ClusterProvider>();
    stateChangeHandlers = new HashMap<Long, CopyOnWriteArrayList<ClusterStateChangeHandler>>();
    providerStateChangeHandlers = new HashMap<Long, MgmtClusterStateChangeHandler>();
    
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask(XynaClusteringServicesManagement.class, "XynaClusteringServicesManagement").
      after(RMIManagement.class).
      execAsync(new Runnable() { public void run() { initXynaClusteringServicesManagement(); }});
    fExec.addTask( FUTURE_EXECUTION_ID__CLUSTERING_SERVICES_CLUSTERED_COMPONENTS, "XynaClusteringServicesManagement.setID").
      deprecated().
      after(XynaClusteringServicesManagement.class).
      execAsync(); //nur dummy
  }
  
  private void initXynaClusteringServicesManagement() {
    ods = ODSImpl.getInstance();
             try {
              //achtung, die storables dürfen nur auf xml konfiguriert sein, weil sonst die abhängigkeiten beim laden von odsimpl nicht passen
              //bei der erstinstallation werden die defaultpersistencelayers erst im update angelegt.
              ods.registerStorable(ClusterComponentStorable.class);
              ods.registerStorable(ClusterInstanceStorable.class);
              ods.registerStorable(ClusterProviderStorable.class);

              registerClusterProviderInternally(new RMIClusterProviderFactory());
              
              registerSavedClusterProviders();
              createSavedClusterInstances();
              
              synchronized (clusterInstances) {
                int counter = 0;
                List<Integer> ids = new ArrayList<Integer>(clusterInstances.size());
                for(Long clusterinstanceId : clusterInstances.keySet()) {
                  int id = XynaFactory.getInstance().getFutureExecution().nextId();
                  ids.add(id);
                  FutureExecutionTaskOnClusterChange fet = new FutureExecutionTaskOnClusterChange(id, "XynaClusteringServicesManagement.WaitForClusterproviderStarted" + counter);
                  XynaFactory.getInstance().getFutureExecution().execAsync(fet);
                  fet.setClusterInstanceId(clusterinstanceId);
                  addClusterStateChangeHandler(clusterinstanceId, fet);
                  setGlobalReadyForChange(clusterinstanceId, true);
                  counter++;
                }
                final int[] allIds = new int[ids.size()];
                for (int i = 0; i<allIds.length; i++) {
                  allIds[i] = ids.get(i);
                }
                //aggregations-task für alle clusterinstances
                FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
                fExec.addTask(XynaClusteringServicesManagementInterface.FUTURE_EXECUTION_ID__START_CLUSTERPROVIDERS_FINISHED, "XynaClusteringServicesManagement_ClusterProvider_Finished").
                after(allIds).
                before(XynaProcessing.FUTUREEXECUTIONID_ORDER_EXECUTION).
                execAsync(); //dummy. aggregiert die future execution tasks der cluster instances, um darauf zu warten, dass sie alle gestartet wurden.
              }
              
              //die clustercomponents haben sich noch nicht registriert -> erst ausführen, wenn alle clustercomponents sich hier registriert haben.
              //das funktioniert so, dass die anderen components ein futureexecutiontask definieren müssen, mit einer before-beziehung auf diese id.
              registerSavedClusterComponents();
              
              connectSavedClusterInstances();
            } catch (XynaException e) {
              throw new RuntimeException(e);
            }
  }

  private void registerSavedClusterComponents() throws PersistenceLayerException,
      XFMG_ClusterComponentConfigurationException {
    //bereits früher auf cluster konfigurierte komponenten 
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      ArrayList<ClusterComponentStorable> components = 
          new ArrayList<ClusterComponentStorable>( con.loadCollection(ClusterComponentStorable.class) );
      if (logger.isDebugEnabled()) {
        logger.debug("Registering saved " + components.size() + " cluster components.");
      }
      Collections.sort( components, new ClusterComponentStorableSorter(clusterInstances) );
      if( logger.isDebugEnabled() ) {
        logger.debug("ClusterComponents are : "+components );
      }
      
      for (ClusterComponentStorable ccs : components) {
        Clustered clustered = clusterableComponents.get(ccs.getComponentname());
        if (clustered == null) {
          logger.info("clustered component '" + ccs.getComponentname()
              + " could not be reconfigured for cluster, because it could not be found in server (yet?).");
          continue;
        }
        clustered.enableClustering(ccs.getClusterid());
      }
    } finally {
      con.closeConnection();
    }
    savedClusterComponentsEnabled = true;
  }
  
  public void connectSavedClusterInstances() {
    Collection<ClusterProvider> instances;
    synchronized (clusterInstances) {
      instances = clusterInstances.values();
    }
    for (ClusterProvider ci : instances) {
      ci.restoreClusterConnect();
    }
  }

  /**
   * Reihenfolge der Initialisierung der ClusterComponent ist wichtig, da es gegenseitige Abhängigkeitem 
   * der Inititalisierung gibt. 
   * (Zumindest fiel es einmal auf, dass auf einem anderen System als der Entwicklungsumgebung  Probleme 
   * auftraten, da die Reihenfolge anders war als bis dahin getetestet.)
   * 
   * Sortierung ist: 
   * a) zugehöriger ClusterProvider, dabei RMIClusterProvider als letzter
   * b) innerhalb des gleichen ClusterProviders: Alphabetisch nach Namen der ClusterComponent
   * TODO Konfigurierbarkeit der Sortierung?
   */
  private static class ClusterComponentStorableSorter implements Comparator<ClusterComponentStorable> {
    private ArrayList<Long> orderedClusterIds;
    
     public ClusterComponentStorableSorter(Map<Long, ClusterProvider> clusterInstances) {
      orderedClusterIds = new ArrayList<Long>(getSortedClusterProviderIdListWithRMILast(clusterInstances));
    }

    public int compare(ClusterComponentStorable o1, ClusterComponentStorable o2) {
      int diff = orderedClusterIds.indexOf(o1.getClusterid()) - orderedClusterIds.indexOf(o2.getClusterid());
      if( diff != 0 ) {
        return diff < 0 ? -1 : 1;
      }
      return o1.getComponentname().compareTo(o2.getComponentname());
    }
    
  }
  
  /**
   * Sortierung der ClusterProviderIds, so dass die vom RMIClusterProvider als letzte in der Liste auftaucht.
   * TODO Konfigurierbarkeit der anderen ClusterProvider?
   * @param clusterInstances
   * @return
   */
  private static List<Long> getSortedClusterProviderIdListWithRMILast(Map<Long, ClusterProvider> clusterInstances) {
    LinkedList<Long> lll = new LinkedList<Long>();
    for( Map.Entry<Long, ClusterProvider> entry : clusterInstances.entrySet() ) {
      if( entry.getValue().getTypeName().equals( RMIClusterProvider.TYPENAME ) ) {
        lll.addLast( entry.getKey() );
      } else {
        lll.addFirst( entry.getKey() );
      }
    }
    return lll;
  }

  private void createSavedClusterInstances() throws PersistenceLayerException, XFMG_UnknownClusterProviderException,
      XFMG_InvalidStartParametersForClusterProviderException, XFMG_ClusterInitializationException,
      XFMG_InvalidConnectionParametersForClusterProviderException, XFMG_ClusterConnectionException {
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    long maxClusterInstanceId = -1;
    try {
      Collection<ClusterInstanceStorable> allInstances = con.loadCollection(ClusterInstanceStorable.class);
      for (ClusterInstanceStorable instance : allInstances) {
        if (instance.getClusterid() > maxClusterInstanceId) {
          maxClusterInstanceId = instance.getClusterid();
        }
        createClusterInternally(instance.getClustertype(), new String[] {String
            .valueOf(instance.getInternalclusterid())}, instance.getDescription(), ClusterCreationType.RESTORE, instance.getClusterid());
      }
    } finally {
      con.closeConnection();
    }
    currentClusterInstanceId = new AtomicLong(maxClusterInstanceId);
  }
  
  
  public FutureExecution getFutureExecutionsOnChangeHandler(long clusterInstanceId) {
    return futureExecutionsOnChangeHandler.get(clusterInstanceId);
  }
  

  private void registerSavedClusterProviders() throws PersistenceLayerException,
      XFMG_ClusterProviderFilesNotFoundException {
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      Collection<ClusterProviderStorable> allProviders = con.loadCollection(ClusterProviderStorable.class);
      for (ClusterProviderStorable provider : allProviders) {
        registerClusterProvider(provider.getClustertype(), false);
      }
    } finally {
      con.closeConnection();
    }
  }


  private void registerClusterProviderInternally(ClusterProviderFactory clusterProviderFactory) {
    synchronized (registeredClusterProviders) {
      registeredClusterProviders.put(clusterProviderFactory.getName(), clusterProviderFactory);
    }
  }


  
  public void shutdownDirectly() throws XynaException {
   if (initialized.compareAndSet(true, false)) {
      disconnectAll();
    }
  }
  

  /** 
   * Shutdown der einzelnen ClusterProvider aufrufen:
   * Achtung: Reihenfolge der einzelnen Shutdowns ist nicht beliebig, da der Shutdown eines 
   * ClusterProviders Auswirkungen auf die anderen ClusterProvider hat.
   * Daher wird RMIClusterProvider als letztes heruntergefahren
   * TODO Reihenfolge für weitere ClusterProvider konfigurierbar machen?
   */
  @Override
  protected void shutdown() throws XynaException {
    //aus cluster rausgehen
    if (initialized.compareAndSet(true, false)) {
      disconnectAll();
    }
  }
  
  private void disconnectAll() {
    Map<Long, ClusterProvider> copyOfMap = null;
    synchronized (clusterInstances) {
      copyOfMap = new HashMap<Long, ClusterProvider>(clusterInstances);
    }
    if( logger.isInfoEnabled() ) {
      logger.info("Disconnecting all ClusterProvider "+copyOfMap);
    }
    for( Long clusterProviderId : getSortedClusterProviderIdListWithRMILast( copyOfMap ) ) {
      copyOfMap.get(clusterProviderId).disconnect();
    }
  }


  public void leaveCluster(long clusterInstanceId) throws XFMG_UnknownClusterInstanceIDException {
    ClusterProvider clusterProvider;
    synchronized (clusterInstances) {
      clusterProvider = clusterInstances.get(clusterInstanceId);
    }
    clusterProvider.leaveCluster();
  }


  public void configureForCluster(String clusterableComponent, long clusterId)
      throws XFMG_ClusterComponentConfigurationException, PersistenceLayerException {

    Clustered component;
    synchronized (clusterableComponents) {
      component = clusterableComponents.get(clusterableComponent);
    }
    if (component == null) {
      throw new XFMG_ClusterComponentConfigurationException(clusterableComponent, clusterId);
    }
    component.enableClustering(clusterId);

    //speichern
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      try {
        con.persistObject(new ClusterComponentStorable(clusterableComponent, clusterId));
        con.commit();
      } catch (PersistenceLayerException e) {
        component.disableClustering();
        throw e;
      }
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Failed to close connection.", e);
      }
    }

  }


  public ClusterProvider getClusterInstance(long clusterId) throws XFMG_UnknownClusterInstanceIDException {
    synchronized (clusterInstances) {
      ClusterProvider instance = clusterInstances.get(clusterId);
      if (instance == null) {
        throw new XFMG_UnknownClusterInstanceIDException(clusterId);
      }
      return instance;
    }
  }


  public Map<Long, ClusterInformation> getClusterInstancesInformation() {
    Map<Long, ClusterInformation> map = new HashMap<Long, ClusterInformation>();
    synchronized (clusterInstances) {
      for (Map.Entry<Long, ClusterProvider> cpEntry : clusterInstances.entrySet()) {
        map.put(cpEntry.getKey(), cpEntry.getValue().getInformation());
      }
    }
    return map;
  }


  public ClusterParameterInformation[] getInformationForSupportedClusterTypes() {
    List<ClusterParameterInformation> cpiList = new ArrayList<ClusterParameterInformation>();
    synchronized (registeredClusterProviders) {
      for (Map.Entry<String, ClusterProviderFactory> entry : registeredClusterProviders.entrySet()) {
        ClusterParameterInformation cpi = new ClusterParameterInformation();
        ClusterProvider provider = entry.getValue().createProvider();
        cpi.connectionParameterInformation = provider.getNodeConnectionParameterInformation();
        cpi.initializationParameterInformation = provider.getStartParameterInformation();
        cpi.name = entry.getKey();
        cpiList.add(cpi);
      }
    }
    return cpiList.toArray(new ClusterParameterInformation[cpiList.size()]);
  }


  public long joinCluster(String clusterType, String[] parameter, String description)
      throws XFMG_UnknownClusterProviderException, XFMG_InvalidConnectionParametersForClusterProviderException,
      XFMG_ClusterConnectionException, PersistenceLayerException {
    try {
      return createClusterInternally(clusterType, parameter, description, ClusterCreationType.JOIN, -1);
    } catch (XFMG_InvalidStartParametersForClusterProviderException e) {
      throw new RuntimeException();
    } catch (XFMG_ClusterInitializationException e) {
      throw new RuntimeException();
    }
  }


  public Set<Clustered> listClusterableComponents() {
    synchronized (clusterableComponents) {
      return new HashSet<Clustered>(clusterableComponents.values());
    }
  }


  public void registerClusterableComponent(Clustered clusterableComponent) throws XFMG_ClusterComponentConfigurationException {
    synchronized (clusterableComponents) {
      clusterableComponents.put(clusterableComponent.getName(), clusterableComponent);
    }
    if (!savedClusterComponentsEnabled) {
      return; //enablement passiert im init.
    }

    //falls die clusterablecomponents (früher) bereits mit einer clusterinstance verknüpft wurde, aber diese
    //verknüpfung nach diesem serverstart noch nicht stattgefunden hat, passiert dies nun.
    //usecase: in ondeployment eines services wird eine clusterablecomponent registriert. zu diesem zeitpunkt
    //sind die clusterprovider bereits initialisiert.
    
    long clusterInstanceId;
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      try {
        ClusterComponentStorable ccs = new ClusterComponentStorable();
        ccs.componentname = clusterableComponent.getName();
        con.queryOneRow(ccs);
        clusterInstanceId = ccs.getClusterid();
      } finally {
        con.closeConnection();
      }
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      return; //not configured. ok
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
    
    try {
      clusterableComponent.enableClustering(clusterInstanceId);
    } catch (XFMG_UnknownClusterInstanceIDException e) {
      throw new RuntimeException(e); //sollte nicht passieren.
    }
  }


  private long createClusterInternally(String clusterType, String[] parameter, String description,
                                       ClusterCreationType clusterCreationType, long clusterInstanceId)
      throws XFMG_UnknownClusterProviderException, XFMG_InvalidStartParametersForClusterProviderException,
      XFMG_ClusterInitializationException, PersistenceLayerException,
      XFMG_InvalidConnectionParametersForClusterProviderException, XFMG_ClusterConnectionException {
    ClusterProviderFactory cpf;
    synchronized (registeredClusterProviders) {
      cpf = registeredClusterProviders.get(clusterType);
    }
    if (cpf == null) {
      throw new XFMG_UnknownClusterProviderException(clusterType);
    }
    ClusterProvider provider = cpf.createProvider();
    long internalClusterId = 0;
    boolean persist = true;
    switch (clusterCreationType) {
      case CREATE :
        internalClusterId = provider.createCluster(parameter);
        break;
      case JOIN :
        internalClusterId = provider.joinCluster(parameter);
        break;
      case RESTORE :
        provider.restoreClusterPrepare(Long.valueOf(parameter[0]));
        persist = false;
        break;
      default :
        throw new RuntimeException("unsupported " + ClusterCreationType.class.getSimpleName());
    }

    long id = clusterInstanceId; //ist nur beim restore schon bekannt.
    if (clusterInstanceId < 0) {
      id = currentClusterInstanceId.incrementAndGet();
    }

    if (persist) {
      //speichern
      ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        con.persistObject(new ClusterInstanceStorable(clusterType, id, internalClusterId, description));
        con.commit();
      } finally {
        con.closeConnection();
      }
    }

    synchronized (clusterInstances) {
      clusterInstances.put(id, provider);
    }

    CopyOnWriteArrayList<ClusterStateChangeHandler> list = new CopyOnWriteArrayList<ClusterStateChangeHandler>();
    synchronized (stateChangeHandlers) {
      stateChangeHandlers.put(id, list);
    }
    MgmtClusterStateChangeHandler clusterSpecificHandler = new MgmtClusterStateChangeHandler(id);
    provider.setClusterStateChangeHandler(clusterSpecificHandler);
    synchronized (providerStateChangeHandlers) {
      providerStateChangeHandlers.put(id, clusterSpecificHandler);
    }
    
    return id;
  }


  public long setupNewCluster(String clusterType, String[] parameter, String description)
      throws XFMG_UnknownClusterProviderException, XFMG_InvalidStartParametersForClusterProviderException,
      XFMG_ClusterInitializationException, PersistenceLayerException {
    try {
      return createClusterInternally(clusterType, parameter, description, ClusterCreationType.CREATE, -1);
    } catch (XFMG_InvalidConnectionParametersForClusterProviderException e) {
      throw new RuntimeException();
    } catch (XFMG_ClusterConnectionException e) {
      throw new RuntimeException();
    }
  }


  private void registerClusterProvider(final String clusterType, boolean persist)
      throws XFMG_ClusterProviderFilesNotFoundException, PersistenceLayerException {

    final String fqClassName = Constants.CLUSTER_PROVIDER_BASE_PACKAGE + "." + clusterType;
    ClusterProviderClassLoader classloader;
    try {
      classloader = new ClusterProviderClassLoader(fqClassName);
    } catch (XFMG_JarFolderNotFoundException e) {
      throw new XFMG_ClusterProviderFilesNotFoundException(clusterType, e);
    }
    // FIXME SPS prio5: bei classloaderdispatcher registrieren, damit man mit listclassloaderinfos über die cli infos über
    //           die classloaders bekommen kann.
    //           aufpassen: auch wieder deregistrieren, wenn persistencelayer undeployed wird. z.B.:
    // ClassLoaderDispatcherFactory.getInstance().getImpl().registerPersistenceLayerClassLoader(persLayerClassLoader);
    final Class<? extends ClusterProvider> clusterProviderClass;
    try {
      clusterProviderClass = (Class<? extends ClusterProvider>) classloader.loadClass(fqClassName);
    } catch (ClassNotFoundException e) {
      throw new XFMG_ClusterProviderFilesNotFoundException(clusterType, e);
    }

    if (persist) {
      //speichern
      ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        con.persistObject(new ClusterProviderStorable(clusterType));
        con.commit();
      } finally {
        con.closeConnection();
      }
    }

    registerClusterProviderInternally(new ClusterProviderFactory() {

      public ClusterProvider createProvider() {
        try {
          return clusterProviderClass.getConstructor().newInstance();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }


      public String getName() {
        return clusterType;
      }

    });
  }


  public void registerClusterProvider(final String clusterType) throws XFMG_ClusterProviderFilesNotFoundException,
      PersistenceLayerException {
    registerClusterProvider(clusterType, true);
  }


  public void addClusterStateChangeHandler(long clusterInstanceId, ClusterStateChangeHandler clusterStateChangeHandler) {
    synchronized (stateChangeHandlers) {
      List<ClusterStateChangeHandler> list = stateChangeHandlers.get(clusterInstanceId);
      list.add(clusterStateChangeHandler);
    }
  }


  public void removeClusterStateChangeHandler(long clusterInstanceId, ClusterStateChangeHandler handler) {
    synchronized (stateChangeHandlers) {
      List<ClusterStateChangeHandler> list = stateChangeHandlers.get(clusterInstanceId);
      list.remove(handler);
    }
  }

  public void readyForClusterStateChange(long clusterInstanceId) {
    MgmtClusterStateChangeHandler mcsch = null;
    synchronized (providerStateChangeHandlers) {
      mcsch = providerStateChangeHandlers.get(clusterInstanceId);
    }
    if( mcsch != null ) {
      mcsch.tryOnChange();
    }
    ClusterProvider cp = null;
    synchronized ( clusterInstances ) {
      cp = clusterInstances.get(clusterInstanceId);
    }
    cp.readyForStateChange();
  }
  
  public void setGlobalReadyForChange(boolean ready) {
    globalReadyForChange = ready;
    synchronized (providerStateChangeHandlers) {
      for(MgmtClusterStateChangeHandler mcsch : providerStateChangeHandlers.values()) {
        mcsch.setGlobalReadyForChange(ready);
      }
    }
    if(ready) {
      synchronized (clusterInstances) {
        for(ClusterProvider provider : clusterInstances.values()) {
          provider.readyForStateChange();
        }
      }
    }
  }
  
  public void setGlobalReadyForChange(long clusterinstanceId, boolean value) {
    synchronized (providerStateChangeHandlers) {
      MgmtClusterStateChangeHandler mcsch = providerStateChangeHandlers.get(clusterinstanceId);
      mcsch.setGlobalReadyForChange(value);
    }
    if(value) {
      synchronized (clusterInstances) {
        ClusterProvider provider = clusterInstances.get(clusterinstanceId);
        provider.readyForStateChange();
      }
    }
  }
  
  private class FutureExecutionTaskOnClusterChange extends FutureExecutionTask implements ClusterStateChangeHandler {

    private String name;
    private long clusterinstanceId;
    private boolean clustered;
    /**
     * nur gesetzt, falls state != starting
     */
    private ClusterState changedStateNotSTARTING = null;
    private Object waitObject = new Object();
        
    public FutureExecutionTaskOnClusterChange(int id, String name) {
      super(id, DEFAULT_PRIORITY - 1);
      this.name = name;
      clustered = false;
    }
    
    public void setClusterInstanceId(long id) {
      clusterinstanceId = id;
      clustered = true;
    }

    public boolean isReadyForChange(ClusterState newState) {
      return true;
    }

    public void onChange(ClusterState newState) {
      if (logger.isDebugEnabled()) {
        logger.debug(name + " got notified of cluster state change to " + newState);
      }
      if (newState != ClusterState.STARTING) {
        setGlobalReadyForChange(clusterinstanceId, false);
        removeClusterStateChangeHandler(clusterinstanceId, this);
        synchronized (waitObject) {
          changedStateNotSTARTING = newState;
          waitObject.notifyAll();
        }
      }
    }

    @Override
    public void execute() {
      if (clustered) {
        synchronized (waitObject) {
          if (changedStateNotSTARTING != null) {
            // ok, der state ist nicht mehr STARTING - ein aktueller ClusterState ist gesetzt --> alles ok
            if(logger.isDebugEnabled()) {
              logger.debug(name + ": onChange has already been called. FutureExecutionTaskOnClusterChange doesn't have to wait.");
            }
            return;
          } else {
            //FIXME whileschleife um spurious interrupts auszuschliessen
            // der aktuelle State ist noch STARTING -> wir warten, bis wir notifiziert werden
            try {
              if(logger.isDebugEnabled()) {
                logger.debug(name + ": wait until onchange is called.");
              }
              waitObject.wait();
            } catch (InterruptedException e) {
              return;
            }
          }
        }
      }
    }
    
    public String toString() {
      return name;
    }
    
    @Override
    public int[] after() {
      return new int[] {XynaClusteringServicesManagementInterface.FUTURE_EXECUTION_ID__CLUSTERING_SERVICES_CLUSTERED_COMPONENTS};
    }

    
  }

  public void changeClusterState(ClusterState clusterState) {
    Map<Long, ClusterProvider> copyOfMap = null;
    synchronized (clusterInstances) {
      copyOfMap = new HashMap<Long, ClusterProvider>(clusterInstances);
    }
    if( logger.isInfoEnabled() ) {
      logger.info("changeClusterState on all ClusterProvider "+copyOfMap+" to "+clusterState);
    }
    for (Map.Entry<Long, ClusterProvider> clusterProviderEntry : copyOfMap.entrySet()) {
      clusterProviderEntry.getValue().changeClusterState(clusterState);
    }
  }

}
