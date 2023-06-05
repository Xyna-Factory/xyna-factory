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


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xdev.xfractmod.xmomlocks.LockManagement;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.deploymentmarker.DeploymentMarker;
import com.gip.xyna.xfmg.xfctrl.deploymentmarker.DeploymentTag;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateManagement;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateReport;
import com.gip.xyna.xfmg.xfctrl.deploystate.DisplayState;
import com.gip.xyna.xfmg.xfctrl.deploystate.selectdeploymentitem.DeploymentItemColumn;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xmcp.ErroneousOrderExecutionResponse.SerializableExceptionInformation;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xnwh.exceptions.XNWH_NoSelectGivenException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_SelectParserException;
import com.gip.xyna.xnwh.exceptions.XNWH_WhereClauseBuildException;
import com.gip.xyna.xnwh.selection.parsing.ArchiveIdentifier;
import com.gip.xyna.xnwh.selection.parsing.SearchRequestBean;
import com.gip.xyna.xprc.exceptions.XPRC_DeploymentDuringUndeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.MDMParallelDeploymentException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.WorkflowProtectionMode;
import com.gip.xyna.xprc.xfractwfe.generation.WF;

import xmcp.Application;
import xmcp.RuntimeContext;
import xmcp.Workspace;
import xmcp.factorymanager.DeploymentItemsServiceOperation;
import xmcp.factorymanager.deploymentitems.DeleteDeploymentItemParam;
import xmcp.factorymanager.deploymentitems.DeleteDeploymentItemResult;
import xmcp.factorymanager.deploymentitems.Dependency;
import xmcp.factorymanager.deploymentitems.DeploymentItem;
import xmcp.factorymanager.deploymentitems.DeploymentItemId;
import xmcp.factorymanager.deploymentitems.DeploymentMarkerTag;
import xmcp.factorymanager.deploymentitems.ExceptionInformation;
import xmcp.factorymanager.deploymentitems.Inconsistency;
import xmcp.factorymanager.deploymentitems.ResolutionFailure;
import xmcp.factorymanager.deploymentitems.UndeployDeploymentItemParam;
import xmcp.factorymanager.deploymentitems.UndeployDeploymentItemResult;
import xmcp.factorymanager.deploymentitems.exceptions.DeleteDeploymentItemException;
import xmcp.factorymanager.deploymentitems.exceptions.DeployDeploymentItemException;
import xmcp.factorymanager.deploymentitems.exceptions.LoadDeploymentItemException;
import xmcp.factorymanager.deploymentitems.exceptions.LoadDeploymentItemsException;
import xmcp.factorymanager.deploymentitems.exceptions.UnDeployDeploymentItemException;
import xmcp.tables.datatypes.TableColumn;
import xmcp.tables.datatypes.TableInfo;
import xmcp.zeta.TableHelper;


public class DeploymentItemsServiceOperationImpl implements ExtendedDeploymentTask, DeploymentItemsServiceOperation {
  
  private static final String TABLE_PATH_TYPE = "typeNiceName";
  private static final String TABLE_PATH_NAME = "id.name";
  private static final String TABLE_PATH_STATE = "state";
  private static final String TABLE_PATH_OPEN_TASK = "openTaskCount";
  private static final String TABLE_PATH_TAGS = "tagsNiceList";
  private static final String TABLE_PATH_LOCKED_BY = "lockedBy";
  
  
  private final DeploymentItemStateManagement deploymentItemStateManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement();
  private final RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
  private final XynaMultiChannelPortal multiChannelPortal = (XynaMultiChannelPortal) XynaFactory.getInstance().getXynaMultiChannelPortal();
  private final LockManagement lockMgmt = XynaFactory.getInstance().getXynaDevelopment().getXynaFractalModelling().getLockManagement();

