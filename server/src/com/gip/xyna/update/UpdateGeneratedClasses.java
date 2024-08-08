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

package com.gip.xyna.update;



import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.XynaFactoryComponent;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.utils.concurrent.FutureCollection;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.XynaActivationBase;
import com.gip.xyna.xact.XynaActivationPortal;
import com.gip.xyna.xact.exceptions.XACT_JarFileUnzipProblem;
import com.gip.xyna.xdev.XynaDevelopmentBase;
import com.gip.xyna.xdev.XynaDevelopmentPortal;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xlibdev.codeaccess.CodeAccessManagement;
import com.gip.xyna.xfmg.XynaFactoryManagement;
import com.gip.xyna.xfmg.XynaFactoryManagementBase;
import com.gip.xyna.xfmg.XynaFactoryManagementPortal;
import com.gip.xyna.xfmg.exceptions.XFMG_DomainDoesNotExistException;
import com.gip.xyna.xfmg.exceptions.XFMG_DomainIsAssignedException;
import com.gip.xyna.xfmg.exceptions.XFMG_ErrorScanningLogFile;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidCapacityCardinality;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidCreationOfExistingOrdertype;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidModificationOfUnexistingOrdertype;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidXynaOrderPriority;
import com.gip.xyna.xfmg.exceptions.XFMG_JarFolderNotFoundException;
import com.gip.xyna.xfmg.exceptions.XFMG_NameContainsInvalidCharacter;
import com.gip.xyna.xfmg.exceptions.XFMG_PasswordRestrictionViolation;
import com.gip.xyna.xfmg.exceptions.XFMG_PredefinedXynaObjectException;
import com.gip.xyna.xfmg.exceptions.XFMG_RightDoesNotExistException;
import com.gip.xyna.xfmg.exceptions.XFMG_RoleDoesNotExistException;
import com.gip.xyna.xfmg.exceptions.XFMG_RoleIsAssignedException;
import com.gip.xyna.xfmg.exceptions.XFMG_SESSION_AUTHENTICATION_FAILED;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownSessionIDException;
import com.gip.xyna.xfmg.exceptions.XFMG_UserDoesNotExistException;
import com.gip.xyna.xfmg.extendedstatus.XynaExtendedStatusManagement;
import com.gip.xyna.xfmg.statistics.XynaStatistics;
import com.gip.xyna.xfmg.statistics.XynaStatisticsLegacy;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagement;
import com.gip.xyna.xfmg.xfctrl.RMIManagement;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable.ApplicationEntryType;
import com.gip.xyna.xfmg.xfctrl.classloading.AutomaticUnDeploymentHandlerManager;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderDispatcher;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderType;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedObject;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelManagement;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.Queue;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.QueueConnectData;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.QueueManagement;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.QueueType;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xfmon.XynaFactoryMonitoring;
import com.gip.xyna.xfmg.xfmon.processmonitoring.ProcessMonitoring;
import com.gip.xyna.xfmg.xods.XynaFactoryManagementODS;
import com.gip.xyna.xfmg.xods.components.Components;
import com.gip.xyna.xfmg.xods.configuration.Configuration;
import com.gip.xyna.xfmg.xods.configuration.PropertyMap;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyWithDefaultValue;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeManagement;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeParameter;
import com.gip.xyna.xfmg.xods.ordertypemanagement.SearchOrdertypeParameter;
import com.gip.xyna.xfmg.xods.priority.PrioritySetting;
import com.gip.xyna.xfmg.xopctrl.DomainTypeSpecificData;
import com.gip.xyna.xfmg.xopctrl.XynaOperatorControl;
import com.gip.xyna.xfmg.xopctrl.managedsessions.ASessionPrivilege;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionCredentials;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionDetails;
import com.gip.xyna.xfmg.xopctrl.managedsessions.notification.ANotificationConnection;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Domain;
import com.gip.xyna.xfmg.xopctrl.usermanagement.DomainType;
import com.gip.xyna.xfmg.xopctrl.usermanagement.PasswordExpiration;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Right;
import com.gip.xyna.xfmg.xopctrl.usermanagement.RightScope;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.User;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.PredefinedCategories;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaUserCredentials;
import com.gip.xyna.xfmg.xopctrl.usermanagement.selectuser.UserSearchResult;
import com.gip.xyna.xfmg.xopctrl.usermanagement.selectuser.UserSelect;
import com.gip.xyna.xmcp.Channel;
import com.gip.xyna.xmcp.OrderExecutionResponse;
import com.gip.xyna.xmcp.ResultController;
import com.gip.xyna.xmcp.XynaMultiChannelPortalBase;
import com.gip.xyna.xmcp.XynaMultiChannelPortalSecurityLayer;
import com.gip.xyna.xnwh.XynaFactoryWarehouseBase;
import com.gip.xyna.xnwh.XynaFactoryWarehousePortal;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_PERSISTENCE_LAYER_CLASS_NOT_FOUND;
import com.gip.xyna.xnwh.exceptions.XNWH_PersistenceLayerMayNotBeUndeployedInUseException;
import com.gip.xyna.xnwh.exceptions.XNWH_PersistenceLayerNotRegisteredException;
import com.gip.xyna.xnwh.persistence.FactoryWarehouseCursor;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.xmom.XMOMPersistenceBase;
import com.gip.xyna.xnwh.persistence.xmom.XMOMPersistenceManagement;
import com.gip.xyna.xnwh.pools.ConnectionPoolManagement;
import com.gip.xyna.xnwh.securestorage.SecureStorage;
import com.gip.xyna.xnwh.statistics.StatisticsStore;
import com.gip.xyna.xnwh.xclusteringservices.XynaClusteringServices;
import com.gip.xyna.xnwh.xwarehousejobs.XynaWarehouseJobManagement;
import com.gip.xyna.xprc.CronLikeOrderCreationParameter;
import com.gip.xyna.xprc.OrderStatus;
import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaProcessing.DispatcherEntry;
import com.gip.xyna.xprc.XynaProcessingBase;
import com.gip.xyna.xprc.XynaProcessingPortal;
import com.gip.xyna.xprc.exceptions.XPRC_AdministrativeVetoAllocationDenied;
import com.gip.xyna.xprc.exceptions.XPRC_AdministrativeVetoDeallocationDenied;
import com.gip.xyna.xprc.exceptions.XPRC_CAPACITY_ALREADY_DEFINED;
import com.gip.xyna.xprc.exceptions.XPRC_CancelFailedException;
import com.gip.xyna.xprc.exceptions.XPRC_CronLikeOrderStorageException;
import com.gip.xyna.xprc.exceptions.XPRC_CronLikeSchedulerException;
import com.gip.xyna.xprc.exceptions.XPRC_CronRemovalException;
import com.gip.xyna.xprc.exceptions.XPRC_DESTINATION_NOT_FOUND;
import com.gip.xyna.xprc.exceptions.XPRC_DeploymentDuringUndeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT;
import com.gip.xyna.xprc.exceptions.XPRC_ExclusiveDeploymentInProgress;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InternalObjectMayNotBeUndeployedException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidCronLikeOrderParametersException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMUndeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH;
import com.gip.xyna.xprc.exceptions.XPRC_UNSUPPORTED_FILE_EXTENSION_DEPLOYMENT;
import com.gip.xyna.xprc.remotecallserialization.XynaXmomSerialization;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcessManagement;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcessManagement.CancelMode;
import com.gip.xyna.xprc.xbatchmgmt.beans.BatchProcessInformation;
import com.gip.xyna.xprc.xbatchmgmt.beans.BatchProcessInput;
import com.gip.xyna.xprc.xbatchmgmt.selectbatch.BatchProcessSearchResult;
import com.gip.xyna.xprc.xbatchmgmt.selectbatch.BatchProcessSelectImpl;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTask;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTaskCreationParameter;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTaskInformation;
import com.gip.xyna.xprc.xfqctrl.XynaFrequencyControl;
import com.gip.xyna.xprc.xfractwfe.XynaPythonSnippetManagement;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DependentObjectMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.MDMParallelDeploymentException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.WorkflowProtectionMode;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xfractwfe.specialpurpose.SpecialPurposeHelper;
import com.gip.xyna.xprc.xpce.EngineSpecificWorkflowProcessor;
import com.gip.xyna.xprc.xpce.OrderContext;
import com.gip.xyna.xprc.xpce.OrderContextServerExtension;
import com.gip.xyna.xprc.xpce.WorkflowEngine;
import com.gip.xyna.xprc.xpce.XynaProcessCtrlExecution;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xpce.monitoring.EngineSpecificStepHandlerManager;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xprcods.XynaProcessingODS;
import com.gip.xyna.xprc.xprcods.capacitymapping.CapacityMappingStorable;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstance;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceDetails;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase.DeploymentStatus;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowInformation;
import com.gip.xyna.xprc.xsched.CapacityInformation;
import com.gip.xyna.xprc.xsched.CapacityManagement.State;
import com.gip.xyna.xprc.xsched.ExtendedCapacityUsageInformation;
import com.gip.xyna.xprc.xsched.VetoInformationStorable;
import com.gip.xyna.xprc.xsched.XynaScheduler;
import com.gip.xyna.xprc.xsched.XynaThreadFactory;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder.OnErrorAction;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrderInformation;
import com.gip.xyna.xprc.xsched.orderabortion.AbortionCause;
import com.gip.xyna.xprc.xsched.ordercancel.CancelBean;
import com.gip.xyna.xprc.xsched.ordercancel.KillStuckProcessBean;
import com.gip.xyna.xprc.xsched.orderseries.OrderSeriesManagementInformation;
import com.gip.xyna.xprc.xsched.orderseries.OrderSeriesManagementInformation.Mode;
import com.gip.xyna.xprc.xsched.orderseries.RescheduleSeriesOrderInformation;
import com.gip.xyna.xprc.xsched.scheduling.SchedulerInformationBean;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSearchResult;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSelectImpl;



