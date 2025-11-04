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

package com.gip.xyna.xmcp;



import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipInputStream;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.FileUtils;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.Section;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaRuntimeException;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.exceptions.exceptioncode.CodeGroupUnknownException;
import com.gip.xyna.utils.exceptions.exceptioncode.NoCodeAvailableException;
import com.gip.xyna.xact.XynaActivationPortal;
import com.gip.xyna.xact.exceptions.XACT_AdditionalDependencyDeploymentException;
import com.gip.xyna.xact.exceptions.XACT_FilterImplClassNotFoundException;
import com.gip.xyna.xact.exceptions.XACT_FilterInstanceNeedsEnabledFilterException;
import com.gip.xyna.xact.exceptions.XACT_FilterMayNotBeRemovedIsDeployedException;
import com.gip.xyna.xact.exceptions.XACT_FilterNotFound;
import com.gip.xyna.xact.exceptions.XACT_IncompatibleFilterImplException;
import com.gip.xyna.xact.exceptions.XACT_IncompatibleTriggerImplException;
import com.gip.xyna.xact.exceptions.XACT_InvalidFilterConfigurationParameterValueException;
import com.gip.xyna.xact.exceptions.XACT_InvalidStartParameterException;
import com.gip.xyna.xact.exceptions.XACT_JarFileUnzipProblem;
import com.gip.xyna.xact.exceptions.XACT_LibOfFilterImplNotFoundException;
import com.gip.xyna.xact.exceptions.XACT_LibOfTriggerImplNotFoundException;
import com.gip.xyna.xact.exceptions.XACT_OldFilterVersionInstantiationException;
import com.gip.xyna.xact.exceptions.XACT_TriggerCouldNotBeStartedException;
import com.gip.xyna.xact.exceptions.XACT_TriggerImplClassNotFoundException;
import com.gip.xyna.xact.exceptions.XACT_TriggerInstanceNeedsEnabledTriggerException;
import com.gip.xyna.xact.exceptions.XACT_TriggerInstanceNotFound;
import com.gip.xyna.xact.exceptions.XACT_TriggerMayNotBeRemovedIsDeployedException;
import com.gip.xyna.xact.exceptions.XACT_TriggerNotFound;
import com.gip.xyna.xact.trigger.DeployFilterParameter;
import com.gip.xyna.xact.trigger.Filter;
import com.gip.xyna.xact.trigger.FilterInformation;
import com.gip.xyna.xact.trigger.Trigger;
import com.gip.xyna.xact.trigger.TriggerInformation;
import com.gip.xyna.xact.trigger.TriggerInstanceIdentification;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.RepositoryEvent;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.SingleRepositoryEvent;
import com.gip.xyna.xdev.exceptions.XDEV_CodeAccessInitializationException;
import com.gip.xyna.xdev.exceptions.XDEV_PackageDefinitionFileInvalidRootException;
import com.gip.xyna.xdev.exceptions.XDEV_PackageDefinitionFileNotFoundException;
import com.gip.xyna.xdev.exceptions.XDEV_PackageDefinitionItemNotFoundException;
import com.gip.xyna.xdev.exceptions.XDEV_RefactoringConflict;
import com.gip.xyna.xdev.exceptions.XDEV_RefactoringFault;
import com.gip.xyna.xdev.exceptions.XDEV_ZipFileCouldNotBeCreatedException;
import com.gip.xyna.xdev.xdelivery.ImportDeliveryItem;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilterInstance;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListenerInstance;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.RefactoringActionParameter;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.RefactoringResult;
import com.gip.xyna.xdev.xfractmod.xmomlocks.LockManagement.Path;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.parameters.InstantiateRepositoryAccessParameters;
import com.gip.xyna.xdev.xlibdev.supp4eclipse.Support4Eclipse;
import com.gip.xyna.xdev.xlibdev.supp4eclipse.base.FilterImplementationTemplate;
import com.gip.xyna.xdev.xlibdev.supp4eclipse.base.ServiceImplementationTemplate;
import com.gip.xyna.xdev.xlibdev.supp4eclipse.base.TriggerImplementationTemplate;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.XynaFactoryManagementPortal;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotBuildNewVersionForApplication;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotBuildNewWorkspace;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotBuildWorkingSet;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotRemoveWorkspace;
import com.gip.xyna.xfmg.exceptions.XFMG_DomainDoesNotExistException;
import com.gip.xyna.xfmg.exceptions.XFMG_DomainIsAssignedException;
import com.gip.xyna.xfmg.exceptions.XFMG_DuplicateSessionException;
import com.gip.xyna.xfmg.exceptions.XFMG_ErrorScanningLogFile;
import com.gip.xyna.xfmg.exceptions.XFMG_FailedToAddObjectToApplication;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidCapacityCardinality;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidCreationOfExistingOrdertype;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidModificationOfUnexistingOrdertype;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidXynaOrderPriority;
import com.gip.xyna.xfmg.exceptions.XFMG_JarFolderNotFoundException;
import com.gip.xyna.xfmg.exceptions.XFMG_NameContainsInvalidCharacter;
import com.gip.xyna.xfmg.exceptions.XFMG_NamingConventionException;
import com.gip.xyna.xfmg.exceptions.XFMG_NoSuchDataModelException;
import com.gip.xyna.xfmg.exceptions.XFMG_NoSuchDataModelTypeException;
import com.gip.xyna.xfmg.exceptions.XFMG_PasswordRestrictionViolation;
import com.gip.xyna.xfmg.exceptions.XFMG_PredefinedXynaObjectException;
import com.gip.xyna.xfmg.exceptions.XFMG_RightDoesNotExistException;
import com.gip.xyna.xfmg.exceptions.XFMG_RoleDoesNotExistException;
import com.gip.xyna.xfmg.exceptions.XFMG_RoleIsAssignedException;
import com.gip.xyna.xfmg.exceptions.XFMG_SESSION_AUTHENTICATION_FAILED;
import com.gip.xyna.xfmg.exceptions.XFMG_SHARED_LIB_NOT_FOUND;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownSessionIDException;
import com.gip.xyna.xfmg.exceptions.XFMG_UserAuthenticationFailedException;
import com.gip.xyna.xfmg.exceptions.XFMG_UserDoesNotExistException;
import com.gip.xyna.xfmg.exceptions.XFMG_UserIsLockedException;
import com.gip.xyna.xfmg.extendedstatus.XynaExtendedStatusManagement;
import com.gip.xyna.xfmg.statistics.XynaStatistics;
import com.gip.xyna.xfmg.statistics.XynaStatisticsLegacy;
import com.gip.xyna.xfmg.xclusteringservices.ClusterInformation;
import com.gip.xyna.xfmg.xfctrl.RMIManagement;
import com.gip.xyna.xfmg.xfctrl.RMIManagement.InitializableRemoteInterface;
import com.gip.xyna.xfmg.xfctrl.RMIManagement.RMIImplFactory;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationDefinitionInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable.ApplicationEntryType;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagement;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.BuildApplicationVersionParameters;
import com.gip.xyna.xfmg.xfctrl.appmgmt.CopyApplicationIntoWorkspaceParameters;
import com.gip.xyna.xfmg.xfctrl.appmgmt.WorkspaceInformation;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassProvider;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelManagement;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelResult;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.parameters.ImportDataModelParameters;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.parameters.ModifyDataModelParameters;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.parameters.RemoveDataModelParameters;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.deploymentmarker.DeploymentMarker;
import com.gip.xyna.xfmg.xfctrl.deploymentmarker.DeploymentMarkerManagement;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateManagement;
import com.gip.xyna.xfmg.xfctrl.deploystate.XMOMSearchDispatcher;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.DeploymentItemBuilder;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.FactoryNodeStorable;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.RemoteDestinationManagement;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.monitoring.InterlinkSearchDispatcher;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.Queue;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.QueueConnectData;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.QueueType;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.CreateWorkspaceResult;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.WorkspaceManagement;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.parameters.RemoveWorkspaceParameters;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResult;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSelect;
import com.gip.xyna.xfmg.xfmon.XynaFactoryMonitoring;
import com.gip.xyna.xfmg.xfmon.processmonitoring.ProcessMonitoring;
import com.gip.xyna.xfmg.xods.XynaFactoryManagementODS;
import com.gip.xyna.xfmg.xods.configuration.PropertyMap;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.UserType;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyInt;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyWithDefaultValue;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.OrderInputSourceManagement.OptionalOISGenerateMetaInformation;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.storables.OrderInputSourceStorable;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeParameter;
import com.gip.xyna.xfmg.xods.ordertypemanagement.SearchOrdertypeParameter;
import com.gip.xyna.xfmg.xods.priority.PrioritySetting;
import com.gip.xyna.xfmg.xopctrl.DomainTypeSpecificData;
import com.gip.xyna.xfmg.xopctrl.managedsessions.ASessionPrivilege;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionCredentials;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionDetails;
import com.gip.xyna.xfmg.xopctrl.managedsessions.notification.ANotificationConnection;
import com.gip.xyna.xfmg.xopctrl.usermanagement.AccessControlled;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Domain;
import com.gip.xyna.xfmg.xopctrl.usermanagement.DomainType;
import com.gip.xyna.xfmg.xopctrl.usermanagement.PasswordExpiration;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Right;
import com.gip.xyna.xfmg.xopctrl.usermanagement.RightScope;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.User;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.PredefinedCategories;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Rights;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaUserCredentials;
import com.gip.xyna.xfmg.xopctrl.usermanagement.selectuser.UserSearchResult;
import com.gip.xyna.xfmg.xopctrl.usermanagement.selectuser.UserSelect;
import com.gip.xyna.xmcp.PluginDescription.PluginType;
import com.gip.xyna.xmcp.exceptions.XMCP_COULD_NOT_DELETE_DIRECTORY;
import com.gip.xyna.xmcp.exceptions.XMCP_InternalObjectModifiedException;
import com.gip.xyna.xmcp.exceptions.XMCP_RMI_BINDING_ERROR;
import com.gip.xyna.xmcp.exceptions.XMCP_RestrictedCallNotAvailableException;
import com.gip.xyna.xmcp.xguisupport.XGUISupport;
import com.gip.xyna.xmcp.xguisupport.XGUISupportPortal;
import com.gip.xyna.xmcp.xguisupport.messagebus.MessageBusManagementPortal;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageInputParameter;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageRetrievalResult;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageSubscriptionParameter;
import com.gip.xyna.xnwh.XynaFactoryWarehousePortal;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_PERSISTENCE_LAYER_CLASS_NOT_FOUND;
import com.gip.xyna.xnwh.exceptions.XNWH_PersistenceLayerMayNotBeUndeployedInUseException;
import com.gip.xyna.xnwh.exceptions.XNWH_PersistenceLayerNotRegisteredException;
import com.gip.xyna.xnwh.persistence.FactoryWarehouseCursor;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.pools.ConnectionPoolManagement;
import com.gip.xyna.xnwh.securestorage.SecureStorage;
import com.gip.xyna.xnwh.selection.parsing.SearchRequestBean;
import com.gip.xyna.xnwh.selection.parsing.SearchRequestBean.OrderBy;
import com.gip.xyna.xnwh.selection.parsing.SearchResult;
import com.gip.xyna.xnwh.selection.parsing.SelectionParser;
import com.gip.xyna.xnwh.statistics.StatisticsStore;
import com.gip.xyna.xprc.CronLikeOrderCreationParameter;
import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.XynaProcessing.DispatcherEntry;
import com.gip.xyna.xprc.XynaProcessingPortal;
import com.gip.xyna.xprc.exceptions.MiProcessingRejected;
import com.gip.xyna.xprc.exceptions.XPRC_AdministrativeVetoAllocationDenied;
import com.gip.xyna.xprc.exceptions.XPRC_AdministrativeVetoDeallocationDenied;
import com.gip.xyna.xprc.exceptions.XPRC_CAPACITY_ALREADY_DEFINED;
import com.gip.xyna.xprc.exceptions.XPRC_CancelFailedException;
import com.gip.xyna.xprc.exceptions.XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain;
import com.gip.xyna.xprc.exceptions.XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState;
import com.gip.xyna.xprc.exceptions.XPRC_CronLikeOrderStorageException;
import com.gip.xyna.xprc.exceptions.XPRC_CronLikeSchedulerException;
import com.gip.xyna.xprc.exceptions.XPRC_CronRemovalException;
import com.gip.xyna.xprc.exceptions.XPRC_DESTINATION_NOT_FOUND;
import com.gip.xyna.xprc.exceptions.XPRC_DeploymentDuringUndeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT;
import com.gip.xyna.xprc.exceptions.XPRC_ExclusiveDeploymentInProgress;
import com.gip.xyna.xprc.exceptions.XPRC_ExecutionDestinationMissingException;
import com.gip.xyna.xprc.exceptions.XPRC_FACTORY_IS_SHUTTING_DOWN;
import com.gip.xyna.xprc.exceptions.XPRC_FileExistsException;
import com.gip.xyna.xprc.exceptions.XPRC_INVALID_MONITORING_TYPE;
import com.gip.xyna.xprc.exceptions.XPRC_IllegalManualInteractionResponse;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InternalObjectMayNotBeUndeployedException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidCronLikeOrderParametersException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXmlMissingRequiredElementException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMUndeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH;
import com.gip.xyna.xprc.exceptions.XPRC_ResumeFailedException;
import com.gip.xyna.xprc.exceptions.XPRC_UNSUPPORTED_FILE_EXTENSION_DEPLOYMENT;
import com.gip.xyna.xprc.exceptions.XPRC_VERSION_DETECTION_PROBLEM;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcessManagement.CancelMode;
import com.gip.xyna.xprc.xbatchmgmt.beans.BatchProcessInformation;
import com.gip.xyna.xprc.xbatchmgmt.beans.BatchProcessInput;
import com.gip.xyna.xprc.xbatchmgmt.selectbatch.BatchProcessSearchResult;
import com.gip.xyna.xprc.xbatchmgmt.selectbatch.BatchProcessSelectImpl;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTaskCreationParameter;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTaskInformation;
import com.gip.xyna.xprc.xfqctrl.search.FrequencyControlledTaskSearchResult;
import com.gip.xyna.xprc.xfqctrl.search.FrequencyControlledTaskSelect;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.AssumedDeadlockException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DependentObjectMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.MDMParallelDeploymentException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.WorkflowProtectionMode;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xpce.manualinteraction.IManualInteraction.ProcessManualInteractionResult;
import com.gip.xyna.xprc.xpce.manualinteraction.ManualInteractionEntry;
import com.gip.xyna.xprc.xpce.manualinteraction.selectmi.ExtendedManualInteractionResult;
import com.gip.xyna.xprc.xpce.manualinteraction.selectmi.ManualInteractionResult;
import com.gip.xyna.xprc.xpce.manualinteraction.selectmi.ManualInteractionSelect;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xpce.statustracking.IStatusChangeListener;
import com.gip.xyna.xprc.xprcods.capacitymapping.CapacityMappingStorable;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive.SearchMode;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchiveStatisticsStorable;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstance;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceColumn;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceDetails;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceResult;
import com.gip.xyna.xprc.xprcods.orderarchive.selectorder.OrderInstanceSelect;
import com.gip.xyna.xprc.xprcods.orderarchive.selectorder.OrderInstanceSelect.OrderByDesignators;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase.DeploymentStatus;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowInformation;
import com.gip.xyna.xprc.xsched.CapacityInformation;
import com.gip.xyna.xprc.xsched.CapacityManagement;
import com.gip.xyna.xprc.xsched.ExtendedCapacityUsageInformation;
import com.gip.xyna.xprc.xsched.VetoInformationStorable;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder.OnErrorAction;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrderInformation;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeScheduler;
import com.gip.xyna.xprc.xsched.cronlikescheduling.selectcrons.CronLikeOrderSearchResult;
import com.gip.xyna.xprc.xsched.cronlikescheduling.selectcrons.CronLikeOrderSelectImpl;
import com.gip.xyna.xprc.xsched.orderabortion.AbortionCause;
import com.gip.xyna.xprc.xsched.ordercancel.CancelBean;
import com.gip.xyna.xprc.xsched.ordercancel.KillStuckProcessBean;
import com.gip.xyna.xprc.xsched.orderseries.OrderSeriesManagementInformation;
import com.gip.xyna.xprc.xsched.orderseries.RescheduleSeriesOrderInformation;
import com.gip.xyna.xprc.xsched.scheduling.SchedulerInformationBean;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSearchResult;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSelectImpl;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeConstraintWindowDefinition;



public class XynaMultiChannelPortal extends XynaMultiChannelPortalBase {

  public static final String DEFAULT_NAME = "Xyna Multi-Channel Portal";
  
  public static final ThreadLocal<Identity> THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY = new ThreadLocal<XynaMultiChannelPortal.Identity>();
  
  public static class Identity {
    private String username;
    private String sessionid;
    public Identity(String username, String sessionid) {
      this.username = username;
      this.sessionid = sessionid;
    }
    public String getUsername() {
      return username;
    }
    @Override
    public String toString() {
      if (sessionid == null) {
        return username;
      } else if (username == null) {
        // TODO we might try to resolve it from the internal sessionManagmenet cache
        return sessionid;
      } else {
        return username + " - " + sessionid;
      }
    }
  }

  
  private Semaphore listCapacityInformationLock;
  private Semaphore listSchedulerInformationLock;
  private Semaphore listOrderSeriesManagementInformationLock;
  private Semaphore searchOrderArchiveLock;
  

  public XynaMultiChannelPortal() throws XynaException {
    super();
  }

  @Override
  public void init() throws XynaException {
    deploySection(new XGUISupport());
//    deploySection(new XMOMGui() );
    
    //defaultvalues bevor die future execution ausgeführt wird
    initLocks();
    
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    
    fExec.addTask(XynaMultiChannelPortal.class, "XynaMultiChannelPortal").
          after(RMIManagement.class, XynaProperty.class).
          after(IDGenerator.class).
          after(XynaProcessing.FUTUREEXECUTIONID_ORDER_EXECUTION).
          after(UserManagement.class).
          after(RemoteDestinationManagement.FUTUREEXECUTION_REMOTEDESTINATIONS_FULL_INIT).
          after(CronLikeScheduler.class).
          execAsync(new Runnable() { public void run() { initXMCP(); } } );
  }
  
