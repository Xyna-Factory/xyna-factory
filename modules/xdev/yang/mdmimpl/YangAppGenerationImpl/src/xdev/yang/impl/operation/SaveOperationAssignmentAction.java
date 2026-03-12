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

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.xprc.XynaOrderServerExtension;

import xdev.yang.impl.operation.implementation.ImplementationProvider;
import xdev.yang.impl.operation.implementation.OpImplProviderSelection;
import xmcp.yang.OperationAssignmentTableData;


public class SaveOperationAssignmentAction {

  public void saveOperationAssignment(XynaOrderServerExtension order, OperationAssignmentTableData data) {
    String fqn = data.getLoadYangAssignmentsData().getFqn();
    String workspaceName = data.getLoadYangAssignmentsData().getWorkspaceName();
    String operationName = data.getLoadYangAssignmentsData().getOperation();

    try(Operation operation = Operation.open(order, fqn, workspaceName, operationName)) {
      Document meta = operation.getMeta();
      ImplementationProvider implProvider = new OpImplProviderSelection().selectProvider(meta);
      updateMeta(meta, data);
      operation.updateMeta();
      String newImpl = implProvider.createImpl(meta, operation.getInputVarNames());
      operation.updateImplementation(newImpl);
      operation.save();
      operation.deploy();
    } catch(Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void updateMeta(Document meta, OperationAssignmentTableData data) {
    String totalYangPath = data.getLoadYangAssignmentsData().getTotalYangPath();
    String totalNamespaces = data.getLoadYangAssignmentsData().getTotalNamespaces();
    String totalKeywords = data.getLoadYangAssignmentsData().getTotalKeywords();
    String value = data.getValue();
    boolean update = false;  
    List<Element> mappings = OperationMapping.loadMappingElements(meta);
    List<MappingPathElement> pathList = OperationMapping.createPathList(totalYangPath, totalNamespaces, totalKeywords);
    for(Element mappingEle : mappings) {
      OperationMapping mapping = OperationMapping.loadOperationMapping(mappingEle);
      mapping.setValue(value);
      if(mapping.match(pathList)) {
        mapping.updateNode(mappingEle);
        update = true;
        break;
      }
    }
    if(!update) {
      OperationMapping mapping = new OperationMapping(totalYangPath, totalNamespaces, value, totalKeywords);
      mapping.createAndAddElement(meta);
    }
  }
  

  


  
}