  public void onDeployment() throws XynaException {
    // This is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
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
  public DeploymentItem getDeploymentItem(DeploymentItemId deploymentItemId, RuntimeContext runtimeContext) throws LoadDeploymentItemException {
    Long revision = runtimeContext.getRevision();
    if (revision == null) {
      com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext rtcRevMgmt;
      if (runtimeContext instanceof Workspace) {
        rtcRevMgmt = new com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace(runtimeContext.getName());
      } else {
        rtcRevMgmt = new com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application(runtimeContext.getName(), ((Application)runtimeContext).getVersionName());
      }

      try {
        revision = revisionManagement.getRevision(rtcRevMgmt);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new LoadDeploymentItemException("Revision could not be determined.");
      }
    }

    DeploymentItemState deploymentItemState = deploymentItemStateManagement.get(deploymentItemId.getName(), revision);
    if (deploymentItemState == null || DisplayState.NON_EXISTENT == deploymentItemState.getStateReport().getState()) {
      throw new LoadDeploymentItemException("Deployment item not found.");
    }

    DeploymentItemStateReport stateReport = deploymentItemState.getStateReport();
    String lockedBy = lockMgmt.getLockingUser(new LockManagement.Path(stateReport.getFqName(), revision));

    return convert(stateReport, lockedBy);
  }

  @Override
  public List<? extends DeleteDeploymentItemResult> delete(List<? extends DeleteDeploymentItemParam> deleteDeploymentItems, RuntimeContext runtimeContext) throws DeleteDeploymentItemException {
    List<DeleteDeploymentItemResult> results = new ArrayList<>();
    CommandControl.tryLock(CommandControl.Operation.XMOM_WORKFLOW_DEPLOY, runtimeContext.getRevision());
    try {
      for (DeleteDeploymentItemParam deleteDeploymentItem : deleteDeploymentItems) {
        DeploymentItemState deploymentItemState = deploymentItemStateManagement.get(deleteDeploymentItem.getDeploymentItemId().getName(), runtimeContext.getRevision());
        DeleteDeploymentItemResult result = new DeleteDeploymentItemResult();
        result.setDeploymentItemId(deleteDeploymentItem.getDeploymentItemId());
        if(deploymentItemState == null || DisplayState.NON_EXISTENT == deploymentItemState.getStateReport().getState()) {
          result.setSuccess(false);
          if(deploymentItemState != null)
            result.setDeploymentItem(convert(deploymentItemState.getStateReport()));
          ExceptionInformation exceptionInformation = new ExceptionInformation();
          exceptionInformation.setMessage("Item not found.");
          result.setExceptionInformation(exceptionInformation);
        } else {
          try {
            XMOMType type = XMOMType.valueOf(deleteDeploymentItem.getDeploymentItemId().getType());
            switch(type) {
              case DATATYPE:
                multiChannelPortal.deleteDatatype(
                       deleteDeploymentItem.getDeploymentItemId().getName(), 
                       deleteDeploymentItem.getRecursivlyUndeployIfDeployedAndDependenciesExist(), 
                       deleteDeploymentItem.getDeleteDependencies(), runtimeContext.getRevision());
                result.setSuccess(true);
                break;
              case WORKFLOW:
                multiChannelPortal.deleteWorkflow(
                       deleteDeploymentItem.getDeploymentItemId().getName(), 
                       deleteDeploymentItem.getRecursivlyUndeployIfDeployedAndDependenciesExist(), 
                       deleteDeploymentItem.getDeleteDependencies(), runtimeContext.getRevision());
                result.setSuccess(true);
                break;
              case EXCEPTION:
                multiChannelPortal.deleteException(
                       deleteDeploymentItem.getDeploymentItemId().getName(), 
                       deleteDeploymentItem.getRecursivlyUndeployIfDeployedAndDependenciesExist(), 
                       deleteDeploymentItem.getDeleteDependencies(), runtimeContext.getRevision());
                result.setSuccess(true);
                break;
              default:
                break;
            }
          } catch (XynaException e) {
            DeploymentItemStateReport report = deploymentItemStateManagement.get(
                    deleteDeploymentItem.getDeploymentItemId().getName(), runtimeContext.getRevision()).getStateReport();
            result.setDeploymentItem(convert(report));
            result.setSuccess(false);
            result.setExceptionInformation(convert(e));
          }
        }
        results.add(result);
      }
    } finally {
      CommandControl.unlock(CommandControl.Operation.XMOM_WORKFLOW_DEPLOY, runtimeContext.getRevision());
    }
    return results;
  }
  
  @Override
  public List<? extends UndeployDeploymentItemResult> undeploy(List<? extends UndeployDeploymentItemParam> undeployDeploymentItems, RuntimeContext runtimeContext)
      throws UnDeployDeploymentItemException {
    List<UndeployDeploymentItemResult> result = new ArrayList<>(undeployDeploymentItems.size());
    CommandControl.tryLock(CommandControl.Operation.XMOM_WORKFLOW_DEPLOY, runtimeContext.getRevision());
    try {
      for (UndeployDeploymentItemParam undeployDeploymentItem : undeployDeploymentItems) {
        UndeployDeploymentItemResult itemResult = new UndeployDeploymentItemResult();
        DeploymentItemStateReport report = deploymentItemStateManagement.get(
                  undeployDeploymentItem.getDeploymentItemId().getName(), runtimeContext.getRevision()).getStateReport();
        itemResult.setDeploymentItem(convert(report));
        try {
          XMOMType type = XMOMType.valueOf(undeployDeploymentItem.getDeploymentItemId().getType());
          switch(type) {
            case DATATYPE:
              multiChannelPortal.undeployMDM(
                    undeployDeploymentItem.getDeploymentItemId().getName(), 
                    undeployDeploymentItem.getUndeployDependendObjects(), 
                    undeployDeploymentItem.getDisableChecks(), runtimeContext.getRevision());
              itemResult.setSuccess(true);
              break;
            case WORKFLOW:
              multiChannelPortal.undeployWF(
                    undeployDeploymentItem.getDeploymentItemId().getName(), 
                    undeployDeploymentItem.getUndeployDependendObjects(), 
                    undeployDeploymentItem.getDisableChecks(), runtimeContext.getRevision());
              itemResult.setSuccess(true);
              break;            
            case EXCEPTION:
              multiChannelPortal.undeployException(
                    undeployDeploymentItem.getDeploymentItemId().getName(), 
                    undeployDeploymentItem.getUndeployDependendObjects(), 
                    undeployDeploymentItem.getDisableChecks(), runtimeContext.getRevision());
              itemResult.setSuccess(true);
              break;
            default:
              break;
          }
        } catch (XynaException e) {
          itemResult.setSuccess(false);
          itemResult.setExceptionInformation(convert(e));
        }
        result.add(itemResult);
      }
    } finally {
      CommandControl.unlock(CommandControl.Operation.XMOM_WORKFLOW_DEPLOY, runtimeContext.getRevision());
    }
    return result;
  }
  
  @Override
  public List<? extends DeploymentItem> deploy(List<? extends DeploymentItemId> deploymentItemIds, RuntimeContext runtimeContext)
      throws DeployDeploymentItemException {
    List<DeploymentItem> result = new ArrayList<>();
    WorkflowProtectionMode mode = WorkflowProtectionMode.BREAK_ON_USAGE;
    CommandControl.tryLock(CommandControl.Operation.XMOM_WORKFLOW_DEPLOY, runtimeContext.getRevision());
    List<GenerationBase> objects = new ArrayList<>();
    boolean succeeded = false;
    try {
      for (DeploymentItemId id : deploymentItemIds) {
        XMOMType type = XMOMType.valueOf(id.getType());
        switch(type) {
          case DATATYPE:
            objects.add(DOM.getInstance(id.getName(), runtimeContext.getRevision()));
            break;
          case WORKFLOW:
            objects.add(WF.getInstance(id.getName(), runtimeContext.getRevision()));
            break;
          case EXCEPTION:
            objects.add(ExceptionGeneration.getInstance(id.getName(), runtimeContext.getRevision()));
            break;
          default :
            break;
        }
      }
      GenerationBase.deploy(objects, DeploymentMode.codeChanged, false, mode);
      succeeded = true;
    } catch (XPRC_InvalidPackageNameException | MDMParallelDeploymentException | XPRC_DeploymentDuringUndeploymentException e) {
      throw new DeployDeploymentItemException(e.getMessage(), e);
    } finally {
      CommandControl.unlock(CommandControl.Operation.XMOM_WORKFLOW_DEPLOY, runtimeContext.getRevision());
    }
    if(succeeded) {
      for (DeploymentItemId id : deploymentItemIds) {
        DeploymentItemStateReport report = deploymentItemStateManagement.get(id.getName(), runtimeContext.getRevision()).getStateReport();
        result.add(convert(report));
      }
    }
    return result;
  }
  
  @Override
  public List<? extends DeploymentItem> getListEntries(TableInfo tableInfo, RuntimeContext runtimeContext) throws LoadDeploymentItemsException {
    
    final TableHelper<DeploymentItem, TableInfo> tableHelper = TableHelper.<DeploymentItem, TableInfo>init(tableInfo)
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
        .addSelectFunction(TABLE_PATH_TYPE, DeploymentItem::getTypeNiceName)
        .addSelectFunction(TABLE_PATH_NAME, x -> x.getId().getName())
        .addSelectFunction(TABLE_PATH_OPEN_TASK, DeploymentItem::getOpenTaskCount)
        .addSelectFunction(TABLE_PATH_STATE, DeploymentItem::getState)
        .addSelectFunction(TABLE_PATH_TAGS, DeploymentItem::getTagsNiceList)
        .addSelectFunction(TABLE_PATH_LOCKED_BY, DeploymentItem::getLockedBy);
        
    SearchRequestBean searchRequest = tableHelper.createSearchRequest(ArchiveIdentifier.deploymentitem);
    StringBuilder selection = new StringBuilder();
    for (DeploymentItemColumn deploymentItemColumn : DeploymentItemColumn.values()) {
      if(selection.length() > 0)
        selection.append(",");
      selection.append(deploymentItemColumn.getColumnName());
    }
    searchRequest.setSelection(selection.toString());
    
    if(runtimeContext != null) {
      if(runtimeContext instanceof Application) {
        Application application = (Application)runtimeContext;
        searchRequest.addFilterEntry(DeploymentItemColumn.APPLICATION.getColumnName(), application.getName());
        searchRequest.addFilterEntry(DeploymentItemColumn.VERSION.getColumnName(), application.getVersionName());
      } else if (runtimeContext instanceof Workspace) {
        Workspace workspace = (Workspace)runtimeContext;
        searchRequest.addFilterEntry(DeploymentItemColumn.WORKSPACE.getColumnName(), workspace.getName());
      }
    }
    try {
      List<DeploymentItemStateReport> reports = deploymentItemStateManagement.search(searchRequest).getResult();
      List<DeploymentItem> result = reports.stream()
          .map(this::convert)
          .filter(tableHelper.filter())
          .collect(Collectors.toList());
      tableHelper.sort(result);
      return tableHelper.limit(result);      
    } catch (XNWH_NoSelectGivenException | XNWH_WhereClauseBuildException | XNWH_SelectParserException | XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new LoadDeploymentItemsException(e.getMessage(), e);
    }
  }

  private DeploymentItem convert(DeploymentItemStateReport in) {
    return convert(in, null);
  }

  private DeploymentItem convert(DeploymentItemStateReport in, String lockedBy) {
    if(in == null)
      return null;
    DeploymentItem i = new DeploymentItem();
    DeploymentItemId id = new DeploymentItemId(in.getType().name(), in.getFqName());
    i.setId(id);

    i.setLabel(in.getLabel());
    i.setLastModified(in.getLastModified());
    i.setLastModifiedBy(in.getLastModifiedBy());
    i.setLastStateChange(in.getLastStateChange());
    i.setLastStateChangeBy(in.getLastStateChangeBy());
    i.setLockedBy(lockedBy);

    i.setOpenTaskCount(in.getOpenTaskCount());
    i.setSpecialType(in.getSpecialType());
    i.setState(in.getState().name());
    List<DeploymentMarker> deploymentMarkers = in.getDeploymentMarker();
    final StringBuilder tagsNiceList = new StringBuilder(); 
    List<DeploymentMarkerTag> tags = deploymentMarkers.stream()
      .filter(m -> m instanceof DeploymentTag)
      .map(m -> {
        DeploymentTag tag = (DeploymentTag)m;
        DeploymentMarkerTag t = new DeploymentMarkerTag();
        t.setDeploymentItemName(tag.getDeploymentItem().getName());
        t.setDeploymentItemType(tag.getDeploymentItem().getType().getNiceName());
        t.setId(tag.getId());
        t.setLabel(tag.getLabel());
        if(tag.getRuntimeContext() instanceof com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application)
          t.setRuntimeContext(convert((com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application)tag.getRuntimeContext()));
        else if (tag.getRuntimeContext() instanceof com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace) {
          t.setRuntimeContext(convert((com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace)tag.getRuntimeContext()));
        }
        if(tagsNiceList.length() > 0)
          tagsNiceList.append(", ");
        tagsNiceList.append(tag.getLabel());
        return t;
      }).collect(Collectors.toList());
    i.setTagsNiceList(tagsNiceList.toString());
    i.setTags(tags);
    
    if(in.getType() != null) {
      i.setTypeNiceName(in.getType().getNiceName());
    }
    i.setRollbackOccurred(in.rollbackOccurred());
    i.setBuildExceptionOccurred(in.buildExceptionOccurred());
    i.setRollbackCause(convert(in.getRollbackCause()));
    i.setRollbackException(convert(in.getRollbackException()));
    i.setBuildException(convert(in.getBuildException()));
    if(in.getInconsitencies() != null) {
      List<Inconsistency> inconsistencies = new ArrayList<>(in.getInconsitencies().size());
      for (com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateReport.Inconsistency inc : in.getInconsitencies()) {
        inconsistencies.add(new Inconsistency(
                (inc.getType() != null) ? inc.getType().name() : "", inc.getEmploymentDescription(), 
                (inc.getEmploymentType() != null) ? inc.getEmploymentType().name() : "", 
                inc.getFqName(), (inc.getXmomtype() != null) ? inc.getXmomtype().name() : "", inc.isItemExists(), 
                inc.getCreationHint(), inc.toFriendlyString()));
      }
      i.setInconsitencies(inconsistencies);
    }
    if(in.getUnresolvable() != null) {
      List<ResolutionFailure> resolutionFailures = new ArrayList<>(in.getUnresolvable().size());
      for (com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateReport.ResolutionFailure r : in.getUnresolvable()) {
        resolutionFailures.add(new ResolutionFailure(r.getType().name(), r.getId(), r.getStepId(), r.toFriendlyString()));
      }
      i.setUnresolvable(resolutionFailures);
    }
    if(in.getDependencies() != null) {
      List<Dependency> dependencies = new ArrayList<>(in.getDependencies().size());
      for (com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateReport.Dependency d : in.getDependencies()) {
        dependencies.add(new Dependency(d.getFqName(), d.getXmomtype().name(), d.getType().name()));
      }
      i.setDependencies(dependencies);
    }    
    return i;
  }
  
  private ExceptionInformation convert(SerializableExceptionInformation in) {
    if(in == null)
      return null;
    ExceptionInformation r = new ExceptionInformation();
    r.setClassName(in.getClassName());
    r.setMessage(in.getMessage());
    StringBuilder sb = new StringBuilder();
    String newLine = System.getProperty("line.separator");
    for (StackTraceElement s : in.getStackTraceElements()) {
      sb.append(s.toString()).append(newLine);
    }
    r.setStacktrace(sb.toString());
    return r;
  }
  
  private ExceptionInformation convert(XynaException in) {
    if(in == null)
      return null;
    ExceptionInformation r = new ExceptionInformation();
    r.setMessage(in.getMessage());
    StringBuilder sb = new StringBuilder();
    String newLine = System.getProperty("line.separator");
    for (StackTraceElement s : in.getStackTrace()) {
      sb.append(s.toString()).append(newLine);
    }
    r.setStacktrace(sb.toString());
    return r;
  }
  
  private xmcp.Application convert(com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application app) {
    xmcp.Application application = new xmcp.Application();
    application.setName(app.getName());
    application.setType(app.getRuntimeDependencyContextType().name());
    application.setVersionName(app.getVersionName());
    try {
      application.setRevision(revisionManagement.getRevision(app));
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      application.setRevision(null);
    }
    return application;
  }
  
  private xmcp.Workspace convert(com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace workspace){
    xmcp.Workspace w = new xmcp.Workspace();
    w.setName(workspace.getName());
    try {
      w.setRevision(revisionManagement.getRevision(workspace));
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      w.setRevision(null);
    }
    w.setType(workspace.getRuntimeDependencyContextType().name());
    return w;
  }

}
