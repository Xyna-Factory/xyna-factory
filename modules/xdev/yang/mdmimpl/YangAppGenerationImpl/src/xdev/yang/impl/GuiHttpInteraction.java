/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.trigger.RunnableForFilterAccess;
import com.gip.xyna.xprc.XynaOrderServerExtension;

import xact.http.URLPath;
import xact.http.enums.httpmethods.DELETE;
import xact.http.enums.httpmethods.GET;
import xact.http.enums.httpmethods.HTTPMethod;
import xact.http.enums.httpmethods.POST;
import xact.http.enums.httpmethods.PUT;
import xmcp.processmodeller.datatypes.Area;
import xmcp.processmodeller.datatypes.Data;
import xmcp.processmodeller.datatypes.Item;
import xmcp.processmodeller.datatypes.ServiceGroup;
import xmcp.processmodeller.datatypes.VariableArea;
import xmcp.processmodeller.datatypes.datatypemodeller.MemberMethodArea;
import xmcp.processmodeller.datatypes.datatypemodeller.Method;
import xmcp.processmodeller.datatypes.response.GetServiceGroupResponse;
import xmcp.processmodeller.datatypes.response.UpdateXMOMItemResponse;

public class GuiHttpInteraction {

  public static POST METHOD_POST = new POST();
  public static GET METHOD_GET = new GET();
  public static PUT METHOD_PUT = new PUT();
  public static DELETE METHOD_DELETE = new DELETE();
  
  public static List<VariableData> loadVarNames(GetServiceGroupResponse response, Integer operationIndex) {
    List<VariableData> result = new ArrayList<VariableData>();
    List<? extends Area> areas = response.getXmomItem().getAreas();
    MemberMethodArea area = ((MemberMethodArea) findAreaByName(areas, "methodsArea"));
    Method method = (Method) area.getItems().get(operationIndex);
    areas = method.getAreas();
    VariableArea varArea = (VariableArea)findAreaByName(areas, "input");
    if(varArea == null || varArea.getItems() == null) {
      return result;
    }
    for (Item item : varArea.getItems()) {
      Data inputVarData = (Data) item;
      result.add(new VariableData(inputVarData.getFqn(), inputVarData.getName()));
    }

    return result;
  }
  
  public static int loadServiceCount(UpdateXMOMItemResponse json) {
    List<? extends Area> areas = ((ServiceGroup)json.getUpdates().get(0)).getAreas();
    MemberMethodArea area = ((MemberMethodArea) findAreaByName(areas, "methodsArea"));
    return area.getItems().size();
  }

  public static String loadServiceId(GetServiceGroupResponse response, String operationName) {
    List<? extends Area> areas = response.getXmomItem().getAreas();
    return loadServiceId(areas, operationName);
  }
  
  public static String loadServiceId(UpdateXMOMItemResponse response, String operationName) {
    List<? extends Area> areas = ((ServiceGroup)response.getUpdates().get(0)).getAreas();
    return loadServiceId(areas, operationName);
  }
  
  private static String loadServiceId(List<? extends Area> areas, String operationName) {
    MemberMethodArea area = ((MemberMethodArea) findAreaByName(areas, "methodsArea"));
    Item method = findMethodByName(area.getItems(), operationName);
    return String.valueOf(area.getItems().indexOf(method));
  }

  public static String loadServiceName(UpdateXMOMItemResponse json, int index) {
    List<? extends Area> areas = ((ServiceGroup)json.getUpdates().get(0)).getAreas();
    MemberMethodArea area = ((MemberMethodArea) findAreaByName(areas, "methodsArea"));
    Method method = (Method)area.getItems().get(index);
    return method.getName();
  }

  private static Item findMethodByName(List<? extends Item> items, String name) {
    for (Item item : items) {
      if (item instanceof Method && ((Method) item).getName().equals(name)) {
        return item;
      }
    }
    return null;
  }


