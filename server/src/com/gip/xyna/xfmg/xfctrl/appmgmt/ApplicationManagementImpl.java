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
package com.gip.xyna.xfmg.xfctrl.appmgmt;



import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.rmi.RemoteException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.FileUtils;
import com.gip.xyna.FileUtils.FileInputStreamCreator;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.update.Update.ExecutionTime;
import com.gip.xyna.update.Updater;
import com.gip.xyna.update.Version;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.utils.collections.GraphUtils;
import com.gip.xyna.utils.collections.GraphUtils.ConnectedEdges;
import com.gip.xyna.utils.collections.LruCacheWithTimingInformation;
import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.collections.Triple;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.EventHandling;
import com.gip.xyna.utils.misc.StringParameter.StringParameterParsingException;
import com.gip.xyna.xact.exceptions.XACT_AdditionalDependencyDeploymentException;
import com.gip.xyna.xact.exceptions.XACT_DuplicateTriggerDefinitionException;
import com.gip.xyna.xact.exceptions.XACT_ErrorDuringTriggerAdditionRollback;
import com.gip.xyna.xact.exceptions.XACT_FilterImplClassNotFoundException;
import com.gip.xyna.xact.exceptions.XACT_FilterNotFound;
import com.gip.xyna.xact.exceptions.XACT_IncompatibleTriggerImplException;
import com.gip.xyna.xact.exceptions.XACT_JarFileUnzipProblem;
import com.gip.xyna.xact.exceptions.XACT_LibOfFilterImplNotFoundException;
import com.gip.xyna.xact.exceptions.XACT_LibOfTriggerImplNotFoundException;
import com.gip.xyna.xact.exceptions.XACT_TriggerImplClassNotFoundException;
import com.gip.xyna.xact.exceptions.XACT_TriggerInstanceNotFound;
import com.gip.xyna.xact.exceptions.XACT_TriggerNotFound;
import com.gip.xyna.xact.trigger.DeployFilterParameter;
import com.gip.xyna.xact.trigger.Filter;
import com.gip.xyna.xact.trigger.FilterInformation;
import com.gip.xyna.xact.trigger.FilterInformation.FilterInstanceInformation;
import com.gip.xyna.xact.trigger.FilterInstanceStorable;
import com.gip.xyna.xact.trigger.FilterStorable;
import com.gip.xyna.xact.trigger.Trigger;
import com.gip.xyna.xact.trigger.TriggerInformation;
import com.gip.xyna.xact.trigger.TriggerInformation.TriggerInstanceInformation;
import com.gip.xyna.xact.trigger.TriggerInstanceStorable;
import com.gip.xyna.xact.trigger.TriggerStorable;
import com.gip.xyna.xact.trigger.XynaActivationTrigger;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.BasicProjectCreationOrChangeEvent;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.BatchRepositoryEvent;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.EmptyRepositoryEvent;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.EventType;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.RepositoryEvent;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.SingleRepositoryEvent;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.XMOMChangeEvent;
import com.gip.xyna.xdev.exceptions.XDEV_AlreadyExistsException;
import com.gip.xyna.xdev.exceptions.XDEV_CodeAccessInitializationException;
import com.gip.xyna.xdev.exceptions.XDEV_PathNotFoundException;
import com.gip.xyna.xdev.exceptions.XDEV_RepositoryAccessException;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.RepositoryAccess;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.RepositoryAccess.RecursionDepth;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.RepositoryAccess.RepositoryTransaction;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.RepositoryAccessManagement;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.RepositoryItemModification;
import com.gip.xyna.xdev.xlibdev.xmomaccess.XMOMAccess;
import com.gip.xyna.xdev.xlibdev.xmomaccess.XMOMAccess.Component;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.exceptions.XFMG_ApplicationConfigurationChanged;
import com.gip.xyna.xfmg.exceptions.XFMG_ApplicationIsNotInstalledOnRemoteClusterNode;
import com.gip.xyna.xfmg.exceptions.XFMG_ApplicationMustBeStopped;
import com.gip.xyna.xfmg.exceptions.XFMG_ClusterComponentConfigurationException;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotBuildNewVersionForApplication;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotBuildWorkingSet;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotExportApplication;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotImportApplication;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotRemoveApplication;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotStartApplication;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotStopApplication;
import com.gip.xyna.xfmg.exceptions.XFMG_CronLikeOrderCopyException;
import com.gip.xyna.xfmg.exceptions.XFMG_DuplicateApplicationName;
import com.gip.xyna.xfmg.exceptions.XFMG_DuplicateVersionForApplicationName;
import com.gip.xyna.xfmg.exceptions.XFMG_FailedToAddObjectToApplication;
import com.gip.xyna.xfmg.exceptions.XFMG_FailedToRemoveObjectFromApplication;
import com.gip.xyna.xfmg.exceptions.XFMG_FailedToRemoveObjectFromApplicationBecauseHasDependentObjects;
import com.gip.xyna.xfmg.exceptions.XFMG_FailedToRemoveObjectFromApplicationBecauseOfMissingWorkingset;
import com.gip.xyna.xfmg.exceptions.XFMG_InputSourceNotUniqueException;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidCapacityCardinality;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidCreationOfExistingOrdertype;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidModificationOfUnexistingOrdertype;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidXynaOrderPriority;
import com.gip.xyna.xfmg.exceptions.XFMG_ObjectAlreadyInDependencyHierarchyException;
import com.gip.xyna.xfmg.exceptions.XFMG_ObjectNotFoundException;
import com.gip.xyna.xfmg.exceptions.XFMG_PredefinedXynaObjectException;
import com.gip.xyna.xfmg.exceptions.XFMG_RunningOrdersException;
import com.gip.xyna.xfmg.exceptions.XFMG_RuntimeContextNotFoundException;
import com.gip.xyna.xfmg.exceptions.XFMG_SHARED_LIB_NOT_FOUND;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownClusterInstanceIDException;
import com.gip.xyna.xfmg.extendedstatus.XynaExtendedStatusManagement;
import com.gip.xyna.xfmg.xclusteringservices.ClusterState;
import com.gip.xyna.xfmg.xclusteringservices.ClusterStateChangeHandler;
import com.gip.xyna.xfmg.xclusteringservices.Clustered;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProvider;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProvider.InvalidIDException;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProviderTools;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProviderTools.RMIRunnable;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProviderTools.RMIRunnableNoException;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagement;
import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagementInterface;
import com.gip.xyna.xfmg.xfctrl.RMIManagement.InitializableRemoteInterface;
import com.gip.xyna.xfmg.xfctrl.RMIManagement.RMIImplFactory;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable.ApplicationEntryType;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.CapacityRequirementXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.CapacityXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.FilterInstanceXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.FilterXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.InheritanceRuleXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.MonitoringLevelXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.OrderInputSourceXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.OrdertypeXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.PriorityXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.RuntimeContextRequirementXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.SharedLibXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.TriggerInstanceXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.TriggerXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.XMOMStorableXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.XMOMXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.XynaPropertyXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ClearWorkingSet.RevisionContentBlackWhiteList;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ClearWorkingSet.RevisionContentBlackWhiteListBean;
import com.gip.xyna.xfmg.xfctrl.appmgmt.OrderEntrance.OrderEntranceType;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RevisionOrderControl.OrderEntryInterfacesCouldNotBeClosedException;
import com.gip.xyna.xfmg.xfctrl.appmgmt.events.AppMgmtEvent;
import com.gip.xyna.xfmg.xfctrl.appmgmt.events.AppMgmtEventHandler;
import com.gip.xyna.xfmg.xfctrl.appmgmt.events.ApplicationExportEvent;
import com.gip.xyna.xfmg.xfctrl.appmgmt.events.ApplicationImportEvent;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderDispatcher;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderType;
import com.gip.xyna.xfmg.xfctrl.classloading.SharedLibDeploymentAlgorithm;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl.Operation;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyNode;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemRegistry;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState.DeploymentLocation;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateImpl;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateManagement;
import com.gip.xyna.xfmg.xfctrl.deploystate.DisplayState;
import com.gip.xyna.xfmg.xfctrl.deploystate.PublishedInterfaces;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.MemberVariableInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.OperationInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.TypeInterface;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.ApplicationDefinition;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xfctrl.rtctxmgmt.RuntimeContextChangeHandler;
import com.gip.xyna.xfmg.xfctrl.rtctxmgmt.RuntimeContextManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.ApplicationName;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.XMOMVersionStorable;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseEntry;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseType;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBuilds;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyDouble;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyInt;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyLong;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyString;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.OrderInputSourceManagement;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.storables.OrderInputSourceStorable;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeManagement;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeParameter;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeParameter.DestinationValueParameter;
import com.gip.xyna.xfmg.xods.priority.PriorityManagement;
import com.gip.xyna.xfmg.xopctrl.usermanagement.TemporarySessionAuthentication;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xmcp.exceptions.XMCP_RMI_BINDING_ERROR;
import com.gip.xyna.xmcp.xfcli.impl.ListsysteminfoImpl;
import com.gip.xyna.xmcp.xguisupport.messagebus.Publisher;
import com.gip.xyna.xnwh.exceptions.XFMG_ObjectAlreadyExists;
import com.gip.xyna.xnwh.exceptions.XFMG_ObjectNotDeployed;
import com.gip.xyna.xnwh.exceptions.XFMG_ObjectUnkownInDeploymentItemStateManagement;
import com.gip.xyna.xnwh.exceptions.XFMG_WrongDeploymentState;
import com.gip.xyna.xnwh.exceptions.XNWH_InvalidXMOMStorablePathException;
import com.gip.xyna.xnwh.exceptions.XNWH_NoPersistenceLayerConfiguredForTableException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_ODSNameChangedButNotDeployedException;
import com.gip.xyna.xnwh.exceptions.XNWH_ODSNameMustBeUniqueException;
import com.gip.xyna.xnwh.exceptions.XNWH_PersistenceLayerInstanceIdUnknownException;
import com.gip.xyna.xnwh.exceptions.XNWH_StorableNotFoundException;
import com.gip.xyna.xnwh.persistence.FactoryWarehouseCursor;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.PreparedQueryCache;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.persistence.xmom.ODSRegistrationParameter;
import com.gip.xyna.xnwh.persistence.xmom.XMOMODSMapping;
import com.gip.xyna.xnwh.persistence.xmom.XMOMODSMappingUtils;
import com.gip.xyna.xnwh.persistence.xmom.XMOMPersistenceManagement;
import com.gip.xyna.xnwh.persistence.xmom.XMOMODSMappingUtils.DiscoveryResult;
import com.gip.xyna.xnwh.selection.parsing.SearchRequestBean;
import com.gip.xyna.xnwh.selection.parsing.SearchResult;
import com.gip.xyna.xprc.XynaOrderServerExtension.ExecutionType;
import com.gip.xyna.xprc.XynaProcessing.DispatcherEntry;
import com.gip.xyna.xprc.XynaProcessingBase;
import com.gip.xyna.xprc.XynaProcessingPortal.DispatcherIdentification;
import com.gip.xyna.xprc.exceptions.XFMG_CouldNotModifyRuntimeContextDependenciesException;
import com.gip.xyna.xprc.exceptions.XFMG_NoCorrespondingApplicationException;
import com.gip.xyna.xprc.exceptions.XFMG_RuntimeContextStillReferencedException;
import com.gip.xyna.xprc.exceptions.XPRC_CAPACITY_ALREADY_DEFINED;
import com.gip.xyna.xprc.exceptions.XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain;
import com.gip.xyna.xprc.exceptions.XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState;
import com.gip.xyna.xprc.exceptions.XPRC_DESTINATION_NOT_FOUND;
import com.gip.xyna.xprc.exceptions.XPRC_DeploymentDuringUndeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_DeploymentHandlerException;
import com.gip.xyna.xprc.exceptions.XPRC_ExclusiveDeploymentInProgress;
import com.gip.xyna.xprc.exceptions.XPRC_ExecutionDestinationMissingException;
import com.gip.xyna.xprc.exceptions.XPRC_INVALID_MONITORING_TYPE;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXMLForObjectCreationException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXmlMissingRequiredElementException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMObjectCreationException;
import com.gip.xyna.xprc.exceptions.XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH;
import com.gip.xyna.xprc.exceptions.XPRC_TimeoutWhileWaitingForUnaccessibleOrderException;
import com.gip.xyna.xprc.exceptions.XPRC_UnDeploymentHandlerException;
import com.gip.xyna.xprc.exceptions.XPRC_VERSION_DETECTION_PROBLEM;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcessManagement;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement;
import com.gip.xyna.xprc.xfractwfe.OrdersInUse;
import com.gip.xyna.xprc.xfractwfe.OrdersInUse.FillingMode;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling.DeploymentHandler;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling.UndeploymentHandler;
import com.gip.xyna.xprc.xfractwfe.base.XynaProcess;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.AdditionalDependencyContainer;
import com.gip.xyna.xprc.xfractwfe.generation.AdditionalDependencyContainer.AdditionalDependencyType;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DeploymentLocks;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.AssumedDeadlockException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.MDMParallelDeploymentException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.WorkflowProtectionMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xpce.cleanup.CleanupDispatcher;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xpce.dispatcher.FractalWorkflowDestination;
import com.gip.xyna.xprc.xpce.dispatcher.XynaDispatcher;
import com.gip.xyna.xprc.xpce.execution.ExecutionDispatcher;
import com.gip.xyna.xprc.xpce.monitoring.MonitoringDispatcher;
import com.gip.xyna.xprc.xpce.parameterinheritance.ParameterInheritanceManagement;
import com.gip.xyna.xprc.xpce.parameterinheritance.ParameterInheritanceManagement.ParameterType;
import com.gip.xyna.xprc.xpce.parameterinheritance.rules.InheritanceRule;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xpce.planning.PlanningDispatcher;
import com.gip.xyna.xprc.xprcods.capacitymapping.CapacityMappingDatabase;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderCount;
import com.gip.xyna.xprc.xprcods.ordercontextconfiguration.OrderContextConfiguration;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase;
import com.gip.xyna.xprc.xsched.CapacityInformation;
import com.gip.xyna.xprc.xsched.CapacityManagement.State;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder;
import com.healthmarketscience.rmiio.RemoteInputStream;
import com.healthmarketscience.rmiio.RemoteInputStreamClient;
import com.healthmarketscience.rmiio.SimpleRemoteInputStream;



public class ApplicationManagementImpl extends FunctionGroup implements ApplicationManagement, ApplicationRemoteInterface, Clustered {

  private static final Logger logger = CentralFactoryLogging.getLogger(ApplicationManagementImpl.class);
  
  public static final String DEFAULT_NAME = "ApplicationManagement";
  public static final String WORKINGSET_VERSION_NAME = "workingset";
  public static final String XML_APPLICATION_FILENAME = "application.xml";

  private ODS ods;
  private PreparedQueryCache queryCache;
  private String sqlGetRuntimeApplicationEntries;
  private String sqlGetApplicationDefinitionEntries;
  private String sqlGetRuntimeApplicationEntry;
  private String sqlGetApplicationDefinitionEntry;
  private String sqlGetRuntimeApplication;
  private String sqlGetApplicationDefinition;
  private String sqlcountRuntimeApplicationEntries;
  private String sqlcountApplicationDefinitionEntries;
  private String sqlGetRuntimeApplicationsByName;
  private Lock writelock;

  private DependencyRegister dependencyRegister;
  private RevisionManagement revisionManagement;
  private ClassLoaderDispatcher classLoaderDispatcher;
  private XynaActivationTrigger xynaActivationTrigger;
  private ReentrantLock buildLock = new ReentrantLock();
  
  private final EventHandling<AppMgmtEvent, AppMgmtEventHandler> eventHandling; 


  public enum BasicApplicationName {
    Base, Processing;

    public static BasicApplicationName valueOfOrNull(String name) {

      for (BasicApplicationName c : values()) {
        if (c.name().equals(name)) {
          return c;
        }
      }

      return null;
    }
  }


  private Map<BasicApplicationName, Application> basicApplications; //cache für Basis-Applications (enthält immer die aktuellste Version)

  //Cache, um für ein XMOMObject zu ermitteln, in welchen ApplicationDefinitions es (expl. oder impl.)
  //enthalten ist (war für die Anzeige von Informationen zu XMOM-Objekten im ProcessModeller gedacht,
  //ist in der GUI aber noch nicht umgesetzt)
  private Map<Long, Map<XMOMDatabaseType, Map<String, Set<ApplicationDefinitionInformation>>>> applicationDefinitionCache;


  public static final XynaPropertyBuilds<DestinationKey> START_APPLICATIONS_DESTINATION =
      new XynaPropertyBuilds<DestinationKey>("xfmg.xfctrl.appmgmt.StartApplications.Destination",
                                             new DestinationKey("xfmg.xfctrl.appmgmt.StartApplications", "GlobalApplicationMgmt", "1.0"));
  public static final XynaPropertyBuilds<DestinationKey> STOP_APPLICATIONS_DESTINATION =
      new XynaPropertyBuilds<DestinationKey>("xfmg.xfctrl.appmgmt.StopApplications.Destination",
                                             new DestinationKey("xfmg.xfctrl.appmgmt.StopApplications", "GlobalApplicationMgmt", "1.0"));
  public static final XynaPropertyBuilds<DestinationKey> LIST_APPLICATIONS_DESTINATION =
      new XynaPropertyBuilds<DestinationKey>("xfmg.xfctrl.appmgmt.ListApplications.Destination",
                                             new DestinationKey("xfmg.xfctrl.appmgmt.ListApplications", "GlobalApplicationMgmt", "1.0"));
  public static final XynaPropertyBuilds<DestinationKey> LIST_LOCAL_APPLICATIONS_DESTINATION =
                  new XynaPropertyBuilds<DestinationKey>("xfmg.xfctrl.appmgmt.ListLocalApplications.Destination",
                                                         new DestinationKey("xfmg.xfctrl.appmgmt.ListLocalApplications", "GlobalApplicationMgmt", "1.0.8"));

  private static final XynaPropertyInt OPEN_FILES_SAFETY_MARGIN = new XynaPropertyInt("xyna.xfmg.xfctrl.appmgmt.openfiles.safetymargin",
                                                                                      100)
      .setDefaultDocumentation(DocumentationLanguage.EN,
                               "Number of open files that can still be opened after an application is imported or built.");

  private static final XynaPropertyDouble PERMGEN_PREDICTION_JAR_FACTOR =
      new XynaPropertyDouble("xyna.xfmg.xfctrl.appmgmt.permgen.jarfactor", 0.2)
          .setDefaultDocumentation(DocumentationLanguage.EN,
                                   "Predicting the permgen space of an additional application, the multiplier used on the size of jar files.");

  private static final XynaPropertyDouble PERMGEN_PREDICTION_CLASS_FACTOR =
      new XynaPropertyDouble("xyna.xfmg.xfctrl.appmgmt.permgen.classfactor", 2.2)
          .setDefaultDocumentation(DocumentationLanguage.EN,
                                   "Predicting the permgen space of an additional application, the multiplier used on the size of xmom class files.");

  private static final XynaPropertyLong PERMGEN_PREDICTION_CONSTANT = new XynaPropertyLong("xyna.xfmg.xfctrl.appmgmt.permgen.safetymargin",
                                                                                           20 * 1000 * 1000L)
      .setDefaultDocumentation(DocumentationLanguage.EN,
                               "Predicting the permgen space of an additional application, the safety margin in bytes.");

  public static final XynaPropertyBuilds<DestinationKey> REMOVE_APPLICATIONS_DESTINATION =
      new XynaPropertyBuilds<DestinationKey>("xfmg.xfctrl.appmgmt.RemoveApplications.Destination",
                                             new DestinationKey("xfmg.xfctrl.appmgmt.RemoveApplications", "GlobalApplicationMgmt", "1.0"));
  public static final XynaPropertyBuilds<DestinationKey> DEPLOY_APPLICATIONS_DESTINATION =
      new XynaPropertyBuilds<DestinationKey>("xfmg.xfctrl.appmgmt.DeployApplications.Destination",
                                             new DestinationKey("xfmg.xfctrl.appmgmt.DeployApplications", "GlobalApplicationMgmt", "1.0"));
  
  public static final XynaPropertyBoolean USE_APPLICATION_ENTRY_CACHE =
                  new XynaPropertyBoolean("xfmg.xfctrl.appmgmt.useApplicationEntryCache", true);

  private static final String EXCLUDED_SUBTYPES_OF_PREFIX = "xfmg.xfctrl.appmgmt.excludedsubtypesof";
  private static Map<Long, XynaPropertyString> excludedSubtypesOfProperties;
  


  public ApplicationManagementImpl() throws XynaException {
    super();
    eventHandling = new EventHandling<>();
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  @Override
  protected void init() throws XynaException {

    logger.trace("Executing ApplicationManagement.init()");

    ods = ODSImpl.getInstance();
    ods.registerStorable(ApplicationStorable.class);
    ods.registerStorable(ApplicationEntryStorable.class);

    transferFromODSTypeToODSType(ODSConnectionType.HISTORY, ODSConnectionType.DEFAULT, ApplicationEntryStorable.TABLE_NAME,
                                 ApplicationEntryStorable.class);
    transferFromODSTypeToODSType(ODSConnectionType.HISTORY, ODSConnectionType.DEFAULT, ApplicationStorable.TABLE_NAME,
                                 ApplicationStorable.class);

    queryCache = new PreparedQueryCache();

    sqlGetRuntimeApplicationEntries =
        "select * from " + ApplicationEntryStorable.TABLE_NAME + " where " + ApplicationEntryStorable.COL_APPLICATION + " = ? and "
            + ApplicationEntryStorable.COL_VERSION + " = ? and (" + ApplicationStorable.COL_PARENT_REVISION + " is null or "
            + ApplicationStorable.COL_PARENT_REVISION + " = 0)";
    sqlGetApplicationDefinitionEntries =
        "select * from " + ApplicationEntryStorable.TABLE_NAME + " where " + ApplicationEntryStorable.COL_APPLICATION + " = ? " + "and "
            + ApplicationStorable.COL_PARENT_REVISION + " = ?";
    sqlcountRuntimeApplicationEntries =
        "select count(*) from " + ApplicationEntryStorable.TABLE_NAME + " where " + ApplicationEntryStorable.COL_APPLICATION + " = ? and "
            + ApplicationEntryStorable.COL_VERSION + " = ? and (" + ApplicationStorable.COL_PARENT_REVISION + " is null or "
            + ApplicationStorable.COL_PARENT_REVISION + " = 0)";
    sqlcountApplicationDefinitionEntries =
        "select count(*) from " + ApplicationEntryStorable.TABLE_NAME + " where " + ApplicationEntryStorable.COL_APPLICATION + " = ? "
            + "and " + ApplicationStorable.COL_PARENT_REVISION + " = ?";
    sqlGetRuntimeApplicationEntry =
        "select * from " + ApplicationEntryStorable.TABLE_NAME + " where " + ApplicationEntryStorable.COL_APPLICATION + " = ? and "
            + ApplicationEntryStorable.COL_VERSION + " = ? and " + ApplicationEntryStorable.COL_NAME + " = ? and  "
            + ApplicationEntryStorable.COL_TYPE + " = ? and (" + ApplicationStorable.COL_PARENT_REVISION + " is null or "
            + ApplicationStorable.COL_PARENT_REVISION + " = 0)";
    sqlGetApplicationDefinitionEntry =
        "select * from " + ApplicationEntryStorable.TABLE_NAME + " where " + ApplicationEntryStorable.COL_APPLICATION + " = ?" + " and "
            + ApplicationEntryStorable.COL_NAME + " = ? and  " + ApplicationEntryStorable.COL_TYPE + " = ? and "
            + ApplicationStorable.COL_PARENT_REVISION + " = ?";
    sqlGetRuntimeApplication =
        "select * from " + ApplicationStorable.TABLE_NAME + " where " + ApplicationStorable.COL_NAME + " = ? and "
            + ApplicationStorable.COL_VERSION + " = ? and (" + ApplicationStorable.COL_PARENT_REVISION + " is null or "
            + ApplicationStorable.COL_PARENT_REVISION + " = 0)";
    sqlGetApplicationDefinition =
        "select * from " + ApplicationStorable.TABLE_NAME + " where " + ApplicationStorable.COL_NAME + " = ? and "
            + ApplicationStorable.COL_PARENT_REVISION + " = ?";
    sqlGetRuntimeApplicationsByName =
        "select * from " + ApplicationStorable.TABLE_NAME + " where " + ApplicationStorable.COL_NAME + " = ? and "
            + "(" + ApplicationStorable.COL_PARENT_REVISION + " is null or "
            + ApplicationStorable.COL_PARENT_REVISION + " = 0)";

    writelock = new ReentrantLock();

    basicApplications = new ConcurrentHashMap<BasicApplicationName, Application>();

    applicationDefinitionCache = new HashMap<Long, Map<XMOMDatabaseType, Map<String, Set<ApplicationDefinitionInformation>>>>();

    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask("ApplicationManagementImpl.initCluster", "ApplicationManagementImpl.initCluster").
        before(XynaClusteringServicesManagement.class).
        execAsync(new Runnable() { public void run() { initCluster(); }});
    
    fExec.addTask(ApplicationManagementImpl.class, "ApplicationManagementImpl.initAll").
      after(RevisionManagement.class).
      execAsync(new Runnable() { public void run() { initAll(); }});
  }


  private void initCluster() {
    try {
      XynaFactory.getInstance().getFactoryManagement().getXynaClusteringServicesManagement()
          .registerClusterableComponent(ApplicationManagementImpl.this);
    } catch (XFMG_ClusterComponentConfigurationException e) {
      throw new RuntimeException(e); //passiert nicht, wegen futureexecution before-beziehung.
    }
  }


  private void initAll() {
    //factory pfade waren im init noch nicht erreichbar
    dependencyRegister = XynaFactory.getInstance().getFactoryManagementPortal().getXynaFactoryControl().getDependencyRegister();
    revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    classLoaderDispatcher = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher();
    xynaActivationTrigger = XynaFactory.getInstance().getActivation().getActivationTrigger();
    try {
      //Deployment findet in WorkflowDatabase.initLater statt

      ODSConnection con = ods.openConnection();
      try {
        //für gestoppte Applications die RMI/CLI-Schnittstelle sperren
        Collection<ApplicationStorable> allApplications = con.loadCollection(ApplicationStorable.class);
        for (ApplicationStorable app : allApplications) {
          if (app.getStateAsEnum() == ApplicationState.STOPPED || app.getStateAsEnum() == ApplicationState.AUDIT_MODE) {
            long revision;
            try {
              revision = revisionManagement.getRevision(new Application(app.getName(), app.getVersion()));
            } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
              logger.error("Could not find revision for application " + app.getName() + " and version " + app.getVersion(), e);
              continue;
            }
            RevisionOrderControl roc = new RevisionOrderControl(revision);
            roc.closeRMICLI();
          } else if (app.isApplicationDefinition()) {
            ApplicationDefinitionKey key = new ApplicationDefinitionKey(app);
            applicationDefinitionVersionNameCache.put(key, new WorkingSet(app.getVersion()));
          }
          RevisionOrderControl.applyCustomOrderEntries(app.getName(), app.getVersion(), app.getStateAsEnum().isRunning());
        }

        //Basis-Applications cachen
        refreshBasicApplicationCache(allApplications);
      } finally {
        if (con != null) {
          con.closeConnection();
        }
      }

      XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
          .addUndeploymentHandler(DeploymentHandling.PRIORITY_XPRC, new UndeploymentHandler() {
            

            private final Set<Long> changedRevisions = new HashSet<Long>();
            
            public void exec(TriggerInstanceStorable object) {
              appHandling(object.getRevision(), object.getTriggerInstanceName(), ApplicationEntryType.TRIGGERINSTANCE);
            }


            public void exec(FilterInstanceStorable object) {
              appHandling(object.getRevision(), object.getFilterInstanceName(), ApplicationEntryType.FILTERINSTANCE);
            }


            public void exec(Capacity object) {
              removeObjectFromAllApplications(object.getCapName(), ApplicationEntryType.CAPACITY);
            }


            public void exec(GenerationBase object) throws XPRC_UnDeploymentHandlerException {
              //beim Undeployment sollen die Objekte noch in den Application-Definitionen
              //erhalten bleiben und erst beim endgültigen Löschen entfernt werden

              clearApplicationDefinitionCache(object.getRevision());
              synchronized (changedRevisions) {
                changedRevisions.add(object.getRevision());
              }
            }


            public void exec(DestinationKey object) {
              Long revision;
              try {
                revision = revisionManagement.getRevision(object.getRuntimeContext());
              } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
                logger.error("Could not remove ordertype from " + object.getRuntimeContext(), e);
                return;
              }
              appHandling(revision, object.getOrderType(), ApplicationEntryType.ORDERTYPE);
            }


            public void finish() throws XPRC_UnDeploymentHandlerException {
              synchronized (changedRevisions) {
                for (Long rev : changedRevisions) {
                  updateApplicationDetailsCache(rev);
                }
                changedRevisions.clear();
              }
            }


            public boolean executeForReservedServerObjects() {
              return true;
            }


            public void exec(FilterStorable object) {
              appHandling(object.getRevision(), object.getFilterName(), ApplicationEntryType.FILTER);
            }


            private void appHandling(Long revision, String name, ApplicationEntryType type) {
              if (revisionManagement.isWorkspaceRevision(revision)) {
                removeObjectFromAllApplications(name, type, revision);
              }
              updateApplicationDetailsCache(revision);
            }


            public void exec(TriggerStorable object) {
              appHandling(object.getRevision(), object.getTriggerName(), ApplicationEntryType.TRIGGER);
            }
          });

      XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
          .addDeploymentHandler(DeploymentHandling.PRIORITY_XPRC, new DeploymentHandler() {
            
            private final Set<Long> changedRevisions = new HashSet<Long>();

            public void exec(GenerationBase object, DeploymentMode mode) throws XPRC_DeploymentHandlerException {
              clearApplicationDefinitionCache(object.getRevision());
              synchronized (changedRevisions) {
                changedRevisions.add(object.getRevision());
              }
            }


            public void finish(boolean success) throws XPRC_DeploymentHandlerException {
              synchronized (changedRevisions) {
                for (Long rev : changedRevisions) {
                  updateApplicationDetailsCache(rev);
                }
                changedRevisions.clear();
              }
            }


            @Override
            public void begin() throws XPRC_DeploymentHandlerException {
            }

          });

    } catch (XynaException e) {
      throw new RuntimeException(e);
    }

    List<XynaPropertyBuilds<DestinationKey>> applicationManagerProperties = new ArrayList<XynaPropertyBuilds<DestinationKey>>();
    applicationManagerProperties.add(START_APPLICATIONS_DESTINATION);
    applicationManagerProperties.add(STOP_APPLICATIONS_DESTINATION);
    applicationManagerProperties.add(LIST_APPLICATIONS_DESTINATION);
    applicationManagerProperties.add(REMOVE_APPLICATIONS_DESTINATION);
    applicationManagerProperties.add(DEPLOY_APPLICATIONS_DESTINATION);
    for (XynaPropertyBuilds<DestinationKey> xynaProperty : applicationManagerProperties) {
      xynaProperty.registerDependency(DEFAULT_NAME);
    }

    OPEN_FILES_SAFETY_MARGIN.registerDependency(DEFAULT_NAME);
    PERMGEN_PREDICTION_CLASS_FACTOR.registerDependency(DEFAULT_NAME);
    PERMGEN_PREDICTION_JAR_FACTOR.registerDependency(DEFAULT_NAME);
    PERMGEN_PREDICTION_CONSTANT.registerDependency(DEFAULT_NAME);

    excludedSubtypesOfProperties = new ConcurrentHashMap<Long, XynaPropertyString>();

    //jeder Workspace braucht eine eigene "excludedSubtypesOf-Property"
    RevisionManagement revisionManagement =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Map<Long, Workspace> workspaces = revisionManagement.getWorkspaces();
    for (Long revision : workspaces.keySet()) {
      addExcludedSubtypesOfProperty(workspaces.get(revision).getName(), revision);
    }
  }


  private void refreshBasicApplicationCache(ODSConnection con, BasicApplicationName basicAppName) throws PersistenceLayerException {
    Collection<ApplicationStorable> allApplications = con.loadCollection(ApplicationStorable.class);
    replaceBasicApplicationInCache(basicAppName, allApplications);
  }


  private void refreshBasicApplicationCache(Collection<ApplicationStorable> allApplications) {
    for (BasicApplicationName basicAppName : BasicApplicationName.values()) {
      replaceBasicApplicationInCache(basicAppName, allApplications);
    }
  }


  private void replaceBasicApplicationInCache(BasicApplicationName basicAppName, Collection<ApplicationStorable> allApplications) {
    long maxRevision = 0;
    Application basicApplication = null;
    for (ApplicationStorable app : allApplications) {
      if (app.isApplicationDefinition()) {
        continue; //Application Definitions nicht beachten
      }
      if (basicAppName.toString().equals(app.getName())) {
        try {
          Application application = new Application(app.getName(), app.getVersion());
          Long rev = revisionManagement.getRevision(application);
          if (maxRevision < rev) {
            maxRevision = rev;
            basicApplication = application;
          }
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          // ok
        }
      }
    }

    if (basicApplication != null) {
      basicApplications.put(basicAppName, basicApplication);
    } else {
      basicApplications.remove(basicAppName);
    }
  }


  @Override
  protected void shutdown() throws XynaException {
    if (ods != null) {
      transferFromODSTypeToODSType(ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY, ApplicationEntryStorable.TABLE_NAME,
                                   ApplicationEntryStorable.class);
      transferFromODSTypeToODSType(ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY, ApplicationStorable.TABLE_NAME,
                                   ApplicationStorable.class);
    }
  }


  private void transferFromODSTypeToODSType(ODSConnectionType from, ODSConnectionType to, String tablename,
                                            Class<? extends Storable<?>> clazz) throws XNWH_NoPersistenceLayerConfiguredForTableException,
      XNWH_PersistenceLayerInstanceIdUnknownException, PersistenceLayerException {
    boolean areHistoryAndDefaultTheSame = ods.isSamePhysicalTable(tablename, from, to);
    if (!areHistoryAndDefaultTheSame) {
      ods.replace(clazz, from, to);
    }
  }


  public Application getBasicApplication(BasicApplicationName applicationName) {
    return basicApplications.get(applicationName);
  }


  public Collection<Application> getBasicApplications() {
    return basicApplications.values();
  }


  /**
   * Liefert die Revision der aktuellsten Version der BasisApplication mit übergebenem Namen.
   * @param applicationName
   * @return Revision, falls die BasisApplication existiert, sonst null
   */
  public Long getBasicApplicationRevision(BasicApplicationName applicationName) {
    Application basicApp = getBasicApplication(applicationName);

    if (basicApp == null) {
      return null;
    }

    try {
      return revisionManagement.getRevision(basicApp);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      return null;
    }
  }


  public void removeApplicationVersion(String applicationName, String versionName) throws XFMG_CouldNotRemoveApplication {
    removeApplicationVersion(new ApplicationName(applicationName, versionName), new RemoveApplicationParameters(), false, null,
                             new EmptyRepositoryEvent(), true);
  }


  public void removeApplicationVersion(String applicationName, String versionName, RemoveApplicationParameters params,
                                       RepositoryEvent repositoryEvent) throws XFMG_CouldNotRemoveApplication {
    removeApplicationVersion(new ApplicationName(applicationName, versionName), params, false, null, repositoryEvent, true);
  }


  /**
   * wird vom GlobalApplicationManagement verwendet
   */
  public void removeApplicationVersion(ApplicationName application, RemoveApplicationParameters params, boolean verbose,
                                       PrintStream statusOutputStream) throws XFMG_CouldNotRemoveApplication {
    removeApplicationVersion(application, params, verbose, statusOutputStream, new EmptyRepositoryEvent(), true);
  }


  public void removeApplicationVersion(ApplicationName application, RemoveApplicationParameters params, boolean verbose,
                                       PrintStream statusOutputStream, RepositoryEvent repositoryEvent, boolean publishRuntimeContextModificationChanges)
      throws XFMG_CouldNotRemoveApplication {

    Long revision = null;
    String applicationName = application.getName();
    String versionName = application.getVersionName();

    if (versionName == null) {
      try {
        Long parentRevision = revisionManagement.getRevision(params.getParentWorkspace());
        versionName = getWorkingsetVersionName(applicationName, parentRevision);
        if (versionName == null) {
          //null bedeutet hier: es gibt keine application definition
          throw new XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY(applicationName + "/" + versionName, ApplicationStorable.TABLE_NAME);
        }
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new XFMG_CouldNotRemoveApplication(applicationName, versionName, e);
      }
    }

    RuntimeDependencyContext rc = new Application(applicationName, versionName);
    try {
      revision = revisionManagement.getRevision(rc.asCorrespondingRuntimeContext());
      if (logger.isDebugEnabled()) {
        logger.debug("Got revision " + revision + " for application " + applicationName + " " + versionName);
      }
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      if (params.getParentWorkspace() == null) {
        rc = new ApplicationDefinition(applicationName, RevisionManagement.DEFAULT_WORKSPACE);
      } else {
        rc = new ApplicationDefinition(applicationName, params.getParentWorkspace());
      }
    }
    
    for (RuntimeContextChangeHandler rdcch : getRtCtxMgmt().getHandlers()) {
      try {
        rdcch.removal(rc);
      } catch (Throwable t) {
        logger.error("Could not execute RuntimeContextChangeHandler " + rdcch, t);
      }
    }
    
    //Überprüfung, dass die Application(Definition) nicht von einem anderen RuntimeContext referenziert wird
    RuntimeContextDependencyManagement rcdMgmt =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
    Set<RuntimeDependencyContext> parentRuntimeContexts = rcdMgmt.getParentRuntimeContexts(rc);

    if (!parentRuntimeContexts.isEmpty()) {
      if (!params.isRemoveIfUsed()) {
        throw new XFMG_CouldNotRemoveApplication(applicationName, versionName,
                                                 new XFMG_RuntimeContextStillReferencedException(rc.toString()));
      }
      TemporarySessionAuthentication tsa =
          TemporarySessionAuthentication.tempAuthWithUniqueUserAndOperationLock("RemoveApplication",
                                                                                TemporarySessionAuthentication.TEMPORARY_CLI_USER_ROLE,
                                                                                revision, null);
      try {
        tsa.initiate();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }      
      try {
        modifyRuntimeContextDependencies(tsa.getUsername(), rc, verbose, statusOutputStream, rcdMgmt, parentRuntimeContexts, publishRuntimeContextModificationChanges);
      } finally {
        try {
          tsa.destroy();
        } catch (PersistenceLayerException e) {
          logger.warn("Could not cleanup temporary session " + tsa.getSessionId(), e);
        } catch (XFMG_PredefinedXynaObjectException e) {
          throw new RuntimeException(e);
        }
      }
    }

    if (rc instanceof ApplicationDefinition) {
      removeApplicationDefinition(applicationName, versionName, params, (ApplicationDefinition)rc, repositoryEvent);
    } else {
      removeRuntimeApplicationInternally(applicationName, versionName, revision, (RuntimeContext)rc, params, verbose, statusOutputStream, repositoryEvent, publishRuntimeContextModificationChanges);
    }

  }


  private void removeRuntimeApplicationInternally(String applicationName, String versionName, long revision, RuntimeContext rc,
                                                  RemoveApplicationParameters params, boolean verbose, PrintStream statusOutputStream,
                                                  RepositoryEvent repositoryEvent, boolean publishRuntimeContextModificationChanges) throws XFMG_CouldNotRemoveApplication {
    RuntimeContextDependencyManagement rcdMgmt =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();

    Pair<Operation, Operation> failure =
        CommandControl.wlock(CommandControl.Operation.APPLICATION_REMOVE, CommandControl.Operation.all(), revision);
    if (failure != null) {
      throw new RuntimeException(failure.getFirst() + " could not be locked because it is locked by another process of type "
          + failure.getSecond() + ".");
    }
    //TODO bei Cluster auch Applications Remote mit Lock gegen Änderungen schützen

    try {
      ODSConnection con = ods.openConnection();
      try {
        ApplicationStorable applicationStorable = queryRuntimeApplicationStorable(applicationName, versionName, con);

        //Application muss im Zustand STOPPED sein
        if (applicationStorable != null) {
          checkApplicationStopped(applicationStorable, params.isGlobal(), params.stopIfRunning());
        }

        //Behandlung laufender Aufträge lokal
        revisionManagement.handleRunningOrders(revision, params.isForce());

        //Behandlung laufender Aufträge auf anderen Knoten
        if (params.isGlobal() && currentClusterState == ClusterState.CONNECTED) {
          if (logger.isDebugEnabled()) {
            logger.debug("Call handleRunningOrders on other cluster nodes.");
          }
          try {
            RMIClusterProviderTools
                .executeAndCumulate(clusterInstance, clusteredInterfaceId, new IsApplicationInUseRunnable(applicationName, versionName,
                                                                                                          params.isForce()), null);
          } catch (XynaException e) {
            throw new XFMG_CouldNotRemoveApplication(applicationName, versionName, e);
          } catch (InvalidIDException e) {
            throw new XFMG_CouldNotRemoveApplication(applicationName, versionName, e);
          }
        }

        PreparedQuery<? extends ApplicationEntryStorable> query =
            queryCache.getQueryFromCache(sqlGetRuntimeApplicationEntries, con, ApplicationEntryStorable.getStaticReader());

        XynaProcess.instanceMethodTypes.remove(revision);

        //RepositoryAccessInstance entfernen
        RepositoryAccessManagement ram =
            XynaFactory.getInstance().getXynaDevelopment().getXynaLibraryDevelopment().getRepositoryAccessManagement();
        ram.removeRepositoryAccessInstance(getBranchName(applicationName, versionName));

        Set<ApplicationEntryStorable> appEntries = new TreeSet<ApplicationEntryStorable>(ApplicationEntryStorable.COMPARATOR);
        appEntries.addAll(con.query(query, new Parameter(applicationName, versionName), -1));

        Collection<ApplicationEntryStorable> implicitEntries =
            getAllImplicitApplicationEntries(appEntries, applicationName, versionName, null, params.isExtraForce());
        appEntries.addAll(implicitEntries);

        //undeployte Trigger werden über die impliziten Abhängigkeiten evtl. nicht gefunden,
        //daher hier nochmal nach allen Triggern suchen, um auch alle davon abhängigen objekte korrekt zu finden
        Set<ApplicationEntryStorable> alreadyProcessedEntries = new TreeSet<ApplicationEntryStorable>(ApplicationEntryStorable.COMPARATOR);
        alreadyProcessedEntries.addAll(implicitEntries);
        XynaActivationTrigger xynaActivationTrigger = XynaFactory.getInstance().getActivation().getActivationTrigger();

        Trigger[] allTriggers = xynaActivationTrigger.getTriggers(revision);
        for (Trigger trigger : allTriggers) {
          Set<ApplicationEntryStorable> dependencies =
              getTriggerDependencies(trigger.getTriggerName(), applicationName, versionName, null, alreadyProcessedEntries);
          appEntries.addAll(dependencies);
        }

        //Trigger und Filter entfernen
        removeFilterInstances(revision, verbose, statusOutputStream);
        removeTriggerInstances(revision, verbose, statusOutputStream);
        removeFilters(revision, verbose, statusOutputStream);
        removeTriggers(revision, verbose, statusOutputStream);

        Set<ApplicationEntryStorable> entriesSameRevision = new HashSet<ApplicationEntryStorable>();
        for (ApplicationEntryStorable entry : appEntries) {
          long r = revisionManagement.getRevision(entry.getApplication(), entry.getVersion(), null);
          if (r == revision) {
            //nur objekte bearbeiten, die in der gleichen revision sind!
            entriesSameRevision.add(entry);
          } else {
            logger.info("Found application entry not belonging to application to be removed: " + entry.getName() + " in "
                + entry.getApplication() + " / " + entry.getVersion());
          }
        }
        appEntries = entriesSameRevision;
        
        // call undeployment handler and remove from cache
        try {
          for (ApplicationEntryStorable entry : appEntries) {
            GenerationBase gb;
            switch (entry.getTypeAsEnum()) {
              case WORKFLOW :
                gb = WF.getInstance(entry.getName(), revision);
                break;
              case DATATYPE :
                gb = DOM.getInstance(entry.getName(), revision);
                break;
              case EXCEPTION :
                gb = ExceptionGeneration.getInstance(entry.getName(), revision);
                break;
              default : //next
                continue;
            }
            if (gb.getRevision() == revision) {
              if (verbose) {
                output(statusOutputStream, "Undeploy " + entry.getName());
              }
              gb.undeployRudimentarily(false);
            } else {
              logger.info("Found application entry not belonging to application to be removed: " + entry.getName() + " in "
                  + entry.getApplication() + " / " + entry.getVersion());
            }
          }
        } finally {
          GenerationBase.finishUndeploymentHandler();
          ClassLoaderBase.cleanupBackuppedDependencies(revision);
        }

        //OrderContextMappings entfernen
        revisionManagement.removeOrderContextMapping(new Application(applicationName, versionName));

        //ordertype configs entfernen
        OrdertypeManagement orderTypeManagement =
            XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderTypeManagement();
        List<OrdertypeParameter> listOrdertypes = orderTypeManagement.listOrdertypes(revision);
        for (OrdertypeParameter otp : listOrdertypes) {
          orderTypeManagement.deleteOrdertype(otp);
        }

        for (ApplicationEntryStorable entry : appEntries) {
          if (entry.getTypeAsEnum() == ApplicationEntryType.SHAREDLIB) {
            if (verbose) {
              output(statusOutputStream, "Delete shared lib " + entry.getName());
            }
            RevisionManagement.removeSharedLib(entry.getName(), revision);
          }
          if (entry.getTypeAsEnum() == ApplicationEntryType.XYNAPROPERTY) {
            //XynaProperty aus DependencyRegister entfernen
            dependencyRegister.removeDependencyNode(entry.getName(), DependencySourceType.XYNAPROPERTY, revision);
          }
        }
        RevisionManagement.removeSharedLib(null, revision); //empty shared lib

        //aus XMOMDatabase deregistrieren
        XMOMDatabase xmomDatabase = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getXMOMDatabase();
        xmomDatabase.unregisterXMOMObjects(revision);
        
        XynaFactory.getInstance().getXynaNetworkWarehouse().getXMOMPersistence().getXMOMPersistenceManagement().removeRevision(revision);

        //aus DeploymentItemStateManagement deregistrieren
        DeploymentItemStateManagement dism =
            XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement();
        dism.removeRegistry(revision);

        //OrderInputSources löschen
        OrderInputSourceManagement oism =
            XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderInputSourceManagement();
        oism.deleteOrderInputSourcesForRevision(revision);

        if (verbose) {
          output(statusOutputStream, "Delete application in database");
        }
        logger.debug("Delete application entries from database");
        con.delete(appEntries);

        boolean keepForAudits =
            params.isKeepForAudits() && applicationStorable != null && applicationStorable.getStateAsEnum() != ApplicationState.AUDIT_MODE 
                && applicationStorable.getCreationDate() <= Updater.getInstance().getUpdateTime(Updater.VERSION_APPLICATION_CREATIONDATE, ExecutionTime.initialUpdate);
        if (applicationStorable != null) {
          if (keepForAudits) {
            if (logger.isDebugEnabled()) {
              logger.debug("Change application state to AUDIT_MODE");
            }
            changeState(con, applicationStorable, ApplicationState.AUDIT_MODE);
          } else {
            if (logger.isDebugEnabled()) {
              logger.debug("Delete application from database");
            }
            con.deleteOneRow(applicationStorable);
          }
        }
        con.commit();


        transferFromODSTypeToODSType(ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY, ApplicationEntryStorable.TABLE_NAME,
                                     ApplicationEntryStorable.class);
        transferFromODSTypeToODSType(ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY, ApplicationStorable.TABLE_NAME,
                                     ApplicationStorable.class);


        if (verbose) {
          output(statusOutputStream, "Delete revision files");
        }

        if (logger.isDebugEnabled()) {
          logger.debug("Delete revision folder for revision " + revision);
        }
        RevisionManagement.removeRevisionFolder(revision, keepForAudits);

        // remove custom ordertype
        for (ApplicationEntryStorable entry : appEntries) {
          switch (entry.getTypeAsEnum()) {
            case ORDERTYPE :
              DestinationValue dv;
              DestinationKey dk = new DestinationKey(entry.getName(), applicationName, versionName);
              try {
                dv =
                    XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution()
                        .getExecutionEngineDispatcher().getDestination(dk);
                XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution().getExecutionEngineDispatcher()
                    .removeCustomDestination(dk, dv);
                dv =
                    XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaPlanning().getPlanningDispatcher()
                        .getDestination(dk);
                XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaPlanning().getPlanningDispatcher()
                    .removeCustomDestination(dk, dv);
                dv =
                    XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaCleanup().getCleanupEngineDispatcher()
                        .getDestination(dk);
                XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaCleanup().getCleanupEngineDispatcher()
                    .removeCustomDestination(dk, dv);
              } catch (XPRC_DESTINATION_NOT_FOUND e) {
                //ok, war wohl default ordertype, der wurde bereits beim undeployment vom workflow entfernt
                //dann braucht man beim cleanup und planning auch nicht mehr schauen
              }
          }
        }

        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
            .cleanClassLoaderMapsForRevision(revision);

        //RuntimeContext Dependencies löschen (muss vor dem Löschen der Revision gemacht werden)
        rcdMgmt.deleteRuntimeContext(RuntimeContextDependencyManagement.asRuntimeDependencyContext(rc));

        if (!keepForAudits) {
          if (logger.isDebugEnabled()) {
            logger.debug("Delete xmomversion for revision " + revision);
          }
          revisionManagement.deleteRevision(revision);

          //Revision aus Liste der gesperrten RMI/CLI-Schnittstellen entfernen,
          //falls die Application endgültig gelöscht wird
          RevisionOrderControl roc = new RevisionOrderControl(revision);
          roc.openRMICLI();
        }

        //Basis-Application cache refreshen
        BasicApplicationName basicAppName = BasicApplicationName.valueOfOrNull(applicationName);
        if (basicAppName != null) {
          refreshBasicApplicationCache(con, basicAppName);
        }

      } catch (XynaException e) {
        if (e instanceof XFMG_CouldNotRemoveApplication) {
          throw (XFMG_CouldNotRemoveApplication) e;
        }
        throw new XFMG_CouldNotRemoveApplication(applicationName, versionName, e);
      } finally {
        try {
          con.closeConnection();
        } catch (PersistenceLayerException e) {
          logger.warn("Could not close connection.", e);
        }
      }

      if (publishRuntimeContextModificationChanges) {
        //Multi-User-Event für RuntimeContext Änderung
        Publisher publisher = new Publisher(params.getUser());
        publisher.publishRuntimeContextDelete(rc.getGUIRepresentation());
      }

      if (params.isGlobal() && currentClusterState == ClusterState.CONNECTED) {
        output(statusOutputStream, "Remove application on other cluster nodes.");
        if (logger.isDebugEnabled()) {
          logger.debug("Remove application " + applicationName + " " + versionName + " on other nodes");
        }
        try {
          startRequestOnOtherNodes(new RemoveApplicationRunnable(applicationName, versionName, params));
        } catch (Throwable e) {
          throw new XFMG_CouldNotRemoveApplication(applicationName, versionName, new RemoteException("Remove remote application",e));
        }
      }
      
      //remove custom order entry types
      RevisionOrderControl.unregisterAllCustomOrderEntryTypes(revision);
      
    } finally {
      CommandControl.wunlock(CommandControl.Operation.all(), revision);
    }
  }


  public void removeTriggerInstances(long revision, boolean verbose, PrintStream statusOutputStream) throws PersistenceLayerException {
    Collection<TriggerInstanceInformation> tiis = xynaActivationTrigger.getTriggerInstanceInformation(revision);
    for (TriggerInstanceInformation tii : tiis) {
      try {
        if (verbose) {
          output(statusOutputStream, "Removing trigger instance " + tii.getTriggerInstanceName() + " in revision " + revision);
        }
        xynaActivationTrigger.undeployTrigger(tii.getTriggerName(), tii.getTriggerInstanceName(), revision);
      } catch (XynaException e) {
        if (logger.isDebugEnabled()) {
          logger.debug("Could not remove trigger instance " + tii.getTriggerInstanceName());
        }
      }
    }
  }


  public void removeFilterInstances(long revision, boolean verbose, PrintStream statusOutputStream) throws PersistenceLayerException {
    Collection<FilterInstanceInformation> fiis = xynaActivationTrigger.getFilterInstanceInformations(revision);
    for (FilterInstanceInformation fii : fiis) {
      try {
        if (verbose) {
          output(statusOutputStream, "Removing filter instance " + fii.getFilterInstanceName() + " in revision " + revision);
        }
        xynaActivationTrigger.undeployFilter(fii.getFilterInstanceName(), revision);
      } catch (XACT_FilterNotFound e) {
        if (logger.isDebugEnabled()) {
          logger.debug("Could not remove filter instance " + fii.getFilterInstanceName());
        }
      }
    }
  }


  /**
   * Entfernt alle Filter der Revision
   */
  public void removeFilters(Long revision, boolean verbose, PrintStream statusOutputStream) throws PersistenceLayerException {
    Filter[] allFilters = xynaActivationTrigger.getFilters(revision);
    for (Filter filter : allFilters) {
      try {
        if (logger.isDebugEnabled()) {
          logger.debug("Removing filter " + filter.getName() + " with revision " + revision);
        }
        xynaActivationTrigger.removeFilterWithUndeployingInstances(filter.getName(), revision);
        if (verbose) {
          output(statusOutputStream, "Removed filter " + filter.getName() + " and undeployed all instances");
        }
      } catch (XACT_FilterNotFound e) {
        // schon gelöscht ... ignore
        logger.debug("Unable to remove filter. This must not be an error!", e);
      }
    }
  }


  /**
   * Entfernt alle Trigger der Revision
   */
  public void removeTriggers(Long revision, boolean verbose, PrintStream statusOutputStream) throws XynaException {
    RuntimeContext runtimeContext = revisionManagement.getRuntimeContext(revision);

    List<TriggerInformation> triggerInformations = xynaActivationTrigger.listTriggerInformation();
    for (TriggerInformation triggerInformation : triggerInformations) {
      String triggerName = triggerInformation.getTriggerName();
      if (runtimeContext.equals(triggerInformation.getRuntimeContext())) {
        for (TriggerInstanceInformation triggerInstanceInformation : triggerInformation.getTriggerInstances()) {
          String triggerInstanceName = triggerInstanceInformation.getTriggerInstanceName();
          if (logger.isDebugEnabled()) {
            logger.debug("Undeploying trigger instance " + triggerInstanceName + " in revision " + revision);
          }
          try {
            xynaActivationTrigger.undeployTrigger(triggerName, triggerInstanceName, revision);
            if (verbose) {
              output(statusOutputStream, "Undeployed trigger instance " + triggerInstanceName);
            }
          } catch (XACT_TriggerInstanceNotFound e) {
            // schon gelöscht ... ignore
            if (logger.isDebugEnabled()) {
              logger.debug("Unable to remove trigger instance " + triggerInstanceName + ". This must not be an error!", e);
            }
          } catch (XACT_TriggerNotFound e) {
            // schon gelöscht ... ignore
            if (logger.isDebugEnabled()) {
              logger.debug("Unable to remove trigger instance " + triggerInstanceName + ". This must not be an error!", e);
            }
          }
        }

        try {
          if (logger.isDebugEnabled()) {
            logger.debug("Removing trigger " + triggerName + " in revision " + revision);
          }
          xynaActivationTrigger.removeTrigger(triggerName, revision);
          if (verbose) {
            output(statusOutputStream, "Removed trigger " + triggerName);
          }
        } catch (XACT_TriggerNotFound e) {
          // schon gelöscht ... ignore
          if (logger.isDebugEnabled()) {
            logger.debug("Unable to remove trigger " + triggerName + ". This must not be an error!", e);
          }
        }
      }
    }
  }


  private void removeApplicationDefinition(String applicationName, String versionName, RemoveApplicationParameters params,
                                           ApplicationDefinition rc, RepositoryEvent repositoryEvent) throws XFMG_CouldNotRemoveApplication {
    RuntimeContextDependencyManagement rcdMgmt =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();

    // wenn die Arbeitskopie (oder inkonsistente RuntimeApplication) gelöscht werden soll,
    // müssen nur die Einträge aus den ApplicationEntries entfernt werden
    ODSConnection con = ods.openConnection();
    try {
      Long parentRevision = revisionManagement.getRevision(params.getParentWorkspace());
      ApplicationStorable applicationStorable = queryApplicationDefinitionStorable(applicationName, parentRevision, con);
      if (applicationStorable != null) {
        if (((applicationStorable.getVersion() == null) ^ (versionName == null))
            || (versionName != null && !versionName.equals(applicationStorable.getVersion()))) {
          //unterschiedliche version -> nicht application definition entfernen
          applicationStorable = null;
        }
      }
      if (applicationStorable == null && params.isExtraForce()) {
        //nach inkonsistenter RuntimeApplication suchen
        applicationStorable = queryRuntimeApplicationStorable(applicationName, versionName, con);
      }
      
      for (RuntimeContextChangeHandler rdcch : getRtCtxMgmt().getHandlers()) {
        try {
          rdcch.removal(rc);
        } catch (Throwable t) {
          logger.error("Could not execute RuntimeContextChangeHandler " + rdcch, t);
        }
      }
      
      if (applicationStorable == null) {
        List<? extends ApplicationEntryStorable> storedAppEntries = queryAllRuntimeApplicationStorables(applicationName, versionName, con);
        if (storedAppEntries.size() > 0) {
          // inconsistent entries found but app does not exist anymore. delete entries and return
          con.delete(storedAppEntries);
          con.commit();

          transferFromODSTypeToODSType(ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY, ApplicationEntryStorable.TABLE_NAME,
                                       ApplicationEntryStorable.class);
          return;
        } else {
          throw new XFMG_CouldNotRemoveApplication(applicationName, versionName, new XFMG_RuntimeContextNotFoundException(rc.toString()));
        }
      }

      if (!applicationStorable.isApplicationDefinition()) {
        if (logger.isDebugEnabled()) {
          logger.debug("Application " + applicationName + " has bad state. No revision, but application is defined and is not workingset.");
        }
        if (!params.isExtraForce()) {
          throw new XFMG_CouldNotRemoveApplication(applicationName, versionName, new XFMG_RuntimeContextNotFoundException(rc.toString()));
        }
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug("removing application definition " + applicationName);
        }
      }

      String sql = applicationStorable.isApplicationDefinition() ? sqlGetApplicationDefinitionEntries : sqlGetRuntimeApplicationEntries;
      Parameter paras =
          applicationStorable.isApplicationDefinition() ? new Parameter(applicationName, parentRevision) : new Parameter(applicationName,
                                                                                                                         versionName,
                                                                                                                         parentRevision);
      PreparedQuery<? extends ApplicationEntryStorable> query =
          queryCache.getQueryFromCache(sql, con, ApplicationEntryStorable.getStaticReader());
      List<? extends ApplicationEntryStorable> appEntries = con.query(query, paras, -1);

      con.deleteOneRow(applicationStorable);
      con.delete(appEntries);
      if (applicationStorable.isApplicationDefinition()) {
        applicationDefinitionVersionNameCache.remove(new ApplicationDefinitionKey(applicationName, parentRevision));
      }
      con.commit();

      transferFromODSTypeToODSType(ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY, ApplicationEntryStorable.TABLE_NAME,
                                   ApplicationEntryStorable.class);
      transferFromODSTypeToODSType(ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY, ApplicationStorable.TABLE_NAME,
                                   ApplicationStorable.class);

      clearApplicationDefinitionCache(parentRevision);
      updateApplicationDetailsCache(parentRevision);
      repositoryEvent.addEvent(new ProjectCreationOrChangeProvider.BasicProjectCreationOrChangeEvent(EventType.APPLICATION_REMOVE,
                                                                                                     applicationName));

      //RuntimeContext Dependencies löschen
      rcdMgmt.deleteRuntimeContext(rc);
      
      //Multi-User-Event für RuntimeContext Änderung
      Publisher publisher = new Publisher(params.getUser());
      publisher.publishRuntimeContextDelete(rc);
    } catch (PersistenceLayerException ep) {
      throw new XFMG_CouldNotRemoveApplication(applicationName, versionName, ep);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY xe) {
      throw new XFMG_CouldNotRemoveApplication(applicationName, versionName, xe);
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException ep) {
        logger.warn("Could not close connection.", ep);
      }
    }
  }


  /**
   * RMIRunnable, um festzustellen, ob auf einem anderen Knoten noch laufende Aufträge
   * für die Application existieren und diese ggf. abbrechen.
   *
   */
  private static class IsApplicationInUseRunnable implements RMIRunnable<Boolean, ApplicationRemoteInterface, XynaException> {

    private String applicationName;
    private String versionName;
    private boolean force;


    public IsApplicationInUseRunnable(String applicationName, String versionName, boolean force) {
      this.applicationName = applicationName;
      this.versionName = versionName;
      this.force = force;
    }


    public Boolean execute(ApplicationRemoteInterface clusteredInterface) throws XynaException, RemoteException {
      return clusteredInterface.isApplicationInUseRemotely(applicationName, versionName, force);
    }
  }

  /**
   * RMIRunnable, um den aktuellen Status einer Application zu ermitteln.
   *
   */
  private static class GetApplicationStateRunnable implements RMIRunnable<ApplicationState, ApplicationRemoteInterface, XynaException> {

    private String applicationName;
    private String versionName;


    public GetApplicationStateRunnable(String applicationName, String versionName) {
      this.applicationName = applicationName;
      this.versionName = versionName;
    }


    public ApplicationState execute(ApplicationRemoteInterface clusteredInterface) throws XynaException, RemoteException {
      return clusteredInterface.getApplicationStateRemotely(applicationName, versionName);
    }
  }


  /**
   * ApplicationRemoteRunnable, um eine Application auf den anderen Knoten zu löschen.
   *
   */
  private static class RemoveApplicationRunnable extends ApplicationRemoteRunnable {

    private String applicationName;
    private String versionName;
    private RemoveApplicationParameters params;


    public RemoveApplicationRunnable(String applicationName, String versionName, RemoveApplicationParameters params) {
      this.applicationName = applicationName;
      this.versionName = versionName;
      this.params = params;
    }


    @Override
    public String run(ApplicationRemoteInterface remoteInterface) throws XynaException, RemoteException {
      return remoteInterface.removeApplicationVersionRemotely(applicationName, versionName, params);
    }
  }


  /**
   * ApplicationRemoteRunnable, um eine Application auf den anderen Knoten zu starten.
   *
   */
  private static class StartApplicationRunnable extends ApplicationRemoteRunnable {

    private String applicationName;
    private String versionName;
    private StartApplicationParameters params;


    public StartApplicationRunnable(String applicationName, String versionName, StartApplicationParameters params) {
      this.applicationName = applicationName;
      this.versionName = versionName;
      this.params = params;
    }


    @Override
    public String run(ApplicationRemoteInterface remoteInterface) throws XynaException, RemoteException {
      return remoteInterface.startApplicationRemotely(applicationName, versionName, params);
    }
  }

  /**
   * RMIRunnable, um die laufenden Aufträge auf den anderen Knoten zu bestimmen.
   *
   */
  private static class ListActiveOrdersRunnable implements RMIRunnable<OrdersInUse, ApplicationRemoteInterface, XynaException> {

    private String applicationName;
    private String versionName;
    private boolean verbose;


    public ListActiveOrdersRunnable(String applicationName, String versionName, boolean verbose) {
      this.applicationName = applicationName;
      this.versionName = versionName;
      this.verbose = verbose;
    }


    public OrdersInUse execute(ApplicationRemoteInterface clusteredInterface) throws XynaException, RemoteException {
      return clusteredInterface.listActiveOrdersRemotely(applicationName, versionName, verbose);
    }
  }


  /**
   * Wirft eine Exception, wenn die Application nicht im Zustand STOPPED oder AUDIT_MODE ist
   */
  private void checkApplicationStopped(ApplicationStorable application, boolean global, boolean stopIfRunning)
      throws XFMG_CouldNotRemoveApplication, XFMG_CouldNotStopApplication {
    String applicationName = application.getName();
    String versionName = application.getVersion();

    //Application muss lokal den Zustand STOPPED oder AUDIT_MODE haben
    if (application.getStateAsEnum() != ApplicationState.STOPPED && application.getStateAsEnum() != ApplicationState.AUDIT_MODE) {
      if (application.getStateAsEnum() == ApplicationState.RUNNING && stopIfRunning) {
        stopApplication(applicationName, versionName, true);
      } else {
        Exception cause =
            new XFMG_ApplicationMustBeStopped(application.getName(), application.getVersion(), application.getStateAsEnum().toString());
        throw new XFMG_CouldNotRemoveApplication(application.getName(), application.getVersion(), cause);
      }
    }

    //Status auf anderen Knoten überprüfen, falls application global entfernt werden soll
    if (global && currentClusterState == ClusterState.CONNECTED) {
      if (logger.isDebugEnabled()) {
        logger.debug("Call checkApplicationStopped on other cluster nodes.");
      }
      try {
        List<ApplicationState> results =
            RMIClusterProviderTools.executeAndCumulate(clusterInstance, clusteredInterfaceId,
                                                       new GetApplicationStateRunnable(applicationName, versionName), null);
        for (ApplicationState result : results) {
          if (result != null && result != ApplicationState.STOPPED && result != ApplicationState.AUDIT_MODE) {
            Exception cause = new Exception("Application in state " + result + " must be STOPPED on other cluster nodes.");
            throw new XFMG_CouldNotRemoveApplication(applicationName, versionName, cause);
          }
        }
      } catch (XynaException e) {
        throw new XFMG_CouldNotRemoveApplication(applicationName, versionName, e);
      } catch (InvalidIDException e) {
        throw new XFMG_CouldNotRemoveApplication(applicationName, versionName, e);
      }
    }
  }


  /**
   * Stoppt die Applikation, indem die zugehörigen Trigger und Filter angehalten werden.
   */
  public void stopApplication(String applicationName, String versionName, boolean clusterwide) throws XFMG_CouldNotStopApplication {
    stopApplication(applicationName, versionName, clusterwide, Optional.<EnumSet<OrderEntranceType>> empty());
  }


  public void stopApplication(final String applicationName, final String versionName, boolean clusterwide,
                              final Optional<EnumSet<OrderEntranceType>> onlyDisableEntranceTypes) throws XFMG_CouldNotStopApplication {

    long revision;
    try {
      revision = revisionManagement.getRevision(new Application(applicationName, versionName));
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new XFMG_CouldNotStopApplication(applicationName, versionName, e);
    }
    if (logger.isDebugEnabled()) {
      logger.debug("Try to stop application with revision " + revision);
    }

    ODSConnection con = ods.openConnection();
    try {
      ApplicationStorable application = queryRuntimeApplicationStorable(applicationName, versionName, con);
      if (application == null) {
        throw new XFMG_CouldNotStopApplication(applicationName, versionName);
      }
      if (application.isApplicationDefinition() || application.getStateAsEnum() == ApplicationState.AUDIT_MODE) {
        Exception cause = new Exception("Could not stop application in state " + application.getState());
        throw new XFMG_CouldNotStopApplication(applicationName, versionName, cause);
      }

      //alle Auftragseingangsschnittstellen deaktivieren
      RevisionOrderControl roc = new RevisionOrderControl(revision);
      if (onlyDisableEntranceTypes.isPresent()) {
        roc.closeRMICLI(convertOrderEntranceTypes(onlyDisableEntranceTypes.get()));
        if (onlyDisableEntranceTypes.get().size() > 1) {
          RevisionOrderControl.closeCustomOrderEntries(revision);
        }
      } else {
        roc.closeOrderEntryInterfaces(null, false);
        RevisionOrderControl.closeCustomOrderEntries(revision);
      }

      boolean allInterfacesClosed;
      //Inzwischen könnte eine Auftragseingangsschnittstelle wieder aktiviert worden sein
      //daher jetzt (mit Lock) überprüfen, ob noch alle geschlossen sind
      Pair<Operation, Operation> failure =
          CommandControl.wlock(CommandControl.Operation.APPLICATION_STOP,
                               CommandControl.Operation.allExcept(CommandControl.Operation.APPLICATION_STOP), revision);
      if (failure != null) {
        throw new RuntimeException(failure.getFirst() + " could not be locked because it is locked by another process of type "
            + failure.getSecond() + ".");
      }
      try {
        allInterfacesClosed = roc.orderEntryInterfacesClosed();
        if (!allInterfacesClosed && !onlyDisableEntranceTypes.isPresent()) {
          Exception e = new Exception("There are still open orderEntryInterfaces.");
          throw new XFMG_CouldNotStopApplication(applicationName, versionName, e);
        }
      } finally {
        CommandControl.wunlock(CommandControl.Operation.allExcept(CommandControl.Operation.APPLICATION_STOP), revision);
      }

      if (allInterfacesClosed) {
        //Zustand auf STOPPED setzen
        changeState(con, application, ApplicationState.STOPPED);
      }
    } catch (OrderEntryInterfacesCouldNotBeClosedException e) {
      throw new XFMG_CouldNotStopApplication(applicationName, versionName, e);
    } catch (XynaException e) {
      throw new XFMG_CouldNotStopApplication(applicationName, versionName, e);
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Could not close connection.", e);
      }
    }
    if (clusterwide && currentClusterState == ClusterState.CONNECTED) {

      try {
        startRequestOnOtherNodes(new ApplicationRemoteRunnable() {

          @Override
          public String run(ApplicationRemoteInterface remoteInterface) throws XynaException, RemoteException {
            return remoteInterface.stopApplicationRemotely(applicationName, versionName,
                                                           onlyDisableEntranceTypes.isPresent() ? onlyDisableEntranceTypes.get() : null);
          }
        });
      } catch (Throwable e) {
        throw new XFMG_CouldNotStopApplication(applicationName, versionName, new RemoteException("Stop remote application", e) );
      }
    }
  }


  /**
   * Startet die Applikation, indem die zugehörigen Trigger und Filter gestartet werden.
   */
  public void startApplication(String applicationName, String versionName, boolean force, boolean clusterwide)
      throws XFMG_CouldNotStartApplication {
    StartApplicationParameters params = new StartApplicationParameters();
    params.setForceStartInInconsistentCluster(force);
    params.setGlobal(clusterwide);

    startApplication(applicationName, versionName, params);
  }


  /**
   * Startet die Applikation, indem die zugehörigen Trigger und Filter gestartet werden.
   */
  public void startApplication(String applicationName, String versionName, StartApplicationParameters params)
      throws XFMG_CouldNotStartApplication {

    if (applicationName == null || versionName == null) {
      throw new XFMG_CouldNotStartApplication(applicationName, versionName);
    }

    Application runtimeContext = new Application(applicationName, versionName);
    long revision;
    try {
      revision = revisionManagement.getRevision(runtimeContext);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new XFMG_CouldNotStartApplication(applicationName, versionName, e);
    }

    if (currentClusterState != ClusterState.NO_CLUSTER && currentClusterState != ClusterState.SINGLE
        && !params.isForceStartInInconsistentCluster()) {
      // check, ob die Application im Cluster auf dem anderen Knoten installiert ist ... wenn dies nicht der Fall ist, darf nicht gestartet werden
      // -> sonst ist nicht sichergestellt, das die Migration erfolgreich funktionieren würde
      if (currentClusterState == ClusterState.CONNECTED) {
        logger.debug("Check on other nodes whether the application is installed there.");
        try {
          List<ApplicationState> results =
              RMIClusterProviderTools.executeAndCumulate(clusterInstance, clusteredInterfaceId,
                                                         new GetApplicationStateRunnable(applicationName, versionName), null);
          for (ApplicationState result : results) {
            if (result == null || result == ApplicationState.AUDIT_MODE) {
              logger.error("Application " + applicationName + " " + versionName
                  + " is not installed or in state AUDIT_MODE on other node. Abort start of application.");
              throw new XFMG_ApplicationIsNotInstalledOnRemoteClusterNode(applicationName, versionName);
            }
          }
        } catch (XynaException e) {
          throw new XFMG_CouldNotStartApplication(applicationName, versionName, e);
        } catch (InvalidIDException e) {
          throw new XFMG_CouldNotStartApplication(applicationName, versionName, e);
        }
      } else {
        logger
            .warn("Could not check on other nodes whether the application is installed there. The clusterstate is " + currentClusterState);
        throw new XFMG_CouldNotStartApplication(applicationName, versionName);
        // FIXME bessere Fehlermeldung!
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Start application with revision " + revision);
    }

    ODSConnection con = ods.openConnection();
    try {
      ApplicationStorable application = queryRuntimeApplicationStorable(applicationName, versionName, con);
      if (application == null) {
        throw new XFMG_CouldNotStartApplication(applicationName, versionName);
      }
      if (application.isApplicationDefinition() || application.getStateAsEnum() == ApplicationState.AUDIT_MODE) {
        Exception cause = new Exception("Could not start application in state " + application.getState());
        throw new XFMG_CouldNotStartApplication(applicationName, versionName, cause);
      }

      //Status auf RUNNING setzen
      changeState(con, application, ApplicationState.RUNNING);

      //RMI/CLI-Schnittstellen öffnen
      RevisionOrderControl roc = new RevisionOrderControl(revision);
      roc.openRMICLI(convertOrderEntranceTypes(params.getOnlyEnableOrderEntrance()));
      RevisionOrderControl.openCustomOrderEntries(revision);

      if (params.getOnlyEnableOrderEntrance() == null) {
        //andere Schnittstellen sollen auch geöffnet werden
        //Trigger/Filter aktivieren
        Map<String, Set<String>> triggerFilterInstances = getTriggerAndFilterInstances(con, runtimeContext);
        enableTriggerAndFilter(triggerFilterInstances, revision);

        //Cron Like Orders enablen
        if (params.isEnableCrons()) {
          roc.enableCrons(null, false);
        }

        //Time Controlled Orders wieder aktivieren
        BatchProcessManagement bpm = XynaFactory.getInstance().getProcessing().getBatchProcessManagement();
        bpm.openBatchProcessEntrance(revision);

        //Frequency Controlled Tasks wieder aktivieren
        roc.resumeFCTasks();
      }
    } catch (XynaException e) {
      throw new XFMG_CouldNotStartApplication(applicationName, versionName, e);
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Could not close connection.", e);
      }
    }
    if (params.isGlobal() && currentClusterState == ClusterState.CONNECTED) {

      logger.debug("Start application on other nodes");
      try {
        startRequestOnOtherNodes(new StartApplicationRunnable(applicationName, versionName, params));
      } catch (Throwable e) {
        throw new XFMG_CouldNotStartApplication(applicationName, versionName, new RemoteException("Start remote application",e));
      }
    }
  }


  private EnumSet<com.gip.xyna.xfmg.xfctrl.appmgmt.OrderEntrance.OrderEntranceType> convertOrderEntranceTypes(EnumSet<OrderEntranceType> types) {
    if (types == null) {
      return null;
    }

    EnumSet<com.gip.xyna.xfmg.xfctrl.appmgmt.OrderEntrance.OrderEntranceType> result =
        EnumSet.noneOf(com.gip.xyna.xfmg.xfctrl.appmgmt.OrderEntrance.OrderEntranceType.class);
    for (OrderEntranceType type : types) {
      switch (type) {
        case RMI :
          result.add(com.gip.xyna.xfmg.xfctrl.appmgmt.OrderEntrance.OrderEntranceType.RMI);
          break;
        case CLI :
          result.add(com.gip.xyna.xfmg.xfctrl.appmgmt.OrderEntrance.OrderEntranceType.CLI);
          break;
      }
    }

    return result;
  }


  private Map<String, Set<String>> getTriggerAndFilterInstances(ODSConnection con, Application application)
      throws PersistenceLayerException {
    Map<String, Set<String>> triggerFilterInstances = new HashMap<String, Set<String>>();
    PreparedQuery<? extends ApplicationEntryStorable> query =
        queryCache.getQueryFromCache(sqlGetRuntimeApplicationEntries, con, ApplicationEntryStorable.getStaticReader());
    List<? extends ApplicationEntryStorable> appEntries =
        con.query(query, new Parameter(application.getName(), application.getVersionName()), -1);

    long revision;
    try {
      revision = revisionManagement.getRevision(application);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new IllegalArgumentException("application unknown", e);
    }

    for (ApplicationEntryStorable entry : appEntries) {
      if (entry.getTypeAsEnum() == ApplicationEntryType.FILTERINSTANCE) {
        FilterInstanceInformation filterInfo =
            XynaFactory.getInstance().getActivation().getActivationTrigger().getFilterInstanceInformation(entry.getName(), revision);
        if (filterInfo == null) {
          logger.warn("Could not find filter instance " + entry.getName());
          continue;
        }
        Set<String> filter = triggerFilterInstances.get(filterInfo.getTriggerInstanceName());
        if (filter == null) {
          filter = new HashSet<String>();
        }
        filter.add(entry.getName());
        triggerFilterInstances.put(filterInfo.getTriggerInstanceName(), filter);
      } else if (entry.getTypeAsEnum() == ApplicationEntryType.TRIGGERINSTANCE) {
        if (!triggerFilterInstances.containsKey(entry.getName())) {
          triggerFilterInstances.put(entry.getName(), new HashSet<String>());
        }
      }
    }

    return triggerFilterInstances;
  }


  private void enableTriggerAndFilter(Map<String, Set<String>> triggerFilterInstances, Long revision) throws XynaException {
    XynaActivationTrigger xynaActivationTrigger = XynaFactory.getInstance().getActivation().getActivationTrigger();

    for (String triggerInstanceName : triggerFilterInstances.keySet()) {
      for (String filterInstanceName : triggerFilterInstances.get(triggerInstanceName)) {
        //Filterinstanzen einzeln enablen, da enableTriggerInstance nur ausgeführt wird,
        //wenn die Triggerinstanz nicht bereits enabled ist
        if (logger.isDebugEnabled()) {
          logger.debug("Enable filter instance " + filterInstanceName);
        }
        try {
          xynaActivationTrigger.enableFilterInstance(filterInstanceName, revision);
        } catch (Exception e) {
          logger.warn("Could not enable filter instance " + filterInstanceName, e);
        }
      }

      if (logger.isDebugEnabled()) {
        logger.debug("Enable trigger instance " + triggerInstanceName);
      }
      try {
        xynaActivationTrigger.enableTriggerInstance(triggerInstanceName, revision, true, -1, false);
      } catch (Exception e) {
        logger.warn("Could not enable trigger instance " + triggerInstanceName, e);
      }
    }
  }


  /**
   * Ändert den Status der Application
   * @param revision
   * @param newState
   */
  public void changeApplicationState(Long revision, ApplicationState newState) {
    Application application;
    try {
      application = revisionManagement.getApplication(revision);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }

    changeApplicationState(application.getName(), application.getVersionName(), newState);
  }


  /**
   * Ändert den Status der Application
   * @param applicationName
   * @param versionName
   * @param newState
   */
  public void changeApplicationState(String applicationName, String versionName, ApplicationState newState) {
    ODSConnection con = ods.openConnection();
    try {
      ApplicationStorable application = queryRuntimeApplicationStorable(applicationName, versionName, con);
      changeState(con, application, newState);
    } catch (PersistenceLayerException e) {
      logger.warn("could not change application state to " + newState, e);
    } finally {
      try {
        if (con != null) {
          con.closeConnection();
        }
      } catch (PersistenceLayerException e) {
        logger.warn("Could not close connection.", e);
      }
    }
  }


  /**
   * Ändert den Status der Application
   * @param con
   * @param application
   * @param newState
   * @throws PersistenceLayerException
   */
  private void changeState(ODSConnection con, ApplicationStorable application, ApplicationState newState) throws PersistenceLayerException {
    if (!application.getState().equals(newState.name())) {
      application.setState(newState);
      con.persistObject(application);
      con.commit();

      try {
        transferFromODSTypeToODSType(ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY, ApplicationStorable.TABLE_NAME,
                                     ApplicationStorable.class);
      } catch (XynaException e) {
        logger.error("Could not persist application informations.", e);
      }
      
      if (newState == ApplicationState.RUNNING) {
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRemoteDestinationManagement().notifyApplicationStarted(application.getName(), application.getVersion());
      }
    }
  }
  

  public void changeApplicationDefinitionComment(String name, Long parentRevision, String comment)
                  throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    ODSConnection con = ods.openConnection();
    try {
      ApplicationStorable application = queryApplicationDefinitionStorable(name, parentRevision, con);
      if (application == null) {
        throw new XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY(name + " - " + parentRevision, ApplicationStorable.TABLE_NAME);
      }
      writelock.lock();
      application.setComment(comment);
      con.persistObject(application);
      con.commit();
      transferFromODSTypeToODSType(ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY, ApplicationStorable.TABLE_NAME, ApplicationStorable.class);
    } finally {
      writelock.unlock();
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Can't close connection.", e);
      }
    }
  }


  public void copyApplicationIntoWorkspace(String applicationName, String versionName, CopyApplicationIntoWorkspaceParameters params)
      throws XFMG_CouldNotBuildWorkingSet {
    copyApplicationIntoWorkspace(applicationName, versionName, params, false, null);
  }


  public void copyApplicationIntoWorkspace(String applicationName, String versionName, CopyApplicationIntoWorkspaceParameters params,
                                           boolean verbose, PrintStream statusOutputStream) throws XFMG_CouldNotBuildWorkingSet {

    Long revision;
    try {
      revision = revisionManagement.getRevision(applicationName, versionName, null);
      if (logger.isDebugEnabled()) {
        logger.debug("Got revision " + revision + " for application " + applicationName + " " + versionName);
      }
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      output(statusOutputStream, "Unknown application/version.");
      throw new XFMG_CouldNotBuildWorkingSet(applicationName, versionName);
    }

    Long targetRevision;
    try {
      targetRevision = revisionManagement.getRevision(params.getTargetWorkspace());
      if (logger.isDebugEnabled()) {
        logger.debug("Got revision " + targetRevision + " for " + params.getTargetWorkspace());
      }
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      output(statusOutputStream, "Unknown workspace.");
      throw new XFMG_CouldNotBuildWorkingSet(applicationName, versionName);
    }

    BatchRepositoryEvent repositoryEvent = new BatchRepositoryEvent(targetRevision);

    boolean success = false;
    ODSConnection con = ods.openConnection();
    try {
      ApplicationStorable application = queryRuntimeApplicationStorable(applicationName, versionName, con);

      if (application.getStateAsEnum() == ApplicationState.AUDIT_MODE) {
        Exception cause = new Exception("Could not copy application in state " + application.getState() + " to workspace.");
        throw new XFMG_CouldNotBuildWorkingSet(applicationName, versionName, cause);
      }

      String workingsetVersion = getWorkingsetVersionName(applicationName, targetRevision);
      if (workingsetVersion != null) {
        if (logger.isDebugEnabled()) {
          logger.debug("Old application definition will be removed");
        }
        if (verbose) {
          output(statusOutputStream, "Delete old application definition");
        }
        List<? extends ApplicationEntryStorable> oldAppEntries =
            queryAllApplicationDefinitionStorables(applicationName, targetRevision, con);
        con.delete(oldAppEntries);
        ApplicationStorable app = queryApplicationDefinitionStorable(applicationName, targetRevision, con);
        con.deleteOneRow(app);

        repositoryEvent.addEvent(new BasicProjectCreationOrChangeEvent(EventType.APPLICATION_DEFINITION_CHANGE, applicationName));
      } else {
        repositoryEvent.addEvent(new BasicProjectCreationOrChangeEvent(EventType.APPLICATION_DEFINE, applicationName));
      }

      workingsetVersion = "workingset of version " + versionName;
      ApplicationStorable app = new ApplicationStorable(applicationName, workingsetVersion, params.getComment(), targetRevision);
      con.persistObject(app);
      applicationDefinitionVersionNameCache.put(new ApplicationDefinitionKey(app), new WorkingSet(workingsetVersion));
      output(statusOutputStream, "Create new application version <" + workingsetVersion + ">");

      if (logger.isDebugEnabled()) {
        logger.debug("New application definition version will be named <" + workingsetVersion + ">");
      }

      Set<ApplicationEntryStorable> appEntries = new TreeSet<ApplicationEntryStorable>(ApplicationEntryStorable.COMPARATOR);
      appEntries.addAll(queryAllRuntimeApplicationStorables(applicationName, versionName, con));
      List<ApplicationEntryStorable> newAppEntries = new ArrayList<ApplicationEntryStorable>();
      for (ApplicationEntryStorable entry : appEntries) {
        if (logger.isDebugEnabled()) {
          logger.debug("Create new application entry " + entry.getType() + " " + entry.getName());
        }
        ApplicationEntryStorable newEntry =
            ApplicationEntryStorable.toStore(applicationName, workingsetVersion, targetRevision, entry.getName(), entry.getTypeAsEnum());
        newAppEntries.add(newEntry);
      }
      con.persistCollection(newAppEntries);

      // for the rest we also need the implicit application entries
      appEntries.addAll(getAllImplicitApplicationEntries(appEntries, applicationName, versionName, null, false));
      newAppEntries = new ArrayList<ApplicationEntryStorable>(); //alle appentries mit neuer version

      for (ApplicationEntryStorable entry : appEntries) {
        ApplicationEntryStorable newEntry =
            ApplicationEntryStorable.create(applicationName, workingsetVersion, targetRevision, entry.getName(), entry.getTypeAsEnum());
        newAppEntries.add(newEntry);
      }

      //RuntimeContext Requirements kopieren
      Workspace targetWorkspace = revisionManagement.getWorkspace(targetRevision);
      copyRuntimeContextRequirements(new Application(applicationName, versionName), new ApplicationDefinition(applicationName,
                                                                                                              targetWorkspace));
      try {
        if (logger.isDebugEnabled()) {
          logger.debug("Copy all files to workspace folder");
        }
        copyFilesToWorkingset(newAppEntries, revision, targetRevision, params.overrideChanges(), getCopyWhiteList(),
                              verbose, statusOutputStream);
        saveFiles(newAppEntries, revision, targetRevision, repositoryEvent);
      } catch (Exception e) {
        throw new XFMG_CouldNotBuildWorkingSet(applicationName, versionName, e);
      }

      //OrderInputSources kopieren
      copyInputSources(newAppEntries, revision, targetRevision);

      copyFormDefinitions(newAppEntries, revision, targetRevision, repositoryEvent, verbose, statusOutputStream);

      try {
        if (verbose) {
          output(statusOutputStream, "Deploy objects");
        }
        if (logger.isDebugEnabled()) {
          logger.debug("Start redeploy");
        }
        deploySharedLibs(newAppEntries, targetRevision, repositoryEvent, statusOutputStream);
        //rekursiv mit codechanged deployen
        redeploy(targetRevision, newAppEntries, DeploymentMode.codeChanged, true, statusOutputStream, "Copy Application into Workspace");
      } catch (Exception e) {
        logger.warn("Failed to redeploy objects in workspace.", e);
        output(statusOutputStream, "Failed to redeploy objects in workspace.");
      }

      con.commit();

      try {
        transferFromODSTypeToODSType(ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY, ApplicationEntryStorable.TABLE_NAME,
                                     ApplicationEntryStorable.class);
        transferFromODSTypeToODSType(ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY, ApplicationStorable.TABLE_NAME,
                                     ApplicationStorable.class);
      } catch (XynaException e) {
        logger.error("Could not persist application informations.", e);
      }

      copyCapacityMappingsMonitoringLevelsOrdertypesAndPriorities(appEntries, revision, targetRevision, verbose, statusOutputStream);

      copyTriggerAndFilterAndInstances(appEntries, revision, targetRevision, false, repositoryEvent);
      success = true;
    } catch (XynaException e) {
      if (e instanceof XFMG_CouldNotBuildWorkingSet) {
        throw (XFMG_CouldNotBuildWorkingSet) e;
      }
      throw new XFMG_CouldNotBuildWorkingSet(applicationName, versionName, e);
    } catch (RuntimeException e) {
      logger.trace(null, e);
      throw e;
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Can't close connection.", e);
      }
      if (!success) {
        //TODO rollback
      }
    }

    clearApplicationDefinitionCache(targetRevision);
    updateApplicationDetailsCache(targetRevision);

    //Multi-User-Event für RuntimeContext Änderung
    Publisher publisher = new Publisher(params.getUser());
    publisher.publishRuntimeContextCreate(new ApplicationDefinition(applicationName, params.getTargetWorkspace()));

    repositoryEvent.execute("copy application " + applicationName + "/" + versionName + " into " + params.getTargetWorkspace());
  }


  private void saveFiles(List<ApplicationEntryStorable> appEntries, long revisionFrom, long revisionTo, RepositoryEvent repositoryEvent)
      throws XynaException {
    //wenn command von gui aus angestossen wurde, nicht den dortigen user verwenden. TODO es wäre aber vermutlich richtig, seine rolle zu übernehmen

    String fromPathXMOM = RevisionManagement.getPathForRevision(PathType.XMOM, revisionFrom);

    TemporarySessionAuthentication tsa =
        TemporarySessionAuthentication.tempAuthWithUniqueUserAndOperationLock("CopyAppToWS",
                                                                              TemporarySessionAuthentication.TEMPORARY_CLI_USER_ROLE,
                                                                              revisionTo, CommandControl.Operation.XMOM_SAVE);
    tsa.initiate();
    try {
      for (ApplicationEntryStorable appEntry : appEntries) {
        if (appEntry.getTypeAsEnum() == ApplicationEntryType.DATATYPE || appEntry.getTypeAsEnum() == ApplicationEntryType.WORKFLOW
            || appEntry.getTypeAsEnum() == ApplicationEntryType.EXCEPTION) {
          StringBuilder fromFilename = new StringBuilder();
          fromFilename.append(fromPathXMOM).append(Constants.fileSeparator)
              .append(appEntry.getName().replace('.', Constants.fileSeparator.charAt(0))).append(".xml");

          ((XynaMultiChannelPortal) XynaFactory.getInstance().getXynaMultiChannelPortal())
              .saveMDM(FileUtils.readFileAsString(new File(fromFilename.toString())), true, tsa.getUsername(), tsa.getSessionId(),
                       revisionTo, repositoryEvent, true, true);
        }
      }
    } finally {
      tsa.destroy();
    }

  }


  private void deploySharedLibs(List<ApplicationEntryStorable> appEntries, Long targetRevision, RepositoryEvent repositoryEvent,
                                PrintStream statusOutputStream) {
    Iterator<ApplicationEntryStorable> iter = appEntries.iterator();
    while (iter.hasNext()) {
      ApplicationEntryStorable appEntry = iter.next();
      if (appEntry.getTypeAsEnum() == ApplicationEntryType.SHAREDLIB) {
        try {
          SharedLibDeploymentAlgorithm.deploySharedLib(appEntry.getName(), targetRevision, repositoryEvent);
        } catch (Throwable t) {
          Department.handleThrowable(t);
          logger.error("Could not deploy sharedLib  " + appEntry.getName() + ", continuing ...", t);
          output(statusOutputStream, "WARN Could not deploy sharedLib " + appEntry.getName() + ".");
        }
        iter.remove();
      }
    }
  }


  private static RevisionContentBlackWhiteList getCopyWhiteList() {
    return new RevisionContentBlackWhiteListBean();
  }


  private void copyCapacityMappingsMonitoringLevelsOrdertypesAndPriorities(Collection<? extends ApplicationEntryStorable> appEntries,
                                                                           Long oldRevision, Long newRevision, boolean verbose,
                                                                           PrintStream statusOutputStream) throws XynaException {

    OrderContextConfiguration orderContext =
        XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderContextConfiguration();
    OrdertypeManagement orderTypeManagement =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderTypeManagement();

    for (ApplicationEntryStorable appEntry : appEntries) {
      // Custom Ordertypes setzen
      if (appEntry.getTypeAsEnum() == ApplicationEntryType.ORDERTYPE || appEntry.getTypeAsEnum() == ApplicationEntryType.WORKFLOW) {
        String orderType = appEntry.getName();
        if (appEntry.getTypeAsEnum() == ApplicationEntryType.WORKFLOW) {
          try {
            orderType = GenerationBase.transformNameForJava(orderType);
          } catch (XPRC_InvalidPackageNameException e) {
            throw new RuntimeException(e);
          }
        }

        RuntimeContext oldRuntimeContext = revisionManagement.getRuntimeContext(oldRevision);
        OrdertypeParameter oldOrdertype;
        try {
          oldOrdertype = orderTypeManagement.getOrdertype(orderType, oldRuntimeContext);
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          String msg = "ordertype not found: " + orderType + " in " + oldRuntimeContext + ".";
          logger.warn(msg, e);
          output(statusOutputStream, "WARN " + msg);
          continue;
        }

        if (!oldOrdertype.containsCustomConfig()) {
          continue;
        }

        OrdertypeParameter newOrdertype = oldOrdertype;
        RuntimeContext newRuntimeContext = revisionManagement.getRuntimeContext(newRevision);
        newOrdertype.setRuntimeContext(newRuntimeContext);
        try {
          orderTypeManagement.createOrdertype(newOrdertype, false);
        } catch (XFMG_InvalidCreationOfExistingOrdertype e) {
          try {
            orderTypeManagement.modifyOrdertype(newOrdertype);
          } catch (XFMG_InvalidModificationOfUnexistingOrdertype e1) {
            String msg = "ordertype configuration for <" + appEntry.getName() + "> could not be copied to " + newRuntimeContext + ".";
            output(statusOutputStream, "WARN " + msg);
            logger.warn(msg, e);
            logger.warn("modify didn't work either. ", e1);
            continue;
          }
        } catch (XFMG_FailedToAddObjectToApplication e) {
          throw new RuntimeException(e);
        }

        DestinationKey oldDestKey = new DestinationKey(appEntry.getName(), oldRuntimeContext);
        DestinationKey newDestKey = new DestinationKey(appEntry.getName(), newRuntimeContext);

        if (orderContext.isDestinationKeyConfiguredForOrderContextMapping(oldDestKey, true)) {
          try {
            orderContext.configureDestinationKey(newDestKey, true);
          } catch (PersistenceLayerException e) {
            String msg = "Could not configure ordercontext for ordertype " + newDestKey.getOrderType();
            output(statusOutputStream, "WARN " + msg);
            logger.warn(msg, e);
          }
        }
      }
    }
  }


  private void copyFormDefinitions(Collection<? extends ApplicationEntryStorable> appEntries, Long oldRevision, Long newRevision,
                                   RepositoryEvent repositoryEvent, boolean verbose, PrintStream statusOutputStream) throws XynaException {


    for (ApplicationEntryStorable appEntry : appEntries) {
      if (appEntry.getTypeAsEnum() == ApplicationEntryType.FORMDEFINITION) {
        String formLocation;
        if (revisionManagement.isWorkspaceRevision(oldRevision)) {
          formLocation = GenerationBase.getFileLocationOfXmlNameForSaving(appEntry.getName(), oldRevision);
        } else {
          formLocation = GenerationBase.getFileLocationForDeploymentStaticHelper(appEntry.getName(), oldRevision);
        }
        File formFile = new File(formLocation + ".xml");
        if (formFile.exists()) {
          String fileName;
          if (revisionManagement.isWorkspaceRevision(newRevision)) {
            fileName =
                GenerationBase.getFileLocationOfXmlNameForSaving(appEntry.getName().substring(0, appEntry.getName().lastIndexOf('.')),
                                                                 newRevision);
          } else {
            fileName =
                GenerationBase.getFileLocationForDeploymentStaticHelper(appEntry.getName()
                    .substring(0, appEntry.getName().lastIndexOf('.')), newRevision);
          }
          File destinationFolder = new File(fileName);
          FileUtils.copyRecursivelyWithFolderStructure(formFile, destinationFolder);
        } else {
          if (logger.isDebugEnabled()) {
            logger.debug("Could not copy form definition " + appEntry.getName() + " as the file " + formLocation + " does not exist.");
          }
        }

        if (repositoryEvent != null) {
          repositoryEvent.addEvent(new XMOMChangeEvent(appEntry.getName(), XMOMType.FORM));
        }
      }
    }
  }


  /**
   * Kopiert alle in appEntries enthaltenden OrderInputSources in einen neuen RuntimeContext
   * @param appEntries
   * @param oldRevision
   * @param newRuntimeContext
   * @throws XynaException
   */
  private void copyInputSources(Collection<? extends ApplicationEntryStorable> appEntries, Long oldRevision, Long newRevision)
      throws XynaException {
    OrderInputSourceManagement oism =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderInputSourceManagement();
    RuntimeContext newRuntimeContext = revisionManagement.getRuntimeContext(newRevision);

    for (ApplicationEntryStorable entry : appEntries) {
      if (entry.getTypeAsEnum() == ApplicationEntryType.ORDERINPUTSOURCE) {
        OrderInputSourceStorable inputSource = oism.getInputSourceByName(oldRevision, entry.getName(), true);
        inputSource.setRuntimeContext(newRuntimeContext);
        try {
          oism.createOrderInputSource(inputSource);
        } catch (XFMG_InputSourceNotUniqueException e) {
          //InputSource existiert bereits -> mit neuen Daten überschreiben
          inputSource.setId(-1); //korrekte id anhand des namens suchen
          oism.modifyOrderInputSource(inputSource);
        }
      }
    }
  }


  /**
   * Kopiert die Requirements eines RuntimeContexts in einen anderen.
   * Requirements auf andere Application Definitions werden in Requirements auf die
   * neuste Version der entsprechenden Runtime-Applications umgewandelt.
   * @param source
   * @param target
   * @throws XFMG_NoCorrespondingApplicationException falls für ein Requirement zu einer ApplicationDefinition keine entsprechende RuntimeApplication existiert
   * @throws PersistenceLayerException
   * @throws XFMG_CouldNotModifyRuntimeContextDependenciesException
   */
  private void copyRuntimeContextRequirements(RuntimeDependencyContext source, RuntimeDependencyContext target)
      throws XFMG_NoCorrespondingApplicationException, PersistenceLayerException, XFMG_CouldNotModifyRuntimeContextDependenciesException {
    RuntimeContextDependencyManagement rcdMgmt =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();

    Collection<RuntimeDependencyContext> sourceRequirements = rcdMgmt.getRequirements(source);
    Collection<RuntimeDependencyContext> targetRequirements = new ArrayList<RuntimeDependencyContext>();

    for (RuntimeDependencyContext req : sourceRequirements) {
      if (req instanceof Application || req instanceof Workspace) {
        //Requirement übernehmen
        targetRequirements.add(req);
      }
      if (req instanceof ApplicationDefinition) {
        //Requirement zu ApplicationDefinition in Requirement auf die neuste Version
        //der entsprechenden RuntimeApplication ändern
        try {
          Long revision = revisionManagement.getRevision(new Application(req.getName(), null)); //liefert die höchste Revision
          targetRequirements.add(RuntimeContextDependencyManagement.asRuntimeDependencyContext(revisionManagement.getRuntimeContext(revision)));
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          throw new XFMG_NoCorrespondingApplicationException(req.toString());
        }
      }
    }

    rcdMgmt.modifyDependencies(target, targetRequirements, null, true, true);
  }


  private void recursiveGetInputSourceDependencies(String inputSourceName, Long oldRevision, Long newRevision, boolean verbose,
                                                   PrintStream statusOutputStream, Set<DependencyNode> processedDependencies,
                                                   PredictionValues predictionValues, Set<String> processedOrderInputSources,
                                                   Set<ApplicationEntryStorable> reqAppEntries) throws XynaException {
    if (processedOrderInputSources.add(inputSourceName)) {
      //verwendete Ordertypes ermitteln
      String deploymentItemStateName = OrderInputSourceManagement.convertNameToUniqueDeploymentItemStateName(inputSourceName);

      DeploymentItemStateImpl diis =
          (DeploymentItemStateImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
              .getDeploymentItemStateManagement().get(deploymentItemStateName, oldRevision);
      if (diis != null) {
        Set<String> usedOrderTypes = diis.getUsedOrderTypes(DeploymentLocation.DEPLOYED);
        
        XynaProcessingBase proc = XynaFactory.getInstance().getProcessing();
        RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
        for (String ot : usedOrderTypes) {
          try {
            DestinationKey dk = new DestinationKey(ot, revMgmt.getRuntimeContext(oldRevision));
            DispatcherEntry de = proc.getDestination(DispatcherIdentification.Execution, dk, false);
            if (de != null) { // only add if resolvable in own revision
              DeploymentLocks.readLock(ot, DependencySourceType.ORDERTYPE, "BuildApplication", oldRevision);
              recursiveLockAndCopyDependencies(ot, DependencySourceType.ORDERTYPE, oldRevision, newRevision, verbose, statusOutputStream,
                                               processedDependencies, predictionValues, processedOrderInputSources, reqAppEntries);
            }
          } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
            // only add if resolvable in own revision
          } catch (XPRC_DESTINATION_NOT_FOUND e) {
            // only add if resolvable in own revision
          }
        }
      }
    }
  }


  private void importXynaProperties(List<XynaPropertyXmlEntry> properties, boolean verbose, PrintStream statusOutputStream)
      throws PersistenceLayerException {
    for (XynaPropertyXmlEntry property : properties) {
      XynaFactory.getInstance().getFactoryManagement().setProperty(property.getName(), property.getValue());
      if (verbose) {
        output(statusOutputStream, "Import xyna property <" + property.getName() + "> with value <" + property.getValue() + ">");
      }
      if (logger.isDebugEnabled()) {
        logger.debug("Import xyna property <" + property.getName() + "> with value <" + property.getValue() + ">");
      }
    }
  }


  private void importCapacities(List<CapacityXmlEntry> capacities, boolean verbose, PrintStream statusOutputStream)
      throws PersistenceLayerException, XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState,
      XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain {
    for (CapacityXmlEntry capacity : capacities) {
      State state = capacity.getState() != null ? capacity.getState() : State.ACTIVE;
      try {
        //Capacity neu anlegen
        XynaFactory.getInstance().getProcessing().getXynaScheduler().getCapacityManagement()
            .addCapacity(capacity.getName(), capacity.getCardinality(), state);
      } catch (XPRC_CAPACITY_ALREADY_DEFINED e) {
        //Capacity existiert bereits
        //Kardinalität übernehmen
        XynaFactory.getInstance().getProcessing().getXynaScheduler().getCapacityManagement()
            .changeCardinality(capacity.getName(), capacity.getCardinality());
        if (capacity.getState() != null) {
          //Status übernehmen, falls er im Export gesetzt wurde
          XynaFactory.getInstance().getProcessing().getXynaScheduler().getCapacityManagement()
              .changeState(capacity.getName(), capacity.getState());
        }
      }
      if (verbose) {
        output(statusOutputStream, "Import capacity <" + capacity.getName() + "> with a cardinality of " + capacity.getCardinality()
            + " and state " + state);
      }
      if (logger.isDebugEnabled()) {
        logger.debug("Import capacity <" + capacity.getName() + "> with a cardinality of " + capacity.getCardinality() + " and state "
            + state);
      }
    }
  }


  private void importRuntimeContextRequirements(Application application, List<RuntimeContextRequirementXmlEntry> requirements,
                                                boolean verbose, PrintStream statusOutputStream) throws PersistenceLayerException,
      XFMG_CouldNotModifyRuntimeContextDependenciesException {
    List<RuntimeDependencyContext> newRequirements = new ArrayList<RuntimeDependencyContext>();
    for (RuntimeContextRequirementXmlEntry rcr : requirements) {
      RuntimeDependencyContext rc = rcr.getRuntimeContext();
      newRequirements.add(rc);
      if (verbose) {
        output(statusOutputStream, "Import runtime context requirement <" + rc.toString() + ">");
      }
      if (logger.isDebugEnabled()) {
        logger.debug("Import runtime context requirement <" + rc.toString() + ">");
      }
    }

    if (newRequirements.size() > 0) {
      RuntimeContextDependencyManagement rcdMgmt =
          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
      rcdMgmt.modifyDependencies(application, newRequirements, null);
    }
  }


  public ApplicationInformation getApplicationInformationForApplicationFile(String fileName, PrintStream statusOutputStream)
      throws XFMG_CouldNotImportApplication {
    ApplicationFileReader afr = new ApplicationFileReader(statusOutputStream, XML_APPLICATION_FILENAME, true); //FIXME true?
    afr.read(fileName);
    return afr.getApplicationInformation();
  }


  public void importApplication(final String fileName, final boolean force, final boolean excludeXynaProperties,
                                final boolean excludeCapacities, final boolean importOnlyXynaProperties,
                                final boolean importOnlyCapacities, boolean clusterwide, boolean verbose, PrintStream statusOutputStream)
      throws XFMG_DuplicateVersionForApplicationName, XFMG_CouldNotImportApplication, XFMG_CouldNotRemoveApplication,
      XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState, XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain {
    importApplication(fileName, force, excludeXynaProperties, excludeCapacities, importOnlyXynaProperties, importOnlyCapacities,
                      clusterwide, false, verbose, statusOutputStream);
  }


  public ApplicationInformation importApplication(final String fileName, final boolean force, final boolean excludeXynaProperties,
                                                  final boolean excludeCapacities, final boolean importOnlyXynaProperties,
                                                  final boolean importOnlyCapacities, boolean clusterwide, boolean regenerateCode,
                                                  boolean verbose, PrintStream statusOutputStream)
      throws XFMG_DuplicateVersionForApplicationName, XFMG_CouldNotImportApplication, XFMG_CouldNotRemoveApplication,
      XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState, XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain {
    return importApplication(fileName, force, false, excludeXynaProperties, excludeCapacities, importOnlyXynaProperties,
                             importOnlyCapacities, clusterwide, false, verbose, null, statusOutputStream, true, true);
  }
 
  public ApplicationInformation importApplication(final String fileName, final boolean force, 
                                                  final boolean stopIfExistingAndRunning,
                                                  final boolean excludeXynaProperties, 
                                                  final boolean excludeCapacities,
                                                  final boolean importOnlyXynaProperties, 
                                                  final boolean importOnlyCapacities,
                                                  boolean clusterwide, boolean regenerateCode, boolean verbose, 
                                                  final String user,
                                                  PrintStream statusOutputStream) 
      throws XFMG_DuplicateVersionForApplicationName,
      XFMG_CouldNotImportApplication, XFMG_CouldNotRemoveApplication, XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState,
      XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain {
    return importApplication(fileName, force, stopIfExistingAndRunning, excludeXynaProperties,
                             excludeCapacities, importOnlyXynaProperties, importOnlyCapacities, clusterwide,
                             regenerateCode, verbose, user, statusOutputStream, true, true);
  }
  
  public static enum GlobalItemImportSetting {
    NORMAL_IMPORT, EXCLUDE, IMPORT_ONLY;
  }
  
  public static enum XMOMODSNameImportSetting {
    ABORT_ON_COLLISION("abort"),
    ALLOW_REGENERATION("allow"),
    EXCLUDE("exclude");
    
    private final String name;
    
    private XMOMODSNameImportSetting(String name) {
      this.name = name;
    }
    
    public static XMOMODSNameImportSetting byName(String name) {
      for (XMOMODSNameImportSetting setting : values()) {
        if (setting.name.equalsIgnoreCase(name)) {
          return setting;
        }
      }
      return ABORT_ON_COLLISION; // default
    }
  }

  public static class ImportApplicationCommandParameter {
    
    private String fileName;
    private boolean force;
    private boolean stopIfExistingAndRunning;
    private GlobalItemImportSetting xynaProperties = GlobalItemImportSetting.NORMAL_IMPORT;
    private GlobalItemImportSetting capacities = GlobalItemImportSetting.NORMAL_IMPORT;
    private boolean clusterwide; 
    private boolean regenerateCode;
    private boolean verbose;
    private String user;
    private PrintStream statusOutputStream;
    private boolean upgradeRequirements;
    private XMOMODSNameImportSetting odsNames = XMOMODSNameImportSetting.ABORT_ON_COLLISION;
    private boolean abortOnCodegeneration;
    private Long revision = null;


    public ImportApplicationCommandParameter() {
    }
    
    public ImportApplicationCommandParameter fileName(String fileName) {
      this.fileName = fileName;
      return this;
    }
    
    public String getFileName() {
      return fileName;
    }
    
    public ImportApplicationCommandParameter force(boolean force) {
      this.force = force;
      return this;
    }
    
    public boolean isForce() {
      return force;
    }
    
    public ImportApplicationCommandParameter abortOnCodegeneration(boolean abortOnCodegeneration) {
      this.abortOnCodegeneration = abortOnCodegeneration;
      return this;
    }
    
    public boolean isAbortOnCodegeneration() {
      return abortOnCodegeneration;
    }
    
    public ImportApplicationCommandParameter stopIfExistingAndRunning(boolean stopIfExistingAndRunning) {
      this.stopIfExistingAndRunning = stopIfExistingAndRunning;
      return this;
    }
    
    public boolean isStopIfExistingAndRunning() {
      return stopIfExistingAndRunning;
    }
    
    public ImportApplicationCommandParameter xynaPropertiesImportSettings(GlobalItemImportSetting xynaProperties) {
      this.xynaProperties = xynaProperties;
      return this;
    }
    
    public ImportApplicationCommandParameter xynaPropertiesImportSettings(boolean excludeXynaProperties, boolean importOnlyXynaProperties) {
      if (importOnlyXynaProperties) {
        this.xynaProperties = GlobalItemImportSetting.IMPORT_ONLY;
      } else if (excludeXynaProperties) {
        this.xynaProperties = GlobalItemImportSetting.EXCLUDE;
      }
      return this;
    }
    
    public GlobalItemImportSetting getXynaPropertiesImportSettings() {
      return xynaProperties;
    }
    
    public boolean importXynaPropertiesOnly() {
      return xynaProperties == GlobalItemImportSetting.IMPORT_ONLY;
    }
    
    public boolean excludeXynaProperties() {
      return xynaProperties == GlobalItemImportSetting.EXCLUDE;
    }
    
    public ImportApplicationCommandParameter capacitiesImportSettings(GlobalItemImportSetting capacities) {
      this.capacities = capacities;
      return this;
    }
    
    public ImportApplicationCommandParameter capacitiesImportSettings(boolean excludeCapacities, boolean importOnlyCapacities) {
      if (importOnlyCapacities) {
        this.capacities = GlobalItemImportSetting.IMPORT_ONLY;
      } else if (excludeCapacities) {
        this.capacities = GlobalItemImportSetting.EXCLUDE;
      }
      return this;
    }
    
    public GlobalItemImportSetting getCapacitiesImportSettings() {
      return capacities;
    }
    
    public boolean importCapacitiesOnly() {
      return capacities == GlobalItemImportSetting.IMPORT_ONLY;
    }
    
    public boolean excludeCapacities() {
      return capacities == GlobalItemImportSetting.EXCLUDE;
    }

    public boolean importGlobalSettingsOnly() {
      return capacities == GlobalItemImportSetting.IMPORT_ONLY ||
             xynaProperties == GlobalItemImportSetting.IMPORT_ONLY;      
    }
    
    public ImportApplicationCommandParameter clusterwide(boolean clusterwide) {
      this.clusterwide = clusterwide;
      return this;
    }
    
    public boolean isClusterwide() {
      return clusterwide;
    }
    
    public ImportApplicationCommandParameter regenerateCode(boolean regenerateCode) {
      this.regenerateCode = regenerateCode;
      return this;
    }
    
    public boolean isRegenerateCode() {
      return regenerateCode;
    }
    
    public ImportApplicationCommandParameter verbose(boolean verbose) {
      this.verbose = verbose;
      return this;
    }
    
    public boolean isVerbose() {
      return verbose;
    }
    
    public ImportApplicationCommandParameter user(String user) {
      this.user = user;
      return this;
    }
    
    public String getUser() {
      return user;
    }
    
    public ImportApplicationCommandParameter statusOutputStream(PrintStream statusOutputStream) {
      this.statusOutputStream = statusOutputStream;
      return this;
    }
    
    public PrintStream getStatusOutputStream() {
      return statusOutputStream;
    }
    
    public ImportApplicationCommandParameter upgradeRequirements(boolean upgradeRequirements) {
      this.upgradeRequirements = upgradeRequirements;
      return this;
    }
    
    public boolean isUpgradeRequirements() {
      return upgradeRequirements;
    }
    
    public ImportApplicationCommandParameter odsNames(XMOMODSNameImportSetting odsNames) {
      this.odsNames = odsNames;
      return this;
    }
    
    public XMOMODSNameImportSetting getOdsNameSettings() {
      return odsNames;
    }

    public ImportApplicationCommandParameter revision(Long revision) {
      this.revision = revision;
      return this;
    }

    public Long getRevision() {
      return revision;
    }

  }
  
  
  
  public ApplicationInformation importApplication(final String fileName, final boolean force, final boolean stopIfExistingAndRunning,
      final boolean excludeXynaProperties, final boolean excludeCapacities,
      final boolean importOnlyXynaProperties, final boolean importOnlyCapacities,
      boolean clusterwide, boolean regenerateCode, boolean verbose, final String user,
      PrintStream statusOutputStream, boolean upgradeRequirements,
      boolean allowStorableNameGeneration) throws XFMG_DuplicateVersionForApplicationName,
XFMG_CouldNotImportApplication, XFMG_CouldNotRemoveApplication, XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState,
XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain {
    ImportApplicationCommandParameter iap = new ImportApplicationCommandParameter();
    iap.fileName(fileName)
       .force(force)
       .stopIfExistingAndRunning(stopIfExistingAndRunning)
       .xynaPropertiesImportSettings(excludeXynaProperties, importOnlyXynaProperties)
       .capacitiesImportSettings(excludeCapacities, importOnlyCapacities)
       .clusterwide(clusterwide)
       .regenerateCode(regenerateCode)
       .verbose(verbose)
       .user(user)
       .statusOutputStream(statusOutputStream)
       .upgradeRequirements(upgradeRequirements)
       .odsNames(allowStorableNameGeneration ? XMOMODSNameImportSetting.ALLOW_REGENERATION : XMOMODSNameImportSetting.ABORT_ON_COLLISION);
    return importApplication(iap);
  }
  
  public ApplicationInformation importApplication(ImportApplicationCommandParameter importParameter) throws XFMG_DuplicateVersionForApplicationName,
      XFMG_CouldNotImportApplication, XFMG_CouldNotRemoveApplication, XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState,
      XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain {

    
    boolean checkBinaries = !importParameter.importGlobalSettingsOnly();
    ApplicationFileReader afr = new ApplicationFileReader(importParameter.getStatusOutputStream(), XML_APPLICATION_FILENAME, checkBinaries);
    afr.read(importParameter.getFileName());

    if (checkBinaries) {
      checkOpenFiles(afr.getPredictedAdditionalOpenFiles());
      checkPermGen(afr.getCumulatedSizeOfJars(), afr.getCumulatedSizeOfXMOMClasses());
    }

    ApplicationXmlEntry applicationXml = afr.getApplicationXml();

    if (importParameter.importGlobalSettingsOnly()) {
      if (importParameter.importCapacitiesOnly()) {
        try {
          logger.debug("Import only Capacities");
          importCapacities(applicationXml.getCapacities(), importParameter.isVerbose(), importParameter.getStatusOutputStream());
        } catch (PersistenceLayerException e) {
          logger.error("Failed to import xyna properties.", e);
        }
      }
      if (importParameter.importXynaPropertiesOnly()) {
        try {
          logger.debug("Import only Xynaproperties");
          importXynaProperties(applicationXml.getXynaProperties(), importParameter.isVerbose(), importParameter.getStatusOutputStream());
        } catch (PersistenceLayerException e) {
          logger.error("Failed to import xyna properties.", e);
        }
      }
      return null;
    }
    
    String applicationName = applicationXml.getApplicationName();
    String versionName = applicationXml.getVersionName();
    Application app = new Application(applicationName, versionName);    
    String factoryVersion = applicationXml.getFactoryVersion();
    boolean regenerateCode = importParameter.isRegenerateCode();
    if (factoryVersion != null && factoryVersion.length() > 0) {
      try {
        if (!Updater.getInstance().getFactoryVersion().equals(new Version(factoryVersion))) {
          regenerateCode = true;
        } else if (afr.getCumulatedSizeOfXMOMClasses() == 0) {
          //es gibt in der application keine classfiles?
          regenerateCode = true;
        }
      } catch (XPRC_VERSION_DETECTION_PROBLEM e) {
        regenerateCode = true;
      } catch (PersistenceLayerException e) {
        regenerateCode = true;
      }
    } else {
      regenerateCode = true;
    }

    if (regenerateCode && importParameter.isAbortOnCodegeneration()) {
      output(importParameter.getStatusOutputStream(), "Code generation required, but forbidden. Abort.");
      throw new XFMG_CouldNotImportApplication(importParameter.getFileName());
    }

    RuntimeContextDependencyManagement rcdMgmt =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
    Set<RuntimeDependencyContext> parentRuntimeContexts = null;

    Long revision = null;
    ODSConnection con = ods.openConnection();
    ApplicationStorable application = null;
    try {
      if (importParameter.isUpgradeRequirements()) {
        new AppRequirementVersionUpgrade().execute(con, applicationXml, this, importParameter.getStatusOutputStream());
      }      
      if (mustRemoveOldApplication(con, applicationName, versionName, importParameter.isForce(), importParameter.getStatusOutputStream())) {
        application = queryRuntimeApplicationStorable(applicationName, versionName, con);
        if (application == null) {
          throw new RuntimeException("Existing application inconsistent. Please repair or remove with -ff flag.");
        }
        checkApplicationStopped(application, importParameter.isClusterwide(), importParameter.isStopIfExistingAndRunning());
        
        // alte Application komplett entfernen
        output(importParameter.getStatusOutputStream(), "Remove Application " + applicationName + " " + versionName);
        if (logger.isDebugEnabled()) {
          logger.debug("Try to remove application " + applicationName + " " + versionName);
        }

        parentRuntimeContexts = rcdMgmt.getParentRuntimeContexts(app);
        modifyRuntimeContextDependencies(importParameter.getUser(), app, importParameter.isVerbose(), importParameter.getStatusOutputStream(), rcdMgmt, parentRuntimeContexts, false);

        RemoveApplicationParameters params = new RemoveApplicationParameters();
        params.setGlobal(importParameter.isClusterwide());
        params.setForce(true);
        params.setExtraForce(true);
        params.setRemoveIfUsed(true);
        params.setStopIfRunning(importParameter.isStopIfExistingAndRunning());
        try {
          removeApplicationVersion(new ApplicationName(applicationName, versionName), params, importParameter.isVerbose(), importParameter.getStatusOutputStream(),
                                   new EmptyRepositoryEvent(), false);
        } catch (RuntimeException e) {
          throw new RuntimeException("Failed to remove Application after removing all referencing runtime context dependencies.", e);
        } catch (XFMG_CouldNotRemoveApplication e) {
          throw new XFMG_CouldNotRemoveApplication(applicationName,
                                                   versionName,
                                                   new RuntimeException(
                                                                        "Failed to remove Application after removing all referencing runtime context dependencies.",
                                                                        e));
        } catch (Error e) {
          Department.handleThrowable(e);
          throw new RuntimeException("Failed to remove Application after removing all referencing runtime context dependencies.", e);
        }
      }

      //neue revision anlegen
      revision = revisionManagement.buildNewRevisionForNewVersion(applicationName, versionName, importParameter.getRevision());
      if (logger.isDebugEnabled()) {
        logger.debug("Got revision " + revision + " for new application " + applicationName + " " + versionName);
      }

      //RMI/CLI-Schnittstellen sperren, customOrderEntries einrichten
      RevisionOrderControl roc = new RevisionOrderControl(revision);
      roc.closeRMICLI();
      RevisionOrderControl.applyCustomOrderEntries(applicationName, versionName, false);

      // Applikation anlegen
      application = new ApplicationStorable(applicationName, versionName, ApplicationState.STOPPED, applicationXml.getComment());
      application.setRemoteStub(applicationXml.getApplicationInfo().isRemoteStub());
      con.persistObject(application);

      logger.debug("Unzip the file to revision folder");
      if (importParameter.isVerbose()) {
        output(importParameter.getStatusOutputStream(), "Extract file");
      }
      
      //Application entpacken
      File applicationDir = unzipApplication(importParameter.getFileName(), revision);
      
      eventHandling.triggerEvent(new ApplicationImportEvent(applicationName, versionName, applicationDir.listFiles()));
      
      //xmls auf aktuelle XMOM Version updaten
      Updater.getInstance().updateMdm(revision);
      Updater.getInstance().updateApplicationAtImport(revision, applicationXml);

      output(importParameter.getStatusOutputStream(), "Setting runtime context dependencies of application");
      //Runtime Context Requirements anlegen (vor dem Deployment)
      importRuntimeContextRequirements(new Application(applicationName, versionName), applicationXml.getRuntimeContextRequirements(),
                                       importParameter.isVerbose(), importParameter.getStatusOutputStream());
      
      saveApplicationEntries(con, applicationXml, null);

      //Order Input Sources anlegen
      createInputSources(applicationXml, revision);

      output(importParameter.getStatusOutputStream(), "Register all objects in deploymentitemregistry");
      //Objekte im DeploymentItemStateManagement registrieren
      DeploymentItemStateManagement dism =
          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement();
      dism.discoverItems(revision);
      
      output(importParameter.getStatusOutputStream(), "Deploy XMOM objects");
      deploySharedLibs(applicationXml, revision);
      deployXmomEntries(applicationXml, revision, regenerateCode, importParameter.isVerbose(), importParameter.getStatusOutputStream(), "Import Application", importParameter.getOdsNameSettings());

      output(importParameter.getStatusOutputStream(), "Add trigger and filters");
      deployTriggerAndFilter(applicationXml, revision, true, importParameter.isVerbose(), importParameter.getStatusOutputStream());
      
      if (parentRuntimeContexts != null) {
        //falls es parentruntimecontexte gibt, wieder hinzufügen
        for (RuntimeDependencyContext parentRC : parentRuntimeContexts) {
          List<RuntimeDependencyContext> requirements = new ArrayList<RuntimeDependencyContext>(rcdMgmt.getRequirements(parentRC));
          if (importParameter.isVerbose()) {
            output(importParameter.getStatusOutputStream(), "Adding " + app + " as previously removed runtime context dependency to parent " + parentRC);
          }
          requirements.add(app);
          try {
            rcdMgmt.modifyDependencies(parentRC, requirements, importParameter.getUser(), true, false);
          } catch (PersistenceLayerException e) {
            logger.warn("Could not add dependency " + app + " to " + parentRC, e);
          } catch (XFMG_CouldNotModifyRuntimeContextDependenciesException e) {
            logger.warn("Could not add dependency " + app + " to " + parentRC, e);
          }
        }
      }
      
      if (!importParameter.excludeCapacities()) {
        logger.debug("Import capacities");
        importCapacities(applicationXml.getCapacities(), importParameter.isVerbose(), importParameter.getStatusOutputStream());
      }
      if (!importParameter.excludeXynaProperties()) {
        logger.debug("Import xynaproperties");
        importXynaProperties(applicationXml.getXynaProperties(), importParameter.isVerbose(), importParameter.getStatusOutputStream());
      }
      
      //als letztes Lizenzen setzen
      ThirdPartyHandling.copyThirdPartiesDir(applicationDir, importParameter.getStatusOutputStream());

    } catch (Throwable e) {
      logger.debug("Exception during application import", e);
      if (revision != null) {
        RemoveApplicationParameters params = new RemoveApplicationParameters();
        params.setForce(true);
        params.setExtraForce(true);
        params.setGlobal(false);
        params.setStopIfRunning(true);
        output(importParameter.getStatusOutputStream(), "Removing application after exception during import");
        try {
          removeApplicationVersion(new ApplicationName(applicationName, versionName), params, importParameter.isVerbose(), importParameter.getStatusOutputStream(),
                                   new EmptyRepositoryEvent(), true);
        } catch (XFMG_CouldNotRemoveApplication e1) {
          output(importParameter.getStatusOutputStream(), "Failed to remove application.");
          throw (XFMG_CouldNotImportApplication) new XFMG_CouldNotImportApplication(importParameter.getFileName()).initCauses(new Throwable[] {e, e1});
        } catch (RuntimeException e1) {
          output(importParameter.getStatusOutputStream(), "Failed to remove application.");
          throw (XFMG_CouldNotImportApplication) new XFMG_CouldNotImportApplication(importParameter.getFileName()).initCauses(new Throwable[] {e, e1});
        }
      }
      if (e instanceof XFMG_CouldNotImportApplication) {
        throw (XFMG_CouldNotImportApplication) e;
      } else {
        throw new XFMG_CouldNotImportApplication(importParameter.getFileName(), e);
      }
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Can't close connection.", e);
      }
    }

    try {
      transferFromODSTypeToODSType(ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY, ApplicationEntryStorable.TABLE_NAME,
                                   ApplicationEntryStorable.class);
      transferFromODSTypeToODSType(ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY, ApplicationStorable.TABLE_NAME,
                                   ApplicationStorable.class);

    } catch (Exception e) {      
      throw new XFMG_CouldNotImportApplication(importParameter.getFileName(), e);
    }

    //Multi-User-Event für RuntimeContext Änderung
    Publisher publisher = new Publisher(importParameter.getUser());
    if (parentRuntimeContexts == null) {
      //neue application
      publisher.publishRuntimeContextCreate(app);
    } else {
      //überschreiben existierender application
      RuntimeContextDependencyManagement rcdm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
      List<RuntimeContext> runtimeContextsToPublishAsXMOMUpdate = rcdm.getParentRuntimeContextsSorted(app);
      if (runtimeContextsToPublishAsXMOMUpdate != null) {
        for (RuntimeContext rc : runtimeContextsToPublishAsXMOMUpdate) {
          publisher.publishXMOMUpdate(rc.getGUIRepresentation());
        }
      }
    }

    //Basis-Applications in cache eintragen
    BasicApplicationName basicApp = BasicApplicationName.valueOfOrNull(applicationName);
    if (basicApp != null) {
      basicApplications.put(basicApp, app);
    }

    if (importParameter.isClusterwide() && currentClusterState == ClusterState.CONNECTED) {
      output(importParameter.getStatusOutputStream(), "Start import on other cluster nodes.");
      final FileInputStream fis;
      try {
        fis = new FileInputStream(new File(importParameter.getFileName()));
      } catch (FileNotFoundException e) {
        throw new XFMG_CouldNotImportApplication(importParameter.getFileName(), e);
      }
      logger.debug("Import application on other nodes");
      try {
        startRequestOnOtherNodes(new ApplicationRemoteRunnable() {

          @Override
          public String run(ApplicationRemoteInterface remoteInterface) throws XynaException, RemoteException {
            return remoteInterface.importApplicationRemotely(importParameter.isForce(), importParameter.excludeXynaProperties(), importParameter.excludeCapacities(), importParameter.importXynaPropertiesOnly(),
                                                             importParameter.importCapacitiesOnly(), false, importParameter.getUser(), new SimpleRemoteInputStream(fis));
          }
        });
      } catch (Throwable e) {
        throw new XFMG_CouldNotImportApplication(importParameter.getFileName(), new RemoteException("Import remote application", e) );
      } finally {
        try {
          fis.close();
        } catch (IOException e) {
          logger.warn("Can't close file.", e);
        }
      }
    }
    try {
      return getApplicationInformation(application, true);
    } catch (PersistenceLayerException e) {
      throw new XFMG_CouldNotImportApplication(importParameter.getFileName(), e);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new XFMG_CouldNotImportApplication(importParameter.getFileName(), e);
    }
  }


  private File unzipApplication(String fileName, Long revision) throws XFMG_CouldNotImportApplication {
    File applicationDir = new File(RevisionManagement.getPathForRevision(PathType.ROOT, revision));
    ZipInputStream zis = null;
    try {
      zis = new ZipInputStream(new FileInputStream(new File(fileName)));
      RevisionManagement.createNewRevisionDirectory(revision);
      File[] files = FileUtils.saveZipToDir(zis, applicationDir);

      //application.xml löschen, damit es bei erneutem Export nicht stört
      for (File file : files) {
        if (file.getName().equals(XML_APPLICATION_FILENAME)) {
          FileUtils.deleteFileWithRetries(file);
          break;
        }
      }
    } catch (Exception e) {
      throw new XFMG_CouldNotImportApplication(fileName, e);
    } finally {
      try {
        if (zis != null) {
          zis.close();
        }
      } catch (IOException e) {
        logger.warn("Could not close file stream.", e);
      }
    }
    return applicationDir;
  }

  private void modifyRuntimeContextDependencies(String user, RuntimeDependencyContext rc, boolean verbose, PrintStream statusOutputStream,
                                                RuntimeContextDependencyManagement rcdMgmt, Set<RuntimeDependencyContext> parentRuntimeContexts, boolean publishChanges) {
    //sortieren: erst von appdefs entfernen, danach von zugehörigen workspaces, ansonsten gibts fehler
    List<RuntimeDependencyContext> sorted = new ArrayList<RuntimeDependencyContext>(parentRuntimeContexts);
    Collections.sort(sorted, new Comparator<RuntimeDependencyContext>() {

      public int compare(RuntimeDependencyContext o1, RuntimeDependencyContext o2) {
        if (o1 instanceof ApplicationDefinition) {
          if (o2 instanceof ApplicationDefinition) {
            return 0;
          }
          return -1;
        }
        if (o2 instanceof ApplicationDefinition) {
          return 1;
        }
        return 0;
      }
      
    });
    for (RuntimeDependencyContext parentRC : sorted) {
      if (verbose) {
        output(statusOutputStream, "Removing " + rc + " as runtime context dependency from parent " + parentRC);
      }
      List<RuntimeDependencyContext> requirements = new ArrayList<RuntimeDependencyContext>(rcdMgmt.getRequirements(parentRC));
      requirements.remove(rc);
      try {
        rcdMgmt.modifyDependencies(parentRC, requirements, user, true, publishChanges);
      } catch (PersistenceLayerException e) {
        throw new RuntimeException("Could not remove dependency " + rc + " from " + parentRC, e);
      } catch (XFMG_CouldNotModifyRuntimeContextDependenciesException e) {
        throw new RuntimeException("Could not remove dependency " + rc + " from " + parentRC, e);
      }
    }
  }


  public ApplicationXmlEntry parseApplicationXml(File source) {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    ApplicationXmlHandler handler = new ApplicationXmlHandler();
    try {
      SAXParser saxParser = factory.newSAXParser();
      saxParser.parse(source, handler);
    } catch (Exception e) {
      logger.warn("Could not parse application xmls", e);
    }
    return handler.getApplicationXmlEntry();
  }


  /**
   * ApplicationStorable und ApplicationEntryStorables anlegen und ggf. Capacities und XynaProperties importieren
   * @param applicationXml
   * @param parentRevision
   * @param includeXynaProperties
   * @param includeCapacities
   * @throws XynaException
   */
  public void saveApplication(ApplicationXmlEntry applicationXml, Long parentRevision, boolean includeXynaProperties,
                              boolean includeCapacities) throws XynaException {
    String applicationName = applicationXml.getApplicationName();
    String versionName = applicationXml.getVersionName();
    if (!versionName.startsWith(WORKINGSET_VERSION_NAME)) {
      versionName = "workingset of version " + versionName;
    }

    ApplicationDefinitionKey key = new ApplicationDefinitionKey(applicationName, parentRevision);
    if (applicationDefinitionVersionNameCache.containsKey(key)) {
      throw new XFMG_DuplicateApplicationName(applicationName);
    }

    ODSConnection con = ods.openConnection();
    try {
      // Application anlegen
      ApplicationStorable app = new ApplicationStorable(applicationName, versionName, applicationXml.getComment(), parentRevision);
      con.persistObject(app);
      applicationDefinitionVersionNameCache.put(new ApplicationDefinitionKey(app), new WorkingSet(versionName));

      //ApplicationEntries anlegen
      saveApplicationEntries(con, applicationXml, parentRevision);

      //Capacities und XynaProperties
      if (includeCapacities) {
        logger.debug("Import capacities");
        importCapacities(applicationXml.getCapacities(), false, null);
      }
      if (includeXynaProperties) {
        logger.debug("Import Xynaproperties");
        importXynaProperties(applicationXml.getXynaProperties(), false, null);
      }
    } catch (Throwable e) {
      RemoveApplicationParameters params = new RemoveApplicationParameters();
      params.setForce(true);
      params.setExtraForce(true);
      params.setGlobal(false);
      params.setStopIfRunning(true);
      params.setParentWorkspace(revisionManagement.getWorkspace(parentRevision));
      if (logger.isDebugEnabled()) {
        logger.debug("Removing application after exception during checkout");
      }
      try {
        removeApplicationVersion(new ApplicationName(applicationName, versionName), params, false, null, new EmptyRepositoryEvent(), true);
      } catch (Exception e1) {
        logger.warn("Rollback failed. Application could not be deleted.", e1);
      }
      throw new XFMG_CouldNotBuildWorkingSet(applicationName, versionName, e);
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Can't close connection.", e);
      }
    }

    transferFromODSTypeToODSType(ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY, ApplicationEntryStorable.TABLE_NAME,
                                 ApplicationEntryStorable.class);
    transferFromODSTypeToODSType(ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY, ApplicationStorable.TABLE_NAME,
                                 ApplicationStorable.class);
  }


  /**
   * Alle Objekte der Application deployen und Order Input Sources anlegen.
   * @param applicationXml
   * @param parentRevision
   * @throws XynaException
   */
  public void deployApplication(ApplicationXmlEntry applicationXml, Long parentRevision, String comment) throws XynaException {
    deploySharedLibs(applicationXml, parentRevision);
    createInputSources(applicationXml, parentRevision);
    deployXmomEntries(applicationXml, parentRevision, true, false, null, comment, XMOMODSNameImportSetting.ALLOW_REGENERATION);
    deployTriggerAndFilter(applicationXml, parentRevision, false, false, null);
  }


  private void saveApplicationEntries(ODSConnection con, ApplicationXmlEntry applicationXml, Long parentRevision) throws XynaException {
    Set<ApplicationEntryStorable> appEntriesToStore = new HashSet<ApplicationEntryStorable>();

    String applicationName = applicationXml.getApplicationName();
    String versionName = applicationXml.getVersionName();

    for (XMOMXmlEntry entry : applicationXml.getXmomEntries()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Create application entry: " + entry.getFqName() + " with type " + entry.getType());
      }
      if (!entry.isImplicitDependency()) {
        ApplicationEntryStorable e =
            ApplicationEntryStorable.toStore(applicationName, versionName, parentRevision, entry.getFqName(),
                                         ApplicationEntryType.valueOf(entry.type));
        appEntriesToStore.add(e);
      }
    }
    for (FilterInstanceXmlEntry entry : applicationXml.getFilterInstances()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Create application entry: Filterinstance " + entry.getName());
      }
      if (!entry.isImplicitDependency()) {
        ApplicationEntryStorable e =
            ApplicationEntryStorable.toStore(applicationName, versionName, parentRevision, entry.getName(), ApplicationEntryType.FILTERINSTANCE);
        appEntriesToStore.add(e);
      }
    }
    for (FilterXmlEntry entry : applicationXml.getFilters()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Create application entry: Filter " + entry.getName());
      }
      if (!entry.isImplicitDependency()) {
        ApplicationEntryStorable e =
            ApplicationEntryStorable.toStore(applicationName, versionName, parentRevision, entry.getName(), ApplicationEntryType.FILTER);
        appEntriesToStore.add(e);
      }
    }
    for (TriggerInstanceXmlEntry entry : applicationXml.getTriggerInstances()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Create application entry: Triggerinstance " + entry.getName());
      }
      if (!entry.isImplicitDependency()) {
        ApplicationEntryStorable e =
            ApplicationEntryStorable.toStore(applicationName, versionName, parentRevision, entry.getName(),
                                         ApplicationEntryType.TRIGGERINSTANCE);
        appEntriesToStore.add(e);
      }
    }

    // triggers
    for (TriggerXmlEntry entry : applicationXml.getTriggers()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Create application entry: Trigger " + entry.getName());
      }
      if (!entry.isImplicitDependency()) {
        ApplicationEntryStorable e =
            ApplicationEntryStorable.toStore(applicationName, versionName, parentRevision, entry.getName(), ApplicationEntryType.TRIGGER);
        appEntriesToStore.add(e);
      }
    }

    // shared libs
    for (SharedLibXmlEntry entry : applicationXml.getSharedLibs()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Create application entry: SharedLib " + entry.getSharedLibName());
      }
      if (!entry.isImplicitDependency()) {
        ApplicationEntryStorable e =
            ApplicationEntryStorable.toStore(applicationName, versionName, parentRevision, entry.getSharedLibName(),
                                         ApplicationEntryType.SHAREDLIB);
        appEntriesToStore.add(e);
      }
    }

    // capacities
    for (CapacityXmlEntry entry : applicationXml.getCapacities()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Create application entry: Capacity " + entry.getName() + " with cardinality " + entry.getCardinality());
      }
      if (!entry.isImplicitDependency()) {
        ApplicationEntryStorable e =
            ApplicationEntryStorable.toStore(applicationName, versionName, parentRevision, entry.getName(), ApplicationEntryType.CAPACITY);
        appEntriesToStore.add(e);
      }
    }

    // properties
    for (XynaPropertyXmlEntry entry : applicationXml.getXynaProperties()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Create application entry: Xynaproperty " + entry.getName() + " with content " + entry.getValue());
      }
      if (!entry.isImplicitDependency()) {
        ApplicationEntryStorable e =
            ApplicationEntryStorable.toStore(applicationName, versionName, parentRevision, entry.getName(), ApplicationEntryType.XYNAPROPERTY);
        appEntriesToStore.add(e);
      }
    }

    // ordertype config
    for (OrdertypeXmlEntry entry : applicationXml.getOrdertypes()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Create application entry: Ordertype " + entry.getDestinationKey());
      }
      if (!entry.isImplicitDependency()) {
        ApplicationEntryStorable e =
            ApplicationEntryStorable.toStore(applicationName, versionName, parentRevision, entry.getDestinationKey(),
                                         ApplicationEntryType.ORDERTYPE);
        appEntriesToStore.add(e);
      }
    }

    // OrderInputSources
    for (OrderInputSourceXmlEntry entry : applicationXml.getOrderInputSources()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Create application entry: OrderInputSource " + entry.getName());
      }
      if (!entry.isImplicitDependency()) {
        ApplicationEntryStorable e =
            ApplicationEntryStorable.toStore(applicationName, versionName, parentRevision, entry.getName(),
                                         ApplicationEntryType.ORDERINPUTSOURCE);
        appEntriesToStore.add(e);
      }
    }

    con.persistCollection(appEntriesToStore);

    //hier schon committen, damit das removeapplication im fehlerfall danach die einträge finden kann
    //ausserdem kann es sein, dass beim deployment crons gestartet werden, die den appstate auf running setzen. 
    con.commit();
  }


  private void createInputSources(ApplicationXmlEntry applicationXml, Long revision) throws XynaException {
    OrderInputSourceManagement oism =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderInputSourceManagement();

    RuntimeContext rc = revisionManagement.getRuntimeContext(revision);
    String applicationName = (rc instanceof Application) ? rc.getName() : null;
    String versionName = (rc instanceof Application) ? ((Application) rc).getVersionName() : null;
    String workspaceName = (rc instanceof Workspace) ? rc.getName() : null;

    // order input sources
    for (OrderInputSourceXmlEntry entry : applicationXml.getOrderInputSources()) {
      OrderInputSourceStorable inputSource =
          new OrderInputSourceStorable(entry.getName(), entry.getType(), entry.getOrderType(), applicationName, versionName, workspaceName,
                                       entry.getDocumentation(), entry.getParameter());
      oism.createOrderInputSource(inputSource);
    }
  }


  private void deploySharedLibs(ApplicationXmlEntry applicationXml, Long revision) throws XynaException {
    // shared libs
    for (SharedLibXmlEntry entry : applicationXml.getSharedLibs()) {
      if (revisionManagement.isWorkspaceRevision(revision)) {
        //sharedLibs in Workspaces deployen, damit die jars aus dem saved ins deployed-Verzeichnis kopiert werden
        try {
          SharedLibDeploymentAlgorithm.deploySharedLib(entry.getSharedLibName(), revision, new EmptyRepositoryEvent());
        } catch (OrderEntryInterfacesCouldNotBeClosedException ex) {
          logger.warn("Failed to deploy sharedlib " + entry.getSharedLibName(), ex);
        }
      } else if (revisionManagement.isApplicationRevision(revision)) {
        WorkflowDatabase.reloadSharedLib(entry.getSharedLibName(), revision);
      }
    }
  }


  private void deployXmomEntries(ApplicationXmlEntry applicationXml, Long revision, boolean regenerateCode, boolean verbose,
                                 PrintStream statusOutputStream, String comment, XMOMODSNameImportSetting allowStorableNameGeneration) throws XynaException {
    RuntimeContext runtimeContext = revisionManagement.getRuntimeContext(revision);

    PriorityManagement priorityManagement =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getPriorityManagement();
    CapacityMappingDatabase capacityMappingDatabase =
        XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getCapacityMappingDatabase();
    MonitoringDispatcher monitoringDispatcher =
        XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getMonitoringDispatcher();
    XMOMPersistenceManagement xmomPersistenceManagment =
        XynaFactory.getInstance().getXynaNetworkWarehouse().getXMOMPersistence().getXMOMPersistenceManagement();
    ParameterInheritanceManagement parameterInheritanceMgmt =
        XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getParameterInheritanceManagement();

    List<ApplicationEntryStorable> xmomEntries = new ArrayList<ApplicationEntryStorable>();

    String applicationName = applicationXml.getApplicationName();
    String versionName = applicationXml.getVersionName();

    Long parentRevision = revisionManagement.isWorkspaceRevision(revision) ? revision : null;

    for (XMOMXmlEntry entry : applicationXml.getXmomEntries()) {
      ApplicationEntryStorable e =
          ApplicationEntryStorable.create(applicationName, versionName, parentRevision, entry.getFqName(),
                                       ApplicationEntryType.valueOf(entry.type));
      xmomEntries.add(e);
    }

    Map<String, OrdertypeParameter> orderTypeParams = new HashMap<String, OrdertypeParameter>();
    // ordertype config
    for (OrdertypeXmlEntry entry : applicationXml.getOrdertypes()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Create application entry: Ordertype " + entry.getDestinationKey());
      }

      OrdertypeParameter otp = new OrdertypeParameter();
      orderTypeParams.put(entry.getDestinationKey(), otp);
      otp.setOrdertypeName(entry.getDestinationKey());
      otp.setRuntimeContext(runtimeContext);
      if (entry.getPlanning() != null) {
        otp.setCustomPlanningDestinationValue(new DestinationValueParameter(new FractalWorkflowDestination(entry.getPlanning())));
      }
      if (entry.getExecution() != null) {
        otp.setCustomExecutionDestinationValue(new DestinationValueParameter(new FractalWorkflowDestination(entry.getExecution())));
      }
      if (entry.getCleanup() != null) {
        otp.setCustomCleanupDestinationValue(new DestinationValueParameter(new FractalWorkflowDestination(entry.getCleanup())));
      }

      DestinationKey dk = new DestinationKey(entry.getDestinationKey(), runtimeContext);
      if (entry.hasOrdercontextMapping()) {
        XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderContextConfiguration().configureDestinationKey(dk, true);
      }
      if (verbose) {
        output(statusOutputStream, "Import ordertype <" + entry.getDestinationKey() + ">");
      }
    }

    // capacity mapping config
    for (CapacityRequirementXmlEntry entry : applicationXml.getCapacityRequirements()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Create application entry: Capacity requirement of " + entry.getCapacityName() + " for ordertype "
            + entry.getOrdertype());
      }
      OrdertypeParameter otp = orderTypeParams.get(entry.getOrdertype());
      if (otp == null) {
        // TODO das ist eigtl ein fehler im xml, der ordertype muss da auch mit drin gestanden haben!
        try {
          capacityMappingDatabase.addCapacity(new DestinationKey(entry.getOrdertype(), runtimeContext),
                                              new Capacity(entry.getCapacityName(), entry.getCardinality()));
          if (verbose) {
            output(statusOutputStream,
                   "Import capacity mapping for ordertype <" + entry.getOrdertype() + "> and capacity <" + entry.getCapacityName() + ">");
          }
        } catch (XFMG_InvalidCapacityCardinality e) {
          logger.warn("Failed to set capacity mapping for ordertype " + entry.getOrdertype() + " and capacity " + entry.getCapacityName(),
                      e);
        }
      } else {
        Set<Capacity> caps = otp.getRequiredCapacities();
        if (caps == null) {
          caps = new HashSet<Capacity>();
          otp.setRequiredCapacities(caps);
        }
        caps.add(new Capacity(entry.getCapacityName(), entry.getCardinality()));
      }
    }

    // priority config
    for (PriorityXmlEntry entry : applicationXml.getPriorities()) {
      if (logger.isDebugEnabled()) {
        logger
            .debug("Create application entry: Priority entry for ordertype" + entry.getOrdertype() + " with value " + entry.getPriority());
      }
      OrdertypeParameter otp = orderTypeParams.get(entry.getOrdertype());
      if (otp == null) {
        // TODO das ist eigtl ein fehler im xml, der ordertype muss da auch mit drin gestanden haben!
        try {
          priorityManagement.setPriority(entry.getOrdertype(), entry.getPriority(), revision);
          if (verbose) {
            output(statusOutputStream, "Import priority for ordertype <" + entry.getOrdertype() + ">");
          }
        } catch (XFMG_InvalidXynaOrderPriority e) {
          logger.warn("Failed to set priority of " + entry.getOrdertype(), e);
        }
      } else {
        otp.setCustomPriority(entry.getPriority());
      }
    }

    // monitoring level config
    for (MonitoringLevelXmlEntry entry : applicationXml.getMonitoringLevels()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Create application entry: Monitoringlevel entry for ordertype" + entry.getOrdertype() + " with value "
            + entry.getMonitoringLevel());
      }
      OrdertypeParameter otp = orderTypeParams.get(entry.getOrdertype());
      if (otp == null) {
        // TODO das ist eigtl ein fehler im xml, der ordertype muss da auch mit drin gestanden haben!
        try {
          monitoringDispatcher.setMonitoringLevel(new DestinationKey(entry.getOrdertype(), runtimeContext), entry.getMonitoringLevel());
          if (verbose) {
            output(statusOutputStream, "Import monitoring level for ordertype <" + entry.getOrdertype() + ">");
          }
        } catch (XynaException e) {
          logger.warn("Failed to set monitoring level of " + entry.getOrdertype(), e);
        }
      } else {
        otp.setCustomMonitoringLevel(entry.getMonitoringLevel());
      }
    }

    // parameter inheritance config
    for (InheritanceRuleXmlEntry entry : applicationXml.getParameterInheritanceRules()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Create application entry: Parameter inheritance rule for ordertype " + entry.getOrderType() + "and childFilter "
            + entry.getChildFilter() + " with value " + entry.getValue());
      }
      InheritanceRule inheritanceRule =
          entry.getParameterType().createInheritanceRuleBuilder(entry.getValue()).childFilter(entry.getChildFilter())
              .precedence(entry.getPrecedence()).build();

      OrdertypeParameter otp = orderTypeParams.get(entry.getOrderType());
      if (otp == null) {
        // TODO das ist eigtl ein fehler im xml, der ordertype muss da auch mit drin gestanden haben!
        try {
          parameterInheritanceMgmt.addInheritanceRule(entry.getParameterType(), new DestinationKey(entry.getOrderType(), runtimeContext),
                                                      inheritanceRule);
          if (verbose) {
            output(statusOutputStream, "Import inheritance rule for ordertype <" + entry.getOrderType() + ">");
          }
        } catch (XynaException e) {
          logger.warn("Failed to add inheritance rule of " + entry.getOrderType(), e);
        }
      } else {
        Map<ParameterType, List<InheritanceRule>> ruleMap = otp.getParameterInheritanceRules();
        if (ruleMap == null) {
          ruleMap = new HashMap<ParameterType, List<InheritanceRule>>();
        }
        List<InheritanceRule> ruleList = ruleMap.get(entry.getParameterType());
        if (ruleList == null) {
          ruleList = new ArrayList<InheritanceRule>();
          ruleMap.put(entry.getParameterType(), ruleList);
        }
        ruleList.add(inheritanceRule);
        otp.setParameterInheritanceRules(ruleMap);
      }
    }

    if (allowStorableNameGeneration == XMOMODSNameImportSetting.EXCLUDE) {
      String msg = "Ods names will not be imported, new or existing names will be assigned during deployment automatically.";
      regenerateCode = true;
      output(statusOutputStream, msg);
    } else {
      GenerationBaseCache parseAdditionalCache = new GenerationBaseCache();
      for (XMOMStorableXmlEntry entry : applicationXml.getXmomStorableEntries()) {
        Collection<XMOMStorableXmlEntry> entriesToRegister;
        if (entry.getFqPath() == null) {
          entriesToRegister = new ArrayList<>();
          Collection<DiscoveryResult> discovered = XMOMODSMappingUtils.discoverFqPathsForPath(entry.getXmlName(), revision, entry.getPath(), parseAdditionalCache);
          for (DiscoveryResult discovery : discovered) {
            XMOMStorableXmlEntry conversion = new XMOMStorableXmlEntry(discovery.getType(), discovery.getPath(), entry.getOdsName(), discovery.getFqPath(), null);
            entriesToRegister.add(conversion);
          }
        } else {
          entriesToRegister = Collections.singleton(entry);
        }
        
        try {
          for (XMOMStorableXmlEntry entryToRegister : entriesToRegister) {
            ODSRegistrationParameter odsRP = new ODSRegistrationParameter(entryToRegister.getXmlName(),
                                                                          revision,
                                                                          entryToRegister.getFqPath(),
                                                                          entryToRegister.getOdsName(),
                                                                          entryToRegister.getColName(),
                                                                          true);
            xmomPersistenceManagment.setODSName(odsRP);
          }
        } catch (XNWH_ODSNameMustBeUniqueException e) {
          if (allowStorableNameGeneration == XMOMODSNameImportSetting.ALLOW_REGENERATION) {
            // ok, dann kann man also die konfig so nicht importieren -> fehler loggen, dann kann der benutzer später den odsnamen manuell umkonfigurieren
            boolean tableName = entry.colName == null;
            String msg = "ODS name not unique. Failed to import configured ODS name '" + 
                            entry.getOdsName() + 
                            (tableName ? "" : "." + entry.getColName()) +
                            "' for datatype '" +
                            entry.getXmlName() + 
                            "', path='" + 
                            entry.getPath() + 
                            "'. A new ods name will be assigned during deployment automatically.";
            regenerateCode = true;
            output(statusOutputStream, msg);
            logger.warn(msg, e);
          } else {
            throw e;
          }
        }
        // restliche fehler sollten nicht vorkommen, oder sind exceptions die zum abbruch des imports führen sollen.
      }
    }

    // Neue Revision deployen
    output(statusOutputStream, "Deploy objects of the application.");

    redeploy(revision, xmomEntries, regenerateCode ? DeploymentMode.codeChanged : DeploymentMode.reloadWithXMOMDatabaseUpdate, false,
             statusOutputStream, comment);
    if (!regenerateCode) { //else: deploymenthandler speichert
      //damit beim Factory-Neustart die Objekte wieder deployed werden, müssen sie in WorkflowDatabase persistiert werden,
      //wenn sie hier mit DeploymentMode = reload deployed werden
      WorkflowDatabase wdb = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getWorkflowDatabase();
      wdb.persistDeployedObjects();
    }

    OrdertypeManagement orderTypeManagement =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderTypeManagement();
    //nach dem deployment ausführen, damit die defaultdestinations bereits existieren
    try {
      orderTypeManagement.createOrUpdateOrdertypes(orderTypeParams.values(), false); //FIXME oben wurde bereits manches geupdated
    } catch (Exception e) {
      logger.warn("Failed to configure ordertypes.", e);
    }
  }


  private void deployTriggerAndFilter(ApplicationXmlEntry applicationXml, Long revision, boolean jarsFromDeployed, boolean verbose,
                                      PrintStream statusOutputStream) throws XPRC_ExclusiveDeploymentInProgress,
      XACT_IncompatibleTriggerImplException, XACT_TriggerImplClassNotFoundException, Ex_FileAccessException, XFMG_SHARED_LIB_NOT_FOUND,
      XACT_LibOfTriggerImplNotFoundException, PersistenceLayerException, XPRC_XmlParsingException,
      XPRC_InvalidXmlMissingRequiredElementException, XACT_JarFileUnzipProblem, XACT_ErrorDuringTriggerAdditionRollback,
      XACT_AdditionalDependencyDeploymentException, XACT_DuplicateTriggerDefinitionException, XACT_FilterImplClassNotFoundException,
      XACT_TriggerNotFound, XACT_LibOfFilterImplNotFoundException, XACT_FilterNotFound {
    XynaActivationTrigger xynaActivationTrigger = XynaFactory.getInstance().getActivation().getActivationTrigger();
    for (TriggerXmlEntry entry : applicationXml.getTriggers()) {
      if (verbose) {
        output(statusOutputStream, "Add trigger " + entry.getName());
      }
      if (logger.isDebugEnabled()) {
        logger.debug("Add trigger " + entry.getName());
      }
      xynaActivationTrigger.addTrigger(entry.getName(),
                                       unwrapToFileArray(entry.getJarFiles(), PathType.TRIGGER, revision, jarsFromDeployed),
                                       entry.getFqTriggerClassname(), unwrapToStringArray(entry.getSharedLibs()), revision,
                                       new EmptyRepositoryEvent());
    }
    for (FilterXmlEntry entry : applicationXml.getFilters()) {
      if (verbose) {
        output(statusOutputStream, "Add filter " + entry.getName());
      }
      if (logger.isDebugEnabled()) {
        logger.debug("Add filter " + entry.getName());
      }
      xynaActivationTrigger.addFilter(entry.getName(), unwrapToFileArray(entry.getJarFiles(), PathType.FILTER, revision, jarsFromDeployed),
                                      entry.getFqFilterClassname(), entry.getTriggerName(), unwrapToStringArray(entry.getSharedLibs()),
                                      null, revision, new EmptyRepositoryEvent());
    }
    for (TriggerInstanceXmlEntry entry : applicationXml.getTriggerInstances()) {
      if (verbose) {
        output(statusOutputStream, "Deploy trigger instance " + entry.getName() + " disabled.");
      }
      if (logger.isDebugEnabled()) {
        logger.debug("Deploy trigger instance " + entry.getName());
      }
      xynaActivationTrigger
          .deployTriggerDisabled(entry.getTriggerName(), entry.getName(),
                                 TriggerInstanceStorable.getStartParameterArray(entry.getStartParameter()), null, revision);
      if (entry.getMaxEvents() != null && entry.getRejectRequestsAfterMaxReceives() != null) {
        xynaActivationTrigger.createConfigureTriggerMaxEventsSetting(entry.getName(), entry.getMaxEvents(),
                                                                     entry.getRejectRequestsAfterMaxReceives(), revision);
      }
    }
    for (FilterInstanceXmlEntry entry : applicationXml.getFilterInstances()) {
      if (verbose) {
        output(statusOutputStream, "Deploy filter instance " + entry.getName() + " disabled.");
      }
      if (logger.isDebugEnabled()) {
        logger.debug("Deploy filter instance " + entry.getName());
      }
      DeployFilterParameter dfp = new DeployFilterParameter.Builder().
          filterName(entry.getFilterName()).instanceName(entry.getName()).
          triggerInstanceName(entry.getTriggerInstanceName()).description(entry.getDescription()).
          optional(false).configuration(entry.getConfigurationParameter()).
          revision(revision).build();
      xynaActivationTrigger.deployFilterDisabled(dfp);
    }
  }


  private boolean mustRemoveOldApplication(ODSConnection con, String applicationName, String versionName, boolean force,
                                           PrintStream statusOutputStream) throws XFMG_DuplicateVersionForApplicationName,
      PersistenceLayerException {
    Long revision = null;
    boolean oldAppExists = false;

    try {
      revision = revisionManagement.getRevision(new Application(applicationName, versionName));
      oldAppExists = true;
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      //ok, keine alte xmomversion da
    }

    ApplicationStorable app = queryRuntimeApplicationStorable(applicationName, versionName, con);
    List<? extends ApplicationEntryStorable> storedAppEntries = queryAllRuntimeApplicationStorables(applicationName, versionName, con);
    if (!oldAppExists && (app != null || storedAppEntries.size() > 0)) {
      oldAppExists = true;
      logger.warn("Data is inconsistent. XMOMVersion knows application " + applicationName + " (" + versionName
          + ") but application management not.");
    }

    if (oldAppExists) {
      output(statusOutputStream, "Application " + applicationName + " " + versionName + " already exists");
      if (force) {
        //bei force immer alte Application entfernen
        return true;
      } else {
        if (revision != null) { //es ist eine alte revision vorhanden
          //zugehöriges revision-Verzeichnis suchen
          File revFolder = new File(RevisionManagement.getPathForRevision(PathType.ROOT, revision));
          if (!revFolder.exists()) {
            //kein revisions-Verzeichnis da -> inkonsistenter Zustand -> Application-Reste sollen entfernt werden
            logger.warn("Data is inconsistent. XMOMVersion knows application " + applicationName + " (" + versionName
                + ") but revision folder is missing.");
            return true;
          } else {
            throw new XFMG_DuplicateVersionForApplicationName(applicationName, versionName);
          }
        } else {
          //alte revision ist nicht bekannt -> nach überflüssigen revisions-Verzeichnissen suchen
          String path = ".." + Constants.fileSeparator + Constants.REVISION_PATH;
          File revisions = new File(path);
          if (revisions.listFiles().length - 1 > con.loadCollection(XMOMVersionStorable.class).size()) {
            //es gibt mindestens einen revisions-Ordner (ohne rev_workingset) mehr als xmomversions
            //-> Application nicht löschen, damit diese Informationen nicht verloren gehen
            throw new XFMG_DuplicateVersionForApplicationName(applicationName, versionName);
          }
          for (File file : revisions.listFiles()) {
            String revisionString = file.getName().replace(Constants.PREFIX_REVISION, "");
            if (!revisionString.equals(Constants.SUFFIX_REVISION_WORKINGSET)) {
              try {
                revisionManagement.getApplication(Long.valueOf(revisionString));
              } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
                //es gibt einen revisions-Ordner ohne xmomversion-Eintrag
                //-> Application nicht löschen, damit diese Informationen nicht verloren gehen
                throw new XFMG_DuplicateVersionForApplicationName(applicationName, versionName);
              }
            }
          }
          //es gibt keinen überflüssigen revisions-Ordner -> Application-Reste können entfernt werden
          //neue xmomversion anlegen, damit remove funktioniert
          logger.warn("Data is inconsistent. XMOMVersion knows application " + applicationName + " (" + versionName
              + ") but revision folder is missing.");
          revisionManagement.buildNewRevisionForNewVersion(applicationName, versionName);
          return true;
        }
      }
    } else {
      return false;
    }
  }


  private String repairPath(String path, String lastPathEntry) {
    // 1. suche nach vorkommen von <lastPathEntry>
    // 2. lösche alles vor <lastPathEntry> inkl. <lastPathEntry>/
    // Bsp. ./filter/DHCPFilter/DHCPFilter.jar --> DHCPFilter/DHCPFilter.jar
    int index = path.indexOf(lastPathEntry);
    if (index < 0) {
      logger.debug("Can't repair path of <" + path + "> with lastPathEntry = " + lastPathEntry);
      return path;
    }
    if (path.length() < lastPathEntry.length()) {
      logger.error("Can't repair path of <" + path + "> with lastPathEntry = " + lastPathEntry + ". Length of path is to small.");
      return path;
    }
    return path.substring(index + lastPathEntry.length());
  }


  private String wrapArray(File[] array, String lastPathEntry) {
    StringBuilder sb = new StringBuilder();
    for (File file : array) {
      sb.append(repairPath(file.getPath(), lastPathEntry)).append(":");
    }
    return sb.toString();
  }


  private String wrapArray(String[] array) {
    StringBuilder sb = new StringBuilder();
    for (String string : array) {
      sb.append(string).append(":");
    }
    return sb.toString();
  }


  private String[] unwrapToStringArray(String string) {
    if (string != null) {
      return string.split(":");
    }
    return new String[0];
  }


  private File[] unwrapToFileArray(String string, PathType type, Long revision, boolean deployed) {
    if (string != null) {
      List<File> files = new ArrayList<File>();
      for (String splitedString : string.split(":")) {
        if (splitedString.trim().length() > 0) {
          files.add(new File(RevisionManagement.getPathForRevision(type, revision, deployed), splitedString));
        }
      }
      return files.toArray(new File[0]);
    }
    return new File[0];
  }


  /**
   * wird vom GlobalApplicationManagement verwendet
   */
  public void exportApplication(String applicationName, String versionName, String fileName, boolean verbose, PrintStream statusOutputStream)
      throws XFMG_CouldNotExportApplication {
    exportApplication(applicationName, versionName, fileName, false, null, true, verbose, statusOutputStream);
  }


  public void exportApplication(String applicationName, String versionName, String fileName, boolean localBuild, String newVersion,
                                boolean local, boolean verbose, PrintStream statusOutputStream) throws XFMG_CouldNotExportApplication {
    exportApplication(applicationName, versionName, fileName, localBuild, newVersion, local, verbose, false, statusOutputStream, null);
  }
  
  
  public void exportApplication(String applicationName, String versionName, String fileName, boolean localBuild, String newVersion,
                                boolean local, boolean verbose, boolean createNewStub, PrintStream statusOutputStream, String user) throws XFMG_CouldNotExportApplication {

    Application runtimeContext = new Application(applicationName, versionName);
    Long revision;
    try {
      revision = revisionManagement.getRevision(runtimeContext);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
      output(statusOutputStream, "Application/version not found!");
      throw new XFMG_CouldNotExportApplication(applicationName, versionName);
    }

    ODSConnection con = ods.openConnection();
    try {
      ApplicationStorable application = queryRuntimeApplicationStorable(applicationName, versionName, con);

      if (application.getStateAsEnum() == ApplicationState.AUDIT_MODE) {
        Exception cause = new Exception("Could not export application in state " + application.getState());
        throw new XFMG_CouldNotExportApplication(applicationName, versionName, cause);
      }

      ApplicationXmlEntry applicationXmlEntry = new ApplicationXmlEntry(applicationName, versionName, application.getComment());

      //aktuelle Factory-Version eintragen
      applicationXmlEntry.setFactoryVersion();

      final List<ApplicationEntryStorable> appEntries = queryAllRuntimeApplicationStorables(applicationName, versionName, con);

      final boolean createStub = application.getRemoteStub() || createNewStub;
      applicationXmlEntry.getApplicationInfo().setIsRemoteStub(createStub);

      //ermittle dependencies zu appentries
      Collection<ApplicationEntryStorable> implicitDependencies;
      Collection<RuntimeDependencyContext> dependentRuntimeContexts;
      if (createStub) {
        dependentRuntimeContexts = new HashSet<RuntimeDependencyContext>();
        Set<Long> revisions = new HashSet<Long>();
        
        RevisionManagement revisionManagement =
            XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
        Application app;
        try {
          app = revisionManagement.getApplication(revision);
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          throw new RuntimeException(e);
        }
        
        implicitDependencies = findDependenciesForStub(statusOutputStream, appEntries, revision, revisions, app, verbose);
        for (Long rev : revisions) {
          dependentRuntimeContexts.add(RuntimeContextDependencyManagement.asRuntimeDependencyContext(revisionManagement.getRuntimeContext(rev)));
        }
        
        createXMLEntries(implicitDependencies, verbose, statusOutputStream, applicationXmlEntry, revision, false, false, createStub);
      } else {
        implicitDependencies = findDependencies(appEntries, revision);
        RuntimeContextDependencyManagement rcdMgmt =
            XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
        dependentRuntimeContexts = rcdMgmt.getRequirements(runtimeContext);

        //erzeuge xmlentries, wobei die impliziten abhängigkeiten entsprechend markiert werden, damit beim import dafür 
        //nicht neue app entries gespeichert werden
        createXMLEntries(appEntries, verbose, statusOutputStream, applicationXmlEntry, revision, false, false, createStub);
        createXMLEntries(implicitDependencies, verbose, statusOutputStream, applicationXmlEntry, revision, true, false, createStub);
      }

      //die RuntimeContextRequirements sind nicht in den ApplicationEntries enthalten, daher extra hinzufügen
      addRuntimeContextRequirementXMLEntries(dependentRuntimeContexts, applicationXmlEntry, verbose, statusOutputStream);

      File file =  new File(fileName);
      
      String newBranch = null;
      RepositoryAccessManagement ram =
          XynaFactory.getInstance().getXynaDevelopment().getXynaLibraryDevelopment().getRepositoryAccessManagement();
      RepositoryAccess repositoryAccess = ram.getRepositoryAccessInstance(getBranchName(applicationName, versionName));
      if (!createStub && repositoryAccess != null) {
        XMOMAccess xmomAccess = new XMOMAccess(repositoryAccess.getName(), revision, repositoryAccess);

        //neuen Branch erstellen, falls Konfigurationsänderungen vorhanden sind
        if (hasApplicationConfigurationChanged(applicationName, applicationXmlEntry, xmomAccess)) {
          if (localBuild) {
            file = new File(file.getParent(), "localBuild_" + file.getName());
          } else if (newVersion != null) {
            try {
              revision = revisionManagement.getRevision(new Application(applicationName, newVersion));
              throw new XFMG_DuplicateVersionForApplicationName(applicationName, newVersion);
            } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
              //ok, es gibt noch keine Version 'newVersion'
            }

            //VersionName auf newVersion ändern
            applicationXmlEntry.versionName = newVersion;

            //Branch erstellen
            newBranch =
                xmomAccess.createBranch(getBranchName(applicationName, newVersion), "export application '" + applicationName + "'/'"
                    + versionName + "'");
          } else {
            throw new XFMG_ApplicationConfigurationChanged(applicationName, versionName);
          }
        }
      }

      //Liefergegenstand erstellen
      String revisionDir = RevisionManagement.getPathForRevision(PathType.ROOT, revision);

      ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file));
      try {
      final Set<String> xmomEntries = new HashSet<String>();
      if (createStub) {
        for (ApplicationEntryStorable aes : implicitDependencies) {
          if (aes.getTypeAsEnum() == ApplicationEntryType.WORKFLOW || aes.getTypeAsEnum() == ApplicationEntryType.DATATYPE
              || aes.getTypeAsEnum() == ApplicationEntryType.EXCEPTION) {
            xmomEntries.add(aes.getName());
          }
        }
      }
      StubFileCreator creator = new StubFileCreator();
      FileUtils.zipDir(new File(revisionDir), zos, new File(revisionDir), null, null, new FileInputStreamCreator() {
        
        public InputStream create(File f) throws FileNotFoundException {
          if (createStub) {
            return creator.createStubForFile(f, xmomEntries);
          } else {
            return new FileInputStream(f);
          }
        }

      });

      ZipEntry ze = new ZipEntry(XML_APPLICATION_FILENAME);

      zos.putNextEntry(ze);
      Document doc = applicationXmlEntry.buildXmlDocument();
      XMLUtils.saveDomToOutputStream(zos, doc);
      
      eventHandling.triggerEvent(new ApplicationExportEvent(applicationName, versionName, zos));

      zos.flush();
      } finally {
        zos.close();
      }

      //falls eine neue Version angelegt wurde, diese importieren und TimeControlledOrders migrieren
      if (!createStub && newBranch != null) {
        importApplication(fileName, false, false, true, true, false, false, !local, false, verbose, user, statusOutputStream, true, true);
        // RepositoryAccess für neue Version anlegen
        Long newRevision = revisionManagement.getRevision(new Application(applicationName, newVersion));
        RepositoryAccess newRepositoryAccess =
            instantiateRepositoryAccess(repositoryAccess, getBranchName(applicationName, newVersion), newBranch, newRevision);

        // im neuen Branch die aktuelle Konfiguration speichern
        XMOMAccess newXmomAccess = new XMOMAccess(newRepositoryAccess.getName(), newRevision, newRepositoryAccess);
        newXmomAccess.saveAndCommitApplicationConfiguration(applicationName, applicationXmlEntry, "application definition modified.");
      }
    } catch (Exception e) {
      throw new XFMG_CouldNotExportApplication(applicationName, versionName, e);
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Could not close connection.", e);
      }
    }
  }


  private Collection<ApplicationEntryStorable> findDependenciesForStub(PrintStream statusOutputStream, List<? extends ApplicationEntryStorable> appEntries, Long revision,
                                                                       Set<Long> revisionsToKeep, Application app, boolean verbose) {
    DeploymentItemStateManagement dism =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement();
    DeploymentItemRegistry registry = dism.getRegistry(revision);
    Map<String, ApplicationEntryStorable> dependencies = new HashMap<String, ApplicationEntryStorable>();
    boolean atLeastOneWorkflow = false;
    

    RuntimeContext rtc;
    try{
      rtc = revisionManagement.getRuntimeContext(revision);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }
    
    for (ApplicationEntryStorable appEntry : appEntries) {
      Stack<String> stack = new Stack<String>();
      if (appEntry.getTypeAsEnum() == ApplicationEntryType.WORKFLOW) {
        // return WF itself as dependency, we return everything that is relevant
        //   so later on it is known if explicit datatypes are needed (becaus we returned them as dependency) or can be skipped
        dependencies.put(appEntry.getName(), appEntry);
        DeploymentItemStateImpl state = (DeploymentItemStateImpl) registry.get(appEntry.getName());
        PublishedInterfaces interfaces = state.getPublishedInterfaces(DeploymentLocation.DEPLOYED);
        OperationInterface wfOperation = interfaces.getAllOperations().iterator().next();
        stack.push("Workflow " + appEntry.getName());
        atLeastOneWorkflow = true;
        findDependenciesOfOperation(statusOutputStream, wfOperation, dependencies, revisionsToKeep, revision, app, verbose, stack);
        try {
          findDependenciesOfOperationOutput(wfOperation, dependencies, revision, appEntry.getApplication(), appEntry.getVersion());
        } catch (Exception e) {
          if (logger.isWarnEnabled()) {
            logger.warn("Exception during collection of Operation (" + wfOperation.getName() + ")subtypes. Stub may be incomplete.", e);
          }
        }
      } else if (appEntry.getTypeAsEnum() == ApplicationEntryType.ORDERTYPE) {
        ExecutionDispatcher dispatcher =
            XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution().getExecutionEngineDispatcher();
        try {
          DestinationKey dk = new DestinationKey(appEntry.getName(), rtc);
          DestinationValue value = dispatcher.getDestination(dk);
          if (value.getDestinationType() == ExecutionType.XYNA_FRACTAL_WORKFLOW && !dispatcher.isPredefined(value)) {
            Set<Long> allRevisions = value.resolveAllRevisions(dk);
            if (allRevisions.contains(revision)) {
              dependencies.put(value.getFQName(), ApplicationEntryStorable.create(app.getName(), app.getVersionName(), value.getFQName(),
                                                                                  ApplicationEntryType.WORKFLOW));
              DeploymentItemStateImpl state = (DeploymentItemStateImpl) registry.get(value.getFQName());
              PublishedInterfaces interfaces = state.getPublishedInterfaces(DeploymentLocation.DEPLOYED);
              OperationInterface wfOperation = interfaces.getAllOperations().iterator().next();
              stack.push("OrderType " + appEntry.getName());
              stack.push("Workflow " + value.getFQName());
              atLeastOneWorkflow = true;
              findDependenciesOfOperation(statusOutputStream, wfOperation, dependencies, revisionsToKeep, revision, app, verbose, stack);
              dependencies.put("OT" + appEntry.getName(), ApplicationEntryStorable
                  .create(app.getName(), app.getVersionName(), value.getFQName(), ApplicationEntryType.ORDERTYPE));
            } else {
              if (revisionsToKeep.add(value.resolveRevision(dk)) && verbose) {
                RuntimeContext runtimeContext;
                try {
                  runtimeContext = revisionManagement.getRuntimeContext(value.resolveRevision(dk));
                } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
                  throw new RuntimeException(e);
                }
                output(statusOutputStream, runtimeContext + " added to dependencies because of orderType " + appEntry.getName() + ".");
              }
            }
          }
        } catch (XPRC_DESTINATION_NOT_FOUND e) {
          logger.warn("OrderType " + appEntry.getName() + " could not be resolved in Execution Dispatcher. Stub may be incomplete.");
        }
      }
    }
    if (atLeastOneWorkflow) {
      //Base Application benötigt für XynaExceptionBase etc
      Long rev = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
          .getRevisionDefiningXMOMObject(GenerationBase.CORE_XYNAEXCEPTIONBASE, revision);
      if (rev != null) {
        revisionsToKeep.add(rev);
      }
    }
    //Datentypen nur mitnehmen, wenn sie in den Workflowschnittstellen verwendet werden. Wenn man sie trotzdem verwenden möchte, gehören sie in eine separate Application.
    return dependencies.values();
  }


  private void findDependenciesOfOperationOutput(OperationInterface wfOperation, Map<String, ApplicationEntryStorable> dependencies,
                                                 Long revision, String app, String version) {

    Set<String> subTypes = new HashSet<String>();
    List<TypeInterface> outputs = wfOperation.getOutput();
    List<AVariable> outputVars = new ArrayList<AVariable>();
    GenerationBaseCache cache = new GenerationBaseCache();
    
    for(TypeInterface ti : outputs) {
      AVariable var = avarFromNameAndRevision(ti.getName(), revision, ti.getType(), cache);
      outputVars.add(var);
    }
    
    Set<String> processedOutputs = new HashSet<String>();
    List<String> empty = new ArrayList<String>();
    for (AVariable outputVar : outputVars) {
      subTypes.addAll(getSubTypesRecursively(outputVar, empty, processedOutputs, cache, revision));
    }
    
    for(String subType : subTypes) {
      if(dependencies.containsKey(subType)) {
        continue;
      }
      
      ApplicationEntryStorable aes = ApplicationEntryStorable.create(app, version, subType, ApplicationEntryType.DATATYPE);
      dependencies.put(subType, aes);
    }
    
  }
  
  
  private AVariable avarFromNameAndRevision(String name, Long revision, XMOMType type, GenerationBaseCache cache) {
    DomOrExceptionGenerationBase gb = null;
    Long correctRevision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
        .getRevisionDefiningXMOMObject(name, revision);
    if(type == XMOMType.DATATYPE) { 
      try {
        gb = DOM.getOrCreateInstance(name, cache, correctRevision);
      } catch (XPRC_InvalidPackageNameException e) {
        throw new RuntimeException(e);
      }
    } else if (type == XMOMType.EXCEPTION) {
      try {
        gb = ExceptionGeneration.getOrCreateInstance(name, cache, correctRevision);
      } catch (XPRC_InvalidPackageNameException e) {
        throw new RuntimeException(e);
      }
    }
    try {
      gb.parseGeneration(true, false, false);
    } catch (XPRC_InheritedConcurrentDeploymentException | AssumedDeadlockException | XPRC_MDMDeploymentException e) {
      throw new RuntimeException(e);
    }
    AVariable var = AVariable.createAVariable("-1", gb, false);
    return var;
  }


  private void findDependenciesOfOperation(PrintStream statusOutputStream, OperationInterface operation, Map<String, ApplicationEntryStorable> dependencies,
                                           Set<Long> revisionsToKeep, Long revision, Application app, boolean verbose, Stack<String> stack) {
    stack.push("Input");
    findDependenciesOfTypes(statusOutputStream, operation.getInput(), dependencies, revisionsToKeep, revision, app, verbose, stack);
    stack.pop();
    stack.push("Output");
    findDependenciesOfTypes(statusOutputStream, operation.getOutput(), dependencies, revisionsToKeep, revision, app, verbose, stack);
    stack.pop();
    stack.push("Exception");
    findDependenciesOfTypes(statusOutputStream, operation.getExceptions(), dependencies, revisionsToKeep, revision, app, verbose, stack);
    stack.pop();
  }


  private void findDependenciesOfTypes(PrintStream statusOutputStream, Collection<TypeInterface> types, Map<String, ApplicationEntryStorable> dependencies,
                                       Set<Long> revisionsToKeep, Long revision, Application app, boolean verbose, Stack<String> stack) {
    for (TypeInterface type : types) {
      findDependenciesOfType(statusOutputStream, type, dependencies, revisionsToKeep, revision, app, verbose, stack);
    }
  }


  private void findDependenciesOfType(PrintStream statusOutputStream, TypeInterface type, Map<String, ApplicationEntryStorable> dependencies, Set<Long> revisionsToKeep,
                                      Long revision, Application app, boolean verbose, Stack<String> stack) {
    if (type.isJavaBaseType()) {
      return;
    }
    if (dependencies.containsKey(type.getName())) {
      return;
    }
    stack.push(type.getName());
    DeploymentItemStateManagement dism =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement();
    DeploymentItemStateImpl dependency = (DeploymentItemStateImpl) dism.get(type.getName(), revision); // TODO check if instanceof or move to interface
    if (dependency == null) {
      RuntimeContextDependencyManagement rcdm =
          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
      Long correctRevision = rcdm.getRevisionDefiningXMOMObject(type.getName(), revision);
      if (correctRevision == null) {
        throw new RuntimeException(type.getName() + " not found in revision " + revision + ".");
      }
      
      if (revisionsToKeep.add(correctRevision) && verbose) {
        RuntimeContext runtimeContext;
        try {
          runtimeContext = revisionManagement.getRuntimeContext(correctRevision);
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          throw new RuntimeException(e);
        }
        output(statusOutputStream, runtimeContext + " added to dependencies because of object " + type.getName() + ". (usage path=" + printStack(stack) + ")");
      }
    } else {
      if (dependency.getType() == null || !dependency.exists()) {
        logger.warn("Type of " + type.getName() + " could not be resolved (exists=" + dependency.exists() + "). Stub may be incomplete. (usage path=" + printStack(stack) + ")");
      } else {
        dependencies.put(dependency.getName(), asApplicationEntryStorable(dependency, app));
        findDependenciesOfType(statusOutputStream, dependency, dependencies, revisionsToKeep, revision, app, verbose, stack);
      }
    }
    stack.pop();
  }


  private String printStack(Stack<String> stack) {
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (String s : stack) {
      if (first) {
        first = false;
      } else {
        sb.append(" -> ");
      }
      sb.append(s);
    }
    return sb.toString();
  }


  private void findDependenciesOfType(PrintStream statusOutputStream, DeploymentItemStateImpl dependency, Map<String, ApplicationEntryStorable> dependencies,
                                      Set<Long> revisionsToKeep, Long revision, Application app, boolean verbose, Stack<String> stack) {
    PublishedInterfaces published = dependency.getPublishedInterfaces(DeploymentLocation.DEPLOYED);
    if (published.getSupertype().isPresent()) {
      stack.push("BaseType");
      findDependenciesOfType(statusOutputStream, published.getSupertype().get(), dependencies, revisionsToKeep, revision, app, verbose, stack);
      stack.pop();
    }
    Set<MemberVariableInterface> memVars = published.filterInterfaces(MemberVariableInterface.class);
    for (MemberVariableInterface memVar : memVars) {
      stack.push("Member");
      findDependenciesOfType(statusOutputStream, memVar.getType(), dependencies, revisionsToKeep, revision, app, verbose, stack);
      stack.pop();
    }
    // TODO rewrite this later if we try to exclude operations. Achtung, dann auch unten bei createStubForFile berücksichtigen
    for (OperationInterface operation : published.getAllOperations()) {
      stack.push("Operation " + operation.getName());
      findDependenciesOfOperation(statusOutputStream, operation, dependencies, revisionsToKeep, revision, app, verbose, stack);
      stack.pop();
    }
  }


  private ApplicationEntryStorable asApplicationEntryStorable(DeploymentItemStateImpl dependency, Application app) {
    return ApplicationEntryStorable.create(app.getName(), app.getVersionName(), dependency.getName(),
                                           ApplicationEntryType.getByXMOMType(dependency.getType()));
  }

  
  public static class StubFileCreator {
    private static final Pattern patternFileIgnoreForStub = Pattern.compile(".*/(trigger|filter|services|sharedLibs)/.*");

    public InputStream createStubForFile(File f, Set<String> xmomEntries) throws FileNotFoundException {
      /*
       * ist file eines, was im stub enthalten sein muss? wenn ja, ist es ein workflow? wenn ja, erzeuge entsprechenden stub. datentype+operations -> entferne alle operations.
       * classfile? -> ignore
       * jars -> ignore
       * trigger/filter/sharedlibs -> ignore
       * 
       */
      if (f.getName().endsWith(".class")) {
        return null;
      }
      if (patternFileIgnoreForStub.matcher(f.getPath()).matches()) {
        return null;
      }
      if (f.getName().endsWith(".xml")) {
        try {
          Document dom = XMLUtils.parse(f);
          String rootElementName = dom.getDocumentElement().getNodeName();
          String fqName = dom.getDocumentElement().getAttribute(GenerationBase.ATT.TYPEPATH) + "."
              + dom.getDocumentElement().getAttribute(GenerationBase.ATT.TYPENAME);
          if (rootElementName.equals(GenerationBase.EL.EXCEPTIONSTORAGE)) {
            Element exel = XMLUtils.getChildElementByName(dom.getDocumentElement(), GenerationBase.EL.EXCEPTIONTYPE);
            fqName = exel.getAttribute(GenerationBase.ATT.TYPEPATH) + "." + exel.getAttribute(GenerationBase.ATT.TYPENAME);
          }
          if (!xmomEntries.contains(fqName)) {
            return null;
          }
          if (rootElementName.equals(GenerationBase.EL.EXCEPTIONSTORAGE)) {
            return new FileInputStream(f);
          }
          if (rootElementName.equals(GenerationBase.EL.DATATYPE)) {
            /*
             *   <Libraries>HashServiceImpl.jar</Libraries>
             */
            for (Element el : XMLUtils.getChildElementsByName(dom.getDocumentElement(), GenerationBase.EL.LIBRARIES)) {
              dom.getDocumentElement().removeChild(el);
            }
            for (Element el : XMLUtils.getChildElementsByName(dom.getDocumentElement(), GenerationBase.EL.SHAREDLIB)) {
              dom.getDocumentElement().removeChild(el);
            }
            /*
             * eigtl würde man gerne den gesamten Service entfernen. Das kann aber dazu führen, dass der typ eigtl abstrakt sein müsste, weil ein basetype
             * abstrakte operations definiert und man selbst nicht abstrakt ist.
             */
            Element serviceEl = XMLUtils.getChildElementByName(dom.getDocumentElement(), GenerationBase.EL.SERVICE);
            /*
             * TODO
             * für jede operation prüfen, ob sie entfernt werden kann:
             * - nein, wenn der erste basistyp in der vererbungshierarchie der die operation definiert/überschreibt, der ausserhalb des stubs liegt die operation als abstrakt definiert.
             * - ja, sonst
             * 
             * TODO
             * instanzmethoden die als workflow implementiert sind
             * 
             */
            if (serviceEl != null) {
              for (Element el : XMLUtils.getChildElementsRecursively(serviceEl, GenerationBase.EL.CODESNIPPET)) {
                XMLUtils
                    .setTextContent(el,
                                    "throw new java.lang.RuntimeException(\"This operation is part of a stub application and may not be called.\");");
              }
            }
          }
          if (rootElementName.equals(GenerationBase.EL.SERVICE)) {
            //Workflow
            Element operation = XMLUtils.getChildElementByName(dom.getDocumentElement(), GenerationBase.EL.OPERATION);
            List<Element> outputDatas = new ArrayList<Element>();
            for (Element child : XMLUtils.getChildElements(operation)) {
              String name = child.getNodeName();
              if (name.equals(GenerationBase.EL.OUTPUT)) {
                outputDatas.addAll(XMLUtils.getChildElementsByName(child, GenerationBase.EL.DATA));
              } else if (name.equals(GenerationBase.EL.INPUT) || name.equals(GenerationBase.EL.THROWS)) {
                //ok
              } else {
                operation.removeChild(child);
              }
            }
            for (Element sourceChild : XMLUtils.getChildElementsRecursively(operation, GenerationBase.EL.SOURCE)) {
              sourceChild.getParentNode().removeChild(sourceChild);
            }
            for (Element targetChild : XMLUtils.getChildElementsRecursively(operation, GenerationBase.EL.TARGET)) {
              targetChild.getParentNode().removeChild(targetChild);
            }
            //nun alle outputs constant vorbelegen
            if (outputDatas.size() > 0) {
              /*
               * Beispiel XML:
     <Operation ID="0" Label="WFTest" Name="WFTest">
      <Input>
        <Data ID="29" Label="Text" ReferenceName="Text" ReferencePath="base" VariableName="text"/>
        <Data IsList="true" Label="Password" ReferenceName="Password" ReferencePath="base" VariableName="password"/>
      </Input>
      <Output>
        <Data ID="23" IsList="true" Label="DT 1a" ReferenceName="DT1a" ReferencePath="cl.bugz21754" VariableName="dT1a23">
          <Source RefID="8"/>
        </Data>
        <Data ID="28" Label="Text" ReferenceName="Text" ReferencePath="base" VariableName="text28">
          <Source RefID="8"/>
        </Data>
      </Output>
      <Data ID="35" IsList="true" Label="DT1a" ReferenceName="DT1a" ReferencePath="cl.bugz21754" VariableName="const_DT1a">
        <Target RefID="8"/>
      </Data>
      <Data ID="36" Label="Text" ReferenceName="Text" ReferencePath="base" VariableName="const_Text">
        <Target RefID="8"/>
        <Data Label="text" VariableName="text">
          <Meta>
            <Type>String</Type>
          </Meta>
        </Data>
      </Data>
      <Assign ID="8">
        <Source RefID="35"/>
        <Source RefID="36"/>
        <Target RefID="23"/>
        <Target RefID="28"/>
        <Copy>
          <Source RefID="35">
            <Meta>
              <LinkType>Constant</LinkType>
            </Meta>
          </Source>
          <Target RefID="23"/>
        </Copy>
        <Copy>
          <Source RefID="36">
            <Meta>
              <LinkType>Constant</LinkType>
            </Meta>
          </Source>
          <Target RefID="28"/>
        </Copy>
      </Assign>
    </Operation>

               */
              //liste aller vorhandenen/referenzierten ids erzeugen
              Set<Integer> ids = new HashSet<Integer>();
              Set<String> varNames = new HashSet<String>();
              collectAllIdsAndVarNames(ids, varNames, dom.getDocumentElement());
              List<Integer> idssorted = new ArrayList<Integer>(ids);
              int max = 0;
              if (idssorted.size() > 0) {
                max = Collections.max(idssorted);
              }
              int assignId = ++max;
              
              //assign element
              Element assign = dom.createElement(GenerationBase.EL.ASSIGN);
              assign.setAttribute(GenerationBase.ATT.ID, String.valueOf(assignId));
              List<Element> targetDataInAssign = new ArrayList<Element>();
              List<Element> sourceDataInAssign = new ArrayList<Element>();
              
              //  erst dataelemente auf operationebene anlegen, dann assign dafür erzeugen
              for (Element output : outputDatas) {
                Element outputCopy = (Element) output.cloneNode(true);
                operation.appendChild(outputCopy);
                int id = ++max;
                idssorted.add(id);
                outputCopy.setAttribute(GenerationBase.ATT.ID, String.valueOf(id));
                String outputId = output.getAttribute(GenerationBase.ATT.ID);
                if (outputId == null || outputId.length() == 0) {
                  outputId = String.valueOf(++max);
                  output.setAttribute(GenerationBase.ATT.ID, outputId);
                }
                
                Element targetAssign = dom.createElement(GenerationBase.EL.TARGET);
                targetAssign.setAttribute(GenerationBase.ATT.REFID, String.valueOf(assignId));
                outputCopy.appendChild(targetAssign);
                String varName = "v" + id;
                while (varNames.contains(varName)) {
                  varName = "v" + varName;
                }
                varNames.add(varName);
                outputCopy.setAttribute(GenerationBase.ATT.VARIABLENAME, varName); //muss unique sein
                
                Element sourceAssign = dom.createElement(GenerationBase.EL.SOURCE);
                sourceAssign.setAttribute(GenerationBase.ATT.REFID, String.valueOf(assignId));
                output.appendChild(sourceAssign);
                
                Element targetData = dom.createElement(GenerationBase.EL.TARGET);
                targetData.setAttribute(GenerationBase.ATT.REFID, outputId);
                targetDataInAssign.add(targetData);
                Element sourceData = dom.createElement(GenerationBase.EL.SOURCE);
                sourceData.setAttribute(GenerationBase.ATT.REFID, String.valueOf(id));
                sourceDataInAssign.add(sourceData);
              }
              
              operation.appendChild(assign);
              
              //assign objekt zusammenbauen
              for (Element source : sourceDataInAssign) {
                assign.appendChild(source);
              }
              for (Element target : targetDataInAssign) {
                assign.appendChild(target);
              }
              for (int i = 0; i<outputDatas.size(); i++) {
                Element copy = dom.createElement(GenerationBase.EL.COPY);
                assign.appendChild(copy);
                Element source = (Element) sourceDataInAssign.get(i).cloneNode(true);
                Element meta = dom.createElement(GenerationBase.EL.META);
                source.appendChild(meta);
                Element linkType = dom.createElement(GenerationBase.EL.LINKTYPE);
                meta.appendChild(linkType);
                Text text = dom.createTextNode("Constant");
                linkType.appendChild(text);
                copy.appendChild(source);
                copy.appendChild(targetDataInAssign.get(i).cloneNode(true));
              }
            }
          }
          //abgespecktes dom zurückgeben
          StringWriter sw = new StringWriter();
          XMLUtils.saveDomToWriter(sw, dom);
          try {
            return new ByteArrayInputStream(sw.toString().getBytes(Constants.DEFAULT_ENCODING));
          } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
          }
        } catch (Ex_FileAccessException e) {
          throw new FileNotFoundException(f.getName());
        } catch (XPRC_XmlParsingException e) {
          throw new RuntimeException(e);
        }
      }
      return null;
    }
  

    private void collectAllIdsAndVarNames(Set<Integer> ids, Set<String> varNames, Element element) {
      String id = element.getAttribute(GenerationBase.ATT.ID);
      if (id != null && id.length() > 0) {
        ids.add(Integer.valueOf(id));
      }
      id = element.getAttribute(GenerationBase.ATT.REFID);
      if (id != null && id.length() > 0) {
        ids.add(Integer.valueOf(id));
      }
      String varName = element.getAttribute(GenerationBase.ATT.VARIABLENAME);
      if (varName != null && varName.length() > 0) {
        varNames.add(varName);
      }
      for (Element e : XMLUtils.getChildElements(element)) {
        collectAllIdsAndVarNames(ids, varNames, e);
      }
    }
  }
  private boolean hasApplicationConfigurationChanged(String applicationName, ApplicationXmlEntry actual, XMOMAccess xmomAccess) {
    File applicationXmlFile = new File(xmomAccess.getAbsoluteAppConfigFileName(applicationName));
    ApplicationXmlEntry old = parseApplicationXml(applicationXmlFile);

    return !actual.entriesEqual(old);
  }


  /**
   * Ermittelt dependencies für RuntimeApplications
   * @return zurückgegebene liste enthält alle dependencies, aber nicht die elemente, die bereits übergeben werden
   */
  private List<ApplicationEntryStorable> findDependencies(List<? extends ApplicationEntryStorable> appEntries, Long revision)
      throws PersistenceLayerException {
    List<ApplicationEntryStorable> result = new ArrayList<ApplicationEntryStorable>();
    if (appEntries.size() == 0) {
      return result;
    }

    //extrabehandlung für dependencies von objekten, die sich nicht über das dependencyregister ergeben
    Set<String> capacities = new HashSet<String>();
    Set<String> existingCapacities = new HashSet<String>();
    Set<DependencyNode> processedEntries = new HashSet<DependencyNode>();
    Set<String> startInputSources = new HashSet<String>();
    for (ApplicationEntryStorable entry : appEntries) {

      //OrderInputSources stehen nicht im DependencyRegister
      if (entry.getTypeAsEnum() == ApplicationEntryType.ORDERINPUTSOURCE) {
        OrderInputSourceManagement oism =
            XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderInputSourceManagement();
        OrderInputSourceStorable inputSource = oism.getInputSourceByName(revision, entry.getName(), false);
        startInputSources.add(inputSource.getName());
        continue;
      }

      DependencyNode node = null;
      DependencySourceType depType = convertApplicationEntryTypeToDependencySourceType(entry.getTypeAsEnum());
      if (depType != null) {
        node = dependencyRegister.getDependencyNode(entry.getName(), depType, revision);
      }
      if (node == null) {
        //sonderbehandlung für filter- und triggerinstanzen, weil es dafür keine dependencynodes gibt
        if (entry.getTypeAsEnum() == ApplicationEntryType.FILTERINSTANCE) {
          //filter und triggerinstance bestimmen
          Triple<String, Long, DependencyNode> triple = getFilterDependencyNodeForFilterInstance(entry.getName(), revision);
          if (triple != null) {
            if (triple.getSecond().equals(revision)) {
              //checken, ob triggerinstance bereits vorhanden
              boolean found = false;
              for (ApplicationEntryStorable e : appEntries) {
                if (e.getTypeAsEnum() == ApplicationEntryType.TRIGGERINSTANCE && e.getName().equals(triple.getFirst())) {
                  found = true;
                  break;
                }
              }

              if (!found) {
                result.add(ApplicationEntryStorable.create(entry.getApplication(), entry.getVersion(), triple.getFirst(),
                                                        ApplicationEntryType.TRIGGERINSTANCE));
              }

            }

            if (triple.getThird() != null && triple.getThird().getRevision().equals(revision)) {
              processedEntries.add(triple.getThird());
              //checken, ob filter bereits vorhanden
              boolean found = false;
              for (ApplicationEntryStorable e : appEntries) {
                if (e.getTypeAsEnum() == ApplicationEntryType.FILTER && e.getName().equals(triple.getThird().getUniqueName())) {
                  found = true;
                  break;
                }
              }

              if (!found) {
                //weil processedEntries unten ignoriert werden
                result.add(ApplicationEntryStorable.create(entry.getApplication(), entry.getVersion(), triple.getThird().getUniqueName(),
                                                        ApplicationEntryType.FILTER));
              }
            }
          }
        } else if (entry.getTypeAsEnum() == ApplicationEntryType.TRIGGERINSTANCE) {
          DependencyNode trigger = getTriggerDependencyNodeForTriggerInstance(entry.getName(), revision);
          if (trigger != null && trigger.getRevision().equals(revision)) {
            processedEntries.add(trigger);
            //checken, ob filter bereits vorhanden
            boolean found = false;
            for (ApplicationEntryStorable e : appEntries) {
              if (e.getTypeAsEnum() == ApplicationEntryType.TRIGGER && e.getName().equals(trigger.getUniqueName())) {
                found = true;
                break;
              }
            }
            if (!found) {
              //weil processedEntries unten ignoriert werden
              result.add(ApplicationEntryStorable.create(entry.getApplication(), entry.getVersion(), trigger.getUniqueName(),
                                                      ApplicationEntryType.TRIGGER));
            }
          }
        } else if (entry.getTypeAsEnum() == ApplicationEntryType.CAPACITY) {
          //ok
          existingCapacities.add(entry.getName());
        } else {
          logger.warn("application entry " + entry.getName() + " of type " + entry.getType()
              + " not found in dependency register in revision " + revision + ".");
        }
      } else {
        processedEntries.add(node);
      }
    }
    List<DependencyNode> startEntries = new ArrayList<DependencyNode>(processedEntries);
    Set<String> processedInputSources = new HashSet<String>();

    String applicationName = appEntries.get(0).getApplication();
    String version = appEntries.get(0).getVersion();

    //eigentliche dependency-suche/rekursion
    for (DependencyNode entry : startEntries) {
      forDependenciesRecursively(entry, processedEntries, processedInputSources, revision);
    }

    for (String inputSource : startInputSources) {
      findOrderInputSourceDependencies(inputSource, processedEntries, processedInputSources, revision);
    }

    //Evtl. werden nicht alle XMOMEntries über das DependencyRegister gefunden (z.B. weil ein
    //Ordertype umkonfiguriert wurde und der ursprüngliche Workflow von keinem Objekt mehr abhängt).
    //Daher hier alle deployed XMOMEntries und deren Abhängigkeiten suchen
    List<ApplicationEntryStorable> xmomEntries = getAllXMOMEntries(applicationName, version);
    for (ApplicationEntryStorable entry : xmomEntries) {
      DependencyNode node = null;
      DependencySourceType depType = convertApplicationEntryTypeToDependencySourceType(entry.getTypeAsEnum());
      if (depType != null) {
        node = dependencyRegister.getDependencyNode(entry.getName(), depType, revision);
      }

      //Dependencies bestimmen, falls noch nicht geschehen
      if (node != null && !processedEntries.contains(node)) {
        forDependenciesRecursively(node, processedEntries, processedInputSources, revision);
      }
    }

    //capacities aller ordertypes bestimmen
    for (DependencyNode dn : processedEntries) {
      if (dn.getType() == DependencySourceType.ORDERTYPE) {
        List<Capacity> capacityList =
            XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getCapacityMappingDatabase()
                .getCapacities(new DestinationKey(dn.getUniqueName(), applicationName, version));
        for (Capacity c : capacityList) {
          capacities.add(c.getCapName());
        }
      }
    }
    capacities.removeAll(existingCapacities);

    //nur die nodes zurückgeben, die nicht oben reingegeben wurden
    processedEntries.removeAll(startEntries);

    for (DependencyNode node : processedEntries) {
      result.add(ApplicationEntryStorable.create(applicationName, version, node.getUniqueName(), convertXMOMTypeToApplicationEntryType(node)));
    }

    processedInputSources.removeAll(startInputSources);
    for (String inputSource : processedInputSources) {
      result.add(ApplicationEntryStorable.create(applicationName, version, inputSource, ApplicationEntryType.ORDERINPUTSOURCE));
    }

    for (String cap : capacities) {
      result.add(ApplicationEntryStorable.create(applicationName, version, cap, ApplicationEntryType.CAPACITY));
    }
    return result;
  }


  private void forDependenciesRecursively(DependencyNode node, Set<DependencyNode> processedEntries, Set<String> processedInputSources,
                                          Long revision) {
    processedEntries.add(node);

    Set<DependencyNode> depNodes =
        dependencyRegister.getAllUsedNodesSameRevision(node.getUniqueName(), node.getType(), false, false, revision);

    //default-ordertypes zu workflows findet man so nicht, weil die abhängigkeit andersherum ist
    if (node.getType() == DependencySourceType.WORKFLOW) {
      String ordertype;
      try {
        ordertype = GenerationBase.transformNameForJava(node.getUniqueName());
      } catch (XPRC_InvalidPackageNameException e) {
        throw new RuntimeException(e);
      }
      Set<DependencyNode> allUsedNodes =
          dependencyRegister.getAllUsedNodesSameRevision(ordertype, DependencySourceType.ORDERTYPE, false, true, revision);
      if (allUsedNodes.size() > 0) {
        //ordertype gefunden
        for (DependencyNode dn : allUsedNodes) {
          if (dn.getType() == DependencySourceType.ORDERTYPE && dn.getUniqueName().equals(ordertype)) {
            if (!processedEntries.contains(dn)) {
              //wahrscheinlich keine rekursion notwendig, aber schadet nichts
              depNodes = new HashSet<DependencyNode>(depNodes);
              depNodes.add(dn);
            }
            break;
          }
        }
      }
    }

    //OrderInputSources
    if (node.getType() == DependencySourceType.ORDERTYPE) {
      ExecutionDispatcher executionDispatcher =
          XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution().getExecutionEngineDispatcher();
      OrderInputSourceManagement oism =
          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderInputSourceManagement();

      try {
        RuntimeContext oldRuntimeContext = revisionManagement.getRuntimeContext(revision);
        DestinationKey dk = new DestinationKey(node.getUniqueName(), oldRuntimeContext);
        DestinationValue dv = executionDispatcher.getDestination(dk);

        List<String> ois = oism.getReferencedOrderInputSources(dv, revision);
        for (String inputSource : ois) {
          findOrderInputSourceDependencies(inputSource, processedEntries, processedInputSources, revision);
        }
      } catch (XynaException e) {
        logger.warn("Could not find order input sources for ordertype " + node.getUniqueName(), e);
      }
    }

    for (DependencyNode depNode : depNodes) {
      if (!processedEntries.contains(depNode)) {
        forDependenciesRecursively(depNode, processedEntries, processedInputSources, revision);
      }
    }
  }


  private void findOrderInputSourceDependencies(String inputSourceName, Set<DependencyNode> processedEntries,
                                                Set<String> processedInputSources, long revision) {
    if (processedInputSources.add(inputSourceName)) {
      String deploymentItemStateName = OrderInputSourceManagement.convertNameToUniqueDeploymentItemStateName(inputSourceName);

      DeploymentItemStateImpl diis =
          (DeploymentItemStateImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
              .getDeploymentItemStateManagement().get(deploymentItemStateName, revision);
      if (diis != null) {
        Set<String> usedOrderTypes = diis.getUsedOrderTypes(DeploymentLocation.DEPLOYED);

        for (String ot : usedOrderTypes) {
          DependencyNode depNode = dependencyRegister.getDependencyNode(ot, DependencySourceType.ORDERTYPE, revision);
          if (depNode != null && 
              !processedEntries.contains(depNode)) {
            forDependenciesRecursively(depNode, processedEntries, processedInputSources, revision);
          }
        }
      }
    }
  }


  public ApplicationXmlEntry createApplicationXmlEntry(String applicationName, String versionName, Long revision, Long parentRevision, boolean ignoreExceptions, boolean createStub)
      throws PersistenceLayerException, XPRC_DESTINATION_NOT_FOUND, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, Ex_FileAccessException,
      XPRC_XmlParsingException, XPRC_InvalidXmlMissingRequiredElementException, XFMG_WrongDeploymentState,
      XFMG_ObjectUnkownInDeploymentItemStateManagement {
    ApplicationXmlEntry applicationXmlEntry = new ApplicationXmlEntry(applicationName, WORKINGSET_VERSION_NAME, null);
    List<ApplicationEntryStorable> appEntries = listApplicationDetails(applicationName, versionName, false, null, parentRevision);
    Long rev = revision != null ? revision : parentRevision;
    createXMLEntries(appEntries, false, null, applicationXmlEntry, rev, false, ignoreExceptions, createStub);
    Set<String> explicitEntries = new HashSet<String>();
    for (ApplicationEntryStorable explicit : appEntries) {
      explicitEntries.add(explicit.getName() + "#" + explicit.getType());
    }

    //implizite dependencies (ohne explizite)
    Collection<ApplicationEntryStorable> implicitDependencies = listApplicationDetails(applicationName, versionName, true, null, parentRevision);
    if (implicitDependencies != null) {
      Iterator<ApplicationEntryStorable> it = implicitDependencies.iterator();
      while (it.hasNext()) {
        ApplicationEntryStorable current = it.next();
        if (explicitEntries.contains(current.getName() + "#" + current.getType())) {
          it.remove();
        }
      }

      createXMLEntries(implicitDependencies, false, null, applicationXmlEntry, rev, true, ignoreExceptions, createStub);
    }

    return applicationXmlEntry;
  }


  private void createXMLEntries(Collection<? extends ApplicationEntryStorable> appEntries, boolean verbose, PrintStream statusOutputStream,
                                ApplicationXmlEntry applicationXmlEntry, Long revision, boolean isImplicitDependency,
                                boolean ignoreExeptions, boolean createStub) throws PersistenceLayerException, XPRC_DESTINATION_NOT_FOUND,
      XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, XFMG_WrongDeploymentState, XFMG_ObjectUnkownInDeploymentItemStateManagement {

    XynaActivationTrigger xynaActivationTrigger = XynaFactory.getInstance().getActivation().getActivationTrigger();
    PriorityManagement priorityManagement =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getPriorityManagement();
    CapacityMappingDatabase capacityMappingDatabase =
        XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getCapacityMappingDatabase();
    MonitoringDispatcher monitoringDispatcher =
        XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getMonitoringDispatcher();
    PlanningDispatcher planningDispatcher =
        XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaPlanning().getPlanningDispatcher();
    ExecutionDispatcher executionDispatcher =
        XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution().getExecutionEngineDispatcher();
    CleanupDispatcher cleanupDispatcher =
        XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaCleanup().getCleanupEngineDispatcher();
    ParameterInheritanceManagement parameterInheritanceMgmt =
        XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getParameterInheritanceManagement();

    for (ApplicationEntryStorable entry : appEntries) {
      XMOMType xmomType = convertApplicationEntryTypeToXMOMType(entry.getTypeAsEnum());
      checkDeploymentItemState(entry.getName(), xmomType, revision, false);

      if (verbose) {
        output(statusOutputStream, "Exporting " + entry.getType() + " " + entry.getName());
      }
      switch (entry.getTypeAsEnum()) {
        case DATATYPE :
          Collection<XMOMODSMapping> mappings = XMOMODSMappingUtils.getAllMappingsForRootType(entry.getName(), revision);
          for (XMOMODSMapping mapping : mappings) {
            applicationXmlEntry.getXmomStorableEntries().add(new XMOMStorableXmlEntry(mapping.getFqxmlname(),
                                                                                      mapping.getPath(),
                                                                                      mapping.getTablename(),
                                                                                      mapping.getFqpath(),
                                                                                      mapping.getColumnname()));            
          }
          //fall through
        case WORKFLOW :
        case EXCEPTION :
          applicationXmlEntry.getXmomEntries().add(new XMOMXmlEntry(isImplicitDependency, entry.getName(), entry.getType()));
          break;
        case FILTER :
          if (createStub) {
            break;
          }
          try {
            Filter filter = xynaActivationTrigger.getFilter(revision, entry.getName(), false);
            applicationXmlEntry.getFilters().add(new FilterXmlEntry(isImplicitDependency, filter.getName(), wrapArray(filter.getJarFiles(),
                                                                                                                      "/filter/"), filter
                                                     .getFQFilterClassName(), filter.getTriggerName(), wrapArray(filter.getSharedLibs())));
          } catch (XACT_FilterNotFound e) {
            logger.warn("Filter not found", e);
          }
          break;
        case FILTERINSTANCE :
          if (createStub) {
            break;
          }
          FilterInstanceInformation filterinstance = xynaActivationTrigger.getFilterInstanceInformation(entry.getName(), revision);
          if (filterinstance != null) {
            applicationXmlEntry.getFilterInstances().add(new FilterInstanceXmlEntry(isImplicitDependency, 
                filterinstance.getFilterInstanceName(), filterinstance.getFilterName(), 
                filterinstance.getTriggerInstanceName(), filterinstance.getConfiguration(), filterinstance.getDescription() ));
          }
          break;
        case SHAREDLIB :
          if (createStub) {
            break;
          }
          SharedLibXmlEntry sharedLibEntry = new SharedLibXmlEntry(isImplicitDependency, entry.getName());
          if (!applicationXmlEntry.getSharedLibs().contains(sharedLibEntry)) {
            applicationXmlEntry.getSharedLibs().add(sharedLibEntry);
          }
          break;
        case TRIGGER :
          if (createStub) {
            break;
          }
          try {
            Trigger trigger = xynaActivationTrigger.getTrigger(revision, entry.getName(), false);
            applicationXmlEntry.getTriggers().add(new TriggerXmlEntry(isImplicitDependency, trigger.getTriggerName(), wrapArray(trigger
                                                      .getJarFiles(), "/trigger/"), trigger.getFQTriggerClassName(), wrapArray(trigger
                                                      .getSharedLibs())));
          } catch (XACT_TriggerNotFound e) {
            logger.warn("Trigger not found", e);
          }
          break;
        case TRIGGERINSTANCE :
          if (createStub) {
            break;
          }
          TriggerInstanceInformation triggerinstance = xynaActivationTrigger.getTriggerInstanceInformation(entry.getName(), revision);
          if (triggerinstance != null) {
            Pair<Long, Boolean> triggerConfig =
                xynaActivationTrigger.getTriggerConfiguration(triggerinstance);

            applicationXmlEntry.getTriggerInstances().add(new TriggerInstanceXmlEntry(isImplicitDependency, triggerinstance
                                                              .getTriggerInstanceName(), triggerinstance.getTriggerName(), triggerinstance
                                                              .getStartParameterAsString(), triggerConfig.getFirst(), //maxReceives
                                                                                      triggerConfig.getSecond())); //autoReject

          }
          break;
        case ORDERTYPE :
          RuntimeContext runtimeContext = revisionManagement.getRuntimeContext(revision);
          DestinationKey dk = new DestinationKey(entry.getName(), runtimeContext);
          if (createStub) {
            DestinationValue dv_execution = executionDispatcher.getDestination(dk);
            if (dv_execution instanceof FractalWorkflowDestination) {
              applicationXmlEntry.getOrdertypes()
                  .add(new OrdertypeXmlEntry(false, null, executionDispatcher.isCustom(dk) ? dv_execution.getFQName() : null, null,
                                             entry.getName(), false));
            }
            //restliche konfiguration ist auf dem zielsystem
            break;
          }
          //TODO ordertypemanagement verwenden?!
          try {
            DestinationValue dv_planning = planningDispatcher.getDestination(dk);
            DestinationValue dv_execution = executionDispatcher.getDestination(dk);
            DestinationValue dv_cleanup = cleanupDispatcher.getDestination(dk);
            boolean hasOrdercontextMapping =
                XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderContextConfiguration()
                    .isDestinationKeyConfiguredForOrderContextMapping(dk, true);
            if (dv_planning instanceof FractalWorkflowDestination && dv_execution instanceof FractalWorkflowDestination
                && dv_cleanup instanceof FractalWorkflowDestination) {

              applicationXmlEntry.getOrdertypes()
                  .add(new OrdertypeXmlEntry(isImplicitDependency, planningDispatcher.isCustom(dk) ? dv_planning.getFQName() : null,
                                             executionDispatcher.isCustom(dk) ? dv_execution.getFQName() : null, cleanupDispatcher
                                                 .isCustom(dk) ? dv_cleanup.getFQName() : null, entry.getName(), hasOrdercontextMapping));
            }

            Integer prio = priorityManagement.getPriority(entry.getName(), revision);
            if (prio != null) {
              applicationXmlEntry.getPriorities().add(new PriorityXmlEntry(entry.getName(), prio));
            }
            List<Capacity> capacityList = capacityMappingDatabase.getCapacities(dk);
            for (Capacity cap : capacityList) {
              applicationXmlEntry.getCapacityRequirements().add(new CapacityRequirementXmlEntry(entry.getName(), cap.getCapName(), cap
                                                                    .getCardinality()));
            }
            Integer monitoringLevel = monitoringDispatcher.getMonitoringLevel(dk);
            if (monitoringLevel != null) {
              applicationXmlEntry.getMonitoringLevels().add(new MonitoringLevelXmlEntry(entry.getName(), monitoringLevel));
            }

            //Parameter Inheritance Rules
            Map<ParameterType, List<InheritanceRule>> inheritanceRules = parameterInheritanceMgmt.listInheritanceRules(dk);
            for (Entry<ParameterType, List<InheritanceRule>> rules : inheritanceRules.entrySet()) {
              for (InheritanceRule rule : rules.getValue()) {
                applicationXmlEntry.getParameterInheritanceRules().add(new InheritanceRuleXmlEntry(entry.getName(), rules.getKey(), rule
                                                                           .getChildFilter(), rule.getUnevaluatedValue(), rule
                                                                           .getPrecedence()));
              }
            }
          } catch (XPRC_DESTINATION_NOT_FOUND e) {
            if (ignoreExeptions) {
              logger.warn("Failed to obtain information about the ordertype " + entry.getName());
            } else {
              throw e;
            }
          }
          break;
        case XYNAPROPERTY :
          if (createStub) {
            break;
          }
          String propertyValue = XynaFactory.getInstance().getFactoryManagement().getProperty(entry.getName());
          if (propertyValue != null) {
            applicationXmlEntry.getXynaProperties().add(new XynaPropertyXmlEntry(isImplicitDependency, entry.getName(), propertyValue));
          }
          break;
        case CAPACITY :
          if (createStub) {
            break;
          }
          boolean found = false;
          try {
            CapacityInformation capInfo =
                XynaFactory.getInstance().getProcessing().getXynaScheduler().getCapacityManagement()
                    .getCapacityInformation(entry.getName());
            found = true;
            applicationXmlEntry.getCapacities().add(new CapacityXmlEntry(isImplicitDependency, capInfo.getName(), capInfo.getCardinality(),
                                                                         capInfo.getState()));
          } catch (Throwable e) {
            logger.warn("Failed to obtain information about the capacity " + entry.getName());
          }
          if (!found) {
            output(statusOutputStream, "Capacity <" + entry.getName() + "> not found!");
          }
          break;
        case FORMDEFINITION :
          if (createStub) {
            break;
          }
          applicationXmlEntry.getXmomEntries().add(new XMOMXmlEntry(isImplicitDependency, entry.getName(), entry.getType()));
          break;
        case ORDERINPUTSOURCE :
          if (createStub) {
            break;
          }
          OrderInputSourceManagement oism =
              XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderInputSourceManagement();
          OrderInputSourceStorable is = oism.getInputSourceByName(revision, entry.getName(), true);
          if (is != null) {
            OrderInputSourceXmlEntry inputSourceXmlEntry =
                new OrderInputSourceXmlEntry(isImplicitDependency, entry.getName(), is.getType(), is.getOrderType(), is.getParameters(),
                                             is.getDocumentation());
            applicationXmlEntry.getOrderInputSources().add(inputSourceXmlEntry);
          } else {
            logger.warn("Order input source '" + entry.getName() + "' not found");
          }
      }
    }
  }


  private void addRuntimeContextRequirementXMLEntries(Collection<RuntimeDependencyContext> dependentRCs, ApplicationXmlEntry applicationXmlEntry, boolean verbose,
                                                      PrintStream statusOutputStream) {
    for (RuntimeDependencyContext dep : dependentRCs) {
      if (verbose) {
        output(statusOutputStream, "Exporting RUNTIME CONTEXT REQUIREMENT " + dep.toString());
      }
      
      RuntimeContextRequirementXmlEntry entry = new RuntimeContextRequirementXmlEntry(dep.asCorrespondingRuntimeContext());
      applicationXmlEntry.getRuntimeContextRequirements().add(entry);
    }
  }

  public void buildApplicationVersion(String applicationName, String versionName, BuildApplicationVersionParameters params)
      throws XFMG_CouldNotBuildNewVersionForApplication {
    buildApplicationVersion(applicationName, versionName, params, false, null);
  }


  private static class PredictionValues {

    private int openFiles;
    private int cumulatedSizeOfJars;
    private int cumulatedSizeOfClasses;


    public int getPredictedAdditionalOpenFiles() {
      return openFiles;
    }


    public int getCumulatedSizeOfJars() {
      return cumulatedSizeOfJars;
    }


    public int getCumulatedSizeOfXMOMClasses() {
      return cumulatedSizeOfClasses;
    }
  }


  /**
   * Baut eine neue Revision für die Applikation unter dem neuen Versionsname.
   */
  @SuppressWarnings("unchecked")
  public void buildApplicationVersion(String applicationName, String versionName, BuildApplicationVersionParameters params,
                                      boolean verbose, PrintStream statusOutputStream) throws XFMG_CouldNotBuildNewVersionForApplication {
    if (applicationName == null || applicationName.length() == 0) {
      output(statusOutputStream, "applicationName must not be empty");
      throw new XFMG_CouldNotBuildNewVersionForApplication(applicationName, versionName,
                                                           new IllegalArgumentException("ApplicationName may not be empty"));
    }

    if (versionName == null || versionName.length() == 0) {
      output(statusOutputStream, "versionName must not be empty");
      throw new XFMG_CouldNotBuildNewVersionForApplication(applicationName, versionName,
                                                           new IllegalArgumentException("VersionName may not be empty"));
    }

    buildLock.lock();

    try {
      Long parentRevision;
      try {
        parentRevision = revisionManagement.getRevision(params.getParentWorkspace());
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new XFMG_CouldNotBuildNewVersionForApplication(applicationName, versionName, e);
      }
      String workingsetVersion = getWorkingsetVersionName(applicationName, parentRevision);

      if (workingsetVersion == null) {
        String msg =
            "Application definition '" + applicationName + "' not found in workspace '" + params.getParentWorkspace().getName() + "'.";
        output(statusOutputStream, msg);
        throw new XFMG_CouldNotBuildNewVersionForApplication(applicationName, versionName, new IllegalArgumentException(msg));
      }

      if (workingsetVersion.equals(versionName)) {
        String msg = "VersionName must be unique.";
        output(statusOutputStream, msg);
        throw new XFMG_CouldNotBuildNewVersionForApplication(applicationName, versionName, new IllegalArgumentException(msg));
      }

      Long revision = null;
      
      //RepAccessInstance
      XMOMAccess xmomAccess =
          XynaFactory.getInstance().getXynaDevelopment().getXynaLibraryDevelopment().getXMOMAccessManagement()
              .getXMOMAccessInstance(parentRevision);
      String branchName = getBranchName(applicationName, versionName);
      
      TreeSet<ApplicationEntryStorable> appEntriesForAllDependencies =
          new TreeSet<ApplicationEntryStorable>(ApplicationEntryStorable.COMPARATOR);
      ODSConnection con = ods.openConnection();

      try {
        List<ApplicationEntryStorable> storedAppEntries =
            (List<ApplicationEntryStorable>) queryAllApplicationDefinitionStorables(applicationName, parentRevision, con);

        // Revision anfordern
        output(statusOutputStream, "Create new revision of objects.");
        try {
          revision = revisionManagement.buildNewRevisionForNewVersion(applicationName, versionName);
        } catch (XFMG_DuplicateVersionForApplicationName e) {
          throw new XFMG_CouldNotBuildNewVersionForApplication(applicationName, versionName, e);
        }

        if (xmomAccess != null) {
          //Überprüfung ob der Branch bereits existiert
          List<String> branches = xmomAccess.listBranches();
          if (branches.contains(branchName)) {
            throw new XDEV_AlreadyExistsException("Branch '" + branchName + "' already exists");
          }
        }

        //RMI/CLI_Schnittstellen sperren
        RevisionOrderControl roc = new RevisionOrderControl(revision);
        roc.closeRMICLI();

        // Applikation anlegen
        ApplicationStorable app = new ApplicationStorable(applicationName, versionName, ApplicationState.STOPPED, params.getComment());
        app.setRemoteStub(params.getRemoteStub());
        con.persistObject(app);

        RevisionManagement.createNewRevisionDirectory(revision);

        // ApplicationEntries mit neuer Version anlegen
        copyApplicationEntriesToNewVersion(storedAppEntries, versionName, con);

        Set<DependencyNode> processedDependencies = new HashSet<DependencyNode>();
        Set<String> processedOrderInputSources = new HashSet<String>();

        Set<ApplicationEntryStorable> reqAppEntries = getRequiredApplicationDefinitionEntries(applicationName, parentRevision);

        PredictionValues predictionValues = new PredictionValues();
        for (ApplicationEntryStorable aes : storedAppEntries) {
          DependencySourceType sourceType = null;

          if (aes.getTypeAsEnum() == ApplicationEntryType.WORKFLOW) {
            sourceType = DependencySourceType.WORKFLOW;
          } else if (aes.getTypeAsEnum() == ApplicationEntryType.DATATYPE) {
            sourceType = DependencySourceType.DATATYPE;
          } else if (aes.getTypeAsEnum() == ApplicationEntryType.EXCEPTION) {
            sourceType = DependencySourceType.XYNAEXCEPTION;
          } else if (aes.getTypeAsEnum() == ApplicationEntryType.FILTER) {
            sourceType = DependencySourceType.FILTER;
          } else if (aes.getTypeAsEnum() == ApplicationEntryType.SHAREDLIB) {
            sourceType = DependencySourceType.SHAREDLIB;
          } else if (aes.getTypeAsEnum() == ApplicationEntryType.TRIGGER) {
            sourceType = DependencySourceType.TRIGGER;
          } else if (aes.getTypeAsEnum() == ApplicationEntryType.ORDERTYPE) {
            sourceType = DependencySourceType.ORDERTYPE;
          } else if (aes.getTypeAsEnum() == ApplicationEntryType.XYNAPROPERTY) {
            sourceType = DependencySourceType.XYNAPROPERTY;
          } else if (aes.getTypeAsEnum() == ApplicationEntryType.FILTERINSTANCE) {
            appEntriesForAllDependencies.add(ApplicationEntryStorable.create(aes.getApplication(), aes.getVersion(), aes.getName(),
                                                                          ApplicationEntryType.FILTERINSTANCE));

            //filter instances und trigger instances stehen nicht im dependency register -> zugehörigen filter ermitteln und dann rekursiv weiter
            Triple<String, Long, DependencyNode> triple = getFilterDependencyNodeForFilterInstance(aes.getName(), parentRevision);

            if (triple != null) {
              if (triple.getSecond().equals(parentRevision)) {
                //trigger instance hinzufügen
                appEntriesForAllDependencies.add(ApplicationEntryStorable.create(aes.getApplication(), aes.getVersion(), triple.getFirst(),
                                                                              ApplicationEntryType.TRIGGERINSTANCE));

              } else {
                //andere revision -> ok, gehört nicht zur application
              }

              if (triple.getThird() != null) {
                if (triple.getThird().getRevision().equals(parentRevision)) {
                  appEntriesForAllDependencies.add(aes); //filter instance hinzufügen
                  aes =
                      ApplicationEntryStorable.create(aes.getApplication(), aes.getVersion(), triple.getThird().getUniqueName(),
                                                   ApplicationEntryType.FILTER);
                  sourceType = DependencySourceType.FILTER;
                } else {
                  //andere revision -> ok, gehört nicht zur application
                  continue;
                }
              } else {
                throw new RuntimeException("filter instance " + aes.getName() + " not found");
              }

            } else {
              throw new RuntimeException("filter instance " + aes.getName() + " not found");
            }
          } else if (aes.getTypeAsEnum() == ApplicationEntryType.TRIGGERINSTANCE) {
            appEntriesForAllDependencies.add(ApplicationEntryStorable.create(aes.getApplication(), aes.getVersion(), aes.getName(),
                                                                          ApplicationEntryType.TRIGGERINSTANCE));

            //trigger instances stehen nicht im dependency register -> zugehörigen trigger ermitteln und dann rekursiv weiter
            DependencyNode trigger = getTriggerDependencyNodeForTriggerInstance(aes.getName(), parentRevision);
            if (trigger != null) {
              if (trigger.getRevision().equals(parentRevision)) {
                appEntriesForAllDependencies.add(aes); //trigger instance hinzufügen
                aes =
                    ApplicationEntryStorable.create(aes.getApplication(), aes.getVersion(), trigger.getUniqueName(),
                                                 ApplicationEntryType.TRIGGER);
                sourceType = DependencySourceType.TRIGGER;
              } else {
                //andere revision -> ok, gehört nicht zur application
                continue;
              }
            } else {
              throw new RuntimeException("trigger instance " + aes.getName() + " not found");
            }
          } else if (aes.getTypeAsEnum() == ApplicationEntryType.FORMDEFINITION) {
            appEntriesForAllDependencies.add(aes);
            continue;
          } else if (aes.getTypeAsEnum() == ApplicationEntryType.ORDERINPUTSOURCE) {
            OrderInputSourceManagement oism =
                XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderInputSourceManagement();
            OrderInputSourceStorable inputSource = oism.getInputSourceByName(parentRevision, aes.getName(), false);
            recursiveGetInputSourceDependencies(inputSource.getName(), parentRevision, revision, verbose, statusOutputStream,
                                                processedDependencies, predictionValues, processedOrderInputSources, reqAppEntries);
            continue;
          } else {
            continue;
          }

          DeploymentLocks.readLock(aes.getName(), sourceType, "BuildApplication", parentRevision);
          recursiveLockAndCopyDependencies(aes.getName(), sourceType, parentRevision, revision, verbose, statusOutputStream,
                                           processedDependencies, predictionValues, processedOrderInputSources, reqAppEntries);
        }

        //Die Subtypen von Output-Parametern von Java-Services sollen automatisch mitgenommen werden,
        //damit man hierfür nicht extra additionalDependencies angeben muss.
        //Die in excludeSubtypes enthaltenen Subtypen werden nicht explizit mitgenommen. Ist "*" enthalten,
        //so werden keine Subtypen mitgenommen, die nicht bereits über das DependencyRegister gefunden wurden.
        Set<String> subTypes =
            getSubTypesOfOutputVars(applicationName, versionName, parentRevision, processedDependencies, params.getExcludeSubtypesOf());
        for (String subType : subTypes) {
          DeploymentLocks.readLock(subType, DependencySourceType.DATATYPE, "BuildApplication", parentRevision);
          recursiveLockAndCopyDependencies(subType, DependencySourceType.DATATYPE, parentRevision, revision, verbose, statusOutputStream,
                                           processedDependencies, predictionValues, processedOrderInputSources, reqAppEntries);
        }

        //falls storables enthalten und forms enthalten sind, sollen auch die default storable-workflows mitgenommen werden.
        //TODO das könnte man hier wegnehmen, wenn man statt dessen die forms korrekt parst und auf dependencies analysiert
        //dort sollte dann diese dependency enthalten sein
        Set<String> storableWFs = getStorableWFs(processedDependencies, parentRevision);
        for (String storableWF : storableWFs) {
          DeploymentLocks.readLock(storableWF, DependencySourceType.ORDERTYPE, "BuildApplication", parentRevision);
          recursiveLockAndCopyDependencies(storableWF, DependencySourceType.ORDERTYPE, parentRevision, revision, verbose,
                                           statusOutputStream, processedDependencies, predictionValues, processedOrderInputSources,
                                           reqAppEntries);
        }

        checkOpenFiles(predictionValues.getPredictedAdditionalOpenFiles());
        checkPermGen(predictionValues.getCumulatedSizeOfJars(), predictionValues.getCumulatedSizeOfXMOMClasses());

        // Neue Revision deployen
        output(statusOutputStream, "Deploy new revision of objects.");

        for (DependencyNode dependency : processedDependencies) {
          appEntriesForAllDependencies.add(ApplicationEntryStorable.create(applicationName, versionName, dependency.getUniqueName(),
                                                                        convertXMOMTypeToApplicationEntryType(dependency)));
        }

        for (String inputSource : processedOrderInputSources) {
          appEntriesForAllDependencies.add(ApplicationEntryStorable.create(applicationName, versionName, inputSource,
                                                                        ApplicationEntryType.ORDERINPUTSOURCE));
        }

        con.commit();
      } catch (Throwable e) {
        Department.handleThrowable(e);
        if (revision != null) {
          try {
            revisionManagement.deleteRevision(revision);
            logger.info("Removed version <" + applicationName + ", " + versionName + "> because it could not be created successfully.");
          } catch (XynaException e1) {
            logger.warn("Rollback failed. Application and version could not be deleted.", e1);
          }
          RevisionManagement.removeRevisionFolder(revision, false);
          RevisionOrderControl roc = new RevisionOrderControl(revision);
          roc.openRMICLI();
        }
        if (e instanceof XFMG_CouldNotBuildNewVersionForApplication) {
          throw (XFMG_CouldNotBuildNewVersionForApplication) e;
        } else {
          throw new XFMG_CouldNotBuildNewVersionForApplication(applicationName, versionName, e);
        }
      } finally {
        try {
          con.closeConnection();
        } catch (PersistenceLayerException e) {
          logger.warn("Could not close connection.", e);
        }
      }

      try {
        //RuntimeContextDependencies kopieren
        Workspace parentWorkspace = revisionManagement.getWorkspace(parentRevision);
        copyRuntimeContextRequirements(new ApplicationDefinition(applicationName, parentWorkspace), new Application(applicationName,
                                                                                                                    versionName));
        
        //OrderInputSources kopieren (muss vor Deployment gemacht werden)
        copyInputSources(appEntriesForAllDependencies, parentRevision, revision);

      //Objekte im DeploymentItemStateManagement registrieren
        DeploymentItemStateManagement dism =
            XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement();
        dism.discoverItems(revision);
        
        //gleiche xmomdosname mappings wie im workspace verwenden
        copyXMOMOdsNameMapping(appEntriesForAllDependencies, parentRevision, revision);
        
        redeploy(revision, appEntriesForAllDependencies, DeploymentMode.reloadWithXMOMDatabaseUpdate, true, statusOutputStream, "Build Application");
        //damit beim Factory-Neustart die Objekte wieder deployed werden, müssen sie in WorkflowDatabase persistiert werden
        WorkflowDatabase wdb = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getWorkflowDatabase();
        wdb.persistDeployedObjects();

        copyCapacityMappingsMonitoringLevelsOrdertypesAndPriorities(appEntriesForAllDependencies, parentRevision, revision, verbose,
                                                                    statusOutputStream);

        copyFormDefinitions(appEntriesForAllDependencies, parentRevision, revision, null, verbose, statusOutputStream);

        transferFromODSTypeToODSType(ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY, ApplicationEntryStorable.TABLE_NAME,
                                     ApplicationEntryStorable.class);
        transferFromODSTypeToODSType(ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY, ApplicationStorable.TABLE_NAME,
                                     ApplicationStorable.class);

        copyTriggerAndFilterAndInstances(appEntriesForAllDependencies, parentRevision, revision, true, new EmptyRepositoryEvent());

        if (xmomAccess != null) {
          ApplicationXmlEntry applicationXmlEntry = createApplicationXmlEntry(applicationName, versionName, revision, null, false, false);

          //TODO RuntimeContextDependencies in applicationXmlEntry aufnehmen

          //aktuelle Konfiguration speichern
          xmomAccess.saveAndCommitApplicationConfiguration(applicationName, applicationXmlEntry, "application definition modified.");

          //Branch erstellen
          String branch =
              xmomAccess.createBranch(branchName, "build new version '" + versionName + "' of application '" + applicationName + "'");

          //neue RepositoryAccessInstance anlegen (branchName als repositoryAccessInstanceName verwenden)
          instantiateRepositoryAccess(xmomAccess.getRepositoryAccess(), branchName, branch, revision);
        }

      } catch (Throwable t) {
        Department.handleThrowable(t);
        if (revision != null) {
          try {
            RemoveApplicationParameters removeParams = new RemoveApplicationParameters();
            removeParams.setExtraForce(true);
            removeParams.setForce(true);
            removeParams.setRemoveIfUsed(true);
            removeApplicationVersion(applicationName, versionName, removeParams, new SingleRepositoryEvent(parentRevision));
            logger.info("Removed version <" + applicationName + ", " + versionName + "> because it could not be created successfully.");
          } catch (Throwable e1) {
            logger.warn("Rollback failed. Application and version could not be deleted.", e1);
          }
        }
        if (t instanceof XFMG_CouldNotBuildNewVersionForApplication) {
          throw (XFMG_CouldNotBuildNewVersionForApplication) t;
        }
        throw new XFMG_CouldNotBuildNewVersionForApplication(applicationName, versionName, t);
      }

      //Multi-User-Event für RuntimeContext Änderung
      Publisher publisher = new Publisher(params.getUser());
      publisher.publishRuntimeContextCreate(new Application(applicationName, versionName));
    } finally {
      buildLock.unlock();
    }
    
    for (RuntimeContextChangeHandler rdcch : getRtCtxMgmt().getHandlers()) {
      try {
        rdcch.creation(new Application(applicationName, versionName));
      } catch (Throwable t) {
        logger.warn("Could not execute RuntimeContextChangeHandler " + rdcch, t);
      }
    }
  }


  private void copyXMOMOdsNameMapping(TreeSet<ApplicationEntryStorable> appEntriesForAllDependencies, Long source, Long target) {
    XMOMPersistenceManagement xmomPersistenceManagment = XynaFactory.getInstance().getXynaNetworkWarehouse().getXMOMPersistence().getXMOMPersistenceManagement();
    Set<String> fqDatatypes = new HashSet<String>();
    for (ApplicationEntryStorable aes : appEntriesForAllDependencies) {
      if (aes.getTypeAsEnum() == ApplicationEntryType.DATATYPE) {
        fqDatatypes.add(aes.getName());
      }
    }
    Collection<XMOMODSMapping> sourceMappings;
    try {
      sourceMappings = XMOMODSMappingUtils.getAllMappingsForRevision(source);
      for (XMOMODSMapping sourceMapping : sourceMappings) {
        if (fqDatatypes.contains(sourceMapping.getFqxmlname())) {
          ODSRegistrationParameter odsRP = new ODSRegistrationParameter(sourceMapping.getFqxmlname(),
                                                                        sourceMapping.getRevision(),
                                                                        sourceMapping.getFqpath(),
                                                                        sourceMapping.getTablename(),
                                                                        sourceMapping.getColumnname(),
                                                                        true);
          odsRP.adjustRevision(target);
          xmomPersistenceManagment.setODSName(odsRP);
          
        }
      }
    } catch (PersistenceLayerException | XNWH_InvalidXMOMStorablePathException | XNWH_StorableNotFoundException |
             XNWH_ODSNameMustBeUniqueException | XNWH_ODSNameChangedButNotDeployedException e) {
      throw new RuntimeException("Failed to copy ods mappings from revision " + source + " to " + target, e);
    }
  }


  private Set<String> getStorableWFs2(Set<ApplicationEntryStorable> appEntries, long workspaceRevision) {
    if (appEntries == null || appEntries.size() == 0) {
      return Collections.emptySet();
    }

    for (ApplicationEntryStorable aes : appEntries) {
      if (aes.getTypeAsEnum() == ApplicationEntryType.DATATYPE && aes.getName().equals(XMOMPersistenceManagement.STORABLE_BASE_CLASS)) {
        return collectStorableWFs(workspaceRevision);
      }
    }
    return Collections.emptySet();
  }


  private Set<String> getStorableWFs(Set<DependencyNode> processedDependencies, long workspaceRevision) {
    if (processedDependencies == null || processedDependencies.size() == 0) {
      return Collections.emptySet();
    }
    DependencyNode storableNode =
        dependencyRegister.getDependencyNode(XMOMPersistenceManagement.STORABLE_BASE_CLASS, DependencySourceType.DATATYPE, workspaceRevision);
    if (storableNode != null && processedDependencies.contains(storableNode)) {
      return collectStorableWFs(workspaceRevision);
    }
    return Collections.emptySet();
  }


  private Set<String> collectStorableWFs(long workspaceRevision) {
    DependencyNode deleteOrdertypeNode =
        dependencyRegister.getDependencyNode("xnwh.persistence.Delete", DependencySourceType.ORDERTYPE, workspaceRevision);
    DependencyNode queryOrdertypeNode =
        dependencyRegister.getDependencyNode("xnwh.persistence.Query", DependencySourceType.ORDERTYPE, workspaceRevision);
    DependencyNode storeOrdertypeNode =
        dependencyRegister.getDependencyNode("xnwh.persistence.Store", DependencySourceType.ORDERTYPE, workspaceRevision);
    Set<String> ret = new HashSet<String>();
    if (deleteOrdertypeNode != null) {
      ret.add(deleteOrdertypeNode.getUniqueName());
    }
    if (queryOrdertypeNode != null) {
      ret.add(queryOrdertypeNode.getUniqueName());
    }
    if (storeOrdertypeNode != null) {
      ret.add(storeOrdertypeNode.getUniqueName());
    }
    return ret;
  }


  private String replaceSpecialCharacters(String s) {
    return s.replaceAll("[^a-zA-Z0-9_]", "_");
  }


  private String getBranchName(String applicationName, String versionName) {
    return replaceSpecialCharacters(applicationName) + "_" + replaceSpecialCharacters(versionName);
  }


  private RepositoryAccess instantiateRepositoryAccess(RepositoryAccess oldRepositoryAccess, String newRepositoryAccessInstanceName,
                                                       String newPath, Long newRevision) throws XDEV_CodeAccessInitializationException,
      StringParameterParsingException {
    //Parameter aus oldRepositoryAccess übernehmen und den ServerPath auf newPath ändern
    Map<String, Object> paramMap = new HashMap<String, Object>(oldRepositoryAccess.getParamMap());

    // "svn://" + paramMap.get("serverName") + "/" abschneiden;
    if (newPath.startsWith("svn://")) { //FIXME hier darf man nicht davon ausgehen, dass es svn ist
      int index = 7 + String.valueOf(paramMap.get("serverName")).length();
      newPath = newPath.substring(index);
    } else {
      throw new RuntimeException("not supported");
    }

    paramMap.put("path", newPath);
    List<String> parameterList = new ArrayList<String>();
    for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
      parameterList.add(entry.getKey() + "=" + entry.getValue());
    }

    //neue RepositoryAccessInstance anlegen
    RepositoryAccess newRepositoryAccess =
        XynaFactory
            .getInstance()
            .getXynaDevelopment()
            .getXynaLibraryDevelopment()
            .getRepositoryAccessManagement()
            .instantiateRepositoryAccessInstance(newRepositoryAccessInstanceName, oldRepositoryAccess.getTypename(), parameterList,
                                                 newRevision);

    //application.xmls auschecken
    checkoutConfiguration(newRepositoryAccess);

    return newRepositoryAccess;
  }


  private List<RepositoryItemModification> checkoutConfiguration(RepositoryAccess repositoryAccess) {
    final RepositoryTransaction transaction = repositoryAccess.beginTransaction("checkout application configurations");
    try {
      List<RepositoryItemModification> modifiedFiles = null;
      try {
        modifiedFiles = transaction.checkout(new String[] {""}, repositoryAccess.getHeadVersion(), RecursionDepth.TARGET_ONLY);
        transaction.update(new String[] {Component.configuration.getRepositorySubFolder()}, repositoryAccess.getHeadVersion(),
                           RecursionDepth.FULL_RECURSION);
      } catch (XDEV_PathNotFoundException e) {
        // ok, dann muss das im svn erst angelegt werden.
        logger.info("Failed to checkout project head", e);
        return null;
      } catch (XDEV_RepositoryAccessException e) {
        logger.warn("Error during repository refresh", e);
        return null;
      }
      return modifiedFiles;
    } finally {
      try {
        transaction.endTransaction();
      } catch (XDEV_RepositoryAccessException e) {
        logger.warn("Failed to end transaction", e);
      }
    }
  }


  /**
   * Liefert die Subtypen der Output-Parameter der Services, deren Basistypen nicht in exludeSubtypesOf enthalten sind.
   * Falls in excludeSubtypesOf '*' enthalten ist, werden überhaupt keine Subtypen gesucht.
   * @param appEntries
   * @param excludeSubtypesOf
   * @return
   */
  private static Set<String> getSubTypesOfOutputVars(Long parentRevision, Collection<ApplicationEntryStorable> appEntries,
                                                     List<String> excludeSubtypesOf) {
    Set<String> subTypes = new HashSet<String>();

    List<String> exclude = new ArrayList<String>();

    if (excludeSubtypesOf != null) {
      exclude.addAll(excludeSubtypesOf);
    }

    //Für die in der XynaProperty 'xfmg.xfctrl.appmgmt.excludedsubtypesof' angegeben Basistypen
    //sollen ebenfalls keine Subtypen mitgenommen werden
    exclude.addAll(getPropertyAsList(excludedSubtypesOfProperties.get(parentRevision)));

    if (exclude.contains("*")) {
      return subTypes; //alle Subtypen werden ausgeschlossen
    }

    GenerationBaseCache cache = new GenerationBaseCache();
    Set<String> processedOutputs = new HashSet<String>();

    for (ApplicationEntryStorable entry : appEntries) {
      if (entry.getTypeAsEnum() == ApplicationEntryType.DATATYPE) {
        File deployedFile = new File(GenerationBase.getFileLocationForDeploymentStaticHelper(entry.getName(), parentRevision) + ".xml");
        if (!deployedFile.exists()) {
          continue; //Entry ist nicht deployed, daher müssen auch keine Subtypen bestimmt werden (implizite ApplicationEntries werden auf dem Deployed-Stand ermittelt)
        }
        try {
          DOM dom = DOM.getOrCreateInstance(entry.getName(), cache, parentRevision);
          dom.parseGeneration(true, false, false);
          //alle Operations bestimmen
          List<com.gip.xyna.xprc.xfractwfe.generation.Operation> operations = dom.getOperations();
          for (com.gip.xyna.xprc.xfractwfe.generation.Operation op : operations) {
            //alle Outputs bestimmen
            List<AVariable> outputVars = op.getOutputVars();
            for (AVariable outputVar : outputVars) {
              subTypes.addAll(getSubTypesRecursively(outputVar, exclude, processedOutputs, cache, parentRevision));
            }
          }
        } catch (XynaException e) {
          throw new RuntimeException("Could not get output-subtypes of operation " + entry.getName(), e);
        }
      }
    }
    return subTypes;
  }


  private static Set<String> getSubTypesRecursively(AVariable variable, List<String> excludeSubtypesOf, Set<String> processedOutputs,
                                                    GenerationBaseCache cache, Long parentRevision) {
    Set<String> subTypes = new HashSet<String>();

    if (processedOutputs.add(variable.getFQClassName())) {
      //Output wurde noch nicht bearbeitet, also nun die Subtypen bestimmen
      GenerationBase gb = variable.getDomOrExceptionObject();
      if (gb instanceof DOM) {
        if (!excludeSubtypesOf.contains(variable.getFQClassName())) {
          //Subtypen dieses Basistyps sollen bestimmt werden
          Set<GenerationBase> types = ((DOM) gb).getSubTypes(cache);
          for (GenerationBase subType : types) {
            if (subType.getRevision().equals(parentRevision)) {
              subTypes.add(subType.getOriginalFqName());
            }
          }
        }

        //für alle MemberVars ebenfalls die Subtypen bestimmen
        List<AVariable> memberVars = ((DOM) gb).getAllMemberVarsIncludingInherited();
        for (AVariable memberVar : memberVars) {
          subTypes.addAll(getSubTypesRecursively(memberVar, excludeSubtypesOf, processedOutputs, cache, parentRevision));
        }
      }
    }

    return subTypes;
  }


  private Set<String> getSubTypesOfOutputVars(String applicationName, String versionName, Long parentRevision, Set<DependencyNode> nodes,
                                              List<String> excludeSubtypesOf) {
    if (excludeSubtypesOf != null && excludeSubtypesOf.contains("*")) {
      return new HashSet<String>(); //alle Subtypen werden ausgeschlossen
    }

    List<ApplicationEntryStorable> appEntries = new ArrayList<ApplicationEntryStorable>();
    for (DependencyNode dependency : nodes) {
      if (dependency.getType().equals(DependencySourceType.DATATYPE)) {
        appEntries.add(ApplicationEntryStorable.create(applicationName, versionName, dependency.getUniqueName(),
                                                    convertXMOMTypeToApplicationEntryType(dependency)));
      }
    }

    return getSubTypesOfOutputVars(parentRevision, appEntries, excludeSubtypesOf);
  }


  private void checkPermGen(int cumulatedSizeOfJars, int cumulatedSizeOfXMOMClasses) {
    double predictedBytes =
        cumulatedSizeOfJars * PERMGEN_PREDICTION_JAR_FACTOR.get() + cumulatedSizeOfXMOMClasses * PERMGEN_PREDICTION_CLASS_FACTOR.get()
            + PERMGEN_PREDICTION_CONSTANT.get();
    final String classMetaSpaceName;
    if (ListsysteminfoImpl.getJavaVersion() >= 8) {
      classMetaSpaceName = "metaspace";
    } else {
      classMetaSpaceName = "permgen space";
    }
    int i = 0;
    long remainingPermGenSpace = ListsysteminfoImpl.getRemainingClassMetaDataSpace();
    while (predictedBytes >= remainingPermGenSpace) {
      if (i >= 10) {
        throw new RuntimeException("The predicted usage of " + classMetaSpaceName + " for the application (" + (int) predictedBytes
            + ") is more than there is available (" + remainingPermGenSpace + "). Increase " + classMetaSpaceName
            + " or reduce prediction factors in properties xyna.xfmg.xfctrl.appmgmt.permgen.*.");
      }
      System.gc();
      i++;
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
      }
      remainingPermGenSpace = ListsysteminfoImpl.getRemainingClassMetaDataSpace();
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Predicting " + (int) predictedBytes + " bytes for new application version. Remaining " + classMetaSpaceName
          + " space = " + remainingPermGenSpace + ".");
    }
  }


  private void checkOpenFiles(int predictedAdditionalOpenFiles) {
    if (logger.isDebugEnabled()) {
      logger.debug("Predicting " + predictedAdditionalOpenFiles + " additional open files in application. Checking open files.");
    }
    //TODO trigger könnte man auch noch mitzählen (offene ports)
    //     oder eine application könnte angeben, wieviele offene files sie maximal hat
    int possibleOpenFiles = ListsysteminfoImpl.getNumberOfPossibleOpenFiles();
    if (possibleOpenFiles > 0) {
      if (possibleOpenFiles < predictedAdditionalOpenFiles + OPEN_FILES_SAFETY_MARGIN.get()) {
        throw new RuntimeException("The number of open files is too high to be able to safely import this application. ("
            + possibleOpenFiles + " open files left)");
      }
    } else {
      logger.warn("could not check for open files");
    }
  }


  private DependencyNode getTriggerDependencyNodeForTriggerInstance(String triggerInstanceName, long revision)
      throws PersistenceLayerException {
    TriggerInstanceInformation triggerInstanceInformation =
        xynaActivationTrigger.getTriggerInstanceInformation(triggerInstanceName, revision);
    TriggerInformation ti;
    try {
      ti = xynaActivationTrigger.getTriggerInformation(triggerInstanceInformation.getTriggerName(), revision, true);
    } catch (XACT_TriggerNotFound e) {
      throw new RuntimeException(e);
    }
    try {
      return dependencyRegister.getDependencyNode(ti.getTriggerName(), DependencySourceType.TRIGGER,
                                                  revisionManagement.getRevision(ti.getRuntimeContext()));
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }
  }


  /**
   * @return (triggerinstancename, revision of triggerinstance, filter)
   */
  private Triple<String, Long, DependencyNode> getFilterDependencyNodeForFilterInstance(String filterInstanceName, long revision)
      throws PersistenceLayerException {
    FilterInstanceInformation filterInstanceInformation = xynaActivationTrigger.getFilterInstanceInformation(filterInstanceName, revision);
    FilterInformation fi;
    try {
      fi = xynaActivationTrigger.getFilterInformation(filterInstanceInformation.getFilterName(), revision, true);
    } catch (XACT_FilterNotFound e) {
      throw new RuntimeException(e);
    }
    TriggerInstanceInformation tii =
        xynaActivationTrigger.getTriggerInstanceInformation(filterInstanceInformation.getTriggerInstanceName(), revision, true);
    try {
      return new Triple<String, Long, DependencyNode>(filterInstanceInformation.getTriggerInstanceName(), tii.getRevision(),
                                                      dependencyRegister.getDependencyNode(fi.getFilterName(), DependencySourceType.FILTER,
                                                                                           revisionManagement.getRevision(fi
                                                                                               .getRuntimeContext())));
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }

  }


  private boolean isSameVersion(RuntimeContext runtimeContext, Long revision) {
    if (runtimeContext == null) {
      return revision.equals(RevisionManagement.REVISION_DEFAULT_WORKSPACE);
    }

    try {
      return runtimeContext.equals(revisionManagement.getRuntimeContext(revision));
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }
  }


  /**
   * fügt die trigger/filter in targetrevision hinzu, wobei die informationen über name, jars, fqclassname und sharedlibs, etc aus der sourcerevision
   * verwendet werden. die eigtl jars werden aber aus der targetrevision genommen, müssen also zu diesem zeitpunkt bereits existieren
   * @param copyFromWorkingSetToRevision true, falls von ws nach revision kopiert wird, false, wenn von revision nach ws kopiert wird.
   */
  private void copyTriggerAndFilterAndInstances(Collection<? extends ApplicationEntryStorable> appEntries, long sourceRevision,
                                                long targetRevision, boolean copyFromWorkingSetToRevision, RepositoryEvent repositoryEvent)
      throws Ex_FileAccessException, PersistenceLayerException, XACT_DuplicateTriggerDefinitionException {
    Map<ApplicationEntryType, ArrayList<ApplicationEntryStorable>> group =
        CollectionUtils.group(appEntries, new Transformation<ApplicationEntryStorable, ApplicationEntryType>() {

          public ApplicationEntryType transform(ApplicationEntryStorable from) {
            return from.getTypeAsEnum();
          }
        });

    if (group.get(ApplicationEntryType.TRIGGER) != null) {
      for (ApplicationEntryStorable triggerEntry : group.get(ApplicationEntryType.TRIGGER)) {
        String triggerName = triggerEntry.getName();
        Trigger trigger;
        try {
          trigger = xynaActivationTrigger.getTrigger(sourceRevision, triggerName, false);
        } catch (XACT_TriggerNotFound e1) {
          throw new RuntimeException(e1);
        }
        boolean disableIfTriggerInstanceProblems = true;
        try {
          xynaActivationTrigger.addTrigger(triggerName,
                                           createTriggerFilterJarsInRevision(trigger.getJarFiles(), PathType.TRIGGER, sourceRevision,
                                                                             targetRevision, copyFromWorkingSetToRevision, triggerName,
                                                                             triggerName), trigger.getFQTriggerClassName(), trigger
                                               .getSharedLibs(), targetRevision, false, disableIfTriggerInstanceProblems, repositoryEvent);

          if (logger.isDebugEnabled()) {
            logger.debug("Added trigger " + triggerEntry.getName() + " in revision " + targetRevision);
          }
        } catch (XPRC_ExclusiveDeploymentInProgress e) {
          throw new RuntimeException(e); //darf nicht vorkommen
        } catch (XACT_IncompatibleTriggerImplException e) {
          throw new RuntimeException(e); //instanz fehler -> kann nicht vorkommen, weil instanz disabled werden sollte.
        } catch (XACT_TriggerImplClassNotFoundException e) {
          throw new RuntimeException(e); //in sourceRevision hats ja auch funktioniert
        } catch (XFMG_SHARED_LIB_NOT_FOUND e) {
          throw new RuntimeException(e); //in sourceRevision hats ja auch funktioniert und sharedlib wurde vorher ausgetauscht
        } catch (XACT_LibOfTriggerImplNotFoundException e) {
          throw new RuntimeException(e); //die wurden ja gerade erst kopiert
        } catch (XPRC_XmlParsingException e) {
          throw new RuntimeException(e); //in sourceRevision hats ja auch funktioniert
        } catch (XPRC_InvalidXmlMissingRequiredElementException e) {
          throw new RuntimeException(e); //in sourceRevision hats ja auch funktioniert
        } catch (XACT_JarFileUnzipProblem e) {
          throw new RuntimeException(e); //die wurden ja gerade erst kopiert
        }
      }
    }

    if (group.get(ApplicationEntryType.TRIGGERINSTANCE) != null) {
      for (ApplicationEntryStorable triggerInstanceEntry : group.get(ApplicationEntryType.TRIGGERINSTANCE)) {
        //finde und kopiere zugehörige triggerinstanceinformation
        TriggerInstanceInformation triggerinstanceinformation =
            xynaActivationTrigger.getTriggerInstanceInformation(triggerInstanceEntry.getName(), sourceRevision);
        if (triggerinstanceinformation == null) {
          throw new RuntimeException("Trigger instance " + triggerInstanceEntry.getName() + " not found");
        }
        try {
          xynaActivationTrigger.deployTriggerDisabled(triggerinstanceinformation.getTriggerName(), triggerinstanceinformation
              .getTriggerInstanceName(), triggerinstanceinformation.getStartParameter().toArray(new String[0]), triggerinstanceinformation
              .getDescription(), targetRevision);
          if (logger.isDebugEnabled()) {
            logger.debug("Deployed trigger instance " + triggerInstanceEntry.getName() + " in revision " + targetRevision);
          }
          Pair<Long, Boolean> triggerConfiguration =
              xynaActivationTrigger.getTriggerConfiguration(triggerinstanceinformation);
          if (triggerConfiguration.getFirst() != null && triggerConfiguration.getSecond() != null) {
            xynaActivationTrigger.createConfigureTriggerMaxEventsSetting(triggerinstanceinformation.getTriggerInstanceName(),
                                                                         triggerConfiguration.getFirst(), triggerConfiguration.getSecond(),
                                                                         targetRevision);
          }
        } catch (XACT_TriggerNotFound e) {
          throw new RuntimeException(e); //trigger wurde gerade geaddet
        }
      }

    }

    if (group.get(ApplicationEntryType.FILTER) != null) {
      for (ApplicationEntryStorable filterEntry : group.get(ApplicationEntryType.FILTER)) {
        String filterName = filterEntry.getName();
        Filter filter;
        try {
          filter = xynaActivationTrigger.getFilter(sourceRevision, filterName, false);
        } catch (XACT_FilterNotFound e1) {
          throw new RuntimeException(e1);
        }

        // Filter gefunden ... mit neuer Revision adden
        try {
          xynaActivationTrigger.addFilter(filterName,
                                          createTriggerFilterJarsInRevision(filter.getJarFiles(), PathType.FILTER, sourceRevision,
                                                                            targetRevision, copyFromWorkingSetToRevision, filterName,
                                                                            filterName), filter.getFQFilterClassName(), filter
                                              .getTriggerName(), filter.getSharedLibs(), filter.getDescription(), targetRevision, false,
                                          true, repositoryEvent);

          if (logger.isDebugEnabled()) {
            logger.debug("Added filter " + filterName + " in revision " + targetRevision);
          }
        } catch (XPRC_ExclusiveDeploymentInProgress e) {
          throw new RuntimeException(e); //darf nicht passieren
        } catch (XACT_FilterImplClassNotFoundException e) {
          throw new RuntimeException(e);//hatte vorher auch funktioniert
        } catch (XACT_TriggerNotFound e) {
          throw new RuntimeException(e); //darf nicht passieren
        } catch (XPRC_XmlParsingException e) {
          throw new RuntimeException(e);//hatte vorher auch funktioniert
        } catch (XPRC_InvalidXmlMissingRequiredElementException e) {
          throw new RuntimeException(e);//hatte vorher auch funktioniert
        } catch (XACT_AdditionalDependencyDeploymentException e) {
          throw new RuntimeException(e);//in sourceRevision hats ja auch funktioniert. sollte auch schon alles deployed sein
        } catch (XACT_JarFileUnzipProblem e) {
          throw new RuntimeException(e);//hatte vorher auch funktioniert
        } catch (XACT_LibOfFilterImplNotFoundException e) {
          throw new RuntimeException(e);//instanzen werden disabled!
        } catch (XFMG_SHARED_LIB_NOT_FOUND e) {
          throw new RuntimeException(e);//instanzen werden disabled!
        }
      }
    }

    if (group.get(ApplicationEntryType.FILTERINSTANCE) != null) {
      for (ApplicationEntryStorable filterInstanceEntry : group.get(ApplicationEntryType.FILTERINSTANCE)) {
        FilterInstanceInformation filterinstanceinformation =
            xynaActivationTrigger.getFilterInstanceInformation(filterInstanceEntry.getName(), sourceRevision);
        if (filterinstanceinformation == null) {
          throw new RuntimeException("Filter instance " + filterInstanceEntry.getName() + " not found");
        }
        try {
          DeployFilterParameter dfp = new DeployFilterParameter.Builder().
              filterName(filterinstanceinformation.getFilterName()).instanceName(filterinstanceinformation.getFilterInstanceName()).
              triggerInstanceName(filterinstanceinformation.getTriggerInstanceName()).description(filterinstanceinformation.getDescription()).
              optional(false).configuration(filterinstanceinformation.getConfiguration()).
              revision(targetRevision).build();
        
          xynaActivationTrigger.deployFilterDisabled(dfp);
          if (logger.isDebugEnabled()) {
            logger.debug("Deployed filter instance " + filterInstanceEntry.getName() + " in revision " + targetRevision);
          }

        } catch (XACT_FilterNotFound e) {
          throw new RuntimeException(e);
        }
      }
    }
  }


  private void renameSavedTriggerDir(String oldName, String newName, PathType type, long revision) {
    String saved = RevisionManagement.getPathForRevision(type, revision, false) + Constants.FILE_SEPARATOR;
    String oldDir = saved + oldName;
    String newDir = saved + newName;
    if (!new File(oldDir).renameTo(new File(newDir))) {
      throw new RuntimeException("could not move '" + oldDir + "' to '" + newDir);
    }
  }


  private File[] createTriggerFilterJarsInRevision(File[] jars, PathType pathType, long sourceRevision, long targetRevision,
                                                   boolean copyFromWorkingSetToRevision, String oldName, String newName) {
    File[] ret = new File[jars.length];
    for (int i = 0; i < ret.length; i++) {
      String relativePath;
      try {
        relativePath =
            FileUtils.getRelativePath(new File(RevisionManagement.getPathForRevision(pathType, sourceRevision, true)).getCanonicalPath(),
                                      jars[i].getCanonicalPath());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      if (!oldName.equals(newName)) {
        relativePath = relativePath.replaceFirst(Pattern.quote(oldName), Matcher.quoteReplacement(newName));
      }
      ret[i] = new File(RevisionManagement.getPathForRevision(pathType, targetRevision, copyFromWorkingSetToRevision), relativePath);
    }
    return ret;
  }


  //TODO
  private static class ThrowableScanner {

    /**
     * gibt ein ggfs reduziertes throwable zurück, welches exceptions/causes, denen vorher bereits begegnet wurde ersetzt durch
     * kürzere referenzierende causes.
     */
    public Throwable scan(Throwable t) {
      return t;
    }

  }


  private void redeploy(long revision, Collection<? extends ApplicationEntryStorable> appEntries, DeploymentMode deploymentMode,
                        boolean suppressErrors, PrintStream statusOutputStream, String comment) throws XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH,
      XPRC_InvalidPackageNameException, MDMParallelDeploymentException, XPRC_DeploymentDuringUndeploymentException {
    if (logger.isDebugEnabled()) {
      logger.debug("Start Redeploy of revision " + revision);
    }
    XMOMDatabase xdb = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getXMOMDatabase();
    boolean isWorkspace = revisionManagement.isWorkspaceRevision(revision);
    ThrowableScanner ts = new ThrowableScanner();
    Iterator<? extends ApplicationEntryStorable> iter = appEntries.iterator();
    List<GenerationBase> objects = new ArrayList<GenerationBase>();
    while (iter.hasNext()) {
      ApplicationEntryStorable app = iter.next();
      GenerationBase gb;
      switch (app.getTypeAsEnum()) {
        case DATATYPE :
          gb = DOM.getInstance(app.getName(), revision);
          break;
        case EXCEPTION :
          gb = ExceptionGeneration.getInstance(app.getName(), revision);
          break;
        case FORMDEFINITION :
          if (isWorkspace) {
            try {
              xdb.registerMOMObject(app.getName(), revision);
            } catch (PersistenceLayerException e) {
              String workspaceName;
              try {
                workspaceName = revisionManagement.getWorkspace(revision).getName();
              } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
                workspaceName = "unknown";
              }
              logger.warn("Could not register form " + app.getName() + " in workspace " + workspaceName, e);
            } catch (AssumedDeadlockException e) {
              throw new RuntimeException(e);
            }
          }
          continue;
        case WORKFLOW :
          gb = WF.getInstance(app.getName(), revision);
          objects.add(gb);
          continue;
        default :
          continue;
      }
      gb.setDeploymentComment(comment);
      objects.add(gb);
      //datatypes und exceptions rauswerfen, die benötigt man in späteren schritten nicht mehr
      iter.remove();
    }

    //alle Objekte parallel deployen
    try {
      GenerationBase.deploy(objects, deploymentMode, deploymentMode == DeploymentMode.codeChanged, WorkflowProtectionMode.FORCE_KILL_DEPLOYMENT);
    } catch (Throwable t) {
      Department.handleThrowable(t);
      handleThrowableOnDeployment(ts, t, revision, suppressErrors, statusOutputStream);
    }
  }


  private void handleThrowableOnDeployment(ThrowableScanner ts, Throwable t, long revision, boolean suppressErrors,
                                           PrintStream statusOutputStream) throws MDMParallelDeploymentException,
      XPRC_DeploymentDuringUndeploymentException {
    if (suppressErrors) {
      if (t instanceof MDMParallelDeploymentException) {
        MDMParallelDeploymentException ex = (MDMParallelDeploymentException) t;
        logger.error("Could not load " + ex.getNumberOfFailedObjects() + " xmomobjects, continuing ...");
        for (GenerationBase object : ex.getFailedObjects()) {
          logger.error("Could not load xmomobject " + object.getOriginalFqName() + " from revision " + object.getRevision(),
                       ts.scan(object.getExceptionCause()));
          if (object.getExceptionWhileOnError() != null) {
            logger.error("Errors occurred during cleanup of xmomobject " + object.getOriginalFqName(),
                         ts.scan(object.getExceptionWhileOnError()));
          }
          output(statusOutputStream, "WARN Could not deploy xmomobject " + object.getOriginalFqName() + ".");
          if (XynaFactory.getInstance().isStartingUp()) {
            XynaExtendedStatusManagement.addFurtherInformationAtStartup(DEFAULT_NAME,
                                                                        "Could not deploy xmomobject " + object.getOriginalFqName()
                                                                            + " @rev_" + object.getRevision());
          }
        }
      } else {
        logger.error("Could not load xmomobjects, continuing ...", ts.scan(t));
        output(statusOutputStream, "WARN Could not deploy xmomobjects.");
        if (XynaFactory.getInstance().isStartingUp()) {
          XynaExtendedStatusManagement.addFurtherInformationAtStartup(DEFAULT_NAME, "Could not deploy xmomobjects in revision " + revision);
        }
      }
    } else {
      if (t instanceof MDMParallelDeploymentException) {
        throw (MDMParallelDeploymentException) t;
      } else if (t instanceof XPRC_DeploymentDuringUndeploymentException) {
        throw (XPRC_DeploymentDuringUndeploymentException) t;
      } else if (t instanceof RuntimeException) {
        throw (RuntimeException) t;
      } else if (t instanceof Error) {
        throw (Error) t;
      } else {
        throw new RuntimeException("Undeclared exception from deployment!", t);
      }
    }
  }


  private void copyFileList(List<Pair<String, String>> copyList, boolean verbose, PrintStream statusOutputStream) throws XynaException {
    for (Pair<String, String> copyEntry : copyList) {
      File from = new File(copyEntry.getFirst());
      File to = new File(copyEntry.getSecond());
      if (from.exists()) {
        if (!from.isDirectory()) {
          to = to.getParentFile();  //FIXME ist das wirklich gewollt  cp a/file b/c/  ->  b/file statt b/c/file
        }
        to.getParentFile().mkdirs();
        if (logger.isDebugEnabled()) {
          logger.debug("Copy file from " + from + " to " + to);
        }
        FileUtils.copyRecursivelyWithFolderStructure(from, to);
        if (verbose) {
          output(statusOutputStream, "Create object " + copyEntry.getSecond());
        }
      }
    }
  }


  /*
   * Methode prüft Verzeichnis auf generierte innere Klassen (com.bla.Blubber$UnpraktsicheInnereKlasse.class)
   */
  private void checkInnerClasses(String fqName, String fileName, String pathToNewRevisionXMOMClasses, List<Pair<String, String>> copyList,
                                 PredictionValues predictionValues) throws XPRC_InvalidPackageNameException {

    File file = new File(fileName);
    File parent = file.getParentFile();
    String className = file.getName().substring(0, file.getName().lastIndexOf(".class"));
    File[] parentFileEntries = parent.listFiles();
    if (parentFileEntries != null) {
      for (File parentEntry : parentFileEntries) {
        if (parentEntry.getName().startsWith(className + "$")) {
          StringBuilder toFilename = new StringBuilder();
          toFilename.append(pathToNewRevisionXMOMClasses).append(Constants.fileSeparator)
              .append(GenerationBase.transformNameForJava(fqName).replaceAll("\\.", Constants.fileSeparator)).append(" inner classes");

          predictionValues.cumulatedSizeOfClasses += parentEntry.length();
          copyList.add(new Pair<String, String>(parentEntry.getPath(), toFilename.toString()));
        }
      }
    }
  }


  private void copyFilesToWorkingset(List<? extends ApplicationEntryStorable> appEntries, long revisionFrom, Long revisionTo,
                                     boolean overrideChanges, RevisionContentBlackWhiteList whiteList,
                                     boolean verbose, PrintStream statusOutputStream) throws XynaException {


    String toPathXMOMsaved = RevisionManagement.getPathForRevision(PathType.XMOM, revisionTo, false);
    String toPathXMOMdeployed = RevisionManagement.getPathForRevision(PathType.XMOM, revisionTo);
    String fromPathXMOM = RevisionManagement.getPathForRevision(PathType.XMOM, revisionFrom);
    String toPathXMOMClasses = RevisionManagement.getPathForRevision(PathType.XMOMCLASSES, revisionTo);
    String fromPathXMOMClasses = RevisionManagement.getPathForRevision(PathType.XMOMCLASSES, revisionFrom);

   
    copyFiles(appEntries, fromPathXMOM, toPathXMOMsaved, toPathXMOMdeployed, fromPathXMOMClasses, toPathXMOMClasses, revisionFrom,
                revisionTo, overrideChanges, whiteList, verbose, statusOutputStream);
  }


  /**
   * kopiert files von revisionFrom in revisionTo in den savedordner und checkt die whiteList
   * @param tsa 
   */
  private void copyFiles(List<? extends ApplicationEntryStorable> appEntries, String fromPathXMOM, String toPathXMOMSaved,
                         String toPathXMOMDeployed, String fromPathXMOMClasses, String toPathXMOMClasses, long revisionFrom,
                         long revisionTo, boolean overrideChanges, RevisionContentBlackWhiteList whiteList, 
                         boolean verbose, PrintStream statusOutputStream) throws XynaException {


    List<Pair<String, String>> copyList = new ArrayList<Pair<String, String>>();

    for (ApplicationEntryStorable appEntry : appEntries) {
      if (appEntry.getTypeAsEnum() == ApplicationEntryType.WORKFLOW || appEntry.getTypeAsEnum() == ApplicationEntryType.DATATYPE
          || appEntry.getTypeAsEnum() == ApplicationEntryType.EXCEPTION) {

        StringBuilder toFilename = new StringBuilder();
        toFilename.append(toPathXMOMSaved).append(Constants.fileSeparator)
            .append(appEntry.getName().replace('.', Constants.fileSeparator.charAt(0))).append(".xml");
        checkCollision(appEntry.getTypeAsEnum(), appEntry.getName(), toFilename.toString(), overrideChanges, whiteList, revisionTo);
        StringBuilder fromFilename = new StringBuilder();
        fromFilename.append(fromPathXMOM).append(Constants.fileSeparator)
            .append(appEntry.getName().replace('.', Constants.fileSeparator.charAt(0))).append(".xml");

        copyList.add(Pair.of(fromFilename.toString(), toFilename.toString()));

        if (appEntry.getTypeAsEnum() == ApplicationEntryType.DATATYPE) {
          // beim Datentypen auch Libraries kopieren
          DOM dom = DOM.generateUncachedInstance(appEntry.getName(), true, revisionFrom);

          Set<String> additionalLibs = new HashSet<String>();
          dom.getDependentJarsWithoutRecursion(additionalLibs, false, false);

          if( ! additionalLibs.isEmpty() ) {
            Pair<String, String> copy = copyFromDeployedToSaved(PathType.SERVICE, revisionFrom, revisionTo, GenerationBase.transformNameForJava(appEntry.getName()) );
            copyList.add( copy );
          }
        } // endif DATATYPE
        
        // endif WORKFLOW || DATATYPE || EXCEPTION
      } else if (appEntry.getTypeAsEnum() == ApplicationEntryType.SHAREDLIB) {
        Pair<String, String> copy = copyFromDeployedToSaved(PathType.SHAREDLIB, revisionFrom, revisionTo, appEntry.getName() );
        checkCollision(appEntry.getTypeAsEnum(), appEntry.getName(), copy.getSecond(), overrideChanges, whiteList, revisionTo);
        copyList.add( copy );
        //SharedLib-Verzeichnis leeren, damit nicht unterschiedliche Versionen eines JAR-Files
        //parallel vorhanden sind
        if (overrideChanges) {
          FileUtils.deleteDirectory(new File(copy.getSecond()) );
        }
      } else if (appEntry.getTypeAsEnum() == ApplicationEntryType.TRIGGER) {
        String simpleClassName = getSimpleTriggerClassName(appEntry.getName(), revisionFrom);
        Pair<String, String> copyD = copyFromDeployedToDeployed(PathType.TRIGGER, revisionFrom, revisionTo, simpleClassName, null );
        //für den kollisionscheck die deployed-lokation verwenden
        checkCollision(appEntry.getTypeAsEnum(), appEntry.getName(), copyD.getSecond(), overrideChanges, whiteList, revisionTo);
        //xmls müssen in die deployed-lokation kopiert werden (da nur hier bei addTrigger gesucht wird)
        copyList.addAll(getCopyXmlList(copyD.getFirst(), copyD.getSecond()));
        //nach saved kopieren
        Pair<String, String> copyS = copyFromDeployedToSaved(PathType.TRIGGER, revisionFrom, revisionTo, simpleClassName );
        copyList.add(copyS);
      } else if (appEntry.getTypeAsEnum() == ApplicationEntryType.FILTER) {
        String simpleClassName = getSimpleFilterClassName(appEntry.getName(), revisionFrom);
        Pair<String, String> copyD = copyFromDeployedToDeployed(PathType.FILTER, revisionFrom, revisionTo, simpleClassName, null );
        //für den kollisionscheck die deployed-lokation verwenden
        checkCollision(appEntry.getTypeAsEnum(), appEntry.getName(), copyD.getSecond(), overrideChanges, whiteList, revisionTo);
        //xmls müssen in die deployed-lokation kopiert werden (da nur hier bei addFilter gesucht wird)
        copyList.addAll(getCopyXmlList(copyD.getFirst(), copyD.getSecond()));
        //nach saved kopieren
        Pair<String, String> copyS = copyFromDeployedToSaved(PathType.FILTER, revisionFrom, revisionTo, simpleClassName );
        copyList.add(copyS);
      }
    } // end for

    copyFileList(copyList, verbose, statusOutputStream);
  }
  


  private Pair<String, String> copyFromDeployedToSaved(PathType pathType, long revisionFrom, long revisionTo,
                                                       String name) {
    Pair<String, String> copy = 
        Pair.of( RevisionManagement.getPathForRevision(pathType, revisionFrom) + Constants.fileSeparator + name,
                 RevisionManagement.getPathForRevision(pathType, revisionTo, false) + Constants.fileSeparator + name
        );
    return copy;
  }


  /**
   * Liefert eine "CopyList", um alle xmls aus sourceDir nach targetDir zu kopieren
   * @param sourceDir
   * @param targetDir
   * @return
   */
  private List<Pair<String, String>> getCopyXmlList(String sourceDir, String targetDir) {
    List<Pair<String, String>> copyXmlList = new ArrayList<Pair<String, String>>();
    List<File> xmlList = new ArrayList<File>();
    FileUtils.getMDMFiles(new File(sourceDir), xmlList);
    for (File xmlFile : xmlList) {
      StringBuilder toFilename = new StringBuilder();
      toFilename.append(targetDir).append(Constants.fileSeparator).append(xmlFile.getName());
      copyXmlList.add((new Pair<String, String>(xmlFile.getPath(), toFilename.toString())));
    }

    return copyXmlList;
  }


  private void checkCollision(ApplicationEntryType entryType, String entryName, String filepath, boolean overrideChanges,
                              RevisionContentBlackWhiteList whiteList, long revision) throws XynaException {
    if (!overrideChanges) {
      File to = new File(filepath);
      if (to.exists() && !overrideChanges && !isContainedInBlackWhiteList(entryName, entryType, whiteList)) {
        GenerationBase gb = null;
        if (entryType == ApplicationEntryType.WORKFLOW) {
          gb = WF.generateUncachedInstance(entryName, false, revision);
        } else if (entryType == ApplicationEntryType.DATATYPE) {
          gb = DOM.generateUncachedInstance(entryName, false, revision);
        } else if (entryType == ApplicationEntryType.EXCEPTION) {
          gb = ExceptionGeneration.generateUncachedInstance(entryName, false, revision);
        }
        if (gb != null && gb.isXynaFactoryComponent()) {
          // TODO remove from copy list? allthough it sould be the same object
        } else {
          throw new XFMG_ObjectAlreadyExists(entryType.toString(), entryName);
        }
      }
    }
  }


  private static boolean isContainedInBlackWhiteList(String name, ApplicationEntryType type, RevisionContentBlackWhiteList list) {
    switch (type) {
      case DATATYPE :
      case EXCEPTION :
      case WORKFLOW :
        return list.getXMOMObjects().contains(name);
      case SHAREDLIB :
        return list.getSharedLibs().contains(name);
      case FILTER :
        return list.getFilterNames().contains(name);
      case FILTERINSTANCE :
        return list.getFilterInstanceNames().contains(name);
      case TRIGGER :
        return list.getTriggersNames().contains(name);
      case TRIGGERINSTANCE :
        return list.getTriggerInstanceNames().contains(name);
      default :
        return false;
    }
  }


  private String getSimpleTriggerClassName(String triggerName, long revisionFrom) throws PersistenceLayerException {
    XynaActivationTrigger xynaActivationTrigger = XynaFactory.getInstance().getActivation().getActivationTrigger();
    Trigger[] triggers = xynaActivationTrigger.getTriggers(revisionFrom);
    for (Trigger trigger : triggers) {
      if (trigger.getTriggerName().equals(triggerName)) {
        return XynaActivationTrigger.getSimpleClassName(trigger.getFQTriggerClassName());
      }
    }
    throw new RuntimeException("trigger " + triggerName + " not found in revision " + revisionFrom);
  }


  private String getSimpleFilterClassName(String filterName, long revisionFrom) throws PersistenceLayerException {
    XynaActivationTrigger xynaActivationTrigger = XynaFactory.getInstance().getActivation().getActivationTrigger();
    try {
      Filter filter = xynaActivationTrigger.getFilter(revisionFrom, filterName, false);
      return XynaActivationTrigger.getSimpleClassName(filter.getFQFilterClassName());
    } catch (XACT_FilterNotFound e) {
      throw new RuntimeException("filter " + filterName + " not found in revision " + revisionFrom);
    }
  }


  private static class JarFilter implements FilenameFilter {

    private final PredictionValues predictionValues;


    public JarFilter(PredictionValues predictionValues) {
      this.predictionValues = predictionValues;
    }


    public boolean accept(File dir, String name) {
      File f = new File(dir, name);
      if (f.isDirectory()) {
        return true;
      }
      boolean ret = name.endsWith(".jar");
      if (ret) {
        predictionValues.cumulatedSizeOfJars += f.length();
        return true;
      }
      return false;
    }
  };


  private void copy(String uniqueName, DependencySourceType type, String fromPathXMOM, String toPathXMOMSaved, String toPathXMOMDeployed,
                    String fromPathXMOMClasses, String toPathXMOMClasses, long revisionFrom, long revisionTo, boolean verbose,
                    PrintStream statusOutputStream, final PredictionValues predictionValues) throws XynaException {

    List<Pair<String, String>> copyList = new ArrayList<Pair<String, String>>();

    if (type == DependencySourceType.WORKFLOW || type == DependencySourceType.DATATYPE || type == DependencySourceType.XYNAEXCEPTION) {

      String toFilename;
      String fromFilename = GenerationBase.getFileLocationForDeploymentStaticHelper(uniqueName, revisionFrom) + ".xml";

      if (toPathXMOMSaved != null) {
        toFilename = toPathXMOMSaved + Constants.fileSeparator + uniqueName.replaceAll("\\.", Constants.fileSeparator) + ".xml";
        copyList.add(Pair.of(fromFilename, toFilename));
      }

      if (toPathXMOMDeployed != null) {
        toFilename = toPathXMOMDeployed + Constants.fileSeparator + uniqueName.replaceAll("\\.", Constants.fileSeparator) + ".xml";
        copyList.add(Pair.of(fromFilename, toFilename));
      }

      if (toPathXMOMClasses != null && !GenerationBase.isReservedServerObjectByFqOriginalName(uniqueName)) {
        
        toFilename =   toPathXMOMClasses   + Constants.fileSeparator + GenerationBase.transformNameForJava(uniqueName).replaceAll("\\.", Constants.fileSeparator) + ".class";
        fromFilename = fromPathXMOMClasses + Constants.fileSeparator + GenerationBase.transformNameForJava(uniqueName).replaceAll("\\.", Constants.fileSeparator) + ".class";
        File f = new File(fromFilename);
        if (f.exists()) {
          predictionValues.cumulatedSizeOfClasses += f.length();
          copyList.add(Pair.of(fromFilename, toFilename));
        }
        checkInnerClasses(uniqueName, fromFilename, toPathXMOMClasses, copyList, predictionValues);
      }

      if (type == DependencySourceType.DATATYPE && !GenerationBase.isReservedServerObjectByFqOriginalName(uniqueName)) {
        // beim Datentypen auch Libraries und Lizenzen kopieren
        DOM dom = DOM.generateUncachedInstance(uniqueName, true, revisionFrom);

        Set<String> additionalLibs = new HashSet<String>();
        dom.getDependentJarsWithoutRecursion(additionalLibs, false, false);

        String serviceName = dom.getFqClassName();
        for (String addLib : additionalLibs) {
          File jar = new File(addLib);
          String simpleJarFileName = jar.getName();
          toFilename = fileNameDeployed(PathType.SERVICE, revisionTo, serviceName, simpleJarFileName );
          copyList.add(Pair.of(jar.getAbsolutePath(), toFilename));
          predictionValues.openFiles++;
          predictionValues.cumulatedSizeOfJars += jar.length();
          
          
          //Evtl. gibt es noch Lizenzen dazu
          String fromDir = jar.getParent();
          String toDir = fileNameDeployed(PathType.SERVICE, revisionTo, serviceName );
          List<Pair<String, String>> copies = ThirdPartyHandling.copyLicensesForJar( fromDir, simpleJarFileName, toDir, revisionTo);
          copyList.addAll( copies );
          predictionValues.openFiles += copies.size();
        }
        
        //alle nicht-jarfiles kopieren
        List<File> files = new ArrayList<File>();
        String serviceBaseDir = fileNameDeployed(PathType.SERVICE, revisionFrom, serviceName);
        FileUtils.findFilesRecursively(new File(serviceBaseDir), files, new FilenameFilter() {

          public boolean accept(File dir, String name) {
            if (name.endsWith(".jar")) {
              return false;
            }
            return true;
          }
          
        });
        String serviceToDir = fileNameDeployed(PathType.SERVICE, revisionTo, serviceName);
        for (File f : files) {
          String from = f.getAbsolutePath();
          String to = serviceToDir + Constants.FILE_SEPARATOR + FileUtils.getRelativePath(new File(serviceBaseDir).getAbsolutePath(), from);
          copyList.add(Pair.of(from, to));
        }
        
      } // endif DATATYPE
      // endif WORKFLOW || DATATYPE || EXCEPTION
    } else if (type == DependencySourceType.SHAREDLIB) {
      Pair<String, String> copy = copyFromDeployedToDeployed(PathType.SHAREDLIB, revisionFrom, revisionTo, uniqueName, predictionValues );
      copyList.add(copy);
      //Evtl. gibt es noch Lizenzen dazu
      List<Pair<String, String>> copies = ThirdPartyHandling.copyLicensesForDir( copy.getFirst(), revisionTo);
      copyList.addAll( copies );
    } else if (type == DependencySourceType.TRIGGER) {
      String simpleClassName = getSimpleTriggerClassName(uniqueName, revisionFrom);
      Pair<String, String> copy = copyFromDeployedToDeployed(PathType.TRIGGER, revisionFrom, revisionTo, simpleClassName, predictionValues );
      copyList.add(copy);
      //Evtl. gibt es noch Lizenzen dazu
      List<Pair<String, String>> copies = ThirdPartyHandling.copyLicensesForDir( copy.getFirst(), revisionTo);
      copyList.addAll( copies );
    } else if (type == DependencySourceType.FILTER) {
      String simpleClassName = getSimpleFilterClassName(uniqueName, revisionFrom);
      Pair<String, String> copy = copyFromDeployedToDeployed(PathType.FILTER, revisionFrom, revisionTo, simpleClassName, predictionValues );
      copyList.add(copy);
      //Evtl. gibt es noch Lizenzen dazu
      List<Pair<String, String>> copies = ThirdPartyHandling.copyLicensesForDir( copy.getFirst(), revisionTo);
      copyList.addAll( copies );
    }
    
    copyFileList(copyList, verbose, statusOutputStream);
  }

  private String fileNameDeployed(PathType pathType, long revision, String ... pathAndFile ) {
    StringBuilder sb = new StringBuilder( RevisionManagement.getPathForRevision(pathType, revision) );
    for( String paf : pathAndFile ) {
      sb.append(Constants.fileSeparator).append(paf);
    }
    return sb.toString();
  }


  private Pair<String, String> copyFromDeployedToDeployed(PathType pathType, long revisionFrom, long revisionTo, 
                                                          String name, PredictionValues predictionValues) {
    Pair<String, String> copy = 
        Pair.of( RevisionManagement.getPathForRevision(pathType, revisionFrom) + Constants.fileSeparator + name,
                 RevisionManagement.getPathForRevision(pathType, revisionTo) + Constants.fileSeparator + name
        );
    if( predictionValues != null ) {
      int cnt = FileUtils.countFilesRecursively(new File(copy.getFirst()), new JarFilter(predictionValues));
      predictionValues.openFiles += cnt;
    }
    return copy;
  }


  private void copyApplicationEntriesToNewVersion(List<? extends ApplicationEntryStorable> appEntries, String newVersionName,
                                                  ODSConnection con) throws PersistenceLayerException {

    for (ApplicationEntryStorable appEntry : appEntries) {
      ApplicationEntryStorable newEntry =
          ApplicationEntryStorable.toStore(appEntry.getApplication(), newVersionName, appEntry.getName(), appEntry.getTypeAsEnum());
      con.persistObject(newEntry);
    }
  }


  /**
   * Liefert true, wenn das übergebene Objekt "deployed" ist.
   */
  private boolean isObjectDeployed(String uniqueName, DependencySourceType sourceType, Long revision)
      throws XPRC_InvalidPackageNameException, PersistenceLayerException {
    if (GenerationBase.isReservedServerObjectByFqOriginalName(uniqueName)) {
      //ReservedServerObjects sind deployed
      return true;
    }

    switch (sourceType) {
    //XMOM, Trigger/Filter und SharedLib sind "deployed" wenn ein ClassLoader existiert
      case WORKFLOW :
        return classLoaderDispatcher.getClassLoaderByType(ClassLoaderType.WF, GenerationBase.transformNameForJava(uniqueName), revision) != null;
      case XYNAEXCEPTION :
        return classLoaderDispatcher.getClassLoaderByType(ClassLoaderType.Exception, GenerationBase.transformNameForJava(uniqueName),
                                                          revision) != null;
      case DATATYPE :
        return classLoaderDispatcher.getClassLoaderByType(ClassLoaderType.MDM, GenerationBase.transformNameForJava(uniqueName), revision) != null;
      case FILTER :
        String fqFilterClassName = xynaActivationTrigger.getFqFilterClassName(uniqueName, revision);
        if (fqFilterClassName == null) {
          return false; //Filter existiert nicht
        }
        return classLoaderDispatcher.getClassLoaderByType(ClassLoaderType.Filter, fqFilterClassName, revision) != null;
      case TRIGGER :
        Trigger trigger;
        try {
          trigger = xynaActivationTrigger.getTrigger(revision, uniqueName, false);
        } catch (XACT_TriggerNotFound e) {
          return false; //Trigger existiert nicht
        }
        return classLoaderDispatcher.getClassLoaderByType(ClassLoaderType.Trigger, trigger.getFQTriggerClassName(), revision) != null;
      case SHAREDLIB :
        return classLoaderDispatcher.getClassLoaderByType(ClassLoaderType.SharedLib, uniqueName, revision) != null;
      case XYNAPROPERTY :
        //XynaProperties sind immer "deployed"
        return true;
      default :
        //andere Objekte (vor allem OrderTypes) werden als "deployed" angesehen, wenn sie im DependencyRegister existieren
        return dependencyRegister.getDependencyNode(uniqueName, sourceType, revision) != null;
    }
  }


  private void checkDeploymentItemState(String uniqueName, XMOMType xmomType, Long revision, boolean throwExceptionIfInvalid)
      throws XFMG_WrongDeploymentState, XFMG_ObjectUnkownInDeploymentItemStateManagement {
    if (!XynaProperty.CHECK_DEPLOYMENT_STATE_FOR_BUILD_APPLICATION.get()) {
      return;
    }
    if (xmomType == null || xmomType == XMOMType.FORM || GenerationBase.isReservedServerObjectByFqOriginalName(uniqueName)) {
      return;
    }

    DeploymentItemStateManagement dism =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement();
    DeploymentItemState dis = dism.get(uniqueName, revision);

    boolean invalid;
    if (dis == null) {
      invalid = true;
    } else {
      Set<DeploymentItemInterface> invalid_dd = dis.getInconsistencies(DeploymentLocation.DEPLOYED, DeploymentLocation.DEPLOYED, false);

      //liegen deployed Inkonsistenzen vor?
      invalid = (invalid_dd.size() > 0);

      if (!invalid) {
        //oder eine ServiceImplInkonsistenz?
        invalid = dis.hasServiceImplInconsistencies(DeploymentLocation.DEPLOYED, true);
      }
    }

    if (invalid) {
      if (throwExceptionIfInvalid) {
        if (dis == null) {
          throw new XFMG_ObjectUnkownInDeploymentItemStateManagement(uniqueName, xmomType.toString());
        } else {
          throw new XFMG_WrongDeploymentState(uniqueName, xmomType.toString(), DisplayState.INVALID.toString());
        }
      } else {
        logger.warn(xmomType + " " + uniqueName + " is in state " + DisplayState.INVALID);
      }
      return;
    }

    DisplayState state = dis.deriveDisplayState();
    if (state == DisplayState.INVALID || state == DisplayState.CHANGED || state == DisplayState.INCOMPLETE) {
      logger.warn(xmomType + " " + uniqueName + " is in state " + state);
    }
  }


  //übergebenes objekt ist hier bereits gelockt
  private void recursiveLockAndCopyDependencies(final String uniqueName, final DependencySourceType sourceType, Long fromRevision,
                                                Long toRevision, boolean verbose, PrintStream statusOutputStream,
                                                Set<DependencyNode> processedDependencies, PredictionValues predictionValues,
                                                Set<String> processedOrderInputSources, Set<ApplicationEntryStorable> reqAppEntries)
      throws XynaException {
    final Set<DependencyNode> depNodes = new HashSet<DependencyNode>();
    try {
      if (!isObjectDeployed(uniqueName, sourceType, fromRevision)) {
        throw new XFMG_ObjectNotDeployed(uniqueName, sourceType.toString());
      }
      DependencyNode eigenNode = dependencyRegister.getDependencyNode(uniqueName, sourceType, fromRevision);
      if (eigenNode == null) {
        //sollte eigentlich nicht vorkommen (außer bei XynaProperties) da vorher überprüft wurde, dass
        //die Objekte deployed sind.
        if (sourceType == DependencySourceType.XYNAPROPERTY) {
          return; //ntbd
        } else {
          logger.warn("did not find dependency node " + uniqueName + " of type " + sourceType + " in revision " + fromRevision);
          //FIXME das ist gefährlich, weil jetzt das unlock passiert, aber das objekt als nicht processed gilt. d.h. es kann für dieses objekt zu einem späteren zeitpunkt nochmal diese methode aufgerufen werden
        }
      } else {
        //Objekte, die explizit oder implizit in einer verwendeten Application Definition enthalten sind, dürfen nicht kopiert werden
        if (reqAppEntries.contains(ApplicationEntryStorable.create(null, null, uniqueName, convertXMOMTypeToApplicationEntryType(eigenNode)))) { //applicationName und versionName sind hier egal, da nur name und type verglichen werden
          return;
        }

        if (!processedDependencies.add(eigenNode)) {
          //für die nicht rekursiven aufrufe dieser methode
          return;
        }
      }

      XMOMType xmomType = convertDependencySourceTypeToXMOMType(sourceType);
      checkDeploymentItemState(uniqueName, xmomType, fromRevision, true);


      String toPathXMOM = RevisionManagement.getPathForRevision(PathType.XMOM, toRevision);
      String fromPathXMOM = RevisionManagement.getPathForRevision(PathType.XMOM, fromRevision);
      String toPathXMOMClasses = RevisionManagement.getPathForRevision(PathType.XMOMCLASSES, toRevision);
      String fromPathXMOMClasses = RevisionManagement.getPathForRevision(PathType.XMOMCLASSES, fromRevision);

      copy(uniqueName, sourceType, fromPathXMOM, null, toPathXMOM, fromPathXMOMClasses, toPathXMOMClasses, fromRevision, toRevision,
           verbose, statusOutputStream, predictionValues);

      //referenzierte OrderInputSources ermitteln
      if (sourceType == DependencySourceType.ORDERTYPE) {
        ExecutionDispatcher executionDispatcher =
            XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution().getExecutionEngineDispatcher();
        OrderInputSourceManagement oism =
            XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderInputSourceManagement();

        RuntimeContext oldRuntimeContext = revisionManagement.getRuntimeContext(fromRevision);
        DestinationKey dk = new DestinationKey(uniqueName, oldRuntimeContext);
        DestinationValue dv = executionDispatcher.getDestination(dk);

        List<String> ois = oism.getReferencedOrderInputSources(dv, fromRevision);
        for (String inputSource : ois) {
          recursiveGetInputSourceDependencies(inputSource, fromRevision, toRevision, verbose, statusOutputStream, processedDependencies,
                                              predictionValues, processedOrderInputSources, reqAppEntries);
        }
      }

      Set<DependencyNode> allUsedNodes = dependencyRegister.getAllUsedNodesSameRevision(uniqueName, sourceType, false, false, fromRevision);

      if (sourceType == DependencySourceType.WORKFLOW) {
        String ot = GenerationBase.transformNameForJava(uniqueName);
        DependencyNode otNode = dependencyRegister.getDependencyNode(ot, DependencySourceType.ORDERTYPE, fromRevision);
        if (otNode != null) {
          if (depNodes.add(otNode)) {
            DeploymentLocks.readLock(ot, DependencySourceType.ORDERTYPE, "BuildApplication", fromRevision);
          }
        }
      }

      for (DependencyNode node : allUsedNodes) {
        if (!processedDependencies.contains(node)) {
          if (depNodes.add(node)) {
            DeploymentLocks.readLock(node.getUniqueName(), node.getType(), "BuildApplication", fromRevision);
            if (node.getType() == DependencySourceType.WORKFLOW) {
              String ot = GenerationBase.transformNameForJava(node.getUniqueName());
              DependencyNode otNode = dependencyRegister.getDependencyNode(ot, DependencySourceType.ORDERTYPE, fromRevision);
              if (otNode != null) {
                if (depNodes.add(otNode)) {
                  DeploymentLocks.readLock(ot, DependencySourceType.ORDERTYPE, "BuildApplication", fromRevision);
                }
              }
            }
          }
        }
      }
    } finally {
      DeploymentLocks.readUnlock(uniqueName, sourceType, fromRevision);
    }

    //sicherstellen, dass alle gelockten nodes auch wieder geunlockt werden. ggfs rekursion aufrufen
    XynaException caughtException = null;
    RuntimeException caughtRTE = null;
    Error caughtError = null;
    for (DependencyNode node : depNodes) {
      if (caughtException != null || caughtError != null) {
        //skip, unlock, am ende fehler werfen
        DeploymentLocks.readUnlock(node.getUniqueName(), node.getType(), fromRevision);
      } else {
        try {
          if (!processedDependencies.contains(node)) {
            recursiveLockAndCopyDependencies(node.getUniqueName(), node.getType(), fromRevision, toRevision, verbose, statusOutputStream,
                                             processedDependencies, predictionValues, processedOrderInputSources, reqAppEntries);
            //Exceptions:
            //wurde geunlocked, aber ist evtl nicht in processedDependencies enthalten.
          } else {
            //bei einer früheren rekursion behandelt - auch geunlocked, aber nur mit dem dort zugeordneten lock, nicht mit dem hiesigen
            DeploymentLocks.readUnlock(node.getUniqueName(), node.getType(), fromRevision);
          }
        } catch (XynaException e) {
          caughtException = e;
        } catch (RuntimeException e) {
          caughtRTE = e;
        } catch (Error e) {
          Department.handleThrowable(e);
          caughtError = e;
        }
      }
    }

    if (caughtError != null) {
      throw caughtError;
    }
    if (caughtRTE != null) {
      throw caughtRTE;
    }
    if (caughtException != null) {
      throw caughtException;
    }
  }


  /**
   * Fügt ein Objekt (Workflow, Datentyp, Exception) und alle Abhängigkeiten zu einer Applikation hinzu. Dies kann nur
   * auf der Arbeitsversion geschehen.
   * 
   * Falls RepositoryAccess konfiguriert ist, werden die Änderungen eingecheckt
   */
  public void addXMOMObjectToApplication(String fqName, String applicationName, Long parentRevision)
      throws XFMG_FailedToAddObjectToApplication {
    addXMOMObjectToApplication(fqName, applicationName, parentRevision, new SingleRepositoryEvent(parentRevision));
  }


  /**
   * Fügt ein Objekt (Workflow, Datentyp, Exception) und alle Abhängigkeiten zu einer Applikation hinzu. Dies kann nur
   * auf der Arbeitsversion geschehen.
   */
  public void addXMOMObjectToApplication(String fqName, String applicationName, Long parentRevision, RepositoryEvent repositoryEvent)
      throws XFMG_FailedToAddObjectToApplication {
    addXMOMObjectToApplication(fqName, applicationName, parentRevision, repositoryEvent, false, null);
  }


  /**
   * Fügt ein Objekt (Workflow, Datentyp, Exception) und alle Abhängigkeiten zu einer Applikation hinzu.
   */
  public void addXMOMObjectToApplication(String fqName, String applicationName, Long parentRevision, RepositoryEvent repositoryEvent,
                                         boolean verbose, PrintStream statusOutputStream) throws XFMG_FailedToAddObjectToApplication {
    try {
      ApplicationEntryType t = getApplicationEntryTypeForXMOMObject(fqName, parentRevision);
      if (t == null) {
        throw new XFMG_FailedToAddObjectToApplication(applicationName, fqName, new RuntimeException("XMOM Object " + fqName + " not found."));
      }
      addObjectToApplicationDefinition(fqName, t, applicationName, parentRevision, false, null, repositoryEvent);
    } catch (PersistenceLayerException e) {
      throw new XFMG_FailedToAddObjectToApplication(applicationName, fqName, e);
    }
  }


  public void addObjectToApplicationDefinition(String name, ApplicationEntryType type, String applicationName, long parentRevision, boolean verbose,
                                      PrintStream statusOutputStream, RepositoryEvent repositoryEvent)
      throws XFMG_FailedToAddObjectToApplication {
    writelock.lock();
    try {

      ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
      try {
        String version = getWorkingsetVersionName(applicationName, parentRevision);
        if (version == null) {
          output(statusOutputStream, "Can't add objects to an application without workingset version.");
          throw new XFMG_FailedToAddObjectToApplication(applicationName, name);
        }

        if (null != queryApplicationDefinitionEntryStorable(applicationName, parentRevision, name, type, con)) {
          output(statusOutputStream, type.toString() + " " + name + " is already contained in application definition " + applicationName
              + ".");
          return;
        }

        boolean exists = type.checkForExistence(name, parentRevision);

        if (exists) {

          addObjectToApplicationInternally(name, type, applicationName, version, parentRevision, con, statusOutputStream);
          con.commit();
          transferFromODSTypeToODSType(ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY, ApplicationEntryStorable.TABLE_NAME,
                                       ApplicationEntryStorable.class);

          clearApplicationDefinitionCache(parentRevision);
          updateApplicationDetailsCache(parentRevision);
          repositoryEvent
              .addEvent(new ProjectCreationOrChangeProvider.BasicProjectCreationOrChangeEvent(EventType.APPLICATION_DEFINITION_CHANGE,
                                                                                              applicationName));
        } else {
          try {
            output(statusOutputStream,
                   type.toString() + " " + name + " does not exist in " + revisionManagement.getRuntimeContext(parentRevision) + ".");
          } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
            throw new RuntimeException(e);
          }
        }
      } catch (PersistenceLayerException e) {
        throw new XFMG_FailedToAddObjectToApplication(applicationName, name, e);
      } finally {
        try {
          con.closeConnection();
        } catch (PersistenceLayerException e) {
          logger.warn("Can't close connection.", e);
        }
      }
    } finally {
      writelock.unlock();
    }
  }


  private void addObjectToApplicationInternally(String name, ApplicationEntryType type, String applicationName, String version,
                                                long parentRevision, ODSConnection con, PrintStream statusOutputStream)
      throws PersistenceLayerException, XFMG_FailedToAddObjectToApplication {

    if (null != queryRuntimeApplicationEntryStorable(applicationName, version, name, type, con)) {
      //bereits in application enthalten
      output(statusOutputStream, "Skipping " + type.toString() + " " + name);
      return;
    }
    long storedParentRevision = parentRevision;
    boolean parentIsRTA = parentRevision != 0 && !revisionManagement.isWorkspaceRevision(parentRevision);
    if (parentIsRTA) {
      //addObjectToApplication in RuntimeApplication soll nicht parentRevision != 0 setzen.
      storedParentRevision = 0L;
    }
    ApplicationEntryStorable appEntry = ApplicationEntryStorable.toStore(applicationName, version, storedParentRevision, name, type);

    if (!parentIsRTA) {
      try {
        //Überprüfen, ob das Anlegen des neuen ApplicationEntries zu Zugehörigkeitskonflikt führen würde
        checkObjectNotInDependencyHierarchy(con, appEntry, parentRevision);
      } catch (XynaException e) {
        throw new XFMG_FailedToAddObjectToApplication(applicationName, name, e);
      }
    }
    
    con.persistObject(appEntry);
    output(statusOutputStream, "Adding " + type.toString() + " " + name + " to application definition " + applicationName + ".");
  }


  ApplicationEntryType getApplicationEntryTypeForXMOMObject(String fqName, Long revision) throws PersistenceLayerException {
    //zunächst im DependencyRegister suchen
    DependencyNode depNodeDataType = dependencyRegister.getDependencyNode(fqName, DependencySourceType.DATATYPE, revision);
    DependencyNode depNodeWorkflow = dependencyRegister.getDependencyNode(fqName, DependencySourceType.WORKFLOW, revision);
    DependencyNode depNodeException = dependencyRegister.getDependencyNode(fqName, DependencySourceType.XYNAEXCEPTION, revision);

    DependencyNode depNode = (depNodeDataType != null) ? depNodeDataType : (depNodeWorkflow != null) ? depNodeWorkflow : depNodeException;

    if (depNode != null) {
      return convertXMOMTypeToApplicationEntryType(depNode);
    } else {
      //Objekt ist nicht deployed -> in XMOMDatabase suchen
      XMOMDatabase db = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getXMOMDatabase();
      Collection<XMOMDatabaseEntry> entries = db.getAllXMOMEntriesFromSingleUnknown(fqName, false, false, revision);
      if (entries.size() == 1) {
        XMOMDatabaseEntry entry = entries.iterator().next();
        return convertXMOMDatabaseTypeToApplicationEntryType(entry.getXMOMDatabaseType());
      } else {
        return null;
      }
    }
  }
  

  /**
   * Liefert für eine Application Definition rekursiv alle benötigten Application Definitions.
   * @param applicationName
   * @param parentRevision
   * @return
   */
  private Set<ApplicationDefinition> getRequiredApplicationDefinitions(String applicationName, Long parentRevision) {
    final RuntimeContextDependencyManagement rcdMgmt =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
    try {
      final Workspace parentWorkspace = revisionManagement.getWorkspace(parentRevision);
      ApplicationDefinition thisappdef = new ApplicationDefinition(applicationName, parentWorkspace);
      Set<ApplicationDefinition> ret = GraphUtils.collectConnectedNodes(new ConnectedEdges<ApplicationDefinition>() {

        @Override
        public Collection<ApplicationDefinition> getConnectedEdges(ApplicationDefinition t) {
          List<ApplicationDefinition> l = new ArrayList<>();
          for (RuntimeDependencyContext rc : rcdMgmt.getDependencies(t)) {
            if (rc instanceof ApplicationDefinition) {
              l.add((ApplicationDefinition) rc);
            }
          }
          return l;
        }

      }, thisappdef, false);
      ret.remove(thisappdef);
      return ret;
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      // wenn es die Revision nicht gibt, kann sie auch keine Requirements haben
      return new HashSet<>();
    }
  }


  /**
   * Liefert alle (expl. und impl.) ApplicationsEntries der verwendeten Application Definitions.
   * @param applicationName
   * @param parentRevision
   * @return
   */
  private Set<ApplicationEntryStorable> getRequiredApplicationDefinitionEntries(String applicationName, Long parentRevision) {
    Set<ApplicationEntryStorable> reqEntries = new TreeSet<ApplicationEntryStorable>(ApplicationEntryStorable.COMPARATOR);
    Set<ApplicationDefinition> reqAppDefs = getRequiredApplicationDefinitions(applicationName, parentRevision);
    for (ApplicationDefinition appDef : reqAppDefs) {
      List<ApplicationEntryStorable> implAppEntries = listApplicationDetails(appDef.getName(), null, true, null, parentRevision);
      if (implAppEntries != null) {
        reqEntries.addAll(implAppEntries);
      }
    }

    return reqEntries;
  }


  /**
   * Liefert für eine ApplicationDefinition rekursiv alle ApplicationDefinitions die sie verwenden.
   * @param applicationName
   * @param parentRevision
   * @return
   */
  private Set<ApplicationDefinition> getParentApplicationDefinitions(String applicationName, Long parentRevision) {
    final RuntimeContextDependencyManagement rcdMgmt =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
    try {
      final Workspace parentWorkspace = revisionManagement.getWorkspace(parentRevision);
      ApplicationDefinition thisappdef = new ApplicationDefinition(applicationName, parentWorkspace);
      Set<ApplicationDefinition> ret = GraphUtils.collectConnectedNodes(new ConnectedEdges<ApplicationDefinition>() {

        @Override
        public Collection<ApplicationDefinition> getConnectedEdges(ApplicationDefinition t) {
          List<ApplicationDefinition> l = new ArrayList<>();
          for (RuntimeDependencyContext rc : rcdMgmt.getParentRuntimeContexts(t)) {
            if (rc instanceof ApplicationDefinition) {
              l.add((ApplicationDefinition) rc);
            }
          }
          return l;
        }

      }, thisappdef, true);
      ret.remove(thisappdef); //zyklen nicht berücksichtigen
      return ret;
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      // wenn es die Revision nicht gibt, kann sie auch keine Parents haben
      return new HashSet<>();
    }
  }


  /**
   * Überprüft, ob 
   *  - der ApplicationEntry schon in einer verwendeten Application Definition (impl. oder expl.) enthalten ist
   *  - der ApplicationEntry oder ein hiervon verwendetes Objekt in einer Parent Application Definition explizit enthalten ist
   * @param con
   * @param appEntry
   * @param parentRevision
   * @return
   * @throws PersistenceLayerException
   * @throws XFMG_ObjectAlreadyInDependencyHierarchyException falls das Objekt (oder ein hiervon verwendetes) bereits in der Abhängigkeitshierarchie enthalten ist
   * @throws XPRC_InvalidXmlMissingRequiredElementException 
   * @throws XPRC_XmlParsingException 
   * @throws Ex_FileAccessException 
   */
  private void checkObjectNotInDependencyHierarchy(ODSConnection con, ApplicationEntryStorable appEntry, Long parentRevision)
      throws PersistenceLayerException, XFMG_ObjectAlreadyInDependencyHierarchyException, Ex_FileAccessException, XPRC_XmlParsingException,
      XPRC_InvalidXmlMissingRequiredElementException {
    if (parentRevision == null) {
      return;
    }

    checkObjectNotInRequirements(con, appEntry, parentRevision);
    checkObjectNotInParents(con, appEntry, parentRevision);
  }


  /**
   * Überprüft, ob der ApplicationEntry schon in einer verwendeten Application Definition (impl. oder expl.) enthalten ist
   */
  private void checkObjectNotInRequirements(ODSConnection con, ApplicationEntryStorable appEntry, Long parentRevision)
      throws PersistenceLayerException, XFMG_ObjectAlreadyInDependencyHierarchyException {
    if (parentRevision == null) {
      return;
    }

    Set<ApplicationEntryStorable> reqAppEntries = getRequiredApplicationDefinitionEntries(appEntry.getApplication(), parentRevision);
    if (reqAppEntries.contains(appEntry)) {
      //ApplicationEntry ist (explizit oder implizit) in einer verwendeten Application Definition enthalten
      throw new XFMG_ObjectAlreadyInDependencyHierarchyException(appEntry.getName());
    }
  }


  /**
   * Überprüft, ob der ApplicationEntry oder ein hiervon verwendetes Objekt in einer Parent Application Definition explizit enthalten ist
   */
  private void checkObjectNotInParents(ODSConnection con, ApplicationEntryStorable appEntry, Long parentRevision)
      throws PersistenceLayerException, XFMG_ObjectAlreadyInDependencyHierarchyException, Ex_FileAccessException, XPRC_XmlParsingException,
      XPRC_InvalidXmlMissingRequiredElementException {
    if (parentRevision == null) {
      return;
    }

    //alle vom ApplicationEntry verwendeten Objekte suchen
    Collection<ApplicationEntryStorable> implEntries =
        getDependencies(appEntry, parentRevision, new TreeSet<ApplicationEntryStorable>(ApplicationEntryStorable.COMPARATOR));
    implEntries.add(appEntry);

    Set<ApplicationDefinition> parentAppDefs = getParentApplicationDefinitions(appEntry.getApplication(), parentRevision);
    for (ApplicationDefinition appDef : parentAppDefs) {
      for (ApplicationEntryStorable parentAppEntry : queryAllApplicationDefinitionStorables(appDef.getName(), parentRevision, con)) {
        if (implEntries.contains(parentAppEntry)) {
          //ApplicationEntry oder ein hiervon verwendetes Objekt ist in einer Parent Application Definition enthalten
          throw new XFMG_ObjectAlreadyInDependencyHierarchyException(parentAppEntry.getName());
        }
      }
    }
  }


  public void addTriggerToApplication(String triggerName, String applicationName, Long parentRevision, boolean verbose,
                                      PrintStream printStream) throws XFMG_FailedToAddObjectToApplication {
    addObjectToApplicationDefinition(triggerName, ApplicationEntryType.TRIGGER, applicationName, parentRevision, verbose, printStream,
                           new SingleRepositoryEvent(parentRevision));
  }


  public void addFilterToApplication(String filterName, String applicationName, Long parentRevision, boolean verbose,
                                     PrintStream printStream) throws XFMG_FailedToAddObjectToApplication {
    addObjectToApplicationDefinition(filterName, ApplicationEntryType.FILTER, applicationName, parentRevision, verbose, printStream,
                           new SingleRepositoryEvent(parentRevision));
  }


  public void addTriggerInstanceToApplication(String triggerInstanceName, String applicationName, Long parentRevision, boolean verbose,
                                              PrintStream statusOutputStream) throws XFMG_FailedToAddObjectToApplication {
    addObjectToApplicationDefinition(triggerInstanceName, ApplicationEntryType.TRIGGERINSTANCE, applicationName, parentRevision, verbose,
                           statusOutputStream, new SingleRepositoryEvent(parentRevision));
  }


  public void addFilterInstanceToApplication(String filterInstanceName, String applicationName, Long parentRevision, boolean verbose,
                                             PrintStream statusOutputStream) throws XFMG_FailedToAddObjectToApplication {
    addObjectToApplicationDefinition(filterInstanceName, ApplicationEntryType.FILTERINSTANCE, applicationName, parentRevision, verbose,
                           statusOutputStream, new SingleRepositoryEvent(parentRevision));
  }


  /**
   * Fügt ein Objekt (XynaProperty, SharedLibs, Capcities) und alle Abhängigkeiten zu einer Applikation hinzu.
   * 
   * Falls RepositoryAccess konfiguriert ist, werden die Änderungen eingecheckt
   */
  public void addNonModelledObjectToApplication(String objectName, String applicationName, String version, ApplicationEntryType entryType,
                                                Long parentRevision, boolean verbose, PrintStream statusOutputStream)
      throws XFMG_FailedToAddObjectToApplication {
    addNonModelledObjectToApplication(objectName, applicationName, version, entryType, parentRevision,
                                      new SingleRepositoryEvent(parentRevision), verbose, statusOutputStream);
  }


  /**
   * Fügt ein Objekt (XynaProperty, SharedLibs, Capcities, OrderInputSource) und alle Abhängigkeiten zu einer Applikation hinzu.
   */
  public void addNonModelledObjectToApplication(String objectName, String applicationName, String version, ApplicationEntryType entryType,
                                                Long parentRevision, RepositoryEvent repositoryEvent, boolean verbose,
                                                PrintStream statusOutputStream) throws XFMG_FailedToAddObjectToApplication {
    if (parentRevision == null) {
      //zu RTA adden
      ODSConnection con = ods.openConnection();
      try {
        try {
          ApplicationEntryStorable aes = ApplicationEntryStorable.toStore(applicationName, version, objectName, entryType);
          con.persistObject(aes);
          con.commit();
        } finally {
          con.closeConnection();
        }
      } catch (PersistenceLayerException e) {
        throw new XFMG_FailedToAddObjectToApplication(applicationName, objectName, e);
      }
      try {
        updateApplicationDetailsCache(revisionManagement.getRevision(applicationName, version, null));
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new RuntimeException(e);
      }
      return;
    }
    addObjectToApplicationDefinition(objectName, entryType, applicationName, parentRevision, verbose, statusOutputStream, repositoryEvent);
  }


  /**
   * Löscht ein XMOM Objekt aus einer Applicationdefinition.
   * 
   * Falls RepositoryAccess konfiguriert ist, werden die Änderungen eingecheckt
   */
  public void removeXMOMObjectFromApplication(String applicationName, String fqName, Long parentRevision)
      throws XFMG_FailedToRemoveObjectFromApplication, XFMG_FailedToRemoveObjectFromApplicationBecauseHasDependentObjects,
      XFMG_ObjectNotFoundException {
    removeXMOMObjectFromApplication(applicationName, fqName, parentRevision, new SingleRepositoryEvent(parentRevision), false, null);
  }


  /**
   * Löscht ein XMOM Objekt aus einer Applicationdefinition.
   */
  public void removeXMOMObjectFromApplication(String applicationName, String fqName, Long parentRevision, RepositoryEvent repositoryEvent)
      throws XFMG_FailedToRemoveObjectFromApplication, XFMG_FailedToRemoveObjectFromApplicationBecauseHasDependentObjects,
      XFMG_ObjectNotFoundException {
    removeXMOMObjectFromApplication(applicationName, fqName, parentRevision, repositoryEvent, false, null);
  }


  /**
   * Löscht ein XMOM Objekt aus einer Applicationdefinition.
   */
  public void removeXMOMObjectFromApplication(String applicationName, String fqName, Long parentRevision, RepositoryEvent repositoryEvent,
                                              boolean verbose, PrintStream statusOutputStream)
      throws XFMG_FailedToRemoveObjectFromApplication, XFMG_FailedToRemoveObjectFromApplicationBecauseHasDependentObjects,
      XFMG_ObjectNotFoundException {
    ApplicationEntryType t;
    try {
      t = getApplicationEntryTypeForXMOMObject(fqName, parentRevision);
    } catch (PersistenceLayerException e) {
      throw new XFMG_FailedToRemoveObjectFromApplication(applicationName, fqName, e);
    }
    if (t == null) {
      throw new XFMG_FailedToRemoveObjectFromApplication(applicationName, fqName, new RuntimeException("Object " + fqName + " not found."));
    }
    removeObjectFromApplicationDefinition(fqName, t, applicationName, parentRevision, repositoryEvent, verbose, statusOutputStream);
  }


  private void removeObjectFromApplicationDefinition(String name, ApplicationEntryType type, String applicationName, long parentRevision,
                                           RepositoryEvent repositoryEvent, boolean verbose, PrintStream statusOutputStream)
      throws XFMG_FailedToRemoveObjectFromApplication {
    writelock.lock();
    try {
      ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
      try {

        String version = getWorkingsetVersionName(applicationName, parentRevision);
        if (version == null) {
          output(statusOutputStream, "Did not find application definition.");
          throw new XFMG_FailedToRemoveObjectFromApplicationBecauseOfMissingWorkingset(applicationName, name);
        }

        ApplicationEntryStorable aes = queryApplicationDefinitionEntryStorable(applicationName, parentRevision, name, type, con);
        if (aes == null) {
          output(statusOutputStream, type + " " + name + " is not entry of application definition " + applicationName);
          return;
        }
        con.deleteOneRow(aes);
        con.commit();

        transferFromODSTypeToODSType(ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY, ApplicationEntryStorable.TABLE_NAME,
                                     ApplicationEntryStorable.class);

        clearApplicationDefinitionCache(parentRevision);
        updateApplicationDetailsCache(parentRevision);

        repositoryEvent
            .addEvent(new ProjectCreationOrChangeProvider.BasicProjectCreationOrChangeEvent(EventType.APPLICATION_DEFINITION_CHANGE,
                                                                                            applicationName));
      } catch (PersistenceLayerException e) {
        throw new XFMG_FailedToRemoveObjectFromApplication(applicationName, name, e);
      } finally {
        try {
          con.closeConnection();
        } catch (PersistenceLayerException e) {
          logger.warn("Can't close connection.", e);
        }
      }
    } finally {
      writelock.unlock();
    }

  }


  public void removeTriggerInstanceFromApplication(String applicationName, String objectName, Long parentRevision, boolean verbose,
                                                   PrintStream printStream) throws XFMG_FailedToRemoveObjectFromApplication,
      XFMG_FailedToRemoveObjectFromApplicationBecauseHasDependentObjects {
    removeObjectFromApplicationDefinition(objectName, ApplicationEntryType.TRIGGERINSTANCE, applicationName, parentRevision,
                                new SingleRepositoryEvent(parentRevision), verbose, printStream);
  }


  public void removeFilterInstanceFromApplication(String applicationName, String objectName, Long parentRevision, boolean verbose,
                                                  PrintStream printStream) throws XFMG_FailedToRemoveObjectFromApplication {
    removeObjectFromApplicationDefinition(objectName, ApplicationEntryType.FILTERINSTANCE, applicationName, parentRevision,
                                new SingleRepositoryEvent(parentRevision), verbose, printStream);
  }


  public void removeTriggerFromApplication(String applicationName, String objectName, Long parentRevision, boolean verbose,
                                           PrintStream printStream) throws XFMG_FailedToRemoveObjectFromApplication {
    removeObjectFromApplicationDefinition(objectName, ApplicationEntryType.TRIGGER, applicationName, parentRevision,
                                new SingleRepositoryEvent(parentRevision), verbose, printStream);
  }


  public void removeFilterFromApplication(String applicationName, String objectName, Long parentRevision, boolean verbose,
                                          PrintStream printStream) throws XFMG_FailedToRemoveObjectFromApplication {
    removeObjectFromApplicationDefinition(objectName, ApplicationEntryType.FILTER, applicationName, parentRevision,
                                new SingleRepositoryEvent(parentRevision), verbose, printStream);
  }


  public void removeObjectFromApplication(String applicationName, String objectName,
                                           ApplicationEntryType entryType, Long parentRevision, RepositoryEvent repositoryEvent, boolean verbose,
                                           PrintStream statusOutputStream) throws XFMG_FailedToRemoveObjectFromApplication, XFMG_FailedToRemoveObjectFromApplicationBecauseHasDependentObjects, XFMG_ObjectNotFoundException {
    switch (entryType) {
      case DATATYPE :
      case EXCEPTION :
      case WORKFLOW :
        removeXMOMObjectFromApplication(applicationName, objectName, parentRevision, repositoryEvent, verbose, statusOutputStream);
        break;
      default :
        String version = getWorkingsetVersionName(applicationName, parentRevision);
        if (version == null) {
          output(statusOutputStream, "Did not find application definition.");
          throw new XFMG_FailedToRemoveObjectFromApplicationBecauseOfMissingWorkingset(applicationName, objectName);
        }
        removeNonModelledObjectFromApplication(applicationName, version, objectName, entryType, parentRevision, repositoryEvent, verbose, statusOutputStream);
        break;
    }
  }
  
  /**
   * Löscht ein Objekt (XynaProperty, SharedLibs, Capacities, Ordertype) aus einer Applikation, falls sie nicht nur implizit
   * vorhanden ist
   * 
   * Falls RepositoryAccess konfiguriert ist, werden die Änderungen eingecheckt
   */
  public void removeNonModelledObjectFromApplication(String applicationName, String version, String objectName,
                                                     ApplicationEntryType entryType, Long parentRevision, boolean verbose,
                                                     PrintStream printStream) throws XFMG_FailedToRemoveObjectFromApplication {
    removeNonModelledObjectFromApplication(applicationName, version, objectName, entryType, parentRevision,
                                           new SingleRepositoryEvent(parentRevision), verbose, printStream);
  }


  /**
   * Löscht ein Objekt (XynaProperty, SharedLibs, Capacities, Ordertype) aus einer Applikation, falls sie nicht nur implizit
   * vorhanden ist
   * 
   * Falls RepositoryAccess konfiguriert ist, werden die Änderungen eingecheckt
   */
  public void removeNonModelledObjectFromApplication(String applicationName, String version, String objectName,
                                                     ApplicationEntryType entryType, Long parentRevision, RepositoryEvent repositoryEvent,
                                                     boolean verbose, PrintStream printStream)
      throws XFMG_FailedToRemoveObjectFromApplication {
    
    if (parentRevision == null) {
      //aus RTA entfernen
      ODSConnection con = ods.openConnection();
      try {
        try {
          ApplicationEntryStorable aes = queryRuntimeApplicationEntryStorable(applicationName, version, objectName, entryType, con);
          if (aes != null) {
            con.deleteOneRow(aes);
            con.commit();
          }
        } finally {
          con.closeConnection();
        }
      } catch (PersistenceLayerException e) {
        throw new XFMG_FailedToRemoveObjectFromApplication(applicationName, objectName, e);
      }
      try {
        updateApplicationDetailsCache(revisionManagement.getRevision(applicationName, version, null));
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new RuntimeException(e);
      }
      return;
    }
    removeObjectFromApplicationDefinition(objectName, entryType, applicationName, parentRevision, repositoryEvent, verbose, printStream);
  }


  /**
   * entfernt objekt aus den application definitions in allen Workspaces
   */
  public void removeObjectFromAllApplications(String uniqueName, ApplicationEntryType type) {
    for (Long revision : revisionManagement.getAllWorkspaceRevisions()) {
      removeObjectFromAllApplications(uniqueName, type, revision);
    }
  }


  /**
   * entfernt objekt aus application definition in einem Workspace
   */
  public void removeObjectFromAllApplications(String uniqueName, ApplicationEntryType type, Long parentRevision) {
    List<ApplicationInformation> appsinfo = listApplications(false, false);
    List<Throwable> throwables = new ArrayList<Throwable>();

    Set<String> handledApps = new HashSet<String>();
    for (ApplicationInformation appInfo : appsinfo) {
      String applicationName = appInfo.getName();
      if (!handledApps.add(applicationName)) {
        //nicht mehrfach die gleiche applikation behandeln
        continue;
      }

      try {
        removeObjectFromApplicationDefinition(uniqueName, type, applicationName, parentRevision, new SingleRepositoryEvent(parentRevision), false,
                                    null);
      } catch (XFMG_FailedToRemoveObjectFromApplicationBecauseOfMissingWorkingset e) {
        //nichts zu tun, objekt gehört also nicht zu applikation
        if (logger.isTraceEnabled()) {
          logger.trace("object not contained in application " + applicationName, e);
        }
      } catch (XFMG_FailedToRemoveObjectFromApplication e) {
        throwables.add(e);
      }
    }

    if (throwables.size() > 0) {
      logger.error("Errors occured while trying to remove object " + uniqueName + " from all applications.");

      for (Throwable throwable : throwables) {
        logger.error("", throwable);
      }
    }
  }


  /**
   * Bestimmt alle aktiven (d.h. unbeendeten) XynaOrders, die TimeControlledOrders (Batch Prozesse
   * und Crons) und die Frequency-Controlled Tasks einer Application
   */
  public OrdersInUse listActiveOrders(String applicationName, String versionName, boolean verbose, boolean global) throws XynaException {
    long revision = revisionManagement.getRevision(applicationName, versionName, null);

    FillingMode mode = verbose ? FillingMode.Complete : FillingMode.EasyInfos;
    OrdersInUse activeOrders = DeploymentManagement.getInstance().getInUse(revision, mode);

    //Cluster
    if (global && currentClusterState == ClusterState.CONNECTED) {
      if (logger.isDebugEnabled()) {
        logger.debug("Call listActiveOrders on other cluster nodes.");
      }
      try {
        List<OrdersInUse> results =
            RMIClusterProviderTools.executeAndCumulate(clusterInstance, clusteredInterfaceId, new ListActiveOrdersRunnable(applicationName,
                                                                                                                           versionName,
                                                                                                                           verbose), null);
        for (OrdersInUse result : results) {
          activeOrders.addOrders(result);
        }
      } catch (XynaException e) {
        throw new XFMG_CouldNotRemoveApplication(applicationName, versionName, e);
      } catch (InvalidIDException e) {
        throw new XFMG_CouldNotRemoveApplication(applicationName, versionName, e);
      }
    }

    return activeOrders;
  }


  public OrdersInUse listActiveOrdersRemotely(String applicationName, String versionName, boolean verbose) throws RemoteException {
    try {
      return listActiveOrders(applicationName, versionName, verbose, false);
    } catch (XynaException e) {
      throw new RemoteException("Unable to list active orders", e);
    }
  }
  
  @SuppressWarnings("unchecked")
  public List<ApplicationEntryStorable> listApplicationDetails(String applicationName, String version, boolean includingDependencies,
                                                               List<String> excludeSubtypesOf, Long parentRevision) {
    return listApplicationDetails(applicationName, version, includingDependencies, excludeSubtypesOf, parentRevision, false);
  }
  
  private static class AppDetailsKey {

    private final String applicationName;
    private final String version;
    private final boolean includingDependencies;
    private final List<String> excludeSubtypesOf;
    private final Long parentRevision;
    private final boolean ignoreErrorsDuringDependencySearch;
    public AppDetailsKey(String applicationName, String version, boolean includingDependencies, List<String> excludeSubtypesOf,
                         Long parentRevision, boolean ignoreErrorsDuringDependencySearch) {
      super();
      this.applicationName = applicationName;
      this.version = version;
      this.includingDependencies = includingDependencies;
      this.excludeSubtypesOf = excludeSubtypesOf;
      this.parentRevision = parentRevision;
      this.ignoreErrorsDuringDependencySearch = ignoreErrorsDuringDependencySearch;
    }
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((applicationName == null) ? 0 : applicationName.hashCode());
      result = prime * result + ((excludeSubtypesOf == null || excludeSubtypesOf.isEmpty()) ? 0 : excludeSubtypesOf.hashCode());
      result = prime * result + (ignoreErrorsDuringDependencySearch ? 1231 : 1237);
      result = prime * result + (includingDependencies ? 1231 : 1237);
      result = prime * result + ((parentRevision == null) ? 0 : parentRevision.hashCode());
      result = prime * result + ((version == null) ? 0 : version.hashCode());
      return result;
    }
    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      AppDetailsKey other = (AppDetailsKey) obj;
      if (applicationName == null) {
        if (other.applicationName != null)
          return false;
      } else if (!applicationName.equals(other.applicationName))
        return false;
      if (excludeSubtypesOf == null || excludeSubtypesOf.isEmpty()) {
        if (other.excludeSubtypesOf != null && !other.excludeSubtypesOf.isEmpty())
          return false;
      } else if (!excludeSubtypesOf.equals(other.excludeSubtypesOf))
        return false;
      if (ignoreErrorsDuringDependencySearch != other.ignoreErrorsDuringDependencySearch)
        return false;
      if (includingDependencies != other.includingDependencies)
        return false;
      if (parentRevision == null) {
        if (other.parentRevision != null)
          return false;
      } else if (!parentRevision.equals(other.parentRevision))
        return false;
      if (version == null) {
        if (other.version != null)
          return false;
      } else if (!version.equals(other.version))
        return false;
      return true;
    }

    
  }
  
  /*
   * bei allen änderungen, die die listappdetails betreffen, muss diese methode aufgerufen werden
   * TODO
   * - ordertype änderungen
   * - inputsource änderungen
   */
  public void updateApplicationDetailsCache(long changedRevision) {
    List<AppDetailsKey> remove = new ArrayList<AppDetailsKey>();
    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    synchronized (getApplicationDetailsCache()) {
      for (AppDetailsKey adk : getApplicationDetailsCache().keySet()) {
        if (adk.parentRevision != null && adk.parentRevision == changedRevision) {
          //gleicher workspace
          remove.add(adk);
          continue;
        }
        if (adk.parentRevision != null) {
          //andere workspaces sind hier irrelevant
          continue;
        }
        try {
          if (changedRevision == rm.getRevision(new Application(adk.applicationName, adk.version))) {
            remove.add(adk);
          }
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          //inzwischen entfernt?
          remove.add(adk);
        }
      }
      for (AppDetailsKey adk : remove) {
        getApplicationDetailsCache().remove(adk);
      }
    }
  }
  
  private static final XynaPropertyInt sizeOfApplicationDetailsCache = new XynaPropertyInt("xfmg.xfctrl.appmgmt.appdetails.cache.size", 200).setDefaultDocumentation(DocumentationLanguage.EN, "Size of LRU Cache for Application Details. Needs restart of factory for changes to be activated.");
  private boolean loggedCacheSizeTooSmall = false;
  
  private LruCacheWithTimingInformation<AppDetailsKey, List<ApplicationEntryStorable>> applicationDetailsCache;
  

  private LruCacheWithTimingInformation<AppDetailsKey, List<ApplicationEntryStorable>> getApplicationDetailsCache() {
    synchronized (sizeOfApplicationDetailsCache) {
      if (applicationDetailsCache == null) {
        applicationDetailsCache =
            new LruCacheWithTimingInformation<AppDetailsKey, List<ApplicationEntryStorable>>(sizeOfApplicationDetailsCache.get());
      }
      return applicationDetailsCache;
    }
  }

  /**
   * Listet die Details für eine Applikation auf.
   */
  public List<ApplicationEntryStorable> listApplicationDetails(String applicationName, String version, boolean includingDependencies,
                                                               List<String> excludeSubtypesOf, Long parentRevision, boolean ignoreErrorsDuringDependencySearch) {
    if (applicationName == null) {
      throw new IllegalArgumentException("Application must not be null");
    }
    AppDetailsKey key = new AppDetailsKey(applicationName, version, includingDependencies, excludeSubtypesOf, parentRevision, ignoreErrorsDuringDependencySearch);
    if (USE_APPLICATION_ENTRY_CACHE.get()) {
      synchronized (getApplicationDetailsCache()) {
        List<ApplicationEntryStorable> val = getApplicationDetailsCache().get(key);
        if (val != null) {
          return val;
        }
      }
    }
    
    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    try {

      if (version == null) {
        version = getWorkingsetVersionName(applicationName, parentRevision);
        if (version == null) {
          return null;
        }
      }

      ApplicationStorable app;
      if (parentRevision == null) {
        app = queryRuntimeApplicationStorable(applicationName, version, con);
      } else {
        app = queryApplicationDefinitionStorable(applicationName, parentRevision, con);
      }

      if (app == null) {
        return null;
      }
      Set<ApplicationEntryStorable> appEntries = new TreeSet<ApplicationEntryStorable>(ApplicationEntryStorable.COMPARATOR);
      if (parentRevision == null) {
        appEntries.addAll(queryAllRuntimeApplicationStorables(applicationName, version, con));
      } else {
        appEntries.addAll(queryAllApplicationDefinitionStorables(applicationName, parentRevision, con));
      }

      if (includingDependencies) {
        Set<ApplicationEntryStorable> implAppEntries = new TreeSet<ApplicationEntryStorable>(ApplicationEntryStorable.COMPARATOR);
        implAppEntries = getAllImplicitApplicationEntries(appEntries, applicationName, version, parentRevision, ignoreErrorsDuringDependencySearch);

        //im Workspace zusätzlich die Subtypen der Outputs von Java-Services bestimmen
        if (parentRevision != null) {
          Set<ApplicationEntryStorable> alreadyProcessedEntries =
              new TreeSet<ApplicationEntryStorable>(ApplicationEntryStorable.COMPARATOR);
          alreadyProcessedEntries.addAll(appEntries);
          alreadyProcessedEntries.addAll(implAppEntries);

          Set<String> subTypes = getSubTypesOfOutputVars(parentRevision, alreadyProcessedEntries, excludeSubtypesOf);

          for (String subType : subTypes) {
            ApplicationEntryStorable appEntry =
                ApplicationEntryStorable.create(applicationName, version, parentRevision, subType, ApplicationEntryType.DATATYPE);
            implAppEntries.add(appEntry);
            alreadyProcessedEntries.add(appEntry);
            //abhängige Objekte der Subtypen auch noch einsammeln
            implAppEntries.addAll(getXMOMDependencies(subType, DependencySourceType.DATATYPE, applicationName, version, parentRevision,
                                                      alreadyProcessedEntries));
          }

          //vgl kommentar bei buildApplication
          Set<String> storableWFs = getStorableWFs2(alreadyProcessedEntries, parentRevision);
          for (String storableWF : storableWFs) {
            ApplicationEntryStorable appEntry =
                ApplicationEntryStorable.create(applicationName, version, parentRevision, storableWF, ApplicationEntryType.ORDERTYPE);
            implAppEntries.add(appEntry);
            alreadyProcessedEntries.add(appEntry);
            implAppEntries.addAll(getXMOMDependencies(storableWF, DependencySourceType.ORDERTYPE, applicationName, version, parentRevision,
                                                      alreadyProcessedEntries));
          }

          //Objekte, die bereits explizit oder implizit in einer verwendeten Application Definition enthalten sind, dürfen auch nicht hinzugefügt werden
          if (parentRevision != null) {
            Set<ApplicationEntryStorable> depAppEntries = getRequiredApplicationDefinitionEntries(applicationName, parentRevision);
            implAppEntries.removeAll(depAppEntries);
          }
        }

        appEntries.addAll(implAppEntries);
      }

      List<ApplicationEntryStorable> result = new ArrayList<ApplicationEntryStorable>(appEntries);
      synchronized (getApplicationDetailsCache()) {
        getApplicationDetailsCache().put(key, result);
        if (!loggedCacheSizeTooSmall && getApplicationDetailsCache().size() >= sizeOfApplicationDetailsCache.get() && getApplicationDetailsCache().creationTimeOfLastEvictedKey() > System.currentTimeMillis() - 10000) {
          logger.warn("performance: xfmg.xfctrl.appmgmt.appdetails.cache.size reached. (will not be logged again)");
          loggedCacheSizeTooSmall = true;
        }
      }
      
      return result;

    } catch (XynaException e) {
      //FIXME fehler weiterwerfen? wieso werden runtimeexceptions anders behandelt?
      logger.error("Can't read entries of the application.", e);
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Can't close connection.", e);
      }
    }
    return null;
  }


  private Set<ApplicationEntryStorable> getAllImplicitApplicationEntries(Collection<ApplicationEntryStorable> appEntries,
                                                                         String applicationName, String version, Long parentRevision,
                                                                         boolean ignoreExceptions) throws PersistenceLayerException,
      Ex_FileAccessException, XPRC_XmlParsingException, XPRC_InvalidXmlMissingRequiredElementException {
    Set<ApplicationEntryStorable> dependencies = new TreeSet<ApplicationEntryStorable>(ApplicationEntryStorable.COMPARATOR);
    Set<ApplicationEntryStorable> alreadyProcessedEntries = new TreeSet<ApplicationEntryStorable>(ApplicationEntryStorable.COMPARATOR);

    Set<ApplicationEntryStorable> allEntries = new TreeSet<ApplicationEntryStorable>(ApplicationEntryStorable.COMPARATOR);
    allEntries.addAll(appEntries);

    //Evtl. werden nicht alle XMOMEntries über das DependencyRegister gefunden (z.B. weil ein
    //Ordertype umkonfiguriert wurde und der ursprüngliche Workflow von keinem Objekt mehr abhängt).
    //Daher hier alle deployed XMOMEntries suchen (falls wir nicht in einem Workspace sind)
    if (parentRevision == null) {
      List<ApplicationEntryStorable> xmomEntries = getAllXMOMEntries(applicationName, version);
      dependencies.addAll(xmomEntries);
      allEntries.addAll(xmomEntries);

      /*
       * es können objekte deployed sein, die nicht in den appentries enthalten sind, und auch nicht von den appentries
       * über das dependencyregister erreichbar sind und die nicht in der workflowdatabase enthalten sind.
       * beispiel:
       * bei importapplication funktioniert das deployment eines services nicht
       * die (alleinigen) expliziten appentries sind workflows, die noch nicht deployed sind
       * removeapplication sucht von den expliziten appentries aus -> die sind aber nochnicht deployed
       *      - in der wf-db ist der service nicth zu finden, weil er nen fehler beim deployment hatte
       *      - im dependencyregister findet man ihn aber
       * 
       * 
       * d.h. es gibt:
       * - objekte, die deployed sein sollten (workflowdatabase)
       * - objekte, die deployed sind (dependencyregister)
       */
      Long revision;
      try {
        revision = revisionManagement.getRevision(new Application(applicationName, version));
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new RuntimeException(e);
      }

      Set<DependencyNode> deployed =
          new HashSet<DependencyNode>(dependencyRegister.getDependencyNodesByType(DependencySourceType.DATATYPE, revision));
      deployed.addAll(dependencyRegister.getDependencyNodesByType(DependencySourceType.WORKFLOW, revision));
      deployed.addAll(dependencyRegister.getDependencyNodesByType(DependencySourceType.XYNAEXCEPTION, revision));
      for (DependencyNode node : deployed) {
        ApplicationEntryStorable appEntry =
            ApplicationEntryStorable.create(applicationName, version, node.getUniqueName(), convertXMOMTypeToApplicationEntryType(node));
        dependencies.add(appEntry);
        allEntries.add(appEntry);
      }
    } else {
      //übergebene version muss nicht stimmen - macht aber nichts.
      version = getWorkingsetVersionName(applicationName, parentRevision);
    }

    //Abhängigkeiten für alle explitziten ApplicationEntries und alle XMOMEntries suchen
    for (ApplicationEntryStorable aes : allEntries) {
      try {
        dependencies.addAll(getDependencies(aes, parentRevision, alreadyProcessedEntries));
      } catch (Ex_FileAccessException e) {
        ignoreOrThrow(ignoreExceptions, e, aes);
      } catch (XPRC_XmlParsingException e) {
        ignoreOrThrow(ignoreExceptions, e, aes);
      } catch (XPRC_InvalidXmlMissingRequiredElementException e) {
        ignoreOrThrow(ignoreExceptions, e, aes);
      } catch (RuntimeException e) {
        ignoreOrThrow(ignoreExceptions, e, aes);
      } catch (Error e) {
        ignoreOrThrow(ignoreExceptions, e, aes);
      }
    }

    // TODO : alreadyProcessedEntries should bethe same. if not then there is something wrong. so why do i need to keep both?
    int oldSize = dependencies.size();
    dependencies.addAll(alreadyProcessedEntries);

    if (dependencies.size() != oldSize) {
      logger.warn("Dependencies of application could not be fetched consistently.");
    }

    return dependencies;
  }


  private <E extends Throwable> void ignoreOrThrow(boolean ignoreExceptions, E e, ApplicationEntryStorable aes) throws E {
    if (ignoreExceptions) {
      logger.trace("could not get dependencies of " + aes.getTypeAsEnum() + " " + aes.getName(), e);
    } else {
      throw e;
    }

  }


  private Collection<ApplicationEntryStorable> getDependencies(ApplicationEntryStorable aes, Long parentRevision,
                                                               Set<ApplicationEntryStorable> alreadyProcessedEntries)
      throws PersistenceLayerException, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_InvalidXmlMissingRequiredElementException {
    switch (aes.getTypeAsEnum()) {
      case FILTERINSTANCE :
        return getFilterInstanceDependencies(aes.getName(), aes.getApplication(), aes.getVersion(), parentRevision, alreadyProcessedEntries);
      case TRIGGERINSTANCE :
        return getTriggerInstanceDependencies(aes.getName(), aes.getApplication(), aes.getVersion(), parentRevision,
                                              alreadyProcessedEntries);
      case FILTER :
        return getFilterDependencies(aes.getName(), aes.getApplication(), aes.getVersion(), parentRevision, alreadyProcessedEntries);
      case TRIGGER :
        return getTriggerDependencies(aes.getName(), aes.getApplication(), aes.getVersion(), parentRevision, alreadyProcessedEntries);
      case DATATYPE :
        return getXMOMDependencies(aes.getName(), DependencySourceType.DATATYPE, aes.getApplication(), aes.getVersion(), parentRevision,
                                   alreadyProcessedEntries);
      case EXCEPTION :
        return getXMOMDependencies(aes.getName(), DependencySourceType.XYNAEXCEPTION, aes.getApplication(), aes.getVersion(),
                                   parentRevision, alreadyProcessedEntries);
      case WORKFLOW :
        Set<ApplicationEntryStorable> xmomDep =
            getXMOMDependencies(aes.getName(), DependencySourceType.WORKFLOW, aes.getApplication(), aes.getVersion(), parentRevision,
                                alreadyProcessedEntries);
        try {
          //default ordertype
          xmomDep.addAll(getOrderTypeDependencies(GenerationBase.transformNameForJava(aes.getName()), aes.getApplication(),
                                                  aes.getVersion(), parentRevision, alreadyProcessedEntries));
        } catch (XPRC_InvalidPackageNameException e) {
          throw new RuntimeException(e);
        }
        return xmomDep;
      case ORDERTYPE :
        return getOrderTypeDependencies(aes.getName(), aes.getApplication(), aes.getVersion(), parentRevision, alreadyProcessedEntries);
      case ORDERINPUTSOURCE :
        return getOrderInputSourceDependencies(aes.getName(), aes.getApplication(), aes.getVersion(), parentRevision,
                                               alreadyProcessedEntries);
      default :
        return new ArrayList<ApplicationEntryStorable>();
    }
  }


  /**
   * Liefert alle deployed Workflows, Datatypes und Exceptions einer RuntimeApplication
   */
  private List<ApplicationEntryStorable> getAllXMOMEntries(String applicationName, String versionName) throws PersistenceLayerException {
    Long revision;
    try {
      revision = revisionManagement.getRevision(new Application(applicationName, versionName));
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new IllegalArgumentException("application unknown", e);
    }

    WorkflowDatabase wdb = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getWorkflowDatabase();

    List<ApplicationEntryStorable> xmomEntries = new ArrayList<ApplicationEntryStorable>();
    //deployed Workflows suchen
    Collection<String> workflows = wdb.getDeployedWfs().get(revision);
    if (workflows != null) {
      for (String name : workflows) {
        xmomEntries.add(ApplicationEntryStorable.create(applicationName, versionName, name, ApplicationEntryType.WORKFLOW));
      }
    }

    //Datatypes suchen
    List<String> datatypes = wdb.getDeployedDatatypes().get(revision);
    if (datatypes != null) {
      for (String name : datatypes) {
        xmomEntries.add(ApplicationEntryStorable.create(applicationName, versionName, name, ApplicationEntryType.DATATYPE));
      }
    }

    //Exceptions suchen
    List<String> exceptions = wdb.getDeployedExceptions().get(revision);
    if (exceptions != null) {
      for (String name : exceptions) {
        xmomEntries.add(ApplicationEntryStorable.create(applicationName, versionName, name, ApplicationEntryType.EXCEPTION));
      }
    }

    return xmomEntries;
  }

  /**
   * @param parentRevision null falls RTA, != null falls Application Definition
   */
  private Set<ApplicationEntryStorable> getTriggerDependencies(String name, String applicationName, String version, Long parentRevision,
                                                               Set<ApplicationEntryStorable> alreadyProcessedEntries) {
    long revision = getRevisionOfApplicationOrParentWorkspace(applicationName, version, parentRevision);

    XynaActivationTrigger at = XynaFactory.getInstance().getActivation().getActivationTrigger();

    Set<ApplicationEntryStorable> appEntries = new TreeSet<ApplicationEntryStorable>(ApplicationEntryStorable.COMPARATOR);

    TriggerInformation ti;
    try {
      ti = at.getTriggerInformation(name, revision, false);
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    } catch (XACT_TriggerNotFound e) {
      throw new RuntimeException(e);
    }
    if (ti.getSharedLibs() != null) {
      for (String sharedLib : ti.getSharedLibs()) {
        ApplicationEntryStorable sharedLibEntry =
            ApplicationEntryStorable.create(applicationName, version, parentRevision, sharedLib, ApplicationEntryType.SHAREDLIB);

        if (!alreadyProcessedEntries.contains(sharedLibEntry)) {
          appEntries.add(sharedLibEntry);
          alreadyProcessedEntries.add(sharedLibEntry);
        }
      }
    }

    // additional dependencies verarbeiten
    appEntries.addAll(getAdditionalDependencies(ti.getAdditionalDependencies(), applicationName, version, parentRevision,
                                                alreadyProcessedEntries));

    return appEntries;
  }


  private Set<ApplicationEntryStorable> getFilterDependencies(String filterName, String applicationName, String version,
                                                              Long parentRevision, Set<ApplicationEntryStorable> alreadyProcessedEntries) {

    long revision = getRevisionOfApplicationOrParentWorkspace(applicationName, version, parentRevision);

    Set<ApplicationEntryStorable> appEntries = new TreeSet<ApplicationEntryStorable>(ApplicationEntryStorable.COMPARATOR);
    XynaActivationTrigger at = XynaFactory.getInstance().getActivation().getActivationTrigger();

    FilterInformation fi;
    try {
      fi = at.getFilterInformation(filterName, revision, false);
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    } catch (XACT_FilterNotFound e) {
      throw new RuntimeException(e);
    }
    if (fi.getSharedLibs() != null) {
      for (String sharedLib : fi.getSharedLibs()) {
        ApplicationEntryStorable sharedLibEntry =
            ApplicationEntryStorable.create(applicationName, version, parentRevision, sharedLib, ApplicationEntryType.SHAREDLIB);

        if (!alreadyProcessedEntries.contains(sharedLibEntry)) {
          appEntries.add(sharedLibEntry);
          alreadyProcessedEntries.add(sharedLibEntry);
        }
      }
    }


    TriggerInformation ti;
    try {
      ti = at.getTriggerInformation(fi.getTriggerName(), revision, false);
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    } catch (XACT_TriggerNotFound e) {
      ti = null;
    }
    if (ti != null) {
      ApplicationEntryStorable triggerEntry =
          ApplicationEntryStorable.create(applicationName, version, parentRevision, ti.getTriggerName(), ApplicationEntryType.TRIGGER);
      if (!alreadyProcessedEntries.contains(triggerEntry)) {
        appEntries.add(triggerEntry);
        alreadyProcessedEntries.add(triggerEntry);

        appEntries.addAll(getTriggerDependencies(ti.getTriggerName(), applicationName, version, parentRevision, alreadyProcessedEntries));
      }
    }

    // additional dependencies verarbeiten
    appEntries.addAll(getAdditionalDependencies(fi.getAdditionalDependencies(), applicationName, version, parentRevision,
                                                alreadyProcessedEntries));

    return appEntries;
  }


  private Set<ApplicationEntryStorable> getTriggerInstanceDependencies(String triggerInstanceName, String applicationName, String version,
                                                                       Long parentRevision,
                                                                       Set<ApplicationEntryStorable> alreadyProcessedEntries)
      throws PersistenceLayerException, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_InvalidXmlMissingRequiredElementException {

    long revision = getRevisionOfApplicationOrParentWorkspace(applicationName, version, parentRevision);

    Set<ApplicationEntryStorable> appEntries = new TreeSet<ApplicationEntryStorable>(ApplicationEntryStorable.COMPARATOR);
    XynaActivationTrigger at = XynaFactory.getInstance().getActivation().getActivationTrigger();

    TriggerInstanceInformation tii = at.getTriggerInstanceInformation(triggerInstanceName, revision);
    if (tii == null) {
      try {
        throw new RuntimeException("Trigger instance " + triggerInstanceName + " not found in "
            + revisionManagement.getRuntimeContext(revision));
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new RuntimeException(e);
      }
    }

    TriggerInformation ti;
    try {
      ti = at.getTriggerInformation(tii.getTriggerName(), revision, false);
    } catch (XACT_TriggerNotFound e) {
      ti = null;
    }
    if (ti != null) {
      ApplicationEntryStorable triggerEntry =
          ApplicationEntryStorable.create(applicationName, version, parentRevision, ti.getTriggerName(), ApplicationEntryType.TRIGGER);

      if (!alreadyProcessedEntries.contains(triggerEntry)) {
        appEntries.add(triggerEntry);
        alreadyProcessedEntries.add(triggerEntry);

        appEntries.addAll(getTriggerDependencies(ti.getTriggerName(), applicationName, version, parentRevision, alreadyProcessedEntries));
      }
    }

    return appEntries;
  }


  private Set<ApplicationEntryStorable> getFilterInstanceDependencies(String filterInstanceName, String applicationName, String version,
                                                                      Long parentRevision,
                                                                      Set<ApplicationEntryStorable> alreadyProcessedEntries)
      throws PersistenceLayerException, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_InvalidXmlMissingRequiredElementException {

    long revision = getRevisionOfApplicationOrParentWorkspace(applicationName, version, parentRevision);

    Set<ApplicationEntryStorable> appEntries = new TreeSet<ApplicationEntryStorable>(ApplicationEntryStorable.COMPARATOR);

    XynaActivationTrigger at = XynaFactory.getInstance().getActivation().getActivationTrigger();
    FilterInstanceInformation fii = at.getFilterInstanceInformation(filterInstanceName, revision);
    if (fii == null) {
      try {
        throw new RuntimeException("Filter instance " + filterInstanceName + " not found in "
            + revisionManagement.getRuntimeContext(revision));
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new RuntimeException(e);
      }
    }

    FilterInformation fi;
    try {
      fi = at.getFilterInformation(fii.getFilterName(), revision, false);
    } catch (XACT_FilterNotFound e) {
      fi = null;
    }
    if (fi != null) {
      ApplicationEntryStorable aesFilter =
          ApplicationEntryStorable.create(applicationName, version, parentRevision, fi.getFilterName(), ApplicationEntryType.FILTER);
      if (!alreadyProcessedEntries.contains(aesFilter)) {
        alreadyProcessedEntries.add(aesFilter);
        appEntries.add(aesFilter);
        appEntries.addAll(getFilterDependencies(fi.getFilterName(), applicationName, version, parentRevision, alreadyProcessedEntries));
      }
    } //else andere revision, ignorieren
    TriggerInstanceInformation tii = at.getTriggerInstanceInformation(fii.getTriggerInstanceName(), revision);
    if (tii != null) {
      ApplicationEntryStorable aesTriggerInstance =
          ApplicationEntryStorable.create(applicationName, version, parentRevision, tii.getTriggerInstanceName(),
                                       ApplicationEntryType.TRIGGERINSTANCE);
      if (!alreadyProcessedEntries.contains(aesTriggerInstance)) {
        alreadyProcessedEntries.add(aesTriggerInstance);
        appEntries.add(aesTriggerInstance);
        appEntries.addAll(getTriggerInstanceDependencies(tii.getTriggerInstanceName(), applicationName, version, parentRevision,
                                                         alreadyProcessedEntries));
      }
    } //else andere revision, ignorieren

    return appEntries;
  }


  private long getRevisionOfApplicationOrParentWorkspace(String applicationName, String version, Long parentRevision) {
    RuntimeContext runtimeContext;
    Long revision;
    try {
      if (parentRevision != null) { // ApplicationDefinition
        String workingSetVersion = getWorkingsetVersionName(applicationName, parentRevision);
        if (workingSetVersion != null && workingSetVersion.equals(version)) {
          runtimeContext = revisionManagement.getRuntimeContext(parentRevision);
          revision = parentRevision;
        } else {
          throw new IllegalArgumentException("application unknown");
        }
      } else { // RuntimeApplication
        runtimeContext = new Application(applicationName, version);
        revision = revisionManagement.getRevision(runtimeContext);
      }
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new IllegalArgumentException("application unknown", e);
    }

    return revision;
  }


  /**
   * Liefert die Revision einer RuntimeApplication bzw. die parentRevision, falls es sich um eine Application Definition handelt
   * @param applicationName
   * @param versionName
   * @param parentRevision
   * @return
   */
  private Long getRevision(String applicationName, String versionName, Long parentRevision) {
    try {
      return revisionManagement.getRevision(applicationName, versionName, null);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      String workingSetName = getWorkingsetVersionName(applicationName, parentRevision);
      if (workingSetName != null && workingSetName.equals(versionName)) {
        return parentRevision;
      } else {
        throw new RuntimeException(e);
      }
    }
  }


  private Set<ApplicationEntryStorable> getXMOMDependencies(String uniqueName, DependencySourceType sourceType, String applicationName,
                                                            String version, Long parentRevision,
                                                            Set<ApplicationEntryStorable> alreadyProcessedEntries) {
    Set<ApplicationEntryStorable> appEntries = new TreeSet<ApplicationEntryStorable>(ApplicationEntryStorable.COMPARATOR);
    Set<DependencyNode> depNodes;
    long revision = getRevision(applicationName, version, parentRevision);

    depNodes = dependencyRegister.getAllUsedNodesSameRevision(uniqueName, sourceType, true, false, revision);

    for (DependencyNode node : depNodes) {
      ApplicationEntryStorable appEntry =
          ApplicationEntryStorable.create(applicationName, version, parentRevision, node.getUniqueName(),
                                       convertXMOMTypeToApplicationEntryType(node));

      if (!alreadyProcessedEntries.contains(appEntry)) {
        appEntries.add(appEntry);
        alreadyProcessedEntries.add(appEntry);

        if (node.getType() == DependencySourceType.WORKFLOW) {
          try {
            appEntries.addAll(getOrderTypeDependencies(GenerationBase.transformNameForJava(node.getUniqueName()), applicationName, version,
                                                       parentRevision, alreadyProcessedEntries));
          } catch (XPRC_InvalidPackageNameException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }

    return appEntries;
  }


  private List<ApplicationEntryStorable> getOrderTypeDependencies(String uniqueName, String applicationName, String version,
                                                                  Long parentRevision, Set<ApplicationEntryStorable> alreadyProcessedEntries) {
    List<ApplicationEntryStorable> appEntries = new ArrayList<ApplicationEntryStorable>();
    ApplicationEntryStorable defaultOrderType =
        ApplicationEntryStorable.create(applicationName, version, parentRevision, uniqueName, ApplicationEntryType.ORDERTYPE);

    if (!alreadyProcessedEntries.contains(defaultOrderType)) {
      appEntries.add(defaultOrderType);
      alreadyProcessedEntries.add(defaultOrderType);
      appEntries.addAll(getXMOMDependencies(uniqueName, DependencySourceType.ORDERTYPE, applicationName, version, parentRevision,
                                            alreadyProcessedEntries));

      RuntimeContext runtimeContext;
      Long revision;
      try {
        if (parentRevision != null) { // ApplicationDefinition
          String workingSetVersion = getWorkingsetVersionName(applicationName, parentRevision);
          if (workingSetVersion != null && workingSetVersion.equals(version)) {
            runtimeContext = revisionManagement.getRuntimeContext(parentRevision);
          } else {
            throw new IllegalArgumentException("application unknown");
          }
          revision = parentRevision;
        } else { // RuntimeApplication
          runtimeContext = new Application(applicationName, version);
          revision = revisionManagement.getRevision(runtimeContext); // Überprüfung, ob Revision existiert
        }
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new IllegalArgumentException("application unknown", e);
      }

      List<Capacity> capacityList =
          XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getCapacityMappingDatabase()
              .getCapacities(new DestinationKey(uniqueName, runtimeContext));

      for (Capacity cap : capacityList) {
        ApplicationEntryStorable capacityEntry =
            ApplicationEntryStorable.create(applicationName, version, parentRevision, cap.getCapName(), ApplicationEntryType.CAPACITY);

        if (!alreadyProcessedEntries.contains(capacityEntry)) {
          appEntries.add(capacityEntry);
          alreadyProcessedEntries.add(capacityEntry);
        }
      }

      //OrderInputSources
      ExecutionDispatcher executionDispatcher =
          XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution().getExecutionEngineDispatcher();
      OrderInputSourceManagement oism =
          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderInputSourceManagement();

      DestinationKey dk = new DestinationKey(uniqueName, runtimeContext);
      try {
        DestinationValue dv = executionDispatcher.getDestination(dk);

        List<String> ois = oism.getReferencedOrderInputSources(dv, revision);
        for (String inputSource : ois) {
          ApplicationEntryStorable inputSourceEntry =
              ApplicationEntryStorable.create(applicationName, version, parentRevision, inputSource, ApplicationEntryType.ORDERINPUTSOURCE);

          if (!alreadyProcessedEntries.contains(inputSourceEntry)) {
            appEntries.addAll(getOrderInputSourceDependencies(inputSource, applicationName, version, parentRevision,
                                                              alreadyProcessedEntries));
          }
        }
      } catch (XynaException e) {
        logger.trace("Could not get used order input sources for orderType " + uniqueName, e);
      }
    }

    return appEntries;
  }


  private Collection<ApplicationEntryStorable> getOrderInputSourceDependencies(String uniqueName, String applicationName,
                                                                               String versionName, Long parentRevision,
                                                                               Set<ApplicationEntryStorable> alreadyProcessedEntries) {
    Set<ApplicationEntryStorable> appEntries = new TreeSet<ApplicationEntryStorable>(ApplicationEntryStorable.COMPARATOR);
    ApplicationEntryStorable appEntry =
        ApplicationEntryStorable.create(applicationName, versionName, parentRevision, uniqueName, ApplicationEntryType.ORDERINPUTSOURCE);

    if (!alreadyProcessedEntries.contains(appEntry)) {
      appEntries.add(appEntry);
      alreadyProcessedEntries.add(appEntry);

      //alle verwendeten Ordertypes mit ihren Dependencies hinzufügen
      String deploymentItemStateName = OrderInputSourceManagement.convertNameToUniqueDeploymentItemStateName(appEntry.getName());

      long revision = getRevision(applicationName, versionName, parentRevision);
      DeploymentItemStateImpl diis =
          (DeploymentItemStateImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
              .getDeploymentItemStateManagement().get(deploymentItemStateName, revision);
      if (diis != null) {
        Set<String> usedOrderTypes = diis.getUsedOrderTypes(DeploymentLocation.DEPLOYED);
        
        XynaProcessingBase proc = XynaFactory.getInstance().getProcessing();
        RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
        for (String ot : usedOrderTypes) {
          try {
            DestinationKey dk = new DestinationKey(ot, revMgmt.getRuntimeContext(revision));
            DispatcherEntry de = proc.getDestination(DispatcherIdentification.Execution, dk, false);
            if (de != null) { // only add if resolvable in own revision
              appEntries.addAll(getOrderTypeDependencies(ot, applicationName, versionName, parentRevision, alreadyProcessedEntries));
            }
          } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
            // only add if resolvable in own revision
          } catch (XPRC_DESTINATION_NOT_FOUND e) {
            // only add if resolvable in own revision
          }
        }
      }
    }

    return appEntries;
  }


  private List<ApplicationEntryStorable> getAdditionalDependencies(AdditionalDependencyContainer additionalDependencies,
                                                                   String applicationName, String version, Long parentRevision,
                                                                   Set<ApplicationEntryStorable> alreadyProcessedEntries) {
    List<ApplicationEntryStorable> appEntries = new ArrayList<ApplicationEntryStorable>();

    if (additionalDependencies == null) {
      return appEntries;
    }

    Set<String> datatypes = additionalDependencies.getAdditionalDependencies(AdditionalDependencyType.DATATYPE);
    Set<String> workflows = additionalDependencies.getAdditionalDependencies(AdditionalDependencyType.WORKFLOW);
    Set<String> exceptions = additionalDependencies.getAdditionalDependencies(AdditionalDependencyType.EXCEPTION);
    Set<String> xynaproperty = additionalDependencies.getAdditionalDependencies(AdditionalDependencyType.XYNA_PROPERTY);
    Set<String> ordertypes = additionalDependencies.getAdditionalDependencies(AdditionalDependencyType.ORDERTYPE);

    for (String entry : xynaproperty) {
      ApplicationEntryStorable propertyEntry =
          ApplicationEntryStorable.create(applicationName, version, parentRevision, entry, ApplicationEntryType.XYNAPROPERTY);

      if (!alreadyProcessedEntries.contains(propertyEntry)) {
        appEntries.add(propertyEntry);
        alreadyProcessedEntries.add(propertyEntry);
      }
    }

    RuntimeContextDependencyManagement rcdm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
    Long revision = getRevision(applicationName, version, parentRevision);
    
    for (String entry : datatypes) {
      if (!revision.equals(rcdm.getRevisionDefiningXMOMObject(entry, revision))) {
        continue; //falsche revision
      }
      ApplicationEntryStorable dataTypeEntry =
          ApplicationEntryStorable.create(applicationName, version, parentRevision, entry, ApplicationEntryType.DATATYPE);

      if (!alreadyProcessedEntries.contains(dataTypeEntry)) {
        appEntries.add(dataTypeEntry);
        alreadyProcessedEntries.add(dataTypeEntry);
        appEntries.addAll(getXMOMDependencies(entry, DependencySourceType.DATATYPE, applicationName, version, parentRevision,
                                              alreadyProcessedEntries));
      }
    }

    for (String entry : exceptions) {
      if (!revision.equals(rcdm.getRevisionDefiningXMOMObject(entry, revision))) {
        continue; //falsche revision
      }
      ApplicationEntryStorable exceptionEntry =
          ApplicationEntryStorable.create(applicationName, version, parentRevision, entry, ApplicationEntryType.EXCEPTION);

      if (!alreadyProcessedEntries.contains(exceptionEntry)) {
        appEntries.add(exceptionEntry);
        alreadyProcessedEntries.add(exceptionEntry);
        appEntries.addAll(getXMOMDependencies(entry, DependencySourceType.XYNAEXCEPTION, applicationName, version,
                                              parentRevision, alreadyProcessedEntries));
      }
    }

    for (String entry : workflows) {
      if (!revision.equals(rcdm.getRevisionDefiningXMOMObject(entry, revision))) {
        continue; //falsche revision
      }
      ApplicationEntryStorable workflowEntry =
          ApplicationEntryStorable.create(applicationName, version, parentRevision, entry, ApplicationEntryType.WORKFLOW);

      if (!alreadyProcessedEntries.contains(workflowEntry)) {
        appEntries.add(workflowEntry);
        alreadyProcessedEntries.add(workflowEntry);
        appEntries.addAll(getXMOMDependencies(entry, DependencySourceType.WORKFLOW, applicationName, version, parentRevision,
                                              alreadyProcessedEntries));

        try {
          appEntries.addAll(getOrderTypeDependencies(GenerationBase.transformNameForJava(entry), applicationName, version,
                                                     parentRevision, alreadyProcessedEntries));
        } catch (XPRC_InvalidPackageNameException e) {
          throw new RuntimeException(e);
        }
      }
    }

    for (String entry : ordertypes) {
      appEntries.addAll(getOrderTypeDependencies(entry, applicationName, version, parentRevision, alreadyProcessedEntries));
    }

    return appEntries;
  }


  public Collection<ApplicationStorable> listApplicationStorables() throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      return con.loadCollection(ApplicationStorable.class);
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Can't close connection.", e);
      }
    }
  }


  public List<ApplicationInformation> listApplications(boolean withObjectCounts, boolean includeProblems) {
    List<ApplicationInformation> appInfos = new ArrayList<ApplicationInformation>();
    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      Collection<ApplicationStorable> allApplications = con.loadCollection(ApplicationStorable.class);

      if (withObjectCounts) {
        PreparedQuery<? extends OrderCount> queryDefinition =
            queryCache.getQueryFromCache(sqlcountApplicationDefinitionEntries, con, OrderCount.getCountReader());
        PreparedQuery<? extends OrderCount> queryRuntimeApplication =
            queryCache.getQueryFromCache(sqlcountRuntimeApplicationEntries, con, OrderCount.getCountReader());

        for (ApplicationStorable app : allApplications) {
          ApplicationInformation appInfo = getApplicationInformation(app, includeProblems);
          OrderCount count;
          if (app.isApplicationDefinition()) {
            count = con.queryOneRow(queryDefinition, new Parameter(app.getName(), app.getParentRevision()));
          } else {
            count = con.queryOneRow(queryRuntimeApplication, new Parameter(app.getName(), app.getVersion()));
          }
          appInfo.setObjectCount(count.getCount());
          appInfos.add(appInfo);
        }
      } else {
        for (ApplicationStorable app : allApplications) {
          ApplicationInformation appInfo = getApplicationInformation(app, includeProblems);
          appInfos.add(appInfo);
        }
      }
    } catch (PersistenceLayerException e) {
      logger.error("Error while reading application database.", e);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      logger.error("Error while reading application.", e);
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Can't close connection.", e);
      }
    }
    return appInfos;
  }


  public ApplicationInformation getApplicationInformation(String name, String version) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    ODSConnection con = ods.openConnection();
    try {
      ApplicationStorable application = queryRuntimeApplicationStorable(name, version, con);
      if (application == null) {
        throw new XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY(name + " - " + version, ApplicationStorable.TABLE_NAME);
      }
      return getApplicationInformation(application, false);
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Can't close connection.", e);
      }
    }
  }
  
  
  public ApplicationInformation getApplicationDefinitionInformation(String name, Long parentRevision) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    ODSConnection con = ods.openConnection();
    try {
      ApplicationStorable application = queryApplicationDefinitionStorable(name, parentRevision, con);
      if (application == null) {
        throw new XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY(name + " - " + parentRevision, ApplicationStorable.TABLE_NAME);
      }
      return getApplicationInformation(application, false);
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Can't close connection.", e);
      }
    }
  }

  
  public ApplicationInformation getApplicationInformation(ApplicationStorable application, boolean includeProblems) throws PersistenceLayerException,
      XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    ApplicationInformation appInfo;
    RuntimeDependencyContext rc;

    if (application.isApplicationDefinition()) {
      Workspace parentWorkspace;
      try {
        parentWorkspace = revisionManagement.getWorkspace(application.getParentRevision());
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        logger.warn("Could not find workspace for revision " + application.getParentRevision(), e);
        parentWorkspace = new Workspace("not found");
      }
      appInfo =
          new ApplicationDefinitionInformation(application.getName(), application.getVersion(), parentWorkspace, application.getComment());
      rc = new ApplicationDefinition(application.getName(), parentWorkspace);
    } else {
      ApplicationState state = application.getStateAsEnum();
      appInfo = new ApplicationInformation(application.getName(), application.getVersion(), state, application.getComment());
      appInfo.setRemoteStub(application.getRemoteStub());
      rc = new Application(application.getName(), application.getVersion());
    }

    //RuntimeContext Requirements und Problems
    Collection<RuntimeDependencyContext> reqs =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement().getRequirements(rc);
    appInfo.setRequirements(reqs);
    if (includeProblems) {
      List<RuntimeContextProblem> problems = new ArrayList<RuntimeContextProblem>(revisionManagement.getRuntimeContextProblems(rc));
      appInfo.setProblems(problems);

      //falls es Problems gibt, den Zustand auf WARNING oder ERROR setzen
      if (!problems.isEmpty()) {
        ApplicationState state = ApplicationState.WARNING;

        for (RuntimeContextProblem problem : problems) {
          if (problem.causeErrorStatus()) {
            //es gibt mindestens ein Problem, das zum Zustand ERROR führt
            state = ApplicationState.ERROR;
            break;
          }
        }
        appInfo.setState(state);
      }
    }

    return appInfo;
  }

  public List<ApplicationDefinitionInformation> listApplicationDefinitions(boolean includeProblems) {
    return listApplicationDefinitions(null, includeProblems);
  }

  public List<ApplicationDefinitionInformation> listApplicationDefinitions(Long revision) {
    return listApplicationDefinitions(revision, true);
  }

  public List<ApplicationDefinitionInformation> listApplicationDefinitions(Long revision, boolean includeProblems) {
    List<ApplicationDefinitionInformation> appInfos = new ArrayList<ApplicationDefinitionInformation>();
    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      Collection<ApplicationStorable> allApplications = con.loadCollection(ApplicationStorable.class);

      for (ApplicationStorable app : allApplications) {
        if (revision != null && !revision.equals(app.getParentRevision())) {
          continue; //falsche Revision
        }
        if (app.isApplicationDefinition()) {
          try {
            ApplicationDefinitionInformation appInfo = (ApplicationDefinitionInformation) getApplicationInformation(app, includeProblems);
            appInfos.add(appInfo);
          } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
            logger.warn("Could not find informations for application definition " + app.getName(), e);
          }
        }
      }
    } catch (PersistenceLayerException e) {
      logger.error("Error while reading application database.", e);
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Can't close connection.", e);
      }
    }
    return appInfos;
  }


  /**
   * Führt Aktionen aus die nach Änderungen von Dependencies mit ApplicationDefintions nötig sind.
   * Es wird der ApplicationDefintion-Cache für den ParentWorkspace geleert.
   * @param owner
   * @throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY
   */
  public void handleApplicationDefinitionDependencyChange(ApplicationDefinition owner) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    Long parentRevision = revisionManagement.getRevision(owner.getParentWorkspace());
    clearApplicationDefinitionCache(parentRevision);
    updateApplicationDetailsCache(parentRevision);
  }


  /**
   * Suche nach ApplicationDefinitions. Im Moment wird nur die Suche mit einem 
   * XMOMDatabase-Objekt (fqname, type, workspace) als Filter unterstützt.
   * 
   * TODO Da diese Suchfunktion von der GUI nicht verwendet wird, ist sie momentan "ausgeschaltet",
   * damit nicht unnötig ApplicationDefinitionInformation-Objekte im Memory gehalten werden.
   * Wenn die Funktionalität verwendet werden soll, sollte der Code angepasst werden, so dass nicht
   * die kompletten ApplicationDefinitionInformation gecached werden, da z.B. Requirements und Problems
   * an dieser Stelle uninteressant sind.
   * (MethodenImpl gelöscht, siehe rev &lt; 194447)
   */
  public SearchResult<ApplicationDefinitionInformation> searchApplicationDefinitions(SearchRequestBean searchRequest)
      throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    throw new UnsupportedOperationException("searchApplicationDefinitions currently not supported");
  }



  private void addObjectToApplicationDefinitionCache(XMOMDatabaseType type, String fqName, Long parentRevision,
                                                     ApplicationDefinitionInformation info) {
    Map<XMOMDatabaseType, Map<String, Set<ApplicationDefinitionInformation>>> appDefForRevision =
        applicationDefinitionCache.get(parentRevision);

    Map<String, Set<ApplicationDefinitionInformation>> appDefForType = appDefForRevision.get(type);
    if (appDefForType == null) {
      appDefForType = new HashMap<String, Set<ApplicationDefinitionInformation>>();
      appDefForRevision.put(type, appDefForType);
    }

    Set<ApplicationDefinitionInformation> appDefForObject = appDefForType.get(fqName);
    if (appDefForObject == null) {
      appDefForObject = new HashSet<ApplicationDefinitionInformation>();
      appDefForType.put(fqName, appDefForObject);
    }

    appDefForObject.add(info);
  }


  private void clearApplicationDefinitionCache(Long parentRevision) {
    synchronized (applicationDefinitionCache) {
      applicationDefinitionCache.remove(parentRevision);
    }
  }


  /**
   * Listet alle Applikationen auf.
   */
  public List<ApplicationInformation> listApplications() {
    return listApplications(true, true);
  }


  /**
   * Definiert eine Application und legt diesbzgl. ein Storeable-Obj an. Applikationsname darf noch nicht verwendet
   * werden.
   */
  public void defineApplication(String applicationName, String comment, Long parentRevision) throws XFMG_DuplicateApplicationName {
    defineApplication(applicationName, comment, parentRevision, null);
  }


  /**
   * Definiert eine Application und legt diesbzgl. ein Storeable-Obj an. Applikationsname darf noch nicht verwendet
   * werden.
   */
  public void defineApplication(String applicationName, String comment, Long parentRevision, String user)
      throws XFMG_DuplicateApplicationName {

    ODSConnection con = ods.openConnection();
    try {
      // prüfen, ob eine Applikation mit diesem Namen in diesem workspace schon existiert?
      Collection<ApplicationStorable> allApplications = con.loadCollection(ApplicationStorable.class);
      for (ApplicationStorable app : allApplications) {
        if (applicationName.equals(app.getName()) && parentRevision.equals(app.getParentRevision())) {
          throw new XFMG_DuplicateApplicationName(applicationName);
        }
      }

      // Applikation anlegen
      String version = WORKINGSET_VERSION_NAME;
      ApplicationStorable app = new ApplicationStorable(applicationName, version, comment, parentRevision);
      con.persistObject(app);
      applicationDefinitionVersionNameCache.put(new ApplicationDefinitionKey(app), new WorkingSet(version));
      con.commit();

      transferFromODSTypeToODSType(ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY, ApplicationStorable.TABLE_NAME,
                                   ApplicationStorable.class);

      new SingleRepositoryEvent(parentRevision)
          .addEvent(new ProjectCreationOrChangeProvider.BasicProjectCreationOrChangeEvent(EventType.APPLICATION_DEFINE, applicationName));
    } catch (PersistenceLayerException e) {
      logger.error("Can't define application.", e);
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Can't close connection.", e);
      }
    }

    //Multi-User-Event für RuntimeContext Änderung
    try {
      Workspace parentWorkspace = revisionManagement.getWorkspace(parentRevision);
      Publisher publisher = new Publisher(user);
      ApplicationDefinition appDef = new ApplicationDefinition(applicationName, parentWorkspace);
      publisher.publishRuntimeContextCreate(appDef);
      for (RuntimeContextChangeHandler rdcch : getRtCtxMgmt().getHandlers()) {
        try {
          rdcch.creation(appDef);
        } catch (Throwable t) {
          logger.error("Could not execute RuntimeContextChangeHandler " + rdcch, t);
        }
      }
    } catch (XynaException e) {
      logger.error("Failed to notify gui of runtime context change result", e);
    }
  }


  private ApplicationEntryType convertXMOMTypeToApplicationEntryType(DependencyNode entry) {
    switch (entry.getType()) {
      case WORKFLOW :
        return ApplicationEntryType.WORKFLOW;
      case XYNAEXCEPTION :
        return ApplicationEntryType.EXCEPTION;
      case DATATYPE :
        return ApplicationEntryType.DATATYPE;
      case FILTER :
        return ApplicationEntryType.FILTER;
      case SHAREDLIB :
        return ApplicationEntryType.SHAREDLIB;
      case XYNAPROPERTY :
        return ApplicationEntryType.XYNAPROPERTY;
      case TRIGGER :
        return ApplicationEntryType.TRIGGER;
      case ORDERTYPE :
        return ApplicationEntryType.ORDERTYPE;
      default :
        return null;
    }
  }


  private XMOMType convertDependencySourceTypeToXMOMType(DependencySourceType dependencySourceType) {
    switch (dependencySourceType) {
      case WORKFLOW :
        return XMOMType.WORKFLOW;
      case XYNAEXCEPTION :
        return XMOMType.EXCEPTION;
      case DATATYPE :
        return XMOMType.DATATYPE;
      default :
        return null;
    }
  }


  private XMOMType convertApplicationEntryTypeToXMOMType(ApplicationEntryType type) {
    switch (type) {
      case WORKFLOW :
        return XMOMType.WORKFLOW;
      case EXCEPTION :
        return XMOMType.EXCEPTION;
      case DATATYPE :
        return XMOMType.DATATYPE;
      case FORMDEFINITION :
        return XMOMType.FORM;
      default :
        return null;
    }
  }


  private ApplicationEntryType convertXMOMDatabaseTypeToApplicationEntryType(XMOMDatabaseType dbt) {
    switch (dbt) {
      case WORKFLOW :
        return ApplicationEntryType.WORKFLOW;
      case EXCEPTION :
        return ApplicationEntryType.EXCEPTION;
      case DATATYPE :
        return ApplicationEntryType.DATATYPE;
      default :
        return null;
    }
  }


  private DependencySourceType convertApplicationEntryTypeToDependencySourceType(ApplicationEntryType type) {
    switch (type) {
      case WORKFLOW :
        return DependencySourceType.WORKFLOW;
      case EXCEPTION :
        return DependencySourceType.XYNAEXCEPTION;
      case DATATYPE :
        return DependencySourceType.DATATYPE;
      case FILTER :
        return DependencySourceType.FILTER;
      case SHAREDLIB :
        return DependencySourceType.SHAREDLIB;
      case XYNAPROPERTY :
        return DependencySourceType.XYNAPROPERTY;
      case TRIGGER :
        return DependencySourceType.TRIGGER;
      case ORDERTYPE :
        return DependencySourceType.ORDERTYPE;
      default :
        return null;
    }
  }



  private ApplicationEntryStorable queryApplicationDefinitionEntryStorable(String application, Long parentRevision, String name,
                                                                           ApplicationEntryType type, ODSConnection con)
      throws PersistenceLayerException {
    PreparedQuery<? extends ApplicationEntryStorable> query =
        queryCache.getQueryFromCache(sqlGetApplicationDefinitionEntry, con, ApplicationEntryStorable.getStaticReader());

    return con.queryOneRow(query, new Parameter(application, name, type.toString(), parentRevision));
  }


  protected ApplicationStorable queryRuntimeApplicationStorable(String application, String version, ODSConnection con)
      throws PersistenceLayerException {
    PreparedQuery<? extends ApplicationStorable> query =
        queryCache.getQueryFromCache(sqlGetRuntimeApplication, con, ApplicationStorable.getStaticReader());

    return con.queryOneRow(query, new Parameter(application, version));
  }

  
  protected List<? extends ApplicationStorable> queryRuntimeApplicationStorableList(String application, 
                                                                                  ODSConnection con)
      throws PersistenceLayerException {
    PreparedQuery<? extends ApplicationStorable> query =
        queryCache.getQueryFromCache(sqlGetRuntimeApplicationsByName, con, ApplicationStorable.getStaticReader());

    return con.query(query, new Parameter(application), -1);
  }
  

  private ApplicationStorable queryApplicationDefinitionStorable(String application, Long parentRevision, ODSConnection con)
      throws PersistenceLayerException {
    PreparedQuery<? extends ApplicationStorable> query =
        queryCache.getQueryFromCache(sqlGetApplicationDefinition, con, ApplicationStorable.getStaticReader());

    return con.queryOneRow(query, new Parameter(application, parentRevision));
  }


  private List<ApplicationEntryStorable> queryAllRuntimeApplicationStorables(String application, String version, ODSConnection con)
      throws PersistenceLayerException {
    PreparedQuery<ApplicationEntryStorable> query =
        queryCache.getQueryFromCache(sqlGetRuntimeApplicationEntries, con, ApplicationEntryStorable.getStaticReader());
    return con.query(query, new Parameter(application, version), -1);
  }


  private List<? extends ApplicationEntryStorable> queryAllApplicationDefinitionStorables(String application, Long parentRevision,
                                                                                          ODSConnection con)
      throws PersistenceLayerException {
    PreparedQuery<? extends ApplicationEntryStorable> query =
        queryCache.getQueryFromCache(sqlGetApplicationDefinitionEntries, con, ApplicationEntryStorable.getStaticReader());
    return con.query(query, new Parameter(application, parentRevision), -1);
  }

  private ApplicationEntryStorable queryRuntimeApplicationEntryStorable(String applicationName, String versionName, String objectName, ApplicationEntryType type,
                                                                                          ODSConnection con)
      throws PersistenceLayerException {
    PreparedQuery<? extends ApplicationEntryStorable> query =
        queryCache.getQueryFromCache(sqlGetRuntimeApplicationEntry, con, ApplicationEntryStorable.getStaticReader());
    return con.queryOneRow(query, new Parameter(applicationName, versionName, objectName, type.toString()));
  }

  private static class WorkingSet {

    private final String name;


    private WorkingSet(String name) {
      this.name = name;
    }
  }

  private static class ApplicationDefinitionKey {

    private final String applicationName;
    private final Long parentRevision;


    private ApplicationDefinitionKey(String applicationName, Long parentRevision) {
      this.applicationName = applicationName;
      this.parentRevision = parentRevision;
    }


    private ApplicationDefinitionKey(ApplicationStorable application) {
      this.applicationName = application.getName();
      this.parentRevision = application.getParentRevision();
    }


    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((applicationName == null) ? 0 : applicationName.hashCode());
      result = prime * result + ((parentRevision == null) ? 0 : parentRevision.hashCode());
      return result;
    }


    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      ApplicationDefinitionKey other = (ApplicationDefinitionKey) obj;
      if (applicationName == null) {
        if (other.applicationName != null)
          return false;
      } else if (!applicationName.equals(other.applicationName))
        return false;
      if (parentRevision == null) {
        if (other.parentRevision != null)
          return false;
      } else if (!parentRevision.equals(other.parentRevision))
        return false;
      return true;
    }


  }


  private static final Map<ApplicationDefinitionKey, WorkingSet> applicationDefinitionVersionNameCache =
      new ConcurrentHashMap<ApplicationDefinitionKey, WorkingSet>();


  /**
   * sollte eigtl heissen: getVersionNameOfApplicationDefinition()
   * @return null, falls keine derartige application definition existiert, ansonsten der versionsname der application definition
   */
  public static String getWorkingsetVersionName(String applicationName, Long parentRevision) {
    ApplicationDefinitionKey applicationKey = new ApplicationDefinitionKey(applicationName, parentRevision);
    WorkingSet v = applicationDefinitionVersionNameCache.get(applicationKey);

    if (v == null) {
      ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
      try {
        Collection<ApplicationStorable> allApplications = con.loadCollection(ApplicationStorable.class);
        for (ApplicationStorable app : allApplications) {
          if (applicationName.equals(app.getName()) && app.isApplicationDefinition() && parentRevision.equals(app.getParentRevision())) {
            v = new WorkingSet(app.getVersion());
            break;
          }
        }
      } catch (PersistenceLayerException e) {
        logger.error("Could not read workingset version.", e);
      } finally {
        try {
          con.closeConnection();
        } catch (PersistenceLayerException e) {
          logger.warn("Could not close connection.", e);
        }
      }

      if (v == null) {
        v = new WorkingSet(null);
      }
      applicationDefinitionVersionNameCache.put(applicationKey, v);
    }

    return v.name;
  }


  static void output(PrintStream statusOutputStream, String text) {
    if (statusOutputStream != null) {
      try {
        statusOutputStream.println(text);
        statusOutputStream.flush();
      } catch (Exception e) {
        logger.warn("Failed to print status information to CLI: <" + text + ">", e);
      }
    }
  }


  public void appendOrderEntryInterfaces(StringBuilder output, String applicationName, String versionName)
      throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    long revision = revisionManagement.getRevision(new Application(applicationName, versionName));

    //startOrder
    output.append(" start order: RMI ");
    try {
      RevisionOrderControl.checkRmiClosed(revision);
      output.append("open");
    } catch (RuntimeException e) {
      output.append("closed");
    }

    output.append(", CLI ");
    try {
      RevisionOrderControl.checkCliClosed(revision);
      output.append("open");
    } catch (RuntimeException e) {
      output.append("closed");
    }
    output.append("\n");

    //Batch Prozesse
    RevisionOrderControl roc = new RevisionOrderControl(revision);
    Pair<Integer, Integer> batchProcesses = roc.countBatchProcesses();
    output.append(" batch processes: ").append(batchProcesses.getFirst()).append(" enabled, ").append(batchProcesses.getSecond())
        .append(" disabled").append("\n");

    //Crons
    Pair<Integer, Integer> crons = roc.countCrons();
    output.append(" cron like orders: ").append(crons.getFirst()).append(" enabled, ").append(crons.getSecond()).append(" disabled")
        .append("\n");

    //FrequencyControlledTasks
    int fcts = roc.getFrequencyControlledTaskIds().size();
    output.append(" frequency controlled tasks: ").append(fcts).append("\n");

    //Trigger
    Triple<Integer, Integer, Integer> triggerInstances = roc.countTriggerInstances();
    output.append(" trigger instances: ").append(triggerInstances.getFirst()).append(" enabled, ").append(triggerInstances.getSecond())
        .append(" disabled, ").append(triggerInstances.getThird()).append(" erroneous").append("\n");

    //Filter
    Triple<Integer, Integer, Integer> filterInstances = roc.countFilterInstances();
    output.append(" filter instances: ").append(filterInstances.getFirst()).append(" enabled, ").append(filterInstances.getSecond())
        .append(" disabled, ").append(filterInstances.getThird()).append(" erroneous");
  }


  // ######## Clusterfunktionalität ########

  private Map<String, Object> mapOfWaitObjects = new HashMap<String, Object>();
  private Map<String, Throwable> mapOfThrowables = new ConcurrentHashMap<String, Throwable>();

  private Lock mapOfWaitObjectsLock = new ReentrantLock();


  private abstract class ApplicationRunnable {

    public abstract void run() throws Throwable;
  }

  private static abstract class ApplicationRemoteRunnable {

    public abstract String run(ApplicationRemoteInterface remoteInterface) throws XynaException, RemoteException;
  }


  public void notifyRequestIsFinishedRemotly(String requestId, Throwable throwableObject) throws RemoteException {
    mapOfWaitObjectsLock.lock();
    try {
      if (throwableObject != null) {
        mapOfThrowables.put(requestId, throwableObject);
      }
      if (!mapOfWaitObjects.containsKey(requestId)) {
        if (logger.isDebugEnabled()) {
          logger.debug("No wait object found. It seems the request with id " + requestId + " was too fast");
        }
        mapOfWaitObjects.put(requestId, null);
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug("Got notified that request with id " + requestId + " is finished");
        }
        Object object = mapOfWaitObjects.get(requestId);
        synchronized (object) {
          object.notifyAll();
        }
      }
    } finally {
      mapOfWaitObjectsLock.unlock();
    }
  }


  /*
   * Die Methode leitet den RMI-Aufruf auf den anderen Knoten ein und wartet dann bis der andere Knoten die Abarbeitung notifiziert. 
   */
  private void startRequestOnOtherNodes(final ApplicationRemoteRunnable runnable) throws Throwable {

    logger.debug("Call other node for process request");

    List<String> requestIds =
        RMIClusterProviderTools.executeAndCumulate(clusterInstance, clusteredInterfaceId,
                                                   new RMIRunnable<String, ApplicationRemoteInterface, XynaException>() {

                                                     public String execute(ApplicationRemoteInterface clusteredInterface)
                                                         throws XynaException, RemoteException {
                                                       return runnable.run(clusteredInterface);
                                                     }
                                                   }, null);

    mapOfWaitObjectsLock.lock();
    try {
      for (String requestId : requestIds) {
        if (logger.isDebugEnabled()) {
          logger.debug("Got requestId " + requestId);
        }

        if (mapOfWaitObjects.containsKey(requestId)) {
          // offensichtlich ist die Abarbeitung schon fertig -> müssen nicht warten
          if (logger.isDebugEnabled()) {
            logger.debug("The request with id " + requestId + " has already been finished");
          }
          mapOfWaitObjects.remove(requestId);
        } else {
          Object waitObject = new Object();
          mapOfWaitObjects.put(requestId, waitObject);
          synchronized (waitObject) {
            if (logger.isDebugEnabled()) {
              logger.debug("Wait until request with id " + requestId + " is finished");
            }
            mapOfWaitObjectsLock.unlock();
            waitObject.wait();
            mapOfWaitObjectsLock.lock();
            if (logger.isDebugEnabled()) {
              logger.debug("Woke up. Request id was " + requestId);
            }
          }
          mapOfWaitObjects.remove(requestId);
          Throwable throwable = mapOfThrowables.get(requestId);
          if (throwable != null) {
            throw throwable;
          }
        }
      }
    } finally {
      mapOfWaitObjectsLock.unlock();
    }
  }


  /*
   * Die Ausführung von einigen RMI-Aufrufen soll auf dem Remote-Knoten asynchron erfolgen, weil sonst das RMI-Timeout zuschlangen könnte, wenn die
   * Ausführung zu lange dauert. 
   */
  private String startRequestInThread(final ApplicationRunnable runnable) {
    final int ownBinding = new XMOMVersionStorable().getLocalBinding(ODSConnectionType.DEFAULT);
    Thread thread = new Thread(new Runnable() {

      public void run() {
        Throwable throwable = null;
        final String requestId = ownBinding + "#" + Thread.currentThread().getId();
        try {
          if (logger.isDebugEnabled()) {
            logger.debug("Begin processing request with id " + requestId);
          }
          runnable.run();
          if (logger.isDebugEnabled()) {
            logger.debug("Finish processing request with id " + requestId);
          }
        } catch (Throwable e) {
          logger.error("Error while executing remotly request with id " + requestId, e);
          throwable = e;
        }
        if (currentClusterState == ClusterState.CONNECTED) {
          final Throwable throwableFinal = throwable;
          try {
            if (logger.isDebugEnabled()) {
              logger.debug("Notify other node of finishing request with id " + requestId);
            }
            RMIClusterProviderTools.executeAndCumulateNoException(clusterInstance, clusteredInterfaceId,
                                                                  new RMIRunnableNoException<Void, ApplicationRemoteInterface>() {

                                                                    public Void execute(ApplicationRemoteInterface clusteredInterface)
                                                                        throws RemoteException {
                                                                      clusteredInterface.notifyRequestIsFinishedRemotly(requestId,
                                                                                                                        throwableFinal);
                                                                      return null;
                                                                    }
                                                                  }, null);
          } catch (InvalidIDException e) {
            logger.error("Failed to notify other nodes that the request <" + requestId + "> is processed.", e);
          }
        }
      }
    });

    String requestId = ownBinding + "#" + thread.getId();
    thread.setName("Thread to process request with id " + requestId);
    if (logger.isDebugEnabled()) {
      logger.debug("Start new thread for request with id " + requestId);
    }
    thread.start();
    return requestId;
  }


  public String startApplicationRemotely(final String applicationName, final String versionName, final StartApplicationParameters params)
      throws RemoteException, XFMG_CouldNotStartApplication {

    return startRequestInThread(new ApplicationRunnable() {

      public void run() throws Throwable {
        params.setForceStartInInconsistentCluster(true);
        params.setGlobal(false);
        startApplication(applicationName, versionName, params);
      }
    });
  }


  public String stopApplicationRemotely(String applicationName, String versionName) throws RemoteException, XFMG_CouldNotStopApplication {
    return stopApplicationRemotely(applicationName, versionName, null);
  }


  public String stopApplicationRemotely(final String applicationName, final String versionName,
                                        EnumSet<OrderEntranceType> onlyOpenOrderEntranceTypes) throws RemoteException,
      XFMG_CouldNotStopApplication {
    final Optional<EnumSet<OrderEntranceType>> onlyOpenEntrances;
    if (onlyOpenOrderEntranceTypes == null) {
      onlyOpenEntrances = Optional.empty();
    } else {
      onlyOpenEntrances = new Optional<EnumSet<OrderEntranceType>>(onlyOpenOrderEntranceTypes);
    }

    return startRequestInThread(new ApplicationRunnable() {

      public void run() throws Throwable {
        stopApplication(applicationName, versionName, false, onlyOpenEntrances);
      }
    });
  }


  public String removeApplicationVersionRemotely(final String applicationName, final String versionName,
                                                 final RemoveApplicationParameters params) throws RemoteException,
      XFMG_CouldNotRemoveApplication {

    return startRequestInThread(new ApplicationRunnable() {

      public void run() throws Throwable {
        params.setGlobal(false);
        removeApplicationVersion(new ApplicationName(applicationName, versionName), params, false, null, new EmptyRepositoryEvent(), true);
      }
    });
  }


  public String importApplicationRemotely(final boolean force, final boolean excludeXynaProperties, final boolean excludeCapacities,
                                          final boolean importOnlyXynaProperties, final boolean importOnlyCapacities,
                                          RemoteInputStream applicationPackageStream) throws RemoteException,
      XFMG_CouldNotImportApplication, XFMG_DuplicateVersionForApplicationName, XFMG_CouldNotRemoveApplication,
      XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState, XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain {
    return importApplicationRemotely(force, excludeXynaProperties, excludeCapacities, importOnlyXynaProperties, importOnlyCapacities,
                                     false, applicationPackageStream);
  }


  public String importApplicationRemotely(final boolean force, final boolean excludeXynaProperties, final boolean excludeCapacities,
                                          final boolean importOnlyXynaProperties, final boolean importOnlyCapacities,
                                          final boolean regenerateCode, RemoteInputStream applicationPackageStream) throws RemoteException,
      XFMG_CouldNotImportApplication, XFMG_DuplicateVersionForApplicationName, XFMG_CouldNotRemoveApplication,
      XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState, XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain {
    return importApplicationRemotely(force, excludeXynaProperties, excludeCapacities, importOnlyXynaProperties, importOnlyCapacities,
                                     false, null, applicationPackageStream);
  }


  public String importApplicationRemotely(final boolean force, final boolean excludeXynaProperties, final boolean excludeCapacities,
                                          final boolean importOnlyXynaProperties, final boolean importOnlyCapacities,
                                          final boolean regenerateCode, final String user, RemoteInputStream applicationPackageStream)
      throws RemoteException, XFMG_CouldNotImportApplication, XFMG_DuplicateVersionForApplicationName, XFMG_CouldNotRemoveApplication,
      XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState, XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain {

    final File tmpFile;
    FileOutputStream fos = null;
    try {
      tmpFile = File.createTempFile("applicationImport", ".zip");
      tmpFile.deleteOnExit();
      if (logger.isDebugEnabled()) {
        logger.debug("Store rmi inputstream in file " + tmpFile.getAbsolutePath());
      }
      fos = new FileOutputStream(tmpFile);
      InputStream is = RemoteInputStreamClient.wrap(applicationPackageStream);
      byte[] buffer = new byte[1048576];
      int len;
      long bytesWritten = 0;
      while ((len = is.read(buffer)) != -1) {
        fos.write(buffer, 0, len);
        bytesWritten += len;
      }
      if (logger.isDebugEnabled()) {
        logger.debug("Wrote " + bytesWritten + " bytes to file " + tmpFile.getAbsolutePath());
      }
      buffer = null;
    } catch (IOException e) {
      logger.warn("Can't build application file.", e);
      throw new XFMG_CouldNotImportApplication("from remotly node", e);
    } finally {
      if (fos != null) {
        try {
          fos.close();
        } catch (IOException e) {
          logger.warn("Can't close file.", e);
        }
      }
    }

    return startRequestInThread(new ApplicationRunnable() {

      public void run() throws Throwable {
        importApplication(tmpFile.getAbsolutePath(), force, true, excludeXynaProperties, excludeCapacities, importOnlyXynaProperties,
                          importOnlyCapacities, false, false, regenerateCode, user, null, true, true);

        if (logger.isDebugEnabled()) {
          logger.debug("Delete temporary file " + tmpFile.getAbsolutePath());
        }
        tmpFile.delete();
      }
    });


  }


  public Boolean isApplicationInUseRemotely(String applicationName, String versionName, boolean force) throws RemoteException,
      XFMG_RunningOrdersException {
    try {
      Long revision;
      try {
        revision = revisionManagement.getRevision(applicationName, versionName, null);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        // wenn nicht bekannt, können auch keine Workflows in Benutzung sein!
        return false;
      }

      //Behandlung laufender Aufträge
      revisionManagement.handleRunningOrders(revision, force);

      return false; //es liefen keine Aufträge mehr oder sie wurden abgebrochen
    } catch (PersistenceLayerException e) {
      throw new RemoteException("Unable to load ApplicationEntries to check whether they are in use.", e);
    } catch (XPRC_TimeoutWhileWaitingForUnaccessibleOrderException e) {
      throw new RemoteException("Unable to check whether orders are running remotely.", e);
    }
  }


  /**
   * Liefert den Status einer Application. Falls die Application nicht vorhanden ist,
   * wird null zurückgegeben
   * @param applicationName
   * @param versionName
   * @throws RemoteException 
   */
  public ApplicationState getApplicationStateRemotely(String applicationName, String versionName) throws RemoteException {
    try {
      return getApplicationState(applicationName, versionName);
    } catch (PersistenceLayerException e) {
      throw new RemoteException("Failed to query application", e);
    }
  }


  /**
   * Liefert den Status einer Runtime Application. Falls die Application nicht vorhanden ist,
   * wird null zurückgegeben
   * @param applicationName
   * @param versionName
   * @throws PersistenceLayerException 
   */
  public ApplicationState getApplicationState(String applicationName, String versionName) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection();
    try {
      ApplicationStorable application = queryRuntimeApplicationStorable(applicationName, versionName, con);
      if (application == null) {
        return null; //Application existiert nicht
      }

      return application.getStateAsEnum();
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Can't close connection.", e);
      }
    }
  }
  
  private ApplicationState getApplicationState(Application app) throws PersistenceLayerException {
    return getApplicationState(app.getName(), app.getVersionName());
  }


  public static final String CLUSTERABLE_COMPONENT = "ClusteredApplicationManagement";
  private long clusterInstanceId;
  private long clusteredInterfaceId;
  private RMIClusterProvider clusterInstance;
  private boolean clustered = false;
  private volatile ClusterState currentClusterState = ClusterState.NO_CLUSTER;
  ClusterStateChangeHandler rmiClusterStateChangeHandler = new ClusterStateChangeHandler() {

    public void onChange(ClusterState newState) {
      logger.debug("Got notified of state transition '" + currentClusterState + "' -> '" + newState + "'");
      currentClusterState = newState;
      if (newState.isDisconnected()) {
        // alle Waitobjekte aufwecken -> sonst warten die ewig auf eine Benachrichtigung vom anderen Knoten
        mapOfWaitObjectsLock.lock();
        try {
          for (Entry<String, Object> entry : mapOfWaitObjects.entrySet()) {
            Object waitObject = entry.getValue();
            if (waitObject != null) {
              if (logger.isDebugEnabled()) {
                logger.debug("Notify waitObjects for requestId " + entry.getKey());
              }
              synchronized (waitObject) {
                waitObject.notifyAll();
              }
            }
          }
          List<String> keys = new ArrayList<String>(mapOfWaitObjects.keySet());
          for (String key : keys) {
            mapOfWaitObjects.put(key, null);
          }
        } finally {
          mapOfWaitObjectsLock.unlock();
        }
      }
    }


    public boolean isReadyForChange(ClusterState newState) {
      return true;
    }
  };


  public void enableClustering(long clusterInstanceId) throws XFMG_UnknownClusterInstanceIDException,
      XFMG_ClusterComponentConfigurationException {

    if (clustered) {
      throw new RuntimeException("already clustered");
    }
    this.clusterInstanceId = clusterInstanceId;
    XynaClusteringServicesManagementInterface clusterMgmt =
        XynaFactory.getInstance().getFactoryManagement().getXynaClusteringServicesManagement();
    clusterInstance = (RMIClusterProvider) clusterMgmt.getClusterInstance(clusterInstanceId);
    if (clusterInstance == null) {
      throw new IllegalArgumentException("Did not find Clusterinstance with id " + clusterInstanceId);
    }

    try {
      clusteredInterfaceId =
          ((RMIClusterProvider) clusterInstance).addRMIInterfaceWithClassReloading(CLUSTERABLE_COMPONENT,
                                                                                   new RMIImplFactory<ApplicationRemoteImpl>() {

                                                                                     public void init(InitializableRemoteInterface rmiImpl) {
                                                                                       rmiImpl.init(ApplicationManagementImpl.this);
                                                                                     }


                                                                                     public String getFQClassName() {
                                                                                       return ApplicationRemoteImpl.class.getName();
                                                                                     }


                                                                                     public void shutdown(InitializableRemoteInterface rmiImpl) {
                                                                                     }
                                                                                   });
    } catch (XMCP_RMI_BINDING_ERROR e) {
      throw new XFMG_ClusterComponentConfigurationException(getName(), clusteredInterfaceId, e);
    }

    clusterMgmt.addClusterStateChangeHandler(clusterInstanceId, rmiClusterStateChangeHandler);

    currentClusterState = clusterInstance.getState();
    clustered = true;
  }


  public void disableClustering() {
    XynaClusteringServicesManagementInterface clusterMgmt =
        XynaFactory.getInstance().getFactoryManagement().getXynaClusteringServicesManagement();
    clusterMgmt.removeClusterStateChangeHandler(clusterInstanceId, rmiClusterStateChangeHandler);

    clustered = false;
    currentClusterState = ClusterState.NO_CLUSTER;
    clusteredInterfaceId = 0;
    clusterInstanceId = 0;
    clusterInstance = null;
  }


  public String getName() {
    return CLUSTERABLE_COMPONENT;
  }


  public boolean isClustered() {
    return clustered;
  }


  public long getClusterInstanceId() {
    return clusterInstanceId;
  }
  
  
  private RuntimeContextManagement getRtCtxMgmt() {
    return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextManagement();
  }


  private boolean setDestination(DestinationValue oldDestination, DestinationKey newKey, XynaDispatcher dispatcher, Long revision)
      throws PersistenceLayerException {
    if (oldDestination instanceof FractalWorkflowDestination) {
      if (!dispatcher.setCustomDestination(newKey, new FractalWorkflowDestination(oldDestination.getFQName()))) {
        if (logger.isInfoEnabled()) {
          logger.info("destination " + newKey.getOrderType() + "->" + oldDestination.getFQName() + " already existed in targetrevision.");
        }
      }
    } else {
      if (logger.isInfoEnabled()) {
        logger.info("destination is not user-defined: " + newKey);
      }
      return false;
    }
    return true;
  }


  public void copyOrderTypes(String applicationName, String sourceVersion, String targetVersion, PrintStream statusOutputStream) {
    if (sourceVersion == null) {
      output(statusOutputStream, "Can't copy ordertypes from workingset version.");
      return;
    }
    if (targetVersion == null) {
      output(statusOutputStream, "Can't copy ordertypes to workingset version.");
      return;
    }

    RuntimeContext runtimeContextTarget = RevisionManagement.getRuntimeContext(applicationName, targetVersion, null);
    RuntimeContext runtimeContextSource = RevisionManagement.getRuntimeContext(applicationName, sourceVersion, null);

    Long revTarget;
    Long revSource;
    try {
      revTarget = revisionManagement.getRevision(runtimeContextTarget);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e2) {
      throw new IllegalArgumentException("version " + targetVersion + " not found im application " + applicationName, e2);
    }
    try {
      revSource = revisionManagement.getRevision(runtimeContextSource);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e2) {
      throw new IllegalArgumentException("version " + sourceVersion + " not found im application " + applicationName, e2);
    }

    //Applications dürfen nicht im Zustand AUDIT_MODE sein
    try {
      ApplicationState sourceState = getApplicationState(applicationName, sourceVersion);
      if (sourceState == ApplicationState.AUDIT_MODE) {
        output(statusOutputStream, "Can't copy ordertypes from audit mode version.");
        return;
      }

      ApplicationState targetState = getApplicationState(applicationName, targetVersion);
      if (targetState == ApplicationState.AUDIT_MODE) {
        output(statusOutputStream, "Can't copy ordertypes to audit mode version.");
        return;
      }
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }

    List<ApplicationEntryStorable> listApplicationDetails = listApplicationDetails(applicationName, sourceVersion, true, null, null);

    PriorityManagement priorityManagement =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getPriorityManagement();
    CapacityMappingDatabase capacityMappingDatabase =
        XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getCapacityMappingDatabase();
    MonitoringDispatcher monitoringDispatcher =
        XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getMonitoringDispatcher();
    PlanningDispatcher planningDispatcher =
        XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaPlanning().getPlanningDispatcher();
    ExecutionDispatcher executionDispatcher =
        XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution().getExecutionEngineDispatcher();
    CleanupDispatcher cleanupDispatcher =
        XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaCleanup().getCleanupEngineDispatcher();
    OrderContextConfiguration orderContextConfig =
        XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderContextConfiguration();

    boolean foundOrderType = false;
    writelock.lock();
    try {

      ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
      try {

        for (ApplicationEntryStorable entry : listApplicationDetails) {
          if (entry.getTypeAsEnum() == ApplicationEntryType.ORDERTYPE) {
            foundOrderType = true;
            //ordertype config kopieren:
            DestinationKey dk = new DestinationKey(entry.getName(), runtimeContextSource);
            DestinationKey dkTarget = new DestinationKey(entry.getName(), runtimeContextTarget);
            try {
              DestinationValue dv_planning = planningDispatcher.getDestination(dk);
              DestinationValue dv_execution = executionDispatcher.getDestination(dk);
              DestinationValue dv_cleanup = cleanupDispatcher.getDestination(dk);

              if (!setDestination(dv_planning, dkTarget, planningDispatcher, revTarget)) {
                continue; //ist keine customdestination oder ist bereits gesetzt
              }
              if (!setDestination(dv_execution, dkTarget, executionDispatcher, revTarget)) {
                continue; //ist keine customdestination oder ist bereits gesetzt
              }
              if (!setDestination(dv_cleanup, dkTarget, cleanupDispatcher, revTarget)) {
                continue; //ist keine customdestination oder ist bereits gesetzt
              }
            } catch (XPRC_DESTINATION_NOT_FOUND e) {
              String msg = "Skipping ordertype " + entry.getName() + ". It is not configured in dispatcher: " + String.valueOf(e);
              output(statusOutputStream, msg);
              logger.debug(msg, e);
              continue; //destination ist nicht ordentlich konfiguriert...
            }

            boolean hasOrdercontextMapping = orderContextConfig.isDestinationKeyConfiguredForOrderContextMapping(dk, true);
            if (hasOrdercontextMapping) {
              orderContextConfig.configureDestinationKey(dkTarget, true);
            }

            Integer prio = priorityManagement.getPriority(entry.getName(), revSource);
            if (prio != null) {
              priorityManagement.setPriority(entry.getName(), prio, revTarget);
            }
            List<Capacity> capacityList = capacityMappingDatabase.getCapacities(dk);
            for (Capacity cap : capacityList) {
              capacityMappingDatabase.addCapacity(dkTarget, cap);
            }
            Integer monitoringLevel = monitoringDispatcher.getMonitoringLevel(dk);
            if (monitoringLevel != null) {
              monitoringDispatcher.setMonitoringLevel(dkTarget, monitoringLevel);
            }

            addObjectToApplicationInternally(entry.getName(), ApplicationEntryType.ORDERTYPE, applicationName, targetVersion, revSource,
                                             con, null);

            output(statusOutputStream, "copied " + entry.getName());
          }
        }


        con.commit();

        transferFromODSTypeToODSType(ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY, ApplicationEntryStorable.TABLE_NAME,
                                     ApplicationEntryStorable.class);

      } catch (PersistenceLayerException e) {
        throw new RuntimeException(e);
      } catch (XFMG_InvalidCapacityCardinality e) {
        throw new RuntimeException(e);
      } catch (XFMG_InvalidXynaOrderPriority e) {
        throw new RuntimeException(e);
      } catch (XPRC_INVALID_MONITORING_TYPE e) {
        throw new RuntimeException(e);
      } catch (XPRC_ExecutionDestinationMissingException e) {
        throw new RuntimeException(e);
      } catch (XFMG_FailedToAddObjectToApplication e) {
        throw new RuntimeException(e);
      } finally {
        try {
          con.closeConnection();
        } catch (PersistenceLayerException e) {
          logger.warn("Can't close connection.", e);
        }
      }

    } finally {
      writelock.unlock();
    }

    if (!foundOrderType) {
      output(statusOutputStream, "Did not find any ordertypes in version");
    } else {
      updateApplicationDetailsCache(revTarget);    
    }
  }


  /**
   * wenn id und ordertypes nicht gesetzt sind, werden alle crons kopiert.
   * wenn global nicht gesetzt ist, werden nur die ordertypes kopiert, die das korrekte binding haben.
   */
  public CopyCLOResult copyCronLikeOrders(final String applicationName, final String sourceVersion, final String targetVersion,
                                          PrintStream statusOutputStream, final String id, final String[] ordertypes, final boolean move,
                                          boolean verbose, boolean global) throws XFMG_CronLikeOrderCopyException {

    return copyCronLikeOrders(new Application(applicationName, sourceVersion), new Application(applicationName, targetVersion),
                              statusOutputStream, id, ordertypes,  move, verbose, global);
  }
  
  
  /**
   * wenn id und ordertypes nicht gesetzt sind, werden alle crons kopiert.
   * wenn global nicht gesetzt ist, werden nur die ordertypes kopiert, die das korrekte binding haben.
   */
  public CopyCLOResult copyCronLikeOrders(final RuntimeContext from, final RuntimeContext to,
                                          PrintStream statusOutputStream, final String id, final String[] ordertypes, final boolean move,
                                          boolean verbose, boolean global) throws XFMG_CronLikeOrderCopyException {
    if (id != null && ordertypes != null && ordertypes.length > 0) {
      throw new IllegalArgumentException(); //sollte nicht so aufgerufen werden
    }

    //Applications dürfen nicht im Zustand AUDIT_MODE sein
    try {
      if (from instanceof Application) {
        ApplicationState sourceState = getApplicationState((Application)from);
  
        if (sourceState == ApplicationState.AUDIT_MODE) {
          output(statusOutputStream, "Can't copy Cron Like Orders from audit mode version.");
          return null;
        }
      }
      if (to instanceof Application) {
        ApplicationState targetState = getApplicationState((Application)to);
        if (targetState == ApplicationState.AUDIT_MODE) {
          output(statusOutputStream, "Can't copy Cron Like Orders to audit mode version.");
          return null;
        }
      }
    } catch (PersistenceLayerException e) {
      throw new XFMG_CronLikeOrderCopyException(e);
    }
    
    Long revTarget;
    Long revSource;
    try {
      revTarget = revisionManagement.getRevision(to);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e2) {
      throw new IllegalArgumentException("Revision " + to + " not found", e2);
    }
    try {
      revSource = revisionManagement.getRevision(from);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e2) {
      throw new IllegalArgumentException("Revision " + from + " not found", e2);
    }

    //bei duplikaten nichts tun und melden
    //bei fehlern melden

    //falls global: wenn CONNECTED -> auf anderem knoten die mit dem entsprechenden binding behandeln
    //              wenn DISCONNECTED -> auf diesem knoten beide bindings behandeln

    //für die crons so vorgehen wie bei der migration: lesen, locken, versions-migrieren

    boolean allBindings = false;
    CopyCLOResult result = new CopyCLOResult();
    if (global) {
      if (currentClusterState == ClusterState.CONNECTED) {
        try {
          CopyCLOResult remoteResult =
              RMIClusterProviderTools
                  .executeAndCumulate(clusterInstance, clusteredInterfaceId,
                                      new RMIRunnable<CopyCLOResult, ApplicationRemoteInterface, XFMG_CronLikeOrderCopyException>() {

                                        public CopyCLOResult execute(ApplicationRemoteInterface clusteredInterface) throws RemoteException,
                                            XFMG_CronLikeOrderCopyException {
                                          return clusteredInterface.copyCronLikeOrders(from, to, id, ordertypes, move);
                                        }
                                      }, null, new CopyCLOResult()).get(0);
          result = remoteResult;
        } catch (InvalidIDException e) {
          throw new RuntimeException(e);
        }
      } else if (currentClusterState == ClusterState.DISCONNECTED_MASTER) {
        allBindings = true;
      } else {
        throw new XFMG_CronLikeOrderCopyException().initCause(new RuntimeException("Cluster state is " + currentClusterState
            + " but must be either " + ClusterState.CONNECTED + " or " + ClusterState.DISCONNECTED_MASTER + "."));
      }
    }

    boolean setApplicationRunning = false;
    try {
      ODSConnection con = ods.openConnection();
      try {
        //crons von sourcerevision suchen
        if (id != null) {
          CronLikeOrder cron = getCronLikeOrder(con, id, revSource);
          if (cron != null && cron.getRootOrderId() == null) {
            //rootorderid != null -> interner cronauftrag, muss nicht kopiert werden
            Long cronId = cron.getId();
            try {
              cron = migrateOneCron(cronId, con, move, revTarget, to);
              if (cron == null) {
                result.notMigrated.add(cronId);
              } else {
                result.countMigrated++;
                if (cron.isEnabled()) {
                  setApplicationRunning = true;
                }
              }
            } catch (XPRC_MDMObjectCreationException e) {
              logger.warn("could not migrate cron like order " + cronId + ".", e);
              result.notMigrated.add(cronId);
            }

            con.commit();

            if (cron != null && cron.isEnabled()) {
              List<CronLikeOrder> list = new ArrayList<CronLikeOrder>();
              list.add(cron);
              XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler().tryAddNewOrders(list);
            }
          } else {
            if (verbose) {
              if (cron == null) {
                output(statusOutputStream, "Cron Like Order with id " + id + " not found in " + from + ".");
              } else if (cron.getRootOrderId() == null) {
                output(statusOutputStream, "WARN Cron Like Order with id " + id + " is internal and may not be copied.");
              }
            }
          }
        } else {

          FactoryWarehouseCursor<CronLikeOrder> cloCursor =
              XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler()
                  .getCursorForCronLikeOrders(con, 100, revSource, ordertypes, allBindings);

          List<CronLikeOrder> cronlikeorders = cloCursor.getRemainingCacheOrNextIfEmpty();
          while (!cronlikeorders.isEmpty()) {
            List<CronLikeOrder> changedOrders = new ArrayList<CronLikeOrder>();
            for (CronLikeOrder cron : cronlikeorders) {
              if (cron.getRootOrderId() == null) {
                //rootorderid != null -> interner cronauftrag, muss nicht kopiert werden
                Long cronId = cron.getId();
                try {
                  cron = migrateOneCron(cronId, con, move, revTarget, to);
                  result.countMigrated++;
                  if (cron != null && cron.isEnabled()) {
                    setApplicationRunning = true;
                  }
                } catch (XPRC_MDMObjectCreationException e) {
                  logger.warn("could not migrate cron like order " + cronId + ".", e);
                  result.notMigrated.add(cronId);
                }

                if (cron != null && cron.isEnabled()) {
                  changedOrders.add(cron);
                }
              }
            }

            con.commit();

            XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler().tryAddNewOrders(changedOrders);
            cronlikeorders = cloCursor.getRemainingCacheOrNextIfEmpty();
          }
        }

      } finally {
        con.closeConnection();
      }
    } catch (PersistenceLayerException e) {
      throw new XFMG_CronLikeOrderCopyException(e);
    }

    //Application auf Running setzen
    if (setApplicationRunning && to instanceof Application) {
      changeApplicationState(((Application)to).getName(), ((Application)to).getVersionName(), ApplicationState.RUNNING);
    }

    if (verbose) {
      for (Long notMigrated : result.notMigrated) {
        output(statusOutputStream, "Could not " + (move ? "move " : "copy ") + " Cron Like Order with id " + notMigrated);
      }
      output(statusOutputStream, "Migrated " + result.countMigrated + " Cron Like Orders.");
    }

    return result;
  }


  private CronLikeOrder getCronLikeOrder(ODSConnection con, String id, Long revSource) throws PersistenceLayerException {
    CronLikeOrder clo =
        XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler().getCronLikeOrder(con, Long.valueOf(id));
    if (clo != null && revSource.equals(clo.getRevision())) {
      if (clo.getBinding() == clo.getLocalBinding(con.getConnectionType())) {
        return clo;
      }
    }
    return null;
  }


  private CronLikeOrder migrateOneCron(final Long cronLikeOrderId, ODSConnection con, boolean move, Long revTarget, RuntimeContext to) throws PersistenceLayerException, XPRC_MDMObjectCreationException {

    CronLikeOrder clo = new CronLikeOrder(cronLikeOrderId);
    XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler()
        .markAsNotToScheduleAndRemoveFromQueue(cronLikeOrderId);
    con.executeAfterCommitFails(new Runnable() {

      public void run() {
        XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler().unmarkAsNotToSchedule(cronLikeOrderId);
      }
    });

    try {
      con.queryOneRowForUpdate(clo);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      // offensichtlich ist CLO schon ausgeführt wurden? Jedenfalls ist nichts zu tun.
      con.executeAfterCommit(new Runnable() {

        public void run() {
          XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler().unmarkAsNotToSchedule(cronLikeOrderId);
        }
      });
      if (logger.isDebugEnabled()) {
        logger.debug("cron like order " + cronLikeOrderId + " could not be copied because it has already been executed.");
      }
      return null;
    }

    clo.setRevision(revTarget);
    if (to instanceof Application) {
      clo.setApplicationname(((Application)to).getName());
      clo.setVersionName(((Application)to).getVersionName());
      clo.setWorkspacename(null);
    } else if (to instanceof Workspace) {
      clo.setApplicationname(null);
      clo.setVersionName(null);
      clo.setWorkspacename(((Workspace)to).getName());
    }
    
    RuntimeContext runtimeContext = to;
    clo.getCreationParameters().getDestinationKey().setRuntimeContext(runtimeContext);
    GeneralXynaObject gxo = clo.getCreationParameters().getInputPayload();
    if (gxo != null) {
      String asXML = gxo.toXml();
      if (asXML.length() > 0) {
        if (gxo instanceof Container) {
          //bei Containern noch ein umschließendes Tag einbauen, damit das xml valide ist
          asXML = "<container>" + asXML + "</container>";
        }
        try {
          gxo = XynaObject.generalFromXml(asXML, revTarget);
        } catch (XPRC_XmlParsingException e) {
          throw new RuntimeException(e);
        } catch (XPRC_InvalidXMLForObjectCreationException e) {
          throw new RuntimeException(e);
        }
        clo.getCreationParameters().setInputPayloadDirectly(gxo);
      }
      //else: leerer container
    }


    if (!move) { //=copy
      //alter entry bleibt bestehen, neuen erzeugen
      clo.setId(XynaFactory.getInstance().getIDGenerator().getUniqueId());
      con.executeAfterCommit(new Runnable() {

        public void run() {
          XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler().unmarkAsNotToSchedule(cronLikeOrderId);
        }
      });
    }
    con.persistObject(clo);

    if (logger.isTraceEnabled()) {
      logger.trace("copied cron like order " + cronLikeOrderId);
    }

    return clo;
  }


  public static class CopyCLOResult implements Serializable {

    private static final long serialVersionUID = 1L;
    List<Long> notMigrated = new ArrayList<Long>();
    long countMigrated;
  }


  //remoteaufruf
  public CopyCLOResult copyCronLikeOrders(RuntimeContext source, RuntimeContext target, String id, String[] ordertypes, boolean move)
                  throws RemoteException, XFMG_CronLikeOrderCopyException {
    return copyCronLikeOrders(source, target, null, id, ordertypes, move, false, false);
  }


  private PreparedQuery<ApplicationName> queryApplicationContainingObject = null;


  public String[] getApplicationsContainingObject(String objectName, ApplicationEntryType type, Long parentRevision)
      throws PersistenceLayerException {
    ODSConnection con = ods.openConnection();
    try {
      if (queryApplicationContainingObject == null) {
        PreparedQuery<ApplicationName> pq =
            con.prepareQuery(new Query<ApplicationName>("select " + ApplicationEntryStorable.COL_APPLICATION + ", "
                + ApplicationEntryStorable.COL_VERSION + " from " + ApplicationEntryStorable.TABLE_NAME + " where "
                + ApplicationEntryStorable.COL_NAME + " =? AND " + ApplicationEntryStorable.COL_TYPE + " =? AND "
                + ApplicationEntryStorable.COL_PARENT_REVISION + " =?", new ResultSetReader<ApplicationName>() {

              public ApplicationName read(ResultSet rs) throws SQLException {
                return new ApplicationName(rs.getString(ApplicationEntryStorable.COL_APPLICATION), rs
                    .getString(ApplicationEntryStorable.COL_VERSION));
              }

            }));
        queryApplicationContainingObject = pq;
      }
      List<ApplicationName> applications =
          con.query(queryApplicationContainingObject, new Parameter(objectName, type.toString(), parentRevision), -1);
      List<String> applicationNames = new ArrayList<String>();
      for (ApplicationName app : applications) {
        if (getWorkingsetVersionName(app.getName(), parentRevision).equals(app.getVersionName())) {
          applicationNames.add(app.getName());
        }
      }
      return applicationNames.toArray(new String[applicationNames.size()]);
    } finally {
      con.closeConnection();
    }
  }


  /**
   * Zerlegt eine XynaProperty in einzelne Werte. Als Trennzeichen werden Kommata
   * verwendet. Leerzeichen vor und hinter einem Komma werden ignoriert.
   * @param property
   * @return
   */
  private static List<String> getPropertyAsList(XynaPropertyString property) {
    String[] values = property.get().trim().split("\\s*,\\s*");
    return Arrays.asList(values);
  }


  /**
   * Legt die "exludedSubtypesOf-Property" für einen Workspace an
   */
  public void addExcludedSubtypesOfProperty(String workspaceName, Long revision) {
    String suffix = "";
    if (!revision.equals(RevisionManagement.REVISION_DEFAULT_WORKSPACE)) {
      suffix = "." + workspaceName;
    }
    XynaPropertyString excludedSubtypesOf =
        new XynaPropertyString(EXCLUDED_SUBTYPES_OF_PREFIX + suffix, XMOMPersistenceManagement.STORABLE_BASE_CLASS)
            .setDefaultDocumentation(DocumentationLanguage.EN,
                                     "Comma separated list of base types whose subtypes are taken into a new application version (parentWorkspace '"
                                         + workspaceName + "') only when they are directly needed. * to disallow all subtypes.");
    excludedSubtypesOf.registerDependency(DEFAULT_NAME);
    excludedSubtypesOfProperties.put(revision, excludedSubtypesOf);
  }


  /**
   * Liefert die "exludedSubtypesOf-Property" für einen Workspace
   */
  public XynaPropertyString getExcludedSubtypesOfProperty(Long revision) {
    return excludedSubtypesOfProperties.get(revision);
  }


  /**
   * Enternt "exludedSubtypesOf-Property" eines Workspaces
   */
  public void removeExcludedSubtypesOfProperty(Long revision) {
    XynaPropertyString removed = excludedSubtypesOfProperties.remove(revision);
    if (removed != null) {
      removed.unregister();
    }
  }


  public static class ExportApplicationBuildParameter implements Serializable {

    private static final long serialVersionUID = 1L;

    private final boolean localBuild;
    private final String newVersion;
    private final boolean local;
    private final String user; //für Multi-User-Events


    public ExportApplicationBuildParameter() {
      localBuild = false;
      newVersion = null;
      local = false;
      user = null;
    }


    private ExportApplicationBuildParameter(boolean localBuild, String newVersion, boolean local, String user) {
      this.localBuild = localBuild;
      this.newVersion = newVersion;
      this.local = local;
      this.user = user;
    }


    public boolean getLocalBuild() {
      return localBuild;
    }


    public String getNewVersion() {
      return newVersion;
    }


    public boolean getLocal() {
      return local;
    }


    public String getUser() {
      return user;
    }

    
    public static ExportApplicationBuildParameter local() {
      return new ExportApplicationBuildParameter(true, null, false, null);
    }

    public static ExportApplicationBuildParameter local(String user) {
      return new ExportApplicationBuildParameter(true, null, false, user);
    }


    public static ExportApplicationBuildParameter newVersion(String newVersion) {
      return new ExportApplicationBuildParameter(false, newVersion, false, null);
    }

    public static ExportApplicationBuildParameter newVersion(String newVersion, String user) {
      return new ExportApplicationBuildParameter(false, newVersion, false, user);
    }


    public static ExportApplicationBuildParameter newLocalVersion(String newVersion) {
      return new ExportApplicationBuildParameter(false, newVersion, true, null);
    }

    public static ExportApplicationBuildParameter newLocalVersion(String newVersion, String user) {
      return new ExportApplicationBuildParameter(false, newVersion, true, user);
    }

  }


  public static enum ApplicationPartImportMode {
    INCLUDE, EXCLUDE, ONLY;
  }

  public static class ImportApplicationParameter implements Serializable {

    private static final long serialVersionUID = 1L;

    private final ApplicationPartImportMode capacities;
    private final ApplicationPartImportMode xynaProperties;
    private final Boolean override;
    private final Boolean local;
    private final String user;
    private boolean stopIfExistingAndRunning;


    public ImportApplicationParameter() {
      capacities = ApplicationPartImportMode.EXCLUDE;
      xynaProperties = ApplicationPartImportMode.EXCLUDE;
      override = false;
      local = false;
      user = null;
    }

    private ImportApplicationParameter(ApplicationPartImportMode xynaProperties, ApplicationPartImportMode capacities, Boolean override,
                                       Boolean local, String user) {
      this.capacities = capacities;
      this.xynaProperties = xynaProperties;
      this.override = override;
      this.local = local;
      this.user = user;
    }


    public void setStopIfExistingAndRunning(boolean stopIfExistingAndRunning) {
      this.stopIfExistingAndRunning = stopIfExistingAndRunning;
    }


    public boolean isStopIfExistingAndRunning() {
      return stopIfExistingAndRunning;
    }


    public ApplicationPartImportMode getCapacities() {
      return capacities;
    }


    public ApplicationPartImportMode getXynaProperties() {
      return xynaProperties;
    }


    public Boolean getOverride() {
      return override;
    }


    public Boolean getLocal() {
      return local;
    }


    public String getUser() {
      return user;
    }


    public static ImportApplicationParameter defaultParameter() {
      return new ImportApplicationParameter();
    }


    public static ImportApplicationParameter defaultOverride() {
      return with(false, false, true, false);
    }


    public static ImportApplicationParameter with(boolean includeXynaProperties, boolean includeCapcities, boolean override, boolean local) {
      return with(includeXynaProperties, includeCapcities, override, local, null);
    }


    public static ImportApplicationParameter with(boolean includeXynaProperties, boolean includeCapcities, boolean override, boolean local,
                                                  String user) {
      return new ImportApplicationParameter(includeXynaProperties ? ApplicationPartImportMode.INCLUDE : ApplicationPartImportMode.EXCLUDE,
                                            includeCapcities ? ApplicationPartImportMode.INCLUDE : ApplicationPartImportMode.EXCLUDE,
                                            override, local, user);
    }
    
    
    public static ImportApplicationParameter with(ApplicationPartImportMode xynaProperties, ApplicationPartImportMode capcities, boolean override, boolean local,
                                                  String user) {
      return new ImportApplicationParameter(xynaProperties, capcities, override, local, user);
    }


    public static ImportApplicationParameter globalSettingsOnly(boolean xynaProperties, boolean capacities) {
      return globalSettingsOnly(xynaProperties, capacities, null);
    }


    public static ImportApplicationParameter globalSettingsOnly(boolean xynaProperties, boolean capacities, String user) {
      if (!xynaProperties && !capacities) {
        throw new IllegalArgumentException("Setting import without settings!");
      }
      return new ImportApplicationParameter(xynaProperties ? ApplicationPartImportMode.ONLY : ApplicationPartImportMode.EXCLUDE,
                                            capacities ? ApplicationPartImportMode.ONLY : ApplicationPartImportMode.EXCLUDE, null, null,
                                            user);
    }


  }
  
  //aus abwärtskompatibilitätsgründen: xtf benutzt das
  public void addObjectToApplication(String objectName, String applicationName, String version, ApplicationEntryType entryType,
                                     Long parentRevision, boolean verbose, PrintStream statusOutputStream) throws XFMG_FailedToAddObjectToApplication {
    addObjectToApplicationDefinition(objectName, entryType, applicationName, parentRevision, verbose, statusOutputStream, new SingleRepositoryEvent(parentRevision));
  }  
  
  //aus abwärtskompatibilitätsgründen: xtf benutzt das
  public void addObjectToApplication(String fqName, String applicationName, Long parentRevision) throws XFMG_FailedToAddObjectToApplication {
    addXMOMObjectToApplication(fqName, applicationName, parentRevision, new SingleRepositoryEvent(parentRevision), false, null);
  }

  public String getHighestVersion(String applicationName, boolean mustBeRunning) {
    Version highestVersion = null;
    
    List<ApplicationInformation> appsInfo = listApplications(false, false);
    for (ApplicationInformation appInfo : appsInfo) {
      if ( (!appInfo.getName().equals(applicationName)) ||
           (mustBeRunning && (appInfo.getState() != ApplicationState.RUNNING)) ) {
        continue;
      }
      
      Version version = new Version(appInfo.getVersion());
      if (version.isStrictlyGreaterThan(highestVersion)) {
        highestVersion = version;
      }
    }
    
    return highestVersion != null ? highestVersion.getString() : null;
  }
  
  
  public void registerEventHandler(AppMgmtEventHandler handler) {
    eventHandling.registerHandler(handler);
  }
  
  public void unregisterEventHandler(String name, String version) {
    eventHandling.unregisterHandler(name, new Version(version));
  }


  public ApplicationXmlEntry createApplicationDefinitionXml(String applicationName, String versionName, String workspaceName,
                                                            boolean createStub) throws XynaException {
 
    ApplicationXmlEntry applicationXmlEntry = new ApplicationXmlEntry(applicationName, versionName, null);
    applicationXmlEntry.setFactoryVersion();
    applicationXmlEntry.getApplicationInfo().setIsRemoteStub(createStub);
    Long parentRevision = revisionManagement.getRevision(null, null, workspaceName);
    
    if (createStub) {
      ODSConnection con = ods.openConnection();
      List<ApplicationEntryStorable> appEntries;
      try {
        appEntries = (List<ApplicationEntryStorable>) queryAllApplicationDefinitionStorables(applicationName, parentRevision, con);
      } finally {
        con.closeConnection();
      }
      Collection<RuntimeDependencyContext> dependentRuntimeContexts = new HashSet<RuntimeDependencyContext>();
      Set<Long> revisions = new HashSet<Long>();
      Application app = new Application(applicationName, versionName);
      Collection<ApplicationEntryStorable> implicitDependencies = findDependenciesForStub(null, appEntries, parentRevision, revisions, app, false);
      for (Long rev : revisions) {
        dependentRuntimeContexts.add(RuntimeContextDependencyManagement.asRuntimeDependencyContext(revisionManagement.getRuntimeContext(rev)));
      }
      
      createXMLEntries(implicitDependencies, false, null, applicationXmlEntry, parentRevision, false, false, createStub);
      addRuntimeContextRequirementXMLEntries(dependentRuntimeContexts, applicationXmlEntry, false, null);
      
    } else {
      TreeSet<ApplicationEntryStorable> plainSet = new TreeSet<>(ApplicationEntryStorable.COMPARATOR);
      List<ApplicationEntryStorable> plainEntries = listApplicationDetails(applicationName, null, false, null, parentRevision);
      if (plainEntries != null) {
        plainSet.addAll(plainEntries);
      }
      TreeSet<ApplicationEntryStorable> dependencySet = new TreeSet<>(ApplicationEntryStorable.COMPARATOR);
      List<ApplicationEntryStorable> includingDeps = listApplicationDetails(applicationName, null, true, null, parentRevision);
      if (includingDeps != null) {
        dependencySet.addAll(includingDeps);
      }
      dependencySet.removeAll(plainSet);
      
      createXMLEntries(plainSet, false, null, applicationXmlEntry, parentRevision, false, false, createStub);
      createXMLEntries(dependencySet, false, null, applicationXmlEntry, parentRevision, true, false, createStub);
      Collection<RuntimeDependencyContext> dependentRuntimeContexts;
      RuntimeContextDependencyManagement rcdMgmt =
          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
      dependentRuntimeContexts = rcdMgmt.getRequirements(new ApplicationDefinition(applicationName, new Workspace(workspaceName)));
      addRuntimeContextRequirementXMLEntries(dependentRuntimeContexts, applicationXmlEntry, false, null);
    }
    
    
    return applicationXmlEntry;
  }
  
}
