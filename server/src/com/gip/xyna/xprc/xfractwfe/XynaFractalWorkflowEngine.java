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

package com.gip.xyna.xprc.xfractwfe;



import java.io.File;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipInputStream;

import org.apache.log4j.Level;
import org.apache.log4j.Priority;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.CentralFactoryLogging.LogChangeListener;
import com.gip.xyna.Department;
import com.gip.xyna.FileUtils;
import com.gip.xyna.FutureExecutionTask;
import com.gip.xyna.Section;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryPath;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.exceptions.XACT_JarFileUnzipProblem;
import com.gip.xyna.xact.trigger.FilterInstanceStorable;
import com.gip.xyna.xact.trigger.FilterStorable;
import com.gip.xyna.xact.trigger.TriggerInstanceStorable;
import com.gip.xyna.xact.trigger.TriggerStorable;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.BatchRepositoryEvent;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.EventType;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.SingleRepositoryEvent;
import com.gip.xyna.xdev.exceptions.XDEV_UNSUPPORTED_FEATURE;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.XynaFactoryManagement;
import com.gip.xyna.xfmg.exceptions.XFMG_MDMObjectClassLoaderNotFoundException;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyNode;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xods.XynaFactoryManagementODS;
import com.gip.xyna.xfmg.xods.configuration.Configuration;
import com.gip.xyna.xfmg.xods.configuration.IPropertyChangeListener;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeParameter;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.PreparedQueryCache;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xprc.CronLikeOrderCreationParameter;
import com.gip.xyna.xprc.ResponseListener;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.exceptions.XPRC_DeploymentDuringUndeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_DeploymentHandlerException;
import com.gip.xyna.xprc.exceptions.XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT;
import com.gip.xyna.xprc.exceptions.XPRC_ExclusiveDeploymentInProgress;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InternalObjectMayNotBeUndeployedException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidServiceIdException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMUndeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH;
import com.gip.xyna.xprc.exceptions.XPRC_OperationNotFoundInDatatypeException;
import com.gip.xyna.xprc.exceptions.XPRC_UNSUPPORTED_FILE_EXTENSION_DEPLOYMENT;
import com.gip.xyna.xprc.exceptions.XPRC_UnDeploymentHandlerException;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement.DispatcherType;
import com.gip.xyna.xprc.xfractwfe.base.AFractalWorkflowProcessor;
import com.gip.xyna.xprc.xfractwfe.base.AFractalWorkflowProcessorProcessingCheckAlgorithm;
import com.gip.xyna.xprc.xfractwfe.base.DefaultAFractalWorkflowProcessorProcessingCheckAlgorithm;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling.DeploymentHandler;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling.UndeploymentHandler;
import com.gip.xyna.xprc.xfractwfe.base.FractalProcessStep;
import com.gip.xyna.xprc.xfractwfe.base.Handler;
import com.gip.xyna.xprc.xfractwfe.base.XynaProcess;
import com.gip.xyna.xprc.xfractwfe.fractalworkflowexecution.ProcessManagement;
import com.gip.xyna.xprc.xfractwfe.fractalworkflowexecution.fractalcleanup.FractalCleanupProcessor;
import com.gip.xyna.xprc.xfractwfe.fractalworkflowexecution.fractalexecution.FractalExecutionProcessor;
import com.gip.xyna.xprc.xfractwfe.fractalworkflowexecution.fractalplanning.FractalPlanningProcessor;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DependentObjectMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.MDMParallelDeploymentException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.WorkflowProtectionMode;
import com.gip.xyna.xprc.xfractwfe.generation.Service;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xfractwfe.specialpurpose.SpecialPurposeHelper;
import com.gip.xyna.xprc.xfractwfe.specialpurpose.synchronization.SynchronizationManagement;
import com.gip.xyna.xprc.xpce.OrderContext;
import com.gip.xyna.xprc.xpce.OrderContextServerExtension;
import com.gip.xyna.xprc.xpce.WorkflowEngine;
import com.gip.xyna.xprc.xpce.XynaProcessCtrlExecution;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.ServiceDestination;
import com.gip.xyna.xprc.xpce.dispatcher.XynaDispatcher;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceCompensationStatus;
import com.gip.xyna.xprc.xsched.XynaScheduler;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrderStartUnderlyingOrderAlgorithm;
import com.gip.xyna.xprc.xsched.cronlikescheduling.DefaultCronLikeOrderStartUnderlyingOrderAlgorithm;
import com.gip.xyna.xprc.xsched.xynaobjects.RemoteCall;



public class XynaFractalWorkflowEngine extends Section implements WorkflowEngine, IPropertyChangeListener {

  public static final String DEFAULT_NAME = "Xyna Fractal Workflow Engine";

  public static final int FUTUREEXECUTION_ID = XynaFactory.getInstance().getFutureExecution().nextId();

  private FractalPlanningProcessor planningProcessor;
  private FractalExecutionProcessor executionProcessor;
  private FractalCleanupProcessor cleanupProcessor;
  private SpecialPurposeHelper specialPurposeHelper;
  private ProcessManagement processManager = new ProcessManagement();
  private DeploymentHandling deploymentHandling;
  private FractalStepHandlerManager stepHandlerManager;
  private SynchronizationManagement synchronisationManagement;

  private static final ConcurrentMap<Thread, OrderContextServerExtension> threadToOrderContextMapping =
      new ConcurrentHashMap<Thread, OrderContextServerExtension>();

  private PreparedQueryCache queryCache = new PreparedQueryCache(60000L, 30000L);



  public XynaFractalWorkflowEngine() throws XynaException {
    super();
  }

  // cyclic dependency, more info in init()
  static {
    /*addDependencies(XynaFractalWorkflowEngine.class, new ArrayList<XynaFactoryPath>(Arrays
                    .asList(new XynaFactoryPath[] {
                                    new XynaFactoryPath(XynaFactoryManagement.class, XynaFactoryControl.class, DependencyRegister.class)})));*/
    addDependencies(XynaFractalWorkflowEngine.class,
                    new ArrayList<XynaFactoryPath>(Arrays
                        .asList(new XynaFactoryPath[] {new XynaFactoryPath(XynaFactoryManagement.class,
                                                                           XynaFactoryManagementODS.class,
                                                                           Configuration.class)})));
    addDependencies(XynaFractalWorkflowEngine.class,
                    new ArrayList<XynaFactoryPath>(Arrays
                        .asList(new XynaFactoryPath[] {new XynaFactoryPath(XynaProcessing.class, XynaScheduler.class)})));
  }


