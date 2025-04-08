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
package xdev.yang.impl.operation;



import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.w3c.dom.Document;
import org.yangcentral.yangkit.model.api.stmt.Module;
import com.gip.xyna.utils.collections.Pair;

import xdev.yang.impl.Constants;
import xdev.yang.impl.YangCapabilityUtils;
import xdev.yang.impl.YangCapabilityUtils.YangDeviceCapability;
import xmcp.yang.LoadYangAssignmentsData;
import xmcp.yang.OperationAssignmentTableData;



public class DetermineOperationAssignments {

  public static class FilteredModuleData {
    boolean fromCache = false;
    List<Module> filteredModules;
  }
  
  private static final Set<String> primitives = Set.of(Constants.TYPE_LEAF, Constants.TYPE_ANYXML, Constants.TYPE_ANYDATA);

  public List<OperationAssignmentTableData> determineOperationAssignments(LoadYangAssignmentsData data) {
    List<OperationAssignmentTableData> result = new ArrayList<OperationAssignmentTableData>();
    String fqn = data.getFqn();
    String workspaceName = data.getWorkspaceName();
    String operation = data.getOperation();
    Pair<Integer, Document> meta = OperationAssignmentUtils.loadOperationMeta(fqn, workspaceName, operation);
    if(meta == null) {
      return result;
    }
    Document operationMeta = meta.getSecond();
    String rpcName = OperationAssignmentUtils.readRpcName(operationMeta);
    String rpcNamespace = OperationAssignmentUtils.readRpcNamespace(operationMeta);
    if(rpcName == null || rpcNamespace == null) {
      return result;
    }
    String deviceFqn = OperationAssignmentUtils.readDeviceFqn(operationMeta);
    List<YangDeviceCapability> moduleCapabilities = YangCapabilityUtils.loadCapabilities(deviceFqn, workspaceName);
    List<String> supportedFeatures = YangCapabilityUtils.getSupportedFeatureNames(moduleCapabilities);
    FilteredModuleData filtered = getFilteredModules(data, workspaceName, moduleCapabilities);
    result = OperationAssignmentUtils.loadPossibleAssignments(filtered.filteredModules, rpcName, rpcNamespace, data, operationMeta,
                                                              supportedFeatures, filtered.fromCache);
    fillValuesAndWarnings(operationMeta, filtered.filteredModules, result);
    return result;
  }

  
  private FilteredModuleData getFilteredModules(LoadYangAssignmentsData data, String workspaceName, List<YangDeviceCapability> capabilities) {
    FilteredModuleData ret = new FilteredModuleData();
    Optional<List<Module>> opt = OperationCache.getInstance().get(data);
    if (opt.isPresent()) {
      ret.filteredModules = opt.get();
      ret.fromCache = true;
    } else {
      List<ModuleGroup> groups = OperationAssignmentUtils.loadModules(workspaceName);
      ret.filteredModules = new ModuleFilterTools().filterAndReload(groups, capabilities);
      OperationCache.getInstance().put(data, ret.filteredModules);
    }
    return ret;
  }

  private void fillValuesAndWarnings(Document meta, List<Module> modules, List<OperationAssignmentTableData> entries) {
    List<OperationMapping> mappings = OperationMapping.loadMappings(meta);
    for (OperationAssignmentTableData entry : entries) {
      String totalYangPath = entry.getLoadYangAssignmentsData().getTotalYangPath();
      String totalNamespaces = entry.getLoadYangAssignmentsData().getTotalNamespaces();
      String totalKeywords = entry.getLoadYangAssignmentsData().getTotalKeywords();
      List<MappingPathElement> entryPath = OperationMapping.createPathList(totalYangPath, totalNamespaces, totalKeywords);
      fillValueAndOptionalWarning(entry, entryPath, mappings);
    }
  }


  private void fillValueAndOptionalWarning(OperationAssignmentTableData entry, List<MappingPathElement> entryPath, List<OperationMapping> mappings) {
    if (primitives.contains(entry.getType())) {
      String value = getPrimitiveValue(entryPath, mappings);
      entry.unversionedSetValue(value);
    }
    else {
      handleValueAndOptionalWarningOfContainer(entryPath, mappings, entry);
    }
  }


  private String getPrimitiveValue(List<MappingPathElement> entryPath, List<OperationMapping> mappings) {
    for (OperationMapping mapping : mappings) {
      if (MappingPathElement.compareLists(entryPath, mapping.createPathList()) == 0) {
        return mapping.getValue();
      }
    }
    return "";
  }


  private void handleValueAndOptionalWarningOfContainer(List<MappingPathElement> entryPath, List<OperationMapping> mappings,
                                                        OperationAssignmentTableData entry) {
    int subAssignments = 0;
    String elemtype = entryPath.get(entryPath.size() - 1).getKeyword();    
    boolean isChoice = Constants.TYPE_CHOICE.equals(elemtype);
    Set<String> caseSet = isChoice ? new HashSet<>() : null;

    for (OperationMapping mapping : mappings) {
      List<MappingPathElement> mappingPath = mapping.createPathList();
      if (MappingPathElement.isMoreSpecificPath(entryPath, mappingPath)) {
        subAssignments++;
        if (isChoice && (mappingPath.size() > entryPath.size())) {
          MappingPathElement childElem = mappingPath.get(entryPath.size());
          caseSet.add(childElem.getYangPath());
        }
      }
    }
    if ((caseSet != null) && (caseSet.size() > 1)) {
      entry.getLoadYangAssignmentsData().unversionedSetWarning("WARNING: Choice has assignments in more than one case.");
    }

    String retval = "";
    if (subAssignments > 0) {
      retval = String.format("contains %s assignment%s", subAssignments, subAssignments == 1 ? "" : "s");
    }
    entry.unversionedSetValue(retval);
  }

}
