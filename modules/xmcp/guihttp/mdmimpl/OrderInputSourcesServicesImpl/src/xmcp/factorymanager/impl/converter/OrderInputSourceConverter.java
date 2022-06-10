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

package xmcp.factorymanager.impl.converter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.storables.OrderInputSourceStorable;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeManagement;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeParameter;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;

import xmcp.factorymanager.orderinputsources.OrderInputSource;
import xmcp.factorymanager.orderinputsources.Parameter;
import xmcp.factorymanager.orderinputsources.SourceType;
import xmcp.factorymanager.shared.OrderType;

public class OrderInputSourceConverter {
  
  private static final RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
  private static final OrdertypeManagement ordertypeManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderTypeManagement();
  
  private OrderInputSourceConverter() {
    
  }
  
  public static OrderInputSource convert(OrderInputSourceStorable in) {
    OrderInputSource ois = new OrderInputSource();
    if(in.getOrderType() != null) {
      OrdertypeParameter ordertypeParameter = null;
      try {
        ordertypeParameter = ordertypeManagement.getOrdertype(in.getOrderType(), revisionManagement.getRuntimeContext(in.getRevision()));
      } catch (PersistenceLayerException | XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        
      }
      if(ordertypeParameter != null) {
        OrderType orderType = new OrderType(
                ordertypeParameter.getOrdertypeName(), 
                ordertypeParameter.getExecutionDestinationValue() != null ? ordertypeParameter.getExecutionDestinationValue().getFullQualifiedName() : "");
        ois.setOrderType(orderType);
      }
    }
    ois.setId(in.getId());
    ois.setRevision(in.getRevision());
    ois.setApplicationName(in.getApplicationName());
    ois.setDocumentation(in.getDocumentation());
    ois.setName(in.getName());
    ois.setParameter(convertParameterMap(in.getParameters()));
    ois.setReferencedInputSourceCount(in.getReferencedInputSourceCount());
    ois.setState(in.getState());
    ois.setSourceType(createSourceTypeFromName(in.getType()));
    ois.setVersionName(in.getVersionName());
    ois.setWorkspaceName(in.getWorkspaceName());
    return ois;
  }
  
  public static SourceType createSourceTypeFromName(String name) {
    return new SourceType(convertTypeName(name), convertTypeLabel(name));
  }
  

  public static String convertTypeName(String backendType) {
    /*
     * ConstantInputSource --> ConstantInputSource
     * WorkflowInputSourceType --> WorkflowInputSource
     * XTFInputSourceType --> XTFInputSource
     */
    if(backendType == null)
      return null;
    return backendType.replace("InputSourceType", "InputSource");
  }
  
  public static String convertTypeLabel(String backendType) {
    if(backendType == null)
      return null;
    String result = backendType.replace("InputSourceType", "");
    result = result.replace("InputSource", "");
    if("XTF".equals(result))
      return "Xyna Test Factory";
    return result;
  }
  
  public static List<Parameter> convertParameterMap(Map<String, String> backendParameterMap) {
    if(backendParameterMap == null)
      return Collections.emptyList();
    List<Parameter> result = new ArrayList<>(backendParameterMap.size());
    backendParameterMap.forEach((key, value) -> {
      Parameter p = new Parameter();
      p.setKey(key);
      p.setValue(value);
      result.add(p);
    });
    return result;
  }
  
}
