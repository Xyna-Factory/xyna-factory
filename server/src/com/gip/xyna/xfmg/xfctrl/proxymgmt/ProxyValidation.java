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
package com.gip.xyna.xfmg.xfctrl.proxymgmt;

import java.rmi.RemoteException;
import java.util.EnumMap;

import com.gip.xyna.xfmg.xfctrl.appmgmt.RemoveApplicationParameters;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.parameters.ImportDataModelParameters;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.parameters.ModifyDataModelParameters;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.parameters.RemoveDataModelParameters;
import com.gip.xyna.xfmg.xfctrl.deploymentmarker.DeploymentMarker;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.right.ProxyCheckAfterwards;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.right.ProxyRight;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.ApplicationDefinition;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyWithDefaultValue;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.storables.OrderInputSourceStorable;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeParameter;
import com.gip.xyna.xfmg.xods.ordertypemanagement.SearchOrdertypeParameter;
import com.gip.xyna.xfmg.xopctrl.usermanagement.ScopedRightUtils;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Action;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Rights;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.ScopedRight;
import com.gip.xyna.xmcp.RemoteCronLikeOrderCreationParameter;
import com.gip.xyna.xmcp.RemoteXynaOrderCreationParameter;
import com.gip.xyna.xnwh.selection.parsing.ArchiveIdentifier;
import com.gip.xyna.xnwh.selection.parsing.SearchRequestBean;
import com.gip.xyna.xprc.xbatchmgmt.beans.BatchProcessInput;
import com.gip.xyna.xprc.xbatchmgmt.selectbatch.BatchProcessSelectImpl;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xsched.cronlikescheduling.selectcrons.CronLikeOrderSelectImpl;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSelectImpl;

public class ProxyValidation {

  private ProxyRole proxyRole;
  private EnumMap<ArchiveIdentifier, ProxyRight> searchMap;
  
  public ProxyValidation(ProxyRole proxyRole) {
    this.proxyRole = proxyRole;
  }
  
  public static class AccessViolationException extends RemoteException {

    private static final long serialVersionUID = 1L;

    public AccessViolationException(String right, String proxyRole) {
      super("Access to "+right+" not allowed for proxy role "+proxyRole);
    }

  }
  
  private void check(String scopedRight) throws AccessViolationException {
    if( ! proxyRole.hasRight(scopedRight) ) {
      throw new AccessViolationException(scopedRight, proxyRole.getName() );
    }
  }

 
  public void check_START_ORDER(String orderType) throws AccessViolationException {
    check_START_ORDER( new DestinationKey(orderType) ); 
  }
  public void check_START_ORDER(RemoteXynaOrderCreationParameter rxocp) throws AccessViolationException {
    check_START_ORDER( rxocp.getDestinationKey() );
  }
  public void check_START_ORDER(DestinationKey destinationKey) throws AccessViolationException {
    check( ScopedRightUtils.getStartOrderRight( destinationKey ) );
  }
 
  public void check_TIME_CONTROLLED_ORDER(Action action) throws AccessViolationException {
    //check( userManagement.getManageTCORight(action, input) ); //TODO
    //check( userManagement.getReadTCORight(batchProcessInfo) );
  }
  public void check_TIME_CONTROLLED_ORDER(Action action, BatchProcessInput batchProcessInput) throws AccessViolationException {
    check( ScopedRightUtils.getManageTCORight(action, batchProcessInput) );
  }
  public void check_TIME_CONTROLLED_ORDER(Action action, Long batchProcessId) throws AccessViolationException {
    //TODO kann im Proxy nicht weiter gepr�ft werden
    check( ScopedRightUtils.getScopedRight(ScopedRight.TIME_CONTROLLED_ORDER, action ) );
  }
  @ProxyCheckAfterwards
  public void check_TIME_CONTROLLED_ORDER(Action action, BatchProcessSelectImpl batchProcessSelect) throws AccessViolationException {
    //ist irgendein read gesetzt?
    check( ScopedRightUtils.getScopedRight(ScopedRight.TIME_CONTROLLED_ORDER, action ) );
  }
  public void check_TIME_CONTROLLED_ORDER(Action action, Long batchProcessId, BatchProcessInput batchProcessInput) throws AccessViolationException {
    //TODO kann im Proxy nicht weiter gepr�ft werden
    check( ScopedRightUtils.getScopedRight(ScopedRight.TIME_CONTROLLED_ORDER, action ) );
  }
  
