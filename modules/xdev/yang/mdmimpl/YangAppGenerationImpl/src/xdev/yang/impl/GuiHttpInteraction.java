/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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
package xdev.yang.impl;

import java.util.ArrayList;
import java.util.List;

import xmcp.processmodeller.datatypes.Area;
import xmcp.processmodeller.datatypes.Data;
import xmcp.processmodeller.datatypes.Item;
import xmcp.processmodeller.datatypes.VariableArea;
import xmcp.processmodeller.datatypes.datatypemodeller.MemberMethodArea;
import xmcp.processmodeller.datatypes.datatypemodeller.Method;
import xmcp.processmodeller.datatypes.response.GetDataTypeResponse;

public class GuiHttpInteraction {

  public static List<String> loadVarNames(GetDataTypeResponse response, Integer operationIndex) {
    List<String> result = new ArrayList<String>();
    List<? extends Area> areas = response.getXmomItem().getAreas();
    MemberMethodArea area = ((MemberMethodArea) findAreaByName(areas, "memberMethods"));
    Method method = (Method) area.getItems().get(operationIndex);
    areas = method.getAreas();
    VariableArea varArea = (VariableArea)findAreaByName(areas, "input");
    for (Item item : varArea.getItems()) {
      Data inputVarData = (Data) item;
      result.add(inputVarData.getName());
    }

    return result;
  }
  
  private static Area findAreaByName(List<? extends Area> areas, String name) {
    for(Area area: areas) {
      if(area.getName().equals(name)) {
        return area;
      }
    }
    return null;
  }
}
