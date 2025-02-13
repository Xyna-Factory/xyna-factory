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

import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.xprc.XynaOrderServerExtension;
import xmcp.yang.UseCaseAssignmentTableData;

public class SaveUsecaseAssignmentAction {


  public void saveUsecaseAssignment(XynaOrderServerExtension order, UseCaseAssignmentTableData data) {
    String fqn = data.getLoadYangAssignmentsData().getFqn();
    String workspaceName = data.getLoadYangAssignmentsData().getWorkspaceName();
    String usecaseName = data.getLoadYangAssignmentsData().getUsecase();
    UsecaseImplementationProvider implProvider = new UsecaseImplementationProvider();

    try(Usecase usecase = Usecase.open(order, fqn, workspaceName, usecaseName)) {
      Document meta = usecase.getMeta();
      updateMeta(meta, data);
      usecase.updateMeta();
      String newImpl = implProvider.createImpl(meta, usecase.getInputVarNames());
      usecase.updateImplementation(newImpl);
      usecase.save();
      usecase.deploy();
    } catch(Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void updateMeta(Document meta, UseCaseAssignmentTableData data) {
    String totalYangPath = data.getLoadYangAssignmentsData().getTotalYangPath();
    String totalNamespaces = data.getLoadYangAssignmentsData().getTotalNamespaces();
    String totalKeywords = data.getLoadYangAssignmentsData().getTotalKeywords();
    String value = data.getValue();
    boolean update = false;  
    List<Element> mappings = UseCaseMapping.loadMappingElements(meta);
    List<MappingPathElement> pathList = UseCaseMapping.createPathList(totalYangPath, totalNamespaces, totalKeywords);
    for(Element mappingEle : mappings) {
      UseCaseMapping mapping = UseCaseMapping.loadUseCaseMapping(mappingEle);
      mapping.setValue(value);
      if(mapping.match(pathList)) {
        mapping.updateNode(mappingEle);
        update = true;
        break;
      }
    }
    if(!update) {
      UseCaseMapping mapping = new UseCaseMapping(totalYangPath, totalNamespaces, value, totalKeywords);
      mapping.createAndAddElement(meta);
    }
  }
  

  


  
}
