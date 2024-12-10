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
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;

import xdev.yang.impl.Constants;
import xmcp.yang.UseCaseTableData;
import xmcp.yang.fman.UsecaseSignatureEntry;

public class LoadUsecaseSignature {

  public Container loadSignature(UseCaseTableData usecase) {
    String fqn = usecase.getUsecaseGroup();
    String workspace = usecase.getRuntimeContext();
    String usecaseName = usecase.getUseCase();
    List<UsecaseSignatureEntry> inputs = new ArrayList<>();
    List<UsecaseSignatureEntry> outputs = new ArrayList<>();
    XynaObjectList<UsecaseSignatureEntry> inputList = new XynaObjectList<UsecaseSignatureEntry>(inputs, UsecaseSignatureEntry.class);
    XynaObjectList<UsecaseSignatureEntry> outputList = new XynaObjectList<UsecaseSignatureEntry>(outputs, UsecaseSignatureEntry.class);
  
    Pair<Integer, Document> meta = UseCaseAssignmentUtils.loadOperationMeta(fqn, workspace, usecaseName);
    if(meta == null) {
      return null;
    }

    inputList = createList(meta.getSecond(), Constants.VAL_LOCATION_INPUT);
    outputList = createList(meta.getSecond(), Constants.VAL_LOCATION_OUTPUT);
    
    Container result = new Container(inputList, outputList);
    return result;
  }
  
  private XynaObjectList<UsecaseSignatureEntry> createList(Document meta, String location) {
    List<UsecaseSignatureEntry> list = new ArrayList<>();
    
    List<UsecaseSignatureVariable> variables = UsecaseSignatureVariable.loadSignatureEntries(meta, location);
    for(int i=0; i<variables.size(); i++) {
      UsecaseSignatureEntry.Builder builder = new UsecaseSignatureEntry.Builder();
      UsecaseSignatureVariable variable = variables.get(i);
      builder.fqn(variable.getFqn());
      builder.index(i);
      builder.location(location);
      builder.variableName(variable.getVarName());
      list.add(builder.instance());
    }
    
    XynaObjectList<UsecaseSignatureEntry> result = new XynaObjectList<UsecaseSignatureEntry>(list, UsecaseSignatureEntry.class);
    return result;
  }
}
