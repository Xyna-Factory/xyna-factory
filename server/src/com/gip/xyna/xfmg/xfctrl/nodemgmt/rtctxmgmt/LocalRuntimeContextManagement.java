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
package com.gip.xyna.xfmg.xfctrl.nodemgmt.rtctxmgmt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.utils.collections.SerializablePair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.streams.StreamUtils;
import com.gip.xyna.xact.trigger.XynaActivationTrigger;
import com.gip.xyna.xact.trigger.FilterInformation.FilterInstanceInformation;
import com.gip.xyna.xact.trigger.FilterInstanceStorable.FilterInstanceState;
import com.gip.xyna.xact.trigger.TriggerInformation.TriggerInstanceInformation;
import com.gip.xyna.xact.trigger.TriggerInstanceStorable.TriggerInstanceState;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.EmptyRepositoryEvent;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.RepositoryAccessManagement;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.parameters.InstantiateRepositoryAccessParameters;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotImportApplication;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationDefinitionInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.ApplicationPartImportMode;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.ExportApplicationBuildParameter;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.ImportApplicationParameter;
import com.gip.xyna.xfmg.xfctrl.appmgmt.BuildApplicationVersionParameters;
import com.gip.xyna.xfmg.xfctrl.appmgmt.CopyApplicationIntoWorkspaceParameters;
import com.gip.xyna.xfmg.xfctrl.appmgmt.CustomOrderEntrance;
import com.gip.xyna.xfmg.xfctrl.appmgmt.OrderEntrance;
import com.gip.xyna.xfmg.xfctrl.appmgmt.OrderEntrance.OrderEntranceType;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RemoveApplicationParameters;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RevisionOrderControl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RevisionOrderControl.CustomOrderEntryInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.StartApplicationParameters;
import com.gip.xyna.xfmg.xfctrl.appmgmt.WorkspaceInformation;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement.TransientFile;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.filemgmt.RemoteFileManagementLanding;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.rtctxmgmt.LocalRuntimeContextManagementSecurity.MigrateRuntimeContextAccessContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.ApplicationDefinition;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext.RuntimeDependencyContextType;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.WorkspaceManagement;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.parameters.RemoveWorkspaceParameters;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBuilds;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xprcods.orderarchive.orderbackuphelper.MigrateRuntimeContext;
import com.gip.xyna.xprc.xprcods.orderarchive.orderbackuphelper.MigrateRuntimeContext.MigrationContext;
import com.gip.xyna.xprc.xprcods.orderarchive.orderbackuphelper.MigrateRuntimeContext.MigrationTargets;


public class LocalRuntimeContextManagement {

  private final static String PLACEHOLDER_USER_NAME = "RtCtxMgmt";
  
