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
package xmcp.factorymanager.shared.impl;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyWithDefaultValue;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeManagement;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeParameter;
import com.gip.xyna.xfmg.xods.ordertypemanagement.SearchOrdertypeParameter;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Rights;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.ScopedRight;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaOrderServerExtension.ExecutionType;

import xfmg.xopctrl.UserAuthenticationRight;
import xfmg.xopctrl.UserAuthenticationRole;
import xmcp.Application;
import xmcp.RuntimeContext;
import xmcp.factorymanager.shared.DefaultMonitoringLevel;
import xmcp.factorymanager.shared.InsufficientRights;
import xmcp.factorymanager.shared.LoadOrderTypesException;
import xmcp.factorymanager.shared.OrderType;
import xmcp.factorymanager.shared.SharedServicesServiceOperation;
import xmcp.factorymanager.shared.Timezone;


public class SharedServicesServiceOperationImpl implements ExtendedDeploymentTask, SharedServicesServiceOperation {
  
  private static final String PROPERTY_KEY_DEFAULT_MONITORING_LEVEL = "xyna.default.monitoringlevel";
  
  private final RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
  private final OrdertypeManagement ordertypeManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderTypeManagement();
  private final XynaMultiChannelPortal multiChannelPortal = (XynaMultiChannelPortal)XynaFactory.getInstance().getXynaMultiChannelPortal();

  
  private static final String PROPERTY_KEY_SHOW_GLOBAL_APP_MGMT = "xfmg.xfctrl.appmgmt.showGlobalApplicationManagement";
  
  private static final XynaPropertyBoolean SHOW_GLOBAL_APP_MGMT = new XynaPropertyBoolean(PROPERTY_KEY_SHOW_GLOBAL_APP_MGMT, false)
      .setDefaultDocumentation(DocumentationLanguage.DE, "Steuert, ob die Applikation \"GlobalApplicationMgmt\" angezeigt wird oder nicht.")
      .setDefaultDocumentation(DocumentationLanguage.EN, "Controls whether the application \"GlobalApplicationMgmt\" is displayed in the GUI or not.");
  
  
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
  public List<? extends UserAuthenticationRight> hasRoleAllRights(UserAuthenticationRole role, List<? extends UserAuthenticationRight> requiredRights) {
    List<UserAuthenticationRight> missingRights = new ArrayList<>();
    for (UserAuthenticationRight requiredRight : requiredRights) {
      try {
        if(!multiChannelPortal.hasRight(requiredRight.getRight(), role.getRole())) {
          missingRights.add(requiredRight);
        }
      } catch (PersistenceLayerException e) {
        missingRights.add(requiredRight);
      }
    }
    return missingRights;
  }
  
  @Override
  public DefaultMonitoringLevel getDefaultMonitoringLevel(DefaultMonitoringLevel defaultMonitoringLevel) {
    XynaPropertyWithDefaultValue property = multiChannelPortal.getPropertyWithDefaultValue(PROPERTY_KEY_DEFAULT_MONITORING_LEVEL);
    if(property == null)
      return defaultMonitoringLevel;
    try {
      String value = property.getValue();
      if(value == null)
        value = property.getDefValue();
      if(value == null)
        return defaultMonitoringLevel;
      return new DefaultMonitoringLevel(Integer.valueOf(value));
    } catch (Exception ex) {
      return defaultMonitoringLevel;
    }
  }
  
  @Override
  public List<? extends Timezone> getTimezones() {
    String[] timezones = TimeZone.getAvailableIDs();
    List<Timezone> result = new ArrayList<>(timezones.length);
    for (String t : timezones) {
      result.add(new Timezone(t));
    }
    return result;
  }
  
