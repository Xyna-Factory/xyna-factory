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
package com.gip.xyna.xfmg.xfctrl.nodemgmt.rtctxmgmt;

import java.util.Collection;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.exceptions.XFMG_ACCESS_VIOLATION;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationDefinitionInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.ExportApplicationBuildParameter;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.ImportApplicationParameter;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.ApplicationDefinition;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext.RuntimeDependencyContextType;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Action;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Rights;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xprcods.orderarchive.orderbackuphelper.MigrateRuntimeContext;
import com.gip.xyna.xprc.xprcods.orderarchive.orderbackuphelper.MigrateRuntimeContext.MigrationTargets;

public class LocalRuntimeContextManagementSecurity {

  
  private final static LocalRuntimeContextManagement lrcm = new LocalRuntimeContextManagement();
  
  public List<RuntimeDependencyContextInformation> listRuntimeDependencyContexts(Role role, ListRuntimeDependencyContextParameter lrdcp) throws XynaException {
    // TODO only abort if one of those is contained and/or part of the requested subset
    if (lrdcp.getSelectedTypes() == null ||
        lrdcp.getSelectedTypes().size() <= 0) {
      checkApplicationRight(role, null, null, Action.list);
      checkWorkspaceRight(role, null, Action.list);
    } else {
      if (lrdcp.isSelectRuntimeDependencyContextType(RuntimeDependencyContextType.Application)) {
        checkApplicationRight(role, null, null, Action.list);  
      }
      if (lrdcp.isSelectRuntimeDependencyContextType(RuntimeDependencyContextType.Workspace)) {
        checkWorkspaceRight(role, null, Action.list);
      }  
    }
    // checkApplicationDefinitionRight(role, null, null, Action.list); // there is no list-Action for AppDefs
    return lrcm.listRuntimeDependencyContexts(lrdcp);
  }

  
  
  public void createRuntimeDependencyContext(Role role, RuntimeDependencyContext rdc) throws XynaException {
    CommandControl.Operation operation;
    switch (rdc.getRuntimeDependencyContextType()) {
      case Application :
        checkStaticRight(role, UserManagement.Rights.WORKINGSET_MANAGEMENT);
        operation = CommandControl.Operation.APPLICATION_BUILD;
        break;
      case ApplicationDefinition :
        ApplicationDefinition newAppDef = (ApplicationDefinition) rdc;
        checkApplicationDefinitionRight(role, newAppDef.getName(), newAppDef.getParentWorkspace().getName(), Action.insert);
        operation = CommandControl.Operation.APPLICATION_DEFINE;
        break;
      case Workspace :
        checkStaticRight(role, UserManagement.Rights.WORKINGSET_MANAGEMENT);
        operation = CommandControl.Operation.WORKSPACE_CREATE;
        break;
      default :
        throw new IllegalArgumentException("No valid RuntimeDependencyContext: " + rdc);
    }
    CommandControl.tryLock(operation, rdc.asCorrespondingRuntimeContext());
    try {
      lrcm.createRuntimeDependencyContext(rdc);
    } finally {
      CommandControl.unlock(operation, rdc.asCorrespondingRuntimeContext());
    }
  }
  
  
  public void deleteRuntimeDependencyContext(Role role, RuntimeDependencyContext rdc) throws XynaException {
    CommandControl.Operation operation;
    switch (rdc.getRuntimeDependencyContextType()) {
      case Application :
        Application app = (Application) rdc;
        checkStaticRight(role, UserManagement.Rights.APPLICATION_ADMINISTRATION);
        checkApplicationRight(role, app.getName(), app.getVersionName(), Action.remove);
        operation = CommandControl.Operation.APPLICATION_REMOVE;
        break;
      case ApplicationDefinition :
        ApplicationDefinition newAppDef = (ApplicationDefinition) rdc;
        checkApplicationDefinitionRight(role, newAppDef.getName(), newAppDef.getParentWorkspace().getName(), Action.delete);
        operation = CommandControl.Operation.APPLICATION_REMOVE_DEFINITION;
        break;
      case Workspace :
        checkStaticRight(role, UserManagement.Rights.WORKINGSET_MANAGEMENT);
        operation = CommandControl.Operation.WORKSPACE_REMOVE;
        break;
      default :
        throw new IllegalArgumentException("No valid RuntimeDependencyContext: " + rdc);
    }
    CommandControl.tryLock(operation, rdc.asCorrespondingRuntimeContext());
    //kurz danach wird writelock geholt, das kann nicht upgegraded werden
    CommandControl.unlock(operation, rdc.asCorrespondingRuntimeContext());
    //CommandControl.tryLock(operation, rdc.asCorrespondingRuntimeContext());
    long revision = getRevMgmt().getRevision(rdc.asCorrespondingRuntimeContext());
    CommandControl.wlock(operation, CommandControl.Operation.allExcept(operation), revision);
    try {
      lrcm.deleteRuntimeDependencyContext(rdc);
    } finally {
      //CommandControl.unlock(operation, rdc.asCorrespondingRuntimeContext());
      CommandControl.wunlock(CommandControl.Operation.allExcept(operation), revision);
    }
  }
  