  public static Object openDatatype(XynaOrderServerExtension order, String fqn, String workspaceName, String type) {
    RunnableForFilterAccess runnable = order.getRunnableForFilterAccess("H5XdevFilter");
    String path = fqn.substring(0, fqn.lastIndexOf("."));
    String label = fqn.substring(fqn.lastIndexOf(".") + 1);
    String workspaceNameEscaped = GuiHttpInteraction.urlEncode(workspaceName);
    String endpoint = "/runtimeContext/" + workspaceNameEscaped + "/xmom/" + type + "/" + path + "/" + label;
    URLPath url = new URLPath(endpoint, null, null);
    return executeRunnable(runnable, url, new GET(), null, "could not open datatype");
  }
  
  
  public static void saveDatatype(String path, String targetPath, String label, String workspace, String viewtype, XynaOrderServerExtension order) {
    RunnableForFilterAccess runnable = order.getRunnableForFilterAccess("H5XdevFilter");
    String workspaceNameEscaped = urlEncode(workspace);
    String baseUrl = "/runtimeContext/" + workspaceNameEscaped + "/xmom/" + viewtype + "/" + path + "/" + label;
    URLPath url = new URLPath(baseUrl + "/save", null, null);
    HTTPMethod method = new POST();
    String payload = "{\"force\":false,\"revision\":2,\"path\":\"" + targetPath + "\",\"label\":\"" + label + "\"}";
    executeRunnable(runnable, url, method, payload, "Could not save datatype.");
    
    //deploy
    baseUrl = "/runtimeContext/" + workspaceNameEscaped + "/xmom/" + viewtype + "/" + targetPath + "/" + label;
    url = new URLPath(baseUrl + "/deploy", null, null);
    payload = "{\"revision\":3}";
    executeRunnable(runnable, url, method, payload, "Could not deploy datatype.");
    
    //close
    url = new URLPath(baseUrl + "/close", null, null);
    payload = "{\"force\":false,\"revision\":4}";
    executeRunnable(runnable, url, method, payload, "Could not close datatype.");
  }
  
  public static void setMetaTag(String path, String label, String workspace, String operation, String tag, XynaOrderServerExtension order) {
    RunnableForFilterAccess runnable = order.getRunnableForFilterAccess("H5XdevFilter");
    String workspaceNameEscaped = urlEncode(workspace);
    String baseUrl = "/runtimeContext/" + workspaceNameEscaped + "/xmom/servicegroups/" + path + "/" + label;
    URLPath url = new URLPath(baseUrl + "/services/" + operation + "/meta", null, null);
    String payload = "{\"$meta\":{\"fqn\":\"xmcp.processmodeller.datatypes.request.MetaTagRequest\"},"
        + "\"metaTag\":{\"$meta\":{\"fqn\":\"xmcp.processmodeller.datatypes.MetaTag\"},\"deletable\":true,\"tag\":\"" + tag + "\"}}";
    executeRunnable(runnable, url, GuiHttpInteraction.METHOD_PUT, payload, "Could not add meta tag to service.");
  }
  
  private static Area findAreaByName(List<? extends Area> areas, String name) {
    for(Area area: areas) {
      if(area.getName().equals(name)) {
        return area;
      }
    }
    return null;
  }
  
  public static Object executeRunnable(RunnableForFilterAccess runnable, URLPath url, HTTPMethod method, String payload, String msg) {
    try {
      return runnable.execute(url, method, payload);
    } catch (XynaException e) {
      throw new RuntimeException(msg, e);
    }
  }

  
  public static String urlEncode(String in) {
    return URLEncoder.encode(in, Charset.forName("UTF-8"));
  }
  
  public static class VariableData {
    private final String fqn;
    private final String name;
    
    public VariableData(String fqn, String name) {
      this.fqn = fqn;
      this.name = name;
    }
    
    public String getFqn() {
      return fqn;
    }
    
    public String getName() {
      return name;
    }
  }
}
