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
package xmcp.factorymanager.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.update.outdatedclasses_6_1_2_3.Container;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.collections.SerializablePair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.SingleRepositoryEvent;
import com.gip.xyna.xdev.exceptions.XDEV_CodeAccessInitializationException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.RepositoryAccess;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.RepositoryAccessManagement;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.parameters.InstantiateRepositoryAccessParameters;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.exceptions.XFMG_ClearWorkingSetFailedBecauseOfRunningOrders;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotBuildNewWorkspace;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotRemoveApplication;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotRemoveWorkspace;
import com.gip.xyna.xfmg.exceptions.XFMG_DuplicateApplicationName;
import com.gip.xyna.xfmg.exceptions.XFMG_NodeConnectException;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationDefinitionInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable.ApplicationEntryType;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.ApplicationPartImportMode;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.ImportApplicationParameter;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationState;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationStorable;
import com.gip.xyna.xfmg.xfctrl.appmgmt.BuildApplicationVersionParameters;
import com.gip.xyna.xfmg.xfctrl.appmgmt.CopyApplicationIntoWorkspaceParameters;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RemoveApplicationParameters;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RuntimeContextProblem;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RuntimeContextProblem.RuntimeContextProblemType;
import com.gip.xyna.xfmg.xfctrl.appmgmt.WorkspaceInformation;
import com.gip.xyna.xfmg.xfctrl.classloading.MDMClassLoader;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement.RuntimeContextValidationException;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement.TransientFile;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.CredentialsCache;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.FactoryNodeStorable;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.InfrastructureLinkProfile;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.InterFactoryLink;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.NodeManagement;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.filemgmt.RemoteFileManagementLinkProfile;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.filemgmt.RemoteFileManagementUtils;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.rtctxmgmt.ListRuntimeDependencyContextParameter;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.rtctxmgmt.RuntimeContextManagementLinkProfile;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.rtctxmgmt.RuntimeDependencyContextInformation;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.DataModel;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext.RuntimeDependencyContextType;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.CreateWorkspaceResult;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.CreateWorkspaceResult.Result;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.WorkspaceManagement;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.parameters.ClearWorkspaceParameters;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.parameters.RemoveWorkspaceParameters;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResult;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSelect;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.storables.OrderInputSourceStorable;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionManagement;
import com.gip.xyna.xfmg.xopctrl.usermanagement.ScopedRightCache;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Action;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaCredentials;
import com.gip.xyna.xmcp.ErroneousOrderExecutionResponse;
import com.gip.xyna.xmcp.OrderExecutionResponse;
import com.gip.xyna.xmcp.RemoteXynaOrderCreationParameter;
import com.gip.xyna.xmcp.ResultController;
import com.gip.xyna.xmcp.SynchronousSuccesfullOrderExecutionResponse;
import com.gip.xyna.xmcp.WrappingType;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.securestorage.SecureStorage;
import com.gip.xyna.xnwh.selection.parsing.ArchiveIdentifier;
import com.gip.xyna.xnwh.selection.parsing.SearchRequestBean;
import com.gip.xyna.xnwh.selection.parsing.SearchResult;
import com.gip.xyna.xnwh.selection.parsing.SelectionParser;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XFMG_CouldNotModifyRuntimeContextDependenciesException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXMLForObjectCreationException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMObjectCreationException;
import com.gip.xyna.xprc.exceptions.XPRC_ResumeFailedException;
import com.gip.xyna.xprc.exceptions.XPRC_TimeoutWhileWaitingForUnaccessibleOrderException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.ordersuspension.ProcessSuspendedException;
import com.gip.xyna.xprc.xpce.ordersuspension.ResumeTarget;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeManagement;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause_Manual;
import com.gip.xyna.xprc.xprcods.orderarchive.orderbackuphelper.MigrateRuntimeContext;
import com.gip.xyna.xprc.xprcods.orderarchive.orderbackuphelper.MigrateRuntimeContext.ActiveOrderType;
import com.gip.xyna.xprc.xprcods.orderarchive.orderbackuphelper.MigrateRuntimeContext.MigrationResult;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase.DeploymentStatus;
import com.gip.xyna.xprc.xsched.ordersuspension.SuspendRevisionsBean;

import xfmg.xfctrl.FactoryNode;
import xfmg.xfctrl.appmgmt.ApplicationDetails;
import xfmg.xfctrl.appmgmt.CLI;
import xfmg.xfctrl.appmgmt.ListApplicationParameter;
import xfmg.xfctrl.appmgmt.OrderEntranceType;
import xfmg.xfctrl.appmgmt.RMI;
import xfmg.xfctrl.appmgmt.RemoteApplicationDetails;
import xfmg.xfctrl.appmgmt.RemoteRuntimeContext;
import xfmg.xfctrl.appmgmt.RemoveApplicationParameter;
import xfmg.xfctrl.appmgmt.Running;
import xfmg.xfctrl.appmgmt.Stopped;
import xfmg.xfctrl.appmgmt.Warning;
import xfmg.xfctrl.appmgmt.Workingcopy;
import xfmg.xfctrl.filemgmt.ManagedFileId;
import xfmg.xfctrl.nodemgmt.ConnectException;
import xfmg.xopctrl.UserAuthenticationRight;
import xmcp.factorymanager.RtcManagerServicesServiceOperation;
import xmcp.factorymanager.rtcmanager.AbortOrders;
import xmcp.factorymanager.rtcmanager.ApplicationDefinition;
import xmcp.factorymanager.rtcmanager.ApplicationDefinitionDetails;
import xmcp.factorymanager.rtcmanager.ApplicationElement;
import xmcp.factorymanager.rtcmanager.ClearWorkspaceRequest;
import xmcp.factorymanager.rtcmanager.CreateADRequest;
import xmcp.factorymanager.rtcmanager.CreateRTARequest;
import xmcp.factorymanager.rtcmanager.CreateWorkspaceRequest;
import xmcp.factorymanager.rtcmanager.DeleteDuplicatesResponse;
import xmcp.factorymanager.rtcmanager.DeleteRTARequest;
import xmcp.factorymanager.rtcmanager.DeleteWorkspaceRequest;
import xmcp.factorymanager.rtcmanager.Dependency;
import xmcp.factorymanager.rtcmanager.Documentation;
import xmcp.factorymanager.rtcmanager.GetApplicationContent;
import xmcp.factorymanager.rtcmanager.GetApplicationContentRequest;
import xmcp.factorymanager.rtcmanager.GetDependentRTCsRequest;
import xmcp.factorymanager.rtcmanager.GetWorkspaceContentRequest;
import xmcp.factorymanager.rtcmanager.ImportRTARequest;
import xmcp.factorymanager.rtcmanager.Issue;
import xmcp.factorymanager.rtcmanager.IssueEntry;
import xmcp.factorymanager.rtcmanager.LoadRTARequest;
import xmcp.factorymanager.rtcmanager.OrderEntry;
import xmcp.factorymanager.rtcmanager.RTCMigration;
import xmcp.factorymanager.rtcmanager.RTCMigrationResult;
import xmcp.factorymanager.rtcmanager.ReferenceDirectionBackwards;
import xmcp.factorymanager.rtcmanager.ReferenceDirectionForward;
import xmcp.factorymanager.rtcmanager.RuntimeApplication;
import xmcp.factorymanager.rtcmanager.RuntimeApplicationDetails;
import xmcp.factorymanager.rtcmanager.RuntimeContextTableEntry;
import xmcp.factorymanager.rtcmanager.SVNRepositoryLink;
import xmcp.factorymanager.rtcmanager.Workspace;
import xmcp.factorymanager.rtcmanager.WorkspaceDetails;
import xmcp.factorymanager.rtcmanager.WorkspaceElement;
import xmcp.factorymanager.rtcmanager.exceptions.ClearWorkspaceException;
import xmcp.factorymanager.rtcmanager.exceptions.CreateApplicationDefinitionException;
import xmcp.factorymanager.rtcmanager.exceptions.CreateRTAException;
import xmcp.factorymanager.rtcmanager.exceptions.CreateWorkspaceException;
import xmcp.factorymanager.rtcmanager.exceptions.DeleteApplicationDefinitionException;
import xmcp.factorymanager.rtcmanager.exceptions.DeleteDuplicatesException;
import xmcp.factorymanager.rtcmanager.exceptions.DeleteRTAException;
import xmcp.factorymanager.rtcmanager.exceptions.DeleteWorkspaceException;
import xmcp.factorymanager.rtcmanager.exceptions.ExportRTAException;
import xmcp.factorymanager.rtcmanager.exceptions.GetADDetailsException;
import xmcp.factorymanager.rtcmanager.exceptions.GetApplicationContentException;
import xmcp.factorymanager.rtcmanager.exceptions.GetDependentRTCException;
import xmcp.factorymanager.rtcmanager.exceptions.GetFactoryNodesException;
import xmcp.factorymanager.rtcmanager.exceptions.GetIssuesException;
import xmcp.factorymanager.rtcmanager.exceptions.GetRTADetailsException;
import xmcp.factorymanager.rtcmanager.exceptions.GetRTCsException;
import xmcp.factorymanager.rtcmanager.exceptions.GetRuntimeApplicationsException;
import xmcp.factorymanager.rtcmanager.exceptions.GetWorkspaceContentException;
import xmcp.factorymanager.rtcmanager.exceptions.GetWorkspaceDetailsException;
import xmcp.factorymanager.rtcmanager.exceptions.ImportRTAException;
import xmcp.factorymanager.rtcmanager.exceptions.LoadRTAIntoWorkspaceException;
import xmcp.factorymanager.rtcmanager.exceptions.MigrateRTCException;
import xmcp.factorymanager.rtcmanager.exceptions.SetADContentException;
import xmcp.factorymanager.rtcmanager.exceptions.SetADDocumentationException;
import xmcp.factorymanager.rtcmanager.exceptions.SetDependentRTCsException;
import xmcp.factorymanager.rtcmanager.exceptions.SetRTAOrderEntryException;
import xmcp.factorymanager.rtcmanager.exceptions.StartRuntimeApplicationException;
import xmcp.factorymanager.rtcmanager.exceptions.StopRuntimeApplicationException;
import xmcp.factorymanager.shared.InsufficientRights;
import xmcp.tables.datatypes.TableColumn;
import xmcp.tables.datatypes.TableInfo;
import xmcp.yggdrasil.Force;
import xmcp.zeta.TableHelper;
import xmcp.zeta.TableHelper.Filter;
import xmcp.zeta.TableHelper.LogicalOperand;


public class RtcManagerServicesServiceOperationImpl implements ExtendedDeploymentTask, RtcManagerServicesServiceOperation {
  
  private static final String APPLICATION_DEFINITION_DEFAULT_VERSION = "workingset";
  private static final String APPLICATION_DEFINITION_VERSION_PREFIX = "workingset of version ";
  
  private static final String TABLE_KEY_REQUIRED_RTC_NAME = "runtimeContext.name";
  private static final String TABLE_KEY_REQUIRED_RTC_RTC_TYPE = "rtcType";
  private static final String TABLE_KEY_REQUIRED_RTC_STATE = "runtimeContext.state";

  private static final String TABLE_KEY_FACTORY_NODE = "runtimeContext.factoryNode";

  private static final String TABLE_KEY_RTA_NAME = "name";
  private static final String TABLE_KEY_RTA_VERSION = "version";
  private static final String TABLE_KEY_RTA_STATE = "state";
  private static final String TABLE_KEY_RTA_FACTORY_NODE = "factoryNode";
  private static final String TABLE_KEY_RTA_DOCUMENTATION = "documentation";
  
  private static final String TABLE_KEY_RTC_ELEMENT_CONTENT_TYPE = "elementType";
  private static final String TABLE_KEY_RTC_ELEMENT_CONTENT_NAME = "name";
  private static final String TABLE_KEY_AD_CONTENT_ORIGIN_RTC = "originRTC";
  private static final String TABLE_KEY_WORKSPACE_CONTENT_ADS = "applicationDefinitions";
  
  private static final String GUI_APPLICATION_NAME = "Runtime Application";
  
  private static final String REPOSITORY_ACCES_KEY_BRANCH_BASE_DIR = "branchBasedir";
  private static final String REPOSITORY_ACCES_KEY_HOOK_PORT = "hookPort";
  private static final String REPOSITORY_ACCES_KEY_PATH = "path";
  private static final String REPOSITORY_ACCES_KEY_SERVER_NAME = "serverName";
  private static final String REPOSITORY_ACCES_KEY_USER = "user";
  private static final String REPOSITORY_ACCES_KEY_PASSWORD = "password";
  
  private static final String ORDER_ENTRY_TYPE_CLI = "CLI";
  private static final String ORDER_ENTRY_TYPE_RMI = "RMI";
  
  private static final String ISSUE_ENTRY_NAME_KEY = "name";
  private static final String ISSUE_ENTRY_TYPE_KEY = "type";
  
  private static final String PROPERTY_KEY_SHOW_GLOBAL_APP_MGMT = "xfmg.xfctrl.appmgmt.showGlobalApplicationManagement";
  
  private static final XynaPropertyBoolean SHOW_GLOBAL_APP_MGMT = new XynaPropertyBoolean(PROPERTY_KEY_SHOW_GLOBAL_APP_MGMT, false)
      .setDefaultDocumentation(DocumentationLanguage.DE, "Steuert, ob die Applikation \"GlobalApplicationMgmt\" angezeigt wird oder nicht.")
      .setDefaultDocumentation(DocumentationLanguage.EN, "Controls whether the application \"GlobalApplicationMgmt\" is displayed in the GUI or not.");
  
  
  private static final Logger logger = CentralFactoryLogging.getLogger(RtcManagerServicesServiceOperationImpl.class);
  
  private static final ConcurrentHashMap<String, SetDependentRTCsException> setDepsExceptionsPerUser = new ConcurrentHashMap<String, SetDependentRTCsException>();
  
  private static final WorkspaceManagement workspaceManagement = 
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getWorkspaceManagement();
  
