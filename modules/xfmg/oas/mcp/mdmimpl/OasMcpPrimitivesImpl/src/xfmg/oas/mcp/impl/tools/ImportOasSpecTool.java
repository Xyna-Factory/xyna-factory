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

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;

import base.Text;
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

  private static final Logger logger = CentralFactoryLogging.getLogger(ImportOasSpecTool.class);

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
    //TODO: allow yaml
    if (specVal == null || !Objects.equals("OBJECT", specVal.getType())) {
      Content resultStringContent = new TextContent.Builder().text("text").text("Error: No spec provided").instance();
      JSONObject resultObj = new JSONObject.Builder().members(List.of(new JSONKeyValue.Builder().key("result")
          .value(new JSONValue.Builder().type("STRING").stringOrNumberValue("Error: No spec provided").instance()).instance())).instance();
      return new ToolCallResult.Builder().isError(true).structuredContent(resultObj).content(List.of(resultStringContent)).instance();
    }

    JSONObject spec = specVal.getObjectValue();
    ManagedFileId id = null;
    try {
      id = FileManagement.storeDocument(spec.toJson(), new Text.Builder().text(UUID.randomUUID().toString()).instance(),
                                        new Text.Builder().text("oasmcpimport").instance());
    } catch (Exception e) {
      Content resultStringContent = new TextContent.Builder().text("text").text("Error: Could not save spec").instance();
      JSONObject resultObj = new JSONObject.Builder().members(List.of(new JSONKeyValue.Builder().key("result")
          .value(new JSONValue.Builder().type("STRING").stringOrNumberValue("Error: Could not save spec").instance()).instance())).instance();
      return new ToolCallResult.Builder().isError(true).structuredContent(resultObj).content(List.of(resultStringContent)).instance();
    }


    //TODO: only import some of the apps
    GeneralXynaObject gxo = new Container(id, new Text.Builder().text("OAS Applications").instance(), new Text());
    RuntimeContext rc;
    try {
      rc = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRuntimeContext(revision);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      Content resultStringContent = new TextContent.Builder().text("text").text("Error: Could not determine runtime context").instance();
      JSONObject resultObj = new JSONObject.Builder()
          .members(List.of(new JSONKeyValue.Builder().key("result")
              .value(new JSONValue.Builder().type("STRING").stringOrNumberValue("Error: Could not determine runtime context").instance()).instance()))
          .instance();
      return new ToolCallResult.Builder().isError(true).structuredContent(resultObj).content(List.of(resultStringContent)).instance();

    }
    DestinationKey dk = new DestinationKey("xmcp.oas.fman.ApplicationImport", rc);
    XynaOrderCreationParameter xocp = new XynaOrderCreationParameter(dk, gxo);

    GeneralXynaObject obj;
    try {
      obj = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().startOrderSynchronously(xocp);
    } catch (Exception e) {
      Content resultStringContent = new TextContent.Builder().text("text").text("Error: Could not save spec").instance();
      JSONObject resultObj = new JSONObject.Builder().members(List.of(new JSONKeyValue.Builder().key("result")
          .value(new JSONValue.Builder().type("STRING").stringOrNumberValue("Error: Could not save spec").instance()).instance())).instance();
      return new ToolCallResult.Builder().isError(true).structuredContent(resultObj).content(List.of(resultStringContent)).instance();
    }

    //TODO: check obj
    if (logger.isInfoEnabled()) {
      logger.info("Imported oas from mcp tool. result: " + obj);
    }

    Content resultStringContent = new TextContent.Builder().text("text").text("Ok").instance();
    JSONObject resultObj = new JSONObject.Builder()
        .members(List
            .of(new JSONKeyValue.Builder().key("result").value(new JSONValue.Builder().type("STRING").stringOrNumberValue("Ok").instance()).instance()))
        .instance();
    return new ToolCallResult.Builder().isError(false).structuredContent(resultObj).content(List.of(resultStringContent)).instance();
  }

}
