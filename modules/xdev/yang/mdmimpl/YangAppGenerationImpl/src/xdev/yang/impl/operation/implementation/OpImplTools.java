/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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

package xdev.yang.impl.operation.implementation;

import java.util.List;
import java.util.Set;

import org.w3c.dom.Document;

import xdev.yang.impl.Constants;
import xdev.yang.impl.operation.OperationSignatureVariable;

public class OpImplTools {

  public static final Set<String> hiddenYangKeywords = Set.of(
                                                              Constants.TYPE_GROUPING,
                                                              Constants.TYPE_USES,
                                                              Constants.TYPE_CHOICE, 
                                                              Constants.TYPE_CASE,
                                                              Constants.TYPE_LEAFLIST,
                                                              Constants.TYPE_LIST);
  
  
  public void createVariables(StringBuilder result, Document meta, List<String> inputVarNames) {
    List<OperationSignatureVariable> variables = OperationSignatureVariable.loadSignatureEntries(meta, Constants.VAL_LOCATION_INPUT);  
    for (int i = 0; i < variables.size(); i++) {
      OperationSignatureVariable variable = variables.get(i);
      String serviceInputVarName = inputVarNames.get(i + 1);
      String fqn = variable.getFqn();
      String customVarName = variable.getVarName();
      result.append(fqn).append(" ").append(customVarName).append(" = ").append(serviceInputVarName).append(";\n");
    }
  }
  
  
  public String determineMappingString(String mappingValue) {
    if (mappingValue.startsWith("\"")) {
      return mappingValue;
    }
    int firstDot = mappingValue.indexOf(".");
    if (firstDot == -1) {
      return String.format("String.valueOf(%s)", mappingValue);
    } else {
      String variable = mappingValue.substring(0, firstDot);
      String path = mappingValue.substring(firstDot + 1);
      return String.format("String.valueOf(((GeneralXynaObject)%s).get(\"%s\"))", variable, path);
    }
  }

  
}
