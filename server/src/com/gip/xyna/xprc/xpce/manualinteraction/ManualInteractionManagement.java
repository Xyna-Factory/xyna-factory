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

package com.gip.xyna.xprc.xpce.manualinteraction;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.FutureExecutionTask;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.exceptions.XFMG_ClusterComponentConfigurationException;
import com.gip.xyna.xfmg.exceptions.XFMG_MDMObjectClassLoaderNotFoundException;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownClusterInstanceIDException;
import com.gip.xyna.xfmg.xclusteringservices.ClusterContext;
import com.gip.xyna.xfmg.xclusteringservices.ClusterState;
import com.gip.xyna.xfmg.xclusteringservices.ClusterStateChangeHandler;
import com.gip.xyna.xfmg.xclusteringservices.Clustered;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProvider;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProvider.InvalidIDException;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProviderTools;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProviderTools.RMIRunnable;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagement;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagementInterface;
import com.gip.xyna.xfmg.xfctrl.RMIManagement.InitializableRemoteInterface;
import com.gip.xyna.xfmg.xfctrl.RMIManagement.RMIImplFactory;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xmcp.exceptions.XMCP_RMI_BINDING_ERROR;
import com.gip.xyna.xnwh.exceptions.XNWH_GeneralPersistenceLayerException;
import com.gip.xyna.xnwh.exceptions.XNWH_IncompatiblePreparedObjectException;
import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xnwh.exceptions.XNWH_WhereClauseBuildException;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl.PersistenceLayerInstances;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.PreparedQueryCache;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.StorableClassList;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutable;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xprc.ManualInteractionXynaOrder;
import com.gip.xyna.xprc.RedirectionXynaOrder;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.exceptions.MiProcessingRejected;
import com.gip.xyna.xprc.exceptions.XPRC_DuplicateMIException;
import com.gip.xyna.xprc.exceptions.XPRC_IllegalManualInteractionResponse;
import com.gip.xyna.xprc.exceptions.XPRC_OrderEntryCouldNotBeAcknowledgedException;
import com.gip.xyna.xprc.exceptions.XPRC_ResumeFailedException;
import com.gip.xyna.xprc.xfractwfe.OrderDeathException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xpce.AbstractBackupAck;
import com.gip.xyna.xprc.xpce.manualinteraction.selectmi.ExtendedManualInteractionResult;
import com.gip.xyna.xprc.xpce.manualinteraction.selectmi.ManualInteractionResult;
import com.gip.xyna.xprc.xpce.manualinteraction.selectmi.ManualInteractionSelect;
import com.gip.xyna.xprc.xpce.ordersuspension.ProcessSuspendedException;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeManagement;
import com.gip.xyna.xprc.xprcods.orderarchive.ClusteredOrderArchive;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive.BackupAction;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderCount;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstance;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup.BackupCause;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceResult;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;
import com.gip.xyna.xprc.xprcods.orderarchive.selectorder.OrderInstanceSelect;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder;
import com.gip.xyna.xprc.xsched.orderabortion.SuspendedOrderAbortionSupportListenerInterface;