  public void check_XYNA_PROPERTY(Action action, String key) throws AccessViolationException {
    check( ScopedRightUtils.getXynaPropertyRight(key, action) );
  }
  public void check_XYNA_PROPERTY(Action action) throws AccessViolationException {
    check( ScopedRightUtils.getXynaPropertyRight( null, action) );
  }
  public void check_XYNA_PROPERTY(Action action, XynaPropertyWithDefaultValue property) throws AccessViolationException {
    check( ScopedRightUtils.getXynaPropertyRight( property.getName(), action) );
  }
 

  public void check_APPLICATION(Action action, String applicationName, String versionName) throws AccessViolationException {
    check( ScopedRightUtils.getApplicationRight(applicationName, versionName, action) );
  }
  public void check_APPLICATION(Action action) throws AccessViolationException {
    check( ScopedRightUtils.getApplicationRight(null, null, action) ); //TODO
  }
  public void check_APPLICATION(Action action, String fileId) throws AccessViolationException {
    //TODO kann im Proxy nicht weiter gepr�ft werden
    check( ScopedRightUtils.getScopedRight(ScopedRight.APPLICATION, action) );
  }
  
  public void check_APPLICATION_DEFINITION(Action action, Workspace workspace, String applicationName) throws AccessViolationException {
    String wsp = workspace != null ? workspace.getName() : RevisionManagement.DEFAULT_WORKSPACE.getName();
    check( ScopedRightUtils.getScopedRight(ScopedRight.APPLICATION_DEFINITION, action, wsp, applicationName) );
  }
  public void check_APPLICATION_DEFINITION(Action action, String applicationName, RemoveApplicationParameters remove) throws AccessViolationException {
    check_APPLICATION_DEFINITION(action, remove.getParentWorkspace(), applicationName);
  }
  
  public void check_WORKSPACE(Action action) throws AccessViolationException {
    check( ScopedRightUtils.getScopedRight(ScopedRight.WORKSPACE, action ) );
  }
  public void check_WORKSPACE() throws AccessViolationException { 
  }
  
  public void check_DATA_MODEL(Action action, ImportDataModelParameters dataModel) throws AccessViolationException {
    check( ScopedRightUtils.getScopedRight(ScopedRight.DATA_MODEL, action, dataModel.getDataModelName() ) );
  }
  public void check_DATA_MODEL(Action action, RemoveDataModelParameters dataModel) throws AccessViolationException {
    check( ScopedRightUtils.getScopedRight(ScopedRight.DATA_MODEL, action, dataModel.getDataModelName() ) );
  }
  public void check_DATA_MODEL(Action action, ModifyDataModelParameters dataModel) throws AccessViolationException {
    check( ScopedRightUtils.getScopedRight(ScopedRight.DATA_MODEL, action, dataModel.getDataModelName() ) );
  }
  
  public void check_DEPLOYMENT_MARKER(Action action, DeploymentMarker deploymentMarker) throws AccessViolationException {
    check( ScopedRightUtils.getScopedRight(ScopedRight.DEPLOYMENT_MARKER, action ) ); //TODO genauer
  }
  /*
  public void check_DEPLOYMENT_ITEM(Action action) throws AccessViolationException {
  }*/

  public void check_ORDER_INPUT_SOURCE(Action action, OrderInputSourceStorable orderInputSource) throws AccessViolationException {
    check( ScopedRightUtils.getScopedRight(ScopedRight.ORDER_INPUT_SOURCE, action ) );
  }
  public void check_ORDER_INPUT_SOURCE(Action action, long orderInputId) throws AccessViolationException {
    //TODO kann im Proxy nicht weiter gepr�ft werden
    check( ScopedRightUtils.getScopedRight(ScopedRight.ORDER_INPUT_SOURCE, action ) );
  }
  