  private static final ApplicationManagementImpl applicationManagement =
      (ApplicationManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement();
  
  private static final RevisionManagement revisionManagement = 
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
  
  private static final RuntimeContextDependencyManagement rtcDependencyManagement = 
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
  
  private static final SessionManagement sessionManagement = 
      XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getSessionManagement();
  
  private static final XynaMultiChannelPortal multiChannelPortal = 
      (XynaMultiChannelPortal) XynaFactory.getInstance().getXynaMultiChannelPortal();
  
  private static final RepositoryAccessManagement repositoryAccessManagement =
      XynaFactory.getInstance().getXynaDevelopment().getXynaLibraryDevelopment().getRepositoryAccessManagement();
  
  private static final FileManagement fileManagement = 
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getFileManagement();
  
  private static final NodeManagement nodeManagement = 
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getNodeManagement();
  
  private static final UserManagement userManagement = 
      XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement();
  
  private static final com.gip.xyna.xfmg.xfctrl.nodemgmt.rtctxmgmt.LocalRuntimeContextManagementSecurity localLrcms =
      new com.gip.xyna.xfmg.xfctrl.nodemgmt.rtctxmgmt.LocalRuntimeContextManagementSecurity();
  
  private static CredentialsCache cache;
  

  enum DependencyType {
   
    explicit("A"), implicit("B"), independent("Z"), indirect("C");

    private String sortPrefix;
    
    private DependencyType(String sortPrefix) {
      this.sortPrefix = sortPrefix;
    }
    
    public String getSortPrefix() {
      return sortPrefix;
    }
  }
  
  static class HierarchicalDependency {
    
    private RuntimeDependencyContext context;
    private RuntimeContext runtimeContext;
    private HierarchicalDependency parent; 
    private List<HierarchicalDependency> children = new ArrayList<>();
    private DependencyType dependencyType;
    private int level = -1;
    
    
    public HierarchicalDependency(
              RuntimeDependencyContext context,
              int level,
              HierarchicalDependency parent,
              DependencyType dependencyType) {
      this.context = context;
      this.runtimeContext = context.asCorrespondingRuntimeContext();
      this.parent = parent;
      this.dependencyType = dependencyType;
      this.level = level;
    }
    
    public List<HierarchicalDependency> getChildrenRecursive(){
      List<HierarchicalDependency> result = new ArrayList<>();
      for (HierarchicalDependency c : children) {
        result.add(c);
        result.addAll(c.getChildrenRecursive());
      }
      return result;
    }
    
    public DependencyType getDependencyType() {
      return dependencyType;
    }
        
    public int getLevel() {
      return level;
    }
    
    public RuntimeDependencyContext getContext() {
      return context;
    }
    
    public boolean hasParent() {
      return parent != null;
    }
    
    public HierarchicalDependency getParent() {
      return parent;
    }
    
    public List<HierarchicalDependency> getChildren() {
      return children;
    }

    public RuntimeContext getRuntimeContext() {
      return runtimeContext;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((runtimeContext == null) ? 0 : runtimeContext.hashCode());
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
      HierarchicalDependency other = (HierarchicalDependency) obj;
      if (runtimeContext == null) {
        if (other.runtimeContext != null)
          return false;
      } else if (!runtimeContext.equals(other.runtimeContext)) {
        return false;
      }
      
      return true;
    }

    @Override
    public String toString() {
      return "HierarchicalDependency [runtimeContext=" + runtimeContext + ", level=" + level + "]";
    }

  }
  
  @Override
  public void deleteRTA(XynaOrderServerExtension correlatedXynaOrder, DeleteRTARequest request) throws DeleteRTAException, InsufficientRights {
    try {
      checkRight(correlatedXynaOrder, UserManagement.ScopedRight.APPLICATION, Action.remove, request.getRuntimeApplication());
    
      String user = sessionManagement.resolveSessionToUser(correlatedXynaOrder.getSessionId());
      
      XynaObjectList<RemoteRuntimeContext> remoteApps = new XynaObjectList<>(RemoteRuntimeContext.class);
      FactoryNode factoryNode = new FactoryNode(request.getRuntimeApplication().getFactoryNode(), request.getRuntimeApplication().getIsLocal());
      remoteApps.add(new RemoteRuntimeContext(factoryNode, new xprc.xpce.Application(request.getRuntimeApplication().getName(), request.getRuntimeApplication().getVersion())));
      RemoveApplicationParameter rap = new RemoveApplicationParameter(request.getStopRunningOrders(), true, user);
      
      OrderExecutionResponse executionResponse = startOrderSynchronouslyAndReturnOrder(ApplicationManagementImpl.REMOVE_APPLICATIONS_DESTINATION.get(), correlatedXynaOrder, new Container(remoteApps, rap));
      if(!executionResponse.hasExecutedSuccesfully() && executionResponse instanceof ErroneousOrderExecutionResponse) {
        ErroneousOrderExecutionResponse erroneousOrderExecutionResponse = (ErroneousOrderExecutionResponse)executionResponse;
        throw new DeleteRTAException(erroneousOrderExecutionResponse.getExceptionInformation().getMessage());
      }
    } catch (XPRC_XmlParsingException | XPRC_InvalidXMLForObjectCreationException | XPRC_MDMObjectCreationException | PersistenceLayerException e) {
      logger.error(e.getMessage(), e);
      throw new DeleteRTAException(e.getMessage(), e);
    }
  }
  
  @Override
  public List<? extends xmcp.factorymanager.rtcmanager.FactoryNode> getFactoryNodes(XynaOrderServerExtension correlatedXynaOrder) throws GetFactoryNodesException {
    List<FactoryNodeStorable> factoryNodeStorables = multiChannelPortal.getAllFactoryNodes();
    List<xmcp.factorymanager.rtcmanager.FactoryNode> result = factoryNodeStorables.stream()
        .map(s -> {
          xmcp.factorymanager.rtcmanager.FactoryNode fn = new xmcp.factorymanager.rtcmanager.FactoryNode();
          fn.setName(s.getName());
          fn.setIsLocal(false);
          return fn;
        })
        .collect(Collectors.toList());
    result.add(new xmcp.factorymanager.rtcmanager.FactoryNode("local", true));
    Collections.sort(result, Comparator.comparing(xmcp.factorymanager.rtcmanager.FactoryNode::getName, String.CASE_INSENSITIVE_ORDER));
    return result;
  }
  
  @Override
  public void importRTA(XynaOrderServerExtension correlatedXynaOrder, ImportRTARequest request) throws ImportRTAException {
    
    try {
      checkRight(correlatedXynaOrder, UserManagement.Rights.APPLICATION_ADMINISTRATION.name());
      
      String user = sessionManagement.resolveSessionToUser(correlatedXynaOrder.getSessionId());
      File tmpFile = File.createTempFile("appImport", null);
      TransientFile file = fileManagement.retrieve(request.getManagedFileId().getId());
      InputStream is = file.openInputStream();
      try {
        FileUtils.writeStreamToFile(is, tmpFile);
      } finally {
        is.close();
        file = null;
      }

      ImportApplicationParameter iap = createImportApplicationParameter(request, user);
      for (xmcp.factorymanager.rtcmanager.FactoryNode factoryNode : request.getTargetNodes()) {
        if (factoryNode.getIsLocal()) {
          localLrcms.importApplication(correlatedXynaOrder.getCreationRole(), iap, request.getManagedFileId().getId());
        } else {
          RemoteFileManagementLinkProfile remoteFileMgmt = getRemoteFileMgmt(factoryNode.getName());
          String remoteFileId = RemoteFileManagementUtils.upload(factoryNode.getName(), request.getManagedFileId().getId(), remoteFileMgmt, getCredentials(factoryNode));
          RuntimeContextManagementLinkProfile remoteAccess = getRemoteRtCtxMgmt(factoryNode.getName());
          remoteAccess.importApplication(getCredentials(factoryNode), iap, remoteFileId);
        }
      }

    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      throw new ImportRTAException(ex.getMessage(), ex);
    }
  }

  private ImportApplicationParameter createImportApplicationParameter(ImportRTARequest importSettings, String user) {
    ApplicationPartImportMode properties = ApplicationPartImportMode.EXCLUDE;
    if (importSettings.getIncludeXynaProperties()) {
      properties = ApplicationPartImportMode.INCLUDE;
    } /* else if (importSettings.getXynaPropertyImportSettings() instanceof ImportXynaPropertiesOnly) {
      properties = ApplicationPartImportMode.ONLY; !!! TODO !!!
    } */
    ApplicationPartImportMode capacities = ApplicationPartImportMode.EXCLUDE;
    if (importSettings.getIncludeXynaCapacities()) {
      capacities = ApplicationPartImportMode.INCLUDE;
    } /* else if (importSettings.getCapacityImportSettings() instanceof ImportCapacitiesOnly) {
      capacities = ApplicationPartImportMode.ONLY; !!! TODO !!!
    } */
    return ImportApplicationParameter.with(properties,
                                           capacities,
                                           importSettings.getOverrideExisting(),
                                           true,
                                           user);
  }

  private RemoteFileManagementLinkProfile getRemoteFileMgmt(String nodeName) throws ConnectException {
    com.gip.xyna.xfmg.xfctrl.nodemgmt.FactoryNode fn = nodeManagement.getNodeByName(nodeName);
    if (fn == null) {
      throw new ConnectException(nodeName, new Exception("node does not exists"));
    }
    
    RemoteFileManagementLinkProfile remoteAccess = fn.getInterFactoryLink().getProfile(InterFactoryLink.InterFactoryLinkProfileIdentifier.FileManagement);
    if (remoteAccess == null) {
      throw new ConnectException(nodeName, new Exception("remoteAccess does not exists"));
    }
    
    return remoteAccess;
  }

  private XynaCredentials getCredentials(xmcp.factorymanager.rtcmanager.FactoryNode node) throws XFMG_NodeConnectException, ConnectException  {
    XynaCredentials credentials = cache.getCredentials(node.getName(), getInfrastructure(node.getName()));
    InfrastructureLinkProfile ilp = getInfrastructure(node.getName());
    try {
      ilp.getExtendedStatus(credentials);
    } catch (XFMG_NodeConnectException e) {
      cache.clearSession(node.getName());
      credentials = cache.getCredentials(node.getName(), getInfrastructure(node.getName()));
      ilp.getExtendedStatus(credentials);
    }
    return credentials;
  }

  @Override
  public ManagedFileId exportRTA(XynaOrderServerExtension correlatedXynaOrder, RuntimeApplication runtimeApplication) throws ExportRTAException, InsufficientRights {
    try {
      checkRight(correlatedXynaOrder, UserManagement.ScopedRight.APPLICATION, Action.deploy, runtimeApplication);
      String user = sessionManagement.resolveSessionToUser(correlatedXynaOrder.getSessionId());
      String exportFileName = runtimeApplication.getName() + "_" + runtimeApplication.getVersion() + "_" + System.currentTimeMillis();
      File tmpFile = File.createTempFile(exportFileName, null);

      applicationManagement.exportApplication(
                  runtimeApplication.getName(), 
                  runtimeApplication.getVersion(), 
                  tmpFile.getPath(), false, null, false, false,
                  runtimeApplication.getIsStub(), null,
                  user);

      FileInputStream fis = new FileInputStream(tmpFile);
      try {
        String fileId = fileManagement.store(multiChannelPortal.getUser(user).getRole(), exportFileName, fis);
        return new ManagedFileId(fileId);
      } finally {
        fis.close();
        tmpFile.delete();
      }
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      throw new ExportRTAException(ex.getMessage(), ex);
    }
  }
  
  @Override
  public void setRTAOrderEntry(XynaOrderServerExtension correlatedXynaOrder, RuntimeApplication runtimeApplication, OrderEntry orderEntry) throws SetRTAOrderEntryException, InsufficientRights {
    try {
      checkRight(correlatedXynaOrder, UserManagement.Rights.APPLICATION_MANAGEMENT.name());
      
      OrderEntranceType orderEntranceType = convert(orderEntry);
      if(orderEntranceType == null) {
        throw new SetRTAOrderEntryException("Unknown OrderEntry");
      }
      if(orderEntry.getIsActive()) {
        startRuntimeApplication(correlatedXynaOrder, runtimeApplication, orderEntranceType);
      } else {
        stopRuntimeApplication(correlatedXynaOrder, runtimeApplication, orderEntranceType);
      }
    } catch (InsufficientRights ex) {
      throw ex;
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      throw new SetRTAOrderEntryException(ex.getMessage(), ex);
    }
  }
  
  private OrderEntranceType convert(OrderEntry orderEntry) {
    if(orderEntry == null) {
      return null;
    }
    if(ORDER_ENTRY_TYPE_RMI.equals(orderEntry.getName())){
      return new RMI();
    } else if (ORDER_ENTRY_TYPE_CLI.equals(orderEntry.getName())) {
      return new CLI();
    } else {
      return null;
    }
  }
  
  @Override
  public RuntimeApplicationDetails getRTADetails(XynaOrderServerExtension correlatedXynaOrder, RuntimeApplication runtimeApplication) throws GetRTADetailsException, InsufficientRights {
    try {
      checkRight(correlatedXynaOrder, UserManagement.ScopedRight.APPLICATION, Action.list, runtimeApplication);
      
      Pair<RemoteApplicationDetails, ApplicationDetails> rtad = getRTADetail(correlatedXynaOrder, runtimeApplication);
      if(rtad == null) {
        throw new GetRTADetailsException("Runtime Application not found");
      }
      RuntimeApplicationDetails result = new RuntimeApplicationDetails();
      result.setDocumentation(rtad.getSecond().getComment());
      result.setFactoryNode(rtad.getFirst().getFactoryNode().getName());
      result.setIsLocal(rtad.getFirst().getFactoryNode().getLocal());
      result.setIsStub(rtad.getSecond().getRemoteStub());
      result.setName(rtad.getSecond().getName());
      List<OrderEntry> orderEntries = new ArrayList<>();
      if(rtad.getSecond().getOrderEntrances() != null) {
        boolean hasRMI = false;
        boolean hasCLI = false;
        for (OrderEntranceType orderEntranceType : rtad.getSecond().getOrderEntrances()) {
          if (orderEntranceType instanceof RMI) {
            hasRMI = true;
          } else if (orderEntranceType instanceof CLI) {
            hasCLI = true;
          }
        }
        orderEntries.add(new OrderEntry("RMI", hasRMI));
        orderEntries.add(new OrderEntry("CLI", hasCLI));
        
      }
      result.setOrderEntries(orderEntries);
      if(rtad.getFirst().getFactoryNode().getLocal()) {
        result.setRevision(revisionManagement.getRevision(rtad.getSecond().getName(), rtad.getSecond().getVersion(), null));
      }
      result.setState(getRTAStateForGui(rtad.getSecond().getState()));
      result.setVersion(rtad.getSecond().getVersion());
      return result;
    } catch (InsufficientRights ex) {
      throw ex;
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      throw new GetRTADetailsException(e.getMessage(), e);
    }
  }
  
  private Pair<RemoteApplicationDetails, ApplicationDetails> getRTADetail(XynaOrderServerExtension correlatedXynaOrder, RuntimeApplication runtimeApplication) throws Exception{
    List<RemoteApplicationDetails> details = getRuntimeApplications(correlatedXynaOrder, !runtimeApplication.getIsLocal());
    for (RemoteApplicationDetails rad : details) {
      if(rad.getFactoryNode().getName().equals(runtimeApplication.getFactoryNode())) {
        for (ApplicationDetails ad : rad.getApplicationDetails()) {
          if(ad.getName().equals(runtimeApplication.getName()) && ad.getVersion().equals(runtimeApplication.getVersion())) {
            return Pair.of(rad, ad);
          }
        }
      }
    }
    return null;
  }
  
  @Override
  public void startRuntimeApplication(XynaOrderServerExtension correlatedXynaOrder, RuntimeApplication application) throws StartRuntimeApplicationException, InsufficientRights {
    try {
      checkRight(correlatedXynaOrder, UserManagement.Rights.APPLICATION_MANAGEMENT.name());
      
      XynaObjectList<OrderEntranceType> entranceTypes = new XynaObjectList<>(OrderEntranceType.class);
      Pair<RemoteApplicationDetails, ApplicationDetails> rtad = getRTADetail(correlatedXynaOrder, application);
      if(rtad != null && rtad.getSecond() != null && rtad.getSecond().getOrderEntrances() != null) {
        entranceTypes.addAll(rtad.getSecond().getOrderEntrances());
      }
      startRuntimeApplication(correlatedXynaOrder, application, entranceTypes.toArray(new OrderEntranceType[] {}));
    } catch (InsufficientRights ex) {
      throw ex;
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      throw new StartRuntimeApplicationException(e.getMessage(), e);
    }
  }
  
  private void startRuntimeApplication(XynaOrderServerExtension correlatedXynaOrder, RuntimeApplication application, OrderEntranceType... orderEntranceTypes) throws StartRuntimeApplicationException {
    XynaObjectList<RemoteRuntimeContext> remoteApps = new XynaObjectList<>(RemoteRuntimeContext.class);
    
    RemoteRuntimeContext remoteRuntimeContext = new RemoteRuntimeContext();
    remoteRuntimeContext.setFactoryNode(new FactoryNode(application.getFactoryNode(), application.getIsLocal()));
    remoteRuntimeContext.setRuntimeContext(new xprc.xpce.Application(application.getName(), application.getVersion()));
    remoteApps.add(remoteRuntimeContext);
    
    XynaObjectList<OrderEntranceType> entranceTypes = new XynaObjectList<>(OrderEntranceType.class);
    for (OrderEntranceType orderEntranceType : orderEntranceTypes) {
      entranceTypes.add(orderEntranceType);
    }    
    
    try {
      
      OrderExecutionResponse executionResponse = startOrderSynchronouslyAndReturnOrder(
                                  ApplicationManagementImpl.START_APPLICATIONS_DESTINATION.get(), 
                                  correlatedXynaOrder, 
                                  remoteApps, 
                                  new xfmg.xfctrl.appmgmt.StartApplicationParameter(entranceTypes, true));
      if(!executionResponse.hasExecutedSuccesfully() && executionResponse instanceof ErroneousOrderExecutionResponse) {
          ErroneousOrderExecutionResponse erroneousOrderExecutionResponse = (ErroneousOrderExecutionResponse)executionResponse;
          throw new StartRuntimeApplicationException(erroneousOrderExecutionResponse.getExceptionInformation().getMessage());
      }
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      throw new StartRuntimeApplicationException(e.getMessage(), e);
    }
  }
  
  
  @Override
  public void stopRuntimeApplication(XynaOrderServerExtension correlatedXynaOrder, RuntimeApplication application) throws StopRuntimeApplicationException {
    try {
      checkRight(correlatedXynaOrder, UserManagement.Rights.APPLICATION_MANAGEMENT.name());
      
      XynaObjectList<OrderEntranceType> orderEntranceTypes = new XynaObjectList<>(OrderEntranceType.class);
      Pair<RemoteApplicationDetails, ApplicationDetails> rtad = getRTADetail(correlatedXynaOrder, application);
      if(rtad != null && rtad.getSecond() != null && rtad.getSecond().getOrderEntrances() != null) {
        orderEntranceTypes.addAll(rtad.getSecond().getOrderEntrances());
      }
      stopRuntimeApplication(correlatedXynaOrder, application, orderEntranceTypes.toArray(new OrderEntranceType[] {}));
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      throw new StopRuntimeApplicationException(e.getMessage(), e);
    }
  }
  
  private void stopRuntimeApplication(XynaOrderServerExtension correlatedXynaOrder, RuntimeApplication application, OrderEntranceType... orderEntranceTypes) throws StopRuntimeApplicationException {
    XynaObjectList<RemoteRuntimeContext> remoteApps = new XynaObjectList<>(RemoteRuntimeContext.class);
    
    RemoteRuntimeContext remoteRuntimeContext = new RemoteRuntimeContext();
    remoteRuntimeContext.setFactoryNode(new FactoryNode(application.getFactoryNode(), application.getIsLocal()));
    remoteRuntimeContext.setRuntimeContext(new xprc.xpce.Application(application.getName(), application.getVersion()));
    remoteApps.add(remoteRuntimeContext);
    
    try {
      OrderExecutionResponse executionResponse = startOrderSynchronouslyAndReturnOrder(
                                  ApplicationManagementImpl.STOP_APPLICATIONS_DESTINATION.get(), 
                                  correlatedXynaOrder, 
                                  remoteApps, 
                                  new xfmg.xfctrl.appmgmt.StopApplicationParameter(Arrays.asList(orderEntranceTypes)));
      if(!executionResponse.hasExecutedSuccesfully() && executionResponse instanceof ErroneousOrderExecutionResponse) {
          ErroneousOrderExecutionResponse erroneousOrderExecutionResponse = (ErroneousOrderExecutionResponse)executionResponse;
          throw new StopRuntimeApplicationException(erroneousOrderExecutionResponse.getExceptionInformation().getMessage());
      }
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      throw new StopRuntimeApplicationException(e.getMessage(), e);
    }
  }
  
  @Override
  public void loadRTAIntoWorkspace(XynaOrderServerExtension correlatedXynaOrder, LoadRTARequest request) throws LoadRTAIntoWorkspaceException, InsufficientRights {
    Long targetRevision = null;
    try {
      checkRight(correlatedXynaOrder, UserManagement.Rights.WORKINGSET_MANAGEMENT.name());
      
      // sowohl workspace als auch quell version locken
      targetRevision = revisionManagement.getRevision(null, null, request.getWorkspace().getName());
      CommandControl.tryLock(CommandControl.Operation.APPLICATION_COPY_TO_WORKINGSET, targetRevision);

      CopyApplicationIntoWorkspaceParameters params = new CopyApplicationIntoWorkspaceParameters();
      params.setTargetWorkspace(revisionManagement.getWorkspace(targetRevision));
      params.setComment(request.getDocumentation());
      params.setOverrideChanges(request.getOverwrite());
      params.setUser(sessionManagement.resolveSessionToUser(correlatedXynaOrder.getSessionId()));

      CommandControl.tryLock(CommandControl.Operation.APPLICATION_COPY_TO_WORKINGSET, new Application(request.getRuntimeApplication().getName(), request.getRuntimeApplication().getVersion()));
      try {
        XynaMultiChannelPortal.Identity identity = new XynaMultiChannelPortal.Identity(null, correlatedXynaOrder.getSessionId());
        XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.set(identity);
        try {
          applicationManagement.copyApplicationIntoWorkspace(request.getRuntimeApplication().getName(), request.getRuntimeApplication().getVersion(), params);
        } catch (XynaException xe) {
          logger.error(xe.getMessage(), xe);
          throw new LoadRTAIntoWorkspaceException(xe.getMessage(), xe.getCause());
        } finally {
          XynaMultiChannelPortal.THREAD_LOCAL_XMOM_MANIPULATION_IDENTITY.remove();
        }
      } finally {
        CommandControl.unlock(CommandControl.Operation.APPLICATION_COPY_TO_WORKINGSET, new Application(request.getRuntimeApplication().getName(), request.getRuntimeApplication().getVersion()));
      }
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      throw new LoadRTAIntoWorkspaceException(e.getMessage(), e.getCause());
    } finally {
      CommandControl.unlock(CommandControl.Operation.APPLICATION_COPY_TO_WORKINGSET, targetRevision);
    }
  }

  @Override
  public void setADContent(XynaOrderServerExtension correlatedXynaOrder, ApplicationDefinition applicationDefinition, List<? extends ApplicationElement> adElements) throws SetADContentException, InsufficientRights {
    try {
      checkRight(correlatedXynaOrder, UserManagement.ScopedRight.APPLICATION_DEFINITION, Action.list, applicationDefinition);
      Long parentRevision = revisionManagement.getRevision(null, null, applicationDefinition.getWorkspaceName());
      
      for (ApplicationElement ade : adElements) {
        ApplicationEntryType entryType = ApplicationEntryType.valueOf(ade.getElementType());
        DependencyType dependencyType = DependencyType.valueOf(ade.getDependencyType());
        if(dependencyType == DependencyType.explicit) {
          switch (entryType) {
            case DATATYPE :
            case EXCEPTION :
            case WORKFLOW :
              applicationManagement.addXMOMObjectToApplication(ade.getName(), applicationDefinition.getName(), parentRevision);
              break;
            case TRIGGERINSTANCE :
              applicationManagement.addTriggerInstanceToApplication(ade.getName(), applicationDefinition.getName(), parentRevision, false, null);
              break;
            case FILTERINSTANCE :
              applicationManagement.addFilterInstanceToApplication(ade.getName(), applicationDefinition.getName(), parentRevision, false, null);
              break;
            default :
              applicationManagement.addNonModelledObjectToApplication(ade.getName(), applicationDefinition.getName(), null, entryType, parentRevision, false, null);
              break;
          }
        } else if(dependencyType == DependencyType.independent) {
          switch (entryType) {
            case DATATYPE :
            case EXCEPTION :
            case WORKFLOW :
              applicationManagement.removeXMOMObjectFromApplication(applicationDefinition.getName(), ade.getName(), parentRevision);
              break;
            case TRIGGERINSTANCE :
              applicationManagement.removeTriggerInstanceFromApplication(applicationDefinition.getName(), ade.getName(), parentRevision, false, null);
              break;
            case FILTERINSTANCE :
              applicationManagement.removeFilterInstanceFromApplication(applicationDefinition.getName(), ade.getName(), parentRevision, false, null);
              break;
            default :
              applicationManagement.removeNonModelledObjectFromApplication(applicationDefinition.getName(), null, ade.getName(), entryType, parentRevision, false, null);
              break;
          }
        }
      } 
    } catch (InsufficientRights ex) {
      throw ex;
    } catch (XynaException ex) {
      logger.error(ex.getMessage(), ex);
      throw new SetADContentException(ex.getMessage(), ex);
    }
  }

  private void checkModifyRTCRights(XynaOrderServerExtension correlatedXynaOrder, xmcp.factorymanager.rtcmanager.RuntimeContext  rtc) throws InsufficientRights, PersistenceLayerException {
    if(rtc instanceof RuntimeApplication) {
      checkRight(correlatedXynaOrder, UserManagement.ScopedRight.APPLICATION, Action.write, rtc);
    } else if (rtc instanceof ApplicationDefinition) {
      checkRight(correlatedXynaOrder, UserManagement.ScopedRight.APPLICATION_DEFINITION, Action.write, rtc);
    } else if (rtc instanceof Workspace) {
      checkRight(correlatedXynaOrder, UserManagement.ScopedRight.WORKSPACE, Action.write, rtc);
    }
  }

  
  private void executeSetDependentRTCs(String user, RuntimeDependencyContext owner, XynaOrderServerExtension correlatedXynaOrder, xmcp.factorymanager.rtcmanager.RuntimeContext rtc, List<? extends Dependency> dependencies, boolean force) throws SetDependentRTCsException {
    RuntimeContextDependencyManagement rcdMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
    Set<RuntimeDependencyContext> newDeps = new HashSet<>(rcdMgmt.getDependencies(owner));

    try {
      for (Dependency dependency : dependencies) {
        DependencyType dependencyType = DependencyType.valueOf(dependency.getDependencyType());
        RuntimeDependencyContext rdc = getRuntimeDependencyContextByGuiRuntimeContext(dependency.getRuntimeContext());
        if (dependencyType == DependencyType.explicit) {
          boolean added = newDeps.add(rdc);
          if (!added) {
            throw new IllegalArgumentException(rdc.toString() + " could not be added to dependencies: " + newDeps);
          }
        } else if (dependencyType == DependencyType.independent) {
          boolean removed = newDeps.remove(rdc);
          if (!removed) {
            throw new IllegalArgumentException(rdc.toString() + " could not be removed from current dependencies: " + newDeps);
          }
        }
      }

      rcdMgmt.modifyDependencies(owner, newDeps, user, force, true);
    } catch (PersistenceLayerException | XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      logger.error(e.getMessage(), e);
      throw new SetDependentRTCsException(e.getMessage(), e);
    } catch (XFMG_CouldNotModifyRuntimeContextDependenciesException e) {
      logger.error(e.getMessage(), e);
      throw new SetDependentRTCsException(e.getMessage(), e);
    } catch(RuntimeContextValidationException e) {
      throw new SetDependentRTCsException(e.createExtendedMessage());
    }

  }


  @Override
  public void setDependentRTCs(XynaOrderServerExtension correlatedXynaOrder, xmcp.factorymanager.rtcmanager.RuntimeContext rtc,
                               List<? extends Dependency> dependencies, Force force)
      throws SetDependentRTCsException, InsufficientRights {

    try {
      boolean bForce = force == null ? false : force.getForce();
      checkModifyRTCRights(correlatedXynaOrder, rtc);

      String user = sessionManagement.resolveSessionToUser(correlatedXynaOrder.getSessionId());

      RuntimeDependencyContext owner = getRuntimeDependencyContextByGuiRuntimeContext(rtc);
      if (owner == null) {
        throw new SetDependentRTCsException("Unknown rtc type");
      }

      boolean canNotRunSync = affectsCurrentRuntimeContext(rtc);
      if (canNotRunSync) {
        Runnable detached = new Runnable() {

          public void run() {
            try {
              executeSetDependentRTCs(user, owner, correlatedXynaOrder, rtc, dependencies, bForce);
            } catch (SetDependentRTCsException e) {
              logger.error(e);
              setDepsExceptionsPerUser.put(user, e);
            }
          }
        };

        Thread detachedThread = new Thread(detached);
        detachedThread.start();
        return;
      } else {
        executeSetDependentRTCs(user, owner, correlatedXynaOrder, rtc, dependencies, bForce);
      }

    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY | PersistenceLayerException ex) {
      logger.error(ex.getMessage(), ex);
      throw new SetDependentRTCsException(ex.getMessage(), ex);
    }
  }
  
  private RuntimeDependencyContext getRuntimeDependencyContextByGuiRuntimeContext(xmcp.factorymanager.rtcmanager.RuntimeContext rtc) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    if(rtc instanceof RuntimeApplication) {
      RuntimeApplication application = (RuntimeApplication)rtc;
      return revisionManagement.getApplication(revisionManagement.getRevision(application.getName(), application.getVersion(), null));
    } else if (rtc instanceof Workspace) {
      Workspace workspace = (Workspace)rtc;
      return revisionManagement.getWorkspace(revisionManagement.getRevision(null, null, workspace.getName()));
    } else if (rtc instanceof ApplicationDefinition) {
      ApplicationDefinition applicationDefinition = (ApplicationDefinition)rtc;
      com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace parentWorkspace = revisionManagement.getWorkspace(
                                    revisionManagement.getRevision(null, null, applicationDefinition.getWorkspaceName()));
      return new com.gip.xyna.xfmg.xfctrl.revisionmgmt.ApplicationDefinition(applicationDefinition.getName(), parentWorkspace);
    }
    return null;
  }
  
  
  @Override
  public void setADDocumentation(XynaOrderServerExtension correlatedXynaOrder, ApplicationDefinition applicationDefinition, Documentation documentation) throws SetADDocumentationException {
    try {
      checkRight(correlatedXynaOrder, UserManagement.ScopedRight.APPLICATION_DEFINITION, Action.write, applicationDefinition);
      Long parentRevision = revisionManagement.getRevision(null, null, applicationDefinition.getWorkspaceName());
      applicationManagement.changeApplicationDefinitionComment(applicationDefinition.getName(), parentRevision, documentation.getValue());
    } catch (XynaException ex) {
      logger.error(ex.getMessage(), ex);
      throw new SetADDocumentationException(ex.getMessage(), ex);
    }
  }
  