  public static final XynaPropertyBuilds<DestinationKey> CREATE_RUNTIME_CONTEXT_DESTINATION =
                  new XynaPropertyBuilds<DestinationKey>("xfmg.xfctrl.appmgmt.CreateRemoteRuntimeContexts.Destination",
                                                         new DestinationKey("xfmg.xfctrl.appmgmt.CreateRemoteRuntimeContexts", new Application("GlobalApplicationMgmt", "1.0.3")));
  public static final XynaPropertyBuilds<DestinationKey> DELETE_RUNTIME_CONTEXT_DESTINATION =
                  new XynaPropertyBuilds<DestinationKey>("xfmg.xfctrl.appmgmt.DeleteRemoteRuntimeContexts.Destination",
                                                         new DestinationKey("xfmg.xfctrl.appmgmt.DeleteRemoteRuntimeContexts", new Application("GlobalApplicationMgmt", "1.0.3")));
  public static final XynaPropertyBuilds<DestinationKey> LIST_RUNTIME_CONTEXTS_DESTINATION =
                  new XynaPropertyBuilds<DestinationKey>("xfmg.xfctrl.appmgmt.ListRuntimeDependencyContextDetails.Destination",
                                                         new DestinationKey("xfmg.xfctrl.appmgmt.ListRuntimeDependencyContextDetails", new Application("GlobalApplicationMgmt", "1.0.3")));
  public static final XynaPropertyBuilds<DestinationKey> MODIFY_RUNTIME_CONTEXT_DESTINATION =
                  new XynaPropertyBuilds<DestinationKey>("xfmg.xfctrl.appmgmt.ModifyRuntimeDependencyContext.Destination",
                                                         new DestinationKey("xfmg.xfctrl.appmgmt.ModifyRuntimeDependencyContext", new Application("GlobalApplicationMgmt", "1.0.3")));
  public static final XynaPropertyBuilds<DestinationKey> COPY_APPLICATION_INTO_WORKSPACE_DESTINATION =
                  new XynaPropertyBuilds<DestinationKey>("xfmg.xfctrl.appmgmt.CopyApplicationIntoWorkspace.Destination",
                                                         new DestinationKey("xfmg.xfctrl.appmgmt.CopyApplicationIntoWorkspace", new Application("GlobalApplicationMgmt", "1.0.3")));
  public static final XynaPropertyBuilds<DestinationKey> EXPORT_APPLICATION_DESTINATION =
                  new XynaPropertyBuilds<DestinationKey>("xfmg.xfctrl.appmgmt.ExportApplication.Destination",
                                                         new DestinationKey("xfmg.xfctrl.appmgmt.ExportApplication", new Application("GlobalApplicationMgmt", "1.0.3")));
  public static final XynaPropertyBuilds<DestinationKey> IMPORT_APPLICATION_DESTINATION =
                  new XynaPropertyBuilds<DestinationKey>("xfmg.xfctrl.appmgmt.ImportApplication.Destination",
                                                         new DestinationKey("xfmg.xfctrl.appmgmt.ImportApplication", new Application("GlobalApplicationMgmt", "1.0.3")));
  public static final XynaPropertyBuilds<DestinationKey> MIGRATE_RUNTIME_CONTEXT_DESTINATION =
                  new XynaPropertyBuilds<DestinationKey>("xfmg.xfctrl.appmgmt.MigrateRuntimeContextDependencies.Destination",
                                                         new DestinationKey("xfmg.xfctrl.appmgmt.MigrateRuntimeContextDependencies", new Application("GlobalApplicationMgmt", "1.0.3")));
  
  
  public List<RuntimeDependencyContextInformation> listRuntimeDependencyContexts(ListRuntimeDependencyContextParameter lrdcp) throws XynaException {
    List<RuntimeDependencyContextInformation> info = new ArrayList<RuntimeDependencyContextInformation>();
    if (lrdcp.isSelectRuntimeDependencyContextType(RuntimeDependencyContextType.Application) ||
        lrdcp.isSelectRuntimeDependencyContextType(RuntimeDependencyContextType.ApplicationDefinition)) {
      info.addAll(listApplicationInformation(lrdcp.isSelectProblems(), lrdcp.getSelectedTypes()));
    }
    if (lrdcp.isSelectRuntimeDependencyContextType(RuntimeDependencyContextType.Workspace)) {
      info.addAll(listWorkspaceInformation(lrdcp.isSelectProblems()));
    }
    enrichRuntimeDependencyContextInformation(info);
    return info;
  }

  private List<ApplicationInformation> listApplicationInformation(boolean includeProblems, Set<RuntimeDependencyContextType> selectedTypes) throws XynaException {
    List<ApplicationInformation> appInfo = getAppMgmt().listApplications(true, includeProblems);
    if (selectedTypes.contains(RuntimeDependencyContextType.Application) && 
        selectedTypes.contains(RuntimeDependencyContextType.ApplicationDefinition)) {
      return appInfo;
    } else {
      Iterator<ApplicationInformation> iter = appInfo.iterator();
      while (iter.hasNext()) {
        ApplicationInformation current = iter.next();
        if (!selectedTypes.contains(current.getRuntimeDependencyContextType())) {
          iter.remove();
        }
      }
      return appInfo;
    }
  }
  
  
  private List<WorkspaceInformation> listWorkspaceInformation(boolean includeProblems) throws XynaException {
    return getWsMgmt().listWorkspaces(includeProblems);
  }

  
  //TODO move into appMgmt?
  private <C extends RuntimeDependencyContextInformation> void enrichRuntimeDependencyContextInformation(List<C> infos) throws XynaException {
    for (C rdci : infos) {
      enrichRuntimeDependencyContextInformation(rdci);    
    }
  }
  