  public void check_CRON_LIKE_ORDER(Action action) throws AccessViolationException {
    check_CRON_LIKE_ORDER( action, (DestinationKey)null);
  }
  public void check_CRON_LIKE_ORDER(Action action, Long id) throws AccessViolationException {
    //TODO kann im Proxy nicht weiter gepr�ft werden
    check( ScopedRightUtils.getScopedRight(ScopedRight.CRON_LIKE_ORDER, action) );
  }
  public void check_CRON_LIKE_ORDER(Action action, String orderType) throws AccessViolationException {
    check_CRON_LIKE_ORDER( action, new DestinationKey(orderType) );
  }
  public void check_CRON_LIKE_ORDER(Action action, String orderType, String applicationName, String versionName) throws AccessViolationException {
    check_CRON_LIKE_ORDER( action, new DestinationKey(orderType, applicationName, versionName) );
  }
  public void check_CRON_LIKE_ORDER(Action action, RemoteCronLikeOrderCreationParameter rclocp) throws AccessViolationException {
    check_CRON_LIKE_ORDER( action, rclocp.getDestinationKey() );
  }
  private void check_CRON_LIKE_ORDER(Action action, DestinationKey destinationKey) throws AccessViolationException {
    check( ScopedRightUtils.getScopedRight(ScopedRight.CRON_LIKE_ORDER, action) );
    if( action == Action.insert || action == Action.write ) {
      //Start-Order-Recht f�r neue Destination wird ben�tigt
      if( proxyRole.hasRight(Rights.START_ORDER ) ) {
        return;
      }
      check_START_ORDER(destinationKey);
    }
  }
  
  @ProxyCheckAfterwards
  public void check_CRON_LIKE_ORDER(Action action, CronLikeOrderSelectImpl clos) throws AccessViolationException {
    check( ScopedRightUtils.getScopedRight(ScopedRight.CRON_LIKE_ORDER, action) );
  }
  
  
  
  public void check_ORDER_TYPE(Action action, String orderType) throws AccessViolationException {
    check( ScopedRightUtils.getScopedRight(ScopedRight.ORDER_TYPE, action, orderType) );
  }
  public void check_ORDER_TYPE(Action action, DestinationKey destinationKey) throws AccessViolationException {
    check( ScopedRightUtils.getScopedRight(ScopedRight.ORDER_TYPE, action, destinationKey.getOrderType(), destinationKey.getApplicationName(), destinationKey.getVersionName() ) );
  }
  public void check_ORDER_TYPE(Action action, OrdertypeParameter ordertype) throws AccessViolationException {
    check( ScopedRightUtils.getScopedRight(ScopedRight.ORDER_TYPE, action, ordertype.getOrdertypeName(), ordertype.getApplicationName(), ordertype.getVersionName() ) );
  }
  public void check_ORDER_TYPE(Action action) throws AccessViolationException {
    check( ScopedRightUtils.getScopedRight(ScopedRight.ORDER_TYPE, action ) );
  }
  
  @ProxyCheckAfterwards
  public void check_ORDER_TYPE(Action action, RuntimeContext runtimeContext) throws AccessViolationException {
    String orderType = "*"; //alle
    if( runtimeContext instanceof Application ) {
      check( ScopedRightUtils.getScopedRight(ScopedRight.ORDER_TYPE, action, orderType, runtimeContext.getName(), ((Application)runtimeContext).getVersionName() ) );
    } else {
      check( ScopedRightUtils.getScopedRight(ScopedRight.ORDER_TYPE, action, orderType, runtimeContext.getName() ) );
    }
  }
  @ProxyCheckAfterwards
  public void check_ORDER_TYPE(Action action, SearchOrdertypeParameter sop ) throws AccessViolationException {
    check( ScopedRightUtils.getScopedRight(ScopedRight.ORDER_TYPE, action ) );
  }

  
  