  @Override
  public void createRTA(XynaOrderServerExtension correlatedXynaOrder, CreateRTARequest request) throws CreateRTAException, InsufficientRights {
    try {
      checkRight(correlatedXynaOrder, UserManagement.Rights.WORKINGSET_MANAGEMENT.name());
      Long parentRevision = revisionManagement.getRevision(null, null, request.getApplicationDefinition().getWorkspaceName());
      BuildApplicationVersionParameters params = new BuildApplicationVersionParameters();
      params.setComment(request.getDocumentation());
      params.setParentWorkspace(revisionManagement.getWorkspace(parentRevision));
      params.setRemoteStub(false);
      params.setUser(sessionManagement.resolveSessionToUser(correlatedXynaOrder.getSessionId()));
      applicationManagement.buildApplicationVersion(request.getApplicationDefinition().getName(), request.getVersion(), params);
    } catch (XynaException ex) {
      logger.error(ex.getMessage(), ex);
      throw new CreateRTAException(ex.getMessage(), ex);
    }
  }
  
  @Override
  public void createApplicationDefinition(XynaOrderServerExtension correlatedXynaOrder, CreateADRequest request) throws CreateApplicationDefinitionException, InsufficientRights {
    try {
      ApplicationDefinition ad = new ApplicationDefinition();
      ad.setName(request.getName());
      ad.setWorkspaceName(request.getWorkspace().getName());
      checkRight(correlatedXynaOrder, UserManagement.ScopedRight.APPLICATION_DEFINITION, Action.insert, ad);
      applicationManagement.defineApplication(request.getName(), request.getDocumentation(), revisionManagement.getRevision(null, null, request.getWorkspace().getName()));
    } catch (XFMG_DuplicateApplicationName | XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY | PersistenceLayerException ex) {
      logger.error(ex.getMessage(), ex);
      throw new CreateApplicationDefinitionException(ex.getMessage(), ex);
    }
  }
  
  @Override
  public List<? extends ApplicationElement> getApplicationContent(XynaOrderServerExtension correlatedXynaOrder, TableInfo tableInfo, GetApplicationContentRequest request) throws GetApplicationContentException, InsufficientRights {
    
    TableHelper<ApplicationElement, TableInfo> tableHelper = TableHelper.<ApplicationElement, TableInfo>init(tableInfo)
        .limitConfig(TableInfo::getLimit)
        .sortConfig(ti -> {
          for (TableColumn tc : ti.getColumns()) {
            TableHelper.Sort sort = TableHelper.createSortIfValid(tc.getPath(), tc.getSort());
            if(sort != null)
              return sort;
          }
          return null;
        })
        .filterConfig(ti -> 
          ti.getColumns().stream()
          .filter(tableColumn -> 
            !tableColumn.getDisableFilter() && tableColumn.getPath() != null && tableColumn.getFilter() != null && tableColumn.getFilter().length() > 0
          )
          .map(tc -> new TableHelper.Filter(tc.getPath(), tc.getFilter()))
          .collect(Collectors.toList())
        )
        .addSelectFunction(TABLE_KEY_RTC_ELEMENT_CONTENT_TYPE, ad -> ad.getElementType() != null ? ad.getElementType() : "")
        .addSelectFunction(TABLE_KEY_RTC_ELEMENT_CONTENT_NAME, ApplicationElement::getName)
        .addSelectFunction(TABLE_KEY_AD_CONTENT_ORIGIN_RTC, ApplicationElement::getOriginRTC);
    
    try {
      
      Long parentRevision = null;
      String applicationName = null;
      String applicationVersion = null;
      ApplicationInformation  applicationInformation = null;
      if(request.getApplication() instanceof RuntimeApplication) {
        checkRight(correlatedXynaOrder, UserManagement.ScopedRight.APPLICATION, Action.list, request.getApplication());
        RuntimeApplication ra = (RuntimeApplication)request.getApplication();
        if(!ra.getIsLocal()) {
          return Collections.emptyList();
        }
        applicationName = ra.getName();
        applicationVersion = ra.getVersion();
        applicationInformation = applicationManagement.getApplicationInformation(applicationName, applicationVersion);
      } else if (request.getApplication() instanceof ApplicationDefinition) {
        checkRight(correlatedXynaOrder, UserManagement.ScopedRight.APPLICATION_DEFINITION, Action.list, request.getApplication());
        ApplicationDefinition ad = (ApplicationDefinition)request.getApplication();
        parentRevision = revisionManagement.getRevision(null, null, ad.getWorkspaceName());
        applicationName = ad.getName();
        applicationInformation = applicationManagement.getApplicationDefinitionInformation(applicationName, parentRevision);
      } else {
        throw new GetApplicationContentException("Unsupported Application");
      }
      List<ApplicationEntryStorable> plainEntries = applicationManagement.listApplicationDetails(applicationName, applicationVersion, false, null, parentRevision);
      
      TreeSet<ApplicationEntryStorable> plainSet = new TreeSet<>(ApplicationEntryStorable.COMPARATOR);
      if (plainEntries != null) {
        plainSet.addAll(plainEntries);
      }
      
      TreeSet<ApplicationEntryStorable> dependencySet = new TreeSet<>(ApplicationEntryStorable.COMPARATOR);
      if (request.getIncludeImplicit()) {
        List<ApplicationEntryStorable> includingDeps = applicationManagement.listApplicationDetails(applicationName, applicationVersion, true, null, parentRevision);
        if (includingDeps != null) {
          dependencySet.addAll(includingDeps);
        }
        dependencySet.removeAll(plainSet);
      }
      List<ApplicationElement> adElements = new ArrayList<>();
      Set<String> includedApplicationEntries = new HashSet<>();
      
      // explicit
      plainSet.stream()
      .filter(aes -> !includedApplicationEntries.contains(aes.getType() + aes.getName()))
      .forEach(aes -> {
        adElements.add(createApplicationElement(aes, DependencyType.explicit));
        includedApplicationEntries.add(aes.getType() + aes.getName());
      });
      tableHelper.sort(adElements);
      
      // implicit
      List<ApplicationElement> adElementsImplicit = new ArrayList<>();
      dependencySet.stream()
      .filter(aes -> !includedApplicationEntries.contains(aes.getType() + aes.getName()))
      .forEach(aes -> {
        adElementsImplicit.add(createApplicationElement(aes, DependencyType.implicit));
        includedApplicationEntries.add(aes.getType() + aes.getName());
      });
      tableHelper.sort(adElementsImplicit);
      adElements.addAll(adElementsImplicit);
      
      // indirect
      if(request.getIncludeIndirect() && applicationInformation != null) {
        List<HierarchicalDependency> dependencies = new ArrayList<>();
        
        Collection<RuntimeDependencyContext> requirements = applicationInformation.getRequirements();
        for (RuntimeDependencyContext rdc : requirements) {
          HierarchicalDependency hierarchicalDependency = new HierarchicalDependency(rdc, 0, null, DependencyType.explicit);
          dependencies.add(hierarchicalDependency);
          dependencies.addAll(getHierarchicalDependencies(rdc, 1, hierarchicalDependency, true, DependencyType.explicit));
          dependencies.forEach(hd -> {
            try {
              List<ApplicationElement> adElementsIndirect = getApplicationElementsByRtc(hd.context.asCorrespondingRuntimeContext(), DependencyType.indirect);
              tableHelper.sort(adElementsIndirect);
              adElementsIndirect.stream()
              .filter(e -> !includedApplicationEntries.contains(e.getElementType() + e.getName()))
              .forEach(ade -> {
                adElements.add(ade);
                includedApplicationEntries.add(ade.getElementType() + ade.getName());
              });
            } catch (XynaException ex) {
              logger.error(ex.getMessage(), ex);
            }
          });
        }
      }
      
      // independent
      if(request.getIncludeUnassigned() && parentRevision != null) {
        List<ApplicationElement> adElementsUnassigned = getApplicationElementsByRtc(revisionManagement.getRuntimeContext(parentRevision), DependencyType.independent);
        tableHelper.sort(adElementsUnassigned);
        adElementsUnassigned.stream()
        .filter(e -> !includedApplicationEntries.contains(e.getElementType() + e.getName()))
        .forEach(ade -> {
          adElements.add(ade);
          includedApplicationEntries.add(ade.getElementType() + ade.getName());
        });
      }

      List<ApplicationElement> result = adElements.stream()
      .filter(tableHelper.filter())
      .collect(Collectors.toList());
      //tableHelper.sort(result);
      return tableHelper.limit(result);
    } catch (XynaException ex) {
      logger.error(ex.getMessage(), ex);
      throw new GetApplicationContentException(ex.getMessage(), ex);
    }
  }
  
