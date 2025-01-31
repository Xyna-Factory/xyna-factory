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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.yangcentral.yangkit.model.api.stmt.Module;

import com.gip.xyna.utils.collections.Pair;

import xdev.yang.impl.Constants;
import xdev.yang.impl.YangCapabilityUtils;
import xdev.yang.impl.YangCapabilityUtils.YangDeviceCapability;
import xmcp.yang.LoadYangAssignmentsData;
import xmcp.yang.UseCaseAssignmentTableData;



public class DetermineUseCaseAssignments {

  private static Logger _logger = Logger.getLogger(DetermineUseCaseAssignments.class);

  private static final Set<String> primitives = Set.of(Constants.TYPE_LEAF, Constants.TYPE_ANYXML, Constants.TYPE_ANYDATA);

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
    List<YangDeviceCapability> moduleCapabilities = YangCapabilityUtils.loadCapabilities(deviceFqn, workspaceName);
    List<String> supportedFeatures = YangCapabilityUtils.getSupportedFeatureNames(modules, moduleCapabilities);
    modules = YangCapabilityUtils.filterModules(modules, moduleCapabilities);
    result = UseCaseAssignmentUtils.loadPossibleAssignments(modules, rpcName, rpcNamespace, data, usecaseMeta, supportedFeatures);
    fillValuesAndWarnings(usecaseMeta, modules, result);

    return result;
  }


  private void fillValuesAndWarnings(Document meta, List<Module> modules, List<UseCaseAssignmentTableData> entries) {
    List<UseCaseMapping> mappings = UseCaseMapping.loadMappings(meta);
    for (UseCaseAssignmentTableData entry : entries) {
      String totalYangPath = entry.getLoadYangAssignmentsData().getTotalYangPath();
      String totalNamespaces = entry.getLoadYangAssignmentsData().getTotalNamespaces();
      String totalKeywords = entry.getLoadYangAssignmentsData().getTotalKeywords();
      List<MappingPathElement> entryPath = UseCaseMapping.createPathList(totalYangPath, totalNamespaces, totalKeywords);
      fillValueAndOptionalWarning(entry, entryPath, mappings);
    }
  }


  private void fillValueAndOptionalWarning(UseCaseAssignmentTableData entry, List<MappingPathElement> entryPath, List<UseCaseMapping> mappings) {
    if (primitives.contains(entry.getType())) {
      String value = getPrimitiveValue(entryPath, mappings);
      entry.unversionedSetValue(value);
    }
    else {
      handleValueAndOptionalWarningOfContainer(entryPath, mappings, entry);
    }
  }


  private String getPrimitiveValue(List<MappingPathElement> entryPath, List<UseCaseMapping> mappings) {
    for (UseCaseMapping mapping : mappings) {
      if (MappingPathElement.compareLists(entryPath, mapping.createPathList()) == 0) {
        return mapping.getValue();
      }
    }
    return "";
  }


  private void handleValueAndOptionalWarningOfContainer(List<MappingPathElement> entryPath, List<UseCaseMapping> mappings,
                                                        UseCaseAssignmentTableData entry) {
    int subAssignments = 0;
    String elemtype = entryPath.get(entryPath.size() - 1).getKeyword();    
    boolean isChoice = Constants.TYPE_CHOICE.equals(elemtype);
    Set<String> caseSet = isChoice ? new HashSet<>() : null;

    for (UseCaseMapping mapping : mappings) {
      List<MappingPathElement> mappingPath = mapping.createPathList();
      if (MappingPathElement.isMoreSpecificPath(entryPath, mappingPath)) {
        subAssignments++;
        if (isChoice && (mappingPath.size() > entryPath.size())) {
          MappingPathElement childElem = mappingPath.get(entryPath.size());
          if (Constants.TYPE_CASE.equals(childElem.getKeyword())) {
            caseSet.add(childElem.getYangPath());
          }
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