  private Priority stepHandlerLogPriority = Level.toLevel(Level.DEBUG_INT);
  private boolean stepHandlerLogging = logger.isEnabledFor(stepHandlerLogPriority);
  private boolean addedAsPropertyChangeHandler = false;

  private class LogHandler extends Handler {

    private String action;
    private boolean input;
    private boolean output;
    private Handler preHandler;

    public LogHandler(String action, boolean input, boolean output) {
      this(action,input,output,null);
    }

    public LogHandler(String action, boolean input, boolean output, Handler prehandler) {
      this.action = action;
      this.input = input;
      this.output = output;
      this.preHandler = prehandler;
    }

    /* (non-Javadoc)
     * @see com.gip.xyna.xprc.xfractwfe.base.Handler#handle(com.gip.xyna.xprc.xfractwfe.base.XynaProcess, com.gip.xyna.xprc.xfractwfe.base.FractalProcessStep)
     */
    @Override
    public void handle(XynaProcess process, FractalProcessStep<?> pstep) {
      if( preHandler != null ) {
        preHandler.handle(process, pstep);
      }
      if (stepHandlerLogging) {
        StringBuilder sb = new StringBuilder();
        sb.append( "[loghandler] ");
        sb.append( action );
        sb.append( " step ").append(pstep.getN());
        String label = pstep.getLabel();
        if( label != null ) {
          sb.append(" \"").append(label).append("\"");
        }
        String lane = pstep.getLaneId();
        if( lane != null ) {
          sb.append(" in lane ").append(lane);
        }
        if( input ) {
          sb.append(" [ Input parameters: ").append(new Container(pstep.getCurrentIncomingValues())).append(" ]");
        }
        if( output ) {
          sb.append(" [ Output parameters: ").append(new Container(pstep.getCurrentOutgoingValues())).append(" ]");
        }
        if( !input && !output ) {
          sb.append(" [ Exception: ").append(pstep.getCurrentUnhandledThrowable()).append(" ]");
        }
        logger.log(stepHandlerLogPriority, sb.toString() );
      }
    }
    
  }
  
  private class StartCompensationHandler extends Handler {

    /* (non-Javadoc)
     * @see com.gip.xyna.xprc.xfractwfe.base.Handler#handle(com.gip.xyna.xprc.xfractwfe.base.XynaProcess, com.gip.xyna.xprc.xfractwfe.base.FractalProcessStep)
     */
    @Override
    public void handle(XynaProcess process, FractalProcessStep<?> pstep) {
      if (!process.hasCalledGlobalCompensatePreHandlersAtLeastOnce()) {
        try {
          XynaFactory.getInstance().getProcessing().getOrderStatus()
              .compensationStatus(process.getCorrelatedXynaOrder(), OrderInstanceCompensationStatus.RUNNING);
        } catch (Throwable t) {
          Department.handleThrowable(t);
          //TODO Exception nicht loggen, sondern als Warnung an XynaOrder anhängen
          logger.warn("Could not write compensation status "+OrderInstanceCompensationStatus.RUNNING+" for "+process.getCorrelatedXynaOrder(), t);
        }
      }
    }
    
  }
  
  
  @Override
  public void init() throws XynaException {

    planningProcessor = new FractalPlanningProcessor();
    deployFunctionGroup(planningProcessor);

    executionProcessor = new FractalExecutionProcessor();
    deployFunctionGroup(executionProcessor);

    cleanupProcessor = new FractalCleanupProcessor();
    deployFunctionGroup(cleanupProcessor);
    
    specialPurposeHelper = new SpecialPurposeHelper();
    deployFunctionGroup(specialPurposeHelper);

    stepHandlerManager = new FractalStepHandlerManager();
    deployFunctionGroup(stepHandlerManager);

    // log handler hinzufügen
    XynaProcess.addGlobalPreHandler(new LogHandler( "Starting", true, false ));
    XynaProcess.addGlobalErrorHandler(new LogHandler( "Error in", false, false ));
    XynaProcess.addGlobalPostHandler(new LogHandler( "Finished", false, true ));
    XynaProcess.addGlobalPreCompensationHandler(new LogHandler( "Starting compensate", true, false,
                                                                new StartCompensationHandler() ) ); //FIXME ist das hier nötig? besser MasterWorkflowPostScheduler?
    XynaProcess.addGlobalPostCompensationHandler(new LogHandler( "Finished compensate", false, true ));

    deploymentHandling = new DeploymentHandling();
    deployFunctionGroup(deploymentHandling);


    synchronisationManagement = new SynchronizationManagement();
    deployFunctionGroup(synchronisationManagement);

    // Moved to DependencyRegister to resolve a cyclic dependency 
    /*XynaFactory.getInstance().getFactoryManagementPortal().getXynaFactoryControl().getDependencyRegister().addDependency(
                                                                                                                         DependencySourceType.XYNAPROPERTY,
                                                                                                                         XynaProperty.XYNA_DISABLE_XSD_VALIDATION,
                                                                                                                         DependencySourceType.XYNAFACTORY,
                                                                                                                         DEFAULT_NAME);*/

    deploymentHandling.addDeploymentHandler(DeploymentHandling.PRIORITY_XPRC, new XynaDispatcherDeploymentHandler());
    deploymentHandling
        .addUndeploymentHandler(DeploymentHandling.PRIORITY_XPRC, new XynaDispatcherUndeploymentHandler());

    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration()
        .addPropertyChangeListener(this);
    CentralFactoryLogging.registerLogChangeListener(new LogChangeListener() {

      public void onLogChanged() {
        stepHandlerLogging = logger.isEnabledFor(stepHandlerLogPriority);
      }
      
    });
    addedAsPropertyChangeHandler = true;
    propertyChanged();
    
    XynaFactory.getInstance().getFutureExecution().execAsync(new FutureExecutionTask(FUTUREEXECUTION_ID) {

      @Override
      public void execute() {
      }
      
    });    
  }