  private <C extends RuntimeDependencyContextInformation> void enrichRuntimeDependencyContextInformation(C info) throws XynaException {
    switch (info.getRuntimeDependencyContextType()) {
      case Application :
      case Workspace :
        Map<OrderEntrance, SerializablePair<Boolean, String>> orderEntrances = new HashMap<OrderEntrance, SerializablePair<Boolean, String>>();
        
        long revision = getRevMgmt().getRevision(info.asRuntimeContext());
        Boolean rmiClosed = RevisionOrderControl.getRmiCliClosed(revision, com.gip.xyna.xfmg.xfctrl.appmgmt.OrderEntrance.OrderEntranceType.RMI);
        orderEntrances.put(new OrderEntrance(OrderEntranceType.RMI, OrderEntranceType.RMI.toString()),
                           SerializablePair.<Boolean, String>of((rmiClosed == null || !rmiClosed), null));
        Boolean cliClosed = RevisionOrderControl.getRmiCliClosed(revision, com.gip.xyna.xfmg.xfctrl.appmgmt.OrderEntrance.OrderEntranceType.CLI);
        orderEntrances.put(new OrderEntrance(OrderEntranceType.CLI, OrderEntranceType.CLI.toString()),
                           SerializablePair.<Boolean, String>of((cliClosed == null || !cliClosed), null));
        XynaActivationTrigger xat = XynaFactory.getInstance().getActivation().getActivationTrigger();
        for (TriggerInstanceInformation tii : xat.getTriggerInstanceInformation(revision)) {
          orderEntrances.put(new OrderEntrance(OrderEntranceType.triggerInstance, tii.getTriggerInstanceName()),
                             SerializablePair.<Boolean, String>of((tii.getState() == TriggerInstanceState.ENABLED), (tii.getState() == TriggerInstanceState.ERROR) ? tii.getErrorCause() : null));
        }
        for (FilterInstanceInformation fii : xat.getFilterInstanceInformations(revision)) {
          orderEntrances.put(new OrderEntrance(OrderEntranceType.filterInstance, fii.getFilterInstanceName()),
                             SerializablePair.<Boolean, String>of((fii.getState() == FilterInstanceState.ENABLED), (fii.getState() == FilterInstanceState.ERROR) ? fii.getErrorCause() : null));
        }
        if (info.getRuntimeDependencyContextType() == RuntimeDependencyContextType.Application) {
          ApplicationInformation app = (ApplicationInformation) info;
          Long appRevision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
              .getRevision(app.getName(), app.getVersion(), null);
          for (CustomOrderEntryInformation coe : RevisionOrderControl.getAllCustomOrderEntryTypes()) {
            Boolean customClosed = RevisionOrderControl.checkCustomOrderEntryClosed(appRevision, coe.getDefiningRevision(), coe.getName());
            orderEntrances.put(new CustomOrderEntrance(coe.getName(), coe.getDefiningRevision()),
                               SerializablePair.<Boolean, String> of(customClosed, null));
          }
        }
        if (info.getRuntimeDependencyContextType() == RuntimeDependencyContextType.Application) {
          ((ApplicationInformation)info).setOrderEntrances(orderEntrances);
        } else {
          ((WorkspaceInformation)info).setOrderEntrances(orderEntrances);
        }
        break;
      case ApplicationDefinition :
        // ntbd
        break;
      default :
        break;
    }
  }
  