  private ApplicationElement createApplicationElement(ApplicationEntryStorable aes, DependencyType dependencyType) {
    ApplicationElement e = new ApplicationElement();
    e.setDependencyType(dependencyType.name());
    if(aes.getTypeAsEnum() != null) {
      e.setElementType(aes.getTypeAsEnum().name());
    }
    e.setName(aes.getName());
    return e;
  }
  
  private String getRtcName(RuntimeContext rtc) {
    if(rtc instanceof com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace) {
      return rtc.getName();
    } else if (rtc instanceof Application) {
      Application app = (Application)rtc;
      return app.getName() + " " + app.getVersionName();
    } else if (rtc instanceof DataModel) {
      DataModel dm = (DataModel)rtc;
      return dm.getName();
    }
    return "";
  }
  
  private List<ApplicationElement> getApplicationElementsByRtc(RuntimeContext rtc, DependencyType dependencyType) throws XynaException {
    List<ApplicationElement> result = new ArrayList<>();
    
    Long revision = revisionManagement.getRevision(rtc);
    
    // WORKFLOW, DATATYPE, EXCEPTION
    Map<ApplicationEntryType, Map<String, DeploymentStatus>> dsMap = multiChannelPortal.listDeploymentStatuses(revision);
    dsMap.entrySet().stream()
    .filter(entry -> entry.getKey() == ApplicationEntryType.WORKFLOW || entry.getKey() == ApplicationEntryType.DATATYPE || entry.getKey() == ApplicationEntryType.EXCEPTION)
    .forEach(entry -> {
      entry.getValue().keySet().forEach(fqn -> {
        ApplicationElement e = new ApplicationElement();
        e.setDependencyType(dependencyType.name());
        if(entry.getKey() != null) {
          e.setElementType(entry.getKey().name());
        }
        e.setName(fqn);
        e.setOriginRTC(getRtcName(rtc));
        result.add(e);
      });
    });
    
    // FORMDEFINITION
    XMOMDatabaseSelect select = new XMOMDatabaseSelect();
    select.addDesiredResultTypes(XMOMDatabaseType.FORMDEFINITION);
    
    List<XMOMDatabaseSelect> selectList = new ArrayList<>();
    selectList.add(select);
    
    XMOMDatabaseSearchResult searchResult = multiChannelPortal.searchXMOMDatabase(selectList, -1, revision);
    searchResult.getResult().forEach(sr -> {
      ApplicationElement e = new ApplicationElement();
      e.setDependencyType(dependencyType.name());
      e.setElementType(ApplicationEntryType.FORMDEFINITION.name());
      e.setName(sr.getFqName());
      e.setOriginRTC(getRtcName(rtc));
      result.add(e);
    });
    
    // TRIGGER, TRIGGERINSTANCE
    multiChannelPortal.listTriggerInformation().stream()
      .filter(ti -> ti.getRuntimeContext().equals(rtc))
      .forEach(ti -> {
        ApplicationElement e = new ApplicationElement();
        e.setDependencyType(dependencyType.name());
        e.setElementType(ApplicationEntryType.TRIGGER.name());
        e.setName(ti.getTriggerName());
        e.setOriginRTC(rtc.getName());
        result.add(e);
        
        ti.getTriggerInstances().forEach(tin -> {
          ApplicationElement e2 = new ApplicationElement();
          e2.setDependencyType(dependencyType.name());
          e2.setElementType(ApplicationEntryType.TRIGGERINSTANCE.name());
          e2.setName(tin.getTriggerInstanceName());
          e2.setOriginRTC(getRtcName(rtc));
          result.add(e2);
        });
    });
    
    // FILTER, FILTERINSTANCE
    multiChannelPortal.listFilterInformation().stream()
    .filter(fi -> fi.getRuntimeContext().equals(rtc))
    .forEach(ti -> {
      ApplicationElement e = new ApplicationElement();
      e.setDependencyType(dependencyType.name());
      e.setElementType(ApplicationEntryType.FILTER.name());
      e.setName(ti.getTriggerName());
      e.setOriginRTC(getRtcName(rtc));
      result.add(e);
      
      ti.getFilterInstances().forEach(fin -> {
        ApplicationElement e2 = new ApplicationElement();
        e2.setDependencyType(dependencyType.name());
        e2.setElementType(ApplicationEntryType.FILTERINSTANCE.name());
        e2.setName(fin.getTriggerInstanceName());
        e2.setOriginRTC(getRtcName(rtc));
        result.add(e2);
      });
    });
    
    // ORDERTYPE
    multiChannelPortal.listOrdertypes(rtc).stream()
    .filter(ot -> ot.getRuntimeContext().equals(rtc))
    .forEach(ti -> {
      ApplicationElement e = new ApplicationElement();
      e.setDependencyType(dependencyType.name());
      e.setElementType(ApplicationEntryType.ORDERTYPE.name());
      e.setName(ti.getOrdertypeName());
      e.setOriginRTC(getRtcName(rtc));
      result.add(e);
    });
    
    // CAPACITY
    multiChannelPortal.listCapacityInformation().stream().forEach(ti -> {
      ApplicationElement e = new ApplicationElement();
      e.setDependencyType(dependencyType.name());
      e.setElementType(ApplicationEntryType.CAPACITY.name());
      e.setName(ti.getName());
      result.add(e);
    });
    
    // XYNAPROPERTY
    multiChannelPortal.getPropertiesReadOnly().entrySet().stream().forEach(p -> {
      ApplicationElement e = new ApplicationElement();
      e.setDependencyType(dependencyType.name());
      e.setElementType(ApplicationEntryType.XYNAPROPERTY.name());
      e.setName(p.getKey());
      result.add(e);
    });
    
    // SHAREDLIB
    multiChannelPortal.listSharedLibs(revision).forEach(sl -> {
      ApplicationElement e = new ApplicationElement();
      e.setDependencyType(dependencyType.name());
      e.setElementType(ApplicationEntryType.SHAREDLIB.name());
      e.setName(sl.getName());
      e.setOriginRTC(getRtcName(rtc));
      result.add(e);
    });
    
    // ORDERINPUTSOURCE
    if(rtc instanceof com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace) {
      SearchRequestBean srb = new SearchRequestBean(ArchiveIdentifier.orderInputSource, -1);
      Map<String,String> filterEntries = new HashMap<>();
      filterEntries.put("workspace", SelectionParser.escape(rtc.getName()));
      srb.setFilterEntries(filterEntries);
      
      @SuppressWarnings("unchecked")
      SearchResult<OrderInputSourceStorable> oisSearchResult = (SearchResult<OrderInputSourceStorable>) multiChannelPortal.search(srb);
      oisSearchResult.getResult().forEach(ois -> {
        ApplicationElement e = new ApplicationElement();
        e.setDependencyType(dependencyType.name());
        e.setElementType(ApplicationEntryType.ORDERINPUTSOURCE.name());
        e.setName(ois.getName());
        e.setOriginRTC(getRtcName(rtc));
        result.add(e);
      });
    }
    
    return result;
  }
  
  @Override
  public void createWorkspace(XynaOrderServerExtension correlatedXynaOrder, CreateWorkspaceRequest request) throws CreateWorkspaceException, InsufficientRights {
    try {
      checkRight(correlatedXynaOrder, UserManagement.Rights.WORKINGSET_MANAGEMENT.name());
      com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace workspace = new com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace(request.getName());
      CreateWorkspaceResult result = workspaceManagement.createWorkspace(workspace, sessionManagement.resolveSessionToUser(correlatedXynaOrder.getSessionId()));
      if(result.getResult() != Result.Failed && request.getRepositoryLink() instanceof SVNRepositoryLink) {
        SVNRepositoryLink svnRepositoryLink = (SVNRepositoryLink)request.getRepositoryLink();
        
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put(REPOSITORY_ACCES_KEY_BRANCH_BASE_DIR, svnRepositoryLink.getBaseDirectoryForBranches());
        paramMap.put(REPOSITORY_ACCES_KEY_HOOK_PORT, svnRepositoryLink.getHookManagerPort());
        paramMap.put(REPOSITORY_ACCES_KEY_PASSWORD, svnRepositoryLink.getPassword());
        paramMap.put(REPOSITORY_ACCES_KEY_PATH, svnRepositoryLink.getPathInSVN());
        paramMap.put(REPOSITORY_ACCES_KEY_SERVER_NAME, svnRepositoryLink.getSVNServerNameIP());
        paramMap.put(REPOSITORY_ACCES_KEY_USER, svnRepositoryLink.getUsername());
        
        String normalizedName = workspace.getName().replaceAll("[^a-zA-Z0-9_]", "_");
        
        Map<String, RepositoryAccess> repositoryAccesses = repositoryAccessManagement.listRepositoryAccessInstances();
        String tmpRepositoryAccessInstanceName = normalizedName;
        int i = 0;
        while(repositoryAccesses.containsKey(tmpRepositoryAccessInstanceName)) {
          i++;
          tmpRepositoryAccessInstanceName = normalizedName + i;
        }
        String repositoryAccessInstanceName = tmpRepositoryAccessInstanceName;
        
        InstantiateRepositoryAccessParameters instantiateRepositoryAccessParameters = new InstantiateRepositoryAccessParameters();
        instantiateRepositoryAccessParameters.setRepositoryAccessInstanceName(repositoryAccessInstanceName);
        instantiateRepositoryAccessParameters.setRepositoryAccessName(svnRepositoryLink.getName());
        instantiateRepositoryAccessParameters.setParameterMap(paramMap);
        instantiateRepositoryAccessParameters.setCodeAccessName(normalizedName);
        instantiateRepositoryAccessParameters.setXmomAccessName(normalizedName);
        
        repositoryAccessManagement.instantiateRepositoryAccessInstance(instantiateRepositoryAccessParameters, revisionManagement.getRevision(workspace));
        
      }
    } catch (XFMG_CouldNotBuildNewWorkspace | XDEV_CodeAccessInitializationException | XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY | PersistenceLayerException ex) {
      logger.error(ex.getMessage(), ex);
      throw new CreateWorkspaceException(ex.getMessage(), ex);
    }
  }
  
  @Override
  public void deleteApplicationDefinition(XynaOrderServerExtension correlatedXynaOrder, ApplicationDefinition applicationDefinition) throws DeleteApplicationDefinitionException, InsufficientRights {
    try {
      checkRight(correlatedXynaOrder, UserManagement.Rights.APPLICATION_ADMINISTRATION.name());
      
      Long revision = revisionManagement.getRevision(null, null, applicationDefinition.getWorkspaceName());
      
      RemoveApplicationParameters params = new RemoveApplicationParameters();
      params.setParentWorkspace(revisionManagement.getWorkspace(revision));
      params.setUser(sessionManagement.resolveSessionToUser(correlatedXynaOrder.getSessionId()));
      
      applicationManagement.removeApplicationVersion(applicationDefinition.getName(), null, params, new SingleRepositoryEvent(revision));
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY | XFMG_CouldNotRemoveApplication | PersistenceLayerException ex) {
      logger.error(ex.getMessage(), ex);
      throw new DeleteApplicationDefinitionException(ex.getMessage(), ex);
    }
  }
  
  @Override
  public ApplicationDefinitionDetails getADDetails(XynaOrderServerExtension correlatedXynaOrder, ApplicationDefinition applicationDefinition) throws GetADDetailsException, InsufficientRights {
    try {
      checkRight(correlatedXynaOrder, UserManagement.ScopedRight.APPLICATION_DEFINITION, Action.list, applicationDefinition);
      
      Long revision = revisionManagement.getRevision(null, null, applicationDefinition.getWorkspaceName());
      ApplicationInformation ai = applicationManagement.getApplicationDefinitionInformation(applicationDefinition.getName(), revision);
      ApplicationDefinitionDetails d = new ApplicationDefinitionDetails();
      d.setDocumentation(ai.getComment());
      d.setName(ai.getName());
      d.setRemoteExecution(ai.getRemoteStub());
      d.setSourceVersion(prepareSourceVersion(ai.getVersion()));
      d.setState(ai.getState().name());
      d.setWorkspaceName(applicationDefinition.getWorkspaceName());
      return d;
    } catch (PersistenceLayerException | XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY ex) {
      logger.error(ex.getMessage(), ex);
      throw new GetADDetailsException(ex.getMessage(), ex);
    }
  }
  
  private String prepareSourceVersion(String sourceVersion) {
    String version = sourceVersion;
    if (version.startsWith(APPLICATION_DEFINITION_VERSION_PREFIX)) {
      version = version.substring(APPLICATION_DEFINITION_VERSION_PREFIX.length());
    } else if (version.equals(APPLICATION_DEFINITION_DEFAULT_VERSION)) {
      version = "";
    }
    return version;
  }
  
  @Override
  public void deleteWorkspace(XynaOrderServerExtension correlatedXynaOrder, DeleteWorkspaceRequest request) throws DeleteWorkspaceException, InsufficientRights {
    try {
      checkRight(correlatedXynaOrder, UserManagement.Rights.WORKINGSET_MANAGEMENT.name());
      
      RemoveWorkspaceParameters params = new RemoveWorkspaceParameters();
      params.setForce(request.getStopRunningOrders());
      params.setUser(sessionManagement.resolveSessionToUser(correlatedXynaOrder.getSessionId()));
      params.setCleanupXmls(true);
      
      Long revision = revisionManagement.getRevision(null, null, request.getWorkspace().getName());
      workspaceManagement.removeWorkspace(revisionManagement.getWorkspace(revision), params);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY | XFMG_CouldNotRemoveWorkspace | PersistenceLayerException e) {
      logger.error(e.getMessage(), e);
      throw new DeleteWorkspaceException(e.getMessage(), e);
    }
  }
  