/**
 * generiert alle generierten klassen neu und kompiliert sie. deploymenthandler werden nicht aufgerufen. dateien werden
 * nicht aus dem saved-verzeichnis kopiert, sodnern aus dem deployedverzeichnis übernommen.
 */
public class UpdateGeneratedClasses {

  public static Logger logger = CentralFactoryLogging.getLogger(UpdateGeneratedClasses.class);

  /**
   * dieses update muss nicht mehrfach bei einem serverstart ausgeführt werden.
   */
  private static boolean hasBeenExecuted = false;


  public UpdateGeneratedClasses() {
  }

  private abstract static class DeploymentTask implements Callable<Void> {
    public abstract String getRevisionInfo();
    
    public abstract ObjectsToDeploy getDeploy();
    
    public abstract List<GenerationBase> getObjects() throws XPRC_InvalidPackageNameException ;
  

    public Void call() throws Exception {
      try {

        List<GenerationBase> objects = getObjects();

        for (GenerationBase gb : objects) {
          gb.setDeploymentComment("Update Generated Classes");
        }
        GenerationBase.deploy(objects, DeploymentMode.regenerateDeployed, false, WorkflowProtectionMode.FORCE_KILL_DEPLOYMENT);
      } catch (MDMParallelDeploymentException ex) {
        logger.error("Could not regenerate " + ex.getNumberOfFailedObjects() + " xmomobjects, continuing ...");
        for (GenerationBase object : ex.getFailedObjects()) {
          logger.error("Could not load xmomobject " + object.getOriginalFqName() + " from revision " + object.getRevision(),
                       object.getExceptionCause());
          if (object.getExceptionWhileOnError() != null) {
            logger.error("Errors occurred during cleanup of xmomobject " + object.getOriginalFqName(), object.getExceptionWhileOnError());
          }
        }
      } catch (Throwable t) {
        Department.handleThrowable(t);
        logger.error("error regenerating code in revision  " + getRevisionInfo(), t);
      }
      return null;
    }
  }

  private static class MultiRevisionDeploymentTask extends DeploymentTask {

    private final List<DeploymentTask> subTasks;
    private final ObjectsToDeploy objectsToDeploy;


    public MultiRevisionDeploymentTask() {
      subTasks = new ArrayList<DeploymentTask>();
      objectsToDeploy = new ObjectsToDeploy();
    }


    public static MultiRevisionDeploymentTask combine(DeploymentTask dt1, DeploymentTask dt2) {
      MultiRevisionDeploymentTask result = new MultiRevisionDeploymentTask();
      result.subTasks.add(dt1);
      result.subTasks.add(dt2);

      ObjectsToDeploy toDeploy = result.getDeploy();
      ObjectsToDeploy otd1 = dt1.getDeploy();
      ObjectsToDeploy otd2 = dt2.getDeploy();
      toDeploy.addWfs(otd1.wfs);
      toDeploy.addWfs(otd2.wfs);
      toDeploy.addDataTypes(otd1.datatypes);
      toDeploy.addDataTypes(otd2.datatypes);
      toDeploy.addExceptions(otd1.exceptions);
      toDeploy.addExceptions(otd2.exceptions);

      return result;
    }


    @Override
    public String getRevisionInfo() {
      return "(" + String.join(", ", subTasks.stream().map(x -> x.getRevisionInfo()).collect(Collectors.toList())) + ")";
    }


    @Override
    public ObjectsToDeploy getDeploy() {
      return objectsToDeploy;
    }


    @Override
    public List<GenerationBase> getObjects() throws XPRC_InvalidPackageNameException {
      List<GenerationBase> result = new ArrayList<GenerationBase>();
      for (DeploymentTask subTask : subTasks) {
        result.addAll(subTask.getObjects());
      }

      return result;
    }

  }

  private static class SingleRevisionDeploymentTask extends DeploymentTask {

    private final ObjectsToDeploy deploy;
    private final Long revision;


    public SingleRevisionDeploymentTask(Long revision, ObjectsToDeploy deploy) {
      this.revision = revision;
      this.deploy = deploy;
    }


    @Override
    public String getRevisionInfo() {
      return "rev_" + revision;
    }


    @Override
    public ObjectsToDeploy getDeploy() {
      return deploy;
    }


    @Override
    public List<GenerationBase> getObjects() throws XPRC_InvalidPackageNameException {
      List<GenerationBase> objects = new ArrayList<GenerationBase>();
      if (deploy.datatypes != null) {
        CodeAccessManagement.updateRegeneratedClasses = true;
        for (String datatype : deploy.datatypes) {
          objects.add(DOM.getInstance(datatype, revision));
        }
      }
      if (deploy.wfs != null) {
        for (String wf : deploy.wfs) {
          objects.add(WF.getInstance(wf, revision));
        }
      }
      if (deploy.exceptions != null) {
        CodeAccessManagement.updateRegeneratedClasses = true;
        for (String exception : deploy.exceptions) {
          objects.add(ExceptionGeneration.getInstance(exception, revision));
        }
      }
      return objects;
    }


  }


  public static void regenerateWorkflowDatabase() throws XynaException {
    WorkflowDatabase wfdb = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getWorkflowDatabase();

    FutureCollection<Void> deployments = new FutureCollection<Void>();
    regenerate(wfdb, deployments);

    try {
      deployments.get(); //wait till all are finished
    } catch (InterruptedException e) {
      throw new XynaException("interrupted");
    } catch (ExecutionException e) {
      Department.handleThrowable(e);
      logger.error("error regenerating code of deployed.", e.getCause());
    }
  }


  private static void regenerate(WorkflowDatabase wfdb, FutureCollection<Void> deployments) throws XynaException {
    int numberOfThreads = Runtime.getRuntime().availableProcessors() + 2;

    ThreadPoolExecutor threadpool = new ThreadPoolExecutor(numberOfThreads, numberOfThreads, 10, TimeUnit.SECONDS,
                                                           new LinkedBlockingQueue<Runnable>(), new XynaThreadFactory(3));
    try {
      regenerateWorkflowDatabase(wfdb, deployments, threadpool);

      deployments.get(); //wait till all are finished
    } catch (InterruptedException e) {
      //wird oben drüber behandelt
    } catch (ExecutionException e) {
      //wird oben drüber behandelt
    } finally {
      threadpool.shutdown();
    }

  }


  private static class ObjectsToDeploy {

    private volatile List<String> wfs;
    private volatile List<String> exceptions;
    private volatile List<String> datatypes;


    public void setWfs(List<String> value) {
      this.wfs = value;
    }


    public void addWfs(List<String> value) {
      if (this.wfs == null) {
        this.wfs = value == null ? null : new ArrayList<String>(value);
      } else if (value != null) {
        wfs.addAll(value);
      }
    }


    public void setExceptions(List<String> value) {
      this.exceptions = value;
    }


    public void addExceptions(List<String> value) {
      if (this.exceptions == null) {
        this.exceptions = value == null ? null : new ArrayList<String>(value);
      } else if (value != null) {
        exceptions.addAll(value);
      }
    }


    public void setDataTypes(List<String> value) {
      this.datatypes = value;
    }


    public void addDataTypes(List<String> value) {
      if (this.datatypes == null) {
        this.datatypes = value == null ? null : new ArrayList<String>(value);
      } else if (value != null) {
        datatypes.addAll(value);
      }
    }


    public int getSize() {
      int size = 0;

      size += wfs != null ? wfs.size() : 0;
      size += exceptions != null ? exceptions.size() : 0;
      size += datatypes != null ? datatypes.size() : 0;

      return size;
    }

  }