  public void createRuntimeDependencyContext(RuntimeDependencyContext rdc) throws XynaException {
    switch (rdc.getRuntimeDependencyContextType()) {
      case Application :
        Application newApp = (Application) rdc;
        BuildApplicationVersionParameters params = new BuildApplicationVersionParameters();
        Workspace parentWorkspace = findParentWorkspace(newApp);
        if (parentWorkspace == null) {
          throw new IllegalArgumentException("Failed to identfy app: " + newApp);
        }
        params.setParentWorkspace(parentWorkspace); 
        getAppMgmt().buildApplicationVersion(newApp.getName(), newApp.getVersionName(), params);
        break;
      case ApplicationDefinition :
        ApplicationDefinition newAppDef = (ApplicationDefinition) rdc;
        getAppMgmt().defineApplication(newAppDef.getName(), "", getRevMgmt().getRevision(newAppDef.getParentWorkspace()));
        break;
      case Workspace :
        Workspace newWs = (Workspace) rdc;
        getWsMgmt().createWorkspace(newWs);
        break;
      default :
        throw new IllegalArgumentException("No valid RuntimeDependencyContext: " + rdc);
    }
  }
  
  public void deleteRuntimeDependencyContext(RuntimeDependencyContext rdc) throws XynaException {
    switch (rdc.getRuntimeDependencyContextType()) {
      case Application :
        Application newApp = (Application) rdc;
        getAppMgmt().removeApplicationVersion(newApp.getName(), newApp.getVersionName());
        break;
      case ApplicationDefinition :
        ApplicationDefinition newAppDef = (ApplicationDefinition) rdc;
        RemoveApplicationParameters appDefParams = new RemoveApplicationParameters();
        appDefParams.setParentWorkspace(newAppDef.getParentWorkspace());
        getAppMgmt().removeApplicationVersion(newAppDef.getName(), null, appDefParams, new EmptyRepositoryEvent());
        break;
      case Workspace :
        WorkspaceInformation newWs = (WorkspaceInformation) rdc;
        RemoveWorkspaceParameters wsParams = new RemoveWorkspaceParameters();
        wsParams.setForce(true);
        wsParams.setCleanupXmls(true);
        // params.setUser(user); // transport user name to here?
        getWsMgmt().removeWorkspace(newWs.getWorkspace(), wsParams);
        break;
      default :
        throw new IllegalArgumentException("No valid RuntimeDependencyContext: " + rdc);
    }
  }
  
  public void modifyRuntimeDependencyContext(RuntimeDependencyContextInformation rdci) throws XynaException {
    switch (rdci.getRuntimeDependencyContextType()) {
      case Application :
        modifyApplication((ApplicationInformation) rdci);
        break;
      case ApplicationDefinition :
        modifyApplicationDefinition((ApplicationDefinitionInformation) rdci);
        break;
      case Workspace :
        modifyWorkspace((WorkspaceInformation) rdci);
        break;
      default :
        throw new IllegalArgumentException("No valid RuntimeDependencyContext: " + rdci);
    }
  }
  
  private void modifyApplication(ApplicationInformation changes) throws XynaException {
    ApplicationInformation current = getAppMgmt().getApplicationInformation(changes.getName(), changes.getVersion());
    enrichRuntimeDependencyContextInformation(current); // move up in call stack and create getRuntimeDependencyContextInformation
    //modifyApplicationState(changes, current);
    modifyOrderEntrances(changes, current);
    
    getRtCtxDepMgmt().modifyDependencies(new Application(changes.getName(), changes.getVersion()), changes.getRequirements(), PLACEHOLDER_USER_NAME, true, true);
  }
  
  /*private void modifyApplicationState(ApplicationInformation changes, ApplicationInformation current) throws XynaException {
    // we'r not selecting problems, state will not be diluted 
    switch (current.getState()) {
      case RUNNING :
        // even if state was diluted the stop-operation would have overridden it
        switch (changes.getState()) {
          case STOPPED :
            getAppMgmt().stopApplication(changes.getName(), changes.getVersion(), false);
            break;
          default :
            break;
        }
        break;
      case STOPPED :
        switch (changes.getState()) {
          case RUNNING :
            getAppMgmt().startApplication(changes.getName(), changes.getVersion(), false, false);
            break;
          default :
            break;
        }
      default :
        break;
    }
  }*/
  