  private void initXMCP() {
    XynaProperty.RMI_XMCP_PORT_COMMUNICATION.registerDependency(UserType.XynaFactory, DEFAULT_NAME );
    
    initLocks();
    
    deployRMIChannel();
  }
  
  
  private void initLocks() {
    listCapacityInformationLock = createLock(XynaProperty.CONCURRENCY_LISTCAPACITYINFO_CALLS);
    listSchedulerInformationLock = createLock(XynaProperty.CONCURRENCY_LISTSCHEDULERINFO_CALLS);
    listOrderSeriesManagementInformationLock = createLock(XynaProperty.CONCURRENCY_LISTORDERSERIESINFO_CALLS);
    searchOrderArchiveLock = createLock(XynaProperty.CONCURRENCY_SEARCHORDERARCHIVE_CALLS);
  }
  
  
  private Semaphore createLock(XynaPropertyInt xynaProperty) {
    xynaProperty.registerDependency(UserType.XynaFactory, DEFAULT_NAME);
    return new Semaphore(xynaProperty.readOnlyOnce());
  }

  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  private XynaProcessingPortal getXynaProcessingPortal() {
    return XynaFactory.getPortalInstance().getProcessingPortal();
  }


  private XynaFactoryManagementPortal getXynaFactoryManagementPortal() {
    return XynaFactory.getPortalInstance().getFactoryManagementPortal();
  }


  private XynaActivationPortal getXynaActivationPortal() {
    return XynaFactory.getPortalInstance().getActivationPortal();
  }


  private XynaFactoryWarehousePortal getXynaNetworkWarehousePortal() {
    return XynaFactory.getPortalInstance().getXynaNetworkWarehousePortal();
  }

  private class RMIImplFactoryXMCP implements RMIImplFactory<RMIChannelImplSessionExtension> {

    public String getFQClassName() {
      return RMIChannelImplSessionExtension.class.getName();
    }

    public void init(InitializableRemoteInterface rmiImpl) {
      rmiImpl.init(new Object[]{});
      deploySection((Section)rmiImpl);
    }
    
    public void shutdown(InitializableRemoteInterface rmiImpl) {
      undeploySection((Section)rmiImpl);
    }

  }


  private void deployRMIChannel() {
    try {
      initRMI();
    } catch (XMCP_RMI_BINDING_ERROR e) {
      logger.warn("Failed to initialize RMI", e);
      XynaExtendedStatusManagement
          .addFurtherInformationAtStartup(XynaMultiChannelPortal.DEFAULT_NAME,
                                          "Failed to export RMI Channel. Please see log file for further information.");
    }
  }
  
  public void unregisterRMIchannel() {
    RMIManagement rmiManagement =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRMIManagement();
    rmiManagement.unregisterRemoteInterface(XynaRMIChannelBase.RMI_NAME);
  }


  private void initRMI() throws XMCP_RMI_BINDING_ERROR {
    RMIManagement rmiManagement =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRMIManagement();
    rmiManagement.registerClassreloadableRMIImplFactory(
        new RMIImplFactoryXMCP(), XynaRMIChannelBase.RMI_NAME,
        XynaProperty.RMI_XMCP_PORT_COMMUNICATION.get(), true);
  }

  // +++ cross domain xml string +++

  public String getCrossDomainXML() {
    String crossDomainXML = getXynaFactoryManagementPortal().getProperty(XynaProperty.CROSS_DOMAIN_XML_PROPERTY);
    if (crossDomainXML != null) {
      return crossDomainXML;
      // "<?xml version=\"1.0\"?><cross-domain-policy><allow-access-from domain=\"*\" to-ports=\"*\" /></cross-domain-policy>";
    }
    else {
      return "";
    }
  }


  /**
   * Returns a minimal crossdomain XML file that allows access from the provided IP address and/or the provided hostname
   * and port(s)
   */
  public String getMinimalCrossDomainXML(String ip, String hostname, String port) {

    String result = "<?xml version=\"1.0\"?>";
    if (hostname != null)
      result += "<cross-domain-policy><allow-access-from domain=\"" + hostname + "\" to-ports=\"" + port + "\" />";
    if (ip != null)
      result += "<allow-access-from domain=\"" + ip + "\" to-ports=\"" + port + "\" />";

    result += "</cross-domain-policy>";
    return result;

  }


  // +++ MDM +++

  @AccessControlled(associatedRight = Rights.DEPLOYMENT_MDM)
  @Deprecated
  public void deployWF(String fqClassName, WorkflowProtectionMode mode)
      throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException,
      XPRC_DeploymentDuringUndeploymentException, XPRC_InheritedConcurrentDeploymentException,
      XPRC_MDMDeploymentException {
    deployWF(fqClassName, mode, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }
  
  @AccessControlled(associatedRight = Rights.DEPLOYMENT_MDM)
  public void deployWF(String fqClassName, WorkflowProtectionMode mode, Long revision)
      throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException,
      XPRC_DeploymentDuringUndeploymentException, XPRC_InheritedConcurrentDeploymentException,
      XPRC_MDMDeploymentException {
    XynaFactory.getInstance().getProcessing().getWorkflowEngine().deployWorkflow(fqClassName, mode, revision);
  }


  @AccessControlled(associatedRight = Rights.DEPLOYMENT_MDM)
  public void undeployWF(String originalFqName, boolean disableChecks) throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH,
      XPRC_InvalidPackageNameException, XPRC_InternalObjectMayNotBeUndeployedException, XPRC_MDMUndeploymentException,
      XPRC_ExclusiveDeploymentInProgress, XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT {
    XynaFactory.getInstance().getProcessing().getWorkflowEngine().undeployWorkflow(originalFqName, false, disableChecks);
  }


  @AccessControlled(associatedRight = Rights.EDIT_MDM)
  @Deprecated
  public String saveMDM(String xml) throws XPRC_XmlParsingException, XMCP_InternalObjectModifiedException,
      Ex_FileAccessException, CodeGroupUnknownException, NoCodeAvailableException, PersistenceLayerException {
    try {
      return saveMDM(xml, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }
  }

  @AccessControlled(associatedRight = Rights.EDIT_MDM)
  public String saveMDM(String xml, Long revision) throws XPRC_XmlParsingException, XMCP_InternalObjectModifiedException,
  Ex_FileAccessException, CodeGroupUnknownException, NoCodeAvailableException, PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    return saveMDM(xml, revision, null);
  }
  
  @AccessControlled(associatedRight = Rights.EDIT_MDM)
  public String saveMDM(String xml, Long revision, RepositoryEvent repositoryEvent) throws XPRC_XmlParsingException, XMCP_InternalObjectModifiedException,
    Ex_FileAccessException, CodeGroupUnknownException, NoCodeAvailableException, PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    return saveMDM(xml, revision, repositoryEvent, false);
  }