  private static void regenerateWorkflowDatabase(WorkflowDatabase wfdb, FutureCollection<Void> deployments, ThreadPoolExecutor threadpool)
      throws XynaException {
    Map<Long, ObjectsToDeploy> map = new HashMap<Long, ObjectsToDeploy>();
    for (final Entry<Long, List<String>> entry : wfdb.getDeployedWfs().entrySet()) {
      ObjectsToDeploy o = map.get(entry.getKey());
      if (o == null) {
        o = new ObjectsToDeploy();
        map.put(entry.getKey(), o);
      }
      o.setWfs(entry.getValue());
    }

    for (Entry<Long, List<String>> entry : wfdb.getDeployedExceptions().entrySet()) {
      ObjectsToDeploy o = map.get(entry.getKey());
      if (o == null) {
        o = new ObjectsToDeploy();
        map.put(entry.getKey(), o);
      }
      o.setExceptions(entry.getValue());
    }


    for (Entry<Long, List<String>> entry : wfdb.getDeployedDatatypes().entrySet()) {
      ObjectsToDeploy o = map.get(entry.getKey());
      if (o == null) {
        o = new ObjectsToDeploy();
        map.put(entry.getKey(), o);
      }
      o.setDataTypes(entry.getValue());
    }


    List<SingleRevisionDeploymentTask> tasks = new ArrayList<SingleRevisionDeploymentTask>();
    for (Entry<Long, ObjectsToDeploy> entry : map.entrySet()) {
      SingleRevisionDeploymentTask task = new SingleRevisionDeploymentTask(entry.getKey(), entry.getValue());
      tasks.add(task);
    }

    List<List<SingleRevisionDeploymentTask>> sorted = sortTasks(tasks);
    FutureCollection<Void> coll = new FutureCollection<Void>();
    List<List<DeploymentTask>> tasksToDeploy = new ArrayList<List<DeploymentTask>>();
    boolean singleBatchDeploy = XynaProperty.WORKFLOW_DB_SINGLE_BATCH_DEPLOY.get();
    if (singleBatchDeploy) {
      tasksToDeploy = furtherCombineDeploymentTasks(sorted);
    } else {
      tasksToDeploy = copyTasks(sorted);
    }


    for (List<DeploymentTask> taskPoint : tasksToDeploy) {
      if (logger.isDebugEnabled()) {
        logger.debug("Starting deployment of " + String.join(", ", taskPoint.stream().map(x -> x.getRevisionInfo()).collect(Collectors.toList())));
      }
      //all DeploymentTasks in taskPoint can be done in parallel
      for (DeploymentTask t : taskPoint) {
        Future<Void> future = threadpool.submit(t);
        coll.add(future);
        deployments.add(future);
      }
      try {
        coll.get(); //synchronize
      } catch (InterruptedException | ExecutionException e) {
        logger.error("Exception during update of generated classes." + e);
      }
      if (logger.isDebugEnabled()) {
        logger.debug("Finished deployment of " + String.join(", ", taskPoint.stream().map(x -> x.getRevisionInfo()).collect(Collectors.toList())));
      }
    }
  }


  private static List<List<DeploymentTask>> furtherCombineDeploymentTasks(List<List<SingleRevisionDeploymentTask>> sorted) {
    List<List<DeploymentTask>> result = new ArrayList<List<DeploymentTask>>();
    for (List<SingleRevisionDeploymentTask> layer : sorted) {
      List<DeploymentTask> l = new ArrayList<DeploymentTask>(layer);
      combineDistinctTasks(l);
      result.add(l);
    }

    combineConsecutiveDeployments(result);

    logger.debug("combinedDeploymentTasks. " + result.size());

    return result;
  }


  //if two consecutive deploymentTaskLayers (Lists of DeploymentTask in sorted)
  //only contain a single DeploymentTask, they can be combined (if the DeploymentTasks can be merged)
  private static void combineConsecutiveDeployments(List<List<DeploymentTask>> deploymentLayers) {
    for (int i = deploymentLayers.size() - 1; i > 0; i--) { //merging down (i to i-1) -> don't check i==0
      List<DeploymentTask> candidate = deploymentLayers.get(i);
      if (candidate.size() != 1) {
        continue; //only combine if there is only one DeploymentTask in this layer
      }
      List<DeploymentTask> target = deploymentLayers.get(i - 1);
      DeploymentTask dt1 = candidate.get(0);
      if (canMergeIntoDeploymentTaskLayer(dt1, target)) {
        logger.debug("merging consecutive deployments");
        DeploymentTask mergeTarget = target.get(0);
        MultiRevisionDeploymentTask merged = MultiRevisionDeploymentTask.combine(dt1, mergeTarget);
        deploymentLayers.remove(i); //remove later List containing merged DeploymentTask
        target.remove(0); //remove mergeTarget from earlier List
        target.add(merged); //add merged DeploymentTask to earlier list
        logger.debug("merged consecutive deployments: " + merged.getRevisionInfo());
      }
    }
  }


  private static boolean canMergeIntoDeploymentTaskLayer(DeploymentTask dt1, List<DeploymentTask> target) {
    if (target.size() != 1) {
      return false;
    }
    DeploymentTask dt2 = target.get(0);
    return canMerge(dt1, dt2);
  }


  private static void combineDistinctTasks(List<DeploymentTask> tasklayer) {
    for (int i = tasklayer.size() - 1; i >= 0; i--) {
      DeploymentTask merge = tasklayer.get(i);
      int mergeIndex = findMergeIndex(tasklayer, merge);
      if (mergeIndex != -1) {
        DeploymentTask mergeTarget = tasklayer.get(mergeIndex);
        DeploymentTask merged = MultiRevisionDeploymentTask.combine(mergeTarget, merge);
        tasklayer.remove(i);
        tasklayer.remove(mergeIndex);
        tasklayer.add(mergeIndex, merged);
      }
    }
  }


  //find the lowest index in taskLayer without any name collisions with deploymentTask
  //return -1 if all DeploymentTasks in taskLayer have collisions
  private static int findMergeIndex(List<DeploymentTask> taskLayer, DeploymentTask deploymentTask) {
    for (int i = 0; i < taskLayer.size(); i++) {
      DeploymentTask cmp = taskLayer.get(i);
      if (canMerge(deploymentTask, cmp)) {
        return i;
      }
    }
    return -1;
  }


  private static boolean canMerge(DeploymentTask dt1, DeploymentTask dt2) {
    Set<String> combinedSet = new HashSet<String>();
    ObjectsToDeploy otd1 = dt1.getDeploy();
    ObjectsToDeploy otd2 = dt2.getDeploy();
    combinedSet.addAll(otd1.datatypes == null ? new HashSet<String>() : otd1.datatypes);
    combinedSet.addAll(otd1.wfs == null ? new HashSet<String>() : otd1.wfs);
    combinedSet.addAll(otd1.exceptions == null ? new HashSet<String>() : otd1.exceptions);
    combinedSet.addAll(otd2.datatypes == null ? new HashSet<String>() : otd2.datatypes);
    combinedSet.addAll(otd2.wfs == null ? new HashSet<String>() : otd2.wfs);
    combinedSet.addAll(otd2.exceptions == null ? new HashSet<String>() : otd2.exceptions);

    int sizeDT1 = otd1.getSize();
    int sizeDT2 = otd2.getSize();
    int combinedSize = sizeDT1 + sizeDT2;

    return combinedSet.size() == combinedSize;
  }


  private static List<List<DeploymentTask>> copyTasks(List<List<SingleRevisionDeploymentTask>> input) {
    List<List<DeploymentTask>> result = new ArrayList<List<DeploymentTask>>();

    for (List<SingleRevisionDeploymentTask> taskList : input) {
      result.add(new ArrayList<DeploymentTask>(taskList));
    }

    return result;
  }


  //all DeploymentTaskLists in the result can be deployed in parallel
  private static List<List<SingleRevisionDeploymentTask>> sortTasks(List<SingleRevisionDeploymentTask> tasks) {
    List<List<SingleRevisionDeploymentTask>> result = new ArrayList<List<SingleRevisionDeploymentTask>>();

    RuntimeContextDependencyManagement rtcDependencyManagement =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();

    List<SingleRevisionDeploymentTask> tasksToSort = new ArrayList<SingleRevisionDeploymentTask>(tasks);
    Set<Long> nonEmptyRtcs = new HashSet<Long>();

    for (SingleRevisionDeploymentTask task : tasksToSort) {
      nonEmptyRtcs.add(task.revision);
    }

    result.add(new ArrayList<SingleRevisionDeploymentTask>());
    boolean progress = true;
    while (!tasksToSort.isEmpty() && progress) {
      progress = false;
      for (int i = tasksToSort.size() - 1; i >= 0; i--) {
        SingleRevisionDeploymentTask task = tasksToSort.get(i);
        if (taskCanBeAdded(task, result, rtcDependencyManagement, tasksToSort, nonEmptyRtcs)) {
          tasksToSort.remove(i);
          result.get(result.size() - 1).add(task);
          progress = true;
        }
      }
      logger.debug("new sorting round");
      result.add(new ArrayList<SingleRevisionDeploymentTask>());
    }

    if (!tasksToSort.isEmpty()) {
      if (logger.isWarnEnabled()) {
        logger.warn("Could not sort all revisions. Remaining revisions will be updated one at a time. Affected revisions: "
            + String.join(", ", tasksToSort.stream().map(x -> x.revision + "").collect(Collectors.toList())));
      }
      for (SingleRevisionDeploymentTask t : tasksToSort) {
        List<SingleRevisionDeploymentTask> list = new ArrayList<SingleRevisionDeploymentTask>();
        list.add(t);
        result.add(list);
      }
    }

    return result;
  }
  

  //returns true if all dependencies of task are in previous Lists
  //does not consider the last list in others. (these Tasks are potentially executed in parallel with task)
  private static boolean taskCanBeAdded(SingleRevisionDeploymentTask task, List<List<SingleRevisionDeploymentTask>> added, RuntimeContextDependencyManagement mgmt,
                                        List<SingleRevisionDeploymentTask> toAdd, Set<Long> allNonEmptyRtcs) {
    Set<Long> deps = new HashSet<Long>(mgmt.getDependencies(task.revision));

    //-1 to ignore tasks in last element
    //remove all dependencies that are already deployed before us
    for (int i = 0; i < added.size() - 1; i++) {
      final int index = i;
      deps.removeIf(x -> added.get(index).stream().anyMatch(y -> y.revision.equals(x)));
    }


    //ignore empty RTCs (empty RTCs are neither in added nor toAdd)
    deps.removeIf(x -> !allNonEmptyRtcs.contains(x));

    if (!deps.isEmpty()) {
      logger.debug("can't add " + task.revision + ". Because we are missing: "
          + String.join(", ", deps.stream().map(x -> "" + x).collect(Collectors.toList())));
    } else {
      logger.debug("can add " + task.revision + " in round " + added.size());
    }

    return deps.isEmpty();
  }