  private void modifyOrderEntrances(ApplicationInformation changes, ApplicationInformation current) throws XynaException {
    EnumSet<OrderEntranceType> orderEntrancesToClose = EnumSet.noneOf(OrderEntranceType.class);
    EnumSet<OrderEntranceType> orderEntrancesToOpen = EnumSet.noneOf(OrderEntranceType.class);
    
    // enable filter instances before trigger instances
    List<OrderEntranceType> orderedOrderEntrances = 
                    Arrays.asList(OrderEntranceType.CLI, OrderEntranceType.RMI, OrderEntranceType.filterInstance, OrderEntranceType.triggerInstance, OrderEntranceType.custom);
    for (OrderEntranceType type : orderedOrderEntrances) {
      for (OrderEntrance orderEntrance : changes.getOrderEntrances().keySet()) {
        if (type.equals(orderEntrance.getType())) {
          if (current.getOrderEntrances().get(orderEntrance).getFirst() && !changes.getOrderEntrances().get(orderEntrance).getFirst()) {
            switch (orderEntrance.getType()) {
              case CLI :
              case RMI :
                orderEntrancesToClose.add(orderEntrance.getType());
                break;
              case triggerInstance :
                modifyTriggerInstances(changes.getName(), changes.getVersion(), orderEntrance, false);
                break;
              case filterInstance :
                modifyFilterInstances(changes.getName(), changes.getVersion(), orderEntrance, false);
                break;
              case custom:
                modifyCustomOrderEntry(changes.getName(), changes.getVersion(), orderEntrance, false);
                break;
              default :
                throw new IllegalArgumentException("OrderEntranceType uknown: " + orderEntrance.getType()); 
            }
          } else if (!current.getOrderEntrances().get(orderEntrance).getFirst() && changes.getOrderEntrances().get(orderEntrance).getFirst()) {
            switch (orderEntrance.getType()) {
              case CLI :
              case RMI :
                orderEntrancesToOpen.add(orderEntrance.getType());
                break;
              case triggerInstance :
                modifyTriggerInstances(changes.getName(), changes.getVersion(), orderEntrance, true);
                break;
              case filterInstance :
                modifyFilterInstances(changes.getName(), changes.getVersion(), orderEntrance, true);
                break;
              case custom:
                modifyCustomOrderEntry(changes.getName(), changes.getVersion(), orderEntrance, true);
                break;
              default :
                throw new IllegalArgumentException("OrderEntranceType uknown: " + orderEntrance.getType()); 
            }
          }
        }
      }
    }
    
    
    if (orderEntrancesToOpen.size() > 0) {
      StartApplicationParameters startParams = new StartApplicationParameters();
      startParams.setOnlyEnableOrderEntrance(orderEntrancesToOpen);
      // we are currently holding a read lock, appMgmt will try to get a write lock, we need to release
      Application asApp = new Application(changes.getName(), changes.getVersion());
      CommandControl.unlock(LocalRuntimeContextManagementSecurity.getOperationLockForModify(RuntimeDependencyContextType.Application), asApp);
      try {
        getAppMgmt().startApplication(changes.getName(), changes.getVersion(), startParams);
      } finally {
        CommandControl.tryLock(LocalRuntimeContextManagementSecurity.getOperationLockForModify(RuntimeDependencyContextType.Application), asApp);
      }
    }
    
    if (orderEntrancesToClose.size() > 0) {
      // we are currently holding a read lock, appMgmt will try to get a write lock, we need to release
      Application asApp = new Application(changes.getName(), changes.getVersion());
      CommandControl.unlock(LocalRuntimeContextManagementSecurity.getOperationLockForModify(RuntimeDependencyContextType.Application), asApp);
      try {
        getAppMgmt().stopApplication(changes.getName(), changes.getVersion(), false, Optional.of(orderEntrancesToClose));
      } finally {
        CommandControl.tryLock(LocalRuntimeContextManagementSecurity.getOperationLockForModify(RuntimeDependencyContextType.Application), asApp);
      }
    }
  }