  @Override
  public void clearWorkspace(XynaOrderServerExtension correlatedXynaOrder, ClearWorkspaceRequest request) throws ClearWorkspaceException, InsufficientRights {
    try {
      checkRight(correlatedXynaOrder, UserManagement.Rights.WORKINGSET_MANAGEMENT.name());
      ClearWorkspaceParameters params = new ClearWorkspaceParameters();
      params.setIgnoreRunningOrders(request.getStopRunningOrders());
      Long revision = revisionManagement.getRevision(null, null, request.getWorkspace().getName());
      workspaceManagement.clearWorkspace(revisionManagement.getWorkspace(revision), params);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY | XFMG_ClearWorkingSetFailedBecauseOfRunningOrders | PersistenceLayerException | XPRC_TimeoutWhileWaitingForUnaccessibleOrderException ex) {
      logger.error(ex.getMessage(), ex);
      throw new ClearWorkspaceException(ex.getMessage(), ex);
    }
  }
  
  private void checkRight(XynaOrderServerExtension correlatedXynaOrder, String right) throws InsufficientRights, PersistenceLayerException {
    if(!multiChannelPortal.hasRight(right, correlatedXynaOrder.getCreationRole())) {
      throw new InsufficientRights(Arrays.asList(new UserAuthenticationRight(right)));
    }
  }
  
  private boolean checkRight(XynaOrderServerExtension correlatedXynaOrder, UserManagement.ScopedRight right, UserManagement.Action action, xmcp.factorymanager.rtcmanager.RuntimeContext rtc) throws InsufficientRights, PersistenceLayerException {
    List<String> parts = new ArrayList<>(3);
    parts.add(action.toString());
    switch(right) {
      case APPLICATION:
        if(rtc == null) {
          parts.add("*");
          parts.add("*");
        } else {
          RuntimeApplication app = (RuntimeApplication)rtc;
          parts.add(app.getName());
          parts.add(app.getVersion());
        }
        break;
      case APPLICATION_DEFINITION:
        if(rtc == null) {
          parts.add("*");
          parts.add("*");
        } else {
          ApplicationDefinition ad = (ApplicationDefinition)rtc;
          parts.add(ad.getWorkspaceName());
          parts.add(ad.getName());
        }
        break;
      case WORKSPACE:
        if(rtc == null) {
          parts.add("*");
        } else {
          Workspace w = (Workspace)rtc;
          parts.add(w.getName());
        }
        break;
      default:
        throw new UnsupportedOperationException("ScopedRight " + right.getKey() + " isn't supported yet.");
    }
    return hasRight(correlatedXynaOrder, right, parts);
  }
  
  private boolean hasRight(XynaOrderServerExtension correlatedXynaOrder, UserManagement.ScopedRight right, List<String> parts) throws PersistenceLayerException, InsufficientRights {
    ScopedRightCache rightCache = userManagement.getRoleRightScope(correlatedXynaOrder.getCreationRole());
    if (!rightCache.hasRight(right.getKey(), parts.toArray(new String[] {}))) {
      throw new InsufficientRights(Arrays.asList(new UserAuthenticationRight(right.getKey())));
    } else {
      return true;
    }
  }
  
  @Override
  public List<? extends RuntimeApplication> getRuntimeApplicationList(XynaOrderServerExtension correlatedXynaOrder) throws GetRuntimeApplicationsException, InsufficientRights {
    try {
      checkRight(correlatedXynaOrder, UserManagement.ScopedRight.APPLICATION, Action.list, null);
      Collection<ApplicationStorable> appInfos = applicationManagement.listApplicationStorables();
      List<RemoteApplicationDetails> remoteApplicationDetails = getRuntimeApplications(correlatedXynaOrder, true);
      List<RuntimeApplication> result = new ArrayList<>();
      for (RemoteApplicationDetails rad : remoteApplicationDetails) {
        if(rad.getApplicationDetails() == null) {
          continue; //happens for applications @ remote node, if factory is not running there
        }
        for (ApplicationDetails app : rad.getApplicationDetails()) {
          result.add(convert(rad, app, appInfos));
        }
      }
      if(!SHOW_GLOBAL_APP_MGMT.get()) {
        for(int i = 0; i < result.size(); i++) {
          RuntimeApplication rta = result.get(i);
          if(rta.getName().equals("GlobalApplicationMgmt")) {
            result.remove(rta);
            i--;
          }
        }
      }
      return result.stream()
          .sorted(Comparator.comparing(ra -> ra.getName() + " " + ra.getVersion(), String.CASE_INSENSITIVE_ORDER))
          .collect(Collectors.toList());
    } catch (InsufficientRights ex) {
      throw ex;
    } catch (Exception ex) {
      throw new GetRuntimeApplicationsException(ex.getMessage(), ex);
    }
  }
  
  private RuntimeApplication convert(RemoteApplicationDetails rad, ApplicationDetails app, Collection<ApplicationStorable> appInfos) {
    RuntimeApplication a = new RuntimeApplication();
    a.setDocumentation(app.getComment());
    a.setFactoryNode(rad.getFactoryNode().getName());
    a.setName(app.getName());
    a.setState(getRTAStateForGui(app.getState()));
    a.setIsLocal(rad.getFactoryNode().getLocal());
    a.setVersion(app.getVersion());
    
    if(a.getIsLocal()) {
      try {
        for (ApplicationStorable as : appInfos) {
          if(as.getName().equals(app.getName()) && as.getVersion().equals(app.getVersion())) {
            ApplicationInformation ai = applicationManagement.getApplicationInformation(as, true);
            a.setState(ai.getState().name());
            break;
          }
        }
      } catch (PersistenceLayerException | XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        logger.error(e.getMessage(), e);
      }
    }
    return a;
  }
  
  @SuppressWarnings("unchecked")
  private List<RemoteApplicationDetails> getRuntimeApplications(XynaOrderServerExtension correlatedXynaOrder, boolean includeRemote) throws Exception {
    DestinationKey dk;
    if(includeRemote) {
      dk = ApplicationManagementImpl.LIST_APPLICATIONS_DESTINATION.get();
    } else {
      dk = ApplicationManagementImpl.LIST_LOCAL_APPLICATIONS_DESTINATION.get();
    }
    
    OrderExecutionResponse oer =  startOrderSynchronouslyAndReturnOrder(dk, correlatedXynaOrder, new ListApplicationParameter(true, true));
    if (oer.hasExecutedSuccesfully()) {
      SynchronousSuccesfullOrderExecutionResponse ssoer = (SynchronousSuccesfullOrderExecutionResponse) oer;
      return (XynaObjectList<RemoteApplicationDetails>) ssoer.getResponse();
    } else {
      if(oer instanceof ErroneousOrderExecutionResponse) {
        ErroneousOrderExecutionResponse erroneousOrderExecutionResponse = (ErroneousOrderExecutionResponse)oer;
        throw new Exception(erroneousOrderExecutionResponse.getExceptionInformation().getMessage());
      }
    }
    return Collections.emptyList();
  }
  
  private OrderExecutionResponse startOrderSynchronouslyAndReturnOrder(DestinationKey destinationKey, XynaOrderServerExtension correlatedXynaOrder, GeneralXynaObject... orderInput) throws XPRC_XmlParsingException, XPRC_InvalidXMLForObjectCreationException, XPRC_MDMObjectCreationException {
    RemoteXynaOrderCreationParameter rxocp = new RemoteXynaOrderCreationParameter(destinationKey, orderInput);
    rxocp.setSessionId(correlatedXynaOrder.getSessionId());
    rxocp.setTransientCreationRole(correlatedXynaOrder.getCreationRole());
    rxocp.convertInputPayload();
    
    ResultController resultController = new ResultController();
    resultController.setDefaultWrappingTypeForExceptions(WrappingType.SIMPLE);
    resultController.setDefaultWrappingTypeForXMOMTypes(WrappingType.ORIGINAL);
    
    return multiChannelPortal.startOrderSynchronouslyAndReturnOrder(rxocp, resultController);
  }
  
  @Override
  public List<? extends RuntimeApplication> getRuntimeApplications(XynaOrderServerExtension correlatedXynaOrder, TableInfo tableInfo) throws GetRuntimeApplicationsException, InsufficientRights {
    TableHelper<RuntimeApplication, TableInfo> tableHelper = TableHelper.<RuntimeApplication, TableInfo>init(tableInfo)
        .limitConfig(TableInfo::getLimit)
        .sortConfig(ti -> {
          for (TableColumn tc : ti.getColumns()) {
            TableHelper.Sort sort = TableHelper.createSortIfValid(tc.getPath(), tc.getSort());
            if(sort != null)
              return sort;
          }
          return null;
        })
        .filterConfig(ti -> 
          ti.getColumns().stream()
          .filter(tableColumn -> 
            !tableColumn.getDisableFilter() && tableColumn.getPath() != null && tableColumn.getFilter() != null && tableColumn.getFilter().length() > 0
          )
          .map(tc -> new TableHelper.Filter(tc.getPath(), tc.getFilter()))
          .collect(Collectors.toList())
        )
        .addSelectFunction(TABLE_KEY_RTA_NAME, RuntimeApplication::getName)
        .addSelectFunction(TABLE_KEY_RTA_VERSION, RuntimeApplication::getVersion)
        .addSelectFunction(TABLE_KEY_RTA_STATE, RuntimeApplication::getState)
        .addSelectFunction(TABLE_KEY_RTA_FACTORY_NODE, RuntimeApplication::getFactoryNode)
        .addSelectFunction(TABLE_KEY_RTA_DOCUMENTATION, RuntimeApplication::getDocumentation);
    try {
      checkRight(correlatedXynaOrder, UserManagement.ScopedRight.APPLICATION, Action.list, null);
      List<RemoteApplicationDetails> remoteApplicationDetails = getRuntimeApplications(correlatedXynaOrder, true);
      Collection<ApplicationStorable> appInfos = applicationManagement.listApplicationStorables();
      List<RuntimeApplication> result = new ArrayList<>();
      for (RemoteApplicationDetails rad : remoteApplicationDetails) {
        if (rad.getApplicationDetails() == null) {
          // can be null when remote node is not reachable
          continue;
        }

        for (ApplicationDetails app : rad.getApplicationDetails()) {
          result.add(convert(rad, app, appInfos));
        }
      }
      result = result.stream().filter(tableHelper.filter()).collect(Collectors.toList());
      tableHelper.sort(result);
      return tableHelper.limit(result);
    } catch (Exception ex) {
      throw new GetRuntimeApplicationsException(ex.getMessage(), ex);
    }
  }
  
  @Override
  public List<? extends Dependency> getDependentRTCs(XynaOrderServerExtension correlatedXynaOrder, TableInfo tableInfo, GetDependentRTCsRequest request) throws GetDependentRTCException, InsufficientRights {
    
    try {
      if(request.getRuntimeContext() instanceof RuntimeApplication) {
        checkRight(correlatedXynaOrder, UserManagement.ScopedRight.APPLICATION, Action.list, request.getRuntimeContext());
      } else if (request.getRuntimeContext() instanceof ApplicationDefinition) {
        checkRight(correlatedXynaOrder, UserManagement.ScopedRight.APPLICATION_DEFINITION, Action.list, request.getRuntimeContext());
      } else if (request.getRuntimeContext() instanceof Workspace) {
        checkRight(correlatedXynaOrder, UserManagement.ScopedRight.WORKSPACE, Action.list, request.getRuntimeContext());
      }
    } catch (PersistenceLayerException ex) {
      logger.error(ex.getMessage(), ex);
      throw new GetDependentRTCException(ex.getMessage(), ex);
    }
    
    List<Dependency> result = new ArrayList<>();
    TableHelper<HierarchicalDependency, TableInfo> tableHelper = TableHelper.<HierarchicalDependency, TableInfo>init(tableInfo)
        .limitConfig(TableInfo::getLimit)
        .sortConfig(ti -> {
          for (TableColumn tc : ti.getColumns()) {
            TableHelper.Sort sort = TableHelper.createSortIfValid(tc.getPath(), tc.getSort());
            if(sort != null)
              return sort;
          }
          return null;
        })
        .filterConfig(ti -> 
          ti.getColumns().stream()
          .filter(tableColumn -> 
            !tableColumn.getDisableFilter() && tableColumn.getPath() != null && tableColumn.getFilter() != null && tableColumn.getFilter().length() > 0
          )
          .map(tc -> new TableHelper.Filter(tc.getPath(), tc.getFilter()))
          .collect(Collectors.toList())
        )
        .addSelectFunction(TABLE_KEY_REQUIRED_RTC_NAME, d -> {
          if(d.getContext().getRuntimeDependencyContextType() == RuntimeDependencyContextType.Application) {
            Application application = (Application) d.getContext().asCorrespondingRuntimeContext();
            return application.getName() + " " + application.getVersionName();
          }
          return d.getContext().getName();
        })
        .addSelectFunction(TABLE_KEY_REQUIRED_RTC_STATE, d -> getStateOfRuntimeDependencyContext(d.getContext()))
        .addSelectFunction(TABLE_KEY_REQUIRED_RTC_RTC_TYPE, d -> getRuntimeDependencyContextTypeGuiName(d.getContext().getRuntimeDependencyContextType()));
    
    
    List<HierarchicalDependency> dependencies;
    if(request.getReferenceDirection() instanceof ReferenceDirectionForward) {
      dependencies = getForwardDependencies(correlatedXynaOrder, request);
    } else if (request.getReferenceDirection() instanceof ReferenceDirectionBackwards) {
      dependencies = getBackwardDependencies(correlatedXynaOrder, request);
    } else {
      throw new GetDependentRTCException("Unsupported ReferenceDirection");
    }
    if(request.getIncludeUnassigned()) {
      appendUnassignedDependencies(request, dependencies);
    }
    updateDependencyTypes(dependencies);
    tableHelper.sort(dependencies);
    
    dependencies = dependencies.stream()
      .filter(tableHelper.filter())
      .collect(Collectors.toList());
    dependencies = tableHelper.limit(dependencies);
    
    dependencies.forEach(hd -> {
      result.add(convert(hd));
      List<HierarchicalDependency> cs = hd.getChildrenRecursive();
      cs.forEach(hd2 -> result.add(convert(hd2)));
    });
    
    return result;
  }
  
  private void updateDependencyTypes(List<HierarchicalDependency> dependencies ) {
    EnumMap<DependencyType, List<HierarchicalDependency>> map = new EnumMap<>(DependencyType.class);
    for (DependencyType dt : DependencyType.values()) {
      map.put(dt, new ArrayList<>());
    }
    for (HierarchicalDependency hd : dependencies) {
      map.get(hd.dependencyType).add(hd);
      for (HierarchicalDependency hdc : hd.getChildrenRecursive()) {
        map.get(hdc.dependencyType).add(hdc);
      }
    }
    List<HierarchicalDependency> implicit = map.get(DependencyType.implicit);
    for (HierarchicalDependency hd : implicit) {
      if(map.get(DependencyType.explicit).contains(hd) && hd.dependencyType != DependencyType.explicit) {
        hd.dependencyType = DependencyType.explicit;
      }
    }
    List<HierarchicalDependency> independent = map.get(DependencyType.independent);
    for (HierarchicalDependency hd : independent) {
      if(map.get(DependencyType.explicit).contains(hd) && hd.dependencyType != DependencyType.explicit) {
        hd.dependencyType = DependencyType.explicit;
      } else if (map.get(DependencyType.implicit).contains(hd) && hd.dependencyType != DependencyType.explicit) {
        hd.dependencyType = DependencyType.implicit;
      }
    }
  }
  
  private String getRTAStateForGui(xfmg.xfctrl.appmgmt.ApplicationState state) {
    if (state instanceof Running) {
      return ApplicationState.RUNNING.name();
    } else if (state instanceof Stopped) {
      return ApplicationState.STOPPED.name();
    } else if (state instanceof Warning) {
      return ApplicationState.WARNING.name();
    } else if (state instanceof xfmg.xfctrl.appmgmt.Error) {
      return ApplicationState.ERROR.name();
    } else if (state instanceof Workingcopy) {
      return ApplicationState.WORKINGCOPY.name();
    } else {
      return ApplicationState.AUDIT_MODE.name();
    }
  }
  
  private String getRuntimeDependencyContextTypeGuiName(RuntimeDependencyContextType type) {
    if(type == null) {
      return "";
    } else if(type == RuntimeDependencyContextType.Application) {
      return GUI_APPLICATION_NAME;
    } else {
      return type.name();
    }
  }
  
  private Dependency convert(HierarchicalDependency hd) {
    Dependency d = new Dependency();
    d.setRtcType(getRuntimeDependencyContextTypeGuiName(hd.getContext().getRuntimeDependencyContextType()));
    d.setDependencyType(hd.getDependencyType().name());
    d.setHierarchyLevel(hd.getLevel());
    xmcp.factorymanager.rtcmanager.RuntimeContext rtc = null;
    switch(hd.getContext().getRuntimeDependencyContextType()) {
      case Application:
        rtc = new RuntimeApplication();
        Application application = (Application) hd.getContext().asCorrespondingRuntimeContext();
        ((RuntimeApplication)rtc).setVersion(application.getVersionName());
        break;
      case ApplicationDefinition:
        rtc = new ApplicationDefinition();
        ((ApplicationDefinition)rtc).setWorkspaceName(hd.getRuntimeContext().getName());
        break;
      case Workspace:
        rtc = new Workspace();
        break;
    }
    rtc.setState(getStateOfRuntimeDependencyContext(hd.getContext()));
    rtc.setName(hd.getContext().getName());
    d.setRuntimeContext(rtc);
    return d;
  }
  