  public void update() throws XynaException {
    if (hasBeenExecuted) {
      logger.debug("skipping update because it has already been executed.");
      return;
    }
    logger.debug("updating all deployed classes");
    WorkflowDatabase wfdb = WorkflowDatabase.getWorkflowDatabasePreInit();

    FutureCollection<Void> deployments = new FutureCollection<Void>();
    regenerate(wfdb, deployments);

    try {
      deployments.get(); //wait till all are finished
    } catch (InterruptedException e) {
      throw new XynaException("serverstart was interrupted");
    } catch (ExecutionException e) {
      logger.error("error regenerating code of deployed.", e.getCause());
      Department.handleThrowable(e);
    }
    hasBeenExecuted = true;

  }


  public static void repeatGeneration() {
    hasBeenExecuted = false;
  }


  // TODO move this into some kind of HelperClass 
  public static void mockFactory() throws XynaException {
    final Components components = new Components();
    XynaFactory.setInstance(new XynaFactoryBase() {

      public void addComponentToBeInitializedLater(XynaFactoryComponent lateInitComponent) throws XynaException {
      }


      public XynaActivationBase getActivation() {
        return null;
      }


      private volatile XynaFactoryManagementBase xfm;


      public XynaFactoryManagementBase getFactoryManagement() {
        if (xfm == null) {
          synchronized (this) {
            if (xfm == null) {
              try {
                xfm = new XynaFactoryManagementBase() {


                  public Components getComponents() {
                    return components;
                  }


                  private volatile XynaFactoryControl xfc;


                  public XynaFactoryControl getXynaFactoryControl() {
                    if (xfc == null) {
                      synchronized (this) {
                        if (xfc == null) {
                          try {
                            xfc = new XynaFactoryControl() {

                              @Override
                              public void init() throws XynaException {

                              }


                              private volatile VersionManagement versionManagement;


                              @Override
                              public VersionManagement getVersionManagement() {
                                if (versionManagement == null) {
                                  synchronized (this) {
                                    if (versionManagement == null) {
                                      try {
                                        versionManagement = new VersionManagement() {

                                          @Override
                                          public List<Long> getAllRevisions() {
                                            return new ArrayList<Long>();
                                          }
                                        };
                                      } catch (XynaException e) {
                                        throw new RuntimeException("problem building mocked server for update process", e);
                                      }
                                    }
                                  }
                                }
                                return versionManagement;
                              }


                              private volatile RevisionManagement revisionManagement;


                              @Override
                              public RevisionManagement getRevisionManagement() {
                                if (revisionManagement == null) {
                                  synchronized (this) {
                                    if (revisionManagement == null) {
                                      try {
                                        revisionManagement = RevisionManagement.getRevisionManagementPreInit();
                                      } catch (XynaException e) {
                                        throw new RuntimeException("problem building mocked server for update process", e);
                                      }
                                    }
                                  }
                                }
                                return revisionManagement;
                              }


                              @Override
                              public RMIManagement getRMIManagement() {
                                return null;
                              }


                              private volatile ClassLoaderDispatcher cld;


                              @Override
                              public ClassLoaderDispatcher getClassLoaderDispatcher() {
                                if (cld == null) {
                                  synchronized (this) {
                                    if (cld == null) {
                                      cld = new ClassLoaderDispatcher("startup") {

                                        @Override
                                        public ClassLoaderBase getClassLoaderByType(ClassLoaderType clt, String classLoaderName,
                                                                                    Long revision, Long parentRevision) {

                                          return UpdateMDMClassloader.getUpdateMDMClassLoaderIfClassIsLoadable(classLoaderName, revision,
                                                                                                               clt, parentRevision);

                                        }

                                      };
                                    }
                                  }
                                }
                                return cld;
                              }


                              public AutomaticUnDeploymentHandlerManager getAutomaticUnDeploymentHandlerManager() {
                                return null;
                              }


                              private volatile DependencyRegister dr;


                              public DependencyRegister getDependencyRegister() {
                                if (dr == null) {
                                  synchronized (this) {
                                    if (dr == null) {
                                      try {
                                        dr = new DependencyRegister("startup");
                                      } catch (XynaException e) {
                                        throw new RuntimeException("problem building mocked server for update process", e);
                                      }
                                    }
                                  }
                                }
                                return dr;
                              }


                              public XMOMDatabase getXMOMDatabase() {
                                return null;
                              }


                              public QueueManagement getQueueManagement() {
                                return null;
                              }


                              private volatile DataModelManagement dataModelManagement;


                              @Override
                              public DataModelManagement getDataModelManagement() {
                                if (dataModelManagement == null) {
                                  synchronized (this) {
                                    if (dataModelManagement == null) {
                                      try {
                                        dataModelManagement = DataModelManagement.getDataModelManagementPreInit();
                                      } catch (XynaException e) {
                                        throw new RuntimeException("problem building mocked server for update process", e);
                                      }
                                    }
                                  }
                                }
                                return dataModelManagement;
                              }


                              private volatile RuntimeContextDependencyManagement rcdm;


                              @Override
                              public RuntimeContextDependencyManagement getRuntimeContextDependencyManagement() {
                                if (rcdm == null) {
                                  synchronized (this) {
                                    if (rcdm == null) {
                                      try {
                                        rcdm = new RuntimeContextDependencyManagement("startup");
                                      } catch (XynaException e) {
                                        throw new RuntimeException("problem building mocked server for update process", e);
                                      }
                                    }
                                  }
                                }
                                return rcdm;
                              }

                            };
                          } catch (XynaException e) {
                            throw new RuntimeException("problem building mocked server for update process", e);
                          }
                        }
                      }
                    }
                    return xfc;
                  }


                  public ProcessMonitoring getProcessMonitoring() {
                    return null;
                  }


                  public PropertyMap<String, String> getPropertiesReadOnly() {
                    return null;
                  }


                  public String getProperty(String key) {
                    if (key.equals(XynaProperty.XYNA_DISABLE_XSD_VALIDATION)) {
                      return "true";
                    } else {
                      return getXynaFactoryManagementODS().getConfiguration().getProperty(key);
                    }
                  }


                  volatile XynaFactoryManagementODS xfmods;


                  public XynaFactoryManagementODS getXynaFactoryManagementODS() {
                    if (xfmods == null) {
                      synchronized (this) {
                        if (xfmods == null) {
                          try {
                            xfmods = new XynaFactoryManagementODS() {

                              @Override
                              public void init() throws XynaException {

                              }


                              volatile Configuration conf;


                              public Configuration getConfiguration() {
                                if (conf == null) {
                                  synchronized (this) {
                                    if (conf == null) {
                                      try {
                                        conf = Configuration.getConfigurationPreInit();
                                      } catch (XynaException e) {
                                        throw new RuntimeException("problem building mocked server for update process", e);
                                      }
                                    }
                                  }
                                }
                                return conf;
                              }


                              public OrdertypeManagement getOrderTypeManagement() {
                                return null;
                              }

                            };
                          } catch (XynaException e) {
                            throw new RuntimeException("problem building mocked server for update process", e);
                          }
                        }
                      }
                    }

                    return xfmods;
                  }


                  public XynaFactoryMonitoring getXynaFactoryMonitoring() {
                    return null;
                  }


                  public void setProperty(String key, String value) {
                  }


                  public void setProperty(XynaPropertyWithDefaultValue property) {
                  }


                  public boolean releaseAllSessionPriviliges(String sessionId) {
                    return false;
                  }


                  public boolean releaseSessionPrivilige(String sessionId, ASessionPrivilege privilige) {
                    return false;
                  }


                  public boolean requestSessionPriviliges(String sessionId, ASessionPrivilege privilige) {
                    return false;
                  }


                  public boolean keepSessionAlive(String sessionId) {
                    return false;
                  }


                  public boolean createUser(String id, String roleName, String password, boolean isPassHashed)
                      throws PersistenceLayerException {
                    return false;
                  }


                  public boolean deleteUser(String id) throws PersistenceLayerException {
                    return false;
                  }


                  public String listUsers() {
                    return "";
                  }


                  public boolean changePassword(String id, String oldPassword, String newPassword, boolean isNewPasswordHashed) {
                    return false;
                  }


                  public User authenticate(String id, String password) {
                    return null;
                  }


                  public boolean resetPassword(String id, String password) {
                    return false;
                  }


                  public boolean changeRole(String id, String name) throws PersistenceLayerException {
                    return false;
                  }


                  public boolean hasRight(String methodName, Role role) {
                    return false;
                  }


                  public boolean grantRightToRole(String roleName, String right) throws PersistenceLayerException {
                    return false;
                  }


                  public boolean revokeRightFromRole(String roleName, String right) throws PersistenceLayerException {
                    return false;
                  }


                  public XynaOperatorControl getXynaOperatorControl() {
                    return null;
                  }


                  public void listenToMdmModifications(String sessionId, ANotificationConnection con) {
                  }


                  public void listenToProcessProgress(String sessionId, ANotificationConnection con, Long orderId) {
                  }


                  public boolean hasRight(String methodName, String role) {
                    return false;
                  }


                  public void quitSession(String sessionId) {
                  }


                  public boolean createRight(String rightName) throws PersistenceLayerException {
                    return false;
                  }


                  public boolean deleteRight(String rightName) throws PersistenceLayerException {
                    return false;
                  }


                  public String resolveFunctionToRight(String methodName) {
                    return null;
                  }


                  public User authenticateHashed(String id, String password) {
                    return null;
                  }


                  public Collection<Right> getRights(String language) {
                    return null;
                  }


                  public Collection<Role> getRoles() {
                    return null;
                  }


                  public Collection<User> getUser() {
                    return null;
                  }


                  public boolean isPredefined(PredefinedCategories category, String id) {
                    return false;
                  }


                  public Map<Long, OrderInstance> getAllRunningProcesses(long offset, int count) {
                    return null;
                  }


                  public OrderInstanceDetails getRunningProcessDetails(Long id) {
                    return null;
                  }


                  public void removeProperty(String key) {
                  }


                  public Role authenticateSession(String sessionId, String token) {
                    return null;
                  }


                  public SessionDetails getSessionDetails(String sessionId) {
                    return null;
                  }


                  public boolean usersExists(String id) {
                    return false;
                  }


                  public boolean setPassword(String id, String password) throws PersistenceLayerException {
                    return false;
                  }


                  public boolean setPasswordHash(String id, String passwordhash) throws PersistenceLayerException {
                    return false;
                  }


                  public boolean importUser(String id, String roleName, String passwordhash) throws PersistenceLayerException {
                    return false;
                  }


                  public Map<Long, CronLikeOrderInformation> getAllCronLikeOrders(long maxRows) throws XPRC_CronLikeSchedulerException {
                    return null;
                  }


                  public String[] scanLogForLinesOfOrder(long orderId, int lineOffset, int maxNumberOfLines, String... excludes)
                      throws XFMG_ErrorScanningLogFile {
                    return null;
                  }


                  public String retrieveLogForOrder(long orderId, int lineOffset, int maxNumberOfLines, String... excludes)
                      throws XFMG_ErrorScanningLogFile {
                    return null;
                  }


                  public boolean createDomain(String domainidentifier, DomainType type, int maxRetries, int connectionTimeout)
                      throws PersistenceLayerException {
                    return false;
                  }


                  public boolean createRole(String name, String domain) throws PersistenceLayerException, XFMG_DomainDoesNotExistException {
                    return false;
                  }


                  public boolean deleteRole(String name, String domain)
                      throws PersistenceLayerException, XFMG_PredefinedXynaObjectException, XFMG_RoleIsAssignedException {
                    return false;
                  }


                  public Domain getDomain(String domainidentifier) throws PersistenceLayerException {
                    return null;
                  }


                  public Collection<Domain> getDomains() throws PersistenceLayerException {
                    return null;
                  }


                  public Right getRight(String rightidentifier, String language) throws PersistenceLayerException {
                    return null;
                  }


                  public Role getRole(String rolename, String domainname) throws PersistenceLayerException {
                    return null;
                  }


                  public User getUser(String useridentifier) throws PersistenceLayerException {
                    return null;
                  }


                  public UserSearchResult searchUsers(UserSelect selection, int maxRows) throws PersistenceLayerException {
                    return null;
                  }


                  public boolean setAliasOfRole(String rolename, String domainname, String newAlias)
                      throws PersistenceLayerException, XFMG_RoleDoesNotExistException, XFMG_PredefinedXynaObjectException {
                    return false;
                  }


                  public boolean setConnectionTimeoutOfDomain(String domainidentifier, int connectionTimeout)
                      throws PersistenceLayerException, XFMG_DomainDoesNotExistException {
                    return false;
                  }


                  public boolean setDescriptionOfRight(String rightidentifier, String description, String language)
                      throws PersistenceLayerException, XFMG_RightDoesNotExistException, XFMG_PredefinedXynaObjectException {
                    return false;
                  }


                  public boolean setDescriptionOfRole(String roleidentifier, String domainname, String newDescription)
                      throws PersistenceLayerException, XFMG_RoleDoesNotExistException, XFMG_PredefinedXynaObjectException {
                    return false;
                  }


                  public boolean setDomainSpecificDataOfDomain(String domainidentifier, DomainTypeSpecificData specificData)
                      throws PersistenceLayerException, XFMG_DomainDoesNotExistException {
                    return false;
                  }


                  public boolean setLockedStateOfUser(String useridentifier, boolean newState)
                      throws PersistenceLayerException, XFMG_UserDoesNotExistException, XFMG_PredefinedXynaObjectException {
                    return false;
                  }


                  public boolean setMaxRetriesOfDomain(String domainidentifier, int maxRetries)
                      throws PersistenceLayerException, XFMG_DomainDoesNotExistException {
                    return false;
                  }


                  public boolean setDescriptionOfDomain(String domainidentifier, String description)
                      throws PersistenceLayerException, XFMG_DomainDoesNotExistException, XFMG_PredefinedXynaObjectException {
                    return false;
                  }


                  public boolean deleteDomain(String domainidentifier) throws PersistenceLayerException, XFMG_PredefinedXynaObjectException,
                      XFMG_DomainDoesNotExistException, XFMG_DomainIsAssignedException {
                    return false;
                  }


                  public String listDomains() throws PersistenceLayerException {
                    return null;
                  }


                  public List<Domain> getDomainsForUser(String useridentifier)
                      throws PersistenceLayerException, XFMG_UserDoesNotExistException, XFMG_DomainDoesNotExistException {
                    return null;
                  }


                  public XynaClusteringServicesManagement getXynaClusteringServicesManagement() {
                    return null;
                  }


                  public void createOrdertype(OrdertypeParameter ordertypeParameter)
                      throws PersistenceLayerException, XFMG_InvalidCreationOfExistingOrdertype {
                  }


                  public void modifyOrdertype(OrdertypeParameter ordertypeParameter)
                      throws PersistenceLayerException, XFMG_InvalidModificationOfUnexistingOrdertype {
                  }


                  public void deleteOrdertype(OrdertypeParameter ordertypeParameter) throws PersistenceLayerException {
                  }


                  public List<OrdertypeParameter> listOrdertypes(RuntimeContext ctx) throws PersistenceLayerException {
                    return null;
                  }


                  public void registerQueue(String uniqueName, String externalName, QueueType queueType, QueueConnectData connectData)
                      throws PersistenceLayerException {
                  }


                  public void deregisterQueue(String uniqueName) throws PersistenceLayerException {
                  }


                  public Collection<Queue> listQueues() throws PersistenceLayerException {
                    return null;
                  }


                  public boolean isSessionAlive(String sessionId) throws PersistenceLayerException {
                    return false;
                  }


                  public XynaStatistics getXynaStatistics() {
                    return null;
                  }


                  public XynaStatisticsLegacy getXynaStatisticsLegacy() {
                    return null;
                  }


                  public XynaExtendedStatusManagement getXynaExtendedStatusManagement() {
                    return null;
                  }


                  public Collection<PrioritySetting> listPriorities() throws PersistenceLayerException {
                    return null;
                  }


                  public void removePriority(String orderType) throws PersistenceLayerException {
                  }


                  public void setPriority(String orderType, int priority) throws XFMG_InvalidXynaOrderPriority, PersistenceLayerException {
                  }


                  public void discoverPriority(XynaOrderServerExtension xo) {
                  }


                  public Integer getPriority(String orderType) throws PersistenceLayerException {
                    return null;
                  }


                  public Collection<XynaPropertyWithDefaultValue> getPropertiesWithDefaultValuesReadOnly() {
                    return null;
                  }


                  public void setPriority(String orderType, int priority, Long revision)
                      throws XFMG_InvalidXynaOrderPriority, PersistenceLayerException {
                  }


                  public Collection<RightScope> getRightScopes(String language) throws PersistenceLayerException {
                    return null;
                  }


                  public XynaPropertyWithDefaultValue getPropertyWithDefaultValue(String key) {
                    return null;
                  }


                  public SessionCredentials getNewSession(User user, boolean force) throws PersistenceLayerException {
                    return null;
                  }


                  public boolean authorizeSession(String sessionId, String token, String roleName)
                      throws PersistenceLayerException, XFMG_UnknownSessionIDException, XFMG_SESSION_AUTHENTICATION_FAILED {
                    return false;
                  }


                  public SessionCredentials createSession(XynaUserCredentials credentials, Optional<String> roleName, boolean force)
                      throws PersistenceLayerException {
                    return null;
                  }


                  public boolean createUser(String id, String roleName, String password, boolean isPassHashed, List<String> domains)
                      throws PersistenceLayerException, XFMG_RoleDoesNotExistException, XFMG_PasswordRestrictionViolation,
                      XFMG_NameContainsInvalidCharacter {
                    return false;
                  }


                  public boolean setDomainsOfUser(String useridentifier, List<String> domains)
                      throws PersistenceLayerException, XFMG_UserDoesNotExistException, XFMG_DomainDoesNotExistException {
                    return false;
                  }


                  public PasswordExpiration getPasswordExpiration(String userName) throws PersistenceLayerException {
                    return null;
                  }


                  @Override
                  public String getDefaultName() {
                    return XynaFactoryManagement.DEFAULT_NAME;
                  }


                  @Override
                  protected void init() throws XynaException {
                  }


                  public List<OrdertypeParameter> listOrdertypes(SearchOrdertypeParameter sop) throws PersistenceLayerException {
                    return null;
                  }
                };
              } catch (XynaException e) {
                throw new RuntimeException(e);
              }
            }
          }
        }
        return xfm;
      }


      volatile XynaProcessingBase xprc = null;


      public XynaProcessingBase getProcessing() {
        if (xprc == null) {
          synchronized (this) {
            if (xprc == null) {
              try {
                xprc = new XynaProcessingBase() {

                  public void unregisterSavedWorkflow(String fqNameFromXml) {
                  }


                  public XynaOrderServerExtension startOrderSynchronouslyAndReturnOrder(XynaOrderCreationParameter xocp)
                      throws XynaException {
                    return null;
                  }


                  public OrderExecutionResponse startOrderSynchronouslyAndReturnOrder(XynaOrderCreationParameter xocp,
                                                                                      ResultController resultController) {
                    return null;
                  }


                  public GeneralXynaObject startOrderSynchronously(XynaOrderCreationParameter xocp) throws XynaException {
                    return null;
                  }


                  public Long startOrder(XynaOrderCreationParameter xocp) {
                    return null;
                  }


                  public long startFrequencyControlledTask(FrequencyControlledTaskCreationParameter creationParameter)
                      throws XynaException {
                    return 0;
                  }


                  public CronLikeOrder startCronLikeOrder(CronLikeOrderCreationParameter clocp) throws XPRC_CronLikeSchedulerException {
                    return null;
                  }


                  public void setDestination(DispatcherIdentification dispatcherId, DestinationKey dk, DestinationValue dv)
                      throws PersistenceLayerException {
                  }


                  public boolean requireCapacityForWorkflow(String workflowName, String capName, int cardinality)
                      throws PersistenceLayerException {
                    return false;
                  }


                  public boolean requireCapacityForOrderType(String orderType, String capName, int cardinality)
                      throws PersistenceLayerException {
                    return false;
                  }


                  public void removeDestination(DispatcherIdentification dispatcherId, DestinationKey dk) throws PersistenceLayerException {
                  }


                  public boolean removeCronLikeOrder(Long id) throws XPRC_CronLikeOrderStorageException, XPRC_CronRemovalException {
                    return false;
                  }


                  public boolean removeCapacityForWorkflow(String wfName, String capacityName) throws PersistenceLayerException {
                    return false;
                  }


                  public boolean removeCapacityForOrderType(String orderType, String capacityName) throws PersistenceLayerException {
                    return false;
                  }


                  public boolean removeCapacity(String capacityName) throws PersistenceLayerException {
                    return false;
                  }


                  public void registerSavedWorkflow(String fqNameFromXml) {
                  }


                  public void registerSavedWorkflow(String fqNameFromXml, Long revision) {
                  }


                  public CronLikeOrder modifyCronLikeOrder(Long id, String label, String orderType, GeneralXynaObject payload,
                                                           Long firstStartupTime, Long interval, Boolean enabled, OnErrorAction onError)
                      throws XPRC_CronLikeSchedulerException {
                    return null;
                  }


                  public Collection<VetoInformationStorable> listVetoInformation() throws PersistenceLayerException {
                    return null;
                  }


                  public FactoryWarehouseCursor<OrderInstanceBackup> listSuspendedOrders(ODSConnection con) {
                    return null;
                  }


                  public List<String> listOrderTypesForWorkflow(String workflowOriginalFQName) {
                    return null;
                  }


                  public ExtendedCapacityUsageInformation listExtendedCapacityInformation() {
                    return null;
                  }


                  public List<DispatcherEntry> listDestinations(DispatcherIdentification dispatcherId) {
                    return null;
                  }


                  public List<WorkflowInformation> listWorkflows() {
                    return null;
                  }


                  public Collection<CapacityInformation> listCapacityInformation() {
                    return null;
                  }


                  public List<Capacity> listCapacitiesForOrderType(DestinationKey destination) {
                    return null;
                  }


                  public KillStuckProcessBean killStuckProcess(Long orderId, boolean forceKill, AbortionCause reason) throws XynaException {
                    return null;
                  }


                  public FrequencyControlledTaskInformation getFrequencyControlledTaskInformation(long taskId, String[] selectedStatistics)
                      throws XynaException {
                    return null;
                  }


                  public FrequencyControlledTaskInformation getFrequencyControlledTaskInformation(long taskId) throws XynaException {
                    return null;
                  }


                  public Map<DestinationKey, DestinationValue> getDestinations(DispatcherIdentification dispatcherId) {
                    return null;
                  }


                  public DispatcherEntry getDestination(DispatcherIdentification dispatcherId, DestinationKey dk)
                      throws XPRC_DESTINATION_NOT_FOUND {
                    return null;
                  }


                  public CapacityInformation getCapacityInformation(String capacityName) {
                    return null;
                  }


                  public Collection<DestinationKey> getAllDestinationKeysForWhichAnOrderContextMappingIsCreated() {
                    return null;
                  }


                  public List<CapacityMappingStorable> getAllCapacityMappings() {
                    return null;
                  }


                  public boolean configureOrderContextMappingForDestinationKey(DestinationKey dk, boolean createMapping)
                      throws PersistenceLayerException {
                    return false;
                  }


                  public boolean changeCapacityState(String capacityName, State newState) throws PersistenceLayerException {
                    return false;
                  }


                  public boolean changeCapacityName(String capacityName, String newName) throws PersistenceLayerException {
                    return false;
                  }


                  public boolean changeCapacityCardinality(String capacityName, int newCardinality) throws PersistenceLayerException {
                    return false;
                  }


                  public CancelBean cancelOrder(Long id, Long timeout) throws XPRC_CancelFailedException {
                    return null;
                  }


                  public boolean cancelFrequencyControlledTask(long taskId) throws XynaException {
                    return false;
                  }


                  public boolean addCapacity(String string, int cardinality, State enumState)
                      throws XPRC_CAPACITY_ALREADY_DEFINED, PersistenceLayerException {
                    return false;
                  }


                  @Override
                  protected void init() throws XynaException {

                  }


                  @Override
                  public String getDefaultName() {
                    return null;
                  }


                  @Override
                  public void stopGracefully() throws XynaException {
                  }


                  @Override
                  public XynaScheduler getXynaScheduler() {
                    return null;
                  }


                  @Override
                  public XynaProcessingODS getXynaProcessingODS() {
                    return null;
                  }


                  @Override
                  public XynaProcessCtrlExecution getXynaProcessCtrlExecution() {
                    return null;
                  }


                  @Override
                  public BatchProcessManagement getBatchProcessManagement() {
                    return null;
                  }


                  volatile WorkflowEngine wfeng = null;


                  @Override
                  public WorkflowEngine getWorkflowEngine() {
                    if (wfeng == null) {
                      synchronized (this) {
                        if (wfeng == null) {
                          wfeng = new WorkflowEngine() {

                            public void undeployWorkflow(String originalFqName, boolean undeployDependentObjects, boolean disableChecks)
                                throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException,
                                XPRC_InternalObjectMayNotBeUndeployedException, XPRC_MDMUndeploymentException {
                            }


                            public void undeployException(String originalFqName, boolean undeployDependentObjects, boolean disableChecks)
                                throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException,
                                XPRC_InternalObjectMayNotBeUndeployedException, XPRC_MDMUndeploymentException {
                            }


                            public void undeployDatatype(String fqClassName, boolean undeployDependendObjects, boolean disableChecks)
                                throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException,
                                XPRC_InternalObjectMayNotBeUndeployedException, XPRC_MDMUndeploymentException {
                            }


                            public OrderContext setOrderContext(OrderContextServerExtension ctx) {
                              return null;
                            }


                            public OrderContext removeOrderContext() {
                              return null;
                            }


                            public EngineSpecificStepHandlerManager getStepHandlerManager() {
                              return null;
                            }


                            public EngineSpecificWorkflowProcessor getPlanningProcessor() {
                              return null;
                            }


                            public OrderContext getOrderContext() {
                              return null;
                            }


                            public int getNumberOfRunningProcesses() {
                              return 0;
                            }


                            public EngineSpecificWorkflowProcessor getExecutionProcessor() {
                              return null;
                            }


                            volatile DeploymentHandling depHand = null;


                            public DeploymentHandling getDeploymentHandling() {
                              if (depHand == null) {
                                synchronized (this) {
                                  if (depHand == null) {
                                    try {
                                      depHand = new DeploymentHandling() {

                                        @Override
                                        public synchronized void executeDeploymentHandler(Integer handlerPriority,
                                                                                          com.gip.xyna.xprc.xfractwfe.generation.GenerationBase object,
                                                                                          DeploymentMode mode)
                                            throws com.gip.xyna.xprc.exceptions.XPRC_DeploymentHandlerException {
                                        };


                                        @Override
                                        public synchronized void executeUndeploymentHandler(Integer handlerPriority,
                                                                                            com.gip.xyna.xprc.xfractwfe.generation.GenerationBase object)
                                            throws com.gip.xyna.xprc.exceptions.XPRC_UnDeploymentHandlerException {
                                        };
                                      };
                                    } catch (XynaException e) {
                                      logger.warn(null, e);
                                    }
                                  }
                                }
                              }
                              return depHand;
                            }


                            public String getDefaultName() {
                              return null;
                            }


                            public EngineSpecificWorkflowProcessor getCleanupProcessor() {
                              return null;
                            }


                            public void deployWorkflowAndDependants(String fqClassName)
                                throws XPRC_DeploymentDuringUndeploymentException, XPRC_InheritedConcurrentDeploymentException,
                                XPRC_MDMDeploymentException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException {
                            }


                            public void deployWorkflow(String fqClassName, WorkflowProtectionMode mode)
                                throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException,
                                XPRC_DeploymentDuringUndeploymentException, XPRC_InheritedConcurrentDeploymentException,
                                XPRC_MDMDeploymentException {
                            }


                            public void deployException(String fqClassName, WorkflowProtectionMode mode)
                                throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException,
                                XPRC_DeploymentDuringUndeploymentException, XPRC_InheritedConcurrentDeploymentException,
                                XPRC_MDMDeploymentException {
                            }


                            public void deployDatatypeAndDependants(String fqClassName, String fileName, InputStream inputStream)
                                throws Ex_FileAccessException, XACT_JarFileUnzipProblem, XPRC_UNSUPPORTED_FILE_EXTENSION_DEPLOYMENT,
                                XPRC_UNSUPPORTED_FILE_EXTENSION_DEPLOYMENT, XPRC_InvalidPackageNameException,
                                XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_DeploymentDuringUndeploymentException,
                                XPRC_InheritedConcurrentDeploymentException, XPRC_MDMDeploymentException {
                            }


                            public void deployDatatype(String fqClassName, WorkflowProtectionMode mode, Map<String, InputStream> jars)
                                throws Ex_FileAccessException, XPRC_InvalidPackageNameException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH,
                                XPRC_DeploymentDuringUndeploymentException, XPRC_InheritedConcurrentDeploymentException,
                                XPRC_MDMDeploymentException {
                            }


                            public void deployDatatype(String fqClassName, WorkflowProtectionMode mode, Map<String, InputStream> jars,
                                                       Long revision)
                                throws Ex_FileAccessException, XPRC_InvalidPackageNameException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH,
                                XPRC_DeploymentDuringUndeploymentException, XPRC_InheritedConcurrentDeploymentException,
                                XPRC_MDMDeploymentException {
                            }


                            public void deployDatatype(String fqClassName, WorkflowProtectionMode mode, String fileName,
                                                       InputStream inputStream)
                                throws Ex_FileAccessException, XACT_JarFileUnzipProblem, XPRC_UNSUPPORTED_FILE_EXTENSION_DEPLOYMENT,
                                XPRC_InvalidPackageNameException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH,
                                XPRC_DeploymentDuringUndeploymentException, XPRC_InheritedConcurrentDeploymentException,
                                XPRC_MDMDeploymentException {
                            }


                            public void deleteWorkflow(String originalFqName, boolean disableChecks,
                                                       boolean recursivlyUndeployIfDeployedAndDependenciesExist,
                                                       boolean deleteDependencies) {
                            }


                            public void deleteWorkflow(String originalFqName, boolean disableChecks,
                                                       boolean recursivlyUndeployIfDeployedAndDependenciesExist, boolean deleteDependencies,
                                                       boolean checkDeploymentLock, Long revision) {
                            }


                            public void deleteException(String originalFqName, boolean disableChecks,
                                                        boolean recursivlyUndeployIfDeployedAndDependenciesExist,
                                                        boolean deleteDependencies) {
                            }


                            public void deleteException(String originalFqName, boolean disableChecks,
                                                        boolean recursivlyUndeployIfDeployedAndDependenciesExist,
                                                        boolean deleteDependencies, boolean checkDeploymentLock, Long revision) {
                            }


                            public void deleteDatatype(String originalFqName, boolean disableChecks,
                                                       boolean recursivlyUndeployIfDeployedAndDependenciesExist,
                                                       boolean deleteDependencies) {
                            }


                            public void deleteDatatype(String originalFqName, boolean disableChecks,
                                                       boolean recursivlyUndeployIfDeployedAndDependenciesExist, boolean deleteDependencies,
                                                       boolean checkDeploymentLock, Long revision) {
                            }


                            public boolean checkForActiveOrders() throws XynaException {
                              return false;
                            }


                            public SpecialPurposeHelper getSpecialPurposeHelper() {
                              return null;
                            }


                            public void deployDatatype(String fqClassName, WorkflowProtectionMode mode, String fileName,
                                                       InputStream inputStream, Long revision)
                                throws Ex_FileAccessException, XACT_JarFileUnzipProblem, XPRC_UNSUPPORTED_FILE_EXTENSION_DEPLOYMENT,
                                XPRC_InvalidPackageNameException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH,
                                XPRC_DeploymentDuringUndeploymentException, XPRC_InheritedConcurrentDeploymentException,
                                XPRC_MDMDeploymentException {
                            }


                            public void undeployDatatype(String fqClassName, boolean undeployDependendObjects, boolean disableChecks,
                                                         Long revision)
                                throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException,
                                XPRC_InternalObjectMayNotBeUndeployedException, XPRC_MDMUndeploymentException {
                            }


                            public void deployWorkflow(String fqClassName, WorkflowProtectionMode mode, Long revision)
                                throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException,
                                XPRC_DeploymentDuringUndeploymentException, XPRC_InheritedConcurrentDeploymentException,
                                XPRC_MDMDeploymentException {
                            }


                            public void undeployWorkflow(String originalFqName, boolean undeployDependentObjects, boolean disableChecks,
                                                         Long revision)
                                throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException,
                                XPRC_InternalObjectMayNotBeUndeployedException, XPRC_MDMUndeploymentException {
                            }


                            public void deployException(String fqClassName, WorkflowProtectionMode mode, Long revision)
                                throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException,
                                XPRC_DeploymentDuringUndeploymentException, XPRC_InheritedConcurrentDeploymentException,
                                XPRC_MDMDeploymentException {
                            }


                            public void undeployException(String originalFqName, boolean undeployDependentObjects, boolean disableChecks,
                                                          Long revision)
                                throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException,
                                XPRC_InternalObjectMayNotBeUndeployedException, XPRC_MDMUndeploymentException {
                            }


                            public void deployExceptionAndDependants(String fqXmlName) throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH,
                                XPRC_InvalidPackageNameException, XPRC_DeploymentDuringUndeploymentException,
                                XPRC_InheritedConcurrentDeploymentException, XPRC_MDMDeploymentException {
                            }


                            public void deployMultiple(Map<XMOMType, List<String>> deploymentItems, WorkflowProtectionMode mode,
                                                       Long revision)
                                throws MDMParallelDeploymentException, XPRC_DeploymentDuringUndeploymentException,
                                XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException {
                            }


                            public void undeployXMOMObject(String originalFqName, XMOMType type, DependentObjectMode dependentObjectMode,
                                                           boolean disableChecks, Long revision)
                                throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException,
                                XPRC_InternalObjectMayNotBeUndeployedException, XPRC_MDMUndeploymentException,
                                XPRC_ExclusiveDeploymentInProgress, XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT {
                            }


                            public void deleteXMOMObject(String fullXmlName, XMOMType type, boolean disableChecks,
                                                         DependentObjectMode dependentObjectMode, boolean checkDeploymentLock,
                                                         Long revision)
                                throws XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT {
                            }


                            public Map<XMOMType, List<String>> existsInRevision(Map<XMOMType, List<String>> xmomObjects, Long revision) {
                              return null;
                            }


                            public void copyToRevisionAndDeploy(Map<XMOMType, List<String>> xmomObjects, Long fromRevision, Long toRevision,
                                                                DeploymentMode deploymentMode, WorkflowProtectionMode wpm,
                                                                boolean inheritCodeChanged)
                                throws Ex_FileAccessException, XPRC_InvalidPackageNameException, MDMParallelDeploymentException,
                                XPRC_DeploymentDuringUndeploymentException {

                            }


                            public void deleteXMOMObjects(Map<XMOMType, List<String>> xmomObjects, boolean disableChecks,
                                                          DependentObjectMode dependentObjectMode, boolean checkDeploymentLock,
                                                          Long revision)
                                throws XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT {
                            }


                            public void copyToRevisionAndDeploy(Map<XMOMType, List<String>> xmomObjects, Long fromRevision, Long toRevision,
                                                                DeploymentMode deploymentMode, WorkflowProtectionMode wpm,
                                                                boolean inheritCodeChanged, String username, String sessionId,
                                                                String comment)
                                throws Ex_FileAccessException, XPRC_InvalidPackageNameException, MDMParallelDeploymentException,
                                XPRC_DeploymentDuringUndeploymentException {
                            }
                          };
                        }
                      }
                    }

                    return wfeng;
                  }


                  @Override
                  public OrderStatus getOrderStatus() {
                    return null;
                  }


                  private volatile XynaFrequencyControl xfc = null;


                  @Override
                  public XynaFrequencyControl getFrequencyControl() {
                    if (xfc == null) {
                      synchronized (this) {
                        if (xfc == null) {
                          try {
                            xfc = new XynaFrequencyControl() {

                              @Override
                              public FrequencyControlledTask getActiveFrequencyControlledTask(long taskId) {
                                return null;
                              }
                            };
                          } catch (XynaException e) {
                          }
                        }
                      }
                    }
                    return xfc;
                  }


                  @Override
                  public CancelBean cancelOrder(Long id, Long timeout, boolean waitForTimeout) throws XPRC_CancelFailedException {
                    return null;
                  }


                  public void restartCronLikeTimerThread() {
                  }


                  public SchedulerInformationBean listSchedulerInformation(SchedulerInformationBean.Mode mode) {
                    return null;
                  }


                  public void freeAdministrativeVeto(String vetoName)
                      throws XPRC_AdministrativeVetoDeallocationDenied, PersistenceLayerException {
                  }


                  public void allocateAdministrativeVeto(String vetoName, String documentation)
                      throws XPRC_AdministrativeVetoAllocationDenied, PersistenceLayerException {
                  }


                  public void setDocumentationOfAdministrativeVeto(String vetoName, String documentation)
                      throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
                  }


                  public VetoSearchResult searchVetos(VetoSelectImpl select, int maxRows) throws PersistenceLayerException {
                    return null;
                  }


                  public OrderSeriesManagementInformation listOrderSeriesManagementInformation(Mode mode) {
                    return null;
                  }


                  public RescheduleSeriesOrderInformation rescheduleSeriesOrder(long orderId, boolean force) {
                    return null;
                  }


                  public boolean requireCapacityForOrderType(String orderType, String capName, int cardinality, String applicationName,
                                                             String versionName)
                      throws PersistenceLayerException, XFMG_InvalidCapacityCardinality {
                    return false;
                  }


                  public boolean removeCapacityForOrderType(String orderType, String capacityName, String applicationName,
                                                            String versionName)
                      throws PersistenceLayerException {
                    return false;
                  }


                  public CronLikeOrder modifyCronLikeOrder(Long id, String label, DestinationKey destinationKey, GeneralXynaObject payload,
                                                           Long firstStartupTime, String timeZoneID, Long interval, Boolean useDST,
                                                           Boolean enabled, OnErrorAction onError, String cloCustom0, String cloCustom1,
                                                           String cloCustom2, String cloCustom3)
                      throws XPRC_CronLikeSchedulerException {
                    return null;
                  }


                  public CronLikeOrder modifyCronLikeOrder(Long id, String label, DestinationKey destinationKey, GeneralXynaObject payload,
                                                           Calendar firstStartupTimeWithTimeZone, Long interval, Boolean useDST,
                                                           Boolean enabled, OnErrorAction onError, String cloCustom0, String cloCustom1,
                                                           String cloCustom2, String cloCustom3)
                      throws XPRC_CronLikeSchedulerException {
                    return null;
                  }


                  public CronLikeOrder modifyTimeControlledOrder(Long id, CronLikeOrderCreationParameter clocp)
                      throws XPRC_CronLikeSchedulerException, XPRC_InvalidCronLikeOrderParametersException {
                    return null;
                  }


                  public KillStuckProcessBean killStuckProcess(KillStuckProcessBean bean) throws XynaException {
                    return null;
                  }


                  public boolean cancelBatchProcess(Long batchProcessId, CancelMode cancelMode) {
                    return false;
                  }


                  public boolean pauseBatchProcess(Long batchProcessId) {
                    return false;
                  }


                  public boolean continueBatchProcess(Long batchProcessId) {
                    return false;
                  }


                  public Long startBatchProcess(BatchProcessInput input) throws XynaException {
                    return null;
                  }


                  public BatchProcessInformation startBatchProcessSynchronous(BatchProcessInput input) throws XynaException {
                    return null;
                  }


                  public BatchProcessInformation getBatchProcessInformation(Long batchProcessId) {
                    return null;
                  }


                  public BatchProcessSearchResult searchBatchProcesses(BatchProcessSelectImpl select, int maxRows)
                      throws PersistenceLayerException {
                    return null;
                  }


                  public boolean modifyBatchProcess(Long batchProcessId, BatchProcessInput input) {
                    return false;
                  }


                  public boolean requireCapacityForOrderType(String orderType, String capName, int cardinality,
                                                             RuntimeContext runtimeContext)
                      throws PersistenceLayerException, XFMG_InvalidCapacityCardinality {
                    return false;
                  }


                  public boolean removeCapacityForOrderType(String orderType, String capacityName, RuntimeContext runtimeContext)
                      throws PersistenceLayerException {
                    return false;
                  }


                  public Map<ApplicationEntryType, Map<String, DeploymentStatus>> listDeploymentStatuses(Long revision) {
                    return null;
                  }


                  @Override
                  public DispatcherEntry getDestination(DispatcherIdentification dispatcherId, DestinationKey dk,
                                                        boolean followRuntimeContextDependencies)
                      throws XPRC_DESTINATION_NOT_FOUND {
                    return null;
                  }


                  @Override
                  public XynaXmomSerialization getXmomSerialization() {
                    return null;
                  }
                  
                  @Override
                  public XynaPythonSnippetManagement getXynaPythonSnippetManagement() {
                    return null;
                  }
                };
              } catch (XynaException e) {
                logger.warn(null, e);
              }
            }
          }
        }

        return xprc;
      }


      public XynaDevelopmentBase getXynaDevelopment() {
        return null;
      }


      public XynaMultiChannelPortalBase getXynaMultiChannelPortal() {
        return null;
      }


      public void init() throws XynaException {
      }


      public void initLateInitComponents(HashMap allDependencies) throws XynaException {
      }


      public boolean isShuttingDown() {
        return false;
      }


      public void shutdown() {
      }


      public void shutdownComponents() throws XynaException {
      }


      public XynaActivationPortal getActivationPortal() {
        return null;
      }


      public XynaFactoryManagementPortal getFactoryManagementPortal() {
        return null;
      }


      public XynaProcessingPortal getProcessingPortal() {
        return null;
      }


      public XynaDevelopmentPortal getXynaDevelopmentPortal() {
        return null;
      }


      public Channel getXynaMultiChannelPortalPortal() {
        return null;
      }


      private volatile XynaFactoryWarehouseBase xnwh;


      public XynaFactoryWarehouseBase getXynaNetworkWarehouse() {
        if (xnwh == null) {
          synchronized (this) {
            if (xnwh == null) {
              try {
                xnwh = new XynaFactoryWarehouseBase() {

                  public void undeployPersistenceLayer(String persistenceLayerFqClassname) throws PersistenceLayerException,
                      XNWH_PersistenceLayerNotRegisteredException, XNWH_PersistenceLayerMayNotBeUndeployedInUseException {

                  }


                  public boolean store(String destination, String key, Serializable serializable) throws PersistenceLayerException {
                    return false;
                  }


                  public Serializable retrieve(String destination, String key) {
                    return null;
                  }


                  public boolean remove(String destination, String key) throws PersistenceLayerException {
                    return false;
                  }


                  public SecureStorage getSecureStorage() {
                    return null;
                  }


                  public void deployPersistenceLayer(String name, String persistenceLayerFqClassname)
                      throws XNWH_PERSISTENCE_LAYER_CLASS_NOT_FOUND, XFMG_JarFolderNotFoundException, PersistenceLayerException {
                  }


                  @Override
                  protected void init() throws XynaException {
                  }


                  @Override
                  public String getDefaultName() {
                    return null;
                  }


                  @Override
                  public boolean removeShutdownWarehouseJobOrder(XynaOrderServerExtension xo) {
                    return false;
                  }


                  @Override
                  public XynaWarehouseJobManagement getXynaWarehouseJobManagement() {
                    return null;
                  }


                  @Override
                  public XynaClusteringServices getXynaClusteringServices() {
                    return null;
                  }


                  private volatile XMOMPersistenceBase xp;


                  @Override
                  public XMOMPersistenceBase getXMOMPersistence() {
                    if (xp == null) {
                      synchronized (this) {
                        if (xp == null) {
                          try {
                            xp = new XMOMPersistenceBase() {

                              private volatile XMOMPersistenceManagement xmpm;


                              @Override
                              public XMOMPersistenceManagement getXMOMPersistenceManagement() {
                                if (xmpm == null) {
                                  synchronized (this) {
                                    if (xmpm == null) {
                                      try {
                                        xmpm = XMOMPersistenceManagement.getXMOMPersistenceManagementPreInit();
                                      } catch (XynaException e) {
                                        throw new RuntimeException(e);
                                      }
                                    }
                                  }
                                }
                                return xmpm;
                              }


                              @Override
                              public String getDefaultName() {
                                return null;
                              }


                              @Override
                              protected void init() throws XynaException {
                              }

                            };
                          } catch (XynaException e) {
                            throw new RuntimeException(e);
                          }
                        }
                      }

                    }
                    return xp;
                  }


                  @Override
                  public boolean addShutdownWarehouseJobOrder(XynaOrderServerExtension xo) {
                    return false;
                  }


                  public ConnectionPoolManagement getConnectionPoolManagement() {
                    return null;
                  }


                  @Override
                  public StatisticsStore getStatisticsStore() {
                    return null;
                  }
                };
              } catch (XynaException e) {
                throw new RuntimeException(e);
              }
            }
          }

        }
        return xnwh;
      }


      public XynaFactoryWarehousePortal getXynaNetworkWarehousePortal() {
        return null;
      }


      public XynaMultiChannelPortalSecurityLayer getXynaMultiChannelPortalSecurityLayer() {
        return null;
      }


      public boolean finishedInitialization() {
        return false;
      }


      public boolean isStartingUp() {
        return true;
      }


      public IDGenerator getIDGenerator() {
        return null;
      }


      public FutureExecution getFutureExecution() {
        return XynaFactory.futureExecutionInstance;
      }


      public FutureExecution getFutureExecutionForInit() {
        return null;
      }


      public long getBootCntId() {
        return 0;
      }


      public boolean lockShutdown(String cause) {
        return false;
      }


      public void unlockShutdown() {
      }


      public int getBootCount() {
        return 0;
      }


    });

    SerializableClassloadedObject.cld = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher();
  }


}