  private void modifyCustomOrderEntry(String name, String version, OrderEntrance orderEntrance, boolean enable) {
    if (!(orderEntrance instanceof CustomOrderEntrance)) {
      throw new IllegalArgumentException();
    }

    CustomOrderEntrance coe = (CustomOrderEntrance) orderEntrance;
    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Long revisionOfApplication = -1L;

    try {
      revisionOfApplication = rm.getRevision(name, version, null);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      return;
    }

    if (enable) {
      RevisionOrderControl.enableCustomOrderEntry(revisionOfApplication, coe.getDefiningRevision(), coe.getName());
    } else {
      RevisionOrderControl.disableCustomOrderEntry(revisionOfApplication, coe.getDefiningRevision(), coe.getName());
    }
  }


  private void modifyTriggerInstances(String name, String version, OrderEntrance orderEntrance, boolean enable) throws XynaException {
    long revision = getRevMgmt().getRevision(name, version, null);

    XynaActivationTrigger xat = XynaFactory.getInstance().getActivation().getActivationTrigger();
    CommandControl.Operation operation = enable ? CommandControl.Operation.TRIGGER_INSTANCE_ENABLE : CommandControl.Operation.TRIGGER_INSTANCE_DISABLE;
    CommandControl.tryLock(operation, revision);
    try {
      if (enable) {
        xat.enableTriggerInstance(orderEntrance.getName(), revision, true, -1, false);
      } else {
        xat.disableTriggerInstance(orderEntrance.getName(), revision, false, false);
      }
      
    } finally {
      CommandControl.unlock(operation, revision);
    }
  }
  
  private void modifyFilterInstances(String name, String version, OrderEntrance orderEntrance, boolean enable) throws XynaException {
    long revision = getRevMgmt().getRevision(name, version, null);

    XynaActivationTrigger xat = XynaFactory.getInstance().getActivation().getActivationTrigger();
    CommandControl.Operation operation = enable ? CommandControl.Operation.FILTER_INSTANCE_ENABLE : CommandControl.Operation.FILTER_INSTANCE_DISABLE;
    CommandControl.tryLock(operation, revision);
    try {
      if (enable) {
        xat.enableFilterInstance(orderEntrance.getName(), revision);
      } else {
        xat.disableFilterInstance(orderEntrance.getName(), revision);
      }
      
    } finally {
      CommandControl.unlock(operation, revision);
    }
  }

  private void modifyApplicationDefinition(ApplicationDefinitionInformation changes) throws XynaException {
    ApplicationDefinitionInformation current = (ApplicationDefinitionInformation) getAppMgmt().getApplicationDefinitionInformation(changes.getName(), getRevMgmt().getRevision(changes.getParentWorkspace()));
    enrichRuntimeDependencyContextInformation(current); // move up in call stack and create getRuntimeDependencyContextInformation
    
    getRtCtxDepMgmt().modifyDependencies(new ApplicationDefinition(changes.getName(), changes.getParentWorkspace()), changes.getRequirements(), PLACEHOLDER_USER_NAME, true, true);
    
    //modifyApplicationEntries(changes, current);
  }
  