  public void modifyRuntimeDependencyContext(Role role, RuntimeDependencyContextInformation rdci) throws XynaException {
    switch (rdci.getRuntimeDependencyContextType()) {
      case Application :
        ApplicationInformation app = (ApplicationInformation) rdci;
        checkStaticRight(role, UserManagement.Rights.APPLICATION_MANAGEMENT);
        checkApplicationRight(role, app.getName(), app.getVersion(), Action.write); // TODO start / stop ?
        break;
      case ApplicationDefinition :
        ApplicationDefinitionInformation newAppDef = (ApplicationDefinitionInformation) rdci;
        checkApplicationDefinitionRight(role, newAppDef.getName(), newAppDef.getParentWorkspace().getName(), Action.write);
        break;
      case Workspace :
        checkStaticRight(role, UserManagement.Rights.WORKINGSET_MANAGEMENT);
        break;
      default :
        throw new IllegalArgumentException("No valid RuntimeDependencyContext: " + rdci);
    }
    CommandControl.tryLock(getOperationLockForModify(rdci.getRuntimeDependencyContextType()), rdci.asRuntimeContext());
    try {
      lrcm.modifyRuntimeDependencyContext(rdci);
    } finally {
      CommandControl.unlock(getOperationLockForModify(rdci.getRuntimeDependencyContextType()), rdci.asRuntimeContext());
    }
  }
  


  public MigrateRuntimeContext.MigrationResult migrateRuntimeContextDependencies(Role role, RuntimeDependencyContext from, RuntimeDependencyContext to, Collection<MigrationTargets> targets, boolean force) throws XynaException {
    MigrateRuntimeContextAccessContext accessCtx = getAccessContext(role);
    return lrcm.migrateRuntimeContextDependencies(from, to, targets, force, accessCtx);
  }
 
  public void importApplication(Role role, ImportApplicationParameter iap, String fileId) throws XynaException {
    lrcm.importApplication(iap, fileId);
  }
  
  public String exportApplication(Role role, Application application, ExportApplicationBuildParameter eabp) throws XynaException {
    checkApplicationRight(role, application.getName(), application.getVersionName(), Action.deploy);
    CommandControl.tryLock(CommandControl.Operation.APPLICATION_EXPORT,application);
    try {
      return lrcm.exportApplication(application, eabp);
    } finally {
      CommandControl.unlock(CommandControl.Operation.APPLICATION_EXPORT, application);
    }
  }
  
