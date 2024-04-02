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
package com.gip.xyna.openapi.codegen;

import java.util.List;
import java.util.Objects;

import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.DefaultCodegen;

import com.gip.xyna.openapi.codegen.factory.XynaCodegenFactory;

public class XynaCodegenProviderOperation extends XynaCodegenOperation {
  
  final String implLabel;
  final String implVarName;
  final String implDescription;

  final int parameterId;
  final String parameterLabel;
  final String parameterVarName;
  final String parameterRefName;
  final String parameterRefPath;

  final int responseId;
  
  final String endpointWorkflowLabel;
  final String endpointWorkflowTypeName;
  final String endpointWorkflowPath;
  
  final String filterRegexPath;
  
  public XynaCodegenProviderOperation(XynaCodegenFactory factory, CodegenOperation operation, DefaultCodegen gen, String pathPrefix, int id) {
    super(factory, operation, gen, pathPrefix);
    
    implLabel = baseLabel;
    implVarName = baseVarName;
    implDescription = operation.summary + "\n" + operation.notes;
    
    parameterId = id;
    parameterLabel = baseLabel + " Parameter";
    parameterVarName = baseVarName + "Parameter";
    parameterRefName = getPropertyClassName(); 
    parameterRefPath = basePath + ".request";
    
    responseId = id + 1;
    
    endpointWorkflowLabel = baseLabel + " Endpoint";
    endpointWorkflowTypeName = baseRefName + "Endpoint";
    endpointWorkflowPath = basePath + ".wf";
    
    filterRegexPath = buildFilterRegexp(operation, pathParams);
  }
  
  @Override
  protected String getPropertyClassName() {
    return baseRefName + "Parameter";
  }
  
  public String buildFilterRegexp(CodegenOperation operation, List<XynaCodegenProperty> pathParams) {
    String regexPath = operation.path;
    for(XynaCodegenProperty param : pathParams) {
      if (param.isPrimitive) {
        if (param.javaType.equals("Integer") || param.javaType.equals("Long") || param.javaType.equals("Double") || param.javaType.equals("Float")) {
          regexPath = regexPath.replaceAll("\\{" + param.propLabel + "\\}", "(?<" + param.propLabel + ">[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?)");
        } else if (param.javaType.equals("String")) {
          regexPath = regexPath.replaceAll("\\{" + param.propLabel + "\\}", "(?<" + param.propLabel + ">[^/?]*)");
        } else if (param.javaType.equals("Boolean")) {
          regexPath = regexPath.replaceAll("\\{" + param.propLabel + "\\}", "(?<" + param.propLabel + ">([fF][aA][lL][sS][eE])|([tT][rR][uU][eE]))");
        }
      }
    }
    return regexPath;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!super.equals(o)) return false;
    if (!(o instanceof XynaCodegenProviderOperation)) return false;
    XynaCodegenProviderOperation that = (XynaCodegenProviderOperation) o;
    return
        Objects.equals(implLabel, that.implLabel) &&
        Objects.equals(implVarName, that.implVarName) &&
        Objects.equals(implDescription, that.implDescription) &&

        parameterId == that.parameterId &&
        Objects.equals(parameterLabel, that.parameterLabel) &&
        Objects.equals(parameterVarName, that.parameterVarName) &&
        Objects.equals(parameterRefName, that.parameterRefName) &&
        Objects.equals(parameterRefPath, that.parameterRefPath) &&

        responseId == that.responseId &&
        
        Objects.equals(endpointWorkflowLabel, that.endpointWorkflowLabel) &&
        Objects.equals(endpointWorkflowTypeName, that.endpointWorkflowTypeName) &&
        Objects.equals(endpointWorkflowPath, that.endpointWorkflowPath) &&

        Objects.equals(filterRegexPath, that.filterRegexPath);
  }

  @Override
  public int hashCode() {
    int hash = super.hashCode();
    hash = 89 * Objects.hash(implLabel, implVarName, implDescription,
                            parameterId, parameterLabel, parameterVarName, parameterRefName, parameterRefPath,
                            responseId,
                            endpointWorkflowLabel, endpointWorkflowTypeName, endpointWorkflowPath,
                            filterRegexPath);
    return hash;
  }
  
  @Override
  protected void toString(StringBuilder sb) {
    if (sb == null) {
      return;
    }
    super.toString(sb);
    sb.append(",\n    ").append("implLabel='").append(implLabel).append('\'');
    sb.append(",\n    ").append("implVarName='").append(implVarName).append('\'');
    sb.append(",\n    ").append("implDescription='").append(String.valueOf(implDescription).replace("\n", "\\n")).append('\'');

    sb.append(",\n    ").append("parameterId='").append(parameterId).append('\'');
    sb.append(",\n    ").append("parameterLabel='").append(parameterLabel).append('\'');
    sb.append(",\n    ").append("parameterVarName='").append(parameterVarName).append('\'');
    sb.append(",\n    ").append("parameterRefName='").append(parameterRefName).append('\'');
    sb.append(",\n    ").append("parameterRefPath='").append(parameterRefPath).append('\'');

    sb.append(",\n    ").append("responseId='").append(responseId).append('\'');
    
    sb.append(",\n    ").append("endpointWorkflowLabel='").append(endpointWorkflowLabel).append('\'');
    sb.append(",\n    ").append("endpointWorkflowTypeName='").append(endpointWorkflowTypeName).append('\'');
    sb.append(",\n    ").append("endpointWorkflowPath='").append(endpointWorkflowPath).append('\'');
    
    sb.append(",\n    ").append("filterRegexPath='").append(filterRegexPath).append('\'');
  }
}