  @Override
  public void shutdown() throws XynaException {
    super.shutdown();
    if (addedAsPropertyChangeHandler) {
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration()
          .removePropertyChangeListener(this);
    }
  }


  public ProcessManagement getProcessManager() {
    return processManager;
  }


  public DeploymentHandling getDeploymentHandling() {
    return deploymentHandling;
  }


  public FractalStepHandlerManager getStepHandlerManager() {
    return stepHandlerManager;
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  public FractalPlanningProcessor getPlanningProcessor() {
    return planningProcessor;
  }


  public FractalCleanupProcessor getCleanupProcessor() {
    return cleanupProcessor;
  }


  public FractalExecutionProcessor getExecutionProcessor() {
    return executionProcessor;
  }


  public SynchronizationManagement getSynchronizationManagement() {
    return synchronisationManagement;
  }
  
  public SpecialPurposeHelper getSpecialPurposeHelper() {
    return specialPurposeHelper;
  }

  public void deployMultiple(Map<XMOMType, List<String>> deploymentItems, WorkflowProtectionMode mode, Long revision) 
                  throws MDMParallelDeploymentException, XPRC_DeploymentDuringUndeploymentException,
                  XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException {
    try {
      GenerationBase.deploy(deploymentItems, DeploymentMode.codeChanged, false, mode, revision, "Explicit simultaneous deployment.");
    } catch (MDMParallelDeploymentException e) {
      e.generateSerializableFailedObjects();
      throw e;
    }
  }
  
  
  public void deployDatatype(String fqClassName, WorkflowProtectionMode mode, String fileName, InputStream inputStream,
                             Long revision) throws Ex_FileAccessException, XACT_JarFileUnzipProblem,
                  XPRC_UNSUPPORTED_FILE_EXTENSION_DEPLOYMENT, XPRC_InvalidPackageNameException,
                  XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_DeploymentDuringUndeploymentException,
                  XPRC_InheritedConcurrentDeploymentException, XPRC_MDMDeploymentException {
    
    deployDatatypeInternally(fqClassName, mode, fileName, inputStream, false, revision);
  }


  public void undeployDatatype(String fqClassName, boolean undeployDependendObjects, boolean disableChecks, 
                               Long revision) throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH,
                  XPRC_InvalidPackageNameException, XPRC_InternalObjectMayNotBeUndeployedException,
                  XPRC_MDMUndeploymentException, XPRC_ExclusiveDeploymentInProgress, XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT {
    
    DOM d = DOM.getInstance(fqClassName, revision);
    DependentObjectMode dependentObjectMode = undeployDependendObjects ? DependentObjectMode.UNDEPLOY : DependentObjectMode.PROTECT;
    d.undeploy(dependentObjectMode, disableChecks);
  }


  public void deployWorkflow(String fqXmlName, WorkflowProtectionMode mode, Long revision)
                  throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException,
                  XPRC_DeploymentDuringUndeploymentException, XPRC_InheritedConcurrentDeploymentException,
                  XPRC_MDMDeploymentException {
    
    WF wf = WF.getInstance(fqXmlName, revision);
    wf.setDeploymentComment("Explicit deployment of " + fqXmlName);
    wf.deploy(DeploymentMode.codeChanged, mode);
  }


  public void undeployWorkflow(String originalFqName, boolean undeployDependentObjects, boolean disableChecks,
                               Long revision) throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH,
                  XPRC_InvalidPackageNameException, XPRC_InternalObjectMayNotBeUndeployedException,
                  XPRC_MDMUndeploymentException, XPRC_ExclusiveDeploymentInProgress, XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT {
    
    WF wf = WF.getInstance(originalFqName, revision);
    DependentObjectMode dependentObjectMode = undeployDependentObjects ? DependentObjectMode.UNDEPLOY : DependentObjectMode.PROTECT;
    wf.undeploy(dependentObjectMode, disableChecks);
  }


  public void deployException(String fqXmlName, WorkflowProtectionMode mode, Long revision)
                  throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException,
                  XPRC_DeploymentDuringUndeploymentException, XPRC_InheritedConcurrentDeploymentException,
                  XPRC_MDMDeploymentException {
    
    ExceptionGeneration exception = ExceptionGeneration.getInstance(fqXmlName, revision);
    exception.setDeploymentComment("Explicit deployment of " + fqXmlName);
    exception.deploy(DeploymentMode.codeChanged, mode);
  }


  public void undeployException(String originalFqName, boolean undeployDependentObjects, boolean disableChecks,
                                Long revision) throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH,
                  XPRC_InvalidPackageNameException, XPRC_InternalObjectMayNotBeUndeployedException,
                  XPRC_MDMUndeploymentException, XPRC_ExclusiveDeploymentInProgress, XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT {
    
    ExceptionGeneration exception = ExceptionGeneration.getInstance(originalFqName, revision);
    DependentObjectMode dependentObjectMode = undeployDependentObjects ? DependentObjectMode.UNDEPLOY : DependentObjectMode.PROTECT;
    exception.undeploy(dependentObjectMode, disableChecks);
  }
  
  
  public void deployDatatypeAndDependants(String fqXmlName, String fileName, InputStream inputStream)
                  throws XACT_JarFileUnzipProblem, XPRC_UNSUPPORTED_FILE_EXTENSION_DEPLOYMENT,
                  XPRC_InvalidPackageNameException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH,
                  XPRC_DeploymentDuringUndeploymentException, XPRC_InheritedConcurrentDeploymentException,
                  XPRC_MDMDeploymentException, Ex_FileAccessException {
    
    deployDatatypeInternally(fqXmlName, WorkflowProtectionMode.FORCE_DEPLOYMENT, fileName, inputStream, true,
                             RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }


  public void deployDatatype(String fqXmlName, WorkflowProtectionMode mode, String fileName, InputStream inputStream)
                  throws XACT_JarFileUnzipProblem, XPRC_UNSUPPORTED_FILE_EXTENSION_DEPLOYMENT,
                  XPRC_InvalidPackageNameException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH,
                  XPRC_DeploymentDuringUndeploymentException, XPRC_InheritedConcurrentDeploymentException,
                  XPRC_MDMDeploymentException, Ex_FileAccessException {
    
    deployDatatypeInternally(fqXmlName, mode, fileName, inputStream, false, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }


  public void deployDatatype(String fqXmlName, WorkflowProtectionMode mode, Map<String, InputStream> jars)
                  throws XPRC_InvalidPackageNameException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH,
                  XPRC_DeploymentDuringUndeploymentException, XPRC_InheritedConcurrentDeploymentException,
                  XPRC_MDMDeploymentException, Ex_FileAccessException {
    deployDatatype(fqXmlName, mode, jars, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }

  public void deployDatatype(String fqXmlName, WorkflowProtectionMode mode, Map<String, InputStream> jars, Long revision)
                  throws XPRC_InvalidPackageNameException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH,
                  XPRC_DeploymentDuringUndeploymentException, XPRC_InheritedConcurrentDeploymentException,
                  XPRC_MDMDeploymentException, Ex_FileAccessException {
    try {
      deployDatatypeInternally(fqXmlName, mode, jars, false, revision);
    } catch (XACT_JarFileUnzipProblem e) {
      throw new Ex_FileAccessException("Encountered an error while working with an unexpected zipFile", e);
    } catch (XPRC_UNSUPPORTED_FILE_EXTENSION_DEPLOYMENT e) {
      throw new Ex_FileAccessException("Unexpected fileExtension in given jars", e);
    }
  }

  
  private void deployDatatypeInternally(String fqXmlName, WorkflowProtectionMode mode, String fileName,
                                        InputStream inputStream, boolean inheritCodeChange, Long revision)
                  throws Ex_FileAccessException, XACT_JarFileUnzipProblem, XPRC_UNSUPPORTED_FILE_EXTENSION_DEPLOYMENT,
                  XPRC_InvalidPackageNameException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH,
                  XPRC_DeploymentDuringUndeploymentException, XPRC_InheritedConcurrentDeploymentException,
                  XPRC_MDMDeploymentException {
    Map<String, InputStream> jarMap;
    if (fileName == null || inputStream == null) {
      jarMap = null;
    } else {
      jarMap = Collections.singletonMap(fileName, inputStream);
    }
    deployDatatypeInternally(fqXmlName, mode, jarMap, inheritCodeChange, revision);
  }
  
  private void deployDatatypeInternally(String fqXmlName, WorkflowProtectionMode mode, Map<String, InputStream> jars,
                                        boolean inheritCodeChange, Long revision)
                  throws Ex_FileAccessException, XACT_JarFileUnzipProblem, XPRC_UNSUPPORTED_FILE_EXTENSION_DEPLOYMENT,
                  XPRC_InvalidPackageNameException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH,
                  XPRC_DeploymentDuringUndeploymentException, XPRC_InheritedConcurrentDeploymentException,
                  XPRC_MDMDeploymentException {
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    if (jars != null && jars.size() > 0 && revisionManagement.isWorkspaceRevision(revision)) {
      String fqClassName = GenerationBase.transformNameForJava(fqXmlName);
      for (Entry<String, InputStream> entry : jars.entrySet()) {
        String savedServicesDir = RevisionManagement.getPathForRevision(PathType.SERVICE, revision, false);
        File targetdir = new File(savedServicesDir + Constants.fileSeparator + fqClassName);
        if (entry.getKey().endsWith("zip")) {
          FileUtils.saveZipToDir(new ZipInputStream(entry.getValue()), targetdir);
        } else if (entry.getKey().endsWith("jar")) {
          File sourceFile = new File(entry.getKey());
          //ans target dir nur den file-namen (ohne pfad) anhängen
          File f = new File(targetdir, sourceFile.getName());
          FileUtils.saveToFile(entry.getValue(), f);
        } else {
          throw new XPRC_UNSUPPORTED_FILE_EXTENSION_DEPLOYMENT();
        }
      }
    }

    DOM d = DOM.getInstance(fqXmlName, revision);
    d.setDeploymentComment("Explicit deployment of " + fqXmlName);
    d.deploy(DeploymentMode.codeChanged, inheritCodeChange, mode);
    
    new SingleRepositoryEvent(revision).addEvent(new ProjectCreationOrChangeProvider.BasicProjectCreationOrChangeEvent(EventType.SERVICE_DEPLOY, fqXmlName));
  }


  public void undeployDatatype(String fqXmlName, boolean undeployDependendObjects, boolean disableChecks)
      throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException,
      XPRC_InternalObjectMayNotBeUndeployedException, XPRC_MDMUndeploymentException, XPRC_ExclusiveDeploymentInProgress, XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT {
    DOM d = DOM.getInstance(fqXmlName);
    DependentObjectMode dependentObjectMode = undeployDependendObjects ? DependentObjectMode.UNDEPLOY : DependentObjectMode.PROTECT;
    d.undeploy(dependentObjectMode, disableChecks);
  }


  public void undeployWorkflow(String fqXmlName, boolean disableChecks)
      throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException,
      XPRC_InternalObjectMayNotBeUndeployedException, XPRC_MDMUndeploymentException, XPRC_ExclusiveDeploymentInProgress, XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT {
    undeployWorkflow(fqXmlName, false, disableChecks);
  }


  public void undeployWorkflow(String fqXmlName, boolean undeployDependentObjects, boolean disableChecks)
      throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException,
      XPRC_InternalObjectMayNotBeUndeployedException, XPRC_MDMUndeploymentException, XPRC_ExclusiveDeploymentInProgress, XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT {
    WF wf = WF.getInstance(fqXmlName);
    DependentObjectMode dependentObjectMode = undeployDependentObjects ? DependentObjectMode.UNDEPLOY : DependentObjectMode.PROTECT;
    wf.undeploy(dependentObjectMode, disableChecks);
  }

  public void undeployXMOMObject(String originalFqName, XMOMType type, DependentObjectMode dependentObjectMode, boolean disableChecks, Long revision) throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH,
                  XPRC_InvalidPackageNameException, XPRC_InternalObjectMayNotBeUndeployedException,
                  XPRC_MDMUndeploymentException, XPRC_ExclusiveDeploymentInProgress, XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT {
    GenerationBase gb = GenerationBase.getInstance(type, originalFqName, revision);
    gb.undeploy(dependentObjectMode, disableChecks);
  }

  private Map<String, XMOMType> retrieveGenerationBaseDependencies(DependencyNode node, Long revision) {
    Set<DependencyNode> nodes =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDependencyRegister()
            .getDependencies(node.getUniqueName(), node.getType(), revision, true);
    Map<String, XMOMType> map = new HashMap<String, XMOMType>();
    for (DependencyNode dn : nodes) {
      switch (dn.getType()) {
        case DATATYPE :
        case WORKFLOW :
        case XYNAEXCEPTION :
          if (dn.getRevision() != null && dn.getRevision().equals(revision)) {
            map.put(dn.getUniqueName(), XMOMType.getXMOMTypeByDependencySourceType(dn.getType()));
          }
          break;

        default :
          break;
      }
    }
    return map;
  }

  @Deprecated
  public void deleteWorkflow(String fullXmlName, boolean disableChecks,
                             boolean recursivlyUndeployIfDeployedAndDependenciesExist, boolean deleteDependencies)
      throws XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT {
    deleteWorkflow(fullXmlName, disableChecks, recursivlyUndeployIfDeployedAndDependenciesExist, deleteDependencies, true, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }
  
  public void deleteWorkflow(String fullXmlName, boolean disableChecks,
                             boolean recursivlyUndeployIfDeployedAndDependenciesExist, boolean deleteDependencies, boolean checkDeploymentLock,
                             Long revision)
      throws XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT {
    DependentObjectMode dependentObjectMode = recursivlyUndeployIfDeployedAndDependenciesExist ? DependentObjectMode.INVALIDATE : DependentObjectMode.PROTECT;
    deleteXMOMObject(fullXmlName, XMOMType.WORKFLOW, disableChecks, dependentObjectMode, checkDeploymentLock, revision);
  }

  @Deprecated
  public void deleteDatatype(String fullXmlName, boolean disableChecks,
                             boolean recursivlyUndeployIfDeployedAndDependenciesExist, boolean deleteDependencies)
        throws XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT {
    deleteDatatype(fullXmlName, disableChecks, recursivlyUndeployIfDeployedAndDependenciesExist, deleteDependencies, true, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }


  public void deleteDatatype(String fullXmlName, boolean disableChecks,
                             boolean recursivlyUndeployIfDeployedAndDependenciesExist, boolean deleteDependencies, boolean checkDeploymentLock,
                             Long revision)
        throws XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT {
    DependentObjectMode dependentObjectMode = recursivlyUndeployIfDeployedAndDependenciesExist ? DependentObjectMode.INVALIDATE : DependentObjectMode.PROTECT;
    deleteXMOMObject(fullXmlName, XMOMType.DATATYPE, disableChecks, dependentObjectMode, checkDeploymentLock, revision);
  }
  
  
  /**
   * Gibt aus der übergebenen Liste alle WORKFLOWs, EXCEPTIONs und DATATYPEs zurück, die bereits in der Revision vorliegen
   * @param xmomObjects
   * @param revision
   * @return
   */
  public Map<XMOMType,List<String>> existsInRevision( Map<XMOMType,List<String>> xmomObjects, Long revision) {
    EnumMap<XMOMType,List<String>> existing = new EnumMap<XMOMType,List<String>>(XMOMType.class);
    for( XMOMType type : Arrays.asList(XMOMType.WORKFLOW,XMOMType.EXCEPTION, XMOMType.DATATYPE) ) {
      if( xmomObjects.get(type) != null ) {
        ArrayList<String> ex = new ArrayList<String>();
        for( String fullXmlName : xmomObjects.get(type) ) {
          File location = new File(GenerationBase.getFileLocationOfXmlNameForSaving(fullXmlName, revision)+".xml");
          if( location.exists() ) {
            ex.add(fullXmlName);
          }
        }

        if( !ex.isEmpty() ) {
          existing.put(type, ex);
        }
      }
    }
    return existing;
  }
  
  
  /**
   * Kopiert aus der übergebenen Liste alle WORKFLOWs, EXCEPTIONs und DATATYPEs aus einer Revision in die andere und deploy anschließend
   * @param xmomObjects
   * @param fromRevision
   * @param toRevision
   * @throws Ex_FileAccessException 
   * @throws XPRC_InvalidPackageNameException 
   * @throws XPRC_DeploymentDuringUndeploymentException 
   * @throws MDMParallelDeploymentException 
   */
  public void copyToRevisionAndDeploy( Map<XMOMType,List<String>> xmomObjects, Long fromRevision, Long toRevision,
                                       DeploymentMode deploymentMode, WorkflowProtectionMode wpm, boolean inheritCodeChanged )
                                           throws Ex_FileAccessException, XPRC_InvalidPackageNameException, MDMParallelDeploymentException, XPRC_DeploymentDuringUndeploymentException {
    copyToRevisionAndDeploy(xmomObjects, fromRevision, toRevision, deploymentMode, wpm, inheritCodeChanged, null, null, null);
  }
  
  
  public void copyToRevisionAndDeploy( Map<XMOMType,List<String>> xmomObjects, Long fromRevision, Long toRevision,
                                       DeploymentMode deploymentMode, WorkflowProtectionMode wpm, boolean inheritCodeChanged,
                                       String username, String sessionId, String comment)
                                           throws Ex_FileAccessException, XPRC_InvalidPackageNameException, MDMParallelDeploymentException, XPRC_DeploymentDuringUndeploymentException {
    List<GenerationBase> doms = new ArrayList<GenerationBase>();
    for( XMOMType type : Arrays.asList(XMOMType.WORKFLOW,XMOMType.EXCEPTION, XMOMType.DATATYPE) ) {
      BatchRepositoryEvent repositoryEvent = new BatchRepositoryEvent(toRevision);
      try {
        if (xmomObjects.get(type) != null) {
          for (String fullXmlName : xmomObjects.get(type)) {
            File from = new File(GenerationBase.getFileLocationOfXmlNameForSaving(fullXmlName, fromRevision) + ".xml");
            if (username != null && sessionId != null) {
              try {
                XynaFactory.getInstance().getXynaMultiChannelPortalSecurityLayer()
                    .saveMDM(FileUtils.readFileAsString(from), true, username, sessionId, toRevision, repositoryEvent);
              } catch (XynaException e) {
                if (e instanceof Ex_FileAccessException) {
                  throw (Ex_FileAccessException) e;
                } else if (e instanceof XPRC_InvalidPackageNameException) {
                  throw (XPRC_InvalidPackageNameException) e;
                } else if (e instanceof MDMParallelDeploymentException) {
                  throw (MDMParallelDeploymentException) e;
                } else if (e instanceof XPRC_DeploymentDuringUndeploymentException) {
                  throw (XPRC_DeploymentDuringUndeploymentException) e;
                } else {
                  throw new RuntimeException(e);
                }
              }
            } else {
              File to = new File(GenerationBase.getFileLocationOfXmlNameForSaving(fullXmlName, toRevision) + ".xml");
              FileUtils.copyFile(from, to, true);
            }
            doms.add(DOM.getInstance(fullXmlName, toRevision));
          }
        }
      } finally {
        repositoryEvent.execute("Copied list of xmomobjects from revision " + fromRevision + ".");
      }
    }
    for (GenerationBase d : doms) {
      d.setDeploymentComment(comment);
    }
    GenerationBase.deploy(doms, deploymentMode, inheritCodeChanged, wpm);
  }
  
  
  public void deleteXMOMObjects( Map<XMOMType,List<String>> xmomObjects, boolean disableChecks,
                                 DependentObjectMode dependentObjectMode, boolean checkDeploymentLock, Long revision ) throws XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT {
    GenerationBase.undeployAndDelete(xmomObjects, disableChecks, dependentObjectMode, checkDeploymentLock, revision, new SingleRepositoryEvent(revision), true);
  }
  
  public void deleteXMOMObject(String fullXmlName, XMOMType type, boolean disableChecks,
                               DependentObjectMode dependentObjectMode, boolean checkDeploymentLock, Long revision)
        throws XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT {
    Map<XMOMType,List<String>> xmomObjects = new HashMap<XMOMDatabase.XMOMType, List<String>>();
    xmomObjects.put(type, Arrays.asList(fullXmlName));
    deleteXMOMObjects(xmomObjects, disableChecks, dependentObjectMode, checkDeploymentLock, revision);
  }

  @Deprecated
  public void deleteException(String fullXmlName, boolean disableChecks,
                              boolean recursivlyUndeployIfDeployedAndDependenciesExist, boolean deleteDependencies)
      throws XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT {
    deleteException(fullXmlName, disableChecks, recursivlyUndeployIfDeployedAndDependenciesExist, deleteDependencies, true, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
}

  public void deleteException(String fullXmlName, boolean disableChecks,
                              boolean recursivlyUndeployIfDeployedAndDependenciesExist, boolean deleteDependencies, boolean checkDeploymentLock,
                              Long revision)
      throws XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT {
    DependentObjectMode dependentObjectMode = recursivlyUndeployIfDeployedAndDependenciesExist ? DependentObjectMode.INVALIDATE : DependentObjectMode.PROTECT;
    deleteXMOMObject(fullXmlName, XMOMType.EXCEPTION, disableChecks, dependentObjectMode, checkDeploymentLock, revision);
  }


  public void deployWorkflow(String fqXmlName, WorkflowProtectionMode mode)
      throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException,
      XPRC_DeploymentDuringUndeploymentException, XPRC_InheritedConcurrentDeploymentException,
      XPRC_MDMDeploymentException {
    WF wf = WF.getInstance(fqXmlName);
    wf.setDeploymentComment("Explicit deployment of " + fqXmlName);
    wf.deploy(DeploymentMode.codeChanged, mode);
  }


  public void deployWorkflowAndDependants(String fqXmlName) throws XPRC_DeploymentDuringUndeploymentException,
      XPRC_InheritedConcurrentDeploymentException, XPRC_MDMDeploymentException,
      XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException {
    WF wf = WF.getInstance(fqXmlName);
    wf.setDeploymentComment("Explicit deployment of " + fqXmlName + " and dependencies.");
    wf.deploy(DeploymentMode.codeChanged, true, WorkflowProtectionMode.FORCE_DEPLOYMENT);
  }


  public int getNumberOfRunningProcesses() {
    return executionProcessor.getNumberOfRunningProcesses();
  }


  public void deployException(String fqXmlName, WorkflowProtectionMode mode)
      throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException,
      XPRC_DeploymentDuringUndeploymentException, XPRC_InheritedConcurrentDeploymentException,
      XPRC_MDMDeploymentException {
    ExceptionGeneration exception = ExceptionGeneration.getInstance(fqXmlName);
    exception.setDeploymentComment("Explicit deployment of " + fqXmlName);
    exception.deploy(DeploymentMode.codeChanged, mode);
  }
  
  public void deployExceptionAndDependants(String fqXmlName) throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH,
  XPRC_InvalidPackageNameException, XPRC_DeploymentDuringUndeploymentException,
  XPRC_InheritedConcurrentDeploymentException, XPRC_MDMDeploymentException {
    ExceptionGeneration exception = ExceptionGeneration.getInstance(fqXmlName);
    exception.setDeploymentComment("Explicit deployment of " + fqXmlName + " and dependencies."); 
    exception.deploy(DeploymentMode.codeChanged, true, WorkflowProtectionMode.FORCE_DEPLOYMENT);
  }


  public void undeployException(String fqXmlName, boolean disableChecks)
      throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException,
      XPRC_InternalObjectMayNotBeUndeployedException, XPRC_MDMUndeploymentException, XPRC_ExclusiveDeploymentInProgress, XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT {
    undeployException(fqXmlName, false, disableChecks);
  }


  public void undeployException(String fqXmlName, boolean undeployDependentObjects, boolean disableChecks)
      throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException,
      XPRC_InternalObjectMayNotBeUndeployedException, XPRC_MDMUndeploymentException, XPRC_ExclusiveDeploymentInProgress, XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT {
    ExceptionGeneration exception = ExceptionGeneration.getInstance(fqXmlName);
    DependentObjectMode dependentObjectMode = undeployDependentObjects ? DependentObjectMode.INVALIDATE : DependentObjectMode.PROTECT;
    exception.undeploy(dependentObjectMode, disableChecks);
  }


  public OrderContext setOrderContext(OrderContextServerExtension ctx) {
    if (ctx == null) {
      // @FIXME vllt. lieber doch ne RuntimeException werfen ...
      return null;
    }
    return threadToOrderContextMapping.put(Thread.currentThread(), ctx);
  }


  public OrderContext removeOrderContext() {
    return threadToOrderContextMapping.remove(Thread.currentThread());
  }


  public OrderContext getOrderContext() {
    OrderContext ctx = threadToOrderContextMapping.get(Thread.currentThread());
    if (ctx == null) {
      throw new IllegalStateException("Could not find order context, this is probably due to configuration problems."
          + " See the property 'xyna.global.set.ordercontext' for a global "
          + "configuration and/or 'help' for a more specific configuration on a per ordertype level.");
    }
    return ctx;
  }


  // TODO this should be somewhere within the workflow engine, refactor that later
  private class XynaDispatcherDeploymentHandler implements DeploymentHandler {

    public void exec(GenerationBase object, DeploymentMode mode) {

      if (!(object instanceof WF)) {
        return;
      }

      WF wf = (WF) object;
      List<Step> detachedSteps = wf.getAllDetachedSteps();
      if (detachedSteps.size() > 0) {
        RuntimeContextDependencyManagement rcdm =
            XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
        for (Step s : detachedSteps) {
          if (s instanceof StepFunction) {
            StepFunction stepAsStepFunction = (StepFunction) s;
            Service service;
            try {
              service = stepAsStepFunction.getParentScope().identifyService(stepAsStepFunction.getServiceId()).service;
            } catch (XPRC_InvalidServiceIdException e) {
              //wurde bereits beim parsen überprüft.
              throw new RuntimeException(e);
            }
            if (service.isDOMRef()) {
              registerServiceDestination(wf.getOriginalFqName() + "." + service.getServiceName() + "."
                  + stepAsStepFunction.getOperationName(), service.getDom().getRevision(), service.getDom().getOriginalFqName(),
                                         service.getServiceName(), stepAsStepFunction.getOperationName(),
                                         service.getDom().getFqClassName());

            } else if (stepAsStepFunction.isRemoteCall()) {
              Long revisionOfRemoteCallService = rcdm.getRevisionDefiningXMOMObject(RemoteCall.FQ_XML_NAME, object.getRevision());
              if (revisionOfRemoteCallService != null) {
                registerServiceDestination(RemoteCall.FQ_CLASS_NAME + ".RemoteCall.initiateRemoteCallForDetachedCalls",
                                           revisionOfRemoteCallService, RemoteCall.FQ_XML_NAME, "RemoteCall",
                                           "initiateRemoteCallForDetachedCalls", RemoteCall.class.getName());
              } else {
                throw new RuntimeException("RemoteCall Object not found");
              }
            }
          } else {
            throw new RuntimeException(new XDEV_UNSUPPORTED_FEATURE("Detached execution of elements of type "
                + s.getClass()));
          }
        }
      }

    }


    private void registerServiceDestination(String orderType, Long revisionOfDestination, String serviceFqXmlName, String serviceName,
                                            String operationName, String serviceFqClassName) {


      RuntimeContext runtimeContext;
      try {
        runtimeContext = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
            .getRuntimeContext(revisionOfDestination);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
        logger.warn("Could not find workspace name or application name and version name for revision " + revisionOfDestination, e1);
        return;
      }

      DestinationKey destinationKey = new DestinationKey(orderType, runtimeContext);

      ServiceDestination serviceDestination;
      try {
        serviceDestination =
            new ServiceDestination(serviceFqXmlName, serviceName, operationName, serviceFqClassName);
      } catch (XFMG_MDMObjectClassLoaderNotFoundException e) {
        throw new RuntimeException(e); //classloader wurde von deploymenthandler vorher erstellt
      } catch (XPRC_OperationNotFoundInDatatypeException e) {
        throw new RuntimeException(e); //dann hätte es vorher schon probleme beim parsen geben müssen
      }

      XynaProcessCtrlExecution xpce = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution();
      xpce.getXynaPlanning().getPlanningDispatcher().setDestination(destinationKey, XynaDispatcher.DESTINATION_EMPTY_PLANNING, false);
      xpce.getXynaExecution().getExecutionEngineDispatcher().setDestination(destinationKey, serviceDestination, true);
      xpce.getXynaCleanup().getCleanupEngineDispatcher().setDestination(destinationKey, XynaDispatcher.DESTINATION_EMPTY_WORKFLOW, false);
    }

    public void finish(boolean success) throws XPRC_DeploymentHandlerException {
    }


    @Override
    public void begin() throws XPRC_DeploymentHandlerException {
    }

  }


  private class XynaDispatcherUndeploymentHandler implements UndeploymentHandler {

    public void exec(GenerationBase object) {

      if (!(object instanceof WF)) {
        return;
      }

      
      WF wf = (WF) object;
      List<Step> detachedSteps = wf.getAllDetachedSteps();
      if (detachedSteps.size() > 0) {
        for (Step s : detachedSteps) {
          if (s instanceof StepFunction) {
            StepFunction stepAsStepFunction = (StepFunction) s;
            Service service;
            try {
              service = stepAsStepFunction.getParentScope().identifyService(stepAsStepFunction.getServiceId()).service;
            } catch (XPRC_InvalidServiceIdException e) {
              //wurde bereits beim parsen überprüft.
              throw new RuntimeException(e);
            }
            if (service.isDOMRef()) {
              unregisterServiceDestination(wf.getOriginalFqName() + "." + service.getServiceName() + "."
                      + stepAsStepFunction.getOperationName(), object.getRevision());

            } else if (stepAsStepFunction.isRemoteCall()) {
              //nicht deregistrieren, weil es noch andere detached remotecalls geben könnte
              //unregisterServiceDestination(RemoteCall.class.getName() + ".RemoteCall.initiateRemoteCallForDetachedCalls", object.getRevision());
            }
          } else {
            throw new RuntimeException(new XDEV_UNSUPPORTED_FEATURE("Detached execution of elements of type "
                + s.getClass()));
          }
        }
      }

    }


    private void unregisterServiceDestination(String orderType, Long revision) {

      RuntimeContext runtimeContext;
      try {
        runtimeContext =
            XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRuntimeContext(revision);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
        logger.warn("Could not find application name and version name for revision " + revision, e1);
        return;
      }
      DestinationKey destinationKey = new DestinationKey(orderType, runtimeContext);

      OrdertypeParameter ordertypeParameter = new OrdertypeParameter();
      ordertypeParameter.setOrdertypeName(destinationKey.getOrderType());
      ordertypeParameter.setRuntimeContext(runtimeContext);

      XynaProcessCtrlExecution xpce = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution();
      xpce.getXynaPlanning().getPlanningDispatcher().removeDestination(destinationKey);
      xpce.getXynaExecution().getExecutionEngineDispatcher().removeDestination(destinationKey);
      xpce.getXynaCleanup().getCleanupEngineDispatcher().removeDestination(destinationKey);
    }

    public void exec(FilterInstanceStorable object) {
    }

    public void exec(TriggerInstanceStorable object) {
    }

    public void exec(Capacity object) {
    }

    public void exec(DestinationKey object) {
    }

    public void finish() throws XPRC_UnDeploymentHandlerException {
    }

    public boolean executeForReservedServerObjects(){
      return false;
    }

    public void exec(FilterStorable object) {
    }

    public void exec(TriggerStorable object) {
    }
  }


  //should include every order currently backuped or in a phase where a shutdown would back it up
  public boolean checkForActiveOrders() throws XynaException {
    try {
      XynaFactory.getInstance().getProcessing().getXynaScheduler().pauseScheduling(true);
      XynaFactory.getInstance().getProcessing().getFrequencyControl().pauseAllFrequencyControlledTasks();

      final AtomicLong orderCounter = new AtomicLong(0);

      CronLikeOrder.setAlgorithm(new CronLikeOrderStartUnderlyingOrderAlgorithm() {
        public void startUnderlyingOrder(CronLikeOrder cronLikeOrder, CronLikeOrderCreationParameter xo,
                                         ResponseListener rl) {
          orderCounter.incrementAndGet();
          DefaultCronLikeOrderStartUnderlyingOrderAlgorithm.singleInstance.startUnderlyingOrder(cronLikeOrder, xo, rl);
        }
      });

      AFractalWorkflowProcessor.setAlgorithm(new AFractalWorkflowProcessorProcessingCheckAlgorithm() {
        public void checkOrderReadyForProcessing(XynaOrderServerExtension xo, DispatcherType type) throws XynaException {
          orderCounter.incrementAndGet();
        }
      });

      int cntBackup;
      ODS ods = ODSImpl.getInstance();
      ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
      try {
        PreparedQuery<Integer> pq =
            queryCache.getQueryFromCache("select count(*) from " + OrderInstanceBackup.TABLE_NAME, con,
                                         new ResultSetReader<Integer>() {

                                           public Integer read(ResultSet rs) throws SQLException {
                                             return rs.getInt(1);
                                           }

                                         });
        cntBackup = con.queryOneRow(pq, null);
      } finally {
        con.closeConnection();
      }

      if (cntBackup > 0) {
        logger.debug("Not stopping orders counted orders in backup");
        return true;
      }

      int waitingOrders = XynaFactory.getInstance().getProcessing().getXynaScheduler().getAllOrdersList().size();
      if (waitingOrders > 0) {
        logger.debug("Not stopping orders waiting inside scheduler");
        return true;
      }

      if (executionProcessor.getNumberOfRunningProcesses() > 0
          || XynaFactory.getInstance().getProcessing().getWorkflowEngine().getCleanupProcessor()
              .getNumberOfRunningProcesses() > 0
          || XynaFactory.getInstance().getProcessing().getWorkflowEngine().getPlanningProcessor()
              .getNumberOfRunningProcesses() > 0) {
        logger.debug("Not stopping orders inside a processor");
        return true;
      }

      if (orderCounter.get() > 0) {
        logger.debug("Not stopping order passed cronlikescheduler or fractalworkflowprocessor");
        return true;
      }

      return false;
    } finally {
      CronLikeOrder.setAlgorithm(DefaultCronLikeOrderStartUnderlyingOrderAlgorithm.singleInstance);
      AFractalWorkflowProcessor.setAlgorithm(new DefaultAFractalWorkflowProcessorProcessingCheckAlgorithm());
      XynaFactory.getInstance().getProcessing().getXynaScheduler().resumeScheduling();
      XynaFactory.getInstance().getProcessing().getFrequencyControl().resumeAllFrequencyControlledTasks();
    }
  }


  public ArrayList<String> getWatchedProperties() {
    ArrayList<String> result = new ArrayList<String>();
    result.add(XynaProperty.XYNA_STEP_LOG_HANDLERS_LOGLEVEL);
    return result;
  }


  public void propertyChanged() {
    String propertyValue =
        XynaFactory.getInstance().getFactoryManagement().getProperty(XynaProperty.XYNA_STEP_LOG_HANDLERS_LOGLEVEL);
    if (propertyValue == null) {
      this.stepHandlerLogPriority = Level.toLevel(Level.DEBUG_INT);
    } else {
      this.stepHandlerLogPriority = Level.toLevel(propertyValue);
    }
    stepHandlerLogging = logger.isEnabledFor(stepHandlerLogPriority);
    if (logger.isInfoEnabled()) {
      logger.info("Step loghandler log level set to <" + stepHandlerLogPriority + ">");
    }
  }




}