public class ManualInteractionManagement extends FunctionGroup
                implements
                  IManualInteraction,
                  Clustered,
                  RMIImplFactory<MIRemoteInterfaceImpl>,
                  ClusterStateChangeHandler,
                  SuspendedOrderAbortionSupportListenerInterface {

  public static final String DEFAULT_NAME = "ManualInteractionManagement";
  public static final Logger logger = CentralFactoryLogging.getLogger(ManualInteractionManagement.class);
  
  public static final String RESPONSEMDMPATH = "xmcp.manualinteraction";
  public final static String MANUALINTERACTION_WORKFLOW_FQNAME = "xmcp.manualinteraction.ManualInteraction";

  public static enum ManualInteractionResponse {
    ABORT("Abort"),
    CONTINUE("Continue"),
    RETRY("Retry");
    
    private final String xmlName;
    private GeneralXynaObject mdmRepresentation = null;
    
    ManualInteractionResponse(String xmlName) {
      this.xmlName = xmlName;
    }
    
    public String getXmlName() {
      return xmlName;
    }

    public GeneralXynaObject getMDMRepresentation() {
      if (this.mdmRepresentation == null) {
        try {
          this.mdmRepresentation = XynaObject.generalFromXml(new StringBuilder("<payload><")
                                                            .append(GenerationBase.EL.DATA).append(" ")
                                                            .append(GenerationBase.ATT.REFERENCENAME).append("=\"")
                                                            .append(xmlName).append("\" ")
                                                            .append(GenerationBase.ATT.REFERENCEPATH).append("=\"")
                                                            .append(RESPONSEMDMPATH).append("\" ")
                                                            .append(GenerationBase.ATT.VARIABLENAME).append("=\"v\"></")
                                                            .append(GenerationBase.EL.DATA).append("></payload>")
                                                            .toString(), RevisionManagement.REVISION_DEFAULT_WORKSPACE);
        } catch (XynaException e) {
          logger.error("Failed to generate predefined ManualInteractionResponse.",e);
        }
      }
      return mdmRepresentation;
    }
    
    
    public static ManualInteractionResponse getManualInteractionResponseFromXmlName(String xmlName) {
      if (xmlName == null) {
        throw new IllegalArgumentException("XML name may not be null"); 
      }
      for (ManualInteractionResponse mir : values()) {
        if (mir.xmlName.equals(xmlName)) {
          return mir;
        }
      }
      throw new IllegalArgumentException(xmlName);
    }
    
  }
  
  public int FUTURE_EXECUTION_TASK_ON_CHANGEHANDLER_ID = XynaFactory.getInstance().getFutureExecution().nextId();
  
  private static PreparedQueryCache cache = new PreparedQueryCache();

  private ODS ods;
  
  private Integer currentOwnStorableBinding = null;
  
  private ClusterContext storableClusterContext;
  private boolean rmiIsClustered;
  private long rmiClusterInstanceId;
  private RMIClusterProvider rmiClusterInstance;
  private long clusteredManualInteractionManagmentInterfaceId;
  private ClusterState actualState;
  
  private PreparedQuery<ManualInteractionEntry> loadAllManualInteractionEntriesForRootOrderId;
  private PreparedQuery<ManualInteractionEntry> loadMIEntryForOrderId;
  
  private volatile ClusterState rmiClusterState = ClusterState.NO_CLUSTER;
  
  public static final int FUTURE_EXECUTION_ID = XynaFactory.getInstance().getFutureExecution().nextId();

  private volatile ManualInteractionProcessingRejectionState processingRejectionState = ManualInteractionProcessingRejectionState.STARTUP;


  public ManualInteractionManagement() throws XynaException {
    super();
  }


  public String getDefaultName() {
    return DEFAULT_NAME;
  }

  public void init() throws XynaException {

    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    
    fExec.addTask("ManualInteractionManagement.initStorables", "ManualInteractionManagement.initStorables").
       after(PersistenceLayerInstances.class).
       before(XynaClusteringServicesManagement.class).
       execAsync( new Runnable() { public void run() { initStorables(); }});
    
    fExec.addTask(FUTURE_EXECUTION_ID, "ManualInteractionInitializer"). //FIXME nötig?
      after(SuspendResumeManagement.class).
      before(XynaProcessing.FUTUREEXECUTIONID_ORDER_EXECUTION).
      execAsync( new Runnable() { public void run() { initialize(); }});
  }

  public void initStorables() {
            try {
              XynaClusteringServicesManagement.getInstance().registerClusterableComponent(ManualInteractionManagement.this);
              
              ods = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS();

              ods.registerStorable(ManualInteractionEntry.class);

              ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
              try {
                loadAllManualInteractionEntriesForRootOrderId = con.prepareQuery(new Query<ManualInteractionEntry>("select * from "
                                + ManualInteractionEntry.TABLE_NAME + " where " + ManualInteractionEntry.MI_COL_XYNAORDER_ROOT_ID + " =? and "
                                + ManualInteractionEntry.COL_BINDING + " =?"
                                , ManualInteractionEntry.reader), true);
                loadMIEntryForOrderId = con.prepareQuery(new Query<ManualInteractionEntry>("select * from "
                                + ManualInteractionEntry.TABLE_NAME + " where " + ManualInteractionEntry.MI_COL_XYNAORDER_ID + " =? and "
                                + ManualInteractionEntry.COL_BINDING + " =?"
                                , ManualInteractionEntry.reader), true);
                storableClusterContext = new ClusterContext(ManualInteractionEntry.class, ODSConnectionType.DEFAULT);
              } finally {
                try {
                  con.closeConnection();
                } catch (PersistenceLayerException e) {
                  logger.warn("Can't close connection.", e);
                }
              }
              storableClusterContext.addClusterStateChangeHandler(ManualInteractionManagement.this);
              ods.addClusteredStorableConfigChangeHandler(storableClusterContext, ODSConnectionType.DEFAULT,
                                                          ManualInteractionEntry.class);
            } catch (XynaException e) {
              // FIXME exception handling??
              throw new RuntimeException(e);
            }
  }
  
  private boolean initialized;

  public void initialize() {
    processingRejectionState = ManualInteractionProcessingRejectionState.NONE;
    XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getSuspendResumeManagement().addListener(ManualInteractionManagement.this);
    initialized = true;
  }
  
  public enum ManualInteractionProcessingRejectionState {

    NONE,
    STARTUP,
    SHUTDOWN,
    DEPLOYMENT

  }


  public void shutdown() throws XynaException {
    if (initialized) {
      setProcessingRejectionState(ManualInteractionProcessingRejectionState.SHUTDOWN);
      XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getSuspendResumeManagement().removeListener(this);
    }
  }


  public void setProcessingRejectionState(ManualInteractionProcessingRejectionState reason) {
    if (reason != ManualInteractionProcessingRejectionState.NONE) {
      logger.debug("Further Manual Interaction processing is disabled: " + reason);
    }
    if (processingRejectionState == ManualInteractionProcessingRejectionState.SHUTDOWN) {
      logger.debug("State can not be set to " + reason + ", component is already shutting down");
      return;
    }
    processingRejectionState = reason;
  }


  // TODO this could be renamed to listUnprocessedMIs and be used by every outside interface
  // a normal "getMIs" might still be nice to have inside the factory (although it's not needed at the moment)
  public Map<Long, ManualInteractionEntry> listManualInteractionEntries() throws PersistenceLayerException {
    return listManualInteractionEntries(100);
  }


  public Map<Long, ManualInteractionEntry> listManualInteractionEntries(int maxRows) throws PersistenceLayerException {

    Collection<ManualInteractionEntry> existingEntries = null;
    ODSConnection con = getODS().openConnection(ODSConnectionType.DEFAULT);
    try {
      // TODO this will result in out of memory for many manual interactions
      PreparedQuery<ManualInteractionEntry> pq =
          cache.getQueryFromCache("select * from " + ManualInteractionEntry.TABLE_NAME + " where "
              + ManualInteractionEntry.MI_COL_RESULT + " is null", con, ManualInteractionEntry.reader);
      existingEntries = con.query(pq, new Parameter(), maxRows);
    } finally {
      con.closeConnection();
    }
    if (existingEntries == null || existingEntries.size() == 0) {
      return Collections.emptyMap();
    }
    Map<Long, ManualInteractionEntry> unprocessedEntries = new HashMap<Long, ManualInteractionEntry>();
    for (ManualInteractionEntry entry : existingEntries) {
      unprocessedEntries.put(entry.getID(), entry);
    }
    return unprocessedEntries;

  }


  public ProcessManualInteractionResult processManualInteractionEntry(final Long id, final GeneralXynaObject response)
      throws PersistenceLayerException, XPRC_ResumeFailedException, XPRC_IllegalManualInteractionResponse,
      MiProcessingRejected {

    if (processingRejectionState != ManualInteractionProcessingRejectionState.NONE) {
      switch (processingRejectionState) {
        case SHUTDOWN :
          throw new MiProcessingRejected("Factory Shutdown");
        case STARTUP :
          throw new MiProcessingRejected("Factory initializing");
        case DEPLOYMENT :
          throw new MiProcessingRejected("Active deployments");
        case NONE :
          break;
        default :
          throw new RuntimeException("Unexpected MI Rejected State");
      }
    }

    if (id == null) {
      throw new IllegalArgumentException("Null not allowed for the manual interaction ID to be processed.");
    }

    final ManualInteractionEntry entry = new ManualInteractionEntry(id, getCurrentOwnStorableBinding());

    WarehouseRetryExecutable<ProcessManualInteractionResult, XPRC_ResumeFailedException, XPRC_IllegalManualInteractionResponse> wre =
      new WarehouseRetryExecutable<ProcessManualInteractionResult, XPRC_ResumeFailedException, XPRC_IllegalManualInteractionResponse>() {

        public ProcessManualInteractionResult executeAndCommit(ODSConnection con) throws PersistenceLayerException,
            XPRC_ResumeFailedException, XPRC_IllegalManualInteractionResponse {
          // lokal prüfen, ob binding für lokalen node
          try {
            con.queryOneRowForUpdate(entry);
          } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
            return ProcessManualInteractionResult.NOT_FOUND;
          }
          
          ProcessManualInteractionResult result;
          
          if (entry.getBinding() == getCurrentOwnStorableBinding()) {
            result = processManualInteractionEntryLocally(entry, con, response);
          } else {
            result = ProcessManualInteractionResult.FOREIGN_BINDING;
          }
          
          con.commit();
          return result;
        }
      };

    ProcessManualInteractionResult localResult =
        WarehouseRetryExecutor.executeWithRetries(wre, ODSConnectionType.DEFAULT,
                                                  Constants.DEFAULT_CONNECTION_TO_CLUSTER_BROKEN_RETRIES,
                                                  Constants.DEFAULT_NO_CONNECTION_AVAILABLE_RETRIES__USERINTERACTION,
                                                  new StorableClassList(ManualInteractionEntry.class,
                                                                        CronLikeOrder.class, OrderInstanceBackup.class,
                                                                        OrderInstance.class));

    if (localResult != ProcessManualInteractionResult.SUCCESS) {
      if (rmiIsClustered && rmiClusterState == ClusterState.CONNECTED
        && localResult == ProcessManualInteractionResult.FOREIGN_BINDING) {

        // remote andere nodes benachrichtigen
        final int binding = entry.getBinding(); // FIXME this should be filled from wre shouldn't it?

        List<ProcessManualInteractionResult> remoteNodeshandled = null;

        try {
          remoteNodeshandled =
              RMIClusterProviderTools
                  .executeAndCumulate(rmiClusterInstance,
                                      clusteredManualInteractionManagmentInterfaceId,
                                      new RMIRunnable<ProcessManualInteractionResult, ClusteredManualInteractionManagementInterface, XynaException>() {

                                        public ProcessManualInteractionResult execute(ClusteredManualInteractionManagementInterface clusteredInterface)
                                            throws RemoteException, XynaException {

                                          
                                          return clusteredInterface
                                              .processManualInteractionEntry(id, binding, response.toXml(), entry.getRevision());
                                        }
                                      }, null);
        } catch (InvalidIDException e) {
          throw new RuntimeException(e); // sollte nicht passieren, weil kein removeRmi aufgerufen wird
        } catch (XynaException e) {
          logger.error("Error while resuming manual interactions remotely.", e);
          throw new XPRC_ResumeFailedException(Long.toString(id), e);
        }

        for (ProcessManualInteractionResult handled : remoteNodeshandled) {
          if (handled == ProcessManualInteractionResult.SUCCESS) {
            return handled;
          }
        }

        logger.error("No remote node found, which want to process the manual interaction with the id " + id);
        throw new XPRC_ResumeFailedException(Long.toString(id));

      } else {
        // anderer Clusterknoten ist offensichtlich verantwortlich, aber nicht mehr verfügbar
        // FIXME 1. bessere Fehlermeldung und 2. bessere Aufspaltung der Fälle in unterschiedliche Fehlermeldungen!
        logger.info("Remote node not connected and the migrating of the binding is running now for manual interaction with the id" + id);
        throw new XPRC_ResumeFailedException(Long.toString(id));
      }
    } else {
      return localResult;
    }

  }


  public ProcessManualInteractionResult processManualInteractionEntryLocally(ManualInteractionEntry entry,
                                                                             ODSConnection con,
                                                                             GeneralXynaObject response)
      throws PersistenceLayerException, XPRC_ResumeFailedException, XPRC_IllegalManualInteractionResponse {

    String responseXML = response.toXml();
    if (!responseXML.contains(RESPONSEMDMPATH)) {
      throw new XPRC_IllegalManualInteractionResponse(responseXML);
    }

    String allowedResponsesString = entry.getAllowedResponses();
    if (allowedResponsesString != null) {
      String[] allowedResponses = allowedResponsesString.split(",");
      boolean found = false;
      for (String allowedResponse : allowedResponses) {
        if (responseXML.indexOf("<Data ReferenceName=\"" + allowedResponse + "\"") != -1) {
          found = true;
        }
      }

      if (!found) {
        throw new XPRC_IllegalManualInteractionResponse(responseXML);
      }
    }

    if (!entry.hasBeenProcessed()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Setting response " + response + " for manual interaction <" + entry.getID() + ">");
      }
      
      entry.setResult(response);
      con.persistObject(entry);
    } else {
      logger.debug("tried to process a manual interaction that already has a result");
      // this cannot happen at runtime since MI entries have different IDs (since ManualInteraction is a subworkflow)
      throw new RuntimeException(new XPRC_DuplicateMIException(entry.getID().longValue()));
    }


    MIProcessingAck ackObj = new MIProcessingAck(con);

    try {
      XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getSuspendResumeManagement().
          resumeOrderAsynchronously(entry.getResumeTarget(), ackObj);
    } catch (XPRC_OrderEntryCouldNotBeAcknowledgedException e1) {
      if (e1.getCause() instanceof XNWH_RetryTransactionException) {
        throw (XNWH_RetryTransactionException) e1.getCause();
      } else {
        throw new XPRC_ResumeFailedException(entry.getResumeTarget().toString(), e1);
      }
    }

    return ProcessManualInteractionResult.SUCCESS;

  }

  
  private static class MIProcessingAck extends AbstractBackupAck {

    private static final long serialVersionUID = 1L;


    public MIProcessingAck(ODSConnection con) {
      super(con);
    }


    @Override
    protected void backupPreFlight(XynaOrderServerExtension xose) throws PersistenceLayerException {
      // should not be called after serialization
      if (getConnection() == null && isConnectionPresent()) {
        throw new IllegalArgumentException();
      }
    }


    @Override
    protected BackupCause getBackupCause() {
      return BackupCause.ACKNOWLEDGED;
    }

  }


  public GeneralXynaObject waitForMI(XynaOrderServerExtension xo, String reason, String type, String userGroup,
                                     String todo, GeneralXynaObject payload) throws PersistenceLayerException,
                                     XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {

    final Long id = xo.getId();
    final Integer binding = getCurrentOwnStorableBinding();
    
    List<ManualInteractionResponse> allowedResponses;
    if (xo instanceof ManualInteractionXynaOrder) {
      allowedResponses = ((ManualInteractionXynaOrder) xo).getAllowedResponses();
    }  else if (xo instanceof RedirectionXynaOrder) {
      allowedResponses = ((RedirectionXynaOrder) xo).getAllowedResponses();
    } else {
      allowedResponses = Arrays.asList(ManualInteractionResponse.CONTINUE, ManualInteractionResponse.ABORT);
    }

    WarehouseRetryExecutableNoException<ManualInteractionEntry> wre =
        new WarehouseRetryExecutableNoException<ManualInteractionEntry>() {

          public ManualInteractionEntry executeAndCommit(ODSConnection con) throws PersistenceLayerException {
            ManualInteractionEntry existingEntry = new ManualInteractionEntry(id, binding);
            try {
              con.queryOneRowForUpdate(existingEntry);
            } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
              existingEntry = null;
            }
            return existingEntry;
          }

        };

    ManualInteractionEntry existingEntry = null;
    try {
      existingEntry =
          WarehouseRetryExecutor
              .executeWithRetriesNoException(wre, ODSConnectionType.DEFAULT,
                                             Constants.DEFAULT_CONNECTION_TO_CLUSTER_BROKEN_RETRIES,
                                             Constants.DEFAULT_NO_CONNECTION_AVAILABLE_RETRIES__CRITICAL,
                                             new StorableClassList(ManualInteractionEntry.class));
    } catch (XNWH_RetryTransactionException e) {
      if (new ManualInteractionEntry().getClusterState(ODSConnectionType.DEFAULT) == ClusterState.DISCONNECTED_SLAVE) {
        throw new OrderDeathException(e);
      } else {
        throw new XNWH_GeneralPersistenceLayerException("Was unable to retry", e);
      }
    }

    if (existingEntry != null && existingEntry.hasBeenProcessed()) {

      logger.debug("Got notified while waiting for ManualInteraction to be processed, processing response");

      xo.addBackupAction(new BackupAction(ODSConnectionType.DEFAULT) {

        final ManualInteractionEntry entryToDelete = new ManualInteractionEntry(id, binding);

        @Override
        public void executeBackupAction(ODSConnection con) throws PersistenceLayerException {
          con.deleteOneRow(entryToDelete);
        }
      });

      XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getStatusChangeProvider()
                      .notifyListenersMI(xo, null);

      GeneralXynaObject result = existingEntry.getResult();

      // Make sure that the result object has been loaded by the correct classloader
      if (result.getClass().getClassLoader() instanceof ClassLoaderBase) {
        ClassLoaderBase objectsClassLoader = (ClassLoaderBase) result.getClass().getClassLoader();
        try {
          if (objectsClassLoader != XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
              .getMDMClassLoader(result.getClass().getName(), existingEntry.getRevision(), true)) {
            //passiert zb wenn MIEntries auf memory konfiguriert sind, und während der 
            //mientry im memory PL wartet, der entsprechende mdm typ neu deployed wird 
            if (logger.isDebugEnabled()) {
              logger.debug("wrong classloader: " + result.getClass().getClassLoader());
            }
            result = recreateWithAppropriateClassLoader(result, existingEntry.getRevision());
          }
        } catch (XFMG_MDMObjectClassLoaderNotFoundException e) {
          logger.warn("ClassLoader for MI result type not found (" + result.getClass().getName() + ")", e);
          result = recreateWithAppropriateClassLoader(result, existingEntry.getRevision());
        }
      } else {
        logger.warn("MI result " + result + " has been loaded by unexpected classloader "
            + result.getClass().getClassLoader());
        result = recreateWithAppropriateClassLoader(result, existingEntry.getRevision());
      }
      
      return result;
    } else if (existingEntry != null) {
      // this happens if e.g. the MI entries are configured on a persistent storage and the server is restarted.
      existingEntry.setXynaOrder(xo);
      throw suspend(existingEntry);
    } else {

      // no entry present: first arrival or no persistent persistence layer chosen and server restarted
      ManualInteractionEntry entry = new ManualInteractionEntry(getCurrentOwnStorableBinding());
      entry.setXynaOrder(xo);

      entry.setID(id);
      entry.setReason(reason);

      entry.setType(type);
      entry.setUserGroup(userGroup);
      entry.setTodo(todo);

      entry.setAllowedResponses(allowedResponses);
      
      WorkflowStacktrace stacktrace = new WorkflowStacktrace(xo.getDestinationKey().getOrderType());

      XynaOrderServerExtension parent = xo.getParentOrder();
      while (parent != null) {
        stacktrace.push(parent.getDestinationKey().getOrderType());
        parent = parent.getParentOrder();
      }

      entry.setWfTrace(stacktrace);

      String oldStatus = null;
      OrderInstanceResult result;
      try {
        result =
            XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive()
                .search(new OrderInstanceSelect().selectId().selectStatus().whereId().isEqual(id).finalizeSelect(OrderInstanceSelect.class), 1);
      } catch (XNWH_WhereClauseBuildException e) {
        throw new RuntimeException(e);
      }
      OrderInstance orderInstance = null;
      if (result.getResult().size() > 0) {
        orderInstance = result.getResult().get(0);
      } else {
        //ntbd: objekt nicht in db vorhanden, weil monitoringlevel nicht hoch genug.
        //TODO ähnlich wie bei updatestatus machen, dass monitoring level verglichen wird? 
      }      
      if (orderInstance != null) {
        oldStatus = orderInstance.getStatusAsString();
        entry.setCorrelatedOrderIsMonitored(true);
      } else {
        oldStatus = OrderInstanceStatus.RUNNING.getName();
        entry.setCorrelatedOrderIsMonitored(false);
        //falls kein monitoringlevel für mi gesetzt ist, gibts hier nichts bessres
        //die listener sollten aber benachrichtigt werden, dass der status nicht mehr mi ist.
      }

      entry.setOldInstanceStatus(oldStatus);
      // the entry is persisted when the exception is caught so that the entry wont be processed before the
      // the suspension has happened.

      logger.debug("Notifying parent order of suspension due to manual interaction");
      throw suspend(entry);
    }

  }




  private GeneralXynaObject recreateWithAppropriateClassLoader(GeneralXynaObject object, long targetRevision) {
    try {
      // Unter Umständen wurde das XynaObject mit dem falschen Classloader erzeugt. Bei Bedarf mit korrektem ClassLoader
      // neu erzeugen, um ClassCastExceptions zu vermeiden.
      GeneralXynaObject result = XynaObject.generalFromXml(object.toXml(), targetRevision);
      return result;
    } catch (XynaException e) {
      logger.warn("Could not recreate response with correct revision."
          + " This may lead to a ClassCastException during workflow execution.", e);
      return object;
    }
  }


  public ManualInteractionResult search(ManualInteractionSelect select, int maxRows) throws PersistenceLayerException {
    try {
      return searchInternally(select, maxRows);
    } catch (XNWH_IncompatiblePreparedObjectException e) {
      // see comment in OrderArchive
      cache.clear();
      return searchInternally(select, maxRows);
    }
  }


  public ExtendedManualInteractionResult searchExtended(ManualInteractionSelect select, int maxRows)
      throws PersistenceLayerException {
    return new ExtendedManualInteractionResult(search(select, maxRows));
  }


  private ManualInteractionResult searchInternally(ManualInteractionSelect select, int maxRows)
                  throws PersistenceLayerException {

    String selectString;
    ResultSetReader<ManualInteractionEntry> reader;
    String selectCountString;
    Parameter paras;

    //select result to filter already processed MIs
    select.selectResult();
    select.selectRevision();

    try {
      selectCountString = select.getSelectCountString();
      if (selectCountString.contains(" where ")) {
        selectCountString += " and " + ManualInteractionEntry.MI_COL_RESULT + " is null";
      } else {
        selectCountString += " where " + ManualInteractionEntry.MI_COL_RESULT + " is null";
      }
      selectString = select.getSelectString();
      if (selectString.contains(" where ")) {
        selectString += " and " + ManualInteractionEntry.MI_COL_RESULT + " is null";
      } else {
        selectString += " where " + ManualInteractionEntry.MI_COL_RESULT + " is null";
      }
      reader = select.getReader();
      paras = select.getParameter();
    } catch (XNWH_InvalidSelectStatementException e) {
      throw new RuntimeException("Problem with select statement: " + e.getMessage(), e);
    }

    int countAll = 0;
    List<ManualInteractionEntry> mies = new ArrayList<ManualInteractionEntry>();

    ODSConnection con = getODS().openConnection(ODSConnectionType.DEFAULT);
    try {
      PreparedQuery<ManualInteractionEntry> query =
        cache.getQueryFromCache(selectString, con, reader);

      mies.addAll(con.query(query, paras, maxRows));
      if (mies.size() >= maxRows) {
        PreparedQuery<? extends OrderCount> queryCount =
          cache.getQueryFromCache(selectCountString, con, OrderCount.getCountReader());
        OrderCount count = con.queryOneRow(queryCount, paras);
        countAll = count.getCount();
      } else {
        countAll = mies.size();
      }
    } finally {
      con.closeConnection();
    }

    return new ManualInteractionResult(mies, countAll);

  }
  
  
  /**
   * @param existingEntry
   * @throws PersistenceLayerException 
   */
  private ProcessSuspendedException suspend(ManualInteractionEntry manualInteractionEntry) throws PersistenceLayerException {
    SuspensionCause_ManualInteraction suspensionCause = new SuspensionCause_ManualInteraction(manualInteractionEntry.getOldInstanceStatusAsEnum());
    
    PersistManualInteractionEntry pmie = new PersistManualInteractionEntry(manualInteractionEntry);
    
    WarehouseRetryExecutor.executeWithRetriesNoException(pmie, ODSConnectionType.DEFAULT,
                                                           Constants.DEFAULT_CONNECTION_TO_CLUSTER_BROKEN_RETRIES,
                                                           Constants.DEFAULT_NO_CONNECTION_AVAILABLE_RETRIES__USERINTERACTION,
                                                           pmie.getStorableClassList() );
    
    return new ProcessSuspendedException(suspensionCause);
  }

  private static class PersistManualInteractionEntry implements WarehouseRetryExecutableNoException<Boolean> {
    private ManualInteractionEntry manualInteractionEntry;
    
    public PersistManualInteractionEntry(ManualInteractionEntry manualInteractionEntry) {
      this.manualInteractionEntry = manualInteractionEntry;
    }

    public StorableClassList getStorableClassList() {
      return new StorableClassList(ManualInteractionEntry.class);
    }

    public Boolean executeAndCommit(ODSConnection con) throws PersistenceLayerException {
      
      con.persistObject(manualInteractionEntry);
      con.commit();
      return Boolean.TRUE;
    }
    
  }
  

  public boolean isClustered() {
    return rmiIsClustered;
  }


  public long getClusterInstanceId() {
    if (!rmiIsClustered) {
      throw new IllegalStateException("Component is not clustered.");
    }
    return rmiClusterInstanceId;
  }


  public void enableClustering(long clusterinstanceId) throws XFMG_UnknownClusterInstanceIDException,
      XFMG_ClusterComponentConfigurationException {
    
    this.rmiClusterInstanceId = clusterinstanceId;
    XynaClusteringServicesManagementInterface clusterMgmt =
        XynaFactory.getInstance().getFactoryManagement().getXynaClusteringServicesManagement();
    rmiClusterInstance = (RMIClusterProvider) clusterMgmt.getClusterInstance(clusterinstanceId);
    if (rmiClusterInstance == null) {
      throw new IllegalArgumentException("Did not find Clusterinstance with id " + clusterinstanceId);
    }
    try {
      
      clusteredManualInteractionManagmentInterfaceId =
             ((RMIClusterProvider) rmiClusterInstance).addRMIInterfaceWithClassReloading("RemoteManualInteractionManagment", this);
      
    } catch (XMCP_RMI_BINDING_ERROR e) {
      throw new XFMG_ClusterComponentConfigurationException(getName(), clusterinstanceId, e);
    }
    rmiIsClustered = true;
    clusterMgmt.addClusterStateChangeHandler(clusterinstanceId, new ClusterStateChangeHandler() {

      public boolean isReadyForChange(ClusterState newState) {
        return true; //immer bereit
      }
      public void onChange(ClusterState newState) {
        rmiClusterState = newState;
      }
      
    });
    rmiClusterState = rmiClusterInstance.getState();
  }
  

  public void disableClustering() {
    rmiIsClustered = false;
    rmiClusterState = ClusterState.NO_CLUSTER;
    rmiClusterInstance = null;
    clusteredManualInteractionManagmentInterfaceId = 0;
    rmiClusterInstanceId = 0;
    currentOwnStorableBinding = XynaClusteringServicesManagement.DEFAULT_BINDING_NO_CLUSTER;
    //TODO remove clusterstatechangehandlers
  }


  public String getName() {
    return getDefaultName();
  }


  // remotely
  public ProcessManualInteractionResult processManualInteractionEntry(Long id, int binding, String responseXML, Long revision)
      throws RemoteException, XynaException {

    if (binding == getCurrentOwnStorableBinding()) {
      ManualInteractionEntry entry = new ManualInteractionEntry(id, getCurrentOwnStorableBinding());
      ODSConnection con = getODS().openConnection(ODSConnectionType.DEFAULT);
      try {
        try {
          con.queryOneRowForUpdate(entry);
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          return ProcessManualInteractionResult.NOT_FOUND;
        }
        
        GeneralXynaObject response = XynaObject.generalFromXml(responseXML, revision);
        
        ProcessManualInteractionResult result = processManualInteractionEntryLocally(entry, con, response);
        con.commit();
        return result;
      } finally {
        con.closeConnection();
      }
    } else {
      return ProcessManualInteractionResult.FOREIGN_BINDING;
    }

  }


  private Integer getCurrentOwnStorableBinding() {
    if(currentOwnStorableBinding == null) {
      ManualInteractionEntry tmpInstance = new ManualInteractionEntry();
      currentOwnStorableBinding = tmpInstance.getLocalBinding(ODSConnectionType.DEFAULT);
    }
    return currentOwnStorableBinding;
  }
  
  private ODS getODS() {
    if (ods == null) {
      ods = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS();
    }
    return ods;
  }


  public void init(InitializableRemoteInterface rmiImpl) {
    rmiImpl.init(this);
  }

  public void shutdown(InitializableRemoteInterface rmiImpl) {
  }
  
  public String getFQClassName() {
    return MIRemoteInterfaceImpl.class.getName();
  }
  
  private volatile boolean isReadyForChange = true;
  public boolean isReadyForChange(ClusterState newState) {
    return isReadyForChange;
  }


  public void onChange(final ClusterState newState) {
    if (logger.isDebugEnabled()) {
      logger.debug("Got notified of state transition "+actualState+ " -> " + newState );
    }
    
    final FutureExecution fe = storableClusterContext.getFutureExecution(); 
    fe.execAsync( new FutureExecutionTask(FUTURE_EXECUTION_TASK_ON_CHANGEHANDLER_ID) {

      @Override
      public void execute() {
        actualState = newState;
      }

      public int[] after() {
        return new int[] {ClusteredOrderArchive.FUTURE_EXECUTION_TASK_ON_CHANGEHANDLER_ID};
      }
      
    });
    
  }


  public boolean cleanupOrderFamily(Long rootOrderId, Set<Long> suspendedOrderIds, ODSConnection con)
      throws PersistenceLayerException {

    List<ManualInteractionEntry> entries = null;
    if (suspendedOrderIds == null) {
      entries =
          con.query(loadAllManualInteractionEntriesForRootOrderId, 
                    new Parameter(rootOrderId, getCurrentOwnStorableBinding()),
                    Integer.MAX_VALUE);
    } else {
      entries = new ArrayList<ManualInteractionEntry>();
      for (Long orderId : suspendedOrderIds ) {
        ManualInteractionEntry mie = con.queryOneRow(loadMIEntryForOrderId, new Parameter(orderId, getCurrentOwnStorableBinding()));
        if (mie != null) {
          entries.add(mie);
        }
      }
    }
    if (!entries.isEmpty()) {
      con.delete(entries);
    }
    return !entries.isEmpty();
  }
  

}
