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
package xfmg.oas.mcp.impl.resources;



import java.util.ArrayList;
import java.util.List;

import xfmg.oas.generation.ApplicationGeneration;
import xint.mcp.schema.ReadResourceRequestParams;
import xint.mcp.schema.ReadResourceResult;
import xint.mcp.schema.Resource;
import xint.mcp.schema.ResourceContent;
import xint.mcp.schema.TextResourceContent;
import xmcp.oas.fman.storables.OAS_ImportHistory;
import xmcp.tables.datatypes.TableColumn;
import xmcp.tables.datatypes.TableInfo;



public class ImportHistoryResource extends Resource {

  private static final long serialVersionUID = 1L;

  private final OAS_ImportHistory history;


  public ImportHistoryResource(OAS_ImportHistory history) {
    this.history = history;
    unversionedSetName(String.valueOf(history.getUniqueIdentifier()) + "_" + history.getFileName());
    unversionedSetTitle(history.getFileName() + " " + history.getDate0());
    unversionedSetUri(createUri(history));
    unversionedSetDescription(createDescription(history));
    unversionedSetMimeType(deriveMimeType(history.getFileName()));
  }


  private String createUri(OAS_ImportHistory history) {
    return "xynamcp://oas/history/" + history.getUniqueIdentifier();
  }


  private String deriveMimeType(String fileName) {
    if (fileName.endsWith(".json")) {
      return "application/json";
    }
    return "text/plain";
  }


  private String createDescription(OAS_ImportHistory history) {
    StringBuilder sb = new StringBuilder();
    sb.append("FileName: ").append(history.getFileName()).append("\n");
    sb.append("Imported on: ").append(history.getDate0()).append("\n");
    sb.append("Import type: ").append(history.getType()).append("\n");
    sb.append("Import status: ").append(history.getImportStatus()).append("\n");
    if (history.getErrorMessage() != null && !history.getErrorMessage().isBlank()) {
      sb.append("Error: ").append(history.getErrorMessage()).append("\n");
    }
    sb.append("Import RuntimeContext: ").append(history.getImportRtc());
    return sb.toString();
  }


  @Override
  public ReadResourceResult read(ReadResourceRequestParams request) {
    List<ResourceContent> contents = new ArrayList<>();
    OAS_ImportHistory data = ApplicationGeneration.queryOasImportHistoryDetails(history);
    contents.add(new TextResourceContent.Builder().uri(createUri(history)).mimeType(deriveMimeType(history.getFileName()))
        .text(data.getSpecificationFile()).instance());
    return new ReadResourceResult.Builder().contents(contents).instance();
  }


  public static List<ImportHistoryResource> listResources() {
    List<TableColumn> columns = new ArrayList<>();
    columns.add(new TableColumn.Builder().path("fileName").instance());
    columns.add(new TableColumn.Builder().path("type").instance());
    columns.add(new TableColumn.Builder().path("date0").instance());
    columns.add(new TableColumn.Builder().path("importStatus").instance());
    columns.add(new TableColumn.Builder().path("importRtc").instance());
    TableInfo tableInfo = new TableInfo.Builder().version("1.2").columns(columns).instance();
    List<? extends OAS_ImportHistory> data = ApplicationGeneration.queryOasImportHistory(tableInfo);
    List<ImportHistoryResource> result = new ArrayList<>();
    for (OAS_ImportHistory entry : data) {
      result.add(new ImportHistoryResource(entry));
    }
    return result;
  }
}
