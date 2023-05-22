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
package com.gip.xyna.xfmg.xfctrl.revisionmgmt;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.CycleUtils;
import com.gip.xyna.utils.misc.CycleUtils.CycleController;
import com.gip.xyna.xact.trigger.Filter;
import com.gip.xyna.xact.trigger.FilterInformation.FilterInstanceInformation;
import com.gip.xyna.xact.trigger.Trigger;
import com.gip.xyna.xact.trigger.TriggerInformation.TriggerInstanceInformation;
import com.gip.xyna.xact.trigger.XynaActivationTrigger;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.exceptions.XFMG_DuplicateVersionForApplicationName;
import com.gip.xyna.xfmg.exceptions.XFMG_DuplicateWorkspace;
import com.gip.xyna.xfmg.exceptions.XFMG_RunningOrdersException;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownClusterInstanceIDException;
import com.gip.xyna.xfmg.xclusteringservices.ClusterState;
import com.gip.xyna.xfmg.xclusteringservices.ClusterStateChangeHandler;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagement;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable.ApplicationEntryType;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.OrderEntrance;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RevisionOrderControl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RuntimeContextProblem;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RuntimeContextProblem.Collision;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RuntimeContextProblem.Collision.RuntimeContextCollisionType;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RuntimeContextProblem.Cycle;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RuntimeContextProblem.UnresolvableRequirement;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemIdentifier;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemRegistry;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateImpl;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateManagementImpl;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateReport;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.CrossRevisionResolver;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.RevisionBasedCrossResolver;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.RevisionIdentifer;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.XMOMVersionStorable;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeParameter;
import com.gip.xyna.xfmg.xods.ordertypemanagement.SearchOrdertypeParameter;
import com.gip.xyna.xnwh.exceptions.XNWH_NoPersistenceLayerConfiguredForTableException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_PersistenceLayerInstanceIdUnknownException;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.ODSImpl.ClusteredStorableConfigChangeHandler;
import com.gip.xyna.xnwh.persistence.ODSImpl.PersistenceLayerInstances;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.PreparedQueryCache;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.xclusteringservices.lockinginterface.ClusterLockingInterface;
import com.gip.xyna.xnwh.xclusteringservices.lockinginterface.DatabaseLock;
import com.gip.xyna.xprc.exceptions.XPRC_TimeoutWhileWaitingForUnaccessibleOrderException;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement;
import com.gip.xyna.xprc.xfractwfe.OrdersInUse;
import com.gip.xyna.xprc.xfractwfe.OrdersInUse.FillingMode;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xprcods.ordercontextconfiguration.OrderContextConfiguration;


public class RevisionManagement extends FunctionGroup implements ClusterStateChangeHandler {

  private static final Logger logger = CentralFactoryLogging.getLogger(RevisionManagement.class);

  private final static int DEFAULT_FILE_REMOVAL_RETRIES = 20;
  
  public static final String DEFAULT_NAME = "RevisionManagement";
  
  public static final Long REVISION_DEFAULT_WORKSPACE = -1L;
  public static final Long REVISION_DATAMODEL = -2L;
  
  public static final Workspace DEFAULT_WORKSPACE = new Workspace("default workspace");
  
  private static final Map<Long, SpecialRevision> SPECIAL_REVISIONS = initSpecialRevisions();
  
  private static Map<Long, SpecialRevision> initSpecialRevisions() {
    Map<Long, SpecialRevision> specialRevisions = new ConcurrentHashMap<Long,SpecialRevision>();
    
    SpecialRevision defWorkspace = new 
        SpecialRevision(DEFAULT_WORKSPACE, true, 
                        Constants.PREFIX_REVISION + Constants.SUFFIX_REVISION_WORKINGSET );
    SpecialRevision datamodel = new 
        SpecialRevision(new DataModel("datamodel"), false,
                        Constants.PREFIX_REVISION + Constants.SUFFIX_REVISION_DATAMODEL );
    
    specialRevisions.put(REVISION_DEFAULT_WORKSPACE, defWorkspace );
    specialRevisions.put(REVISION_DATAMODEL, datamodel );
    return specialRevisions;
  }
    
  private ODS ods;
  private ClusterState currentStorableState;
  private Integer ownBinding;
  private PreparedQueryCache queryCache;
  
  private String sqlQueryXMOMVersionForRevision;
  private String sqlQueryXMOMVersionForOwnBinding;
  private String sqlQueryXMOMVersionForApplicationAndVersionForAllBindings;
  private String sqlQueryXMOMVersionForWorkspaceForAllBindings;
  
  //aus Performancegr�nden werden die Zuordnungen Revision vs. Application bzw. Workspace gecached.
  //die Maps enthalten auch den Default-Workspace
  private Map<Long, Application> applications;
  private Map<Long, Workspace> workspaces;
  private Map<RuntimeContext, Long> revisions;
  
  public RevisionManagement() throws XynaException {
    super();
  }
  
  private RevisionManagement(String cause) throws XynaException {
    super(cause);
    
    ods = ODSImpl.getInstance();
    ods.registerStorable(XMOMVersionStorable.class);
    
    transferFromODSTypeToODSType(ODSConnectionType.HISTORY, ODSConnectionType.DEFAULT, XMOMVersionStorable.TABLE_NAME,
                                 XMOMVersionStorable.class);
    
    queryCache = new PreparedQueryCache();
    sqlQueryXMOMVersionForOwnBinding = "select * from " + XMOMVersionStorable.TABLE_NAME + " where " + 
                    XMOMVersionStorable.COL_BINDING + " = ?";
    
    recreateCache();
  }


  public static RevisionManagement getRevisionManagementPreInit() throws XynaException {
    return new RevisionManagement("preInit");
  }
  
  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  @Override
  protected void init() throws XynaException {
    
    ods = ODSImpl.getInstance();
    ods.registerStorable(RevisionIdentifer.class);
    ods.registerStorable(XMOMVersionStorable.class);
    
    transferFromODSTypeToODSType(ODSConnectionType.HISTORY, ODSConnectionType.DEFAULT, XMOMVersionStorable.TABLE_NAME,
                                 XMOMVersionStorable.class);
    transferFromODSTypeToODSType(ODSConnectionType.HISTORY, ODSConnectionType.DEFAULT, RevisionIdentifer.TABLE_NAME,
                                 RevisionIdentifer.class);

    queryCache = new PreparedQueryCache();
    
    currentStorableState = ClusterState.NO_CLUSTER;
    ownBinding = XynaClusteringServicesManagement.DEFAULT_BINDING_NO_CLUSTER;
    
    sqlQueryXMOMVersionForApplicationAndVersionForAllBindings = "select * from " + XMOMVersionStorable.TABLE_NAME + " where " + 
                    XMOMVersionStorable.COL_APPLICATION + " = ? and " + XMOMVersionStorable.COL_VERSIONNAME + " = ?";
    sqlQueryXMOMVersionForWorkspaceForAllBindings = "select * from " + XMOMVersionStorable.TABLE_NAME + " where " + 
                    XMOMVersionStorable.COL_WORKSPACE + " = ?";
    sqlQueryXMOMVersionForRevision = "select * from " + XMOMVersionStorable.TABLE_NAME + " where " + 
                    XMOMVersionStorable.COL_REVISION + " = ? and " + XMOMVersionStorable.COL_BINDING + " = ?";
    sqlQueryXMOMVersionForOwnBinding = "select * from " + XMOMVersionStorable.TABLE_NAME + " where " + 
                    XMOMVersionStorable.COL_BINDING + " = ?";
    
    recreateCache();
    
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    
    fExec.addTask("RevisionManagement.clustering", "RevisionManagement.clustering").
      before(XynaClusteringServicesManagement.class).
      after(PersistenceLayerInstances.class).
      execAsync( new Runnable() {
        @Override
        public void run() {
        ods.addClusteredStorableConfigChangeHandler(new ClusteredStorableConfigChangeHandler() {

          public void enableClustering(long clusterInstanceId) {
            try {
              currentStorableState = XynaClusteringServicesManagement.getInstance().getClusterInstance(clusterInstanceId)
                              .getState();
            } catch (XFMG_UnknownClusterInstanceIDException e) {
              logger.error("clusterinstanceid " + clusterInstanceId + " unknown", e);
              throw new RuntimeException(e);
            }
            XynaClusteringServicesManagement.getInstance().addClusterStateChangeHandler(clusterInstanceId,
                                                                                        RevisionManagement.this);
            ownBinding = null;
          }

          public void disableClustering() {
            currentStorableState = ClusterState.NO_CLUSTER;
            ownBinding = XynaClusteringServicesManagement.DEFAULT_BINDING_NO_CLUSTER;
          }
          
        }, ODSConnectionType.DEFAULT, XMOMVersionStorable.class);
      }
      
    } );
 
     
    fExec.addTask(RevisionManagement.class, "RevisionManagement").
          before(XynaActivationTrigger.FUTUREEXECUTION_ADDTRIGGER_ID).
          after(XynaClusteringServicesManagement.class).
          after("RevisionManagement.clustering").
          execAsync( new Runnable() { public void run() { 
            ownBinding = null;
            getOwnBinding();
            recreateCache();
            runtimeContextCycleController = new RuntimeContextCycleController();
            }
          });
  }