  @Override
  public List<? extends OrderType> getOrderTypes(RuntimeContext guiRuntimeContext) throws LoadOrderTypesException {
    try {
      
      com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext runtimeContext = revisionManagement.getRuntimeContext(guiRuntimeContext.getRevision());
      List<OrdertypeParameter> ordertypeParameters = ordertypeManagement.listOrdertypes(SearchOrdertypeParameter.hierarchy(runtimeContext));
      return ordertypeParameters.stream()
          .filter(otp -> 
            otp.getExecutionDestinationValue() != null &&
                otp.getExecutionDestinationValue().getDestinationTypeEnum() == ExecutionType.XYNA_FRACTAL_WORKFLOW 
          )
          .map(otp -> {
            OrderType ot = new OrderType();
            ot.setName(otp.getOrdertypeName());
            ot.setType(otp.getExecutionDestinationValue().getFullQualifiedName());
            return ot;
          }).collect(Collectors.toList());
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY | PersistenceLayerException e) {
      throw new LoadOrderTypesException(e.getMessage(), e);
    }
  }

  private boolean hasScopedRight(Role role, ScopedRight scopedRight) {
    if (role == null || scopedRight == null) {
      return false;
    }

    boolean hasRight = false;
    for (String rightName : role.getScopedRights()) {
      if (rightName.startsWith(scopedRight.getKey())) {
        hasRight = true;
        break;
      }
    }

    return hasRight;
  }

  @Override
  public List<? extends RuntimeContext> getRuntimeContexts(XynaOrderServerExtension xo) throws InsufficientRights {
    // check if role has necessary right
    if (xo == null || xo.getCreationRole() == null || !hasScopedRight(xo.getCreationRole(), ScopedRight.WORKSPACE)) {
      throw new InsufficientRights(Arrays.asList(new UserAuthenticationRight(ScopedRight.WORKSPACE.getKey())));
    }

    try {
      if (!XynaFactory.getInstance().getFactoryManagementPortal().hasRight(Rights.READ_MDM.toString(), xo.getCreationRole())) {
        throw new InsufficientRights(Arrays.asList(new UserAuthenticationRight(Rights.READ_MDM.toString())));
      }
    } catch (Exception e) {
      throw new InsufficientRights(Arrays.asList(new UserAuthenticationRight(Rights.READ_MDM.toString())));
    }

    Collection<com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application> applications = revisionManagement.getApplications();
    Map<Long, Workspace> workspaces = revisionManagement.getWorkspaces();
    List<xmcp.RuntimeContext> result = new ArrayList<>(applications.size() + workspaces.size());
    applications.stream()
      .map(this::convert)
      .forEach(result::add);
    if(!SHOW_GLOBAL_APP_MGMT.get()) {
      // hide "GlobalApplicationMgmt"
      for(int i = 0; i < result.size(); i++) {
        RuntimeContext rtc = result.get(i);
        if(rtc.getName().equals("GlobalApplicationMgmt")) {
          result.remove(rtc);
          i--;
        }
      }
    }
    workspaces.forEach((revision, workspace) -> result.add(convert(workspace, revision)));
    Collections.sort(result, (rc1, rc2) -> {
        if(rc1 == null && rc2 == null)
          return 0;
        else if (rc1 == null) {
          return 1;
        } else if (rc2 == null) {
          return -1;
        } else {
          if(rc1 instanceof xmcp.Application && rc2 instanceof xmcp.Workspace) {
            return -1;
          } else if (rc1 instanceof xmcp.Workspace && rc2 instanceof Application) {
            return 1;
          } else if (rc1 instanceof Application && rc2 instanceof Application) {
            Application a1 = (Application)rc1;
            Application a2 = (Application)rc2;
            int r = a1.getName().compareToIgnoreCase(a2.getName());
            if(r == 0)
              return a1.getVersionName().compareToIgnoreCase(a2.getVersionName());
            return r;
          } else if (rc1 instanceof xmcp.Workspace && rc2 instanceof xmcp.Workspace) {
            xmcp.Workspace w1 = (xmcp.Workspace)rc1;
            xmcp.Workspace w2 = (xmcp.Workspace)rc2;
            return w1.getName().compareToIgnoreCase(w2.getName());
          } else {
            return 0;
          }
      }
    });
    return result;
  }
  
  private Application convert(com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application app) {
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
  
  private xmcp.Workspace convert(Workspace workspace, Long revision){
    xmcp.Workspace w = new xmcp.Workspace();
    w.setName(workspace.getName());
    w.setRevision(revision);
    w.setType(workspace.getRuntimeDependencyContextType().name());
    return w;
  }

}