  /*private void modifyApplicationEntries(ApplicationDefinitionInformation changes, ApplicationDefinitionInformation current) throws XynaException {
    Map<String, ApplicationEntryStorable> changedFqEntries = new HashMap<String, ApplicationEntryStorable>();
    for (ApplicationEntryStorable entry : changes.getApplicationEntries()) {
      changedFqEntries.put(entry.getName(), entry);
    }
    Map<String, ApplicationEntryStorable> currentFqEntries = new HashMap<String, ApplicationEntryStorable>();
    for (ApplicationEntryStorable entry : current.getApplicationEntries()) {
      currentFqEntries.put(entry.getName(), entry);
    }
    
    Set<String> newEntries = new HashSet<String>(changedFqEntries.keySet());
    newEntries.removeAll(currentFqEntries.keySet());
    
    for (String newEntry : newEntries) {
      ApplicationEntryStorable entry = changedFqEntries.get(newEntry);
      getAppMgmt().addObjectToApplicationDefinition(entry.getName(), entry.getTypeAsEnum(), changes.getName(), getRevMgmt().getRevision(changes.getParentWorkspace()), false, null, new EmptyRepositoryEvent());
    }
    
    Set<String> deprecatedEntries = new HashSet<String>(currentFqEntries.keySet());
    deprecatedEntries.removeAll(changedFqEntries.keySet());
    
    for (String newEntry : deprecatedEntries) {
      ApplicationEntryStorable entry = changedFqEntries.get(newEntry);
      getAppMgmt().removeObjectFromApplication(changes.getName(), entry.getName(), entry.getTypeAsEnum(), getRevMgmt().getRevision(changes.getParentWorkspace()), new EmptyRepositoryEvent(), false, null);
    }
  }*/

  private void modifyWorkspace(WorkspaceInformation changes) throws XynaException {
    WorkspaceInformation current = getWsMgmt().getWorkspaceDetails(changes.getWorkspace(), false);
    enrichRuntimeDependencyContextInformation(current); // move up in call stack and create getRuntimeDependencyContextInformation
    
    getRtCtxDepMgmt().modifyDependencies(changes.getWorkspace(), changes.getRequirements(), PLACEHOLDER_USER_NAME, true, true);
    
    if (changes.getRepositoryAccess() == null && current.getRepositoryAccess() != null) {
      String repositoryAccessInstanceName = changes.getWorkspace().getName().replaceAll("[^a-zA-Z0-9_]", "_");
      getRepAccMgmt().removeRepositoryAccessInstance(repositoryAccessInstanceName);
    } else if (changes.getRepositoryAccess() != null && current.getRepositoryAccess() == null) {
      String repositoryAccessInstanceName = changes.getWorkspace().getName().replaceAll("[^a-zA-Z0-9_]", "_");
      InstantiateRepositoryAccessParameters parameters = new InstantiateRepositoryAccessParameters();
      parameters.setRepositoryAccessInstanceName(repositoryAccessInstanceName);
      parameters.setRepositoryAccessName(changes.getRepositoryAccess().getName());
      parameters.setParameterMap(convertParameterMap(changes.getRepositoryAccess().getParamMap()));
      parameters.setCodeAccessName(changes.getWorkspace().getName().replaceAll("[^a-zA-Z0-9_]", "_"));
      parameters.setXmomAccessName(changes.getWorkspace().getName().replaceAll("[^a-zA-Z0-9_]", "_"));
      getRepAccMgmt().instantiateRepositoryAccessInstance(parameters, getRevMgmt().getRevision(changes.getWorkspace()));
    }
  }

  public MigrateRuntimeContext.MigrationResult migrateRuntimeContextDependencies(RuntimeDependencyContext from, RuntimeDependencyContext to, Collection<MigrationTargets> targets, boolean force) throws XynaException {
    return migrateRuntimeContextDependencies(from, to, targets, force, new LocalRuntimeContextManagementSecurity.AllAccess());
  }

  public MigrateRuntimeContext.MigrationResult migrateRuntimeContextDependencies(RuntimeDependencyContext from, RuntimeDependencyContext to, Collection<MigrationTargets> targets, boolean force, MigrateRuntimeContextAccessContext accessCtx) throws XynaException {
    MigrationContext mc = MigrateRuntimeContext.migrateRuntimeContext(from, to, targets, force, accessCtx);
    return MigrateRuntimeContext.MigrationResult.of(mc);
  }
 