  private String getStateOfRuntimeDependencyContext(RuntimeDependencyContext context) {
    switch(context.getRuntimeDependencyContextType()) {
      case Application:
        com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application application = (com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application) context.asCorrespondingRuntimeContext();
        try {
          ApplicationState applicationState = applicationManagement.getApplicationState(application.getName(), application.getVersionName());
          return applicationState != null ? applicationState.name() : "UNKNOWN";
        } catch (PersistenceLayerException e) {
          logger.error(e.getMessage(), e);
        }
        break;
      case ApplicationDefinition:
        com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace workspace = (com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace) context.asCorrespondingRuntimeContext();
        try {
          ApplicationInformation applicationInformation = applicationManagement.getApplicationDefinitionInformation(context.getName(), revisionManagement.getRevision(workspace));
          return (applicationInformation != null && applicationInformation.getState() != null) ? applicationInformation.getState().name() : "UNKNOWN";
        } catch (PersistenceLayerException | XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY ex) {
          logger.error(ex.getMessage(), ex);
        }
        break;
      case Workspace:
        workspace = (com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace) context.asCorrespondingRuntimeContext();
        try {
          WorkspaceInformation workspaceInformation = workspaceManagement.getWorkspaceDetails(workspace, true);
          return (workspaceInformation != null && workspaceInformation.getState() != null) ? workspaceInformation.getState().name() : "UNKNOWN";
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY | PersistenceLayerException ex) {
          logger.error(ex.getMessage(), ex);
        }
        break;
    }
    return "";
  }
  
  private List<HierarchicalDependency> getHierarchicalDependencies(RuntimeDependencyContext context, int level, HierarchicalDependency parent, boolean includeImplicit, DependencyType dependencyType){
    List<HierarchicalDependency> result = new ArrayList<>();
    Collection<RuntimeDependencyContext> dependencies = rtcDependencyManagement.getDependencies(context);
    for (RuntimeDependencyContext rdc : dependencies) {      
      HierarchicalDependency d = new HierarchicalDependency(rdc, level, parent, dependencyType);
      if(includeImplicit) {
        d.getChildren().addAll(getHierarchicalDependencies(rdc, level + 1, d, includeImplicit, (dependencyType == DependencyType.independent) ? DependencyType.independent : DependencyType.implicit));
      }
      result.add(d);
    }
    return result;
  }
  
  private List<HierarchicalDependency> getForwardDependencies(XynaOrderServerExtension correlatedXynaOrder, GetDependentRTCsRequest request) throws GetDependentRTCException {
    try {
      List<HierarchicalDependency> dependencies = new ArrayList<>();
      if(request.getRuntimeContext() instanceof Workspace) {
        Workspace workspace = (Workspace)request.getRuntimeContext();
        Long revision = revisionManagement.getRevision(null, null, workspace.getName());
        dependencies.addAll(getHierarchicalDependencies(revisionManagement.getWorkspace(revision), 0, null, request.getIncludeImplicit(), DependencyType.explicit));
      } else if (request.getRuntimeContext() instanceof RuntimeApplication) {
        RuntimeApplication runtimeApplication = (RuntimeApplication)request.getRuntimeContext();
        if(runtimeApplication.getIsLocal()) {
          Long revision = revisionManagement.getRevision(runtimeApplication.getName(), runtimeApplication.getVersion(), null);
          dependencies.addAll(getHierarchicalDependencies(revisionManagement.getApplication(revision), 0, null, request.getIncludeImplicit(), DependencyType.explicit));
        } else {
          List<RuntimeDependencyContextInformation> runtimeDependencyContextInformations = listRuntimeDependencyContexts(runtimeApplication.getFactoryNode());

          for (RuntimeDependencyContextInformation info : runtimeDependencyContextInformations) {
            if(info instanceof ApplicationInformation) {
              ApplicationInformation appInfo = (ApplicationInformation)info;
              if(appInfo.getName().equals(runtimeApplication.getName()) && appInfo.getVersion().equals(runtimeApplication.getVersion())) {
                Collection<RuntimeDependencyContext>  requirements = appInfo.getRequirements();
                for (RuntimeDependencyContext rdc : requirements) {
                  HierarchicalDependency d = new HierarchicalDependency(rdc, 0, null, DependencyType.explicit);
                  if(request.getIncludeImplicit()) {
                    d.getChildren().addAll(getHierarchicalDependencies(runtimeDependencyContextInformations, rdc, 1, d, request.getIncludeImplicit(), DependencyType.implicit));
                  }
                  dependencies.add(d);
                }
                break;
              }
            }
          }
        }
      } else if (request.getRuntimeContext() instanceof ApplicationDefinition) {
        ApplicationDefinition applicationDefinition = (ApplicationDefinition)request.getRuntimeContext();
        Long workspaceRevision = revisionManagement.getRevision(null, null, applicationDefinition.getWorkspaceName());
        ApplicationInformation  applicationInformation = applicationManagement.getApplicationDefinitionInformation(applicationDefinition.getName(), workspaceRevision);
        Collection<RuntimeDependencyContext> requirements = applicationInformation.getRequirements();
        for (RuntimeDependencyContext rdc : requirements) {
          HierarchicalDependency d = new HierarchicalDependency(rdc, 0, null, DependencyType.explicit);
          if(request.getIncludeImplicit()) {
            d.getChildren().addAll(getHierarchicalDependencies(rdc, 1, d, request.getIncludeImplicit(), DependencyType.implicit));
          }
          dependencies.add(d);
        }
      }
      
      return dependencies;
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      throw new GetDependentRTCException(ex.getMessage(), ex);
    }
  }
  
  private List<HierarchicalDependency> getHierarchicalDependencies(List<RuntimeDependencyContextInformation> runtimeDependencyContextInformations, RuntimeDependencyContext context, int level, HierarchicalDependency parent, boolean includeImplicit, DependencyType dependencyType){
    List<HierarchicalDependency> result = new ArrayList<>();
    for (RuntimeDependencyContextInformation info : runtimeDependencyContextInformations) {
      Collection<RuntimeDependencyContext> requirements = null;
      if (context instanceof com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application && info instanceof ApplicationInformation) {
        com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application application = (com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application)context;
        ApplicationInformation appInfo = (ApplicationInformation)info;
        if (appInfo.getName().equals(application.getName()) && appInfo.getVersion().equals(application.getVersionName())) {
          requirements = appInfo.getRequirements();
        }
      } else if (context instanceof com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace && info instanceof WorkspaceInformation) {
        com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace workspace = (com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace)context;
        WorkspaceInformation workspaceInfo = (WorkspaceInformation)info;
        if (workspaceInfo.getWorkspace().getName().equals(workspace.getName())) {
          requirements = workspaceInfo.getRequirements();
        }
      }

      if (requirements != null) {
        for (RuntimeDependencyContext rdc : requirements) {
          HierarchicalDependency d = new HierarchicalDependency(rdc, level, parent, dependencyType);
          if (includeImplicit) {
            d.getChildren().addAll(getHierarchicalDependencies(runtimeDependencyContextInformations, rdc, level+1, d, includeImplicit, (dependencyType == DependencyType.independent) ? DependencyType.independent : DependencyType.implicit));
          }

          result.add(d);
        }
      }
    }

    return result;
  }
  
  private List<RuntimeDependencyContextInformation> listRuntimeDependencyContexts(String nodeName) throws ConnectException, XFMG_NodeConnectException, XynaException{
    com.gip.xyna.xfmg.xfctrl.nodemgmt.FactoryNode factoryNode = nodeManagement.getNodeByName(nodeName);
    
    RuntimeContextManagementLinkProfile remoteAccessRTM = factoryNode.getInterFactoryLink().getProfile(InterFactoryLink.InterFactoryLinkProfileIdentifier.RuntimeContextManagement);
    InfrastructureLinkProfile remoteAccessINFRA = factoryNode.getInterFactoryLink().getProfile(InterFactoryLink.InterFactoryLinkProfileIdentifier.Infrastructure);
    
    ListRuntimeDependencyContextParameter lrdcp = new ListRuntimeDependencyContextParameter(false, new HashSet<>(Arrays.asList(RuntimeDependencyContextType.Application, RuntimeDependencyContextType.Workspace)));
    
    return remoteAccessRTM.listRuntimeDependencyContexts(getCredentials(remoteAccessINFRA, factoryNode.getNodeInformation().getName()), lrdcp);
  }
  
  private XynaCredentials getCredentials(InfrastructureLinkProfile profile, String nodeName) throws ConnectException, XFMG_NodeConnectException {
    return checkConnectivityAndAccess(profile, nodeName);
  }

  
  private XynaCredentials checkConnectivityAndAccess(InfrastructureLinkProfile profile, String nodeName) throws XFMG_NodeConnectException, ConnectException  {
    XynaCredentials credentials = cache.getCredentials(nodeName, profile);
    try {
      profile.getExtendedStatus(credentials);
    } catch (XFMG_NodeConnectException e) {
      cache.clearSession(nodeName);
      credentials = cache.getCredentials(nodeName, profile);
      profile.getExtendedStatus(credentials);
    }
    return credentials;
  }
  
  private List<HierarchicalDependency> getBackwardDependencies(XynaOrderServerExtension correlatedXynaOrder, GetDependentRTCsRequest request) throws GetDependentRTCException {
    List<HierarchicalDependency> dependencies = new ArrayList<>();
    try {
      if(request.getRuntimeContext() instanceof ApplicationDefinition) {
        ApplicationDefinition ad = (ApplicationDefinition)request.getRuntimeContext();
        Long revision = revisionManagement.getRevision(null, null, ad.getWorkspaceName());
        List<ApplicationDefinitionInformation> adis = applicationManagement.listApplicationDefinitions(revision);
        for (ApplicationDefinitionInformation adi : adis) {
          if(!adi.getName().contentEquals(ad.getName())) {
            Collection<RuntimeDependencyContext> requirements = adi.getRequirements();
            for (RuntimeDependencyContext rdc : requirements) {
              if(rdc.getRuntimeDependencyContextType() == RuntimeDependencyContextType.ApplicationDefinition
                  && rdc.getName().equals(ad.getName())) {
                dependencies.add(new HierarchicalDependency(rdc, 0, null, DependencyType.explicit));                
              }
            }
          }
        }
      } else {
        Long revision = null;
        if(request.getRuntimeContext() instanceof Workspace) {
          Workspace workspace = (Workspace)request.getRuntimeContext();
          revision = revisionManagement.getRevision(null, null, workspace.getName());
        } else if (request.getRuntimeContext() instanceof RuntimeApplication) {
          RuntimeApplication runtimeApplication = (RuntimeApplication)request.getRuntimeContext();
          if(runtimeApplication.getIsLocal()) {
            revision = revisionManagement.getRevision(runtimeApplication.getName(), runtimeApplication.getVersion(), null);
          }
          else {
            List<RuntimeDependencyContextInformation> runtimeDependencyContextInformations = listRuntimeDependencyContexts(runtimeApplication.getFactoryNode());
            for (RuntimeDependencyContextInformation info : runtimeDependencyContextInformations) {
              if(info instanceof ApplicationInformation) {
                ApplicationInformation appInfo = (ApplicationInformation)info;
                Collection<RuntimeDependencyContext>  requirements = appInfo.getRequirements();
                for (RuntimeDependencyContext rdc : requirements) {
                  if(rdc instanceof Application) {
                    if(rdc.getName().equals(runtimeApplication.getName()) && ((Application)(rdc.asCorrespondingRuntimeContext())).getVersionName().equals(runtimeApplication.getVersion())) {
                      RuntimeDependencyContext context = RuntimeContextDependencyManagement.asRuntimeDependencyContext(info.asRuntimeContext());
                      dependencies.add(new HierarchicalDependency(context, 0, null, DependencyType.explicit));
                    }
                  }
                }
              }
            }
          }
        }
        if(revision != null) {
          RuntimeContext runtimeContext = revisionManagement.getRuntimeContext(revision);
          rtcDependencyManagement.getAllDependencies().forEach((rdc, deps) -> {
            for (RuntimeDependencyContext dep : deps) {
              if(dep.asCorrespondingRuntimeContext().equals(runtimeContext)) {
                dependencies.add(new HierarchicalDependency(rdc, 0, null, DependencyType.explicit));
              }
            }
          });
        }
      }
      return dependencies;
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      throw new GetDependentRTCException(ex.getMessage(), ex);
    } 
  }
  
  private void appendUnassignedDependencies(GetDependentRTCsRequest request, List<HierarchicalDependency> dependencies) {
    
    List<Long> revs = revisionManagement.getAllRevisions();
    for (Long revision : revs) {
      try {
        RuntimeContext rtc = revisionManagement.getRuntimeContext(revision);
        boolean contains = false;
        for (HierarchicalDependency hierarchicalDependency : dependencies) {
          if(hierarchicalDependency.level == 0 && hierarchicalDependency.getRuntimeContext().equals(rtc)) {
            contains = true;
            break;
          }
        }
        if(!contains && rtc instanceof RuntimeDependencyContext) {          
          HierarchicalDependency d = new HierarchicalDependency((RuntimeDependencyContext) rtc, 0, null, DependencyType.independent);
          if(request.getIncludeImplicit()) {
            d.getChildren().addAll(getHierarchicalDependencies((RuntimeDependencyContext) rtc, 1, d, request.getIncludeImplicit(), DependencyType.independent));
          }
          dependencies.add(d);
        }

      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY ex) {
        logger.error(ex.getMessage(), ex);
      }
    }
    if(request.getRuntimeContext() instanceof ApplicationDefinition) {
      ApplicationDefinition applicationDefinition = (ApplicationDefinition)request.getRuntimeContext();
      List<ApplicationDefinitionInformation> applicationDefinitionInformations = applicationManagement.listApplicationDefinitions(true);
      for (ApplicationDefinitionInformation adi : applicationDefinitionInformations){
        if(applicationDefinition.getWorkspaceName().equals(adi.getParentWorkspace().getName()) && !applicationDefinition.getName().equals(adi.getName())) {
          boolean contains = false;
          for (HierarchicalDependency hierarchicalDependency : dependencies) {
            if(hierarchicalDependency.level == 0 
                && hierarchicalDependency.getContext().getRuntimeDependencyContextType() == RuntimeDependencyContextType.ApplicationDefinition
                && hierarchicalDependency.getContext().asCorrespondingRuntimeContext().equals(adi.getParentWorkspace())
                && hierarchicalDependency.getContext().getName().equals(adi.getName())) {
              contains = true;
              break;
            }
          }
          if(!contains) {        
            com.gip.xyna.xfmg.xfctrl.revisionmgmt.ApplicationDefinition ad = new com.gip.xyna.xfmg.xfctrl.revisionmgmt.ApplicationDefinition(adi.getName(), adi.getParentWorkspace());
            
            HierarchicalDependency d = new HierarchicalDependency((RuntimeDependencyContext) ad, 0, null, DependencyType.independent);
            if(request.getIncludeImplicit()) {
              Collection<RuntimeDependencyContext> requirements = adi.getRequirements();
              for (RuntimeDependencyContext rdc : requirements) {
                HierarchicalDependency d2 = new HierarchicalDependency(rdc, 1, null, DependencyType.independent);
                if(request.getIncludeImplicit()) {
                  d2.getChildren().addAll(getHierarchicalDependencies(rdc, 2, d, request.getIncludeImplicit(), DependencyType.independent));
                }
                d.getChildren().add(d2);
              }
            }
            dependencies.add(d);
          }
        }
      }
    }
  }
  
