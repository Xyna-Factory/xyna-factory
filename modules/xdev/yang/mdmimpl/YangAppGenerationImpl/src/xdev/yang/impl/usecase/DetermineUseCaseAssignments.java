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
package xdev.yang.impl.usecase;



import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Document;
import com.gip.xyna.utils.collections.Pair;

import xdev.yang.impl.Constants;
import xdev.yang.impl.YangCapabilityUtils;

import org.yangcentral.yangkit.model.api.stmt.Module;

import xmcp.yang.LoadYangAssignmentsData;
import xmcp.yang.UseCaseAssignmentTableData;



public class DetermineUseCaseAssignments {

  private static final Set<String> primitives = Set.of(Constants.TYPE_LEAF, Constants.TYPE_ANYXML);

  public List<UseCaseAssignmentTableData> determineUseCaseAssignments(LoadYangAssignmentsData data) {
    List<UseCaseAssignmentTableData> result = new ArrayList<UseCaseAssignmentTableData>();
    String fqn = data.getFqn();
    String workspaceName = data.getWorkspaceName();
    String usecase = data.getUsecase();
    Pair<Integer, Document> meta = UseCaseAssignmentUtils.loadOperationMeta(fqn, workspaceName, usecase);
    if(meta == null) {
      return result;
    }
    Document usecaseMeta = meta.getSecond();
    String rpcName = UseCaseAssignmentUtils.readRpcName(usecaseMeta);
    String rpcNamespace = UseCaseAssignmentUtils.readRpcNamespace(usecaseMeta);
    if(rpcName == null || rpcNamespace == null) {
      return result;
    }
    
    List<Module> modules = UseCaseAssignmentUtils.loadModules(workspaceName);
    String deviceFqn = UseCaseAssignmentUtils.readDeviceFqn(usecaseMeta);
    List<String> moduleCapabilities = YangCapabilityUtils.loadCapabilities(deviceFqn, workspaceName);
    modules = YangCapabilityUtils.filterModules(modules, moduleCapabilities);
    result = UseCaseAssignmentUtils.loadPossibleAssignments(modules, rpcName, rpcNamespace, data, usecaseMeta);
    fillValues(usecaseMeta, modules, result);

    return result;
  }


  private void fillValues(Document meta, List<Module> modules, List<UseCaseAssignmentTableData> entries) {
    List<UseCaseMapping> mappings = UseCaseMapping.loadMappings(meta);
    for (UseCaseAssignmentTableData entry : entries) {
      String totalYangPath = entry.getLoadYangAssignmentsData().getTotalYangPath();
      String totalNamespaces = entry.getLoadYangAssignmentsData().getTotalNamespaces();
      String totalKeywords = entry.getLoadYangAssignmentsData().getTotalKeywords();
      List<MappingPathElement> entryPath = UseCaseMapping.createPathList(totalYangPath, totalNamespaces, totalKeywords);
      fillValue(entry, entryPath, mappings);
    }
  }
  

  private void fillValue(UseCaseAssignmentTableData entry, List<MappingPathElement> entryPath, List<UseCaseMapping> mappings) {
    boolean isPrimitive = primitives.contains(entry.getType());
    String value = isPrimitive ? getPrimitiveValue(entryPath, mappings) : getContainerValue(entryPath, mappings);
    entry.unversionedSetValue(value);
  }
  

  private String getPrimitiveValue(List<MappingPathElement> entryPath, List<UseCaseMapping> mappings) {
    for (UseCaseMapping mapping : mappings) {
      if (MappingPathElement.compareLists(entryPath, mapping.createPathList()) == 0) {
        return mapping.getValue();
      }
    }
    return "";
  }


  private String getContainerValue( List<MappingPathElement> entryPath, List<UseCaseMapping> mappings) {
    int subAssignments = 0;
    for (UseCaseMapping mapping : mappings) {
      if (MappingPathElement.isMoreSpecificPath(entryPath, mapping.createPathList())) {
        subAssignments++;
      }
    }

    if (subAssignments > 0) {
      return String.format("contains %s assignment%s", subAssignments, subAssignments == 1 ? "" : "s");
    }
    
    return "";
  }
}