  @Override
  protected void shutdown() throws XynaException {
    if (ods != null) {
      ods.unregisterStorable(XMOMVersionStorable.class);
      ods.unregisterStorable(RevisionIdentifer.class);
    }
  }
  
  
  public void deleteRevision(Long revision) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    ODSConnection con = ods.openConnection();
    try {
      PreparedQuery<? extends XMOMVersionStorable> query = queryCache
                      .getQueryFromCache(sqlQueryXMOMVersionForRevision, con, XMOMVersionStorable.getStaticReader());
      XMOMVersionStorable xmomversion = con.queryOneRow(query, new Parameter(revision, getOwnBinding()));
      
      if (xmomversion == null) {
         throw new XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY("No object found for revision " + revision,
                        XMOMVersionStorable.TABLE_NAME);
      }
      
      if(logger.isDebugEnabled()) {
        logger.debug("Delete xmomversion " + xmomversion.toString());
      }
      
      con.deleteOneRow(xmomversion);
      con.commit();
      
      removeFromCache(xmomversion.getRevision());
      
      transferFromODSTypeToODSType(ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY, XMOMVersionStorable.TABLE_NAME,
                                   XMOMVersionStorable.class);  
    } finally {
      con.closeConnection();
    }
  }
  
  
  private void recreateCache() {
    ODSConnection con = ods.openConnection();
    try {
      applications = new ConcurrentHashMap<Long, Application>();
      workspaces = new ConcurrentHashMap<Long, Workspace>();
      revisions = new ConcurrentHashMap<RuntimeContext, Long>();
      for( Map.Entry<Long,SpecialRevision> entry : SPECIAL_REVISIONS.entrySet() ) {
        SpecialRevision sr = entry.getValue();
        if( sr.isVisible() ) {
          putIntoCache(entry.getKey(), sr.getRuntimeContext() );
        } else {
          //Nur in Revisions eintragen
          revisions.put(sr.getRuntimeContext(), entry.getKey() );
        }
      }
      PreparedQuery<? extends XMOMVersionStorable> query = queryCache
                      .getQueryFromCache(sqlQueryXMOMVersionForOwnBinding, con, XMOMVersionStorable.getStaticReader());
      List<? extends XMOMVersionStorable> xmomversions = con.query(query, new Parameter(getOwnBinding()), -1);
      
      for(XMOMVersionStorable xmomversion : xmomversions) {
        RuntimeContext runtimeContext;
        if (xmomversion.getApplication() != null && xmomversion.getApplication().length() > 0) {
          runtimeContext = new Application(xmomversion.getApplication(), xmomversion.getVersionName());
        } else {
          runtimeContext = new Workspace(xmomversion.getWorkspace());
        }
        putIntoCache(xmomversion.getRevision(), runtimeContext);
      }
    } catch (PersistenceLayerException e) {
      logger.error("Could not load xmomversion table.", e);
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Can't close connection.", e);
      }
    }
  }
  
  
  private void putIntoCache(Long revision, RuntimeContext runtimeContext) {
    if (runtimeContext instanceof Application) {
      applications.put(revision, (Application) runtimeContext);
    }
    if (runtimeContext instanceof Workspace) {
      workspaces.put(revision, (Workspace) runtimeContext);
    }
    revisions.put(runtimeContext, revision);
  }
  
  
  private void removeFromCache(Long revision) {
    RuntimeContext removed = applications.remove(revision);
    if (removed == null) {
      removed = workspaces.remove(revision);
    }
    revisions.remove(removed);
  }
  
  public Map<Long, Workspace> getWorkspaces() {
    return Collections.unmodifiableMap(workspaces);
  }

  public Collection<Application> getApplications() {
    return applications.values();
  }
  
  /**
   * Ermittelt die Revisionsnummer f�r eine Version einer Application oder einen Workspace.
   * Der ApplicationName und WorkspaceName d�rfen nicht beide gesetzt sein.
   * Falls keiner von beiden gesetzt ist, wird die Revision des Default-Workspaces zur�ckgeliefert.
   * Falls nur der Applicationsname gegeben ist, wird die h�chste Revision (== neuste Version)
   * zur�ckgeliefert.
   * 
   */
  public Long getRevision(String applicationName, String versionName, String workspaceName) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    RuntimeContext runtimeContext = getRuntimeContext(applicationName, versionName, workspaceName);
    return getRevision(runtimeContext);
  }
  
  
  public Long getRevision(RuntimeContext runtimeContext) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    if (runtimeContext == null || runtimeContext.getName() == null) {
      return REVISION_DEFAULT_WORKSPACE;
    }
    
    Long revision = revisions.get(runtimeContext);
    if(revision != null) {
      return revision;
    }
    
    //falls versionName == null die h�chste Revision suchen
    if (runtimeContext instanceof Application) {
      if (((Application)runtimeContext).getVersionName() == null) {
        
        revision = -1L;
        for (Long revNumber : applications.keySet()) {
          if(runtimeContext.getName().equals(applications.get(revNumber).getName())) {
            if (revNumber > revision) {
              revision = revNumber;
            }
          }
        }
        
        if (revision != -1) {
          return revision;
        }
      }
    }
    
    if (runtimeContext instanceof DataModel ) {
      return REVISION_DATAMODEL;
    }
    
    throw new XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY(runtimeContext.toString(),
                                                    XMOMVersionStorable.TABLE_NAME);
  }
  
  /**
   * Liefert ein RuntimeContext-Objekt f�r eine Application-Version bzw. einen Workspace
   * @param applicationName
   * @param versionName
   * @param workspaceName
   * @return
   */
  public static RuntimeContext getRuntimeContext(String applicationName, String versionName, String workspaceName) {
    if (applicationName != null && workspaceName != null) {
      throw new IllegalArgumentException("'applicationName' and 'workspaceName' may not both be set");
    }
    
    if (applicationName != null) {
      return new Application(applicationName, versionName);
    }
    
    if (workspaceName != null) {
      return new Workspace(workspaceName);
    }
    
    return DEFAULT_WORKSPACE;
  }
  
  
  /**
   * Liefert den RuntimeContext zur Revision
   * @param revision
   * @return
   * @throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY
   */
  public RuntimeContext getRuntimeContext(Long revision) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    RuntimeContext rc = applications.get(revision);
    if( rc != null ) {
      return rc;
    }
    rc = workspaces.get(revision);
    if( rc != null ) {
      return rc;
    }
    
    SpecialRevision sr = SPECIAL_REVISIONS.get(revision); 
    if( sr != null ) {
      return sr.getRuntimeContext();
    }
    
    throw new XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY("No object found for revision " + revision,
                                                    XMOMVersionStorable.TABLE_NAME);
  }
  
  
  public Application getApplication(Long revision) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    RuntimeContext application = getRuntimeContext(revision);
    
    if (application instanceof Application) {
      return (Application) application;
    }
    
    throw new XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY("No application object found for revision " + revision,
                                                    XMOMVersionStorable.TABLE_NAME);
  }

  public Workspace getWorkspace(Long revision) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    RuntimeContext workspace = getRuntimeContext(revision);
    
    if (workspace instanceof Workspace) {
      return (Workspace) workspace;
    }
    
    throw new XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY("No workspace object found for revision " + revision,
                                                    XMOMVersionStorable.TABLE_NAME);
  }
  
  /**
   * �berpr�ft, ob die revision zu einem Workspace geh�rt.
   * @param revision
   * @return
   */
  public boolean isWorkspaceRevision(Long revision) {
    if (revision == null) {
      return false;
    }
    
    return workspaces.containsKey(revision);
  }
  
  
  /**
   * Legt eine neue Revision f�r eine neue Application-Version an.
   * @param applicationName
   * @param newVersion
   * @return
   * @throws XFMG_DuplicateVersionForApplicationName
   * @throws PersistenceLayerException
   */
  public Long buildNewRevisionForNewVersion(String applicationName, String newVersion) throws XFMG_DuplicateVersionForApplicationName, PersistenceLayerException {
    return buildNewRevisionForNewVersion(applicationName, newVersion, null);
  }


  public long buildNewRevisionForNewVersion(String applicationName, String newVersion, Long preferredRevision) throws XFMG_DuplicateVersionForApplicationName, PersistenceLayerException {
    Long revision = null;
    
    DatabaseLock lock = XynaFactory.getInstance().getXynaNetworkWarehouse().getXynaClusteringServices().getClusterLockingInterface()
                                     .createLockIfNonexistent(DEFAULT_NAME,ClusterLockingInterface.DatabaseLockType.ExternalConnection);
    ODSConnection con = ods.openConnection();
    lock.lock(con);
    boolean success = false;
    try {
      PreparedQuery<? extends XMOMVersionStorable> query = queryCache
                        .getQueryFromCache(sqlQueryXMOMVersionForApplicationAndVersionForAllBindings, con, XMOMVersionStorable.getStaticReader());
      List<? extends XMOMVersionStorable> xmomversions = con.query(query, new Parameter(applicationName, newVersion), -1);
      
      for(XMOMVersionStorable xmomversion : xmomversions) {
        if (xmomversion.getApplication().equals(applicationName) && xmomversion.getVersionName().equals(newVersion) && xmomversion
                        .getBinding() == getOwnBinding()) {
          throw new XFMG_DuplicateVersionForApplicationName(applicationName, newVersion);
        }
        if(xmomversion.getApplication().equals(applicationName) && xmomversion.getVersionName().equals(newVersion) && xmomversion
                        .getBinding() != getOwnBinding()) {
          if(logger.isDebugEnabled()) {
            logger.debug("Other node with binding " + xmomversion.getBinding() +
                         " has already created the revision " + revision + " for application " + applicationName + " " + newVersion);
          }
          revision = xmomversion.getRevision();
        }
      }

      if (preferredRevision != null && revision != null && !preferredRevision.equals(revision)) {
        throw new RuntimeException("Could not use preferred revision. Other node already created revision " + revision
            + " for this application.");
      }

      if (revision == null) {
        if (logger.isDebugEnabled()) {
          logger.debug("No revision found - create new one." + (preferredRevision != null ? "Preferred Revision: " + preferredRevision : ""));
        }
        revision = createNewRevision(preferredRevision);
      }
      
      Application application = new Application(applicationName, newVersion);
      
      XMOMVersionStorable xmomversion = new XMOMVersionStorable(application, revision, getOwnBinding());
      con.persistObject(xmomversion);
      lock.commit();
      success = true;

      putIntoCache(revision, application);
    } finally {
      if (!success) {
        lock.rollback();
      }
      lock.unlock();
    }

    // do this after unlocking the lock since that will do the actual commit. doing this outside the database lock is not dangerous
    // since in cluster mode the following copy wont have any effect since default = history
    transferFromODSTypeToODSType(ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY, XMOMVersionStorable.TABLE_NAME,
                                 XMOMVersionStorable.class);
    return revision;
  }
  
  
  /**
   * Legt eine neue Revision f�r eine neuen Workspace an.
   * @param workspace
   * @return
   * @throws PersistenceLayerException
   * @throws XFMG_DuplicateWorkspace
   */
  public long buildNewRevisionForNewWorkspace(Workspace workspace) throws PersistenceLayerException, XFMG_DuplicateWorkspace {
    if (revisions.containsKey(workspace)) {
      throw new XFMG_DuplicateWorkspace(workspace.getName());
    }
    
    Long revision = null;
    DatabaseLock lock = XynaFactory.getInstance().getXynaNetworkWarehouse().getXynaClusteringServices().getClusterLockingInterface()
                    .createLockIfNonexistent(DEFAULT_NAME,ClusterLockingInterface.DatabaseLockType.ExternalConnection);
    ODSConnection con = ods.openConnection();
    lock.lock(con);
    boolean success = false;
    try {
      PreparedQuery<? extends XMOMVersionStorable> query = queryCache
                      .getQueryFromCache(sqlQueryXMOMVersionForWorkspaceForAllBindings, con, XMOMVersionStorable.getStaticReader());
      List<? extends XMOMVersionStorable> xmomversions = con.query(query, new Parameter(workspace.getName()), -1);
      
      for(XMOMVersionStorable xmomversion : xmomversions) {
        if (xmomversion.getBinding() == getOwnBinding()) {
          throw new XFMG_DuplicateWorkspace(workspace.getName());
        } else {
          if(logger.isDebugEnabled()) {
            logger.debug("Other node with binding " + xmomversion.getBinding() +
                         " has already created the revision " + revision + " for workspace " + workspace.getName());
          }
          revision = xmomversion.getRevision();
        }
      }
      
      if(revision == null) {
        // neue unbenutzte Revisionen erzeugen und anlegen
        logger.debug("No revision found - create new one");
        revision = createNewRevision(null);
      }
      
      XMOMVersionStorable xmomversion = new XMOMVersionStorable(workspace, revision, getOwnBinding());
      con.persistObject(xmomversion);
      lock.commit();
      success = true;
      
      putIntoCache(revision, workspace);
    } finally {
      if (!success) {
        lock.rollback();
      }
      lock.unlock();
    }
    
    // do this after unlocking the lock since that will do the actual commit. doing this outside the database lock is not dangerous
    // since in cluster mode the following copy wont have any effect since default = history
    transferFromODSTypeToODSType(ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY, XMOMVersionStorable.TABLE_NAME,
                                 XMOMVersionStorable.class);
    return revision;
  }

  
  private long createNewRevision(Long preferredRevision) throws PersistenceLayerException {
    
    // eigene Connection, weil ggf. ein Rollback durchgef�hrt wird
    ODSConnection con = ods.openConnection();
    try {      
      RevisionIdentifer revision = new RevisionIdentifer(RevisionIdentifer.REVISION_ID);
      try {
        con.queryOneRowForUpdate(revision);
        logger.debug("Got lock for generate new revision");
      } catch(XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        // Revision muss erst noch angelegt werden ...
        logger.debug("No revision found. Create new one.");
        revision.setMaxrevision(0L);
        if(con.persistObject(revision)) {
          // hmmm, offensichtlich gab's das Objekt jetzt doch. Ein anderer Knoten scheint ebenfalls eine Revision angelegt zu haben.
          logger.debug("Persist was an update. So it's seems the other node was faster ... rollback my new revision entry");
          con.rollback();
        }
        try {
          con.queryOneRowForUpdate(revision);
          logger.debug("Got lock for generate new revision");
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
           throw new RuntimeException("Can't initalize revision.");
        }
      }
      
      long nextrevision;
      if (preferredRevision == null) {
        nextrevision = revision.getMaxrevision() + 1;
        revision.setMaxrevision(nextrevision);
      } else {
        checkRevisionInUse(preferredRevision);
        nextrevision = preferredRevision;
        revision.setMaxrevision(Math.max(revision.getMaxrevision(), preferredRevision));
      }
      
      con.persistObject(revision);
      con.commit();
      
      if(logger.isDebugEnabled()) {
        logger.debug("New revision is " + revision);
      }
      
      transferFromODSTypeToODSType(ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY, RevisionIdentifer.TABLE_NAME,
                                   RevisionIdentifer.class);
      
      return nextrevision; 
    } finally {
      con.closeConnection();
    }
  }
  
  public static void createNewRevisionDirectory(long revision) {
    for(PathType pathType : PathType.values()) {
      File dir = new File(getPathForRevision(pathType, revision));
      dir.mkdirs();
    }
  }
  
  
  /**
   * Liefert alle Revisionsnummern der Applications und Workspaces 
   * @return
   */
  public List<Long> getAllRevisions() {
    List<Long> result = new ArrayList<Long>();
    result.addAll(revisions.values()); //der Default-Workspace ist in revisions enthalten
    
    return result;
  }

  /**
   * Liefert die Revisionsnummern aller Applications
   * @return
   */
  public List<Long> getAllApplicationRevisions() {
    List<Long> result = new ArrayList<Long>();
    result.addAll(applications.keySet());
    return result;
  }

  /**
   * Liefert die Revisionsnummern aller Workspaces
   * @return
   */
  public List<Long> getAllWorkspaceRevisions() {
    List<Long> result = new ArrayList<Long>();
    result.addAll(workspaces.keySet());
    return result;
  }

  
  public static String getPathForRevision(PathType pathtype, long revision) {
    return getPathForRevision(pathtype, revision, true);
  }
  
  /**
   * hat am ende keinen fileseparator, ausser bei shared libs und root.
   * 
   * Achtung: Diese Methode wird auch f�r Updates verwendet.
   * �ndert sich ein Pfad, muss in com.gip.xyna.update.VersionDependentPath
   * eine neue Konstante erstellt werden.
   */
  public static String getPathForRevision(PathType pathtype, Long revision, boolean deployed) {
    StringBuilder builder = new StringBuilder();
    builder.append("..").append(Constants.fileSeparator).append(Constants.REVISION_PATH).append(Constants.fileSeparator);
    if( SPECIAL_REVISIONS.containsKey( revision ) ) {
      SpecialRevision sr = SPECIAL_REVISIONS.get(revision);
      builder.append(sr.getFolder());
    } else {
      builder.append(Constants.PREFIX_REVISION).append(revision);
    }
    builder.append(Constants.fileSeparator);
    if (!deployed) {
      builder.append(Constants.PREFIX_SAVED).append(Constants.fileSeparator);
    }
    switch(pathtype) {
      case XMOM:
        builder.append(Constants.SUBDIR_XMOM);
        break;
      case TRIGGER:
        builder.append(Constants.SUBDIR_TRIGGER);
        break;
      case FILTER:
        builder.append(Constants.SUBDIR_FILTER);
        break;
      case SERVICE:
        builder.append(Constants.SUBDIR_SERVICES);
        break;
      case SHAREDLIB:
        builder.append(Constants.SUBDIR_SHAREDLIBS).append(Constants.fileSeparator); //FIXME einheitlich machen
        break;
      case XMOMCLASSES:
        builder.append(Constants.SUBDIR_XMOMCLASSES);
        break;
      case THIRD_PARTIES:
        builder.append(Constants.SUBDIR_THIRD_PARTIES);
        break;
    }
    return builder.toString();
  }

  
  private static class RevisionXmomFilter implements FileFilter {

    long revision;


    public RevisionXmomFilter(long revision) {
      this.revision = revision;
    }


    public boolean accept(File pathname) {
      if (pathname.getPath().equals(getPathForRevision(PathType.XMOM, revision))) {
        return true;
      }
      return false;
    }
  }
  
  
  public static void removeRevisionFolder(long revision, boolean keepForAudit) {
    File revFolder = new File(getPathForRevision(PathType.ROOT, revision));
    if (logger.isInfoEnabled()) {
      logger.info("Remove folder " + revFolder.getAbsolutePath());
    }
    if (keepForAudit) {
      FileUtils.deleteAllBut(revFolder, new RevisionXmomFilter(revision));
    } else {
      FileUtils.deleteDirectoryRecursively(revFolder, DEFAULT_FILE_REMOVAL_RETRIES);
    }
  }
  
  
  public static void removeSharedLib(String name, long revision) {
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
        .removeSharedLibClassLoader(name, revision);
    if (name != null) {
      File sharedLibDir = new File(getPathForRevision(PathType.SHAREDLIB, revision), name);
      if (!FileUtils.deleteDirectoryRecursively(sharedLibDir, DEFAULT_FILE_REMOVAL_RETRIES)) {
        logger.debug("could not remove shared lib " + sharedLibDir.getAbsolutePath());
      }
      //SharedLib aus DependencyRegister entfernen
      DependencyRegister dependencyRegister = XynaFactory.getInstance().getFactoryManagementPortal().getXynaFactoryControl().getDependencyRegister();
      dependencyRegister.removeDependencyNode(name, DependencySourceType.SHAREDLIB, revision);
    }
  }

  private Integer getOwnBinding() {
    if(ownBinding == null) {
      XMOMVersionStorable tmpInstance = new XMOMVersionStorable();
      ownBinding = tmpInstance.getLocalBinding(ODSConnectionType.DEFAULT);
    }
    return ownBinding;
  }

  private void transferFromODSTypeToODSType(ODSConnectionType from, ODSConnectionType to, String tablename,
                                            Class<? extends Storable<?>> clazz)
                  throws XNWH_NoPersistenceLayerConfiguredForTableException,
                  XNWH_PersistenceLayerInstanceIdUnknownException, PersistenceLayerException {
    boolean areHistoryAndDefaultTheSame = ods.isSamePhysicalTable(tablename, from, to);
    if (!areHistoryAndDefaultTheSame) {
      ods.replace(clazz, from, to);
    }
  }

  public boolean isReadyForChange(ClusterState newState) {
    return true;
  }

  public void onChange(ClusterState newState) {
    // Hier muss eigentlich nur das Binding migriert werden, wenn ein Cluster konfiguriert wird.
    switch(newState) {
      case SINGLE:
        if(currentStorableState == ClusterState.NO_CLUSTER) {
          ODSConnection con = ods.openConnection();
          try {
            Collection<XMOMVersionStorable> allVersions = con.loadCollection(XMOMVersionStorable.class);
            List<XMOMVersionStorable> newVersions = new ArrayList<XMOMVersionStorable>();
            for(XMOMVersionStorable version : allVersions) {
              XMOMVersionStorable newVersion = new XMOMVersionStorable(version.getApplication(), version.getVersionName(), version.getRevision(), getOwnBinding());
              newVersion.setWorkspace(version.getWorkspace());
              newVersions.add(newVersion);
            }
            
            con.delete(allVersions);
            con.persistCollection(newVersions);
            con.commit();
            
            transferFromODSTypeToODSType(ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY, XMOMVersionStorable.TABLE_NAME,
                                         XMOMVersionStorable.class);
            
            ownBinding = null;
            recreateCache();
          } catch (PersistenceLayerException e) {
            logger.error("Can't migrate binding of storable.", e);
          } finally {
            try {
              con.closeConnection();
            } catch (PersistenceLayerException e) {
              logger.warn("Failed to close connection.", e);
            }
          }
        }
        break;
    }
    currentStorableState = newState;
    
  }
  
  public boolean isApplicationDefinition(String applicationName, String versionName, Long parentRevision) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    //FIXME zusammenhang zwischen applications, workingsets und revisions ist meiner meinung nach nicht ordentlich getrennt. hier sollte man mal aufr�umen
    String workingsetVersion = ApplicationManagementImpl.getWorkingsetVersionName(applicationName, parentRevision);
    return workingsetVersion == null ? (versionName == null ? true : false) : workingsetVersion.equals(versionName);
  }
  
  /**
   * �berpr�ft, ob noch laufende Auftr�ge oder TimeControlled Orders f�r
   * die angegebene Revision existieren.
   * @param revision
   * @param force laufende Auftr�ge werden abgebrochen
   * @throws XPRC_TimeoutWhileWaitingForUnaccessibleOrderException
   * @throws PersistenceLayerException
   * @throws XFMG_RunningOrdersException es laufen Auftr�ge und sie sollen nicht
   * abgebrochen werden (force = false)
   */
  public void handleRunningOrders(long revision, boolean force) throws XPRC_TimeoutWhileWaitingForUnaccessibleOrderException,
      PersistenceLayerException, XFMG_RunningOrdersException {
    if (DeploymentManagement.getInstance().isInUse(revision)) {
      if (force) {
        RevisionOrderControl roc = new RevisionOrderControl(revision);
        roc.killRunningOrders();
      } else {
        OrdersInUse ordersInUse = DeploymentManagement.getInstance().getInUse(revision, FillingMode.OnlyIds);
        throw new XFMG_RunningOrdersException(ordersInUse.getRootOrdersAndBatchProcesses().size(), ordersInUse.getCronIds().size(),
                                              ordersInUse.getFrequencyControlledTaskIds().size());
      }
    }
  }

  /**
   * Entfernt das OrderContextMapping f�r den �bergebenen orderType in dem angegebenen runtimeContext
   * @param runtimeContext
   * @throws PersistenceLayerException
   */
  public void removeOrderContextMapping(RuntimeContext runtimeContext) throws PersistenceLayerException {
    OrderContextConfiguration orderContextConfig =
        XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderContextConfiguration();

    Collection<DestinationKey> destinationKeys = orderContextConfig.getAllDestinationKeysForWhichAnOrderContextMappingIsCreated();
    for (DestinationKey dk : destinationKeys) {
      if (dk.getRuntimeContext().equals(runtimeContext)) {
        orderContextConfig.configureDestinationKey(dk, false);
      }
    }
  }
  
  private static class SpecialRevision {

    private RuntimeContext runtimeContext;
    private boolean visible;
    private String folder;

    public SpecialRevision(RuntimeContext runtimeContext, boolean visible, String folder) {
      this.runtimeContext = runtimeContext;
      this.visible = visible;
      this.folder = folder;
    }

    public RuntimeContext getRuntimeContext() {
      return runtimeContext;
    }

    public boolean isVisible() {
      return visible;
    }

    public String getFolder() {
      return folder;
    }
    
  }

  
  public boolean isSpecialRevision(Long revision) {
    return SPECIAL_REVISIONS.containsKey(revision);
  }

  public boolean isApplicationRevision(Long revision) {
    if (revision == null) {
      return false;
    }
    
    return applications.containsKey(revision);
  }

  /**
   * @return Integer.MAXVALUE, falls klasse nicht von classloaderbase geladen wird
   */
  public static long getRevisionByClass(Class<?> cl) {
    if (cl.getClassLoader() instanceof ClassLoaderBase) {
      ClassLoaderBase clb = (ClassLoaderBase) cl.getClassLoader();
      return clb.getRevision();
    }
    return Integer.MAX_VALUE;
  }
  
  public long getRevision(Class<?> cl) {
    return getRevisionByClass(cl);
  }
  
  /**
   * �berpr�ft, ob der RuntimeContext exitiert
   * @param rc
   * @return true, falls es den RuntimeContext gibt
   */
  public boolean runtimeContextExists(RuntimeDependencyContext rc) {
    try {
      if (rc instanceof RuntimeContext) {
        getRevision(rc.asCorrespondingRuntimeContext());
        //FIXME muss auch laufende Application sein, AUDIT_MODE reicht nicht
      } else {
        if (rc instanceof ApplicationDefinition) {
          ApplicationManagementImpl appMgmt = (ApplicationManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement();
          ApplicationDefinition appDef = (ApplicationDefinition)rc;
          appMgmt.getApplicationDefinitionInformation(appDef.getName(), getRevision(appDef.getParentWorkspace()));
        } else {
          return false;
        }
      }
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      return false;
    } catch (PersistenceLayerException e) {
      return false;
    }
    return true;
  }
  
  
  public boolean runtimeContextExists(RuntimeContext rc) {
    try {
      getRevision(rc.asCorrespondingRuntimeContext());
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      return false;
    }
    return true;
  }
  
  
  public Collection<RuntimeContextProblem> getRuntimeContextProblems(RuntimeDependencyContext rc) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    RuntimeContextProblemParameter rcpp = new RuntimeContextProblemParameter(rc, runtimeContextCycleController, true, true, true, true, true);
    return getRuntimeContextProblems(rc, rcpp);
  }
                                                                     
  
  /*
   * Bei der Kollisionspr�fung in A werden alle Objekte aus dem Sichtbarkeitsbereich im gesamten Sichtbarkeitsbereich auf Kollision gepr�ft.
   *  In einem rekursivem Schritt steigt man mit der Sammlung aller Kollsionen hinab in die Requirements und pr�ft ob die Kollision
   *  auch in ihrem (eingeschr�nktem) Sichtbarkeitsbereich besteht.
   */
  public Collection<RuntimeContextProblem> getRuntimeContextProblems(RuntimeDependencyContext rc, RuntimeContextProblemParameter rcpp)
                  throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    Collection<RuntimeContextProblem> problems = new ArrayList<RuntimeContextProblem>();
    if (rcpp.collectUnresolvable()) {
      problems.addAll(collectUnresolvable(rc, rcpp));
    }
    if (rcpp.collectCycles()) {
      problems.addAll(collectCycles(rc, rcpp));
    }
    if (rcpp.collectCollisions()) {
      problems.addAll(collectCollisions(rc, rcpp));
    }
    if (rcpp.collectErroneousOrderEntrance()) {
      problems.addAll(collectErroneousOrderEntrance(rc));
    }
    if (rcpp.collectDeploymentItemStateErrors()) {
      problems.addAll(collectDeploymentItemStateErrors(rc, rcpp));
    }

    return removeRuntimeContextProblemsAlreadyContainedInDependencies(rc, rcpp, problems);
  }
  
  
  public static class RuntimeContextProblemParameter implements CrossRevisionResolver, CycleController<RuntimeDependencyContext, Cycle> {
    
    private final CycleController<RuntimeDependencyContext, Cycle> cycleController;
    private final CrossRevisionResolver crossRevisionResolver;
    private final Set<RuntimeDependencyContext> allRequirements;
    private final boolean collectUnresolvable;
    private final boolean collectCycles;
    private final boolean collectCollisions;
    private final boolean collectErroneousOrderEntrance;
    private final boolean collectDeploymentItemStateErrors;
    
    public RuntimeContextProblemParameter(RuntimeDependencyContext base, 
                                          CycleController<RuntimeDependencyContext, Cycle> cycleController,
                                          boolean collectUnresolvable,
                                          boolean collectCycles,
                                          boolean collectCollisions,
                                          boolean collectErroneousOrderEntrance,
                                          boolean collectDeploymentItemStateErrors) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      this.cycleController = cycleController;
      this.collectUnresolvable = collectUnresolvable;
      this.collectCycles = collectCycles;
      this.collectCollisions = collectCollisions;
      this.collectErroneousOrderEntrance = collectErroneousOrderEntrance;
      this.collectDeploymentItemStateErrors = collectDeploymentItemStateErrors;
      allRequirements = CycleUtils.collectNodes(base, cycleController);
      RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      Set<Long> revisions = new HashSet<Long>();
      for (RuntimeDependencyContext rc : allRequirements) {
          revisions.add(revMgmt.getRevision(rc.asCorrespondingRuntimeContext()));
      }
      crossRevisionResolver = RevisionBasedCrossResolver.customDependencyResolver(revisions);
    }
    
    Set<RuntimeDependencyContext> resolveAllRequirements(RuntimeDependencyContext rc) {
      return CycleUtils.collectNodes(rc, cycleController);
    }
    
    public boolean collectUnresolvable() {
      return collectUnresolvable;
    }
    
    boolean collectCycles() {
      return collectCycles;
    }
    
    boolean collectCollisions() {
      return collectCollisions;
    }
    
    boolean collectErroneousOrderEntrance() {
      return collectErroneousOrderEntrance;
    }
    
    boolean collectDeploymentItemStateErrors() {
      return collectDeploymentItemStateErrors;
    }

    public Set<RuntimeDependencyContext> getBranchingElements(RuntimeDependencyContext element) {
      return cycleController.getBranchingElements(element);
    }

    public void addToCycle(Cycle cycleRepresentation, RuntimeDependencyContext element) {
      cycleController.addToCycle(cycleRepresentation, element);
    }

    public Cycle newCycle() {
      return cycleController.newCycle();
    }

    public Optional<DeploymentItemState> resolve(DeploymentItemIdentifier identifier, Long rootRevision) {
      return crossRevisionResolver.resolve(identifier, rootRevision);
    }
    
    
    public static RuntimeContextProblemParameter defaultParameter(RuntimeDependencyContext rc) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      return new RuntimeContextProblemParameter(rc, new RuntimeContextCycleController(), true, true, true, true, true);
    }

    public static RuntimeContextProblemParameter simulatedChanges(RuntimeDependencyContext rc, Collection<RuntimeDependencyContext> newDependencies) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      return new RuntimeContextProblemParameter(rc, new RuntimeContextWithChangesCycleController(rc, new HashSet<RuntimeDependencyContext>(newDependencies)), true, true, true, true, false);
    }

    public Set<Long> identifyReachableRevisions() {
      return crossRevisionResolver.identifyReachableRevisions();
    }

    public boolean checkForInvalidGeneration() {
      return true;
    }

    @Override
    public boolean updateCallSites() {
      return false;
    }
    
  }

  
  private Collection<? extends RuntimeContextProblem> collectCycles(RuntimeDependencyContext rc, RuntimeContextProblemParameter rcpp) {
    return CycleUtils.collectCycles(rc, rcpp);
  }
  
  
  private Collection<RuntimeContextProblem> collectUnresolvable(RuntimeDependencyContext rc, RuntimeContextProblemParameter rcpp) {
    Set<RuntimeDependencyContext> encountered = new HashSet<RuntimeDependencyContext>();
    return collectUnresolvableRecursivly(rc, rcpp, encountered);
  }
  
  
  private List<RuntimeContextProblem> collectUnresolvableRecursivly(RuntimeDependencyContext rc, RuntimeContextProblemParameter rcpp, Set<RuntimeDependencyContext> encountered) {
    List<RuntimeContextProblem> unresolvables = new ArrayList<RuntimeContextProblem>();
    if (encountered.add(rc)) {
      Collection<RuntimeDependencyContext> reqs = rcpp.getBranchingElements(rc);
      for (RuntimeDependencyContext req : reqs) {
        if (runtimeContextExists(req)) {
          unresolvables.addAll(collectUnresolvableRecursivly(req, rcpp, encountered));
        } else {
          unresolvables.add(RuntimeContextProblem.unresolvableRequirement(req));
        }
      }
    }
    return unresolvables;
  }
  
  
  private Collection<RuntimeContextProblem> collectCollisions(RuntimeDependencyContext rc, RuntimeContextProblemParameter rcpp) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, PersistenceLayerException {
    Collection<RuntimeContextProblem> collisions = new ArrayList<RuntimeContextProblem>();
    collisions.addAll(collectSimpleCollisions(RuntimeContextCollisionType.XMOM, rc, rcpp));
    collisions.addAll(collectSimpleCollisions(RuntimeContextCollisionType.ORDERTYPE, rc, rcpp));
    collisions.addAll(collectActivationCollisions(rc, rcpp));
    return collisions;
  }
  
  
  

  private Collection<RuntimeContextProblem> collectSimpleCollisions(RuntimeContextCollisionType rcct, RuntimeDependencyContext rc, RuntimeContextProblemParameter rcpp) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    Map<String, Map<RuntimeDependencyContext, ApplicationEntryType>> elements = collectAllElementsInHierarchy(rcct, rc, rcpp);
    Collection<RuntimeContextProblem> problems = new ArrayList<RuntimeContextProblem>();
    for (String name : elements.keySet()) {
      Map<RuntimeDependencyContext, ApplicationEntryType> revSubMap = elements.get(name);
      if (revSubMap.size() > 1) {
        Collision collision = RuntimeContextProblem.collision(rcct, name);
        for (Entry<RuntimeDependencyContext, ApplicationEntryType> definingRC : revSubMap.entrySet()) {
          collision.addRuntimeContext(definingRC.getKey(), getTypeName(rcct, definingRC.getValue()));
        }
        problems.add(collision);
      }
    }
    return problems;
  }
  
  
  private Collection<RuntimeContextProblem> collectErroneousOrderEntrance(RuntimeDependencyContext rc) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    Collection<RuntimeContextProblem> problems = new ArrayList<RuntimeContextProblem>();
    RevisionOrderControl roc = new RevisionOrderControl(getRevision(rc.asCorrespondingRuntimeContext()));
    for (OrderEntrance erroneous : roc.getErronousOrderEntrances()) {
      problems.add(RuntimeContextProblem.erroneousOrderEntrance(erroneous));
    }
    return problems;
  }
  
  
  private Collection<RuntimeContextProblem> collectActivationCollisions(RuntimeDependencyContext rc, RuntimeContextProblemParameter rcpp) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, PersistenceLayerException {
    Map<String, Map<RuntimeDependencyContext, ApplicationEntryType>> elements = collectAllElementsInHierarchy(RuntimeContextCollisionType.ACTIVATION, rc, rcpp);
    Collection<RuntimeContextProblem> problems = new ArrayList<RuntimeContextProblem>();
    for (Entry<String, Map<RuntimeDependencyContext, ApplicationEntryType>> entry : elements.entrySet()) {
      String elementName = entry.getKey();
      Map<RuntimeDependencyContext, ApplicationEntryType> revSubMap = entry.getValue();
      Map<ApplicationEntryType, Set<RuntimeDependencyContext>> typedMap = new HashMap<ApplicationEntryType, Set<RuntimeDependencyContext>>();
      for (Entry<RuntimeDependencyContext, ApplicationEntryType> entry2 : revSubMap.entrySet()) {
        Set<RuntimeDependencyContext> rcSet = typedMap.get(entry2.getValue());
        if (rcSet == null) {
          rcSet = new HashSet<RuntimeDependencyContext>();
          typedMap.put(entry2.getValue(), rcSet);
        }
        rcSet.add(entry2.getKey());
      }
      for (Entry<ApplicationEntryType, Set<RuntimeDependencyContext>> entry2 : typedMap.entrySet()) {
        if (entry2.getValue().size() > 1) {
          Collision problem = RuntimeContextProblem.collision(RuntimeContextCollisionType.ACTIVATION, elementName);
          for (RuntimeDependencyContext collisionRC : entry2.getValue()) {
            problem.addRuntimeContext(collisionRC, entry2.getKey().toString());
          }
          problems.add(problem);
        }
      }
    }
    return problems;
  }
  
  
  private Collection<RuntimeContextProblem> collectDeploymentItemStateErrors(RuntimeDependencyContext rc, RuntimeContextProblemParameter rcpp) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    DeploymentItemStateManagementImpl dism = 
                    (DeploymentItemStateManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement();
    Long revision;
    revision = getRevision(rc.asCorrespondingRuntimeContext());
    Collection<RuntimeContextProblem> problems = new ArrayList<RuntimeContextProblem>();
    Map<String, Map<RuntimeDependencyContext, ApplicationEntryType>> xmomElements = collectElements(RuntimeContextCollisionType.XMOM, rc);
    for (String xmomName : xmomElements.keySet()) {
      DeploymentItemState dis = dism.get(xmomName, revision);
      if (dis != null &&
          dis.exists()) {
        DeploymentItemStateReport disr;
        try {
          // TODO discern between detail and list select
          disr = dis.getStateReport(DeploymentItemStateImpl.getDefaultDetailSelect(), rcpp);
        } catch (Throwable e) {
          problems.add(RuntimeContextProblem.deploymentStateError(dis, rc, e));
          continue;
        }
        switch (disr.getState()) {
          case INCOMPLETE :
          case INVALID :
            RuntimeContextProblem problem = RuntimeContextProblem.deploymentState(disr, rc);
            problems.add(problem);
            break;
          default :
            break;
        }
      }
    }
    return problems;
  }
  
  
  
  private Map<String, Map<RuntimeDependencyContext, ApplicationEntryType>> collectAllElementsInHierarchy(RuntimeContextCollisionType rcct, RuntimeDependencyContext rc, RuntimeContextProblemParameter rcpp) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    Set<RuntimeDependencyContext> reqs = rcpp.resolveAllRequirements(rc);
    Map<String, Map<RuntimeDependencyContext, ApplicationEntryType>> xmomObjects = new HashMap<String, Map<RuntimeDependencyContext, ApplicationEntryType>>();
    for (RuntimeDependencyContext req : reqs) {
      Map<String, Map<RuntimeDependencyContext, ApplicationEntryType>> elements = collectElements(rcct, req);
      for (String name : elements.keySet()) {
        if (xmomObjects.containsKey(name)) {
          xmomObjects.get(name).put(req, elements.get(name).get(req));
        } else {
          xmomObjects.put(name, elements.get(name));
        }
      }
    }
    return xmomObjects;
  }
  
  
  private Map<String, Map<RuntimeDependencyContext, ApplicationEntryType>> collectElements(RuntimeContextCollisionType rcct, RuntimeDependencyContext rc) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    boolean appEntryBased = !(rc instanceof Workspace);
    if (appEntryBased) {
      return collectAppEntries(resolveToAppEntryTypes(rcct), rc);
    } else if (rc instanceof Workspace) {
      Workspace ws = (Workspace) rc;
      Long revision = getRevision(ws);
      switch (rcct) {
        case ACTIVATION :
          XynaActivationTrigger xact = XynaFactory.getInstance().getActivation().getActivationTrigger();
          Filter[] filters = xact.getFilters(revision);
          Collection<FilterInstanceInformation> filterInstances = xact.getFilterInstanceInformations(revision);
          Trigger[] triggers = xact.getTriggers(revision);
          Collection<TriggerInstanceInformation> triggerInstances = xact.getTriggerInstanceInformation(revision);
          Map<String, Map<RuntimeDependencyContext, ApplicationEntryType>> activationEntries = new HashMap<String, Map<RuntimeDependencyContext, ApplicationEntryType>>();
          for (Filter filter : filters) {
            Map<RuntimeDependencyContext, ApplicationEntryType> subMap = activationEntries.get(ApplicationEntryType.FILTER);
            if (subMap == null) {
              subMap = new HashMap<RuntimeDependencyContext, ApplicationEntryType>();
              activationEntries.put(filter.getName(), subMap);
            }
            subMap.put(rc, ApplicationEntryType.FILTER);
          }
          for (FilterInstanceInformation filterInstance : filterInstances) {
            Map<RuntimeDependencyContext, ApplicationEntryType> subMap = activationEntries.get(ApplicationEntryType.FILTERINSTANCE);
            if (subMap == null) {
              subMap = new HashMap<RuntimeDependencyContext, ApplicationEntryType>();
              activationEntries.put(filterInstance.getFilterInstanceName(), subMap);
            }
            subMap.put(rc, ApplicationEntryType.FILTERINSTANCE);
          }
          for (Trigger trigger : triggers) {
            Map<RuntimeDependencyContext, ApplicationEntryType> subMap = activationEntries.get(ApplicationEntryType.TRIGGER);
            if (subMap == null) {
              subMap = new HashMap<RuntimeDependencyContext, ApplicationEntryType>();
              activationEntries.put(trigger.getTriggerName(), subMap);
            }
            subMap.put(rc, ApplicationEntryType.TRIGGER);
          }
          for (TriggerInstanceInformation triggerInstance : triggerInstances) {
            Map<RuntimeDependencyContext, ApplicationEntryType> subMap = activationEntries.get(ApplicationEntryType.TRIGGERINSTANCE);
            if (subMap == null) {
              subMap = new HashMap<RuntimeDependencyContext, ApplicationEntryType>();
              activationEntries.put(triggerInstance.getTriggerInstanceName(), subMap);
            }
            subMap.put(rc, ApplicationEntryType.TRIGGERINSTANCE);
          }
          return activationEntries;
        case ORDERTYPE :
          List<OrdertypeParameter> ois = XynaFactory.getInstance().getFactoryManagement().listOrdertypes(SearchOrdertypeParameter.single(rc.asCorrespondingRuntimeContext()));
          Map<String, Map<RuntimeDependencyContext, ApplicationEntryType>> ordertypeEntries = new HashMap<String, Map<RuntimeDependencyContext, ApplicationEntryType>>();
          for (OrdertypeParameter oi : ois) {
            Map<RuntimeDependencyContext, ApplicationEntryType> subMap = ordertypeEntries.get(ApplicationEntryType.ORDERTYPE);
            if (subMap == null) {
              subMap = new HashMap<RuntimeDependencyContext, ApplicationEntryType>();
              ordertypeEntries.put(oi.getOrdertypeName(), subMap);
            }
            subMap.put(rc, ApplicationEntryType.ORDERTYPE);
          }
          return ordertypeEntries;
        case XMOM :
          DeploymentItemStateManagementImpl dism = 
            (DeploymentItemStateManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement();
          Map<String, Map<RuntimeDependencyContext, ApplicationEntryType>> xmomEntries = new HashMap<String, Map<RuntimeDependencyContext, ApplicationEntryType>>();
          DeploymentItemRegistry dir = dism.lazyCreateOrGet(revision);
          for (DeploymentItemState dis : dir.list()) {
            if (dis.exists()) {
              Map<RuntimeDependencyContext, ApplicationEntryType> subMap = xmomEntries.get(dis.getType().getApplicationEntryRepresentation());
              if (subMap == null) {
                subMap = new HashMap<RuntimeDependencyContext, ApplicationEntryType>();
                xmomEntries.put(dis.getName(), subMap);
              }
              subMap.put(rc, dis.getType().getApplicationEntryRepresentation());
            }
          }
          return xmomEntries;
        default :
          throw new UnsupportedOperationException();
      }
    } else {
      return Collections.emptyMap();
    }
  }
  

  // TODO requesting all appEntries serveral times does not improve performance...ThreadLocal- or LRU-Cache?
  private Map<String, Map<RuntimeDependencyContext, ApplicationEntryType>> collectAppEntries(Set<ApplicationEntryType> resolveToAppEntryTypes, RuntimeDependencyContext rc) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    Map<String, Map<RuntimeDependencyContext, ApplicationEntryType>>  filteredEntries = new HashMap<String, Map<RuntimeDependencyContext, ApplicationEntryType>>(); 
    ApplicationManagementImpl appMgmt = (ApplicationManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement();
    List<ApplicationEntryStorable> entries;
    if (rc instanceof ApplicationDefinition) {
      ApplicationDefinition appDef = (ApplicationDefinition) rc;
      entries = appMgmt.listApplicationDetails(appDef.getName(), null, true, Collections.<String>emptyList(), getRevision(appDef.getParentWorkspace()), true);
    } else {
      Application app = (Application) rc;
      entries = appMgmt.listApplicationDetails(app.getName(), app.getVersionName(), true, Collections.<String>emptyList(), null, true);
    }
    
    if (entries != null) {
      for (ApplicationEntryStorable aes : entries) {
        if (resolveToAppEntryTypes.contains(aes.getTypeAsEnum())) {
          Map<RuntimeDependencyContext, ApplicationEntryType> subMap = filteredEntries.get(aes.getName());
          if (subMap == null) {
            subMap = new HashMap<RuntimeDependencyContext, ApplicationEntryType>();
            filteredEntries.put(aes.getName(), subMap);
          }
          subMap.put(rc, aes.getTypeAsEnum());
        }
      }
    }
    return filteredEntries;
  }


  private Set<ApplicationEntryType> resolveToAppEntryTypes(RuntimeContextCollisionType rcct) {
    Set<ApplicationEntryType> aets = new HashSet<ApplicationEntryType>();
    switch (rcct) {
      case ACTIVATION :
        aets.add(ApplicationEntryType.FILTER);
        aets.add(ApplicationEntryType.FILTERINSTANCE);
        aets.add(ApplicationEntryType.TRIGGER);
        aets.add(ApplicationEntryType.TRIGGERINSTANCE);
        break;
      case ORDERTYPE :
        aets.add(ApplicationEntryType.ORDERTYPE);
        break;
      case XMOM :
        aets.add(ApplicationEntryType.DATATYPE);
        aets.add(ApplicationEntryType.EXCEPTION);
        aets.add(ApplicationEntryType.WORKFLOW);
      default :
        break;
    }
    return aets;
  }
  
  
  private String getTypeName(RuntimeContextCollisionType rcct, ApplicationEntryType value) {
    switch (rcct) {
      case ORDERTYPE :
        return "Ordertype";
      case XMOM :
        return XMOMType.deriveXMOMType(value).getNiceName();
      default :
        return value.toString();
    }
  }


  private Collection<RuntimeContextProblem> removeRuntimeContextProblemsAlreadyContainedInDependencies(RuntimeDependencyContext rc, RuntimeContextProblemParameter rcpp, Collection<RuntimeContextProblem> problems) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, PersistenceLayerException {
    Set<RuntimeDependencyContext> reqs = rcpp.resolveAllRequirements(rc);
    reqs.remove(rc);
    Set<RuntimeDependencyContext> erroneousRequirements = new HashSet<RuntimeDependencyContext>();
    Collection<RuntimeContextProblem> ownProblems = new ArrayList<RuntimeContextProblem>();
    for (RuntimeContextProblem rcp : problems) {
      boolean problemFoundInDependentRuntimeContext = false;
      for (RuntimeDependencyContext req : reqs) {
        if (problemExistsInRuntimeContext(req, rcpp, rcp)) {
          erroneousRequirements.add(req);
          problemFoundInDependentRuntimeContext = true;
        }
      }
      if (!problemFoundInDependentRuntimeContext) {
        ownProblems.add(rcp);
      }
    }
    for (RuntimeDependencyContext rtCtx : erroneousRequirements) {
      ownProblems.add(RuntimeContextProblem.erroneousRequirement(rtCtx));
    }
    return ownProblems;
  }
  
  
  /**
   * @return true, falls das problem im �bergebenen runtimecontext gefunden wird, ansonsten false
   */
  private boolean problemExistsInRuntimeContext(RuntimeDependencyContext rc, RuntimeContextProblemParameter rcpp, RuntimeContextProblem rcp) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, PersistenceLayerException {
    switch (rcp.getId()) {
      case CYCLE :
        return runtimeContextContainsCycle(rc, rcpp, rcp);
      case UNRESOLVABLE_REQUIREMENT :
        return runtimeContextContainsUnresolvableRequirement(rc, rcpp, (UnresolvableRequirement) rcp);
      case COLLISION :
        Collision crcp = (Collision) rcp;
        switch (crcp.getCollisionType()) {
          case XMOM :
          case ORDERTYPE :
            return runtimeContextContainsAllSimpleCollisions(rc, rcpp, crcp.getCollisionType(), crcp);
          case ACTIVATION :
            return runtimeContextContainsAllActivationCollision(rc, rcpp, crcp);
          default :
            return false;
        }
      default :
        return false;
    }
  }


  private boolean runtimeContextContainsCycle(RuntimeDependencyContext rc, RuntimeContextProblemParameter rcpp, RuntimeContextProblem rcp) {
    Collection<? extends RuntimeContextProblem> subCycles = collectCycles(rc, rcpp);
    for (RuntimeContextProblem subCycle : subCycles) {
      if (subCycle.equals(rcp)) {
        return true;
      }
    }
    return false;
  }


  private boolean runtimeContextContainsUnresolvableRequirement(RuntimeDependencyContext rc, RuntimeContextProblemParameter rcpp, UnresolvableRequirement rcp) {
    Set<RuntimeDependencyContext> reqs = rcpp.getBranchingElements(rc);
    for (RuntimeDependencyContext req : reqs) {
      if (rcp.getRuntimeContext().equals(req) &&
          !runtimeContextExists(req)) {
        return true;
      }
    }
    return false;
  }

  /**
   * @return true, falls der �bergebene RC die kollisionen vollst�ndig enth�lt
   */
  private boolean runtimeContextContainsAllSimpleCollisions(RuntimeDependencyContext rc, RuntimeContextProblemParameter rcpp, RuntimeContextCollisionType rcct, Collision rcp) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, PersistenceLayerException {
    Map<String, Map<RuntimeDependencyContext, ApplicationEntryType>> elements = collectAllElementsInHierarchy(rcct, rc, rcpp);
    if (elements.containsKey(rcp.getName())) {
      boolean problemDoesNotExist = elements.get(rcp.getName()).size() <= 1;
      if (problemDoesNotExist) {
        return false;
      } else {
        // if the problem persists in an changed amount of rcs it should still be reported 
        return rcp.getParticipatingRuntimeContexts() == elements.get(rcp.getName()).size();
      }
    } else {
      return false;
    }
  }
  

  private boolean runtimeContextContainsAllActivationCollision(RuntimeDependencyContext rc, RuntimeContextProblemParameter rcpp, Collision rcp)
      throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, PersistenceLayerException {
    String type = rcp.getType();
    if (type == null) {
      return false;
    }
    Map<String, Map<RuntimeDependencyContext, ApplicationEntryType>> elements =
        collectAllElementsInHierarchy(RuntimeContextCollisionType.ACTIVATION, rc, rcpp);
    if (elements.containsKey(rcp.getName())) {
      Map<ApplicationEntryType, Set<RuntimeDependencyContext>> typedMap = new HashMap<ApplicationEntryType, Set<RuntimeDependencyContext>>();
      for (Entry<RuntimeDependencyContext, ApplicationEntryType> entry : elements.get(rcp.getName()).entrySet()) {
        Set<RuntimeDependencyContext> rcSet = typedMap.get(entry.getValue());
        if (rcSet == null) {
          rcSet = new HashSet<RuntimeDependencyContext>();
          typedMap.put(entry.getValue(), rcSet);
        }
        rcSet.add(entry.getKey());
      }

      ApplicationEntryType aet = ApplicationEntryType.valueOf(type);
      boolean problemDoesNotExist = !typedMap.containsKey(aet) || typedMap.get(aet).size() <= 1;
      if (problemDoesNotExist) {
        return false;
      } else {
        // if the problem persists in an changed amount of rcs it should still be reported 
        return rcp.getParticipatingRuntimeContexts() == elements.get(rcp.getName()).size();
      }
    } else {
      return false;
    }
  }


  private static RuntimeContextCycleController runtimeContextCycleController;
  
  private static class RuntimeContextCycleController implements CycleUtils.CycleController<RuntimeDependencyContext, Cycle> {
    
    
    private RuntimeContextCycleController() {
    }
    
    
    
    public Set<RuntimeDependencyContext> getBranchingElements(RuntimeDependencyContext element) {
      Collection<RuntimeDependencyContext> reqs = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement().getRequirements(element);
      Set<RuntimeDependencyContext> validReqs = new HashSet<RuntimeDependencyContext>();
      for (RuntimeDependencyContext rc : reqs) {
        validReqs.add(rc);
      }
      return validReqs;
    }

    public void addToCycle(Cycle cycleRepresentation, RuntimeDependencyContext element) {
      cycleRepresentation.addRuntimeContext(element);
    }

    public Cycle newCycle() {
      return RuntimeContextProblem.cycle();
    }
    
  }
  
  
  private static class RuntimeContextWithChangesCycleController extends RuntimeContextCycleController {
    
    private final RuntimeDependencyContext base;
    private final Set<RuntimeDependencyContext> changes; 
    
    private RuntimeContextWithChangesCycleController(RuntimeDependencyContext base, Set<RuntimeDependencyContext> changes) {
      this.base = base;
      this.changes = changes;
    }
    
    
    @Override
    public Set<RuntimeDependencyContext> getBranchingElements(RuntimeDependencyContext element) {
      if (element.equals(base)) {
        return changes;
      } else {
        return super.getBranchingElements(element);
      }
    }
    
  }


  private void checkRevisionInUse(Long revision) {
    if (revisions.containsValue(revision)) {
      java.util.Optional<RuntimeContext> rev =
          revisions.entrySet().stream().filter(x -> revision.equals(x.getValue())).map(x -> x.getKey()).findFirst();
      throw new RuntimeException("Revision " + revision + " already in use " + (rev.isPresent() ? "by " + rev.get() : ""));
    }


    // check if the rev folder already exsits
    File revFolder = new File(getPathForRevision(PathType.ROOT, revision));
    if (revFolder.exists()) {
      throw new RuntimeException("Revision " + revision + " already exists on file system: " + revFolder.getAbsolutePath());
    }
  }
}