  @Override
  public WorkspaceDetails getWorkspaceDetails(XynaOrderServerExtension correlatedXynaOrder, Workspace workspace) throws GetWorkspaceDetailsException, InsufficientRights {
    try {
      checkRight(correlatedXynaOrder, UserManagement.ScopedRight.WORKSPACE, Action.list, workspace);
      
      Long revision = revisionManagement.getRevision(null, null, workspace.getName());
      List<ApplicationDefinitionInformation> applicationDefinitionInformations = applicationManagement.listApplicationDefinitions(revision, true);
      WorkspaceInformation information = workspaceManagement.getWorkspaceDetails(revisionManagement.getWorkspace(revision), true);
      WorkspaceDetails details = new WorkspaceDetails();
      details.setName(information.getWorkspace().getName());
      details.setState(information.getState() != null ? information.getState().name() : "");
      details.setApplicationDefinitions(getWorkspaceApplicationDefinitions(information.getWorkspace(), applicationDefinitionInformations));
      details.setRevision(revision);
      
      if(information.getRepositoryAccess() != null && information.getRepositoryAccess().getName() != null) {
        SVNRepositoryLink link = new SVNRepositoryLink();
        link.setName(information.getRepositoryAccess().getName());
        Map<String, Object> params = information.getRepositoryAccess().getParamMap();
        if(params != null) {
          link.setBaseDirectoryForBranches(String.valueOf(params.get(REPOSITORY_ACCES_KEY_BRANCH_BASE_DIR)));
          link.setHookManagerPort(String.valueOf(params.get(REPOSITORY_ACCES_KEY_HOOK_PORT)));
          link.setLinkType(information.getRepositoryAccess().getName());
          link.setPathInSVN(String.valueOf(params.get(REPOSITORY_ACCES_KEY_PATH))); 
          link.setSVNServerNameIP(String.valueOf(params.get(REPOSITORY_ACCES_KEY_SERVER_NAME)));
          link.setUsername(String.valueOf(params.get(REPOSITORY_ACCES_KEY_USER)));
          link.setPassword(String.valueOf(params.get(REPOSITORY_ACCES_KEY_PASSWORD)));
        }
        
        details.setRepositoryLink(link);
      }
      List<Issue> issues = new ArrayList<>();
      Collection<RuntimeContextProblem> problems =  information.getProblems();
//      for (RuntimeContextProblem problem : problems) {
//        Issue issue = new Issue();
//        problem.getMessage();
//        problem.causeErrorStatus();
//        problem.causeValidationError();
//        issues.add(issue);
//      }
      details.setIssues(issues);
      return details;
    } catch (InsufficientRights ex) {
      throw ex;
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      throw new GetWorkspaceDetailsException(ex.getMessage(), ex);
    }
  }
  
  public List<? extends Workspace> getWorkspaces(XynaOrderServerExtension correlatedXynaOrder) throws InsufficientRights {
    try {
      checkRight(correlatedXynaOrder, UserManagement.ScopedRight.WORKSPACE, Action.list, null);
    } catch (PersistenceLayerException e) {
      
    }
    List<WorkspaceInformation> workspaceInformations = workspaceManagement.listWorkspaces(true);
    List<ApplicationDefinitionInformation> applicationDefinitionInformations = applicationManagement.listApplicationDefinitions(true);
    return workspaceInformations.stream().map(wi -> {
      Workspace w = new Workspace();
      w.setName(wi.getWorkspace().getName());
      w.setState(wi.getState() != null ? wi.getState().name() : "");
      w.setApplicationDefinitions(getWorkspaceApplicationDefinitions(wi.getWorkspace(), applicationDefinitionInformations));
      return w;
    }).collect(Collectors.toList());
  }
  
  private List<ApplicationDefinition> getWorkspaceApplicationDefinitions(com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace workspace, List<ApplicationDefinitionInformation> applicationDefinitionInformations){
    return applicationDefinitionInformations.stream()
        .filter(adi -> workspace.equals(adi.getParentWorkspace()))
        .map(adi -> {
          ApplicationDefinition ad = new ApplicationDefinition();
          ad.setName(adi.getName());
          ad.setSourceVersion(prepareSourceVersion(adi.getVersion()));
          ad.setWorkspaceName(workspace.getName());
          ad.setState(adi.getState() != null ? adi.getState().name() : "");
          return ad;
        })
        .sorted(Comparator.comparing(ApplicationDefinition::getName, String.CASE_INSENSITIVE_ORDER))
        .collect(Collectors.toList());
  }

  public void onDeployment() throws XynaException {
    // This is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
    cache = CredentialsCache.getInstance();
  }

