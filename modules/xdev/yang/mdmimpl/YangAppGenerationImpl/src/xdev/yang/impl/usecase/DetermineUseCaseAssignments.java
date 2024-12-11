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

import org.w3c.dom.Document;
import com.gip.xyna.utils.collections.Pair;

import xdev.yang.impl.YangCapabilityUtils;

import org.yangcentral.yangkit.model.api.stmt.Module;

import xmcp.yang.LoadYangAssignmentsData;
import xmcp.yang.UseCaseAssignmentTableData;



public class DetermineUseCaseAssignments {


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
      for (UseCaseMapping mapping : mappings) {
        if (mapping.getMappingYangPath().equals(entry.getLoadYangAssignmentsData().getTotalYangPath()) &&
            mapping.getNamespace().equals(entry.getLoadYangAssignmentsData().getTotalNamespaces())) {
          entry.unversionedSetValue(mapping.getValue());
        }
      }
    }
  }
  


}