  public String saveMDM(String xml, Long revision, RepositoryEvent repositoryEvent, boolean allowSavingOfReservedServerObject) throws XPRC_XmlParsingException, XMCP_InternalObjectModifiedException,
    Ex_FileAccessException, CodeGroupUnknownException, NoCodeAvailableException, PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    return saveMDM(xml, revision, repositoryEvent, allowSavingOfReservedServerObject, false);
  }
  
  public String saveMDM(String xml, Long revision, RepositoryEvent repositoryEvent, boolean allowSavingOfReservedServerObject, boolean preserveLastModified) throws XPRC_XmlParsingException, XMCP_InternalObjectModifiedException,
    Ex_FileAccessException, CodeGroupUnknownException, NoCodeAvailableException, PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {

    Document doc = XMLUtils.parseString(xml);
    Element root = doc.getDocumentElement();
    String rootTagName = root.getTagName();

    String fqNameFromXML = GenerationBase.getFqXMLName(doc);

    if (!allowSavingOfReservedServerObject && GenerationBase.isReservedServerObjectByFqOriginalName(fqNameFromXML)) {
      throw new XMCP_InternalObjectModifiedException(fqNameFromXML);
    }

    File file = new File(GenerationBase.getFileLocationOfXmlNameForSaving(fqNameFromXML, revision) + ".xml");

    if (rootTagName.equals(GenerationBase.EL.EXCEPTIONSTORAGE)) {
      //überprüfung, ob in gespeichertem xml der exceptioncode bereits gesetzt wurde und ihn dann ggfs
      //in das xml einfügen.
      //falls nicht, muss das exceptionmanagement einen code generieren.
      if (file.exists()) {
        Document oldDoc = XMLUtils.parse(file.getAbsolutePath());
        XPath xpath = XPathFactory.newInstance().newXPath();
        try {
          String xpathType = "/" + GenerationBase.EL.EXCEPTIONSTORAGE + "/" + GenerationBase.EL.EXCEPTIONTYPE;
          String oldCode = xpath.evaluate(xpathType + "/@" + GenerationBase.ATT.EXCEPTION_CODE, oldDoc);
          if (oldCode == null || oldCode.length() == 0) {
            XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getExceptionManagement().checkExceptionCode(doc);
          } else {
            Element exceptionTypeElement = (Element) xpath.evaluate(xpathType, doc, XPathConstants.NODE);
            exceptionTypeElement.setAttribute(GenerationBase.ATT.EXCEPTION_CODE, oldCode);
          }
        } catch (XPathExpressionException e) {
          throw new RuntimeException("could not evaluate xpath in file " + file.getAbsolutePath(), e);
        }
      } else {
        XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getExceptionManagement().checkExceptionCode(doc);
      }
    }

    Long previousLastModified = file.lastModified();
    XMLUtils.saveDom(file, doc);
    if (preserveLastModified) {
      file.setLastModified(previousLastModified);
    }

    XMOMType type = XMOMType.getXMOMTypeByRootTag(rootTagName);
    
    if (type == XMOMType.WORKFLOW) {
      registerSavedWorkflow(fqNameFromXML, revision);
    }
    
    try {
      DeploymentItemStateManagement dism = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement();
      dism.save(DeploymentItemBuilder.build(fqNameFromXML, Optional.of(type), true, revision).get(), revision);
    } catch (Exception e) {
      logger.warn("Failed to build item from saved XML for " + fqNameFromXML,e);
    }
    
    try {
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getXMOMDatabase()
                      .registerMOMObject(fqNameFromXML, rootTagName, revision);
    } catch (AssumedDeadlockException e) {
      logger.warn("Error while trying to register XMOMObject, skipping",e);
    }
    
    // TODO kind of the wrong place to determine this as it depends on the codeAccess algorithm modifying the saved xml
    boolean executeSync = false; //only if there are operations but no library tag
    if (rootTagName.equals(GenerationBase.EL.DATATYPE)) {
      try {
        String expectedLibName = GenerationBase.getSimpleNameFromFQName(GenerationBase.transformNameForJava(fqNameFromXML)) + "Impl.jar";
        boolean usesImplLib = false;
        List<Element> libraries = XMLUtils.getChildElementsRecursively(doc.getDocumentElement(), GenerationBase.EL.LIBRARIES);
        for (Element library : libraries) {
          String libName = XMLUtils.getTextContent(library);
          if (expectedLibName.equals(libName)) {
            usesImplLib = true;
            break;
          }
        }
        if (!usesImplLib) {
          List<Element> operations = XMLUtils.getChildElementsRecursively(doc.getDocumentElement(), GenerationBase.EL.OPERATION);
          executeSync = operations.size() > 0;
        }
      } catch (XPRC_InvalidPackageNameException e) {
        throw new RuntimeException(e);
      }
    }
    
    if (repositoryEvent == null) {
      repositoryEvent = new SingleRepositoryEvent(revision);
    }
    repositoryEvent.setExecuteSynchron(executeSync);
    repositoryEvent.addEvent(new ProjectCreationOrChangeProvider.XMOMChangeEvent(fqNameFromXML, type));
    return fqNameFromXML;
  }
  
  
  @AccessControlled(associatedRight = Rights.EDIT_MDM)
  @Deprecated
  public String saveMDM(String xml, boolean override, String user, String sessionId) throws XynaException {
    return saveMDM(xml, override, user, sessionId, RevisionManagement.REVISION_DEFAULT_WORKSPACE, null);
  }
  
  @AccessControlled(associatedRight = Rights.EDIT_MDM)
  public String saveMDM(String xml, boolean override, String user, String sessionId, Long revision, RepositoryEvent repositoryEvent) throws XynaException {
    return saveMDM(xml, override, user, sessionId, revision, repositoryEvent, false);
  }

  @AccessControlled(associatedRight = Rights.EDIT_MDM)
  public String saveMDM(String xml, boolean override, String user, String sessionId, Long revision, RepositoryEvent repositoryEvent,
                        boolean allowSavingOfReservedServerObject) throws XynaException {
    return saveMDM(xml, override, user, sessionId, revision, repositoryEvent, allowSavingOfReservedServerObject, false);
  }
  
  @AccessControlled(associatedRight = Rights.EDIT_MDM)
  public String saveMDM(String xml, boolean override, String user, String sessionId, Long revision, RepositoryEvent repositoryEvent, boolean allowSavingOfReservedServerObject, boolean preserverLastModified) throws XynaException {
    Document doc = XMLUtils.parseString(xml);
    Element root = doc.getDocumentElement();
    String rootTagName = root.getTagName();

    String fqNameFromXML = GenerationBase.getFqXMLName(doc);
    
    if (!override) {
      File destination= new File(GenerationBase.getFileLocationOfXmlNameForSaving(fqNameFromXML, revision) + ".xml");
      if (destination.exists()) {
        throw new XPRC_FileExistsException();
      }
    }
    XMOMType type = XMOMType.getXMOMTypeByRootTag(rootTagName);
    Path path = new Path(fqNameFromXML, revision);
    if (XynaFactory.getInstance().getXynaDevelopment().getXynaFractalModelling().getLockManagement().lockXMOM(sessionId, user, path, type.getNiceName())) {
      try {
        String result;
        THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.set(new Identity(user, sessionId));
        try {
          result = saveMDM(xml, revision, repositoryEvent, allowSavingOfReservedServerObject, preserverLastModified);
        } finally {
          THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.remove();
        }
        // xmom saves in the datamodel revision deliver too few informations for the gui to handle it ()
        if (!RevisionManagement.REVISION_DATAMODEL.equals(revision)) {
          XynaFactory.getInstance().getXynaDevelopment().getXynaFractalModelling().getLockManagement()
                     .propagateXMOMSave(sessionId, user, path, type.getNiceName(), xml);
        }
        return result;
      } finally {
        XynaFactory.getInstance().getXynaDevelopment().getXynaFractalModelling().getLockManagement().unlockXMOM(sessionId, user, path, type.getNiceName());
      }
    } else {
      throw new RuntimeException(type.getNiceName() + " " + fqNameFromXML + " is locked by another user.");
    }
  }


  @AccessControlled(associatedRight = Rights.EDIT_MDM)
  public void unsecureDeleteSavedMDM(String fqNameFromXml) throws Ex_FileAccessException, XPRC_XmlParsingException {

    String fileLocation = GenerationBase.getFileLocationOfXmlNameForSaving(fqNameFromXml) + ".xml";

    File f = new File(fileLocation);
    if (f.exists()) {
      try {
        Document doc = XMLUtils.parse(fileLocation);
        Element root = doc.getDocumentElement();
        if (root.getTagName().equals(GenerationBase.EL.SERVICE)) {
          unregisterSavedWorkflow(fqNameFromXml);
        }
      } finally {
        logger.debug("removing temporary file: " + f.getAbsolutePath());
        f.delete();
      }
    } else {
      logger.debug("could not remove temporary file, does not exist: " + f.getAbsolutePath());
    }

  }


  @AccessControlled(associatedRight = Rights.EDIT_MDM)
  public void unregisterSavedWorkflow(String fqNameFromXml) {
    getXynaProcessingPortal().unregisterSavedWorkflow(fqNameFromXml);
  }


  @AccessControlled(associatedRight = Rights.EDIT_MDM)
  @Deprecated
  public void registerSavedWorkflow(String fqNameFromXml) {
    registerSavedWorkflow(fqNameFromXml, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }

  @AccessControlled(associatedRight = Rights.EDIT_MDM)
  public void registerSavedWorkflow(String fqNameFromXml, Long revision) {
    getXynaProcessingPortal().registerSavedWorkflow(fqNameFromXml, revision);
  }

  @AccessControlled(associatedRight = Rights.DEPLOYMENT_MDM)
  public void deployMultiple(Map<XMOMType, List<String>> deploymentItems, WorkflowProtectionMode mode, Long revision) 
                  throws MDMParallelDeploymentException, XPRC_DeploymentDuringUndeploymentException,
                  XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException {
    XynaFactory.getInstance().getProcessing().getWorkflowEngine().deployMultiple(deploymentItems, mode, revision);
  }

  @AccessControlled(associatedRight = Rights.DEPLOYMENT_MDM)
  public void deployMDM(String fqXMLName, WorkflowProtectionMode mode, String fileName, InputStream inputStream) throws XACT_JarFileUnzipProblem, XPRC_UNSUPPORTED_FILE_EXTENSION_DEPLOYMENT, XPRC_InvalidPackageNameException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_DeploymentDuringUndeploymentException, XPRC_InheritedConcurrentDeploymentException, XPRC_MDMDeploymentException, Ex_FileAccessException {
    XynaFactory.getInstance().getProcessing().getWorkflowEngine().deployDatatype(fqXMLName, mode, fileName, inputStream);
  }

  @AccessControlled(associatedRight = Rights.DEPLOYMENT_MDM)
  @Deprecated
  public void deployDatatype(String fqXmlName, WorkflowProtectionMode mode, Map<String, InputStream> jars) throws Ex_FileAccessException, XPRC_InvalidPackageNameException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_DeploymentDuringUndeploymentException, XPRC_InheritedConcurrentDeploymentException, XPRC_MDMDeploymentException {
    deployDatatype(fqXmlName, mode, jars, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }

  @AccessControlled(associatedRight = Rights.DEPLOYMENT_MDM)
  public void deployDatatype(String fqXmlName, WorkflowProtectionMode mode, Map<String, InputStream> jars, Long revision) throws Ex_FileAccessException, XPRC_InvalidPackageNameException, XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_DeploymentDuringUndeploymentException, XPRC_InheritedConcurrentDeploymentException, XPRC_MDMDeploymentException {
    XynaFactory.getInstance().getProcessing().getWorkflowEngine().deployDatatype(fqXmlName, mode, jars, revision);
  }


  @AccessControlled(associatedRight = Rights.DEPLOYMENT_MDM)
  @Deprecated
  public void undeployMDM(String fqXmlName, boolean undeployDependendObjects, boolean disableChecks) throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InternalObjectMayNotBeUndeployedException, XPRC_MDMUndeploymentException, XPRC_ExclusiveDeploymentInProgress, XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT {
    undeployMDM(fqXmlName, undeployDependendObjects, disableChecks, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }

  @AccessControlled(associatedRight = Rights.DEPLOYMENT_MDM)
  public void undeployMDM(String fqXmlName, boolean undeployDependendObjects, boolean disableChecks, Long revision) throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InternalObjectMayNotBeUndeployedException, XPRC_MDMUndeploymentException, XPRC_ExclusiveDeploymentInProgress, XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT {
    XynaFactory.getInstance().getProcessing().getWorkflowEngine().undeployDatatype(fqXmlName, undeployDependendObjects, disableChecks, revision);
  }

  @AccessControlled(associatedRight = Rights.DEPLOYMENT_MDM)
  @Deprecated
  public void deployException(String fqXmlName, WorkflowProtectionMode mode) throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_DeploymentDuringUndeploymentException, XPRC_InheritedConcurrentDeploymentException, XPRC_MDMDeploymentException{
    deployException(fqXmlName, mode, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }

  @AccessControlled(associatedRight = Rights.DEPLOYMENT_MDM)
  public void deployException(String fqXmlName, WorkflowProtectionMode mode, Long revision) throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_DeploymentDuringUndeploymentException, XPRC_InheritedConcurrentDeploymentException, XPRC_MDMDeploymentException{
    XynaFactory.getInstance().getProcessing().getWorkflowEngine().deployException(fqXmlName, mode, revision);
  }


  @AccessControlled(associatedRight = Rights.DEPLOYMENT_MDM)
  public void undeployException(String originalFqName, boolean disableChecks) throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InternalObjectMayNotBeUndeployedException, XPRC_MDMUndeploymentException, XPRC_ExclusiveDeploymentInProgress, XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT {
    XynaFactory.getInstance().getProcessing().getWorkflowEngine().undeployException(originalFqName, false, disableChecks);
  }


  @AccessControlled(associatedRight = Rights.DEPLOYMENT_MDM)
  @Deprecated
  public void undeployException(String originalFqName, boolean undeployDependendObjects, boolean disableChecks) throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InternalObjectMayNotBeUndeployedException, XPRC_MDMUndeploymentException, XPRC_ExclusiveDeploymentInProgress, XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT {
    undeployException(originalFqName, undeployDependendObjects, disableChecks, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }

  @AccessControlled(associatedRight = Rights.DEPLOYMENT_MDM)
  public void undeployException(String originalFqName, boolean undeployDependendObjects, boolean disableChecks, Long revision) throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException, XPRC_InternalObjectMayNotBeUndeployedException, XPRC_MDMUndeploymentException, XPRC_ExclusiveDeploymentInProgress, XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT {
    XynaFactory.getInstance().getProcessing().getWorkflowEngine().undeployException(originalFqName, undeployDependendObjects, disableChecks, revision);
  }

  // +++ Xyna Activation +++


  /**
   * Publish a new trigger implementation
   */
  @AccessControlled(associatedRight = Rights.TRIGGER_FILTER_MANAGEMENT)
  public void addTrigger(String name, ZipInputStream jarFiles, String fqTriggerClassName, String[] sharedLibs,
                         String description, String startParameterDocumentation, long revision)
      throws XynaException {
    getXynaActivationPortal().addTrigger(name, jarFiles, fqTriggerClassName, sharedLibs, description,
                                         startParameterDocumentation, revision);
  }


  /**
   * Remove an undeployed trigger
   */
  @AccessControlled(associatedRight = Rights.TRIGGER_FILTER_MANAGEMENT)
  public void removeTrigger(String nameOfTrigger) throws XACT_TriggerNotFound, XACT_TriggerMayNotBeRemovedIsDeployedException, PersistenceLayerException {
    getXynaActivationPortal().removeTrigger(nameOfTrigger);
  }

  @AccessControlled(associatedRight = Rights.TRIGGER_FILTER_MANAGEMENT)
  public void removeTrigger(String nameOfTrigger, Long revision) throws XACT_TriggerNotFound, XACT_TriggerMayNotBeRemovedIsDeployedException, PersistenceLayerException {
    getXynaActivationPortal().removeTrigger(nameOfTrigger, revision);
  }


  /**
   * Start trigger instance with specific parameters
   */
  @AccessControlled(associatedRight = Rights.TRIGGER_FILTER_MANAGEMENT)
  public void deployTrigger(String nameOfTrigger, String nameOfTriggerInstance, String[] startParameter,
                            String description, long revision) throws XACT_IncompatibleTriggerImplException,
      XACT_TriggerImplClassNotFoundException, XACT_TriggerNotFound, XACT_InvalidStartParameterException,
      PersistenceLayerException, XFMG_SHARED_LIB_NOT_FOUND, XACT_LibOfTriggerImplNotFoundException,
      XACT_TriggerCouldNotBeStartedException,
      XACT_AdditionalDependencyDeploymentException, XACT_TriggerInstanceNeedsEnabledTriggerException {
    getXynaActivationPortal().deployTrigger(nameOfTrigger, nameOfTriggerInstance, startParameter, description, revision);
  }


  /**
   * Stop trigger instance
   */
  @AccessControlled(associatedRight = Rights.TRIGGER_FILTER_MANAGEMENT)
  public void undeployTrigger(String nameOfTrigger, String nameOfTriggerInstance) throws XACT_TriggerNotFound,
      PersistenceLayerException, XACT_TriggerInstanceNotFound {
    getXynaActivationPortal().undeployTrigger(nameOfTrigger, nameOfTriggerInstance);

  }


  /**
   * Publish a new filter implementation
   */
  @AccessControlled(associatedRight = Rights.TRIGGER_FILTER_MANAGEMENT)
  public void addFilter(String filterName, ZipInputStream jarFiles, String fqFilterClassName, String triggerName, String[] sharedLibs,
                        String description, long revision) throws Ex_FileAccessException, XACT_JarFileUnzipProblem, XACT_TriggerNotFound,
      PersistenceLayerException, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_InvalidXmlMissingRequiredElementException,
      XACT_FilterImplClassNotFoundException, XACT_IncompatibleFilterImplException, XFMG_SHARED_LIB_NOT_FOUND,
      XACT_LibOfFilterImplNotFoundException, XACT_AdditionalDependencyDeploymentException, XPRC_ExclusiveDeploymentInProgress,
      XACT_OldFilterVersionInstantiationException {
    getXynaActivationPortal().addFilter(filterName, jarFiles, fqFilterClassName, triggerName, sharedLibs, description, revision);
  }


  /**
   * Remove an undeployed filter
   */
  @AccessControlled(associatedRight = Rights.TRIGGER_FILTER_MANAGEMENT)
  public void removeFilter(String nameOfFilter) throws XACT_FilterNotFound, XACT_FilterMayNotBeRemovedIsDeployedException, PersistenceLayerException {
    getXynaActivationPortal().removeFilter(nameOfFilter);
  }

  @AccessControlled(associatedRight = Rights.TRIGGER_FILTER_MANAGEMENT)
  public void removeFilter(String nameOfFilter, Long revision) throws XACT_FilterNotFound, XACT_FilterMayNotBeRemovedIsDeployedException, PersistenceLayerException {
    getXynaActivationPortal().removeFilter(nameOfFilter, revision);
  }
  
  @AccessControlled(associatedRight = Rights.TRIGGER_FILTER_MANAGEMENT)
  public void removeFilterWithUndeployingInstances(String nameOfFilter) throws XACT_FilterNotFound, PersistenceLayerException {
    getXynaActivationPortal().removeFilterWithUndeployingInstances(nameOfFilter);
  }


  /**
   * Start filter instance at specific trigger instance
   * @throws XACT_InvalidFilterConfigurationParameterValueException 
   */
  @AccessControlled(associatedRight = Rights.TRIGGER_FILTER_MANAGEMENT)
  public void deployFilter(DeployFilterParameter deployFilterParameter)
      throws XACT_FilterImplClassNotFoundException, XACT_IncompatibleFilterImplException, XACT_FilterNotFound, PersistenceLayerException,
      XFMG_SHARED_LIB_NOT_FOUND, XACT_LibOfFilterImplNotFoundException, XACT_InvalidFilterConfigurationParameterValueException {
    getXynaActivationPortal().deployFilter(deployFilterParameter);
  }
  /**
   * Start filter instance at specific trigger instance
   */
  @Deprecated
  @AccessControlled(associatedRight = Rights.TRIGGER_FILTER_MANAGEMENT)
  public void deployFilter(String filtername, String nameOfFilterInstance, String nameOfTriggerInstance, String description, long revision)
      throws XACT_FilterImplClassNotFoundException, XACT_IncompatibleFilterImplException, XACT_FilterNotFound, PersistenceLayerException,
      XFMG_SHARED_LIB_NOT_FOUND, XACT_LibOfFilterImplNotFoundException {
    getXynaActivationPortal().deployFilter(filtername, nameOfFilterInstance, nameOfTriggerInstance, description, revision);
  }


  /**
   * Stop filter instance at specific trigger instance
   */
  @AccessControlled(associatedRight = Rights.TRIGGER_FILTER_MANAGEMENT)
  public void undeployFilter(String filterName) throws XACT_FilterNotFound, PersistenceLayerException {
    getXynaActivationPortal().undeployFilter(filterName);
  }
  
  
  @AccessControlled(associatedRight = Rights.TRIGGER_FILTER_MANAGEMENT)
  public boolean disableTriggerInstance(String triggerInstanceName) throws PersistenceLayerException,
      XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, XACT_TriggerNotFound, XACT_TriggerInstanceNotFound {
    return getXynaActivationPortal().disableTriggerInstance(triggerInstanceName);
  }


  @AccessControlled(associatedRight = Rights.TRIGGER_FILTER_MANAGEMENT)
  public boolean enableTriggerInstance(String triggerInstanceName) throws PersistenceLayerException,
      XACT_IncompatibleTriggerImplException,
      XACT_TriggerImplClassNotFoundException, XACT_TriggerNotFound, XACT_InvalidStartParameterException,
      XFMG_SHARED_LIB_NOT_FOUND, XACT_LibOfTriggerImplNotFoundException, 
      XACT_TriggerCouldNotBeStartedException, XACT_AdditionalDependencyDeploymentException,
      XACT_TriggerInstanceNeedsEnabledTriggerException, XACT_TriggerInstanceNotFound {
    return getXynaActivationPortal().enableTriggerInstance(triggerInstanceName);
  }


  public boolean enableFilterInstance(String filterInstanceName) throws PersistenceLayerException,
      XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, XACT_FilterImplClassNotFoundException,
      XACT_IncompatibleFilterImplException, XACT_FilterNotFound, XACT_TriggerInstanceNotFound,
      XFMG_SHARED_LIB_NOT_FOUND, XACT_LibOfFilterImplNotFoundException, 
      XACT_FilterInstanceNeedsEnabledFilterException {
    return getXynaActivationPortal().enableFilterInstance(filterInstanceName);
  }


  /**
   * Registers an {@link IStatusChangeListener} that is notified when a process instance belonging to one of the watched
   * order types.
   *
   * The listener is automatically removed if the the provided class is undeployed. The class should thus be the one that
   * calls this method so that the listener is not duplicated once the class is redeployed.
   */
  public void addStatusChangeListener(ClassProvider cp, IStatusChangeListener listener) {
    XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getStatusChangeProvider()
                    .addStatusChangeListener(cp, listener);
  }


  /**
   * Removes an {@link IStatusChangeListener} that was previously registered.
   */
  public void removeStatusChangeListener(IStatusChangeListener listener) {
    XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getStatusChangeProvider()
                    .removeStatusChangeListener(listener);
  }


  // +++ Implementation templates

  @Deprecated
  public InputStream getServiceImplTemplate(String baseDir, String fqXMLNameDOM,
                                            boolean deleteServiceImplAfterStreamClose, boolean deleteBaseDir)
      throws XynaException {
    return getServiceImplTemplate(baseDir, fqXMLNameDOM, RevisionManagement.REVISION_DEFAULT_WORKSPACE, deleteServiceImplAfterStreamClose, deleteBaseDir);
  }
  
  
  public InputStream getServiceImplTemplate(String baseDir, String fqXMLNameDOM, Long revision,
                                            boolean deleteServiceImplAfterStreamClose, boolean deleteBaseDir)
      throws XynaException {

    // first deploy the datatype (and thus its dependencies) since only deployed classes are included in the built jar
    if (logger.isDebugEnabled()) {
      logger.debug("deploying " + fqXMLNameDOM + " due to service implementation template request");
    }
    String xmlfileLocation = GenerationBase.getFileLocationOfXmlNameForSaving(fqXMLNameDOM, revision);

    // get an instance of the object to be deployed and keep the parsed object for later
    DOM d = DOM.getInstance(fqXMLNameDOM, revision);

    // deploy
    deployDatatype(fqXMLNameDOM, WorkflowProtectionMode.FORCE_DEPLOYMENT, null, revision);

    logger.debug("finished deployment, building service implementation template");
    File tempdir = new File(baseDir);
    try {
      ServiceImplementationTemplate paras = new ServiceImplementationTemplate(fqXMLNameDOM, revision);
      Support4Eclipse.buildProjectTemplate(tempdir, paras);

      File xmlFileTarget =
          new File(tempdir.getAbsolutePath(), paras.getXMLDefinitionFolder() + File.separator
              + new File(xmlfileLocation).getName() + ".xml");
      Document doc = XMLUtils.parse(xmlFileTarget);
      DOM.addLibraryTagAndCodeSnippetInXML(doc, d, false, false);
      XMLUtils.saveDom(xmlFileTarget, doc);

      
      File f = new File(fqXMLNameDOM + "_" + getDateSuffix() + ".zip");
      while (f.exists()) {
        f = new File(fqXMLNameDOM + "_" + getDateSuffix() + ".zip");
      }

      return XynaFactory.getInstance().getXynaDevelopment().getXynaLibraryDevelopment()
          .getEclipseProjectTemplateFileProvider()
          .getHandledInputStreamFromFile(f, tempdir, deleteServiceImplAfterStreamClose);

    } finally {
      if (tempdir.exists()) {
        if (deleteBaseDir) {
          if (!FileUtils.deleteDirectory(tempdir)) {
            logger.warn("could not delete directory " + tempdir + ".");
          }
        }
      }
    }

  }

  @Deprecated
  public InputStream getServiceImplTemplate(String fqClassNameDOM, boolean deleteServiceImplAfterStreamClose)
      throws XynaException {
    return getServiceImplTemplate(getTempDir(), fqClassNameDOM, deleteServiceImplAfterStreamClose, true);
  }

  public InputStream getServiceImplTemplate(String fqClassNameDOM, Long revision, boolean deleteServiceImplAfterStreamClose)
                  throws XynaException {
    return getServiceImplTemplate(getTempDir(), fqClassNameDOM, revision, deleteServiceImplAfterStreamClose, true);
  }


  public InputStream getPythonServiceImplTemplate(String fqClassNameDOM, Long revision, boolean deleteServiceImplAfterStreamClose)
      throws XynaException {
    return XynaFactory.getInstance().getProcessing().getXynaPythonSnippetManagement()
        .getPythonServiceImplTemplate(getTempDir(), fqClassNameDOM, revision, deleteServiceImplAfterStreamClose);
  }


  @Deprecated
  public InputStream getTriggerImplTemplate(String baseDir, String triggerName,
                                            boolean deleteTriggerImplAfterStreamClose, boolean deleteBaseDir)
      throws XynaException {
    return getTriggerImplTemplate(baseDir, triggerName, RevisionManagement.REVISION_DEFAULT_WORKSPACE, deleteTriggerImplAfterStreamClose, deleteBaseDir);
  }
  
  public InputStream getTriggerImplTemplate(String baseDir, String triggerName, Long revision,
                                            boolean deleteTriggerImplAfterStreamClose, boolean deleteBaseDir)
      throws XynaException {

    InputStream result = null;

    File tempdir = new File(baseDir);
    try {
      Support4Eclipse.buildProjectTemplate(tempdir, new TriggerImplementationTemplate(triggerName, revision));
      File f = new File(triggerName + "_" + getDateSuffix() + ".zip");
      while (f.exists()) {
        f = new File(triggerName + "_" + getDateSuffix() + ".zip");
      }

      result =
          XynaFactory.getInstance().getXynaDevelopment().getXynaLibraryDevelopment()
              .getEclipseProjectTemplateFileProvider()
              .getHandledInputStreamFromFile(f, tempdir, deleteTriggerImplAfterStreamClose);

    } finally {
      if (tempdir.exists()) {
        if (deleteBaseDir) {
          if (!FileUtils.deleteDirectory(tempdir)) {
            logger.warn("could not delete directory " + tempdir + ".");
          }
        }
      }
    }

    return result;

  }


  public InputStream getTriggerImplTemplate(String triggerName, boolean deleteTriggerImplAfterStreamClose)
      throws XynaException {
    return getTriggerImplTemplate(getTempDir(), triggerName, deleteTriggerImplAfterStreamClose, true);
  }

  @Deprecated
  public InputStream getFilterImplTemplate(String baseDir, String filterName, String triggerName,
                                           boolean deleteFilterImplAfterStreamClose, boolean deleteBaseDir)
      throws XynaException {
    return getFilterImplTemplate(baseDir, filterName, triggerName, RevisionManagement.REVISION_DEFAULT_WORKSPACE, deleteFilterImplAfterStreamClose, deleteBaseDir);
  }
  
  public InputStream getFilterImplTemplate(String baseDir, String filterName, String triggerName, Long revision,
                                           boolean deleteFilterImplAfterStreamClose, boolean deleteBaseDir)
      throws XynaException {

    InputStream result = null;

    File tempdir = new File(baseDir);
    try {
      Support4Eclipse.buildProjectTemplate(tempdir, new FilterImplementationTemplate(filterName, triggerName, revision));
      File f = new File(filterName + "_" + getDateSuffix() + ".zip");
      int cnt = 0;
      while (f.exists()) {
        f = new File(filterName + "_" + getDateSuffix() + "_" + cnt + ".zip");
        cnt++;
      }

      result =
          XynaFactory.getInstance().getXynaDevelopment().getXynaLibraryDevelopment()
              .getEclipseProjectTemplateFileProvider()
              .getHandledInputStreamFromFile(f, tempdir, deleteFilterImplAfterStreamClose);

    } finally {
      if (tempdir.exists()) {
        if (deleteBaseDir) {
          if (!FileUtils.deleteDirectory(tempdir)) {
            logger.warn("could not delete directory " + tempdir + ".");
          }
        }
      }
    }

    return result;

  }

  public InputStream getFilterImplTemplate(String filterName, String fqTriggerClassName, boolean deleteFilterImplAfterStreamClose) throws XynaException {
    return getFilterImplTemplate(getTempDir(), filterName, fqTriggerClassName, deleteFilterImplAfterStreamClose, true);
  }

  /**
   * garantiert eindeutigkeit, erstellt das verzeichnis
   */
  private synchronized String getTempDir() {
    File candidate = new File(XynaProperty.TMP_DIR.get(), "tempdir" + new Random().nextInt(10000));
    while(candidate.exists()) {
      candidate = new File(XynaProperty.TMP_DIR.get(), "tempdir" + new Random().nextInt(10000));
    }
    candidate.mkdirs();
    return candidate.getPath();
  }

  private String getDateSuffix() {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
    return sdf.format(new Date());
  }

  // +++ Xyna Processing +++


  /**
   * Remove a cron like order
   * @throws XPRC_CronRemovalException
   * @throws XPRC_CronLikeOrderStorageException
   */
  public boolean removeCronLikeOrder(Long id) throws XPRC_CronLikeOrderStorageException, XPRC_CronRemovalException {
    return getXynaProcessingPortal().removeCronLikeOrder(id);
  }


  /**
   * Add a new cron like order
   * @throws XPRC_CronLikeSchedulerException
   */
  public CronLikeOrder startCronLikeOrder(CronLikeOrderCreationParameter clocp) throws XPRC_CronLikeSchedulerException {
    return getXynaProcessingPortal().startCronLikeOrder(clocp);
  }

  public CronLikeOrder modifyCronLikeOrder(Long id, String label, DestinationKey destination,
                                           GeneralXynaObject payload, Long firstStartupTime, String timeZoneID,
                                           Long interval, Boolean useDST, Boolean enabled, OnErrorAction onError,
                                           String cloCustom0, String cloCustom1, String cloCustom2, String cloCustom3)
      throws XPRC_CronLikeSchedulerException, XPRC_InvalidCronLikeOrderParametersException {
    return getXynaProcessingPortal().modifyCronLikeOrder(id, label, destination, payload, firstStartupTime, timeZoneID,
                                                         interval, useDST, enabled, onError, cloCustom0, cloCustom1,
                                                         cloCustom2, cloCustom3);
  }

  public CronLikeOrder modifyCronLikeOrder(Long id, String label, DestinationKey destination,
                                           GeneralXynaObject payload, Calendar firstStartupTimeWithTimeZone,
                                           Long interval, Boolean useDST, Boolean enabled, OnErrorAction onError,
                                           String cloCustom0, String cloCustom1, String cloCustom2, String cloCustom3)
      throws XPRC_CronLikeSchedulerException, XPRC_InvalidCronLikeOrderParametersException {
    return getXynaProcessingPortal().modifyCronLikeOrder(id, label, destination, payload, firstStartupTimeWithTimeZone,
                                                         interval, useDST, enabled, onError, cloCustom0, cloCustom1,
                                                         cloCustom2, cloCustom3);
  }

  /**
   * Modify an existing cron like order
   */
  @Deprecated
  public CronLikeOrder modifyCronLikeOrder(Long id, String label, String orderType, GeneralXynaObject payload,
                                           Long firstStartupTime, Long interval, Boolean enabled, OnErrorAction onError)
      throws XPRC_CronLikeSchedulerException, XPRC_InvalidCronLikeOrderParametersException {
    return getXynaProcessingPortal().modifyCronLikeOrder(id, label, new DestinationKey(orderType), payload, firstStartupTime,
                                                         Constants.DEFAULT_TIMEZONE, interval, false, enabled, onError,
                                                         null, null, null, null);
  }

  /**
   * @deprecated version mit DestinationKey als Parameter verwenden 
   */
  @Deprecated
  public CronLikeOrder modifyCronLikeOrder(Long id, String label, String orderType, GeneralXynaObject payload,
                                           Long firstStartupTime, String timeZoneID, Long interval, Boolean useDST,
                                           Boolean enabled, OnErrorAction onError, String cloCustom0,
                                           String cloCustom1, String cloCustom2, String cloCustom3)
      throws XPRC_CronLikeSchedulerException, XPRC_InvalidCronLikeOrderParametersException {
    return getXynaProcessingPortal().modifyCronLikeOrder(id, label, new DestinationKey(orderType), payload, firstStartupTime, timeZoneID,
                                                         interval, useDST, enabled, onError, cloCustom0, cloCustom1,
                                                         cloCustom2, cloCustom3);
  }
  

  /**
   * @deprecated version mit DestinationKey als Parameter verwenden 
   */
  @Deprecated
  public CronLikeOrder modifyCronLikeOrder(Long id, String label, String orderType, GeneralXynaObject payload,
                                           Calendar firstStartupTimeWithTimeZone, Long interval, Boolean useDST,
                                           Boolean enabled, OnErrorAction onError, String cloCustom0,
                                           String cloCustom1, String cloCustom2, String cloCustom3)
      throws XPRC_CronLikeSchedulerException, XPRC_InvalidCronLikeOrderParametersException {
    return getXynaProcessingPortal().modifyCronLikeOrder(id, label, new DestinationKey(orderType), payload, firstStartupTimeWithTimeZone,
                                                         interval, useDST, enabled, onError, cloCustom0, cloCustom1,
                                                         cloCustom2, cloCustom3);
  }

  public CronLikeOrder modifyTimeControlledOrder(Long id, CronLikeOrderCreationParameter clocp) throws XPRC_CronLikeSchedulerException,
                                 XPRC_InvalidCronLikeOrderParametersException {
    return getXynaProcessingPortal().modifyTimeControlledOrder(id, clocp);
  }

  /**
   * Start an order asynchronously
   * @throws XynaRuntimeException falls der auftrag nicht gestartet werden konnte
   */
  @AccessControlled(associatedRight = Rights.START_ORDER)
  public Long startOrder(XynaOrderCreationParameter xocp) {    
    return getXynaProcessingPortal().startOrder(xocp);
  }


  /**
   * Start an order synchronously
   */
  @AccessControlled(associatedRight = Rights.START_ORDER)
  public GeneralXynaObject startOrderSynchronously(XynaOrderCreationParameter xocp) throws XynaException {
    return getXynaProcessingPortal().startOrderSynchronously(xocp);
  }

  /**
   * Start an order synchronously
   */
  @AccessControlled(associatedRight = Rights.START_ORDER)
  public XynaOrderServerExtension startOrderSynchronouslyAndReturnOrder(XynaOrderCreationParameter xocp) throws XynaException {
    return getXynaProcessingPortal().startOrderSynchronouslyAndReturnOrder(xocp);
  }

  /**
   * Start an order synchronously
   */
  @AccessControlled(associatedRight = Rights.START_ORDER)
  public OrderExecutionResponse startOrderSynchronouslyAndReturnOrder(XynaOrderCreationParameter xocp, ResultController resultController) {
    return getXynaProcessingPortal().startOrderSynchronouslyAndReturnOrder(xocp, resultController);
  }


  /**
   * Cancel an order by ID
   *
   * @param id
   * @param timeout (relative, in ms)
   * @return true, if the order could immediately be canceled and false otherwise
   */
  public CancelBean cancelOrder(Long id, Long timeout) throws XPRC_CancelFailedException {
    return getXynaProcessingPortal().cancelOrder(id, timeout);
  }

  public CancelBean cancelOrder(Long id, Long timeout, boolean waitForTimeout) throws XPRC_CancelFailedException {
    return getXynaProcessingPortal().cancelOrder(id, timeout, waitForTimeout);
  }


  /**
   * Start a batch process
   */
  public Long startBatchProcess(BatchProcessInput input) throws XynaException {
    return getXynaProcessingPortal().startBatchProcess(input);
  }
  
  public BatchProcessInformation startBatchProcessSynchronous(BatchProcessInput input) throws XynaException {
    return getXynaProcessingPortal().startBatchProcessSynchronous(input);
  }
  
  public BatchProcessInformation getBatchProcessInformation(Long batchProcessId) throws XynaException {
    return getXynaProcessingPortal().getBatchProcessInformation(batchProcessId);
  }
  
  public BatchProcessSearchResult searchBatchProcesses(BatchProcessSelectImpl select, int maxRows) throws PersistenceLayerException {
    return getXynaProcessingPortal().searchBatchProcesses(select,maxRows);
  }

  public boolean cancelBatchProcess(Long batchProcessId, CancelMode cancelMode) throws PersistenceLayerException {
    return getXynaProcessingPortal().cancelBatchProcess(batchProcessId, cancelMode);
  }

  public boolean pauseBatchProcess(Long batchProcessId) throws PersistenceLayerException {
    return getXynaProcessingPortal().pauseBatchProcess(batchProcessId);
  }

  public boolean continueBatchProcess(Long batchProcessId) throws PersistenceLayerException {
    return getXynaProcessingPortal().continueBatchProcess(batchProcessId);
  }

  public boolean modifyBatchProcess(Long batchProcessId, BatchProcessInput input) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    return getXynaProcessingPortal().modifyBatchProcess(batchProcessId, input);
  }

  // +++ Xyna Factory Management +++


  public String getProperty(String key) {
    return getXynaFactoryManagementPortal().getProperty(key);
  }

  public XynaPropertyWithDefaultValue getPropertyWithDefaultValue(String key) {
    return getXynaFactoryManagementPortal().getPropertyWithDefaultValue(key);
  }


  public PropertyMap<String, String> getPropertiesReadOnly() {
    return getXynaFactoryManagementPortal().getPropertiesReadOnly();
  }

  public  Collection<XynaPropertyWithDefaultValue> getPropertiesWithDefaultValuesReadOnly() {
    return getXynaFactoryManagementPortal().getPropertiesWithDefaultValuesReadOnly();
  }
  
  public void setProperty(String key, String value) throws PersistenceLayerException {
    getXynaFactoryManagementPortal().setProperty(key, value);
  }

  public void setProperty(XynaPropertyWithDefaultValue property) throws PersistenceLayerException {
    getXynaFactoryManagementPortal().setProperty(property);
  }
  
  
  public XynaStatistics getXynaStatistics() {
    return getXynaFactoryManagementPortal().getXynaStatistics();
  }
  
  
  public XynaStatisticsLegacy getXynaStatisticsLegacy() {
    return getXynaFactoryManagementPortal().getXynaStatisticsLegacy();
  }


  @AccessControlled(associatedRight = Rights.MONITORING_LEVEL_MANAGEMENT)
  public void setDefaultMonitoringLevel(Integer code) throws PersistenceLayerException {
    setProperty(XynaProperty.XYNA_DEFAULT_MONITORING_LEVEL.getPropertyName(), code.toString());
  }


  @AccessControlled(associatedRight = Rights.MONITORING_LEVEL_MANAGEMENT)
  public void setMonitoringLevel(String orderType, Integer code) throws XPRC_INVALID_MONITORING_TYPE, PersistenceLayerException, XPRC_ExecutionDestinationMissingException {

    if (code == null) {
      throw new IllegalArgumentException("null not allowed for monitoring code");
    }

    if (orderType == null) {
      throw new IllegalArgumentException("null not allowed for destination key");
    }

    XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getMonitoringDispatcher()
                    .setMonitoringLevel(new DestinationKey(orderType), code);

  }


  public Integer getMonitoringLevel(String orderType) {
    if (orderType == null) {
      return null;
    }
    return XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getMonitoringDispatcher()
                    .getMonitoringLevel(new DestinationKey(orderType));
  }


  @AccessControlled(associatedRight = Rights.MONITORING_LEVEL_MANAGEMENT)
  public void removeMonitoringLevel(String orderType) throws PersistenceLayerException, XPRC_DESTINATION_NOT_FOUND {
    XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getMonitoringDispatcher()
                    .removeMonitoringLevel(new DestinationKey(orderType));
  }

  public Collection<CapacityInformation> listCapacityInformation() {
    Collection<CapacityInformation> capacityInformation = null;
    try {
      Semaphore s = listCapacityInformationLock;
      s.acquire();
      try {
        /*
         * falls der scheduler gerade am laufen ist und keine cpu-zeit bekommt, hat er nun vorrang.
         * soll verhindern, dass ein spam dieser methode nicht zu stark den scheduler bremst
         */
        Thread.yield();
        capacityInformation = getXynaProcessingPortal().listCapacityInformation();
      } finally {
        s.release();
      }
    } catch (InterruptedException e) {
      capacityInformation = new ArrayList<CapacityInformation>();
    }
    return capacityInformation;
  }


  public ExtendedCapacityUsageInformation listExtendedCapacityInformation() {
    ExtendedCapacityUsageInformation extendedCapacityUsageInformation = null;
    try {
      Semaphore s = listCapacityInformationLock;
      s.acquire();
      try {
        /*
         * falls der scheduler gerade am laufen ist und keine cpu-zeit bekommt, hat er nun vorrang.
         * soll verhindern, dass ein spam dieser methode nicht zu stark den scheduler bremst
         */
        Thread.yield();
        extendedCapacityUsageInformation = getXynaProcessingPortal().listExtendedCapacityInformation();
      } finally {
        s.release();
      }
    } catch (InterruptedException e) {
      extendedCapacityUsageInformation = new ExtendedCapacityUsageInformation();
    }
    return extendedCapacityUsageInformation;
  }


  public SchedulerInformationBean listSchedulerInformation(SchedulerInformationBean.Mode mode) {
    SchedulerInformationBean schedulerInformationBean = null;
    try {
      Semaphore s = listSchedulerInformationLock;
      s.acquire();
      try {
        Thread.sleep(50);
        schedulerInformationBean = getXynaProcessingPortal().listSchedulerInformation(mode);
        Thread.sleep(50);

      } finally {
        s.release();
      }
    } catch (InterruptedException e) {
      schedulerInformationBean = new SchedulerInformationBean();
    }
    return schedulerInformationBean;
  }
  
  public OrderSeriesManagementInformation listOrderSeriesManagementInformation(OrderSeriesManagementInformation.Mode mode) {
    OrderSeriesManagementInformation osmi = null;
    try {
      Semaphore s = listOrderSeriesManagementInformationLock;
      s.acquire();
      try {
        osmi = getXynaProcessingPortal().listOrderSeriesManagementInformation(mode);
      } finally {
        s.release();
      }
    } catch (InterruptedException e) {
      osmi = new OrderSeriesManagementInformation();
    }
    return osmi;
  }
  
  public RescheduleSeriesOrderInformation rescheduleSeriesOrder(long orderId, boolean force) {
    return getXynaProcessingPortal().rescheduleSeriesOrder(orderId, force);
  }


  public Map<ApplicationEntryType, Map<String, DeploymentStatus>> listDeploymentStatuses(Long revision) {
    return getXynaProcessingPortal().listDeploymentStatuses(revision);
  }

  public List<WorkflowInformation> listWorkflows() throws XynaException {
    return getXynaProcessingPortal().listWorkflows();
  }


  public boolean addCapacity(String name, int cardinality, CapacityManagement.State state) throws XPRC_CAPACITY_ALREADY_DEFINED, PersistenceLayerException {
    return getXynaProcessingPortal().addCapacity(name, cardinality, state);
  }


  public boolean removeCapacity(String name) throws PersistenceLayerException {
    return getXynaProcessingPortal().removeCapacity(name);
  }


  public boolean changeCapacityName(String name, String newName) throws PersistenceLayerException {
    return getXynaProcessingPortal().changeCapacityName(name, newName);
  }


  public boolean changeCapacityCardinality(String name, int cardinality) throws PersistenceLayerException, XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState, XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain {
    return getXynaProcessingPortal().changeCapacityCardinality(name, cardinality);
  }


  public boolean changeCapacityState(String name, CapacityManagement.State state) throws PersistenceLayerException {
    return getXynaProcessingPortal().changeCapacityState(name, state);
  }


  @Deprecated
  public boolean requireCapacityForWorkflow(String workflowFqName, String capacityName, int capacityCardinality)
      throws PersistenceLayerException, XFMG_InvalidCapacityCardinality {
    return getXynaProcessingPortal().requireCapacityForWorkflow(workflowFqName, capacityName, capacityCardinality);
  }


  @Deprecated
  public boolean removeCapacityForWorkflow(String workflowFqName, String capacityName) throws PersistenceLayerException {
    return getXynaProcessingPortal().removeCapacityForWorkflow(workflowFqName, capacityName);
  }


  public boolean requireCapacityForOrderType(String orderType, String capacityName, int capacityCardinality)
      throws PersistenceLayerException, XFMG_InvalidCapacityCardinality {
    return getXynaProcessingPortal().requireCapacityForOrderType(orderType, capacityName, capacityCardinality);
  }


  public boolean removeCapacityForOrderType(String orderType, String capacityName) throws PersistenceLayerException {
    return getXynaProcessingPortal().removeCapacityForOrderType(orderType, capacityName);
  }


  public List<Capacity> listCapacitiesForOrderType(DestinationKey destination) {
    return getXynaProcessingPortal().listCapacitiesForOrderType(destination);
  }

  public List<String> listOrderTypesForWorkflow(String workflowOriginalFQName) {
    return getXynaProcessingPortal().listOrderTypesForWorkflow(workflowOriginalFQName);
  }


  public OrderInstanceDetails getOrderInstanceDetails(Long id) throws PersistenceLayerException,
                  XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    return getXynaFactoryManagementPortal().getRunningProcessDetails(id);
  }


  public Map<String, OrderArchiveStatisticsStorable> getCompleteCallStatistics() {
    ProcessMonitoring mon = (ProcessMonitoring) XynaFactory.getInstance().getFactoryManagement()
                    .getSection(XynaFactoryMonitoring.DEFAULT_NAME).getFunctionGroup(ProcessMonitoring.DEFAULT_NAME);
    return mon.getStatistics();
  }
  
  
  @Deprecated
  public HashMap<String, Long> getCallStatistics() {
    ProcessMonitoring mon = (ProcessMonitoring) XynaFactory.getInstance().getFactoryManagement()
                    .getSection(XynaFactoryMonitoring.DEFAULT_NAME).getFunctionGroup(ProcessMonitoring.DEFAULT_NAME);
    return mon.getCallStatistics();
  }


  @Deprecated
  public HashMap<String, Long> getFinishedStatistics() {
    ProcessMonitoring mon = (ProcessMonitoring) XynaFactory.getInstance().getFactoryManagement()
                    .getSection(XynaFactoryMonitoring.DEFAULT_NAME).getFunctionGroup(ProcessMonitoring.DEFAULT_NAME);
    return mon.getFinishedStatistics();
  }


  @Deprecated
  public HashMap<String, Long> getErrorStatistics() {
    ProcessMonitoring mon = (ProcessMonitoring) XynaFactory.getInstance().getFactoryManagement()
                    .getSection(XynaFactoryMonitoring.DEFAULT_NAME).getFunctionGroup(ProcessMonitoring.DEFAULT_NAME);
    return mon.getErrorStatistics();
  }


  @Deprecated
  public HashMap<String, Long> getTimeoutStatistics() {
    ProcessMonitoring mon = (ProcessMonitoring) XynaFactory.getInstance().getFactoryManagement()
                    .getSection(XynaFactoryMonitoring.DEFAULT_NAME).getFunctionGroup(ProcessMonitoring.DEFAULT_NAME);
    return mon.getTimeoutStatistics();
  }

  public Map<Long, CronLikeOrderInformation> getAllCronLikeOrders(long maxRows) throws XPRC_CronLikeSchedulerException {
    ProcessMonitoring mon = (ProcessMonitoring) XynaFactory.getInstance().getFactoryManagement()
                    .getSection(XynaFactoryMonitoring.DEFAULT_NAME).getFunctionGroup(ProcessMonitoring.DEFAULT_NAME);
    return mon.getAllCronLikeOrders(maxRows);
  }

  public CronLikeOrderSearchResult searchCronLikeOrders(CronLikeOrderSelectImpl selectCron, int maxRows) throws XynaException {
    return XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler().searchCronLikeOrders(selectCron, maxRows);
  }

  public Map<Long, OrderInstance> getAllRunningProcesses(long offset, int count) throws PersistenceLayerException {
    try {
      Semaphore s = searchOrderArchiveLock;
      if (!s.tryAcquire(1000, TimeUnit.MILLISECONDS)) {
        throw new XMCP_RestrictedCallNotAvailableException("searchOrderArchive");
      }
      try {
        //FIXME das geht auch übers orderarchive, aber evtl sollte man das hier besser kenntlich machen.
        //was unterscheidet diese methode schliesslich von search/searchorderinstances, ausser dem suchkriterium
        return getXynaFactoryManagementPortal().getAllRunningProcesses(offset, count);
      } finally {
        s.release();
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }


  /**
   * Returns a Java representation of the available information on a running workflow instance
   */
  public OrderInstanceDetails getRunningProcessDetails(Long id) throws PersistenceLayerException,
                  XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    ProcessMonitoring mon = (ProcessMonitoring) XynaFactory.getInstance().getFactoryManagement()
                    .getSection(XynaFactoryMonitoring.DEFAULT_NAME).getFunctionGroup(ProcessMonitoring.DEFAULT_NAME);
    return mon.getRunningProcessDetails(id);
  }


  /**
   * Returns an XML representation of the parameter values as far as the requested workflow instance has executed and as
   * far as the information is available
   */
  public String getRunningProcessDetailsXML(Long id) throws PersistenceLayerException,
                  XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    OrderInstanceDetails wfid = getRunningProcessDetails(id);
    if (wfid != null) {
      return wfid.getAuditDataAsXML();
    } else {
      return null;
    }
  }


  /**
   * Manual interaction
   */
  public Map<Long, ManualInteractionEntry> listManualInteractionEntries() throws PersistenceLayerException {
    return XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getManualInteractionManagement()
        .listManualInteractionEntries();
  }


  public Map<Long, ManualInteractionEntry> listManualInteractionEntries(int maxRows) throws PersistenceLayerException {
    return XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getManualInteractionManagement()
        .listManualInteractionEntries(maxRows);
  }


  @AccessControlled(associatedRight = Rights.VIEW_MANUAL_INTERACTION)
  public ManualInteractionResult searchManualInteractionEntries(ManualInteractionSelect select, int maxRows)
      throws PersistenceLayerException {
    return XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getManualInteractionManagement()
        .search(select, maxRows);
  }


  @AccessControlled(associatedRight = Rights.VIEW_MANUAL_INTERACTION)
  public ExtendedManualInteractionResult searchExtendedManualInteractionEntries(ManualInteractionSelect select,
                                                                                int maxRows)
      throws PersistenceLayerException {
    return XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getManualInteractionManagement()
        .searchExtended(select, maxRows);
  }


  @AccessControlled(associatedRight = Rights.PROCESS_MANUAL_INTERACTION)
  public ProcessManualInteractionResult processManualInteraction(Long id, GeneralXynaObject response)
      throws PersistenceLayerException, XPRC_ResumeFailedException, XPRC_IllegalManualInteractionResponse,
      MiProcessingRejected {
    return XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getManualInteractionManagement()
        .processManualInteractionEntry(id, response);
  }


  @Deprecated
  @AccessControlled(associatedRight = Rights.PROCESS_MANUAL_INTERACTION)
  public ProcessManualInteractionResult processManualInteractionEntry(Long id, GeneralXynaObject response)
      throws PersistenceLayerException, XPRC_ResumeFailedException, XPRC_IllegalManualInteractionResponse,
      XPRC_FACTORY_IS_SHUTTING_DOWN {
    try {
      return XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getManualInteractionManagement()
          .processManualInteractionEntry(id, response);
    } catch (MiProcessingRejected e) {
      throw new XPRC_FACTORY_IS_SHUTTING_DOWN("Process Manual Interaction", e);
    }
  }


  @Override
  public GeneralXynaObject waitForMI(XynaOrderServerExtension xo, String reason, String type, String userGroup,
                                     String todo, GeneralXynaObject payload) throws PersistenceLayerException,
      XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    return XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getManualInteractionManagement()
        .waitForMI(xo, reason, type, userGroup, todo, payload);
  }


  public ConnectionFilterInstance<?>[] getFilterInstances(String filterName) {
    return getXynaActivationPortal().getFilterInstances(filterName);
  }


  public Filter[] getFilters(String triggerName) {
    return getXynaActivationPortal().getFilters(triggerName);
  }


  public EventListenerInstance<?, ?>[] getTriggerInstances(String triggerName) throws XACT_TriggerNotFound {
    return getXynaActivationPortal().getTriggerInstances(triggerName);
  }


  public Trigger[] getTriggers() {
    return getXynaActivationPortal().getTriggers();
  }

  public EventListener<?, ?> getTriggerInstance(TriggerInstanceIdentification triggerInstanceId) throws XACT_TriggerNotFound {
    return getXynaActivationPortal().getTriggerInstance(triggerInstanceId);
  }

  public static void exportMDM(TemporaryFileHandler tfh) throws XynaException, IOException {
    // mdm kopieren, ausser ausgewählte dateien TODO
    FilenameFilter xmlFilter = new FilenameFilter() {

      public boolean accept(File dir, String name) {
        return name.endsWith(".xml") || new File(dir, name).isDirectory();
      }

    };
    ArrayList<File> files = new ArrayList<File>();
    String savedMdmDir = RevisionManagement.getPathForRevision(PathType.XMOM, RevisionManagement.REVISION_DEFAULT_WORKSPACE, false);
    String deployedMdmDir = RevisionManagement.getPathForRevision(PathType.XMOM, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
    String deployedServicesDir = RevisionManagement.getPathForRevision(PathType.SERVICE, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
    FileUtils.findFilesRecursively(new File(deployedMdmDir), files, xmlFilter);
    FileUtils.findFilesRecursively(new File(savedMdmDir), files, xmlFilter);
    FileUtils.findFilesRecursively(new File(deployedServicesDir), files, null); // alle files in service-dirs

    // kopieren
    File tempDir = new File("tempdir" + Math.random());
    FileUtils.copyFiles(files, new File(Constants.BASEDIR), tempDir);

    // temporäres zipfile bauen
    File f = new File("myzip" + Math.random() + ".zip");
    FileUtils.zipDirectory(f, tempDir);
    FileInputStream fis = new FileInputStream(f);
    try {
      tfh.handleFile(fis);
    } finally {
      fis.close();
      f.delete();
      if (!FileUtils.deleteDirectoryRecursively(tempDir)) {
        throw new XMCP_COULD_NOT_DELETE_DIRECTORY(tempDir.getAbsolutePath());
      }
    }
  }


  public static void importMDM(ZipInputStream zis) throws Ex_FileAccessException, Ex_FileAccessException, XACT_JarFileUnzipProblem, XPRC_XmlParsingException {

    // TODO evtl progressindikator übergeben, wenn das lange dauert

    // zip auspacken
    File[] files = FileUtils.saveZipToDir(zis, new File(Constants.BASEDIR));
    // deploy mdm
    for (File f : files) {
      try {
        String path = f.getCanonicalPath();
        if (path.startsWith(Constants.DEPLOYED_MDM_DIR) && path.endsWith(".xml")) {
          Document doc = XMLUtils.parse(path);
          Element root = doc.getDocumentElement();
          if (root.getTagName().equals(GenerationBase.EL.SERVICE)) {
            // workflow
          } else {
            // datatype
          }
        }
      } catch (IOException e) {
        throw new Ex_FileAccessException(f.getAbsolutePath());
      }
    }
  }

  public ProcessMonitoring getProcessMonitoring() {
    return XynaFactory.getInstance().getFactoryManagement().getProcessMonitoring();
  }


  public XynaFactoryManagementODS getXynaFactoryManagementODS() {
    return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS();
  }


  public XynaFactoryMonitoring getXynaFactoryMonitoring() {
    return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring();
  }


  public XynaFactoryControl getXynaFactoryControl() {
    return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl();
  }


  public FactoryWarehouseCursor<OrderInstanceBackup> listSuspendedOrders(ODSConnection defaultConnection)
      throws PersistenceLayerException {
    return getXynaProcessingPortal().listSuspendedOrders(defaultConnection);
  }
  

  @Deprecated
  @AccessControlled(associatedRight = Rights.SESSION_CREATION)
  public SessionCredentials getNewSession(User user, boolean force) throws PersistenceLayerException, XFMG_DuplicateSessionException {
    return getXynaFactoryManagementPortal().getNewSession(user, force);
  }


  @AccessControlled(associatedRight = Rights.SESSION_CREATION)
  public SessionCredentials createSession(XynaUserCredentials credentials, Optional<String> roleName, boolean force)
      throws PersistenceLayerException, XFMG_DuplicateSessionException {
    return getXynaFactoryManagementPortal().createSession(credentials, roleName, force);
  }


  @AccessControlled(associatedRight = Rights.SESSION_CREATION)
  public boolean authorizeSession(String sessionId, String token, String roleName) throws PersistenceLayerException,
                  XFMG_UnknownSessionIDException, XFMG_SESSION_AUTHENTICATION_FAILED, XFMG_DuplicateSessionException {
    return getXynaFactoryManagementPortal().authorizeSession(sessionId, token, roleName);
  }


  public boolean releaseAllSessionPriviliges(String sessionId) throws PersistenceLayerException {
    return getXynaFactoryManagementPortal().releaseAllSessionPriviliges(sessionId);
  }


  public boolean releaseSessionPrivilige(String sessionId, ASessionPrivilege privilige) throws PersistenceLayerException {
    return getXynaFactoryManagementPortal().releaseSessionPrivilige(sessionId, privilige);
  }


  public boolean requestSessionPriviliges(String sessionId, ASessionPrivilege privilige) throws PersistenceLayerException {
    return getXynaFactoryManagementPortal().requestSessionPriviliges(sessionId, privilige);
  }


  public boolean keepSessionAlive(String sessionId) throws PersistenceLayerException {
    return getXynaFactoryManagementPortal().keepSessionAlive(sessionId);
  }
  
  
  @AccessControlled(associatedRight = Rights.USER_MANAGEMENT)
  public boolean isSessionAlive(String sessionId) throws PersistenceLayerException {
    return getXynaFactoryManagementPortal().isSessionAlive(sessionId);
  }


  public Role authenticateSession(String sessionId, String token) throws PersistenceLayerException, XFMG_UnknownSessionIDException, XFMG_SESSION_AUTHENTICATION_FAILED {
    return getXynaFactoryManagementPortal().authenticateSession(sessionId, token);
  }


  public SessionDetails getSessionDetails(String sessionId) throws PersistenceLayerException,
      XFMG_UnknownSessionIDException {
    return getXynaFactoryManagementPortal().getSessionDetails(sessionId);
  }


  @AccessControlled(associatedRight = Rights.USER_MANAGEMENT)
  public boolean createUser(String id, String roleName, String password, boolean isPassHashed)
      throws PersistenceLayerException, XFMG_RoleDoesNotExistException, XFMG_PasswordRestrictionViolation, XFMG_NameContainsInvalidCharacter {
    return getXynaFactoryManagementPortal().createUser(id, roleName, password, isPassHashed);
  }


  public boolean createUser(String id, String roleName, String password, boolean isPassHashed, List<String> domains)
      throws PersistenceLayerException, XFMG_RoleDoesNotExistException, XFMG_PasswordRestrictionViolation, XFMG_NameContainsInvalidCharacter {
    return getXynaFactoryManagementPortal().createUser(id, roleName, password, isPassHashed, domains);
  }


  @AccessControlled(associatedRight = Rights.USER_MANAGEMENT)
  public boolean importUser(String id, String roleName, String passwordhash) throws PersistenceLayerException, XFMG_NameContainsInvalidCharacter {
    return getXynaFactoryManagementPortal().importUser(id, roleName, passwordhash);
  }


  @AccessControlled(associatedRight = Rights.USER_MANAGEMENT)
  public boolean deleteUser(String id) throws PersistenceLayerException, XFMG_PredefinedXynaObjectException {
    return getXynaFactoryManagementPortal().deleteUser(id);
  }


  public String listUsers() throws PersistenceLayerException {
    return getXynaFactoryManagementPortal().listUsers();
  }


  @AccessControlled(associatedRight = Rights.USER_MANAGEMENT_EDIT_OWN)
  public boolean changePassword(String id, String oldPassword, String newPassword, boolean isNewPasswordHashed) throws PersistenceLayerException,
      XFMG_UserAuthenticationFailedException, XFMG_UserIsLockedException, XFMG_UserDoesNotExistException, XFMG_PasswordRestrictionViolation {
    return getXynaFactoryManagementPortal().changePassword(id, oldPassword, newPassword, isNewPasswordHashed);
  }


  public User authenticate(String id, String password) throws XFMG_UserAuthenticationFailedException,
      XFMG_UserIsLockedException, PersistenceLayerException {
    return getXynaFactoryManagementPortal().authenticate(id, password);
  }


  public User authenticateHashed(String id, String password) throws XFMG_UserAuthenticationFailedException,
      XFMG_UserIsLockedException, PersistenceLayerException {
    return getXynaFactoryManagementPortal().authenticateHashed(id, password);
  }


  public boolean usersExists(String id) throws PersistenceLayerException {
    return getXynaFactoryManagementPortal().usersExists(id);
  }


  @AccessControlled(associatedRight = Rights.USER_MANAGEMENT)
  public boolean resetPassword(String id, String newPassword) throws PersistenceLayerException,
      XFMG_UserDoesNotExistException, XFMG_PasswordRestrictionViolation {
    return getXynaFactoryManagementPortal().resetPassword(id, newPassword);
  }


  @AccessControlled(associatedRight = Rights.USER_MANAGEMENT)
  public boolean setPassword(String id, String password) throws PersistenceLayerException,
      XFMG_UserDoesNotExistException, XFMG_PasswordRestrictionViolation {
    return getXynaFactoryManagementPortal().setPassword(id, password);
  }


  @AccessControlled(associatedRight = Rights.USER_MANAGEMENT)
  public boolean setPasswordHash(String id, String passwordhash) throws PersistenceLayerException,
      XFMG_UserDoesNotExistException {
    return getXynaFactoryManagementPortal().setPasswordHash(id, passwordhash);
  }


  @AccessControlled(associatedRight = Rights.USER_MANAGEMENT)
  public boolean changeRole(String id, String name) throws PersistenceLayerException,
      XFMG_PredefinedXynaObjectException, XFMG_UserDoesNotExistException, XFMG_RoleDoesNotExistException {
    return getXynaFactoryManagementPortal().changeRole(id, name);
  }


  @AccessControlled(associatedRight = Rights.USER_MANAGEMENT)
  public boolean createRole(String name, String domain) throws PersistenceLayerException,
      XFMG_DomainDoesNotExistException, XFMG_NameContainsInvalidCharacter {
    return getXynaFactoryManagementPortal().createRole(name, domain);
  }


  @AccessControlled(associatedRight = Rights.USER_MANAGEMENT)
  public boolean deleteRole(String name, String domain) throws PersistenceLayerException,
      XFMG_PredefinedXynaObjectException, XFMG_RoleIsAssignedException {
    return getXynaFactoryManagementPortal().deleteRole(name, domain);
  }


  public boolean hasRight(String methodName, Role role) throws PersistenceLayerException {
    return getXynaFactoryManagementPortal().hasRight(methodName, role);
  }


  public boolean hasRight(String methodName, String role) throws PersistenceLayerException {
    return getXynaFactoryManagementPortal().hasRight(methodName, role);
  }


  public String resolveFunctionToRight(String methodName) {
    return getXynaFactoryManagementPortal().resolveFunctionToRight(methodName);
  }


  @AccessControlled(associatedRight = Rights.USER_MANAGEMENT)
  public boolean grantRightToRole(String roleName, String right) throws PersistenceLayerException,
                  XFMG_RightDoesNotExistException, XFMG_RoleDoesNotExistException {
    return getXynaFactoryManagementPortal().grantRightToRole(roleName, right);
  }


  @AccessControlled(associatedRight = Rights.USER_MANAGEMENT)
  public boolean revokeRightFromRole(String roleName, String right) throws PersistenceLayerException,
                  XFMG_RoleDoesNotExistException, XFMG_RightDoesNotExistException {
    return getXynaFactoryManagementPortal().revokeRightFromRole(roleName, right);
  }


  @AccessControlled(associatedRight = Rights.USER_MANAGEMENT)
  public boolean createRight(String rightName) throws PersistenceLayerException, XFMG_NamingConventionException, XFMG_NameContainsInvalidCharacter {
    return getXynaFactoryManagementPortal().createRight( rightName);
  }


  @AccessControlled(associatedRight = Rights.USER_MANAGEMENT)
  public boolean deleteRight(String rightName) throws PersistenceLayerException, XFMG_PredefinedXynaObjectException, XFMG_RightDoesNotExistException {
    return getXynaFactoryManagementPortal().deleteRight(rightName);
  }


  public void listenToMdmModifications(String sessionId, ANotificationConnection con) throws PersistenceLayerException {
    getXynaFactoryManagementPortal().listenToMdmModifications(sessionId, con);
  }


  public void listenToProcessProgress(String sessionId, ANotificationConnection con, Long orderId) throws PersistenceLayerException {
    getXynaFactoryManagementPortal().listenToProcessProgress(sessionId, con, orderId);
  }


  public void quitSession(String sessionId) throws PersistenceLayerException {
    getXynaFactoryManagementPortal().quitSession(sessionId);
  }


  public ODS getXynaActivationODS() {
    return getXynaActivationPortal().getXynaActivationODS();
  }


  public SecureStorage getSecureStorage() {
    return XynaFactory.getInstance().getXynaNetworkWarehousePortal().getSecureStorage();
  }


  public boolean remove(String destination, String key) throws PersistenceLayerException {
    return getSecureStorage().remove(destination, key);
  }


  public Serializable retrieve(String destination, String key) {
    return getSecureStorage().retrieve(destination, key);
  }


  public boolean store(String destination, String key, Serializable serializable) throws PersistenceLayerException {
    return getSecureStorage().store(destination, key, serializable);
  }

  @AccessControlled(associatedRight = Rights.USER_MANAGEMENT)
  public Collection<Right> getRights() throws PersistenceLayerException {
    return getRights(null);
  }
  
  @AccessControlled(associatedRight = Rights.USER_MANAGEMENT)
  public Collection<Right> getRights(String language) throws PersistenceLayerException {
    return getXynaFactoryManagementPortal().getRights(language);
  }

  @AccessControlled(associatedRight = Rights.USER_MANAGEMENT)
  public Collection<RightScope> getRightScopes() throws PersistenceLayerException {
    return getRightScopes(null);
  }

  @AccessControlled(associatedRight = Rights.USER_MANAGEMENT)
  public Collection<RightScope> getRightScopes(String language) throws PersistenceLayerException {
    return getXynaFactoryManagementPortal().getRightScopes(language);
  }

  @AccessControlled(associatedRight = Rights.USER_MANAGEMENT)
  public Collection<Role> getRoles() throws PersistenceLayerException {
    return getXynaFactoryManagementPortal().getRoles();
  }


  @AccessControlled(associatedRight = Rights.USER_MANAGEMENT)
  public Collection<User> getUser() throws PersistenceLayerException {
    return getXynaFactoryManagementPortal().getUser();
  }


  public boolean isPredefined(PredefinedCategories category, String id) {
    return getXynaFactoryManagementPortal().isPredefined(category, id);
  }


  public void removeProperty(String key) throws PersistenceLayerException {
    getXynaFactoryManagementPortal().removeProperty(key);
  }


  @AccessControlled(associatedRight = Rights.FREQUENCY_CONTROL_VIEW)
  public FrequencyControlledTaskSearchResult searchFrequencyControlledTasks(FrequencyControlledTaskSelect select,
                                                                            int maxRows)
                  throws PersistenceLayerException {
    return XynaFactory.getInstance().getProcessing().getFrequencyControl().searchFrequencyControlledTasks(select,
                                                                                                          maxRows);
  }


  @AccessControlled(associatedRight = Rights.ORDERARCHIVE_VIEW)
  public OrderInstanceResult search(OrderInstanceSelect select, int maxRows) throws PersistenceLayerException {
    return searchOrderInstances(select, maxRows, SearchMode.FLAT);
  }



  @AccessControlled(associatedRight = Rights.ORDERARCHIVE_VIEW)
  public OrderInstanceResult searchOrderInstances(OrderInstanceSelect select, int maxRows, SearchMode searchMode)
      throws PersistenceLayerException {
    try {
      Semaphore s = searchOrderArchiveLock; 
      if (!s.tryAcquire(1000, TimeUnit.MILLISECONDS)) {
        throw new XMCP_RestrictedCallNotAvailableException("searchOrderArchive");
      }
      try {
        return XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive()
                        .searchOrderInstances(select, maxRows, searchMode);
      } finally {
        s.release();
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }


  @AccessControlled(associatedRight = Rights.ORDERARCHIVE_DETAILS)
  public OrderInstanceDetails getCompleteOrder(long id) throws PersistenceLayerException,
                  XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    return XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive().getCompleteOrder(id);
  }


  @AccessControlled(associatedRight = Rights.DISPATCHER_MANAGEMENT)
  public Map<DestinationKey, DestinationValue> getDestinations(DispatcherIdentification dispatcherId) {
    return XynaFactory.getInstance().getProcessing().getDestinations(dispatcherId);
  }


  @AccessControlled(associatedRight = Rights.DISPATCHER_MANAGEMENT)
  public List<DispatcherEntry> listDestinations(DispatcherIdentification dispatcherId) {
    return XynaFactory.getInstance().getProcessing().listDestinations(dispatcherId);
  }


  @AccessControlled(associatedRight = Rights.DISPATCHER_MANAGEMENT)
  public DispatcherEntry getDestination(DispatcherIdentification dispatcherId, DestinationKey dk)
                  throws XPRC_DESTINATION_NOT_FOUND {
    return XynaFactory.getInstance().getProcessing().getDestination(dispatcherId, dk);
  }
  
  
  @AccessControlled(associatedRight = Rights.DISPATCHER_MANAGEMENT)
  public DispatcherEntry getDestination(DispatcherIdentification dispatcherId, DestinationKey dk, boolean followRuntimeContextDependencies)
                  throws XPRC_DESTINATION_NOT_FOUND {
    return XynaFactory.getInstance().getProcessing().getDestination(dispatcherId, dk, followRuntimeContextDependencies);
  }


  @AccessControlled(associatedRight = Rights.DISPATCHER_MANAGEMENT)
  public void removeDestination(DispatcherIdentification dispatcherId, DestinationKey dk)
                  throws PersistenceLayerException {
    XynaFactory.getInstance().getProcessing().removeDestination(dispatcherId, dk);
  }


  @AccessControlled(associatedRight = Rights.DISPATCHER_MANAGEMENT)
  public void setDestination(DispatcherIdentification dispatcherId, DestinationKey dk, DestinationValue dv)
                  throws PersistenceLayerException {
    XynaFactory.getInstance().getProcessing().setDestination(dispatcherId, dk, dv);

  }


  @AccessControlled(associatedRight = Rights.KILL_STUCK_PROCESS)
  public KillStuckProcessBean killStuckProcess(Long orderId, boolean forceKill, AbortionCause reason) throws XynaException {
    return getXynaProcessingPortal().killStuckProcess(orderId, forceKill, reason);
  }


  public KillStuckProcessBean killStuckProcess(KillStuckProcessBean bean) throws XynaException {
    return getXynaProcessingPortal().killStuckProcess(bean);
  }


  public void deployPersistenceLayer(String name, String persistenceLayerFqClassname) throws XNWH_PERSISTENCE_LAYER_CLASS_NOT_FOUND,
      XFMG_JarFolderNotFoundException, PersistenceLayerException {
    getXynaNetworkWarehousePortal().deployPersistenceLayer(name, persistenceLayerFqClassname);
  }


  public void undeployPersistenceLayer(String persistenceLayerFqClassname) throws PersistenceLayerException,
                  XNWH_PersistenceLayerNotRegisteredException, XNWH_PersistenceLayerMayNotBeUndeployedInUseException {
    getXynaNetworkWarehousePortal().undeployPersistenceLayer(persistenceLayerFqClassname);
  }


  public boolean configureOrderContextMappingForDestinationKey(DestinationKey dk, boolean createMapping)
                  throws PersistenceLayerException {
    return getXynaProcessingPortal().configureOrderContextMappingForDestinationKey(dk, createMapping);
  }


  public Collection<DestinationKey> getAllDestinationKeysForWhichAnOrderContextMappingIsCreated() {
    return getXynaProcessingPortal().getAllDestinationKeysForWhichAnOrderContextMappingIsCreated();
  }


  public void configureTriggerMaxEvents(String triggerInstanceName, long maxNumberEvents, boolean autoReject)
                  throws XACT_TriggerInstanceNotFound, PersistenceLayerException {
    getXynaActivationPortal().configureTriggerMaxEvents(triggerInstanceName, maxNumberEvents, autoReject);
  }


  public String[] scanLogForLinesOfOrder(long orderId, int lineOffset, int maxNumberOfLines, String... excludes)
                  throws XFMG_ErrorScanningLogFile {
    ProcessMonitoring mon = (ProcessMonitoring) XynaFactory.getInstance().getFactoryManagement()
                    .getSection(XynaFactoryMonitoring.DEFAULT_NAME).getFunctionGroup(ProcessMonitoring.DEFAULT_NAME);
    return mon.scanLogForLinesOfOrder(orderId, lineOffset, maxNumberOfLines, excludes);
  }


  public String retrieveLogForOrder(long orderId, int lineOffset, int maxNumberOfLines, String... excludes)
                  throws XFMG_ErrorScanningLogFile {
    ProcessMonitoring mon = (ProcessMonitoring) XynaFactory.getInstance().getFactoryManagement()
                    .getSection(XynaFactoryMonitoring.DEFAULT_NAME).getFunctionGroup(ProcessMonitoring.DEFAULT_NAME);
    return mon.retrieveLogForOrder(orderId, lineOffset, maxNumberOfLines, excludes);
  }


  @AccessControlled(associatedRight = Rights.FREQUENCY_CONTROL_MANAGEMENT)
  public long startFrequencyControlledTask(FrequencyControlledTaskCreationParameter creationParameter)
                  throws XynaException {
    return getXynaProcessingPortal().startFrequencyControlledTask(creationParameter);
  }


  @AccessControlled(associatedRight = Rights.FREQUENCY_CONTROL_MANAGEMENT)
  public boolean cancelFrequencyControlledTask(long taskId) throws XynaException {
    return getXynaProcessingPortal().cancelFrequencyControlledTask(taskId);
  }


  @AccessControlled(associatedRight = Rights.FREQUENCY_CONTROL_VIEW)
  public FrequencyControlledTaskInformation getFrequencyControlledTaskInformation(long taskId,
                                                                                  String[] selectedStatistics)
      throws XynaException {
    return getXynaProcessingPortal().getFrequencyControlledTaskInformation(taskId, selectedStatistics);
  }


  @AccessControlled(associatedRight = Rights.FREQUENCY_CONTROL_VIEW)
  public FrequencyControlledTaskInformation getFrequencyControlledTaskInformation(long taskId) throws XynaException {
    return getXynaProcessingPortal().getFrequencyControlledTaskInformation(taskId);
  }


  public User getUser(String useridentifier) throws PersistenceLayerException {
    return getXynaFactoryManagementPortal().getUser(useridentifier);
  }


  public Role getRole(String rolename, String domain) throws PersistenceLayerException {
    return getXynaFactoryManagementPortal().getRole(rolename, domain);
  }


  public Right getRight(String rightidentifier) throws PersistenceLayerException {
    return getRight(rightidentifier, null);
  }

  public Right getRight(String rightidentifier, String language) throws PersistenceLayerException {
    return getXynaFactoryManagementPortal().getRight(rightidentifier, language);
  }

  public Domain getDomain(String domainidentifier) throws PersistenceLayerException {
    return getXynaFactoryManagementPortal().getDomain(domainidentifier);
  }


  @AccessControlled(associatedRight = Rights.USER_MANAGEMENT)
  public boolean setLockedStateOfUser(String useridentifier, boolean newState) throws PersistenceLayerException, XFMG_UserDoesNotExistException, XFMG_PredefinedXynaObjectException {
    return getXynaFactoryManagementPortal().setLockedStateOfUser(useridentifier, newState);
  }


  @AccessControlled(associatedRight = Rights.USER_MANAGEMENT)
  public boolean setDomainsOfUser(String useridentifier, List<String> domains) throws PersistenceLayerException, XFMG_UserDoesNotExistException, XFMG_DomainDoesNotExistException {
    return getXynaFactoryManagementPortal().setDomainsOfUser(useridentifier, domains);
  }


  @AccessControlled(associatedRight = Rights.USER_MANAGEMENT)
  public boolean setDescriptionOfRole(String rolename, String domainname, String newDescription) throws PersistenceLayerException, XFMG_RoleDoesNotExistException, XFMG_PredefinedXynaObjectException {
    return getXynaFactoryManagementPortal().setDescriptionOfRole(rolename, domainname, newDescription);
  }


  @AccessControlled(associatedRight = Rights.USER_MANAGEMENT)
  public boolean setAliasOfRole(String rolename, String domainname, String newAlias) throws PersistenceLayerException, XFMG_RoleDoesNotExistException, XFMG_PredefinedXynaObjectException {
    return getXynaFactoryManagementPortal().setAliasOfRole(rolename, domainname, newAlias);
  }


  @AccessControlled(associatedRight = Rights.USER_MANAGEMENT)
  public UserSearchResult searchUsers(UserSelect selection, int maxRows) throws PersistenceLayerException {
    return getXynaFactoryManagementPortal().searchUsers(selection, maxRows);
  }

  @AccessControlled(associatedRight = Rights.USER_MANAGEMENT)
  public Collection<Domain> getDomains() throws PersistenceLayerException {
    return getXynaFactoryManagementPortal().getDomains();
  }


  @AccessControlled(associatedRight = Rights.USER_MANAGEMENT)
  public boolean createDomain(String domainidentifier, DomainType type, int maxRetries, int connectionTimeout) throws PersistenceLayerException, XFMG_NameContainsInvalidCharacter {
    return getXynaFactoryManagementPortal().createDomain(domainidentifier, type, maxRetries, connectionTimeout);
  }


  @AccessControlled(associatedRight = Rights.USER_MANAGEMENT)
  public boolean setDomainSpecificDataOfDomain(String domainidentifier, DomainTypeSpecificData specificData) throws PersistenceLayerException, XFMG_DomainDoesNotExistException {
    return getXynaFactoryManagementPortal().setDomainSpecificDataOfDomain(domainidentifier, specificData);
  }


  @AccessControlled(associatedRight = Rights.USER_MANAGEMENT)
  public boolean setDescriptionOfRight(String rightidentifier, String description) throws PersistenceLayerException, XFMG_RightDoesNotExistException, XFMG_PredefinedXynaObjectException {
    try {
      return setDescriptionOfRight(rightidentifier, description, null);
    } catch (XynaException e) {
      return false;
    }
  }

  @AccessControlled(associatedRight = Rights.USER_MANAGEMENT)
  public boolean setDescriptionOfRight(String rightidentifier, String description, String language) throws XynaException {
    return getXynaFactoryManagementPortal().setDescriptionOfRight(rightidentifier, description, language);
  }

  @AccessControlled(associatedRight = Rights.USER_MANAGEMENT)
  public boolean setMaxRetriesOfDomain(String domainidentifier, int maxRetries) throws PersistenceLayerException, XFMG_DomainDoesNotExistException {
    return getXynaFactoryManagementPortal().setMaxRetriesOfDomain(domainidentifier, maxRetries);
  }


  @AccessControlled(associatedRight = Rights.USER_MANAGEMENT)
  public boolean setConnectionTimeoutOfDomain(String domainidentifier, int connectionTimeout) throws PersistenceLayerException, XFMG_DomainDoesNotExistException {
    return getXynaFactoryManagementPortal().setConnectionTimeoutOfDomain(domainidentifier, connectionTimeout);
  }


  @AccessControlled(associatedRight = Rights.USER_MANAGEMENT)
  public boolean setDescriptionOfDomain(String domainidentifier, String description) throws PersistenceLayerException,
                  XFMG_DomainDoesNotExistException, XFMG_PredefinedXynaObjectException {
    return getXynaFactoryManagementPortal().setDescriptionOfDomain(domainidentifier, description);
  }


  @AccessControlled(associatedRight = Rights.USER_MANAGEMENT)
  public boolean deleteDomain(String domainidentifier) throws PersistenceLayerException,
                  XFMG_DomainDoesNotExistException, XFMG_PredefinedXynaObjectException, XFMG_DomainIsAssignedException {
    return getXynaFactoryManagementPortal().deleteDomain(domainidentifier);
  }


  public String listDomains() throws PersistenceLayerException {
    return getXynaFactoryManagementPortal().listDomains();
  }


  public List<Domain> getDomainsForUser(String useridentifier) throws PersistenceLayerException,
      XFMG_UserDoesNotExistException, XFMG_DomainDoesNotExistException {
    return getXynaFactoryManagementPortal().getDomainsForUser(useridentifier);
  }


  @Override
  public void createDeliveryItem(File packageDefininition, File deliveryItem, OutputStream out, boolean verboseOutput,
                                 boolean includeXynaComponents) throws XDEV_PackageDefinitionFileNotFoundException,
      XDEV_PackageDefinitionFileInvalidRootException, XDEV_ZipFileCouldNotBeCreatedException, Ex_FileAccessException,
      XDEV_PackageDefinitionItemNotFoundException, PersistenceLayerException, XPRC_VERSION_DETECTION_PROBLEM,
      XPRC_XmlParsingException, IOException, XPRC_DESTINATION_NOT_FOUND {
    XynaFactory.getPortalInstance().getXynaDevelopmentPortal().createDeliveryItem(packageDefininition, deliveryItem, out, verboseOutput, includeXynaComponents);
  }


  @Override
  public void installDeliveryItem(File deliveryItem, OutputStream out, boolean forceOverwrite, boolean dontUpdateMdm,
                                  boolean verboseOutput) throws IOException, Ex_FileAccessException, XynaException {

    XynaFactory.getPortalInstance().getXynaDevelopmentPortal().installDeliveryItem(deliveryItem, out, forceOverwrite, dontUpdateMdm, verboseOutput);

  }


  @Override
  public void createDeliveryItem(InputStream packageDefininition, OutputStream targetFile, OutputStream logStream,
                                 boolean verboseOutput, boolean includeXynaComponents)
      throws XDEV_PackageDefinitionFileNotFoundException,
      XDEV_PackageDefinitionFileInvalidRootException, XDEV_ZipFileCouldNotBeCreatedException, Ex_FileAccessException,
      XDEV_PackageDefinitionItemNotFoundException, PersistenceLayerException, XPRC_VERSION_DETECTION_PROBLEM,
      XPRC_XmlParsingException, IOException, XPRC_DESTINATION_NOT_FOUND {
    XynaFactory.getPortalInstance().getXynaDevelopmentPortal().createDeliveryItem(packageDefininition, targetFile, logStream, verboseOutput, includeXynaComponents);
  }


  @Override
  public void installDeliveryItem(InputStream deliveryItem, OutputStream out, boolean forceOverwrite,
                                  boolean dontUpdateMdm, boolean verboseOutput) throws IOException, XynaException {

    ImportDeliveryItem rdi = new ImportDeliveryItem(deliveryItem, out);
    rdi.setVerboseOutput(verboseOutput);
    rdi.doRestore(forceOverwrite, dontUpdateMdm);
  }

  @Deprecated
  public void undeployWF(String originalFqName, boolean undeployDependentObjects, boolean disableChecks)
      throws XynaException {
    undeployWF(originalFqName, undeployDependentObjects, disableChecks, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }

  public void undeployWF(String originalFqName, boolean undeployDependentObjects, boolean disableChecks, Long revision)
                  throws XynaException {
    XynaFactory.getInstance().getProcessing().getWorkflowEngine()
    .undeployWorkflow(originalFqName, undeployDependentObjects, disableChecks, revision);
  }

  @AccessControlled(associatedRight = Rights.DEPLOYMENT_MDM)
  public void undeployXMOMObject(String originalFqName, XMOMType type, DependentObjectMode dependentObjectMode, boolean disableChecks, Long revision)
                  throws XynaException {
    XynaFactory.getInstance().getProcessing().getWorkflowEngine()
    .undeployXMOMObject(originalFqName, type, dependentObjectMode, disableChecks, revision);
  }


  @AccessControlled(associatedRight = Rights.EDIT_MDM)
  @Deprecated
  public void deleteDatatype(String originalFqName, boolean recursivlyUndeployIfDeployedAndDependenciesExist,
                             boolean deleteDependencies) throws XynaException {
    deleteDatatype(originalFqName, recursivlyUndeployIfDeployedAndDependenciesExist, deleteDependencies, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }
  
  
  @AccessControlled(associatedRight = Rights.EDIT_MDM)
  public void deleteDatatype(String originalFqName, boolean recursivlyUndeployIfDeployedAndDependenciesExist,
                             boolean deleteDependencies, Long revision) throws XynaException {
    XynaFactory.getInstance().getProcessing().getWorkflowEngine()
        .deleteDatatype(originalFqName, false, recursivlyUndeployIfDeployedAndDependenciesExist, deleteDependencies, true, revision);
  }

  @AccessControlled(associatedRight = Rights.EDIT_MDM)
  @Deprecated
  public void deleteException(String fqXmlName, boolean recursivlyUndeployIfDeployedAndDependenciesExist,
                              boolean deleteDependencies) throws XynaException {
    deleteException(fqXmlName, recursivlyUndeployIfDeployedAndDependenciesExist, deleteDependencies, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }
  
  @AccessControlled(associatedRight = Rights.EDIT_MDM)
  public void deleteException(String fqXmlName, boolean recursivlyUndeployIfDeployedAndDependenciesExist,
                              boolean deleteDependencies, Long revision) throws XynaException {
    XynaFactory.getInstance().getProcessing().getWorkflowEngine()
        .deleteException(fqXmlName, false, recursivlyUndeployIfDeployedAndDependenciesExist, deleteDependencies, true, revision);

  }

  @AccessControlled(associatedRight = Rights.EDIT_MDM)
  @Deprecated
  public void deleteWorkflow(String fqXmlName, boolean recursivlyUndeployIfDeployedAndDependenciesExist,
                             boolean deleteDependencies) throws XynaException {
    deleteWorkflow(fqXmlName, recursivlyUndeployIfDeployedAndDependenciesExist, deleteDependencies, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }
  
  @AccessControlled(associatedRight = Rights.EDIT_MDM)
  public void deleteWorkflow(String fqXmlName, boolean recursivlyUndeployIfDeployedAndDependenciesExist,
                             boolean deleteDependencies, Long revision) throws XynaException {
    XynaFactory.getInstance().getProcessing().getWorkflowEngine()
        .deleteWorkflow(fqXmlName, false, recursivlyUndeployIfDeployedAndDependenciesExist, deleteDependencies, true, revision);
    unregisterSavedWorkflow(fqXmlName);
  }

  @Deprecated
  public XMOMDatabaseSearchResult searchXMOMDatabase(List<XMOMDatabaseSelect> selects, int maxRows)
      throws XynaException {
    return searchXMOMDatabase(selects, maxRows, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }
  
  public XMOMDatabaseSearchResult searchXMOMDatabase(List<XMOMDatabaseSelect> selects, int maxRows, Long revision)
      throws XynaException {
    return XMOMSearchDispatcher.dispatchXMOMDatabaseSelects(selects, maxRows, revision);
  }


  public List<CapacityMappingStorable> getAllCapacityMappings() {
    return XynaFactory.getInstance().getProcessing().getAllCapacityMappings();
  }


  public CapacityInformation getCapacityInformation(String capacityName) {
    return XynaFactory.getInstance().getProcessing().getCapacityInformation(capacityName);
  }


  public Collection<VetoInformationStorable> listVetoInformation() throws PersistenceLayerException {
    return XynaFactory.getInstance().getProcessing().listVetoInformation();
  }


  public void createOrdertype(OrdertypeParameter ordertypeParameter) throws PersistenceLayerException,
                  XFMG_InvalidCreationOfExistingOrdertype, XFMG_FailedToAddObjectToApplication {
    XynaFactory.getInstance().getFactoryManagement().createOrdertype(ordertypeParameter);
  }


  public void modifyOrdertype(OrdertypeParameter ordertypeParameter) throws PersistenceLayerException,
      XFMG_InvalidModificationOfUnexistingOrdertype, XFMG_InvalidCapacityCardinality {
    XynaFactory.getInstance().getFactoryManagement().modifyOrdertype(ordertypeParameter);
  }


  public void deleteOrdertype(OrdertypeParameter ordertypeParameter) throws PersistenceLayerException {
    XynaFactory.getInstance().getFactoryManagement().deleteOrdertype(ordertypeParameter);
  }


  /**
   * @param runtimeContext falls null, werden die ordertypeparas aller revisions zurückgegeben 
   */
  public List<OrdertypeParameter> listOrdertypes(RuntimeContext runtimeContext) throws PersistenceLayerException {
    return XynaFactory.getInstance().getFactoryManagement().listOrdertypes(runtimeContext);
  }
  
  public List<OrdertypeParameter> listOrdertypes(SearchOrdertypeParameter sop) throws PersistenceLayerException {
    return XynaFactory.getInstance().getFactoryManagement().listOrdertypes(sop);
  }

  /**
   * QueueManagement
   */
  public void registerQueue(String uniqueName, String externalName, QueueType queueType,
                            QueueConnectData connectData) throws PersistenceLayerException {
    XynaFactory.getInstance().getFactoryManagement().registerQueue(uniqueName, externalName, queueType,
                                                                   connectData);
  }


  public void deregisterQueue(String uniqueName) throws PersistenceLayerException {
    XynaFactory.getInstance().getFactoryManagement().deregisterQueue(uniqueName);
  }

  public Collection<Queue> listQueues() throws PersistenceLayerException {
    return XynaFactory.getInstance().getFactoryManagement().listQueues();
  }

  public void restartCronLikeTimerThread() {
    getXynaProcessingPortal().restartCronLikeTimerThread();    
  }

  public List<TriggerInformation> listTriggerInformation() throws PersistenceLayerException {
    return getXynaActivationPortal().listTriggerInformation();
  }

  public List<FilterInformation> listFilterInformation() throws PersistenceLayerException {
    return getXynaActivationPortal().listFilterInformation();
  }

  public Collection<PrioritySetting> listPriorities() throws PersistenceLayerException {
    return XynaFactory.getInstance().getFactoryManagement().listPriorities();
  }

  public void removePriority(String orderType) throws PersistenceLayerException {
    XynaFactory.getInstance().getFactoryManagement().removePriority(orderType);
  }


  public void setPriority(String orderType, int priority) throws XFMG_InvalidXynaOrderPriority,
      PersistenceLayerException {
    XynaFactory.getInstance().getFactoryManagement().setPriority(orderType, priority);
  }


  public void setPriority(String orderType, int priority, Long revision) throws XFMG_InvalidXynaOrderPriority,
      PersistenceLayerException {
    XynaFactory.getInstance().getFactoryManagement().setPriority(orderType, priority, revision);
  }


  public void discoverPriority(XynaOrderServerExtension xo) {
    XynaFactory.getInstance().getFactoryManagement().discoverPriority(xo);
  }


  public Integer getPriority(String orderType) throws PersistenceLayerException {
    return XynaFactory.getInstance().getFactoryManagement().getPriority(orderType);
  }
  
  
  public void allocateAdministrativeVeto(String vetoName, String documentation) throws XPRC_AdministrativeVetoAllocationDenied, PersistenceLayerException {
    XynaFactory.getInstance().getProcessing().allocateAdministrativeVeto(vetoName, documentation);
  }
  
  
  public void freeAdministrativeVeto(String vetoName) throws XPRC_AdministrativeVetoDeallocationDenied,
                  PersistenceLayerException {
    XynaFactory.getInstance().getProcessing().freeAdministrativeVeto(vetoName);
  }

  
  public void setDocumentationOfAdministrativeVeto(String vetoName, String documentation)
                  throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    XynaFactory.getInstance().getProcessing().setDocumentationOfAdministrativeVeto(vetoName, documentation);
  }

  
  public VetoSearchResult searchVetos(VetoSelectImpl select, int maxRows) throws PersistenceLayerException {
    return XynaFactory.getInstance().getProcessing().searchVetos(select, maxRows);
  }
  
  public Map<Long, ClusterInformation> listClusterInstances() throws XynaException {
    return XynaFactory.getInstance().getFactoryManagement().getXynaClusteringServicesManagement().getClusterInstancesInformation();
  }

  public boolean requireCapacityForOrderType(String orderType, String capName, int cardinality, String applicationName,
                                             String versionName) throws PersistenceLayerException,
      XFMG_InvalidCapacityCardinality {
    
    return getXynaProcessingPortal().requireCapacityForOrderType(orderType, capName, cardinality, applicationName, versionName);
  }

  public boolean requireCapacityForOrderType(String orderType, String capacityName, int capacityCardinality, RuntimeContext runtimeContext)
      throws PersistenceLayerException, XFMG_InvalidCapacityCardinality {
    return getXynaProcessingPortal().requireCapacityForOrderType(orderType, capacityName, capacityCardinality, runtimeContext);
  }
  
  public boolean removeCapacityForOrderType(String orderType, String capacityName, String applicationName,
                                            String versionName) throws PersistenceLayerException {
    
    return getXynaProcessingPortal().removeCapacityForOrderType(orderType, capacityName, applicationName, versionName);
  }

  public boolean removeCapacityForOrderType(String orderType, String capacityName, RuntimeContext runtimeContext) throws PersistenceLayerException {
    return getXynaProcessingPortal().removeCapacityForOrderType(orderType, capacityName, runtimeContext);
  }
  
  
  @AccessControlled(associatedRight = Rights.EDIT_MDM)
  public RefactoringResult refactorXMOM(RefactoringActionParameter action) throws XDEV_RefactoringConflict, XDEV_RefactoringFault {
    return XynaFactory.getPortalInstance().getXynaDevelopmentPortal().refactorXMOM(action);
  }

  @AccessControlled(associatedRight = Rights.EDIT_MDM)
  @Deprecated
  public void deleteXMOMObject(XMOMType type, String originalFqName,
                               boolean recursivlyUndeploy, boolean recursivlyDelete, String user, String sessionId) throws XynaException {
    deleteXMOMObject(type, originalFqName, recursivlyUndeploy, recursivlyDelete, user, sessionId, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
  }
  
  @AccessControlled(associatedRight = Rights.EDIT_MDM)
  public void deleteXMOMObject(XMOMType type, String originalFqName,
                               boolean recursivlyUndeploy, boolean recursivlyDelete, String user, String sessionId, Long revision) throws XynaException {
    Path path = new Path(originalFqName, revision);
    if (XynaFactory.getInstance().getXynaDevelopment().getXynaFractalModelling().getLockManagement().lockXMOM(sessionId, user, path, type.getNiceName())) {
      try {
        THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.set(new Identity(user, sessionId));
        try {
          switch (type) {
            case DATATYPE :
              XynaFactory.getInstance().getProcessing().getWorkflowEngine().deleteDatatype(originalFqName, false, recursivlyUndeploy, recursivlyDelete, true, revision);
              break;
            case EXCEPTION :
              XynaFactory.getInstance().getProcessing().getWorkflowEngine().deleteException(originalFqName, false, recursivlyUndeploy, recursivlyDelete, true, revision);
              break;
            case WORKFLOW :
              XynaFactory.getInstance().getProcessing().getWorkflowEngine().deleteWorkflow(originalFqName, false, recursivlyUndeploy, recursivlyDelete, true, revision);
              break;
          }
        } finally {
          THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.remove();
        }
        
        XynaFactory.getInstance().getXynaDevelopment().getXynaFractalModelling().getLockManagement()
                   .propagateXMOMDelete(sessionId, user, path, type.getNiceName());
      } finally {
        XynaFactory.getInstance().getXynaDevelopment().getXynaFractalModelling().getLockManagement().unlockXMOM(sessionId, user, path, type.getNiceName());
      }
    } else {
      throw new RuntimeException(type.getNiceName() + " " + originalFqName + " is locked by another user.");
    }
  }

  @AccessControlled(associatedRight = Rights.EDIT_MDM)
  public void deleteXMOMObject(String originalFqName, XMOMType type,
                               DependentObjectMode dependentObjectMode, String user, String sessionId, Long revision) throws XynaException {
    Path path = new Path(originalFqName, revision);
    if (sessionId != null) {
      if (XynaFactory.getInstance().getXynaDevelopment().getXynaFractalModelling().getLockManagement().lockXMOM(sessionId, user, path, type.getNiceName())) {
        try {
          THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.set(new Identity(user, sessionId));
          try {
            XynaFactory.getInstance().getProcessing().getWorkflowEngine().deleteXMOMObject(originalFqName, type, false, dependentObjectMode, true, revision);
          } finally {
            THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.remove();
          }
          
          XynaFactory.getInstance().getXynaDevelopment().getXynaFractalModelling().getLockManagement()
                     .propagateXMOMDelete(sessionId, user, path, type.getNiceName());
        } finally {
          XynaFactory.getInstance().getXynaDevelopment().getXynaFractalModelling().getLockManagement().unlockXMOM(sessionId, user, path, type.getNiceName());
        }
      } else {
        throw new RuntimeException(type.getNiceName() + " " + originalFqName + " is locked by another user.");
      }
    } else {
      THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.set(new Identity(user, null));
      try {
        XynaFactory.getInstance().getProcessing().getWorkflowEngine().deleteXMOMObject(originalFqName, type, false, dependentObjectMode, true, revision);
      } finally {
        THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.remove();
      }
    }
  }

  public MessageBusManagementPortal getMessageBusManagement() {
    return ((XGUISupportPortal) getSection(XGUISupport.DEFAULT_NAME)).getMessageBusManagement();
  }

  
  @AccessControlled(associatedRight = Rights.EDIT_MDM)
  public Long publish(MessageInputParameter message) throws XynaException {
    return ((XGUISupportPortal) getSection(XGUISupport.DEFAULT_NAME)).publish(message);
  }

  
  @AccessControlled(associatedRight = Rights.EDIT_MDM)
  public boolean addSubscription(String subscriptionSessionId, MessageSubscriptionParameter subscription) {
    return ((XGUISupportPortal) getSection(XGUISupport.DEFAULT_NAME)).addSubscription(subscriptionSessionId, subscription);
  }

  
  @AccessControlled(associatedRight = Rights.EDIT_MDM)
  public boolean cancelSubscription(String subscriptionSessionId, Long subscriptionId) {
    return ((XGUISupportPortal) getSection(XGUISupport.DEFAULT_NAME)).cancelSubscription(subscriptionSessionId, subscriptionId);
  }

  
  @AccessControlled(associatedRight = Rights.EDIT_MDM)
  public MessageRetrievalResult fetchMessages(String subscriptionSessionId, Long lastReceivedId) {
    return ((XGUISupportPortal) getSection(XGUISupport.DEFAULT_NAME)).fetchMessages(subscriptionSessionId, lastReceivedId);
  }
  
  
  public void removePersistentMessage(String product, String context, String correlation, Long messageId) {
    ((XGUISupportPortal) getSection(XGUISupport.DEFAULT_NAME)).removePersistentMessage(product, context, correlation, messageId);
  }
  
  
  @AccessControlled(associatedRight = Rights.EDIT_MDM)
  @Deprecated
  public boolean lockXMOM(String sessionId, String creator, String path, String type) throws XynaException {
    Path pathWithRevision = new Path(path, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
    return lockXMOM(sessionId, creator, pathWithRevision, type);
  }

  @AccessControlled(associatedRight = Rights.EDIT_MDM)
  public boolean lockXMOM(String sessionId, String creator, Path path, String type) throws XynaException {
    return XynaFactory.getPortalInstance().getXynaDevelopmentPortal().lockXMOM(sessionId, creator, path, type);
  }

  
  @AccessControlled(associatedRight = Rights.EDIT_MDM)
  @Deprecated
  public boolean unlockXMOM(String sessionId, String creator, String path, String type) throws XynaException {
    Path pathWithRevision = new Path(path, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
    return unlockXMOM(sessionId, creator, pathWithRevision, type);
  }

  @AccessControlled(associatedRight = Rights.EDIT_MDM)
  public boolean unlockXMOM(String sessionId, String creator, Path path, String type) throws XynaException {
    return XynaFactory.getPortalInstance().getXynaDevelopmentPortal().unlockXMOM(sessionId, creator, path, type);
  }

  
  @AccessControlled(associatedRight = Rights.EDIT_MDM)
  @Deprecated
  public void publishXMOM(String sessionId, String creator, String path, String type, String payload,
                          Long autosaveCounter) throws XynaException {
    Path pathWithRevision = new Path(path, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
    publishXMOM(sessionId, creator, pathWithRevision, type, payload, autosaveCounter);
  }

  @AccessControlled(associatedRight = Rights.EDIT_MDM)
  public void publishXMOM(String sessionId, String creator, Path path, String type, String payload,
                          Long autosaveCounter) throws XynaException {
    XynaFactory.getPortalInstance().getXynaDevelopmentPortal().publishXMOM(sessionId, creator, path, type, payload, autosaveCounter);
  }

  public void addTimeWindow(TimeConstraintWindowDefinition definition) throws XynaException {
    XynaFactory.getInstance().getProcessing().getXynaScheduler().getTimeConstraintManagement().
    addTimeWindow(definition);
  }
  
  public void removeTimeWindow(String name, boolean force) throws XynaException {
    XynaFactory.getInstance().getProcessing().getXynaScheduler().getTimeConstraintManagement().
    removeTimeWindow(name,force);
  }

  public void changeTimeWindow(TimeConstraintWindowDefinition definition) throws XynaException {
    XynaFactory.getInstance().getProcessing().getXynaScheduler().getTimeConstraintManagement().
    changeTimeWindow(definition);
  }
  
  
  @AccessControlled(associatedRight = Rights.WORKINGSET_MANAGEMENT)
  public void buildApplicationVersion(String applicationName, String versionName, String comment) throws XFMG_CouldNotBuildNewVersionForApplication {
    BuildApplicationVersionParameters params = new BuildApplicationVersionParameters();
    params.setComment(comment);
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement().buildApplicationVersion(applicationName, versionName, params);
  }

  @AccessControlled(associatedRight = Rights.WORKINGSET_MANAGEMENT)
  public void buildApplicationVersion(String applicationName, String versionName, BuildApplicationVersionParameters params) throws XFMG_CouldNotBuildNewVersionForApplication {
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement().buildApplicationVersion(applicationName, versionName, params);
  }
  
  
  @AccessControlled(associatedRight = Rights.WORKINGSET_MANAGEMENT)
  @Deprecated
  public void copyApplicationIntoWorkingSet(String applicationName, String versionName, String comment, boolean overrideChanges) throws XFMG_CouldNotBuildWorkingSet {
    CopyApplicationIntoWorkspaceParameters params = new CopyApplicationIntoWorkspaceParameters();
    params.setComment(comment);
    params.setOverrideChanges(overrideChanges);
    copyApplicationIntoWorkspace(applicationName, versionName, params);
  }

  @AccessControlled(associatedRight = Rights.WORKINGSET_MANAGEMENT)
  public void copyApplicationIntoWorkspace(String applicationName, String versionName, CopyApplicationIntoWorkspaceParameters params) throws XFMG_CouldNotBuildWorkingSet {
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement().copyApplicationIntoWorkspace(applicationName, versionName, params);
  }
  
  public  List<FactoryNodeStorable> getAllFactoryNodes() {
    return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getNodeManagement().
           getAllFactoryNodes();
  }

  public ConnectionPoolManagement getConnectionPoolManagement() {
    return XynaFactory.getInstance().getXynaNetworkWarehouse().getConnectionPoolManagement();
  }

  
  public List<SharedLib> listAllSharedLibs() {
    List<Long> allRevisions = getXynaFactoryManagementPortal().getXynaFactoryControl().getRevisionManagement().getAllRevisions();
    List<SharedLib> sharedLibs = new ArrayList<SharedLib>();
    for (Long revision : allRevisions) {
      sharedLibs.addAll(listSharedLibs(revision, false));
    }
    return sharedLibs;
  }
                                              
  
  public List<SharedLib> listSharedLibs(long revision) {
    return listSharedLibs(revision, true);
  }
  
  
  public List<SharedLib> listSharedLibs(long revision, boolean withContent) {
    String basepath = RevisionManagement.getPathForRevision(PathType.SHAREDLIB, revision);
    RuntimeContext runtimeContext;
    try {
      runtimeContext = getXynaFactoryManagementPortal().getXynaFactoryControl().getRevisionManagement().getRuntimeContext(revision);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      logger.warn("Specified revision could not be found.",e);
      return Collections.emptyList();
    }
    File basedir = new File(basepath);
    File[] sharedLibFolders = basedir.listFiles(new FileFilter() {
      public boolean accept(File pathname) {
        return pathname.isDirectory();
      }
    });
    List<SharedLib> sharedLibs = new ArrayList<SharedLib>();
    if (sharedLibFolders != null) {
      for (File sharedLibFolder : sharedLibFolders) {
        String sharedLibName = sharedLibFolder.getName();
        boolean inUse =
            XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDependencyRegister()
                .getDependencyNode(sharedLibName, DependencySourceType.SHAREDLIB, revision) != null;
        final List<String> libContent = new ArrayList<String>();
        if (withContent) {
          sharedLibFolder.listFiles(new FileFilter() {

            public boolean accept(File pathname) {
              if (pathname.isFile()) {
                libContent.add(pathname.getName());
              }
              return false;
            }
          });
        }
        sharedLibs.add(new SharedLib(sharedLibName, inUse, libContent, runtimeContext));
      }
    }
    return sharedLibs;
  }

  public List<WorkspaceInformation> listWorkspaces(boolean includeProblems) {
    WorkspaceManagement workspaceManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getWorkspaceManagement();
    return workspaceManagement.listWorkspaces(includeProblems);
  }


  public List<ApplicationDefinitionInformation> listApplicationDefinitions(boolean includeProblems) {
    ApplicationManagementImpl applicationManagement =
                    (ApplicationManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
                        .getApplicationManagement();
    return applicationManagement.listApplicationDefinitions(includeProblems);
  }

  @AccessControlled(associatedRight = Rights.WORKINGSET_MANAGEMENT)
  public CreateWorkspaceResult createWorkspace(Workspace workspace) throws XFMG_CouldNotBuildNewWorkspace {
    WorkspaceManagement workspaceManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getWorkspaceManagement();
    return workspaceManagement.createWorkspace(workspace);
  }
  
  @AccessControlled(associatedRight = Rights.WORKINGSET_MANAGEMENT)
  public CreateWorkspaceResult createWorkspace(Workspace workspace, String user) throws XFMG_CouldNotBuildNewWorkspace {
    WorkspaceManagement workspaceManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getWorkspaceManagement();
    return workspaceManagement.createWorkspace(workspace, user);
  }

  @AccessControlled(associatedRight = Rights.WORKINGSET_MANAGEMENT)
  public void removeWorkspace(Workspace workspace, RemoveWorkspaceParameters params) throws XFMG_CouldNotRemoveWorkspace {
    WorkspaceManagement workspaceManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getWorkspaceManagement();
    workspaceManagement.removeWorkspace(workspace, params);
  }
  
  public void instantiateRepositoryAccessInstance(InstantiateRepositoryAccessParameters parameters, Long revision) throws XDEV_CodeAccessInitializationException {
    XynaFactory.getInstance()
               .getXynaDevelopment()
               .getXynaLibraryDevelopment()
               .getRepositoryAccessManagement()
               .instantiateRepositoryAccessInstance(parameters, revision);

  }

  public DataModelResult importDataModel(ImportDataModelParameters parameters) throws XFMG_NoSuchDataModelTypeException {
    DataModelResult dmr = new DataModelResult();
    DataModelManagement dmm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDataModelManagement();
    dmm.importDataModel( dmr, parameters);
    return dmr;
  }

  public DataModelResult modifyDataModel(ModifyDataModelParameters parameters) throws PersistenceLayerException, XFMG_NoSuchDataModelTypeException, XFMG_NoSuchDataModelException {
    DataModelResult dmr = new DataModelResult();
    DataModelManagement dmm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDataModelManagement();
    dmm.modifyDataModel(dmr, parameters);
    return dmr;
  }

  public DataModelResult removeDataModel(RemoveDataModelParameters parameters) throws PersistenceLayerException, XFMG_NoSuchDataModelTypeException, XFMG_NoSuchDataModelException {
    DataModelResult dmr = new DataModelResult();
    DataModelManagement dmm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDataModelManagement();
    dmm.removeDataModel(dmr, parameters);
    return dmr;
  }

  public SearchResult<?> search(SearchRequestBean searchRequest) throws XynaException {
    switch( searchRequest.getArchiveIdentifier() ) {
      case orderarchive :
        if (searchRequest.isLocal()) {
          OrderInstanceSelect select = (OrderInstanceSelect) SelectionParser.generateSelectObjectFromSearchRequestBean(searchRequest);
          for (OrderBy ob : searchRequest.getOrderBys()) {
            select.addOrderBy(OrderInstanceColumn.getByColumnName(ob.colName), ob.asc ? OrderByDesignators.ASC : OrderByDesignators.DESC);
          }
          int maxRows = searchRequest.getMaxRows();
          String sm = searchRequest.getAdditionalParameter("searchMode");
          OrderInstanceResult oir = searchOrderInstances(select, maxRows, sm == null ?  SearchMode.FLAT : SearchMode.valueOf(sm));
          SearchResult<OrderInstance> res = new SearchResult<OrderInstance>();
          res.setCount(oir.getCount());
          res.setResult(oir.getResult());
          return res;
        }
        return getInterlinkSearchDispatcher().dispatch(searchRequest);
      case vetos : 
        if (searchRequest.isLocal()) {
          VetoSelectImpl select = (VetoSelectImpl) SelectionParser.generateSelectObjectFromSearchRequestBean(searchRequest);       
          VetoSearchResult vsr = searchVetos(select, searchRequest.getMaxRows());
          SearchResult<VetoInformationStorable> res = new SearchResult<VetoInformationStorable>();
          res.setCount(vsr.getCount());
          res.setResult(vsr.getResult());
          return res;
        }
        return getInterlinkSearchDispatcher().dispatch(searchRequest);
      case datamodel :
        DataModelManagement dmm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDataModelManagement();
        return dmm.search(searchRequest);
      case deploymentitem:
        DeploymentItemStateManagement dism = getXynaFactoryManagementPortal().getXynaFactoryControl().getDeploymentItemStateManagement();
        return dism.search(searchRequest);
      case runtimecontext:
        RevisionManagement rm = getXynaFactoryManagementPortal().getXynaFactoryControl().getRevisionManagement();
        List<RuntimeContext> result = new ArrayList<RuntimeContext>();
        result.addAll(rm.getWorkspaces().values());
        result.addAll(rm.getApplications());
        return new SearchResult<RuntimeContext>(result, result.size());
      case xmomdetails:
        //im Moment wird nur die Suche nach ApplicationDefinitions eines XMOMObjekts unterstützt
        ApplicationManagement appMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement();
        return appMgmt.searchApplicationDefinitions(searchRequest);
      case orderInputSource :
        return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderInputSourceManagement()
            .searchInputSources(searchRequest);        
      default :
        throw new UnsupportedOperationException("Search for "+searchRequest.getArchiveIdentifier()+" is unsupported" );
    }
  }

  private InterlinkSearchDispatcher getInterlinkSearchDispatcher() {
    return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getNodeManagement().getInterlinkSearchDispatcher();
  }

  public DeploymentMarker createDeploymentMarker(DeploymentMarker marker) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, PersistenceLayerException {
    DeploymentMarkerManagement dmm = getXynaFactoryManagementPortal().getXynaFactoryControl().getDeploymentMarkerManagement();
    return dmm.createDeploymentMarker(marker);
  }

  public void deleteDeploymentMarker(DeploymentMarker marker) throws PersistenceLayerException {
    DeploymentMarkerManagement dmm = getXynaFactoryManagementPortal().getXynaFactoryControl().getDeploymentMarkerManagement();
    dmm.deleteDeploymentMarker(marker);
  }

  public void modifyDeploymentMarker(DeploymentMarker marker) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, PersistenceLayerException {
    DeploymentMarkerManagement dmm = getXynaFactoryManagementPortal().getXynaFactoryControl().getDeploymentMarkerManagement();
    dmm.modifyDeploymentMarker(marker);
  }

  public void createOrderInputSource(OrderInputSourceStorable inputSource) throws XynaException {
    getXynaFactoryManagementPortal().getXynaFactoryManagementODS().getOrderInputSourceManagement().createOrderInputSource(inputSource);
  }

  public XynaOrderCreationParameter generateOrderInput(long inputSourceId,
                                                       OptionalOISGenerateMetaInformation parameters)
      throws XynaException {
    return getXynaFactoryManagementPortal().getXynaFactoryManagementODS().getOrderInputSourceManagement()
        .generateOrderInput(inputSourceId, parameters);
  }

  public XynaOrderCreationParameter generateOrderInput(long inputSourceId) throws XynaException {
    return generateOrderInput(inputSourceId, new OptionalOISGenerateMetaInformation());
  }


  public List<PluginDescription> listPluginDescriptions(PluginType type) {
    switch (type) {
      case orderInputSource :
        return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderInputSourceManagement()
            .listOrderInputSourceTypes();
      case dataModelType :
        return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDataModelManagement().listDataModelTypeDescriptions();
      case repositoryAccess : 
        return XynaFactory.getInstance().getXynaDevelopment()
            .getXynaLibraryDevelopment().getRepositoryAccessManagement().listRepositoryAccessImpls();
      default :
        throw new UnsupportedOperationException("listPluginDescriptions for " + type + " is unsupported");
    }
  }

  public void modifyOrderInputSource(OrderInputSourceStorable inputSource) throws XynaException {
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderInputSourceManagement()
        .modifyOrderInputSource(inputSource);
  }

  public void deleteOrderInputSource(long inputSourceId) throws XynaException {
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderInputSourceManagement()
        .deleteOrderInputSource(inputSourceId);
  }

  public PasswordExpiration getPasswordExpiration(String userName) throws PersistenceLayerException {
    return getXynaFactoryManagementPortal().getPasswordExpiration(userName);
  }

  public void modifyRuntimeContextDependencies(RuntimeDependencyContext owner, List<RuntimeDependencyContext> newDependencies, boolean force, String user) throws XynaException {
    getXynaFactoryControl().getRuntimeContextDependencyManagement().modifyDependencies(owner, newDependencies, user, force, true);
  }
/*
  @Override
  public XMOMGuiReply processXmomGuiRequest(Session session, XMOMGuiRequest request) {
    XMOMGui xmomGui = (XMOMGui)getSection(XMOMGui.DEFAULT_NAME);
    return xmomGui.processXmomGuiRequest(session, request);
  }
*/
  @Override
  public StatisticsStore getStatisticsStore() {
    return XynaFactory.getInstance().getXynaNetworkWarehouse().getStatisticsStore();
  }


}
