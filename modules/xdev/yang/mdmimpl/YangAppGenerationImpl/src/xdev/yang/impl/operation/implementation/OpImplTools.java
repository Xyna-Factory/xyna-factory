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
import java.util.Optional;
import java.util.Set;

import org.w3c.dom.Document;

import xdev.yang.impl.Constants;
import xdev.yang.impl.operation.ListConfiguration;
import xdev.yang.impl.operation.MappingPathElement;
import xdev.yang.impl.operation.OperationMapping;
import xdev.yang.impl.operation.OperationSignatureVariable;
import xdev.yang.impl.operation.ListConfiguration.DynamicListLengthConfig;

public class OpImplTools {

  public static final Set<String> hiddenYangKeywords = Set.of(
                                                              Constants.TYPE_GROUPING,
                                                              Constants.TYPE_USES,
                                                              Constants.TYPE_CHOICE, 
                                                              Constants.TYPE_CASE,
                                                              Constants.TYPE_LEAFLIST,
                                                              Constants.TYPE_LIST);
  
  
  public static final Set<String> listKeywords = Set.of(
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

  
  public ListConfiguration isDynamicList(List<MappingPathElement> mappingElements, List<ListConfiguration> listConfigs) {
    String keyword = mappingElements.get(mappingElements.size()-1).getKeyword();
    if (Constants.TYPE_LEAFLIST.equals(keyword) || Constants.TYPE_LIST.equals(keyword)) {
      for (ListConfiguration listConfig : listConfigs) {
        if (!(listConfig.getConfig() instanceof DynamicListLengthConfig)) {
          continue;
        }
        List<MappingPathElement> listPath = OperationMapping.createPathList(listConfig.getYang(), listConfig.getNamespaces(), listConfig.getKeywords());
        if (MappingPathElement.compareLists(mappingElements, listPath) == 0) {
          return listConfig;
        }
      }
    }
    return null;
  }
  
  
  public String determineMappingValueObject(String mappingValue) {
    int firstDot = mappingValue.indexOf(".");
    if (firstDot == -1 || mappingValue.startsWith("\"")) {
      return mappingValue;
    } else {
      String variable = mappingValue.substring(0, firstDot);
      String path = mappingValue.substring(firstDot + 1);
      return String.format("%s.get(\"%s\")", variable, path);
    }
  }

  
  public String cleanupTag(String tag) {
    int listIndexSeparatorIndex = tag.indexOf(Constants.LIST_INDEX_SEPARATOR);
    if (listIndexSeparatorIndex > 0) {
      tag = tag.substring(listIndexSeparatorIndex + Constants.LIST_INDEX_SEPARATOR.length());
    }
    return tag;
  }
  
  
  public Optional<Integer> getOptionalConstListIndex(List<MappingPathElement> mappingList, int index) {
    if (index >= mappingList.size()) { return Optional.empty(); }
    // constant list index is written in element name of path element below list element
    MappingPathElement elem = mappingList.get(index + 1);
    String tag = elem.getYangPath();
    int listIndexSeparatorIndex = tag.indexOf(Constants.LIST_INDEX_SEPARATOR);
    if (listIndexSeparatorIndex < 1) { return Optional.empty(); }
    String val = tag.substring(0, listIndexSeparatorIndex);
    try {
      int ret = Integer.valueOf(val);
      return Optional.ofNullable(ret);
    } catch (Exception e) {
      // do nothing
    }
    return Optional.empty();
  }
  
}