  public void copyApplicationIntoWorkspace(Role role, Application from, Workspace to) throws XynaException {
    checkStaticRight(role, UserManagement.Rights.WORKINGSET_MANAGEMENT);
    CommandControl.tryLock(CommandControl.Operation.APPLICATION_COPY_TO_WORKINGSET, to);
    try {
      CommandControl.tryLock(CommandControl.Operation.APPLICATION_COPY_TO_WORKINGSET, from);
      try {
        lrcm.copyApplicationIntoWorkspace(from, to);
      } finally {
        CommandControl.unlock(CommandControl.Operation.APPLICATION_COPY_TO_WORKINGSET, from);
      }
    } finally {
      CommandControl.unlock(CommandControl.Operation.APPLICATION_COPY_TO_WORKINGSET, to);
    }
  }
  
  
  private void checkStaticRight(Role role, Rights right) throws PersistenceLayerException, XFMG_ACCESS_VIOLATION {
    if (!getUserMgmt().hasRight(right.toString(), role)) {
      throw new XFMG_ACCESS_VIOLATION(right.toString(), role.getName());
    }
  }
  
  private void checkApplicationRight(Role role, String applicationName, String versionName, Action action) throws PersistenceLayerException, XFMG_ACCESS_VIOLATION {
      String right = getUserMgmt().getApplicationRight(applicationName, versionName, action);
      if (!getUserMgmt().hasRight(right, role)) {
        throw new XFMG_ACCESS_VIOLATION(right, role.getName());
      }
  }
  
  private void checkApplicationDefinitionRight(Role role, String applicationName, String workspace, Action action) throws PersistenceLayerException, XFMG_ACCESS_VIOLATION {
    String right = getUserMgmt().getApplicationDefinitionRight(applicationName, workspace, action);
    if (!getUserMgmt().hasRight(right, role)) {
      throw new XFMG_ACCESS_VIOLATION(right, role.getName());
    }
  }
  
  private void checkWorkspaceRight(Role role, String workspace, Action action) throws PersistenceLayerException, XFMG_ACCESS_VIOLATION {
    String right = getUserMgmt().getWorkspaceRight(workspace, action);
    if (!getUserMgmt().hasRight(right, role)) {
      throw new XFMG_ACCESS_VIOLATION(right, role.getName());
    }
  }
  
  private UserManagement getUserMgmt() {
    return XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement();
  }
  
  // we circumvent a full diff by just locking one modifying operation
  public final static CommandControl.Operation getOperationLockForModify(RuntimeDependencyContextType type) {
    switch (type) {
      case Application :
        return CommandControl.Operation.APPLICATION_START;
      case ApplicationDefinition :
        return CommandControl.Operation.APPLICATION_ADDOBJECT;
      case Workspace :
      return CommandControl.Operation.WORKSPACE_CREATE;
      default :
        throw new IllegalArgumentException("No valid RuntimeDependencyContextType: " + type);
    }
  }
  
  private RevisionManagement getRevMgmt() {
    return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
  }
  
  
  public interface MigrateRuntimeContextAccessContext {
    
    void checkAccess(RuntimeDependencyContext rdc) throws XFMG_ACCESS_VIOLATION, PersistenceLayerException;
    
  }
  
  public static class AllAccess implements MigrateRuntimeContextAccessContext {

    public void checkAccess(RuntimeDependencyContext rdc) throws XFMG_ACCESS_VIOLATION, PersistenceLayerException {
    }
    
  }
  
  private class RoleBaseMigrateRuntimeContextAccessContext implements MigrateRuntimeContextAccessContext {

    private final Role role;
    
    private RoleBaseMigrateRuntimeContextAccessContext(Role role) {
      this.role = role;
    }
    
    public void checkAccess(RuntimeDependencyContext rdc) throws XFMG_ACCESS_VIOLATION, PersistenceLayerException {
      switch (rdc.getRuntimeDependencyContextType()) {
        case Application :
          Application app = (Application) rdc;
          checkApplicationRight(role, app.getName(), app.getVersionName(), Action.write);
          break;
        case Workspace :
          Workspace ws = (Workspace) rdc;
          checkWorkspaceRight(role, ws.getName(), Action.write);
          break;
        default :
          // there is no corresponding right for appDefs 
          break;
      }
    }
  }
  
  private MigrateRuntimeContextAccessContext getAccessContext(Role role) {
    // TODO check wether all access rights are granted and return AllAccess 
    return new RoleBaseMigrateRuntimeContextAccessContext(role);
  }
  
  
}