  public void check_CAPACITY(Action action) throws AccessViolationException {
    check( ScopedRightUtils.getScopedRight(ScopedRight.CAPACITY, action) );
  }
  public void check_CAPACITY(Action action, String capacityName) throws AccessViolationException {
    check( ScopedRightUtils.getScopedRight(ScopedRight.CAPACITY, action, capacityName) );
  }

  public void check_VETO(Action action) throws AccessViolationException {
    check( ScopedRightUtils.getScopedRight(ScopedRight.VETO, action) );
  }
  public void check_VETO(Action action, String vetoName) throws AccessViolationException {
    check( ScopedRightUtils.getScopedRight(ScopedRight.VETO, action, vetoName) );
  }
  @ProxyCheckAfterwards
  public void check_VETO(Action action, VetoSelectImpl vetoSelect) throws AccessViolationException {
    check( ScopedRightUtils.getScopedRight(ScopedRight.VETO, action) );
  }
  
  
  public void check_RUNTIMECONTEXT_MANAGEMENT(Action action, RuntimeDependencyContext runtimeContext) throws AccessViolationException {
    if (runtimeContext instanceof Application) {
      Application app = (Application)runtimeContext;
      check( ScopedRightUtils.getScopedRight(ScopedRight.APPLICATION, action, app.getName(), app.getVersionName() ) );
    } else if (runtimeContext instanceof Workspace ) {
      if( proxyRole.hasRight(Rights.WORKINGSET_MANAGEMENT ) ) {
        return;
      }
      Workspace wsp = (Workspace)runtimeContext;
      check( ScopedRightUtils.getScopedRight(ScopedRight.WORKSPACE, action, wsp.getName() ) );
    } else if ( runtimeContext instanceof ApplicationDefinition ) {
      if( proxyRole.hasRight(Rights.WORKINGSET_MANAGEMENT ) ) {
        return;
      }
      ApplicationDefinition appDef = (ApplicationDefinition)runtimeContext;
      check( ScopedRightUtils.getScopedRight(ScopedRight.APPLICATION_DEFINITION, action, appDef.getParentWorkspace().getName(), appDef.getName() ) );
   } else {
      throw new RuntimeException("Unsupported runtime context: " + runtimeContext.getClass().getName());
    }
  }
  
  @ProxyCheckAfterwards
  public void check_SEARCH(Action action, SearchRequestBean searchRequest) throws AccessViolationException {
    EnumMap<ArchiveIdentifier, ProxyRight> searchMap = getOrCreateSearchMap();
    ProxyRight proxyRight = searchMap.get( searchRequest.getArchiveIdentifier() );
    if( proxyRight != null ) {
      if( ! proxyRight.isAllowedIn(proxyRole, action) ) {
        throw new AccessViolationException(proxyRight.name(), proxyRole.getName() );
      }
    }
  }
  
  /**
   * TODO alle anderen derzeit nicht erlaubt. 
   * Siehe UnsupportedOperationException in {@code XynaMultiChannelPortal.search(SearchRequestBean searchRequest)}
   * TODO bei Erweiterungen ProxyRight.SEARCH erweitern!
   **/
  private EnumMap<ArchiveIdentifier, ProxyRight> getOrCreateSearchMap() {
    if( searchMap == null ) {
      EnumMap<ArchiveIdentifier, ProxyRight> sm = new EnumMap<ArchiveIdentifier, ProxyRight>(ArchiveIdentifier.class);
      sm.put(ArchiveIdentifier.datamodel, ProxyRight.DATA_MODEL);
      sm.put(ArchiveIdentifier.deploymentitem, ProxyRight.DEPLOYMENT_ITEM);
      sm.put(ArchiveIdentifier.runtimecontext, ProxyRight.RUNTIMECONTEXT_MANAGEMENT);
      sm.put(ArchiveIdentifier.xmomdetails, ProxyRight.APPLICATION_DEFINITION); //TODO korrekt?
      sm.put(ArchiveIdentifier.orderInputSource, ProxyRight.ORDER_INPUT_SOURCE);
      searchMap = sm;
    }
    return searchMap;
  }

}