  public void importApplication(ImportApplicationParameter iap, String fileId) throws XynaException {
    try {
      File tmpFile = File.createTempFile("import_", "app");
      try {
        FileManagement fm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getFileManagement();
        TransientFile tFile = fm.retrieve(fileId);
        InputStream is = tFile.openInputStream();
        try {
          FileOutputStream fos = new FileOutputStream(tmpFile);
          try {
            StreamUtils.copy(is, fos);
          } finally {
            fos.close();
          }
        } finally {
          is.close();
        }
        boolean force = iap.getOverride() != null && iap.getOverride();
        boolean stopIfRunning = iap.isStopIfExistingAndRunning();
        boolean excludeXynaProperties = iap.getXynaProperties() == ApplicationPartImportMode.EXCLUDE;
        boolean excludeCapacities = iap.getCapacities() == ApplicationPartImportMode.EXCLUDE;
        boolean importOnlyXynaProperties = iap.getXynaProperties() == ApplicationPartImportMode.ONLY;
        boolean importOnlyCapacities = iap.getCapacities() == ApplicationPartImportMode.ONLY;
        boolean clusterwide = !iap.getLocal();
        boolean regenerateCode = false;
        boolean verbose = false;
        getAppMgmt().importApplication(tmpFile.getAbsolutePath(), force, stopIfRunning, excludeXynaProperties, excludeCapacities, 
                                       importOnlyXynaProperties, importOnlyCapacities, clusterwide, regenerateCode, verbose, iap.getUser(), null);
      } finally {
        tmpFile.delete();
      }
      
    } catch (Exception e) {
      throw new XFMG_CouldNotImportApplication("", e);
    }
  }
  
  public String exportApplication(Application application, ExportApplicationBuildParameter eabp) throws XynaException {
    try {
      File tmpFile = File.createTempFile(application.getName() + "." + application.getVersionName(), "app");
      getAppMgmt().exportApplication(application.getName(), application.getVersionName(), tmpFile.getAbsolutePath(), eabp.getLocalBuild(), "", true, false, false, (PrintStream)null, eabp.getUser());
      FileManagement fm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getFileManagement();
      FileInputStream is = new FileInputStream(tmpFile);
      try {
        return fm.store(RemoteFileManagementLanding.REMOTE_FILE_UPLOAD_LOCATION, application.getName() + "." + application.getVersionName() + ".app", is);
      } finally {
        is.close();
        tmpFile.delete();
      }
    } catch (IOException e) {
      throw new XFMG_CouldNotImportApplication("", e);
    }
  }
  
  public void copyApplicationIntoWorkspace(Application from, Workspace to) throws XynaException {
    CopyApplicationIntoWorkspaceParameters caowp = new CopyApplicationIntoWorkspaceParameters();
    caowp.setOverrideChanges(true);
    caowp.setTargetWorkspace(to);
    getAppMgmt().copyApplicationIntoWorkspace(from.getName(), from.getVersionName(), caowp);
  }
  
  
  private ApplicationManagementImpl getAppMgmt() {
    return (ApplicationManagementImpl)XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement();
  }
  
  private WorkspaceManagement getWsMgmt() {
    return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getWorkspaceManagement();
  }
  
  private RevisionManagement getRevMgmt() {
    return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
  }
  
  private RuntimeContextDependencyManagement getRtCtxDepMgmt() {
    return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
  }
  
  private RepositoryAccessManagement getRepAccMgmt() {
    return XynaFactory.getInstance().getXynaDevelopment().getXynaLibraryDevelopment().getRepositoryAccessManagement();
  }
  
  private Map<String, String> convertParameterMap(Map<String, Object> paramMap) {
    Map<String, String> stringMap = new HashMap<String, String>();
    for (Entry<String, Object> entry : paramMap.entrySet()) {
      stringMap.put(entry.getKey(), entry.getValue().toString());
    }
    return stringMap;
  }
  
  private Workspace findParentWorkspace(Application app) throws PersistenceLayerException {
    ApplicationManagementImpl appMgmt = getAppMgmt();
    Collection<WorkspaceInformation> workspaces = getWsMgmt().listWorkspaces(false);
    for (WorkspaceInformation wsInfo : workspaces) {
      try {
        appMgmt.getApplicationDefinitionInformation(app.getName(), getRevMgmt().getRevision(wsInfo.getWorkspace()));
        return wsInfo.getWorkspace();
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        // not here...
      }
    }
    return null;
  }
  
  
}
