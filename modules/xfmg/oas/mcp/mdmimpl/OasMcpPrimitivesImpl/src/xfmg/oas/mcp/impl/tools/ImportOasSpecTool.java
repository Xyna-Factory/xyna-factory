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



import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;

import base.Text;
import xact.templates.Document;
import xact.templates.JSON;
import xfmg.oas.mcp.impl.ResourceManagement;
import xfmg.xfctrl.datamodel.json.JSONKeyValue;
import xfmg.xfctrl.datamodel.json.JSONObject;
import xfmg.xfctrl.datamodel.json.JSONValue;
import xfmg.xfctrl.filemgmt.FileManagement;
import xfmg.xfctrl.filemgmt.ManagedFileId;
import xint.mcp.schema.Content;
import xint.mcp.schema.TextContent;
import xint.mcp.schema.Tool;
import xint.mcp.schema.ToolCallResult;



public class ImportOasSpecTool extends Tool {

  private static final long serialVersionUID = 1L;
  private final Long revision;


  public ImportOasSpecTool(Long revision) {
    this.revision = revision;
    unversionedSetName("importOasSpec");
    unversionedSetTitel("Import Oas Specification");
    unversionedSetDescription("Upload an Oas Specification and generate Oas applications");
    unversionedSetInputSchema(JSONObject.fromJson(ResourceManagement.loadResource("ImportOasSpecTool/inputschema.json")));
    unversionedSetOutputSchema(JSONObject.fromJson(ResourceManagement.loadResource("ImportOasSpecTool/outputschema.json")));
  }


  @Override
  public ToolCallResult call(JSONObject input) {

    JSONValue specVal = input.getMember("spec");
    if (specVal == null) {
      return buildErrorResult("Error: No spec provided");
    }


    Document specDoc = readSpecString(specVal);

    ManagedFileId id = null;
    try {
      String suffix = specDoc.getDocumentType() instanceof JSON ? ".json" : ".yaml";
      Text uuid = new Text.Builder().text(UUID.randomUUID().toString() + suffix).instance();
      id = FileManagement.storeDocument(specDoc, uuid, new Text.Builder().text("oasmcpimport").instance());
    } catch (Exception e) {
      return buildErrorResult("Error: Could not save spec");
    }

    boolean generateProvider = false;
    boolean generateClient = false;

    JSONValue options = input.getMember("options");
    if (options != null && Objects.equals(options.getType(), "OBJECT")) {
      JSONObject optionsObj = options.getObjectValue();
      JSONValue providerValue = optionsObj.getMember("createProvider");
      if (providerValue != null && Objects.equals(providerValue.getType(), "BOOLEAN")) {
        generateProvider = providerValue.getBooleanValue();
      }
      JSONValue clientValue = optionsObj.getMember("createClient");
      if (clientValue != null && Objects.equals(clientValue.getType(), "BOOLEAN")) {
        generateClient = clientValue.getBooleanValue();
      }
    }

    String appString = "Datamodel Application";
    if (generateProvider && generateClient) {
      appString = "OAS Applications";
    } else if (generateProvider) {
      appString = "Provider Application";
    } else if (generateClient) {
      appString = "Client Application";
    }

    GeneralXynaObject gxo = new Container(id, new Text.Builder().text(appString).instance(), new Text());
    RuntimeContext rc;
    try {
      rc = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRuntimeContext(revision);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      return buildErrorResult("Error: Could not determine runtime context of oas mcp runtime context");
    }
    DestinationKey dk = new DestinationKey("xmcp.oas.fman.ApplicationImport", rc);
    XynaOrderCreationParameter xocp = new XynaOrderCreationParameter(dk, gxo);

    try {
      XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().startOrderSynchronously(xocp);
    } catch (Exception e) {
      return buildErrorResult("Error: Could not import spec");
    }

    Content resultStringContent = new TextContent.Builder().text("text").text("Ok").instance();
    JSONValue val = new JSONValue.Builder().type("STRING").stringOrNumberValue("Ok").instance();
    List<JSONKeyValue> members = List.of(new JSONKeyValue.Builder().key("result").value(val).instance());
    JSONObject resultObj = new JSONObject.Builder().members(members).instance();
    return new ToolCallResult.Builder().isError(false).structuredContent(resultObj).content(List.of(resultStringContent)).instance();
  }


  private ToolCallResult buildErrorResult(String message) {
    Content resultStringContent = new TextContent.Builder().text("text").text(message).instance();
    JSONValue val = new JSONValue.Builder().type("STRING").stringOrNumberValue(message).instance();
    List<JSONKeyValue> members = List.of(new JSONKeyValue.Builder().key("result").value(val).instance());
    JSONObject resultObj = new JSONObject.Builder().members(members).instance();
    return new ToolCallResult.Builder().isError(true).structuredContent(resultObj).content(List.of(resultStringContent)).instance();
  }


  private Document readSpecString(JSONValue specVal) {
    if (Objects.equals(specVal.getType(), "OBJECT")) {
      return specVal.getObjectValue().toJson();
    }
    return new Document.Builder().text(specVal.getStringOrNumberValue()).instance();
  }

}
