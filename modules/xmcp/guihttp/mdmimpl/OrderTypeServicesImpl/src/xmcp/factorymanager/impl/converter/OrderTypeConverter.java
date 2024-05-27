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

package xmcp.factorymanager.impl.converter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeParameter;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeParameter.DestinationValueParameter;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.xpce.parameterinheritance.ParameterInheritanceManagement.ParameterType;
import com.gip.xyna.xprc.xpce.parameterinheritance.rules.InheritanceRule;
import com.gip.xyna.xprc.xpce.planning.Capacity;

import xmcp.RuntimeContext;
import xmcp.factorymanager.DestinationType;
import xmcp.factorymanager.ParameterInheritanceRule;
import xmcp.factorymanager.ordertypes.OrderType;
import xmcp.factorymanager.ordertypes.OrderTypeTableFilter;

public class OrderTypeConverter {
  
  
  private OrderTypeConverter() {

  }
  
  public static OrderType convert(OrdertypeParameter in, OrderTypeTableFilter filter) {
    if(in == null)
      return null;
    OrderType r = new OrderType();
    r.setDocumentation(in.getDocumentation());
    if(in.getExecutionDestinationValue() != null)
      r.setExecutionDestination(filter.getShowPath() ? convert(in.getExecutionDestinationValue(), filter) : convert(in.getExecutionDestinationValue(), filter).substring(in.getExecutionDestinationValue().lastIndexOf('.')) + 1);
    if(in.getMonitoringLevel() != null)
      r.setMonitoringLevel(String.valueOf(in.getMonitoringLevel()));
    r.setEvaluatedMonitoringLevel(in.getMonitoringLevel());
    r.setName(filter.getShowPath() ? in.getOrdertypeName() : in.getOrdertypeName().substring(in.getOrdertypeName().lastIndexOf('.')) + 1);
    // r.setName(in.getOrdertypeName());
    if(in.getPlanningDestinationValue() != null) 
      r.setPlanningDestination(convert(in.getPlanningDestinationValue()));
    r.setPriority(in.getPriority());
    r.setUsedCapacities(usedCapacities(in.getRequiredCapacities()));
    r.setApplication(in.getApplicationName());
    if(in.getCleanupDestinationValue() != null)
      r.setCleanupDestination(convert(in.getCleanupDestinationValue()));
    r.setRuntimeContext(convert(in.getRuntimeContext()));
    if(in.getRequiredCapacities() != null)
      r.setRequiredCapacities(in.getRequiredCapacities().stream().map(cap -> {
        xmcp.factorymanager.ordertypes.Capacity c = new xmcp.factorymanager.ordertypes.Capacity();
        c.setCardinality(cap.getCardinality());
        c.setName(cap.getCapName());
        return c;
      }).collect(Collectors.toList()));
    r.setVersion(in.getVersionName());
    r.setWorkspace(in.getWorkspaceName());
    
    r.setCleanupDestinationIsCustom(in.isCustomCleanupDestinationValue());
    r.setPlanningDestinationIsCustom(in.isCustomPlanningDestinationValue());
    r.setExecutionDestinationIsCustom(in.isCustomExecutionDestinationValue());
    r.setPriorityIsCustom(in.getPriority() != null);
    
    List<InheritanceRule> inheritanceRules = in.getParameterInheritanceRules(ParameterType.MonitoringLevel);
    if(inheritanceRules != null) {
      for (InheritanceRule rule : inheritanceRules) {
        if((rule.getChildFilter() == null || rule.getChildFilter().length() == 0)) {
          // Hierbei handelt es sich um die eigene Precedence und den eigenen MonitoringLevel des OrderTypes
          r.setPrecedence(rule.getPrecedence());
          r.setMonitoringLevel(rule.getUnevaluatedValue());
          r.setEvaluatedMonitoringLevel(rule.getValueAsInt());

        } else {
          r.addToParameterInheritanceRules(new ParameterInheritanceRule(rule.getChildFilter(), rule.getUnevaluatedValue(), rule.getPrecedence()));
        }
      }
    }
    return r;
  }
  
  private static String usedCapacities(Set<Capacity> capacities) {
    StringBuilder sb = new StringBuilder();
    if(capacities == null)
      return sb.toString();
    capacities.forEach(c -> {
      if(sb.length() > 0)
        sb.append(", ");
      sb.append(c.getCapName()).append(" (").append(c.getCardinality()).append(")");
    });
    return sb.toString();
  }
  
  private static DestinationType convert(DestinationValueParameter in) {
    if(in == null)
      return null;
    DestinationType r = new DestinationType();
    r.setName(in.getFullQualifiedName());
    r.setType(in.getDestinationType());
    return r;
  }
  
  private static RuntimeContext convert(com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext in) {
    if(in == null)
      return null;
    if(in instanceof Application) {
      Application application = (Application)in;
      xmcp.Application a = new xmcp.Application();
      a.setName(application.getName());
      try {
        a.setRevision(XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRevision(application));
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new RuntimeException(e.getMessage(), e);
      }
      a.setVersionName(application.getVersionName());
      return a;
    } else if (in instanceof Workspace) {
      Workspace workspace = (Workspace)in;
      xmcp.Workspace w = new xmcp.Workspace();
      w.setName(workspace.getName());
      w.setType(workspace.getType().name());
      try {
        w.setRevision(XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRevision(workspace));
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new RuntimeException(e.getMessage(), e);
      }
      return w;
    }
    return null;
  }
  
}
