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
package com.gip.xyna.xfmg.xfctrl.proxymgmt.right;

import java.util.EnumSet;

import com.gip.xyna.xfmg.xfctrl.proxymgmt.ProxyRole;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.ProxyRole.RightStringList;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Action;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Rights;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.ScopedRight;

public enum ProxyRight {
  
  //Rights
  TRIGGER_FILTER_MANAGEMENT(Rights.TRIGGER_FILTER_MANAGEMENT),
  PROCESS_MANUAL_INTERACTION(Rights.PROCESS_MANUAL_INTERACTION),
  EDIT_MDM(Rights.EDIT_MDM),
  DEPLOYMENT_MDM(Rights.DEPLOYMENT_MDM),
  USER_MANAGEMENT(Rights.USER_MANAGEMENT),
  ORDERARCHIVE_VIEW(Rights.ORDERARCHIVE_VIEW),
  ORDERARCHIVE_DETAILS(Rights.ORDERARCHIVE_DETAILS),
  DISPATCHER_MANAGEMENT(Rights.DISPATCHER_MANAGEMENT),
  VIEW_MANUAL_INTERACTION(Rights.VIEW_MANUAL_INTERACTION),
  FREQUENCY_CONTROL_MANAGEMENT(Rights.FREQUENCY_CONTROL_MANAGEMENT),
  FREQUENCY_CONTROL_VIEW(Rights.FREQUENCY_CONTROL_VIEW),
  USER_MANAGEMENT_EDIT_OWN(Rights.USER_MANAGEMENT_EDIT_OWN),
  KILL_STUCK_PROCESS(Rights.KILL_STUCK_PROCESS),
  TOPOLOGY(Rights.TOPOLOGY_MODELLER), 
  
  //nicht verwendete Rights
  //MONITORING_LEVEL_MANAGEMENT(Rights.MONITORING_LEVEL_MANAGEMENT),
  //PERSISTENCE_MANAGEMENT(Rights.PERSISTENCE_MANAGEMENT),
  //SESSION_CREATION(Rights.SESSION_CREATION),
  //USER_LOGIN(Rights.USER_LOGIN),
  //WORKINGSET_MANAGEMENT(Rights.WORKINGSET_MANAGEMENT),
  //APPLICATION_MANAGEMENT(Rights.APPLICATION_MANAGEMENT),
  //APPLICATION_ADMINISTRATION(Rights.APPLICATION_ADMINISTRATION),

  
  //ScopedRights
  TIME_CONTROLLED_ORDER(ScopedRight.TIME_CONTROLLED_ORDER),
  XYNA_PROPERTY(ScopedRight.XYNA_PROPERTY),
  APPLICATION_DEFINITION(ScopedRight.APPLICATION_DEFINITION),
  DATA_MODEL(ScopedRight.DATA_MODEL),
  DEPLOYMENT_MARKER(ScopedRight.DEPLOYMENT_MARKER),
  DEPLOYMENT_ITEM(ScopedRight.DEPLOYMENT_ITEM),
  ORDER_INPUT_SOURCE(ScopedRight.ORDER_INPUT_SOURCE),
  CRON_LIKE_ORDER(ScopedRight.CRON_LIKE_ORDER),
  ORDER_TYPE(ScopedRight.ORDER_TYPE),
  CAPACITY(ScopedRight.CAPACITY),
  VETO(ScopedRight.VETO),
  APPLICATION(ScopedRight.APPLICATION),
  
  //Mixed
  START_ORDER(ScopedRight.START_ORDER, Rights.START_ORDER), 
  WORKSPACE(ScopedRight.WORKSPACE, Rights.WORKINGSET_MANAGEMENT),
  
