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
import java.util.List;

import org.w3c.dom.Document;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;

import xdev.yang.impl.Constants;
import xmcp.yang.OperationTableData;
import xmcp.yang.fman.OperationSignatureEntry;

public class LoadOperationSignature {

  public Container loadSignature(OperationTableData operation) {
    String fqn = operation.getOperationGroup();
    String workspace = operation.getRuntimeContext();
    String operationName = operation.getOperation();
    List<OperationSignatureEntry> inputs = new ArrayList<>();
    List<OperationSignatureEntry> outputs = new ArrayList<>();
    XynaObjectList<OperationSignatureEntry> inputList = new XynaObjectList<OperationSignatureEntry>(inputs, OperationSignatureEntry.class);
    XynaObjectList<OperationSignatureEntry> outputList = new XynaObjectList<OperationSignatureEntry>(outputs, OperationSignatureEntry.class);
  
    Pair<Integer, Document> meta = OperationAssignmentUtils.loadOperationMeta(fqn, workspace, operationName);
    if(meta == null) {
      return null;
    }

    inputList = createList(meta.getSecond(), Constants.VAL_LOCATION_INPUT);
    outputList = createList(meta.getSecond(), Constants.VAL_LOCATION_OUTPUT);
    
    Container result = new Container(inputList, outputList);
    return result;
  }
  
  private XynaObjectList<OperationSignatureEntry> createList(Document meta, String location) {
    List<OperationSignatureEntry> list = new ArrayList<>();
    
    List<OperationSignatureVariable> variables = OperationSignatureVariable.loadSignatureEntries(meta, location);
    for(int i=0; i<variables.size(); i++) {
      OperationSignatureEntry.Builder builder = new OperationSignatureEntry.Builder();
      OperationSignatureVariable variable = variables.get(i);
      builder.fqn(variable.getFqn());
      builder.index(i);
      builder.location(location);
      builder.variableName(variable.getVarName());
      list.add(builder.instance());
    }
    
    XynaObjectList<OperationSignatureEntry> result = new XynaObjectList<OperationSignatureEntry>(list, OperationSignatureEntry.class);
    return result;
  }
}
