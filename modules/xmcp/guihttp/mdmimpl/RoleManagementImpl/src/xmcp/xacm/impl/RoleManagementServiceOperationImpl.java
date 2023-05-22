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
package xmcp.xacm.impl;


import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.XynaFactoryManagement;
import com.gip.xyna.xfmg.exceptions.XFMG_PredefinedXynaObjectException;
import com.gip.xyna.xfmg.exceptions.XFMG_RightDoesNotExistException;
import com.gip.xyna.xfmg.exceptions.XFMG_RoleDoesNotExistException;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;

import xmcp.xacm.RoleManagementServiceOperation;
import xmcp.xacm.rightmanagement.datatypes.RightParameter;
import xmcp.xacm.rolemanagement.datatypes.Role;
import xmcp.xacm.rolemanagement.exceptions.ChangeRoleException;


public class RoleManagementServiceOperationImpl implements ExtendedDeploymentTask, RoleManagementServiceOperation {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(RoleManagementServiceOperationImpl.class);
  
  private static final XynaFactoryManagement factoryManagement =  (XynaFactoryManagement) XynaFactory.getInstance().getFactoryManagement();

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
  public void changeRole(Role role) throws ChangeRoleException {
    try {
      if(role.getDescription() != null) {
        factoryManagement.setDescriptionOfRole(role.getRoleName(), role.getDomainName0(), role.getDescription());
      }
      com.gip.xyna.xfmg.xopctrl.usermanagement.Role factoryRole = factoryManagement.getRole(role.getRoleName(), role.getDomainName0());
      List<String> factoryRights = new ArrayList<>();
      factoryRights.addAll(factoryRole.getRightsAsList());
      factoryRights.addAll(factoryRole.getScopedRights());
      
      List<String> rightsToGrant = new ArrayList<>();
      for (xmcp.xacm.rightmanagement.datatypes.Right right : role.getRightList()) {
        String rightWithParameter = getRightWithParameter(right);
        if(!factoryRights.contains(rightWithParameter)) {
          rightsToGrant.add(rightWithParameter);
        }
      }
      List<String> rightsToRevoke = new ArrayList<>();
      for (String r : factoryRights) {
        boolean contains = false;
        for (xmcp.xacm.rightmanagement.datatypes.Right right : role.getRightList()) {
          String rightWithParameter = getRightWithParameter(right);
          if(r.equals(rightWithParameter)) {
            contains = true;
            break;
          }
        }
        if(!contains) {
          rightsToRevoke.add(r);
        }
      }
      for (String right : rightsToRevoke) {
        try {
          factoryManagement.revokeRightFromRole(role.getRoleName(), right);
        } catch (XFMG_RightDoesNotExistException e) {
          logger.error(e.getMessage(), e);
        }
      }
      for (String right : rightsToGrant) {
        try {
          factoryManagement.grantRightToRole(role.getRoleName(), right);
        } catch (XFMG_RightDoesNotExistException e) {
          logger.error(e.getMessage(), e);
        }
      }
      
    } catch (PersistenceLayerException | XFMG_PredefinedXynaObjectException | XFMG_RoleDoesNotExistException e) {
      throw new ChangeRoleException(e.getMessage(), e);
    }
  }
  
  private String getRightWithParameter(xmcp.xacm.rightmanagement.datatypes.Right right) {
    if(right.getParameterList() == null) {
      return right.getRightName();
    }
    StringBuilder sb = new StringBuilder(right.getRightName());
    for (RightParameter param : right.getParameterList()) {
      sb.append(":").append(param.getValue());
    }
    return sb.toString();
  }

}