  //spezielle 
  SEARCH() {
    private EnumSet<ScopedRight> scopedRights = EnumSet.of(ScopedRight.DATA_MODEL,
        ScopedRight.DEPLOYMENT_ITEM, 
        ScopedRight.ORDER_INPUT_SOURCE,
        ScopedRight.APPLICATION_DEFINITION,
        ScopedRight.WORKSPACE, 
        ScopedRight.APPLICATION 
        );
 
    @Override
    public boolean needsValidation(ProxyRole proxyRole) {
      return true;
    }
    @Override
    public boolean isAllowedIn(ProxyRole proxyRole, Action action) {
      for( ScopedRight sr : scopedRights ) {
        if( proxyRole.hasRight(sr, action) ) {
          return true;
        }
      }
      return false;
    }
    @Override
    public String filterRightStrings(ProxyRole proxyRole, Action action) {
      RightStringList rs = proxyRole.getRightStrings();
      for( ScopedRight sr : scopedRights ) {
        rs.add( sr, action);
      }
      return rs.asString();
     }
  },
  
  RUNTIMECONTEXT_MANAGEMENT() { //Kombination aus WORKSPACE, APPLICATION und APPLICATION_DEFINITION
    @Override
    public boolean needsValidation(ProxyRole proxyRole) {
      return true;
    }
    @Override
    public boolean isAllowedIn(ProxyRole proxyRole, Action action) {
      if( proxyRole.hasRight(Rights.WORKINGSET_MANAGEMENT) ) {
        return true;
      }
      if( proxyRole.hasRight(ScopedRight.WORKSPACE, action) ) {
        return true;
      }
      if( proxyRole.hasRight(ScopedRight.APPLICATION, action) ) {
        return true;
      }
      if( proxyRole.hasRight(ScopedRight.APPLICATION_DEFINITION, action) ) {
        return true;
      }
      return false;
    }
    @Override
    public String filterRightStrings(ProxyRole proxyRole, Action action) {
      return proxyRole.getRightStrings().
          add(Rights.WORKINGSET_MANAGEMENT).
          add(ScopedRight.WORKSPACE, action).
          add(ScopedRight.APPLICATION, action).
          add(ScopedRight.APPLICATION_DEFINITION, action).
          asString();
     }
  },

  PUBLIC() { //FIXME: sind diese wirklich alle Public?
    @Override
    public boolean needsValidation(ProxyRole proxyRole) {
      return false;
    }
    @Override
    public boolean isAllowedIn(ProxyRole proxyRole, Action action) {
      return proxyRole.getGenerationData().acceptPublic();
    }
    @Override
    public String filterRightStrings(ProxyRole proxyRole, Action action) {
      return "Public";
    }
  }
  
  ;
  
  private final ScopedRight scopedRight;
  private final Rights right;
  
  private ProxyRight() {
    this.scopedRight = null;
    this.right = null;
  }
  private ProxyRight(ScopedRight scopedRight) {
    this.scopedRight = scopedRight;
    this.right = null;
  }
  private ProxyRight(Rights right) {
    this.scopedRight = null;
    this.right = right;
  }
  private ProxyRight(ScopedRight scopedRight, Rights right) {
    this.scopedRight = scopedRight;
    this.right = right;
  }
  
  public boolean needsValidation(ProxyRole proxyRole) {
    if( right != null && proxyRole.hasRight(right) ) {
      //right erlaubt alles, daher keine genaue Validierung nötig
      return false;
    }
    if( scopedRight != null ) {
      //genauere Validierung nur bei ScopedRight nötig
      return true;
    }
    return false; //sollte nicht vorkommen FIXME
  }
  
  public boolean isAllowedIn(ProxyRole proxyRole, Action action) {
    if( proxyRole.hasRight(right) ) {
      return true;
    }
    if( proxyRole.hasRight( scopedRight, action) ) {
      return true;
    }
    return false;
  }
  
  public String filterRightStrings(ProxyRole proxyRole, Action action) {
    return proxyRole.getRightStrings().
        add(right).
        add(scopedRight, action).
        asString();
  }
  

}
