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

package com.gip.xyna.xact.filter.util;

import java.util.List;

import com.gip.xyna.xact.filter.session.XMOMGuiReply;

import xmcp.processmodeller.datatypes.Area;
import xmcp.processmodeller.datatypes.ContainerArea;
import xmcp.processmodeller.datatypes.Formula;
import xmcp.processmodeller.datatypes.Item;
import xmcp.processmodeller.datatypes.ModellingItem;
import xmcp.processmodeller.datatypes.OrderInputSourceArea;
import xmcp.processmodeller.datatypes.Variable;
import xmcp.processmodeller.datatypes.response.GetXMOMItemResponse;
import xmcp.processmonitor.datatypes.response.GetAuditResponse;

public class ReadonlyUtil {
  
  
  private ReadonlyUtil() {
    
  }
  
  public static void setReadonlyRecursive(XMOMGuiReply reply) {    
    if(reply.getXynaObject() instanceof GetXMOMItemResponse) {
      changeGetXMOMItemResponse((GetXMOMItemResponse) reply.getXynaObject());
    } else if(reply.getXynaObject() instanceof GetAuditResponse) {
      changeGetAuditResponse((GetAuditResponse)reply.getXynaObject());
    }
  }
  
  private static void changeGetAuditResponse(GetAuditResponse getAuditResponse) {
    if(getAuditResponse.getWorkflow() == null || getAuditResponse.getWorkflow().getAreas() == null) {
      return;
    }
    getAuditResponse.getWorkflow().setReadonly(true);
    getAuditResponse.getWorkflow().getAreas().forEach(ReadonlyUtil::setReadonly);
  }
  
  private static void changeGetXMOMItemResponse(GetXMOMItemResponse getXMOMItemResponse) {
    if(getXMOMItemResponse.getXmomItem() == null || !getXMOMItemResponse.getXmomItem().getReadonly()) {
      return;
    }
    if(getXMOMItemResponse.getXmomItem().getAreas() != null) {
      getXMOMItemResponse.getXmomItem().getAreas().forEach(ReadonlyUtil::setReadonly);
    }
  }
  
  private static void setAreasReadonly(List<? extends Area> areas) {
    if(areas != null) {
      areas.forEach(ReadonlyUtil::setReadonly);
    }
  }
  
  private static void setItemsReadonly(List<? extends Item> items) {
    if(items != null) {
      items.forEach(ReadonlyUtil::setReadonly);
    }
  }
  
  private static void setVariablesReadonly(List<? extends Variable> variables) {
    if(variables != null) {
      variables.forEach(ReadonlyUtil::setReadonly);
    }
  }
  
  private static void setReadonly(Variable variable) {
    variable.setReadonly(true);
    setAreasReadonly(variable.getAreas());
  }
  
  private static void setReadonly(Area area) {
    area.setReadonly(true);
    if(area instanceof ContainerArea) {
      ContainerArea containerArea = (ContainerArea)area;
      setItemsReadonly(containerArea.getItems());
    }
    if (area instanceof OrderInputSourceArea) {
      OrderInputSourceArea inputSourceArea = (OrderInputSourceArea)area;
      if(inputSourceArea.getUsedInputSource() != null) {
        inputSourceArea.getUsedInputSource().setReadonly(true);
      }
    }
  }
  
  private static void setReadonly(Item item) {
    item.setReadonly(true);
    if(item instanceof ModellingItem) {
      setReadonly((ModellingItem)item);
    } else if(item instanceof Formula) {
      setReadonly((Formula)item);
    }
  }
  
  private static void setReadonly(ModellingItem item) {
    setAreasReadonly(item.getAreas());
  }
  
  private static void setReadonly(Formula formula) {
    setVariablesReadonly(formula.getInput());
    setVariablesReadonly(formula.getOutput());
    setVariablesReadonly(formula.getThrown());
  }
}
