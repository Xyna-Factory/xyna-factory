/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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
package xfmg.oas.mcp.impl.tools;



import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import xfmg.oas.mcp.impl.ResourceManagement;
import xfmg.xfctrl.datamodel.json.JSONKeyValue;
import xfmg.xfctrl.datamodel.json.JSONObject;
import xfmg.xfctrl.datamodel.json.JSONValue;
import xint.mcp.schema.Content;
import xint.mcp.schema.TextContent;
import xint.mcp.schema.Tool;
import xint.mcp.schema.ToolCallResult;
import xmcp.oas.fman.OasGuiServices;
import xmcp.oas.fman.datatypes.OasApiDatatypeInfo;
import xmcp.tables.datatypes.TableColumn;
import xmcp.tables.datatypes.TableInfo;



public class ListOasEndpointsTool extends Tool {

  private static final long serialVersionUID = 1L;


  public ListOasEndpointsTool() {
    unversionedSetName("listOasEndpoints");
    unversionedSetTitel("List Oas Endpoints");
    unversionedSetDescription("List oas endpoints with their implementation state");
    unversionedSetInputSchema(JSONObject.fromJson(ResourceManagement.loadResource("ListOasEndpointsTool/inputschema.json")));
    unversionedSetOutputSchema(JSONObject.fromJson(ResourceManagement.loadResource("ListOasEndpointsTool/outputschema.json")));
  }


  @Override
  public ToolCallResult call(JSONObject input) {
    RequestFilter filter = readRequestFilter(input);
    TableInfo tableInfo = createTableInfo(filter);
    List<? extends OasApiDatatypeInfo> endpoints = OasGuiServices.getOasApiEndpoints(tableInfo);
    List<Content> content = new ArrayList<>();
    JSONObject.Builder structuredContentBuilder = new JSONObject.Builder();
    List<JSONKeyValue> members = new ArrayList<>();
    JSONValue endpointCountVal = new JSONValue.Builder().type("NUMBER").stringOrNumberValue(String.valueOf(endpoints.size())).instance();
    members.add(new JSONKeyValue.Builder().key("endpointCount").value(endpointCountVal).instance());
    List<JSONValue> endpointsValueList = new ArrayList<>();
    for (OasApiDatatypeInfo endpoint : endpoints) {
      JSONObject obj = createEndpointObj(endpoint);
      content.add(new TextContent.Builder().type("text").text(obj.toJson().getText()).instance());
      endpointsValueList.add(new JSONValue.Builder().type("OBJECT").objectValue(obj).instance());
    }
    structuredContentBuilder.members(members);
    JSONValue endpointsValue = new JSONValue.Builder().type("ARRAY").arrayValue(endpointsValueList).instance();
    members.add(new JSONKeyValue.Builder().key("endpoints").value(endpointsValue).instance());
    return new ToolCallResult.Builder().isError(false).content(content).structuredContent(structuredContentBuilder.instance()).instance();
  }


  private TableInfo createTableInfo(RequestFilter filter) {
    List<TableColumn> columns = new ArrayList<>();
    columns.add(new TableColumn.Builder().path("generatedRtc").filter(filter.generatedRtc).instance());
    columns.add(new TableColumn.Builder().path("implementationRtc").filter(filter.implementationRtc).instance());
    columns.add(new TableColumn.Builder().path("apiDatatype").filter(filter.apiDatatype).instance());
    columns.add(new TableColumn.Builder().path("implementationDatatype").filter(filter.implementationDatatype).instance());
    columns.add(new TableColumn.Builder().path("status").filter(filter.status).instance());
    return new TableInfo.Builder().columns(columns).limit(-1).rootType(OasApiDatatypeInfo.class.getCanonicalName()).version("1.2")
        .instance();
  }


  private JSONObject createEndpointObj(OasApiDatatypeInfo data) {
    List<JSONKeyValue> members = new ArrayList<>();
    members.add(createMemberString("generatedRtc", data.getGeneratedRtc(), "STRING"));
    members.add(createMemberString("implementationRtc", data.getImplementationRtc(), "STRING"));
    members.add(createMemberString("apiDatatype", data.getApiDatatype(), "STRING"));
    members.add(createMemberString("implementationDatatype", data.getImplementationDatatype(), "STRING"));
    members.add(createMemberString("status", data.getStatus(), "STRING"));
    members.add(createMemberString("implementationRtcRevision", String.valueOf(data.getImplementationRtcRevision()), "NUMBER"));
    members.add(createMemberString("generatedRtcRevision", String.valueOf(data.getGeneratedRtcRevision()), "NUMBER"));
    members.add(new JSONKeyValue.Builder().key("implementationRtcIsWorkspace")
        .value(new JSONValue.Builder().type("BOOLEAN").booleanValue(data.getImplementationRtcIsWorkspace()).instance()).instance());
    return new JSONObject.Builder().members(members).instance();
  }

  
  private JSONKeyValue createMemberString(String key, String value, String type) {
    if(value != null && !Objects.equals("null", value)) {
      return new JSONKeyValue.Builder().key(key).value(new JSONValue.Builder().type(type).stringOrNumberValue(value).instance()).instance();
    } else {
      return new JSONKeyValue.Builder().key(key).value(new JSONValue.Builder().type("NULL").instance()).instance();
    }
  }


  private RequestFilter readRequestFilter(JSONObject input) {
    String oasVersionFilter = null;
    String nameFilter = null;
    String apiDatatype = null;
    String implementationDatatype = null;
    String status = null;
    JSONValue filter = input.getMember("filter");
    if (filter != null) {
      if (!Objects.equals("OBJECT", filter.getType())) {
        return RequestFilter.EMPTY;
      }
      JSONObject filterObj = filter.getObjectValue();
      if (filterObj != null) {
        oasVersionFilter = readMemberString(filterObj, "oasVersion");
        nameFilter = readMemberString(filterObj, "name");
        apiDatatype = readMemberString(filterObj, "apiDatatype");
        implementationDatatype = readMemberString(filterObj, "implementationDatatype");
        status = readMemberString(filterObj, "status");
      }

    }
    return new RequestFilter(oasVersionFilter, nameFilter, apiDatatype, implementationDatatype, status);
  }


  private String readMemberString(JSONObject obj, String memberName) {
    JSONValue val = obj.getMember(memberName);
    if (val == null) {
      return null;
    }
    if (!Objects.equals("STRING", val.getType())) {
      return null;
    }
    return val.getStringOrNumberValue();
  }


  private static class RequestFilter {

    private final String generatedRtc;
    private final String implementationRtc;
    private final String apiDatatype;
    private final String implementationDatatype;
    private final String status;

    public static final RequestFilter EMPTY = new RequestFilter(null, null, null, null, null);


    public RequestFilter(String generatedRtc, String implementationRtc, String apiDatatype, String implementationDatatype, String status) {
      this.generatedRtc = generatedRtc;
      this.implementationRtc = implementationRtc;
      this.apiDatatype = apiDatatype;
      this.implementationDatatype = implementationDatatype;
      this.status = status;
    }
  }
}
