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

import java.util.Objects;

import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.DefaultCodegen;

import com.gip.xyna.openapi.codegen.factory.XynaCodegenFactory;

public class XynaCodegenClientOperation extends XynaCodegenOperation {
  
  final String sendLabel;
  final String sendVarName;
  final String sendRefName;
  final String sendRefPath;

  final String parseResponseLabel;
  final String parseResponseRefName;
  final String parseResponseRefPath;
  
  final String requestWorkflowLabel;
  final String requestWorkflowTypeName;
  final String requestWorkflowPath;
  final String requestWorkflowURLPath;
  
  public XynaCodegenClientOperation(XynaCodegenFactory factory, CodegenOperation operation, DefaultCodegen gen, String pathPrefix, int id) {
     super(factory, operation, gen, pathPrefix);
    
    parseResponseLabel = "Parse " + baseLabel + " Response";
    parseResponseRefName = "Parse" + baseRefName + "Response";
    parseResponseRefPath = basePath + ".request";
     
    sendLabel = baseLabel + " Send Parameter";
    sendVarName = baseVarName + "SendParameter";
    sendRefName = baseRefName + "SendParameter";
    sendRefPath = basePath + ".send";
        
    requestWorkflowLabel = "Request " +  baseLabel;
    requestWorkflowTypeName = "Request" + baseRefName;
    requestWorkflowPath = basePath + ".wf";
    requestWorkflowURLPath = buildPath(operation);
  }
  
  public String buildPath(CodegenOperation operation) {
    String result = "\"" + operation.path;
    if (!result.endsWith("}")) {
      result = result + "\"";
    }
    for (XynaCodegenProperty pathPara: pathParams) {
      result = result.replaceAll("\\{" + pathPara.propLabel + "\\}$", "\", %1%." + pathPara.propVarName);
      result = result.replaceAll("\\{" + pathPara.propLabel + "\\}", "\", %1%." + pathPara.propVarName + ", \"");
    }
    return result;
  }
  
  
  @Override
  protected String getPropertyClassName() {
    return responseRefName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!super.equals(o)) return false;
    if (!(o instanceof XynaCodegenClientOperation)) return false;
    XynaCodegenClientOperation that = (XynaCodegenClientOperation) o;
    return
        Objects.equals(sendLabel, that.sendLabel) &&
        Objects.equals(sendVarName, that.sendVarName) &&
        Objects.equals(sendRefName, that.sendRefName) &&
        Objects.equals(sendRefPath, that.sendRefPath) &&

        Objects.equals(parseResponseLabel, that.parseResponseLabel) &&
        Objects.equals(parseResponseRefName, that.parseResponseRefName) &&
        Objects.equals(parseResponseRefPath, that.parseResponseRefPath) &&
        
        Objects.equals(requestWorkflowLabel, that.requestWorkflowLabel) &&
        Objects.equals(requestWorkflowTypeName, that.requestWorkflowTypeName) &&
        Objects.equals(requestWorkflowPath, that.requestWorkflowPath) &&
        Objects.equals(requestWorkflowURLPath, that.requestWorkflowURLPath);
  }

  @Override
  public int hashCode() {
    int hash = super.hashCode();
    hash = 89 * Objects.hash(sendLabel, sendRefName, sendRefPath, sendVarName,
                             parseResponseLabel, parseResponseRefName, parseResponseRefPath,
                            requestWorkflowLabel, requestWorkflowTypeName, requestWorkflowPath, requestWorkflowURLPath);
    return hash;
  }

  @Override
  protected void toString(StringBuilder sb) {
    if (sb == null) {
      return;
    }
    sb.append(",\n    ").append("sendLabel='").append(sendLabel).append('\'');
    sb.append(",\n    ").append("sendVarName='").append(sendVarName).append('\'');
    sb.append(",\n    ").append("sendRefName='").append(sendRefName).append('\'');
    sb.append(",\n    ").append("sendRefPath='").append(sendRefPath).append('\'');

    sb.append(",\n    ").append("parseResponseLabel='").append(parseResponseLabel).append('\'');
    sb.append(",\n    ").append("parseResponseRefName='").append(parseResponseRefName).append('\'');
    sb.append(",\n    ").append("parseResponseRefPath='").append(parseResponseRefPath).append('\'');

    sb.append(",\n    ").append("requestWorkflowLabel='").append(requestWorkflowLabel).append('\'');
    sb.append(",\n    ").append("requestWorkflowTypeName='").append(requestWorkflowTypeName).append('\'');
    sb.append(",\n    ").append("requestWorkflowPath='").append(requestWorkflowPath).append('\'');
    sb.append(",\n    ").append("requestWorkflowURLPath='").append(requestWorkflowURLPath).append('\'');
  }
}