  public void onUndeployment() throws XynaException {
    // This is executed again on each classloader-unload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.
    return null;
  }

  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    // Defines the behavior of the (un)deployment after reaching the timeout and if this service ignores a Thread.interrupt.
    // - BehaviorAfterOnUnDeploymentTimeout.EXCEPTION: Deployment will be aborted, while undeployment will log the exception and NOT abort.
    // - BehaviorAfterOnUnDeploymentTimeout.IGNORE: (Un)Deployment will be continued in another thread asynchronously.
    // - BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD: (Un)Deployment will be continued after calling Thread.stop on the thread.
    //   executing the (Un)Deployment.
    // If null is returned, the factory default <IGNORE> will be used.
    return null;
  }


  @Override
  public List<? extends Issue> getIssues(XynaOrderServerExtension correlatedXynaOrder, xmcp.factorymanager.rtcmanager.RuntimeContext rtc) throws GetIssuesException, InsufficientRights {
    
    try {
      if(rtc instanceof RuntimeApplication) {
        checkRight(correlatedXynaOrder, UserManagement.ScopedRight.APPLICATION, Action.list, rtc);
      } else if (rtc instanceof ApplicationDefinition) {
        checkRight(correlatedXynaOrder, UserManagement.ScopedRight.APPLICATION_DEFINITION, Action.list, rtc);
      } else if (rtc instanceof Workspace) {
        checkRight(correlatedXynaOrder, UserManagement.ScopedRight.WORKSPACE, Action.list, rtc);
      }
    } catch (PersistenceLayerException ex) {
      logger.error(ex.getMessage(), ex);
      throw new GetIssuesException(ex.getMessage(), ex);
    }
    
    List<Issue> result = new ArrayList<>();
    Collection<RuntimeContextProblem> issues = null;

    if (rtc instanceof ApplicationDefinition) {
      ApplicationDefinition ad = (ApplicationDefinition) rtc;
      Long parentRevision;
      try {
        parentRevision = revisionManagement.getRevision(null, null, ad.getWorkspaceName());
        //we need to query all application definitions to get their problems
        List<ApplicationDefinitionInformation> applicationDefinitionInformationObjects =
            applicationManagement.listApplicationDefinitions(parentRevision);
        ApplicationDefinitionInformation applicationDefinitionInformation = null;
        Optional<ApplicationDefinitionInformation> op =
            applicationDefinitionInformationObjects.stream().filter(x -> x.getName() != null && x.getName().equals(ad.getName())).findAny();
        if (op.isPresent()) {
          applicationDefinitionInformation = op.get();
        } else {
          throw new GetIssuesException("application definition not found.");
        }
        issues = applicationDefinitionInformation.getProblems();
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new GetIssuesException("application definition not found.");
      }
    } else if (rtc instanceof Workspace) {
      try {
        long revision = revisionManagement.getRevision(new com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace(rtc.getName()));
        WorkspaceInformation information = workspaceManagement.getWorkspaceDetails(revisionManagement.getWorkspace(revision), true);
        issues = information.getProblems();
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY | PersistenceLayerException e) {
        throw new GetIssuesException("workspace not found.");
      }
    } else if (rtc instanceof RuntimeApplication) {
      try {
        RuntimeApplication ra = (RuntimeApplication)rtc;
        if(ra.getIsLocal()) {
          Collection<ApplicationStorable> appInfos = applicationManagement.listApplicationStorables();
          Optional<ApplicationStorable> op = appInfos.stream()
            .filter(as -> as.getName().equals(ra.getName()) && as.getVersion().equals(ra.getVersion())).findAny();
          if(op.isPresent()) {
            ApplicationInformation appInfo = applicationManagement.getApplicationInformation(op.get(), true);
            issues = appInfo.getProblems();
          }
        }
      } catch (PersistenceLayerException | XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        logger.error(e.getMessage(), e);
      }
    } else {
      throw new GetIssuesException("Unsupported type.");
    }


    if (issues != null) {
      result = createIssueList(issues);
    }


    return result;
  }

  
  private List<Issue> createIssueList(Collection<RuntimeContextProblem> problems) {
    List<Issue> result = new LinkedList<Issue>();
    IssueEntry entry;
    List<IssueEntry> entries;
    for (RuntimeContextProblem problem : problems) {
      Issue issue = new Issue();
      issue.setIdentifier(problem.getId().getDescription());
      entries = new LinkedList<IssueEntry>();
      for (SerializablePair<String, String> kvp : problem.getDetails()) {
        entry = new IssueEntry();
        entry.setKey(kvp.getFirst());
        entry.setValue(kvp.getSecond());
        entries.add(entry);
      }
      issue.setEntries(entries);
      result.add(issue);
    }

    return result;
  }

  @Override
  public List<? extends RuntimeContextTableEntry> getRTCs(XynaOrderServerExtension correlatedXynaOrder, TableInfo tableInfo, xmcp.factorymanager.rtcmanager.FactoryNode factoryNode) throws GetRTCsException, InsufficientRights {
    try {
      checkRight(correlatedXynaOrder, UserManagement.ScopedRight.APPLICATION, Action.list, null);
      checkRight(correlatedXynaOrder, UserManagement.ScopedRight.APPLICATION_DEFINITION, Action.list, null);
      checkRight(correlatedXynaOrder, UserManagement.ScopedRight.WORKSPACE, Action.list, null);
    } catch (InsufficientRights ex) {
      throw ex;
    } catch (Exception ex) {
      throw new GetRTCsException(ex.getMessage(), ex);
    }

    Function<TableInfo, List<Filter>> nodeFilter = ti ->  Arrays.asList(new TableHelper.Filter(TABLE_KEY_FACTORY_NODE, factoryNode.getName(), true));

    List<RuntimeContextTableEntry> result = new ArrayList<>();
    TableHelper<RuntimeContextTableEntry, TableInfo> tableHelper = TableHelper.<RuntimeContextTableEntry, TableInfo>init(tableInfo)
        .limitConfig(TableInfo::getLimit)
        .sortConfig(ti -> {
          for (TableColumn tc : ti.getColumns()) {
            TableHelper.Sort sort = TableHelper.createSortIfValid(tc.getPath(), tc.getSort());
            if(sort != null)
              return sort;
          }
          return null;
        })
        .filterConfig(ti -> 
          ti.getColumns().stream()
          .filter(tableColumn -> 
            !tableColumn.getDisableFilter() && tableColumn.getPath() != null && tableColumn.getFilter() != null && tableColumn.getFilter().length() > 0
          )
          .map(tc -> new TableHelper.Filter(tc.getPath(), tc.getFilter()))
          .collect(Collectors.toList())
        )
        .secondaryFilterConfig(Arrays.asList(nodeFilter), LogicalOperand.AND)
        .addSelectFunction(TABLE_KEY_REQUIRED_RTC_NAME, d -> {
          if(d.getRuntimeContext() instanceof RuntimeApplication) {
            RuntimeApplication application = (RuntimeApplication) d.getRuntimeContext();
            return application.getName() + " " + application.getVersion();
          }
          return d.getRuntimeContext().getName();
        })
        .addSelectFunction(TABLE_KEY_REQUIRED_RTC_STATE, d -> d.getRuntimeContext().getState())
        .addSelectFunction(TABLE_KEY_REQUIRED_RTC_RTC_TYPE, d -> d.getRuntimeContext().getGUITypeName())
        .addSelectFunction(TABLE_KEY_FACTORY_NODE, d -> {
          if(d.getRuntimeContext() instanceof RuntimeApplication) {
            RuntimeApplication application = (RuntimeApplication) d.getRuntimeContext();
            return application.getFactoryNode();
          }
          return null;
        });

    // add workspaces
    List<WorkspaceInformation> workspaceInformations = workspaceManagement.listWorkspaces(true);
    result.addAll(workspaceInformations.stream().map(wi -> {
      RuntimeContextTableEntry tableEntry = new RuntimeContextTableEntry();
      Workspace workspace = new Workspace();
      workspace.setName(wi.getWorkspace().getName());
      workspace.setState(wi.getState() != null ? wi.getState().name() : "");
      tableEntry.setRuntimeContext(workspace);
      tableEntry.setRtcType(getRuntimeDependencyContextTypeGuiName(RuntimeDependencyContextType.Workspace));
      return tableEntry;
    }).collect(Collectors.toList()));

    // add application definitions
    List<ApplicationDefinitionInformation> adInformations = applicationManagement.listApplicationDefinitions(true);
    result.addAll(adInformations.stream().map(adi -> {
      RuntimeContextTableEntry tableEntry = new RuntimeContextTableEntry();
      ApplicationDefinition ad = new ApplicationDefinition();
      ad.setName(adi.getName());
      ad.setState(adi.getState().name());
      tableEntry.setRuntimeContext(ad);
      tableEntry.setRtcType(getRuntimeDependencyContextTypeGuiName(RuntimeDependencyContextType.ApplicationDefinition));
      return tableEntry;
    }).collect(Collectors.toList()));

    // add applications
    try {
      result.addAll(getRuntimeApplicationList(correlatedXynaOrder).stream().map(rta -> {
        RuntimeContextTableEntry tableEntry = new RuntimeContextTableEntry();
        tableEntry.setRuntimeContext(rta);
        tableEntry.setRtcType(getRuntimeDependencyContextTypeGuiName(RuntimeDependencyContextType.Application));
        return tableEntry;
      }).collect(Collectors.toList()));
    } catch (GetRuntimeApplicationsException e) {
      throw new GetRTCsException(e.getMessage(), e);
    }

    // sort, filter and limit
    tableHelper.sort(result);
    result = result.stream()
      .filter(tableHelper.filter())
      .collect(Collectors.toList());
    result = tableHelper.limit(result);

    return result;
  }

  private RuntimeContextManagementLinkProfile getRemoteRtCtxMgmt(String nodeName) throws ConnectException {
    com.gip.xyna.xfmg.xfctrl.nodemgmt.FactoryNode fn = nodeManagement.getNodeByName(nodeName);
    if (fn == null) {
      throw new ConnectException(nodeName, new Exception("node does not exists"));
    }

    RuntimeContextManagementLinkProfile remoteAccess = fn.getInterFactoryLink().getProfile(InterFactoryLink.InterFactoryLinkProfileIdentifier.RuntimeContextManagement);
    if (remoteAccess == null) {
      throw new ConnectException(nodeName, new Exception("remoteAccess does not exists"));
    }

    return remoteAccess;
  }

  private InfrastructureLinkProfile getInfrastructure(String nodeName) throws ConnectException {
    com.gip.xyna.xfmg.xfctrl.nodemgmt.FactoryNode fn = nodeManagement.getNodeByName(nodeName);
    if (fn == null) {
      throw new ConnectException(nodeName, new Exception("node does not exists"));
    }
    
    InfrastructureLinkProfile remoteAccess = fn.getInterFactoryLink().getProfile(InterFactoryLink.InterFactoryLinkProfileIdentifier.Infrastructure);
    if (remoteAccess == null) {
      throw new ConnectException(nodeName, new Exception("remoteAccess does not exists"));
    }
    
    return remoteAccess;
  }

  private XynaCredentials checkConnectivityAndAccess(String nodeName) throws XFMG_NodeConnectException, ConnectException  {
    XynaCredentials credentials = cache.getCredentials(nodeName, getInfrastructure(nodeName));
    InfrastructureLinkProfile ilp = getInfrastructure(nodeName);
    try {
      ilp.getExtendedStatus(credentials);
    } catch (XFMG_NodeConnectException e) {
      cache.clearSession(nodeName);
      credentials = cache.getCredentials(nodeName, getInfrastructure(nodeName));
      ilp.getExtendedStatus(credentials);
    }
    return credentials;
  }

  private boolean appendMigrationDescription(StringBuilder msgBuilder, MigrationResult result) {
    boolean wasSuccessful = true;
    if (result.activeOrdersFound()) {
      wasSuccessful = false;
      msgBuilder.append("Migration aborted.\nActive orders present, force the migration to proceed anyway.").append(Constants.LINE_SEPARATOR);
      if (result.getActiveOrderIds(ActiveOrderType.CRON).size() > 0) {
        msgBuilder.append(result.getActiveOrderIds(ActiveOrderType.CRON).size()).append(" affected Crons").append(Constants.LINE_SEPARATOR);
        msgBuilder.append(result.getActiveOrderIds(ActiveOrderType.CRON)).append(Constants.LINE_SEPARATOR);
      }
      if (result.getActiveOrderIds(ActiveOrderType.BATCH).size() > 0) {
        msgBuilder.append(result.getActiveOrderIds(ActiveOrderType.BATCH).size()).append(" affected BatchProcesses").append(Constants.LINE_SEPARATOR);
        msgBuilder.append(result.getActiveOrderIds(ActiveOrderType.BATCH)).append(Constants.LINE_SEPARATOR);
      }
      if (result.getActiveOrderIds(ActiveOrderType.ORDER).size() > 0) {
        msgBuilder.append(result.getActiveOrderIds(ActiveOrderType.ORDER).size()).append(" affected orders").append(Constants.LINE_SEPARATOR);
        msgBuilder.append(result.getActiveOrderIds(ActiveOrderType.ORDER)).append(Constants.LINE_SEPARATOR);
      }
    } else {
      msgBuilder.append("Migration finished").append(Constants.LINE_SEPARATOR);
      if (result.getAbortedOrderIds(ActiveOrderType.CRON).size() > 0) {
        msgBuilder.append(result.getAbortedOrderIds(ActiveOrderType.CRON).size()).append(" aborted Crons").append(Constants.LINE_SEPARATOR);
        msgBuilder.append(result.getAbortedOrderIds(ActiveOrderType.CRON)).append(Constants.LINE_SEPARATOR);
      }
      if (result.getAbortedOrderIds(ActiveOrderType.BATCH).size() > 0) {
        msgBuilder.append(result.getAbortedOrderIds(ActiveOrderType.BATCH).size()).append(" aborted BatchProccesses").append(Constants.LINE_SEPARATOR);
        msgBuilder.append(result.getAbortedOrderIds(ActiveOrderType.BATCH)).append(Constants.LINE_SEPARATOR);
      }
      if (result.getAbortedOrderIds(ActiveOrderType.ORDER).size() > 0) {
        msgBuilder.append(result.getAbortedOrderIds(ActiveOrderType.ORDER).size()).append(" aborted Orders").append(Constants.LINE_SEPARATOR);
        msgBuilder.append(result.getAbortedOrderIds(ActiveOrderType.ORDER)).append(Constants.LINE_SEPARATOR);
      }
      Pair<SuspendRevisionsBean, XPRC_ResumeFailedException> resumeInfo = result.getResumeInformation();
      if (resumeInfo.getFirst().getSuspendedRootOrderIds().size() > 0) {
        msgBuilder.append(resumeInfo.getFirst().getSuspendedRootOrderIds().size()).append(" RootOrders were suspended");
        if (resumeInfo.getSecond() == null) {
          msgBuilder.append(" and resumed.").append(Constants.LINE_SEPARATOR);
        } else {
          wasSuccessful = false;
          msgBuilder.append(".").append(Constants.LINE_SEPARATOR);
          msgBuilder.append(resumeInfo.getFirst().getSuspendedRootOrderIds()).append(Constants.LINE_SEPARATOR);
          msgBuilder.append("There has been an error during resume: ").append(resumeInfo.getSecond().getMessage());
        }
      }
    }

    return wasSuccessful;
  }

  private RTCMigrationResult convertResult(MigrationResult result, RTCMigration migration) {
    RTCMigrationResult rmr = new RTCMigrationResult();
    rmr.setRTCMigration(migration);

    StringBuilder messageBuilder = new StringBuilder();
    if (result.getMigrationAbortionReason() == null) {
      boolean wasSuccessful = appendMigrationDescription(messageBuilder, result);
      rmr.setWasSuccessful(wasSuccessful);
    } else {
      rmr.setWasSuccessful(false);
      switch (result.getMigrationAbortionReason()) {
        case UNACCESSIBLE_ORDERS :
          messageBuilder.append("Unaccesible orders prevented a successfull migration.");
          break;
        case UNSPECIFIED :
          messageBuilder.append("Migration failed.");
          break;
        case EXCEPTION :
          messageBuilder.append("Migration failed");
          if (result.getMigrationAbortionCause() == null) {
            messageBuilder.append(".");
          } else {
            messageBuilder.append(": ").append(result.getMigrationAbortionCause().getMessage());
          }
          break;
        default :
          break;
      }
    }

    String message = messageBuilder.toString();
    if (message != null) {
      rmr.setMessage(message.trim());
    }

    return rmr;
  }

  private RTCMigrationResult checkForPreviousResult(XynaOrderServerExtension correlatedOrder, RTCMigration migration) {
    SecureStorage secStore = XynaFactory.getInstance().getXynaMultiChannelPortal().getSecureStorage();
    Serializable result = secStore.retrieve("migrateRuntimeContextDependencies", String.valueOf(correlatedOrder.getId()));
    if (result != null) {
      if (result instanceof MigrationResult) {
        try {
          secStore.remove("migrateRuntimeContextDependencies", String.valueOf(correlatedOrder.getId()));
        } catch (PersistenceLayerException e) {
          // ntbd
        }
        return convertResult((MigrationResult)result, migration);
      } else if (result instanceof XynaException) {
        try {
          secStore.remove("migrateRuntimeContextDependencies", String.valueOf(correlatedOrder.getId()));
        } catch (PersistenceLayerException e) {
          // ntbd
        }
//        return failedOperation((XynaException)result, node); TODO!!!!!!!!!!!!!!!!!!!
        return null;
      } else {
        return null;
      }
    } else {
      return null;
    }
  }

  private boolean affectsCurrentRuntimeContext(xmcp.factorymanager.rtcmanager.RuntimeContext from) {
    ClassLoader cl = RtcManagerServicesServiceOperationImpl.class.getClassLoader();
    if (cl instanceof MDMClassLoader) {
      RuntimeContextDependencyManagement rcdMgmt = 
                      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
      RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      Long revision = ((MDMClassLoader)cl).getRevision();
      Set<Long> allDepsOfThis = new HashSet<Long>();
      rcdMgmt.getDependenciesRecursivly(revision, allDepsOfThis);
      allDepsOfThis.add(revision);
      try {
        return allDepsOfThis.contains(rm.getRevision(getRuntimeDependencyContextByGuiRuntimeContext(from).asCorrespondingRuntimeContext()));
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        return false;
      }
    } else {
      return false;
    }
  }

  public RTCMigrationResult migrateRTC(XynaOrderServerExtension correlatedXynaOrder, RTCMigration migration, AbortOrders abortProblemeticOrders) throws MigrateRTCException, InsufficientRights {
    // check rights
    try {
      checkModifyRTCRights(correlatedXynaOrder, migration.getSource());
      checkModifyRTCRights(correlatedXynaOrder, migration.getTarget());
    } catch (InsufficientRights e) {
      throw e;
    } catch (Exception e) {
      throw new MigrateRTCException(e.getMessage(), e);
    }

    try {
      com.gip.xyna.xprc.xprcods.orderarchive.orderbackuphelper.MigrateRuntimeContext.MigrationResult result;
      if (migration.getFactoryNode().getIsLocal()) {
        RTCMigrationResult ror = checkForPreviousResult(correlatedXynaOrder, migration);
        if (ror != null) {
          return ror;
        } else {
          boolean canNotRunSync = affectsCurrentRuntimeContext(migration.getSource());
          if (canNotRunSync) {
            Runnable detached = new Runnable() {
              
              public void run() {
                try {
                  MigrationResult detachedResult =
                      localLrcms.migrateRuntimeContextDependencies(correlatedXynaOrder.getCreationRole(),
                                                                   getRuntimeDependencyContextByGuiRuntimeContext(migration.getSource()),
                                                                   getRuntimeDependencyContextByGuiRuntimeContext(migration.getTarget()),
                                                                   Arrays.asList(MigrateRuntimeContext.MigrationTargets.values()),
                                                                   abortProblemeticOrders.getAbortOrders());
                  try {
                    XynaFactory.getInstance().getXynaMultiChannelPortal().getSecureStorage().store("migrateRuntimeContextDependencies", String.valueOf(correlatedXynaOrder.getId()), detachedResult);
                  } catch (PersistenceLayerException e1) {
                    logger.warn("Failed to store result of migrateRuntimeContextDependencies from detached thread", e1);
                  }
                } catch (XynaException e) {
                  try {
                    XynaFactory.getInstance().getXynaMultiChannelPortal().getSecureStorage().store("migrateRuntimeContextDependencies", String.valueOf(correlatedXynaOrder.getId()), e);
                  } catch (PersistenceLayerException e1) {
                    logger.warn("Failed to store result of migrateRuntimeContextDependencies from detached thread", e1);
                  }
                } finally {
                  SuspendResumeManagement srm = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getSuspendResumeManagement();
                  try {
                    srm.resumeOrder(new ResumeTarget(correlatedXynaOrder));
                  } catch (PersistenceLayerException e) {
                    logger.warn("Failed to resume migrateRuntimeContextDependencies main invocation from detached thread", e);
                  }
                }
              }
            };

            Thread detachedThread = new Thread(detached);
            detachedThread.start();
            throw new ProcessSuspendedException(new SuspensionCause_Manual());
          } else {
            result = localLrcms.migrateRuntimeContextDependencies(correlatedXynaOrder.getCreationRole(),
                                                                  getRuntimeDependencyContextByGuiRuntimeContext(migration.getSource()),
                                                                  getRuntimeDependencyContextByGuiRuntimeContext(migration.getTarget()),
                                                                  Arrays.asList(MigrateRuntimeContext.MigrationTargets.values()),
                                                                  abortProblemeticOrders.getAbortOrders());
          }
        }
      } else {
        RuntimeContextManagementLinkProfile remoteAccess = getRemoteRtCtxMgmt(migration.getFactoryNode().getName());
        result = remoteAccess.migrateRuntimeContextDependencies(checkConnectivityAndAccess(migration.getFactoryNode().getName()),
                                                                getRuntimeDependencyContextByGuiRuntimeContext(migration.getSource()),
                                                                getRuntimeDependencyContextByGuiRuntimeContext(migration.getTarget()),
                                                                Arrays.asList(MigrateRuntimeContext.MigrationTargets.values()),
                                                                abortProblemeticOrders.getAbortOrders());
      }

      return convertResult(result, migration);
    } catch (XynaException e) {
      throw new MigrateRTCException(e.getMessage(), e);
    }
  }

  @Override
  public void checkDependentRTCChange(XynaOrderServerExtension correlatedXynaOrder) throws SetDependentRTCsException {
    String user = sessionManagement.resolveSessionToUser(correlatedXynaOrder.getSessionId());
    if(setDepsExceptionsPerUser.contains(user)) {
      SetDependentRTCsException ex = setDepsExceptionsPerUser.get(user);
      setDepsExceptionsPerUser.remove(user);
      throw new SetDependentRTCsException(ex.getMessage(), ex);
    }
  }

  @Override
  public DeleteDuplicatesResponse deleteDuplicates(XynaOrderServerExtension correlatedXynaOrder, Workspace workspace) throws DeleteDuplicatesException, InsufficientRights {
    List<? extends Issue> issues;
    try {
      issues = getIssues(correlatedXynaOrder, workspace);
    } catch (GetIssuesException e) {
      throw new DeleteDuplicatesException("Could not determine duplicates.", e);
    }

    DeleteDuplicatesResponse response = new DeleteDuplicatesResponse(new ArrayList<>());

    for (Issue issue : issues) {
      if (Objects.equals(issue.getIdentifier(), RuntimeContextProblemType.COLLISION.getDescription())) {
        String fqn = null;
        XMOMType type = null;

        for (IssueEntry entry : issue.getEntries()) {
          if (Objects.equals(entry.getKey(), ISSUE_ENTRY_NAME_KEY)) {
            fqn = entry.getValue();
          } else if (Objects.equals(entry.getKey(), ISSUE_ENTRY_TYPE_KEY)) {
            type = getXMOMType(entry.getValue());
          }
        }

        if (fqn == null || type == null) {
          continue;
        }

        String user = sessionManagement.resolveSessionToUser(correlatedXynaOrder.getSessionId());
        try {
          long workspaceRev = revisionManagement.getRevision(null, null, workspace.getName());
          multiChannelPortal.deleteXMOMObject(type, fqn, false, false, user, correlatedXynaOrder.getSessionId(), workspaceRev);
        } catch (Exception e) {
          response.addToProblematicFQNs(fqn);
        }
      }
    }

    return response;
  }

  private XMOMType getXMOMType(String niceName) {
    if (Objects.equals(niceName, XMOMType.WORKFLOW.getNiceName())) {
      return XMOMType.WORKFLOW;
    } else if (Objects.equals(niceName, XMOMType.DATATYPE.getNiceName())) {
      return XMOMType.DATATYPE;
    } if (Objects.equals(niceName, XMOMType.EXCEPTION.getNiceName())) {
      return XMOMType.EXCEPTION;
    } else {
      return null;
    }
  }

  @Override
  public List<? extends WorkspaceElement> getWorkspaceContent(XynaOrderServerExtension correlatedXynaOrder, TableInfo tableInfo, GetWorkspaceContentRequest request) throws GetWorkspaceContentException, InsufficientRights {
    try {
      checkRight(correlatedXynaOrder, UserManagement.ScopedRight.WORKSPACE, Action.read, request.getWorkspace());
    } catch (PersistenceLayerException e) {
      throw new GetWorkspaceContentException("Unable to check whether user has the right to determine content of Workspace " + request.getWorkspace().getName(), e);
    }

    Long workspaceRev;
    try {
      workspaceRev = revisionManagement.getRevision(null, null, request.getWorkspace().getName());
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new GetWorkspaceContentException("Could not determine revision for Workspace " + request.getWorkspace().getName(), e);
    }

    // determine content of every AD in the workspace
    Map<String, List<String>> mapADtoFQNs;
    try {
      mapADtoFQNs = getContentOfADs(correlatedXynaOrder, workspaceRev);
    } catch (PersistenceLayerException | GetApplicationContentException e) {
      throw new GetWorkspaceContentException("Could not determine content of Application Definitions in Workspace " + request.getWorkspace().getName(), e);
    }

    // get content of workspace as application elements
    List<ApplicationElement> applicationElements;
    try {
      applicationElements = getApplicationElementsByRtc(revisionManagement.getRuntimeContext(workspaceRev), DependencyType.independent);
    } catch (Exception e) {
      throw new GetWorkspaceContentException("Could not determine content of Workspace " + request.getWorkspace().getName(), e);
    }

    // convert content to workspace elements
    final List<WorkspaceElement> workspaceElements = new ArrayList<>();
    applicationElements.stream()
      .filter(ae -> !Objects.equals(ae.getElementType(), ApplicationEntryType.CAPACITY.name()) && !Objects.equals(ae.getElementType(), ApplicationEntryType.XYNAPROPERTY.name()))
      .forEach(ae -> workspaceElements.add(new WorkspaceElement(ae.getElementType(), ae.getName(), getADsElementIsAssignedTo(ae.getName(), mapADtoFQNs))));

    // filter, sort and limit result

    TableHelper<WorkspaceElement, TableInfo> tableHelper = TableHelper.<WorkspaceElement, TableInfo>init(tableInfo)
        .limitConfig(TableInfo::getLimit)
        .sortConfig(ti -> {
          for (TableColumn tc : ti.getColumns()) {
            TableHelper.Sort sort = TableHelper.createSortIfValid(tc.getPath(), tc.getSort());
            if(sort != null)
              return sort;
          }
          return null;
        })
        .filterConfig(ti -> 
          ti.getColumns().stream()
          .filter(tableColumn -> 
            !tableColumn.getDisableFilter() && tableColumn.getPath() != null && tableColumn.getFilter() != null && tableColumn.getFilter().length() > 0
          )
          .map(tc -> new TableHelper.Filter(tc.getPath(), tc.getFilter()))
          .collect(Collectors.toList())
        )
        .addSelectFunction(TABLE_KEY_RTC_ELEMENT_CONTENT_TYPE, workspaceElement -> workspaceElement.getElementType() != null ? workspaceElement.getElementType() : "")
        .addSelectFunction(TABLE_KEY_RTC_ELEMENT_CONTENT_NAME, WorkspaceElement::getName)
        .addSelectFunction(TABLE_KEY_WORKSPACE_CONTENT_ADS, WorkspaceElement::getApplicationDefinitions);

    List<WorkspaceElement> result = workspaceElements.stream()
        .filter(tableHelper.filter())
        .filter(we -> request.getOnlyUnassigned() ? we.getApplicationDefinitions().length() == 0 : true)
        .collect(Collectors.toList());

    tableHelper.sort(result);

    return tableHelper.limit(result);
  }

  private Map<String, List<String>> getContentOfADs(XynaOrderServerExtension correlatedXynaOrder, Long workspaceRev) throws InsufficientRights, PersistenceLayerException, GetApplicationContentException {
    Map<String, List<String>> mapADtoFQNs = new HashMap<>();
    for (ApplicationDefinitionInformation adi : applicationManagement.listApplicationDefinitions(workspaceRev, true)) {
      TableInfo ti = new TableInfo(null, null, null, true, null, null, null, false);
      xmcp.factorymanager.rtcmanager.RuntimeContext ad = new xmcp.factorymanager.rtcmanager.ApplicationDefinition(adi.getState().name(), adi.getName(), prepareSourceVersion(adi.getVersion()), adi.getParentWorkspace().getName());
      checkRight(correlatedXynaOrder, UserManagement.ScopedRight.APPLICATION_DEFINITION, Action.list, ad);
      GetApplicationContentRequest gacr = new GetApplicationContentRequest(ad, true, false, false);
      List<? extends ApplicationElement> content = getApplicationContent(correlatedXynaOrder, ti, gacr);

      List<String> fqnsInAD = new ArrayList<>();
      mapADtoFQNs.put(ad.getName(), fqnsInAD);
      for (ApplicationElement ae : content) {
        fqnsInAD.add(ae.getName());
      }
    }

    return mapADtoFQNs;
  }

  private String getADsElementIsAssignedTo(String fqn, Map<String, List<String>> mapADtoFQNs) {
    String applicationDefinitions = "";
    for (String ad : mapADtoFQNs.keySet()) {
      if (mapADtoFQNs.get(ad).contains(fqn)) {
        applicationDefinitions = applicationDefinitions.length() == 0 ? ad : applicationDefinitions + ", " + ad;
      }
    }

    return applicationDefinitions;
  }

}
